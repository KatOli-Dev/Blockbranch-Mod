package blockbranch.realistic;

import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Actual branch wood with matching collision and model sections; never a decorative fence. */
public final class DarkOakBranchBlock extends Block {
    public static final IntegerProperty AXES=IntegerProperty.create("axes",1,7);
    public static final BooleanProperty THICK=BooleanProperty.create("thick");
    private static final VoxelShape[][] SHAPES=new VoxelShape[2][8];
    static{
        for(int thick=0;thick<2;thick++)for(int mask=1;mask<=7;mask++){
            double lo=thick==1?3:5,hi=16-lo;var shape=Shapes.empty();
            if((mask&1)!=0)shape=Shapes.or(shape,Block.box(lo,0,lo,hi,16,hi));
            if((mask&2)!=0)shape=Shapes.or(shape,Block.box(0,lo,lo,16,hi,hi));
            if((mask&4)!=0)shape=Shapes.or(shape,Block.box(lo,lo,0,hi,hi,16));
            SHAPES[thick][mask]=shape;
        }
    }
    public DarkOakBranchBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(AXES,1).setValue(THICK,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AXES,THICK);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPES[state.getValue(THICK)?1:0][state.getValue(AXES)];}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(AXES,switch(context.getClickedFace().getAxis()){case Y->1;case X->2;case Z->4;});}
}
