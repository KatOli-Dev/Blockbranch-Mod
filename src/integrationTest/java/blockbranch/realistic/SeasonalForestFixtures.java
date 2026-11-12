package blockbranch.realistic;

import com.google.gson.JsonObject;

/** Explicitly unwrap only the reviewed new-world refinement for named older
 * fixtures. Real old-world reopening always reads its actual saved generator. */
final class SeasonalForestFixtures {
    static void published268(JsonObject json){
        AlluvialFixtures.published269(json);
        if(!json.has("seasonal_forest_definition"))return;
        if(json.get("shape_version").getAsInt()!=269||json.get("seasonal_forest_definition").getAsInt()!=1)
            throw new AssertionError("Reviewed complete seasonal-forest wrapper required");
        json.remove("seasonal_forest_definition");json.addProperty("shape_version",268);
    }
    private SeasonalForestFixtures(){}
}
