package blockbranch.realistic;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import net.minecraft.server.level.ServerLevel;

/** Opt-in sampled generator export. No chunks, raster interpolation or invented layers. */
public final class WorldMapExport {
 public static void write(ServerLevel level,RealisticGenerator g)throws Exception{
  var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());var resolver=g.getBiomeSource().createUncachedResolver(r);var border=level.getWorldBorder();
  var root=new JsonObject();root.addProperty("seed",r.seed());root.addProperty("shape_version",TerrainShape.SHAPE_VERSION);root.addProperty("source_commit",System.getProperty("blockbranch.mapCommit","unknown"));root.addProperty("units","blocks = metres; Y absolute; sea level 64; x/z horizontal, z latitude-proxy axis");
  root.addProperty("elevation_stage","DrainageNetwork.sample ground, without canopy, floating iceberg solids or local block/material height details; not a mesh-height export");
  root.addProperty("sampling","Row-major z then x, cell centres. Exact point samples; no area aggregation/interpolation. River and ice sheets can be missed at coarse spacing. Iceberg flag is habitat eligibility, NOT individual berg positions.");
  root.addProperty("config","Bundled realistic preset with cold_ocean/deep_cold_ocean palette entries and icebergs=true");
  root.addProperty("sandy_coasts_enabled",g.sandyCoastsEnabled());root.addProperty("cold_oceans_enabled",g.coldOceansEnabled());root.addProperty("icebergs_enabled",g.icebergsEnabled());
  var bounds=new JsonArray();for(double v:new double[]{border.getMinX(),border.getMinZ(),border.getMaxX(),border.getMaxZ()})bounds.add(v);root.add("actual_world_bounds",bounds);
  var names=new java.util.LinkedHashMap<String,Integer>();var views=new JsonArray();
  double[][] areas={{border.getMinX(),border.getMinZ(),border.getMaxX(),border.getMaxZ()},{-262144,-262144,262144,262144},{-117024,591520,-51488,657056},{-2004029,-2043595,-1995837,-2035403}};
  String[] titles={"Whole world","Central 524 km","Spawn region 65 km","Polar iceberg region 8 km"};
  for(int k=0;k<areas.length;k++){
   int n=k==0?256:128;double[] b=areas[k];double sx=(b[2]-b[0])/n,sz=(b[3]-b[1])/n;var v=new JsonObject();v.addProperty("name",titles[k]);v.addProperty("width",n);v.addProperty("height",n);v.addProperty("origin_x",b[0]);v.addProperty("origin_z",b[1]);v.addProperty("step_x",sx);v.addProperty("step_z",sz);
   var h=new JsonArray();var depth=new JsonArray();var temp=new JsonArray();var moisture=new JsonArray();var biomes=new JsonArray();var flags=new JsonArray();
   for(int iz=0;iz<n;iz++)for(int ix=0;ix<n;ix++){
    int x=(int)Math.floor(b[0]+(ix+.5)*sx),z=(int)Math.floor(b[1]+(iz+.5)*sz);var s=d.sample(x,z);var climate=c.sample(x,z,d::sample);boolean ocean=d.oceanWater(x,z,s),berg=g.icebergsEnabled()&&IcebergField.habitat(r.seed(),x,z,d,c);
    String id=resolver.getNoiseBiome(x>>2,0,z>>2).unwrapKey().orElseThrow().identifier().toString();int index=names.computeIfAbsent(id,unused->names.size());
    int bits=(ocean?1:0)|(d.terminalLakeWater(x,z,s)?2:0)|((RiverChannel.contains(x,z,d)||RoutedRiver.contains(r.seed(),x,z,d,c))?4:0)|(berg?8:0)|(!berg&&FrozenOcean.ice(r.seed(),x,z,d,c)?16:0);
    h.add(s.height());depth.add(s.waterLevel()==DrainageNetwork.DRY?0:Math.max(0,s.waterLevel()-s.height()-1));temp.add((int)Math.round(climate.meanTemperatureC()*10));moisture.add((int)Math.round(climate.moisture()*1000));biomes.add(index);flags.add(bits);
   }
   v.add("elevation_m",h);v.add("water_depth_m",depth);v.add("mean_air_C_x10",temp);v.add("moisture_x1000",moisture);v.add("biome_index",biomes);v.add("flags",flags);views.add(v);
   blockbranch.Blockbranch.LOGGER.info("WORLD_MAP_SAMPLED view={} samples={}",titles[k],n*n);
  }
  var dictionary=new JsonArray();names.keySet().forEach(dictionary::add);root.add("biome_dictionary",dictionary);root.add("views",views);root.addProperty("flag_bits","1 connected ocean; 2 terminal lake; 4 physical channel/routed river; 8 iceberg habitat eligibility; 16 actual static sea-ice point (iceberg habitat excluded)");
  Path out=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).resolveSibling("world-map.json.gz");try(var stream=new GZIPOutputStream(Files.newOutputStream(out))){stream.write(new Gson().toJson(root).getBytes(StandardCharsets.UTF_8));}
  blockbranch.Blockbranch.LOGGER.info("WORLD_MAP_EXPORT_PASS path={} bytes={} biomeIds={}",out,Files.size(out),names.size());
 }
 private WorldMapExport(){}
}
