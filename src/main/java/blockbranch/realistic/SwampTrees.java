package blockbranch.realistic;
import java.util.*;

/** Low crooked oak scaffolds and broad unequal crowns on solid saturated banks. */
public final class SwampTrees {
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);
        for(var point:TreeScatter.points(seed,minX-8,minZ-8,maxX+8,maxZ+8)){
            int x=point.x(),z=point.z();if(!Swamp.contains(seed,x,z,d,c)||!TreeScatter.establishes(seed,point,0,ecology.sample(x,z))||OakBirchWoodland.glade(seed^0x5357414D50474150L,x,z))continue;
            var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)continue;boolean safe=true;
            for(int dz=-2;dz<=2;dz+=2)for(int dx=-2;dx<=2;dx+=2){var q=d.sample(x+dx,z+dz);if(q.waterLevel()!=DrainageNetwork.DRY||Math.abs(q.height()-s.height())>1||d.channelAt(x+dx,z+dz).edge()>=0)safe=false;}
            if(!safe)continue;long v=OakBirchWoodland.mix(point.priority()^0x5357414D50545245L);int h=point.young()?6+(int)Long.remainderUnsigned(v,4):12+(int)Long.remainderUnsigned(v,7);
            result.add(new WoodlandTrees.Root(x,s.height()+1,z,false,h,(int)(v>>>32),3));
        }
        return result;
    }
    private record P(int x,int y,int z){}
    private record Crown(double x,double y,double z,double rx,double ry,double rz,long seed){}
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){
        long seed=OakBirchWoodland.mix(root.variant()^0x5357414D50464F52L);int h=root.height();byte[] grid=new byte[17*17*Math.max(24,h+2)];var crowns=new ArrayList<Crown>();double angle=unit(seed)*Math.PI*2;
        P base=new P(0,0,0),straight=new P(0,Math.min(3,h/2),0),fork=new P((int)Math.round(Math.cos(angle)*(1+unit(seed^7))),Math.max(3,h/2),(int)Math.round(Math.sin(angle)*(1+unit(seed^7))));
        limb(grid,base,straight);limb(grid,straight,fork);
        int arms=h<9?2+(int)(seed&1):3+(int)Long.remainderUnsigned(seed>>>8,3);
        for(int n=0;n<arms;n++){
            long v=OakBirchWoodland.mix(seed+n*1297L);double a=angle+n*2.39996+(unit(v^17)-.5)*.9,length=(h<9?2:3)+unit(v^23)*(h<9?1:2);
            P tip=new P((int)Math.clamp(Math.round(fork.x()+Math.cos(a)*length),-5,5),Math.max(fork.y()+1,h-2-(int)Long.remainderUnsigned(v>>>12,h<9?2:4)),(int)Math.clamp(Math.round(fork.z()+Math.sin(a)*length),-5,5));
            P elbow=new P((fork.x()+tip.x())/2,fork.y()+Math.max(1,(tip.y()-fork.y())/2),(fork.z()+tip.z())/2);limb(grid,fork,elbow);limb(grid,elbow,tip);
            crowns.add(new Crown(tip.x()*.8,tip.y()+.5,tip.z()*.8,h<9?2.1:2.8+unit(v^29),1.8+unit(v^31),h<9?2.0:2.7+unit(v^37),v));
        }
        // All terms in q are nonnegative. Each crown lies within its horizontal
        // radii and y +/- (ry + .12*rx); pad integer bounds by one to keep boundary
        // rounding conservative. Evaluate the original expression, never an
        // approximated ellipsoid. Earlier leaves and wood have identical priority.
        for(var c:crowns){
            int minX=Math.max(-8,(int)Math.floor(c.x()-c.rx())-1),maxX=Math.min(8,(int)Math.ceil(c.x()+c.rx())+1);
            int minZ=Math.max(-8,(int)Math.floor(c.z()-c.rz())-1),maxZ=Math.min(8,(int)Math.ceil(c.z()+c.rz())+1);
            int minY=Math.max(1,(int)Math.floor(c.y()-c.ry()-.12*c.rx())-1),maxY=Math.min(23,(int)Math.ceil(c.y()+c.ry()+.12*c.rx())+1);
            for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++)for(int x=minX;x<=maxX;x++){
                int i=index(x,y,z);if(grid[i]!=0)continue;
                double dx=x-c.x(),dz=z-c.z(),q=dx*dx/(c.rx()*c.rx())+dz*dz/(c.rz()*c.rz())+(y-c.y()-.12*dx)*(y-c.y()-.12*dx)/(c.ry()*c.ry());
                if(q<1)grid[i]=7;
            }
        }
        // The generated scaffold stays in +/-5, so its terminal twig envelope
        // fits the same +/-8 raster. Only wood participates in the degree test.
        for(int i=0;i<grid.length;i++)if(grid[i]>0&&grid[i]<=3){
            int x=i%17-8,z=i/17%17-8,y=i/(17*17);if(y<=2)continue;
            int neighbours=0;for(var d:DIR){int code=at(grid,x+d[0],y+d[1],z+d[2]);if(code>0&&code<=3)neighbours++;}
            if(neighbours<=1)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)put(grid,x+dx,y+dy,z+dz,7);
        }
        byte[] distance=new byte[grid.length];Arrays.fill(distance,(byte)7);
        int[] queue=new int[grid.length];int head=0,tail=0;
        for(int i=0;i<grid.length;i++)if(grid[i]>0&&grid[i]<=3){distance[i]=0;queue[tail++]=i;}
        while(head<tail){
            int i=queue[head++],v=distance[i];if(v>=6)continue;
            int x=i%17-8,z=i/17%17-8,y=i/(17*17);
            for(var d:DIR){int nx=x+d[0],ny=y+d[1],nz=z+d[2];if(at(grid,nx,ny,nz)!=7)continue;
                int next=index(nx,ny,nz);if(distance[next]>v+1){distance[next]=(byte)(v+1);queue[tail++]=next;}}
        }
        // Raster order is the original y,z,x output order. BFS source order does
        // not affect shortest six-neighbour distance from the immutable scaffold.
        var result=new ArrayList<WoodlandTrees.Voxel>();
        for(int i=0;i<grid.length;i++){int code=grid[i];if(code==7)code=distance[i]>6?0:6+distance[i];
            if(code>0)result.add(new WoodlandTrees.Voxel(root.x()+i%17-8,root.y()+i/(17*17),root.z()+i/17%17-8,code));}
        return result;
    }
    private static int index(int x,int y,int z){return (y*17+z+8)*17+x+8;}
    private static int at(byte[] grid,int x,int y,int z){return x < -8||x>8||z < -8||z>8||y<0||y>=grid.length/(17*17)?0:grid[index(x,y,z)];}
    private static void put(byte[] grid,int x,int y,int z,int code){int i=index(x,y,z);if(grid[i]==0)grid[i]=(byte)code;}
    private static void limb(byte[] grid,P a,P b){int x=a.x(),y=a.y(),z=a.z(),dy=Math.abs(b.y()-y),dx=Math.abs(b.x()-x),dz=Math.abs(b.z()-z),axis=dy>=dx&&dy>=dz?1:dx>=dz?2:3,steps=Math.max(dy,Math.max(dx,dz));put(grid,x,y,z,axis);for(int n=1;n<=steps;n++){int tx=(int)Math.round(a.x()+(b.x()-a.x())*n/(double)steps),ty=(int)Math.round(a.y()+(b.y()-a.y())*n/(double)steps),tz=(int)Math.round(a.z()+(b.z()-a.z())*n/(double)steps);while(y!=ty){y+=Integer.signum(ty-y);put(grid,x,y,z,axis);}while(x!=tx){x+=Integer.signum(tx-x);put(grid,x,y,z,axis);}while(z!=tz){z+=Integer.signum(tz-z);put(grid,x,y,z,axis);}}}
    private static double unit(long v){return(OakBirchWoodland.mix(v)>>>11)*0x1.0p-53;}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private SwampTrees(){}
}
