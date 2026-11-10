package blockbranch.realistic;

import java.util.*;

/** Warm dry upland remnants and faces in the existing terrain; no simulated uplift, fire or wind. */
public final class SavannaUplands {
    public static final int PLATEAU=1,WINDSWEPT=2;
    public static String key(int kind){return kind==WINDSWEPT?"windswept_savanna":"savanna_plateau";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceKind(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var t=d.sample(x,z);int e=t.height()+1-TerrainShape.SEA_LEVEL;
        if(e<150||e>1600||LandVegetation.surfaceReserved(d,x,z))return 0;
        var q=c.sample(x,z,d::sample);
        if(q.meanTemperatureC()<=19||q.meanTemperatureC()>30||q.moisture()<.18||q.moisture()>=.55)return 0;
        if(Savanna.contains(seed,x,z,d,c)||Savanna.surfaceContains(seed,x,z,d,c)||Plains.potential(seed,x,z,d,c)||DrySteppe.surfaceContains(x,z,d,c)||Swamp.surfaceSoilContains(seed,x,z,d,c)||MangroveSwamp.exact(seed,x,z,d,c)||RoutedRiver.bankContains(seed,x,z,d,c)||RoutedRiver.surfaceBankContains(seed,x,z,d,c))return 0;
        double slope=SlopeCorrection.slope(d,x,z);
        if(slope>.35&&slope<=1.2)return WINDSWEPT;
        if(slope>.35||!WarmWoodland.surfaceContains(seed,x,z,d,c))return 0;
        int low=t.height();
        for(int[] p:new int[][]{{512,0},{-512,0},{0,512},{0,-512}})low=Math.min(low,d.sample(x+p[0],z+p[1]).height());
        return t.height()-low>=40?PLATEAU:0;
    }
    static boolean soil(long seed,int x,int z,DrainageNetwork d){return WindsweptUplands.soil(seed,x,z,d);}
    /** Native grass/dirt, coherent bare mineral-soil patches and living/dry grass colonies. */
    static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 9;
        double bare=SurfacePatches.signed(seed^0x5341565550534F49L,x,z,48)+.10*SurfacePatches.signed(seed^0x5341565550454447L,x,z,6);
        if(bare<-.38&&site.exposure()>.25)return 12;
        double colony=GroundEcology.colonyStrength(seed,0x5341565550475241L,x,z,32,9,22,site);
        double roll=(OakBirchWoodland.mix(seed^0x5341565550434F56L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
        if(colony>.18&&roll<(.12+.55*colony)*Math.clamp(.45+site.wetness(),.5,.95))return site.wetness()>.38&&roll<.22?11:10;
        return 9;
    }
    static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);int radius=WoodlandTrees.RADIUS;
        for(var p:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=p.x(),z=p.z(),kind=kind(seed,x,z,d,c);
            if(kind==0||surfaceKind(seed,x,z,d,c)==0||!soil(seed,x,z,d))continue;
            var site=ecology.sample(x,z);long h=OakBirchWoodland.mix(p.priority()^0x5341565550535445L);
            double colony=GroundEcology.colonyStrength(seed,0x5341565550434F48L,x,z,64,18,40,site);
            if((h>>>11)*0x1.0p-53>=(kind==PLATEAU?.06+.20*colony:.04+.14*colony))continue;
            if(!WindsweptTrees.supportedFootprint(x,z,d,site,true))continue;
            int height=p.young()?5+(int)Long.remainderUnsigned(h>>>24,4):9+(int)Long.remainderUnsigned(h>>>24,kind==PLATEAU?7:4);
            result.add(new WoodlandTrees.Root(x,d.sample(x,z).height()+1,z,false,height,(int)(h>>>32)));
        }
        return result;
    }
    static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){return SavannaTrees.uplandTree(root);}
    public record Point(int x,int z){}

    static int interiorCount(long seed,int x,int z,int kind,DrainageNetwork d,ClimateField c){int selected=0;for(int dz=-64;dz<=64;dz+=16)for(int dx=-64;dx<=64;dx+=16)if(kind(seed,x+dx,z+dz,d,c)==kind)selected++;return selected;}
    public record View(double x,double y,double z,float yaw,float pitch){}

    private SavannaUplands(){}
}
