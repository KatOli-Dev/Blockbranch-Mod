package blockbranch.realistic;

import java.util.*;

/** Warm woodland oak and open, elevated umbrella-pine architecture using existing oak/spruce materials. */
public final class WarmTrees {
    public record Root(int x,int y,int z,boolean pine,int height,int variant) {}
    public static List<Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork drainage,ClimateField climate) {
        var result=new ArrayList<Root>();int radius=WoodlandTrees.RADIUS;
        var ecology=new GroundEcology.Field(drainage::sample,climate);
        for(var point:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=point.x(),z=point.z();if(Swamp.contains(seed,x,z,drainage,climate)||TropicalForestVariants.kind(seed,x,z,drainage,climate)>0||SavannaUplands.kind(seed,x,z,drainage,climate)>0||SavannaUplands.surfaceKind(seed,x,z,drainage,climate)>0)continue;long v=OakBirchWoodland.mix(point.priority()^0x5741524D50494E45L);
            if(WarmWoodland.glade(seed,x,z) || !WarmWoodland.contains(seed,x,z,drainage,climate) || !TreeScatter.establishes(seed,point,2,ecology.sample(x,z)))continue;
            var center=drainage.sample(x,z);boolean safe=true;
            for(int dz=-radius;dz<=radius;dz+=radius)for(int dx=-radius;dx<=radius;dx+=radius)
                if(Math.abs(drainage.sample(x+dx,z+dz).height()-center.height())>3 || LandVegetation.reserved(drainage,x+dx,z+dz))safe=false;
            if(!safe)continue;
            boolean pine=Long.remainderUnsigned(v>>>16,10)<6;int height=point.young()?9+(int)Long.remainderUnsigned(v>>>24,4):(pine?14:13)+(int)Long.remainderUnsigned(v>>>24,pine?6:6);
            result.add(new Root(x,center.height()+1,z,pine,height,(int)(v>>>32)));
        }
        return result;
    }
    public static List<WoodlandTrees.Voxel> tree(Root root) {
        if(!root.pine())return WoodlandTrees.tree(new WoodlandTrees.Root(root.x(),root.y(),root.z(),false,root.height(),root.variant()));
        if(root.height()<13)return BorealTrees.legacyTree(new WoodlandTrees.Root(root.x(),root.y(),root.z(),false,root.height(),root.variant()));
        int radius=WoodlandTrees.RADIUS,size=radius*2+1,height=WoodlandTrees.MAX_HEIGHT,count=size*size*height;
        byte[] grid=new byte[count],distance=new byte[count];Arrays.fill(distance,(byte)7);
        int fork=root.height()-5;line(grid,0,0,0,0,root.height()-2,0);
        int rotation=Math.floorMod(root.variant(),4);int[] dx={3,0,-3,0},dz={0,3,0,-3};var crowns=new ArrayList<int[]>();
        for(int branch=0;branch<3;branch++) {
            int d=(rotation+branch)&3,bx=dx[d],bz=dz[d],by=root.height()-3+(branch&1);
            line(grid,0,fork,0,bx,by,bz);crowns.add(new int[]{bx,by+1,bz,4,2,4});
        }
        crowns.add(new int[]{0,root.height()-1,0,4,2,4});
        for(int y=fork-1;y<height;y++)for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++) {
            int i=index(x,y,z);if(grid[i]!=0)continue;
            for(var c:crowns) {
                double d=Math.pow((x-c[0])/(double)c[3],2)+Math.pow((y-c[1])/(double)c[4],2)+Math.pow((z-c[2])/(double)c[5],2);
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
        return result;
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
    private WarmTrees() {}
}
