package blockbranch.realistic;

/** Temperate low-relief grassland, partitioned from the existing soil-supported meadow domain. */
public final class Plains {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=6&&c.meanTemperatureC()<=23&&c.moisture()>=.27&&c.moisture()<=.55;}
    static boolean potential(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        if(!climateSuitable(c.sample(x,z,d::sample))||!LowlandMeadow.potential(x,z,d,c)||OakBirchWoodland.potential(seed,x,z,d,c)||WarmWoodland.contains(seed,x,z,d,c))return false;
        double dx=(d.sample(x+128,z).height()-d.sample(x-128,z).height())/256.0,dz=(d.sample(x,z+128).height()-d.sample(x,z-128).height())/256.0;
        return Math.hypot(dx,dz)<=.15;
    }
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return potential(seed,x,z,d,c)&&!SunflowerPlains.contains(seed,x,z,d,c);}
    /** 7 bare grass,8 short grass,9 native two-block tall grass;4..6 occasional existing forbs. */
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 7;
        long cell=OakBirchWoodland.mix(seed^0x504C41494E53474CL^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        // Forbs occupy their own colonies instead of being thinned a second time
        // by unrelated grass colonies. A colony shares one species across its
        // jittered ellipse, including chunk edges, while dry/exposed sites thin it.
        double flowerChance=unit(OakBirchWoodland.mix(cell^0xF10AE25L));
        if(!site.shaded()&&flowerChance<.42){
            var flowers=GroundEcology.colony(seed,0x504C41494E53464CL,x,z,40,5,11,site);
            double soil=Math.clamp(1-Math.abs(site.wetness()-.5)*2,.25,1)*(1-.5*site.exposure());
            if(flowerChance<.42*flowers.strength()*soil)return 4+(int)Long.remainderUnsigned(flowers.seed(),3);
        }
        double grass=GroundEcology.colonyStrength(seed,0x504C41494E534752L,x,z,28,12,22,site);
        double vigor=Math.clamp(.55+.45*site.wetness()+.12*site.retention()-.25*site.exposure(),.2,.9)*(site.shaded()?.35:1);
        if(unit(cell)>(.12+.84*grass)*vigor)return 7;
        double tall=GroundEcology.colonyStrength(seed,0x504C41494E53544CL,x,z,32,7,17,site);
        return !site.shaded()&&unit(OakBirchWoodland.mix(cell^0x7A11L))<.65*tall*Math.clamp(site.wetness()+.2*site.retention(),.1,.9)?9:8;
    }
    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
    public record Point(int x,int z){}

    private Plains(){}
}
