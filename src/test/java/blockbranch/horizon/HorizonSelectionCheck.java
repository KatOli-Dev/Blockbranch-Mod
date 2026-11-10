package blockbranch.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import java.util.*;
/** Quadtree keys and level-of-detail selection; no Minecraft classes are loaded. */
public final class HorizonSelectionCheck {
    public static void main(String[] args) {
        keys();
        sampling();
        var none=(HorizonSelection.NativeCoverage)(x,z)->false;
        for(double[] c:new double[][]{{0,0},{-1234.5,987.25},{1_000_003,-2_000_017}}) {
            var v=new HorizonSelection.View(c[0],c[1],160,2048,64);
            long[] wanted=HorizonSelection.wanted(v,none);
            require(wanted.length>0&&wanted.length<=HorizonRequest.MAX_KEYS,"wanted count "+wanted.length+" fits one request");
            disjoint(wanted,"wanted");
            var levels=new int[MAX_LEVEL+1];for(long k:wanted)levels[level(k)]++;
            int used=0;for(int n:levels)if(n>0)used++;
            require(used>=5,"detail falls off over at least five levels: "+java.util.Arrays.toString(levels));
            System.out.println("HORIZON_SELECTION wanted="+wanted.length+" perLevel="+Arrays.toString(levels));
            for(long k:wanted) {
                double d=distance(k,v.x(),v.z());
                require(d<v.maxRadius(),"within max radius");
                require(level(k)==0||d>=v.nativeRadius(),"only level 0 reaches into the native radius");
                require(level(k)==0||d>=v.nativeRadius()+v.quality()*(1<<(level(k)-1))-1e-9,"level falls off with distance");
            }
            for(int i=1;i<wanted.length;i++)require(distance(wanted[i-1],v.x(),v.z())/span(level(wanted[i-1]))<=distance(wanted[i],v.x(),v.z())/span(level(wanted[i]))+1e-9,"priority order");
            // Covering the whole native disc removes every level-0 tile it covers.
            int cx=(int)Math.floor(c[0])>>4,cz=(int)Math.floor(c[1])>>4;
            HorizonSelection.NativeCoverage disc=(x,z)->(long)(x-cx)*(x-cx)+(long)(z-cz)*(z-cz)<=100;
            for(long k:HorizonSelection.wanted(v,disc))require(level(k)>0||!disc.covers(x(k),z(k)),"covered chunks are not requested");

            // Everything ready: drawing equals the ideal set plus nothing overlapping native terrain.
            var all=set(wanted);
            var drawn=HorizonSelection.drawn(v,all::contains,none);
            require(set(drawn).equals(all),"ready tiles are drawn exactly");
            disjoint(drawn,"drawn");
            // Only coarse tiles ready: parents stand in outside the native area, never inside it.
            var coarse=new HashSet<Long>();for(long k:all){long p=k;while(level(p)<MAX_LEVEL)p=parent(p);coarse.add(p);}
            var partial=HorizonSelection.drawn(v,coarse::contains,none);
            disjoint(partial,"partial");
            for(long k:partial)require(level(k)==MAX_LEVEL&&(distance(k,v.x(),v.z())>=v.nativeRadius()+16||!HorizonSelection.split(k,distance(k,v.x(),v.z()),v)),"stand-ins stay outside native terrain");
            require(partial.length>0,"coarse tiles fill the horizon before refinement");
            // Nothing ready: nothing drawn.
            require(HorizonSelection.drawn(v,k->false,none).length==0,"no data, no tiles");
        }
        // The level-0 ring meets the native edge: a chunk straddling the native radius is wanted.
        var v=new HorizonSelection.View(8,8,160,2048,64);var set=set(HorizonSelection.wanted(v,(x,z)->false));
        require(set.contains(key(0,10,0)),"chunk at the native edge is a level-0 tile");
        require(!set.contains(key(0,0,0)),"camera chunk is left to native terrain");
        // A high camera: native terrain never reaches the ground inside its radius, so the
        // horizon requests and draws those chunks at block resolution too.
        var high=new HorizonSelection.NativeCoverage(){public boolean covers(int x,int z){return false;}public boolean expected(int x,int z){return false;}};
        var under=set(HorizonSelection.wanted(v,high));
        require(under.contains(key(0,0,0))&&under.contains(key(0,3,-2)),"unreached chunks under a high camera are requested");
        require(set(HorizonSelection.drawn(v,under::contains,high)).equals(under),"and drawn");
        System.out.println("HORIZON_SELECTION highCamera wanted="+under.size());
        System.out.println("PASS");
    }
    private static void keys() {
        for(int level=0;level<=MAX_LEVEL;level++)for(int x:new int[]{0,1,-1,12345,-12345,(1<<27)-1,-(1<<27)})for(int z:new int[]{0,-7,99999,-(1<<27)}) {
            long k=key(level,x,z);
            require(level(k)==level&&x(k)==x&&z(k)==z,"key roundtrip "+level+" "+x+" "+z);
            if(level>0&&valid(k))for(int q=0;q<4;q++)require(parent(child(k,q))==k,"child/parent "+level+" "+x+" "+z+" q"+q);
        }
        require(originX(key(2,-1,0))==-64&&span(2)==64&&cell(2)==4,"origin and span");
        require(sampleCoordinate(0,0,1)==0&&sampleCoordinate(0,0,0)==-1&&sampleCoordinate(0,2,1)==2,"sample centres");
        require(valid(key(0,1_874_999,0))&&!valid(key(6,30_000,0))&&!valid(-1L),"world-border bounds");
        require(distance(key(0,0,0),8,8)==0&&distance(key(0,1,0),8,8)==8,"tile distance");
    }
    /** No block column belongs to two tiles. */
    private static void disjoint(long[] keys,String what) {
        var sorted=keys.clone();java.util.Arrays.sort(sorted);
        var set=set(keys);require(set.size()==keys.length,what+" has no duplicates");
        for(long k:keys)for(long p=k;level(p)<MAX_LEVEL;){p=parent(p);require(!set.contains(p),what+" overlaps an ancestor");}
    }
    private static Set<Long> set(long[] keys){var s=new HashSet<Long>();for(long k:keys)s.add(k);return s;}
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    /** Tiles refreshed for a changed chunk are exactly those with a sample inside it. */
    private static void sampling() {
        for(int[] c:new int[][]{{0,0},{-1,-1},{7,-13},{-4097,2050},{62,63}}) {
            var found=set(HorizonTiles.sampling(c[0],c[1]));var expected=new HashSet<Long>();
            int x0=c[0]*16,z0=c[1]*16;
            for(int l=0;l<=MAX_LEVEL;l++){int s=span(l);
                for(int tz=Math.floorDiv(z0,s)-2;tz<=Math.floorDiv(z0,s)+2;tz++)for(int tx=Math.floorDiv(x0,s)-2;tx<=Math.floorDiv(x0,s)+2;tx++){
                    long k=key(l,tx,tz);boolean hit=false;
                    for(int j=0;j<SAMPLES&&!hit;j++)for(int i=0;i<SAMPLES&&!hit;i++){int x=sampleCoordinate(originX(k),l,i),z=sampleCoordinate(originZ(k),l,j);hit=x>=x0&&x<x0+16&&z>=z0&&z<z0+16;}
                    if(hit)expected.add(k);
                }}
            require(found.equals(expected),"chunk "+Arrays.toString(c)+" sampling tiles "+found.size()+" match brute force "+expected.size());
        }
    }
}
