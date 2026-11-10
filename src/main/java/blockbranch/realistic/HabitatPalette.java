package blockbranch.realistic;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/** Legacy holder groups retain their field names; a required map holds additional broad biomes. */
public record HabitatPalette(Margins margins,Land land,java.util.Map<String,Holder<Biome>> additional) {
    public HabitatPalette { additional=java.util.Map.copyOf(additional); }
    public HabitatPalette(Margins margins,Land land){this(margins,land,java.util.Map.of());}
    public Holder<Biome> additional(String key){return additional.getOrDefault(key,margins.fallback());}
    public static final MapCodec<HabitatPalette> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Margins.CODEC.forGetter(HabitatPalette::margins),Land.CODEC.forGetter(HabitatPalette::land),
        com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,Biome.CODEC).validate(map->map.containsKey("savanna")&&map.containsKey("jungle")&&map.containsKey("desert")&&map.containsKey("plains")&&map.containsKey("sunflower_plains")&&map.containsKey("forest")&&map.containsKey("birch_forest")&&map.containsKey("swamp")&&map.containsKey("mangrove_swamp")&&map.containsKey("taiga")&&map.containsKey("snowy_taiga")&&map.containsKey("ocean")&&map.containsKey("deep_ocean")&&map.containsKey("snowy_plains")&&map.containsKey("snowy_slopes")&&map.containsKey("meadow")&&map.containsKey("grove")&&map.containsKey("river")&&map.containsKey("frozen_river")&&map.containsKey("beach")&&map.containsKey("snowy_beach")&&map.containsKey("lukewarm_ocean")&&map.containsKey("deep_lukewarm_ocean")&&map.containsKey("old_growth_pine_taiga")&&map.containsKey("old_growth_spruce_taiga")&&map.containsKey("flower_forest")&&map.containsKey("old_growth_birch_forest")&&map.containsKey("dark_forest")&&map.containsKey("cherry_grove")&&map.containsKey("sparse_jungle")&&map.containsKey("bamboo_jungle")&&map.containsKey("windswept_hills")&&map.containsKey("windswept_forest")&&map.containsKey("savanna_plateau")&&map.containsKey("windswept_savanna")&&map.containsKey("stony_peaks")&&map.containsKey("jagged_peaks")&&map.containsKey("eroded_badlands")&&map.containsKey("badlands")&&map.containsKey("wooded_badlands")&&map.containsKey("windswept_gravelly_hills")&&map.containsKey("frozen_peaks")&&map.containsKey("warm_ocean")&&map.containsKey("dappled_forest")&&map.containsKey("stony_shore")&&map.containsKey("frozen_ocean")&&map.containsKey("deep_frozen_ocean")?com.mojang.serialization.DataResult.success(map):com.mojang.serialization.DataResult.error(()->"Missing completed broad biome holder")).fieldOf("additional_biomes").forGetter(HabitatPalette::additional)
    ).apply(i,HabitatPalette::new));
    public record Margins(Holder<Biome> fallback,Holder<Biome> scree,Holder<Biome> reedbed,Holder<Biome> riverMargin,Holder<Biome> sandyShore,Holder<Biome> lake,Holder<Biome> riverChannel,Holder<Biome> shelf,Holder<Biome> deepOcean) {
        public static final MapCodec<Margins> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Biome.CODEC.fieldOf("biome").forGetter(Margins::fallback),
            Biome.CODEC.fieldOf("scree_biome").forGetter(Margins::scree),
            Biome.CODEC.fieldOf("reedbed_biome").forGetter(Margins::reedbed),
            Biome.CODEC.fieldOf("river_margin_biome").forGetter(Margins::riverMargin),
            Biome.CODEC.fieldOf("sandy_shore_biome").forGetter(Margins::sandyShore),
            Biome.CODEC.fieldOf("lake_biome").forGetter(Margins::lake),
            Biome.CODEC.fieldOf("river_channel_biome").forGetter(Margins::riverChannel),
            Biome.CODEC.fieldOf("marine_shelf_biome").forGetter(Margins::shelf),
            Biome.CODEC.fieldOf("deep_ocean_biome").forGetter(Margins::deepOcean)
        ).apply(i,Margins::new));
    }
    public record Land(Holder<Biome> meadow,Holder<Biome> woodland,Holder<Biome> steppe,Holder<Biome> boreal,Holder<Biome> warm,Holder<Biome> coldSteppe,Holder<Biome> desert,Holder<Biome> humid,Holder<Biome> tundra) {
        public static final MapCodec<Land> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Biome.CODEC.fieldOf("meadow_biome").forGetter(Land::meadow),
            Biome.CODEC.fieldOf("woodland_biome").forGetter(Land::woodland),
            Biome.CODEC.fieldOf("steppe_biome").forGetter(Land::steppe),
            Biome.CODEC.fieldOf("boreal_biome").forGetter(Land::boreal),
            Biome.CODEC.fieldOf("warm_woodland_biome").forGetter(Land::warm),
            Biome.CODEC.fieldOf("cold_steppe_biome").forGetter(Land::coldSteppe),
            Biome.CODEC.fieldOf("desert_biome").forGetter(Land::desert),
            Biome.CODEC.fieldOf("humid_woodland_biome").forGetter(Land::humid),
            Biome.CODEC.fieldOf("tundra_biome").forGetter(Land::tundra)
        ).apply(i,Land::new));
    }
}
