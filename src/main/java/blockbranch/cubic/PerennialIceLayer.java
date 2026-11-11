package blockbranch.cubic;

import blockbranch.realistic.TerrainShape;
import java.util.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Profile-77 solid spans: one int per affected column, no vertical arrays.
 * Snow flag distinguishes a grounded body ending at ground from a packed-ice
 * freshwater lid at the unchanged water surface. Parent profile validates support.
 * Only material discontinuities are retained as boundaries; interiors are uniform. */
public final class PerennialIceLayer {
    private static final int MASK=32767,GROUNDED=1<<30;
    public static final PerennialIceLayer EMPTY=new PerennialIceLayer(new int[256]);
    private final int[] spans;
    private final Set<Integer> boundaries;
    public PerennialIceLayer(int[] values){
        if(values.length!=256)throw new IllegalArgumentException("Perennial ice requires 256 spans");
        spans=values.clone();var edges=new TreeSet<Integer>();
        for(int v:spans)if(v!=0){int a=bottom(v),b=top(v);if(v<0||a<=TerrainShape.FLOOR||a>b||b>=TerrainShape.CEILING||b-a+1>blockbranch.realistic.ContinentalIceField.MAX_DEPTH||!grounded(v)&&a!=b)throw new IllegalArgumentException("Invalid perennial ice span");edges.add(a>>4);edges.add(b>>4);}
        boundaries=Set.copyOf(edges);
    }
    public static int pack(int bottom,int top,boolean grounded){
        if(bottom<=TerrainShape.FLOOR||bottom>top||top>=TerrainShape.CEILING||top-bottom+1>blockbranch.realistic.ContinentalIceField.MAX_DEPTH||!grounded&&bottom!=top)throw new IllegalArgumentException("Invalid perennial ice bounds");
        return bottom-TerrainShape.FLOOR|((top-TerrainShape.FLOOR)<<15)|(grounded?GROUNDED:0);
    }
    private static int bottom(int v){return TerrainShape.FLOOR+(v&MASK);}
    private static int top(int v){return TerrainShape.FLOOR+((v>>>15)&MASK);}
    private static boolean grounded(int v){return(v&GROUNDED)!=0;}
    public int bottom(int x,int z){int v=spans[(z&15)*16+(x&15)];return v==0?Integer.MIN_VALUE:bottom(v);}
    public int top(int x,int z){int v=spans[(z&15)*16+(x&15)];return v==0?Integer.MIN_VALUE:top(v);}
    public boolean grounded(int x,int z){return grounded(spans[(z&15)*16+(x&15)]);}
    public BlockState state(int x,int y,int z){int v=spans[(z&15)*16+(x&15)];return v==0||y<bottom(v)||y>top(v)?null:(grounded(v)&&y==top(v)?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE).defaultBlockState();}
    public Set<Integer> boundaries(){return boundaries;}
    public int[] encode(){return spans.clone();}
    public boolean enabled(){return !boundaries.isEmpty();}
}
