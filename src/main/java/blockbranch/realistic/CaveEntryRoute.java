package blockbranch.realistic;

import java.util.*;

/** Finite local continuation over existing cave floors; only its walking headroom is cleared. */
public final class CaveEntryRoute {
    public static final CaveEntryRoute NONE=new CaveEntryRoute(new int[0]);
    public static final int MAX_STEPS=32, RADIUS=24, MAX_VISITS=768;
    private final int[] points;
    private final int minX,maxX,minZ,maxZ,lowY,highY;
    public CaveEntryRoute(int[] data){
        if(data.length!=0&&(data.length<21||data.length%3!=0||data.length>3*(MAX_STEPS+1)))throw new IllegalArgumentException("Invalid entry route length");
        points=data.clone();int x0=Integer.MAX_VALUE,x1=Integer.MIN_VALUE,z0=x0,z1=x1,y0=x0,y1=x1;var seen=new HashSet<Long>();
        for(int i=0;i<points.length;i+=3){
            int x=points[i],y=points[i+1],z=points[i+2];
            x0=Math.min(x0,x);x1=Math.max(x1,x);z0=Math.min(z0,z);z1=Math.max(z1,z);y0=Math.min(y0,y);y1=Math.max(y1,y+3);
            if(Math.abs((long)x)>30000000||Math.abs((long)z)>30000000||y<=TerrainShape.FLOOR+32||y>=TerrainShape.CEILING-4)throw new IllegalArgumentException("Entry route outside terrain envelope");
            if(!seen.add(key(x,z)))throw new IllegalArgumentException("Entry route revisits a floor column");
            if(i>0&&(Math.abs(x-points[i-3])+Math.abs(z-points[i-1])!=1||Math.abs(y-points[i-2])>1))throw new IllegalArgumentException("Entry route lacks local walking steps");
            if(Math.floorDiv(x,384)!=Math.floorDiv(points[0],384)||Math.floorDiv(z,384)!=Math.floorDiv(points[2],384))throw new IllegalArgumentException("Entry route crosses planning cell");
            if(Math.max(Math.abs(x-points[0]),Math.abs(z-points[2]))>RADIUS)throw new IllegalArgumentException("Unbounded entry route");
        }
        minX=x0;maxX=x1;minZ=z0;maxZ=z1;lowY=y0;highY=y1;
    }
    public boolean present(){return points.length>0;}
    public int[] encode(){return points.clone();}
    public boolean column(int x,int z){if(x<minX||x>maxX||z<minZ||z>maxZ)return false;for(int i=0;i<points.length;i+=3)if(points[i]==x&&points[i+2]==z)return true;return false;}
    public boolean contains(int x,int y,int z){
        if(x<minX||x>maxX||z<minZ||z>maxZ||y<lowY||y>highY)return false;
        for(int i=0;i<points.length;i+=3)if(points[i]==x&&points[i+2]==z){
            int top=points[i+1]+2;
            if(i>0)top=Math.max(top,points[i-2]+2);
            if(i+3<points.length)top=Math.max(top,points[i+4]+2);
            return y>=points[i+1]&&y<=top;
        }return false;
    }
    public boolean intersects(int cx,int cz){for(int i=0;i<points.length;i+=3)if(points[i]>>4==cx&&points[i+2]>>4==cz)return true;return false;}
    public int minY(){return lowY;}
    public int maxY(){return highY;}
    public List<int[]> route(){var out=new ArrayList<int[]>();for(int i=0;i<points.length;i+=3)out.add(new int[]{points[i],points[i+1],points[i+2]});return List.copyOf(out);}
    private record P(int x,int y,int z){}
    private record Visit(P p,int depth){}
    private static long key(int x,int z){return(x&0xffffffffL)|((long)z<<32);}
    public static CaveEntryRoute plan(CaveApproach approach,DrainageNetwork terrain,UndergroundStructure field){
        if(!approach.present())return NONE;var a=approach.encode();var start=new P(a[9],a[10],a[11]);int cellX=Math.floorDiv(start.x,384),cellZ=Math.floorDiv(start.z,384);
        var parents=new LinkedHashMap<P,P>();var queue=new ArrayDeque<Visit>();var heights=new HashMap<Long,Integer>();parents.put(start,null);queue.add(new Visit(start,0));
        while(!queue.isEmpty()&&parents.size()<=MAX_VISITS){
            var v=queue.remove();var p=v.p;
            if(v.depth>=6&&Math.hypot(p.x-start.x,p.z-start.z)>=5&&chamber(p,terrain,field,heights,approach)){
                var path=new ArrayList<P>();for(P q=p;q!=null;q=parents.get(q))path.add(q);Collections.reverse(path);int[] data=new int[path.size()*3];
                for(int i=0;i<path.size();i++){var q=path.get(i);data[i*3]=q.x;data[i*3+1]=q.y;data[i*3+2]=q.z;}
                try{return new CaveEntryRoute(data);}catch(IllegalArgumentException rejected){continue;}
            }
            if(v.depth==MAX_STEPS)continue;
            for(int[] d:new int[][]{{1,0},{0,1},{-1,0},{0,-1}}){int x=p.x+d[0],z=p.z+d[1];
                if(Math.max(Math.abs(x-start.x),Math.abs(z-start.z))>RADIUS||Math.floorDiv(x,384)!=cellX||Math.floorDiv(z,384)!=cellZ)continue;
                for(int dy:new int[]{0,-1,1}){var q=new P(x,p.y+dy,z);if(Math.abs(q.y-start.y)>12||!floor(q,terrain,field,heights,approach))continue;
                    if(parents.size()<MAX_VISITS&&!parents.containsKey(q)){parents.put(q,p);queue.add(new Visit(q,v.depth+1));}break;}
            }
        }return NONE;
    }
    private static boolean floor(P p,DrainageNetwork terrain,UndergroundStructure field,Map<Long,Integer> heights,CaveApproach approach){
        int ground=heights.computeIfAbsent(key(p.x,p.z),k->{var s=terrain.sample(p.x,p.z);return s.waterLevel()==DrainageNetwork.DRY?s.height():Integer.MIN_VALUE;});
        return ground!=Integer.MIN_VALUE&&!approach.contains(p.x,p.y-1,p.z)&&p.y<=ground-16&&field.voidAt(p.x,p.y,p.z,ground)&&field.voidAt(p.x,p.y+1,p.z,ground)&&!field.voidAt(p.x,p.y-1,p.z,ground);
    }
    private static boolean chamber(P p,DrainageNetwork terrain,UndergroundStructure field,Map<Long,Integer> heights,CaveApproach approach){
        int open=0;for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++)for(int dy:new int[]{0,-1,1})if(floor(new P(p.x+x,p.y+dy,p.z+z),terrain,field,heights,approach)){open++;break;}
        if(open>=7)return true;
        int exits=0;for(int[] d:new int[][]{{1,0},{0,1},{-1,0},{0,-1}})for(int dy:new int[]{0,-1,1})if(floor(new P(p.x+d[0],p.y+dy,p.z+d[1]),terrain,field,heights,approach)){exits++;break;}
        return exits>=3;
    }
}
