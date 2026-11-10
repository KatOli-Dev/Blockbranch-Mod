package blockbranch.realistic;

import blockbranch.cubic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native source-water ice, profile/snapshot parity, melt restoration and saved edits. */
public final class FrozenRiverSmoke {
    static BlockPos scene(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r){
        var d=g.drainage(r);var p=blockbranch.realistic.FrozenRiverFixtures.fixture(d,r.seed());return new BlockPos(p.x(),d.sample(p.x(),p.z()).waterLevel()-1,p.z());
    }
    /** Additional broad highland valley; original gorge remains the native regression witness. */
    static BlockPos visualScene(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r){
        if(r.seed()!=424242)return scene(g,r);
        int x=1064664,z=1284184;var d=g.drainage(r);var c=new ClimateField(r.seed());
        if(!FrozenRiver.contains(x,z,d,c))throw new AssertionError("Frozen visual witness lost eligibility");
        return new BlockPos(x,d.sample(x,z).waterLevel()-1,z);
    }
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception{
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var ice=scene(g,r);var saved=ice.below(16);
        if(reload&&!level.getBlockState(saved).is(Blocks.GOLD_BLOCK))throw new AssertionError("Frozen River saved deep edit");
        var visual=visualScene(g,r);var visualChunk=level.getChunkAt(visual);
        if(!level.getBiome(visual).is(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER)||!level.getBlockState(visual).is(Blocks.ICE)||!level.getFluidState(visual.below()).isSource())throw new AssertionError("Additional visual site ice/identity/liquid");
        var visualBase=((SparseColumn)visualChunk).blockbranch$terrain().base();var visualCopy=TerrainProfile.decode(visualBase.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
        for(int dy=-3;dy<=1;dy++){var q=visual.above(dy);if(level.getBlockState(q)!=g.surfaceState(q.getX(),q.getY(),q.getZ(),r)||visualCopy.state(q.getX(),q.getY(),q.getZ())!=level.getBlockState(q))throw new AssertionError("Visual site native/source/decoded parity");}
        int count=0;var climate=new ClimateField(r.seed());
        for(int cz=(ice.getZ()>>4)-1;cz<=(ice.getZ()>>4)+1;cz++)for(int cx=(ice.getX()>>4)-1;cx<=(ice.getX()>>4)+1;cx++){
            var chunk=level.getChunk(cx,cz);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            for(int value:FrozenRiver.column(cx,cz,d,climate)){
                int key=value>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var p=new BlockPos(x,y,z);
                for(int dy=-2;dy<=1;dy++){var q=p.above(dy);var expected=g.surfaceState(x,q.getY(),z,r);if(chunk.getBlockState(q)!=expected||copy.state(x,q.getY(),z)!=expected||copy.generate(q.getY()>>4).getBlockState(x&15,q.getY()&15,z&15)!=expected)throw new AssertionError("Frozen River native/decoded/materialized parity "+q);}
                if(!level.getBlockState(p).is(Blocks.ICE)||!level.getFluidState(p.below()).isSource()||!base.randomTickSections().contains(y>>4))throw new AssertionError("Ice surface/source liquid/ticking");
                if(chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15)+1!=y+1)throw new AssertionError("Ice surface heightmap");count++;
            }
            int[] iceEntries=FrozenRiver.column(cx,cz,d,climate);
            if(iceEntries.length>0){
                var bad=tag.copy();int value=iceEntries[0],key=value>>>blockbranch.cubic.CanopyLayer.STATE_BITS;
                // Move a single ice block one metre above the allowed water surface.
                bad.putIntArray("canopy",new int[]{((key+256)<<blockbranch.cubic.CanopyLayer.STATE_BITS)|FrozenRiver.ICE});
                boolean rejected=false;try{TerrainProfile.decode(bad,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}
                if(!rejected)throw new AssertionError("Ice above the physical water surface accepted");
            }
            int[] snapshot=tag.getIntArray("canopy").orElseThrow();if(snapshot.length>0){int old=base.encode(level.palettedContainerFactory()).getIntArray("canopy").orElseThrow()[0];snapshot[0]=0;if(base.encode(level.palettedContainerFactory()).getIntArray("canopy").orElseThrow()[0]!=old)throw new AssertionError("Frozen canopy snapshot isolation");}
        }
        if(count<16||!level.getBiome(ice).is(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER))throw new AssertionError("Frozen River identity/nonempty cover");
        var melt=net.minecraft.world.level.block.IceBlock.class.getDeclaredMethod("melt",net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class);melt.setAccessible(true);melt.invoke(Blocks.ICE,level.getBlockState(ice),level,ice);
        if(!level.getBlockState(ice).is(Blocks.WATER)||!level.getFluidState(ice).isSource())throw new AssertionError("Native ice melt must restore source water");
        RiverSmoke.restore(level,ice);if(!level.getBlockState(ice).is(Blocks.ICE))throw new AssertionError("Restored procedural ice");
        level.setBlock(saved,Blocks.GOLD_BLOCK.defaultBlockState(),2);var terrain=((SparseColumn)level.getChunkAt(ice)).blockbranch$terrain();level.getChunkAt(ice).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);if(!level.getBlockState(saved).is(Blocks.GOLD_BLOCK))throw new AssertionError("Frozen saved edit eviction");
        blockbranch.Blockbranch.LOGGER.info("FROZEN_RIVER_NATIVE_PASS mode={} ice={} count={} waterBelow=source melt=source restored=true",reload?"RELOAD":"NEW",ice,count);
    }
    private FrozenRiverSmoke(){}
}
