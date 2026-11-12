package blockbranch.realistic;
import com.google.gson.JsonObject;
/** QA-only explicit prerequisite view; never promotes a saved world. */
final class MontaneForestFixtures {
    static void published270(JsonObject json){
        if(!json.has("montane_forest_definition"))return;
        if(json.get("shape_version").getAsInt()!=271||json.get("montane_forest_definition").getAsInt()!=1)throw new AssertionError("Complete montane wrapper required");
        json.remove("montane_forest_definition");json.getAsJsonObject("additional_biomes").remove(MontaneForest.KEY);json.addProperty("shape_version",270);
    }
    private MontaneForestFixtures(){}
}
