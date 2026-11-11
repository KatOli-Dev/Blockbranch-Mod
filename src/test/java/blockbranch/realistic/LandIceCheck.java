package blockbranch.realistic;
import java.util.*;
/** Source-bounded idealized controls and finite physical witnesses, no Earth-area target. */
public final class LandIceCheck {
    static final int[][][] SITES={{{1039616,1296768},{1039616,1297024},{592640,1392256}},{{-303232,-1872512},{-303232,-1872384}},{{-1423104,-1807872},{1456384,-49024}}};
    public static void main(String[] args){
        require(LandIceSchema.schema(262,1)==263&&LandIceSchema.baseSchema(263,1)==262,"Complete foundation");
        for(int v:new int[]{82,257,258,259,260,261,262,264})require(!LandIceSchema.matches(v,1),"No partial/future foundation");
        require(!LandIceSchema.matches(263,0)&&!LandIceSchema.matches(263,2),"Absent/unknown opt-in rejects263");
        require(LandIceField.climateStrength(0,-15,.7)==1,"Cold supplied synthetic land independent of elevation");
        require(LandIceField.climateStrength(1800000,-12,.7)==0,"Subzero annual mean with thawing warm season is seasonal-snow control");
        require(LandIceField.climateStrength(0,-30,.12)==0,"Cold arid rock is not inferred ice");
        require(LandIceField.climateStrength(0,-8,.7)==0,"Warm spells exceed safe bound despite cold monthly means");
        for(double t:new double[]{-9,-11})require(Math.abs(LandIceField.climateStrength(0,t-1e-6,.7)-LandIceField.climateStrength(0,t+1e-6,.7))<1e-9,"Continuous thermal response endpoints");
        for(int n=1;n<=LandIceField.MAX_DEPTH;n++)require(LandIceField.depth(LandIceField.code(n))==n,"Append-only thickness roles");
        int seedIndex=0,total=0;
        for(long seed:new long[]{424242,1,-17}){
            var old=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true);var now=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true);var c=new ClimateField(seed);var field=new LandIceField(seed,old);
            if(seed==424242){
                int[][] negatives={{-1904000,-1904000},{-1040000,-1904000},{-1008000,-1680000},{1904000,-1552000},{-1328000,-1904000},{-1360000,-1808000}};
                for(var p:negatives){require(field.thickness(p[0],p[1])==0&&old.sample(p[0],p[1]).equals(now.sample(p[0],p[1])),"Fixed ocean/coast/river/lake/seasonal/steep-cold negative control");}
                require(SlopeCorrection.slope(old,-1360000,-1808000)>.20,"Actual steep-cold negative exercises shedding support");
            }
            int positive=0,core=0,max=0,edge=0,samples=0,maxStep=0;var found=new ArrayList<Object>();
            for(var p:SITES[seedIndex]){
                int count=0,siteMax=0;
                for(int dz=-160;dz<=160;dz+=4)for(int dx=-160;dx<=160;dx+=4){
                    int x=p[0]+dx,z=p[1]+dz,n=field.thickness(x,z);var a=old.sample(x,z);var b=now.sample(x,z);samples++;
                    require(n==now.landIceThickness(x,z)&&b.height()==a.height()+n,"Exact geometry/depth composition");
                    require(a.waterLevel()==b.waterLevel()&&a.basin()==b.basin()&&a.closed()==b.closed()&&a.regionalHeight()==b.regionalHeight()&&a.regionalWaterLevel()==b.regionalWaterLevel()&&c.sample(x,z,old::sample).equals(c.sample(x,z,now::sample)),"Water/mean climate references unchanged");
                    if(n>0){positive++;count++;if(n>=8)core++;siteMax=Math.max(siteMax,n);max=Math.max(max,n);require(a.waterLevel()==DrainageNetwork.DRY&&!LandVegetation.surfaceReserved(old,x,z)&&LandVegetation.surfaceReserved(now,x,z),"Dry protected native support");require(LandIceField.climateStrength(z,c.sample(x,z,old::sample).meanTemperatureC(),c.atmosphericSupply(x,z,old::sample))>0,"Actual supported thermal/supply convention");}
                    int east=field.thickness(x+1,z),south=field.thickness(x,z+1);maxStep=Math.max(maxStep,Math.max(Math.abs(n-east),Math.abs(n-south)));if(n>0&&(east==0||south==0))edge++;
                }
                found.add(Map.of("center",Arrays.toString(p),"positive",count,"maxDepth",siteMax));
            }
            total+=positive;require(positive>0&&core>0&&max>=8&&edge>0,"Nonvacuous body, perennial fringe and mixed edge");require(max<=64&&maxStep<=3,"Bounded thickness and no one-block wall at cap edges");
            System.out.println("LAND_ICE seed="+seed+" window=+/-160 stride=4 samples="+samples+" positive="+positive+" core8="+core+" maxDepth="+max+" edge="+edge+" maxDepthStep="+maxStep+" sites="+found);
            if(seedIndex==0)sensitivity(seed,old,SITES[0]);seedIndex++;
        }
        System.out.println("PASS bounded perennial ice snapshots; original climate/water, mixed edges, negative controls, source-limited sensitivity; positive="+total);
    }
    private static void sensitivity(long seed,DrainageNetwork d,int[][] sites){
        double[][] controls={{4,.45,.20,16},{3,.45,.20,16},{5,.45,.20,16},{4,.43,.20,16},{4,.47,.20,16},{4,.45,.18,16},{4,.45,.22,16},{4,.45,.20,14},{4,.45,.20,18}};
        for(var a:controls){var f=new LandIceField(seed,d,a[0],a[1],a[2],a[3]);int count=0,max=0;
            for(var p:sites)for(int dz=-160;dz<=160;dz+=16)for(int dx=-160;dx<=160;dx+=16){int n=f.thickness(p[0]+dx,p[1]+dz);if(n>0)count++;max=Math.max(max,n);}
            System.out.println("ICE_SENSITIVITY controls="+Arrays.toString(a)+" same3Windows stride=16 denominator=1323 positive="+count+" maxDepth="+max);
        }
    }
    private static void require(boolean v,String message){if(!v)throw new AssertionError(message);}
}
