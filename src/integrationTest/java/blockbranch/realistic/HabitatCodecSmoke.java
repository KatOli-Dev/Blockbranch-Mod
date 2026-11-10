package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.RegistryOps;

/** Registry-backed flat-schema compatibility, rejection and seeded round-trip checks. */
public final class HabitatCodecSmoke {
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        try{
            var level=server.overworld();var savedGenerator=(RealisticGenerator)level.getChunkSource().getGenerator();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var savedEncoded=RealisticGenerator.CODEC.codec().encodeStart(ops,savedGenerator).getOrThrow();
            require(RealisticGenerator.CODEC.codec().parse(ops,savedEncoded).getOrThrow().entryRoutesEnabled()==savedGenerator.entryRoutesEnabled(),"Actual saved world preserves entry-route opt-in");
            JsonObject preset;
            try(var reader=new java.io.InputStreamReader(HabitatCodecSmoke.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json"),java.nio.charset.StandardCharsets.UTF_8)){
                preset=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();
            }
            preset.remove("type");var generator=RealisticGenerator.CODEC.codec().parse(ops,preset).getOrThrow();
            var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().getAsJsonObject();
            require(encoded.equals(preset),"Existing flat generator preset is byte-value/schema compatible");
            for(var entry:encoded.entrySet())require(entry.getKey().equals("additional_biomes")?entry.getValue().isJsonObject():entry.getValue().isJsonPrimitive(),"Only the optional additional-biome map is nested");
            require(encoded.getAsJsonObject("additional_biomes").has("ice_spikes"),"Current preset enables marine icebergs");
            var legacyPreset=preset.deepCopy();legacyPreset.remove("cave_biome_definition");legacyPreset.addProperty("shape_version",82);legacyPreset.remove("lit_cave_growth_definition");legacyPreset.remove("dripstone_definition");legacyPreset.remove("underground");legacyPreset.remove("cave_approaches");legacyPreset.remove("cave_entry_routes");legacyPreset.remove("ore_definition");legacyPreset.remove("sandy_coasts");legacyPreset.remove("icebergs");legacyPreset.getAsJsonObject("additional_biomes").remove("ice_spikes");legacyPreset.getAsJsonObject("additional_biomes").remove("cold_ocean");legacyPreset.getAsJsonObject("additional_biomes").remove("deep_cold_ocean");
            var legacyGenerator=RealisticGenerator.CODEC.codec().parse(ops,legacyPreset).getOrThrow();
            require(!legacyGenerator.undergroundEnabled(),"Legacy saves do not acquire caves");
            var frozenCaves=preset.deepCopy();frozenCaves.remove("cave_biome_definition");frozenCaves.addProperty("shape_version",82);frozenCaves.remove("lit_cave_growth_definition");frozenCaves.remove("dripstone_definition");frozenCaves.remove("cave_approaches");frozenCaves.remove("cave_entry_routes");frozenCaves.remove("ore_definition");require(!RealisticGenerator.CODEC.codec().parse(ops,frozenCaves).getOrThrow().approachesEnabled(),"Saved cave generator does not acquire approaches");
            var oldApproaches=preset.deepCopy();oldApproaches.remove("cave_biome_definition");oldApproaches.addProperty("shape_version",82);oldApproaches.remove("lit_cave_growth_definition");oldApproaches.remove("cave_entry_routes");oldApproaches.remove("ore_definition");require(!RealisticGenerator.CODEC.codec().parse(ops,oldApproaches).getOrThrow().entryRoutesEnabled(),"Saved approaches retain original route boundary");
            var invalidApproaches=preset.deepCopy();invalidApproaches.addProperty("cave_approaches",true);invalidApproaches.addProperty("underground",false);require(RealisticGenerator.CODEC.codec().parse(ops,invalidApproaches).error().isPresent(),"Approach flag requires caves");
            require(generator.undergroundEnabled(),"New preset enables underground geometry");
            require(generator.entryRoutesEnabled(),"New preset enables first chamber routes");
            var oldEntries=preset.deepCopy();oldEntries.remove("cave_biome_definition");oldEntries.addProperty("shape_version",82);oldEntries.remove("lit_cave_growth_definition");oldEntries.remove("ore_definition");require(RealisticGenerator.CODEC.codec().parse(ops,oldEntries).getOrThrow().oreDefinition()==0,"Saved entry-route worlds do not acquire ores");
            require(legacyGenerator.dripstoneDefinition()==0,"Legacy saves do not acquire dripstone");
            require(generator.litCaveGrowthDefinition()==2,"New preset enables moisture-gated growth");
            require(encoded.get("shape_version").getAsInt()==85,"Moss save schema rejects older binaries before server construction");
            var prototype=preset.deepCopy();prototype.remove("cave_biome_definition");prototype.addProperty("shape_version",82);prototype.addProperty("dripstone_definition",1);
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,prototype).getOrThrow()).getOrThrow().getAsJsonObject().get("shape_version").getAsInt()==83,"Prior private moss settings upgrade schema without changing feature flags");
            var missingCaves=preset.deepCopy();missingCaves.remove("cave_biome_definition");require(RealisticGenerator.CODEC.codec().parse(ops,missingCaves).error().isPresent(),"Schema85 cannot silently lose cave definition");
            var beforeGrowth=preset.deepCopy();beforeGrowth.remove("cave_biome_definition");beforeGrowth.remove("lit_cave_growth_definition");beforeGrowth.addProperty("shape_version",TerrainShape.SHAPE_VERSION);beforeGrowth.addProperty("dripstone_definition",1);
            require(RealisticGenerator.CODEC.codec().parse(ops,beforeGrowth).getOrThrow().litCaveGrowthDefinition()==0,"Saved generators do not acquire growth");
            var unsupportedGrowth=preset.deepCopy();unsupportedGrowth.addProperty("lit_cave_growth_definition",1);require(RealisticGenerator.CODEC.codec().parse(ops,unsupportedGrowth).error().isPresent(),"Unqualified prototype rejected");
            require(legacyGenerator.litCaveGrowthDefinition()==0,"Legacy roundtrip growth opt-out");
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,legacyGenerator).getOrThrow().getAsJsonObject().get("shape_version").getAsInt()==82,"Legacy settings retain schema82");
            require(generator.dripstoneDefinition()==2,"New preset enables dripstone definition one");
            require(generator.oreDefinition()==2,"New preset explicitly enables versioned existing ores");
            var invalidOre=preset.deepCopy();invalidOre.addProperty("ore_definition",3);require(RealisticGenerator.CODEC.codec().parse(ops,invalidOre).error().isPresent(),"Unknown ore definition rejected");
            var invalidEntries=preset.deepCopy();invalidEntries.addProperty("cave_approaches",false);require(RealisticGenerator.CODEC.codec().parse(ops,invalidEntries).error().isPresent(),"Entry routes require approaches");
            require(!legacyGenerator.sandyCoastsEnabled(),"Legacy saves retain the original sandy coast");
            require(generator.sandyCoastsEnabled(),"Current preset opts into local sandy profiles");
            require(!legacyGenerator.coldOceansEnabled(),"Legacy palette does not acquire cold ocean routing");
            require(generator.coldOceansEnabled(),"Current preset explicitly includes the cold pair");
            require(!legacyGenerator.icebergsEnabled(),"Saved legacy palette does not acquire iceberg content");
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,legacyGenerator).getOrThrow().getAsJsonObject().getAsJsonObject("additional_biomes").get("ice_spikes")==null,"Legacy opt-out round trips");
            var legacyExtra=preset.deepCopy();legacyExtra.remove("icebergs");require(!RealisticGenerator.CODEC.codec().parse(ops,legacyExtra).getOrThrow().icebergsEnabled(),"Unused legacy palette entry does not opt in");
            var missingIce=preset.deepCopy();missingIce.getAsJsonObject("additional_biomes").remove("ice_spikes");require(RealisticGenerator.CODEC.codec().parse(ops,missingIce).error().isPresent(),"Explicit iceberg feature requires its holder");
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,preset).getOrThrow();
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(encoded),"Generator holder identities round-trip");
            var source=(ScreeBiomeSource)generator.getBiomeSource();var sourceJson=ScreeBiomeSource.CODEC.codec().encodeStart(ops,source).getOrThrow().getAsJsonObject();var expectedSource=preset.deepCopy();expectedSource.remove("shape_version");expectedSource.remove("lit_cave_growth_definition");expectedSource.remove("underground");expectedSource.remove("cave_approaches");expectedSource.remove("cave_entry_routes");expectedSource.remove("ore_definition");
            require(sourceJson.equals(expectedSource),"Biome source shares the exact flat palette");
            var sourceCopy=ScreeBiomeSource.CODEC.codec().parse(ops,sourceJson).getOrThrow();require(ScreeBiomeSource.CODEC.codec().encodeStart(ops,sourceCopy).getOrThrow().equals(sourceJson),"Biome-source round-trip");
            for(String key:encoded.keySet()){
                var missing=encoded.deepCopy();missing.remove(key);if(!key.equals("icebergs")&&!key.equals("sandy_coasts")&&!key.equals("underground")&&!key.equals("cave_approaches")&&!key.equals("cave_entry_routes")&&!key.equals("ore_definition")&&!key.equals("dripstone_definition")&&!key.equals("lit_cave_growth_definition")&&!key.equals("cave_biome_definition"))require(RealisticGenerator.CODEC.codec().parse(ops,missing).error().isPresent(),"Required generator field rejected when missing: "+key);
                if(!key.equals("shape_version")&&!key.equals("icebergs")&&!key.equals("sandy_coasts")&&!key.equals("underground")&&!key.equals("cave_approaches")&&!key.equals("cave_entry_routes")&&!key.equals("ore_definition")&&!key.equals("dripstone_definition")&&!key.equals("lit_cave_growth_definition")&&!key.equals("cave_biome_definition")){var missingSource=sourceJson.deepCopy();missingSource.remove(key);require(ScreeBiomeSource.CODEC.codec().parse(ops,missingSource).error().isPresent(),"Required source field rejected: "+key);}
            }
            for(String key:new String[]{"savanna","jungle","desert","plains","sunflower_plains","forest","birch_forest","swamp","mangrove_swamp","taiga","snowy_taiga","ocean","deep_ocean","snowy_plains","snowy_slopes","meadow","grove","river","frozen_river","beach","snowy_beach","lukewarm_ocean","deep_lukewarm_ocean","old_growth_pine_taiga","old_growth_spruce_taiga","flower_forest","old_growth_birch_forest","dark_forest","cherry_grove","sparse_jungle","bamboo_jungle","windswept_hills","windswept_forest","savanna_plateau","windswept_savanna","stony_peaks","jagged_peaks","eroded_badlands","badlands","wooded_badlands","windswept_gravelly_hills","frozen_peaks","warm_ocean","dappled_forest","stony_shore","frozen_ocean","deep_frozen_ocean"}){var missing=encoded.deepCopy();missing.getAsJsonObject("additional_biomes").remove(key);require(RealisticGenerator.CODEC.codec().parse(ops,missing).error().isPresent(),"Completed additional holder required: "+key);}
            for(int version:new int[]{TerrainShape.SHAPE_VERSION-1,RealisticGenerator.CAVE_BIOME_SAVE_VERSION+1}){var wrong=encoded.deepCopy();wrong.addProperty("shape_version",version);require(RealisticGenerator.CODEC.codec().parse(ops,wrong).error().isPresent(),"Different terrain definition rejected");}
            var random=level.getChunkSource().randomState();var original=generator.drainage(random);var copy=decoded.drainage(random);sourceCopy.bind(random.seed(),copy,new ClimateField(random.seed()),(cx,cz)->decoded.independentLitCavePlan(cx,cz,random));
            var a=source.createUncachedResolver(random);var b=sourceCopy.createUncachedResolver(random);
            for(int[] p:new int[][]{{-765624,-1700133},{-798688,-1695080},{-1461088,-1698016},{-1428000,-1692000},{387960,-804006},{-1994386,-2040114},{-1994384,-2040112},{-1,0},{0,0},{11991,-756038},{1683985,-1900032}}){
                require(original.sample(p[0],p[1]).equals(copy.sample(p[0],p[1])),"Seeded drainage unchanged by grouping");
                for(int y:new int[]{-2000,0,2000})require(a.getNoiseBiome(p[0]>>2,y,p[1]>>2).equals(b.getNoiseBiome(p[0]>>2,y,p[1]>>2)),"Seeded all-altitude palette unchanged by grouping");
            }
            server.saveEverything(false,true,true);Files.writeString(Path.of(System.getProperty("blockbranch.habitatCodecResult")),"PASS\n");Blockbranch.LOGGER.info("HABITAT_FLAT_CODEC_COMPATIBILITY_PASS definition={} fields={}",TerrainShape.SHAPE_VERSION,encoded.size());
        }catch(Throwable failure){Blockbranch.LOGGER.error("HABITAT_CODEC_FAIL",failure);}finally{server.halt(false);}
    });}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private HabitatCodecSmoke(){}
}
