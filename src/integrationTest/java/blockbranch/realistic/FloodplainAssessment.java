package blockbranch.realistic;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;

/** Diagnosis of published fixed-water cross sections, not a flood-frequency model.
 * Stride and offsets are recorded sampling conventions, never ecology thresholds. */
public final class FloodplainAssessment {
    public static void main(String[] args)throws Exception{
        var reports=new ArrayList<Object>();
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true);
            var climate=new ClimateField(seed,true);int rivers=0,probed=0;
            var gradients=new TreeMap<String,Integer>();var heights=new TreeMap<String,Integer>();var witnesses=new ArrayList<Object>();var witnessKinds=new TreeMap<String,Integer>();
            for(int edge=0;edge<d.size();edge++){
                if(!d.river(edge))continue;rivers++;
                int parent=d.parent(edge);double length=Math.hypot(d.nodeX(parent)-d.nodeX(edge),d.nodeZ(parent)-d.nodeZ(edge));
                double gradient=(d.waterLevel(edge)-d.waterLevel(parent))/length;
                gradients.merge(gradient==0?"zero":gradient<.0005?"0-.0005":gradient<.002?".0005-.002":gradient<.01?".002-.01":">=.01",1,Integer::sum);
                if(edge%23!=0||d.waterLevel(edge)<=TerrainShape.SEA_LEVEL||d.waterLevel(parent)<=TerrainShape.SEA_LEVEL)continue;
                int x=(int)Math.round(d.edgeX(edge,.5)),z=(int)Math.round(d.edgeZ(edge,.5));
                var ch=d.channelAt(x,z);if(ch.edge()!=edge)continue;
                double tx=d.edgeX(edge,.51)-d.edgeX(edge,.49),tz=d.edgeZ(edge,.51)-d.edgeZ(edge,.49),norm=Math.hypot(tx,tz);
                if(norm==0)continue;double nx=-tz/norm,nz=tx/norm;probed++;
                var section=new ArrayList<Object>();
                for(double factor:new double[]{-4,-3,-2,-1.4,-1,0,1,1.4,2,3,4}){
                    int wx=x+(int)Math.round(nx*ch.halfWidth()*factor),wz=z+(int)Math.round(nz*ch.halfWidth()*factor);var s=d.sample(wx,wz);
                    int above=s.height()+1-ch.waterLevel();boolean dry=s.waterLevel()==DrainageNetwork.DRY;
                    heights.merge((Math.abs(factor)<=1.4?"inner:":"outer:")+(dry?(above<=0?"dry<=0":above<=3?"dry1-3":above<=10?"dry4-10":"dry>10"):"water"),1,Integer::sum);
                    section.add(Map.of("factor",factor,"x",wx,"z",wz,"aboveChannelWater",above,"water",s.waterLevel(),"height",s.height()));
                }
                var center=d.sample(x,z);var c=climate.sample(x,z,d::sample);double left=((Number)((Map<?,?>)section.get(2)).get("aboveChannelWater")).doubleValue(),right=((Number)((Map<?,?>)section.get(8)).get("aboveChannelWater")).doubleValue();String kind=center.waterLevel()!=ch.waterLevel()?"non-wet-center":gradient>.003?"steep":Math.min(left,right)<=12?"low-near-valley-floor":"low-confined";{witnessKinds.merge(kind,1,Integer::sum);witnesses.add(Map.of("kind",kind,"edge",edge,"center",List.of(x,z),"width",ch.halfWidth()*2,"water",ch.waterLevel(),"gradient",gradient,"contributingCells",d.accumulation(edge),"meanC",c.meanTemperatureC(),"moisture",c.moisture(),"crossSection",section));}
            }
            reports.add(Map.of("seed",seed,"routedEdges",rivers,"edgeGradientBins",gradients,"sampledFreshwaterSections",probed,"crossSectionHeights",heights,"sectionKinds",witnessKinds,"sections",witnesses));
            System.out.println("FLOODPLAIN_ASSESSMENT seed="+seed+" edges="+rivers+" sampled="+probed+" gradients="+gradients+" sectionHeights="+heights);
        }
        Path output=Path.of(args[0]);Files.createDirectories(output.getParent());Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("productionBase","a7bea44a4a10baea5ab57559444807789f64b4cc","edgeStride",23,"sectionOffsetsInHalfWidths",List.of(-4,-3,-2,-1.4,-1,0,1,1.4,2,3,4),"limit","Finite fixed-water geometry diagnosis; gradient bins are descriptive. No discharge, flood-frequency, nutrient or Earth-area inference.","seeds",reports))+"\n");
    }
    private FloodplainAssessment(){}
}
