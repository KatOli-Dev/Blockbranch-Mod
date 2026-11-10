package blockbranch.wildlife;

import java.util.Set;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.world.entity.*;

/** Bounded legacy-holder roles; preserve native biome tables and shared category caps. */
final class VanillaHabitatIntegration {
    private static final Set<String> WOODLAND=Set.of("dappled_oak_birch_woodland","boreal_spruce_birch_woodland","warm_subhumid_oak_pine_woodland");
    private static final Set<String> MEADOW=Set.of("rolling_lowland_forb_grass_meadow");
    private static final Set<String> FRESHWATER=Set.of("open_inland_lake_sediment_bed","open_routed_river_channel","inland_lake_reedbed","gravel_bed_river_margin");
    static void initialize(){
        for(var set:java.util.List.of(WOODLAND,MEADOW)){
            add(set,MobCategory.CREATURE,EntityTypes.SHEEP,12,4,4);
            add(set,MobCategory.CREATURE,EntityTypes.PIG,10,4,4);
            add(set,MobCategory.CREATURE,EntityTypes.CHICKEN,10,4,4);
            add(set,MobCategory.CREATURE,EntityTypes.COW,8,4,4);
        }
        add(Set.of("dappled_oak_birch_woodland","warm_subhumid_oak_pine_woodland"),MobCategory.CREATURE,EntityTypes.WOLF,5,4,4);
        var boreal=Set.of("boreal_spruce_birch_woodland");
        add(boreal,MobCategory.CREATURE,EntityTypes.WOLF,8,4,4);
        add(boreal,MobCategory.CREATURE,EntityTypes.RABBIT,4,2,3);
        add(boreal,MobCategory.CREATURE,EntityTypes.FOX,8,2,4);
        // Old-growth redwood stands carry no livestock; their fauna is shy forest wildlife (deer, bear, boar and beaver arrive from WildlifeRuntime).
        var redwood=Set.of("maritime_redwood_fern_forest");
        add(redwood,MobCategory.CREATURE,EntityTypes.WOLF,3,3,4);
        add(redwood,MobCategory.CREATURE,EntityTypes.FOX,4,2,3);
        add(redwood,MobCategory.CREATURE,EntityTypes.RABBIT,2,2,3);
        add(MEADOW,MobCategory.CREATURE,EntityTypes.HORSE,5,2,6);
        add(MEADOW,MobCategory.CREATURE,EntityTypes.DONKEY,1,1,3);
        add(Set.of("alpine_scree_slope"),MobCategory.CREATURE,EntityTypes.GOAT,5,1,3);
        add(Set.of("cold_continental_fescue_steppe","cold_dwarf_shrub_tundra","continental_gravel_desert"),MobCategory.CREATURE,EntityTypes.RABBIT,2,2,3);
        var drySteppe=Set.of("dry_continental_scrub_steppe");
        // Native grass-floor placement keeps these grazers on the grassy portions.
        add(drySteppe,MobCategory.CREATURE,EntityTypes.HORSE,2,2,4);
        add(drySteppe,MobCategory.CREATURE,EntityTypes.DONKEY,1,1,2);
        var humid=Set.of("humid_subtropical_oak_fern_woodland");
        add(humid,MobCategory.CREATURE,WildlifeRuntime.WILD_BOAR,3,2,4);
        add(humid,MobCategory.CREATURE,EntityTypes.RABBIT,2,2,3);
        // Geography is seabed-defined; unchanged native predicates confine these to surface waters.
        var marine=Set.of("open_marine_shelf_coastal_sea","deep_open_ocean_basin_bed");
        add(marine,MobCategory.WATER_AMBIENT,EntityTypes.COD,10,3,6);
        add(marine,MobCategory.WATER_CREATURE,EntityTypes.SQUID,1,1,4);
        add(FRESHWATER,MobCategory.WATER_AMBIENT,EntityTypes.SALMON,5,1,5);
        add(FRESHWATER,MobCategory.WATER_CREATURE,EntityTypes.SQUID,2,1,4);
    }
    private static void add(Set<String> habitats,MobCategory category,EntityType<?> type,int weight,int min,int max){
        BiomeModifications.addSpawn(context->{var id=context.getBiomeKey().identifier();return id.getNamespace().equals("blockbranch")&&habitats.contains(id.getPath());},category,type,weight,min,max);
    }
    private VanillaHabitatIntegration(){}
}
