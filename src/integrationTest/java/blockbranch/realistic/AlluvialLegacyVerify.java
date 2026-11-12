package blockbranch.realistic;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Original rejected profiles remain explicit holes, distinct from byte-identical
 * valid profiles and the exact original scalar footprint. */
public final class AlluvialLegacyVerify {
    static JsonObject frozen(long seed)throws Exception{
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(AlluvialLegacyVerify.class.getResourceAsStream("/blockbranch-fixtures/alluvial-reader-269/cover-"+seed+".json")),java.nio.charset.StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject();}
    }
    static Map<String,Object> verify(JsonObject frozen,JsonObject observed,JsonObject scoped){
        require(frozen.get("densePhysicalReads").equals(scoped.get("densePhysicalReads")),"Dense comparison requires the exact frozen probe footprint after repairs");
        require(frozen.get("densePhysicalSHA256").equals(scoped.get("densePhysicalSHA256")),"Original dense scalar states, including rejected-profile columns, changed");
        var profiles=new HashMap<String,JsonObject>();observed.getAsJsonArray("profiles").forEach(v->{var p=v.getAsJsonObject();profiles.put(key(p),p);require(p.get("valid").getAsBoolean(),"New reader still produces an invalid profile");});
        int valid=0,repaired=0;
        for(var value:frozen.getAsJsonArray("profiles")){
            var prior=value.getAsJsonObject();var now=profiles.get(key(prior));require(now!=null,"Frozen profile missing");
            if(prior.get("valid").getAsBoolean()){require(prior.get("profileSHA256").equals(now.get("profileSHA256")),"Previously valid profile encoding changed at "+key(prior));valid++;}
            else{require(now.get("valid").getAsBoolean()&&now.has("profileSHA256"),"Original rejected profile did not become valid");repaired++;}
        }
        return Map.of("seed",frozen.get("seed").getAsLong(),"denseScalarReads",frozen.get("densePhysicalReads").getAsInt(),"identicalValidProfiles",valid,"repairedOriginalRejectedProfiles",repaired);
    }
    public static void main(String[] args)throws Exception{
        var old=frozen(Long.parseLong(args[0]));var observed=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();var scoped=args.length>2?JsonParser.parseString(Files.readString(Path.of(args[2]))).getAsJsonObject():observed;
        System.out.println("ALLUVIAL_LEGACY_DENSE_PROFILE_PASS "+verify(old,observed,scoped));
    }
    private static String key(JsonObject p){return p.get("point")+":"+p.get("chunk");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private AlluvialLegacyVerify(){}
}
