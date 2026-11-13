package blockbranch.realistic;
import com.google.gson.JsonObject;
/** QA-only explicit prerequisite views (boreal slope 272 over montane 271 over 270); never promote a saved world. */
final class MontaneForestFixtures {
    /** Complete montane 271: removes only the explicit boreal slope wrapper. */
    static void montane271(JsonObject json){
        if(!json.has("boreal_slope_definition"))return;
        if(json.get("shape_version").getAsInt()!=272||json.get("boreal_slope_definition").getAsInt()!=1)throw new AssertionError("Complete boreal slope wrapper required");
        json.remove("boreal_slope_definition");json.addProperty("shape_version",271);
    }
    static void published270(JsonObject json){
        montane271(json);
        if(!json.has("montane_forest_definition"))return;
        if(json.get("shape_version").getAsInt()!=271||json.get("montane_forest_definition").getAsInt()!=1)throw new AssertionError("Complete montane wrapper required");
        json.remove("montane_forest_definition");json.getAsJsonObject("additional_biomes").remove(MontaneForest.KEY);json.addProperty("shape_version",270);
    }
    private MontaneForestFixtures(){}
}
