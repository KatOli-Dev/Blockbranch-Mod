package blockbranch.realistic;
import java.util.*;

/** Sparse irregular acacia age cohorts with connected spreading, flat-topped crowns. */
public final class SavannaTrees {
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();int radius=WoodlandTrees.RADIUS;var field=new GroundEcology.Field(d::sample,c);
        for(var point:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=point.x(),z=point.z();if(Swamp.contains(seed,x,z,d,c))continue;if(!Savanna.contains(seed,x,z,d,c))continue;
            var site=field.sample(x,z);long v=OakBirchWoodland.mix(point.priority()^0x5341564143414349L);
            double group=GroundEcology.colonyStrength(seed,0x5341565354454D53L,x,z,64,20,42,site);
            double density=(.10+.40*group)*Math.clamp(.6+site.wetness(),.6,1);
            if((v>>>11)*0x1.0p-53>=density)continue;
            var center=d.sample(x,z);if(center.waterLevel()!=DrainageNetwork.DRY||d.channelAt(x,z).edge()>=0)continue;boolean safe=true;
            for(int dz=-radius;dz<=radius;dz+=radius)for(int dx=-radius;dx<=radius;dx+=radius)
                if(Math.abs(d.sample(x+dx,z+dz).height()-center.height())>3||LandVegetation.reserved(d,x+dx,z+dz))safe=false;
            if(!safe)continue;
            int height=point.young()?6+(int)Long.remainderUnsigned(v>>>24,3):9+(int)Long.remainderUnsigned(v>>>24,4);
            result.add(new WoodlandTrees.Root(x,center.height()+1,z,false,height,(int)(v>>>32)));
        }
        return result;
    }
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){return tree(root,false);}
    static List<WoodlandTrees.Voxel> uplandTree(WoodlandTrees.Root root){return tree(root,true);}
    private static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root,boolean varied){
        int radius=WoodlandTrees.RADIUS,size=radius*2+1,height=WoodlandTrees.MAX_HEIGHT,count=size*size*height;
        byte[] grid=new byte[count],distance=new byte[count];Arrays.fill(distance,(byte)7);
        int fork=root.height()-4;var crowns=new ArrayList<int[]>();
        if(!varied){
            // Two-block rises separate the short bends of a leaning native-log
            // stem. The first bend stays above the terrain-clearance margin.
            int v=root.variant();boolean divided=root.height()>=9&&((v>>>18)&1)==0;
            int forkY=root.height()-(divided?5:4);
            int[] dx={1,0,-1,0},dz={0,1,0,-1};
            int rotation=v&3,lean=root.height()<9?1:2+(root.height()>=11?((v>>>10)&1):0);
            int bendY=3+((v>>>14)&1),x=0,z=0,steps=0,forkX=0,forkZ=0;
            for(int y=0;y<=root.height()-2;y++){
                grid[index(x,y,z)]=19;
                if(steps<lean&&y==bendY+steps*2){
                    x+=dx[rotation];z+=dz[rotation];steps++;grid[index(x,y,z)]=19;
                }
                if(y==forkY){forkX=x;forkZ=z;}
            }
            int direction=(rotation+(divided?2:1+((v>>>7)&1)))&3;
            int reach=divided?5:2,by=root.height()-3;
            int bx=forkX+dx[direction]*reach,bz=forkZ+dz[direction]*reach;
            line(grid,forkX,forkY,forkZ,bx,by,bz);
            int spread=root.height()<9||divided?2:3;
            crowns.add(new int[]{bx,by+1,bz,spread+(divided?0:((v>>>20)&1)),1,spread+(divided?0:((v>>>24)&1))});
            int mainSpread=root.height()<9||divided?3:4;
            crowns.add(new int[]{x,root.height()-1,z,mainSpread+(divided?0:((v>>>5)&1)),1,mainSpread+(divided?0:((v>>>8)&1))});
        }else{
            // Upper stems, scaffold directions and unequal umbrella lobes vary;
            // the upland scaffold remains separate from lowland umbrella geometry.
            int v=root.variant(),leanX=root.height()<9?0:Math.floorMod(v>>>7,3)-1,leanZ=root.height()<9?0:Math.floorMod(v>>>11,3)-1;
            line(grid,0,0,0,0,fork,0);line(grid,0,fork,0,leanX,root.height()-2,leanZ);
            int[] dx={3,2,0,-2,-3,-2,0,2},dz={0,2,3,2,0,-2,-3,-2};
            int branches=2+((v>>>4)&1),rotation=v&7;
            for(int branch=0;branch<branches;branch++){
                int direction=(rotation+branch*3)&7,bx=leanX+dx[direction],bz=leanZ+dz[direction],by=root.height()-4+((v>>>(14+branch))&1);
                line(grid,0,fork,0,bx,by,bz);
                int rx=3+((v>>>(18+branch))&1),rz=3+((v>>>(22+branch))&1),ry=root.height()>=12?1+((v>>>(26+branch))&1):1;
                crowns.add(new int[]{bx,by+1,bz,rx,ry,rz});
            }
            crowns.add(new int[]{leanX,root.height()-1,leanZ,3+((v>>>5)&1),1,3+((v>>>6)&1)});
        }
        for(int y=fork-1;y<height;y++)for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++) {
            int i=index(x,y,z);if(grid[i]!=0)continue;
            for(var c:crowns) {
                double d;
                if(varied)d=Math.pow((x-c[0])/(double)c[3],2)+Math.pow((y-c[1])/(double)c[4],2)+Math.pow((z-c[2])/(double)c[5],2);
                else{
                    // Two shallow layers keep a broad flat umbrella with a
                    // smaller continuous underside, rather than hanging tips.
                    int dy=y-c[1];if(dy>0||dy< -1)continue;
                    double taper=dy==0?1:.76;
                    d=Math.pow((x-c[0])/(c[3]*taper),2)+Math.pow((z-c[2])/(c[5]*taper),2);
                }
                long v=OakBirchWoodland.mix(root.variant() ^ x*0x9E3779B97F4A7C15L ^ z*0xC2B2AE3D27D4EB4FL ^ y*0x165667B19E3779F9L);
                if(d<=1 && (d<0.75 || (v&7)!=0)){grid[i]=22;break;}
            }
        }
        var queue=new ArrayDeque<Integer>();for(int i=0;i<count;i++)if(WoodlandTrees.logCode(grid[i])){distance[i]=0;queue.add(i);}
        while(!queue.isEmpty()) {
            int i=queue.removeFirst(),x=i%size-radius,z=i/size%size-radius,y=i/(size*size);if(distance[i]>=6)continue;
            for(var d:DIRECTIONS) {
                int nx=x+d[0],ny=y+d[1],nz=z+d[2];if(Math.abs(nx)>radius || Math.abs(nz)>radius || ny<0 || ny>=height)continue;
                int n=index(nx,ny,nz);if(grid[n]==22 && distance[n]>distance[i]+1){distance[n]=(byte)(distance[i]+1);queue.add(n);}
            }
        }
        var result=new ArrayList<WoodlandTrees.Voxel>();
        for(int y=0;y<height;y++)for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++) {
            int i=index(x,y,z),code=grid[i];if(code==22)code=distance[i]>6 ? 0 : 21+distance[i];
            if(code>0)result.add(new WoodlandTrees.Voxel(root.x()+x,root.y()+y,root.z()+z,code));
        }
        // Existing saved log codes retain their meanings. New upper scaffolds
        // use native all-bark wood; the rooted lower stem stays a log.
        return result.stream().map(v->new WoodlandTrees.Voxel(v.x(),v.y(),v.z(),
            !varied&&WoodlandTrees.logCode(v.code())&&v.y()>root.y()+1
                ?blockbranch.cubic.CanopyLayer.ACACIA_WOOD_CODE:v.code()+9)).toList();
    }
    private static void line(byte[] grid,int x,int y,int z,int tx,int ty,int tz) {
        grid[index(x,y,z)]=19;
        while(x!=tx || y!=ty || z!=tz) {
            if(y<ty){y++;grid[index(x,y,z)]=19;}
            if(x!=tx){x+=Integer.signum(tx-x);grid[index(x,y,z)]=20;}
            if(z!=tz){z+=Integer.signum(tz-z);grid[index(x,y,z)]=21;}
        }
    }
    private static int index(int x,int y,int z){int size=WoodlandTrees.RADIUS*2+1;return (y*size+z+WoodlandTrees.RADIUS)*size+x+WoodlandTrees.RADIUS;}
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private SavannaTrees(){}
}
