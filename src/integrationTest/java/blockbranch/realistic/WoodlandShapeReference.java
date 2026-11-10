package blockbranch.realistic;
import java.util.*;
/** Frozen d61ff42 shape and helpers; QA only. */
final class WoodlandShapeReference {
    private static final int RADIUS=8,MAX_HEIGHT=24;
    private record Crown(int x,int y,int z,int rx,int ry,int rz) {}
    static List<WoodlandTrees.Voxel> shape(WoodlandTrees.Root root,boolean humid) {
        int size=RADIUS*2+1,height=MAX_HEIGHT,count=size*size*height;
        byte[] blocks=new byte[count],distance=new byte[count];Arrays.fill(distance,(byte)7);
        int wood=root.birch() ? 4 : 1;
        int stem=root.height()-(root.birch() ? 4 : 6);
        line(blocks,0,0,0,0,stem,0,wood);
        var crowns=new ArrayList<Crown>();
        if(root.birch()) {
            line(blocks,0,stem,0,0,root.height()-2,0,wood);
            crowns.add(new Crown(0,root.height()-3,0,3,4,3));
            crowns.add(new Crown((root.variant()&1)==0 ? 1 : -1,root.height()-5,0,3,3,3));
        } else {
            int rotation=Math.floorMod(root.variant(),4);
            for(int branch=0;branch<3;branch++) {
                int[] dx={3,0,-3,0},dz={0,3,0,-3};int direction=(branch+rotation)&3;
                int bx=dx[direction],bz=dz[direction],by=root.height()-4+(branch&1);
                line(blocks,0,stem-1,0,bx,by,bz,wood);
                crowns.add(new Crown(bx,by+1,bz,3+(branch==0 ? 1 : 0),3,3+(branch==1 ? 1 : 0)));
            }
            line(blocks,0,stem,0,0,root.height()-2,0,wood);
            crowns.add(new Crown(0,root.height()-2,0,4,3,4));
        }
        if(humid && root.height()>=18){
            int rotation=Math.floorMod(root.variant()>>>4,4);int[] dx={3,0,-3,0},dz={0,3,0,-3};
            for(int branch=0;branch<2;branch++){
                int direction=(rotation+branch*2)&3,by=root.height()-10+(branch&1);
                line(blocks,0,Math.max(2,by-3),0,dx[direction],by,dz[direction],wood);
                crowns.add(new Crown(dx[direction],by+1,dz[direction],4,3,4));
            }
        }
        for(int y=1;y<height;y++) for(int z=-RADIUS;z<=RADIUS;z++) for(int x=-RADIUS;x<=RADIUS;x++) {
            int index=index(x,y,z);if(blocks[index]!=0) continue;
            for(var crown:crowns) {
                double d=square((x-crown.x())/(double)crown.rx())+square((y-crown.y())/(double)crown.ry())+square((z-crown.z())/(double)crown.rz());
                if(d>1) continue;
                long edge=OakBirchWoodland.mix(root.variant() ^ x*0x9E3779B97F4A7C15L ^ y*0xC2B2AE3D27D4EB4FL ^ z*0x165667B19E3779F9L);
                if(d<0.72 || (edge&7)!=0) blocks[index]=19;
                break;
            }
        }
        // Calculate actual six-neighbour support through foliage, rather than marking
        // natural leaves persistent or assuming distance through holes in a crown.
        var queue=new ArrayDeque<Integer>();
        for(int i=0;i<count;i++) if(blocks[i]>0 && blocks[i]<=6) { distance[i]=0;queue.add(i); }
        while(!queue.isEmpty()) {
            int i=queue.removeFirst(),x=i%size-RADIUS,z=i/size%size-RADIUS,y=i/(size*size);
            if(distance[i]>=6) continue;
            for(int[] direction:DIRECTIONS) {
                int nx=x+direction[0],ny=y+direction[1],nz=z+direction[2];
                if(Math.abs(nx)>RADIUS || Math.abs(nz)>RADIUS || ny<0 || ny>=height) continue;
                int n=index(nx,ny,nz);
                if(blocks[n]==19 && distance[n]>distance[i]+1) { distance[n]=(byte)(distance[i]+1);queue.add(n); }
            }
        }
        var result=new ArrayList<WoodlandTrees.Voxel>();
        for(int y=0;y<height;y++) for(int z=-RADIUS;z<=RADIUS;z++) for(int x=-RADIUS;x<=RADIUS;x++) {
            int i=index(x,y,z),code=blocks[i];
            if(code==19) code=distance[i]>6 ? 0 : (root.birch() ? 12 : 6)+distance[i];
            if(code>0) result.add(new WoodlandTrees.Voxel(root.x()+x,root.y()+y,root.z()+z,code));
        }
        return result;
    }
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static int index(int x,int y,int z) { int size=RADIUS*2+1;return (y*size+z+RADIUS)*size+x+RADIUS; }
    private static double square(double x) { return x*x; }
    private static void line(byte[] blocks,int x,int y,int z,int tx,int ty,int tz,int wood) {
        blocks[index(x,y,z)]=(byte)wood;
        // Connected rising branch steps with directional log axes.
        while(x!=tx || y!=ty || z!=tz) {
            if(y<ty) { y++;blocks[index(x,y,z)]=(byte)wood; }
            if(x!=tx) { x+=Integer.signum(tx-x);blocks[index(x,y,z)]=(byte)(wood+1); }
            if(z!=tz) { z+=Integer.signum(tz-z);blocks[index(x,y,z)]=(byte)(wood+2); }
        }
    }
}
