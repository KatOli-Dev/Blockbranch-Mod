package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Sward/flower material, interaction, boundary, protocol profile and saved crown regressions. */
public final class MeadowSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var p=blockbranch.realistic.LowlandMeadowFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static void restore(ServerLevel level,BlockPos bed) {
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        for(int dy=0;dy<=1;dy++) level.setBlock(bed.above(dy),base.state(bed.getX(),bed.getY()+dy,bed.getZ()),3);
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        var random=level.getChunkSource().randomState();var resolver=generator.getBiomeSource().createUncachedResolver(random);
        require(level.getBiome(bed).is(Blockbranch.id("rolling_lowland_forb_grass_meadow")),"Actual meadow identity");
        var fescue=MeadowContent.MEADOW_FESCUE;var mature=fescue.defaultBlockState().setValue(MeadowFescueBlock.CROPPED,false);
        require(level.getBlockState(bed).is(Blocks.GRASS_BLOCK) && level.getBlockState(bed.above())==mature,"Natural sward and seedhead tussock");
        var witnesses=new BlockPos[7];
        for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++) for(int x=bed.getX()-64;x<=bed.getX()+64;x++) {
            int c=generator.meadowCover(x,z,random);if(c>0 && c<=6 && witnesses[c]==null && generator.canopy(x,z,random).topY(x,z)==Integer.MIN_VALUE) witnesses[c]=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
        }
        for(int cover=1;cover<=6;cover++) {
            var p=witnesses[cover];require(p!=null,"Natural component "+cover);
            int x=p.getX(),z=p.getZ(),h=p.getY();var chunk=level.getChunkAt(p);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            for(int y=h-4;y<=h+2;y++) {
                var expected=RealisticGenerator.material(y,h,DrainageNetwork.DRY,0,0,0,0,cover);
                require(chunk.getBlockState(new BlockPos(x,y,z))==expected && decoded.state(x,y,z)==expected,"Native/procedural/decoded meadow cover="+cover+" pos="+new BlockPos(x,y,z)+" native="+chunk.getBlockState(new BlockPos(x,y,z))+" stored="+base.state(x,y,z)+" decoded="+decoded.state(x,y,z)+" generator="+generator.surfaceState(x,y,z,random)+" rawMaterial="+expected);
                require(decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==expected,"Client materialized meadow section");
            }
            require(base.top(s->s.is(Blocks.STONE),x,z,h)==h-2,"Three metre soil over unchanged rock");
            require(base.top(s->s.is(Blocks.DIRT),x,z,h)==h,"Dirt below sward");
            if(cover>1) require(level.getBlockState(p.above()).canSurvive(level,p.above()),"Plants supported on meadow sward");
            for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-3,2),LevelHeightAccessor.create(h-1,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+3,2)}) {
                var column=generator.getBaseColumn(x,z,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bounds.getMinY();
                    for(int y=Math.min(bounds.getMaxY(),h+2);y>=bounds.getMinY();y--) if(type.isOpaque().test(column.getBlock(y))) { expected=y+1;break; }
                    require(generator.getBaseHeight(x,z,type,bounds,random)==expected,"Meadow clipped heightmap "+type);
                    if(bounds==level) require(chunk.getHeight(type,x&15,z&15)+1==expected,"Native meadow heightmap "+type);
                }
            }
            for(int qy:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2))
                && decoded.biomes().get((x&15)>>2,qy&3,(z&15)>>2).equals(resolver.getNoiseBiome(x>>2,qy,z>>2)),"All-altitude biome palette round trip");
        }
        boundary(level,generator);
        require(fescue.defaultBlockState().getValue(MeadowFescueBlock.CROPPED),"Transplants begin cropped, preventing repeated divide/replant duplication");
        var drops=Block.getDrops(mature,level,bed.above(),null,null,new ItemStack(Items.SHEARS));
        require(drops.size()==1 && drops.getFirst().is(MeadowContent.FESCUE_ITEM),"Shears recover the planting tuft");
        require(Block.getDrops(mature,level,bed.above(),null,null,ItemStack.EMPTY).isEmpty(),"No generic seed/crafting ingredient");
        require(fescue.divide(level,bed.above(),mature),"Divide mature tussock");
        var cropped=level.getBlockState(bed.above());
        require(cropped.is(fescue) && cropped.getValue(MeadowFescueBlock.CROPPED),"Living cropped crown remains");
        require(!fescue.divide(level,bed.above(),cropped),"Cropped crown cannot be divided again");
        require(!cropped.isRandomlyTicking(),"No automatic spread or regrowth scans");
        require(fescue.isValidBonemealTarget(level,bed.above(),cropped,BonemealSource.INTERACTION),"Cropped crown accepts deliberate regrowth");
        fescue.performBonemeal(level,level.getRandom(),bed.above(),cropped,BonemealSource.INTERACTION);
        require(level.getBlockState(bed.above())==mature && !fescue.isValidBonemealTarget(level,bed.above(),mature,BonemealSource.INTERACTION),"Regrowth stops at one mature tuft");
        for(var soil:List.of(Blocks.STONE,Blocks.SAND,Blocks.MUD)) {
            level.setBlock(bed,soil.defaultBlockState(),3);
            require(level.getBlockState(bed.above()).isAir(),"Removing suitable soil removes tuft");
            require(!cropped.canSurvive(level,bed.above()),"Reject unsuitable substrate");
            restore(level,bed);
        }
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();
        var invalid=base.encode(level.palettedContainerFactory());invalid.putInt("version",5);boolean rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Pre-meadow profile rejected");
        invalid=base.encode(level.palettedContainerFactory());invalid.putByteArray("meadow",new byte[255]);rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Truncated meadow array rejected");
        var saved=bed.below(8);
        if(reload) require(level.getBlockState(saved).is(fescue) && level.getBlockState(saved).getValue(MeadowFescueBlock.CROPPED),"Saved cropped crown survives reopen");
        level.setBlock(saved.below(),Blocks.DIRT.defaultBlockState(),2);level.setBlock(saved,cropped,3);
        chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(saved)==cropped,"Cropped property survives section eviction");
        Blockbranch.LOGGER.info("ROLLING_LOWLAND_MEADOW_PASS {} bed={} components=sward,short-grass,fescue,daisy,cornflower,dandelion retainedSections={}",reload ? "RELOAD" : "NEW",bed,((SparseColumn)chunk).blockbranch$sections().size());
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator) {
        var random=level.getChunkSource().randomState();var center=RiverMarginSmoke.fixture(generator,random);var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-256;dz<=256;dz+=16) for(int dx=-256;dx<=256;dx+=16) {
            int x=Math.floorDiv(center.getX()+dx,16)*16,z=Math.floorDiv(center.getZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(Blockbranch.id("rolling_lowland_forb_grass_meadow"))) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz;var sample=generator.drainage(random).sample(wx,wz);var chunk=level.getChunk(wx>>4,wz>>4);
                for(int y=sample.height()-3;y<=sample.height()+2;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))==generator.surfaceState(wx,y,wz,random),"Meadow/channel/native chunk boundary agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Meadow biome boundary agreement");
            }
            Blockbranch.LOGGER.info("ROLLING_LOWLAND_MEADOW_BOUNDARY_PASS x={} z={} meadowQuartCells={}",x,z,count);return;
        }
        throw new AssertionError("No mixed meadow boundary by routed river");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private MeadowSmoke() {}
}
