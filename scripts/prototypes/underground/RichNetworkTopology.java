package blockbranch.realistic;

import java.util.*;
import java.nio.file.*;

/** Read-only dense sampling of the rich revision; frozen native counts independently cross-checked. No Minecraft bootstrap. */
public final class RichNetworkTopology {
    static final int N=96,H=2,S=N+2*H,V=N*N*N;
    static final int[][] D={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    final long seed;final RichNetworkRevision field;final DrainageNetwork terrain;
    final int ox,oy,oz;final byte[] halo=new byte[S*S*S],air=new byte[V],body=new byte[V],floor=new byte[V];
    int minGround=Integer.MAX_VALUE,maxGround=Integer.MIN_VALUE;
    RichNetworkTopology(long seed,DrainageNetwork terrain,int cx,int cy,int cz){
        this.seed=seed;this.terrain=terrain;field=new RichNetworkRevision(seed,(x,z)->terrain.sample(x,z).height());ox=cx-N/2;oy=cy-N/2;oz=cz-N/2;
        for(int z=-H;z<N+H;z++)for(int x=-H;x<N+H;x++){
            int ground=terrain.sample(ox+x,oz+z).height();minGround=Math.min(minGround,ground);maxGround=Math.max(maxGround,ground);
            for(int y=-H;y<N+H;y++)if(field.voidAt(ox+x,oy+y,oz+z,ground))halo[hi(x,y,z)]=1;
        }
        // In this band only native deepslate can be carved; no surface material,
        // water, terrain envelope or protected-boundary approximation is used.
        if(oy+N+H>=0||minGround-(oy+N+H)<64||oy-H<=TerrainShape.FLOOR+32)throw new AssertionError("Sample leaves unambiguous deep-rock band");
        for(int i=0;i<V;i++){int x=x(i),y=y(i),z=z(i);if(a(x,y,z)){air[i]=1;if(a(x,y+1,z)){body[i]=1;if(!a(x,y-1,z))floor[i]=1;}}}
    }
    static int id(int x,int y,int z){return (y*N+z)*N+x;}
    static int x(int i){return i%N;} static int y(int i){return i/(N*N);}static int z(int i){return(i/N)%N;}
    static boolean inside(int x,int y,int z){return x>=0&&x<N&&y>=0&&y<N&&z>=0&&z<N;}
    static int hi(int x,int y,int z){return ((y+H)*S+(z+H))*S+(x+H);}
    boolean a(int x,int y,int z){return halo[hi(x,y,z)]!=0;}
    boolean externalAir(int x,int y,int z){
        if(x>=-H&&x<N+H&&y>=-H&&y<N+H&&z>=-H&&z<N+H)return a(x,y,z);
        return field.voidAt(ox+x,oy+y,oz+z,terrain.sample(ox+x,oz+z).height());
    }
    static class C{int n,minX=N,minY=N,minZ=N,maxX=-1,maxY=-1,maxZ=-1;boolean edge;}
    record Labels(int[] ids,List<C> comps,int total,int largest){}
    Labels label(byte[] mask,boolean platforms,boolean steps){
        int[] ids=new int[V],queue=new int[V];var comps=new ArrayList<C>();comps.add(null);int total=0,largest=0;
        for(int start=0;start<V;start++)if(mask[start]!=0&&ids[start]==0){
            C c=new C();int ci=comps.size(),head=0,tail=0;comps.add(c);queue[tail++]=start;ids[start]=ci;
            while(head<tail){int i=queue[head++],x=x(i),y=y(i),z=z(i);c.n++;total++;
                c.minX=Math.min(c.minX,x);c.maxX=Math.max(c.maxX,x);c.minY=Math.min(c.minY,y);c.maxY=Math.max(c.maxY,y);c.minZ=Math.min(c.minZ,z);c.maxZ=Math.max(c.maxZ,z);
                c.edge|=x==0||x==N-1||y==0||y==N-1||z==0||z==N-1;
                if(!platforms){for(int[]d:D){int nx=x+d[0],ny=y+d[1],nz=z+d[2];if(inside(nx,ny,nz)){int j=id(nx,ny,nz);if(mask[j]!=0&&ids[j]==0){ids[j]=ci;queue[tail++]=j;}}}}
                else for(int[]d:D)if(d[1]==0)for(int dy=steps?-1:0;dy<=(steps?1:0);dy++){
                    int nx=x+d[0],ny=y+dy,nz=z+d[2];if(!inside(nx,ny,nz))continue;int j=id(nx,ny,nz);
                    if(mask[j]==0||ids[j]!=0)continue;
                    // One-block-rise adjacency requires room to raise the body
                    // over the lower platform before moving across. This is a
                    // geometric graph, not a native jump-controller claim.
                    if(dy==1&&!a(x,y+2,z)||dy==-1&&!a(nx,ny+2,nz))continue;
                    ids[j]=ci;queue[tail++]=j;
                }
            }
            if(largest==0||c.n>comps.get(largest).n)largest=ci;
        }
        return new Labels(ids,comps,total,largest);
    }
    static Map<String,Object> stats(Labels l){
        int edge=0,enclosed=0,enclosedNodes=0,small=0,smallNodes=0;
        for(int i=1;i<l.comps.size();i++){C c=l.comps.get(i);if(c.edge)edge++;else{enclosed++;enclosedNodes+=c.n;if(c.n<=8){small++;smallNodes+=c.n;}}}
        C big=l.largest==0?null:l.comps.get(l.largest);
        return map("nodes",l.total,"components",l.comps.size()-1,"largest_nodes",big==null?0:big.n,"largest_share",ratio(big==null?0:big.n,l.total),"boundary_touching_components",edge,"enclosed_components",enclosed,"enclosed_nodes",enclosedNodes,"enclosed_le8_nodes_components",small,"enclosed_le8_nodes",smallNodes,"largest_touches_boundary",big!=null&&big.edge,"largest_extent_xyz",big==null?List.of():List.of(big.maxX-big.minX+1,big.maxY-big.minY+1,big.maxZ-big.minZ+1));
    }
    Map<String,Object> measure(String kind,int wx,int wy,int wz){
        Labels raw=label(air,false,false);byte[] mainBody=new byte[V],mainFloor=new byte[V];
        int floorSites=0,low1=0,two=0,threePlus=0,capped=0,narrow=0,spacious=0;
        for(int i=0;i<V;i++)if(raw.ids[i]==raw.largest&&air[i]!=0){
            int x=x(i),y=y(i),z=z(i);if(body[i]!=0)mainBody[i]=1;
            if(!a(x,y-1,z)){
                floorSites++;int head=0;while(head<32&&externalAir(x,y+head,z))head++;
                if(head==1)low1++;else if(head==2)two++;else threePlus++;if(head==32)capped++;
                if(floor[i]!=0){mainFloor[i]=1;
                    boolean xp=a(x+1,y,z)&&a(x+1,y+1,z),xm=a(x-1,y,z)&&a(x-1,y+1,z),zp=a(x,y,z+1)&&a(x,y+1,z+1),zm=a(x,y,z-1)&&a(x,y+1,z-1);
                    if(!xp&&!xm||!zp&&!zm)narrow++;
                    boolean wide=true;for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<3;dy++)wide&=a(x+dx,y+dy,z+dz);if(wide)spacious++;
                }
            }
        }
        Labels flying=label(mainBody,false,false),level=label(mainFloor,true,false),step=label(mainFloor,true,true);
        int drop23=0,drop4to15=0,drop16=0,rise2plus=0,riseNoLanding=0,dirs=0;
        for(int i=0;i<V;i++)if(mainFloor[i]!=0){int x=x(i),y=y(i),z=z(i);
            for(int[]d:D)if(d[1]==0){dirs++;int nx=x+d[0],nz=z+d[2];
                if(externalAir(nx,y,nz)&&externalAir(nx,y+1,nz)){
                    int down=1;while(down<=16&&externalAir(nx,y-down,nz))down++;
                    if(down>16)drop16++;else{int drop=down-1;if(drop>=4)drop4to15++;else if(drop>=2)drop23++;}
                }else if(!externalAir(nx,y,nz)){
                    int up=1;while(up<=16&&!(externalAir(nx,y+up,nz)&&externalAir(nx,y+up+1,nz)&&!externalAir(nx,y+up-1,nz)))up++;
                    if(up>16)riseNoLanding++;else if(up>=2)rise2plus++;
                }
            }
        }
        int witnessId=inside(wx-ox,wy-oy,wz-oz)?id(wx-ox,wy-oy,wz-oz):-1;
        int origin=-1;
        if(witnessId>=0&&step.ids[witnessId]!=0)origin=witnessId;
        else if(step.largest!=0){double best=Double.POSITIVE_INFINITY;for(int i=0;i<V;i++)if(step.ids[i]==step.largest){double d=Math.pow(x(i)-N/2,2)+Math.pow(y(i)-N/2,2)+Math.pow(z(i)-N/2,2);if(d<best){best=d;origin=i;}}}
        return map("kind",kind,"seed",seed,"min_xyz",List.of(ox,oy,oz),"side_m",N,"sample_volume_m3",V,"actual_surface_height_range",List.of(minGround,maxGround),"min_overburden_to_sample_top_m",minGround-(oy+N-1),"air_components",stats(raw),"main_air_component",raw.largest,"witness_raw_component",witnessId>=0?raw.ids[witnessId]:null,"witness_in_main",witnessId>=0&&raw.ids[witnessId]==raw.largest,"main_floor_sites",floorSites,"main_floor_headroom_1m",low1,"main_floor_headroom_2m",two,"main_floor_headroom_ge3m",threePlus,"main_floor_headroom_ge32m",capped,"main_supported_player_positions",step.total,"main_player_positions_one_cell_wide_axis",narrow,"main_player_positions_3x3x3_clear",spacious,"main_free_flight_clearance",stats(flying),"main_level_only_floor_graph",stats(level),"main_one_block_rise_floor_graph",stats(step),"ledge_direction_denominator",dirs,"drop_2to3m_directions",drop23,"drop_4to15m_directions",drop4to15,"drop_at_least16m_directions",drop16,"rise_2to16m_directions",rise2plus,"wall_no_supported_landing_within16m_directions",riseNoLanding,"example_platform_path",geodesic(mainFloor,step,origin));
    }
    record Visit(int i,double distance){}
    Map<String,Object> geodesic(byte[] mask,Labels l,int origin){
        if(origin<0)return map("origin",null,"reachable_positions",0);
        int component=l.ids[origin],reach=0,far=origin;double[] dist=new double[V];Arrays.fill(dist,Double.POSITIVE_INFINITY);dist[origin]=0;
        var q=new PriorityQueue<Visit>(Comparator.comparingDouble(Visit::distance));q.add(new Visit(origin,0));
        while(!q.isEmpty()){Visit v=q.poll();int i=v.i;if(v.distance!=dist[i])continue;reach++;if(dist[i]>dist[far])far=i;int x=x(i),y=y(i),z=z(i);
            for(int[]d:D)if(d[1]==0)for(int dy=-1;dy<=1;dy++){
                int nx=x+d[0],ny=y+dy,nz=z+d[2];if(!inside(nx,ny,nz))continue;int j=id(nx,ny,nz);if(l.ids[j]!=component||mask[j]==0)continue;
                if(dy==1&&!a(x,y+2,z)||dy==-1&&!a(nx,ny+2,nz))continue;
                double nd=dist[i]+(dy==0?1:Math.sqrt(2));if(nd<dist[j]){dist[j]=nd;q.add(new Visit(j,nd));}
            }
        }
        return map("origin_xyz",List.of(ox+x(origin),oy+y(origin),oz+z(origin)),"component",component,"reachable_positions",reach,"max_shortest_graph_path_m",dist[far],"endpoint_xyz",List.of(ox+x(far),oy+y(far),oz+z(far)),"endpoint_straight_distance_m",Math.sqrt(Math.pow(x(origin)-x(far),2)+Math.pow(y(origin)-y(far),2)+Math.pow(z(origin)-z(far),2)),"denominator","one selected origin's platform graph component; not total passage length");
    }
    static Object ratio(int n,int d){return d==0?null:n/(double)d;}
    static Map<String,Object> map(Object...kv){var out=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)out.put((String)kv[i],kv[i+1]);return out;}
    static String json(Object o){if(o==null)return"null";if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map<?,?> m){var a=new ArrayList<String>();m.forEach((k,v)->a.add(json(k.toString())+":"+json(v)));return"{"+String.join(",",a)+"}";}if(o instanceof Collection<?> c)return"["+String.join(",",c.stream().map(RichNetworkTopology::json).toList())+"]";return"\""+o.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
    public static void main(String[] args)throws Exception{
        long[] seeds={424242,1,-17};int[][] witnesses={{237582,-277,-262158},{57336,-214,-57376},{-122868,-285,180198}};int[] expected={4331,22459,4479};var rows=new ArrayList<Object>();
        for(int c=0;c<seeds.length;c++){
            long seed=seeds[c];int[]p=witnesses[c];var terrain=new DrainageNetwork(new TerrainShape(seed),seed,true,true);var field=new UndergroundStructure(seed);
            int count=0;for(int z=(p[2]>>4)*16-16;z<(p[2]>>4)*16+32;z++)for(int x=(p[0]>>4)*16-16;x<(p[0]>>4)*16+32;x++){int h=terrain.sample(x,z).height();for(int y=(p[1]>>4)*16-16;y<(p[1]>>4)*16+32;y++)if(field.voidAt(x,y,z,h))count++;}
            if(count!=expected[c])throw new AssertionError("Frozen field does not match native qualified volume seed="+seed+" count="+count);
            for(int sample=0;sample<2;sample++){
                var probe=new RichNetworkTopology(seed,terrain,p[0]+sample*192,p[1]-sample*512,p[2]-sample*192);
                var row=probe.measure(sample==0?"qualified_witness_centered":"fixed_unselected_offset_192_-512_-192",p[0],p[1],p[2]);row.put("native_volume_air_count_match",count);rows.add(row);
                System.out.println("CHARACTERIZED seed="+seed+" sample="+sample+" air="+((Map<?,?>)row.get("air_components")).get("nodes")+" supported="+row.get("main_supported_player_positions"));
            }
        }
        Path out=Path.of(args[0]);Files.createDirectories(out.getParent());Files.writeString(out,json(map("baseline_field_version",UndergroundStructure.VERSION,"experimental_revision",2,"voxel_edge_m",1,"samples",rows))+"\n");System.out.println("READ_ONLY_CHARACTERIZATION_COMPLETE samples="+rows.size());
    }
}
