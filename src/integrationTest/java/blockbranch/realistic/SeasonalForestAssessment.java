package blockbranch.realistic;

import com.google.gson.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Optional diagnosis over recorded current-selector samples, never a second
 * biome selector or an Earth-area estimate. Reports the input preset/hash so
 * a historical survey cannot be mistaken for today's whole-world inventory. */
public final class SeasonalForestAssessment {
    private static final Set<String> FORESTS=Set.of("minecraft:jungle","minecraft:sparse_jungle",
        "blockbranch:warm_subhumid_oak_pine_woodland","blockbranch:humid_subtropical_oak_fern_woodland");
    public static void main(String[] args)throws Exception{
        Path input=Path.of(args[0]),output=Path.of(args[1]);
        var preset=JsonParser.parseString(Files.readString(input.resolveSibling("survey.json"))).getAsJsonObject().getAsJsonObject("preset");
        var lines=Files.readAllLines(input);var columns=Arrays.asList(lines.getFirst().split(","));
        var counts=new TreeMap<String,Map<String,Integer>>();var witnesses=new ArrayList<Object>();var regionalWitnesses=new ArrayList<Object>();
        int total=0,land=0,forest=0,faopositive=0,regionalPositive=0;
        for(var line:lines.subList(1,lines.size())){
            var row=line.split(",");total++;if(!row[columns.indexOf("footprint")].equals("land"))continue;land++;
            String biome=row[columns.indexOf("selected_biome")];
            int z=Integer.parseInt(row[columns.indexOf("z")]);
            double mean=Double.parseDouble(row[columns.indexOf("mean_temperature_c")]),rain=Double.parseDouble(row[columns.indexOf("annual_model_rain_mm")]);
            if(!FORESTS.contains(biome))continue;forest++;
            var monthly=DrySeasonPolicy.monthly(z/2097152.,rain);
            double amplitude=CoolDesert.season(z,mean).warmestC()-mean;
            // Phase follows the existing ITCZ model's warm-belt extreme: month5
            // for positive Z, month11 for negative Z. This is a declared local
            // climatology convention, not simulated weather/observed latitude.
            int phase=z>=0?5:11,dry=0,run=0,longest=0;boolean[] isDry=new boolean[12];
            double summer=0,winter=0;
            for(int m=0;m<12;m++){
                double season=Math.cos(2*Math.PI*(m-phase)/12),temperature=mean+amplitude*season;
                if(isDry[m]=monthly[m]<=2*temperature)dry++;
                if(season>1e-9)summer+=monthly[m];else if(season< -1e-9)winter+=monthly[m];
            }
            for(int m=0;m<24;m++){run=isDry[m%12]?run+1:0;longest=Math.min(12,Math.max(longest,run));}
            boolean fao=rain>=1000&&rain<=2000&&mean-amplitude>18&&dry>=3&&dry<=5&&longest==dry&&summer>winter;
            var old=DrySeasonPolicy.summarize(monthly);
            boolean regional=rain>=1300&&rain<=2000&&mean>=20&&mean<=30&&old.dryMonths()>=4&&old.dryMonths()<=5&&old.longestDryRun()==old.dryMonths();
            if(fao){faopositive++;counts.computeIfAbsent("FAO_global_moist_deciduous",k->new TreeMap<>()).merge(biome,1,Integer::sum);}
            if(regional){regionalPositive++;counts.computeIfAbsent("Asia_rain_and_mean_with_existing_Dry100",k->new TreeMap<>()).merge(biome,1,Integer::sum);}
            if(regional&&regionalWitnesses.size()<40)regionalWitnesses.add(Map.of("seed",row[columns.indexOf("seed")],"x",row[columns.indexOf("x")],"z",z,"priorBiome",biome,"meanC",mean,"annualModelRain",rain,"dryMonths_under100",old.dryMonths()));
            if(fao&&witnesses.size()<24)witnesses.add(Map.of("seed",row[columns.indexOf("seed")],"x",row[columns.indexOf("x")],"z",z,"priorBiome",biome,"annualModelRain",rain,"coldestModelMonth",mean-amplitude,"dryMonths_P_le_2T",dry));
        }
        var report=new LinkedHashMap<String,Object>();
        report.put("sourcePreset",preset);report.put("input",input.toString());report.put("inputSHA256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input))));
        report.put("samples",total);report.put("landSamples",land);report.put("existingForestSamples",forest);report.put("FAOGlobalCandidates",faopositive);report.put("AsiaEnvelopeExistingDry100Candidates",regionalPositive);report.put("candidatePriorOwners",counts);report.put("witnesses",witnesses);report.put("regionalWitnesses",regionalWitnesses);
        report.put("limits","Classification alternatives over inherited uncalibrated rain/thermal models. No threshold selection by target count, no generation change, no nutrient/hydroperiod or Earth-area claim. FAO global P<=2T and the composed Asia-envelope/existing <100mm screen remain separate; the latter is not an official FAO classifier.");
        Files.createDirectories(output.getParent());Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
        System.out.println("SEASONAL_FOREST_ASSESSMENT samples="+total+" forests="+forest+" FAO-global="+faopositive+" Asia-envelope/existing-dry100="+regionalPositive+" priorOwners="+counts+" report="+output);
    }
    private SeasonalForestAssessment(){}
}
