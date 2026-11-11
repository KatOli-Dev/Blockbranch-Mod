package blockbranch.realistic;

import java.util.*;

/** Existing fixed-grid/finite-neighbourhood diagnosis, never a population or Earth-area estimate. */
public final class AbyssalTraceSiteSurvey {
    private static int[] stages(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        int[] a=new int[5];a[0]=1;var s=d.sample(x,z);int depth=s.waterLevel()==DrainageNetwork.DRY?0:s.waterLevel()-s.height()-1;
        if(depth<3000||depth>6000||!d.habitatOceanWater(x,z,s)||d.terminalLakeWater(x,z,s)||d.channelAt(x,z).edge()>=0)return a;a[1]=1;
        double slope=SlopeCorrection.slope(d,x,z);int bed=AbyssalTraces.bed(seed,x,z,d,c,true);
        if(bed==5||bed==6)a[2]=1;
        if(slope<=.08)a[3]=1;
        if(AbyssalTraces.eligible(seed,x,z,d,c,true))a[4]=1;return a;
    }
    private static void add(int[] a,int[] b){for(int i=0;i<a.length;i++)a[i]+=b[i];}
    public static void main(String[] args){
        System.out.println("STAGES=[all,depth+marine,selectedFineBed,slope<=.08,bothFine+slope]");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true);var c=new ClimateField(seed);int[] grid=new int[5];var selected=new ArrayList<int[]>();
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){var a=stages(seed,x,z,d,c);add(grid,a);if(a[4]>0&&selected.size()<3)selected.add(new int[]{x,z});}
            System.out.println("GRID seed="+seed+" bounds=[-1920000,1920000) centeredStride=32000 denominator=14400 stages="+Arrays.toString(grid)+" firstEligible="+Arrays.deepToString(selected.toArray()));
            int[][] original=seed==1?new int[][]{{-1168000,-1904000},{-1136000,-1904000},{-1104000,-1904000}}:new int[][]{{-1904000,-1904000},{-1872000,-1904000},{-1840000,-1904000}};
            for(String kind:new String[]{"originalDepthOnly","fineAndSlope"}){
                var centers=kind.equals("originalDepthOnly")?Arrays.asList(original):selected;int[] totals=new int[5];int traces=0;var forms=new HashSet<Integer>();
                for(var center:centers){int[] counts=new int[5];
                    for(int gz=Math.floorDiv(center[1]-64,12);gz<=Math.floorDiv(center[1]+79,12);gz++)for(int gx=Math.floorDiv(center[0]-64,12);gx<=Math.floorDiv(center[0]+79,12);gx++){
                        var p=AbyssalTraces.candidate(seed,gx,gz);if(p==null||p.x()<center[0]-64||p.x()>center[0]+79||p.z()<center[1]-64||p.z()>center[1]+79)continue;
                        var a=stages(seed,p.x(),p.z(),d,c);add(counts,a);if(a[4]>0){traces++;forms.add(p.form());System.out.println("POSITIVE seed="+seed+" x="+p.x()+" z="+p.z()+" form="+p.form());}
                    }
                    add(totals,counts);System.out.println("WINDOW seed="+seed+" kind="+kind+" center="+Arrays.toString(center)+" bounds=[center-64,center+79] stride=allCandidates stages="+Arrays.toString(counts));
                }
                System.out.println("TOTAL seed="+seed+" kind="+kind+" windows="+centers.size()+" chunks=243 candidateStages="+Arrays.toString(totals)+" traces="+traces+" forms="+forms);
            }
        }
    }
    private AbyssalTraceSiteSurvey(){}
}
