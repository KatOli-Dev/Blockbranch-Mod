package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.Blocks;

/** Finite native structural, selector, codec and cross-process saved-edit checks. */
public final class WarmXericQualification {
    private static final int[][] POINTS={{432000,-784000},{400000,-784000},{1328000,-496000}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.warmXericResult"));
        try{
            boolean reopen=Boolean.getBoolean("blockbranch.warmXericReopen");
            var level=server.overworld();var random=level.getChunkSource().randomState();
            var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var fresh=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
            require(fresh.get("shape_version").getAsInt()==268&&g.warmXericDefinition()==1,"Actual new-world saved gate");
            var parsed=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,parsed).getOrThrow().equals(fresh),"Exact fresh codec roundtrip");
            var old=fresh.deepCopy();old.remove("warm_xeric_definition");old.addProperty("shape_version",267);
            old.getAsJsonObject("additional_biomes").remove(WarmXericShrubland.KEY);
            var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(retained.warmXericDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent gate retains exact267");
            for(int schema:new int[]{264,265,266,267,269}){var bad=fresh.deepCopy();bad.addProperty("shape_version",schema);reject(ops,bad);}
            for(int definition:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("warm_xeric_definition",definition);reject(ops,bad);}
            for(String key:new String[]{"warm_xeric_definition","continental_ice_definition","leaf_litter_definition","summer_dry_definition","land_ice_definition"}){var bad=fresh.deepCopy();bad.remove(key);reject(ops,bad);}
            var bad=fresh.deepCopy();bad.addProperty("warm_xeric_definition","invalid");reject(ops,bad);
            bad=fresh.deepCopy();bad.getAsJsonObject("additional_biomes").remove(WarmXericShrubland.KEY);reject(ops,bad);
            var report=new LinkedHashMap<String,Object>();
            report.put("published264To267CodecCases",LeafLitterAudit.gateMatrix(ops,g));
            if(!reopen)report.put("selectors",selectors(HabitatPalette.CODEC.codec().parse(ops,old).getOrThrow(),HabitatPalette.CODEC.codec().parse(ops,fresh).getOrThrow()));
            var d=g.drainage(random);var owner=((ScreeBiomeSource)g.getBiomeSource()).warmXericSurface();
            int owned=0,shrubs=0,grasses=0,bare=0,different=0,states=0,profiles=0;
            var chunks=new HashSet<Long>();
            for(var p:POINTS)for(int z=p[1]-16;z<=p[1]+16;z++)for(int x=p[0]-16;x<=p[0]+16;x++){
                var chunk=level.getChunk(x>>4,z>>4);long key=((long)(x>>4)<<32)|((z>>4)&0xffffffffL);
                if(chunks.add(key)){
                    var profile=((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain().base();
                    var encoded=profile.encode(level.palettedContainerFactory());
                    require(blockbranch.cubic.TerrainProfile.decode(encoded,level.palettedContainerFactory()).encode(level.palettedContainerFactory()).equals(encoded),"Profile exact roundtrip");profiles++;
                }
                int h=d.sample(x,z).height();boolean domain=owner.test(x,z);if(domain)owned++;
                for(int y=h-3;y<=h+8;y++){
                    var state=level.getBlockState(new BlockPos(x,y,z));
                    // The persisted edit is checked independently below.
                    if(reopen&&x==POINTS[0][0]+1&&z==POINTS[0][1]+1&&y==h-2)continue;
                    require(state.equals(g.surfaceState(x,y,z,random)),"Native/analytic state "+x+","+y+","+z);states++;
                    if(domain&&y==h&&!state.equals(retained.surfaceState(x,y,z,random)))different++;
                    if(domain&&y==h+1){if(state.is(SteppeContent.SILVER_SHRUB))shrubs++;else if(state.is(Blocks.SHORT_DRY_GRASS))grasses++;else if(state.isAir())bare++;}
                }
            }
            require(owned>1000&&shrubs>owned*.10&&shrubs<owned*.30&&shrubs>grasses*3,"Native open shrub-dominant structure");
            require(bare>owned*.60&&different>owned*.30,"Open ground and material difference from prior dryland");
            if(!reopen)report.put("boundaryAndBaseAPIs",boundary(level,g));
            int ex=POINTS[0][0]+1,ez=POINTS[0][1]+1;var edit=new BlockPos(ex,d.sample(ex,ez).height()-2,ez);
            if(reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Saved edit survives relaunch");
            else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            report.put("ownedColumns",owned);report.put("shrubs",shrubs);report.put("shortGrass",grasses);report.put("bareAboveGround",bare);report.put("differentGroundFrom267",different);report.put("nativeStates",states);report.put("profileRoundtrips",profiles);
            server.saveEverything(false,true,true);
            Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            Files.writeString(result,"PASS warm xeric saved gate, native structure, profiles, APIs and saved edit\n");
            Blockbranch.LOGGER.info("WARM_XERIC_NATIVE_PASS {}",report);
        }catch(Throwable failure){Blockbranch.LOGGER.error("WARM_XERIC_NATIVE_FAIL",failure);try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static Object selectors(HabitatPalette oldPalette,HabitatPalette palette){
        var rows=new ArrayList<Object>();
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);
            var before=source(oldPalette,seed,d,c,false);var after=source(palette,seed,d,c,true);int count=0;
            int[] sensitivity=new int[5],changedAdmission=new int[5];
            int marineBoundComparisons=0;
            var priorOwners=new TreeMap<String,Integer>();
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var a=before.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);var b=after.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);
                require(after.surfaceResolver().getNoiseBiome(x>>2,-4096,z>>2)==b&&after.surfaceResolver().getNoiseBiome(x>>2,4096,z>>2)==b,"Cached surface answers remain height-independent");
                boolean priorDry=a==oldPalette.land().steppe()||a==oldPalette.land().desert()||a==oldPalette.additional("desert");
                boolean supported=false;
                if(priorDry){
                    supported=CoolDesert.physicalSupport(x,z,d);
                    require(supported==CoolDesert.physicalSupport(x,z,d,true),"Marine negative bound retains complete-support eligibility");marineBoundComparisons++;
                }
                if(supported){
                    double rain=DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample)),mean=c.sample(x,z,d::sample).meanTemperatureC();
                    boolean nominal=WarmXericShrubland.climateSuitable(rain,CoolDesert.season(z,mean).coldestC());
                    for(int i=0;i<5;i++){
                        double scale=new double[]{1,.9,1.1,1,1}[i],gradient=new double[]{16,16,16,14.4,17.6}[i];
                        boolean admitted=WarmXericShrubland.climateSuitable(rain*scale,CoolDesert.season(z,mean,gradient).coldestC());
                        if(admitted)sensitivity[i]++;if(admitted!=nominal)changedAdmission[i]++;
                    }
                }
                if(a==b)continue;
                require(b==palette.additional(WarmXericShrubland.KEY)&&WarmXericShrubland.eligible(x,z,d,c),"Only physically supported new subtype changes");
                require(a==oldPalette.land().steppe()||a==oldPalette.land().desert()||a==oldPalette.additional("desert"),"Only prior steppe or desert owners change; intentional/fallback plains preserved");
                priorOwners.merge(a.unwrapKey().orElseThrow().identifier().toString(),1,Integer::sum);count++;
            }
            require(count>0&&count==sensitivity[0],"Nonvacuous fixed-seed subtype and nominal control");
            rows.add(Map.of("seed",seed,"samples",14400,"changed",count,"priorOwners",priorOwners,
                "sensitivityOrder","nominal, rain scale -10%, rain scale +10%, thermal amplitude gradient -10%, gradient +10%",
                "sensitivityAdmitted",sensitivity,"sensitivityChangedAdmission",changedAdmission,"marineNegativeBoundComparisons",marineBoundComparisons));
        }
        return rows;
    }
    private static ScreeBiomeSource source(HabitatPalette p,long seed,DrainageNetwork d,ClimateField c,boolean enabled){
        var s=new ScreeBiomeSource(p,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true).withSummerDryWoodlands(true).withWarmXeric(enabled);s.bind(seed,d,c);return s;
    }
    private static Map<String,Object> boundary(net.minecraft.server.level.ServerLevel level,RealisticGenerator g){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var owner=((ScreeBiomeSource)g.getBiomeSource()).warmXericSurface();
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
        require(edge!=null,"Warm-xeric edge must occur inside two fixed ±512-block candidate neighborhoods; never expand search");
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
    private static void reject(RegistryOps<JsonElement> ops,JsonObject bad){require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Reject incomplete/malformed/future definition");}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private WarmXericQualification(){}
}
