package blockbranch.realistic;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Finite before/after design diagnosis using frozen original cross sections.
 * Stages are prescribed sensitivity controls, never a flood recurrence model. */
public final class AlluvialDesignSurvey {
    public static void main(String[] args)throws Exception{
        var input=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();var reports=new ArrayList<Object>();
        for(var seedRow:input.getAsJsonArray("seeds")){
            var row=seedRow.getAsJsonObject();long seed=row.get("seed").getAsLong();
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);
            int samples=0,changed=0,core=0,maxCut=0,referenceMismatch=0,climateMismatch=0;long totalCut=0;int[] stages=new int[4];var witnesses=new ArrayList<Object>();
            for(var value:row.getAsJsonArray("sections")){
                var section=value.getAsJsonObject();var center=section.getAsJsonArray("center");int cx=center.get(0).getAsInt(),cz=center.get(1).getAsInt();
                var climate=c.sample(cx,cz,d::sample);if(climate.meanTemperatureC()!=section.get("meanC").getAsDouble()||climate.moisture()!=section.get("moisture").getAsDouble())climateMismatch++;
                var changes=new ArrayList<Object>();
                for(var point:section.getAsJsonArray("crossSection")){
                    var p=point.getAsJsonObject();int x=p.get("x").getAsInt(),z=p.get("z").getAsInt();samples++;
                    var old=d.alluvialReference().sample(x,z);var now=d.sample(x,z);var plan=d.alluvialPlan(x,z);
                    if(old.height()!=p.get("height").getAsInt()||old.waterLevel()!=p.get("water").getAsInt())referenceMismatch++;
                    if(old.waterLevel()!=now.waterLevel()||old.basin()!=now.basin()||old.closed()!=now.closed()||old.regionalHeight()!=now.regionalHeight()||old.regionalWaterLevel()!=now.regionalWaterLevel())throw new AssertionError("Water/catchment/climate datum changed");
                    int cut=old.height()-now.height();if(cut<0||cut>8)throw new AssertionError("Cut bound");
                    if(cut>0){changed++;totalCut+=cut;maxCut=Math.max(maxCut,cut);if(plan.connectedCore())core++;int i=0;for(int rise:new int[]{3,4,5,6}){if(d.alluvialPotential(x,z,rise))stages[i]++;i++;}changes.add(Map.of("x",x,"z",z,"factor",p.get("factor"),"oldHeight",old.height(),"height",now.height(),"cut",cut,"core",plan.connectedCore()));}
                }
                if(!changes.isEmpty())witnesses.add(Map.of("center",center,"edge",section.get("edge"),"width",section.get("width"),"kind",section.get("kind"),"gradient",section.get("gradient"),"changes",changes));
            }
            if(referenceMismatch!=0||climateMismatch!=0)throw new AssertionError("Published reference/mean climate mismatch: "+referenceMismatch+"/"+climateMismatch);
            var report=new LinkedHashMap<String,Object>();report.put("seed",seed);report.put("samples",samples);report.put("changed",changed);report.put("connectedCoreSamples",core);report.put("maxCut",maxCut);report.put("summedCut",totalCut);report.put("referenceMismatch",referenceMismatch);report.put("climateMismatch",climateMismatch);report.put("modifiedConnectedBelowStage3_4_5_6",stages);report.put("witnesses",witnesses);reports.add(report);
            System.out.println("ALLUVIAL_DESIGN seed="+seed+" probes="+samples+" changed="+changed+" core="+core+" maxCut="+maxCut+" witnesses="+witnesses.size());
        }
        Path output=Path.of(args[1]);Files.createDirectories(output.getParent());Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("input",args[0],"scope","Fixed original section probes; reference stages are prescribed, not discharge/hydroperiod or Earth area. Native connectedness and profile tests remain required.","seeds",reports))+"\n");
    }
    private AlluvialDesignSurvey(){}
}
