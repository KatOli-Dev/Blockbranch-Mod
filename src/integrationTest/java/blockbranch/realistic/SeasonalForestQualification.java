package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Native canopy/understory, original-owner, codec, saved-plan and API evidence.
 * Static growing-season structure, never proof of phenology or full ecology. */
public final class SeasonalForestQualification {
    private static final long SEED=Long.getLong("blockbranch.seasonalForestSeed",424242);
    private static final int[][] POINTS=SEED==1?new int[][]{{1488000,-304000},{1520000,-336000}}:new int[][]{{528000,-272000},{560000,-208000}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.seasonalForestResult"));
        try{
            boolean reopen=Boolean.getBoolean("blockbranch.seasonalForestReopen");
            var level=server.overworld();var random=level.getChunkSource().randomState();require(random.seed()==SEED,"Disclosed native seed");
            var g=(RealisticGenerator)level.getChunkSource().getGenerator();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var fresh=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
            require(g.seasonalForestDefinition()==1&&fresh.get("shape_version").getAsInt()==269,"Actual saved refinement");
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow()).getOrThrow().equals(fresh),"Flat exact codec roundtrip");
            var old=fresh.deepCopy();SeasonalForestFixtures.published268(old);
            var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(retained.seasonalForestDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent retains complete268 exactly");
            for(int version:new int[]{264,265,266,267,268,270}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);reject(ops,bad);}
            for(int definition:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("seasonal_forest_definition",definition);reject(ops,bad);}
            for(String key:new String[]{"seasonal_forest_definition","dry_forest_definition","warm_xeric_definition","leaf_litter_definition"}){var bad=fresh.deepCopy();bad.remove(key);reject(ops,bad);}
            var bad=fresh.deepCopy();bad.addProperty("seasonal_forest_definition","invalid");reject(ops,bad);
            var report=new LinkedHashMap<String,Object>();report.put("seed",SEED);report.put("oldCodecCases",LeafLitterAudit.gateMatrix(ops,g));
            if(!reopen&&SEED==424242)report.put("selector",selectors(HabitatPalette.CODEC.codec().parse(ops,fresh).getOrThrow()));
            var d=g.drainage(random);var owner=g.dryForestOwner(random);var oldOwner=retained.dryForestOwner(random);
            var nativeHash=MessageDigest.getInstance("SHA-256");var profileHash=MessageDigest.getInstance("SHA-256");
            var chunks=new HashSet<Long>();var scenes=new ArrayList<Object>();int reads=0;
            for(var p:POINTS){
                int owned=0,closed=0,oldClosed=0,pine=0,litter=0,ferns=0;var heights=new ArrayList<Integer>();
                var roots=DryForestTrees.roots(SEED,p[0]-32,p[1]-32,p[0]+32,p[1]+32,d,g.climate(random),owner);
                require(!roots.isEmpty()&&roots.stream().allMatch(r->r.family()==DryForest.FAMILY&&!r.birch()&&r.height()>=8&&r.height()<=30),"Supported established broadleaf cohorts");
                for(int z=p[1]-16;z<=p[1]+16;z++)for(int x=p[0]-16;x<=p[0]+16;x++){
                    var chunk=level.getChunk(x>>4,z>>4);long ck=((long)(x>>4)<<32)|((z>>4)&0xffffffffL);
                    if(chunks.add(ck)){
                        var profile=((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain().base();var encoded=profile.encode(level.palettedContainerFactory());
                        require(blockbranch.cubic.TerrainProfile.decode(encoded,level.palettedContainerFactory()).encode(level.palettedContainerFactory()).equals(encoded),"Exact native profile");add(profileHash,ck+":"+encoded);
                    }
                    int h=d.sample(x,z).height();boolean newly=owner.test(x,z)&&!oldOwner.test(x,z);
                    if(newly){
                        owned++;int top=g.canopy(x,z,random).topY(x,z),oldTop=retained.canopy(x,z,random).topY(x,z);
                        if(top>=h+5){closed++;heights.add(top-h);}if(oldTop>=h+5)oldClosed++;
                    }
                    for(int y=h-3;y<=h+40;y++){
                        if(x==POINTS[0][0]+1&&z==POINTS[0][1]+1&&y==h-2)continue;
                        var state=level.getBlockState(new BlockPos(x,y,z));require(state.equals(g.surfaceState(x,y,z,random)),"Native/full state at "+x+","+y+","+z);reads++;add(nativeHash,x+","+y+","+z+":"+state);
                        if(newly){if(state.is(Blocks.SPRUCE_LOG))pine++;if(y==h+1){if(state.is(Blocks.LEAF_LITTER))litter++;if(state.is(Blocks.FERN)||state.is(Blocks.LARGE_FERN))ferns++;}}
                    }
                }
                require(owned>500&&closed>owned*.40,"Closed growing-season forest structure in the native core, not merely more roots");
                require(litter>0&&ferns<owned*.05,"Litter floor without humid-forest fern abundance");
                Collections.sort(heights);scenes.add(Map.of("center",p,"newlyOwnedColumns",owned,"canopyColumnsAtLeast5m",closed,"oldCanopyColumnsAtLeast5m",oldClosed,"canopyHeightsMinMedianMax",List.of(heights.getFirst(),heights.get(heights.size()/2),heights.getLast()),"spruceLogVoxels",pine,"litter",litter,"ferns",ferns,"supportedRoots",roots.size()));
            }
            String stateSignature=HexFormat.of().formatHex(nativeHash.digest()),profileSignature=HexFormat.of().formatHex(profileHash.digest());
            report.put("nativeStateSHA256",stateSignature);report.put("nativeProfileSHA256",profileSignature);report.put("nativeReads",reads);report.put("profileRoundtrips",chunks.size());report.put("scenes",scenes);
            if(reopen){var prior=JsonParser.parseString(Files.readString(result.resolveSibling("result.txt.json"))).getAsJsonObject();require(prior.get("nativeStateSHA256").getAsString().equals(stateSignature)&&prior.get("nativeProfileSHA256").getAsString().equals(profileSignature),"Saved native canopy/floor/profile signatures retained");var edge=prior.getAsJsonObject("boundaryAndBaseAPIs").getAsJsonArray("adjacentCoveredUncoveredXZ");int ax=edge.get(0).getAsInt(),az=edge.get(1).getAsInt();require(owner.test(ax,az)&&!oldOwner.test(ax,az),"Recorded native API edge exercises newly refined ownership");}
            else report.put("boundaryAndBaseAPIs",boundary(level,g,retained));
            report.put("savedClearing",clearing(level,g,retained,result.getParent(),reopen));
            int ex=POINTS[0][0]+1,ez=POINTS[0][1]+1;var edit=new BlockPos(ex,d.sample(ex,ez).height()-2,ez);
            if(reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Saved gold edit retained");else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            server.saveEverything(false,true,true);Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(result,"PASS seasonal forest native structure, APIs, profiles, saved plans and edit\n");
            Blockbranch.LOGGER.info("SEASONAL_FOREST_NATIVE_PASS {}",report);
        }catch(Throwable failure){Blockbranch.LOGGER.error("SEASONAL_FOREST_NATIVE_FAIL",failure);try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static Object selectors(HabitatPalette p){
        var rows=new ArrayList<Object>();
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);
            var before=source(p,seed,d,c,false);var after=source(p,seed,d,c,true);int changed=0,retained=0;var owners=new TreeMap<String,Integer>();int[] counts=new int[9],differences=new int[9];
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var a=before.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);var b=after.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);
                if(a==p.additional(DryForest.KEY)){require(a==b,"Existing dry-forest owner retained");retained++;}
                boolean allowed=a==p.land().warm()||a==p.additional("sparse_jungle");
                if(allowed){
                    var q=c.sample(x,z,d::sample);double annual=DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample));
                    boolean nominal=SeasonalForestExpansion.climateSuitable(z,q,DrySeasonPolicy.potential(x,z,d,c));
                    for(int i=0;i<9;i++){
                        double rain=annual*(i==1?.95:i==2?1.05:1),migration=DrySeasonPolicy.MIGRATION*(i==3?.95:i==4?1.05:1),width=DrySeasonPolicy.WIDTH*(i==5?.95:i==6?1.05:1),threshold=i==7?95:i==8?105:100;
                        boolean admitted=SeasonalForestExpansion.climateSuitable(z,q,DrySeasonPolicy.summarize(DrySeasonPolicy.monthly(z/2097152.,rain,migration,width,DrySeasonPolicy.BACKGROUND),threshold));
                        if(admitted)counts[i]++;if(admitted!=nominal)differences[i]++;
                    }
                }
                if(a==b)continue;
                require(allowed&&b==p.additional(DryForest.KEY)&&SeasonalForestExpansion.eligible(seed,x,z,d,c),"Only supported existing forest owners refine");changed++;owners.merge(a.unwrapKey().orElseThrow().identifier().toString(),1,Integer::sum);
            }
            require(changed>0&&changed==counts[0],"Nonvacuous exact nominal envelope");rows.add(Map.of("seed",seed,"samples",14400,"changed",changed,"priorOwners",owners,"retainedDryForest",retained,"sensitivityOrder","nominal,rain-.05,rain+.05,migration-.05,migration+.05,width-.05,width+.05,dry95,dry105","admitted",counts,"changedAdmission",differences));
        }
        return rows;
    }
    private static ScreeBiomeSource source(HabitatPalette p,long seed,DrainageNetwork d,ClimateField c,boolean seasonal){var s=new ScreeBiomeSource(p,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true).withSummerDryWoodlands(true).withWarmXeric(true).withSeasonalForests(seasonal);s.bind(seed,d,c);return s;}
    private static Map<String,Object> boundary(net.minecraft.server.level.ServerLevel level,RealisticGenerator g,RealisticGenerator retained){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var owner=g.dryForestOwner(random);
        int[] edge=null;int discoveryPoints=0;
        search:for(var center:POINTS)for(int z=center[1]-512;z<=center[1]+512;z+=16)for(int x=center[0]-512;x<=center[0]+512;x+=16){
            discoveryPoints++;if(!owner.test(x,z)||retained.dryForestOwner(random).test(x,z))continue;
            for(var direction:new int[][]{{16,0},{0,16},{-16,0},{0,-16}}){
                int bx=x+direction[0],bz=z+direction[1];
                if(Math.abs(bx-center[0])>512||Math.abs(bz-center[1])>512||owner.test(bx,bz))continue;
                int ax=x,az=z;
                while(Math.abs(ax-bx)+Math.abs(az-bz)>1){int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);if(owner.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
                if(d.sample(ax,az).waterLevel()!=DrainageNetwork.DRY||d.sample(bx,bz).waterLevel()!=DrainageNetwork.DRY)continue;
                edge=new int[]{ax,az,bx,bz};break search;
            }
        }
        require(edge!=null,"Seasonal-forest edge must occur inside two fixed ±512-block candidate neighborhoods; never expand search");
        require(owner.test(edge[0],edge[1])&&!retained.dryForestOwner(random).test(edge[0],edge[1])&&!owner.test(edge[2],edge[3]),"Adjacent covered/uncovered edge witness");
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
    private static Object clearing(net.minecraft.server.level.ServerLevel level,RealisticGenerator g,RealisticGenerator retained,Path dir,boolean reopen)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);
        Path saved=dir.resolve("seasonal-clear.nbt"),states=dir.resolve("seasonal-clear-states.tsv"),signature=dir.resolve("seasonal-clear-signature.txt");
        if(reopen){
            var piece=BorealClearSmoke.load(context,NbtIo.readCompressed(saved,NbtAccounter.unlimitedHeap()));
            var construction=piece.createTag(context).getIntArray("Construction").orElseThrow();require(g.dryForestOwner(random).test(construction[0],construction[2])&&!retained.dryForestOwner(random).test(construction[0],construction[2]),"Saved clearing root belongs to newly refined forest");
            var plan=BorealClearSmoke.plan(piece,g,random);
            require(Files.readString(signature).equals(new TreeMap<>(plan).toString()),"Saved clearing footprint reopens exactly");
            int checked=0;for(var line:Files.readAllLines(states)){var fields=line.split("\t",2);var pos=BlockPos.of(Long.parseLong(fields[0]));require(level.getBlockState(pos).toString().equals(fields[1]),"Native cleared blocks/player edit retained");checked++;}
            return Map.of("reopenedNativeStates",checked,"savedDefinition",piece.createTag(context).getIntOr("ClearDefinition",0));
        }
        var p=POINTS[0];var d=g.drainage(random);
        var root=DryForestTrees.roots(SEED,p[0]-160,p[1]-160,p[0]+160,p[1]+160,d,g.climate(random),g.dryForestOwner(random)).stream()
            .filter(r->r.height()>=18&&!retained.dryForestOwner(random).test(r.x(),r.z())&&Math.hypot(r.x()-p[0],r.z()-p[1])>96).findFirst().orElseThrow();
        var box=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());
        var piece=new ReimaginedTreeClearPiece(box,SEED,g.borealCrownDefinition(),g.vegetationFlags());var tag=piece.createTag(context);
        require(tag.getIntOr("ClearDefinition",0)==128,"Explicit complete refined root/crown flags");
        var plan=BorealClearSmoke.plan(piece,g,random);
        require(!plan.isEmpty()&&ForestTrees.tree(root).stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))),"Whole supported broadleaf footprint cleared");
        NbtIo.writeCompressed(tag,saved);Files.writeString(signature,new TreeMap<>(plan).toString());
        var restored=BorealClearSmoke.load(context,NbtIo.readCompressed(saved,NbtAccounter.unlimitedHeap()));require(plan.equals(BorealClearSmoke.plan(restored,g,random)),"Saved NBT exact clearing footprint");
        int historical=0;
        for(int flags=0;flags<32;flags++)for(int crown=0;crown<2;crown++){
            var oldPiece=new ReimaginedTreeClearPiece(box,SEED,crown,flags);var oldTag=oldPiece.createTag(context);
            require(oldTag.getIntOr("ClearDefinition",0)==1+crown+2*flags,"All64 old definition numbers retained");
            require(oldPiece.getBoundingBox().maxY()==box.maxY()+((flags&RedwoodForest.FLAG)!=0?144:80),"Old saved halo retained");
            require(BorealClearSmoke.load(context,oldTag).createTag(context).equals(oldTag),"Old NBT roundtrip");historical++;
        }
        int compared=0;
        for(int flags:new int[]{4,12,20,28})for(int crown=0;crown<2;crown++){
            var oldTag=new ReimaginedTreeClearPiece(box,SEED,crown,flags).createTag(context);
            var a=BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),retained,random);
            var b=BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),g,random);
            require(a.equals(b),"Old saved forest plans must not adopt refined roots");compared++;
        }
        var initial=new HashMap<Long,net.minecraft.world.level.block.state.BlockState>();
        for(long key:plan.keySet()){var pos=BlockPos.of(key);level.getChunk(pos.getX()>>4,pos.getZ()>>4);initial.put(key,level.getBlockState(pos));}
        long playerEdit=initial.entrySet().stream().filter(e->e.getValue().is(BlockTags.LOGS)).mapToLong(Map.Entry::getKey).findFirst().orElseThrow();
        initial.put(playerEdit,Blocks.GOLD_BLOCK.defaultBlockState());level.setBlock(BlockPos.of(playerEdit),Blocks.GOLD_BLOCK.defaultBlockState(),3);
        var forward=BorealClearSmoke.replay(level,g,context,tag,initial,false,true,dir);
        var reverse=BorealClearSmoke.replay(level,g,context,tag,initial,true,true,dir);
        require(forward.equals(reverse)&&forward.get(playerEdit).is(Blocks.GOLD_BLOCK),"Clipped replay order and non-tree player edit retained");
        var nativeResult=BorealClearSmoke.replay(level,g,context,tag,initial,false,true,dir,true);
        var text=new StringBuilder();for(long key:new TreeSet<>(plan.keySet())){var state=level.getBlockState(BlockPos.of(key));require(state.equals(nativeResult.get(key)),"Actual native replay matches virtual clipped replay");text.append(key).append('\t').append(state).append('\n');}
        Files.writeString(states,text.toString());
        return Map.of("newDefinition",128,"footprintCells",plan.size(),"historicalNBTRoundtrips",historical,"oldForestFootprintsCompared",compared,"nativeReplayStates",plan.size());
    }
    private static void reject(RegistryOps<JsonElement> ops,JsonObject json){require(RealisticGenerator.CODEC.codec().parse(ops,json).error().isPresent(),"Malformed/incomplete/future refinement rejects");}
    private static void add(MessageDigest digest,String text){digest.update((text+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SeasonalForestQualification(){}
}
