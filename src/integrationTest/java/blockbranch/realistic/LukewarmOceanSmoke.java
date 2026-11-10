package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native warm marine identity, photic grass, aphotic sediment, source water and edited saves. */
public final class LukewarmOceanSmoke {
    public static boolean cold(){return Boolean.getBoolean("blockbranch.coldOceanClientOnly")||"cold_ocean".equals(System.getProperty("blockbranch.focusHabitat"));}
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());

    public static BlockPos fixture(RealisticGenerator g,RandomState r,boolean deep) {
        var d=g.drainage(r);
        synchronized(FIXTURES) {
            var pair=FIXTURES.computeIfAbsent(d,unused->new BlockPos[2]);int index=deep?1:0;
            if(pair[index]==null) {
                if(deep){var p=cold()?blockbranch.realistic.DeepColdOceanFixtures.fixture(d,r.seed()):blockbranch.realistic.DeepLukewarmOceanFixtures.fixture(d,r.seed());pair[index]=new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());}
                else{
                    String requested=System.getProperty("blockbranch.lukewarmCapturePoint");
                    var raw=cold()?blockbranch.realistic.ColdOceanFixtures.fixture(d,r.seed()):null; var p=raw==null?blockbranch.realistic.LukewarmOceanFixtures.fixture(d,r.seed()):new LukewarmOcean.Point(raw.x(),raw.z());
                    if(requested!=null){
                        String[] coordinates=requested.split(",",-1);require(coordinates.length==2,"Capture point must be x,z");
                        p=new LukewarmOcean.Point(Integer.parseInt(coordinates[0]),Integer.parseInt(coordinates[1]));
                        var climate=new ClimateField(r.seed());
                        require(LukewarmOcean.contains(r.seed(),p.x(),p.z(),d,climate)&&LukewarmOcean.cover(r.seed(),p.x(),p.z(),d,climate)==12,"Requested capture point must retain natural warm seagrass");
                    }
                    pair[index]=new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());
                }
            }
            return pair[index];
        }
    }

    public static void restore(ServerLevel level,BlockPos bed) {
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        for(int dy=0;dy<=2;dy++){var p=bed.above(dy);level.setBlock(p,base.state(p.getX(),p.getY(),p.getZ()),3);}
    }

    public static void removeSupport(ServerLevel level,BlockPos bed) {
        level.setBlock(bed,Blocks.WATER.defaultBlockState(),3);
    }

    public static double[] camera(RealisticGenerator g,RandomState r,BlockPos bed,boolean deep,boolean second) {
        // Both shallow views are submerged and unaided. Deep view one records
        // natural darkness; view two is explicitly a night-vision diagnostic.
        var view=LukewarmOceanViews.at(bed.getX(),bed.getZ(),second);int x=view.x(),z=view.z();
        int h=g.drainage(r).sample(x,z).height();double y=h+view.aboveBed();
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=2;dy<=3;dy++)
            require(g.surfaceState(x+dx,h+dy,z+dz,r).is(Blocks.WATER),"Unobstructed native underwater lens");
        return new double[]{x+.5,y,z+.5,view.yaw(),view.pitch()};
    }

    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload) {
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());
        for(boolean deep:new boolean[]{false,true}) {
            var bed=fixture(g,r,deep);var saved=bed.below(8);
            if(reload){require(level.getBlockState(saved).is(Blocks.CLAY),"Saved warm marine bed edit");if(!deep)require(level.getBlockState(bed.above()).is(Blocks.TALL_SEAGRASS)&&level.getBlockState(bed.above(2)).is(Blocks.TALL_SEAGRASS),"Saved native tall grass tuple");}
            restore(level,bed);var light=level.getChunkSource().getLightEngine().waitForPendingTasks(bed.getX()>>4,bed.getZ()>>4);level.getServer().managedBlock(light::isDone);light.join();
            require(level.getBiome(bed).is(cold()?(deep?Biomes.DEEP_COLD_OCEAN:Biomes.COLD_OCEAN):(deep?Biomes.DEEP_LUKEWARM_OCEAN:Biomes.LUKEWARM_OCEAN)),"Actual vanilla warm marine identity");
            require(deep?level.getBrightness(LightLayer.SKY,bed.above())==0:level.getBrightness(LightLayer.SKY,bed.above())>=5,"Actual aphotic/photic native light");
            int plants=0;var codes=new TreeSet<Integer>();var resolver=g.getBiomeSource().createUncachedResolver(r);
            for(int cz=(bed.getZ()>>4)-1;cz<=(bed.getZ()>>4)+1;cz++)for(int cx=(bed.getX()>>4)-1;cx<=(bed.getX()>>4)+1;cx++) {
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
                var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
                if(deep)require(base.canopy().encode().length==0,"Aphotic floor has no photosynthetic canopy");
                for(int oz=0;oz<16;oz+=4)for(int ox=0;ox<16;ox+=4) {
                    int x=cx*16+ox,z=cz*16+oz;var sample=d.sample(x,z);int h=sample.height(),water=sample.waterLevel(),cover=g.riverMarginCover(x,z,r);codes.add(cover);
                    require(cover==(deep?DeepOcean.cover(r.seed(),x,z,true,DeepOcean.site(d,x,z)):(cold()?ColdOcean.cover(r.seed(),x,z,d,c):LukewarmOcean.cover(r.seed(),x,z,d,c))),"Exact shared warm marine material rule");
                    for(int y:new int[]{h-3,h-2,h-1,h,h+1,h+2,h+8,(h+water)/2,water-1,water,water+1}) {
                        var p=new BlockPos(x,y,z);var expected=g.surfaceState(x,y,z,r);
                        require(chunk.getBlockState(p)==expected&&copy.state(x,y,z)==expected&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==expected,"Native/encoded/client warm marine column agreement");
                        if(y>h&&y<water)require(expected.getFluidState().isSource(),"Unchanged ocean source water");
                    }
                    if(level.getBlockState(new BlockPos(x,h+1,z)).is(Blocks.SEAGRASS)){plants++;require(!deep&&MarineShelf.plantSuitable(MarineShelf.site(d,x,z),c.sample(x,z,d::sample)),"Photic native grass domain");require(level.getBlockState(new BlockPos(x,h+1,z)).canSurvive(level,new BlockPos(x,h+1,z)),"Supported native aquatic grass");}
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,ox,oz)+1==g.getBaseHeight(x,z,type,level,r),"All native warm marine heightmaps");
                    for(int qy:new int[]{-3000,h>>2,0,2000})require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))&&copy.biomes().get(ox>>2,qy&3,oz>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"Warm marine all-altitude stored/source/client identity");
                }
            }
            if(deep){require(plants==0&&codes.stream().allMatch(v->v>=5&&v<=9&&v!=7),"Deep sediment without plants");for(int y=bed.getY()+1;y<TerrainShape.SEA_LEVEL;y++)require(level.getFluidState(new BlockPos(bed.getX(),y,bed.getZ())).isSource(),"Entire actual deep water column remains source water");}
            else {
                require(plants>10&&level.getBlockState(bed.above()).is(Blocks.SEAGRASS),"Natural seagrass meadow");
                var grass=level.getBlockState(bed.above());var drops=Block.getDrops(grass,level,bed.above(),null,null,new ItemStack(Items.SHEARS));require(drops.stream().anyMatch(s->s.is(Items.SEAGRASS)),"Native shears recover seagrass");
                require(Block.getDrops(grass,level,bed.above(),null,null,ItemStack.EMPTY).isEmpty(),"Bare-hand harvest does not duplicate seagrass");
                removeSupport(level,bed);require(level.getBlockState(bed.above()).is(Blocks.WATER)&&level.getFluidState(bed.above()).isSource(),"Unsupported grass restores source water");restore(level,bed);
            }
            for(boolean second:new boolean[]{false,true})Blockbranch.LOGGER.info("LUKEWARM_REVIEW_CAMERA deep={} second={} view={}",deep,second,Arrays.toString(camera(g,r,bed,deep,second)));
            if(!deep){var pos=bed.above();var grass=level.getBlockState(pos);var plant=(BonemealableBlock)Blocks.SEAGRASS;require(plant.isValidBonemealTarget(level,pos,grass,BonemealSource.INTERACTION),"Photic grass has growth room");plant.performBonemeal(level,level.getRandom(),pos,grass,BonemealSource.INTERACTION);require(level.getBlockState(pos).is(Blocks.TALL_SEAGRASS)&&level.getBlockState(pos.above()).is(Blocks.TALL_SEAGRASS)&&level.getFluidState(pos).isSource()&&level.getFluidState(pos.above()).isSource(),"Native growth preserves waterlogged lower/upper tuple");}
            level.setBlock(saved,Blocks.CLAY.defaultBlockState(),2);level.getChunkAt(bed).markUnsaved();var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(saved).is(Blocks.CLAY),"Warm marine edit survives eviction");
            Blockbranch.LOGGER.info("LUKEWARM_MARINE_NATIVE_PASS deep={} mode={} bed={} depth={} plants={} codes={}",deep,reload?"RELOAD":"NEW",bed,TerrainShape.SEA_LEVEL-bed.getY()-1,plants,codes);
        }
        for(var edge:(cold()?List.of(DeepColdOcean.shelfTransition(d,r.seed()),DeepColdOcean.thermalTransition(d,r.seed(),false)):List.of(blockbranch.realistic.DeepLukewarmOceanFixtures.shelfTransition(d,r.seed()),DeepLukewarmOcean.thermalTransition(d,r.seed(),false),DeepLukewarmOcean.thermalTransition(d,r.seed(),true)))) {
            for(int[] p:new int[][]{{edge.insideX(),edge.insideZ()},{edge.outsideX(),edge.outsideZ()}}) {
                var chunk=level.getChunk(p[0]>>4,p[1]>>4);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());int h=d.sample(p[0],p[1]).height();
                for(int dy=-2;dy<=2;dy++)require(level.getBlockState(new BlockPos(p[0],h+dy,p[1]))==g.surfaceState(p[0],h+dy,p[1],r),"Native thermal/depth material seam");
                var expected=g.getBiomeSource().createUncachedResolver(r).getNoiseBiome(p[0]>>2,0,p[1]>>2);for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(p[0]>>2,y,p[1]>>2).equals(expected)&&copy.biomes().get((p[0]&15)>>2,y&3,(p[1]&15)>>2).equals(expected),"Stored/source/decoded thermal and 200m depth edge");
            }
            Blockbranch.LOGGER.info("LUKEWARM_MARINE_BOUNDARY_PASS {}",edge);
        }
    }

    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private LukewarmOceanSmoke(){}
}
