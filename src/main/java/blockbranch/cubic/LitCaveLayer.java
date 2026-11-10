package blockbranch.cubic;

import blockbranch.realistic.DripstoneHabitat;
import blockbranch.realistic.TerrainShape;
import java.util.*;
import java.util.function.BiPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Definition2: sparse moist moss floors in existing directly skylit entrance recesses. */
public final class LitCaveLayer {
    private static final int[] EMPTY=new int[0];
    public static final LitCaveLayer NONE=new LitCaveLayer();
    public static final int DEFINITION=2,MAX_PLACEMENTS=64,MAX_DEPTH=12;
    private final boolean enabled;
    private final long seed;
    private final int cx,cz,minY,maxY;
    private final int[] placements,floors;
    private final Set<Integer> sections;
    private LitCaveLayer(){enabled=false;seed=0;cx=cz=0;minY=Integer.MAX_VALUE;maxY=Integer.MIN_VALUE;placements=EMPTY;floors=null;sections=Set.of();}
    public LitCaveLayer(long seed,int cx,int cz,int[] placements){
        if(placements.length%2!=0||placements.length>MAX_PLACEMENTS*2||cx<-(ColumnLimits.HORIZONTAL_LIMIT>>4)||cx>=(ColumnLimits.HORIZONTAL_LIMIT>>4)||cz<-(ColumnLimits.HORIZONTAL_LIMIT>>4)||cz>=(ColumnLimits.HORIZONTAL_LIMIT>>4))throw new IllegalArgumentException("Invalid lit cave descriptor");
        enabled=true;this.seed=seed;this.cx=cx;this.cz=cz;this.placements=placements.length==0?EMPTY:placements.clone();
        if(this.placements.length==0){floors=null;minY=Integer.MAX_VALUE;maxY=Integer.MIN_VALUE;sections=Set.of();return;}
        floors=new int[256];Arrays.fill(floors,Integer.MIN_VALUE);var sy=new HashSet<Integer>();int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(int i=0;i<this.placements.length;i+=2){int xz=this.placements[i],y=this.placements[i+1];
            if(xz<0||xz>255||y<=TerrainShape.FLOOR||y>=TerrainShape.CEILING||floors[xz]!=Integer.MIN_VALUE)throw new IllegalArgumentException("Invalid lit cave placement");
            floors[xz]=y;sy.add(y>>4);low=Math.min(low,y);high=Math.max(high,y);
        }
        minY=low;maxY=high;sections=Set.copyOf(sy);
    }
    public boolean enabled(){return enabled;}
    public boolean owns(int x,int z){return !enabled||cx==x&&cz==z;}
    public Set<Integer> sections(){return sections;}
    public boolean hasSection(int sy){return floors!=null&&sy>=(minY>>4)&&sy<=(maxY>>4)&&sections.contains(sy);}
    public int[] placements(){return placements.clone();}
    public static boolean host(BlockState s){return s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE)||s.is(Blocks.DIRT)||s.is(Blocks.COARSE_DIRT)||s.is(Blocks.GRASS_BLOCK);}
    public BlockState state(int x,int y,int z,BlockState base){
        if(floors==null||y<minY||y>maxY)return base;
        return floors[(z&15)*16+(x&15)]==y&&host(base)?Blocks.MOSS_BLOCK.defaultBlockState():base;
    }
    public void applyColumn(int x,int z,int min,BlockState[] states){
        if(floors==null)return;int y=floors[(z&15)*16+(x&15)];
        if(y>=min&&y-min<states.length)states[y-min]=state(x,y,z,states[y-min]);
    }
    public interface Sampler {
        boolean hasApproach();boolean suitable(int x,int z);int ground(int x,int z);int top(int x,int z);
        BlockState state(int x,int y,int z);boolean approachAt(int x,int y,int z);
    }
    public static LitCaveLayer plan(TerrainProfile base,long seed,int cx,int cz,BiPredicate<Integer,Integer> suitability){
        return plan(new Sampler(){
            public boolean hasApproach(){return base.underground().hasApproach();}
            public boolean suitable(int x,int z){return suitability.test(cx*16+x,cz*16+z);}
            public int ground(int x,int z){return base.groundHeight(x,z);}
            public int top(int x,int z){return base.top(Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,base.ceiling()-1);}
            public BlockState state(int x,int y,int z){return base.state(x,y,z);}
            public boolean approachAt(int x,int y,int z){return base.underground().approachAt(x,y,z);}
        },seed,cx,cz);
    }
    public static LitCaveLayer plan(Sampler base,long seed,int cx,int cz){
        if(!base.hasApproach())return new LitCaveLayer(seed,cx,cz,EMPTY);
        int[] result=new int[MAX_PLACEMENTS*2];int n=0;
        scan:for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            if(n==result.length)break scan;
            long hash=DripstoneHabitat.mix(seed^(long)Math.floorDiv(cx*16+x,3)*0x9E3779B97F4A7C15L^(long)Math.floorDiv(cz*16+z,3)*0xA24BAED4963EE407L^0x6c69745f6d6f7373L);
            if((hash&3)!=0||!base.suitable(x,z))continue;
            int floor=base.top(x,z)-1,depth=base.ground(x,z)-floor;
            if(depth<1||depth>MAX_DEPTH||!base.approachAt(x,floor+1,z)||!host(base.state(x,floor,z))||!base.state(x,floor+1,z).isAir())continue;
            result[n++]=z*16+x;result[n++]=floor;
        }
        return new LitCaveLayer(seed,cx,cz,Arrays.copyOf(result,n));
    }
    public void validate(TerrainProfile base){
        if(!enabled)return;
        if(!base.underground().enabled()||base.underground().seed()!=seed||!base.underground().owns(cx,cz)||!base.underground().hasApproach()&&placements.length!=0)throw new IllegalArgumentException("Lit cave requires its existing cave/approach descriptor");
        for(int i=0;i<placements.length;i+=2){int x=placements[i]&15,z=placements[i]>>4,y=placements[i+1],depth=base.groundHeight(x,z)-y;
            if(depth<1||depth>MAX_DEPTH||!base.underground().approachAt(x,y+1,z)||!host(base.state(x,y,z))||base.top(Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,base.ceiling()-1)!=y+1)throw new IllegalArgumentException("Lit cave violates natural-light/host bounds");
        }
    }
    public CompoundTag encode(){
        if(!enabled)throw new IllegalStateException("Disabled lit cave layer has no descriptor");
        var t=new CompoundTag();t.putInt("definition",DEFINITION);t.putLong("seed",seed);t.putInt("column_x",cx);t.putInt("column_z",cz);t.putIntArray("placements",placements.clone());return t;
    }
    public static LitCaveLayer decode(CompoundTag t){
        if(t.getIntOr("definition",0)!=DEFINITION)throw new IllegalArgumentException("Unsupported lit cave definition");
        return new LitCaveLayer(t.getLong("seed").orElseThrow(),t.getInt("column_x").orElseThrow(),t.getInt("column_z").orElseThrow(),t.getIntArray("placements").orElseThrow());
    }
}
