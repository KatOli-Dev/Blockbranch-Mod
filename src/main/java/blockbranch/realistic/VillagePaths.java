package blockbranch.realistic;

import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Clear unsupported ground cover only on paths actually written by a new village. */
public final class VillagePaths {
    private static final ThreadLocal<LinkedHashSet<BlockPos>> WRITTEN=new ThreadLocal<>();
    private VillagePaths(){}
    public static void written(BlockPos pos,BlockState state){
        var paths=WRITTEN.get();if(paths!=null&&state.is(Blocks.DIRT_PATH))paths.add(pos.immutable());
    }
    public static void place(WorldGenLevel level,Runnable placement){
        var before=WRITTEN.get();var paths=new LinkedHashSet<BlockPos>();WRITTEN.set(paths);
        try{
            placement.run();
            // Terrain vegetation precedes native templates. Path blocks do not
            // send normal neighbour updates during worldgen, so both plant halves
            // need explicit removal. Do not touch surrounding gardens or lawns.
            for(var path:paths)if(level.getBlockState(path).is(Blocks.DIRT_PATH)){
                for(int dy=1;dy<=2;dy++){
                    var pos=path.above(dy);var state=level.getBlockState(pos);
                    if(groundCover(state)&&!state.canSurvive(level,pos))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                }
            }
        }finally{if(before==null)WRITTEN.remove();else WRITTEN.set(before);}
    }
    public static boolean groundCover(BlockState state){
        return state.getBlock() instanceof VegetationBlock||state.getBlock() instanceof MeadowFescueBlock||state.getBlock() instanceof SteppeShrubBlock||state.getBlock() instanceof BeachGrassBlock;
    }
}
