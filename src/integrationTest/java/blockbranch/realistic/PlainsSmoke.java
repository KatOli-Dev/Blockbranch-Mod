package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.*;

/** Native paired grass states across section boundaries, clipping, interactions and isolated saved snapshots. */
public final class PlainsSmoke {
    public static BlockPos fixture(RealisticGenerator g,RandomState r){var p=blockbranch.realistic.PlainsFixtures.fixture(g.drainage(r),r.seed());return new BlockPos(p.x(),g.drainage(r).sample(p.x(),p.z()).height(),p.z());}
    public static void restore(ServerLevel level,BlockPos bed){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=0;dy<=2;dy++)level.setBlock(bed.above(dy),base.state(bed.getX(),bed.getY()+dy,bed.getZ()),2);}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var bed=fixture(g,r);var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var saved=bed.below(8);
        require(level.getBiome(bed).is(Biomes.PLAINS),"Actual vanilla Plains identity");
        if(reload){require(level.getBlockState(bed.above()).isAir()&&level.getBlockState(bed.above(2)).isAir(),"Saved complete tall-grass harvest persists");require(level.getBlockState(saved).is(Blocks.GOLD_BLOCK),"Saved Plains edit persists");}
        restore(level,bed);require(level.getBlockState(bed.above()).is(Blocks.TALL_GRASS)&&level.getBlockState(bed.above(2)).getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER,"Natural two-block grass across section seam");
        var witnesses=new BlockPos[10];int missing=3;
        for(int z=bed.getZ()-96;z<=bed.getZ()+96&&missing>0;z++)for(int x=bed.getX()-96;x<=bed.getX()+96&&missing>0;x++){int code=g.meadowCover(x,z,r);if(code>=7&&code<=9&&witnesses[code]==null){witnesses[code]=new BlockPos(x,g.drainage(r).sample(x,z).height(),z);missing--;}}
        require(missing==0,"Natural bare,short and tall grass witnesses");var resolver=g.getBiomeSource().createUncachedResolver(r);
        for(int code=7;code<=9;code++){
            var p=witnesses[code];int x=p.getX(),z=p.getZ(),h=p.getY();var nativeChunk=level.getChunkAt(p);var base=((SparseColumn)nativeChunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            for(int y=h-4;y<=h+4;y++){var expected=g.surfaceState(x,y,z,r);var pos=new BlockPos(x,y,z);require(nativeChunk.getBlockState(pos)==expected&&copy.state(x,y,z)==expected&&copy.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Paired plant native/snapshot/materialized profile equality");require(level.getFluidState(pos).isEmpty(),"Dry grassland fluid invariant");}
            require(base.top(s->s.is(Blocks.DIRT),x,z,h)==h&&base.top(s->s.is(Blocks.STONE),x,z,h)==h-2,"Three-metre soil over unchanged bedrock");
            for(int stem=1;stem<=TerrainProfile.meadowPlantHeight(code);stem++)require(level.getBlockState(p.above(stem)).canSurvive(level,p.above(stem)),"Both plant halves have real support");
            for(var bounds:List.of((LevelHeightAccessor)level,LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,1),LevelHeightAccessor.create(h+2,1),LevelHeightAccessor.create(h+3,2))){var col=g.getBaseColumn(x,z,bounds,r);for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),h+4);y>=bounds.getMinY();y--)if(type.isOpaque().test(col.getBlock(y))){expected=y+1;break;}require(g.getBaseHeight(x,z,type,bounds,r)==expected,"Clipped upper/lower grass heightmap "+type);if(bounds==level)require(nativeChunk.getHeight(type,x&15,z&15)+1==expected,"Native paired-plant heightmap "+type);}}
            if(code==9)for(var half:DoubleBlockHalf.values()){int y=h+(half==DoubleBlockHalf.LOWER?1:2);require(copy.top(s->s.is(Blocks.TALL_GRASS)&&s.getValue(DoublePlantBlock.HALF)==half,x,z,h+3)==y+1,"State-specific upper/lower predicate height");require(copy.view(y>>4).maybeHas(s->s.is(Blocks.TALL_GRASS)&&s.getValue(DoublePlantBlock.HALF)==half),"Procedural section advertises both plant states");require(copy.boundaries().contains(y>>4),"Both plant sections are material boundaries");}
            for(int qy:new int[]{-3000,0,h>>2,2000})require(nativeChunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))&&copy.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"Native/source/saved all-altitude identity");
            tag.getByteArray("meadow").orElseThrow()[(z&15)*16+(x&15)]=14;boolean rejected=false;try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Unsupported meadow code rejected");
        }
        var snapshot=TerrainProfile.decode(terrain.base().encode(level.palettedContainerFactory()),level.palettedContainerFactory());
        level.destroyBlock(bed.above(2),false);require(level.getBlockState(bed.above()).isAir()&&level.getBlockState(bed.above(2)).isAir(),"Removing upper half removes lower");require(snapshot.state(bed.getX(),bed.getY()+1,bed.getZ()).is(Blocks.TALL_GRASS)&&snapshot.state(bed.getX(),bed.getY()+2,bed.getZ()).is(Blocks.TALL_GRASS),"Live edits cannot mutate isolated snapshot");
        restore(level,bed);level.setBlock(bed,Blocks.STONE.defaultBlockState(),3);require(level.getBlockState(bed.above()).isAir()&&level.getBlockState(bed.above(2)).isAir(),"Removing suitable soil removes both grass halves");restore(level,bed);
        var drops=Block.getDrops(level.getBlockState(bed.above()),level,bed.above(),null,null,new ItemStack(Items.SHEARS));require(drops.stream().mapToInt(ItemStack::getCount).sum()==2&&drops.stream().allMatch(s->s.is(Items.SHORT_GRASS)),"Native tall-grass shears yield two short grass");
        level.setBlock(bed.above(2),Blocks.AIR.defaultBlockState(),2);level.setBlock(bed.above(),Blocks.SHORT_GRASS.defaultBlockState(),2);var shortGrass=(TallGrassBlock)Blocks.SHORT_GRASS;require(shortGrass.isValidBonemealTarget(level,bed.above(),level.getBlockState(bed.above()),BonemealSource.INTERACTION),"Native short grass accepts bonemeal");shortGrass.performBonemeal(level,level.getRandom(),bed.above(),level.getBlockState(bed.above()),BonemealSource.INTERACTION);require(level.getBlockState(bed.above(2)).is(Blocks.TALL_GRASS),"Native bonemeal creates upper half");
        var old=terrain.base().encode(level.palettedContainerFactory());old.putInt("version",20);boolean rejected=false;try{TerrainProfile.decode(old,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Pre-double-plant profile explicitly rejected");
        level.setBlock(saved,Blocks.GOLD_BLOCK.defaultBlockState(),2);level.destroyBlock(bed.above(),false);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(saved).is(Blocks.GOLD_BLOCK)&&level.getBlockState(bed.above(2)).isAir(),"Grassland edits survive eviction");
        Blockbranch.LOGGER.info("PLAINS_NATIVE_PASS {} bed={} covers=7..9 pairedStates=true snapshotIsolated=true",reload?"RELOAD":"NEW",bed);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private PlainsSmoke(){}
}
