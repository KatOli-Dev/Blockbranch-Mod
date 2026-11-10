package blockbranch.realistic;

import java.util.*;
import java.util.function.BiPredicate;

/** Optional short approaches to existing caves. Never closes or fills natural voids. */
public final class CaveApproach {
    public static final int CELL=384;
    public static final CaveApproach NONE=new CaveApproach(new int[0]);
    private final int[] points;
    private final CaveEntryRoute entry;
    private final int minX,maxX,minY,maxY,minZ,maxZ;
    public CaveApproach(int[] points){this(points,CaveEntryRoute.NONE);}
    public CaveApproach(int[] points,CaveEntryRoute entry){
        this.entry=entry;
        if(entry.present()&&(points.length!=12||!java.util.Arrays.equals(java.util.Arrays.copyOfRange(points,9,12),entry.route().getFirst())))throw new IllegalArgumentException("Entry route must start at approach endpoint");
        if(points.length!=0&&points.length!=12)throw new IllegalArgumentException("Approach requires four control points");
        this.points=points.clone();
        int x0=Integer.MAX_VALUE,x1=Integer.MIN_VALUE,y0=x0,y1=x1,z0=x0,z1=x1;
        for(int i=0;i<points.length;i+=3){
            int x=points[i],y=points[i+1],z=points[i+2];
            if(Math.abs((long)x)>30000000||Math.abs((long)z)>30000000||y<=TerrainShape.FLOOR+32||y>=TerrainShape.CEILING)throw new IllegalArgumentException("Approach outside terrain envelope");
            x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);z0=Math.min(z0,z);z1=Math.max(z1,z);
            if(i>0){double horizontal=Math.hypot(x-points[i-3],z-points[i-1]);if(horizontal<1||Math.abs(y-points[i-2])>horizontal*.85)throw new IllegalArgumentException("Approach lacks a bounded walking slope");}
        }
        if(points.length>0&&(x1-x0>288||z1-z0>288||y1-y0>160))throw new IllegalArgumentException("Unbounded approach");
        minX=x0;maxX=x1;minY=y0;maxY=y1;minZ=z0;maxZ=z1;
    }
    public boolean present(){return points.length!=0;}
    public boolean affectsColumn(int x,int z){return present()&&(nearPath(x,z,2.4)||entry.column(x,z));}
    public CaveEntryRoute entry(){return entry;}
    public CaveApproach extend(DrainageNetwork terrain,UndergroundStructure field){var route=CaveEntryRoute.plan(this,terrain,field);return route.present()?new CaveApproach(points,route):NONE;}
    public List<int[]> walkingRoute(DrainageNetwork terrain,UndergroundStructure field){var result=new ArrayList<>(route(terrain,field));if(entry.present()){var continuation=entry.route();result.addAll(continuation.subList(1,continuation.size()));}return List.copyOf(result);}
    public int[] encode(){return points.clone();}
    public int minY(){return entry.present()?Math.min(minY,entry.minY()):minY;}
    public int maxY(){return entry.present()?Math.max(maxY+7,entry.maxY()):maxY+7;}
    public boolean intersectsColumn(int cx,int cz){return entry.intersects(cx,cz)||present()&&cx*16L+15>=minX-3L&&cx*16L<=maxX+3L&&cz*16L+15>=minZ-3L&&cz*16L<=maxZ+3L;}
    public boolean contains(int x,int y,int z){
        if(entry.contains(x,y,z))return true;
        if(!present()||y<minY||y>maxY+4||x<minX-3||x>maxX+3||z<minZ-3||z>maxZ+3)return false;
        for(int i=3;i<points.length;i+=3){
            double dx=points[i]-points[i-3],dz=points[i+2]-points[i-1];
            double t=Math.clamp(((x-points[i-3])*dx+(z-points[i-1])*dz)/(dx*dx+dz*dz),0,1);
            double distance=sq(x-points[i-3]-t*dx)+sq(z-points[i-1]-t*dz);
            double radius=2.2+.15*Math.sin((x+z)*.23);
            if(distance>=radius*radius)continue;
            int foot=(int)Math.round(points[i-2]+t*(points[i+1]-points[i-2]));
            if(y>=foot&&y<foot+2.2+1.3*Math.sqrt(1-distance/(radius*radius)))return true;
        }
        return false;
    }
    /** Surface vegetation must not remain above terrain removed by the mouth. */
    public boolean clears(int x,int y,int z,int ground){
        return contains(x,y,z)||(y>ground&&y<=ground+3&&contains(x,ground,z));
    }
    private boolean nearPath(int x,int z,double radius){
        for(int i=3;i<points.length;i+=3){double dx=points[i]-points[i-3],dz=points[i+2]-points[i-1];double t=Math.clamp(((x-points[i-3])*dx+(z-points[i-1])*dz)/(dx*dx+dz*dz),0,1);
            if(sq(x-points[i-3]-t*dx)+sq(z-points[i-1]-t*dz)<radius*radius)return true;}
        return false;
    }
    /** A bounded candidate site, not a cave-system identity or entrance quota. */
    public static CaveApproach plan(long seed,int cellX,int cellZ,DrainageNetwork terrain,BiPredicate<Integer,Integer> rootBlocked){
        return plan(seed,cellX,cellZ,terrain,rootBlocked,false);
    }
    /** Survey the same candidate before selection; this does not generate a new entrance. */
    static CaveApproach eligibleSite(long seed,int cellX,int cellZ,DrainageNetwork terrain,BiPredicate<Integer,Integer> rootBlocked){
        return plan(seed,cellX,cellZ,terrain,rootBlocked,true);
    }
    private static CaveApproach plan(long seed,int cellX,int cellZ,DrainageNetwork terrain,BiPredicate<Integer,Integer> rootBlocked,boolean survey){
        long h=hash(seed,cellX,cellZ);
        if(!survey&&(h&1)==0)return NONE;
        int mx=cellX*CELL+144+(int)((h>>>8)%96),mz=cellZ*CELL+144+(int)((h>>>16)%96);
        var mouth=terrain.sample(mx,mz);if(mouth.waterLevel()!=DrainageNetwork.DRY)return NONE;
        var field=new UndergroundStructure(seed);
        int[] dx={1,1,0,-1,-1,-1,0,1},dz={0,1,1,1,0,-1,-1,-1};
        for(int direction=0;direction<8;direction++){
            int d=(direction+(int)((h>>>24)&7))&7;
            int ex=mx+dx[d]*(dx[d]*dz[d]==0?120:88),ez=mz+dz[d]*(dx[d]*dz[d]==0?120:88);
            int ground=terrain.sample(ex,ez).height();
            for(int depth:new int[]{64,56,72,48,80,88,96})for(int delta=-3;delta<=3;delta++){
                int ey=ground-depth+delta;
                if(!field.voidAt(ex,ey,ez,ground)||!field.voidAt(ex,ey+1,ez,ground)||field.voidAt(ex,ey-1,ez,ground))continue;
                int bend=(int)((h>>>32)&15)-7;
                int[] p={mx,mouth.height()+1,mz,
                    mx+(ex-mx)/3+dz[d]*bend,mouth.height()+1+(ey-mouth.height()-1)/3,mz+(ez-mz)/3-dx[d]*bend,
                    mx+2*(ex-mx)/3-dz[d]*bend, mouth.height()+1+2*(ey-mouth.height()-1)/3,mz+2*(ez-mz)/3+dx[d]*bend,
                    ex,ey,ez};
                CaveApproach candidate;
                try{candidate=new CaveApproach(p);}catch(IllegalArgumentException invalid){continue;}
                if(candidate.minX-4<cellX*CELL||candidate.maxX+4>cellX*CELL+CELL-1||candidate.minZ-4<cellZ*CELL||candidate.maxZ+4>cellZ*CELL+CELL-1)continue;
                if(candidate.route(terrain,field).isEmpty())continue;
                boolean safe=true;
                // Check every affected column plus a water buffer, not only the centreline.
                for(int z=candidate.minZ-4;z<=candidate.maxZ+4&&safe;z++)for(int x=candidate.minX-4;x<=candidate.maxX+4;x++)if(candidate.nearPath(x,z,4.2)){
                    var sample=terrain.sample(x,z);
                    if(sample.waterLevel()!=DrainageNetwork.DRY||terrain.channelAt(x,z).edge()>=0){safe=false;break;}
                    if((candidate.contains(x,sample.height(),z)||candidate.contains(x,sample.height()+1,z))&&rootBlocked.test(x,z)){safe=false;break;}
                }
                if(safe)return candidate;
            }
        }
        return NONE;
    }
    /** Supported voxel-centre route; native movement remains a separate check. */
    public List<int[]> route(DrainageNetwork terrain,UndergroundStructure field){
        if(!present())return List.of();
        var route=new ArrayList<int[]>();int previousY=points[1];
        for(int i=3;i<points.length;i+=3){
            double length=Math.hypot(points[i]-points[i-3],points[i+2]-points[i-1]);int steps=(int)Math.ceil(length*2);
            for(int k=0;k<=steps;k++){
                double t=k/(double)steps;int x=(int)Math.round(points[i-3]+t*(points[i]-points[i-3])),z=(int)Math.round(points[i-1]+t*(points[i+2]-points[i-1]));
                int foot=(int)Math.round(points[i-2]+t*(points[i+1]-points[i-2]));var sample=terrain.sample(x,z);
                if(sample.waterLevel()!=DrainageNetwork.DRY)return List.of();
                int found=Integer.MIN_VALUE;
                for(int delta:new int[]{0,-1,1,-2,2}){int y=foot+delta;
                    if(Math.abs(y-previousY)>1||y-1>sample.height())continue;
                    if(!air(x,y-1,z,sample.height(),field)&&air(x,y,z,sample.height(),field)&&air(x,y+1,z,sample.height(),field)){found=y;break;}}
                if(found==Integer.MIN_VALUE)return List.of();
                if(route.isEmpty()||route.getLast()[0]!=x||route.getLast()[1]!=found||route.getLast()[2]!=z)route.add(new int[]{x,found,z});previousY=found;
            }
        }
        return List.copyOf(route);
    }
    private boolean air(int x,int y,int z,int ground,UndergroundStructure field){return y>ground||contains(x,y,z)||field.voidAt(x,y,z,ground);}
    private static long hash(long seed,int x,int z){long h=seed^x*0x9E3779B97F4A7C15L^z*0xD6E8FEB86659FD93L;h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;return h^(h>>>31);}
    private static double sq(double x){return x*x;}
}
