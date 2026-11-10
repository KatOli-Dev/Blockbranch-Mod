package blockbranch.realistic;

/** Connected shallow marine water; thin static substrate sorting without reshaping the seabed. */
public final class MarineShelf {
    public record Site(int height,int water,boolean ocean,boolean lake,boolean river,double slope){
        public int depth(){return water==DrainageNetwork.DRY?0:water-height-1;}
    }
    static Site site(DrainageNetwork d,int x,int z){
        var s=d.sample(x,z);boolean ocean=d.habitatOceanWater(x,z,s);
        if(!ocean||s.waterLevel()-s.height()-1>200)return new Site(s.height(),s.waterLevel(),ocean,d.terminalLakeWater(x,z,s),false,0);
        double dx=(d.sample(x+16,z).height()-d.sample(x-16,z).height())/32.0,dz=(d.sample(x,z+16).height()-d.sample(x,z-16).height())/32.0;
        // A routed curve may geometrically overlap raw marine seabed near its outlet.
        // Confirmed connected marine water owns that side; no freshwater/salinity plume is simulated.
        return new Site(s.height(),s.waterLevel(),true,false,false,Math.hypot(dx,dz));
    }
    public static boolean eligible(Site s){return s.ocean()&&!s.lake()&&!s.river()&&s.water()==TerrainShape.SEA_LEVEL&&s.depth()>=1&&s.depth()<=200;}
    public static boolean contains(int x,int z,DrainageNetwork d){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,d);
    }
    /** Existing aquatic palette 5..12: fines/clay/sand/gravel/rock, with seagrass on shallow stable sand. */
    public static int cover(long seed,int x,int z,boolean habitat,Site s,ClimateField.Sample climate){
        if(!habitat||!eligible(s))return 0;
        if(s.slope()>.5)return 9;
        double mineral=materialPatch(seed,x,z);
        int bed=s.slope()>.2||s.depth()<=25&&mineral>.85?8:s.depth()<=40?(mineral>.7&&mineral<=.82?6:7):mineral>.65?6:5;
        if(plantSuitable(s,climate)){
            // Material repainting must not move the established seagrass colonies.
            double prior=OakBirchWoodland.stand(seed^0x5348454C46424544L,x*6,z*6);
            double colony=GroundEcology.colonyStrength(seed,0x5348454C46475253L,x,z,32,6,14,new GroundEcology.Site(.75,0,0,0,0,false));
            double chance=(OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
            if(prior<=.85&&!(prior>.7&&prior<=.82)&&chance<.55*colony)return 12;
        }
        return bed;
    }
    public static boolean plantSuitable(Site s,ClimateField.Sample c){return eligible(s)&&s.depth()>=2&&s.depth()<=7&&s.slope()<=.12&&c.meanTemperatureC()>=4&&c.meanTemperatureC()<=27;}
    static double materialPatch(long seed,int x,int z){return SurfacePatches.sample(seed^0x5348454C46424544L,x,z,64);}
    public record Point(int x,int z){}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ){}


    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork d){
        if(d.sample(x,z).bedrockCoast()||!eligible(site(d,x,z)))return false;
        return !LowGradientSandyShore.surfaceContains(x,z,(wx,wz)->LowGradientSandyShore.site(d,wx,wz))&&!GravelRiverMargin.contains(x,z,(wx,wz)->GravelRiverMargin.site(d,wx,wz))&&!InlandLakeReedbed.surfaceContains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz))&&!LakeSediment.surfaceContains(x,z,d)&&!RiverChannel.contains(x,z,d);
    }
    private MarineShelf(){}
}
