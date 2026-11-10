package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
/** Actual immutable crypt/core anatomy and native feature configuration, with volatile Delay separate. */
public final class CryptNativeExport {
 public static void write(ServerLevel level,ReimaginedLayoutPiece piece,StructureStart start,Path dir,boolean reopen)throws Exception {
  String prefix=reopen?"reopen-":"";var gson=new com.google.gson.Gson();var pretty=new com.google.gson.GsonBuilder().setPrettyPrinting().create();var entities=new ArrayList<Object>();var emitters=new ArrayList<Object>();var palette=new TreeMap<String,Integer>();var light=java.security.MessageDigest.getInstance("SHA-256");var ctx=StructurePieceSerializationContext.fromLevel(level);var tag=piece.createTag(ctx);
  try(var out=Files.newBufferedWriter(dir.resolve(prefix+"native-core.voxels.jsonl"));var site=Files.newBufferedWriter(dir.resolve(prefix+"native-site.voxels.jsonl"))) {
   for(int y=-20;y<=40;y++)for(int z=-32;z<=32;z++)for(int x=-32;x<=32;x++) {
    var at=piece.local(x,y,z);var state=level.getBlockState(at);boolean core=x>=-5&&x<=5&&z>=-5&&z<=5&&y>=-1&&y<=4;int blockLight=level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,at);
    var row=Map.of("x",x,"y",y,"z",z,"state",state.toString(),"collision",state.getCollisionShape(level,at).toAabbs().toString(),"fluid",net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(state.getFluidState().getType()).toString()+"/"+state.getFluidState().getAmount(),"blockLight",blockLight,"skyLight",level.getBrightness(net.minecraft.world.level.LightLayer.SKY,at));site.write(gson.toJson(row)+"\n");if(core){out.write(gson.toJson(row)+"\n");palette.merge(state.toString(),1,Integer::sum);light.update((byte)blockLight);}
    if(state.getLightEmission()>0)emitters.add(Map.of("local",List.of(x,y,z),"state",state.toString(),"emission",state.getLightEmission()));
    var be=level.getBlockEntity(at);if(be!=null){var full=be.saveWithFullMetadata(level.registryAccess());var stable=full.copy();stable.remove("Delay");entities.add(Map.of("local",List.of(x,y,z),"world",List.of(at.getX(),at.getY(),at.getZ()),"core",core,"state",state.toString(),"type",be.getClass().getSimpleName(),"fullNBT",full.toString(),"stableNBTWithoutDelay",stable.toString()));}
   }
  }
  int d=tag.getIntOr("CaveDirection",0),dx=d==0?1:d==1?-1:0,dz=d==2?1:d==3?-1:0;var endpoint=piece.local(dx*24,0,dz*24);var data=new LinkedHashMap<String,Object>();data.put("source","actual native crypt snapshot before qualification; saved generator/piece identify recipe");data.put("coreBounds",List.of(-5,-1,-5,5,4,5));data.put("siteBounds",List.of(-32,-20,-32,32,40,32));data.put("palette",palette);data.put("coreBlockLightHash",java.util.HexFormat.of().formatHex(light.digest()));data.put("blockEntities",entities);data.put("emitters",emitters);data.put("caveDirection",d);data.put("naturalEndpointLocal",List.of(dx*24,0,dz*24));data.put("naturalEndpointWorld",List.of(endpoint.getX(),endpoint.getY(),endpoint.getZ()));var rng=net.minecraft.util.RandomSource.create(tag.getLongOr("LayoutSeed",0));int hx=2+rng.nextInt(2),hz=2+rng.nextInt(2);data.put("nativeFeatureBoundsFromOriginalRNG",List.of(-hx-1,-1,-hz-1,hx+1,4,hz+1));data.put("nativeFeatureInteriorHalfExtents",List.of(hx,hz));data.put("nativeFeatureSeed",tag.getLongOr("LayoutSeed",0));data.put("nativeFeatureAlreadyPlaced",tag.getBooleanOr("NativeCrypt",false));data.put("pieces",start.getPieces().stream().map(p->p.createTag(ctx).toString()).toList());Files.writeString(dir.resolve(prefix+"native-anatomy.json"),pretty.toJson(data));
 }
}
