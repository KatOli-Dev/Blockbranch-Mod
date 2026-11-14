package blockbranch.realistic;
import java.util.*;

/** Irregular tall broadleaf crowns with a lower leafy branch tier and regeneration cohorts. */
public final class HumidTrees {
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();int radius=WoodlandTrees.RADIUS;var ecology=new GroundEcology.Field(d::sample,c);
        for(var p:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius,TreeScatter.Spacing.CLOSED)){
            int x=p.x(),z=p.z();if(Swamp.contains(seed,x,z,d,c)||TropicalForestVariants.kind(seed,x,z,d,c)>0)continue;if(!HumidWoodland.contains(seed,x,z,d,c)||HumidWoodland.glade(seed,x,z)||!TreeScatter.establishes(seed,p,3,ecology.sample(x,z)))continue;
            var center=d.sample(x,z);boolean safe=true;
            for(int dz=-radius;dz<=radius;dz+=radius)for(int dx=-radius;dx<=radius;dx+=radius)
                if(Math.abs(d.sample(x+dx,z+dz).height()-center.height())>3||LandVegetation.reserved(d,x+dx,z+dz))safe=false;
            if(!safe)continue;
            long h=OakBirchWoodland.mix(p.priority()^0x48554D49444F414BL);
            if((OakBirchWoodland.mix(h^0x44454E53495459L)>>>11)*0x1.0p-53>=HumidWoodland.canopyDensity(seed,x,z))continue;
            int height=p.young()?8+(int)Long.remainderUnsigned(h,5):21+(int)Long.remainderUnsigned(h,7);
            result.add(new WoodlandTrees.Root(x,center.height()+1,z,false,height,(int)(h>>>32)));
        }
        return result;
    }
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){return WoodlandTrees.humidTree(root);}
    /** {@code gated} selects the saved {@link HumidCrowns} architecture; young stems always keep the published crown. */
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root,boolean gated){return WoodlandTrees.humidTree(root,gated);}
    private HumidTrees(){}
}
