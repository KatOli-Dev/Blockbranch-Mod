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

/** Independent published-258 outputs plus new forest/native/save contracts. */
public final class DryForestQualification {
    private static final int[][] POINTS={{1648000,-208000},{1744000,-176000}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.dryForestResult"));
        try {
            var level=server.overworld();var random=level.getChunkSource().randomState();
            if(Boolean.getBoolean("blockbranch.dryForestBoundaryOnly")){
                var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.dryForestDefinition()==1,"Boundary world uses candidate definition");
                var report=boundary(level,actual);
                Files.writeString(result.resolveSibling("boundary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                Files.writeString(result,"PASS bounded mixed dry-forest edge; native surface/base-column/base-height contracts on covered and uncovered columns\n");
                Blockbranch.LOGGER.info("DRY_FOREST_BOUNDARY_PASS {}",report);return;
            }
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());JsonObject fresh;
            try(var input=new InputStreamReader(Objects.requireNonNull(DryForestQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),StandardCharsets.UTF_8)){
                fresh=JsonParser.parseReader(input).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();
            }
            WarmXericFixtures.published267(fresh);
            if(fresh.has("leaf_litter_definition")){fresh.remove("leaf_litter_definition");fresh.addProperty("shape_version",264);}
            if(fresh.get("shape_version").getAsInt()==264){
                require(fresh.get("summer_dry_definition").getAsInt()==1,"Known complete summer-dry wrapper");
                fresh.remove("summer_dry_definition");fresh.addProperty("shape_version",263);fresh.getAsJsonObject("additional_biomes").remove(SummerDryWoodland.KEY);
            }
            if(fresh.get("shape_version").getAsInt()==263){
                require(fresh.get("land_ice_definition").getAsInt()==1,"Known complete land-ice wrapper");
                fresh.remove("land_ice_definition");fresh.addProperty("shape_version",262);fresh.getAsJsonObject("additional_biomes").remove(LandIceField.KEY);
            }
            if(fresh.get("shape_version").getAsInt()==262){
                require(fresh.get("deep_basin_relief_definition").getAsInt()==1,"Known complete deep-basin wrapper");
                fresh.remove("deep_basin_relief_definition");fresh.addProperty("shape_version",261);
            }
            if(fresh.get("shape_version").getAsInt()==261){
                require(fresh.get("abyssal_trace_definition").getAsInt()==1,"Known complete abyssal wrapper");
                fresh.remove("abyssal_trace_definition");fresh.addProperty("shape_version",260);
            }
            if(fresh.get("shape_version").getAsInt()==260){
                require(fresh.get("cool_desert_definition").getAsInt()==1,"Known complete cool-desert wrapper");
                fresh.remove("cool_desert_definition");fresh.addProperty("shape_version",259);fresh.getAsJsonObject("additional_biomes").remove(CoolDesert.KEY);
            }
            fresh.remove("type");if(fresh.has("continental_ice_definition")){fresh.remove("continental_ice_definition");fresh.addProperty("shape_version",264);fresh.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);}require(fresh.get("shape_version").getAsInt()==259,"Reviewed current wrapping preset");
            var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Exact fresh codec roundtrip");
            var old=fresh.deepCopy();old.remove("dry_forest_definition");old.addProperty("shape_version",258);old.getAsJsonObject("additional_biomes").remove(DryForest.KEY);
            var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            if(Boolean.getBoolean("blockbranch.dryForestClearingOnly")){
                var report=clearing(level,candidate,retained,result.getParent());
                Files.writeString(result.resolveSibling("clearing.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                Files.writeString(result,"PASS explicit schema259 dry-forest clearing model under current wrapping preset; historical clear plans retained\n");return;
            }
            require(retained.dryForestDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent gate retains exact published258 settings");
            for(int version:new int[]{82,257,258,260}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);rejected(ops,bad,"Partial/future foundation");}
            for(int value:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("dry_forest_definition",value);rejected(ops,bad,"Bad definition");}
            for(String key:new String[]{"dry_forest_definition","surface_ownership_definition","redwood_forest_definition","biome_transition_definition"}){var bad=fresh.deepCopy();bad.remove(key);rejected(ops,bad,"Missing complete foundation");}
            var malformed=fresh.deepCopy();malformed.addProperty("dry_forest_definition","invalid");rejected(ops,malformed,"Malformed definition");
            var missing=fresh.deepCopy();missing.getAsJsonObject("additional_biomes").remove(DryForest.KEY);rejected(ops,missing,"Missing biome holder");
            @SuppressWarnings("unchecked") var reader=(MapCodec<Object>)Class.forName("blockbranch.realistic.Published258RealisticGenerator").getField("CODEC").get(null);
            require(reader.codec().parse(ops,fresh).error().isPresent(),"Actual published258 reader rejects259");
            require(reader.codec().parse(ops,old).result().isPresent(),"Original reader accepts its own settings");
            boolean legacy=Boolean.getBoolean("blockbranch.dryForestLegacy"),reopen=Boolean.getBoolean("blockbranch.dryForestReopen");
            JsonObject baseline;
            try(var input=new InputStreamReader(Objects.requireNonNull(DryForestQualification.class.getResourceAsStream("/blockbranch-fixtures/dry-season-reader-258/baseline.json")),StandardCharsets.UTF_8)){baseline=JsonParser.parseReader(input).getAsJsonObject();}
            if(!legacy){baseline.remove("nativeStates");baseline.remove("nativeSHA256");}
            var snapshot=Published258Snapshot.capture(level,retained,legacy);
            require(baseline.equals(snapshot),"Gate-disabled output equals independently recorded complete published258 production: "+snapshot);
            var report=new LinkedHashMap<String,Object>();report.put("independentPublished258",snapshot);
            report.put("selector",selectors(HabitatPalette.CODEC.codec().parse(ops,old).getOrThrow(),HabitatPalette.CODEC.codec().parse(ops,fresh).getOrThrow()));
            var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.dryForestDefinition()==(legacy?0:1),"Actual saved gate retained");
            var d=actual.drainage(random);int nativeStates=0,owned=0,shade=0,oldShade=0,litter=0,grass=0;
            var scenes=new ArrayList<Object>();
            for(var p:POINTS){
                var source=(ScreeBiomeSource)actual.getBiomeSource();var selected=source.surfaceResolver().getNoiseBiome(p[0]>>2,0,p[1]>>2);
                scenes.add(Map.of("x",p[0],"z",p[1],"biome",selected.unwrapKey().orElseThrow().identifier().toString(),"potential",DrySeasonPolicy.potential(p[0],p[1],d,new ClimateField(random.seed()))));
                for(int z=p[1]-32;z<=p[1]+32;z++)for(int x=p[0]-32;x<=p[0]+32;x++){
                    level.getChunk(x>>4,z>>4);int ground=d.sample(x,z).height();
                    boolean domain=!legacy&&source.dryForestSurface().test(x,z);
                    if(domain){
                        owned++;if(actual.canopy(x,z,random).topY(x,z)>ground+2)shade++;
                        if(retained.canopy(x,z,random).topY(x,z)>ground+2)oldShade++;
                    }
                    for(int y=ground-3;y<=ground+40;y++){
                        var state=level.getBlockState(new BlockPos(x,y,z));
                        require(state.equals(actual.surfaceState(x,y,z,random)),"Native/analytic forest boundary at "+x+","+y+","+z+" native="+state+" analytic="+actual.surfaceState(x,y,z,random));
                        if(domain&&y==ground+1){if(state.is(Blocks.LEAF_LITTER))litter++;if(state.is(Blocks.SHORT_GRASS))grass++;}
                        nativeStates++;
                    }
                }
            }
            if(!legacy){
                require(owned>1000&&shade>owned*.60&&shade>oldShade+owned*.10,"Distinct closed broadleaf canopy rather than old open pine/oak: owned="+owned+" shade="+shade+" old="+oldShade);
                require(litter>owned*.35&&grass<owned*.05,"Litter-dominated native floor, rare grass: litter="+litter+" grass="+grass);
                // Qualify this fixture's explicit259 root/crown definition,
                // independently of later flags in the bundled world's generator.
                report.put("savedTreeClear",clearing(level,candidate,retained,result.getParent()));
            }
            var editPoint=Published258Snapshot.POINTS[0];int ex=editPoint[0]+1,ez=editPoint[1]+1;
            var edit=new BlockPos(ex,d.sample(ex,ez).height()-2,ez);
            if(legacy||reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Actual saved edit survives full relaunch");else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            report.put("actualLegacyWorld",legacy);report.put("nativeStates",nativeStates);report.put("ownedColumns",owned);report.put("shadedColumns",shade);report.put("publishedShadedColumns",oldShade);
            report.put("litterColumns",litter);report.put("grassColumns",grass);report.put("scenes",scenes);
            server.saveEverything(false,true,true);
            Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            Files.writeString(result,"PASS independent published258 hashes; saved gate/edit; original reader; forest-only selector; native canopy/floor\n");
            Blockbranch.LOGGER.info("DRY_FOREST_NATIVE_PASS {}",report);
        }catch(Throwable failure){Blockbranch.LOGGER.error("DRY_FOREST_NATIVE_FAIL",failure);try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static Map<String,Object> selectors(HabitatPalette oldPalette,HabitatPalette palette)throws Exception {
        int samples=0,changed=0;var seeds=new ArrayList<Object>();
        var type=Class.forName("blockbranch.realistic.Published258ScreeBiomeSource");
        var redwood=type.getDeclaredMethod("withRedwoodForests",boolean.class);redwood.setAccessible(true);
        var ownership=type.getDeclaredMethod("withSurfaceOwnership",boolean.class);ownership.setAccessible(true);
        var bind=type.getDeclaredMethod("bind",long.class,DrainageNetwork.class,ClimateField.class);bind.setAccessible(true);
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true);var c=new ClimateField(seed);
            var reader=type.getConstructor(HabitatPalette.class,boolean.class,boolean.class).newInstance(oldPalette,true,true);
            redwood.invoke(reader,true);ownership.invoke(reader,true);bind.invoke(reader,seed,d,c);
            var prior=(BiomeResolver)type.getMethod("surfaceResolver").invoke(reader);
            var now=new ScreeBiomeSource(palette,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true);now.bind(seed,d,c);
            int count=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var before=prior.getNoiseBiome(x>>2,0,z>>2);var after=now.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);
                if(before!=after){
                    require(before==oldPalette.land().warm()&&after==palette.additional(DryForest.KEY),"Only established warm-woodland owner can split: "+seed+" "+x+","+z+" "+before+" -> "+after);
                    require(DryForest.eligible(seed,x,z,d,c),"Genuine new independent seasonality predicate at split");count++;
                }
                samples++;
            }
            require(count>0,"Native registry/selector actually admits a subtype for each fixed seed");changed+=count;seeds.add(Map.of("seed",seed,"samples",14400,"warmForestSplits",count));
        }
        return Map.of("samples",samples,"warmForestSplits",changed,"unrelatedOwnerChanges",0,"seeds",seeds);
    }
    private static Map<String,Object> clearing(net.minecraft.server.level.ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory)throws Exception {
        var random=level.getChunkSource().randomState();var d=actual.drainage(random);var c=new ClimateField(random.seed());var context=StructurePieceSerializationContext.fromLevel(level);
        var p=POINTS[0];var owner=actual.dryForestOwner(random);
        var root=DryForestTrees.roots(random.seed(),p[0]-16,p[1]-16,p[0]+16,p[1]+16,d,c,owner).stream().filter(r->r.height()>=18).findFirst().orElseThrow();
        var box=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());
        var piece=new ReimaginedTreeClearPiece(box,random.seed(),actual.borealCrownDefinition(),actual.vegetationFlags());
        var tag=piece.createTag(context);require(tag.getIntOr("ClearDefinition",0)==16,"Saved current root/crown flags include dry forest");
        var plan=BorealClearSmoke.plan(piece,actual,random);
        require(!plan.isEmpty()&&ForestTrees.tree(root).stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))),"New tree-clear plan covers entire immutable dry-forest footprint");
        Path file=directory.resolve("dry-tree-clear.nbt");NbtIo.writeCompressed(tag,file);
        var restored=BorealClearSmoke.load(context,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()));
        require(plan.equals(BorealClearSmoke.plan(restored,actual,random)),"Compressed saved new clear plan retains exact footprint");
        var oldRoot=WarmTrees.roots(random.seed(),p[0]-16,p[1]-16,p[0]+16,p[1]+16,d,c).getFirst();
        var oldBox=new BoundingBox(oldRoot.x(),oldRoot.y(),oldRoot.z(),oldRoot.x(),oldRoot.y(),oldRoot.z());
        for(int flags=0;flags<4;flags++)for(int crown=0;crown<2;crown++){
            var oldPiece=new ReimaginedTreeClearPiece(oldBox,random.seed(),crown,flags);var oldTag=oldPiece.createTag(context);
            require(oldTag.getIntOr("ClearDefinition",0)==1+crown+2*flags,"Historical clear definition retained");
            require(oldPiece.getBoundingBox().maxY()==oldBox.maxY()+(flags>=2?144:80),"Historical halo retained");
            var before=BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),retained,random);
            var after=BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),actual,random);
            require(!before.isEmpty()&&before.equals(after),"Historical clear footprints do not adopt current dry-forest roots");
        }
        return Map.of("newDefinition",16,"newFootprintCells",plan.size(),"historicalDefinitionsCompared",8);
    }
    private static Map<String,Object> boundary(net.minecraft.server.level.ServerLevel level,RealisticGenerator g){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var owner=g.dryForestOwner(random);
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
        require(edge!=null,"Dry-land edge must occur inside two fixed ±512-block candidate neighborhoods; never expand search");
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
    private DryForestQualification(){}
}
