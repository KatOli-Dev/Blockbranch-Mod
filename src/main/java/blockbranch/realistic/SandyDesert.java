package blockbranch.realistic;

/** Sandy Desert: static substrate/deposition proxies, never wind transport or dune carving. */
public final class SandyDesert {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=10&&c.meanTemperatureC()<=30&&c.moisture()<.12;}
    public static double province(long seed,int x,int z){
        int scale=12000,gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double a=Math.floorMod(x,scale)/(double)scale,b=Math.floorMod(z,scale)/(double)scale;a=a*a*(3-2*a);b=b*b*(3-2*b);
        return(value(seed,gx,gz)*(1-a)+value(seed,gx+1,gz)*a)*(1-b)+(value(seed,gx,gz+1)*(1-a)+value(seed,gx+1,gz+1)*a)*b;
    }
    private static double value(long seed,int x,int z){return(OakBirchWoodland.mix(seed^0x53414E4450524F56L^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,d,c);
    }
    /** Broad convexity has a rounding dead-band; local bodies grade veneer depth, never biome identity. */
    public static double mantle(long seed,int x,int z,DrainageNetwork d){
        var center=d.sample(x,z);double dx=(d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.0,dz=(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.0;
        double hollow=(d.sample(x-128,z).height()+d.sample(x+128,z).height()+d.sample(x,z-128).height()+d.sample(x,z+128).height())*.25-center.height();
        double supply=GroundEcology.colonyStrength(seed,0x53414E44534F494CL,x,z,64,18,44,GroundEcology.NEUTRAL);
        double rock=GroundEcology.colonyStrength(seed,0x53414E44524F434BL,x,z,96,22,56,GroundEcology.NEUTRAL);
        return mantleFromSite(Math.hypot(dx,dz),hollow,supply,rock);
    }
    static double mantleFromSite(double slope,double hollow,double supply,double rock){
        double convex=Math.max(0,-hollow-1.25),retention=Math.clamp(1-convex/8-slope*.8,0,1),exposure=Math.clamp(slope/.30+convex/6,0,1);
        return Math.clamp(.70*retention+.30*supply-.5*rock*(.35+.65*exposure),0,1);
    }
    static int bareCover(double mantle){return mantle>=.62?8:mantle>=.41?13:mantle>=.28?14:mantle>=.15?15:16;}
    public static boolean cactus(int code){return code>=10&&code<=12||code>=18&&code<=20;}

    private static long cell(long seed,int x,int z){return OakBirchWoodland.mix(seed^0x434143545553L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);}
    private static double cactusColony(long seed,int x,int z){return GroundEcology.colonyStrength(seed,0x53414E4443414354L,x,z,40,10,23,GroundEcology.NEUTRAL);}
    private static boolean potential(long seed,int x,int z){double roll=(cell(seed,x,z)>>>11)*0x1.0p-53;return roll<.008&&roll<.008*cactusColony(seed,x,z);}
    private static boolean scrub(long seed,int x,int z){
        double roll=(OakBirchWoodland.mix(cell(seed,x,z))>>>11)*0x1.0p-53;
        return roll<.025&&roll<.025*GroundEcology.colonyStrength(seed,0x53414E4442555348L,x,z,32,6,15,GroundEcology.NEUTRAL);
    }
    /** Raw composition candidate; neighbourhood checks belong only to the queried winner. */
    private static boolean succulent(long seed,int x,int z){return scrub(seed,x,z)&&favoursSucculent(seed,x,z);}
    private static boolean favoursSucculent(long seed,int x,int z){
        double favour=Math.clamp((cactusColony(seed,x,z)-.25)*2,0,1);
        return (OakBirchWoodland.mix(cell(seed,x,z)^0x53554343554C454EL)>>>11)*0x1.0p-53<favour;
    }
    private static boolean clearSides(int x,int z,int height,DrainageNetwork d){for(int[] step:DIR)if(d.sample(x+step[0],z+step[1]).height()>height)return false;return true;}
    /** 8..12 deep sand/vegetation;13 thin sand,14 gravel lag,15 sandstone,16 rock;17..20 thin-sand plants. */
    public static int cover(long seed,int x,int z,boolean habitat,DrainageNetwork d){
        var t=d.sample(x,z);if(!habitat||t.waterLevel()!=DrainageNetwork.DRY||d.channelAt(x,z).edge()>=0)return 0;
        int bare=bareCover(mantle(seed,x,z,d));if(bare!=8&&bare!=13)return bare;boolean thin=bare==13;
        long h=cell(seed,x,z);
        if(potential(seed,x,z)){
            boolean clear=true;for(int[] step:DIR)if(d.sample(x+step[0],z+step[1]).height()>t.height())clear=false;
            for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)if((dx!=0||dz!=0)&&potential(seed,x+dx,z+dz)&&Long.compareUnsigned(cell(seed,x+dx,z+dz),h)>0)clear=false;
            if(clear)return(thin?18:10)+(int)Long.remainderUnsigned(h>>>24,3);
        }
        if(!scrub(seed,x,z))return bare;
        if(favoursSucculent(seed,x,z)&&clearSides(x,z,t.height(),d)){
            boolean clear=true;
            for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){
                // Reserve the original spacing halo even across old/new chunk edges.
                if(potential(seed,x+dx,z+dz))clear=false;
                int priority=Long.compareUnsigned(cell(seed,x+dx,z+dz),h);
                if((dx!=0||dz!=0)&&(priority>0||priority==0&&(dz>0||dz==0&&dx>0))&&succulent(seed,x+dx,z+dz))clear=false;
            }
            if(clear)return(thin?18:10)+(int)Long.remainderUnsigned(h>>>24,3);
        }
        return thin?17:9;
    }
    public record Point(int x,int z){}



    private static final int[][] DIR={{1,0},{-1,0},{0,1},{0,-1}};
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(LandVegetation.surfaceReserved(d,x,z)||!climateSuitable(c.sample(x,z,d::sample))||SlopeCorrection.slope(d,x,z)>.30||province(seed,x,z)<.48)return false;
        return d.sample(x,z).waterLevel()==DrainageNetwork.DRY;
    }
    private SandyDesert(){}
}
