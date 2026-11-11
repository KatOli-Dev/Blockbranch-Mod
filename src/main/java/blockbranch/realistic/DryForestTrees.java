package blockbranch.realistic;

import java.util.*;
import java.util.function.BiPredicate;

/** Closed broadleaf cohorts using existing oak materials/scaffold geometry,
 * not an acacia savanna or the predecessor's open umbrella-pine stands. */
public final class DryForestTrees {
    static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> owner){
        var out=new ArrayList<WoodlandTrees.Root>();int radius=WoodlandTrees.RADIUS;
        var ecology=new GroundEcology.Field(d::sample,c);
        for(var p:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius,TreeScatter.Spacing.CLOSED)){
            int x=p.x(),z=p.z();if(!owner.test(x,z)||!TreeScatter.establishes(seed,p,3,ecology.sample(x,z)))continue;
            var ground=d.sample(x,z);boolean safe=true;
            for(int dz=-radius;dz<=radius;dz+=radius)for(int dx=-radius;dx<=radius;dx+=radius)
                if(Math.abs(d.sample(x+dx,z+dz).height()-ground.height())>3||LandVegetation.reserved(d,x+dx,z+dz))safe=false;
            if(!safe)continue;
            long h=OakBirchWoodland.mix(p.priority()^0x445259464F524553L);
            int cohort=(int)Long.remainderUnsigned(h>>>16,10);
            int height=p.young()?8+(int)Long.remainderUnsigned(h,5):cohort<2?13+(int)Long.remainderUnsigned(h,5):cohort<9?20+(int)Long.remainderUnsigned(h,7):27+(int)Long.remainderUnsigned(h,4);
            out.add(new WoodlandTrees.Root(x,ground.height()+1,z,false,height,(int)(h>>>32),DryForest.FAMILY));
        }
        return out;
    }
    private DryForestTrees(){}
}
