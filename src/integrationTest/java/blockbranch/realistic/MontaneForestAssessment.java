package blockbranch.realistic;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Source-bounded diagnostic, not a global ecological classifier. FAO supplies
 * the temperate4–8 warm-month domain and approximate800m mountain convention;
 * Pan et al.(2013) Table1 supplies1000–2500mm as a broad montane context.
 * IUCN T2.1 motivates cold-season needleleaf structure, not a deterministic
 * climate-to-species map. Existing rain/thermal coefficients are uncalibrated.
 */
public final class MontaneForestAssessment {
    private static final Set<String> FORESTS=Set.of("minecraft:forest","minecraft:birch_forest","blockbranch:dappled_oak_birch_woodland");
    public static void main(String[] args)throws Exception{
        var lines=Files.readAllLines(Path.of(args[0]));var columns=Arrays.asList(lines.getFirst().split(","));var rows=new ArrayList<Object>();var owners=new TreeMap<String,Integer>();var selected=new TreeMap<String,Integer>();int land=0,climate=0,forest=0,province=0;
        for(var line:lines.subList(1,lines.size())){
            var a=line.split(",");if(!a[columns.indexOf("footprint")].equals("land"))continue;land++;
            int x=Integer.parseInt(a[columns.indexOf("x")]),z=Integer.parseInt(a[columns.indexOf("z")]);long seed=Long.parseLong(a[columns.indexOf("seed")]);int elevation=Integer.parseInt(a[columns.indexOf("height")])+1-TerrainShape.SEA_LEVEL;
            double mean=Double.parseDouble(a[columns.indexOf("mean_temperature_c")]),rain=Double.parseDouble(a[columns.indexOf("annual_model_rain_mm")]);var season=CoolDesert.season(z,mean);
            if(elevation<800||rain<1000||rain>2500||season.coldestC()>=0||season.warmMonths()<4||season.warmMonths()>8)continue;
            climate++;String owner=a[columns.indexOf("selected_biome")];owners.merge(owner,1,Integer::sum);if(!FORESTS.contains(owner))continue;forest++;
            // Existing compositional-province convention, independently salted;
            // it represents unmodeled floristics/history, not frequency fitting.
            if(!Taiga.provinceSelected(seed^0x4D4F4E54414E454CL,x,z))continue;province++;selected.merge(owner,1,Integer::sum);
            rows.add(Map.of("seed",seed,"x",x,"z",z,"elevation",elevation,"meanC",mean,"coldestC",season.coldestC(),"warmMonths",season.warmMonths(),"annualModelRain",rain,"priorOwner",owner));
        }
        var report=new LinkedHashMap<String,Object>();report.put("input",args[0]);report.put("inputSHA256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[0])))));report.put("landSamples",land);report.put("climateCandidates",climate);report.put("climatePriorOwners",owners);report.put("ordinaryForestCandidates",forest);report.put("provinceCandidates",province);report.put("selectedPriorOwners",selected);report.put("witnesses",rows);report.put("limits","Finite game samples. No Earth area, soil acidity, fire history, nutrient ecology or deterministic floristic classification. Excludes grasslands and specialist forest variants; retained broadleaf provinces are deliberate. Thresholds are source-context composition over inherited uncalibrated model units.");
        Path output=Path.of(args[1]);Files.createDirectories(output.getParent());Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");System.out.println("MONTANE_FOREST_ASSESSMENT climate="+climate+" ordinaryForest="+forest+" province="+province+" owners="+selected);
    }
    private MontaneForestAssessment(){}
}
