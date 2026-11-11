package blockbranch.realistic;

/** Finite falsification controls, not empirical calibration or Earth-area accounting. */
public final class ContinentalIceCheck {
    public static void main(String[] args){
        require(ContinentalIceSchema.schema(264,1)==265&&ContinentalIceSchema.baseSchema(265,1)==264,"Complete foundation");
        for(int v:new int[]{82,257,258,259,260,261,262,263,264,266})require(!ContinentalIceSchema.matches(v,1),"Reject partial/future foundations");
        require(ContinentalIceSchema.matches(264,0)&&!ContinentalIceSchema.matches(265,0)&&!ContinentalIceSchema.matches(265,2),"Absent/future definitions");
        for(int z:new int[]{0,500000,1000000,1258291})require(PolarClimate.cooling(PolarClimate.latitude(z))==0,"Central climate unchanged");
        require(PolarClimate.cooling(1)==36,"Disclosed edge convention");
        double previous=0;for(int z=0;z<=2097152;z+=256){double q=PolarClimate.cooling(PolarClimate.latitude(z));require(q>=previous&&q<=36,"Monotone bounded transition");previous=q;}
        for(int edge:new int[]{1258291,2097152})require(Math.abs(PolarClimate.cooling(PolarClimate.latitude(edge+1))-PolarClimate.cooling(PolarClimate.latitude(edge-1)))<1e-6,"No climate seam at taper ends");
        var flat=new DrainageNetwork.Sample(63,DrainageNetwork.DRY,0,false);
        for(long seed:new long[]{424242,1,-17}){
            var old=new ClimateField(seed);var fresh=new ClimateField(seed,true);
            for(int z:new int[]{0,1000000})require(old.sample(12345,z,(x,q)->flat).equals(fresh.sample(12345,z,(x,q)->flat)),"Bit-exact unchanged central sample");
            for(int z:new int[]{-2097152,2097152}){
                double mean=fresh.sample(12345,z,(x,q)->flat).meanTemperatureC();var season=CoolDesert.season(z,mean);
                require(season.warmestC()+4< -1&&season.coldestC()< -40,"Genuinely cold winter AND summer lowland, not mean-cold seasonal snow");
                require(LandIceField.climateStrength(z,mean,.1)==0,"Cold arid rock remains a counterexample");
            }
        }
        var d=new DrainageNetwork(new TerrainShape(424242),424242,true,true,true,true,true,true,true);var c=new ClimateField(424242,true);
        for(int[] p:new int[][]{{-1552000,-1904000},{-1456000,-1904000}}){
            require(d.continentalIceThickness(p[0],p[1])>64,"Nonvacuous lowland/upland body exceeds legacy cap limit");
            require(d.sample(p[0],p[1]).height()-d.iceReference().sample(p[0],p[1]).height()==d.continentalIceThickness(p[0],p[1]),"Physical body and original rock bed agree");
            require(d.continentalIceThickness(p[0],p[1])<=ContinentalIceField.MAX_DEPTH,"Stored depth bound");
        }
        for(int[] p:new int[][]{{272000,-720000},{-1648000,-1904000},{-1619720,-1904000}})require(d.continentalIceThickness(p[0],p[1])==0,"Ordinary/marine/exposed-shore negative controls");
        // First positive edge in the same finite 32 km coastal bracket used by
        // native measurements. Preserve the physical coast; do not chase seeds.
        int edge=Integer.MIN_VALUE;
        for(int x=-1619720;x<=-1587720;x+=64)if(d.continentalIceThickness(x,-1904000)>0){for(int q=x-63;q<=x;q++)if(d.continentalIceThickness(q,-1904000)>0){edge=q;break;}break;}
        require(edge!=Integer.MIN_VALUE,"Nonvacuous inland mixed ice/rock edge");
        int maxStep=0;for(int x=edge-256;x<=edge+256;x++){int a=d.sample(x,-1904000).height()-d.iceReference().sample(x,-1904000).height(),b=d.sample(x+1,-1904000).height()-d.iceReference().sample(x+1,-1904000).height();maxStep=Math.max(maxStep,Math.abs(a-b));}
        require(maxStep<=1,"Finite one-block edge continuity, not a bulk ice wall at zero admission");
        require(d.continentalIceThickness(1039616,1296768)==0&&d.landIceThickness(1039616,1296768)>0,"Cold alpine peak retains its alpine cap, not a continental sheet in a mild lowland regime");
        require(LandIceField.climateStrength(1677721,-8,.7)==0,"Cold annual mean with a thawing warm season is seasonal-snow terrain, not perennial admission");
        for(int[] p:new int[][]{{-1556480,-1904000},{-1552000,-1900544}})for(int axis=0;axis<2;axis++){
            for(int q=-128;q<128;q++){int x=p[0]+(axis==0?q:0),z=p[1]+(axis==1?q:0),nx=x+(axis==0?1:0),nz=z+(axis==1?1:0);
                require(d.continentalIceThickness(x,z)>0&&Math.abs(d.sample(x,z).height()-d.sample(nx,nz).height())<=1,"Finite actual ice surface is continuous across the fixed 16 km bed mesh, no artificial contour seam");}
        }
        var marine=d.iceReference().sample(-1008000,-1776000);
        require(d.iceReference().habitatOceanWater(-1008000,-1776000,marine)&&!d.iceReference().oceanWater(-1008000,-1776000,marine),"Native river-mouth witness distinguishes corrected/legacy marine ownership");
        require(d.continentalIceThickness(-1008000,-1776000)==0&&!d.permanentFreshwaterLid(-1008000,-1776000),"Basin-owned marine water never becomes the new freshwater lid or grounded sheet");
        var river=d.iceReference().sample(-1589245,-1966053);
        require(!d.iceReference().habitatOceanWater(-1589245,-1966053,river)&&river.waterLevel()==TerrainShape.SEA_LEVEL&&d.permanentFreshwaterLid(-1589245,-1966053),"A true sea-level river is not discarded just for sharing the sea datum");
        int marineX=-1872000,marineZ=-1904000;var marineSite=d.sample(marineX,marineZ);
        require(d.habitatOceanWater(marineX,marineZ,marineSite)&&IcebergField.habitat(424242,marineX,marineZ,d,c),"Existing open-water iceberg habitat remains marine");
        require(d.continentalIceThickness(marineX,marineZ)==0&&!d.permanentFreshwaterLid(marineX,marineZ),"Grounded bodies and freshwater lids exclude iceberg water");
        require(FrozenOcean.column(424242,marineX>>4,marineZ>>4,d,c,true).length==0,"Open iceberg habitat retains water between berg spans, no Frozen Ocean skin");
        require(FrozenOcean.column(424242,marineX>>4,marineZ>>4,d,c,false).length>0,"Separate Frozen Ocean cover remains supported");
        var bounds=new SpawnRegions.Bounds(-2097136,-2097136,2097136,2097136).reserve(10);
        var spawn=SpawnRegions.select(424242,bounds,d,c);require(bounds.allows(spawn.x(),spawn.z())&&SpawnRegions.eligible(spawn.x(),spawn.z(),d,c),"Mild dry safe/diverse-region spawn policy retained");
        System.out.println("PASS polar conventions and actual static body/negative controls; mixed-edge x="+edge+" maxAddedStep="+maxStep+" spawn="+spawn);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private ContinentalIceCheck(){}
}
