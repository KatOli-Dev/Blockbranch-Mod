package blockbranch.realistic;

/** Cold treeless,mesic low-relief plateaus; elevation acts through the existing climate lapse signal. */
public final class SnowyPlains {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=-20&&c.meanTemperatureC()<-6&&c.moisture()>=.4&&c.moisture()<=.85;}
    public static boolean contains(int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,d,c);
    }
    public static int cover(long seed,int x,int z,GroundEcology.Site site,ClimateField.Sample climate,boolean covered){if(climate.meanTemperatureC()<-14)return 0;int base=ColdTundra.cover(seed^0x534E4F57504C4149L,x,z,site);if(!covered)return base;return switch(base){case 6->3;case 8,11->14;case 9->2;default->base;};}
    public record Point(int x,int z){}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork d,ClimateField c){var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY||LandVegetation.surfaceReserved(d,x,z)||!climateSuitable(c.sample(x,z,d::sample))||SlopeCorrection.slope(d,x,z)>LowlandMeadow.GENTLE_SLOPE)return false;
        double dx=(d.sample(x+128,z).height()-d.sample(x-128,z).height())/256.0,dz=(d.sample(x,z+128).height()-d.sample(x,z-128).height())/256.0;if(Math.hypot(dx,dz)>.15)return false;
        int lo=s.height(),hi=lo;for(int oz=-128;oz<=128;oz+=128)for(int ox=-128;ox<=128;ox+=128){int h=d.sample(x+ox,z+oz).height();lo=Math.min(lo,h);hi=Math.max(hi,h);}return hi-lo<=64;
    }
    private SnowyPlains(){}
}
