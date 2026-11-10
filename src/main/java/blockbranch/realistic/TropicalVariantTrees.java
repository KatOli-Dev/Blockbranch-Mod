package blockbranch.realistic;

import java.util.*;

/** Existing irregular stem process and jungle architectures, with open-stand recruitment. */
public final class TropicalVariantTrees {
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var roots=new ArrayList<WoodlandTrees.Root>();int radius=WoodlandTrees.RADIUS;var field=new GroundEcology.Field(d::sample,c);
        for(var point:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=point.x(),z=point.z(),kind=TropicalForestVariants.kind(seed,x,z,d,c);if(kind==0)continue;
            var site=field.sample(x,z);long h=OakBirchWoodland.mix(point.priority()^0x54524F50434F484FL);
            double patch=GroundEcology.colonyStrength(seed,0x54524F5054524545L,x,z,64,16,35,site);
            double density=kind==1?.18+.40*patch:.06+.12*patch;
            if((h>>>11)*0x1.0p-53>density||!TreeScatter.establishes(seed,point,3,site))continue;
            var center=d.sample(x,z);boolean safe=true;
            for(int dz=-radius;dz<=radius;dz+=radius)for(int dx=-radius;dx<=radius;dx+=radius)if(Math.abs(d.sample(x+dx,z+dz).height()-center.height())>3||LandVegetation.reserved(d,x+dx,z+dz))safe=false;
            if(!safe)continue;
            int cohort=(int)Long.remainderUnsigned(h>>>24,10),height=point.young()?6+(int)Long.remainderUnsigned(h,5):cohort<4?12+(int)Long.remainderUnsigned(h,5):18+(int)Long.remainderUnsigned(h,7);
            roots.add(new WoodlandTrees.Root(x,center.height()+1,z,false,height,(int)(h>>>32),kind==1?12:13));
        }
        return roots;
    }
    private TropicalVariantTrees(){}
}
