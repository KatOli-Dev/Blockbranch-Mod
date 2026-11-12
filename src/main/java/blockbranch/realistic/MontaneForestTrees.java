package blockbranch.realistic;

import java.util.*;
import java.util.function.BiPredicate;

/** Existing spruce materials stand in for a mixed natural needleleaf canopy.
 * NPS ROMO conifer descriptions bound mature spruce/fir examples near24–30m:
 * https://www.nps.gov/romo/learn/nature/conifers.htm . These varied cohorts
 * represent structure, not identified species or an age/succession simulation.
 * Existing conifer spacing/recruitment supplies irregular gaps and young stems;
 * reference photos guide crown taper and variation, not calibrated stem counts. */
public final class MontaneForestTrees {
    static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> owner){
        var roots=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);
        for(var p:TreeScatter.points(seed,minX-8,minZ-8,maxX+8,maxZ+8,TreeScatter.Spacing.CONIFER)){
            int x=p.x(),z=p.z();if(!owner.test(x,z)||OakBirchWoodland.glade(seed,x,z)
                ||!TreeScatter.establishes(seed,p,1,ecology.sample(x,z)))continue;
            if(ForestGrasslandTransition.active(seed,x,z,d,c)&&!ForestGrasslandTransition.tree(seed,p))continue;
            var center=d.sample(x,z);boolean safe=true;
            // Same bounded footing convention as ordinary forest. Crown radius
            // never exceeds8m; no stem is established across a wet/exposed niche.
            for(int dz=-8;dz<=8;dz+=8)for(int dx=-8;dx<=8;dx+=8)
                if(Math.abs(d.sample(x+dx,z+dz).height()-center.height())>3||LandVegetation.reserved(d,x+dx,z+dz))safe=false;
            if(!safe)continue;
            long h=OakBirchWoodland.mix(p.priority()^0x4D4F4E54414E4554L);
            int height=p.young()?10+(int)Long.remainderUnsigned(h,7):22+(int)Long.remainderUnsigned(h,10);
            roots.add(new WoodlandTrees.Root(x,center.height()+1,z,false,height,(int)(h>>>32),MontaneForest.FAMILY));
        }
        return roots;
    }
    private MontaneForestTrees(){}
}
