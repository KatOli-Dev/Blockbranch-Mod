package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Actual dark deep-water columns, clipped maps, immutable snapshots and saved bed edits. */
public final class DeepOceanSmoke {
    public static BlockPos fixture(RealisticGenerator g,RandomState r){return fixture(g,r,false);}
    public static BlockPos fixture(RealisticGenerator g,RandomState r,boolean broad){var p=broad?blockbranch.realistic.TemperateDeepOceanFixtures.fixture(g.drainage(r),r.seed()):blockbranch.realistic.DeepOceanFixtures.fixture(g.drainage(r),r.seed());return new BlockPos(p.x(),g.drainage(r).sample(p.x(),p.z()).height(),p.z());}
    public static void restore(ServerLevel level,BlockPos bed){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=-1;dy<=2;dy++){var p=bed.above(dy);level.setBlock(p,base.state(p.getX(),p.getY(),p.getZ()),3);}}
    public static void verify(ServerLevel level,RealisticGenerator g,BlockPos bed,boolean reload){verify(level,g,bed,reload,false);}
    public static void verify(ServerLevel level,RealisticGenerator g,BlockPos bed,boolean reload,boolean broad){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var source=g.getBiomeSource().createUncachedResolver(r);var site=DeepOcean.site(d,bed.getX(),bed.getZ());
        level.getChunkAt(bed);var lightFence=level.getChunkSource().getLightEngine().waitForPendingTasks(bed.getX()>>4,bed.getZ()>>4);level.getServer().managedBlock(lightFence::isDone);lightFence.join();
        require(level.getBiome(bed).is(broad?net.minecraft.world.level.biome.Biomes.DEEP_OCEAN.identifier():g.coldOceansEnabled()&&DeepColdOcean.contains(bed.getX(),bed.getZ(),d,new ClimateField(r.seed()))?net.minecraft.world.level.biome.Biomes.DEEP_COLD_OCEAN.identifier():Blockbranch.id("deep_open_ocean_basin_bed"))&&DeepOcean.eligible(site),"Natural confirmed deep-ocean identity");
        require(level.getBrightness(LightLayer.SKY,bed.above())==0,"Actual deep floor has no photosynthetic skylight");int[] codes=new int[15];
        for(int cz=(bed.getZ()>>4)-1;cz<=(bed.getZ()>>4)+1;cz++)for(int cx=(bed.getX()>>4)-1;cx<=(bed.getX()>>4)+1;cx++){
            var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
            for(int oz=0;oz<16;oz+=8)for(int ox=0;ox<16;ox+=8){int x=cx*16+ox,z=cz*16+oz;var s=d.sample(x,z);int h=s.height(),water=s.waterLevel(),cover=g.riverMarginCover(x,z,r);codes[cover]++;
                require(cover==DeepOcean.cover(r.seed(),x,z,DeepOcean.contains(x,z,d),DeepOcean.site(d,x,z))&&cover>=5&&cover<=9,"Native exclusive deep substrate rule");
                for(int y=h-3;y<=water+1;y++){var expected=RealisticGenerator.material(y,h,water,0,0,cover);var p=new BlockPos(x,y,z);require(chunk.getBlockState(p)==expected&&decoded.state(x,y,z)==expected,"Native/encoded entire deep bed/water column agreement");require(sections.computeIfAbsent(y>>4,decoded::generate).getBlockState(x&15,y&15,z&15)==expected,"Client deep sections match definition");if(y>h&&y<water)require(expected.is(Blocks.WATER)&&expected.getFluidState().isSource(),"Every deep-water cell is source water with no vegetation");}
                for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-2,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(water-1,2),LevelHeightAccessor.create(water+1,2)}){var column=g.getBaseColumn(x,z,bounds,r);for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),water+1);y>=bounds.getMinY();y--)if(type.isOpaque().test(column.getBlock(y))){expected=y+1;break;}require(g.getBaseHeight(x,z,type,bounds,r)==expected,"Clipped deep bed/fluid query");if(bounds==level)require(chunk.getHeight(type,x&15,z&15)+1==expected&&base.top(type.isOpaque(),x,z,ColumnLimits.MAX_Y-1)==expected,"Native deep heightmaps distinguish bed and water");}}
                for(int qy:new int[]{-3000,h>>2,0,2000})require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(source.getNoiseBiome(x>>2,qy,z>>2))&&decoded.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(source.getNoiseBiome(x>>2,qy,z>>2)),"Deep all-altitude palette survives encoding");
            }
        }
        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();var base=terrain.base();int index=(bed.getZ()&15)*16+(bed.getX()&15);
        for(int code:new int[]{5,6,8,9}){var tag=base.encode(level.palettedContainerFactory());tag.getByteArray("river_margin").orElseThrow()[index]=(byte)code;var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());for(int y=bed.getY()-2;y<=bed.getY()+2;y++){var expected=RealisticGenerator.material(y,bed.getY(),site.water(),0,0,code);require(decoded.state(bed.getX(),y,bed.getZ())==expected&&decoded.generate(y>>4).getBlockState(bed.getX()&15,y&15,bed.getZ()&15)==expected,"Every deep material and original rock survives encoding/materialization");}}
        var original=base.encode(level.palettedContainerFactory()).getByteArray("river_margin").orElseThrow().clone();for(int invalid:new int[]{12,13}){var tag=base.encode(level.palettedContainerFactory());tag.getByteArray("river_margin").orElseThrow()[index]=(byte)invalid;boolean rejected=false;try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Deep photosynthetic or unknown palette state rejected");require(Arrays.equals(original,base.encode(level.palettedContainerFactory()).getByteArray("river_margin").orElseThrow()),"Malformed snapshot cannot mutate deep profile");}
        level.setBlock(bed,Blocks.WATER.defaultBlockState(),3);require(level.getHeight(Heightmap.Types.OCEAN_FLOOR,bed.getX(),bed.getZ())==bed.getY()&&level.getHeight(Heightmap.Types.WORLD_SURFACE,bed.getX(),bed.getZ())==site.water(),"Excavated flooded bed updates solid height while preserving water plane");restore(level,bed);
        var saved=bed.below(8);if(reload)require(level.getBlockState(saved).is(Blocks.CLAY),"Saved deep-bed edit survives reopen");level.setBlock(saved,Blocks.CLAY.defaultBlockState(),2);level.getChunkAt(bed).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(saved).is(Blocks.CLAY),"Deep edit survives section eviction");
        boundary(level,g,broad);Blockbranch.LOGGER.info("DEEP_OPEN_OCEAN_PASS {} broadVanilla={} bed={} water={} depth={} nearbyCodes={}",reload?"RELOAD":"NEW",broad,bed,site.water(),site.depth(),Arrays.toString(codes));
    }
    private static void boundary(ServerLevel level,RealisticGenerator g,boolean broad){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var edge=broad?blockbranch.realistic.TemperateDeepOceanFixtures.transition(d,r.seed()):blockbranch.realistic.DeepOceanFixtures.transition(d,r.seed());var source=g.getBiomeSource().createUncachedResolver(r);
        for(int z=Math.min(edge.insideZ(),edge.outsideZ())-8;z<=Math.max(edge.insideZ(),edge.outsideZ())+8;z++)for(int x=Math.min(edge.insideX(),edge.outsideX())-8;x<=Math.max(edge.insideX(),edge.outsideX())+8;x++){
            var s=d.sample(x,z);var chunk=level.getChunk(x>>4,z>>4);for(int y=s.height()-3;y<=s.height()+4;y++)require(chunk.getBlockState(new BlockPos(x,y,z))==g.surfaceState(x,y,z,r),"Native shelf/deep material seam");for(int y:new int[]{s.height()+5,(s.height()+s.waterLevel())/2,s.waterLevel()-1,s.waterLevel()})require(chunk.getBlockState(new BlockPos(x,y,z))==g.surfaceState(x,y,z,r),"Native boundary fluid column retained");require(chunk.getNoiseBiome(x>>2,0,z>>2).equals(source.getNoiseBiome(x>>2,0,z>>2)),"Native shelf/deep biome seam");
        }
        var climate=new ClimateField(r.seed());boolean broadShelf=TemperateOcean.contains(r.seed(),edge.insideX(),edge.insideZ(),d,climate),broadBasin=TemperateDeepOcean.contains(edge.outsideX(),edge.outsideZ(),d,climate);
        require(source.getNoiseBiome(edge.insideX()>>2,0,edge.insideZ()>>2).is(broadShelf?net.minecraft.world.level.biome.Biomes.OCEAN.identifier():g.coldOceansEnabled()&&ColdOcean.contains(r.seed(),edge.insideX(),edge.insideZ(),d,new ClimateField(r.seed()))?net.minecraft.world.level.biome.Biomes.COLD_OCEAN.identifier():Blockbranch.id("open_marine_shelf_coastal_sea"))&&source.getNoiseBiome(edge.outsideX()>>2,0,edge.outsideZ()>>2).is(broadBasin?net.minecraft.world.level.biome.Biomes.DEEP_OCEAN.identifier():g.coldOceansEnabled()&&DeepColdOcean.contains(edge.outsideX(),edge.outsideZ(),d,new ClimateField(r.seed()))?net.minecraft.world.level.biome.Biomes.DEEP_COLD_OCEAN.identifier():Blockbranch.id("deep_open_ocean_basin_bed")),"Canonical shelf/deep identities respect the explicit thermal partition");
        Blockbranch.LOGGER.info("DEEP_OCEAN_SHELF_BOUNDARY_PASS {} depths={}/{}",edge,MarineShelf.site(d,edge.insideX(),edge.insideZ()).depth(),DeepOcean.site(d,edge.outsideX(),edge.outsideZ()).depth());
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private DeepOceanSmoke(){}
}
