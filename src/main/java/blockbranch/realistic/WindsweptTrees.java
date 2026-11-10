package blockbranch.realistic;
import java.util.*;

/** Irregular oak/spruce cohorts rooted on actual soil ledges, with sparse exposed and wooded stands. */
public final class WindsweptTrees {
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);int radius=WoodlandTrees.RADIUS;
        for(var p:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=p.x(),z=p.z(),kind=WindsweptUplands.kind(seed,x,z,d,c);if(kind==0||WindsweptUplands.surfaceKind(seed,x,z,d,c)==0||!WindsweptUplands.soil(seed,x,z,d))continue;
            var site=ecology.sample(x,z);long h=OakBirchWoodland.mix(p.priority()^0x57494E445354454DL);
            double colony=GroundEcology.colonyStrength(seed,0x57494E4452454352L,x,z,56,14,35,site);
            if((h>>>11)*0x1.0p-53>=(kind==2?.3+.65*colony:.035+.14*colony))continue;
            int ground=d.sample(x,z).height();if(!supportedFootprint(x,z,d,site,kind))continue;
            int height=p.young()?5+(int)Long.remainderUnsigned(h>>>24,5):10+(int)Long.remainderUnsigned(h>>>24,9);
            boolean spruce=Long.remainderUnsigned(h>>>16,100)<(kind==2?65:40);
            result.add(new WoodlandTrees.Root(x,ground+1,z,false,height,(int)(h>>>32),spruce?15:14));
        }
        return result;
    }
    /** Retain old flat footholds; retentive Forest may follow a smooth sloping soil plane. */
    static boolean supportedFootprint(int x,int z,DrainageNetwork d,GroundEcology.Site site,int kind){
        return supportedFootprint(x,z,d,site,kind==WindsweptUplands.FOREST);
    }
    /** Shared physical support rule; callers explicitly opt into retentive sloping planes. */
    static boolean supportedFootprint(int x,int z,DrainageNetwork d,GroundEcology.Site site,boolean allowPlane){
        int radius=WoodlandTrees.RADIUS,h=d.sample(x,z).height();double gx=(d.sample(x+radius,z).height()-d.sample(x-radius,z).height())/(2.*radius),gz=(d.sample(x,z+radius).height()-d.sample(x,z-radius).height())/(2.*radius);
        boolean flat=true,plane=true;
        for(int dz=-radius;dz<=radius;dz+=radius)for(int dx=-radius;dx<=radius;dx+=radius){
            if(LandVegetation.surfaceReserved(d,x+dx,z+dz))return false;
            double rise=d.sample(x+dx,z+dz).height()-h;if(Math.abs(rise)>5)flat=false;
            if(Math.abs(rise)>10||Math.abs(rise-gx*dx-gz*dz)>2)plane=false;
        }
        return flat||allowPlane&&site.retention()>=-.25&&plane;
    }
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){return root.family()==15?TaigaTrees.tree(root):ForestTrees.tree(root);}
    private WindsweptTrees(){}
}
