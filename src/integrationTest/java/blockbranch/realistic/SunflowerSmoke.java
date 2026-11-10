package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.phys.AABB;

/** Native sunflower pairs,section seams,clipped heights,vanilla interactions and saved snapshot isolation. */
public final class SunflowerSmoke {
    public static BlockPos fixture(RealisticGenerator g,RandomState r){var p=blockbranch.realistic.SunflowerPlainsFixtures.fixture(g.drainage(r),r.seed());return new BlockPos(p.x(),g.drainage(r).sample(p.x(),p.z()).height(),p.z());}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var bed=fixture(g,r);var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();var saved=bed.below(8);int x=bed.getX(),z=bed.getZ(),h=bed.getY();
        require(level.getBiome(bed).is(Biomes.SUNFLOWER_PLAINS),"Actual vanilla Sunflower Plains identity");
        if(Boolean.getBoolean("blockbranch.sunflowerFringeAudit")){
            require(!reload,"Fringe audit requires a fresh generated fixture, not legacy saved profiles");
            fringe(level,g,bed,r);
        }
        if(reload)require(level.getBlockState(saved).is(Blocks.GOLD_BLOCK)&&level.getBlockState(bed.above()).isAir()&&level.getBlockState(bed.above(2)).isAir(),"Saved complete flower harvest and marker survive reopen");
        PlainsSmoke.restore(level,bed);var copy=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
        for(int y=h-4;y<=h+4;y++){var expected=g.surfaceState(x,y,z,r);require(level.getBlockState(new BlockPos(x,y,z))==expected&&copy.state(x,y,z)==expected&&copy.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Native/snapshot/materialized flower equality");require(level.getFluidState(new BlockPos(x,y,z)).isEmpty(),"Dry flower fluid invariant");}
        for(var half:DoubleBlockHalf.values()){
            int stem=half==DoubleBlockHalf.LOWER?1:2;var pos=bed.above(stem);var state=level.getBlockState(pos);require(state.is(Blocks.SUNFLOWER)&&state.getValue(DoublePlantBlock.HALF)==half&&state.canSurvive(level,pos),"Both natural flower halves supported");
            require(copy.top(s->s.is(Blocks.SUNFLOWER)&&s.getValue(DoublePlantBlock.HALF)==half,x,z,h+3)==h+stem+1,"Half-specific predicate top");require(copy.view(pos.getY()>>4).maybeHas(s->s.is(Blocks.SUNFLOWER)&&s.getValue(DoublePlantBlock.HALF)==half)&&copy.boundaries().contains(pos.getY()>>4),"Both vertical sections advertise flower state/material boundary");
        }
        for(var bounds:List.of((LevelHeightAccessor)level,LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,1),LevelHeightAccessor.create(h+2,1),LevelHeightAccessor.create(h+3,2))){var col=g.getBaseColumn(x,z,bounds,r);for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),h+4);y>=bounds.getMinY();y--)if(type.isOpaque().test(col.getBlock(y))){expected=y+1;break;}require(g.getBaseHeight(x,z,type,bounds,r)==expected,"Flower clipped heightmap "+type);if(bounds==level)require(chunk.getHeight(type,x&15,z&15)+1==expected,"Flower native heightmap "+type);}}
        var resolver=g.getBiomeSource().createUncachedResolver(r);
        for(int qy:new int[]{-3000,0,h>>2,2000}){
            var expected=resolver.getNoiseBiome(x>>2,qy,z>>2);
            require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(expected),"All-altitude native/source flower identity quartY="+qy);
            // The raw surface palette excludes saved cave/dripstone biome layers.
            require(copy.biome((x&15)>>2,qy,(z&15)>>2).equals(expected),"All-altitude decoded/source flower identity quartY="+qy);
        }
        var drops=Block.getDrops(level.getBlockState(bed.above()),level,bed.above(),null,null,ItemStack.EMPTY);require(drops.size()==1&&drops.getFirst().is(Items.SUNFLOWER)&&drops.getFirst().getCount()==1,"Vanilla sunflower harvest item");
        var flower=(TallFlowerBlock)Blocks.SUNFLOWER;require(flower.isValidBonemealTarget(level,bed.above(),level.getBlockState(bed.above()),BonemealSource.INTERACTION),"Vanilla flower accepts bonemeal");flower.performBonemeal(level,level.getRandom(),bed.above(),level.getBlockState(bed.above()),BonemealSource.INTERACTION);require(level.getBlockState(bed.above()).is(Blocks.SUNFLOWER)&&level.getBlockState(bed.above(2)).is(Blocks.SUNFLOWER),"Native bonemeal retains paired plant;item visibility is asserted with active client");
        level.destroyBlock(bed.above(2),false);require(level.getBlockState(bed.above()).isAir()&&level.getBlockState(bed.above(2)).isAir(),"Upper harvest removes entire paired flower");require(copy.state(x,h+1,z).is(Blocks.SUNFLOWER)&&copy.state(x,h+2,z).is(Blocks.SUNFLOWER),"Live edits cannot mutate frozen flower snapshot");
        PlainsSmoke.restore(level,bed);level.setBlock(bed,Blocks.STONE.defaultBlockState(),3);require(level.getBlockState(bed.above()).isAir()&&level.getBlockState(bed.above(2)).isAir(),"Removing soil removes both flower halves");PlainsSmoke.restore(level,bed);
        var old=base.encode(level.palettedContainerFactory());old.putInt("version",21);boolean rejected=false;try{TerrainProfile.decode(old,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Pre-sunflower profile explicitly rejected");
        level.setBlock(saved,Blocks.GOLD_BLOCK.defaultBlockState(),2);level.destroyBlock(bed.above(),false);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(saved).is(Blocks.GOLD_BLOCK)&&level.getBlockState(bed.above(2)).isAir(),"Saved marker/flower removal survive eviction");
        Blockbranch.LOGGER.info("SUNFLOWER_PLAINS_NATIVE_PASS {} bed={} pairedStates=true snapshotIsolated=true",reload?"RELOAD":"NEW",bed);
    }
    /** Fresh-only witness selected by the new generator but absent from the shipped core selector. */
    private static void fringe(ServerLevel level,RealisticGenerator g,BlockPos bed,RandomState random){
        long seed=random.seed();var drainage=g.drainage(random);var climate=new ClimateField(seed);
        var ecology=new GroundEcology.Field(drainage::sample,climate);
        for(int dz=-64;dz<=64;dz++)for(int dx=-64;dx<=64;dx++){
            int x=bed.getX()+dx,z=bed.getZ()+dz;
            if(!SunflowerPlains.contains(seed,x,z,drainage,climate))continue;
            var site=ecology.sample(x,z);
            double core=GroundEcology.colonyStrength(seed,0x53554E464C4F5743L,x,z,40,10,23,site);
            double soil=Math.clamp(.5+site.wetness()+.25*site.retention()-.35*site.exposure(),.15,1);
            long hash=OakBirchWoodland.mix(seed^0x53554E504C414E54L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
            if((hash>>>11)*0x1.0p-53<.65*core*soil)continue;
            int h=drainage.sample(x,z).height();var ground=new BlockPos(x,h,z);
            if(!g.surfaceState(x,h+1,z,random).is(Blocks.SUNFLOWER))continue;
            var chunk=level.getChunkAt(ground);
            if(!level.getBlockState(ground.above()).is(Blocks.SUNFLOWER))continue;
            var profile=((SparseColumn)chunk).blockbranch$terrain().base();
            var copy=TerrainProfile.decode(profile.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(level.getBiome(ground).is(Biomes.SUNFLOWER_PLAINS)&&level.getBlockState(ground).is(Blocks.GRASS_BLOCK),"Fresh fringe has actual sunflower habitat and real grass support");
            for(int stem=1;stem<=2;stem++){
                var at=ground.above(stem);var expected=stem==1?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER;var actual=level.getBlockState(at);
                require(actual.is(Blocks.SUNFLOWER)&&actual.getValue(DoublePlantBlock.HALF)==expected&&actual.canSurvive(level,at),"Fresh fringe pair has correct supported native half");
                require(copy.state(x,h+stem,z)==actual&&copy.generate((h+stem)>>4).getBlockState(x&15,(h+stem)&15,z&15)==actual,"Fresh fringe native/saved/materialized states agree");
                require(profile.view((h+stem)>>4).maybeHas(state->state.is(Blocks.SUNFLOWER)&&state.getValue(DoublePlantBlock.HALF)==expected)&&profile.boundaries().contains((h+stem)>>4),"Fresh fringe sections advertise each half");
            }
            require(copy.top(state->state.is(Blocks.SUNFLOWER)&&state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER,x,z,h+3)==h+3,"Fresh fringe predicate height includes upper half");
            Blockbranch.LOGGER.info("SUNFLOWER_FRESH_FRINGE_NATIVE_PASS bed={} originalCore=false pairedNativeSavedMaterialized=true",ground);
            return;
        }
        throw new AssertionError("No actual new fringe flower found within the bounded fresh native fixture");
    }

    /** Inactive startup chunks are HIDDEN to vanilla entity queries; retain exact item assertion in an active player view. */
    public static void activeBonemeal(ServerLevel level,BlockPos bed){
        require(VerticalRuntime.active(level,bed.getX(),bed.getY()+1,bed.getZ()),"Real player view activates flower surface");
        int before=items(level,bed);((TallFlowerBlock)Blocks.SUNFLOWER).performBonemeal(level,level.getRandom(),bed.above(),level.getBlockState(bed.above()),BonemealSource.INTERACTION);
        int after=items(level,bed);require(after==before+1&&level.getBlockState(bed.above()).is(Blocks.SUNFLOWER)&&level.getBlockState(bed.above(2)).is(Blocks.SUNFLOWER),"Active native bonemeal produces exactly one visible flower item and retains both halves;before="+before+" after="+after);
        Blockbranch.LOGGER.info("SUNFLOWER_ACTIVE_BONEMEAL_EXACT_ITEM_PASS bed={} before={} after={}",bed,before,after);
    }
    private static int items(ServerLevel level,BlockPos bed){return level.getEntitiesOfClass(ItemEntity.class,new AABB(bed.above()).inflate(4)).stream().filter(e->e.getItem().is(Items.SUNFLOWER)).mapToInt(e->e.getItem().getCount()).sum();}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SunflowerSmoke(){}
}
