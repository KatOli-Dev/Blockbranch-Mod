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

/** Steppe structure, transitions, fescue use/drop/state behavior and saved crown regression. */
public final class ColdSteppeSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        String fixed=System.getProperty("blockbranch.coldSteppeCaptureXZ");
        if(fixed!=null){
            var parts=fixed.split(",");int x=Integer.parseInt(parts[0]),z=Integer.parseInt(parts[1]);
            if(generator.coldSteppeCover(x,z,random)!=4)throw new AssertionError("Fixed capture anchor remains natural fescue");
            return new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
        }
        var p=blockbranch.realistic.ColdSteppeFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static void restore(ServerLevel level,BlockPos bed) {
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        for(int dy=0;dy<=1;dy++) level.setBlock(bed.above(dy),base.state(bed.getX(),bed.getY()+dy,bed.getZ()),3);
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        var random=level.getChunkSource().randomState();var resolver=generator.getBiomeSource().createUncachedResolver(random);
        var fescue=MeadowContent.MEADOW_FESCUE;var mature=fescue.defaultBlockState().setValue(MeadowFescueBlock.CROPPED,false);
        require(level.getBiome(bed).is(Blockbranch.id("cold_continental_fescue_steppe")),"Actual steppe identity");
        require(level.getBlockState(bed).is(Blocks.GRASS_BLOCK) && level.getBlockState(bed.above())==mature,"Natural mature bunchgrass on dry sward");
        var witnesses=new BlockPos[8];
        for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++) for(int x=bed.getX()-64;x<=bed.getX()+64;x++) {
            int c=generator.coldSteppeCover(x,z,random);if(c>0 && witnesses[c]==null) witnesses[c]=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
        }
        for(int cover=1;cover<=7;cover++) {
            var pos=witnesses[cover];require(pos!=null,"Natural soil/sward/fescue component "+cover);
            int x=pos.getX(),z=pos.getZ(),h=pos.getY();var chunk=level.getChunkAt(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(base.canopy().size()==0,"Dry climate remains treeless, without replacing prior woodland");
            for(int y=h-4;y<=h+2;y++) {
                var expected=RealisticGenerator.material(y,h,DrainageNetwork.DRY,0,0,0,0,0,0,0,0,0,cover);
                require(chunk.getBlockState(new BlockPos(x,y,z))==expected && decoded.state(x,y,z)==expected,"Native/procedural/decoded steppe structure "+cover);
                require(decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Materialized client steppe definition");
            }
            require(base.top(s->s.is(Blocks.STONE),x,z,h)==h-2,"Bounded three metre root zone over unchanged rock");
            require(base.top(s->s.is(Blocks.DIRT),x,z,h)==h,"Fine soil below surface");
            require(base.top(s->s.is(Blocks.GRAVEL),x,z,h)==(cover==2 ? h+1 : h-1),"Gravel lag versus buried stony layer");
            if(cover==3 || cover==4 || cover==6 || cover==7) require(level.getBlockState(pos.above()).canSurvive(level,pos.above()),"Every natural plant has suitable dry support");
            for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-3,2),LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+4,2)}) {
                var column=generator.getBaseColumn(x,z,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),h+2);y>=bounds.getMinY();y--) if(type.isOpaque().test(column.getBlock(y))) { expected=y+1;break; }
                    require(generator.getBaseHeight(x,z,type,bounds,random)==expected,"Cropped steppe heightmap "+type);
                    if(bounds==level) require(chunk.getHeight(type,x&15,z&15)+1==expected,"Native soil/vegetation heightmap "+type);
                }
            }
            for(int qy:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))
                && decoded.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"All-altitude biome palette round trip");
        }
        boundary(level,generator);
        var shears=new ItemStack(Items.SHEARS);var drops=Block.getDrops(mature,level,bed.above(),null,null,shears);
        require(drops.size()==1 && drops.getFirst().is(MeadowContent.FESCUE_ITEM),"Shears retain a live planting crown");
        drops=Block.getDrops(mature,level,bed.above(),null,null,ItemStack.EMPTY);
        require(drops.isEmpty(),"Ordinary fescue breaking produces no extra ingredient");
        require(fescue.defaultBlockState().getValue(MeadowFescueBlock.CROPPED),"Transplants start cropped");
        require(fescue.divide(level,bed.above(),mature),"Clip mature fescue");var cropped=level.getBlockState(bed.above());
        require(cropped.is(fescue) && cropped.getValue(MeadowFescueBlock.CROPPED) && !fescue.divide(level,bed.above(),cropped),"Retained crown cannot be cropped repeatedly");
        require(cropped.getShape(level,bed.above()).max(Direction.Axis.Y)<=0.375 && mature.getShape(level,bed.above()).max(Direction.Axis.Y)>0.8,"Cropped versus mature geometry");
        require(Block.getDrops(cropped,level,bed.above(),null,null,ItemStack.EMPTY).isEmpty(),"No extra ingredient from a cropped crown");
        drops=Block.getDrops(cropped,level,bed.above(),null,null,shears);require(drops.size()==1 && drops.getFirst().is(MeadowContent.FESCUE_ITEM),"Cropped crown remains transplantable");
        require(!cropped.isRandomlyTicking() && fescue.isValidBonemealTarget(level,bed.above(),cropped,BonemealSource.INTERACTION),"Deliberate regrowth without world scans");
        fescue.performBonemeal(level,level.getRandom(),bed.above(),cropped,BonemealSource.INTERACTION);
        require(level.getBlockState(bed.above())==mature && !fescue.isValidBonemealTarget(level,bed.above(),mature,BonemealSource.INTERACTION),"Regrowth stops at one mature fescue");
        for(var soil:List.of(Blocks.STONE,Blocks.SAND,Blocks.MUD)) {
            level.setBlock(bed,soil.defaultBlockState(),3);require(level.getBlockState(bed.above()).isAir() && !cropped.canSurvive(level,bed.above()),"Unsuitable soil removes fescue");restore(level,bed);
        }
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putInt("version",10);boolean rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }require(rejected,"Pre-steppe profile rejected");
        invalid=terrain.base().encode(level.palettedContainerFactory());invalid.putByteArray("cold_steppe",new byte[255]);rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }require(rejected,"Truncated steppe array rejected");
        var saved=bed.below(8);
        if(reload) require(level.getBlockState(saved).is(fescue) && level.getBlockState(saved).getValue(MeadowFescueBlock.CROPPED),"Saved cropped fescue survives reopen");
        level.setBlock(saved.below(),Blocks.COARSE_DIRT.defaultBlockState(),2);level.setBlock(saved,cropped,3);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(saved)==cropped,"Cropped property survives section eviction");
        Blockbranch.LOGGER.info("COLD_CONTINENTAL_FESCUE_STEPPE_PASS {} bed={} components=bare-soil,gravel,dry-grass,fescue,sward,sparse-silver-shrub,dead-stems",reload ? "RELOAD" : "NEW",bed);
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator) {
        var random=level.getChunkSource().randomState();var edge=blockbranch.realistic.ColdSteppeFixtures.transition(generator.drainage(random),random.seed(),false);var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-32;dz<=32;dz+=16) for(int dx=-32;dx<=32;dx+=16) {
            int x=Math.floorDiv(edge.insideX()+dx,16)*16,z=Math.floorDiv(edge.insideZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(Blockbranch.id("cold_continental_fescue_steppe"))) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz,h=generator.drainage(random).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);
                for(int y=h-3;y<=h+2;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))==generator.surfaceState(wx,y,wz,random),"Steppe/native chunk edge structure agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Steppe biome edge agrees with source");
            }
            Blockbranch.LOGGER.info("COLD_CONTINENTAL_FESCUE_STEPPE_BOUNDARY_PASS x={} z={} steppeQuartCells={} transition={}",x,z,count,edge);return;
        }
        throw new AssertionError("No mixed steppe native biome boundary");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private ColdSteppeSmoke() {}
}
