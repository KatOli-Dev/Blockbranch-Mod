package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Steppe structure, transitions, shrub use/drop/state behavior and saved crown regression. */
public final class SteppeSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var p=blockbranch.realistic.DrySteppeFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static void restore(ServerLevel level,BlockPos bed) {
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        for(int dy=0;dy<=1;dy++) level.setBlock(bed.above(dy),base.state(bed.getX(),bed.getY()+dy,bed.getZ()),3);
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        var random=level.getChunkSource().randomState();var resolver=generator.getBiomeSource().createUncachedResolver(random);
        var shrub=SteppeContent.SILVER_SHRUB;var mature=shrub.defaultBlockState().setValue(SteppeShrubBlock.CLIPPED,false);
        require(level.getBiome(bed).is(Blockbranch.id("dry_continental_scrub_steppe")),"Actual steppe identity");
        require(level.getBlockState(bed).is(Blocks.COARSE_DIRT) && level.getBlockState(bed.above())==mature,"Natural live shrub in stony soil");
        var witnesses=new BlockPos[7];
        for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++) for(int x=bed.getX()-64;x<=bed.getX()+64;x++) {
            int c=generator.steppeCover(x,z,random);if(c>0 && witnesses[c]==null) witnesses[c]=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
        }
        for(int cover=1;cover<=6;cover++) {
            var pos=witnesses[cover];require(pos!=null,"Natural soil/sward/shrub component "+cover);
            int x=pos.getX(),z=pos.getZ(),h=pos.getY();var chunk=level.getChunkAt(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(base.canopy().size()==0,"Dry climate remains treeless, without replacing prior woodland");
            for(int y=h-4;y<=h+2;y++) {
                var expected=RealisticGenerator.material(y,h,DrainageNetwork.DRY,0,0,0,0,0,0,cover);
                require(chunk.getBlockState(new BlockPos(x,y,z))==expected && decoded.state(x,y,z)==expected,"Native/procedural/decoded steppe structure "+cover);
                require(decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Materialized client steppe definition");
            }
            require(base.top(s->s.is(Blocks.STONE),x,z,h)==h-2,"Bounded three metre root zone over unchanged rock");
            require(base.top(s->s.is(Blocks.DIRT),x,z,h)==h,"Fine soil below surface");
            require(base.top(s->s.is(Blocks.GRAVEL),x,z,h)==(cover==2 ? h+1 : h-1),"Gravel lag versus buried stony layer");
            if(cover==3 || cover==4 || cover==6) require(level.getBlockState(pos.above()).canSurvive(level,pos.above()),"Every natural plant has suitable dry support");
            for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-3,2),LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+4,2)}) {
                var column=generator.getBaseColumn(x,z,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),h+2);y>=bounds.getMinY();y--) if(type.isOpaque().test(column.getBlock(y))) { expected=y+1;break; }
                    require(generator.getBaseHeight(x,z,type,bounds,random)==expected,"Clipped steppe heightmap "+type);
                    if(bounds==level) require(chunk.getHeight(type,x&15,z&15)+1==expected,"Native soil/vegetation heightmap "+type);
                }
            }
            for(int qy:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))
                && decoded.biome((x&15)>>2,qy,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"All-altitude biome palette round trip");
        }
        boundary(level,generator);
        var shears=new ItemStack(Items.SHEARS);var drops=Block.getDrops(mature,level,bed.above(),null,null,shears);
        require(drops.size()==1 && drops.getFirst().is(SteppeContent.SHRUB_ITEM),"Shears retain a live planting crown");
        drops=Block.getDrops(mature,level,bed.above(),null,null,ItemStack.EMPTY);
        require(drops.size()==1 && drops.getFirst().is(Items.STICK),"Mature woody shrub gives ordinary useful stick");
        require(shrub.defaultBlockState().getValue(SteppeShrubBlock.CLIPPED),"Transplants start clipped");
        require(shrub.clip(level,bed.above(),mature),"Clip mature shrub");var clipped=level.getBlockState(bed.above());
        require(clipped.is(shrub) && clipped.getValue(SteppeShrubBlock.CLIPPED) && !shrub.clip(level,bed.above(),clipped),"Retained crown cannot be clipped repeatedly");
        require(clipped.getShape(level,bed.above()).max(Direction.Axis.Y)<=0.25 && mature.getShape(level,bed.above()).max(Direction.Axis.Y)>0.8,"Clipped versus mature geometry");
        require(Block.getDrops(clipped,level,bed.above(),null,null,ItemStack.EMPTY).isEmpty(),"No free wood from clipped crown");
        drops=Block.getDrops(clipped,level,bed.above(),null,null,shears);require(drops.size()==1 && drops.getFirst().is(SteppeContent.SHRUB_ITEM),"Clipped crown remains transplantable");
        require(!clipped.isRandomlyTicking() && shrub.isValidBonemealTarget(level,bed.above(),clipped,BonemealSource.INTERACTION),"Deliberate regrowth without world scans");
        shrub.performBonemeal(level,level.getRandom(),bed.above(),clipped,BonemealSource.INTERACTION);
        require(level.getBlockState(bed.above())==mature && !shrub.isValidBonemealTarget(level,bed.above(),mature,BonemealSource.INTERACTION),"Regrowth stops at one mature shrub");
        for(var soil:List.of(Blocks.STONE,Blocks.SAND,Blocks.MUD)) {
            level.setBlock(bed,soil.defaultBlockState(),3);require(level.getBlockState(bed.above()).isAir() && !clipped.canSurvive(level,bed.above()),"Unsuitable soil removes shrub");restore(level,bed);
        }
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putInt("version",7);boolean rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }require(rejected,"Pre-steppe profile rejected");
        invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putByteArray("steppe",new byte[255]);rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }require(rejected,"Truncated steppe array rejected");
        var saved=bed.below(8);
        if(reload) require(level.getBlockState(saved).is(shrub) && level.getBlockState(saved).getValue(SteppeShrubBlock.CLIPPED),"Saved clipped shrub survives reopen");
        level.setBlock(saved.below(),Blocks.COARSE_DIRT.defaultBlockState(),2);level.setBlock(saved,clipped,3);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(saved)==clipped,"Clipped property survives section eviction");
        Blockbranch.LOGGER.info("DRY_CONTINENTAL_SCRUB_STEPPE_PASS {} bed={} components=bare-soil,gravel,dry-grass,live-shrub,sward,dead-stems",reload ? "RELOAD" : "NEW",bed);
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator) {
        var random=level.getChunkSource().randomState();var edge=blockbranch.realistic.DrySteppeFixtures.transition(generator.drainage(random),random.seed(),false);var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-32;dz<=32;dz+=16) for(int dx=-32;dx<=32;dx+=16) {
            int x=Math.floorDiv(edge.insideX()+dx,16)*16,z=Math.floorDiv(edge.insideZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(Blockbranch.id("dry_continental_scrub_steppe"))) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz,h=generator.drainage(random).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);
                for(int y=h-3;y<=h+2;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))==generator.surfaceState(wx,y,wz,random),"Steppe/native chunk edge structure agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Steppe biome edge agrees with source");
            }
            Blockbranch.LOGGER.info("DRY_CONTINENTAL_SCRUB_STEPPE_BOUNDARY_PASS x={} z={} steppeQuartCells={} transition={}",x,z,count,edge);return;
        }
        throw new AssertionError("No mixed steppe native biome boundary");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private SteppeSmoke() {}
}
