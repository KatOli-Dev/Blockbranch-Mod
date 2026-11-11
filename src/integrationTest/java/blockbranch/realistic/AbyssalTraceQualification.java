package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.Heightmap;
import blockbranch.cubic.*;

/** Native contents, append-only readers and the independent complete public260 oracle. */
public final class AbyssalTraceQualification {
    private static final int[][] POINTS={{-1744000,-1904000},{-528000,-1904000},{304000,-1904000}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.abyssalTraceResult"));
        try{
            var level=server.overworld();var random=level.getChunkSource().randomState();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());JsonObject fresh;
            if(Boolean.getBoolean("blockbranch.abyssalTraceBlockOnly")){
                var actual=(RealisticGenerator)level.getChunkSource().getGenerator();var report=new LinkedHashMap<String,Object>(blockBehavior(level,actual,Boolean.getBoolean("blockbranch.abyssalTraceReopen")));
                var marker=Files.readString(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("blockbranch-columns.txt"));
                require(marker.contains("blockbranch-columns=4\nblock-position-bits=22,20,22\nmetallurgy-catalog=21\n"),"Conservative content barrier retains coordinate packing");
                var scratch=Files.createTempDirectory(result.getParent(),"original-marker-reader-");Files.writeString(scratch.resolve("blockbranch-columns.txt"),marker);
                boolean rejects=false;try{Class.forName("blockbranch.cubic.Published260ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class).invoke(null,scratch,true,true,true);}catch(java.lang.reflect.InvocationTargetException e){rejects=e.getCause() instanceof IllegalStateException&&e.getCause().getMessage().contains("Unsupported Blockbranch world format");}
                require(rejects&&Files.readString(scratch.resolve("blockbranch-columns.txt")).equals(marker),"Actual catalog20 reader rejects21 without rewriting it");report.put("contentCatalog",21);report.put("originalCatalog20Rejects",true);
                server.saveEverything(false,true,true);Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                Files.writeString(result,"PASS explicit trace edit/source water; neighbor/support changes; saved properties; unchanged old generator/profile gate\n");return;
            }
            try(var in=new InputStreamReader(Objects.requireNonNull(AbyssalTraceQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),StandardCharsets.UTF_8)){fresh=JsonParser.parseReader(in).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();}
            if(fresh.get("shape_version").getAsInt()==262){
                require(fresh.get("deep_basin_relief_definition").getAsInt()==1,"Known complete deep-basin wrapper");
                fresh.remove("deep_basin_relief_definition");fresh.addProperty("shape_version",261);
            }
            fresh.remove("type");require(fresh.get("shape_version").getAsInt()==261,"Current complete preset");
            var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Exact candidate roundtrip");
            var old=fresh.deepCopy();old.remove("abyssal_trace_definition");old.addProperty("shape_version",260);var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(retained.abyssalTraceDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent gate retains exact published260 definition");
            for(int version:new int[]{82,257,258,259,260,262}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);rejected(ops,bad);}
            for(int value:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("abyssal_trace_definition",value);rejected(ops,bad);}
            for(String key:new String[]{"abyssal_trace_definition","cool_desert_definition","dry_forest_definition","surface_ownership_definition"}){var bad=fresh.deepCopy();bad.remove(key);rejected(ops,bad);}
            var bad=fresh.deepCopy();bad.addProperty("abyssal_trace_definition","invalid");rejected(ops,bad);
            @SuppressWarnings("unchecked") var reader=(MapCodec<Object>)Class.forName("blockbranch.realistic.Published260RealisticGenerator").getField("CODEC").get(null);
            require(reader.codec().parse(ops,fresh).error().isPresent()&&reader.codec().parse(ops,old).result().isPresent(),"Actual original260 reader rejects261 and accepts260");
            var published=reader.codec().parse(ops,old).getOrThrow();var publishedState=published.getClass().getDeclaredMethod("surfaceState",int.class,int.class,int.class,net.minecraft.world.level.levelgen.RandomState.class);publishedState.setAccessible(true);
            boolean legacy=Boolean.getBoolean("blockbranch.abyssalTraceLegacy"),reopen=Boolean.getBoolean("blockbranch.abyssalTraceReopen");JsonObject baseline;
            try(var in=new InputStreamReader(Objects.requireNonNull(AbyssalTraceQualification.class.getResourceAsStream("/blockbranch-fixtures/abyssal-reader-260/baseline.json")),StandardCharsets.UTF_8)){baseline=JsonParser.parseReader(in).getAsJsonObject();}
            if(!legacy){baseline.remove("nativeStates");baseline.remove("nativeSHA256");baseline.remove("nativeProfileSHA256");}
            var snapshot=Published260Snapshot.capture(level,retained,legacy);require(snapshot.equals(baseline),"Gate-disabled output equals independently recorded complete public260 production: "+snapshot);
            var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.abyssalTraceDefinition()==(legacy?0:1),"Actual saved definition retained");var d=actual.drainage(random);var c=new ClimateField(random.seed());
            int editX=-1744014,editZ=-1904012,editY=d.sample(editX,editZ).height()+1;
            var report=new LinkedHashMap<String,Object>();report.put("independentPublished260",snapshot);int columns=0,states=0,traces=0,profile75=0,legacyProfiles=0,apiStates=0,apiHeights=0;var roots=new ArrayList<Object>();var chunks=new HashSet<Long>();
            var originalProfile=Class.forName("blockbranch.cubic.Published260TerrainProfile");var originalDecode=originalProfile.getMethod("decode",CompoundTag.class,net.minecraft.world.level.chunk.PalettedContainerFactory.class);
            for(var p:POINTS)for(int cz=(p[1]>>4)-4;cz<=(p[1]>>4)+4;cz++)for(int cx=(p[0]>>4)-4;cx<=(p[0]>>4)+4;cx++){
                if(!chunks.add((cx&0xffffffffL)|((long)cz<<32)))continue;var chunk=level.getChunk(cx,cz);var profile=((SparseColumn)chunk).blockbranch$terrain().base();var tag=profile.encode(level.palettedContainerFactory());
                var restored=TerrainProfile.decode(tag,level.palettedContainerFactory());boolean hasTrace=Arrays.stream(profile.canopy().encode()).anyMatch(v->(v&255)>=AbyssalTraces.FIRST);
                require((tag.getIntOr("version",-1)==75)==hasTrace,"Only appended-state profiles receive version75");
                if(hasTrace){profile75++;boolean rejects=false;try{originalDecode.invoke(null,tag,level.palettedContainerFactory());}catch(java.lang.reflect.InvocationTargetException e){rejects=e.getCause() instanceof IllegalArgumentException;}require(rejects,"Actual original260 profile reader rejects75");
                    var empty=tag.copy();empty.putIntArray("canopy",Arrays.stream(profile.canopy().encode()).filter(v->(v&255)<AbyssalTraces.FIRST).toArray());boolean rejectsEmpty=false;try{TerrainProfile.decode(empty,level.palettedContainerFactory());}catch(IllegalArgumentException e){rejectsEmpty=true;}require(rejectsEmpty,"Wrapper without appended content rejects");
                    var stripped=tag.copy();stripped.putInt("version",stripped.getIntOr("benthic_base_profile_version",-1));stripped.remove("benthic_base_profile_version");boolean rejectsStripped=false;try{TerrainProfile.decode(stripped,level.palettedContainerFactory());}catch(IllegalArgumentException e){rejectsStripped=true;}require(rejectsStripped,"Appended states cannot bypass wrapper");
                    for(int version:new int[]{-1,75,76}){var wrong=tag.copy();wrong.putInt("benthic_base_profile_version",version);boolean reject=false;try{TerrainProfile.decode(wrong,level.palettedContainerFactory());}catch(IllegalArgumentException e){reject=true;}require(reject,"Malformed/nested/future profile foundation");}
                }else{
                    legacyProfiles++;require(!tag.contains("benthic_base_profile_version"),"Unchanged profile descriptor absent");
                    // The renamed reader cannot pass itself to today's cave-layer
                    // validators, whose JVM signatures name production TerrainProfile.
                    // Exercise its unchanged core reader separately; authentic whole
                    // original-output hashes cover complete native cave profiles.
                    var core=tag.copy();core.putInt("version",60);
                    for(String key:new String[]{"cave_biomes","lit_caves","dripstone","lit_cave_definition","dripstone_definition","ore_definition","ores","underground","base_profile_version","reed_base_profile_version"})core.remove(key);
                    var originalCore=originalDecode.invoke(null,core,level.palettedContainerFactory());
                    var encodedCore=originalProfile.getMethod("encode",net.minecraft.world.level.chunk.PalettedContainerFactory.class).invoke(originalCore,level.palettedContainerFactory());
                    require(encodedCore.equals(TerrainProfile.decode(core,level.palettedContainerFactory()).encode(level.palettedContainerFactory())),"Original and current core profile encoding");
                }
                for(int lz=0;lz<16;lz++)for(int lx=0;lx<16;lx++){
                    int x=cx*16+lx,z=cz*16+lz,ground=d.sample(x,z).height();columns++;boolean root=false;
                    for(int y=ground-3;y<=ground+4;y++){
                        var state=level.getBlockState(new BlockPos(x,y,z));
                        if(!legacy&&reopen&&x==editX&&z==editZ&&y==editY){require(state.is(Blocks.WATER)&&state.getFluidState().isSource(),"Saved removal of natural trace retains source water");continue;}
                        require(state.equals(actual.surfaceState(x,y,z,random))&&state.equals(restored.state(lx,y,lz)),"Native/analytic/NBT state at "+x+","+y+","+z);states++;
                        var before=retained.surfaceState(x,y,z,random);
                        if(legacy)require(state.equals(publishedState.invoke(published,x,y,z,random)),"Old-mode new surrounding columns agree with original generator reader");
                        if(!state.equals(before)){require(!legacy&&y==ground+1&&state.is(BenthicTraceContent.TRACE)&&before.is(Blocks.WATER),"Only source water over supported bed changes");root=true;traces++;
                            require(AbyssalTraces.eligible(random.seed(),x,z,d,c,true),"Actual eligibility");var below=level.getBlockState(new BlockPos(x,ground,z));require(below.is(Blocks.MUD)||below.is(Blocks.CLAY),"Literal fine-bed support");require(state.getFluidState().is(Fluids.WATER)&&state.getFluidState().isSource()&&state.getCollisionShape(level,new BlockPos(x,y,z)).isEmpty(),"Source water and collision preserved");
                            roots.add(Map.of("x",x,"y",y,"z",z,"form",state.getValue(BenthicTraceBlock.FORM),"facing",state.getValue(BenthicTraceBlock.FACING).toString(),"bed",below.toString()));
                        }
                    }
                    if(root||((lx&7)==0&&(lz&7)==0)){
                        for(var bounds:List.of(net.minecraft.world.level.LevelHeightAccessor.create(ground-3,8),net.minecraft.world.level.LevelHeightAccessor.create(60,8))){var col=actual.getBaseColumn(x,z,bounds,random);for(int y=bounds.getMinY();y<=bounds.getMaxY();y++){require(col.getBlock(y).equals(level.getBlockState(new BlockPos(x,y,z))),"Direct base column");apiStates++;}
                            for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(level.getBlockState(new BlockPos(x,y,z)))){expected=y+1;break;}require(actual.getBaseHeight(x,z,type,bounds,random)==expected,"Clipped base-height native predicate");apiHeights++;}}
                        for(var type:Heightmap.Types.values()){require(actual.getBaseHeight(x,z,type,level,random)==chunk.getHeight(type,lx,lz)+1,"Full native heightmap");apiHeights++;}
                    }
                }
            }
            require(legacy?traces==0:traces>0&&profile75>0,"Nonvacuous current / absent legacy traces");require(legacyProfiles>0,"Non-trace profile preservation exercised");
            var p=Published260Snapshot.POINTS[0];int ex=p[0]+1,ez=p[1]+1;var edit=new BlockPos(ex,d.sample(ex,ez).height()-2,ez);if(legacy||reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Actual saved edit retained");else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            if(!legacy&&!reopen){var pos=new BlockPos(editX,editY,editZ);require(level.getBlockState(pos).is(BenthicTraceContent.TRACE),"Edit witness is a natural trace");level.setBlock(pos,Blocks.WATER.defaultBlockState(),3);}
            report.put("traceRemovalEdit",legacy?"not applied to original world":List.of(editX,editY,editZ));
            report.put("legacy",legacy);report.put("nativeChunks",chunks.size());report.put("nativeColumns",columns);report.put("nativeStateReads",states);report.put("traceBlocks",traces);report.put("profile75Chunks",profile75);report.put("legacyProfileChunks",legacyProfiles);report.put("baseColumnReads",apiStates);report.put("baseHeightChecks",apiHeights);report.put("roots",roots);report.put("centers",POINTS);report.put("inclusiveChunkRadius",4);report.put("stateYBounds","ground-3 through ground+4");
            server.saveEverything(false,true,true);Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(result,"PASS independent published260 outputs; actual saved gate/edit; original readers; native supported waterlogged traces; profile and base APIs\n");Blockbranch.LOGGER.info("ABYSSAL_TRACE_NATIVE_PASS {}",report);
        }catch(Throwable e){Blockbranch.LOGGER.error("ABYSSAL_TRACE_NATIVE_FAIL",e);try{Files.writeString(result,"FAIL "+e+"\n");}catch(Exception ignored){}}finally{server.halt(false);}
    });}
    private static void rejected(RegistryOps<JsonElement> ops,JsonObject j){require(RealisticGenerator.CODEC.codec().parse(ops,j).error().isPresent(),"Unsupported settings rejected");}
    private static Map<String,Object> blockBehavior(net.minecraft.server.level.ServerLevel level,RealisticGenerator g,boolean reopen){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);int x=-527972,z=-1904037,y=d.sample(x,z).height()+1;var pos=new BlockPos(x,y,z);var chunk=level.getChunkAt(pos);
        require(g.abyssalTraceDefinition()==0,"Block-only fixture uses authentic old generation");
        var settings=RealisticGenerator.CODEC.codec().encodeStart(RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().getAsJsonObject();
        require(settings.get("shape_version").getAsInt()==260&&!settings.has("abyssal_trace_definition"),"Manual content never promotes old generation");
        var profile=((SparseColumn)chunk).blockbranch$terrain().base();var before=profile.encode(level.palettedContainerFactory());require(before.getIntOr("version",-1)!=75,"Old base profile retains old format");
        var expected=BenthicTraceContent.TRACE.defaultBlockState().setValue(BenthicTraceBlock.FORM,2).setValue(BenthicTraceBlock.FACING,Direction.WEST);var bed=level.getBlockState(pos.below());
        require(bed.is(Blocks.MUD)||bed.is(Blocks.CLAY),"Native fine support at edited column");
        if(reopen)require(level.getBlockState(pos).equals(expected),"Exact explicit form/facing/waterlogged edit survives full relaunch");
        else{require(level.getBlockState(pos).is(Blocks.WATER),"Old gate does not naturally generate trace");level.setBlock(pos,expected,3);}
        require(level.getBlockState(pos).getFluidState().is(Fluids.WATER)&&level.getBlockState(pos).getFluidState().isSource(),"Persisted trace contains source water");
        var side=pos.east();var priorSide=level.getBlockState(side);level.setBlock(side,Blocks.STONE.defaultBlockState(),3);require(level.getBlockState(pos).equals(expected)&&level.getFluidState(pos).isSource(),"Lateral neighbor update retains properties and water");level.setBlock(side,priorSide,3);
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);require(level.getBlockState(pos).is(Blocks.WATER)&&level.getFluidState(pos).isSource(),"Losing fine support replaces trace with source water");level.setBlock(pos.below(),bed,3);level.setBlock(pos,expected,3);
        require(before.equals(profile.encode(level.palettedContainerFactory())),"Explicit edits stay in section overrides, not appended procedural states");
        return Map.of("seed",random.seed(),"position",List.of(x,y,z),"shapeVersion",260,"abyssalTraceDefinition",0,"baseProfileVersion",before.getIntOr("version",-1),"reopened",reopen,"savedBlock",expected.toString(),"checks",List.of("source water","lateral neighbor","support removal","restore exact bed","saved form/facing","unchanged generator/profile"));
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private AbyssalTraceQualification(){}
}
