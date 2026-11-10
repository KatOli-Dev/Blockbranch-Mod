package blockbranch.realistic;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Bound by its owning generator before biome queries; resolvers capture an immutable seed field. */
public final class ScreeBiomeSource extends BiomeSource {
    public static final MapCodec<ScreeBiomeSource> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.<ScreeBiomeSource>mapCodec(i->i.group(
        HabitatPalette.CODEC.forGetter((ScreeBiomeSource s)->s.palette),
        com.mojang.serialization.Codec.BOOL.optionalFieldOf("icebergs",false).forGetter(s->s.icebergsEnabled),
        com.mojang.serialization.Codec.BOOL.optionalFieldOf("sandy_coasts",false).forGetter(s->s.sandyCoastsEnabled),
        com.mojang.serialization.Codec.intRange(0,2).optionalFieldOf("dripstone_definition",0).forGetter(s->s.dripstoneDefinition),
        com.mojang.serialization.Codec.intRange(0,1).optionalFieldOf("cave_biome_definition",0).forGetter(s->s.caveBiomeDefinition),
        com.mojang.serialization.Codec.BOOL.optionalFieldOf("deep_dark",false).forGetter(s->s.deepDarkEnabled)
    ).apply(i,(palette,icebergs,sandy,dripstone,caves,dark)->new ScreeBiomeSource(palette,icebergs,sandy,dripstone,caves).withDeepDark(dark))).validate(s->s.deepDarkEnabled&&(!s.palette.additional().containsKey("deep_dark")||s.caveBiomeDefinition!=1)?com.mojang.serialization.DataResult.error(()->"Deep dark requires its holder and cave biome definition"):s.caveBiomeDefinition>0&&(s.dripstoneDefinition!=2||!s.palette.additional().containsKey("lush_caves")||!s.palette.additional().containsKey("sulfur_caves"))?com.mojang.serialization.DataResult.error(()->"Cave source requires its holders and formation definition"):s.dripstoneDefinition>0&&!s.palette.additional().containsKey("dripstone_caves")?com.mojang.serialization.DataResult.error(()->"Dripstone source requires holder"):s.icebergsEnabled&&!s.palette.additional().containsKey("ice_spikes")?com.mojang.serialization.DataResult.error(()->"Iceberg source requires ice_spikes holder"):com.mojang.serialization.DataResult.success(s));
    private final HabitatPalette palette;
    private final boolean icebergsEnabled;
    private final boolean sandyCoastsEnabled;
    private final Holder<Biome> fallback, scree, reedbed, riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,humid,tundra,lake,riverChannel,shelf,deepOcean;
    private final int dripstoneDefinition,caveBiomeDefinition;
    private volatile Bound bound;
    private boolean deepDarkEnabled,redwoodForestsEnabled;
    ScreeBiomeSource withRedwoodForests(boolean enabled){if(bound!=null)throw new IllegalStateException("Already bound biome source");redwoodForestsEnabled=enabled;return this;}
    ScreeBiomeSource withDeepDark(boolean enabled){if(bound!=null)throw new IllegalStateException("Already bound biome source");deepDarkEnabled=enabled;return this;}
    private record Bound(BiomeResolver surface,BiomeResolver full,DrainageNetwork drainage,long seed,java.util.function.BiFunction<Integer,Integer,blockbranch.cubic.CaveBiomeLayer> caves){}

    public ScreeBiomeSource(HabitatPalette palette){this(palette,false);}
    public ScreeBiomeSource(HabitatPalette palette,boolean icebergsEnabled) {this(palette,icebergsEnabled,false);}
    public ScreeBiomeSource(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled) {this(palette,icebergsEnabled,sandyCoastsEnabled,0);}
    public ScreeBiomeSource(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,int dripstoneDefinition) {this(palette,icebergsEnabled,sandyCoastsEnabled,dripstoneDefinition,0);}
    public ScreeBiomeSource(HabitatPalette palette,boolean icebergsEnabled,boolean sandyCoastsEnabled,int dripstoneDefinition,int caveBiomeDefinition) {
        this.caveBiomeDefinition=caveBiomeDefinition;
        this.dripstoneDefinition=dripstoneDefinition;
        this.sandyCoastsEnabled=sandyCoastsEnabled;
        this.icebergsEnabled=icebergsEnabled;
        this.palette=palette;
        this.fallback=palette.margins().fallback();
        this.scree=palette.margins().scree();
        this.reedbed=palette.margins().reedbed();
        this.riverMargin=palette.margins().riverMargin();
        this.sandyShore=palette.margins().sandyShore();
        this.lake=palette.margins().lake();
        this.riverChannel=palette.margins().riverChannel();
        this.shelf=palette.margins().shelf();
        this.deepOcean=palette.margins().deepOcean();
        this.meadow=palette.land().meadow();
        this.woodland=palette.land().woodland();
        this.steppe=palette.land().steppe();
        this.boreal=palette.land().boreal();
        this.warm=palette.land().warm();
        this.coldSteppe=palette.land().coldSteppe();
        this.desert=palette.land().desert();
        this.humid=palette.land().humid();
        this.tundra=palette.land().tundra();
    }
    void bind(long seed,DrainageNetwork drainage,ClimateField climate) {bind(seed,drainage,climate,null);}
    void bind(long seed,DrainageNetwork drainage,ClimateField climate,java.util.function.BiFunction<Integer,Integer,blockbranch.cubic.LitCaveLayer> growth) {
        if(caveBiomeDefinition>0&&growth==null)throw new IllegalArgumentException("Cave biomes require a fixed-seed moss planner");
        var ecology=new GroundEcology.Field(drainage::sample,climate);
        BiomeResolver rawSurface = (x, y, z) -> StonyShore.contains(x*4,z*4,drainage) ? palette.additional("stony_shore") : InlandLakeReedbed.contains(x*4,z*4,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz)) ? reedbed
            : LakeSediment.contains(x*4,z*4,drainage) ? lake
            : SnowySlopes.contains(x*4,z*4,drainage,climate) ? palette.additional("snowy_slopes")
            : MountainPeaks.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(MountainPeaks.key(MountainPeaks.kind(seed,x*4,z*4,drainage,climate)))
            : AlpineScree.contains(x * 4, z * 4, drainage::sample) ? scree
            : GravelRiverMargin.contains(x*4,z*4,(wx,wz)->GravelRiverMargin.site(drainage,wx,wz)) ? riverMargin
            : MangroveSwamp.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("mangrove_swamp")
            : LowGradientSandyShore.contains(x*4,z*4,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz)) ? (CoastalBeach.contains(x*4,z*4,drainage,climate,true) ? palette.additional("snowy_beach") : CoastalBeach.contains(x*4,z*4,drainage,climate,false) ? palette.additional("beach") : sandyShore)
            : FrozenRiver.contains(x*4,z*4,drainage,climate) ? palette.additional("frozen_river")
            : RoutedRiver.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("river")
            : RiverChannel.contains(x*4,z*4,drainage) ? riverChannel
            : icebergsEnabled&&IcebergField.habitat(seed,x*4,z*4,drainage,climate)?palette.additional("ice_spikes")
            : FrozenOcean.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(FrozenOcean.key(FrozenOcean.kind(seed,x*4,z*4,drainage,climate)))
            : palette.additional().containsKey("cold_ocean")&&palette.additional().containsKey("deep_cold_ocean")&&ColdOcean.contains(seed,x*4,z*4,drainage,climate)?palette.additional("cold_ocean")
            : palette.additional().containsKey("cold_ocean")&&palette.additional().containsKey("deep_cold_ocean")&&DeepColdOcean.contains(x*4,z*4,drainage,climate)?palette.additional("deep_cold_ocean")
            : TemperateOcean.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("ocean")
            : LukewarmOcean.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("lukewarm_ocean")
            : WarmOcean.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("warm_ocean")
            : MarineShelf.contains(x*4,z*4,drainage) ? shelf
            : TemperateDeepOcean.contains(x*4,z*4,drainage,climate) ? palette.additional("deep_ocean")
            : DeepLukewarmOcean.contains(x*4,z*4,drainage,climate) ? palette.additional("deep_lukewarm_ocean")
            : DeepOcean.contains(x*4,z*4,drainage) ? deepOcean
            : ColdUplands.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(ColdUplands.key(ColdUplands.kind(seed,x*4,z*4,drainage,climate)))
            : Badlands.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(Badlands.key(Badlands.kind(seed,x*4,z*4,drainage,climate)))
            : SavannaUplands.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(SavannaUplands.key(SavannaUplands.kind(seed,x*4,z*4,drainage,climate)))
            : WindsweptUplands.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(WindsweptUplands.key(WindsweptUplands.kind(seed,x*4,z*4,drainage,climate)))
            : Swamp.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("swamp")
            : redwoodForestsEnabled&&RedwoodForest.contains(seed,x*4,z*4,drainage,climate) ? palette.additional(RedwoodForest.KEY)
            : BirchForest.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("birch_forest")
            : Forest.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("forest")
            : TemperateForestVariants.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(TemperateForestVariants.key(TemperateForestVariants.kind(seed,x*4,z*4,drainage,climate)))
            : DarkCherryWoodland.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(DarkCherryWoodland.key(DarkCherryWoodland.kind(seed,x*4,z*4,drainage,climate)))
            : DappledForest.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("dappled_forest")
            : OakBirchWoodland.contains(seed,x*4,z*4,drainage,climate) ? woodland
            : TropicalForestVariants.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(TropicalForestVariants.key(TropicalForestVariants.kind(seed,x*4,z*4,drainage,climate)))
            : WarmWoodland.contains(seed,x*4,z*4,drainage,climate) ? warm
            : HumidWoodland.contains(seed,x*4,z*4,drainage,climate) ? humid
            : SunflowerPlains.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("sunflower_plains")
            : Plains.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("plains")
            : MontaneMeadow.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("meadow")
            : LowlandMeadow.contains(x*4,z*4,drainage,climate) ? meadow
            : DrySteppe.contains(x*4,z*4,drainage,climate) ? steppe
            : Grove.contains(seed,x*4,z*4,drainage,climate,ecology) ? palette.additional("grove")
            : SnowyTaiga.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("snowy_taiga")
            : Taiga.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("taiga")
            : OldGrowthTaiga.kind(seed,x*4,z*4,drainage,climate)>0 ? palette.additional(OldGrowthTaiga.kind(seed,x*4,z*4,drainage,climate)==OldGrowthTaiga.PINE?"old_growth_pine_taiga":"old_growth_spruce_taiga")
            : BorealWoodland.contains(seed,x*4,z*4,drainage,climate) ? boreal
            : ColdSteppe.contains(x*4,z*4,drainage,climate) ? coldSteppe
            : GravelDesert.contains(x*4,z*4,drainage,climate) ? desert
            : SnowyPlains.contains(x*4,z*4,drainage,climate) ? palette.additional("snowy_plains")
            : ColdTundra.contains(x*4,z*4,drainage,climate) ? tundra
            : palette.additional().containsKey("savanna") && Savanna.contains(seed,x*4,z*4,drainage,climate) ? palette.additional("savanna")
            : palette.additional().containsKey("jungle")&&Jungle.contains(seed,x*4,z*4,drainage,climate)?palette.additional("jungle")
            : SandyDesert.contains(seed,x*4,z*4,drainage,climate)?palette.additional("desert"):fallback;
        BiomeResolver surface=(x,y,z)->MapPreviewSamples.surface(x,y,z,rawSurface);
        BiomeResolver full=dripstoneDefinition==0?surface:(x,y,z)->{double province=DripstoneHabitat.province(seed,x*4,z*4);return province>.2&&DripstoneHabitat.contains(seed,x*4,y*4,z*4,drainage.sample(x*4,z*4).height(),province)?palette.additional("dripstone_caves"):surface.getNoiseBiome(x,y,z);};
        java.util.function.BiFunction<Integer,Integer,blockbranch.cubic.CaveBiomeLayer> caves=null;
        if(caveBiomeDefinition>0){
            var cache=new java.util.LinkedHashMap<Long,blockbranch.cubic.CaveBiomeLayer>(64,.75f,true);
            caves=(cx,cz)->{
                long key=(cx&0xffffffffL)|((long)cz<<32);
                synchronized(cache){var found=cache.get(key);if(found!=null)return found;}
                var result=new blockbranch.cubic.CaveBiomeLayer(seed,cx,cz,palette.additional("lush_caves"),palette.additional("sulfur_caves"),growth.apply(cx,cz));
                synchronized(cache){var old=cache.get(key);if(old!=null)return old;cache.put(key,result);if(cache.size()>64)cache.remove(cache.keySet().iterator().next());}return result;
            };
            var base=full;var selection=caves;
            full=(x,y,z)->selection.apply(x>>2,z>>2).biome(x,y,z,drainage.sample(x*4,z*4).height(),base.getNoiseBiome(x,y,z));
        }
        if(deepDarkEnabled){
            var base=full;
            full=(x,y,z)->OverworldStructures.deepDark(seed,x*4,y*4,z*4,drainage)?palette.additional("deep_dark"):base.getNoiseBiome(x,y,z);
        }
        bound=new Bound(surface,full,drainage,seed,caves);
    }
    public boolean bound(){return bound!=null;}
    public BiomeResolver surfaceResolver(){var b=bound;if(b==null)throw new IllegalStateException("Unbound surface source");return b.surface;}
    @Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return Stream.concat(palette.additional().values().stream(),Stream.of(fallback, scree, reedbed, riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,humid,tundra,lake,riverChannel,shelf,deepOcean)); }
    @Override public BiomeResolver createResolverForChunk(Climate.Sampler sampler,
            int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        var b=bound;if(b==null)throw new IllegalStateException("Unbound biome source");var current=b.surface;
        // The surface source is independent of Y. Native biome filling queries the same
        // 16 quart columns at every height in all 24 physical sections. Capture
        // their results once per chunk, with no shared mutable cache or seed key.
        if (sizeX != 4 || sizeZ != 4) return b.full;
        var cached=columnResolver(current,minX,minZ);if(dripstoneDefinition==0)return cached;
        double[] provinces=new double[16];int[] heights=new int[16];
        for(int z=0;z<4;z++)for(int x=0;x<4;x++){int i=z*4+x,wx=(minX+x)*4,wz=(minZ+z)*4;provinces[i]=DripstoneHabitat.province(b.seed,wx,wz);if(provinces[i]>.2||b.caves!=null)heights[i]=b.drainage.sample(wx,wz).height();}
        var caveLayer=b.caves==null?null:b.caves.apply(minX>>2,minZ>>2);
        if(caveLayer!=null&&((minX&3)!=0||(minZ&3)!=0))return b.full;
        final int darkMask=deepDarkEnabled?OverworldStructures.deepDarkMask(b.seed,minX>>2,minZ>>2,b.drainage):0;
        return (x,y,z)->{int dx=x-minX,dz=z-minZ;if(dx<0||dx>=4||dz<0||dz>=4)return b.full.getNoiseBiome(x,y,z);int i=dz*4+dx;if((darkMask&(1<<i))!=0&&DeepDarkHabitat.atHeight(heights[i],(long)y*4))return palette.additional("deep_dark");var selected=provinces[i]>.2&&DripstoneHabitat.contains(b.seed,x*4,y*4,z*4,heights[i],provinces[i])?palette.additional("dripstone_caves"):cached.getNoiseBiome(x,y,z);return caveLayer==null?selected:caveLayer.biome(x,y,z,heights[i],selected);};
    }

    static BiomeResolver columnResolver(BiomeResolver current, int minX, int minZ) {
        var columns = new java.util.ArrayList<Holder<Biome>>(16);
        for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++)
            columns.add(current.getNoiseBiome(minX + x, 0, minZ + z));
        var values = java.util.List.copyOf(columns);
        return (x, y, z) -> {
            int dx = x - minX, dz = z - minZ;
            return dx >= 0 && dx < 4 && dz >= 0 && dz < 4
                ? values.get(dz * 4 + dx) : current.getNoiseBiome(x, y, z);
        };
    }

    @Override public BiomeResolver createResolver(Climate.Sampler sampler) {
        var current = bound;
        if (current == null) throw new IllegalStateException("Scree biome source requires its seeded realistic generator");
        return current.full;
    }
}
