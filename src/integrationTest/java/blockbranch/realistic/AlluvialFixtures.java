package blockbranch.realistic;
import com.google.gson.JsonObject;
/** Named older fixtures unwrap only the reviewed new-world definition. Real
 * saved worlds always read their actual generator without this QA helper. */
final class AlluvialFixtures {
    static void published269(JsonObject json){
        if(!json.has("alluvial_floodplain_definition"))return;
        if(json.has("boreal_slope_definition")){
            if(json.get("shape_version").getAsInt()!=271||json.get("boreal_slope_definition").getAsInt()!=1)throw new AssertionError("Complete reviewed boreal slope wrapper required");
            json.remove("boreal_slope_definition");json.addProperty("shape_version",270);
        }
        if(json.get("shape_version").getAsInt()!=270||json.get("alluvial_floodplain_definition").getAsInt()!=1)throw new AssertionError("Complete reviewed alluvial wrapper required");
        json.remove("alluvial_floodplain_definition");json.addProperty("shape_version",269);
    }
    private AlluvialFixtures(){}
}
