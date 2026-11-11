package blockbranch.realistic;

import java.util.*;

/** Finite physical decomposition; not a timing, trace-density or Earth-area target. */
public final class DeepBasinAssessment {
    static double gradient(TerrainShape shape,int x,int z){
        return Math.hypot((shape.continuousHeight(x+128,z)-shape.continuousHeight(x-128,z))/256,
            (shape.continuousHeight(x,z+128)-shape.continuousHeight(x,z-128))/256);
    }
    public static void main(String[] args){
        long seed=424242;var shape=new TerrainShape(seed);var d=new DrainageNetwork(shape,seed,true,true,true,true);
        var sites=new LinkedHashMap<String,int[]>();
        sites.put("previousFineBed",new int[]{-1744000,-1904000});
        sites.put("previousDepthOnly",new int[]{-1904000,-1904000});
        // Discovery is bounded to the established seed's 14,400-point grid; freeze
        // the resulting controls in qualification rather than searching every run.
        for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
            var s=d.sample(x,z);int depth=s.waterLevel()==DrainageNetwork.DRY?0:s.waterLevel()-1-s.height();
            double g=gradient(shape,x,z);boolean marine=d.habitatOceanWater(x,z,s);
            String kind=marine?(depth>6500?"trench":depth>3000&&g>=.10?"ruggedDeep":depth>3000&&g<=.02?"gentleBasin":depth>=1000&&depth<3000&&g<.02?"gentleTransition":depth>=1000&&depth<3000?"deepTransition":depth>25&&depth<200?"shelf":depth>0&&depth<=25?"coast":null):d.terminalLakeWater(x,z,s)?"lake":s.waterLevel()!=DrainageNetwork.DRY&&d.channelAt(x,z).edge()>=0?"river":s.waterLevel()==DrainageNetwork.DRY&&s.height()>1500?"upland":null;
            if(kind!=null)sites.putIfAbsent(kind,new int[]{x,z});
        }
        for(var entry:sites.entrySet()){
            int x=entry.getValue()[0],z=entry.getValue()[1];var center=d.sample(x,z);
            double sum=0,square=0;int n=0,structMin=Integer.MAX_VALUE,structMax=Integer.MIN_VALUE,min=999,max=-999,exactBank=0;
            for(int dz=-128;dz<=128;dz+=4)for(int dx=-128;dx<=128;dx+=4){
                var raw=d.structuralSample(x+dx,z+dz);var old=d.sample(x+dx,z+dz);int offset=old.height()-raw.height();
                min=Math.min(min,offset);max=Math.max(max,offset);sum+=offset;square+=(double)offset*offset;n++;
                structMin=Math.min(structMin,raw.height());structMax=Math.max(structMax,raw.height());
                if(offset==Math.round(52*shape.bankFraction(x+dx,z+dz)))exactBank++;
            }
            System.out.printf(Locale.ROOT,"SITE %s x=%d z=%d physical=%d water=%d marine=%s gradient=%.6f structureRange=%d..%d detailRange=%d..%d detailMean=%.4f detailRMS=%.4f exactBank=%d/%d bounds=+/-128 stride=4%n",entry.getKey(),x,z,center.height(),center.waterLevel(),d.habitatOceanWater(x,z,center),gradient(shape,x,z),structMin,structMax,min,max,sum/n,Math.sqrt(square/n),exactBank,n);
        }
    }
    private DeepBasinAssessment(){}
}
