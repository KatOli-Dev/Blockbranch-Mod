package blockbranch.cubic;

import blockbranch.realistic.TerrainShape;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Compact immutable local tree blocks; deep terrain and empty vertical layers remain implicit. */
public final class CanopyLayer {
    public static final int MAX_BLOCKS=8192;
    public static final int ACACIA_WOOD_CODE=169;
    /** Codes 170..172 are mangrove bark on the Y/X/Z axis for redwood boles. */
    public static final int REDWOOD_WOOD_CODE=170;
    public static final int LAST_CODE=REDWOOD_WOOD_CODE+2;
    public static final int STATE_BITS=8,STATE_MASK=(1<<STATE_BITS)-1;
    public static final CanopyLayer EMPTY=new CanopyLayer(new int[0]);
    private static final BlockState[] STATES=states();
    private final int[] blocks,top=new int[256];
    private final int min,max;
    private final Set<Integer> sections;
    public CanopyLayer(int[] packed) {
        if(packed.length>MAX_BLOCKS) throw new IllegalArgumentException("Oversized canopy layer");
        blocks=packed.clone();Arrays.fill(top,Integer.MIN_VALUE);
        int previous=-1,minimum=Integer.MAX_VALUE,maximum=Integer.MIN_VALUE;var occupied=new TreeSet<Integer>();
        for(int value:blocks) {
            int key=value>>>STATE_BITS,code=value&STATE_MASK,y=(key>>>8)+TerrainShape.FLOOR;
            if(key<=previous || code<1 || code>LAST_CODE || y<=TerrainShape.FLOOR || y>=TerrainShape.CEILING) throw new IllegalArgumentException("Invalid canopy block");
            previous=key;minimum=Math.min(minimum,y);maximum=Math.max(maximum,y);top[key&255]=y;occupied.add(y>>4);
        }
        min=minimum;max=maximum;sections=Collections.unmodifiableSet(occupied);
    }
    public int[] encode() { return blocks.clone(); }
    public int size() { return blocks.length; }
    public int highest() { return max; }
    public int topY(int x,int z) { return top[(z&15)*16+(x&15)]; }
    public Set<Integer> sections() { return sections; }
    public BlockState state(int x,int y,int z) {
        if(y<min || y>max) return null;
        int key=((y-TerrainShape.FLOOR)<<8)|((z&15)*16+(x&15)),lo=0,hi=blocks.length-1;
        while(lo<=hi) {
            int mid=(lo+hi)>>>1,position=blocks[mid]>>>STATE_BITS;
            if(position==key) return STATES[blocks[mid]&STATE_MASK];
            if(position<key) lo=mid+1;else hi=mid-1;
        }
        return null;
    }
    public boolean maybeHas(Predicate<BlockState> predicate) {
        return maybeHas(Integer.MIN_VALUE,predicate);
    }
    /** Only states actually present in this layer/section can require a block scan. */
    public boolean maybeHas(int sectionY,Predicate<BlockState> predicate) {
        if(sectionY!=Integer.MIN_VALUE&&!sections.contains(sectionY))return false;
        var tested=new BitSet(LAST_CODE+1);
        for(int value:blocks) {
            if(sectionY!=Integer.MIN_VALUE&&((((value>>>STATE_BITS)>>>8)+TerrainShape.FLOOR)>>4)!=sectionY)continue;
            int code=value&STATE_MASK;
            if(!tested.get(code)){tested.set(code);if(predicate.test(STATES[code]))return true;}
        }
        return false;
    }
    private static BlockState[] states() {
        var states=new BlockState[LAST_CODE+1];var axes=new Direction.Axis[]{Direction.Axis.Y,Direction.Axis.X,Direction.Axis.Z};
        for(int i=0;i<3;i++) {
            states[1+i]=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
            states[37+i]=Blocks.JUNGLE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
            states[28+i]=Blocks.ACACIA_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
            states[19+i]=Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
            states[4+i]=Blocks.BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
        }
        for(int distance=1;distance<=6;distance++) {
            states[6+distance]=Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);
            states[39+distance]=Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);
            states[30+distance]=Blocks.ACACIA_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);
            states[21+distance]=Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);
            states[12+distance]=Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);
        }
        var faces=new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
        for(int mask=1;mask<16;mask++){var vine=Blocks.VINE.defaultBlockState();for(int bit=0;bit<4;bit++)vine=vine.setValue(VineBlock.getPropertyForFace(faces[bit]),(mask&(1<<bit))!=0);states[45+mask]=vine;}
        states[61]=Blocks.LILY_PAD.defaultBlockState();
        for(int layers=1;layers<=8;layers++)states[80+layers]=Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS,layers);
        for(int i=0;i<3;i++)states[78+i]=Blocks.SPRUCE_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
        for(int i=0;i<3;i++)states[62+i]=Blocks.MANGROVE_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
        for(int distance=1;distance<=6;distance++)states[64+distance]=Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);
        states[71]=Blocks.MANGROVE_ROOTS.defaultBlockState();states[72]=states[71].setValue(MangroveRootsBlock.WATERLOGGED,true);
        for(int age=0;age<=4;age++)states[73+age]=MangrovePropaguleBlock.createNewHangingPropagule(age);
        states[89]=Blocks.KELP_PLANT.defaultBlockState();states[90]=Blocks.KELP.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE,23);
        states[91]=Blocks.ICE.defaultBlockState();
        for(int i=0;i<3;i++){states[92+i]=Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);states[101+i]=Blocks.CHERRY_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);}
        for(int distance=1;distance<=6;distance++){states[94+distance]=Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);states[103+distance]=Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);}
        for(int code=blockbranch.realistic.DarkOakBranches.FIRST;code<=blockbranch.realistic.DarkOakBranches.LAST;code++)states[code]=blockbranch.realistic.DarkOakBranchContent.BRANCH.defaultBlockState().setValue(blockbranch.realistic.DarkOakBranchBlock.AXES,blockbranch.realistic.DarkOakBranches.axes(code)).setValue(blockbranch.realistic.DarkOakBranchBlock.THICK,blockbranch.realistic.DarkOakBranches.thick(code));
        var bamboo=Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE,1);
        states[124]=bamboo.setValue(BambooStalkBlock.LEAVES,net.minecraft.world.level.block.state.properties.BambooLeaves.NONE).setValue(BambooStalkBlock.STAGE,0);
        states[125]=bamboo.setValue(BambooStalkBlock.LEAVES,net.minecraft.world.level.block.state.properties.BambooLeaves.SMALL).setValue(BambooStalkBlock.STAGE,0);
        states[126]=bamboo.setValue(BambooStalkBlock.LEAVES,net.minecraft.world.level.block.state.properties.BambooLeaves.LARGE).setValue(BambooStalkBlock.STAGE,1);
        states[127]=bamboo.setValue(BambooStalkBlock.LEAVES,net.minecraft.world.level.block.state.properties.BambooLeaves.LARGE).setValue(BambooStalkBlock.STAGE,0);
        for(int i=0;i<3;i++)states[128+i]=Blocks.POPLAR_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
        Block[] poplar={Blocks.RED_POPLAR_LEAVES,Blocks.ORANGE_POPLAR_LEAVES,Blocks.YELLOW_POPLAR_LEAVES};
        for(int color=0;color<3;color++)for(int distance=1;distance<=6;distance++)states[130+color*6+distance]=poplar[color].defaultBlockState().setValue(LeavesBlock.DISTANCE,distance);
        states[149]=Blocks.RED_SHRUB.defaultBlockState();
        Block[] coral={Blocks.TUBE_CORAL_BLOCK,Blocks.BRAIN_CORAL_BLOCK,Blocks.BUBBLE_CORAL_BLOCK,Blocks.FIRE_CORAL_BLOCK,Blocks.HORN_CORAL_BLOCK};
        Block[] fans={Blocks.TUBE_CORAL_FAN,Blocks.BRAIN_CORAL_FAN,Blocks.BUBBLE_CORAL_FAN,Blocks.FIRE_CORAL_FAN,Blocks.HORN_CORAL_FAN};
        for(int i=0;i<5;i++){states[150+i]=coral[i].defaultBlockState();states[155+i]=fans[i].defaultBlockState().setValue(BaseCoralFanBlock.WATERLOGGED,true);}
        for(int i=0;i<4;i++)states[160+i]=Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES,i+1).setValue(SeaPickleBlock.WATERLOGGED,true);
        Block[] dead={Blocks.DEAD_TUBE_CORAL_BLOCK,Blocks.DEAD_BRAIN_CORAL_BLOCK,Blocks.DEAD_BUBBLE_CORAL_BLOCK,Blocks.DEAD_FIRE_CORAL_BLOCK,Blocks.DEAD_HORN_CORAL_BLOCK};
        for(int i=0;i<5;i++)states[164+i]=dead[i].defaultBlockState();
        states[ACACIA_WOOD_CODE]=Blocks.ACACIA_WOOD.defaultBlockState();
        for(int i=0;i<3;i++)states[REDWOOD_WOOD_CODE+i]=Blocks.MANGROVE_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS,axes[i]);
        return states;
    }
}
