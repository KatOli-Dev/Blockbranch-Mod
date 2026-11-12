package blockbranch.realistic;

import com.google.gson.JsonObject;

/** Historical surface fixtures test their named definition, while the current
 * survey and xeric qualification exercise today's full preset. Strip the new
 * wrapper explicitly so neither a renamed schema nor a lingering gate can
 * silently reinterpret a historical fixture. */
final class WarmXericFixtures {
    static void published267(JsonObject json){
        SeasonalForestFixtures.published268(json);
        if(!json.has("warm_xeric_definition"))return;
        if(json.get("shape_version").getAsInt()!=268||json.get("warm_xeric_definition").getAsInt()!=1)
            throw new AssertionError("Reviewed complete xeric wrapper required");
        json.remove("warm_xeric_definition");json.addProperty("shape_version",267);
        json.getAsJsonObject("additional_biomes").remove(WarmXericShrubland.KEY);
    }
    private WarmXericFixtures(){}
}
