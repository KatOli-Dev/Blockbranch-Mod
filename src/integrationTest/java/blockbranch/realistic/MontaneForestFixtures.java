package blockbranch.realistic;
import com.google.gson.JsonObject;
/** QA-only explicit prerequisite views (broadleaf crowns 274 over mangrove roots 273 over boreal slope 272 over montane 271 over 270); never promote a saved world. */
final class MontaneForestFixtures {
    /** Complete mangrove roots 273: removes only the explicit broadleaf crowns wrapper. */
    static void mangrove273(JsonObject json){
        if(!json.has("broadleaf_crown_definition"))return;
        if(json.get("shape_version").getAsInt()!=274||json.get("broadleaf_crown_definition").getAsInt()!=1)throw new AssertionError("Complete broadleaf crowns wrapper required");
        json.remove("broadleaf_crown_definition");json.addProperty("shape_version",273);
    }
    /** Complete boreal slope 272: removes the broadleaf crowns and mangrove roots wrappers. */
    static void boreal272(JsonObject json){
        mangrove273(json);
        if(!json.has("mangrove_roots_definition"))return;
        if(json.get("shape_version").getAsInt()!=273||json.get("mangrove_roots_definition").getAsInt()!=1)throw new AssertionError("Complete mangrove roots wrapper required");
        json.remove("mangrove_roots_definition");json.addProperty("shape_version",272);
    }
    /** Complete montane 271: removes the mangrove roots and boreal slope wrappers. */
    static void montane271(JsonObject json){
        boreal272(json);
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
