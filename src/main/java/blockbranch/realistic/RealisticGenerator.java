package blockbranch.realistic;

import blockbranch.cubic.SparseColumn;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** Procedural terrain with specific mountain, lake, river and ocean-shore habitats. */
public final class RealisticGenerator extends ChunkGenerator {
    // Independent saved opt-ins wrap the unchanged definition-82 terrain foundation.
    public static final int MOSS_SAVE_VERSION=83,FORMATION_SAVE_VERSION=84,CAVE_BIOME_SAVE_VERSION=85,STRUCTURE_SAVE_VERSION=86,CALIBRATED_STRUCTURE_SAVE_VERSION=87,ATLAS_STRUCTURE_SAVE_VERSION=88;
    // Group flat extension fields instead of exceeding RecordCodecBuilder's arity limit.
    private record DiscoveryData(int definition,java.util.Optional<BuriedTreasureDiscovery> treasure,java.util.Optional<DesertWaygates> gates,java.util.Optional<ShipwreckDiscovery> ships,java.util.Optional<WetlandHuts> huts,java.util.Optional<SnowShelters> shelters,java.util.Optional<WarmOceanRuins> ruins,java.util.Optional<ColdOceanRuins> coldRuins,java.util.Optional<DeepColdOceanRuins> deepColdRuins,java.util.Optional<DeepWarmOceanRuins> deepWarmRuins,int nativeBodies,int nativeOrdinary,java.util.Optional<ReimaginedSites> reimagined,int overworld,int mansionWings,int pyramidGallery){
        static final MapCodec<DiscoveryData> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Codec.intRange(0,1).optionalFieldOf("small_discovery_definition",0).forGetter(DiscoveryData::definition),
            BuriedTreasureDiscovery.CODEC.lenientOptionalFieldOf("treasure_cache").forGetter(DiscoveryData::treasure),
            DesertWaygates.CODEC.optionalFieldOf("desert_waygates").forGetter(DiscoveryData::gates),
            ShipwreckDiscovery.CODEC.optionalFieldOf("shipwrecks").forGetter(DiscoveryData::ships),
            WetlandHuts.CODEC.optionalFieldOf("wetland_huts").forGetter(DiscoveryData::huts),
            SnowShelters.CODEC.optionalFieldOf("snow_shelters").forGetter(DiscoveryData::shelters),
            WarmOceanRuins.CODEC.optionalFieldOf("warm_ruins").forGetter(DiscoveryData::ruins),
            ColdOceanRuins.CODEC.optionalFieldOf("cold_ruins").forGetter(DiscoveryData::coldRuins),
            DeepColdOceanRuins.CODEC.optionalFieldOf("deep_cold_ruins").forGetter(DiscoveryData::deepColdRuins),
            DeepWarmOceanRuins.CODEC.optionalFieldOf("deep_warm_ruins").forGetter(DiscoveryData::deepWarmRuins),
            Codec.intRange(0,1).optionalFieldOf("native_hut_igloo",0).forGetter(DiscoveryData::nativeBodies),
            Codec.intRange(0,1).optionalFieldOf("native_ordinary_structures",0).forGetter(DiscoveryData::nativeOrdinary),
            ReimaginedSites.CODEC.optionalFieldOf("reimagined_layouts").forGetter(DiscoveryData::reimagined),
            Codec.intRange(0,1).optionalFieldOf("native_overworld_structures",0).forGetter(DiscoveryData::overworld),
            Codec.intRange(0,1).optionalFieldOf("mansion_wing_definition",0).forGetter(DiscoveryData::mansionWings),
            Codec.intRange(0,1).optionalFieldOf("pyramid_gallery_definition",0).forGetter(DiscoveryData::pyramidGallery)
        ).apply(i,DiscoveryData::new));
        static DiscoveryData of(RealisticGenerator g){return new DiscoveryData(g.discoveryDefinition,g.discoveryDefinition>0&&g.treasure!=null&&g.treasure.canSave()?java.util.Optional.of(g.treasure):java.util.Optional.empty(),java.util.Optional.ofNullable(g.waygates),java.util.Optional.ofNullable(g.shipwrecks),java.util.Optional.ofNullable(g.wetlandHuts),java.util.Optional.ofNullable(g.snowShelters),java.util.Optional.ofNullable(g.warmRuins),java.util.Optional.ofNullable(g.coldRuins),java.util.Optional.ofNullable(g.deepColdRuins),java.util.Optional.ofNullable(g.deepWarmRuins),g.nativeHutIglooDefinition,g.nativeOrdinaryDefinition,java.util.Optional.ofNullable(g.reimaginedSites),g.overworldDefinition,g.mansionWingDefinition,g.pyramidGalleryDefinition);}
        RealisticGenerator applyTo(RealisticGenerator g){return g.withDiscoveries(definition,treasure).withWaygates(gates).withShipwrecks(ships).withWetlandHuts(huts).withSnowShelters(shelters).withWarmRuins(ruins).withColdRuins(coldRuins).withDeepColdRuins(deepColdRuins).withDeepWarmRuins(deepWarmRuins).withNativeHutIgloo(nativeBodies).withNativeOrdinary(nativeOrdinary).withReimagined(reimagined).withOverworld(overworld).withMansionWings(mansionWings).withPyramidGallery(pyramidGallery);}
    }
    private record FeatureSettings(DiscoveryData discovery,int gravelRelief,int sanctuaryReplacement,int mansionReplacement,int biomeTransition,int redwoodForest,int surfaceOwnership,int dryForest,int coolDesert,int abyssalTraces,int deepBasin,int landIce) {
        static final MapCodec<FeatureSettings> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            DiscoveryData.CODEC.forGetter(FeatureSettings::discovery),
            Codec.intRange(0,1).optionalFieldOf("gravel_relief_definition",0).forGetter(FeatureSettings::gravelRelief),
            Codec.intRange(0,1).optionalFieldOf("sanctuary_replacement_definition",0).forGetter(FeatureSettings::sanctuaryReplacement),
            Codec.intRange(0,1).optionalFieldOf("mansion_replacement_definition",0).forGetter(FeatureSettings::mansionReplacement),
            Codec.intRange(0,1).optionalFieldOf("biome_transition_definition",0).forGetter(FeatureSettings::biomeTransition),
            Codec.intRange(0,1).optionalFieldOf("redwood_forest_definition",0).forGetter(FeatureSettings::redwoodForest),
            Codec.intRange(0,1).optionalFieldOf("surface_ownership_definition",0).forGetter(FeatureSettings::surfaceOwnership),
            Codec.intRange(0,1).optionalFieldOf("dry_forest_definition",0).forGetter(FeatureSettings::dryForest),
            Codec.intRange(0,1).optionalFieldOf("cool_desert_definition",0).forGetter(FeatureSettings::coolDesert),
            Codec.intRange(0,1).optionalFieldOf("abyssal_trace_definition",0).forGetter(FeatureSettings::abyssalTraces),
            Codec.intRange(0,1).optionalFieldOf("deep_basin_relief_definition",0).forGetter(FeatureSettings::deepBasin),
            Codec.intRange(0,1).optionalFieldOf("land_ice_definition",0).forGetter(FeatureSettings::landIce)
        ).apply(i,FeatureSettings::new));
        static FeatureSettings of(RealisticGenerator g){return new FeatureSettings(DiscoveryData.of(g),g.gravelReliefDefinition,g.sanctuaryReplacementDefinition,g.mansionReplacementDefinition,g.biomeTransitionDefinition,g.redwoodForestDefinition,g.surfaceOwnershipDefinition,g.dryForestDefinition,g.coolDesertDefinition,g.abyssalTraceDefinition,g.deepBasinReliefDefinition,g.landIceDefinition);}
        RealisticGenerator applyTo(RealisticGenerator g){return discovery.applyTo(g).withGravelRelief(gravelRelief).withSanctuaryReplacement(sanctuaryReplacement).withMansionReplacement(mansionReplacement).withBiomeTransitions(biomeTransition).withRedwoodForests(redwoodForest) .withSurfaceOwnership(surfaceOwnership).withDryForests(dryForest).withCoolDeserts(coolDesert).withAbyssalTraces(abyssalTraces).withDeepBasinRelief(deepBasin).withLandIce(landIce);}
    }
    public static final MapCodec<RealisticGenerator> CODEC = RecordCodecBuilder.<RealisticGenerator>mapCodec(instance -> instance.group(
        HabitatPalette.CODEC.forGetter(generator -> generator.palette),
        Codec.intRange(TerrainShape.SHAPE_VERSION,LandIceSchema.MAX).fieldOf("shape_version").forGetter(RealisticGenerator::saveSchema),
        Codec.BOOL.optionalFieldOf("icebergs",false).forGetter(generator -> generator.icebergsEnabled),
        Codec.BOOL.optionalFieldOf("sandy_coasts",false).forGetter(generator -> generator.sandyCoastsEnabled),
        Codec.BOOL.optionalFieldOf("underground",false).forGetter(generator -> generator.undergroundEnabled),
        Codec.BOOL.optionalFieldOf("cave_approaches",false).forGetter(generator -> generator.approachesEnabled),
        Codec.BOOL.optionalFieldOf("cave_entry_routes",false).forGetter(generator -> generator.entryRoutesEnabled),
        Codec.intRange(0,OreLayer.DEFINITION).optionalFieldOf("ore_definition",0).forGetter(generator -> generator.oreDefinition),
        Codec.intRange(0,2).optionalFieldOf("dripstone_definition",0).forGetter(generator -> generator.dripstoneDefinition),
        Codec.intRange(0,2).validate(d->d==0||d==2?com.mojang.serialization.DataResult.success(d):com.mojang.serialization.DataResult.error(()->"Unsupported prototype lit cave definition")).optionalFieldOf("lit_cave_growth_definition",0).forGetter(generator -> generator.litCaveGrowthDefinition),
        Codec.intRange(0,1).optionalFieldOf("cave_biome_definition",0).forGetter(generator -> generator.caveBiomeDefinition),
        Codec.intRange(0,3).optionalFieldOf("structure_definition",0).forGetter(generator -> generator.structureDefinition),
        StructurePlacementCache.CODEC.lenientOptionalFieldOf("structure_cache").forGetter(generator -> generator.structureDefinition==3?generator.structureCache:java.util.Optional.empty()),
        Codec.intRange(0,1).optionalFieldOf("boreal_crown_definition",0).forGetter(RealisticGenerator::borealCrownDefinition),
        FeatureSettings.CODEC.forGetter(FeatureSettings::of)
    ).apply(instance,(palette,version,icebergs,sandy,underground,approaches,entries,ores,dripstone,growth,caveBiomes,structures,cache,crowns,features)->features.applyTo(new RealisticGenerator(palette,icebergs,sandy,underground,approaches,entries,ores,dripstone,growth,caveBiomes,version,structures).withStructureCache(cache).withBorealCrowns(crowns)))).validate(g->!LandIceSchema.matches(g.inputSchema,g.landIceDefinition)?com.mojang.serialization.DataResult.error(()->"Land ice schema/definition mismatch"):g.landIceDefinition>0&&!g.palette.additional().containsKey(LandIceField.KEY)?com.mojang.serialization.DataResult.error(()->"Land ice requires biome holder"):!DeepBasinSchema.matches(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition)?com.mojang.serialization.DataResult.error(()->"Deep basin relief schema/definition mismatch"):!AbyssalTraceSchema.matches(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition)?com.mojang.serialization.DataResult.error(()->"Abyssal trace schema/definition mismatch"):!CoolDesertSchema.matches(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition)?com.mojang.serialization.DataResult.error(()->"Cool desert schema/definition mismatch"):g.coolDesertDefinition>0&&!g.palette.additional().containsKey(CoolDesert.KEY)?com.mojang.serialization.DataResult.error(()->"Cool desert requires biome holder"):!DryForestSchema.matches(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition)?com.mojang.serialization.DataResult.error(()->"Dry forest schema/definition mismatch"):g.dryForestDefinition>0&&!g.palette.additional().containsKey(DryForest.KEY)?com.mojang.serialization.DataResult.error(()->"Dry forest requires biome holder"):!SurfaceOwnershipSchema.matches(DryForestSchema.baseSchema(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition),g.surfaceOwnershipDefinition)?com.mojang.serialization.DataResult.error(()->"Surface ownership schema/definition mismatch"):g.surfaceOwnershipDefinition>0&&!g.palette.additional().containsKey(ColdRockland.KEY)?com.mojang.serialization.DataResult.error(()->"Surface ownership requires cold rockland holder"):!RedwoodForestSchema.matches(SurfaceOwnershipSchema.baseSchema(DryForestSchema.baseSchema(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition),g.surfaceOwnershipDefinition),g.redwoodForestDefinition)?com.mojang.serialization.DataResult.error(()->"Redwood forest schema/definition mismatch"):g.redwoodForestDefinition>0&&!g.palette.additional().containsKey(RedwoodForest.KEY)?com.mojang.serialization.DataResult.error(()->"Redwood forests require their biome holder"):!BiomeTransitionSchema.matches(RedwoodForestSchema.baseSchema(SurfaceOwnershipSchema.baseSchema(DryForestSchema.baseSchema(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition),g.surfaceOwnershipDefinition),g.redwoodForestDefinition),g.biomeTransitionDefinition)?com.mojang.serialization.DataResult.error(()->"Biome transition schema/definition mismatch"):!MansionReplacementSchema.matches(BiomeTransitionSchema.baseSchema(RedwoodForestSchema.baseSchema(SurfaceOwnershipSchema.baseSchema(DryForestSchema.baseSchema(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition),g.surfaceOwnershipDefinition),g.redwoodForestDefinition),g.biomeTransitionDefinition),g.mansionReplacementDefinition)?com.mojang.serialization.DataResult.error(()->"Mansion replacement schema/definition mismatch"):!SanctuaryReplacementSchema.matches(MansionReplacementSchema.baseSchema(BiomeTransitionSchema.baseSchema(RedwoodForestSchema.baseSchema(SurfaceOwnershipSchema.baseSchema(DryForestSchema.baseSchema(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition),g.surfaceOwnershipDefinition),g.redwoodForestDefinition),g.biomeTransitionDefinition),g.mansionReplacementDefinition),g.sanctuaryReplacementDefinition)?com.mojang.serialization.DataResult.error(()->"Sanctuary replacement schema/definition mismatch"):!GravelReliefSchema.matches(SanctuaryReplacementSchema.baseSchema(MansionReplacementSchema.baseSchema(BiomeTransitionSchema.baseSchema(RedwoodForestSchema.baseSchema(SurfaceOwnershipSchema.baseSchema(DryForestSchema.baseSchema(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition),g.surfaceOwnershipDefinition),g.redwoodForestDefinition),g.biomeTransitionDefinition),g.mansionReplacementDefinition),g.sanctuaryReplacementDefinition),g.gravelReliefDefinition)?com.mojang.serialization.DataResult.error(()->"Gravel relief schema/definition mismatch"):g.pyramidGalleryDefinition>0&&g.reimaginedDefinition()==0?com.mojang.serialization.DataResult.error(()->"Pyramid galleries require reimagined layouts"):g.overworldDefinition>0&&!g.palette.additional().containsKey("deep_dark")?com.mojang.serialization.DataResult.error(()->"Overworld coverage requires deep_dark holder"):!StructureGenerationSchema.matches(PyramidGallerySchema.baseSchema(BorealCrownSchema.baseSchema(GravelReliefSchema.baseSchema(SanctuaryReplacementSchema.baseSchema(MansionReplacementSchema.baseSchema(BiomeTransitionSchema.baseSchema(RedwoodForestSchema.baseSchema(SurfaceOwnershipSchema.baseSchema(DryForestSchema.baseSchema(CoolDesertSchema.baseSchema(AbyssalTraceSchema.baseSchema(DeepBasinSchema.baseSchema(LandIceSchema.baseSchema(g.inputSchema,g.landIceDefinition),g.deepBasinReliefDefinition),g.abyssalTraceDefinition),g.coolDesertDefinition),g.dryForestDefinition),g.surfaceOwnershipDefinition),g.redwoodForestDefinition),g.biomeTransitionDefinition),g.mansionReplacementDefinition),g.sanctuaryReplacementDefinition),g.gravelReliefDefinition),g.borealCrownDefinition),g.pyramidGalleryDefinition),g.discoveryDefinition,g.shipwreckDefinition(),g.waygateDefinition(),g.wetlandHutDefinition(),g.snowShelterDefinition(),g.warmRuinDefinition(),g.coldRuinDefinition(),g.deepColdRuinDefinition(),g.deepWarmRuinDefinition(),g.nativeHutIglooDefinition(),g.nativeOrdinaryDefinition(),g.reimaginedDefinition(),g.overworldDefinition,g.mansionWingDefinition)?com.mojang.serialization.DataResult.error(()->"Structure feature schema mismatch"):(g.inputSchema>=ATLAS_STRUCTURE_SAVE_VERSION)!=(g.structureDefinition==3)?com.mojang.serialization.DataResult.error(()->"Structure atlas schema/definition mismatch"):(g.inputSchema==CALIBRATED_STRUCTURE_SAVE_VERSION)!=(g.structureDefinition==2)?com.mojang.serialization.DataResult.error(()->"Calibrated structure schema/definition mismatch"):(g.inputSchema==STRUCTURE_SAVE_VERSION)!=(g.structureDefinition==1)?com.mojang.serialization.DataResult.error(()->"Structure schema/definition mismatch"):(g.inputSchema>=CAVE_BIOME_SAVE_VERSION)!=(g.caveBiomeDefinition==1)?com.mojang.serialization.DataResult.error(()->"Cave biome schema/definition mismatch"):g.caveBiomeDefinition>0&&(g.dripstoneDefinition!=2||g.litCaveGrowthDefinition!=2||!g.palette.additional().containsKey("lush_caves")||!g.palette.additional().containsKey("sulfur_caves"))?com.mojang.serialization.DataResult.error(()->"Cave biomes require formation/moss definitions and both holders"):g.litCaveGrowthDefinition>0&&(!g.undergroundEnabled||!g.approachesEnabled)?com.mojang.serialization.DataResult.error(()->"Lit cave growth requires existing cave approaches"):g.dripstoneDefinition>0&&(!g.undergroundEnabled||!g.palette.additional().containsKey("dripstone_caves"))?com.mojang.serialization.DataResult.error(()->"Dripstone requires caves and its biome holder"):g.entryRoutesEnabled&&!g.approachesEnabled?com.mojang.serialization.DataResult.error(()->"Entry routes require approaches"):g.approachesEnabled&&!g.undergroundEnabled?com.mojang.serialization.DataResult.error(()->"Approaches require caves"):g.icebergsEnabled&&!g.palette.additional().containsKey("ice_spikes")?com.mojang.serialization.DataResult.error(()->"Iceberg habitat requires ice_spikes holder"):com.mojang.serialization.DataResult.success(g));
    private int saveSchema(){return LandIceSchema.schema(DeepBasinSchema.schema(AbyssalTraceSchema.schema(CoolDesertSchema.schema(DryForestSchema.schema(SurfaceOwnershipSchema.schema(RedwoodForestSchema.schema(BiomeTransitionSchema.schema(MansionReplacementSchema.schema(SanctuaryReplacementSchema.schema(GravelReliefSchema.schema(BorealCrownSchema.schema(PyramidGallerySchema.schema(StructureGenerationSchema.versionFor(structureDefinition==3?ATLAS_STRUCTURE_SAVE_VERSION:structureDefinition==2?CALIBRATED_STRUCTURE_SAVE_VERSION:structureDefinition>0?STRUCTURE_SAVE_VERSION:caveBiomeDefinition>0?CAVE_BIOME_SAVE_VERSION:dripstoneDefinition==2?FORMATION_SAVE_VERSION:litCaveGrowthDefinition>0?MOSS_SAVE_VERSION:TerrainShape.SHAPE_VERSION,discoveryDefinition,shipwreckDefinition(),waygateDefinition(),wetlandHutDefinition(),snowShelterDefinition(),warmRuinDefinition(),coldRuinDefinition(),deepColdRuinDefinition(),deepWarmRuinDefinition(),nativeHutIglooDefinition(),nativeOrdinaryDefinition(),reimaginedDefinition(),overworldDefinition,mansionWingDefinition),pyramidGalleryDefinition),borealCrownDefinition),gravelReliefDefinition),sanctuaryReplacementDefinition),mansionReplacementDefinition),biomeTransitionDefinition),redwoodForestDefinition),surfaceOwnershipDefinition),dryForestDefinition),coolDesertDefinition),abyssalTraceDefinition),deepBasinReliefDefinition),landIceDefinition);}
    private int biomeTransitionDefinition,redwoodForestDefinition,surfaceOwnershipDefinition,dryForestDefinition,coolDesertDefinition,abyssalTraceDefinition,deepBasinReliefDefinition,landIceDefinition;
    private RealisticGenerator withLandIce(int definition){landIceDefinition=definition;((ScreeBiomeSource)biomeSource).withLandIce(definition>0);return this;}
    public int landIceDefinition(){return landIceDefinition;}
    private RealisticGenerator withDeepBasinRelief(int definition){deepBasinReliefDefinition=definition;return this;}
    public int deepBasinReliefDefinition(){return deepBasinReliefDefinition;}
    private RealisticGenerator withAbyssalTraces(int definition){abyssalTraceDefinition=definition;return this;}
    public int abyssalTraceDefinition(){return abyssalTraceDefinition;}
    private RealisticGenerator withCoolDeserts(int definition){coolDesertDefinition=definition;((ScreeBiomeSource)biomeSource).withCoolDeserts(definition>0);return this;}
    public int coolDesertDefinition(){return coolDesertDefinition;}
    private RealisticGenerator withDryForests(int definition){dryForestDefinition=definition;((ScreeBiomeSource)biomeSource).withDryForests(definition>0);return this;}
    public int dryForestDefinition(){return dryForestDefinition;}
    private RealisticGenerator withSurfaceOwnership(int definition){surfaceOwnershipDefinition=definition;((ScreeBiomeSource)biomeSource).withSurfaceOwnership(definition>0);return this;}
    public int surfaceOwnershipDefinition(){return surfaceOwnershipDefinition;}
    private RealisticGenerator withRedwoodForests(int definition){redwoodForestDefinition=definition;((ScreeBiomeSource)biomeSource).withRedwoodForests(definition>0);return this;}
    public int redwoodForestDefinition(){return redwoodForestDefinition;}
    /** Bits 0/1/2 retain transitions/redwoods/dry forest in saved tree-clear plans. */
    public int vegetationFlags(){return biomeTransitionDefinition|(redwoodForestDefinition>0?RedwoodForest.FLAG:0)|(dryForestDefinition>0?DryForest.FLAG:0);}
    private RealisticGenerator withBiomeTransitions(int definition){biomeTransitionDefinition=definition;return this;}
    public int biomeTransitionDefinition(){return biomeTransitionDefinition;}
    private int mansionReplacementDefinition;
    private RealisticGenerator withMansionReplacement(int definition){if(definition<0||definition>1)throw new IllegalArgumentException("Unsupported mansion replacement definition");mansionReplacementDefinition=definition;return this;}
    public int mansionReplacementDefinition(){return mansionReplacementDefinition;}
    private int sanctuaryReplacementDefinition;
    private RealisticGenerator withSanctuaryReplacement(int definition){if(definition<0||definition>1)throw new IllegalArgumentException("Unsupported sanctuary replacement definition");sanctuaryReplacementDefinition=definition;return this;}
    public int sanctuaryReplacementDefinition(){return sanctuaryReplacementDefinition;}
    private int gravelReliefDefinition;
    private RealisticGenerator withGravelRelief(int definition){
        if(definition<0||definition>1)throw new IllegalArgumentException("Unsupported gravel relief definition");
        gravelReliefDefinition=definition;return this;
    }
    public int gravelReliefDefinition(){return gravelReliefDefinition;}
    private int borealCrownDefinition;
    private RealisticGenerator withBorealCrowns(int definition){borealCrownDefinition=definition;return this;}
    public int borealCrownDefinition(){return borealCrownDefinition;}
    private volatile DesertWaygates waygates;
    private long waygateSeed,waygateInitNanos;private boolean waygateBound,waygateRestored;
    private RealisticGenerator withWaygates(java.util.Optional<DesertWaygates> gates){waygates=gates.orElse(null);return this;}
    public int waygateDefinition(){return waygates==null?0:1;}
    public long waygateInitializationNanos(){return waygateInitNanos;}
    public boolean waygateCacheRestored(){return waygateRestored;}
    public synchronized DesertWaygates waygates(RandomState random){
        if(waygates==null)throw new IllegalStateException("Waygates disabled");if(waygateBound&&waygateSeed==random.seed())return waygates;
        long start=System.nanoTime();var source=wells(random);waygateRestored=waygates.valid(random.seed(),sandyCoastsEnabled);
        if(!waygateRestored){var t=terrain(random);waygates=DesertWaygates.build(random.seed(),sandyCoastsEnabled,source,t.drainage(),t.climate());}
        waygateSeed=random.seed();waygateBound=true;waygateInitNanos=System.nanoTime()-start;return waygates;
    }
    private final HabitatPalette palette;
    private final int structureDefinition;
    private volatile DesertWell wells;
    private int discoveryDefinition;
    private volatile BuriedTreasureDiscovery treasure;
    private boolean treasureRestored,treasureBound;
    private long treasureSeed,treasureInitializationNanos;
    public long treasureInitializationNanos(){return treasureInitializationNanos;}
    public boolean treasureRestored(){return treasureRestored;}
    private RealisticGenerator withDiscoveries(int definition,java.util.Optional<BuriedTreasureDiscovery> cache){discoveryDefinition=definition;if(definition>0)treasure=cache.orElse(null);return this;}
    public int discoveryDefinition(){return discoveryDefinition;}
    public synchronized BuriedTreasureDiscovery treasures(RandomState random){
        if(treasureBound&&treasureSeed==random.seed())return treasure;
        long started=System.nanoTime();
        treasureRestored=treasure!=null&&treasure.valid(random.seed(),sandyCoastsEnabled);
        if(!treasureRestored){var field=terrain(random);treasure=BuriedTreasureDiscovery.build(random.seed(),sandyCoastsEnabled,field.drainage(),field.climate());}
        var field=terrain(random);treasure.bind(field.drainage(),field.climate());treasureSeed=random.seed();treasureBound=true;treasureInitializationNanos=System.nanoTime()-started;return treasure;
    }
    private volatile WarmOceanRuins warmRuins;private boolean ruinBound,ruinRestored;private long ruinSeed,ruinInitNanos;
    private RealisticGenerator withWarmRuins(java.util.Optional<WarmOceanRuins> state){warmRuins=state.orElse(null);return this;}
    public int warmRuinDefinition(){return warmRuins==null?0:1;}
    public long warmRuinInitializationNanos(){return ruinInitNanos;}public boolean warmRuinCacheRestored(){return ruinRestored;}
    public synchronized WarmOceanRuins warmRuins(RandomState random){if(warmRuins==null)throw new IllegalStateException("Warm ruins disabled");if(ruinBound&&ruinSeed==random.seed())return warmRuins;long begin=System.nanoTime();ruinRestored=warmRuins.valid(random.seed());var built=warmRuins.bindSaved(random.seed(),this,random);warmRuins=built;ruinSeed=random.seed();ruinBound=true;ruinInitNanos=System.nanoTime()-begin;return built;}
    private volatile ColdOceanRuins coldRuins;private boolean coldBound,coldRestored;private long coldSeed,coldInitNanos;
    private RealisticGenerator withColdRuins(java.util.Optional<ColdOceanRuins> state){coldRuins=state.orElse(null);return this;}
    public int coldRuinDefinition(){return coldRuins==null?0:1;}
    public long coldRuinInitializationNanos(){return coldInitNanos;}public boolean coldRuinCacheRestored(){return coldRestored;}
    public synchronized ColdOceanRuins coldRuins(RandomState random){if(coldRuins==null)throw new IllegalStateException("Cold ruins disabled");if(coldBound&&coldSeed==random.seed())return coldRuins;long begin=System.nanoTime();coldRestored=coldRuins.valid(random.seed());var built=coldRuins.bindSaved(random.seed(),this,random);coldRuins=built;coldSeed=random.seed();coldBound=true;coldInitNanos=System.nanoTime()-begin;return built;}
    private volatile DeepColdOceanRuins deepColdRuins;private boolean deepColdBound,deepColdRestored;private long deepColdSeed,deepColdInitNanos;
    private RealisticGenerator withDeepColdRuins(java.util.Optional<DeepColdOceanRuins> state){deepColdRuins=state.orElse(null);return this;}
    public int deepColdRuinDefinition(){return deepColdRuins==null?0:1;}
    public long deepColdRuinInitializationNanos(){return deepColdInitNanos;}public boolean deepColdRuinCacheRestored(){return deepColdRestored;}
    public synchronized DeepColdOceanRuins deepColdRuins(RandomState random){if(deepColdRuins==null)throw new IllegalStateException("Deep cold ruins disabled");if(deepColdBound&&deepColdSeed==random.seed())return deepColdRuins;long begin=System.nanoTime();deepColdRestored=deepColdRuins.valid(random.seed());var built=deepColdRuins.bindSaved(random.seed(),this,random);deepColdRuins=built;deepColdSeed=random.seed();deepColdBound=true;deepColdInitNanos=System.nanoTime()-begin;return built;}
    private volatile ReimaginedSites reimaginedSites;private boolean reimaginedBound;private long reimaginedSeed;
    private RealisticGenerator withReimagined(java.util.Optional<ReimaginedSites> sites){reimaginedSites=sites.orElse(null);return this;}
    public int reimaginedDefinition(){return reimaginedSites==null?0:reimaginedSites.definition();}
    public synchronized ReimaginedSites reimagined(RandomState random){if(reimaginedSites==null)throw new IllegalStateException("Reimagined layouts disabled");if(reimaginedBound&&reimaginedSeed==random.seed())return reimaginedSites;var built=reimaginedSites.bind(random.seed(),this,random);reimaginedSites=built;reimaginedSeed=random.seed();reimaginedBound=true;return built;}
    private int mansionWingDefinition;
    private RealisticGenerator withMansionWings(int definition){mansionWingDefinition=definition;return this;}
    public int mansionWingDefinition(){return mansionWingDefinition;}
    private int pyramidGalleryDefinition;
    private RealisticGenerator withPyramidGallery(int definition){pyramidGalleryDefinition=definition;return this;}
    public int pyramidGalleryDefinition(){return pyramidGalleryDefinition;}
    private int overworldDefinition;
    private RealisticGenerator withOverworld(int definition){overworldDefinition=definition;((ScreeBiomeSource)biomeSource).withDeepDark(definition>0);return this;}
    public int overworldDefinition(){return overworldDefinition;}
    private int nativeOrdinaryDefinition;
    private RealisticGenerator withNativeOrdinary(int definition){nativeOrdinaryDefinition=definition;return this;}
    public int nativeOrdinaryDefinition(){return nativeOrdinaryDefinition;}
    private int nativeHutIglooDefinition;
    private RealisticGenerator withNativeHutIgloo(int definition){nativeHutIglooDefinition=definition;return this;}
    public int nativeHutIglooDefinition(){return nativeHutIglooDefinition;}
    private volatile DeepWarmOceanRuins deepWarmRuins;private boolean deepWarmBound,deepWarmRestored;private long deepWarmSeed,deepWarmInitNanos;
    private RealisticGenerator withDeepWarmRuins(java.util.Optional<DeepWarmOceanRuins> state){deepWarmRuins=state.orElse(null);return this;}
    public int deepWarmRuinDefinition(){return deepWarmRuins==null?0:1;}
    public long deepWarmRuinInitializationNanos(){return deepWarmInitNanos;}public boolean deepWarmRuinCacheRestored(){return deepWarmRestored;}
    public synchronized DeepWarmOceanRuins deepWarmRuins(RandomState random){if(deepWarmRuins==null)throw new IllegalStateException("Deep warm ruins disabled");if(deepWarmBound&&deepWarmSeed==random.seed())return deepWarmRuins;long begin=System.nanoTime();deepWarmRestored=deepWarmRuins.valid(random.seed());var built=deepWarmRuins.bindSaved(random.seed(),this,random);deepWarmRuins=built;deepWarmSeed=random.seed();deepWarmBound=true;deepWarmInitNanos=System.nanoTime()-begin;return built;}
    private volatile SnowShelters snowShelters;private boolean snowBound,snowRestored;private long snowSeed,snowInitNanos;
    private RealisticGenerator withSnowShelters(java.util.Optional<SnowShelters> state){snowShelters=state.orElse(null);return this;}
    public int snowShelterDefinition(){return snowShelters==null?0:1;}
    public long snowShelterInitializationNanos(){return snowInitNanos;}public boolean snowShelterCacheRestored(){return snowRestored;}
    public synchronized SnowShelters snowShelters(RandomState random){if(snowShelters==null)throw new IllegalStateException("Snow shelters disabled");if(snowBound&&snowSeed==random.seed())return snowShelters;long begin=System.nanoTime();snowRestored=snowShelters.valid(random.seed());var built=snowShelters.bindSaved(random.seed(),this,random);snowShelters=built;snowSeed=random.seed();snowBound=true;snowInitNanos=System.nanoTime()-begin;return built;}
    private volatile WetlandHuts wetlandHuts;private boolean hutRestored,hutBound;private long hutSeed,hutInitNanos;
    private RealisticGenerator withWetlandHuts(java.util.Optional<WetlandHuts> state){wetlandHuts=state.orElse(null);return this;}
    public int wetlandHutDefinition(){return wetlandHuts==null?0:1;}
    public long wetlandHutInitializationNanos(){return hutInitNanos;}public boolean wetlandHutCacheRestored(){return hutRestored;}
    public synchronized WetlandHuts wetlandHuts(RandomState random){if(wetlandHuts==null)throw new IllegalStateException("Huts disabled");if(hutBound&&hutSeed==random.seed())return wetlandHuts;long t=System.nanoTime();hutRestored=wetlandHuts.valid(random.seed());if(!hutRestored)wetlandHuts=WetlandHuts.bind(random.seed(),this,random);hutSeed=random.seed();hutBound=true;hutInitNanos=System.nanoTime()-t;return wetlandHuts;}
    private volatile ShipwreckDiscovery shipwrecks;
    private boolean shipwreckBound,shipwreckRestored;private long shipwreckSeed,shipwreckInitNanos;
    private RealisticGenerator withShipwrecks(java.util.Optional<ShipwreckDiscovery> state){shipwrecks=state.orElse(null);return this;}
    // Feature-combination flag; the required discovery codec separately guards template revision.
    public int shipwreckDefinition(){return shipwrecks==null?0:1;}
    public long shipwreckInitializationNanos(){return shipwreckInitNanos;}
    public boolean shipwreckCacheRestored(){return shipwreckRestored;}
    public synchronized ShipwreckDiscovery shipwrecks(RandomState random){
        if(shipwrecks==null)throw new IllegalStateException("Shipwrecks not enabled");if(shipwreckBound&&shipwreckSeed==random.seed())return shipwrecks;
        long started=System.nanoTime();var source=treasures(random);shipwreckRestored=shipwrecks.valid(random.seed(),source);if(!shipwreckRestored)shipwrecks=ShipwreckDiscovery.build(random.seed(),source,shipwrecks.templateRevision());
        var t=terrain(random);shipwrecks.bind(t.drainage(),t.climate(),source);shipwreckSeed=random.seed();shipwreckBound=true;shipwreckInitNanos=System.nanoTime()-started;return shipwrecks;
    }
    private volatile java.util.Optional<StructurePlacementCache> structureCache=java.util.Optional.empty();
    private RealisticGenerator withStructureCache(java.util.Optional<StructurePlacementCache> cache){if(structureDefinition==3)structureCache=cache;return this;}
    boolean completedWetlandHutCacheNeedsSave(){return wetlandHuts!=null&&wetlandHuts.canSave()&&!hutRestored;}
    public boolean structureCacheNeedsSave(){return wells!=null&&!wells.restoredFromSave()&&cachedStructureBytes()>0||discoveryDefinition>0&&treasure!=null&&treasure.canSave()&&!treasureRestored||waygates!=null&&waygates.canSave()&&!waygateRestored||shipwrecks!=null&&shipwrecks.canSave()&&!shipwreckRestored||wetlandHuts!=null&&wetlandHuts.canSave()&&!hutRestored||snowShelters!=null&&snowShelters.canSave()&&!snowRestored||warmRuins!=null&&warmRuins.canSave()&&!ruinRestored||coldRuins!=null&&coldRuins.canSave()&&!coldRestored||deepColdRuins!=null&&deepColdRuins.canSave()&&!deepColdRestored||deepWarmRuins!=null&&deepWarmRuins.canSave()&&!deepWarmRestored;}
    public int cachedStructureBytes(){return structureCache.map(StructurePlacementCache::retainedBytes).orElse(0);}
    private long wellSeed;
    public boolean structureAtlasPrepared(){return wells!=null&&wells.atlas()!=null;}
    public int structureDefinition(){return structureDefinition;}
    public synchronized DesertWell wells(RandomState random){
        if(wells==null||wellSeed!=random.seed()){
            var t=terrain(random);var cached=structureDefinition==3?structureCache.map(c->c.restore(t.seed(),sandyCoastsEnabled)).orElse(null):null;
            wells=new DesertWell(t.seed(),t.drainage(),t.climate(),Math.max(1,structureDefinition),cached);wellSeed=t.seed();
            if(structureDefinition==3)structureCache=StructurePlacementCache.capture(t.seed(),sandyCoastsEnabled,wells);
        }
        return wells;
    }
    private final boolean undergroundEnabled;
    private final boolean approachesEnabled;
    private final boolean entryRoutesEnabled;
    private final int oreDefinition,dripstoneDefinition,litCaveGrowthDefinition,caveBiomeDefinition,inputSchema;
    // A profile has no back-reference to its chunk. Weak keys and the hard cap
    // bound cancelled/out-of-order native stages without retaining a world.
    private final java.util.Map<ChunkAccess,blockbranch.cubic.TerrainProfile> preparedProfiles=new java.util.WeakHashMap<>();
    private final java.util.Map<Holder<Biome>,Integer> oreRegions=new java.util.concurrent.ConcurrentHashMap<>();
    private final boolean icebergsEnabled;
    private final boolean sandyCoastsEnabled;
    private final Holder<Biome> biome;
    private final Holder<Biome> screeBiome;
    private final Holder<Biome> reedbedBiome;
    private final Holder<Biome> riverMarginBiome;
    private final Holder<Biome> sandyShoreBiome;
    private final Holder<Biome> meadowBiome;
    private final Holder<Biome> woodlandBiome;
    private final Holder<Biome> steppeBiome;
    private final Holder<Biome> borealBiome;
    private final Holder<Biome> warmBiome;
    private final Holder<Biome> coldSteppeBiome;
    private final Holder<Biome> desertBiome;
    private final Holder<Biome> humidBiome;
    private final Holder<Biome> tundraBiome;
    private final Holder<Biome> lakeBiome;
    private final Holder<Biome> riverChannelBiome;
    private final Holder<Biome> shelfBiome;
    private final Holder<Biome> deepOceanBiome;
    private volatile TerrainState terrainState;
    private record TerrainState(long seed, TerrainShape shape, DrainageNetwork drainage,ClimateField climate,GroundEcology.Field groundEcology,UndergroundStructure underground,IcebergField icebergs,java.util.LinkedHashMap<Long,blockbranch.cubic.CanopyLayer> canopies,java.util.LinkedHashMap<Long,CaveApproach> approaches,java.util.LinkedHashMap<Long,blockbranch.cubic.DripstoneLayer> dripstone,java.util.LinkedHashMap<Long,blockbranch.cubic.LitCaveLayer> litCaves) {}

    // Minecraft may request base columns from several generation threads. Build
    // once, publish the immutable graph, and never use a process-global seed cache.
    private TerrainState terrain(RandomState random) {
        var current = terrainState;
        if (current != null && current.seed() == random.seed()) return current;
        synchronized (this) {
            current = terrainState;
            if (current == null || current.seed() != random.seed()) {
                var shape = new TerrainShape(random.seed());
                var drainage=new DrainageNetwork(shape,random.seed(),true,sandyCoastsEnabled,gravelReliefDefinition==1,surfaceOwnershipDefinition==1,deepBasinReliefDefinition==1,landIceDefinition==1);var climate=new ClimateField(random.seed());
                current = new TerrainState(random.seed(),shape,drainage,climate,new GroundEcology.Field(drainage::sample,climate),new UndergroundStructure(random.seed()),new IcebergField(random.seed(),drainage,climate),new java.util.LinkedHashMap<>(128,.75f,true),new java.util.LinkedHashMap<>(64,.75f,true),new java.util.LinkedHashMap<>(64,.75f,true),new java.util.LinkedHashMap<>(64,.75f,true));
                // A resolver may outlive a subsequent bind. Give its lazy moss planner a
                // fixed-seed view, sharing this immutable graph and its bounded caches.
                // It never references this generator, a Level, or a chunk owner.
                java.util.function.BiFunction<Integer,Integer,blockbranch.cubic.LitCaveLayer> growth=null;
                if(caveBiomeDefinition>0){
                    var view=new RealisticGenerator(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,entryRoutesEnabled,oreDefinition,dripstoneDefinition,litCaveGrowthDefinition);
                    // The view shares canopy caches, so it must use their saved crown and transition definitions.
                    view.borealCrownDefinition=borealCrownDefinition;
                    view.biomeTransitionDefinition=biomeTransitionDefinition;view.redwoodForestDefinition=redwoodForestDefinition;view.surfaceOwnershipDefinition=surfaceOwnershipDefinition;view.dryForestDefinition=dryForestDefinition;view.coolDesertDefinition=coolDesertDefinition;view.abyssalTraceDefinition=abyssalTraceDefinition;view.deepBasinReliefDefinition=deepBasinReliefDefinition;view.landIceDefinition=landIceDefinition;
                    view.gravelReliefDefinition=gravelReliefDefinition;
                    view.terrainState=current;
                    ((ScreeBiomeSource)view.biomeSource).withLandIce(landIceDefinition>0).withRedwoodForests(redwoodForestDefinition>0).withSurfaceOwnership(surfaceOwnershipDefinition>0).withDryForests(dryForestDefinition>0).withCoolDeserts(coolDesertDefinition>0).bind(current.seed(),current.drainage(),current.climate());
                    growth=(cx,cz)->view.litCaveLayer(cx,cz,random,null);
                }
                ((ScreeBiomeSource)biomeSource).bind(current.seed(),current.drainage(),current.climate(),growth);
                terrainState = current;
            }
            return current;
        }
    }

    public int caveBiomeDefinition(){return caveBiomeDefinition;}
    public int litCaveGrowthDefinition(){return litCaveGrowthDefinition;}
    public int dripstoneDefinition(){return dripstoneDefinition;}
    public int oreDefinition(){return oreDefinition;}
    public boolean entryRoutesEnabled(){return entryRoutesEnabled;}
    public boolean approachesEnabled(){return approachesEnabled;}
    CaveApproach plannedApproach(int cellX,int cellZ,RandomState random){
        var state=terrain(random);long key=(cellX&0xffffffffL)|((long)cellZ<<32);
        synchronized(state.approaches()){var found=state.approaches().get(key);if(found!=null)return found;}
        var planned=CaveApproach.plan(random.seed(),cellX,cellZ,state.drainage(),(x,z)->{
            int ground=state.drainage().sample(x,z).height();var root=canopy(x,z,random).state(x,ground+1,z);
            return root!=null&&!blockbranch.cubic.UndergroundLayer.approachMaterial(root);
        });
        if(entryRoutesEnabled)planned=planned.extend(state.drainage(),state.underground());
        synchronized(state.approaches()){
            state.approaches().put(key,planned);
            if(state.approaches().size()>64)state.approaches().remove(state.approaches().keySet().iterator().next());
        }
        return planned;
    }
    private CaveApproach approach(int x,int z,RandomState random){return approachesEnabled?plannedApproach(Math.floorDiv(x,CaveApproach.CELL),Math.floorDiv(z,CaveApproach.CELL),random):CaveApproach.NONE;}
    public boolean undergroundEnabled(){return undergroundEnabled;}
    private boolean cave(int x,int y,int z,int ground,RandomState r){return undergroundEnabled&&terrain(r).underground().voidAt(x,y,z,ground);}
    boolean sandyCoastsEnabled(){return sandyCoastsEnabled;}
    public boolean icebergsEnabled(){return icebergsEnabled;}
    public boolean keepsIcebergWaterOpen(int x,int z,RandomState random){if(!icebergsEnabled())return false;var f=terrain(random);return IcebergField.habitat(f.seed(),x,z,f.drainage(),f.climate());}
    public IcebergField icebergs(RandomState random){return terrain(random).icebergs();}
    private blockbranch.cubic.FloatingIceLayer floatingIce(int x,int z,RandomState random){return icebergsEnabled()?icebergs(random).column(x>>4,z>>4):blockbranch.cubic.FloatingIceLayer.EMPTY;}
    DrainageNetwork drainage(RandomState random) { return terrain(random).drainage(); }
    ClimateField.Sample climate(int x,int z,RandomState random) {
        var field=terrain(random);return field.climate().sample(x,z,field.drainage()::sample);
    }

    @FunctionalInterface interface CanopyReader {blockbranch.cubic.CanopyLayer at(int x,int z);}
    record HutPlanningTerrain(DrainageNetwork drainage,ClimateField climate,CanopyReader canopy) {}
    /** Capture a fixed-seed view before starting the helper. The callback captures
     * this TerrainState and a value flag, never a generator, RandomState or Level. */
    HutPlanningTerrain hutPlanningTerrain(RandomState random) {
        var field=terrain(random);var rockland=((ScreeBiomeSource)biomeSource).coldRocklandSurface();var dryForest=((ScreeBiomeSource)biomeSource).dryForestSurface();boolean icebergs=icebergsEnabled(),whorled=borealCrownDefinition==1,traces=abyssalTraceDefinition>0;int transitions=vegetationFlags();
        return new HutPlanningTerrain(field.drainage(),field.climate(),(x,z)->canopy(field,x,z,icebergs,whorled,transitions,rockland,dryForest,traces));
    }
    blockbranch.cubic.CanopyLayer canopy(int x,int z,RandomState random) {return canopy(terrain(random),x,z,icebergsEnabled(),borealCrownDefinition==1,vegetationFlags(),((ScreeBiomeSource)biomeSource).coldRocklandSurface(),((ScreeBiomeSource)biomeSource).dryForestSurface(),abyssalTraceDefinition>0);}
    private static blockbranch.cubic.CanopyLayer canopy(TerrainState field,int x,int z,boolean icebergs,boolean whorled,int transitions,java.util.function.BiPredicate<Integer,Integer> rockland,java.util.function.BiPredicate<Integer,Integer> dryForest,boolean abyssalTraces) {
        int cx=x>>4,cz=z>>4;long key=(cx&0xffffffffL)|((long)cz<<32);
        synchronized(field.canopies()) { var cached=field.canopies().get(key);if(cached!=null) return cached; }
        var packed=WoodlandTrees.column(field.seed(),cx,cz,field.drainage(),field.climate(),whorled,transitions,dryForest);
        packed=SwampCanopy.merge(packed,MangroveTrees.column(field.seed(),cx,cz,field.drainage(),field.climate()));
        packed=SwampCanopy.merge(packed,FrozenOcean.column(field.seed(),cx,cz,field.drainage(),field.climate(),icebergs));
        packed=SwampCanopy.merge(packed,FrozenRiver.column(cx,cz,field.drainage(),field.climate()));
        packed=SwampCanopy.merge(packed,TemperateOcean.column(field.seed(),cx,cz,field.drainage(),field.climate()));
        packed=SwampCanopy.merge(packed,WarmOcean.column(field.seed(),cx,cz,field.drainage(),field.climate()));
        packed=DappledForest.shrubs(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        packed=SwampCanopy.merge(packed,BambooGroves.column(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology()));
        packed=SnowyTaiga.snowColumn(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        packed=Grove.snowColumn(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        packed=OpenSnowCover.plainsColumn(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        packed=OpenSnowCover.slopesColumn(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        packed=OpenSnowCover.coldPeaksColumn(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        packed=OpenSnowCover.peaksColumn(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        packed=OpenSnowCover.beachColumn(field.seed(),cx,cz,packed,field.drainage(),field.climate(),field.groundEcology());
        if(rockland!=null)packed=OpenSnowCover.column(field.seed(),cx,cz,packed,field.drainage(),field.groundEcology(),(wx,wz)->rockland.test(wx,wz)&&ColdRockland.retainsSnow(wx,wz,field.drainage(),field.climate(),field.groundEcology()),true);
        if(abyssalTraces)packed=SwampCanopy.merge(packed,AbyssalTraces.column(field.seed(),cx,cz,field.drainage(),field.climate(),icebergs));
        var created=packed.length==0 ? blockbranch.cubic.CanopyLayer.EMPTY : new blockbranch.cubic.CanopyLayer(packed);
        synchronized(field.canopies()) {
            field.canopies().put(key,created);
            if(field.canopies().size()>128) field.canopies().remove(field.canopies().keySet().iterator().next());
        }
        return created;
    }

    boolean coldOceansEnabled(){return palette.additional().containsKey("cold_ocean")&&palette.additional().containsKey("deep_cold_ocean");}
    public RealisticGenerator(HabitatPalette palette){this(palette,false);}
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled) {this(palette,icebergsEnabled,false);}
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled) {
        this(palette,icebergsEnabled,sandyCoastsEnabled,false);
    }
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled) {
        this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,false);
    }
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled) {this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,false);}
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled,boolean entryRoutesEnabled) {this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,entryRoutesEnabled,0);}
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled,boolean entryRoutesEnabled,int oreDefinition) {this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,entryRoutesEnabled,oreDefinition,0);}
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled,boolean entryRoutesEnabled,int oreDefinition,int dripstoneDefinition) {this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,entryRoutesEnabled,oreDefinition,dripstoneDefinition,0);}
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled,boolean entryRoutesEnabled,int oreDefinition,int dripstoneDefinition,int litCaveGrowthDefinition) {this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,entryRoutesEnabled,oreDefinition,dripstoneDefinition,litCaveGrowthDefinition,0);}
    public RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled,boolean entryRoutesEnabled,int oreDefinition,int dripstoneDefinition,int litCaveGrowthDefinition,int caveBiomeDefinition) {this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,entryRoutesEnabled,oreDefinition,dripstoneDefinition,litCaveGrowthDefinition,caveBiomeDefinition,caveBiomeDefinition>0?CAVE_BIOME_SAVE_VERSION:dripstoneDefinition==2?FORMATION_SAVE_VERSION:litCaveGrowthDefinition>0?MOSS_SAVE_VERSION:TerrainShape.SHAPE_VERSION);}
    private RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled,boolean entryRoutesEnabled,int oreDefinition,int dripstoneDefinition,int litCaveGrowthDefinition,int caveBiomeDefinition,int inputSchema) {this(palette,icebergsEnabled,sandyCoastsEnabled,undergroundEnabled,approachesEnabled,entryRoutesEnabled,oreDefinition,dripstoneDefinition,litCaveGrowthDefinition,caveBiomeDefinition,inputSchema,0);}
    private RealisticGenerator(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,boolean undergroundEnabled,boolean approachesEnabled,boolean entryRoutesEnabled,int oreDefinition,int dripstoneDefinition,int litCaveGrowthDefinition,int caveBiomeDefinition,int inputSchema,int structureDefinition) {
        this.structureDefinition=structureDefinition;
        this.inputSchema=inputSchema;
        if(caveBiomeDefinition<0||caveBiomeDefinition>1)throw new IllegalArgumentException("Unsupported cave biome definition");this.caveBiomeDefinition=caveBiomeDefinition;
        if(litCaveGrowthDefinition!=0&&litCaveGrowthDefinition!=2)throw new IllegalArgumentException("Unsupported lit cave definition");this.litCaveGrowthDefinition=litCaveGrowthDefinition;
        if(dripstoneDefinition<0||dripstoneDefinition>2)throw new IllegalArgumentException("Unsupported dripstone definition");this.dripstoneDefinition=dripstoneDefinition;
        if(oreDefinition<0||oreDefinition>OreLayer.DEFINITION)throw new IllegalArgumentException("Unsupported ore definition");
        this.oreDefinition=oreDefinition;
        this.entryRoutesEnabled=entryRoutesEnabled;
        super(new ScreeBiomeSource(palette,icebergsEnabled,sandyCoastsEnabled,dripstoneDefinition,caveBiomeDefinition));
        this.undergroundEnabled=undergroundEnabled;this.approachesEnabled=approachesEnabled;
        this.sandyCoastsEnabled=sandyCoastsEnabled;
        this.icebergsEnabled=icebergsEnabled;
        this.palette=palette;
        this.biome=palette.margins().fallback();
        this.screeBiome=palette.margins().scree();
        this.reedbedBiome=palette.margins().reedbed();
        this.riverMarginBiome=palette.margins().riverMargin();
        this.sandyShoreBiome=palette.margins().sandyShore();
        this.lakeBiome=palette.margins().lake();
        this.riverChannelBiome=palette.margins().riverChannel();
        this.shelfBiome=palette.margins().shelf();
        this.deepOceanBiome=palette.margins().deepOcean();
        this.meadowBiome=palette.land().meadow();
        this.woodlandBiome=palette.land().woodland();
        this.steppeBiome=palette.land().steppe();
        this.borealBiome=palette.land().boreal();
        this.warmBiome=palette.land().warm();
        this.coldSteppeBiome=palette.land().coldSteppe();
        this.desertBiome=palette.land().desert();
        this.humidBiome=palette.land().humid();
        this.tundraBiome=palette.land().tundra();
    }

    @Override public CompletableFuture<ChunkAccess> createBiomes(RandomState random, Blender blender,
            StructureManager structures, ChunkAccess chunk) {
        terrain(random); // Bind before the superclass requests a biome resolver.
        if(caveBiomeDefinition==0)return super.createBiomes(random,blender,structures,chunk);
        // Native BIOMES precedes terrain construction. Prepare the same immutable
        // profile on a generation worker, so source queries reuse its approved
        // moss plan instead of separately recomputing surface materials.
        return CompletableFuture.runAsync(()->prepareCaveProfile(chunk,random),net.minecraft.util.Util.backgroundExecutor().forName("prepareCaveBiomes"))
            .thenCompose(ignored->super.createBiomes(random,blender,structures,chunk))
            .whenComplete((result,error)->{if(error!=null)synchronized(preparedProfiles){preparedProfiles.remove(chunk);}});
    }

    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
    @Override public int getMinY() { return TerrainShape.FLOOR; }
    @Override public int getGenDepth() { return TerrainShape.CEILING - TerrainShape.FLOOR; }
    @Override public int getSeaLevel() { return TerrainShape.SEA_LEVEL; }
    @Override public int getSpawnHeight(LevelHeightAccessor bounds) { return getSeaLevel(); }

    @Override public ChunkPos getOrigin(RandomState random) {
        var terrain = terrain(random);
        var spawn = terrain.shape().spawnPoint();
        // The raw lowland candidate may now contain a river or lake. Find a dry
        // chunk nearby using the final terrain, rather than spawning in water.
        for (int ring = 0; ring <= 128; ring++)
            for (int dz = -ring; dz <= ring; dz++) for (int dx = -ring; dx <= ring; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                int x = spawn.x() + dx * 64, z = spawn.z() + dz * 64;
                var sample = terrain.drainage().sample(x, z);
                if (sample.waterLevel() == DrainageNetwork.DRY && sample.height() >= TerrainShape.SEA_LEVEL + 4 && sample.height() <= TerrainShape.SEA_LEVEL + 250)
                    return new ChunkPos(x >> 4, z >> 4);
            }
        throw new IllegalStateException("No dry spawn near lowland candidate");
    }

    @Override public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structures, RandomState random, long seed) {
        // Precompute the finite atlas during generator initialization, before chunk work is admitted.
        if(structureDefinition==3)wells(random);
        var sets=new java.util.ArrayList<Holder<StructureSet>>();
        if(discoveryDefinition>0){
            var planned=treasures(random);
            var nativeSet=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.BURIED_TREASURE))).findFirst().orElseThrow();
            var holder=nativeSet.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.BURIED_TREASURE)).findFirst().orElseThrow().structure();
            sets.add(Holder.direct(new StructureSet(holder,planned.placement())));
        }
        if(shipwreckDefinition()>0){
            var planned=shipwrecks(random);
            var nativeSet=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SHIPWRECK_BEACHED))).findFirst().orElseThrow();
            var holder=nativeSet.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SHIPWRECK_BEACHED)).findFirst().orElseThrow().structure();
            sets.add(Holder.direct(new StructureSet(holder,planned.placement())));
        }
        if(waygateDefinition()>0){var gates=waygates(random);var nativeSet=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.RUINED_PORTAL_DESERT))).findFirst().orElseThrow();var holder=nativeSet.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.RUINED_PORTAL_DESERT)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,gates.placement())));}
        if(wetlandHutDefinition()>0){var huts=wetlandHuts(random);var set=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT))).findFirst().orElseThrow();var holder=set.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,huts.placement())));}
        if(snowShelterDefinition()>0){var shelters=snowShelters(random);var set=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.IGLOO))).findFirst().orElseThrow();var holder=set.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.IGLOO)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,shelters.placement())));}
        if(warmRuinDefinition()>0){var ruins=warmRuins(random);var set=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM))).findFirst().orElseThrow();var holder=set.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,ruins.placement())));}
        if(coldRuinDefinition()>0){var ruins=coldRuins(random);var set=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD))).findFirst().orElseThrow();var holder=set.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,ruins.placement())));}
        if(deepColdRuinDefinition()>0){var ruins=deepColdRuins(random);var set=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD))).findFirst().orElseThrow();var holder=set.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,ruins.placement())));}
        if(deepWarmRuinDefinition()>0){var ruins=deepWarmRuins(random);var set=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM))).findFirst().orElseThrow();var holder=set.value().structures().stream().filter(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,ruins.placement())));}
        if(reimaginedDefinition()>0){var plans=reimagined(random);for(int kind=1;kind<=5;kind++){var key=ReimaginedStructures.key(kind);var set=structures.listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(key))).findFirst().orElseThrow();var holder=set.value().structures().stream().filter(e->e.structure().is(key)).findFirst().orElseThrow().structure();sets.add(Holder.direct(new StructureSet(holder,plans.placement(kind))));}}
        if(overworldDefinition>0)OverworldStructures.addSets(structures,sets,this);
        return ChunkGeneratorStructureState.createForFlat(random, seed, getOrigin(random), biomeSource, sets.stream());
    }
    @Override public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        if(overworldDefinition>0)OverworldStructures.decorate(level,chunk,structures,this);
        var random=level.getLevel().getChunkSource().randomState();if(structureDefinition>0){if(nativeOrdinaryDefinition>0)NativeOrdinaryStructures.placeWell(wells(random),level,chunk.getPos(),this);else wells(random).place(level,chunk.getPos());}
        if(discoveryDefinition>0&&treasures(random).containsChunk(chunk.getPos().x(),chunk.getPos().z())){
            var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.BURIED_TREASURE).value();
            var start=structures.getStartForStructure(nativeStructure,chunk);
            if(start!=null&&start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());
        }
        if(shipwreckDefinition()>0&&(nativeOrdinaryDefinition>0||shipwrecks(random).touchesChunk(chunk.getPos().x(),chunk.getPos().z()))){
            var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SHIPWRECK_BEACHED).value();
            for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),nativeStructure))if(start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());
        }
        if(wetlandHutDefinition()>0&&wetlandHuts(random).forStart(chunk.getPos().x(),chunk.getPos().z()).isPresent()){
            var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT).value();
            for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),nativeStructure))if(start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());
        }
        if(snowShelterDefinition()>0&&snowShelters(random).forStart(chunk.getPos().x(),chunk.getPos().z()).isPresent()){
            var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.IGLOO).value();
            for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),nativeStructure))if(start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());
        }
        if(warmRuinDefinition()>0){var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM).value();for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),nativeStructure))if(start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());}
        if(coldRuinDefinition()>0){var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD).value();for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),nativeStructure))if(start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());}
        if(waygateDefinition()>0&&(nativeOrdinaryDefinition>0||waygates(random).touches(chunk.getPos().x(),chunk.getPos().z()))){
            var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.RUINED_PORTAL_DESERT).value();
            for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),nativeStructure))if(start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());
        }
        if(reimaginedDefinition()>0)for(int kind=1;kind<=5;kind++){var nativeStructure=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(ReimaginedStructures.key(kind)).value();for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),nativeStructure))if(start.isValid())start.placeInChunk(level,structures,this,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(random.seed()^chunk.getPos().pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR,chunk.getPos().getMinBlockZ(),chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ()),chunk.getPos());}
    }
    private record Located(net.minecraft.core.BlockPos pos,Holder<net.minecraft.world.level.levelgen.structure.Structure> holder,net.minecraft.world.level.levelgen.structure.StructureStart start,ChunkAccess chunk){}
    @Override public com.mojang.datafixers.util.Pair<net.minecraft.core.BlockPos,Holder<net.minecraft.world.level.levelgen.structure.Structure>> findNearestMapStructure(net.minecraft.server.level.ServerLevel level,net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> targets,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        boolean treasure=discoveryDefinition>0&&targets.contains(registry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.BURIED_TREASURE));
        boolean gate=waygateDefinition()>0&&targets.contains(registry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.RUINED_PORTAL_DESERT));
        boolean wreck=shipwreckDefinition()>0&&targets.contains(registry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SHIPWRECK_BEACHED));
        boolean hut=wetlandHutDefinition()>0&&targets.contains(registry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT));
        boolean snow=snowShelterDefinition()>0&&targets.contains(registry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.IGLOO));
        boolean coldRuin=coldRuinDefinition()>0&&targets.contains(registry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD));
        boolean ruin=warmRuinDefinition()>0&&targets.contains(registry.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM));
        boolean reimagined=reimaginedDefinition()>0&&java.util.stream.IntStream.rangeClosed(1,5).anyMatch(k->targets.contains(registry.getOrThrow(ReimaginedStructures.key(k))));
        if(!treasure&&!gate&&!wreck&&!hut&&!snow&&!ruin&&!coldRuin&&!reimagined&&overworldDefinition==0)return super.findNearestMapStructure(level,targets,origin,radius,skipKnown);
        Located best=treasure?locateTreasure(level,targets,origin,radius,skipKnown):null;
        Located other=gate?locateWaygate(level,targets,origin,radius,skipKnown):null;
        if(other!=null&&(best==null||other.pos.distSqr(origin)<best.pos.distSqr(origin)))best=other;
        Located ship=wreck?locateShipwreck(level,origin,radius,skipKnown):null;
        if(ship!=null&&(best==null||ship.pos.distSqr(origin)<best.pos.distSqr(origin)))best=ship;
        Located marsh=hut?locateWetlandHut(level,origin,radius,skipKnown):null;
        if(marsh!=null&&(best==null||marsh.pos.distSqr(origin)<best.pos.distSqr(origin)))best=marsh;
        Located shelter=snow?locateSnowShelter(level,origin,radius,skipKnown):null;
        if(shelter!=null&&(best==null||shelter.pos.distSqr(origin)<best.pos.distSqr(origin)))best=shelter;
        Located sea=ruin?locateWarmRuin(level,origin,radius,skipKnown):null;
        if(sea!=null&&(best==null||sea.pos.distSqr(origin)<best.pos.distSqr(origin)))best=sea;
        Located cold=coldRuin?locateColdRuin(level,origin,radius,skipKnown):null;
        if(cold!=null&&(best==null||cold.pos.distSqr(origin)<best.pos.distSqr(origin)))best=cold;
        if(reimagined)for(int kind=1;kind<=5;kind++)if(targets.contains(registry.getOrThrow(ReimaginedStructures.key(kind)))){var candidate=locateReimagined(level,origin,radius,skipKnown,kind);if(candidate!=null&&(best==null||candidate.pos.distSqr(origin)<best.pos.distSqr(origin)))best=candidate;}
        if(overworldDefinition>0){
            var nativeSite=OverworldStructures.locate(level,targets,origin,radius,skipKnown);
            if(nativeSite!=null&&(best==null||nativeSite.pos().distSqr(origin)<best.pos.distSqr(origin)))best=new Located(nativeSite.pos(),nativeSite.holder(),nativeSite.start(),nativeSite.chunk());
        }
        if(best==null)return null;
        if(skipKnown){level.structureManager().addReference(best.start);best.chunk.markUnsaved();}
        return com.mojang.datafixers.util.Pair.of(best.pos,best.holder);
    }
    private Located locateReimagined(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown,int kind){
        if(!level.structureManager().shouldGenerateStructures())return null;
        var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(ReimaginedStructures.key(kind));double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);Located best=null;int owner=-1;
        for(var region:reimagined(level.getChunkSource().randomState()).ordered(kind,origin,nearest)){if(region.lower(origin)>nearest)break;var candidate=region.plan();if(candidate.isEmpty())continue;var p=candidate.get();double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearest||distance==nearest&&p.region()<owner)continue;var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;owner=p.region();best=new Located(p.center(),holder,start,chunk);}}
        return best;
    }
    private Located locateTreasure(net.minecraft.server.level.ServerLevel level,net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> targets,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(discoveryDefinition==0)return null;
        var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.BURIED_TREASURE);
        if(!targets.contains(holder))return null;
        if(!level.structureManager().shouldGenerateStructures())return null;
        long range=Math.min(6000000L,Math.max(0L,radius)*2048L);
        var planner=treasures(level.getChunkSource().randomState());
        double nearestDistance=range;net.minecraft.world.level.levelgen.structure.StructureStart bestStart=null;ChunkAccess bestChunk=null;net.minecraft.core.BlockPos best=null;
        for(var group:planner.orderedGroups(origin,range)){
            if(group.lowerDistance(origin)>nearestDistance)break;var candidate=group.site();if(candidate.isEmpty())continue;var p=candidate.get();double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearestDistance)continue;
            var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);
            if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearestDistance=distance;bestStart=start;bestChunk=chunk;best=new net.minecraft.core.BlockPos(p.x(),64,p.z());}
        }
        if(best!=null)return new Located(best,holder,bestStart,bestChunk);
        return null;
    }
    private Located locateWarmRuin(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(deepWarmRuinDefinition()>0)return locateCombinedWarmRuin(level,origin,radius,skipKnown);
        if(!level.structureManager().shouldGenerateStructures())return null;var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM);double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);Located best=null;int owner=-1;
        for(var region:warmRuins(level.getChunkSource().randomState()).ordered(origin,nearest)){if(region.lower(origin)>nearest)break;var candidate=region.plan();if(candidate.isEmpty())continue;var p=candidate.get();double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearest||distance==nearest&&p.region()<owner)continue;var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;owner=p.region();best=new Located(p.center(),holder,start,chunk);}}
        return best;
    }
    private Located locateColdRuin(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(deepColdRuinDefinition()>0)return locateCombinedColdRuin(level,origin,radius,skipKnown);
        if(!level.structureManager().shouldGenerateStructures())return null;var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD);double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);Located best=null;int owner=-1;
        for(var region:coldRuins(level.getChunkSource().randomState()).ordered(origin,nearest)){if(region.lower(origin)>nearest)break;var candidate=region.plan();if(candidate.isEmpty())continue;var p=candidate.get();double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearest||distance==nearest&&p.region()<owner)continue;var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;owner=p.region();best=new Located(p.center(),holder,start,chunk);}}
        return best;
    }
    private record ColdRegion(double lower,int owner,java.util.function.Supplier<java.util.Optional<ColdOceanRuins.Plan>> plan){}
    private Located locateCombinedColdRuin(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(!level.structureManager().shouldGenerateStructures())return null;
        var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_COLD);
        double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);var regions=new java.util.ArrayList<ColdRegion>();
        for(var r:coldRuins(level.getChunkSource().randomState()).ordered(origin,nearest))regions.add(new ColdRegion(r.lower(origin),r.owner(),r::plan));
        for(var r:deepColdRuins(level.getChunkSource().randomState()).ordered(origin,nearest))regions.add(new ColdRegion(r.lower(origin),r.owner(),()->r.plan().map(p->new ColdOceanRuins.Plan(p.x(),p.y(),p.z(),p.variant(),p.region()))));
        regions.sort(java.util.Comparator.comparingDouble(ColdRegion::lower).thenComparingInt(ColdRegion::owner));Located best=null;int owner=-1;
        for(var r:regions){if(r.lower()>nearest)break;var candidate=r.plan().get();if(candidate.isEmpty())continue;var p=candidate.get();double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearest||distance==nearest&&p.region()<owner)continue;var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;owner=p.region();best=new Located(p.center(),holder,start,chunk);}}
        return best;
    }
    private record WarmRegion(double lower,int owner,java.util.function.Supplier<java.util.Optional<WarmOceanRuins.Plan>> plan){}
    private Located locateCombinedWarmRuin(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(!level.structureManager().shouldGenerateStructures())return null;
        var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.OCEAN_RUIN_WARM);
        double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);var regions=new java.util.ArrayList<WarmRegion>();
        for(var r:warmRuins(level.getChunkSource().randomState()).ordered(origin,nearest))regions.add(new WarmRegion(r.lower(origin),r.owner(),r::plan));
        for(var r:deepWarmRuins(level.getChunkSource().randomState()).ordered(origin,nearest))regions.add(new WarmRegion(r.lower(origin),r.owner(),()->r.plan().map(p->new WarmOceanRuins.Plan(p.x(),p.y(),p.z(),p.variant(),p.region()))));
        regions.sort(java.util.Comparator.comparingDouble(WarmRegion::lower).thenComparingInt(WarmRegion::owner));Located best=null;int owner=-1;
        for(var r:regions){if(r.lower()>nearest)break;var candidate=r.plan().get();if(candidate.isEmpty())continue;var p=candidate.get();double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearest||distance==nearest&&p.region()<owner)continue;var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;owner=p.region();best=new Located(p.center(),holder,start,chunk);}}
        return best;
    }
    private Located locateSnowShelter(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(!level.structureManager().shouldGenerateStructures())return null;var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.IGLOO);double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);Located best=null;int owner=-1;
        for(var region:snowShelters(level.getChunkSource().randomState()).ordered(origin,nearest)){if(region.lower(origin)>nearest)break;var candidate=region.plan();if(candidate.isEmpty())continue;var p=candidate.get();double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearest||distance==nearest&&p.region()<owner)continue;var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;owner=p.region();best=new Located(p.center(),holder,start,chunk);}}
        return best;
    }
    private Located locateWetlandHut(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(!level.structureManager().shouldGenerateStructures())return null;
        var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT);
        double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);Located best=null;
        int bestLake=-1;
        for(var group:wetlandHuts(level.getChunkSource().randomState()).orderedGroups(origin,nearest)){
            if(group.lowerDistance(origin)>nearest)break;var candidate=group.plan();if(candidate.isEmpty())continue;var p=candidate.get();
            double distance=Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ());if(distance>nearest||distance==nearest&&p.lake()<bestLake)continue;
            var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);
            if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;bestLake=p.lake();best=new Located(p.center(),holder,start,chunk);}
        }
        return best;
    }
    private Located locateShipwreck(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(!level.structureManager().shouldGenerateStructures())return null;
        var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SHIPWRECK_BEACHED);
        double nearest=Math.min(6000000L,Math.max(0L,radius)*2048L);Located best=null;
        for(var group:shipwrecks(level.getChunkSource().randomState()).orderedGroups(origin,(long)Math.ceil(nearest))){
            if(group.lowerDistance(origin)>nearest)break;var plan=group.plan();if(plan.isEmpty())continue;var p=plan.get().center();double distance=Math.hypot(p.getX()-(double)origin.getX(),p.getZ()-(double)origin.getZ());if(distance>nearest)continue;
            var chunk=level.getChunk(p.getX()>>4,p.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);
            if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){nearest=distance;best=new Located(p,holder,start,chunk);}
        }
        return best;
    }
    private Located locateWaygate(net.minecraft.server.level.ServerLevel level,net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> targets,net.minecraft.core.BlockPos origin,int radius,boolean skipKnown){
        if(waygateDefinition()==0)return null;
        var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.RUINED_PORTAL_DESERT);
        if(!targets.contains(holder))return null;
        if(!level.structureManager().shouldGenerateStructures())return null;
        double range=Math.min(6000000L,Math.max(0L,radius)*2048L);
        for(var p:waygates(level.getChunkSource().randomState()).plans().stream().filter(p->Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ())<=range).sorted(java.util.Comparator.comparingDouble(p->Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ()))).toList()){
            var chunk=level.getChunk(p.x()>>4,p.z()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);
            if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced())){return new Located(p.center(),holder,start,chunk);}
        }
        return null;
    }
    @Override public void spawnOriginalMobs(WorldGenRegion region) {}

    @Override public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState random,
            StructureManager structures, BiomeManager biomes, WorldGenRegion region, Set<Holder<Biome>> relevantBiomes) {
        // Match native terrain generation: return to the consecutive chunk
        // dispatcher before expensive work, and publish only through its future.
        return CompletableFuture.supplyAsync(() -> {
            var sparse = (SparseColumn)chunk;
            var profile = profileForBuild(chunk, random);
            sparse.blockbranch$terrain(new blockbranch.cubic.ColumnTerrain(chunk, profile, sparse.blockbranch$factory(),
                region == null ? null : blockbranch.cubic.SectionStore.of(region.getLevel()), new int[0]));
            Heightmap.primeHeightmaps(chunk, Set.of(Heightmap.Types.values()));
            return chunk;
        }, net.minecraft.util.Util.backgroundExecutor().forName("buildRealisticTerrain"));
    }

    void prepareCaveProfile(ChunkAccess chunk,RandomState random){
        if(caveBiomeDefinition==0)return;
        synchronized(preparedProfiles){var found=preparedProfiles.get(chunk);if(found!=null&&found.underground().seed()==random.seed())return;}
        // Native BIOMES also visits prerequisite columns that may never reach
        // terrain construction. Without an approach, both moss planners return
        // the same empty descriptor; no full terrain/vegetation profile is needed.
        var pos=chunk.getPos();
        if(!approach(pos.getMinBlockX(),pos.getMinBlockZ(),random).intersectsColumn(pos.x(),pos.z()))return;
        var prepared=profile(chunk,random);
        synchronized(preparedProfiles){preparedProfiles.put(chunk,prepared);while(preparedProfiles.size()>64)preparedProfiles.remove(preparedProfiles.keySet().iterator().next());}
    }
    blockbranch.cubic.TerrainProfile profileForBuild(ChunkAccess chunk,RandomState random){
        blockbranch.cubic.TerrainProfile prepared;
        synchronized(preparedProfiles){prepared=preparedProfiles.remove(chunk);}
        return prepared!=null&&prepared.underground().seed()==random.seed()?prepared:profile(chunk,random);
    }

    /** The terrain definition is small; no underground block sections are generated here. */
    public blockbranch.cubic.TerrainProfile profile(ChunkAccess chunk, RandomState random) {
        var drainage = drainage(random);
        var canopyLayer=canopy(chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ(),random);
        int[] heights = new int[256], water = new int[256];
        byte[] scree = new byte[256],reedbed=new byte[256],riverMargin=new byte[256],sandyShore=new byte[256],meadow=new byte[256],woodland=new byte[256],steppe=new byte[256],boreal=new byte[256],warm=new byte[256],coldSteppe=new byte[256],desert=new byte[256];
        boolean[] habitats = new boolean[16],wetlands=new boolean[16],rivers=new boolean[16],shores=new boolean[16],meadows=new boolean[16],woods=new boolean[16],steppes=new boolean[16],boreals=new boolean[16],warms=new boolean[16],coldSteppes=new boolean[16],deserts=new boolean[16],humids=new boolean[16],tundras=new boolean[16],lakes=new boolean[16],channels=new boolean[16],shelves=new boolean[16],deeps=new boolean[16],savannas=new boolean[16],jungles=new boolean[16],sandyDeserts=new boolean[16],plains=new boolean[16],sunflowers=new boolean[16],forests=new boolean[16],birchForests=new boolean[16],swamps=new boolean[16];
        var factory = ((SparseColumn)chunk).blockbranch$factory();
        var biomes = new PalettedContainer<>(biome, factory.biomeStrategy());
        for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
            boolean habitat = AlpineScree.contains(chunk.getPos().getMinBlockX() + x * 4,
                chunk.getPos().getMinBlockZ() + z * 4, drainage::sample);
            habitats[z * 4 + x] = habitat;
            if (habitat) for (int y = 0; y < 4; y++) biomes.set(x, y, z, screeBiome);
            boolean wetland=InlandLakeReedbed.contains(chunk.getPos().getMinBlockX()+x*4,
                chunk.getPos().getMinBlockZ()+z*4,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz));
            wetlands[z*4+x]=wetland;
            if(wetland) for(int y=0;y<4;y++) biomes.set(x,y,z,reedbedBiome);
            boolean lake=!habitat&&!wetland&&LakeSediment.contains(chunk.getPos().getMinBlockX()+x*4,chunk.getPos().getMinBlockZ()+z*4,drainage);lakes[z*4+x]=lake;
            if(lake)for(int y=0;y<4;y++)biomes.set(x,y,z,lakeBiome);
            boolean river=!lake&&!habitat && !wetland && GravelRiverMargin.contains(chunk.getPos().getMinBlockX()+x*4,
                chunk.getPos().getMinBlockZ()+z*4,(wx,wz)->GravelRiverMargin.site(drainage,wx,wz));
            rivers[z*4+x]=river;
            if(river) for(int y=0;y<4;y++) biomes.set(x,y,z,riverMarginBiome);
            boolean shore=!habitat && !wetland && !river && LowGradientSandyShore.contains(chunk.getPos().getMinBlockX()+x*4,
                chunk.getPos().getMinBlockZ()+z*4,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz));
            shores[z*4+x]=shore;
            if(shore) {
                int sx=chunk.getPos().getMinBlockX()+x*4,sz=chunk.getPos().getMinBlockZ()+z*4;
                var shoreBiome=CoastalBeach.contains(sx,sz,drainage,terrain(random).climate(),true) ? palette.additional("snowy_beach")
                    : CoastalBeach.contains(sx,sz,drainage,terrain(random).climate(),false) ? palette.additional("beach") : sandyShoreBiome;
                for(int y=0;y<4;y++) biomes.set(x,y,z,shoreBiome);
            }
            boolean channel=!habitat&&!wetland&&!lake&&!river&&!shore&&(RiverChannel.contains(chunk.getPos().getMinBlockX()+x*4,chunk.getPos().getMinBlockZ()+z*4,drainage)||RoutedRiver.bankContains(random.seed(),chunk.getPos().getMinBlockX()+x*4,chunk.getPos().getMinBlockZ()+z*4,drainage,terrain(random).climate()));channels[z*4+x]=channel;
            if(channel)for(int y=0;y<4;y++)biomes.set(x,y,z,RoutedRiver.contains(random.seed(),chunk.getPos().getMinBlockX()+x*4,chunk.getPos().getMinBlockZ()+z*4,drainage,terrain(random).climate())?palette.additional("river"):riverChannelBiome);
            if(channel&&FrozenRiver.contains(chunk.getPos().getMinBlockX()+x*4,chunk.getPos().getMinBlockZ()+z*4,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("frozen_river"));
            boolean shelf=!habitat&&!wetland&&!lake&&!river&&!shore&&!channel&&MarineShelf.contains(chunk.getPos().getMinBlockX()+x*4,chunk.getPos().getMinBlockZ()+z*4,drainage);shelves[z*4+x]=shelf;
            if(shelf)for(int y=0;y<4;y++)biomes.set(x,y,z,shelfBiome);
            boolean deep=!habitat&&!wetland&&!lake&&!river&&!shore&&!channel&&!shelf&&DeepOcean.contains(chunk.getPos().getMinBlockX()+x*4,chunk.getPos().getMinBlockZ()+z*4,drainage);deeps[z*4+x]=deep;
            if(deep)for(int y=0;y<4;y++)biomes.set(x,y,z,deepOceanBiome);
            int cellX=chunk.getPos().getMinBlockX()+x*4,cellZ=chunk.getPos().getMinBlockZ()+z*4;
            if(coldOceansEnabled()&&ColdOcean.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("cold_ocean"));
            if(coldOceansEnabled()&&DeepColdOcean.contains(cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("deep_cold_ocean"));
            int frozenMarine=FrozenOcean.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());if(frozenMarine>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(FrozenOcean.key(frozenMarine)));
            if(TemperateOcean.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("ocean"));
            if(LukewarmOcean.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("lukewarm_ocean"));
            int badlandKind=Badlands.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(DeepLukewarmOcean.contains(cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("deep_lukewarm_ocean"));
            if(TemperateDeepOcean.contains(cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("deep_ocean"));
            if(SnowyPlains.contains(cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("snowy_plains"));
            if(MontaneMeadow.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("meadow"));
            if(SnowySlopes.contains(cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("snowy_slopes"));
            boolean birchForest=BirchForest.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());birchForests[z*4+x]=birchForest;if(birchForest)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("birch_forest"));
            boolean forest=Forest.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());forests[z*4+x]=forest;if(forest)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("forest"));
            boolean wood=!deep&&!shelf&&!channel&&!lake&&!habitat && !wetland && !river && !shore && OakBirchWoodland.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            woods[z*4+x]=wood;
            if(wood) for(int y=0;y<4;y++) biomes.set(x,y,z,woodlandBiome);
            int temperateVariant=TemperateForestVariants.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(temperateVariant>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(TemperateForestVariants.key(temperateVariant)));
            int darkCherry=DarkCherryWoodland.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(darkCherry>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(DarkCherryWoodland.key(darkCherry)));
            boolean warmWood=!deep&&!shelf&&!channel&&!lake&&!habitat && !wetland && !river && !shore && !wood && WarmWoodland.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            warms[z*4+x]=warmWood;
            if(warmWood)for(int y=0;y<4;y++)biomes.set(x,y,z,warmBiome);
            boolean humidWood=!deep&&!shelf&&!channel&&!lake&&!habitat&&!wetland&&!river&&!shore&&!wood&&!warmWood&&HumidWoodland.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            humids[z*4+x]=humidWood;if(humidWood)for(int y=0;y<4;y++)biomes.set(x,y,z,humidBiome);
            boolean sunflower=SunflowerPlains.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());sunflowers[z*4+x]=sunflower;if(sunflower)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("sunflower_plains"));
            boolean plain=Plains.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());plains[z*4+x]=plain;if(plain)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("plains"));
            boolean meadowHabitat=!birchForest&&!forest&&!deep&&!shelf&&!channel&&!lake&&!humidWood&&!warmWood && !habitat && !wetland && !river && !shore && !wood && LowlandMeadow.retained(cellX,cellZ,drainage,terrain(random).climate());
            meadows[z*4+x]=meadowHabitat;
            if(meadowHabitat) for(int y=0;y<4;y++) biomes.set(x,y,z,meadowBiome);
            boolean dryland=!deep&&!shelf&&!channel&&!lake&&!habitat && !wetland && !river && !shore && !wood && !meadowHabitat && DrySteppe.contains(cellX,cellZ,drainage,terrain(random).climate());
            steppes[z*4+x]=dryland;
            if(dryland) for(int y=0;y<4;y++) biomes.set(x,y,z,steppeBiome);
            boolean coldWood=!deep&&!shelf&&!channel&&!lake&&!habitat && !wetland && !river && !shore && !wood && !meadowHabitat && !dryland && BorealWoodland.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            boreals[z*4+x]=coldWood;
            if(coldWood) for(int y=0;y<4;y++) biomes.set(x,y,z,borealBiome);
            boolean coldGrass=!deep&&!shelf&&!channel&&!lake&&!habitat&&!wetland&&!river&&!shore&&!wood&&!meadowHabitat&&!dryland&&!coldWood&&!warmWood&&ColdSteppe.contains(cellX,cellZ,drainage,terrain(random).climate());
            coldSteppes[z*4+x]=coldGrass;if(coldGrass)for(int y=0;y<4;y++)biomes.set(x,y,z,coldSteppeBiome);
            boolean arid=!deep&&!shelf&&!channel&&!lake&&!habitat&&!wetland&&!river&&!shore&&!wood&&!meadowHabitat&&!dryland&&!coldWood&&!warmWood&&!coldGrass&&GravelDesert.contains(cellX,cellZ,drainage,terrain(random).climate());
            deserts[z*4+x]=arid;if(arid)for(int y=0;y<4;y++)biomes.set(x,y,z,desertBiome);
            boolean tundra=!deep&&!shelf&&!channel&&!lake&&!habitat&&!wetland&&!river&&!shore&&!wood&&!warmWood&&!humidWood&&!meadowHabitat&&!dryland&&!coldWood&&!coldGrass&&!arid&&ColdTundra.retained(cellX,cellZ,drainage,terrain(random).climate());
            tundras[z*4+x]=tundra;if(tundra)for(int y=0;y<4;y++)biomes.set(x,y,z,tundraBiome);
            boolean savanna=palette.additional().containsKey("savanna")&&Savanna.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());savannas[z*4+x]=savanna;
            if(savanna)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("savanna"));
            boolean jungle=Jungle.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());jungles[z*4+x]=jungle;
            if(jungle)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("jungle"));
            int peak=MountainPeaks.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(peak>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(MountainPeaks.key(peak)));
            int savannaUpland=SavannaUplands.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(savannaUpland>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(SavannaUplands.key(savannaUpland)));
            int windswept=WindsweptUplands.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(windswept>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(WindsweptUplands.key(windswept)));
            int tropical=TropicalForestVariants.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(tropical>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(TropicalForestVariants.key(tropical)));
            boolean sandy=SandyDesert.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());sandyDeserts[z*4+x]=sandy;if(sandy)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("desert"));
            if(Taiga.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("taiga"));
            int oldGrowth=OldGrowthTaiga.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(oldGrowth>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(oldGrowth==OldGrowthTaiga.PINE?"old_growth_pine_taiga":"old_growth_spruce_taiga"));
            if(SnowyTaiga.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("snowy_taiga"));
            if(Grove.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate(),terrain(random).groundEcology()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("grove"));
            boolean swamp=Swamp.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate());swamps[z*4+x]=swamp;
            if(MangroveSwamp.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("mangrove_swamp"));
            if(swamp){for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("swamp"));int i=z*4+x;meadows[i]=woods[i]=forests[i]=birchForests[i]=warms[i]=humids[i]=steppes[i]=boreals[i]=coldSteppes[i]=deserts[i]=savannas[i]=jungles[i]=sandyDeserts[i]=plains[i]=sunflowers[i]=false;}
            if(badlandKind>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(Badlands.key(badlandKind)));
            if(icebergsEnabled()&&IcebergField.habitat(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("ice_spikes"));
            if(WarmOcean.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("warm_ocean"));
            if(DappledForest.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("dappled_forest"));
            if(redwoodForestDefinition>0&&wood&&RedwoodForest.contains(random.seed(),cellX,cellZ,drainage,terrain(random).climate()))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(RedwoodForest.KEY));
            if(StonyShore.contains(cellX,cellZ,drainage))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional("stony_shore"));
            int coldKind=ColdUplands.kind(random.seed(),cellX,cellZ,drainage,terrain(random).climate());
            if(coldKind>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(ColdUplands.key(coldKind)));
            if(coldRocklandSurface(cellX,cellZ,random))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(ColdRockland.KEY));
            if(coolDesertSurface(cellX,cellZ,random))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(CoolDesert.KEY));
            if(dryForestSurface(cellX,cellZ,random))for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(DryForest.KEY));
            if(drainage.landIceThickness(cellX,cellZ)>0)for(int y=0;y<4;y++)biomes.set(x,y,z,palette.additional(LandIceField.KEY));
        }
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int wx = chunk.getPos().getMinBlockX() + x, wz = chunk.getPos().getMinBlockZ() + z;
            var sample = drainage.sample(wx, wz);
            heights[z * 16 + x] = sample.height(); water[z * 16 + x] = sample.waterLevel();
            // Material/ground-cover palettes use physical block eligibility. The
            // quart palette and canopy above remain the biome/vegetation contract.
            int i=z*16+x;
            int iceDepth=drainage.landIceThickness(wx,wz);if(iceDepth>0){coldSteppe[i]=(byte)LandIceField.code(iceDepth);continue;}
            if(coldRocklandSurface(wx,wz,random)){scree[i]=(byte)ColdRockland.debrisDepth(random.seed(),wx,wz,drainage);continue;}
            if(coolDesertSurface(wx,wz,random)){desert[i]=(byte)coolDesertCover(wx,wz,random);continue;}
            if(dryForestSurface(wx,wz,random)){woodland[i]=(byte)dryForestCover(wx,wz,random);continue;}
            if(StonyShore.surfaceContains(wx,wz,drainage)){sandyShore[i]=(byte)StonyShore.cover(random.seed(),wx,wz,drainage);continue;}
            int coldUpland=coldUplandCover(wx,wz,random);
            if(coldUpland>0){coldSteppe[i]=(byte)coldUpland;continue;}
            int badlands=badlandsCover(wx,wz,random);
            if(badlands>0){desert[i]=(byte)badlands;continue;}
            reedbed[i]=(byte)reedbedCover(wx,wz,random);
            riverMargin[i]=(byte)riverMarginCover(wx,wz,random);
            sandyShore[i]=(byte)sandyShoreCover(wx,wz,random);
            meadow[i]=(byte)meadowCover(wx,wz,random);
            woodland[i]=(byte)woodlandCover(wx,wz,random);
            steppe[i]=(byte)steppeCover(wx,wz,random);
            boreal[i]=(byte)borealCover(wx,wz,random);
            warm[i]=(byte)warmCover(wx,wz,random);
            coldSteppe[i]=(byte)coldSteppeCover(wx,wz,random);
            desert[i]=(byte)desertCover(wx,wz,random);
            scree[i]=(byte)screeDepth(wx,wz,random);
        }
        var profile=new blockbranch.cubic.TerrainProfile(TerrainShape.FLOOR, TerrainShape.CEILING, heights, water,
            scree, reedbed, riverMargin, sandyShore, meadow, woodland,steppe,boreal,warm,coldSteppe,desert,canopyLayer,floatingIce(chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ(),random), biomes, factory.blockStatesStrategy());
        var caves=undergroundEnabled?new blockbranch.cubic.UndergroundLayer(random.seed(),chunk.getPos().x(),chunk.getPos().z(),approach(chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ(),random)):blockbranch.cubic.UndergroundLayer.NONE;
        int emerald=0,gold=0;
        if(oreDefinition>0)for(int z=0;z<4;z++)for(int x=0;x<4;x++){
            int region=oreRegion(profile.biomes().get(x,0,z));
            if((region&1)!=0)emerald|=1<<(z*4+x);if((region&2)!=0)gold|=1<<(z*4+x);
        }
        var layered=!undergroundEnabled&&oreDefinition==0?profile:profile.withLayers(caves,oreDefinition>0?new OreLayer(random.seed(),chunk.getPos().x(),chunk.getPos().z(),oreDefinition,emerald,gold):OreLayer.NONE);
        var decorated=dripstoneDefinition==0?layered:layered.withDripstone(dripstoneLayer(chunk.getPos().x(),chunk.getPos().z(),random,layered));
        var grown=litCaveGrowthDefinition==0?decorated:decorated.withLitCaves(litCaveLayer(chunk.getPos().x(),chunk.getPos().z(),random,decorated));
        if(caveBiomeDefinition==0)return grown;
        var caveBiomes=new blockbranch.cubic.CaveBiomeLayer(random.seed(),chunk.getPos().x(),chunk.getPos().z(),palette.additional("lush_caves"),palette.additional("sulfur_caves"),grown.litCaves());
        if(overworldDefinition>0)caveBiomes=caveBiomes.withDeepDark(palette.additional("deep_dark"),OverworldStructures.deepDarkMask(random.seed(),chunk.getPos().x(),chunk.getPos().z(),drainage(random)));
        return grown.withCaveBiomes(caveBiomes);
    }

    private blockbranch.cubic.DripstoneLayer dripstoneLayer(int cx,int cz,RandomState random,blockbranch.cubic.TerrainProfile profile){
        if(dripstoneDefinition==0)return blockbranch.cubic.DripstoneLayer.NONE;var state=terrain(random);long key=(cx&0xffffffffL)|((long)cz<<32);
        synchronized(state.dripstone()){var cached=state.dripstone().get(key);if(cached!=null)return cached;}
        var result=profile==null?independentDripstonePlan(cx,cz,random):blockbranch.cubic.DripstoneLayer.plan(profile,random.seed(),cx,cz,palette.additional("dripstone_caves"),dripstoneDefinition);
        synchronized(state.dripstone()){var previous=state.dripstone().get(key);if(previous!=null)return previous;state.dripstone().put(key,result);if(state.dripstone().size()>64)state.dripstone().remove(state.dripstone().keySet().iterator().next());}return result;
    }
    blockbranch.cubic.DripstoneLayer independentDripstonePlan(int cx,int cz,RandomState random){
        int[] ground=new int[256];for(int z=0;z<16;z++)for(int x=0;x<16;x++)ground[z*16+x]=drainage(random).sample(cx*16+x,cz*16+z).height();
        int emerald=0,gold=0;for(int z=0;z<4;z++)for(int x=0;x<4;x++){int flags=oreRegion(((ScreeBiomeSource)biomeSource).surfaceResolver().getNoiseBiome(cx*4+x,0,cz*4+z));if((flags&1)!=0)emerald|=1<<(z*4+x);if((flags&2)!=0)gold|=1<<(z*4+x);}
        var ores=oreDefinition==0?OreLayer.NONE:new OreLayer(random.seed(),cx,cz,oreDefinition,emerald,gold).bind(java.util.Arrays.stream(ground).min().orElseThrow(),java.util.Arrays.stream(ground).max().orElseThrow());
        var under=new blockbranch.cubic.UndergroundLayer(random.seed(),cx,cz,approach(cx*16,cz*16,random));
        return blockbranch.cubic.DripstoneLayer.plan(new blockbranch.cubic.DripstoneLayer.Sampler(){
            public int groundHeight(int x,int z){return ground[(z&15)*16+(x&15)];}
            public boolean protectedRoute(){return under.hasApproach();}
            public BlockState state(int x,int y,int z){int h=groundHeight(x,z);var value=blockbranch.cubic.TerrainProfile.material(TerrainShape.FLOOR,TerrainShape.CEILING,y,h,Integer.MIN_VALUE,0);if(under.approachClears(x,y,z,h)&&blockbranch.cubic.UndergroundLayer.approachMaterial(value)||(value.is(Blocks.STONE)||value.is(Blocks.DEEPSLATE))&&under.voidAt(x,y,z,h))return Blocks.AIR.defaultBlockState();return ores.state(x,y,z,h,value,true);}
        },random.seed(),cx,cz,palette.additional("dripstone_caves"),dripstoneDefinition);
    }
    private blockbranch.cubic.LitCaveLayer litCaveLayer(int cx,int cz,RandomState random,blockbranch.cubic.TerrainProfile profile){
        var state=terrain(random);long key=(cx&0xffffffffL)|((long)cz<<32);synchronized(state.litCaves()){var found=state.litCaves().get(key);if(found!=null)return found;}
        var planned=profile==null?independentLitCavePlan(cx,cz,random):blockbranch.cubic.LitCaveLayer.plan(profile,random.seed(),cx,cz,(wx,wz)->GroundEcology.borealMossSite(state.groundEcology().sample(wx,wz)));
        synchronized(state.litCaves()){var old=state.litCaves().get(key);if(old!=null)return old;state.litCaves().put(key,planned);if(state.litCaves().size()>64)state.litCaves().remove(state.litCaves().keySet().iterator().next());}return planned;
    }
    blockbranch.cubic.LitCaveLayer independentLitCavePlan(int cx,int cz,RandomState random){
        var entry=approach(cx*16,cz*16,random);if(!entry.intersectsColumn(cx,cz))return new blockbranch.cubic.LitCaveLayer(random.seed(),cx,cz,new int[0]);
        int emerald=0,gold=0;for(int z=0;z<4;z++)for(int x=0;x<4;x++){int flags=oreRegion(((ScreeBiomeSource)biomeSource).surfaceResolver().getNoiseBiome(cx*4+x,0,cz*4+z));if((flags&1)!=0)emerald|=1<<(z*4+x);if((flags&2)!=0)gold|=1<<(z*4+x);}
        var ores=oreDefinition==0?OreLayer.NONE:new OreLayer(random.seed(),cx,cz,oreDefinition,emerald,gold);
        var ecology=terrain(random).groundEcology();
        return blockbranch.cubic.LitCaveLayer.plan(new blockbranch.cubic.LitCaveLayer.Sampler(){
            public boolean hasApproach(){return true;}
            public boolean suitable(int x,int z){return GroundEcology.borealMossSite(ecology.sample(cx*16+x,cz*16+z));}
            public int ground(int x,int z){return drainage(random).sample(cx*16+x,cz*16+z).height();}
            public int top(int x,int z){int wx=cx*16+x,wz=cz*16+z,low=ground(x,z)-blockbranch.cubic.LitCaveLayer.MAX_DEPTH;int top=uncarvedBaseHeight(wx,wz,Heightmap.Types.WORLD_SURFACE,blockbranch.cubic.ColumnLimits.BUILD,random);for(int y=top-1;y>=low;y--)if(!surfaceState(wx,y,wz,random).isAir())return y+1;return low;}
            public BlockState state(int x,int y,int z){return ores.state(x,y,z,ground(x,z),surfaceState(cx*16+x,y,cz*16+z,random),undergroundEnabled);}
            public boolean approachAt(int x,int y,int z){return entry.contains(cx*16+x,y,cz*16+z);}
        },random.seed(),cx,cz);
    }

    private int oreRegion(Holder<Biome> biome){return oreRegions.computeIfAbsent(biome,holder->{int mask=0;for(var group:holder.value().getGenerationSettings().features())for(var placed:group){var key=placed.unwrapKey();if(key.isEmpty())continue;String path=key.get().identifier().getPath();if(path.equals("ore_emerald"))mask|=1;if(path.equals("ore_gold_extra"))mask|=2;}return mask;});}

    private boolean coolDesertSurface(int x,int z,RandomState random){if(coolDesertDefinition==0)return false;terrain(random);return ((ScreeBiomeSource)biomeSource).coolDesertSurface().test(x,z);}
    private int coolDesertCover(int x,int z,RandomState random){return CoolDesert.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z));}
    java.util.function.BiPredicate<Integer,Integer> dryForestOwner(RandomState random){terrain(random);return ((ScreeBiomeSource)biomeSource).dryForestSurface();}
    private boolean dryForestSurface(int x,int z,RandomState random){
        if(dryForestDefinition==0)return false;terrain(random);return ((ScreeBiomeSource)biomeSource).dryForestSurface().test(x,z);
    }
    private int dryForestCover(int x,int z,RandomState random){
        var f=terrain(random);var trees=canopy(x,z,random);int h=f.drainage().sample(x,z).height();
        return DryForest.cover(random.seed(),x,z,f.groundEcology().sample(x,z).shade(trees.topY(x,z)>h+2),trees.state(x,h+1,z)==null);
    }
    private boolean coldRocklandSurface(int x,int z,RandomState random) {
        if(surfaceOwnershipDefinition==0)return false;
        terrain(random);return ((ScreeBiomeSource)biomeSource).coldRocklandSurface().test(x,z);
    }

    int screeDepth(int x, int z, RandomState random) {
        if(coldRocklandSurface(x,z,random))return ColdRockland.debrisDepth(random.seed(),x,z,drainage(random));
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var drainage = drainage(random);
        var sample = drainage.sample(x, z);
        int depth=AlpineScree.depth(random.seed(), x, z,
            sample.waterLevel() <= sample.height() && AlpineScree.surfaceContains(x, z, drainage::sample));
        // Only newly admitted boundary blocks can overlap an existing specialist
        // soil. Match the stored profile's precedence at every subsurface depth.
        if(depth>0
            &&(reedbedCover(x,z,random)>0||riverMarginCover(x,z,random)>0||sandyShoreCover(x,z,random)>0
            ||meadowCover(x,z,random)>0||woodlandCover(x,z,random)>0||steppeCover(x,z,random)>0
            ||borealCover(x,z,random)>0||warmCover(x,z,random)>0||coldSteppeCover(x,z,random)>0||desertCover(x,z,random)>0))return 0;
        return depth;
    }

    int reedbedCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        var drainage=drainage(random);
        return InlandLakeReedbed.cover(random.seed(),x,z,InlandLakeReedbed.surfaceContains(x,z,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz)),InlandLakeReedbed.site(drainage,x,z));
    }
    int riverMarginCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return MangroveSwamp.bed(random.seed(),x,z,drainage(random));
        // Preserve the swamp-soil override from profile assembly in every query path.
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var drainage=drainage(random);
        // The quart kelp mask may cross a physical sandy-shore edge. Definition
        // 1 gives that block one cover owner; do not encode both sorted marine
        // sediment and shore sand. Keep historical definitions' saved routing.
        if(surfaceOwnershipDefinition>0&&sandyShoreCover(x,z,random)>0)return 0;
        if(TemperateOcean.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate())||TemperateOcean.kelpHeight(random.seed(),x,z,drainage,terrain(random).climate())>0)return TemperateOcean.cover(random.seed(),x,z,drainage,terrain(random).climate());
        if(LukewarmOcean.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate()))return LukewarmOcean.cover(random.seed(),x,z,drainage,terrain(random).climate());
        if(DeepOcean.surfaceContains(x,z,drainage)){
            int frozen=FrozenSeabed.frozenCover(random.seed(),x,z,drainage,terrain(random).climate(),icebergsEnabled());
            return FrozenSeabed.replacement(DeepOcean.cover(random.seed(),x,z,true,DeepOcean.site(drainage,x,z)),frozen);
        }
        if(MarineShelf.surfaceContains(x,z,drainage)){
            int frozen=FrozenSeabed.frozenCover(random.seed(),x,z,drainage,terrain(random).climate(),icebergsEnabled());
            return FrozenSeabed.replacement(MarineShelf.cover(random.seed(),x,z,true,MarineShelf.site(drainage,x,z),terrain(random).climate().sample(x,z,drainage::sample)),frozen);
        }
        if(RiverChannel.surfaceContains(x,z,drainage)||RoutedRiver.surfaceBankContains(random.seed(),x,z,drainage,terrain(random).climate()))return RoutedRiver.cover(random.seed(),x,z,drainage,terrain(random).climate());
        if(LakeSediment.surfaceContains(x,z,drainage))return LakeSediment.cover(random.seed(),x,z,true,LakeSediment.site(drainage,x,z),terrain(random).climate().sample(x,z,drainage::sample));
        if(AlpineScree.surfaceContains(x,z,drainage::sample) || InlandLakeReedbed.surfaceContains(x,z,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz))) return 0;
        return GravelRiverMargin.cover(random.seed(),x,z,GravelRiverMargin.surfaceContains(x,z,(wx,wz)->GravelRiverMargin.site(drainage,wx,wz)),GravelRiverMargin.site(drainage,x,z));
    }
    int sandyShoreCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return StonyShore.cover(random.seed(),x,z,drainage(random));
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var drainage=drainage(random);var site=LowGradientSandyShore.site(drainage,x,z);
        if(!LowGradientSandyShore.eligible(site) || AlpineScree.surfaceContains(x,z,drainage::sample)
            || InlandLakeReedbed.surfaceContains(x,z,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz))
            || GravelRiverMargin.surfaceContains(x,z,(wx,wz)->GravelRiverMargin.site(drainage,wx,wz))) return 0;
        if(CoastalBeach.surfaceContains(x,z,drainage,terrain(random).climate(),true)) return 1;
        int cover=LowGradientSandyShore.cover(random.seed(),x,z,LowGradientSandyShore.surfaceContains(x,z,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz)),site);
        return cover==2 && CoastalBeach.surfaceContains(x,z,drainage,terrain(random).climate(),false)
            ? CoastalBeach.pioneerCover(random.seed(),x,z,site,cover) : cover;
    }

    int meadowCover(int x,int z,RandomState random) {
        var field=terrain(random);
        if(biomeTransitionDefinition==1&&ForestGrasslandTransition.active(random.seed(),x,z,field.drainage(),field.climate())){
            boolean forest=ForestGrasslandTransition.forestFloor(random.seed(),x,z);
            if(forest)return 0;
            var sample=field.drainage().sample(x,z);var trees=canopy(x,z,random);
            var site=field.groundEcology().sample(x,z).shade(trees.topY(x,z)>sample.height()+2);
            boolean clear=trees.state(x,sample.height()+1,z)==null;
            int original=originalMeadowCover(x,z,random);
            return original>0?original:LowlandMeadow.cover(random.seed(),x,z,true,LowlandMeadow.surfaceSite(field.drainage(),x,z),site,clear);
        }
        return originalMeadowCover(x,z,random);
    }
    private int originalMeadowCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(TropicalForestVariants.surfaceKind(random.seed(),x,z,drainage(random),terrain(random).climate())>0)return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var drainage=drainage(random);var site=LowlandMeadow.surfaceSite(drainage,x,z);
        // Every grass/flower refinement needs actual soil support, including at a quart-cell edge.
        if(!LowlandMeadow.surfacePotential(x,z,drainage,terrain(random).climate()))return 0;
        if(!LowlandMeadow.eligible(site) || HumidWoodland.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate()) || WarmWoodland.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate()) || !OakBirchWoodland.meadowClimateSuitable(climate(x,z,random)) || OakBirchWoodland.surfacePotential(random.seed(),x,z,drainage,terrain(random).climate()) || AlpineScree.surfaceContains(x,z,drainage::sample)
            || InlandLakeReedbed.surfaceContains(x,z,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz))
            || GravelRiverMargin.surfaceContains(x,z,(wx,wz)->GravelRiverMargin.site(drainage,wx,wz))
            || LowGradientSandyShore.surfaceContains(x,z,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz))) return 0;
        if(MontaneMeadow.contains(random.seed(),x,z,drainage,terrain(random).climate()))return MontaneMeadow.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>site.height()+2),canopy(x,z,random).state(x,site.height()+1,z)==null&&canopy(x,z,random).state(x,site.height()+2,z)==null);
        if(SunflowerPlains.contains(random.seed(),x,z,drainage,terrain(random).climate()))return SunflowerPlains.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>site.height()+2),canopy(x,z,random).state(x,site.height()+1,z)==null&&canopy(x,z,random).state(x,site.height()+2,z)==null);
        if(Plains.contains(random.seed(),x,z,drainage,terrain(random).climate()))return Plains.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>site.height()+2),canopy(x,z,random).state(x,site.height()+1,z)==null&&canopy(x,z,random).state(x,site.height()+2,z)==null);
        return LowlandMeadow.cover(random.seed(),x,z,LowlandMeadow.surfacePotential(x,z,drainage,terrain(random).climate()),site,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>site.height()+2),canopy(x,z,random).state(x,site.height()+1,z)==null);
    }

    int woodlandCover(int x,int z,RandomState random) {
        var field=terrain(random);
        if(biomeTransitionDefinition==1&&ForestGrasslandTransition.active(random.seed(),x,z,field.drainage(),field.climate())){
            boolean forest=ForestGrasslandTransition.forestFloor(random.seed(),x,z);
            if(!forest)return 0;
            var sample=field.drainage().sample(x,z);var trees=canopy(x,z,random);
            var site=field.groundEcology().sample(x,z).shade(trees.topY(x,z)>sample.height()+2);
            boolean clear=trees.state(x,sample.height()+1,z)==null;
            int original=originalWoodlandCover(x,z,random);
            return original>0?original:GroundEcology.forest(random.seed(),x,z,0,site,clear);
        }
        return originalWoodlandCover(x,z,random);
    }
    private int originalWoodlandCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return drainage(random).sample(x,z).waterLevel()==DrainageNetwork.DRY?MangroveSwamp.floor(random.seed(),x,z,drainage(random),terrain(random).groundEcology().sample(x,z)):0;
        var drainage=drainage(random);var sample=drainage.sample(x,z);
        if(sample.waterLevel()!=DrainageNetwork.DRY || drainage.channelAt(x,z).edge()>=0) return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage,terrain(random).climate()))return Swamp.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null,drainage);
        int windswept=WindsweptUplands.surfaceMaterialKind(random.seed(),x,z,drainage,terrain(random).climate());
        if(windswept>0)return WindsweptUplands.soil(random.seed(),x,z,drainage)?WindsweptUplands.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null,windswept):0;
        int tropical=TropicalForestVariants.surfaceKind(random.seed(),x,z,drainage,terrain(random).climate());
        if(tropical>0)return TropicalForestVariants.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null,tropical);
        if(Jungle.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate())){
            var site=terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2);
            int cover=Jungle.cover(random.seed(),x,z,site,canopy(x,z,random).state(x,sample.height()+1,z)==null);
            return Jungle.withLargeFern(random.seed(),x,z,site,cover,canopy(x,z,random).state(x,sample.height()+2,z)==null);
        }
        if(HumidWoodland.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate()))return HumidWoodland.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
        if(!OakBirchWoodland.surfacePotential(random.seed(),x,z,drainage,terrain(random).climate()))return 0;
        if(redwoodForestDefinition>0&&RedwoodForest.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate())){
            var site=terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2);
            int cover=RedwoodForest.cover(random.seed(),x,z,site,canopy(x,z,random).state(x,sample.height()+1,z)==null);
            return RedwoodForest.withLargeFern(random.seed(),x,z,site,cover,canopy(x,z,random).state(x,sample.height()+2,z)==null);
        }
        if(BirchForest.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate()))return BirchForest.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
        if(Forest.surfacePotential(random.seed(),x,z,drainage,terrain(random).climate()))return Forest.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
        int variant=TemperateForestVariants.surfaceKind(random.seed(),x,z,drainage,terrain(random).climate());
        if(variant>0){var site=terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2);boolean clear=canopy(x,z,random).state(x,sample.height()+1,z)==null;return variant==TemperateForestVariants.OLD_BIRCH?TemperateForestVariants.birchCover(random.seed(),x,z,site,clear):TemperateForestVariants.flowerCover(random.seed(),x,z,site,clear);}
        if(DappledForest.surfaceContains(random.seed(),x,z,drainage,terrain(random).climate()))return DappledForest.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
        int darkCherry=DarkCherryWoodland.surfaceKind(random.seed(),x,z,drainage,terrain(random).climate());
        if(darkCherry>0)return DarkCherryWoodland.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null,darkCherry);
        return OakBirchWoodland.cover(random.seed(),x,z,OakBirchWoodland.surfacePotential(random.seed(),x,z,drainage,terrain(random).climate()),true,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
    }
    int steppeCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var field=terrain(random);var sample=field.drainage().sample(x,z);
        if(sample.waterLevel()!=DrainageNetwork.DRY || field.drainage().channelAt(x,z).edge()>=0) return 0;
        return DrySteppe.cover(random.seed(),x,z,DrySteppe.surfaceContains(x,z,field.drainage(),field.climate()),true);
    }
    int borealCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(meadowCover(x,z,random)>0||woodlandCover(x,z,random)>0||steppeCover(x,z,random)>0)return 0;
        if(Grove.surfaceContains(random.seed(),x,z,drainage(random),terrain(random).climate())){var sample=drainage(random).sample(x,z);if(sample.waterLevel()!=DrainageNetwork.DRY||drainage(random).channelAt(x,z).edge()>=0)return 0;var above=canopy(x,z,random).state(x,sample.height()+1,z);return Grove.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),above==null,above!=null&&above.is(Blocks.SNOW));}
        if(SnowyPlains.surfaceContains(x,z,drainage(random),terrain(random).climate())){var s=drainage(random).sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY||drainage(random).channelAt(x,z).edge()>=0)return 0;var above=canopy(x,z,random).state(x,s.height()+1,z);return SnowyPlains.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z),terrain(random).climate().sample(x,z,drainage(random)::sample),above!=null&&above.is(Blocks.SNOW));}
        if(SnowyTaiga.surfaceContains(random.seed(),x,z,drainage(random),terrain(random).climate())){var sample=drainage(random).sample(x,z);if(sample.waterLevel()!=DrainageNetwork.DRY||drainage(random).channelAt(x,z).edge()>=0)return 0;var above=canopy(x,z,random).state(x,sample.height()+1,z);return SnowyTaiga.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),above==null,above!=null&&above.is(Blocks.SNOW));}
        if(Taiga.surfaceContains(random.seed(),x,z,drainage(random),terrain(random).climate())){var sample=drainage(random).sample(x,z);if(sample.waterLevel()==DrainageNetwork.DRY&&drainage(random).channelAt(x,z).edge()<0)return Taiga.cover(random.seed(),x,z,terrain(random).groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);return 0;}
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var field=terrain(random);var sample=field.drainage().sample(x,z);
        if(sample.waterLevel()!=DrainageNetwork.DRY || field.drainage().channelAt(x,z).edge()>=0) return 0;
        if(ColdTundra.surfaceContains(x,z,field.drainage(),field.climate()))return ColdTundra.cover(random.seed(),x,z,field.groundEcology().sample(x,z));
        if(OldGrowthTaiga.surfaceKind(random.seed(),x,z,field.drainage(),field.climate())>0)return OldGrowthFloor.cover(random.seed(),x,z,field.groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
        return BorealWoodland.cover(random.seed(),x,z,BorealWoodland.surfaceContains(random.seed(),x,z,field.drainage(),field.climate()),true,field.groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
    }
    int warmCover(int x,int z,RandomState random) {
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(TropicalForestVariants.surfaceKind(random.seed(),x,z,drainage(random),terrain(random).climate())>0)return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var field=terrain(random);var sample=field.drainage().sample(x,z);
        if(sample.waterLevel()!=DrainageNetwork.DRY || field.drainage().channelAt(x,z).edge()>=0)return 0;
        if(SavannaUplands.surfaceKind(random.seed(),x,z,field.drainage(),field.climate())>0)return SavannaUplands.soil(random.seed(),x,z,field.drainage())?SavannaUplands.cover(random.seed(),x,z,field.groundEcology().sample(x,z),canopy(x,z,random).state(x,sample.height()+1,z)==null):0;
        if(palette.additional().containsKey("savanna")&&Savanna.surfaceContains(random.seed(),x,z,field.drainage(),field.climate()))return Savanna.cover(random.seed(),x,z,field.groundEcology().sample(x,z),canopy(x,z,random).state(x,sample.height()+1,z)==null);
        return WarmWoodland.cover(random.seed(),x,z,WarmWoodland.surfaceContains(random.seed(),x,z,field.drainage(),field.climate()),true,field.groundEcology().sample(x,z).shade(canopy(x,z,random).topY(x,z)>sample.height()+2),canopy(x,z,random).state(x,sample.height()+1,z)==null);
    }
    int coldSteppeCover(int x,int z,RandomState random){
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var field=terrain(random);var sample=field.drainage().sample(x,z);if(sample.waterLevel()!=DrainageNetwork.DRY || field.drainage().channelAt(x,z).edge()>=0)return 0;
        return ColdSteppe.cover(random.seed(),x,z,ColdSteppe.surfaceContains(x,z,field.drainage(),field.climate()),true);
    }
    int coldUplandCover(int x,int z,RandomState random){
        var f=terrain(random);int ice=f.drainage().landIceThickness(x,z);if(ice>0)return LandIceField.code(ice);int kind=ColdUplands.surfaceKind(f.seed(),x,z,f.drainage(),f.climate());
        return kind==0?0:ColdUplands.cover(f.seed(),x,z,kind,f.drainage(),f.groundEcology());
    }
    int badlandsCover(int x,int z,RandomState random){
        var f=terrain(random);int kind=Badlands.surfaceKind(f.seed(),x,z,f.drainage(),f.climate());
        return kind==0?0:Badlands.cover(f.seed(),x,z,kind,f.drainage(),f.groundEcology());
    }
    int desertCover(int x,int z,RandomState random){
        if(StonyShore.surfaceContains(x,z,drainage(random)))return 0;
        if(MangroveSwamp.exact(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        if(Swamp.surfaceSoilContains(random.seed(),x,z,drainage(random),terrain(random).climate()))return 0;
        var field=terrain(random);var t=field.drainage().sample(x,z);if(t.waterLevel()!=DrainageNetwork.DRY||field.drainage().channelAt(x,z).edge()>=0)return 0;
        if(SandyDesert.surfaceContains(random.seed(),x,z,field.drainage(),field.climate()))return SandyDesert.cover(random.seed(),x,z,true,field.drainage());
        return GravelDesert.cover(random.seed(),x,z,GravelDesert.surfaceContains(x,z,field.drainage(),field.climate()),true);
    }
    private BlockState landIceBlock(int x,int y,int z,RandomState random){
        if(landIceDefinition==0)return null;var d=drainage(random);int depth=d.landIceThickness(x,z);if(depth==0)return null;int top=d.sample(x,z).height();
        return y>top||y<=top-depth?null:(y==top?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE).defaultBlockState();
    }
    BlockState surfaceState(int x,int y,int z,RandomState random) {
        var solidIce=landIceBlock(x,y,z,random);if(solidIce!=null)return solidIce;
        var value=uncarvedState(x,y,z,random);
        var entry=approach(x,z,random);
        if(entry.present()&&y>=entry.minY()&&y<=entry.maxY()&&entry.clears(x,y,z,drainage(random).sample(x,z).height())&&blockbranch.cubic.UndergroundLayer.approachMaterial(value))return Blocks.AIR.defaultBlockState();
        return (value.is(Blocks.STONE)||value.is(Blocks.DEEPSLATE))&&cave(x,y,z,drainage(random).sample(x,z).height(),random)?Blocks.AIR.defaultBlockState():value;
    }
    /**
     * Height of the top water block at a column, or {@link blockbranch.horizon.HorizonTiles#NONE}
     * when dry, from the drainage network alone; much cheaper than {@link #horizonColumn}, whose
     * ground may sit a block or two off the drainage height used here.
     */
    public int waterSurface(int x,int z,RandomState random) {
        var sample=drainage(random).sample(x,z);
        return sample.waterLevel()!=DrainageNetwork.DRY&&sample.waterLevel()-1>sample.height()?sample.waterLevel()-1:blockbranch.horizon.HorizonTiles.NONE;
    }
    public void horizonColumn(int x,int z,RandomState random,int[] out,int offset,java.util.function.ToIntFunction<Holder<Biome>> biomeIds) {horizonColumn(x,z,1,random,out,offset,biomeIds);}
    /**
     * Visual horizon column at (x,z) for a cell {@code cell} blocks wide, written as
     * {@link blockbranch.horizon.HorizonTiles#FIELDS} ints. It reads only the analytic terrain
     * (drainage, canopy, floating ice, surface cover and planned cave approaches), so it never
     * loads chunks; structures and player edits come from generated chunks instead. Plants
     * without collision and water are looked through, matching what dominates a distant view.
     * Thread-safe like chunk generation.
     */
    public void horizonColumn(int x,int z,int cell,RandomState random,int[] out,int offset,java.util.function.ToIntFunction<Holder<Biome>> biomeIds) {
        var sample=drainage(random).sample(x,z);int ground=sample.height();
        var trees=canopy(x,z,random);var ice=floatingIce(x,z,random);
        int top=Integer.MIN_VALUE;BlockState topState=null;
        int overlay=Math.max(trees.topY(x,z),ice.top(x,z));
        if(overlay!=Integer.MIN_VALUE)for(int y=overlay;y>ground+2;y--){var s=ice.state(x,y,z);if(s==null)s=trees.state(x,y,z);if(s!=null&&blockbranch.horizon.HorizonColumns.visible(s)){top=y;topState=s;break;}}
        // Follow the top run down (leaves only below leaves, so a trunk ends it) to find
        // where a canopy stops; the client draws the gap beneath it.
        int runBottom=top;
        if(topState!=null){boolean leaves=topState.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock;for(int y=top-1;y>ground;y--){var s=ice.state(x,y,z);if(s==null)s=trees.state(x,y,z);if(s==null||!(leaves?s.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock:blockbranch.horizon.HorizonColumns.visible(s)))break;runBottom=y;}}
        int groundTop=ground;BlockState groundState=null;
        for(int y=ground+2;y>=ground-2;y--){var s=uncarvedState(x,y,z,random);if(blockbranch.horizon.HorizonColumns.visible(s)){groundTop=y;groundState=s;break;}}
        if(groundState==null)groundState=Blocks.STONE.defaultBlockState();
        int opening=approachFloor(x,z,groundTop,cell,random);
        if(opening!=groundTop){groundTop=opening;var s=surfaceState(x,opening,z,random);groundState=blockbranch.horizon.HorizonColumns.visible(s)?s:Blocks.STONE.defaultBlockState();}
        if(topState==null){top=groundTop;topState=groundState;runBottom=groundTop;}
        var deep=uncarvedState(x,groundTop-3,z,random);if(!blockbranch.horizon.HorizonColumns.visible(deep))deep=Blocks.STONE.defaultBlockState();
        int water=sample.waterLevel()!=DrainageNetwork.DRY&&sample.waterLevel()-1>groundTop?sample.waterLevel()-1:Integer.MIN_VALUE;
        int biome=horizonBiome(x,Math.max(groundTop,water),z,biomeIds);
        // A trunk under the canopy that reaches the ground keeps the column solid: its wood
        // becomes the ground and deep material up to the canopy, so crowns do not float.
        // Wood that stops short of the ground (a branch) leaves the air gap.
        if(runBottom>groundTop+1&&topState.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) {
            var trunk=trees.state(x,runBottom-1,z);
            if(trunk!=null&&!(trunk.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)&&blockbranch.horizon.HorizonColumns.visible(trunk)) {
                int bottom=runBottom-1;
                while(bottom-1>ground&&trunk.equals(trees.state(x,bottom-1,z)))bottom--;
                if(bottom<=ground+1){groundTop=runBottom-1;groundState=trunk;deep=trunk;}
            }
        }
        out[offset]=top;out[offset+1]=Block.getId(topState);out[offset+2]=groundTop;out[offset+3]=Block.getId(groundState);out[offset+4]=Block.getId(deep);out[offset+5]=water;
        out[offset+6]=biome;
        out[offset+7]=runBottom;
    }
    /** Deepest a cave approach opening is followed down from the surface, in blocks. */
    private static final int OPENING_DEPTH=64;
    /**
     * Floor of a planned cave approach that opens the surface at (x,z), or {@code ground} when
     * it does not. Caves keep a solid roof, so approaches are the only openings seen from above.
     * A cell wider than one block opens only when most of it is open (three of four points a
     * quarter cell from its centre), so a narrow ramp never becomes a cell-sized pit.
     */
    private int approachFloor(int x,int z,int ground,int cell,RandomState random) {
        var entry=approach(x,z,random);
        if(!entry.present()||!entry.affectsColumn(x,z)||blockbranch.horizon.HorizonColumns.visible(surfaceState(x,ground,z,random)))return ground;
        if(cell>1){
            int d=cell>>2,open=0;
            for(int i=0;i<4;i++){int px=x+((i&1)==0?-d:d),pz=z+((i&2)==0?-d:d);
                if(approach(px,pz,random).affectsColumn(px,pz)&&!blockbranch.horizon.HorizonColumns.visible(surfaceState(px,drainage(random).sample(px,pz).height(),pz,random)))open++;}
            if(open<3)return ground;
        }
        for(int y=ground-1;y>ground-OPENING_DEPTH;y--)if(blockbranch.horizon.HorizonColumns.visible(surfaceState(x,y,z,random)))return y;
        return ground-OPENING_DEPTH;
    }
    /** Biome id a horizon column shows, from the surface resolver used by generation. */
    public int horizonBiome(int x,int y,int z,java.util.function.ToIntFunction<Holder<Biome>> biomeIds) {
        return biomeIds.applyAsInt(((ScreeBiomeSource)biomeSource).surfaceResolver().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),QuartPos.fromBlock(z)));
    }
    private BlockState uncarvedState(int x,int y,int z,RandomState random) {
        var solidIce=landIceBlock(x,y,z,random);if(solidIce!=null)return solidIce;
        var floating=floatingIce(x,z,random).state(x,y,z);if(floating!=null)return floating;
        var tree=canopy(x,z,random).state(x,y,z);if(tree!=null) return tree;
        var sample=drainage(random).sample(x,z);
        if(coldRocklandSurface(x,z,random))return material(y,sample.height(),sample.waterLevel(),screeDepth(x,z,random));
        if(coolDesertSurface(x,z,random))return material(y,sample.height(),sample.waterLevel(),0,0,0,0,0,0,0,0,0,0,coolDesertCover(x,z,random));
        if(dryForestSurface(x,z,random))return material(y,sample.height(),sample.waterLevel(),0,0,0,0,0,dryForestCover(x,z,random));
        int coldUpland=coldUplandCover(x,z,random);
        if(coldUpland>0)return material(y,sample.height(),sample.waterLevel(),0,0,0,0,0,0,0,0,0,coldUpland,0);
        int badlands=badlandsCover(x,z,random);
        if(badlands>0)return material(y,sample.height(),sample.waterLevel(),0,0,0,0,0,0,0,0,0,0,badlands);
        return material(y,sample.height(),sample.waterLevel(),screeDepth(x,z,random),reedbedCover(x,z,random),riverMarginCover(x,z,random),sandyShoreCover(x,z,random),meadowCover(x,z,random),woodlandCover(x,z,random),steppeCover(x,z,random),borealCover(x,z,random),warmCover(x,z,random),coldSteppeCover(x,z,random),desertCover(x,z,random));
    }

    static BlockState material(int y, int height, int waterLevel) {
        return material(y, height, waterLevel, 0);
    }

    static BlockState material(int y, int height, int waterLevel, int screeDepth) {
        return material(y,height,waterLevel,screeDepth,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed) {
        return material(y,height,waterLevel,screeDepth,reedbed,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin) {
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore) {
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore,int meadow) {
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,meadow,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland) {
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,meadow,woodland,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe) {
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal) {
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm) {
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm,int coldSteppe){
        return material(y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,0);
    }
    static BlockState material(int y,int height,int waterLevel,int screeDepth,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm,int coldSteppe,int desert){
        return blockbranch.cubic.TerrainProfile.material(TerrainShape.FLOOR,TerrainShape.CEILING,y,height,waterLevel,screeDepth,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert);
    }

    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor bounds,RandomState random) {
        if(overworldDefinition>0&&OverworldStructures.terrainQueries())return OverworldStructures.baseHeight(this,random,x,z,type,bounds);
        int top=uncarvedBaseHeight(x,z,type,bounds,random);
        if(!undergroundEnabled)return top;
        int min=Math.max(getMinY(),bounds.getMinY());
        var decoration=blockbranch.cubic.DripstoneLayer.NONE;int decorationRoof=dripstoneDefinition==0?Integer.MIN_VALUE:drainage(random).sample(x,z).height()-48;
        for(int y=top-1;y>=min;y--){if(y<=decorationRoof&&!decoration.enabled())decoration=dripstoneLayer(x>>4,z>>4,random,null);if(type.isOpaque().test(decoration.state(x,y,z,surfaceState(x,y,z,random))))return y+1;}
        return bounds.getMinY();
    }
    private int uncarvedBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor bounds, RandomState random) {
        int min = Math.max(getMinY(), bounds.getMinY());
        int max = Math.min(TerrainShape.CEILING - 1, bounds.getMaxY());
        var sample = drainage(random).sample(x, z);
        int height = sample.height();
        var trees=canopy(x,z,random);
        var ice=floatingIce(x,z,random);
        int overlay=Math.max(trees.topY(x,z),ice.top(x,z));
        if(overlay!=Integer.MIN_VALUE && max>height) {
            if(sample.waterLevel()>height){int fluidTop=Math.min(max,sample.waterLevel()-1);if(fluidTop>=min&&fluidTop>overlay&&type.isOpaque().test(Blocks.WATER.defaultBlockState()))return fluidTop+1;}
            for(int y=Math.min(max,overlay);y>height && y>=min;y--) {
                var state=ice.state(x,y,z);if(state==null)state=trees.state(x,y,z);
                if(state==null && (y<=height+2||sample.waterLevel()!=DrainageNetwork.DRY)) state=surfaceState(x,y,z,random);
                if(state!=null && type.isOpaque().test(state)) return y+1;
            }
            max=Math.min(max,height);
        }
        if(coldRocklandSurface(x,z,random)){int top=blockbranch.cubic.TerrainProfile.top(TerrainShape.FLOOR,TerrainShape.CEILING,height,sample.waterLevel(),screeDepth(x,z,random),0,0,0,0,0,0,0,0,0,0,type.isOpaque(),max);return top>min?top:bounds.getMinY();}
        if(coolDesertSurface(x,z,random)){int top=blockbranch.cubic.TerrainProfile.top(TerrainShape.FLOOR,TerrainShape.CEILING,height,sample.waterLevel(),0,0,0,0,0,0,0,0,0,0,coolDesertCover(x,z,random),type.isOpaque(),max);return top>min?top:bounds.getMinY();}
        if(dryForestSurface(x,z,random)){int top=blockbranch.cubic.TerrainProfile.top(TerrainShape.FLOOR,TerrainShape.CEILING,height,sample.waterLevel(),0,0,0,0,0,dryForestCover(x,z,random),0,0,0,0,0,type.isOpaque(),max);return top>min?top:bounds.getMinY();}
        int coldUpland=coldUplandCover(x,z,random);
        if(coldUpland>0){int top=blockbranch.cubic.TerrainProfile.top(TerrainShape.FLOOR,TerrainShape.CEILING,height,sample.waterLevel(),0,0,0,0,0,0,0,0,0,coldUpland,0,type.isOpaque(),max);return top>min?top:bounds.getMinY();}
        int badlands=badlandsCover(x,z,random);
        if(badlands>0){int top=blockbranch.cubic.TerrainProfile.top(TerrainShape.FLOOR,TerrainShape.CEILING,height,sample.waterLevel(),0,0,0,0,0,0,0,0,0,0,badlands,type.isOpaque(),max);return top>min?top:bounds.getMinY();}
        int top=blockbranch.cubic.TerrainProfile.top(TerrainShape.FLOOR,TerrainShape.CEILING,height,sample.waterLevel(),
            screeDepth(x,z,random),reedbedCover(x,z,random),riverMarginCover(x,z,random),sandyShoreCover(x,z,random),meadowCover(x,z,random),woodlandCover(x,z,random),steppeCover(x,z,random),borealCover(x,z,random),warmCover(x,z,random),coldSteppeCover(x,z,random),desertCover(x,z,random),type.isOpaque(),max);
        return top>min ? top : bounds.getMinY();
    }

    @Override public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor bounds, RandomState random) {
        int min = Math.max(getMinY(), bounds.getMinY());
        int end = Math.min(TerrainShape.CEILING, bounds.getMaxY() + 1);
        BlockState[] states = new BlockState[Math.max(0, end - min)];
        var sample = drainage(random).sample(x, z);
        int height = sample.height();
        int scree = screeDepth(x, z, random);
        int reedbed=reedbedCover(x,z,random);
        int riverMargin=riverMarginCover(x,z,random);
        int sandyShore=sandyShoreCover(x,z,random);
        int meadow=meadowCover(x,z,random),woodland=woodlandCover(x,z,random),steppe=steppeCover(x,z,random),boreal=borealCover(x,z,random),warm=warmCover(x,z,random),coldSteppe=coldSteppeCover(x,z,random),desert=desertCover(x,z,random);var trees=canopy(x,z,random);
        int badlands=badlandsCover(x,z,random);
        if(badlands>0){scree=reedbed=riverMargin=sandyShore=meadow=woodland=steppe=boreal=warm=coldSteppe=0;desert=badlands;}
        int coldUpland=coldUplandCover(x,z,random);
        if(coldUpland>0){scree=reedbed=riverMargin=sandyShore=meadow=woodland=steppe=boreal=warm=desert=0;coldSteppe=coldUpland;}
        if(coldRocklandSurface(x,z,random)){scree=screeDepth(x,z,random);reedbed=riverMargin=sandyShore=meadow=woodland=steppe=boreal=warm=coldSteppe=desert=0;}
        if(coolDesertSurface(x,z,random)){desert=coolDesertCover(x,z,random);scree=reedbed=riverMargin=sandyShore=meadow=woodland=steppe=boreal=warm=coldSteppe=0;}
        if(dryForestSurface(x,z,random)){woodland=dryForestCover(x,z,random);scree=reedbed=riverMargin=sandyShore=meadow=steppe=boreal=warm=coldSteppe=desert=0;}
        var floating=floatingIce(x,z,random);var entrance=approach(x,z,random);
        int region=oreDefinition==0?0:oreRegion(((ScreeBiomeSource)biomeSource).surfaceResolver().getNoiseBiome(x>>2,0,z>>2));
        var ores=oreDefinition==0?OreLayer.NONE:new OreLayer(random.seed(),x>>4,z>>4,oreDefinition,(region&1)!=0?65535:0,(region&2)!=0?65535:0);
        for(int i=0;i<states.length;i++) {
            var tree=floating.state(x,min+i,z);if(tree==null)tree=trees.state(x,min+i,z);
            states[i]=tree!=null ? tree : material(min+i,height,sample.waterLevel(),scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert);
            var solidIce=landIceBlock(x,min+i,z,random);if(solidIce!=null){states[i]=solidIce;continue;}
            if(entrance.clears(x,min+i,z,height)&&blockbranch.cubic.UndergroundLayer.approachMaterial(states[i]))states[i]=Blocks.AIR.defaultBlockState();
            if((states[i].is(Blocks.STONE)||states[i].is(Blocks.DEEPSLATE))&&cave(x,min+i,z,height,random))states[i]=Blocks.AIR.defaultBlockState();
            states[i]=ores.state(x,min+i,z,height,states[i],undergroundEnabled);
        }
        if(dripstoneDefinition>0)dripstoneLayer(x>>4,z>>4,random,null).applyColumn(x,z,min,states);
        if(litCaveGrowthDefinition>0)litCaveLayer(x>>4,z>>4,random,null).applyColumn(x,z,min,states);
        return new NoiseColumn(min, states);
    }

    @Override public void addDebugScreenInfo(List<String> lines, RandomState random, net.minecraft.core.BlockPos pos, SamplerContext context) {
        var sample = drainage(random).sample(pos.getX(), pos.getZ());
        lines.add("Realistic terrain: surface=" + sample.height() + " water=" + (sample.waterLevel() == DrainageNetwork.DRY ? "dry" : sample.waterLevel())
            + " outlet=" + (sample.closed() ? "inland lake" : "ocean") + " basin=" + sample.basin());
        lines.add("Habitat: " + (InlandLakeReedbed.contains(pos.getX(),pos.getZ(),(x,z)->InlandLakeReedbed.site(drainage(random),x,z)) ? "inland lake reedbed" : AlpineScree.contains(pos.getX(), pos.getZ(), drainage(random)::sample) ? "alpine scree slope" : GravelRiverMargin.contains(pos.getX(),pos.getZ(),(x,z)->GravelRiverMargin.site(drainage(random),x,z)) ? "gravel-bed river margin" : LowGradientSandyShore.contains(pos.getX(),pos.getZ(),(x,z)->LowGradientSandyShore.site(drainage(random),x,z)) ? "low-gradient sandy shore" : Swamp.contains(random.seed(),pos.getX(),pos.getZ(),drainage(random),terrain(random).climate()) ? "Swamp" : BirchForest.contains(random.seed(),pos.getX(),pos.getZ(),drainage(random),terrain(random).climate()) ? "Birch Forest" : Forest.contains(random.seed(),pos.getX(),pos.getZ(),drainage(random),terrain(random).climate()) ? "Forest" : OakBirchWoodland.contains(random.seed(),pos.getX(),pos.getZ(),drainage(random),terrain(random).climate()) ? "dappled oak-birch woodland" : meadowCover(pos.getX(),pos.getZ(),random)>0 ? "rolling lowland forb-grass meadow" : steppeCover(pos.getX(),pos.getZ(),random)>0 ? "dry continental scrub-steppe" : borealCover(pos.getX(),pos.getZ(),random)>0 ? "boreal spruce-birch woodland" : warmCover(pos.getX(),pos.getZ(),random)>0 ? "warm subhumid oak-pine woodland" : coldSteppeCover(pos.getX(),pos.getZ(),random)>0 ? "cold continental fescue steppe" : desertCover(pos.getX(),pos.getZ(),random)>=8 ? "Desert" : desertCover(pos.getX(),pos.getZ(),random)>0 ? "continental gravel desert" : "unassigned")
            + " debris=" + screeDepth(pos.getX(), pos.getZ(), random) + "m");
    }
}
