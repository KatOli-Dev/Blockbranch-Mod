package blockbranch.realistic;

import java.util.*;

/** Existing dry gravelly hills and colder high summits with static retained snow/land ice. */
public final class ColdUplands {
    public static final int GRAVELLY=1,FROZEN=2;
    public static String key(int kind){return kind==FROZEN?"frozen_peaks":"windswept_gravelly_hills";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceKind(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var s=d.sample(x,z);int e=s.height()+1-TerrainShape.SEA_LEVEL;
        if(e<120||e>7000||LandVegetation.surfaceReserved(d,x,z))return 0;
        var q=c.sample(x,z,d::sample);double slope=SlopeCorrection.slope(d,x,z);
        if(e>4200){
            if(MountainPeaks.kind(seed,x,z,d,c)>0)return 0;
            if(SnowyPlains.contains(x,z,d,c)||SnowyPlains.surfaceContains(x,z,d,c)||SnowySlopes.contains(x,z,d,c)||SnowySlopes.surfaceContains(x,z,d,c))return 0;
            if(q.meanTemperatureC()>-12||q.meanTemperatureC()<-45||q.moisture()<.30||slope>1.2)return 0;
            int lower=0,max=s.height();for(int dz=-512;dz<=512;dz+=512)for(int dx=-512;dx<=512;dx+=512){if(dx==0&&dz==0)continue;int near=d.sample(x+dx,z+dz).height();max=Math.max(max,near);if(near<=s.height()-40)lower++;}
            return lower>=5&&max-s.height()<=120?FROZEN:0;
        }
        if(e>2000||q.meanTemperatureC()<4||q.meanTemperatureC()>12||q.moisture()<.12||q.moisture()>=.35||slope<=.45||slope>1.2)return 0;
        if(WindsweptUplands.kind(seed,x,z,d,c)>0||WindsweptUplands.surfaceKind(seed,x,z,d,c)>0)return 0;
        if(Badlands.kind(seed,x,z,d,c)>0||Badlands.surfaceKind(seed,x,z,d,c)>0||Plains.potential(seed,x,z,d,c)
                ||RoutedRiver.bankContains(seed,x,z,d,c)||RoutedRiver.surfaceBankContains(seed,x,z,d,c))return 0;
        return GRAVELLY;
    }
    public static boolean encoded(int code){return code>=8&&code<=13||code>=16&&code<=31;}
    public static boolean snowCap(int code){return code>=16&&code<=23;}
    public static int depth(int code){return code>=16?3+(code-16)%8:code>=11?3:code>=8?code-7:3;}
    static boolean soil(long seed,int x,int z,DrainageNetwork d){return SlopeCorrection.slope(d,x,z)<=.68&&SurfacePatches.signed(seed^0x47524156454C534FL,x,z,39)+.12*SurfacePatches.signed(seed^0x47524156454C4544L,x,z,6)>.28;}
    static int cover(long seed,int x,int z,int kind,DrainageNetwork d,GroundEcology.Field ecology){
        var site=ecology.sample(x,z);
        if(kind==FROZEN){
            double patch=SurfacePatches.signed(seed^0x46524F5A454E4341L,x,z,47)+.10*SurfacePatches.signed(seed^0x46524F5A454E4544L,x,z,7);
            int depth=3+(int)Math.round(4*Math.clamp(.5+.25*site.retention()+.20*patch,0,1));
            boolean snow=site.retention()+.45*patch-.35*site.exposure()>-.28;
            return(snow?16:24)+depth-3;
        }
        if(soil(seed,x,z,d)){
            double colony=GroundEcology.colonyStrength(seed,0x4752415647524153L,x,z,29,7,17,site);
            double roll=(OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
            return roll<.20*colony?(site.wetness()>.30?13:12):11;
        }
        double mantle=SurfacePatches.sample(seed^0x47524156454C4D41L,x,z,31)+.10*site.retention();
        return mantle>.65?10:mantle>.35?9:8;
    }
    static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var out=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);int radius=WoodlandTrees.RADIUS;
        for(var p:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=p.x(),z=p.z();if(kind(seed,x,z,d,c)!=GRAVELLY||surfaceKind(seed,x,z,d,c)!=GRAVELLY||!soil(seed,x,z,d))continue;
            var site=ecology.sample(x,z);long h=OakBirchWoodland.mix(p.priority()^0x47524156454C5452L);double colony=GroundEcology.colonyStrength(seed,0x47524156454C434FL,x,z,59,15,34,site);
            if((h>>>11)*0x1.0p-53>=.10+.24*colony||!WindsweptTrees.supportedFootprint(x,z,d,site,true))continue;
            boolean spruce=c.sample(x,z,d::sample).meanTemperatureC()<8;int height=p.young()?(spruce?8:6)+(int)Long.remainderUnsigned(h>>>24,4):(spruce?12:10)+(int)Long.remainderUnsigned(h>>>24,7);
            out.add(new WoodlandTrees.Root(x,d.sample(x,z).height()+1,z,false,height,(int)(h>>>32),spruce?15:1));
        }
        return out;
    }
    public record Point(int x,int z){}
    static int interiorCount(long seed,int x,int z,int kind,DrainageNetwork d,ClimateField c){int n=0;for(int dz=-64;dz<=64;dz+=16)for(int dx=-64;dx<=64;dx+=16)if(kind(seed,x+dx,z+dz,d,c)==kind)n++;return n;}


    private ColdUplands(){}
}
