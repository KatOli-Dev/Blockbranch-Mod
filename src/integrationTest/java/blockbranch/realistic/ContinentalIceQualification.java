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
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded physical/API/lifecycle witnesses and authentic complete-original replay.
 * The frozen original-output oracle is independent; renamed readers share helpers.
 * No sample percentage is an Earth coverage claim or full ice ecology. */
public final class ContinentalIceQualification {
    private static final int[][] POINTS={{272000,-720000},{-1552000,-1904000},{-1456000,-1904000},{-1615115,-1904000},{-1619720,-1904000},{1039616,1296768}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.polarResult"));
        try{
            var level=server.overworld();var random=level.getChunkSource().randomState();var actual=(RealisticGenerator)level.getChunkSource().getGenerator();
            boolean legacy=Boolean.getBoolean("blockbranch.polarLegacy"),reopen=Boolean.getBoolean("blockbranch.polarReopen");
            require(actual.continentalIceDefinition()==(legacy?0:1),"Actual saved generation definition");
            if(!legacy){var savedShape=RealisticGenerator.CODEC.codec().encodeStart(RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),actual).getOrThrow().getAsJsonObject().get("shape_version").getAsInt();require(savedShape==(actual.seasonalCrownDefinition()==1?275:actual.broadleafCrownDefinition()==1?274:actual.mangroveRootsDefinition()==1?273:actual.borealSlopeDefinition()==1?272:actual.montaneForestDefinition()==1?271:actual.alluvialDefinition()==1?270:actual.seasonalForestDefinition()==1?269:actual.warmXericDefinition()==1?268:actual.leafLitterDefinition()==1?267:265),"The opened world's saved schema names exactly its definitions (published polar-only 265 carries no litter)");}
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());JsonObject fresh;
            try(var input=new InputStreamReader(Objects.requireNonNull(ContinentalIceQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),StandardCharsets.UTF_8)){fresh=JsonParser.parseReader(input).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();}
            WarmXericFixtures.published267(fresh);
            fresh.remove("type");require(fresh.get("shape_version").getAsInt()==267&&fresh.get("leaf_litter_definition").getAsInt()==1,"Explicit new-world default (continental ice with leaf-litter variation)");
            var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Exact preset roundtrip");
            var old=fresh.deepCopy();old.remove("continental_ice_definition");old.remove("leaf_litter_definition");old.addProperty("shape_version",264);old.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);
            var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();require(retained.continentalIceDefinition()==0&&!retained.climate(random).polar(),"Absent definition selects legacy helpers");
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Old settings roundtrip unchanged");
            @SuppressWarnings("unchecked") var reader=(MapCodec<Object>)Class.forName("blockbranch.realistic.Published264RealisticGenerator").getField("CODEC").get(null);
            require(reader.codec().parse(ops,old).result().isPresent()&&reader.codec().parse(ops,fresh).error().isPresent(),"Actual original264 codec accepts264 and rejects the combined 267");
            {var iceOnly=fresh.deepCopy();iceOnly.remove("leaf_litter_definition");iceOnly.addProperty("shape_version",265);var iceRead=RealisticGenerator.CODEC.codec().parse(ops,iceOnly).getOrThrow();require(iceRead.continentalIceDefinition()==1&&iceRead.leafLitterDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,iceRead).getOrThrow().equals(iceOnly),"Polar-only schema 265 keeps its meaning without leaf litter");
             var litterOnly=fresh.deepCopy();litterOnly.remove("continental_ice_definition");litterOnly.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);litterOnly.addProperty("shape_version",266);var litterRead=RealisticGenerator.CODEC.codec().parse(ops,litterOnly).getOrThrow();require(litterRead.continentalIceDefinition()==0&&litterRead.leafLitterDefinition()==1&&!litterRead.climate(random).polar(),"Litter-only schema 266 keeps its meaning without polar geography");
             require(reader.codec().parse(ops,iceOnly).error().isPresent()&&reader.codec().parse(ops,litterOnly).error().isPresent(),"The published 264 codec rejects 265 and 266");}
            for(int v:new int[]{82,257,258,259,260,261,262,263,264,265,266,268}){var bad=fresh.deepCopy();bad.addProperty("shape_version",v);rejected(ops,bad);}
            for(int def:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("continental_ice_definition",def);rejected(ops,bad);bad=fresh.deepCopy();bad.addProperty("leaf_litter_definition",def);rejected(ops,bad);}
            for(String key:new String[]{"continental_ice_definition","leaf_litter_definition","summer_dry_definition","land_ice_definition","deep_basin_relief_definition","abyssal_trace_definition","cool_desert_definition","dry_forest_definition","surface_ownership_definition","redwood_forest_definition","biome_transition_definition"}){var bad=fresh.deepCopy();bad.remove(key);rejected(ops,bad);}
            var bad=fresh.deepCopy();bad.addProperty("continental_ice_definition","bad");rejected(ops,bad);bad=fresh.deepCopy();bad.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);rejected(ops,bad);
            var report=new LinkedHashMap<String,Object>();report.put("preset",fresh);
            if(Boolean.getBoolean("blockbranch.polarApiFirst")){
                var cold=new ArrayList<int[]>();for(int[] center:new int[][]{{-1552000,-1904000},{-1456000,-1904000}})for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)cold.add(new int[]{(center[0]>>4)+dx,(center[1]>>4)+dz});
                for(var p:cold){require(level.getChunkSource().getChunk(p[0],p[1],net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null,"Cold API site has no native full chunk");int x=p[0]*16+8,z=p[1]*16+8,h=actual.drainage(random).sample(x,z).height();var sampler=actual.independentDripstoneSampler(p[0],p[1],random);require(sampler.state(8,h-100,8).is(Blocks.PACKED_ICE),"Independent cold-plan sampler sees actual permanent ice, regardless of habitat province");actual.getBaseColumn(x,z,LevelHeightAccessor.create(h-350,355),random);require(level.getChunkSource().getChunk(p[0],p[1],net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null,"Base API does not generate native chunk");}
                int placements=0;for(var p:cold){var profile=((SparseColumn)level.getChunk(p[0],p[1])).blockbranch$terrain().base();var independent=actual.independentDripstonePlan(p[0],p[1],random);require(Arrays.equals(independent.placements(),profile.dripstone().placements()),"Cold API/native dripstone plans agree");placements+=profile.dripstone().placements().length;}
                report.put("apiFirstChunks",cold.size());report.put("dripstonePlacementInts",placements);if(Boolean.getBoolean("blockbranch.polarApiFirstOnly")){server.saveEverything(false,true,true);Files.writeString(result.resolveSibling("api-first.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(result,"PASS API-before-native planning at 18 existing body-window chunks\n");return;}
            }
            if(Boolean.getBoolean("blockbranch.polarInterfaceOnly")){
                profileBounds(level,((SparseColumn)level.getChunk(272000>>4,-720000>>4)).blockbranch$terrain().base());
                int x=-1589245,z=-1966053;require(actual.drainage(random).permanentFreshwaterLid(x,z),"Existing exposed-lid witness");int[] column=new int[blockbranch.horizon.HorizonTiles.FIELDS];actual.horizonColumn(x,z,random,column,0,b->0);
                require(column[0]==63&&column[1]==net.minecraft.world.level.block.Block.getId(Blocks.PACKED_ICE.defaultBlockState())&&column[7]==63,"Horizon depicts the actual one-block packed lid");require(column[5]==62&&actual.waterSurface(x,z,random)==63,"Visible underlying water and unchanged hydrological datum distinguished");
                require(level.getBlockState(new BlockPos(x,63,z)).is(Blocks.PACKED_ICE)&&level.getFluidState(new BlockPos(x,62,z)).is(net.minecraft.tags.FluidTags.WATER),"Actual native lid and source water match horizon");
                Files.writeString(result.resolveSibling("interface.json"),new Gson().toJson(column));Files.writeString(result,"PASS parent profile bounds and exact existing lid horizon/material/datum interfaces\n");return;
            }
            if(Boolean.getBoolean("blockbranch.polarSavedPlanOnly")){
                report.put("savedTreeClear",savedPlan(level,actual,result.getParent(),reopen));server.saveEverything(false,true,true);
                Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(result,"PASS bounded saved tree-clear climate/footprint replay only\n");return;
            }
            if(legacy&&!reopen){
                JsonObject baseline;try(var input=new InputStreamReader(Objects.requireNonNull(ContinentalIceQualification.class.getResourceAsStream("/blockbranch-fixtures/polar-reader-264/baseline.json")),StandardCharsets.UTF_8)){baseline=JsonParser.parseReader(input).getAsJsonObject();}
                var observed=Published264Snapshot.capture(level,actual,true);require(observed.equals(baseline),"Complete published264 independent native/profile/physical/climate/selector hashes: "+observed);report.put("independentPublished264",observed);
            }else if(legacy)require(Files.readString(result.resolveSibling("result.txt")).startsWith("PASS"),"Completed independent legacy evidence reused on reopen");
            var d=actual.drainage(random);var scenes=new ArrayList<Object>();int states=0,owned=0,uncovered=0;var profiles=new HashSet<Long>();
            BlockPos removed=new BlockPos(-1551999,d.sample(-1551999,-1903999).height(),-1903999);
            BlockPos edit=removed.below(d.continentalIceThickness(removed.getX(),removed.getZ())+1);
            if(reopen&&!legacy){require(level.getBlockState(removed).isAir()&&level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Cleared ice and saved bed edit survive relaunch");}
            for(var point:POINTS){
                int px=point[0],pz=point[1];int h=d.sample(px,pz).height();level.getChunk(px>>4,pz>>4);
                var selected=((ScreeBiomeSource)actual.getBiomeSource()).surfaceResolver().getNoiseBiome(px>>2,0,pz>>2);
                scenes.add(Map.of("x",px,"z",pz,"height",h,"depth",d.continentalIceThickness(px,pz),"selector",selected.unwrapKey().orElseThrow().identifier().toString(),"generatedQuart",level.getNoiseBiome(px>>2,(h+1)>>2,pz>>2).unwrapKey().orElseThrow().identifier().toString(),"visible",level.getBiome(new BlockPos(px,h+1,pz)).unwrapKey().orElseThrow().identifier().toString()));
                for(int z=pz-16;z<=pz+16;z++)for(int x=px-16;x<=px+16;x++){
                    var chunk=level.getChunk(x>>4,z>>4);var profile=((SparseColumn)chunk).blockbranch$terrain().base();int depth=d.continentalIceThickness(x,z),top=d.sample(x,z).height();
                    if(depth>0)owned++;else uncovered++;
                    if(legacy)require(depth==0&&!profile.perennialIce().enabled(),"Old saved/unexplored terrain has no new ice spans");
                    if(profiles.add(chunk.getPos().pack())){
                        var encoded=profile.encode(level.palettedContainerFactory());require(TerrainProfile.decode(encoded,level.palettedContainerFactory()).encode(level.palettedContainerFactory()).equals(encoded),"Native profile exact roundtrip");
                        if(profile.caveBiomes().enabled()){int expected=DeepDarkHabitat.mask(random.seed(),chunk.getPos().x(),chunk.getPos().z(),d);require(profile.caveBiomes().deepDarkMask()==expected&&(!profile.perennialIce().enabled()||expected==DeepDarkHabitat.mask(random.seed(),chunk.getPos().x(),chunk.getPos().z(),d.iceReference())),"Native deep-dark mask follows the rock-bed datum under a sheet");}
                        if(profile.perennialIce().enabled())profileBarrier(encoded,level);
                        PolarHabitatChecks.chunk(level,actual,random,d,chunk,profile);
                    }
                    int bottom=top-depth+1;
                    for(int y=top-2;y<=top+2;y++)if(!(reopen&&!legacy&&(removed.equals(new BlockPos(x,y,z))||edit.equals(new BlockPos(x,y,z))))){require(level.getBlockState(new BlockPos(x,y,z)).equals(actual.surfaceState(x,y,z,random)),"Native material contract "+new BlockPos(x,y,z));states++;}
                    if(depth>0&&Math.floorMod(x,4)==0&&Math.floorMod(z,4)==0)for(int y=bottom;y<=top;y++){
                        var pos=new BlockPos(x,y,z);if(reopen&&removed.equals(pos))continue;
                        require(level.getBlockState(pos).is(y==top?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE),"Whole sampled ice column has no buried flora/ores/caves: "+pos);states++;
                    }
                }
                for(int offset:new int[]{-8,0,8})states+=baseApis(level,actual,px+offset,pz+offset);
            }
            if(!legacy){require(owned>2000&&uncovered>1000,"Nonvacuous body and uncovered controls");report.put("waterWitnesses",waterWitnesses(level,actual,reopen,result.getParent()));profileBounds(level,((SparseColumn)level.getChunk(272000>>4,-720000>>4)).blockbranch$terrain().base());report.put("horizonInterfaces",horizonInterfaces(level,actual,retained,reader.codec().parse(ops,old).getOrThrow()));}
            if(!legacy)report.put("habitatWindows",PolarHabitatChecks.windows(level,actual,random,d,reopen,result.getParent()));
            if(!legacy)report.put("structureFit",PolarHabitatChecks.structures(level,actual,random,d));report.put("habitatCoverage",PolarHabitatChecks.finish(legacy));
            int ex=Published264Snapshot.POINTS[0][0]+1,ez=Published264Snapshot.POINTS[0][1]+1;
            var oldEdit=new BlockPos(ex,d.sample(ex,ez).height()-2,ez);
            if(legacy)require(level.getBlockState(oldEdit).is(Blocks.GOLD_BLOCK),"Authentic original-build saved gold edit");
            if(!legacy&&!reopen){require(level.getBlockState(removed).is(Blocks.SNOW_BLOCK),"Natural ice before player clearing");level.setBlock(removed,Blocks.AIR.defaultBlockState(),3);level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);level.updateNeighborsAt(removed,Blocks.AIR);require(level.getBlockState(removed.below()).is(Blocks.PACKED_ICE)&&level.getFluidState(removed).isEmpty(),"Permanent material neighbour change does not melt or emit water");}
            report.put("savedTreeClear",savedPlan(level,actual,result.getParent(),reopen));
            report.put("scenes",scenes);report.put("nativeAndApiReads",states);report.put("ownedColumns",owned);report.put("uncoveredColumns",uncovered);report.put("profiles",profiles.size());report.put("clearedIce",removed.toShortString());report.put("bedEdit",edit.toShortString());
            server.saveEverything(false,true,true);Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(result,"PASS bounded continental ice codec/native/base APIs, immutable ice material and saved edits"+(legacy?"; authentic published264 legacy":"")+"\n");
        }catch(Throwable t){Blockbranch.LOGGER.error("CONTINENTAL_ICE_QUALIFICATION_FAIL",t);try{Files.writeString(result,"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static Map<String,Object> horizonInterfaces(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Object frozen)throws Exception{
        var random=level.getChunkSource().randomState();int x=-1589245,z=-1966053;int[] lid=new int[blockbranch.horizon.HorizonTiles.FIELDS];actual.horizonColumn(x,z,random,lid,0,b->0);
        require(lid[0]==63&&lid[1]==net.minecraft.world.level.block.Block.getId(Blocks.PACKED_ICE.defaultBlockState())&&lid[7]==63&&lid[5]==62&&actual.waterSurface(x,z,random)==63,"Physical lid/underlying water horizon and separate pre-cover datum");
        var method=frozen.getClass().getMethod("horizonColumn",int.class,int.class,net.minecraft.world.level.levelgen.RandomState.class,int[].class,int.class,java.util.function.ToIntFunction.class);
        var ids=new LinkedHashMap<String,Integer>();java.util.function.ToIntFunction<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> mapper=b->ids.computeIfAbsent(b.unwrapKey().orElseThrow().identifier().toString(),key->ids.size());
        int samples=0;for(var p:new int[][]{{272000,-720000},{-1552000,-1904000},{-1589245,-1966053},{-1872000,-1904000}}){
            int[] oldTuple=new int[8],retainedTuple=new int[8];method.invoke(frozen,p[0],p[1],random,oldTuple,0,mapper);retained.horizonColumn(p[0],p[1],random,retainedTuple,0,mapper);require(Arrays.equals(oldTuple,retainedTuple),"Gate-disabled horizon equals original264 method at "+Arrays.toString(p));samples++;
        }
        return Map.of("lidCoordinate",List.of(x,z),"lidTuple",lid,"legacyOriginalMethodWitnesses",samples,"limit","Frozen original horizon method; shared helpers separately qualified by the whole-original oracle, not a fully independent original runtime here.");
    }
    private static void profileBounds(ServerLevel level,TerrainProfile example){
        int[] ground=new int[256],water=new int[256];Arrays.fill(ground,10);Arrays.fill(water,Integer.MIN_VALUE);var base=new TerrainProfile(0,32,ground,water,example.biomes(),level.palettedContainerFactory().blockStatesStrategy());
        int[] spans=new int[256];spans[0]=PerennialIceLayer.pack(-1,10,true);boolean failed=false;try{base.withPerennialIce(new PerennialIceLayer(spans));}catch(IllegalArgumentException expected){failed=true;}require(failed,"Globally valid ice below custom parent floor rejects");
        spans[0]=PerennialIceLayer.pack(1,10,true);var valid=base.withPerennialIce(new PerennialIceLayer(spans));var tag=valid.encode(level.palettedContainerFactory());var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());require(decoded.state(0,-1,0).isAir()&&decoded.state(0,0,0).is(Blocks.BEDROCK)&&decoded.state(0,1,0).is(Blocks.PACKED_ICE),"Nonvacuous custom-bounds profile77 roundtrip and boundary blocks");
    }
    private static Map<String,Object> savedPlan(ServerLevel level,RealisticGenerator g,Path dir,boolean reopen)throws Exception{
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=g.climate(random);
        var root=SummerDryTrees.roots(random.seed(),271984,-720016,272016,-719984,d,c,g.summerDryOwner(random)).stream().filter(r->r.height()>=12).findFirst().orElseThrow();
        var box=new net.minecraft.world.level.levelgen.structure.BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());
        var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
        var piece=new ReimaginedTreeClearPiece(box,random.seed(),g.borealCrownDefinition(),g.vegetationFlags());var tag=piece.createTag(context);int definition=1+g.borealCrownDefinition()+2*g.vegetationFlags();
        require(tag.getIntOr("ClearDefinition",0)==definition&&((g.vegetationFlags()&16)!=0)==(g.continentalIceDefinition()>0),"Saved tree-clear climate bit derives from the actual old/new generation gate");
        var plan=BorealClearSmoke.plan(piece,g,random);require(!plan.isEmpty()&&SummerDryTrees.tree(root).stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))),"Whole saved woody footprint is nonvacuous");
        Path file=dir.resolve("tree-clear.nbt");if(!reopen){require(!Files.exists(file),"Preserve original plan evidence");net.minecraft.nbt.NbtIo.writeCompressed(tag,file);}
        var saved=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap());require(tag.equals(saved)&&plan.equals(BorealClearSmoke.plan(BorealClearSmoke.load(context,saved),g,random)),"Exact compressed plan and footprint replay across process/gate");
        require(!Blocks.PACKED_ICE.defaultBlockState().isRandomlyTicking()&&!Blocks.SNOW_BLOCK.defaultBlockState().isRandomlyTicking(),"Existing permanent materials have no random melt tick");
        return Map.of("definition",definition,"footprintCells",plan.size(),"polarClimate",c.polar());
    }
    private static int baseApis(ServerLevel level,RealisticGenerator g,int x,int z){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);int top=d.sample(x,z).height(),bottom=top-d.continentalIceThickness(x,z);
        var profile=((SparseColumn)level.getChunk(x>>4,z>>4)).blockbranch$terrain().base();int checked=0;
        for(var bounds:new LevelHeightAccessor[]{LevelHeightAccessor.create(TerrainShape.FLOOR,TerrainShape.CEILING-TerrainShape.FLOOR),LevelHeightAccessor.create(bottom-3,Math.max(16,top-bottom+8))}){
            var column=g.getBaseColumn(x,z,bounds,random);int min=Math.max(TerrainShape.FLOOR,bounds.getMinY()),max=Math.min(TerrainShape.CEILING-1,bounds.getMaxY());
            for(int y=min;y<=max;y++){require(column.getBlock(y).equals(profile.state(x,y,z)),"Base column/profile "+x+","+y+","+z+" base="+column.getBlock(y)+" profile="+profile.state(x,y,z));checked++;}
            for(var type:Heightmap.Types.values()){
                int expected=bounds.getMinY();for(int y=max;y>=min;y--)if(type.isOpaque().test(profile.state(x,y,z))){expected=y+1;break;}
                require(g.getBaseHeight(x,z,type,bounds,random)==expected,"Base height/native material contract "+type+" "+x+","+z);
                require(profile.top(type.isOpaque(),x,z,max)==expected||expected==bounds.getMinY(),"Profile height/span contract "+type);checked++;
            }
        }
        return checked;
    }
    private static List<Object> waterWitnesses(ServerLevel level,RealisticGenerator g,boolean reopen,Path dir)throws Exception{
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var bed=d.iceReference();
        Path file=dir.resolve("water-sites.json");int[] buried=null,lid=null;int probes=0;
        if(Files.exists(file)){var rows=JsonParser.parseString(Files.readString(file)).getAsJsonArray();buried=new int[]{rows.get(0).getAsJsonArray().get(0).getAsInt(),rows.get(0).getAsJsonArray().get(1).getAsInt()};lid=new int[]{rows.get(1).getAsJsonArray().get(0).getAsInt(),rows.get(1).getAsJsonArray().get(1).getAsInt()};}
        else for(int i=0;i<d.size()&&(buried==null||lid==null);i++){
            int x=d.nodeX(i),z=d.nodeZ(i);if(Math.abs((long)z)<1835008||Math.abs((long)z)>1966080||x< -1664000||x> -1344000||!bed.river(i))continue;
            probes++;var original=bed.sample(x,z);if(original.waterLevel()<=original.height()+1||bed.habitatOceanWater(x,z,original))continue;
            if(buried==null&&d.continentalIceThickness(x,z)>0)buried=new int[]{x,z};
            if(lid==null&&d.permanentFreshwaterLid(x,z))lid=new int[]{x,z};
        }
        require(buried!=null&&lid!=null,"Finite polar river-node bracket must supply buried and exposed water witnesses: probes="+probes+" buried="+Arrays.toString(buried)+" lid="+Arrays.toString(lid));
        Files.writeString(file,new Gson().toJson(new int[][]{buried,lid})+"\n");var out=new ArrayList<Object>();
        for(var p:new int[][]{buried,lid}){
            int x=p[0],z=p[1];var original=bed.sample(x,z);var now=d.sample(x,z);var pos=new BlockPos(x,original.waterLevel()-1,z);level.getChunk(x>>4,z>>4);
            require(level.getBlockState(pos).is(Blocks.PACKED_ICE)||level.getBlockState(pos).is(Blocks.SNOW_BLOCK),"Native formerly exposed inland water is static ice");
            require(d.channelAt(x,z).equals(bed.channelAt(x,z))&&original.regionalWaterLevel()==now.regionalWaterLevel()&&original.basin()==now.basin(),"Solved hydrology/reference data retained");
            require(!bed.habitatOceanWater(x,z,original),"Freshwater witness is not corrected basin-owned marine water");
            if(p==buried)require(now.waterLevel()==DrainageNetwork.DRY&&now.height()>original.waterLevel(),"Buried river has no exposed surface-water datum");
            else{require(now.equals(original)&&!InlandLakeReedbed.site(d,x,z).lake(),"Exposed frozen water keeps physical geometry and excludes reeds");if(original.waterLevel()-original.height()>2)require(level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER),"Source water under permanent lid");}
            baseApis(level,g,x,z);out.add(Map.of("x",x,"z",z,"original",original.toString(),"current",now.toString(),"sheetDepth",d.continentalIceThickness(x,z),"lid",d.permanentFreshwaterLid(x,z),"marine",bed.habitatOceanWater(x,z,original),"terminalLake",bed.terminalLakeWater(x,z,original),"channel",bed.channelAt(x,z).toString()));
        }
        return out;
    }
    private static void profileBarrier(CompoundTag tag,ServerLevel level)throws Exception{
        require(tag.getIntOr("version",-1)==77,"Explicit affected profile barrier");
        var original=Class.forName("blockbranch.cubic.Published264TerrainProfile").getMethod("decode",CompoundTag.class,net.minecraft.world.level.chunk.PalettedContainerFactory.class);boolean rejected=false;
        try{original.invoke(null,tag,level.palettedContainerFactory());}catch(java.lang.reflect.InvocationTargetException expected){rejected=true;}require(rejected,"Actual original profile reader rejects77");
        for(String field:new String[]{"perennial_ice","perennial_ice_base_profile_version"}){var bad=tag.copy();bad.remove(field);rejectProfile(bad,level);}
        var bad=tag.copy();bad.putInt("perennial_ice_base_profile_version",77);rejectProfile(bad,level);bad=tag.copy();bad.putIntArray("perennial_ice",new int[256]);rejectProfile(bad,level);bad=tag.copy();bad.putInt("version",76);rejectProfile(bad,level);
    }
    private static void rejectProfile(CompoundTag tag,ServerLevel level){boolean failed=false;try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(RuntimeException expected){failed=true;}require(failed,"Malformed or unwrapped ice span rejected");}
    private static void rejected(RegistryOps<JsonElement> ops,JsonObject json){require(RealisticGenerator.CODEC.codec().parse(ops,json).error().isPresent(),"Malformed generator rejected");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private ContinentalIceQualification(){}
}
