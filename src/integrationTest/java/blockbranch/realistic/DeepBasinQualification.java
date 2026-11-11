package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import com.google.gson.*;
import com.mojang.serialization.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

/** New physical surfaces, old untouched terrain, authentic reopen and base API contracts. */
public final class DeepBasinQualification {
    private static final int[][] SITES={{-1744000,-1904000},{-1904000,-1904000},{-1872000,-1904000},
        {-1648000,-1904000},{-1328000,-1904000},{-1168000,-1904000},{-1040000,-1904000},
        {-336000,-1904000},{-400000,-1744000},{-1008000,-1680000},{1904000,-1552000},{-1136000,-1776000}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.deepBasinResult"));
        try{
            var level=server.overworld();var random=level.getChunkSource().randomState();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());JsonObject fresh;
            try(var in=new InputStreamReader(Objects.requireNonNull(DeepBasinQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),StandardCharsets.UTF_8)){fresh=JsonParser.parseReader(in).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();}
            if(fresh.get("shape_version").getAsInt()==264){
                require(fresh.get("summer_dry_definition").getAsInt()==1,"Known complete summer-dry wrapper");
                fresh.remove("summer_dry_definition");fresh.addProperty("shape_version",263);fresh.getAsJsonObject("additional_biomes").remove(SummerDryWoodland.KEY);
            }
            if(fresh.get("shape_version").getAsInt()==263){
                require(fresh.get("land_ice_definition").getAsInt()==1,"Known complete land-ice wrapper");
                fresh.remove("land_ice_definition");fresh.addProperty("shape_version",262);fresh.getAsJsonObject("additional_biomes").remove(LandIceField.KEY);
            }
            fresh.remove("type");if(fresh.has("continental_ice_definition")){fresh.remove("continental_ice_definition");fresh.addProperty("shape_version",264);fresh.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);}require(fresh.get("shape_version").getAsInt()==262,"Complete current preset");
            var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Exact262 roundtrip");
            var old=fresh.deepCopy();old.remove("deep_basin_relief_definition");old.addProperty("shape_version",261);var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(retained.deepBasinReliefDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent gate retains261 exactly");
            for(int version:new int[]{82,257,258,259,260,261,263}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);reject(ops,bad);}
            for(String key:new String[]{"deep_basin_relief_definition","abyssal_trace_definition","cool_desert_definition","dry_forest_definition","surface_ownership_definition"}){var bad=fresh.deepCopy();bad.remove(key);reject(ops,bad);}
            for(int value:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("deep_basin_relief_definition",value);reject(ops,bad);}
            var malformed=fresh.deepCopy();malformed.addProperty("deep_basin_relief_definition","invalid");reject(ops,malformed);
            @SuppressWarnings("unchecked") var reader=(MapCodec<Object>)Class.forName("blockbranch.realistic.Published261RealisticGenerator").getField("CODEC").get(null);
            require(reader.codec().parse(ops,fresh).error().isPresent()&&reader.codec().parse(ops,old).result().isPresent(),"Original261 reader rejects262 and accepts261");
            var published=reader.codec().parse(ops,old).getOrThrow();var publishedState=published.getClass().getDeclaredMethod("surfaceState",int.class,int.class,int.class,net.minecraft.world.level.levelgen.RandomState.class);publishedState.setAccessible(true);
            boolean legacy=Boolean.getBoolean("blockbranch.deepBasinLegacy"),reopen=Boolean.getBoolean("blockbranch.deepBasinReopen");JsonObject baseline;
            try(var in=new InputStreamReader(Objects.requireNonNull(DeepBasinQualification.class.getResourceAsStream("/blockbranch-fixtures/deep-basin-reader-261/baseline.json")),StandardCharsets.UTF_8)){baseline=JsonParser.parseReader(in).getAsJsonObject();}
            if(!legacy){baseline.remove("nativeStates");baseline.remove("nativeSHA256");baseline.remove("nativeProfileSHA256");}
            var snapshot=Published261Snapshot.capture(level,retained,legacy);require(snapshot.equals(baseline),"Whole-original independent261 output oracle: "+snapshot);
            var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.deepBasinReliefDefinition()==(legacy?0:1),"Actual saved definition retained");
            var d=actual.drainage(random);var prior=retained.drainage(random);var climate=new ClimateField(random.seed());
            int editX=Published261Snapshot.POINTS[0][0]+1,editZ=Published261Snapshot.POINTS[0][1]+1,editY=d.sample(editX,editZ).height()-2;
            var edit=new BlockPos(editX,editY,editZ);int columns=0,states=0,changed=0,apiStates=0,apiHeights=0,traceStates=0;var chunks=new HashSet<Long>();var examples=new ArrayList<Object>();
            for(int i=0;i<SITES.length;i++){
                int siteChanges=0;
                for(int cz=(SITES[i][1]>>4)-1;cz<=(SITES[i][1]>>4)+1;cz++)for(int cx=(SITES[i][0]>>4)-1;cx<=(SITES[i][0]>>4)+1;cx++){
                    require(chunks.add((cx&0xffffffffL)|((long)cz<<32)),"Distinct fixed windows");var chunk=level.getChunk(cx,cz);var profile=((SparseColumn)chunk).blockbranch$terrain().base();var tag=profile.encode(level.palettedContainerFactory());var restored=TerrainProfile.decode(tag,level.palettedContainerFactory());
                    require(restored.encode(level.palettedContainerFactory()).equals(tag),"Unchanged profile codec roundtrip");
                    for(int lz=0;lz<16;lz++)for(int lx=0;lx<16;lx++){
                        int x=cx*16+lx,z=cz*16+lz;var before=prior.sample(x,z);var after=d.sample(x,z);int ground=after.height();columns++;
                        require(before.waterLevel()==after.waterLevel()&&before.basin()==after.basin()&&before.closed()==after.closed()&&before.regionalHeight()==after.regionalHeight()&&before.regionalWaterLevel()==after.regionalWaterLevel()&&climate.sample(x,z,prior::sample).equals(climate.sample(x,z,d::sample)),"Native physical-only change retains water/climate references");
                        if(before.height()!=ground){changed++;siteChanges++;require(!legacy&&(i<3||i==11),"Only intended finite offshore windows change");}
                        for(int y=Math.min(ground,before.height())-3;y<=Math.max(ground,before.height())+5;y++){
                            var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
                            if((legacy||reopen)&&pos.equals(edit)){require(state.is(Blocks.GOLD_BLOCK),"Actual saved edit survives");continue;}
                            require(state.equals(actual.surfaceState(x,y,z,random))&&state.equals(restored.state(lx,y,lz)),"Native/analytic/decoded profile at "+pos);states++;
                            if(legacy)require(state.equals(publishedState.invoke(published,x,y,z,random)),"Old-mode surrounding native columns versus published reader (shared-helper limit covered by original oracle)");
                            if(state.is(BenthicTraceContent.TRACE)){traceStates++;require(y==ground+1&&state.getFluidState().isSource()&&AbyssalTraces.eligible(random.seed(),x,z,d,climate,true),"Existing trace support/slope/water exclusions remain");}
                        }
                        if((lx&7)==0&&(lz&7)==0){
                            for(var bounds:List.of(LevelHeightAccessor.create(ground-3,9),LevelHeightAccessor.create(60,8))){
                                var col=actual.getBaseColumn(x,z,bounds,random);
                                for(int y=bounds.getMinY();y<=bounds.getMaxY();y++){var pos=new BlockPos(x,y,z);if((legacy||reopen)&&pos.equals(edit))continue;require(col.getBlock(y).equals(level.getBlockState(pos)),"Direct clipped base-column/native contract");apiStates++;}
                                for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(level.getBlockState(new BlockPos(x,y,z)))){expected=y+1;break;}require(actual.getBaseHeight(x,z,type,bounds,random)==expected,"Clipped native base height");apiHeights++;}
                            }
                            for(var type:Heightmap.Types.values()){require(actual.getBaseHeight(x,z,type,level,random)==chunk.getHeight(type,lx,lz)+1,"Full native heightmap");apiHeights++;}
                        }
                    }
                }
                examples.add(Map.of("center",SITES[i],"changedColumns",siteChanges,"chunkRadius",1));
            }
            require(legacy?changed==0:changed>0,"Nonvacuous new / absent legacy correction");require(traceStates>0,"Inherited trace exclusions exercise actual content, without a count target");
            if(legacy||reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Reopened original edit");else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            var settings=RealisticGenerator.CODEC.codec().encodeStart(ops,actual).getOrThrow().getAsJsonObject();require(settings.get("shape_version").getAsInt()==(legacy?261:262),"Saved schema is never promoted");
            server.saveEverything(false,true,true);var report=new LinkedHashMap<String,Object>();report.put("independentPublished261",snapshot);report.put("legacy",legacy);report.put("reopen",reopen);report.put("settings",settings);report.put("chunks",chunks.size());report.put("columns",columns);report.put("changedColumns",changed);report.put("nativeStateReads",states);report.put("baseColumnReads",apiStates);report.put("baseHeightChecks",apiHeights);report.put("traceStates",traceStates);report.put("savedEdit",List.of(editX,editY,editZ));report.put("windows",examples);report.put("stateBounds","min(oldGround,newGround)-3 through max(oldGround,newGround)+5, stride1");
            Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(result,"PASS independent published261 outputs; actual saved definition/edit; native physical surfaces; inherited trace exclusions; profiles and base APIs\n");
        }catch(Throwable e){Blockbranch.LOGGER.error("DEEP_BASIN_NATIVE_FAIL",e);try{Files.writeString(result,"FAIL "+e+"\n");}catch(Exception ignored){}}finally{server.halt(false);}
    });}
    private static void reject(com.mojang.serialization.DynamicOps<JsonElement> ops,JsonObject bad){require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Malformed/incomplete gate rejects");}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private DeepBasinQualification(){}
}
