package blockbranch.realistic;

import java.util.*;

/** Mathematical controls and finite physical witnesses, not station calibration. */
public final class SummerDryCheck {
    public static void main(String[] args){
        shrubSupport();
        require(SummerDrySchema.schema(263,1)==264&&SummerDrySchema.baseSchema(264,1)==263,"Complete saved foundation");
        for(int version:new int[]{82,257,258,259,260,261,262,263,265})require(!SummerDrySchema.matches(version,1),"Reject partial/future foundation");
        require(SummerDrySchema.matches(263,0)&&!SummerDrySchema.matches(264,0)&&!SummerDrySchema.matches(264,2),"Absent gate retains old only");
        // Hand-specified synthetic envelope, independent of the generated curve:
        // mild winter/hot summer and ~700mm annual rain lie within FAO's regional
        // European description. These are not observed monthly station data.
        double[] temperature={28,27,24,20,15,11,9,10,14,19,23,26};
        double[] rain={4,8,18,50,80,110,125,105,65,45,15,5};
        var anchor=SummerDryClimate.summarize(rain,temperature,2);
        require(anchor.suitable()&&anchor.dryMonths()==5,"Source-bounded independent summer-drought envelope");
        var threshold=rain.clone();threshold[0]=2*temperature[0];
        require(SummerDryClimate.summarize(threshold,temperature,2).suitable(),"P equals 2T is dry in this convention");
        threshold[0]=Math.nextUp(threshold[0]);
        require(!SummerDryClimate.summarize(threshold,temperature,2).suitable(),"Warmest month just above threshold is not a summer-drought anchor");
        var opposite=new double[12];for(int m=0;m<12;m++)opposite[m]=rain[(m+6)%12];
        require(!SummerDryClimate.summarize(opposite,temperature,2).suitable(),"Same annual total with winter drought rejected");
        var uniform=new double[12];Arrays.fill(uniform,anchor.annualModelRainMm()/12);
        require(!SummerDryClimate.summarize(uniform,temperature,2).suitable(),"Same annual total without drought rejected");
        Arrays.fill(uniform,5);require(!SummerDryClimate.summarize(uniform,temperature,2).suitable(),"Year-round aridity rejected");
        for(double delta:new double[]{-20,12}){var t=temperature.clone();for(int m=0;m<12;m++)t[m]+=delta;require(!SummerDryClimate.summarize(rain,t,2).suitable(),"Cold upland and year-round tropical controls");}
        var split=rain.clone();split[0]=200;split[5]=0;require(!SummerDryClimate.summarize(split,temperature,2).suitable(),"Disconnected/cold-season drought rejected");
        for(double total:new double[]{0,400,900,1600,2400}){
            var p=SummerDryClimate.monthly(total,.08);require(Math.abs(Arrays.stream(p).sum()-total)<1e-9,"Annual conservation");
            require(total==0||p[6]>p[0]*3,"Declared winter maximum");
        }
        for(double invalid:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})expect(()->SummerDryClimate.monthly(invalid,.08));
        expect(()->SummerDryClimate.monthly(900,0));expect(()->SummerDryClimate.summarize(new double[11],temperature,2));
        var d=new DrainageNetwork(new TerrainShape(424242),424242,true,true,true,true,true,true);var c=new ClimateField(424242);
        for(int[] p:new int[][]{{1904000,-752000},{272000,-720000},{144000,-688000}}){
            require(SummerDryWoodland.eligible(424242,p[0],p[1],d,c),"Actual first coastal candidate retained");
            require(SummerDryClimate.potential(p[0],p[1],d,c).equals(SummerDryClimate.potential(p[0],p[1],d,c)),"Deterministic static signal");
        }
        for(int[] p:new int[][]{{-1904000,-1904000},{-1328000,-1904000},{1904000,-1552000},{1039616,1296768},{1648000,-208000},{48000,-752000}})
            require(!SummerDryWoodland.eligible(424242,p[0],p[1],d,c),"Marine/lake/coast/ice/tropical/inland negative controls");
        require(WarmWoodland.contains(424242,48000,-752000,d,c)&&SummerDryClimate.potential(48000,-752000,d,c).suitable()
            &&!SummerDryClimate.coastalRegime(48000,-752000,d),"Inland control qualifies thermally and seasonally; missing marine witness actually excludes it");
        System.out.println("PASS summer-dry climate: independent synthetic seasonal controls, finite physical candidates/exclusions, deterministic totals and schema gates");
    }
    private record Position(int x,int y,int z){}
    private static void shrubSupport(){
        int leaves=0;
        for(int height=3;height<=5;height++)for(int variant=0;variant<64;variant++){
            var shape=SummerDryTrees.tree(new WoodlandTrees.Root(0,100,0,false,height,variant,SummerDryWoodland.FAMILY));
            var cells=new HashMap<Position,Integer>();
            for(var v:shape)require(cells.put(new Position(v.x(),v.y(),v.z()),v.code())==null,"Unique raw shrub voxel");
            for(var v:shape)if(!WoodlandTrees.logCode(v.code())){
                int nearest=7;
                for(int[] step:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){
                    int code=cells.getOrDefault(new Position(v.x()+step[0],v.y()+step[1],v.z()+step[2]),0);
                    if(code>0)nearest=Math.min(nearest,WoodlandTrees.logCode(code)?0:code-6);
                }
                // Native leaf updates use this local distance recurrence. Strict
                // descent also proves a finite path to wood, without copying BFS.
                require(v.code()>=7&&v.code()<=12&&v.code()-6==nearest+1,"Raw saved-clearing leaf distance matches neighbouring support");leaves++;
            }
            if(height==3&&variant==0)require(cells.get(new Position(0,101,2))==8,"Concrete outer shrub leaf is two steps from wood");
        }
        require(leaves>0,"Nonvacuous shrub leaf/support check");
        System.out.println("SUMMER_DRY_SHRUB_SUPPORT heights=3..5 variants=0..63 shapes=192 leaves="+leaves);
    }
    private static void expect(Runnable action){boolean rejected=false;try{action.run();}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Invalid input rejected");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SummerDryCheck(){}
}
