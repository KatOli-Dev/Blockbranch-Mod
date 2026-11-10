package blockbranch.wildlife;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Wildlife uses ordinary category caps, chunk ownership and simulation distances. */
public final class WildlifeRuntime {
    public static final EntityType<WildBoar> WILD_BOAR = Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("wild_boar"), FabricEntityType.Builder.createMob(WildBoar::new, MobCategory.CREATURE,
            builder -> builder.defaultAttributes(WildBoar::attributes).spawnPlacement(SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WildlifeHabitat::woodlandSpawn))
            .sized(.85F, .95F).eyeHeight(.7F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Blockbranch.id("wild_boar"))));
    public static final EntityType<RedDeer> RED_DEER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("red_deer"), FabricEntityType.Builder.createMob(RedDeer::new, MobCategory.CREATURE,
            builder -> builder.defaultAttributes(RedDeer::attributes).spawnPlacement(SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WildlifeHabitat::woodlandSpawn))
            .sized(.8F, 1.6F).eyeHeight(1.4F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Blockbranch.id("red_deer"))));

    public static final EntityType<AmericanBison> AMERICAN_BISON=Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("american_bison"),FabricEntityType.Builder.createMob(AmericanBison::new,MobCategory.CREATURE,
            builder->builder.defaultAttributes(AmericanBison::attributes).spawnPlacement(SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WildlifeHabitat::grasslandSpawn))
            .sized(1.3F,1.8F).eyeHeight(1.05F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Blockbranch.id("american_bison"))));

    public static final EntityType<BrownBear> BROWN_BEAR=Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("brown_bear"),FabricEntityType.Builder.createMob(BrownBear::new,MobCategory.CREATURE,
            builder->builder.defaultAttributes(BrownBear::attributes).spawnPlacement(SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WildlifeHabitat::woodlandSpawn))
            .sized(1.1F,1.35F).eyeHeight(.9F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Blockbranch.id("brown_bear"))));

    public static final EntityType<Mallard> MALLARD=Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("mallard"),FabricEntityType.Builder.createMob(Mallard::new,MobCategory.CREATURE,
            builder->builder.defaultAttributes(Mallard::attributes).spawnPlacement(SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WildlifeHabitat::wetlandSpawn))
            .sized(.45F,.55F).eyeHeight(.45F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Blockbranch.id("mallard"))));

    public static final EntityType<EurasianBeaver> EURASIAN_BEAVER=Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("eurasian_beaver"),FabricEntityType.Builder.createMob(EurasianBeaver::new,MobCategory.CREATURE,
            builder->builder.defaultAttributes(EurasianBeaver::attributes).spawnPlacement(SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WildlifeHabitat::beaverSpawn))
            .sized(.55F,.5F).eyeHeight(.36F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Blockbranch.id("eurasian_beaver"))));

    public static final EntityType<EuropeanPerch> EUROPEAN_PERCH=Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("european_perch"),FabricEntityType.Builder.createMob(EuropeanPerch::new,MobCategory.WATER_AMBIENT,
            builder->builder.defaultAttributes(EuropeanPerch::createAttributes).spawnPlacement(SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WildlifeHabitat::perchSpawn))
            .sized(.35F,.25F).eyeHeight(.15F).clientTrackingRange(4)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Blockbranch.id("european_perch"))));

    public static final EntityType<HarborSeal> HARBOR_SEAL=Registry.register(BuiltInRegistries.ENTITY_TYPE,
        Blockbranch.id("harbor_seal"),FabricEntityType.Builder.createMob(HarborSeal::new,MobCategory.CREATURE,
            builder->builder.defaultAttributes(HarborSeal::attributes).spawnPlacement(SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WildlifeHabitat::sealSpawn))
            .sized(.8F,.55F).eyeHeight(.4F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Blockbranch.id("harbor_seal"))));

    public static void initialize() {
        WildlifeSounds.initialize();
        VanillaHabitatIntegration.initialize();
        WildlifeItems.initialize();
        PerchFishing.initialize();
        // Weight 4 vs vanilla pigs' 10. The normal creature cap is shared, not multiplied.
        BiomeModifications.addSpawn(context -> {
            String id = context.getBiomeKey().identifier().toString();
            return id.equals("minecraft:forest") || id.equals("minecraft:birch_forest")
                || id.equals("minecraft:dark_forest") || id.equals("blockbranch:dappled_oak_birch_woodland") || id.equals("blockbranch:maritime_redwood_fern_forest")
                || id.equals("blockbranch:warm_subhumid_oak_pine_woodland");
        }, MobCategory.CREATURE, WILD_BOAR, 4, 2, 4);
        BiomeModifications.addSpawn(context -> switch (context.getBiomeKey().identifier().toString()) {
            case "minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                 "minecraft:flower_forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga",
                 "minecraft:old_growth_spruce_taiga", "blockbranch:dappled_oak_birch_woodland",
                 "blockbranch:boreal_spruce_birch_woodland", "blockbranch:warm_subhumid_oak_pine_woodland",
                 "blockbranch:maritime_redwood_fern_forest" -> true;
            default -> false;
        }, MobCategory.CREATURE, RED_DEER, 5, 3, 6);
        BiomeModifications.addSpawn(context->switch(context.getBiomeKey().identifier().toString()){
            case "minecraft:plains","minecraft:sunflower_plains","blockbranch:rolling_lowland_forb_grass_meadow",
                 "blockbranch:dry_continental_scrub_steppe"->true;
            default->false;
        },MobCategory.CREATURE,AMERICAN_BISON,2,3,5);
        BiomeModifications.addSpawn(context->switch(context.getBiomeKey().identifier().toString()){
            case "minecraft:forest","minecraft:birch_forest","minecraft:old_growth_birch_forest","minecraft:dappled_forest",
                 "minecraft:taiga","minecraft:old_growth_pine_taiga","minecraft:old_growth_spruce_taiga",
                 "blockbranch:dappled_oak_birch_woodland","blockbranch:boreal_spruce_birch_woodland",
                 "blockbranch:maritime_redwood_fern_forest"->true;
            default->false;
        },MobCategory.CREATURE,BROWN_BEAR,1,1,2);
        BiomeModifications.addSpawn(context->switch(context.getBiomeKey().identifier().toString()){
            case "minecraft:swamp","minecraft:mangrove_swamp","minecraft:river",
                 "blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"->true;
            default->false;
        },MobCategory.CREATURE,MALLARD,3,2,5);
        BiomeModifications.addSpawn(context->switch(context.getBiomeKey().identifier().toString()){
            case "minecraft:river","minecraft:forest","minecraft:birch_forest","minecraft:taiga",
                 "blockbranch:dappled_oak_birch_woodland","blockbranch:boreal_spruce_birch_woodland","blockbranch:maritime_redwood_fern_forest",
                 "blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"->true;
            default->false;
        },MobCategory.CREATURE,EURASIAN_BEAVER,2,1,3);
        BiomeModifications.addSpawn(context->switch(context.getBiomeKey().identifier().toString()){
            case "minecraft:river","minecraft:swamp","blockbranch:open_inland_lake_sediment_bed",
                 "blockbranch:open_routed_river_channel","blockbranch:inland_lake_reedbed",
                 "blockbranch:gravel_bed_river_margin"->true;
            default->false;
        },MobCategory.WATER_AMBIENT,EUROPEAN_PERCH,4,3,6);
        BiomeModifications.addSpawn(context->switch(context.getBiomeKey().identifier().toString()){
            case "minecraft:beach","minecraft:stony_shore","minecraft:snowy_beach","blockbranch:low_gradient_sandy_shore"->true;
            default->false;
        },MobCategory.CREATURE,HARBOR_SEAL,2,2,4);
    }
    private WildlifeRuntime() {}
}
