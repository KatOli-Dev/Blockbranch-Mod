package blockbranch.realistic;

import java.util.*;

/** Static dry sedimentary outcrops and soil-bearing woodland remnants; never carves terrain. */
public final class Badlands {
    public static final int BARE=1,WOODED=2,ERODED=3;
    public static String key(int kind){return kind==ERODED?"eroded_badlands":kind==WOODED?"wooded_badlands":"badlands";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceKind(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(d.erodedInterior(x,z))return ERODED;
        var s=d.sample(x,z);int elevation=s.height()+1-TerrainShape.SEA_LEVEL;
        if(elevation<150||elevation>1800||LandVegetation.surfaceReserved(d,x,z))return 0;
        var q=c.sample(x,z,d::sample);
        if(q.meanTemperatureC()<10||q.meanTemperatureC()>24||q.moisture()<.08||q.moisture()>=.32)return 0;
        double slope=SlopeCorrection.slope(d,x,z);if(slope>1.4)return 0;
        if(SurfacePatches.sample(seed^0x424144534544494DL,x,z,24000)<.52)return 0;
        if(SandyDesert.contains(seed,x,z,d,c)||SandyDesert.surfaceContains(seed,x,z,d,c)
                ||Savanna.contains(seed,x,z,d,c)||Savanna.surfaceContains(seed,x,z,d,c)
                ||SavannaUplands.kind(seed,x,z,d,c)>0||SavannaUplands.surfaceKind(seed,x,z,d,c)>0
                ||Plains.potential(seed,x,z,d,c)||RoutedRiver.bankContains(seed,x,z,d,c)||RoutedRiver.surfaceBankContains(seed,x,z,d,c))return 0;
        int low=s.height();for(int[] p:new int[][]{{512,0},{-512,0},{0,512},{0,-512}})low=Math.min(low,d.sample(x+p[0],z+p[1]).height());
        if(s.height()-low<45)return 0;
        if(q.moisture()>=.20&&slope<=.4)return WOODED;
        return slope>.15?BARE:0;
    }
    // Six cap states and a smooth local stratal datum fit the existing signed byte.
    // The 64 m sequence represents exposed sedimentary beds, not altitude paint.
    public static boolean encoded(int code){return code>=32&&code<=127;}
    public static int cap(int code){return (code-32)%6;}
    public static int datum(int code){return (code-32)/6-8;}
    public static int stratum(int code,int y){
        int band=Math.floorMod(y+datum(code),64);
        return band<18?0:band<30?1:band<42?2:band<50?3:band<53?4:band<56?5:0;
    }
    static boolean soil(long seed,int x,int z,DrainageNetwork d){
        if(SlopeCorrection.slope(d,x,z)>.4)return false;
        return SurfacePatches.signed(seed^0x424144534F494C50L,x,z,43)+.12*SurfacePatches.signed(seed^0x424144534F494C45L,x,z,7)>.02;
    }
    static int cover(long seed,int x,int z,int kind,DrainageNetwork d,GroundEcology.Field ecology){
        double slope=SlopeCorrection.slope(d,x,z);
        int cap=kind==WOODED&&soil(seed,x,z,d)?3:0;
        if(kind!=ERODED&&cap==0&&slope<.45&&SurfacePatches.signed(seed^0x42414453414E444DL,x,z,37)+.10*SurfacePatches.signed(seed^0x42414453414E4445L,x,z,5)>.05)cap=1;
        if(cap>0){
            var site=ecology.sample(x,z);double colony=GroundEcology.colonyStrength(seed,0x424144554E444552L,x,z,30,7,17,site);
            double roll=(OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
            if(roll<.08*colony)cap=cap==1?2:site.wetness()>.3?4:5;
        }
        int datum=(int)Math.round(4*SurfacePatches.signed(seed^0x4241445354524154L,x,z,320));
        return 32+(datum+8)*6+cap;
    }
    static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var out=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);int r=WoodlandTrees.RADIUS;
        for(var p:TreeScatter.points(seed,minX-r,minZ-r,maxX+r,maxZ+r)){
            int x=p.x(),z=p.z();if(kind(seed,x,z,d,c)!=WOODED||surfaceKind(seed,x,z,d,c)!=WOODED||!soil(seed,x,z,d))continue;
            var site=ecology.sample(x,z);double patch=GroundEcology.colonyStrength(seed,0x4241444F414B5354L,x,z,56,15,34,site);
            long h=OakBirchWoodland.mix(p.priority()^0x4241444F414B524FL);
            if((h>>>11)*0x1.0p-53>=.18+.32*patch||!WindsweptTrees.supportedFootprint(x,z,d,site,false))continue;
            int height=p.young()?6+(int)Long.remainderUnsigned(h>>>24,4):11+(int)Long.remainderUnsigned(h>>>24,8);
            out.add(new WoodlandTrees.Root(x,d.sample(x,z).height()+1,z,false,height,(int)(h>>>32),1));
        }
        return out;
    }
    public record Point(int x,int z){}
    static int interiorCount(long seed,int x,int z,int kind,DrainageNetwork d,ClimateField c){int n=0;for(int dz=-64;dz<=64;dz+=16)for(int dx=-64;dx<=64;dx+=16)if(kind(seed,x+dx,z+dz,d,c)==kind)n++;return n;}



    private Badlands(){}
}
