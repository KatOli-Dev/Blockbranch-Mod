package blockbranch.realistic;

/** Warm humid sheltered marine fringe. Concavity is a static shelter proxy,not salinity or tides. */
public final class MangroveSwamp {
    private static final int[][] AROUND={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=20&&c.moisture()>=.60;}
    public static double slope(int x,int z,DrainageNetwork d){return Math.hypot((d.sample(x+16,z).height()-d.sample(x-16,z).height())/32.,(d.sample(x,z+16).height()-d.sample(x,z-16).height())/32.);}
    /** Confirm shallow ocean or immediately adjoining dry coast;never inland lake or routed river. */
    public static boolean local(int x,int z,DrainageNetwork d){
        var s=d.sample(x,z);if(s.height()<TerrainShape.SEA_LEVEL-4||s.height()>TerrainShape.SEA_LEVEL+3||d.channelAt(x,z).edge()>=0||slope(x,z,d)>.12)return false;
        if(s.waterLevel()!=DrainageNetwork.DRY)return d.habitatOceanWater(x,z,s)&&s.waterLevel()-s.height()-1<=3;
        for(var q:AROUND){int wx=x+q[0]*32,wz=z+q[1]*32;if(d.oceanWater(wx,wz,d.sample(wx,wz)))return true;}return false;
    }
    // Preserve the established dry fringe; definition 1 admits submerged habitat.
    public static int marineRays(int x,int z,DrainageNetwork d){int count=0;for(int n=0;n<8;n++){double a=n*Math.PI/4;int wx=x+(int)(Math.cos(a)*2048),wz=z+(int)(Math.sin(a)*2048);if(d.sample(x,z).waterLevel()==DrainageNetwork.DRY?d.oceanWater(wx,wz,d.sample(wx,wz)):d.habitatOceanWater(wx,wz,d.sample(wx,wz)))count++;}return count;}
    public static boolean exact(long seed,int x,int z,DrainageNetwork d,ClimateField c){return local(x,z,d)&&climateSuitable(c.sample(x,z,d::sample))&&marineRays(x,z,d)<=3;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return exact(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    /** Continuous substrate selection: muddy low margin,clay patches,higher rooted/moss banks. */
    public static int bed(long seed,int x,int z,DrainageNetwork d){
        var s=d.sample(x,z);double patch=patch(seed,x,z,24);return s.waterLevel()!=DrainageNetwork.DRY?(patch>.45?6:5):0;
    }
    public static int floor(long seed,int x,int z,DrainageNetwork d,GroundEcology.Site site){
        double above=d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;double p=patch(seed,x,z,24);
        if(above<=1.5+p)return 13;return p<-.25?19:16;
    }
    static double patch(long seed,int x,int z,int scale){return 1.4*SurfacePatches.signed(seed^0x4D414E47534F494CL,x,z,scale);}
    public record Point(int x,int z){}

    private MangroveSwamp(){}
}
