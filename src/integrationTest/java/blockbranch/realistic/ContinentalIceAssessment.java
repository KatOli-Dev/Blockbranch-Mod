package blockbranch.realistic;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Parameter sensitivity, deliberately separate from correctness/empirical validation.
 * One seed and fixed finite windows/transect; no chunks, cost benchmark or area target.
 * Cooling variations here change sheet admission only, not a whole alternate world. */
public final class ContinentalIceAssessment {
    public static void main(String[] args)throws Exception{
        var d=new DrainageNetwork(new TerrainShape(424242),424242,true,true,true,true,true,true,true);var bed=d.iceReference();
        var nominal=new ContinentalIceField(424242,bed);var points=new ArrayList<int[]>();
        for(int[] center:new int[][]{{-1552000,-1904000},{-1456000,-1904000},{-1615115,-1904000}})
            for(int z=center[1]-128;z<=center[1]+128;z+=8)for(int x=center[0]-128;x<=center[0]+128;x+=8)points.add(new int[]{x,z});
        for(int z=-1800000;z>=-1904000;z-=128)points.add(new int[]{-1552000,z});
        var rows=new ArrayList<Object>();
        for(var controls:new double[][]{{1,1},{.95,1},{1.05,1},{1,.95},{1,1.05}}){
            var field=new ContinentalIceField(424242,bed,(int)Math.round(ContinentalIceField.RISE*controls[0]),controls[1]);
            int positive=0,changedAdmission=0,maxDepthDelta=0,minDepthDelta=0;long totalDepth=0;Integer thermalOnset=null;
            for(var p:points){int actual=field.thickness(p[0],p[1]),reference=nominal.thickness(p[0],p[1]);
                if(actual>0)positive++;if((actual>0)!=(reference>0))changedAdmission++;
                minDepthDelta=Math.min(minDepthDelta,actual-reference);maxDepthDelta=Math.max(maxDepthDelta,actual-reference);totalDepth+=actual;
            }
            for(int z=-1800000;z>=-1904000;z-=128)if(field.thickness(-1552000,z)>0){thermalOnset=z;break;}
            rows.add(Map.of("riseScale",controls[0],"sheetAdmissionCoolingScale",controls[1],"positiveSamples",positive,"changedAdmission",changedAdmission,"minDepthDeltaM",minDepthDelta,"maxDepthDeltaM",maxDepthDelta,"sumDepthM",totalDepth,"firstPositiveTransectZ",thermalOnset==null?"none":thermalOnset));
        }
        var report=new LinkedHashMap<String,Object>();report.put("seed",424242);report.put("windowCenters",List.of(List.of(-1552000,-1904000),List.of(-1456000,-1904000),List.of(-1615115,-1904000)));report.put("inclusiveWindowRadiusM",128);report.put("windowStrideM",8);report.put("transect",Map.of("x",-1552000,"zStart",-1800000,"zEnd",-1904000,"strideM",128));report.put("samplesPerControl",points.size());report.put("controls",rows);report.put("limits","Chosen coefficient sensitivity only; not empirical calibration, whole-world biome replacement, Earth coverage or cost acceptance.");
        Path file=Path.of("build/polar-ice-evidence/sensitivity.json");Files.createDirectories(file.getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
        System.out.println("RECORDED bounded static-sheet sensitivity "+file+" samples/control="+points.size());
    }
    private ContinentalIceAssessment(){}
}
