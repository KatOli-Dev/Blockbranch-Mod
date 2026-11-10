package blockbranch.realistic;

/** Confirmed terminal-lake open water; sediment veneers and shallow macrophyte proxies never carve terrain. */
public final class LakeSediment {
    public record Site(int height,int water,boolean lake,boolean ocean,boolean channel,double slope){
        public int depth(){return water==DrainageNetwork.DRY?0:water-height-1;}
    }
    static Site site(DrainageNetwork d,int x,int z){
        var s=d.sample(x,z);boolean lake=d.terminalLakeWater(x,z,s);
        if(!lake)return new Site(s.height(),s.waterLevel(),false,d.oceanWater(x,z,s),false,0);
        double dx=(d.sample(x+16,z).height()-d.sample(x-16,z).height())/32.0,dz=(d.sample(x,z+16).height()-d.sample(x,z-16).height())/32.0;
        return new Site(s.height(),s.waterLevel(),true,d.oceanWater(x,z,s),d.channelAt(x,z).edge()>=0,Math.hypot(dx,dz));
    }
    public static boolean eligible(Site s){return s.lake()&&!s.ocean()&&!s.channel()&&s.depth()>=1;}
    public static boolean contains(int x,int z,DrainageNetwork d){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,d);
    }
    /** Shared aquatic palette: 5 mud/silt,6 clay,7 sand,8 gravel,9 exposed rock;10..12 submerged grass on mud/clay/sand. */
    public static int cover(long seed,int x,int z,boolean habitat,Site s,ClimateField.Sample climate){
        if(!habitat||!eligible(s))return 0;
        if(s.slope()>.5)return 9;
        double mineral=materialPatch(seed,x,z);
        int bed=s.slope()>.2?8:s.depth()<=6?(mineral>.78?8:mineral>.62?6:mineral>.3?7:5):mineral>.57?6:5;
        if(plantSuitable(s,climate)){
            // Keep existing macrophyte sites and their supporting substrate.
            double prior=OakBirchWoodland.stand(seed^0x4C414B45424544L,x*12,z*12);
            int plantBed=prior>.78?8:prior>.62?6:prior>.3?7:5;
            var ecology=new GroundEcology.Site(.8,0,0,0,0,false);
            double colony=GroundEcology.colonyStrength(seed,0x4C414B4557454544L,x,z,24,6,13,ecology);
            double chance=(OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
            if(plantBed!=8&&chance<.6*colony)return plantBed+5;
        }
        return bed;
    }
    public static boolean plantSuitable(Site s,ClimateField.Sample c){return eligible(s)&&s.depth()>=2&&s.depth()<=5&&s.slope()<=.18&&c.meanTemperatureC()>=4&&c.meanTemperatureC()<=27;}
    static double materialPatch(long seed,int x,int z){return SurfacePatches.sample(seed^0x4C414B45424544L,x,z,32);}
    public record Point(int x,int z){}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ){}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork d){
        var sample=d.sample(x,z);if(!d.terminalLakeWater(x,z,sample)||sample.waterLevel()<=sample.height()+1)return false;
        return !InlandLakeReedbed.surfaceContains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz))&&eligible(site(d,x,z));
    }
    private LakeSediment(){}
}
