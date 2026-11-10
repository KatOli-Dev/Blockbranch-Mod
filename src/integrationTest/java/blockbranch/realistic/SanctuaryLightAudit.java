package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;

/** Pure comparison of actual native light exports, also usable against known failed captures. */
public final class SanctuaryLightAudit {
    public static Map<String,Integer> read(Path file)throws Exception {
        var result=new HashMap<String,Integer>();
        try(var lines=Files.lines(file)){lines.forEach(line->{var row=com.google.gson.JsonParser.parseString(line).getAsJsonObject();String key=row.get("x").getAsInt()+","+row.get("y").getAsInt()+","+row.get("z").getAsInt();if(result.put(key,row.get("blockLight").getAsInt())!=null)throw new AssertionError("Duplicate native light coordinate "+key);});}
        return result;
    }
    public static Map<String,Object> compare(Path reference,Map<String,Integer> actual,Set<String> harvested)throws Exception {
        return compare(reference,actual,harvested,Set.of());
    }
    public static Map<String,Object> compare(Path reference,Map<String,Integer> actual,Set<String> harvested,Set<String> mayBrighten)throws Exception {
        var expected=read(reference);
        if(!expected.keySet().equals(actual.keySet())||!actual.keySet().containsAll(harvested)||!actual.keySet().containsAll(mayBrighten))throw new AssertionError("Every protected native light row is represented in actual172 reference");
        var edits=new ArrayList<Object>();var brighter=new ArrayList<Object>();int unchanged=0;
        for(var entry:expected.entrySet()){String p=entry.getKey();int before=entry.getValue(),now=actual.get(p);
            if(harvested.contains(p)){edits.add(Map.of("local",p,"beforeHarvest",before,"afterHarvest",now));continue;}
            if(before==now){unchanged++;continue;}
            if(mayBrighten.contains(p)&&now>before){brighter.add(Map.of("local",p,"before",before,"after",now));continue;}
            throw new AssertionError("Protected native block lighting changed "+p+" "+before+" -> "+now);
        }
        return Map.of("reference",reference.toString(),"verifiedCells",actual.size()-harvested.size(),"exactComparedCells",unchanged,"explicitHarvestCells",edits,"brightenedCells",brighter.size(),"brightening",brighter,"allOtherRowsExact",brighter.isEmpty(),"noUnpermittedChange",true);
    }
}
