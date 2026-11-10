package blockbranch.realistic;

/** Named vanilla entries in the remaining open temperate woodland; completed closed stands stay intact. */
public final class TemperateForestVariants {
    public static final int FLOWER=1, OLD_BIRCH=2, BIRCH_FAMILY=9;
    public static String key(int kind){return kind==OLD_BIRCH?"old_growth_birch_forest":"flower_forest";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return selected(seed,x,z,d,c,false);
    }
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return selected(seed,x,z,d,c,true);}
    private static int selected(long seed,int x,int z,DrainageNetwork d,ClimateField c,boolean physical){
        if(physical?Swamp.surfaceSoilContains(seed,x,z,d,c)||(!OakBirchWoodland.surfacePotential(seed,x,z,d,c)||Forest.surfacePotential(seed,x,z,d,c)):Swamp.contains(seed,x,z,d,c)||!OakBirchWoodland.contains(seed,x,z,d,c))return 0;
        var climate=c.sample(x,z,d::sample);
        if(BirchForest.climateSuitable(climate)&&BirchForest.province(seed,x,z)>=.6)return OLD_BIRCH;
        return climate.meanTemperatureC()>=9&&climate.moisture()<=.8&&BirchForest.province(seed^0x464C4F5745524652L,x,z)<.55?FLOWER:0;
    }
    static WoodlandTrees.Root root(long seed,TreeScatter.Point p,DrainageNetwork.Sample terrain,GroundEcology.Site site,int kind){
        if(kind==FLOWER){
            if(unit(OakBirchWoodland.mix(p.priority()^0x464C4F5745524741L))>.78)return null;
            return ForestTrees.root(seed,p,terrain,site,false);
        }
        long h=OakBirchWoodland.mix(p.priority()^0x4F4C444249524348L);
        boolean birch=Long.remainderUnsigned(h>>>16,100)<94;
        int height=p.young()?7+(int)Long.remainderUnsigned(h,6):Long.remainderUnsigned(h>>>24,10)<3?15+(int)Long.remainderUnsigned(h,7):24+(int)Long.remainderUnsigned(h,7);
        if(!birch)height=Math.min(height,12);
        return new WoodlandTrees.Root(p.x(),terrain.height()+1,p.z(),birch,height,(int)(h>>>32),BIRCH_FAMILY);
    }
    /** Reuse balanced colony shapes with deciduous grass, litter and fern identities. */
    public static int birchCover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        return OldGrowthFloor.deciduousCover(seed^0x4249524348464C52L,x,z,site,clear);
    }
    /** Grass-based lit colonies with variable rendered-stem spacing, not uniform flower scatter. */
    public static int flowerCover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 3;
        // Broaden lit groups while retaining the shaded litter/fern field.
        // Maximum ellipse reach 24*1.2=28.8m fits the 32m neighbour search.
        double colony=GroundEcology.colonyStrength(seed,0x464C4F5745525041L,x,z,32,site.shaded()?9:12,site.shaded()?20:24,site);
        double tuft=GroundEcology.colonyStrength(seed,0x464C4F5747524153L,x,z,24,5,12,site);
        long h=OakBirchWoodland.mix(seed^0x464C4F5745524345L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double roll=unit(h);
        if(site.shaded())return roll<.3*tuft?4:roll<.18*colony?7:3;
        // Microsite competition caps crowding within stronger flower cores.
        if(colony>.15&&roll<Math.min(1,1.4*colony)&&FlowerMicrosites.accepts(seed^0x464C4F5745525350L,x,z)){
            // Smaller dandelion/lily patches complete the palette without
            // replacing the established broad colour bands or flower spacing.
            double accent=SurfacePatches.signed(seed^0x464C4F574143434EL,x,z,8);
            int species=accent<-.45?0:accent>.45?6:
                (int)Math.clamp((SurfacePatches.signed(seed^0x464C4F5753504543L,x,z,40)+1)*3.5,0,6);
            return 30+species;
        }
        return roll<.24*tuft?6:3;
    }
    public record Point(int x,int z){}

    public record View(double x,double y,double z,float yaw,float pitch){}

    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
    private TemperateForestVariants(){}
}
