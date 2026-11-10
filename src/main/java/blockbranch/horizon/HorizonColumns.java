package blockbranch.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import java.util.function.Predicate;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** Which block a distant column shows as its surface, and how a generated column is read. */
public final class HorizonColumns {
    /**
     * Solid ground, leaves, snow layers and other collidable blocks are surfaces. Air,
     * fluids, lily pads and plants without collision are looked through: at horizon scale
     * one cell stands for many blocks and the ground or canopy below dominates its colour.
     */
    public static boolean visible(BlockState state) {
        if(state.isAir()||state.getBlock() instanceof LiquidBlock||state.is(Blocks.LILY_PAD))return false;
        if(!state.getFluidState().isEmpty()&&!state.isSolid())return false;
        return state.isSolid()||state.getBlock() instanceof LeavesBlock||state.getBlock() instanceof SnowLayerBlock;
    }
    private static boolean surfaceOrWater(BlockState state){return visible(state)||state.getFluidState().is(FluidTags.WATER);}
    private static boolean leaves(BlockState state){return state.getBlock() instanceof LeavesBlock;}
    /** A visible block that is not part of a tree: what a crown stands over. */
    private static boolean terrain(BlockState state){return visible(state)&&!leaves(state)&&!state.is(BlockTags.LOGS);}

    /** Blocks of one generated chunk, read without loading or ticking it. */
    public interface Source {
        BlockState state(int x,int y,int z);
        /** Highest y at or below {@code limit} whose block matches, plus one; at most the floor when none does. */
        int top(Predicate<BlockState> predicate,int x,int z,int limit);
    }

    /** Longest crown, trunk and non-leaf run followed down from the top, in blocks. */
    static final int CROWN=128,TRUNK=64,RUN=8;

    /**
     * Writes a generated column in the same form as the analytic sampler, so structures,
     * cave openings and edits match the terrain around them. A leaf crown floats over the
     * first non-tree block below it unless a trunk joins them; a trunk then becomes the
     * ground, as in the analytic sampler. A cactus stands on the block under its base. Any
     * other top run of at most {@link #RUN} blocks over air (a roof, a bridge, floating ice)
     * also floats, so interiors are not filled; a deeper run is solid. The biome is left to
     * the caller. Returns false for a column with no surface.
     */
    public static boolean read(Source s,int x,int z,int floor,int ceiling,int[] out,int o) {
        int top=s.top(HorizonColumns::visible,x,z,ceiling)-1;
        if(top<=floor)return false;
        var topState=s.state(x,top,z);
        int runBottom=top,ground;BlockState groundState=null,deep=null;
        int cactus=topState.is(Blocks.CACTUS)?cactusBase(s,x,z,top):Integer.MIN_VALUE;
        if(leaves(topState)) {
            int y=top-1;while(y>top-CROWN&&leaves(s.state(x,y,z)))y--;
            runBottom=y+1;var below=s.state(x,y,z);
            ground=Integer.MIN_VALUE;
            if(below.is(BlockTags.LOGS)) {
                int bottom=y;while(bottom>y-TRUNK&&s.state(x,bottom-1,z).is(BlockTags.LOGS))bottom--;
                if(terrain(s.state(x,bottom-1,z))){ground=y;groundState=below;deep=below;}
            }
            // Lower crowns, crown gaps and branches are looked through to the terrain, as the
            // analytic canopy shows only the top run over the ground; a log or bush resting
            // within two blocks of the terrain is ground, as the analytic ground window is.
            if(ground==Integer.MIN_VALUE) {
                ground=s.top(HorizonColumns::terrain,x,z,y)-1;
                for(int k=Math.min(2,y+1-ground);k>0;k--)if(visible(s.state(x,ground+k,z))){ground+=k;break;}
            }
        } else if(cactus!=Integer.MIN_VALUE&&visible(s.state(x,cactus-1,z))) {
            // A cactus stands on solid ground, so the scan below would find no air and make its
            // tip the ground, drawing the shaft in the deep material. The block under its base
            // is the ground instead, and the mesher draws the run above it in cactus.
            ground=cactus-1;runBottom=cactus;
        } else {
            int y=top-1,k=0;while(k<RUN&&visible(s.state(x,y,z))){y--;k++;}
            ground=top;
            if(k<RUN){int below=s.top(HorizonColumns::visible,x,z,y)-1;if(below>floor){runBottom=y+1;ground=below;}}
        }
        if(ground<=floor){ground=runBottom=top;groundState=topState;}
        if(groundState==null)groundState=s.state(x,ground,z);
        if(deep==null){deep=s.state(x,ground-3,z);if(!visible(deep))deep=Blocks.STONE.defaultBlockState();}
        int wet=s.top(HorizonColumns::surfaceOrWater,x,z,runBottom>ground+1?runBottom-1:ceiling)-1;
        int water=wet>ground&&!visible(s.state(x,wet,z))?wet:NONE;
        out[o+TOP]=top;out[o+TOP_STATE]=Block.getId(topState);out[o+GROUND]=ground;out[o+GROUND_STATE]=Block.getId(groundState);
        out[o+DEEP_STATE]=Block.getId(deep);out[o+WATER]=water;out[o+CANOPY_BOTTOM]=runBottom;
        return true;
    }
    /** Lowest block of the cactus run ending at {@code top}, followed down at most {@link #TRUNK} blocks. */
    private static int cactusBase(Source s,int x,int z,int top){int y=top;while(y>top-TRUNK&&s.state(x,y-1,z).is(Blocks.CACTUS))y--;return y;}
    private HorizonColumns(){}
}
