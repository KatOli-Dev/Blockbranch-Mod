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
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.nbt.*;

/** Independent published-263 outputs plus new forest/native/save contracts. */
public final class SummerDryQualification {
    private static final int[][] POINTS={{272000,-720000},{144000,-688000},{-16000,-656000}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.summerDryResult"));
        try {
            var level=server.overworld();var random=level.getChunkSource().randomState();
            if(Boolean.getBoolean("blockbranch.summerDryBoundaryOnly")){
                var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.summerDryDefinition()==1,"Boundary world uses candidate definition");
                var report=boundary(level,actual);
                Files.writeString(result.resolveSibling("boundary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                Files.writeString(result,"PASS bounded mixed summer-dry woodland edge; native surface/base-column/base-height contracts on covered and uncovered columns\n");
                Blockbranch.LOGGER.info("SUMMER_DRY_BOUNDARY_PASS {}",report);return;
            }
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());JsonObject fresh;
            try(var input=new InputStreamReader(Objects.requireNonNull(SummerDryQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),StandardCharsets.UTF_8)){
                fresh=JsonParser.parseReader(input).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();
            }
            WarmXericFixtures.published267(fresh);
            fresh.remove("type");require(fresh.get("shape_version").getAsInt()==267,"Reviewed current combined preset before stripping to the 264 wrapper");if(fresh.has("leaf_litter_definition"))fresh.remove("leaf_litter_definition");if(fresh.has("continental_ice_definition")){fresh.remove("continental_ice_definition");fresh.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);}fresh.addProperty("shape_version",264);require(fresh.get("shape_version").getAsInt()==264,"Reviewed current wrapping preset");
            var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Exact fresh codec roundtrip");
            var old=fresh.deepCopy();old.remove("summer_dry_definition");old.addProperty("shape_version",263);old.getAsJsonObject("additional_biomes").remove(SummerDryWoodland.KEY);
            var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(retained.summerDryDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent gate retains exact published263 settings");
            for(int version:new int[]{82,257,258,259,260,261,262,263,265}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);rejected(ops,bad,"Partial/future foundation");}
            for(int value:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("summer_dry_definition",value);rejected(ops,bad,"Bad definition");}
            for(String key:new String[]{"summer_dry_definition","land_ice_definition","deep_basin_relief_definition","abyssal_trace_definition","cool_desert_definition","dry_forest_definition","surface_ownership_definition","redwood_forest_definition","biome_transition_definition"}){var bad=fresh.deepCopy();bad.remove(key);rejected(ops,bad,"Missing complete foundation");}
            var malformed=fresh.deepCopy();malformed.addProperty("summer_dry_definition","invalid");rejected(ops,malformed,"Malformed definition");
            var missing=fresh.deepCopy();missing.getAsJsonObject("additional_biomes").remove(SummerDryWoodland.KEY);rejected(ops,missing,"Missing biome holder");
            @SuppressWarnings("unchecked") var reader=(MapCodec<Object>)Class.forName("blockbranch.realistic.Published263RealisticGenerator").getField("CODEC").get(null);
            require(reader.codec().parse(ops,fresh).error().isPresent(),"Actual published263 reader rejects264");
            require(reader.codec().parse(ops,old).result().isPresent(),"Original reader accepts its own settings");
            boolean legacy=Boolean.getBoolean("blockbranch.summerDryLegacy"),reopen=Boolean.getBoolean("blockbranch.summerDryReopen");
            var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.summerDryDefinition()==(legacy?0:1),"Actual saved gate retained");
            var report=new LinkedHashMap<String,Object>();
            if(Boolean.getBoolean("blockbranch.summerDrySavedPlanOnly")){
                require(reopen&&!legacy,"Saved-plan-only uses the actual new world in a later process");
                report.put("savedTreeClear",clearing(level,actual,retained,result.getParent(),true));
                report.put("retainedShrubLeaf",retainedShrub(level,actual));
                report.put("unchangedSavedCanopies",unchangedCanopies(level,actual));
                Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                Files.writeString(result,"PASS cross-process tree-clear replay; retained shrub leaf distances; unchanged saved native canopy arrays; all16 historical plans retained\n");return;
            }
            boolean nativeOnly=Boolean.getBoolean("blockbranch.summerDryNativeOnly");
            if(nativeOnly){require(!legacy,"Native-only mode is for affected new content, not independent legacy qualification");if(reopen)require(Files.readString(result.resolveSibling("result.txt")).startsWith("PASS"),"Completed initial affected qualification required");report.put("independentPublished263Evidence","Not rerun in this affected-content phase; full baseline and selector qualification are separate");}
            else if(!reopen){
                JsonObject baseline;
                try(var input=new InputStreamReader(Objects.requireNonNull(SummerDryQualification.class.getResourceAsStream("/blockbranch-fixtures/summer-dry-reader-263/baseline.json")),StandardCharsets.UTF_8)){baseline=JsonParser.parseReader(input).getAsJsonObject();}
                if(!legacy){baseline.remove("nativeStates");baseline.remove("nativeSHA256");baseline.remove("nativeProfileSHA256");}
                var snapshot=Published263Snapshot.capture(level,legacy?actual:retained,legacy);
                require(baseline.equals(snapshot),"Gate-disabled output equals independently recorded complete published263 production: "+snapshot);
                report.put("independentPublished263",snapshot);
            }else{
                require(Files.readString(result.resolveSibling("result.txt")).startsWith("PASS"),"Reuse completed qualification before reopen");
                report.put("independentPublished263Evidence","result.txt.json; recorded and checked on initial qualification, not re-recorded");
            }
            if(!legacy&&!nativeOnly&&(!reopen||Boolean.getBoolean("blockbranch.summerDrySurvey"))){
                report.put("selector",selectors(HabitatPalette.CODEC.codec().parse(ops,old).getOrThrow(),HabitatPalette.CODEC.codec().parse(ops,fresh).getOrThrow(),result.getParent()));report.put("preset",fresh);
            }
            var d=actual.drainage(random);int nativeStates=0,owned=0,shade=0,oldShade=0,litter=0,grass=0;
            var scenes=new ArrayList<Object>();var checkedProfiles=new HashSet<Long>();
            BlockPos cleared=null;Path clearedFile=result.resolveSibling("cleared-stem.json");
            if(reopen&&!legacy){var e=JsonParser.parseString(Files.readString(clearedFile)).getAsJsonArray();cleared=new BlockPos(e.get(0).getAsInt(),e.get(1).getAsInt(),e.get(2).getAsInt());require(level.getBlockState(cleared).isAir(),"Player-cleared woody stem retained");}
            for(var p:POINTS){
                var source=(ScreeBiomeSource)actual.getBiomeSource();var selected=source.surfaceResolver().getNoiseBiome(p[0]>>2,0,p[1]>>2);
                int witnessY=d.sample(p[0],p[1]).height()+1;level.getChunk(p[0]>>4,p[1]>>4);
                var generated=level.getNoiseBiome(p[0]>>2,witnessY>>2,p[1]>>2);var visible=level.getBiome(new BlockPos(p[0],witnessY,p[1]));
                scenes.add(Map.of("x",p[0],"y",witnessY,"z",p[1],"surfaceSelector",selected.unwrapKey().orElseThrow().identifier().toString(),"generatedQuartBiome",generated.unwrapKey().orElseThrow().identifier().toString(),"visibleBiome",visible.unwrapKey().orElseThrow().identifier().toString(),"quartOverrideObserved",!selected.equals(generated),"potential",SummerDryClimate.potential(p[0],p[1],d,new ClimateField(random.seed()))));
                for(int z=p[1]-32;z<=p[1]+32;z++)for(int x=p[0]-32;x<=p[0]+32;x++){
                    var nativeChunk=level.getChunk(x>>4,z>>4);int ground=d.sample(x,z).height();
                    if(checkedProfiles.add(((long)(x>>4)<<32)|((z>>4)&0xffffffffL))){var profile=((blockbranch.cubic.SparseColumn)nativeChunk).blockbranch$terrain().base();var tag=profile.encode(level.palettedContainerFactory());require(blockbranch.cubic.TerrainProfile.decode(tag,level.palettedContainerFactory()).encode(level.palettedContainerFactory()).equals(tag),"Existing profile codec roundtrip");}
                    boolean domain=!legacy&&source.summerDrySurface().test(x,z);
                    if(domain){
                        owned++;if(actual.canopy(x,z,random).topY(x,z)>ground+2)shade++;
                        if(retained.canopy(x,z,random).topY(x,z)>ground+2)oldShade++;
                    }
                    for(int y=ground-3;y<=ground+40;y++){
                        var position=new BlockPos(x,y,z);if(position.equals(cleared))continue;
                        var state=level.getBlockState(position);
                        require(state.equals(actual.surfaceState(x,y,z,random)),"Native/analytic forest boundary at "+x+","+y+","+z+" native="+state+" analytic="+actual.surfaceState(x,y,z,random));
                        if(domain&&y==ground+1){if(state.is(Blocks.LEAF_LITTER))litter++;if(state.is(Blocks.SHORT_GRASS))grass++;}
                        nativeStates++;
                    }
                }
            }
            if(!legacy){
                require(owned>1000&&shade>owned*.60&&shade>oldShade+owned*.10,"Distinct closed broadleaf canopy rather than old open pine/oak: owned="+owned+" shade="+shade+" old="+oldShade);
                require(litter>owned*.35&&grass>0&&grass<owned*.10,"Litter-dominated native floor, rare grass: litter="+litter+" grass="+grass);
                int overstory=0,shrubs=0;
                for(var p:POINTS)for(var root:SummerDryTrees.roots(random.seed(),p[0]-32,p[1]-32,p[0]+32,p[1]+32,d,new ClimateField(random.seed()),actual.summerDryOwner(random))){if(root.height()<=5)shrubs++;else overstory++;}
                require(overstory>0&&shrubs>0,"Nonvacuous oak canopy and woody shrub layer");report.put("overstoryRoots",overstory);report.put("shrubRoots",shrubs);
                if(!reopen)report.put("boundaryAndBaseApis",boundary(level,actual));
                report.put("savedTreeClear",clearing(level,actual,retained,result.getParent(),reopen));
            }
            var editPoint=Published263Snapshot.POINTS[0];int ex=editPoint[0]+1,ez=editPoint[1]+1;
            var edit=new BlockPos(ex,d.sample(ex,ez).height()-2,ez);
            if(legacy||reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Actual saved edit survives full relaunch");else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            if(!legacy&&!reopen){
                var p=POINTS[0];var root=SummerDryTrees.roots(random.seed(),p[0]-16,p[1]-16,p[0]+16,p[1]+16,d,new ClimateField(random.seed()),actual.summerDryOwner(random)).stream().filter(r->r.height()<=5).findFirst().orElseThrow();
                cleared=new BlockPos(root.x(),root.y(),root.z());require(level.getBlockState(cleared).is(net.minecraft.tags.BlockTags.LOGS),"Actual natural woody stem before clearing");level.setBlock(cleared,Blocks.AIR.defaultBlockState(),3);Files.writeString(clearedFile,new Gson().toJson(new int[]{cleared.getX(),cleared.getY(),cleared.getZ()})+"\n");
            }
            report.put("clearedStem",cleared==null?null:cleared.toShortString());
            report.put("actualLegacyWorld",legacy);report.put("nativeStates",nativeStates);report.put("ownedColumns",owned);report.put("shadedColumns",shade);report.put("publishedShadedColumns",oldShade);
            report.put("litterColumns",litter);report.put("grassColumns",grass);report.put("scenes",scenes);
            server.saveEverything(false,true,true);
            Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            Files.writeString(result,legacy?"PASS authentic original263 world; retained saved gate/edit and untouched native terrain; independent initial published hashes\n":"PASS saved new woodland gate/edits; original reader barrier; native oak/shrub canopy/floor; initial published-hash and selector evidence retained\n");
            Blockbranch.LOGGER.info("SUMMER_DRY_NATIVE_PASS {}",report);
        }catch(Throwable failure){Blockbranch.LOGGER.error("SUMMER_DRY_NATIVE_FAIL",failure);try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static Map<String,Object> selectors(HabitatPalette oldPalette,HabitatPalette palette,Path directory)throws Exception {
        int samples=0,changed=0;var seeds=new ArrayList<Object>();var footprints=new TreeMap<String,Integer>();var fallbackCounts=new TreeMap<String,Integer>();var occupancy=new TreeMap<String,TreeMap<String,Integer>>();var inventory=new TreeSet<String>();for(var holder:new ScreeBiomeSource(palette).possibleBiomes())inventory.add(holder.unwrapKey().orElseThrow().identifier().toString());
        for(String cave:new String[]{"lush_caves","dripstone_caves","deep_dark","sulfur_caves"}){var holder=palette.additional().get(cave);if(holder!=null)inventory.remove(holder.unwrapKey().orElseThrow().identifier().toString());}
        var csv=new StringBuilder("seed,x,z,footprint,publishedBiome,currentBiome,fallback\n");
        var type=Class.forName("blockbranch.realistic.Published263ScreeBiomeSource");
        var redwood=type.getDeclaredMethod("withRedwoodForests",boolean.class);redwood.setAccessible(true);
        var ownership=type.getDeclaredMethod("withSurfaceOwnership",boolean.class);ownership.setAccessible(true);
        var bind=type.getDeclaredMethod("bind",long.class,DrainageNetwork.class,ClimateField.class);bind.setAccessible(true);
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true);var c=new ClimateField(seed);
            var reader=type.getConstructor(HabitatPalette.class,boolean.class,boolean.class).newInstance(oldPalette,true,true);
            redwood.invoke(reader,true);ownership.invoke(reader,true);
            for(String method:new String[]{"withDryForests","withCoolDeserts","withLandIce"}){var flag=type.getDeclaredMethod(method,boolean.class);flag.setAccessible(true);flag.invoke(reader,true);}
            bind.invoke(reader,seed,d,c);
            var prior=(BiomeResolver)type.getMethod("surfaceResolver").invoke(reader);
            var now=new ScreeBiomeSource(palette,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true).withSummerDryWoodlands(true);now.bind(seed,d,c);
            var m=palette.margins();var sentinel=Holder.direct(m.fallback().value());
            var taggedPalette=new HabitatPalette(new HabitatPalette.Margins(sentinel,m.scree(),m.reedbed(),m.riverMargin(),m.sandyShore(),m.lake(),m.riverChannel(),m.shelf(),m.deepOcean()),palette.land(),palette.additional());
            var tagged=new ScreeBiomeSource(taggedPalette,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true).withSummerDryWoodlands(true);tagged.bind(seed,d,c);
            int count=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var before=prior.getNoiseBiome(x>>2,0,z>>2);var after=now.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);
                var site=d.sample(x,z);String footprint=CurrentBiomeSurvey.footprint(site,d.habitatOceanWater(x,z,site),d.terminalLakeWater(x,z,site),d.channelAt(x,z).edge()>=0);
                var marked=tagged.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);require(after.value()==marked.value(),"Sentinel cannot alter selection");boolean fallback=marked==sentinel;
                String id=after.unwrapKey().orElseThrow().identifier().toString();inventory.remove(id);footprints.merge(footprint,1,Integer::sum);if(fallback)fallbackCounts.merge(footprint,1,Integer::sum);occupancy.computeIfAbsent(footprint,k->new TreeMap<>()).merge(id,1,Integer::sum);
                csv.append(seed).append(',').append(x).append(',').append(z).append(',').append(footprint).append(',').append(before.unwrapKey().orElseThrow().identifier()).append(',').append(id).append(',').append(fallback).append('\n');
                if(before!=after){
                    require(before==oldPalette.land().warm()&&after==palette.additional(SummerDryWoodland.KEY),"Only established warm-woodland owner can split: "+seed+" "+x+","+z+" "+before+" -> "+after);
                    require(SummerDryWoodland.eligible(seed,x,z,d,c),"Genuine new independent seasonality predicate at split");count++;
                }
                samples++;
            }
            require(count>0,"Native registry/selector actually admits a subtype for each fixed seed");changed+=count;seeds.add(Map.of("seed",seed,"samples",14400,"warmForestSplits",count));
        }
        Files.writeString(directory.resolve("selector-samples.csv"),csv);
        var report=new LinkedHashMap<String,Object>();report.put("samples",samples);report.put("warmForestSplits",changed);report.put("unrelatedOwnerChanges",0);report.put("seeds",seeds);
        report.put("boundsXZHalfOpen",List.of(-1920000,1920000));report.put("stride",32000);report.put("centerOffset",16000);report.put("publishedSchema",263);report.put("currentSchema",264);report.put("footprints",footprints);report.put("fallbacksByFootprint",fallbackCounts);report.put("occupancyByFootprint",occupancy);report.put("missingInSampleNotUnreachable",inventory);report.put("accounting","Disjoint coarse game-grid samples only; no Earth-area or ecological-completeness estimate");return report;
    }
    private static Map<String,Object> clearing(net.minecraft.server.level.ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory,boolean reopen)throws Exception {
        var random=level.getChunkSource().randomState();var d=actual.drainage(random);var c=new ClimateField(random.seed());var context=StructurePieceSerializationContext.fromLevel(level);
        var p=POINTS[0];var owner=actual.summerDryOwner(random);
        var root=SummerDryTrees.roots(random.seed(),p[0]-16,p[1]-16,p[0]+16,p[1]+16,d,c,owner).stream().filter(r->r.height()>=12).findFirst().orElseThrow();
        var box=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());
        var piece=new ReimaginedTreeClearPiece(box,random.seed(),actual.borealCrownDefinition(),actual.vegetationFlags());
        var tag=piece.createTag(context);require(tag.getIntOr("ClearDefinition",0)==32,"Saved current root/crown flags include summer-dry woodland");
        var plan=BorealClearSmoke.plan(piece,actual,random);
        require(!plan.isEmpty()&&SummerDryTrees.tree(root).stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))),"New tree-clear plan covers entire immutable summer-dry woodland footprint");
        Path file=directory.resolve("summer-dry-tree-clear.nbt");
        if(reopen)require(Files.exists(file),"Requires prior process's actual compressed plan");
        else{require(!Files.exists(file),"Preserve prior saved-plan evidence");NbtIo.writeCompressed(tag,file);}
        var saved=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());require(saved.equals(tag),"Prior compressed plan retains exact saved definition/seed/box");
        var restored=BorealClearSmoke.load(context,saved);
        require(plan.equals(BorealClearSmoke.plan(restored,actual,random)),"Compressed saved new clear plan retains exact footprint");
        var oldRoot=WarmTrees.roots(random.seed(),p[0]-16,p[1]-16,p[0]+16,p[1]+16,d,c).getFirst();
        var oldBox=new BoundingBox(oldRoot.x(),oldRoot.y(),oldRoot.z(),oldRoot.x(),oldRoot.y(),oldRoot.z());
        for(int flags=0;flags<8;flags++)for(int crown=0;crown<2;crown++){
            var oldPiece=new ReimaginedTreeClearPiece(oldBox,random.seed(),crown,flags);var oldTag=oldPiece.createTag(context);
            require(oldTag.getIntOr("ClearDefinition",0)==1+crown+2*flags,"Historical clear definition retained");
            require(oldPiece.getBoundingBox().maxY()==oldBox.maxY()+((flags&RedwoodForest.FLAG)!=0?144:80),"Historical halo retained");
            var before=BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),retained,random);
            var after=BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),actual,random);
            require(!before.isEmpty()&&before.equals(after),"Historical clear footprints do not adopt current summer-dry woodland roots");
        }
        return Map.of("newDefinition",32,"newFootprintCells",plan.size(),"historicalDefinitionsCompared",16);
    }
    private static Map<String,Object> unchangedCanopies(net.minecraft.server.level.ServerLevel level,RealisticGenerator g){
        var random=level.getChunkSource().randomState();var chunks=new HashSet<Long>();int states=0;
        for(var p:POINTS)for(int cz=(p[1]-32)>>4;cz<=(p[1]+32)>>4;cz++)for(int cx=(p[0]-32)>>4;cx<=(p[0]+32)>>4;cx++){
            if(!chunks.add(((long)cx<<32)|(cz&0xffffffffL)))continue;
            var profile=((blockbranch.cubic.SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain().base();
            var saved=profile.canopy().encode();var computed=g.canopy(cx*16,cz*16,random).encode();
            require(Arrays.equals(saved,computed),"Raw support correction cannot change previously saved rasterized canopy at "+cx+","+cz);states+=saved.length;
        }
        return Map.of("chunks",chunks.size(),"packedCanopyStates",states,"centers",POINTS,"inclusiveRadiusBlocks",32,"comparison","Current canopy arrays exactly equal prior process's saved native base arrays");
    }
    private static Map<String,Object> retainedShrub(net.minecraft.server.level.ServerLevel level,RealisticGenerator g)throws Exception {
        record Retained(WoodlandTrees.Root root,int code){}
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed());var context=StructurePieceSerializationContext.fromLevel(level);int candidates=0;
        for(var p:POINTS){
            var geometry=new LinkedHashMap<WoodlandTrees.Root,List<WoodlandTrees.Voxel>>();
            for(var root:SummerDryTrees.roots(random.seed(),p[0]-32,p[1]-32,p[0]+32,p[1]+32,d,c,g.summerDryOwner(random)))geometry.put(root,SummerDryTrees.tree(root));
            for(var cut:geometry.keySet()){
                if(cut.height()<=5||Math.abs(cut.x()-p[0])>16||Math.abs(cut.z()-p[1])>16)continue;candidates++;
                var box=new BoundingBox(cut.x(),cut.y(),cut.z(),cut.x(),cut.y(),cut.z());var removed=new HashSet<Long>();var retained=new HashMap<Long,Retained>();
                for(var entry:geometry.entrySet()){
                    boolean selected=entry.getValue().stream().anyMatch(v->WoodlandTrees.logCode(v.code())&&box.isInside(v.x(),v.y(),v.z()));
                    for(var v:entry.getValue()){
                        long key=BlockPos.asLong(v.x(),v.y(),v.z());
                        if(selected)removed.add(key);
                        else{var old=retained.get(key);if(old==null||WoodlandTrees.logCode(v.code())&&!WoodlandTrees.logCode(old.code()))retained.put(key,new Retained(entry.getKey(),v.code()));}
                    }
                }
                for(var entry:retained.entrySet())if(entry.getValue().root().height()<=5&&entry.getValue().code()>7&&removed.contains(entry.getKey())){
                    var piece=new ReimaginedTreeClearPiece(box,random.seed(),g.borealCrownDefinition(),g.vegetationFlags());var plan=BorealClearSmoke.plan(piece,g,random);
                    int expected=entry.getValue().code();require(Objects.equals(plan.get(entry.getKey()),expected),"Actual retained-shrub replacement preserves raw supported leaf state");
                    var field=ReimaginedTreeClearPiece.class.getDeclaredField("STATES");field.setAccessible(true);var states=(net.minecraft.world.level.block.state.BlockState[])field.get(null);
                    require(states[plan.get(entry.getKey())].getValue(LeavesBlock.DISTANCE)==expected-6,"Actual clear replacement table retains distance greater than one");
                    var at=BlockPos.of(entry.getKey());var shrub=entry.getValue().root();
                    return Map.of("cutRoot",List.of(cut.x(),cut.y(),cut.z()),"retainedShrubRoot",List.of(shrub.x(),shrub.y(),shrub.z()),"replacementPosition",List.of(at.getX(),at.getY(),at.getZ()),"replacementLeafCode",expected,"replacementLeafDistance",expected-6,"candidateRootsExamined",candidates,"bounds","Existing three centers +/-32m; cut-root centers within +/-16m; deterministic finite geometry search");
                }
            }
        }
        throw new AssertionError("No retained-shrub overlap in the finite existing candidate neighborhoods");
    }
    private static Map<String,Object> boundary(net.minecraft.server.level.ServerLevel level,RealisticGenerator g){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var owner=g.summerDryOwner(random);
        int[] edge=null;int discoveryPoints=0;
        search:for(var center:POINTS)for(int z=center[1]-512;z<=center[1]+512;z+=16)for(int x=center[0]-512;x<=center[0]+512;x+=16){
            discoveryPoints++;if(!owner.test(x,z))continue;
            for(var direction:new int[][]{{16,0},{0,16},{-16,0},{0,-16}}){
                int bx=x+direction[0],bz=z+direction[1];
                if(Math.abs(bx-center[0])>512||Math.abs(bz-center[1])>512||owner.test(bx,bz))continue;
                int ax=x,az=z;
                while(Math.abs(ax-bx)+Math.abs(az-bz)>1){int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);if(owner.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
                if(d.sample(ax,az).waterLevel()!=DrainageNetwork.DRY||d.sample(bx,bz).waterLevel()!=DrainageNetwork.DRY)continue;
                edge=new int[]{ax,az,bx,bz};break search;
            }
        }
        require(edge!=null,"Dry-land edge must occur inside three fixed ±512-block candidate neighborhoods; never expand search");
        require(owner.test(edge[0],edge[1])&&!owner.test(edge[2],edge[3]),"Adjacent covered/uncovered edge witness");
        int columns=0,covered=0,uncovered=0,surfaceStates=0;var api=new LinkedHashSet<Long>();
        api.add((edge[0]&0xffffffffL)|((long)edge[1]<<32));api.add((edge[2]&0xffffffffL)|((long)edge[3]<<32));
        var chunks=new HashSet<Long>();
        for(int z=edge[1]-16;z<=edge[1]+16;z++)for(int x=edge[0]-16;x<=edge[0]+16;x++){
            var chunk=level.getChunk(x>>4,z>>4);chunks.add(((long)(x>>4)<<32)|((z>>4)&0xffffffffL));
            if(owner.test(x,z))covered++;else uncovered++;
            int ground=d.sample(x,z).height();
            for(int y=ground-3;y<=ground+40;y++){require(level.getBlockState(new BlockPos(x,y,z)).equals(g.surfaceState(x,y,z,random)),"Native/analytic mixed edge at "+x+","+y+","+z);surfaceStates++;}
            if(((x-edge[0])&3)==0&&((z-edge[1])&3)==0)api.add((x&0xffffffffL)|((long)z<<32));
            columns++;
        }
        require(covered>0&&uncovered>0,"Nonvacuous mixed native window");
        int apiCovered=0,apiUncovered=0,columnStates=0,heights=0;
        for(long key:api){
            int x=(int)key,z=(int)(key>>32),ground=d.sample(x,z).height();var chunk=level.getChunk(x>>4,z>>4);
            if(owner.test(x,z))apiCovered++;else apiUncovered++;
            for(var bounds:List.of((net.minecraft.world.level.LevelHeightAccessor)level,
                net.minecraft.world.level.LevelHeightAccessor.create(ground-3,3),
                net.minecraft.world.level.LevelHeightAccessor.create(ground+1,3),
                net.minecraft.world.level.LevelHeightAccessor.create(ground+8,8))){
                var column=g.getBaseColumn(x,z,bounds,random);
                for(int y=Math.max(ground-3,bounds.getMinY());y<=Math.min(ground+40,bounds.getMaxY());y++){
                    require(column.getBlock(y).equals(level.getBlockState(new BlockPos(x,y,z))),"Base-column/native changed surface contract at "+x+","+y+","+z);columnStates++;
                }
                for(var type:net.minecraft.world.level.levelgen.Heightmap.Types.values()){
                    int expected=bounds.getMinY();
                    if(bounds==level)expected=chunk.getHeight(type,x&15,z&15)+1;
                    else for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(level.getBlockState(new BlockPos(x,y,z)))){expected=y+1;break;}
                    int actual=g.getBaseHeight(x,z,type,bounds,random);
                    require(actual==expected,"Base-height/native or clipped native predicate "+type+" at "+x+","+z+" min/max="+bounds.getMinY()+"/"+bounds.getMaxY()+" actual="+actual+" expected="+expected);heights++;
                }
            }
        }
        require(apiCovered>0&&apiUncovered>0,"Direct base APIs exercise both ownership branches");
        var report=new LinkedHashMap<String,Object>();report.put("seed",random.seed());report.put("candidateCenters",POINTS);
        report.put("discoveryInclusiveRadiusBlocks",512);report.put("discoveryStrideBlocks",16);report.put("discoveryPoints",discoveryPoints);report.put("adjacentCoveredUncoveredXZ",edge);
        report.put("nativeInclusiveRadiusBlocks",16);report.put("nativeStrideBlocks",1);report.put("nativeChunks",chunks.size());report.put("nativeColumns",columns);
        report.put("coveredColumns",covered);report.put("uncoveredColumns",uncovered);report.put("nativeSurfaceStates",surfaceStates);report.put("surfaceYBounds","local ground -3 through +40 inclusive");
        report.put("apiStrideBlocks",4);report.put("apiCoveredColumns",apiCovered);report.put("apiUncoveredColumns",apiUncovered);report.put("baseColumnNativeStates",columnStates);report.put("baseHeightNativeOrClippedComparisons",heights);
        report.put("apiBounds",List.of(List.of(level.getMinY(),level.getMaxY()),"[ground-3,ground-1]","[ground+1,ground+3]","[ground+8,ground+15]"));return report;
    }
    private static void rejected(RegistryOps<JsonElement> ops,JsonObject json,String message){require(RealisticGenerator.CODEC.codec().parse(ops,json).error().isPresent(),message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SummerDryQualification(){}
}
