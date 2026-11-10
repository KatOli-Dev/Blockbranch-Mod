package blockbranch.cubic;

import blockbranch.realistic.TerrainShape;
import java.util.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable solid spans, separate from the bounded sparse vegetation dictionary. */
public final class FloatingIceLayer {
    private static final int MASK=32767,SNOW=1<<30;
    public static final FloatingIceLayer EMPTY=new FloatingIceLayer(new int[256]);
    private final int[] spans;
    private final Set<Integer> sections;
    private final int highest;
    public FloatingIceLayer(int[] values){
        if(values.length!=256)throw new IllegalArgumentException("Floating ice requires 256 spans");
        spans=values.clone();var occupied=new TreeSet<Integer>();int high=Integer.MIN_VALUE;
        for(int value:spans){
            if(value==0)continue;
            int lo=bottom(value),hi=top(value);
            if(value<0||lo<=TerrainShape.FLOOR||lo>hi||hi>=TerrainShape.CEILING||snow(value)&&hi<TerrainShape.SEA_LEVEL)throw new IllegalArgumentException("Invalid floating ice span");
            high=Math.max(high,hi);for(int sy=lo>>4;sy<=hi>>4;sy++)occupied.add(sy);
        }
        highest=high;sections=Set.copyOf(occupied);
    }
    public static int pack(int bottom,int top,boolean snow){
        if(bottom<=TerrainShape.FLOOR||bottom>top||top>=TerrainShape.CEILING||snow&&top<TerrainShape.SEA_LEVEL)throw new IllegalArgumentException("Invalid floating ice bounds");
        return bottom-TerrainShape.FLOOR|((top-TerrainShape.FLOOR)<<15)|(snow?SNOW:0);
    }
    private static int bottom(int value){return TerrainShape.FLOOR+(value&MASK);}
    private static int top(int value){return TerrainShape.FLOOR+((value>>>15)&MASK);}
    private static boolean snow(int value){return(value&SNOW)!=0;}
    public int bottom(int x,int z){int v=spans[(z&15)*16+(x&15)];return v==0?Integer.MIN_VALUE:bottom(v);}
    public int top(int x,int z){int v=spans[(z&15)*16+(x&15)];return v==0?Integer.MIN_VALUE:top(v);}
    public BlockState state(int x,int y,int z){int v=spans[(z&15)*16+(x&15)];if(v==0||y<bottom(v)||y>top(v))return null;return snow(v)&&y==top(v)?Blocks.SNOW_BLOCK.defaultBlockState():Blocks.PACKED_ICE.defaultBlockState();}
    public int highest(){return highest;}
    public Set<Integer> sections(){return sections;}
    public int[] encode(){return spans.clone();}
}
