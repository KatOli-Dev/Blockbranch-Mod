package blockbranch.realistic;

import java.util.*;
import java.util.function.BiFunction;

/** Model controls, classification distinctions and lack of lake-to-rain feedback. */
public final class DrySeasonPolicyCheck {
    public static void main(String[] args) {
        var uniform=new double[12];Arrays.fill(uniform,120);
        var seasonal=new double[12];for(int m=0;m<12;m++)seasonal[m]=m<6?20:220;
        var split=new double[12];for(int m=0;m<12;m++)split[m]=(m&1)==0?20:220;
        var a=DrySeasonPolicy.summarize(uniform);var b=DrySeasonPolicy.summarize(seasonal);
        require(a.annualModelRainMm()==1440&&a.annualModelRainMm()==b.annualModelRainMm(),"Same annual supply, independently different seasonality");
        require(!a.regionalEnvelope()&&b.regionalEnvelope()&&b.dryMonths()==6&&b.longestDryRun()==6,"Six-month seasonal control, not another annual moisture cut");
        require(!DrySeasonPolicy.summarize(split).regionalEnvelope(),"Two or more disconnected short droughts do not become one long dry season");
        // Smithsonian BCI reports 2655.3 mm/year and 299 mm over its four-month
        // dry season. This synthetic aggregation preserves those totals, not
        // its individual monthly observations. Humid seasonal forest is negative.
        var bci=new double[12];for(int m=0;m<12;m++)bci[m]=m<4?299./4:(2655.3-299)/8;
        var humid=DrySeasonPolicy.summarize(bci);
        require(humid.dryMonths()==4&&!humid.regionalEnvelope(),"Humid BCI-style profile is not dry forest just because it has four dry months");
        // Huang et al. 2021 doi:10.1002/ecs2.3824 reports Santa Rosa dry forest
        // at 1723 mm/year (1980–2017), 89% in May–October. This aggregate control
        // is an intentional exclusion of our narrower analog, not a non-forest.
        var santaRosa=new double[12];for(int m=0;m<12;m++)santaRosa[m]=1723*(m<6?.11:.89)/6;
        require(!DrySeasonPolicy.summarize(santaRosa).regionalEnvelope(),"Known wetter dry forest proves the subtype ceiling is not universal");
        Arrays.fill(uniform,80);require(!DrySeasonPolicy.summarize(uniform).regionalEnvelope(),"Year-round low rainfall rejects rather than becoming dry forest");
        Arrays.fill(uniform,100);require(DrySeasonPolicy.summarize(uniform).dryMonths()==0,"Regional rule is strictly below 100, not FAO P<=2T");
        require(Math.abs(DrySeasonPolicy.annualModelRain(.18)-305.470129)<1e-5,"Documented arid design anchor");
        require(Math.abs(DrySeasonPolicy.annualModelRain(.55)-1631.563667)<1e-5,"Documented open-warm annual design anchor");
        require(Math.abs(DrySeasonPolicy.annualModelRain(.65)-2096.187024)<1e-5,"Documented humid annual design anchor");
        require(DrySeasonPolicy.summarize(DrySeasonPolicy.monthly(0,2100)).dryMonths()==0,"Wet equatorial two-peak control");
        var mid=DrySeasonPolicy.summarize(DrySeasonPolicy.monthly(.12,1450));
        require(mid.regionalEnvelope()&&mid.longestDryRun()==5,"Warm-belt seasonal control");
        require(!DrySeasonPolicy.summarize(DrySeasonPolicy.monthly(.24,1450)).regionalEnvelope(),"Seven-month edge is outside this narrow regional subtype");
        for(double latitude:new double[]{-1,-.24,-.12,0,.12,.24,1})for(double annual:new double[]{0,305,1450,2655.3,4000}) {
            var rain=DrySeasonPolicy.monthly(latitude,annual);var mirror=DrySeasonPolicy.monthly(-latitude,annual);
            require(Math.abs(Arrays.stream(rain).sum()-annual)<1e-9,"Annual model total preserved");
            require(Arrays.equals(rain,DrySeasonPolicy.monthly(latitude,annual)),"Repeated immutable month profiles");
            for(int m=0;m<12;m++)require(rain[m]>=0&&Double.isFinite(rain[m])&&Math.abs(rain[m]-mirror[(m+6)%12])<1e-9,"Nonnegative rain, hemisphere phase reversal");
        }
        var c=new ClimateField(424242);
        BiFunction<Integer,Integer,DrainageNetwork.Sample> dry=(x,z)->new DrainageNetwork.Sample(63,DrainageNetwork.DRY,0,false);
        BiFunction<Integer,Integer,DrainageNetwork.Sample> lake=(x,z)->new DrainageNetwork.Sample(63,66,0,true);
        require(c.sample(1000,2000,lake).moisture()>c.sample(1000,2000,dry).moisture(),"Original wet-ground moisture contribution retained");
        require(c.atmosphericSupply(1000,2000,lake)==c.atmosphericSupply(1000,2000,dry),"Wet ground does not become atmospheric rainfall");
        for(int seam:new int[]{-240000,-64000,-256,-16,0,16,256,64000,240000}) {
            double p=DrySeasonPolicy.annualModelRain(c.atmosphericSupply(seam-1,seam,dry));
            double q=DrySeasonPolicy.annualModelRain(c.atmosphericSupply(seam,seam,dry));
            require(Math.abs(p-q)<1,"No native/negative-coordinate rainfall seam");
        }
        for(long seed:new long[]{424242,1,-17}) {
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true);var climate=new ClimateField(seed);
            int warmStands=0,eligible=0;var points=new ArrayList<Object>();
            int[] sensitivity=new int[9],changed=new int[9];
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000) {
                if(!WarmWoodland.contains(seed,x,z,d,climate)||climate.sample(x,z,d::sample).meanTemperatureC()<=23)continue;
                warmStands++;var potential=DrySeasonPolicy.potential(x,z,d,climate);
                double annual=potential.annualModelRainMm(),latitude=z/2097152.;
                for(int variation=0;variation<9;variation++) {
                    double scale=variation==1?.95:variation==2?1.05:1;
                    double migration=DrySeasonPolicy.MIGRATION*(variation==3?.95:variation==4?1.05:1);
                    double width=DrySeasonPolicy.WIDTH*(variation==5?.95:variation==6?1.05:1);
                    double threshold=variation==7?95:variation==8?105:100;
                    boolean candidate=DrySeasonPolicy.summarize(DrySeasonPolicy.monthly(latitude,annual*scale,migration,width,DrySeasonPolicy.BACKGROUND),threshold).regionalEnvelope();
                    if(candidate)sensitivity[variation]++;
                    if(candidate!=potential.regionalEnvelope())changed[variation]++;
                }
                if(!potential.regionalEnvelope())continue;
                eligible++;if(points.size()<8)points.add(List.of(x,z,potential));
                require(potential.equals(DrySeasonPolicy.potential(x,z,d,climate)),"Deterministic seeded sidecar");
            }
            require(warmStands>0&&eligible>0&&eligible<warmStands,"Source-bounded seasonality splits actual warm forest affinity");
            require(sensitivity[0]==eligible&&changed[0]==0,"Sensitivity includes exact nominal control");
            System.out.println("DRY_SEASON_SENSITIVITY seed="+seed+" denominator=14400 warmForestAffinity="+warmStands+" order=[nominal,annual-.05,annual+.05,migration-.05,migration+.05,width-.05,width+.05,threshold95,threshold105] admitted="+Arrays.toString(sensitivity)+" changed="+Arrays.toString(changed)+" unrelatedHabitatChanges=0_by_domain_contract");
            System.out.println("DRY_SEASON_CANDIDATES seed="+seed+" warmStands="+warmStands+" eligible="+eligible+" points="+points);
            @SuppressWarnings("unchecked") var first=(List<Object>)points.getFirst();
            geometry(seed,(Integer)first.get(0),(Integer)first.get(1),d,climate);
        }
        require(DryForestSchema.schema(258,1)==259&&DryForestSchema.baseSchema(259,1)==258,"New complete foundation");
        for(int old:new int[]{82,141,255,256,257,258})require(DryForestSchema.baseSchema(old,0)==old,"Legacy definitions pass through unchanged");
        require(!DryForestSchema.matches(259,0)&&!DryForestSchema.matches(258,1)&&!DryForestSchema.matches(260,1),"Partial/future gates reject");
        System.out.println("DRY_SEASON_IDEALIZED_PRECIPITATION_NOT_PET_REFERENCE_CONTROLS_PASS");
    }
    private static void geometry(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var owner=DryForest.domain(seed,d,c,(qx,qz)->DryForest.eligible(seed,qx*4,qz*4,d,c));
        var roots=DryForestTrees.roots(seed,x-32,z-32,x+32,z+32,d,c,owner);
        require(!roots.isEmpty(),"Actual sidecar-positive stand has supported roots");
        int maxHeight=0;
        for(var root:roots){
            require(root.family()==DryForest.FAMILY&&!root.birch()&&root.height()>=8&&root.height()<=30,"Bounded broadleaf cohorts, not old umbrella pine");
            require(root.y()==d.sample(root.x(),root.z()).height()+1&&owner.test(root.x(),root.z()),"Actual terrain-supported selected roots");
            maxHeight=Math.max(maxHeight,root.height());
            var tree=ForestTrees.tree(root);var map=new HashMap<String,Integer>();
            for(var v:tree){require(Math.abs(v.x()-root.x())<=WoodlandTrees.RADIUS&&Math.abs(v.z()-root.z())<=WoodlandTrees.RADIUS,"Bounded crown halo");map.put(v.x()+","+v.y()+","+v.z(),v.code());}
            for(var v:tree)if(!WoodlandTrees.logCode(v.code())){
                int distance=WoodlandTrees.leafDistance(v.code());boolean support=false;
                for(var q:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){int near=map.getOrDefault((v.x()+q[0])+","+(v.y()+q[1])+","+(v.z()+q[2]),0);if(WoodlandTrees.logCode(near)&&distance==1||near>0&&!WoodlandTrees.logCode(near)&&WoodlandTrees.leafDistance(near)<distance)support=true;}
                require(distance>=1&&distance<=6&&support,"Natural foliage retains a support path");
            }
        }
        var columns=new LinkedHashMap<String,int[]>();
        for(int cz=(z>>4)-1;cz<=(z>>4)+1;cz++)for(int cx=(x>>4)-1;cx<=(x>>4)+1;cx++)columns.put(cx+","+cz,WoodlandTrees.column(seed,cx,cz,d,c,true,3,owner));
        var order=new ArrayList<>(columns.keySet());Collections.reverse(order);
        for(var key:order){var q=key.split(",");int cx=Integer.parseInt(q[0]),cz=Integer.parseInt(q[1]);require(Arrays.equals(columns.get(key),WoodlandTrees.column(seed,cx,cz,d,c,true,3,owner)),"Reverse chunk request order retains exact new canopy");}
        boolean[] expected=new boolean[64];for(int i=0;i<64;i++)expected[i]=owner.test(x+i-32,z+i-32);
        // Evict both bounded caches, then concurrent reads must still be exact.
        for(int i=0;i<10000;i++)owner.test(x+(i%100)*4,z+(i/100)*4);
        java.util.stream.IntStream.range(0,64).parallel().forEach(i->require(expected[i]==owner.test(x+i-32,z+i-32),"Concurrent/evicted domain reads retain immutable seed semantics"));
        System.out.println("DRY_FOREST_GEOMETRY seed="+seed+" roots="+roots.size()+" maxStem="+maxHeight+" reverseChunks="+columns.size());
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private DrySeasonPolicyCheck(){}
}
