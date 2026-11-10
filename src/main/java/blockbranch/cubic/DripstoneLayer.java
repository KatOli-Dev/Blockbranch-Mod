package blockbranch.cubic;

import blockbranch.realistic.*;
import java.util.*;
import it.unimi.dsi.fastutil.longs.*;
import java.util.function.Predicate;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.chunk.*;

/** Immutable vertical biome and sparse placements; never reads mutable world state. */
public final class DripstoneLayer {
    private static final int MOSS=100;
    public static final DripstoneLayer NONE=new DripstoneLayer();
    public static final int MAX_PLACEMENTS=256;
    private static final BlockState[] STATES={Blocks.DRIPSTONE_BLOCK.defaultBlockState(),Blocks.CALCITE.defaultBlockState(),
        Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.UP).setValue(PointedDripstoneBlock.THICKNESS,SpeleothemThickness.TIP),
        Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.DOWN).setValue(PointedDripstoneBlock.THICKNESS,SpeleothemThickness.TIP),
        Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.UP).setValue(PointedDripstoneBlock.THICKNESS,SpeleothemThickness.FRUSTUM),
        Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.DOWN).setValue(PointedDripstoneBlock.THICKNESS,SpeleothemThickness.FRUSTUM),
        Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.UP).setValue(PointedDripstoneBlock.THICKNESS,SpeleothemThickness.BASE),
        Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.DOWN).setValue(PointedDripstoneBlock.THICKNESS,SpeleothemThickness.BASE)};
    private final int definition;private final long seed;private final int cx,cz;private final Holder<Biome> biome;private final int[] placements;
    private final boolean biomePossible;
    private final double[] provinces;private final Long2IntMap values;private final Map<Integer,List<Integer>> columns;
    private final Set<Integer> sections,ticking;
    private DripstoneLayer(){definition=0;seed=0;cx=cz=0;biome=null;biomePossible=false;placements=new int[0];provinces=new double[0];values=Long2IntMaps.EMPTY_MAP;columns=Map.of();sections=ticking=Set.of();}
    public DripstoneLayer(long seed,int cx,int cz,Holder<Biome> biome,int[] placements){this(seed,cx,cz,biome,placements,1);}
    public DripstoneLayer(long seed,int cx,int cz,Holder<Biome> biome,int[] placements,int definition){
        if(definition<1||definition>2)throw new IllegalArgumentException("Unsupported dripstone definition");this.definition=definition;
        if(cx<-(ColumnLimits.HORIZONTAL_LIMIT>>4)||cx>=(ColumnLimits.HORIZONTAL_LIMIT>>4)||cz<-(ColumnLimits.HORIZONTAL_LIMIT>>4)||cz>=(ColumnLimits.HORIZONTAL_LIMIT>>4)||placements.length%3!=0||placements.length>MAX_PLACEMENTS*3)throw new IllegalArgumentException("Invalid dripstone descriptor");
        this.seed=seed;this.cx=cx;this.cz=cz;this.biome=Objects.requireNonNull(biome);this.placements=placements.clone();provinces=new double[16];
        for(int z=0;z<4;z++)for(int x=0;x<4;x++)provinces[z*4+x]=DripstoneHabitat.province(seed,cx*16+x*4,cz*16+z*4);
        boolean any=false;for(double province:provinces)any|=province>.2;biomePossible=any;
        var map=new HashMap<Long,Integer>();var cols=new HashMap<Integer,List<Integer>>();var secs=new TreeSet<Integer>();var ticks=new TreeSet<Integer>();
        for(int i=0;i<placements.length;i+=3){int xz=placements[i],y=placements[i+1],kind=placements[i+2];if(xz<0||xz>255||y<=TerrainShape.FLOOR+32||y>=TerrainShape.CEILING||kind<0||kind>=(definition==1?4:STATES.length)||map.put(key(xz,y),kind)!=null)throw new IllegalArgumentException("Invalid dripstone placement");cols.computeIfAbsent(xz,k->new ArrayList<>()).add(i);secs.add(y>>4);if(kind>=2)ticks.add(y>>4);}
        values=freeze(map);var frozen=new HashMap<Integer,List<Integer>>();cols.forEach((k,v)->frozen.put(k,List.copyOf(v)));columns=Map.copyOf(frozen);sections=Set.copyOf(secs);ticking=Set.copyOf(ticks);
    }
    private static Long2IntMap freeze(Map<Long,Integer> source){
        var result=new Long2IntOpenHashMap(source);result.defaultReturnValue(-1);
        return Long2IntMaps.unmodifiable(result);
    }
    public int definition(){return definition;}
    public boolean enabled(){return biome!=null;}
    /** Runtime-only merged lookup. Original descriptors and validation remain independent. */
    DripstoneLayer withLitCaves(LitCaveLayer growth){
        var moss=growth.placements();return moss.length==0?this:new DripstoneLayer(this,moss);
    }
    private DripstoneLayer(DripstoneLayer base,int[] moss){
        definition=base.definition;seed=base.seed;cx=base.cx;cz=base.cz;biome=base.biome;biomePossible=base.biomePossible;
        placements=base.placements;provinces=base.provinces;columns=base.columns;ticking=base.ticking;
        var merged=new HashMap<>(base.values);var covered=new TreeSet<>(base.sections);
        for(int i=0;i<moss.length;i+=2){
            if(merged.putIfAbsent(key(moss[i],moss[i+1]),MOSS)!=null)throw new IllegalArgumentException("Conflicting cave decorations");
            covered.add(moss[i+1]>>4);
        }
        values=freeze(merged);sections=Set.copyOf(covered);
    }
    /** Reject only sections that cannot contain a qualifying quart sample. */
    public boolean mayHaveBiome(int sy,int lowestGround,int highestGround){
        long bottom=(long)sy*16,top=bottom+12;
        return biomePossible&&bottom<=highestGround-DripstoneHabitat.MIN_DEPTH
            &&top>=lowestGround-DripstoneHabitat.MAX_DEPTH&&top>TerrainShape.FLOOR+32;
    }
    public boolean owns(int x,int z){return !enabled()||cx==x&&cz==z;}
    public int[] placements(){return placements.clone();}
    public Set<Integer> sections(){return sections;}
    public Set<Integer> ticking(){return ticking;}
    private static long key(int xz,int y){return ((long)y<<8)|(xz&255);}
    public Holder<Biome> biome(int x,int absoluteQuartY,int z,int surface,Holder<Biome> fallback){
        return enabled()&&DripstoneHabitat.contains(seed,cx*16+(x&3)*4,absoluteQuartY*4,cz*16+(z&3)*4,surface,provinces[(z&3)*4+(x&3)])?biome:fallback;
    }
    public BlockState state(int x,int y,int z,BlockState base){
        if(values.isEmpty()||!sections.contains(y>>4))return base;
        var kind=values.get(key((z&15)*16+(x&15),y));if(kind<0)return base;
        if(kind==MOSS)return LitCaveLayer.host(base)?Blocks.MOSS_BLOCK.defaultBlockState():base;
        return kind<2?(host(base)?STATES[kind]:base):(base.isAir()?STATES[kind]:base);
    }
    public void applyColumn(int x,int z,int min,BlockState[] target){var list=columns.get((z&15)*16+(x&15));if(list==null)return;for(int i:list){int y=placements[i+1],offset=y-min;if(offset>=0&&offset<target.length)target[offset]=state(x,y,z,target[offset]);}}
    public boolean maybeHas(int sy,Predicate<BlockState> predicate){if(!sections.contains(sy))return false;for(var s:STATES)if(predicate.test(s))return true;return false;}
    public int top(Predicate<BlockState> p,int x,int z,int limit,int original,java.util.function.IntFunction<BlockState> actual){
        if(!enabled())return original;var list=columns.get((z&15)*16+(x&15));if(list==null)return original;
        int result=original;for(int i:list){int y=placements[i+1];if(y<=limit&&y+1>result&&p.test(actual.apply(y)))result=y+1;}return result;
    }
    public static boolean host(BlockState s){return s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE);}
    public void validate(TerrainProfile base){
        if(!enabled())return;if(!base.underground().owns(cx,cz)||!base.ores().owns(cx,cz))throw new IllegalArgumentException("Foundation column mismatch");if(!base.underground().enabled())throw new IllegalArgumentException("Dripstone requires caves");
        for(int i=0;i<placements.length;i+=3){int x=placements[i]&15,z=placements[i]>>4,y=placements[i+1],kind=placements[i+2],ground=base.groundHeight(x,z);
            if(!DripstoneHabitat.contains(seed,cx*16+x,y,cz*16+z,ground))throw new IllegalArgumentException("Dripstone outside its province/depth");
            var original=base.state(x,y,z);if(kind<2){if(!host(original))throw new IllegalArgumentException("Dripstone facing would replace nonhost/ore");}
            else{if(!original.isAir()||x<1||x>14||z<1||z>14||base.underground().hasApproach())throw new IllegalArgumentException("Pointed formation violates clearance");int step=(kind&1)==0?-1:1,root=y+step;
                if(definition==1){if(!base.state(x,root,z).isSolidRender())throw new IllegalArgumentException("Unsupported pointed formation");}
                else{
                    if(kind>=4&&values.get(key(z*16+x,y-step))!=kind-2)throw new IllegalArgumentException("Incomplete pointed chain");
                    int at=y;boolean supported=false;
                    for(int depth=0;depth<3;depth++){
                        int parent=at+step;var nativeParent=base.state(x,parent,z);
                        if(nativeParent.isSolidRender()){supported=true;break;}
                        int parentKind=values.get(key(z*16+x,parent));
                        int expected=kind<4?4+(kind&1):kind<6?6+(kind&1):-1;
                        if(parentKind!=expected)throw new IllegalArgumentException("Broken pointed chain");
                        kind=parentKind;at=parent;
                    }
                    if(!supported)throw new IllegalArgumentException("Unsupported pointed chain");
                }}
        }
    }
    public CompoundTag encode(PalettedContainerFactory factory){
        var tag=new CompoundTag();tag.putInt("definition",definition);tag.putLong("seed",seed);tag.putInt("column_x",cx);tag.putInt("column_z",cz);tag.putIntArray("placements",placements.clone());var palette=new PalettedContainer<>(biome,factory.biomeStrategy());tag.store("biome",factory.biomeContainerCodec(),palette);return tag;
    }
    public static DripstoneLayer decode(CompoundTag tag,PalettedContainerFactory factory){
        if(tag.getIntOr("definition",0)<1||tag.getIntOr("definition",0)>2)throw new IllegalArgumentException("Unsupported dripstone definition");var palette=factory.biomeContainerCodec().parse(NbtOps.INSTANCE,tag.getCompoundOrEmpty("biome")).getOrThrow();var holder=palette.get(0,0,0);if(!holder.is(net.minecraft.world.level.biome.Biomes.DRIPSTONE_CAVES))throw new IllegalArgumentException("Wrong foundation biome");
        for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)if(!palette.get(x,y,z).equals(holder))throw new IllegalArgumentException("Nonuniform foundation holder");return new DripstoneLayer(tag.getLong("seed").orElseThrow(),tag.getInt("column_x").orElseThrow(),tag.getInt("column_z").orElseThrow(),holder,tag.getIntArray("placements").orElseThrow(),tag.getIntOr("definition",0));
    }
    public interface Sampler {int groundHeight(int x,int z);BlockState state(int x,int y,int z);boolean protectedRoute();}
    public static DripstoneLayer plan(TerrainProfile base,long seed,int cx,int cz,Holder<Biome> biome){return plan(base,seed,cx,cz,biome,1);}
    public static DripstoneLayer plan(TerrainProfile base,long seed,int cx,int cz,Holder<Biome> biome,int definition){return plan(new Sampler(){public int groundHeight(int x,int z){return base.groundHeight(x,z);}public BlockState state(int x,int y,int z){return base.state(x,y,z);}public boolean protectedRoute(){return base.underground().hasApproach();}},seed,cx,cz,biome,definition);}
    public static DripstoneLayer plan(Sampler base,long seed,int cx,int cz,Holder<Biome> biome){return plan(base,seed,cx,cz,biome,1);}
    public static DripstoneLayer plan(Sampler base,long seed,int cx,int cz,Holder<Biome> biome,int definition){
        boolean possible=false;for(int z=0;z<4;z++)for(int x=0;x<4;x++)possible|=DripstoneHabitat.province(seed,cx*16+x*4,cz*16+z*4)>.2;
        var out=new TreeMap<Long,int[]>();if(possible){
            // Four sparse interior probes per 16m vertical band. Small clusters,
            // no wall-to-wall paint and no neighbor chunk/world reads.
            for(int band=0;band<15;band++)for(int sample=0;sample<4;sample++){
                long h=DripstoneHabitat.mix(seed^(long)cx*0x9E3779B97F4A7C15L^(long)cz*0xA24BAED4963EE407L^(long)band*0xD6E8FEB86659FD93L^sample*197L);
                int x=2+(int)Math.floorMod(h,12),z=2+(int)Math.floorMod(h>>>8,12),ground=base.groundHeight(x,z),start=ground-80-band*16;
                if(Math.abs(base.groundHeight(x+1,z)-ground)>8||Math.abs(base.groundHeight(x-1,z)-ground)>8||Math.abs(base.groundHeight(x,z+1)-ground)>8||Math.abs(base.groundHeight(x,z-1)-ground)>8)continue;
                for(int dy=0;dy<8;dy++){int y=start-dy;if(!DripstoneHabitat.contains(seed,cx*16+x,y,cz*16+z,ground))continue;var value=base.state(x,y,z);if(!host(value))continue;
                    boolean exposed=base.state(x,y+1,z).isAir()||base.state(x,y-1,z).isAir()||base.state(x+1,y,z).isAir()||base.state(x-1,y,z).isAir()||base.state(x,y,z+1).isAir()||base.state(x,y,z-1).isAir();if(!exposed)continue;
                    int kind=(h&7)==0?1:0;put(out,x,y,z,kind);
                    for(int d=-1;d<=1;d+=2){int xx=x+d;if(base.groundHeight(xx,z)-y>=68&&host(base.state(xx,y,z))&&DripstoneHabitat.contains(seed,cx*16+xx,y,cz*16+z,base.groundHeight(xx,z)))put(out,xx,y,z,kind);}
                    if(!base.protectedRoute()&&((h>>>16)&3)==0){
                        if(definition==2){addChain(out,base,seed,cx,cz,x,y,z,ground,h);}
                        else if(base.state(x,y+1,z).isAir()&&base.state(x,y+2,z).isAir()&&base.state(x,y+3,z).isAir()&&DripstoneHabitat.contains(seed,cx*16+x,y+1,cz*16+z,ground))put(out,x,y+1,z,2);
                        else if(base.state(x,y-1,z).isAir()&&base.state(x,y-2,z).isAir()&&base.state(x,y-3,z).isAir()&&DripstoneHabitat.contains(seed,cx*16+x,y-1,cz*16+z,ground))put(out,x,y-1,z,3);
                    }
                    break;
                }
            }
        }
        int[] values=new int[out.size()*3];int at=0;for(var v:out.values())for(int n:v)values[at++]=n;return new DripstoneLayer(seed,cx,cz,biome,values,definition);
    }
    private static void addChain(Map<Long,int[]> out,Sampler base,long seed,int cx,int cz,int x,int root,int z,int ground,long hash){
        int length=1+(int)((hash>>>20)%3);
        for(int step:new int[]{1,-1}){
            boolean clear=true;
            for(int part=1;part<=length+2;part++)if(!base.state(x,root+step*part,z).isAir()||out.containsKey(key(z*16+x,root+step*part)))clear=false;
            for(int part=1;part<=length;part++)if(out.containsKey(key(z*16+x,root+step*part))||!DripstoneHabitat.contains(seed,cx*16+x,root+step*part,cz*16+z,ground))clear=false;
            if(!clear||out.size()+length>MAX_PLACEMENTS)continue;
            for(int part=1;part<=length;part++){
                int kind=(part==length?2:part==length-1?4:6)+(step<0?1:0);
                put(out,x,root+step*part,z,kind);
            }
            return;
        }
    }
    private static void put(Map<Long,int[]> values,int x,int y,int z,int kind){if(values.size()<MAX_PLACEMENTS)values.putIfAbsent(key(z*16+x,y),new int[]{z*16+x,y,kind});}
}
