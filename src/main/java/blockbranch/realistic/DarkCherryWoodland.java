package blockbranch.realistic;

import java.util.*;

/** Two named vanilla stands in remaining temperate woodland; static composition, not a stand-age simulation. */
public final class DarkCherryWoodland {
    public static final int DARK=1,CHERRY=2,DARK_FAMILY=10,CHERRY_FAMILY=11;
    public static String key(int kind){return kind==CHERRY?"cherry_grove":"dark_forest";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return selected(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c,false);}
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return selected(seed,x,z,d,c,true);}
    private static int selected(long seed,int x,int z,DrainageNetwork d,ClimateField c,boolean physical){
        boolean open=physical?OakBirchWoodland.surfacePotential(seed,x,z,d,c)&&!Forest.surfacePotential(seed,x,z,d,c):OakBirchWoodland.contains(seed,x,z,d,c);
        if(!open||(physical?Swamp.surfaceSoilContains(seed,x,z,d,c):Swamp.contains(seed,x,z,d,c))||(physical?TemperateForestVariants.surfaceKind(seed,x,z,d,c):TemperateForestVariants.kind(seed,x,z,d,c))>0)return 0;
        var q=c.sample(x,z,d::sample);int elevation=d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
        if(q.meanTemperatureC()>=8&&q.meanTemperatureC()<=16&&q.moisture()<=.75&&elevation>=400)return CHERRY;
        return q.moisture()>=.6&&BirchForest.province(seed^0x4441524B464F5245L,x,z)>.45?DARK:0;
    }
    public static int family(int kind){return kind==CHERRY?CHERRY_FAMILY:DARK_FAMILY;}
    static WoodlandTrees.Root root(TreeScatter.Point p,DrainageNetwork.Sample terrain,int kind){
        long h=OakBirchWoodland.mix(p.priority()^0x4441524B43484552L);boolean middle=Long.remainderUnsigned(h>>>24,10)<4;
        int height=kind==CHERRY?(p.young()?5+(int)Long.remainderUnsigned(h,5):middle?10+(int)Long.remainderUnsigned(h,4):14+(int)Long.remainderUnsigned(h,5)):(p.young()?7+(int)Long.remainderUnsigned(h,6):middle?16+(int)Long.remainderUnsigned(h,5):21+(int)Long.remainderUnsigned(h,7));
        return new WoodlandTrees.Root(p.x(),terrain.height()+1,p.z(),false,height,(int)(h>>>32),family(kind));
    }
    /** Keep connected, unequal native scaffold geometry and express species through native wood/leaves. */
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){
        boolean cherry=root.family()==CHERRY_FAMILY;int log=cherry?101:92,leaf=cherry?103:94;
        var tree=ForestTrees.tree(root).stream().map(v->new WoodlandTrees.Voxel(v.x(),v.y(),v.z(),v.code()<=3?log+v.code()-1:leaf+WoodlandTrees.leafDistance(v.code()))).toList();
        return cherry?tree:DarkOakBranches.taper(root,tree);
    }
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,int kind){
        if(kind==CHERRY){
            if(!clear)return 3;
            double colony=GroundEcology.colonyStrength(seed,0x4348455252595045L,x,z,32,10,21,site),tuft=GroundEcology.colonyStrength(seed,0x4348455252594752L,x,z,24,5,12,site);
            long h=OakBirchWoodland.mix(seed^0x504554414C535041L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
            if(unit(h)<.7*colony&&FlowerMicrosites.accepts(seed^0x434845525259504CL,x,z))return 37+(int)Long.remainderUnsigned(h>>>20,16);
            return unit(h)<.2*tuft?6:3;
        }
        int base=TemperateForestVariants.birchCover(seed^0x4441524B464C4F52L,x,z,site,clear);
        if(clear&&site.shaded()&&site.wetness()>.5&&unit(OakBirchWoodland.mix(seed^x*1297L^z*1237L))<.2*GroundEcology.colonyStrength(seed,0x4441524B46554E47L,x,z,24,3,8,site))return 5;
        return base;
    }
    public record Point(int x,int z){}

    public record View(double x,double y,double z,float yaw,float pitch){}

    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
    private DarkCherryWoodland(){}
}
