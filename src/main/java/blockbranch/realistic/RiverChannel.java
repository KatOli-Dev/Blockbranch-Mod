package blockbranch.realistic;

/** Open routed freshwater channel; static slope/geometry sorting, never a flow or sediment simulation. */
public final class RiverChannel {
    public record Site(int height,int water,DrainageNetwork.Channel channel,boolean lake,boolean ocean,double slope){
        public int depth(){return water==DrainageNetwork.DRY?0:water-height-1;}
        public double lateral(){return channel.halfWidth()>0?channel.distance()/channel.halfWidth():Double.POSITIVE_INFINITY;}
    }
    static Site site(DrainageNetwork d,int x,int z){
        var s=d.sample(x,z);
        if(s.waterLevel()==DrainageNetwork.DRY)return new Site(s.height(),s.waterLevel(),DrainageNetwork.Channel.NONE,false,false,0);
        boolean lake=d.terminalLakeWater(x,z,s),ocean=d.habitatOceanWater(x,z,s);
        if(lake||ocean)return new Site(s.height(),s.waterLevel(),DrainageNetwork.Channel.NONE,lake,ocean,0);
        var c=d.channelAt(x,z);if(c.edge()<0)return new Site(s.height(),s.waterLevel(),c,false,false,0);
        double dx=(d.sample(x+16,z).height()-d.sample(x-16,z).height())/32.0,dz=(d.sample(x,z+16).height()-d.sample(x,z-16).height())/32.0;
        return new Site(s.height(),s.waterLevel(),c,false,false,Math.hypot(dx,dz));
    }
    public static boolean eligible(Site s){return !s.lake()&&!s.ocean()&&s.channel().edge()>=0&&s.depth()>=1&&s.water()==s.channel().waterLevel()&&s.lateral()<1;}
    public static boolean contains(int x,int z,DrainageNetwork d){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        if(!eligible(site(d,x,z)))return false;
        return !GravelRiverMargin.contains(x,z,(wx,wz)->GravelRiverMargin.site(d,wx,wz))&&!InlandLakeReedbed.contains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz))&&!LakeSediment.contains(x,z,d)&&!LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(d,wx,wz));
    }
    static boolean surfaceContains(int x,int z,DrainageNetwork d){
        if(!eligible(site(d,x,z)))return false;
        return !GravelRiverMargin.surfaceContains(x,z,(wx,wz)->GravelRiverMargin.site(d,wx,wz))&&!InlandLakeReedbed.surfaceContains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz))&&!LakeSediment.surfaceContains(x,z,d)&&!LowGradientSandyShore.surfaceContains(x,z,(wx,wz)->LowGradientSandyShore.site(d,wx,wz));
    }
    /** Reuses aquatic codes 5..12: fines/clay/sand/gravel/rock and source-water plants. */
    public static int cover(long seed,int x,int z,boolean habitat,Site s,ClimateField.Sample climate){
        if(!habitat||!eligible(s))return 0;
        double gradient = s.channel().gradient(), lateral = s.lateral();
        if(gradient>.02||s.slope()>.5)return 9;
        double mineralPatch = SurfacePatches.sample(seed^0x5249564552424544L,x,z,28);
        // Flow and slope bias grain size, but do not paint constant-distance
        // stripes along every bank. Neighboring depositional patches share a
        // continuous field; water depth and plant support remain exact.
        double coarseProbability = Math.clamp(.12+120*gradient+.8*s.slope()+.12*(1-lateral),0,1);
        // Shared aquatic palette: gravel8, sand7, clay6, mud5; rock9 above.
        // Plant variants preserve the bed code and add5 (mud/clay/sand only).
        int bedCode = gradient > .003 || mineralPatch < coarseProbability ? 8
            : gradient > .0005 ? 7
            : mineralPatch > .78 ? 6
            : mineralPatch > .62 ? 5 : 7;
        if(plantSuitable(s,climate)&&bedCode!=8){
            double colony=GroundEcology.colonyStrength(seed,0x5249564552504C54L,x,z,28,5,12,new GroundEcology.Site(.9,0,0,0,0,false));
            double chance=(OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
            if(chance<.5*colony)return bedCode+5;
        }
        return bedCode;
    }
    public static boolean plantSuitable(Site s,ClimateField.Sample c){return eligible(s)&&s.depth()>=2&&s.depth()<=5&&s.channel().gradient()<=.0005&&s.channel().halfWidth()>=24&&s.lateral()>=.55&&s.slope()<=.18&&c.meanTemperatureC()>=4&&c.meanTemperatureC()<=27;}
    public record Point(int x,int z){}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ){}

    private RiverChannel(){}
}
