package blockbranch.realistic;

import java.util.*;
import java.util.function.IntBinaryOperator;

/** Isolated experiment, deliberately not on any production source set or save path. */
public final class RichNetworkRevision {
    public static final int CELL=256, BAND=192, CACHE_LIMIT=256;
    public record Point(double x,double y,double z) {}
    public record Segment(Point a,Point b,boolean walkableDesign,boolean entrance) {
        double length(){return Math.sqrt(sq(a.x-b.x)+sq(a.y-b.y)+sq(a.z-b.z));}
        boolean intersects(int x,int y,int z){
            return x+15>=Math.min(a.x,b.x)-4&&x<=Math.max(a.x,b.x)+4
                &&y+15>=Math.min(a.y,b.y)-4&&y<=Math.max(a.y,b.y)+5
                &&z+15>=Math.min(a.z,b.z)-4&&z<=Math.max(a.z,b.z)+4;
        }
        boolean contains(int x,int y,int z){
            double dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z;
            double t;
            if(walkableDesign){
                t=clamp(((x-a.x)*dx+(z-a.z)*dz)/(dx*dx+dz*dz));
                double r=2.6+.25*Math.sin((a.x+a.z+t*length())*.19);
                double d2=sq(x-a.x-t*dx)+sq(z-a.z-t*dz);
                double foot=Math.round(a.y+t*dy);
                return d2<r*r&&y>=foot&&y<foot+2.2+1.8*Math.sqrt(Math.max(0,1-d2/(r*r)));
            }
            t=clamp(((x-a.x)*dx+(y-a.y)*dy+(z-a.z)*dz)/(dx*dx+dy*dy+dz*dz));
            return sq(x-a.x-t*dx)+sq(y-a.y-t*dy)+sq(z-a.z-t*dz)<sq(2.2);
        }
    }
    private record Key(int x,int band,int z) {}
    public record NetworkId(int x,int z) {}
    public record Section(UndergroundStructure frozen,List<Segment> segments) {
        public boolean voidAt(int x,int y,int z,int surface){
            if(y<=TerrainShape.FLOOR+32||y>surface+5)return false;
            int ex=Math.floorMod(x,CELL),ez=Math.floorMod(z,CELL);
            int edge=Math.min(Math.min(ex,255-ex),Math.min(ez,255-ez));
            double closure=edge<8?(8-edge)*.075:0;
            // Retain frozen irregular voids inside each bounded ID, closing only
            // a narrow boundary strip. Explicit connections may cross that strip.
            if(frozen.density(x,y,z,surface)+closure<0)return true;
            // Avoid allocating an iterator for every individual block query.
            for(int i=0;i<segments.size();i++){
                Segment s=segments.get(i);
                if((s.entrance||y<surface-24)&&s.contains(x,y,z))return true;
            }
            return false;
        }
    }
    private final long seed;
    private final UndergroundStructure frozen;
    private final LinkedHashMap<Key,Section> sectionCache=new LinkedHashMap<>(32,.75f,true);
    private final IntBinaryOperator surface;
    // Per-worker experiment object: hard-cap every retained descriptor; no global graph.
    private final LinkedHashMap<Key,List<Segment>> bands=new LinkedHashMap<>(32,.75f,true);
    private final LinkedHashMap<NetworkId,Integer> anchors=new LinkedHashMap<>(32,.75f,true);
    public int peakDescriptors;
    public long constructedDescriptors;
    public RichNetworkRevision(long seed,IntBinaryOperator surface){this.seed=seed;this.surface=surface;this.frozen=new UndergroundStructure(seed);}
    public boolean entranceNetwork(int nx,int nz){
        int rx=Math.floorDiv(nx,4),rz=Math.floorDiv(nz,4),local=Math.floorMod(nx,4)+4*Math.floorMod(nz,4);
        int quota=new int[]{4,8,12}[(int)Math.floorMod(hash(rx,rz,59),3)];
        // Odd multiplier is a permutation of the sixteen positions: exact regional quota.
        int rank=(local*(2*(int)(hash(rx,rz,61)&7)+1)+(int)(hash(rx,rz,67)&15))&15;
        return rank<quota;
    }
    public int anchorSurface(int nx,int nz){
        NetworkId key=new NetworkId(nx,nz);Integer value=anchors.get(key);
        if(value==null){value=surface.applyAsInt(nx*CELL+128,nz*CELL+128);anchors.put(key,value);
            if(anchors.size()>CACHE_LIMIT)anchors.remove(anchors.keySet().iterator().next());}
        return value;
    }
    private Point node(int nx,int band,int nz,int node){
        double x=node==0?96+64*unit(hash(nx,nz,band*65537L)):36+184*unit(hash(nx,nz,band*65537L+node*17L));
        double z=node==0?96+64*unit(hash(nx,nz,band*65537L+3)):36+184*unit(hash(nx,nz,band*65537L+node*17L+3));
        return new Point(nx*CELL+x,anchorSurface(nx,nz)-64-band*BAND+
            Math.round(5*(unit(hash(nx,nz,band*257L+node*7L+13))-.5)),nz*CELL+z);
    }
    private void curved(List<Segment> result,Point a,Point b,boolean walk,long salt,int nx,int nz){
        double dx=b.x-a.x,dz=b.z-a.z,span=Math.hypot(dx,dz);
        double bend=(unit(hash(nx,nz,salt))-.5)*Math.min(24,span*.5);
        Point middle=new Point((a.x+b.x)/2+(span==0?0:-dz/span*bend),
            (a.y+b.y)/2,(a.z+b.z)/2+(span==0?0:dx/span*bend));
        result.add(new Segment(a,middle,walk,false));result.add(new Segment(middle,b,walk,false));
    }
    public List<Segment> band(int nx,int band,int nz){
        if(band<0||anchorSurface(nx,nz)-64-band*BAND<=TerrainShape.FLOOR+64)return List.of();
        Key key=new Key(nx,band,nz);List<Segment> found=bands.get(key);if(found!=null)return found;
        var result=new ArrayList<Segment>();
        // A seeded local minimum-spanning tree: irregular branches, no polygon loop.
        // Construction is bounded (18 nodes), independent of generated neighbours.
        boolean[] joined=new boolean[18];joined[0]=true;
        Point[] points=new Point[18];
        for(int i=0;i<18;i++)points[i]=node(nx,band,nz,i);
        for(int edge=1;edge<18;edge++){
            int from=-1,to=-1;double best=Double.POSITIVE_INFINITY;
            for(int i=0;i<18;i++)if(joined[i])for(int j=0;j<18;j++)if(!joined[j]){
                Point a=points[i],b=points[j];
                double d=sq(a.x-b.x)+sq(a.y-b.y)+sq(a.z-b.z);
                if(d<best){best=d;from=i;to=j;}
            }
            joined[to]=true;
            curved(result,points[from],points[to],true,band*719L+edge,nx,nz);
        }
        Point a=node(nx,band,nz,0),b=node(nx,band+1,nz,0);
        if(b.y>TerrainShape.FLOOR+64){
            Point bend=new Point((a.x+b.x)/2+48*(unit(hash(nx,nz,band*251L+5))-.5),
                (a.y+b.y)/2,(a.z+b.z)/2+48*(unit(hash(nx,nz,band*251L+7))-.5));
            result.add(new Segment(a,bend,false,false));result.add(new Segment(bend,b,false,false));
        }
        if(band==0&&entranceNetwork(nx,nz)&&anchorSurface(nx,nz)>=64){
            int mx=nx*CELL+48,mz=nz*CELL+48;
            Point mouth=new Point(mx,surface.applyAsInt(mx,mz)+1,mz);
            Point bend=new Point(nx*CELL+110,Math.round((mouth.y+a.y)/2),nz*CELL+65);
            result.add(new Segment(a,bend,true,true));result.add(new Segment(bend,mouth,true,true));
        }
        // Occasional reciprocal east/west connection at one bounded top band.
        if(band==0){
            if((hash(nx,nz,101)&7)==0)result.add(new Segment(node(nx,0,nz,0),node(nx+1,0,nz,4),true,false));
            if((hash(nx-1,nz,101)&7)==0)result.add(new Segment(node(nx-1,0,nz,0),node(nx,0,nz,4),true,false));
        }
        found=List.copyOf(result);bands.put(key,found);constructedDescriptors++;
        if(bands.size()>CACHE_LIMIT)bands.remove(bands.keySet().iterator().next());
        peakDescriptors=Math.max(peakDescriptors,bands.size());return found;
    }
    /** At most nine cells and three vertical bands; caller reuses this list for 4096 blocks. */
    public Section section(int sx,int sy,int sz){
        int x=sx*16,y=sy*16,z=sz*16,nx=Math.floorDiv(x,CELL),nz=Math.floorDiv(z,CELL);
        var result=new ArrayList<Segment>();
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            int gx=nx+dx,gz=nz+dz,base=Math.floorDiv(anchorSurface(gx,gz)-64-y,BAND);
            for(int db=-1;db<=1;db++)for(Segment s:band(gx,base+db,gz))if(s.intersects(x,y,z))result.add(s);
        }
        return new Section(frozen,List.copyOf(result));
    }
    public boolean voidAt(int x,int y,int z,int surface){
        Key key=new Key(Math.floorDiv(x,16),Math.floorDiv(y,16),Math.floorDiv(z,16));
        Section found=sectionCache.get(key);
        if(found==null){found=section(key.x,key.band,key.z);sectionCache.put(key,found);
            if(sectionCache.size()>CACHE_LIMIT)sectionCache.remove(sectionCache.keySet().iterator().next());}
        return found.voidAt(x,y,z,surface);
    }
    public int retainedSections(){return sectionCache.size();}
    public int retainedDescriptors(){return bands.size();}
    private long hash(int x,int z,long salt){long h=seed^x*0x9E3779B97F4A7C15L^z*0xD6E8FEB86659FD93L^salt*0xC2B2AE3D27D4EB4FL;h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;return h^(h>>>31);}
    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
    private static double sq(double x){return x*x;}
    private static double clamp(double x){return Math.max(0,Math.min(1,x));}
}
