package blockbranch.realistic;

/** Warm mesic lake-fringing wooded swamp; static saturation proximity,not simulated flood recurrence. */
public final class Swamp {
    public record Bank(int height,int water,double slope,boolean adjacent){public int above(){return height+1-water;}}
    static Bank bank(int x,int z,DrainageNetwork d){
        var s=d.sample(x,z);int basin=s.basin();
        if(s.waterLevel()!=DrainageNetwork.DRY||!s.closed()||d.parent(basin)!=-1||d.waterLevel(basin)>TerrainShape.SEA_LEVEL+600||d.waterLevel(basin)<=TerrainShape.SEA_LEVEL)return null;
        int water=d.waterLevel(basin),above=s.height()+1-water;if(above<0||above>3||d.channelAt(x,z).edge()>=0)return null;
        double angle=Math.atan2(z-d.nodeZ(basin),x-d.nodeX(basin));if(Math.hypot(x-d.nodeX(basin),z-d.nodeZ(basin))>d.lakeShoreRadius(basin,angle)+40)return null;
        double slope=Math.hypot((d.sample(x+16,z).height()-d.sample(x-16,z).height())/32.,(d.sample(x,z+16).height()-d.sample(x,z-16).height())/32.);if(slope>.20)return null;
        boolean wet=false;for(int[] v:AROUND){int wx=x+v[0]*24,wz=z+v[1]*24;var q=d.sample(wx,wz);if(d.terminalLakeWater(wx,wz,q))wet=true;}
        return wet?new Bank(s.height(),water,slope,true):null;
    }
    private static final int[][] AROUND={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=10&&c.meanTemperatureC()<=27&&c.moisture()>=.55&&c.moisture()<=.95;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;return bank(x,z,d)!=null&&climateSuitable(c.sample(x,z,d::sample));}
    /** Historical vegetation footprint; physical soil has a separate block-resolution predicate. */
    public static boolean soilContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var actual=d.sample(x,z);if(actual.waterLevel()!=DrainageNetwork.DRY||d.channelAt(x,z).edge()>=0)return false;
        return contains(seed,x,z,d,c)||bank(x,z,d)!=null&&climateSuitable(c.sample(x,z,d::sample));
    }
    static boolean surfaceSoilContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return bank(x,z,d)!=null&&climateSuitable(c.sample(x,z,d::sample));}
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){return cover(seed,x,z,site,clear,1.5);}
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,DrainageNetwork d){
        var s=d.sample(x,z);return cover(seed,x,z,site,clear,Math.clamp(s.height()+1-d.waterLevel(s.basin()),0,4));
    }
    private static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,double above){
        if(!clear)return 19; // Full solid rooted footing below stems and fallen timber.
        int substrate=substrate(seed,x,z,site,above);
        if(substrate==1&&WoodlandDetails.marshFlower(seed,x,z,site))return 20;
        double fern=GroundEcology.colonyStrength(seed,0x5357414D50464552L,x,z,24,6,13,site);
        long h=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);double v=(h>>>11)*0x1.0p-53;
        // Native mud has a lowered collision top;it cannot support litter or mushrooms.
        // Mushroom light suitability is not inferred from a single canopy column.
        int plant=v<.55*fern?1:site.shaded()&&v<.35&&substrate!=0?4:v>.83?3:0;
        return substrate==0?switch(plant){case 1->14;case 2->15;case 3->17;case 4->18;default->13;}
            :substrate==1?switch(plant){case 1->21;case 2->22;case 3->23;case 4->24;default->16;}
            :switch(plant){case 1->26;case 2->27;case 3->28;case 4->29;default->25;};
    }
    /** Continuous saturation/substrate fields select ground;per-block chance controls plants only. */
    static int substrate(long seed,int x,int z,GroundEcology.Site site,double above){
        double wet=site.wetness()-(site.shaded()?.04:0);
        double saturation=.74-.13*above+.12*(wet-.65)+.10*site.retention()+.16*patch(seed,0x5357534F494CL,x,z,24);
        if(saturation>.65+.07*patch(seed,0x53574D5544L,x,z,12))return 0;
        return saturation>.39+.08*patch(seed,0x53574D4F5353L,x,z,18)?1:2;
    }
    static double patch(long seed,long salt,int x,int z,int scale){return 1.4*SurfacePatches.signed(seed^salt,x,z,scale);}
    public record Point(int x,int z){}


    private Swamp(){}
}
