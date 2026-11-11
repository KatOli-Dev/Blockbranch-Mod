package blockbranch.realistic;

import java.util.*;
import java.util.function.BiPredicate;

/** Mature oak cohorts plus a separate irregular woody shrub layer. Existing
 * oak materials are structural stand-ins, not identified Mediterranean species.
 * Metre-wide vanilla logs exaggerate shrub-stem thickness at this voxel scale.
 * No new block behavior, harvesting, spreading or fire mechanics are added. */
public final class SummerDryTrees {
    static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> owner){
        var roots=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);
        for(var p:TreeScatter.points(seed,minX-8,minZ-8,maxX+8,maxZ+8,TreeScatter.Spacing.CLOSED)){
            int x=p.x(),z=p.z();if(!owner.test(x,z)||!TreeScatter.establishes(seed,p,3,ecology.sample(x,z))||!supported(x,z,8,3,d))continue;
            long h=OakBirchWoodland.mix(p.priority()^0x53554D4D45524F41L);
            int height=p.young()?6+(int)Long.remainderUnsigned(h,5):12+(int)Long.remainderUnsigned(h,7);
            roots.add(new WoodlandTrees.Root(x,d.sample(x,z).height()+1,z,false,height,(int)(h>>>32),SummerDryWoodland.FAMILY));
        }
        long shrubSeed=seed^0x53554D4D45525348L;
        for(var p:TreeScatter.points(shrubSeed,minX-2,minZ-2,maxX+2,maxZ+2,TreeScatter.Spacing.CLOSED)){
            int x=p.x(),z=p.z();if(!owner.test(x,z)||!supported(x,z,2,2,d))continue;
            double colony=GroundEcology.colonyStrength(seed,0x53554D5348525542L,x,z,28,7,17,ecology.sample(x,z));
            long h=OakBirchWoodland.mix(p.priority()^0x53554D5348524F4FL);
            if((h>>>11)*0x1.0p-53>.25+.60*colony)continue;
            roots.add(new WoodlandTrees.Root(x,d.sample(x,z).height()+1,z,false,3+(int)Long.remainderUnsigned(h>>>12,3),(int)(h>>>32),SummerDryWoodland.FAMILY));
        }
        return roots;
    }
    private static boolean supported(int x,int z,int radius,int relief,DrainageNetwork d){
        var center=d.sample(x,z);
        for(int dz=-radius;dz<=radius;dz+=radius)for(int dx=-radius;dx<=radius;dx+=radius)
            if(Math.abs(d.sample(x+dx,z+dz).height()-center.height())>relief||LandVegetation.reserved(d,x+dx,z+dz))return false;
        return true;
    }
    static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){
        if(root.height()>5)return ForestTrees.tree(root);
        var out=new ArrayList<WoodlandTrees.Voxel>();int h=root.height();
        // Low branched woody axis. Raw leaves also need valid support distances:
        // saved tree clearing consumes this geometry before the common canopy
        // rasterizer recomputes support after terrain clipping/crown merging.
        for(int y=0;y<h-1;y++)out.add(new WoodlandTrees.Voxel(root.x(),root.y()+y,root.z(),1));
        int turn=Math.floorMod(root.variant(),4),dx=turn==0?1:turn==2?-1:0,dz=turn==1?1:turn==3?-1:0;
        out.add(new WoodlandTrees.Voxel(root.x()+dx,root.y()+1,root.z()+dz,dx!=0?2:3));
        for(int y=1;y<h;y++)for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++){
            double width=y==h-1?1.4:2.2;
            if(x*x+z*z>width*width||(x==0&&z==0&&y<h-1)||(x==dx&&z==dz&&y==1))continue;
            long bits=OakBirchWoodland.mix(root.variant()^x*1297L^z*1237L^y*1049L);
            if(x*x+z*z>2&&(bits&7)==0)continue;
            out.add(new WoodlandTrees.Voxel(root.x()+x,root.y()+y,root.z()+z,7));
        }
        int plane=25;byte[] cells=new byte[plane*h],distance=new byte[cells.length];Arrays.fill(distance,(byte)7);
        var queue=new ArrayDeque<Integer>();
        for(var v:out){int i=(v.y()-root.y())*plane+(v.z()-root.z()+2)*5+v.x()-root.x()+2;cells[i]=(byte)v.code();if(WoodlandTrees.logCode(v.code())){distance[i]=0;queue.add(i);}}
        while(!queue.isEmpty()){
            int i=queue.removeFirst(),x=i%5,z=i/5%5,y=i/plane;
            for(var step:DIRECTIONS){int nx=x+step[0],ny=y+step[1],nz=z+step[2];if(nx<0||nx>=5||ny<0||ny>=h||nz<0||nz>=5)continue;
                int next=ny*plane+nz*5+nx;if(cells[next]>=7&&distance[next]>distance[i]+1){distance[next]=(byte)(distance[i]+1);queue.add(next);}}
        }
        for(int n=0;n<out.size();n++){
            var v=out.get(n);if(WoodlandTrees.logCode(v.code()))continue;
            int i=(v.y()-root.y())*plane+(v.z()-root.z()+2)*5+v.x()-root.x()+2;
            // The full inner cross guarantees support for every outer voxel.
            // Fail if a future shape breaks that invariant, rather than silently
            // changing its footprint or emitting unsupported retained leaves.
            if(distance[i]>6)throw new IllegalStateException("Unsupported shrub leaf");
            out.set(n,new WoodlandTrees.Voxel(v.x(),v.y(),v.z(),6+distance[i]));
        }
        return out;
    }
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private SummerDryTrees(){}
}
