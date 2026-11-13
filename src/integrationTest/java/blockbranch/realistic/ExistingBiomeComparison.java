package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.BlockTags;

/** Native dimensions at the already captured270 sites. Heights are generator
 * cohort heights of native-confirmed roots, not heights inferred from photos.
 * This finite audit records evidence; no density or aesthetic pass target. */
public final class ExistingBiomeComparison {
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.existingBiomeComparisonDirectory"));
        try{
            var level=server.overworld();var random=level.getChunkSource().randomState();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var saved=RealisticGenerator.CODEC.codec().encodeStart(RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().getAsJsonObject();
            require(saved.get("shape_version").getAsInt()==270&&g.montaneForestDefinition()==0,"Actual captured original270 definition");
            var d=g.drainage(random);var c=g.climate(random);var rows=new ArrayList<Object>();
            var views=JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.existingBiomeComparisonViews")))).getAsJsonArray();
            var seen=new HashSet<String>();
            for(var value:views){var view=value.getAsJsonObject();var target=view.getAsJsonArray("target");int x=target.get(0).getAsInt(),z=target.get(2).getAsInt();if(!seen.add(x+","+z))continue;
                int minX=x-24,minZ=z-24,maxX=x+23,maxZ=z+23;var confirmed=new ArrayList<WoodlandTrees.Root>();
                for(var root:WoodlandTrees.roots(level.getSeed(),minX,minZ,maxX,maxZ,d,c,g.vegetationFlags())){
                    if(root.x()<minX||root.x()>maxX||root.z()<minZ||root.z()>maxZ)continue;
                    if(ColdUplands.surfaceKind(level.getSeed(),root.x(),root.z(),d,c)>0||Badlands.surfaceKind(level.getSeed(),root.x(),root.z(),d,c)>0)continue;
                    var actual=level.getBlockState(new BlockPos(root.x(),root.y(),root.z()));
                    if(actual.is(BlockTags.LOGS))confirmed.add(root);
                }
                var heights=confirmed.stream().map(WoodlandTrees.Root::height).sorted().toList();double nearest=Double.POSITIVE_INFINITY;
                for(int a=0;a<confirmed.size();a++)for(int b=a+1;b<confirmed.size();b++)nearest=Math.min(nearest,Math.hypot(confirmed.get(a).x()-confirmed.get(b).x(),confirmed.get(a).z()-confirmed.get(b).z()));
                int leafColumns=0,wet=0,reads=0;var floor=new TreeMap<String,Integer>();
                for(int wz=minZ;wz<=maxZ;wz++)for(int wx=minX;wx<=maxX;wx++){
                    int h=d.sample(wx,wz).height();boolean leaves=false;
                    for(int y=h+1;y<=h+40;y++){if(level.getBlockState(new BlockPos(wx,y,wz)).is(BlockTags.LEAVES))leaves=true;reads++;}
                    if(leaves)leafColumns++;if(d.sample(wx,wz).waterLevel()!=DrainageNetwork.DRY)wet++;
                    floor.merge(level.getBlockState(new BlockPos(wx,h+1,wz)).getBlock().toString(),1,Integer::sum);
                }
                var climate=c.sample(x,z,d::sample);var row=new LinkedHashMap<String,Object>();row.put("view",view.get("view").getAsInt());row.put("x",x);row.put("z",z);row.put("selectedBiome",view.get("selectedBiome").getAsString());row.put("seed",level.getSeed());row.put("elevation",d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL);row.put("modelMeanC",climate.meanTemperatureC());row.put("modelMoisture",climate.moisture());row.put("windowColumns",2304);row.put("nativeStateReads",reads);row.put("leafColumnsAboveGroundWithin40m",leafColumns);row.put("wetColumns",wet);row.put("nativeConfirmedOrdinaryRoots",confirmed.size());row.put("confirmedGeneratorHeights",heights);row.put("minimumConfirmedRootSpacingMetres",Double.isFinite(nearest)?nearest:null);row.put("floorAboveGround",floor);rows.add(row);
            }
            Files.writeString(dir.resolve("comparison.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("schema",270,"seed",level.getSeed(),"sites",rows,"limits","Finite48x48m native windows. Ordinary root families only; other tree families are not counted as absent. Heights are generator cohorts of native-confirmed roots, not empirical stand calibration; leaf columns are within40m of local ground. Photographs provide structural context, not measured spacing. No water-transition claim when no wet columns occur."))+"\n");
            Files.writeString(dir.resolve("result.txt"),"PASS existing captured270 native dimension audit\n");
        }catch(Throwable failure){Blockbranch.LOGGER.error("EXISTING_BIOME_COMPARISON_FAIL",failure);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private ExistingBiomeComparison(){}
}
