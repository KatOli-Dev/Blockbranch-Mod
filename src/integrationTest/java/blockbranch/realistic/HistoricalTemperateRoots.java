package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.WoodlandTrees.RADIUS;
import blockbranch.realistic.WoodlandTrees.Root;
/** Frozen root selector from 146b2c5a, before biome transition definition 1. */
final class HistoricalTemperateRoots {
    public static List<Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork drainage,ClimateField climate) {
        var result=new ArrayList<Root>();int radius=WoodlandTrees.RADIUS;
        var ecology=new GroundEcology.Field(drainage::sample,climate);
        for(var point:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=point.x(),z=point.z();long v=OakBirchWoodland.mix(point.priority()^0L);
            if(Swamp.contains(seed,x,z,drainage,climate)||OakBirchWoodland.glade(seed,x,z) || !OakBirchWoodland.potential(seed,x,z,drainage,climate) || !TreeScatter.establishes(seed,point,0,ecology.sample(x,z)))continue;
                var center=drainage.sample(x,z);boolean safe=true;
                // Keep stems and crowns off lake/river niches and away from buried hillsides.
                for(int dz=-RADIUS;dz<=RADIUS;dz+=RADIUS) for(int dx=-RADIUS;dx<=RADIUS;dx+=RADIUS) {
                    var s=drainage.sample(x+dx,z+dz);
                    if(Math.abs(s.height()-center.height())>3 || LandVegetation.reserved(drainage,x+dx,z+dz)) safe=false;
                }
                if(!safe) continue;
                if(Forest.potential(seed,x,z,drainage,climate)){var root=ForestTrees.root(seed,point,center,ecology.sample(x,z),BirchForest.contains(seed,x,z,drainage,climate));if(root!=null)result.add(root);continue;}
                int variant=TemperateForestVariants.kind(seed,x,z,drainage,climate);
                if(variant>0){var root=TemperateForestVariants.root(seed,point,center,ecology.sample(x,z),variant);if(root!=null)result.add(root);continue;}
                if(DappledForest.contains(seed,x,z,drainage,climate)){result.add(DappledForest.root(point,center));continue;}
                int darkCherry=DarkCherryWoodland.kind(seed,x,z,drainage,climate);
                if(darkCherry>0){result.add(DarkCherryWoodland.root(point,center,darkCherry));continue;}
                boolean birch=Long.remainderUnsigned(v>>>16,10)<3;
                int height=point.young()?(birch?9:10)+(int)Long.remainderUnsigned(v>>>24,3):(birch?12:14)+(int)Long.remainderUnsigned(v>>>24,birch?5:7);
                result.add(new Root(x,center.height()+1,z,birch,height,(int)(v>>>32)));
            }
        return result;
    }
}
