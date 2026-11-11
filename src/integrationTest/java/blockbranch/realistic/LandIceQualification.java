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
public final class LandIceQualification {
    private static final int[][] SITES={{1039616,1296768},{592640,1392256}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.landIceResult"));
        try{
            if(Boolean.getBoolean("blockbranch.landIceProbe")){
                var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();int x=1039627,z=1296739;
                var d=g.drainage(r);var profile=((SparseColumn)level.getChunk(x>>4,z>>4)).blockbranch$terrain().base();var tag=profile.encode(level.palettedContainerFactory());
                System.out.println("ICE_PROBE height="+d.sample(x,z)+" depth="+d.landIceThickness(x,z)+" original="+d.iceReference().sample(x,z)+" savedGround="+profile.groundHeight(x,z)+" profileVersion="+tag.getIntOr("version",-1)+" role="+tag.getByteArray("cold_steppe").orElseThrow()[(z&15)*16+(x&15)]);
                for(int y=6250;y<=6310;y++)System.out.println("ICE_PROBE y="+y+" native="+level.getBlockState(new BlockPos(x,y,z))+" analytic="+g.surfaceState(x,y,z,r)+" profile="+profile.state(x,y,z));
                Files.writeString(result,"PASS bounded diagnostic recorded; not qualification\n");return;
            }
            var level=server.overworld();var random=level.getChunkSource().randomState();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());JsonObject fresh;
            try(var in=new InputStreamReader(Objects.requireNonNull(LandIceQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),StandardCharsets.UTF_8)){fresh=JsonParser.parseReader(in).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();}
            if(fresh.has("leaf_litter_definition")){fresh.remove("leaf_litter_definition");fresh.addProperty("shape_version",264);}
            if(fresh.get("shape_version").getAsInt()==264){
                require(fresh.get("summer_dry_definition").getAsInt()==1,"Known complete summer-dry wrapper");
                fresh.remove("summer_dry_definition");fresh.addProperty("shape_version",263);fresh.getAsJsonObject("additional_biomes").remove(SummerDryWoodland.KEY);
            }
            fresh.remove("type");require(fresh.get("shape_version").getAsInt()==263,"Complete current preset");
            var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Exact263 roundtrip");
            var old=fresh.deepCopy();old.remove("land_ice_definition");old.addProperty("shape_version",262);old.getAsJsonObject("additional_biomes").remove(LandIceField.KEY);var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(retained.landIceDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent gate retains262 exactly");
            for(int version:new int[]{82,257,258,259,260,261,262,264}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);reject(ops,bad);}
            for(String key:new String[]{"land_ice_definition","deep_basin_relief_definition","abyssal_trace_definition","cool_desert_definition","dry_forest_definition","surface_ownership_definition"}){var bad=fresh.deepCopy();bad.remove(key);reject(ops,bad);}
            for(int value:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("land_ice_definition",value);reject(ops,bad);}
            var malformed=fresh.deepCopy();malformed.addProperty("land_ice_definition","invalid");reject(ops,malformed);
            @SuppressWarnings("unchecked") var reader=(MapCodec<Object>)Class.forName("blockbranch.realistic.Published262RealisticGenerator").getField("CODEC").get(null);
            require(reader.codec().parse(ops,fresh).error().isPresent()&&reader.codec().parse(ops,old).result().isPresent(),"Original262 reader rejects263 and accepts262");
            var published=reader.codec().parse(ops,old).getOrThrow();var publishedState=published.getClass().getDeclaredMethod("surfaceState",int.class,int.class,int.class,net.minecraft.world.level.levelgen.RandomState.class);publishedState.setAccessible(true);
            boolean legacy=Boolean.getBoolean("blockbranch.landIceLegacy"),reopen=Boolean.getBoolean("blockbranch.landIceReopen");JsonObject baseline;
            try(var in=new InputStreamReader(Objects.requireNonNull(LandIceQualification.class.getResourceAsStream("/blockbranch-fixtures/land-ice-reader-262/baseline.json")),StandardCharsets.UTF_8)){baseline=JsonParser.parseReader(in).getAsJsonObject();}
            if(!legacy){baseline.remove("nativeStates");baseline.remove("nativeSHA256");baseline.remove("nativeProfileSHA256");}
            var snapshot=Published262Snapshot.capture(level,retained,legacy);require(snapshot.equals(baseline),"Whole-original independent262 output oracle: "+snapshot);
            var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.landIceDefinition()==(legacy?0:1),"Actual saved definition retained");
            var d=actual.drainage(random);var prior=retained.drainage(random);var climate=new ClimateField(random.seed());
            int editX=Published262Snapshot.POINTS[0][0]+1,editZ=Published262Snapshot.POINTS[0][1]+1,editY=d.sample(editX,editZ).height()-2;
            var edit=new BlockPos(editX,editY,editZ);
            int clearX=SITES[1][0]+3,clearZ=SITES[1][1]+3,clearY=d.sample(clearX,clearZ).height()-4;var cleared=new BlockPos(clearX,clearY,clearZ);int columns=0,states=0,changed=0,apiStates=0,apiHeights=0,traceStates=0;var chunks=new HashSet<Long>();var examples=new ArrayList<Object>();
            var centers=new ArrayList<int[]>();centers.addAll(Arrays.asList(SITES));
            // A deterministic mixed edge, searched only in the first existing
            // candidate's +/-160m window. No random/global reachability search.
            int[] edge=null;outer:for(int z=SITES[0][1]-160;z<=SITES[0][1]+160;z++)for(int x=SITES[0][0]-160;x<=SITES[0][0]+160;x++)if(candidate.drainage(random).landIceThickness(x,z)>0&&candidate.drainage(random).landIceThickness(x+1,z)==0){edge=new int[]{x,z};break outer;}
            require(edge!=null,"Nonvacuous deterministic ice/rock boundary");centers.add(edge);
            int iceColumns=0,rockColumns=0,profile76=0,legacyProfiles=0,packed=0,snow=0,maximumDepth=0;
            var originalProfile=Class.forName("blockbranch.cubic.Published262TerrainProfile");
            var originalDecode=originalProfile.getMethod("decode",net.minecraft.nbt.CompoundTag.class,net.minecraft.world.level.chunk.PalettedContainerFactory.class);
            for(int i=0;i<centers.size();i++){
                int siteChanges=0;
                for(int cz=(centers.get(i)[1]>>4)-2;cz<=(centers.get(i)[1]>>4)+2;cz++)for(int cx=(centers.get(i)[0]>>4)-2;cx<=(centers.get(i)[0]>>4)+2;cx++){
                    if(!chunks.add((cx&0xffffffffL)|((long)cz<<32)))continue;var chunk=level.getChunk(cx,cz);var profile=((SparseColumn)chunk).blockbranch$terrain().base();var tag=profile.encode(level.palettedContainerFactory());var restored=TerrainProfile.decode(tag,level.palettedContainerFactory());
                    require(restored.encode(level.palettedContainerFactory()).equals(tag),"Profile codec roundtrip");
                    boolean hasIce=tag.getIntOr("version",-1)==76;
                    if(hasIce){profile76++;boolean rejected=false;try{originalDecode.invoke(null,tag,level.palettedContainerFactory());}catch(java.lang.reflect.InvocationTargetException e){rejected=e.getCause() instanceof IllegalArgumentException;}require(rejected,"Actual published262 reader rejects profile76");
                        var unwrapped=tag.copy();unwrapped.putInt("version",unwrapped.getIntOr("land_ice_base_profile_version",-1));unwrapped.remove("land_ice_base_profile_version");rejectProfile(unwrapped,level);
                        for(int version:new int[]{-1,76,77}){var invalid=tag.copy();invalid.putInt("land_ice_base_profile_version",version);rejectProfile(invalid,level);}
                        var empty=tag.copy();var roles=empty.getByteArray("cold_steppe").orElseThrow();for(int k=0;k<roles.length;k++)if(LandIceField.encoded(roles[k]))roles[k]=0;empty.putByteArray("cold_steppe",roles);rejectProfile(empty,level);
                    }else{legacyProfiles++;require(!tag.contains("land_ice_base_profile_version"),"No wrapper on unchanged profiles");}
                    for(int lz=0;lz<16;lz++)for(int lx=0;lx<16;lx++){
                        int x=cx*16+lx,z=cz*16+lz;var before=prior.sample(x,z);var after=d.sample(x,z);int ground=after.height();columns++;int depth=d.landIceThickness(x,z);if(depth>0)iceColumns++;else rockColumns++;maximumDepth=Math.max(maximumDepth,depth);
                        require(before.waterLevel()==after.waterLevel()&&before.basin()==after.basin()&&before.closed()==after.closed()&&before.regionalHeight()==after.regionalHeight()&&before.regionalWaterLevel()==after.regionalWaterLevel()&&climate.sample(x,z,prior::sample).equals(climate.sample(x,z,d::sample)),"Native physical-only change retains water/climate references");
                        if(before.height()!=ground){changed++;siteChanges++;require(!legacy&&depth>0&&ground-before.height()==depth,"Physical ice body equals saved-role thickness");}
                        int low=Math.min(ground,before.height())-3,high=Math.max(ground,before.height())+5;
                        // surfaceState is a bare-surface helper without ores. The
                        // buried original bed now has real overburden; compare the
                        // full base-column API, including ores/caves/decorations.
                        var direct=actual.getBaseColumn(x,z,LevelHeightAccessor.create(low,high-low+1),random);
                        for(int y=low;y<=high;y++){
                            var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
                            if((legacy||reopen)&&pos.equals(edit)){require(state.is(Blocks.GOLD_BLOCK),"Actual saved edit survives");continue;}
                            if(!legacy&&reopen&&pos.equals(cleared)){require(state.isAir()&&state.getFluidState().isEmpty(),"Player-cleared ice remains air, never regenerated/source water");continue;}
                            require(state.equals(direct.getBlock(y))&&state.equals(restored.state(lx,y,lz)),"Native/analytic/decoded profile at "+pos+" native="+state+" analytic="+actual.surfaceState(x,y,z,random)+" decoded="+restored.state(lx,y,lz)+" depth="+depth+" old="+before.height()+" new="+ground);states++;
                            if(depth>0&&y>ground-depth&&y<=ground){require(state.equals(actual.surfaceState(x,y,z,random)),"Direct solid-ice sampler");require(state.is(y==ground?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE),"Literal solid snow/ice body, no approach/ore intrusion");if(state.is(Blocks.PACKED_ICE))packed++;else snow++;}
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
                examples.add(Map.of("center",centers.get(i),"changedColumns",siteChanges,"chunkRadius",2));
            }
            require(legacy?changed==0:changed>0,"Nonvacuous new / absent legacy correction");require(legacy?profile76==0:profile76>0&&iceColumns>0&&packed>0&&snow>0&&maximumDepth>=8,"Nonvacuous solid body / no legacy ice roles");require(rockColumns>0&&legacyProfiles>0,"Native mixed ice/rock edges and unchanged profiles");
            if(legacy||reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Reopened original edit");else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            if(!legacy){
                require(d.landIceThickness(clearX,clearZ)>8,"Clearance witness is inside a thick body");
                if(reopen)require(level.getBlockState(cleared).isAir()&&level.getFluidState(cleared).isEmpty(),"Reopened cleared ice/empty fluid");
                else{
                    var packedState=level.getBlockState(cleared);require(packedState.is(Blocks.PACKED_ICE)&&!packedState.isRandomlyTicking(),"Packed perennial material has no random thermal melt tick");
                    var kept=cleared.east();var support=kept.below();var beforeSupport=level.getBlockState(support);var keptState=level.getBlockState(kept);require(keptState.is(Blocks.PACKED_ICE),"Neighbour witness in solid body");
                    level.destroyBlock(support,false);require(level.getBlockState(kept).equals(keptState),"Permanent packed ice does not fall when support is removed");level.setBlock(support,beforeSupport,3);
                    level.destroyBlock(cleared,false);require(level.getBlockState(cleared).isAir()&&level.getFluidState(cleared).isEmpty(),"Destroyed packed ice does not create source water");
                    require(level.getBlockState(kept).equals(keptState),"Lateral neighbour update retains packed ice");
                    var top=new BlockPos(clearX,d.sample(clearX,clearZ).height(),clearZ);require(level.getBlockState(top).is(Blocks.SNOW_BLOCK)&&!level.getBlockState(top).isRandomlyTicking(),"Solid perennial snow cap is not a melting seasonal snow layer");
                }
            }
            var biomeRecords=new ArrayList<Object>();var surface=((ScreeBiomeSource)actual.getBiomeSource()).surfaceResolver();
            for(var p:List.of(SITES[0],SITES[1],edge,new int[]{edge[0]+1,edge[1]})){
                int x=p[0],z=p[1],y=d.sample(x,z).height()+1;var pos=new BlockPos(x,y,z);var zoom=new PreviewBiomeQueries.Selector(level).select(pos);
                var raw=surface.getNoiseBiome(x>>2,y>>2,z>>2);var generated=level.getNoiseBiome(x>>2,y>>2,z>>2);var visible=level.getBiome(pos);var zoomed=surface.getNoiseBiome(zoom.x(),zoom.y(),zoom.z());
                require(raw.equals(generated),"Generated quart matches current selected ice/rock owner");require(visible.equals(zoomed),"Actual visible biome matches native zoomed source");
                if(!legacy&&(p==SITES[0]||p==SITES[1]))require(id(raw).equals("blockbranch:perennial_alpine_icefield")&&id(visible).equals(id(raw)),"Nonvacuous native core identity");
                biomeRecords.add(Map.of("position",List.of(x,y,z),"physicalIceDepth",d.landIceThickness(x,z),"surfaceSelector",id(raw),"generatedQuart",id(generated),"zoomSurface",id(zoomed),"visible",id(visible),"quartOverride",!raw.equals(generated)));
            }
            var settings=RealisticGenerator.CODEC.codec().encodeStart(ops,actual).getOrThrow().getAsJsonObject();require(settings.get("shape_version").getAsInt()==(legacy?262:263),"Saved schema is never promoted");
            server.saveEverything(false,true,true);var report=new LinkedHashMap<String,Object>();report.put("independentPublished262",snapshot);report.put("legacy",legacy);report.put("reopen",reopen);report.put("settings",settings);report.put("chunks",chunks.size());report.put("columns",columns);report.put("changedColumns",changed);report.put("nativeStateReads",states);report.put("baseColumnReads",apiStates);report.put("baseHeightChecks",apiHeights);report.put("profile76",profile76);report.put("legacyProfiles",legacyProfiles);report.put("iceColumns",iceColumns);report.put("rockColumns",rockColumns);report.put("packedIceReads",packed);report.put("snowTopReads",snow);report.put("maximumDepth",maximumDepth);report.put("mixedEdge",edge);report.put("savedEdit",List.of(editX,editY,editZ));report.put("clearedIce",legacy?"not applied to legacy world":List.of(clearX,clearY,clearZ));report.put("windows",examples);report.put("nativeBiomes",biomeRecords);report.put("stateBounds","min(oldGround,newGround)-3 through max(oldGround,newGround)+5, stride1");
            Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(result,"PASS independent published262 outputs; saved ice definition/edits; native solid bodies/mixed edges/biomes; material lifecycle; profile and base APIs\n");
        }catch(Throwable e){Blockbranch.LOGGER.error("DEEP_BASIN_NATIVE_FAIL",e);try{Files.writeString(result,"FAIL "+e+"\n");}catch(Exception ignored){}}finally{server.halt(false);}
    });}
    private static String id(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> b){return b.unwrapKey().orElseThrow().identifier().toString();}
    private static void rejectProfile(net.minecraft.nbt.CompoundTag tag,net.minecraft.server.level.ServerLevel level){boolean rejected=false;try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException e){rejected=true;}require(rejected,"Malformed/unwrapped/empty ice profile rejects");}
    private static void reject(com.mojang.serialization.DynamicOps<JsonElement> ops,JsonObject bad){require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Malformed/incomplete gate rejects");}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private LandIceQualification(){}
}
