package blockbranch.realistic;

import java.util.*;
import java.util.function.IntBinaryOperator;

/** Isolated experiment, deliberately not on any production source set or save path. */
public final class NetworkCavePrototype {
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
    public record Section(List<Segment> segments) {
        public boolean voidAt(int x,int y,int z,int surface){
            if(y<=TerrainShape.FLOOR+32||y>surface+5)return false;
            // Avoid allocating an iterator for every individual block query.
            for(int i=0;i<segments.size();i++){
                Segment s=segments.get(i);
                if((s.entrance||y<surface-24)&&s.contains(x,y,z))return true;
            }
            return false;
        }
    }
    private final long seed;
    private final IntBinaryOperator surface;
    // Per-worker experiment object: hard-cap every retained descriptor; no global graph.
    private final LinkedHashMap<Key,List<Segment>> bands=new LinkedHashMap<>(32,.75f,true);
    private final LinkedHashMap<NetworkId,Integer> anchors=new LinkedHashMap<>(32,.75f,true);
    public int peakDescriptors;
    public long constructedDescriptors;
    public NetworkCavePrototype(long seed,IntBinaryOperator surface){this.seed=seed;this.surface=surface;}
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
        double angle=node*Math.PI/4;
        double radius=72+unit(hash(nx,nz,band*41L+node))*9;
        return new Point(nx*CELL+128+Math.cos(angle)*radius,
            anchorSurface(nx,nz)-64-band*BAND+Math.round(3*Math.sin(node*1.7+unit(hash(nx,nz,7))*6)),
            nz*CELL+128+Math.sin(angle)*radius);
    }
    public List<Segment> band(int nx,int band,int nz){
        if(band<0||anchorSurface(nx,nz)-64-band*BAND<=TerrainShape.FLOOR+64)return List.of();
        Key key=new Key(nx,band,nz);List<Segment> found=bands.get(key);if(found!=null)return found;
        var result=new ArrayList<Segment>();
        for(int i=0;i<8;i++)result.add(new Segment(node(nx,band,nz,i),node(nx,band,nz,(i+1)&7),true,false));
        Point a=node(nx,band,nz,0),b=node(nx,band+1,nz,0);
        if(b.y>TerrainShape.FLOOR+64)result.add(new Segment(a,b,false,false));
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
        return new Section(List.copyOf(result));
    }
    public int retainedDescriptors(){return bands.size();}
    private long hash(int x,int z,long salt){long h=seed^x*0x9E3779B97F4A7C15L^z*0xD6E8FEB86659FD93L^salt*0xC2B2AE3D27D4EB4FL;h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;return h^(h>>>31);}
    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
    private static double sq(double x){return x*x;}
    private static double clamp(double x){return Math.max(0,Math.min(1,x));}
}
