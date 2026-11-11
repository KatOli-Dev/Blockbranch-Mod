package blockbranch.cubic;

import blockbranch.metallurgy.SectionVeins;
import blockbranch.realistic.UndergroundStructure;

import java.util.*;
import blockbranch.realistic.TerrainShape;
import blockbranch.realistic.OreLayer;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.material.FluidState;

/** Immutable heightfield definition. No vertical block arrays are retained here. */
public final class TerrainProfile {
    /** Existing definition82 host-rock boundary, shared with structure fitting. */
    public static final int DEEPSLATE_TOP=-1;
    public static boolean deepslateAt(int y){return y<=DEEPSLATE_TOP;}
    private final int floor, ceiling;
    private final UndergroundLayer underground;
    private final OreLayer ores;
    private final DripstoneLayer dripstone;
    private final DripstoneLayer decorations;
    private final LitCaveLayer litCaves;
    private final CaveBiomeLayer caveBiomes;
    private final int[] ground, water,rockGround;
    /** Highest canopy block above local ground. 48 until redwoods; taller canopies exist only in schema-257 worlds, which earlier readers refuse. */
    public static final int CANOPY_WINDOW=128;
    private final byte[] scree, reedbed, riverMargin, sandyShore, meadow,woodland,steppe,boreal,warm,coldSteppe,desert;
    private final PalettedContainerRO<Holder<Biome>> biomes;
    private final Strategy<BlockState> states;
    private final int highest;
    private final int highestGround,lowestGround,highestRock,lowestRock;
    private final FloatingIceLayer floatingIce;
    private final PerennialIceLayer perennialIce;
    private final CanopyLayer canopy;
    private final Set<Integer> boundaries;
    private final Set<Integer> randomTickSections;

    public TerrainProfile(int floor, int ceiling, int[] ground, int[] water,
            PalettedContainerRO<Holder<Biome>> biomes, Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,new byte[256],biomes,states);
    }
    public TerrainProfile(int floor, int ceiling, int[] ground, int[] water, byte[] scree,
            PalettedContainerRO<Holder<Biome>> biomes, Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,new byte[256],biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,new byte[256],biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,new byte[256],biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,new byte[256],biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,new byte[256],CanopyLayer.EMPTY,biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,CanopyLayer canopy,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,new byte[256],canopy,biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,CanopyLayer canopy,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,new byte[256],canopy,biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,CanopyLayer canopy,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,new byte[256],canopy,biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,byte[] warm,CanopyLayer canopy,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,new byte[256],canopy,biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,byte[] warm,byte[] coldSteppe,CanopyLayer canopy,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,new byte[256],canopy,biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,byte[] warm,byte[] coldSteppe,byte[] desert,CanopyLayer canopy,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,canopy,FloatingIceLayer.EMPTY,biomes,states);
    }
    public TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,byte[] warm,byte[] coldSteppe,byte[] desert,CanopyLayer canopy,FloatingIceLayer floatingIce,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,canopy,floatingIce,biomes,states,UndergroundLayer.NONE);
    }
    private TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,byte[] warm,byte[] coldSteppe,byte[] desert,CanopyLayer canopy,FloatingIceLayer floatingIce,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states,UndergroundLayer underground) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,canopy,floatingIce,biomes,states,underground,OreLayer.NONE);
    }
    private TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,byte[] warm,byte[] coldSteppe,byte[] desert,CanopyLayer canopy,FloatingIceLayer floatingIce,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states,UndergroundLayer underground,OreLayer ores) {
        this(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,canopy,floatingIce,biomes,states,underground,ores,PerennialIceLayer.EMPTY);
    }
    private TerrainProfile(int floor,int ceiling,int[] ground,int[] water,byte[] scree,byte[] reedbed,byte[] riverMargin,byte[] sandyShore,byte[] meadow,byte[] woodland,byte[] steppe,byte[] boreal,byte[] warm,byte[] coldSteppe,byte[] desert,CanopyLayer canopy,FloatingIceLayer floatingIce,
            PalettedContainerRO<Holder<Biome>> biomes,Strategy<BlockState> states,UndergroundLayer underground,OreLayer ores,PerennialIceLayer perennialIce) {
        this.perennialIce=perennialIce;
        if(ores.enabled()&&(floor!=TerrainShape.FLOOR||ceiling!=TerrainShape.CEILING))throw new IllegalArgumentException("Ores require Realistic terrain envelope");
        if(underground.enabled()&&(floor!=TerrainShape.FLOOR||ceiling!=TerrainShape.CEILING))throw new IllegalArgumentException("Underground requires its defined terrain envelope");
        this.underground=underground;this.dripstone=DripstoneLayer.NONE;this.decorations=DripstoneLayer.NONE;this.litCaves=LitCaveLayer.NONE;this.caveBiomes=CaveBiomeLayer.NONE;
        if(floor < ColumnLimits.MIN_Y || ceiling > ColumnLimits.MAX_Y || floor >= ceiling || ground.length != 256 || water.length != 256 || scree.length != 256 || reedbed.length != 256 || riverMargin.length != 256 || sandyShore.length != 256 || meadow.length != 256 || woodland.length != 256 || steppe.length != 256 || boreal.length != 256 || warm.length != 256 || coldSteppe.length != 256 || desert.length != 256)
            throw new IllegalArgumentException("Invalid procedural column bounds");
        this.floatingIce=floatingIce;
        this.highestGround=Arrays.stream(ground).max().orElse(floor);this.lowestGround=Arrays.stream(ground).min().orElse(floor);
        this.floor=floor; this.ceiling=ceiling; this.ground=ground.clone(); this.water=water.clone();
        int[] rock=this.ground;
        if(perennialIce.enabled())for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(perennialIce.grounded(x,z)){if(rock==this.ground)rock=this.ground.clone();rock[z*16+x]=perennialIce.bottom(x,z)-1;}
        rockGround=rock;lowestRock=rock==this.ground?lowestGround:Arrays.stream(rock).min().orElse(floor);highestRock=rock==this.ground?highestGround:Arrays.stream(rock).max().orElse(floor);this.ores=ores.bind(lowestRock,highestRock);
        if(underground.hasApproach())for(int z=0;z<16;z++)for(int x=0;x<16;x++)
            if(water[z*16+x]!=Integer.MIN_VALUE&&underground.approachColumn(x,z))throw new IllegalArgumentException("Approach crosses source water");
        this.scree=scree.clone(); this.reedbed=reedbed.clone(); this.riverMargin=riverMargin.clone();this.sandyShore=sandyShore.clone();this.meadow=meadow.clone();this.woodland=woodland.clone();this.steppe=steppe.clone();this.boreal=boreal.clone();this.warm=warm.clone();this.coldSteppe=coldSteppe.clone();this.desert=desert.clone();this.canopy=canopy;
        this.biomes=blockbranch.realistic.UniformSnapshots.compactBiomes(biomes); this.states=states;
        int top=floor; var ticking=new TreeSet<Integer>();var transitions=new TreeSet<Integer>(); transitions.add(floor>>4);
        for(int i=0;i<256;i++) {
            if(ground[i]<floor || ground[i]>=ceiling || water[i]>ceiling) throw new IllegalArgumentException("Invalid heightfield sample");
            if(scree[i]<0 || scree[i]>3 || (scree[i]>0 && (ground[i]-scree[i]<=floor || water[i]>ground[i])))
                throw new IllegalArgumentException("Invalid scree cover");
            // Water is exclusive: depth-one/two beds have offsets 2/3; stacks
            // leave one or two dry stems. Cover 1 retains its mud-only meaning.
            if(reedbed[i]!=0 && (scree[i]!=0 || (reedbed[i]!=1 && reedbed[i]!=2 && reedbed[i]!=3 && reedbed[i]!=4)
                || ground[i]-2<=floor || water[i]<=ground[i]+1 || water[i]>ground[i]+4
                || (reedbed[i]>1 && ((water[i]-ground[i]<2 || water[i]-ground[i]>3 || (reedbed[i]!=water[i]-ground[i]+1 && reedbed[i]!=water[i]-ground[i])) || ground[i]+reedbed[i]>=ceiling))))
                throw new IllegalArgumentException("Invalid reedbed cover");
            if(riverMargin[i]<0 || riverMargin[i]>14 || (riverMargin[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || ground[i]-2<=floor))
                || (riverMargin[i]==4 && (ground[i]+1>=ceiling || water[i]>ground[i])))
                throw new IllegalArgumentException("Invalid river-margin cover");
            if(riverMargin[i]>=5&&riverMargin[i]<=12&&(water[i]<=ground[i]+1||water[i]>ceiling||(lakePlantPresent(riverMargin[i])&&(water[i]-ground[i]-1<2||water[i]-ground[i]-1>7))))throw new IllegalArgumentException("Invalid aquatic sediment/water cover");
            // Physical dry bank material can cross a quart biome edge; it still cannot carry water.
            if(riverMargin[i]>=13&&water[i]!=Integer.MIN_VALUE)throw new IllegalArgumentException("Dry River bank requires dry datum");
            if(sandyShore[i]>=3&&(meadow[i]!=0||woodland[i]!=0||steppe[i]!=0||boreal[i]!=0||warm[i]!=0||coldSteppe[i]!=0||desert[i]!=0))throw new IllegalArgumentException("Exposed coastal bedrock overlaps soil palette");
            if(sandyShore[i]<0 || sandyShore[i]>4 || (sandyShore[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || riverMargin[i]!=0 || ground[i]-5<=floor))
                || (sandyShore[i]==2 && (ground[i]+1>=ceiling || water[i]>ground[i])))
                throw new IllegalArgumentException("Invalid sandy-shore cover");
            if(meadow[i]<0 || meadow[i]>13 || (meadow[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || riverMargin[i]!=0 || sandyShore[i]!=0
                || water[i]>ground[i] || ground[i]-3<=floor)) || (meadowPlantHeight(meadow[i])>0 && ground[i]+meadowPlantHeight(meadow[i])>=ceiling))
                throw new IllegalArgumentException("Invalid meadow cover");
            if(woodland[i]<0 || woodland[i]>blockbranch.realistic.LeafLitter.WOODLAND_LAST || (woodland[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || riverMargin[i]!=0 || sandyShore[i]!=0 || meadow[i]!=0
                || water[i]>ground[i] || ground[i]-3<=floor)) || (woodlandPlantHeight(woodland[i])>0 && ground[i]+woodlandPlantHeight(woodland[i])>=ceiling)) throw new IllegalArgumentException("Invalid woodland cover");
            if(steppe[i]<0 || steppe[i]>6 || (steppe[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || riverMargin[i]!=0 || sandyShore[i]!=0 || meadow[i]!=0 || woodland[i]!=0
                || water[i]>ground[i] || ground[i]-3<=floor)) || (steppePlantPresent(steppe[i]) && ground[i]+1>=ceiling)) throw new IllegalArgumentException("Invalid steppe cover");
            if(boreal[i]<0 || boreal[i]>blockbranch.realistic.BorealSurface.LAST || (boreal[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || riverMargin[i]!=0 || sandyShore[i]!=0 || meadow[i]!=0 || woodland[i]!=0 || steppe[i]!=0
                || water[i]>ground[i] || ground[i]-3<=floor)) || (borealPlantPresent(boreal[i]) && ground[i]+1>=ceiling)) throw new IllegalArgumentException("Invalid boreal cover");
            if(warm[i]<0 || warm[i]>blockbranch.realistic.LeafLitter.WARM_LAST || (warm[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || riverMargin[i]!=0 || sandyShore[i]!=0 || meadow[i]!=0 || woodland[i]!=0 || steppe[i]!=0 || boreal[i]!=0
                || water[i]>ground[i] || ground[i]-3<=floor)) || (warmPlantPresent(warm[i]) && ground[i]+1>=ceiling)) throw new IllegalArgumentException("Invalid warm woodland cover");
            if(coldSteppe[i]<0 || coldSteppe[i]>7&&!blockbranch.realistic.ColdUplands.encoded(coldSteppe[i])&&!blockbranch.realistic.LandIceField.encoded(coldSteppe[i]) || (coldSteppe[i]>0 && (scree[i]!=0 || reedbed[i]!=0 || riverMargin[i]!=0 || sandyShore[i]!=0 || meadow[i]!=0 || woodland[i]!=0 || steppe[i]!=0 || boreal[i]!=0 || warm[i]!=0
                || water[i]>ground[i] || ground[i]-coldLayerDepth(coldSteppe[i])<=floor)) || (coldPlantPresent(coldSteppe[i]) && ground[i]+1>=ceiling))throw new IllegalArgumentException("Invalid cold steppe cover");
            if(desert[i]<0||desert[i]>20&&!blockbranch.realistic.Badlands.encoded(desert[i])||(desert[i]>0&&(scree[i]!=0||reedbed[i]!=0||riverMargin[i]!=0||sandyShore[i]!=0||meadow[i]!=0||woodland[i]!=0||steppe[i]!=0||boreal[i]!=0||warm[i]!=0||coldSteppe[i]!=0||water[i]>ground[i]||ground[i]-desertLayerDepth(desert[i])<=floor))||(desertPlantHeight(desert[i])>0&&ground[i]+desertPlantHeight(desert[i])>=ceiling))throw new IllegalArgumentException("Invalid desert cover");
            if(desertPlantHeight(desert[i])>0){top=Math.max(top,ground[i]+desertPlantHeight(desert[i]));transitions.add((ground[i]+desertPlantHeight(desert[i]))>>4);}
            if(blockbranch.realistic.SandyDesert.cactus(desert[i]))for(int py=ground[i]+1;py<=ground[i]+desertPlantHeight(desert[i]);py++)ticking.add(py>>4);
            if(blockbranch.realistic.Badlands.encoded(desert[i]))for(int sy=(ground[i]-64)>>4;sy<=ground[i]>>4;sy++)transitions.add(sy);
            if(blockbranch.realistic.ColdUplands.encoded(coldSteppe[i])||blockbranch.realistic.LandIceField.encoded(coldSteppe[i]))transitions.add((ground[i]-coldLayerDepth(coldSteppe[i]))>>4);
            if(desert[i]>=8){transitions.add((ground[i]-desertSandDepth(desert[i]))>>4);transitions.add((ground[i]-desertLayerDepth(desert[i]))>>4);}
            if(meadowPlantHeight(meadow[i])>0){top=Math.max(top,ground[i]+meadowPlantHeight(meadow[i]));for(int stem=1;stem<=meadowPlantHeight(meadow[i]);stem++)transitions.add((ground[i]+stem)>>4);}
            top=Math.max(top,Math.max(ground[i],water[i]- (water[i]==Integer.MIN_VALUE ? 0 : 1)));
            if(reedbed[i]>1) top=Math.max(top,ground[i]+reedbed[i]);
            if(riverMargin[i]==4) top=Math.max(top,ground[i]+1);
            if(sandyShore[i]==2 || woodlandPlantPresent(woodland[i]) || steppePlantPresent(steppe[i]) || borealPlantPresent(boreal[i]) || warmPlantPresent(warm[i]) || coldPlantPresent(coldSteppe[i]) || desertPlantHeight(desert[i])>0) top=Math.max(top,ground[i]+1);
            if(woodlandPlantHeight(woodland[i])>0){top=Math.max(top,ground[i]+woodlandPlantHeight(woodland[i]));for(int stem=1;stem<=woodlandPlantHeight(woodland[i]);stem++)transitions.add((ground[i]+stem)>>4);}
            transitions.add(ground[i]>>4); if(water[i]>ground[i]) transitions.add((water[i]-1)>>4);
            if(scree[i]>0) transitions.add((ground[i]-scree[i])>>4);
            if(reedbed[i]>0) transitions.add((ground[i]-2)>>4);
            if(reedbed[i]>1) transitions.add((ground[i]+reedbed[i])>>4);
            if(riverMargin[i]>0) transitions.add((ground[i]-2)>>4);
            if(riverMargin[i]==4) transitions.add((ground[i]+1)>>4);
            if(lakePlantPresent(riverMargin[i]))transitions.add((ground[i]+1)>>4);
            if(sandyShore[i]>0) { transitions.add((ground[i]-3)>>4);transitions.add((ground[i]-5)>>4); }
            if(sandyShore[i]==2 || woodlandPlantPresent(woodland[i]) || steppePlantPresent(steppe[i]) || borealPlantPresent(boreal[i]) || warmPlantPresent(warm[i]) || coldPlantPresent(coldSteppe[i]) || desertPlantHeight(desert[i])>0) transitions.add((ground[i]+1)>>4);
            if(desert[i]>0)transitions.add((ground[i]-2)>>4);
            if(meadow[i]>0 || woodland[i]>0 || steppe[i]>0 || boreal[i]>0 || warm[i]>0 || coldSteppe[i]>0) transitions.add((ground[i]-3)>>4);
        }
        for(int value:canopy.encode()) {
            int key=value>>>CanopyLayer.STATE_BITS,y=(key>>>8)+blockbranch.realistic.TerrainShape.FLOOR,i=key&255;
            int code=value&CanopyLayer.STATE_MASK;
            boolean benthic=code>=blockbranch.realistic.AbyssalTraces.FIRST&&code<=blockbranch.realistic.AbyssalTraces.LAST;
            if(benthic&&(y!=ground[i]+1||water[i]-ground[i]-1<3000||water[i]-ground[i]-1>6000||(riverMargin[i]!=5&&riverMargin[i]!=6)))throw new IllegalArgumentException("Benthic trace requires abyssal source water and fine-bed support");
            if(code>=46&&code<=60||code>=73&&code<=77||code>=81&&code<=88||code==90||code==91||code==149||blockbranch.realistic.WarmOcean.aquatic(code))ticking.add(y>>4);
            if((code==89||code==90)&&(water[i]<=ground[i]+1||y>=water[i]))throw new IllegalArgumentException("Kelp requires existing source water");
            // Ice is per-block material, not a quart-biome paint mask. The profile
            // can validate its water geometry; generation validates routed water/climate.
            if(code==91&&(water[i]<=ground[i]+2||y!=water[i]-1))throw new IllegalArgumentException("River ice requires surface water and liquid beneath");
            if(code==72&&(water[i]<=ground[i]+1||y>=water[i]))throw new IllegalArgumentException("Waterlogged root requires existing source water");
            if(code==61&&(water[i]<=ground[i]+1||y!=water[i]))throw new IllegalArgumentException("Lily pad requires existing water surface");
            if(blockbranch.realistic.WarmOcean.aquatic(code)&&(water[i]<=ground[i]+1||y>=water[i]))throw new IllegalArgumentException("Reef requires existing source water");
            if(y<=ground[i] || y>=ceiling || water[i]>ground[i]&&y<water[i]&&code!=72&&code!=89&&code!=90&&code!=91&&!benthic&&!blockbranch.realistic.WarmOcean.aquatic(code) || code!=91&&y-ground[i]>CANOPY_WINDOW) throw new IllegalArgumentException("Canopy intersects terrain/water or exceeds its surface window");
        }
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int low=floatingIce.bottom(x,z),high=floatingIce.top(x,z),i=z*16+x;
            if(low!=Integer.MIN_VALUE&&(low<=ground[i]||water[i]!=TerrainShape.SEA_LEVEL||high>=ceiling))throw new IllegalArgumentException("Floating ice requires marine water and seabed clearance");
        }
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int low=perennialIce.bottom(x,z),high=perennialIce.top(x,z),i=z*16+x;if(low==Integer.MIN_VALUE)continue;
            if(low<=floor||high>=ceiling)throw new IllegalArgumentException("Perennial ice exceeds parent profile bounds");
            if(perennialIce.grounded(x,z)){
                if(high!=ground[i]||water[i]!=Integer.MIN_VALUE||scree[i]!=0||reedbed[i]!=0||riverMargin[i]!=0||sandyShore[i]!=0||meadow[i]!=0||woodland[i]!=0||steppe[i]!=0||boreal[i]!=0||warm[i]!=0||coldSteppe[i]!=0||desert[i]!=0||canopy.topY(x,z)!=Integer.MIN_VALUE)throw new IllegalArgumentException("Grounded perennial ice conflicts with surface cover");
            }else if(high!=water[i]-1||low<=ground[i]||water[i]==TerrainShape.SEA_LEVEL&&floatingIce.top(x,z)!=Integer.MIN_VALUE||canopy.topY(x,z)!=Integer.MIN_VALUE)throw new IllegalArgumentException("Freshwater lid requires exposed source water and no decorations");
        }
        transitions.addAll(perennialIce.boundaries());
        top=Math.max(top,floatingIce.highest());transitions.addAll(floatingIce.sections());
        top=Math.max(top,canopy.highest());transitions.addAll(canopy.sections());
        highest=top; boundaries=Collections.unmodifiableSet(transitions);randomTickSections=Collections.unmodifiableSet(ticking);
    }
    private TerrainProfile(TerrainProfile base,DripstoneLayer layer){this(base,layer,base.litCaves);}
    private TerrainProfile(TerrainProfile base,DripstoneLayer layer,LitCaveLayer growth){this(base,layer,growth,base.caveBiomes);}
    private TerrainProfile(TerrainProfile base,DripstoneLayer layer,LitCaveLayer growth,CaveBiomeLayer caveBiomes){
        this.caveBiomes=caveBiomes;
        if(layer!=base.dripstone)layer.validate(base);if(growth!=base.litCaves)growth.validate(base);litCaves=growth;floor=base.floor;ceiling=base.ceiling;underground=base.underground;ores=base.ores;dripstone=layer;decorations=layer==base.dripstone&&growth==base.litCaves?base.decorations:layer.withLitCaves(growth);
        ground=base.ground;water=base.water;rockGround=base.rockGround;scree=base.scree;reedbed=base.reedbed;riverMargin=base.riverMargin;sandyShore=base.sandyShore;meadow=base.meadow;woodland=base.woodland;steppe=base.steppe;boreal=base.boreal;warm=base.warm;coldSteppe=base.coldSteppe;desert=base.desert;biomes=base.biomes;states=base.states;highest=base.highest;highestGround=base.highestGround;lowestGround=base.lowestGround;highestRock=base.highestRock;lowestRock=base.lowestRock;floatingIce=base.floatingIce;perennialIce=base.perennialIce;canopy=base.canopy;
        var b=new TreeSet<>(base.boundaries);b.addAll(layer.sections());b.addAll(growth.sections());boundaries=Set.copyOf(b);var t=new TreeSet<>(base.randomTickSections);t.addAll(layer.ticking());randomTickSections=Set.copyOf(t);
    }
    public TerrainProfile withDripstone(DripstoneLayer layer){if(layer==dripstone)return this;var base=dripstone.enabled()?withLayers(underground,ores).withLitCaves(litCaves):this;return layer.enabled()?new TerrainProfile(base,layer):base;}
    public TerrainProfile withLitCaves(LitCaveLayer layer){if(layer==litCaves)return this;var base=litCaves.enabled()?withLayers(underground,ores).withDripstone(dripstone):this;return layer.enabled()?new TerrainProfile(base,base.dripstone,layer):base;}
    public CaveBiomeLayer caveBiomes(){return caveBiomes;}
    public TerrainProfile withCaveBiomes(CaveBiomeLayer layer){if(layer.enabled())layer.validate(this);return new TerrainProfile(this,dripstone,litCaves,layer);}
    public LitCaveLayer litCaves(){return litCaves;}
    public int ceiling(){return ceiling;}
    public DripstoneLayer dripstone(){return dripstone;}
    public Holder<Biome> biome(int x,int absoluteQuartY,int z){return caveBiomes.biome(x,absoluteQuartY,z,habitatGround((x&3)*4,(z&3)*4),dripstone.biome(x,absoluteQuartY,z,habitatGround((x&3)*4,(z&3)*4),biomes.get(x&3,absoluteQuartY&3,z&3)));}
    private PalettedContainerRO<Holder<Biome>> sectionBiomes(int sy){
        if(!dripstone.mayHaveBiome(sy,lowestRock,highestRock)&&!caveBiomes.mayHaveBiome(sy,lowestRock,highestRock))return biomes;var result=biomes.recreate();boolean changed=false;
        for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++){var value=biome(x,(sy<<2)+y,z);result.set(x,y,z,value);changed|=!value.equals(biomes.get(x,y,z));}return changed?blockbranch.realistic.UniformSnapshots.compactBiomes(result):biomes;
    }
    public OreLayer ores(){return ores;}
    public boolean oresMayAffect(int sy){return ores.mayAffect(sy,lowestRock,highestRock);}
    public TerrainProfile withLayers(UndergroundLayer layer,OreLayer oreLayer){return new TerrainProfile(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,canopy,floatingIce,biomes,states,layer,oreLayer,perennialIce);}
    public TerrainProfile withUnderground(UndergroundLayer layer) {
        return new TerrainProfile(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,canopy,floatingIce,biomes,states,layer,ores,perennialIce);
    }
    public TerrainProfile withPerennialIce(PerennialIceLayer layer){
        if(layer==perennialIce)return this;
        var result=new TerrainProfile(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert,canopy,floatingIce,biomes,states,underground,ores,layer);
        return result.withDripstone(dripstone).withLitCaves(litCaves).withCaveBiomes(caveBiomes);
    }
    public PerennialIceLayer perennialIce(){return perennialIce;}
    public UndergroundLayer underground(){return underground;}
    public boolean undergroundMayAffect(int sectionY){return underground.mayAffect(sectionY,highestRock);}
    public int highest() { return highest; }
    /** Immutable bare terrain datum, independent of surface block type and canopy. */
    public int groundHeight(int x,int z){return ground[(z&15)*16+(x&15)];}
    private int rockHeight(int x,int z){return rockGround[(z&15)*16+(x&15)];}
    /** Datum for dripstone and sulfur habitat windows, formations and cave labels: the rock surface, which is the
     * ice-free bed under a grounded continental sheet (new ice is overburden) and exactly {@link #groundHeight}
     * everywhere else, including legacy alpine caps and every profile without perennial ice. */
    public int habitatGround(int x,int z){return rockHeight(x,z);}
    public Set<Integer> randomTickSections(){return randomTickSections;}
    public FloatingIceLayer floatingIce(){return floatingIce;}
    public CanopyLayer canopy() { return canopy; }
    public PalettedContainerRO<Holder<Biome>> biomes() { return biomes; }
    public BlockState state(int x,int y,int z) {
        return state(x,y,z,null);
    }
    /**
     * Scratch for evaluating every block of one section: lazily built cave lattice tables and
     * ore vein masks that give the same answers as the per-block path. Single-threaded.
     */
    private final class SectionScratch {
        private final int sectionY;
        private UndergroundStructure.Section caves;
        private SectionVeins veins;
        private boolean cavesReady,veinsReady;
        SectionScratch(int sectionY){this.sectionY=sectionY;}
        UndergroundStructure.Section caves(){if(!cavesReady){caves=underground.section(sectionY);cavesReady=true;}return caves;}
        SectionVeins veins(){if(!veinsReady){veins=ores.sectionVeins(sectionY,rockGround);veinsReady=true;}return veins;}
    }
    private BlockState state(int x,int y,int z,SectionScratch scratch) {
        int i=(z&15)*16+(x&15);
        var permanent=perennialIce.state(x,y,z);if(permanent!=null)return permanent;
        // Prescribed perennial bodies bury old openings; do not carve a new
        // approach or ore/decorative feature into their stored solid ice.
        if(blockbranch.realistic.LandIceField.encoded(coldSteppe[i])&&y<=ground[i]&&y>ground[i]-coldLayerDepth(coldSteppe[i]))return material(floor,ceiling,y,ground[i],water[i],0,0,0,0,0,0,0,0,0,coldSteppe[i],0);
        var ice=floatingIce.state(x,y,z);if(ice!=null)return ice;
        var tree=canopy.state(x,y,z);
        var value=tree!=null ? tree : material(floor,ceiling,y,ground[i],water[i],scree[i],reedbed[i],riverMargin[i],sandyShore[i],meadow[i],woodland[i],steppe[i],boreal[i],warm[i],coldSteppe[i],desert[i]);
        if(underground.approachClears(x,y,z,rockGround[i])&&UndergroundLayer.approachMaterial(value))value=Blocks.AIR.defaultBlockState();
        if((value.is(Blocks.STONE)||value.is(Blocks.DEEPSLATE))&&(scratch==null?underground.voidAt(x,y,z,rockGround[i]):underground.voidAt(x,y,z,rockGround[i],scratch.caves())))value=Blocks.AIR.defaultBlockState();
        return decorations.state(x,y,z,ores.state(x,y,z,rockGround[i],value,underground.enabled(),scratch==null||!ores.enabled()?null:scratch.veins()));
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree) {
        return material(floor,ceiling,y,ground,water,scree,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm,int coldSteppe) {
        return material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,0);
    }
    public static BlockState material(int floor,int ceiling,int y,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm,int coldSteppe,int desert){
        if(y<floor || y>=ceiling) return Blocks.AIR.defaultBlockState();
        if(y==floor) return Blocks.BEDROCK.defaultBlockState();
        if(y<=ground) {
            if(blockbranch.realistic.Badlands.encoded(desert)&&y>ground-64){
                int cap=blockbranch.realistic.Badlands.cap(desert);
                if(y>ground-2&&cap>0)return(cap<=2?Blocks.RED_SAND:Blocks.COARSE_DIRT).defaultBlockState();
                return switch(blockbranch.realistic.Badlands.stratum(desert,y)){
                    case 1->Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.ORANGE).defaultBlockState();case 2->Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState();
                    case 3->Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.BROWN).defaultBlockState();case 4->Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState();
                    case 5->Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY).defaultBlockState();default->Blocks.TERRACOTTA.defaultBlockState();
                };
            }
            if(desert>=8&&desert<32&&desert!=16&&y>ground-desertLayerDepth(desert)){
                if(desert==14&&y==ground)return Blocks.GRAVEL.defaultBlockState();
                return(y>ground-desertSandDepth(desert)&&desert!=14&&desert!=15?Blocks.SAND:Blocks.SANDSTONE).defaultBlockState();
            }
            if(desert>0&&desert<8&&y>ground-2){
                if(y<ground)return(desert>=4?Blocks.DIRT:Blocks.SAND).defaultBlockState();
                return switch(desert){case 1->blockbranch.realistic.DesertContent.PAVEMENT.defaultBlockState();case 2->Blocks.GRAVEL.defaultBlockState();case 3->Blocks.SAND.defaultBlockState();case 6->Blocks.GRASS_BLOCK.defaultBlockState();default->Blocks.COARSE_DIRT.defaultBlockState();};
            }
            if(blockbranch.realistic.LandIceField.encoded(coldSteppe)&&y>ground-coldLayerDepth(coldSteppe))return(y==ground?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE).defaultBlockState();
            if(blockbranch.realistic.ColdUplands.encoded(coldSteppe)&&y>ground-coldLayerDepth(coldSteppe)){
                if(coldSteppe>=16)return(blockbranch.realistic.ColdUplands.snowCap(coldSteppe)&&y==ground?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE).defaultBlockState();
                if(coldSteppe<=10)return Blocks.GRAVEL.defaultBlockState();
                return(y==ground?(coldSteppe==13?Blocks.GRASS_BLOCK:Blocks.COARSE_DIRT):Blocks.DIRT).defaultBlockState();
            }
            if(coldSteppe>0&&coldSteppe<8 && y>ground-3){
                if(y==ground-2)return Blocks.GRAVEL.defaultBlockState();
                if(y==ground-1)return Blocks.DIRT.defaultBlockState();
                return (coldSteppe==2?Blocks.GRAVEL:coldSteppe==3||coldSteppe==4||coldSteppe==5?Blocks.GRASS_BLOCK:Blocks.COARSE_DIRT).defaultBlockState();
            }
            if(warm>0 && y>ground-3) {
                if(y<ground)return Blocks.DIRT.defaultBlockState();
                return (warm==2 || warm==4 || warm==9 || warm==10 || warm==11 ? Blocks.GRASS_BLOCK : warm==1 || warm==8 || warm==12 ? Blocks.COARSE_DIRT : Blocks.PODZOL).defaultBlockState();
            }
            if(boreal>0 && y>ground-3) {
                if(boreal==13||boreal==14){if(y<ground)return Blocks.DIRT.defaultBlockState();return (boreal==13?Blocks.PODZOL:Blocks.GRASS_BLOCK).defaultBlockState().setValue(net.minecraft.world.level.block.SnowyBlock.SNOWY,true);}
                if(boreal==10)return (y==ground?Blocks.GRAVEL:Blocks.COARSE_DIRT).defaultBlockState();
                if(y<ground) return Blocks.DIRT.defaultBlockState();
                return (boreal==2 ? Blocks.COARSE_DIRT : boreal==3 || boreal==6 ? Blocks.MOSS_BLOCK : boreal==8 || boreal==9 || boreal==11 || boreal==12 ? Blocks.GRASS_BLOCK : Blocks.PODZOL).defaultBlockState();
            }
            if(steppe>0 && y>ground-3) {
                if(y==ground-2) return Blocks.GRAVEL.defaultBlockState();
                if(y==ground-1) return Blocks.DIRT.defaultBlockState();
                return (steppe==2 ? Blocks.GRAVEL : steppe==3 || steppe==5 ? Blocks.GRASS_BLOCK : Blocks.COARSE_DIRT).defaultBlockState();
            }
            if(woodland>0 && y>ground-3) {
                woodland=blockbranch.realistic.LeafLitter.woodlandBase(woodland);
                if(blockbranch.realistic.BambooSoil.encoded(woodland)) {
                    if(y<ground)return Blocks.DIRT.defaultBlockState();
                    return switch(blockbranch.realistic.BambooSoil.soil(woodland)) {
                        case 0 -> Blocks.DIRT.defaultBlockState();
                        case 1 -> Blocks.COARSE_DIRT.defaultBlockState();
                        case 3 -> Blocks.GRASS_BLOCK.defaultBlockState();
                        default -> Blocks.ROOTED_DIRT.defaultBlockState();
                    };
                }
                if(woodland==53)return(y==ground?Blocks.ROOTED_DIRT:Blocks.DIRT).defaultBlockState();
                if(woodland>=30)return(y==ground?Blocks.GRASS_BLOCK:Blocks.DIRT).defaultBlockState();
                if(woodland>=13){if(y==ground-2)return Blocks.CLAY.defaultBlockState();if(y<ground)return Blocks.MUD.defaultBlockState();return(woodland>=25?Blocks.GRASS_BLOCK:woodland==16||woodland==20||woodland>=21?Blocks.MOSS_BLOCK:woodland==19?Blocks.ROOTED_DIRT:Blocks.MUD).defaultBlockState();}
                if(y<ground) return Blocks.DIRT.defaultBlockState();
                if(woodland>=8)return (woodland==11||woodland==12?Blocks.GRASS_BLOCK:Blocks.ROOTED_DIRT).defaultBlockState();
                return (woodland==2 ? Blocks.COARSE_DIRT : woodland==3 || woodland==6 ? Blocks.GRASS_BLOCK : Blocks.PODZOL).defaultBlockState();
            }
            if(meadow>0 && y>ground-3) return (y==ground ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
            if(sandyShore>=3 && y>ground-5)return(sandyShore==4&&y==ground?Blocks.GRAVEL:Blocks.STONE).defaultBlockState();
            if(sandyShore>0 && y>ground-5) {
                if(y<=ground-3) return Blocks.SANDSTONE.defaultBlockState();
                if(sandyShore==2 && y>ground-2) return blockbranch.realistic.SandyShoreContent.ROOT_BOUND_SAND.defaultBlockState();
                return Blocks.SAND.defaultBlockState();
            }
            if(riverMargin>=13&&y>ground-2)return (riverMargin==13?Blocks.MUD:Blocks.SAND).defaultBlockState();
            if(riverMargin>=5&&riverMargin<=12&&riverMargin!=9&&y>ground-2)return lakeBed(riverMargin);
            if(riverMargin>0&&riverMargin<=4 && y>ground-2) return switch(riverMargin) {
                case 1 -> blockbranch.realistic.RiverMarginContent.PACKED_RIVER_SHINGLE.defaultBlockState();
                case 2 -> Blocks.GRAVEL.defaultBlockState();
                default -> (y==ground ? Blocks.COARSE_DIRT : Blocks.GRAVEL).defaultBlockState();
            };
            if(reedbed>0 && y>ground-2) return Blocks.MUD.defaultBlockState();
            if(y>ground-scree) return blockbranch.realistic.ScreeContent.LOOSE_SCREE.defaultBlockState();
            return (deepslateAt(y) ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
        }
        if(desertPlantHeight(desert)>0&&y>ground&&y<=ground+desertPlantHeight(desert))return desertPlant(desert);
        if(coldPlantPresent(coldSteppe) && y==ground+1)return coldPlant(coldSteppe);
        if(warmPlantPresent(warm) && y==ground+1)return warmPlant(warm);
        if(borealPlantPresent(boreal) && y==ground+1) return borealPlant(boreal);
        if(steppePlantPresent(steppe) && y==ground+1) return steppePlant(steppe);
        if(y>ground&&y<=ground+woodlandPlantHeight(woodland))return woodlandPlant(woodland,y-ground);
        if(y>ground && y<=ground+meadowPlantHeight(meadow)) return meadowPlant(meadow,y-ground);
        if(sandyShore==2 && y==ground+1) return blockbranch.realistic.SandyShoreContent.BEACH_GRASS.defaultBlockState();
        if(riverMargin==4 && y==ground+1) return Blocks.SHORT_GRASS.defaultBlockState();
        if(lakePlantPresent(riverMargin)&&y==ground+1)return Blocks.SEAGRASS.defaultBlockState();
        if(reedbed>1 && y<=ground+reedbed) return blockbranch.realistic.ReedbedContent.LAKE_REED.defaultBlockState()
            .setValue(blockbranch.realistic.LakeReedBlock.WATERLOGGED,y<water)
            .setValue(blockbranch.realistic.LakeReedBlock.TIP,y==ground+reedbed);
        return (y<water ? Blocks.WATER : Blocks.AIR).defaultBlockState();
    }
    public static boolean lakePlantPresent(int cover){return cover>=10&&cover<=12;}
    private static BlockState lakeBed(int cover){return switch(cover>=10?cover-5:cover){case 5->Blocks.MUD.defaultBlockState();case 6->Blocks.CLAY.defaultBlockState();case 7->Blocks.SAND.defaultBlockState();case 8->Blocks.GRAVEL.defaultBlockState();default->Blocks.STONE.defaultBlockState();};}
    public static int desertSandDepth(int c){if(blockbranch.realistic.Badlands.encoded(c))return blockbranch.realistic.Badlands.cap(c)>0?2:0;return c>=8&&c<=12?3:c==13||c>=17&&c<=20?1:0;}
    public static int desertLayerDepth(int c){if(blockbranch.realistic.Badlands.encoded(c))return 64;return c==16?0:c>=8&&c<=12?5:c==13||c>=17&&c<=20?3:c>0?2:0;}
    public static int desertPlantHeight(int c){if(blockbranch.realistic.Badlands.encoded(c)){int cap=blockbranch.realistic.Badlands.cap(c);return cap==2||cap>=4?1:0;}return c>=10&&c<=12?c-9:c>=18&&c<=20?c-17:c>=5&&c<=7||c==9||c==17?1:0;}
    private static BlockState desertPlant(int c){if(blockbranch.realistic.Badlands.encoded(c))return(blockbranch.realistic.Badlands.cap(c)==4?Blocks.SHORT_DRY_GRASS:Blocks.DEAD_BUSH).defaultBlockState();return switch(c){case 5,9,17->Blocks.DEAD_BUSH.defaultBlockState();case 10,11,12,18,19,20->Blocks.CACTUS.defaultBlockState();case 6->Blocks.SHORT_DRY_GRASS.defaultBlockState();case 7->blockbranch.realistic.SteppeContent.SILVER_SHRUB.defaultBlockState().setValue(blockbranch.realistic.SteppeShrubBlock.CLIPPED,false);default->Blocks.AIR.defaultBlockState();};}
    private static int coldLayerDepth(int c){return blockbranch.realistic.LandIceField.encoded(c)?blockbranch.realistic.LandIceField.depth(c):blockbranch.realistic.ColdUplands.encoded(c)?blockbranch.realistic.ColdUplands.depth(c):3;}
    private static boolean coldPlantPresent(int c){return c==12||c==13||c==3 || c==4 || c==6 || c==7;}
    private static BlockState coldPlant(int c){return switch(c){case 12->Blocks.SHORT_DRY_GRASS.defaultBlockState();case 13->Blocks.SHORT_GRASS.defaultBlockState();case 3->Blocks.SHORT_DRY_GRASS.defaultBlockState();case 4->blockbranch.realistic.MeadowContent.MEADOW_FESCUE.defaultBlockState().setValue(blockbranch.realistic.MeadowFescueBlock.CROPPED,false);case 6->blockbranch.realistic.SteppeContent.SILVER_SHRUB.defaultBlockState().setValue(blockbranch.realistic.SteppeShrubBlock.CLIPPED,false);case 7->Blocks.DEAD_BUSH.defaultBlockState();default->Blocks.AIR.defaultBlockState();};}
    private static final net.minecraft.core.Direction[] LITTER_FACING={net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.WEST};
    /** One of the vanilla leaf-litter block's 16 facing x segment states; index 0 is the original default state. */
    public static BlockState leafLitterState(int index){
        if(index<0||index>=blockbranch.realistic.LeafLitter.STATES)throw new IllegalArgumentException("Leaf-litter variant");
        return Blocks.LEAF_LITTER.defaultBlockState().setValue(net.minecraft.world.level.block.LeafLitterBlock.FACING,LITTER_FACING[index&3]).setValue(net.minecraft.world.level.block.SegmentableBlock.AMOUNT,1+(index>>2));
    }
    private static boolean warmPlantPresent(int cover){cover=blockbranch.realistic.LeafLitter.warmBase(cover);return cover>=3 && cover<=8 && cover!=7 || cover==10 || cover==11;}
    private static BlockState warmPlant(int cover){if(blockbranch.realistic.LeafLitter.warmVariant(cover))return leafLitterState(blockbranch.realistic.LeafLitter.warmIndex(cover));return switch(cover){case 3->Blocks.LEAF_LITTER.defaultBlockState();case 4->Blocks.SHORT_DRY_GRASS.defaultBlockState();case 5->Blocks.FERN.defaultBlockState();case 6->Blocks.BROWN_MUSHROOM.defaultBlockState();case 8->Blocks.DEAD_BUSH.defaultBlockState();case 10->Blocks.SHORT_DRY_GRASS.defaultBlockState();case 11->Blocks.SHORT_GRASS.defaultBlockState();default->Blocks.AIR.defaultBlockState();};}
    public static boolean borealPlantPresent(int cover){return cover>=4&&cover<=9||cover==11||cover==blockbranch.realistic.BorealSurface.CARPET;}
    private static BlockState borealPlant(int cover) {
        return switch(cover) { case 4->Blocks.FERN.defaultBlockState();case 5->Blocks.BROWN_MUSHROOM.defaultBlockState();case 6,blockbranch.realistic.BorealSurface.CARPET->Blocks.MOSS_CARPET.defaultBlockState();case 7->Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE,3);case 8->Blocks.SHORT_GRASS.defaultBlockState();case 9->Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE,1);case 11->blockbranch.realistic.MeadowContent.MEADOW_FESCUE.defaultBlockState().setValue(blockbranch.realistic.MeadowFescueBlock.CROPPED,false);default->Blocks.AIR.defaultBlockState(); };
    }
    private static boolean steppePlantPresent(int cover) { return cover==3 || cover==4 || cover==6; }
    private static BlockState steppePlant(int cover) {
        return switch(cover) { case 3->Blocks.SHORT_DRY_GRASS.defaultBlockState();case 4->blockbranch.realistic.SteppeContent.SILVER_SHRUB.defaultBlockState().setValue(blockbranch.realistic.SteppeShrubBlock.CLIPPED,false);case 6->Blocks.DEAD_BUSH.defaultBlockState();default->Blocks.AIR.defaultBlockState(); };
    }
    private static int woodlandPlantHeight(int cover){return cover==53?2:woodlandPlantPresent(cover)?1:0;}
    private static BlockState woodlandPlant(int cover,int stem){return cover==53?Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF,stem==2?net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER:net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER):woodlandPlant(cover);}
    private static boolean woodlandPlantPresent(int cover){cover=blockbranch.realistic.LeafLitter.woodlandBase(cover);cover=blockbranch.realistic.BambooSoil.vegetation(cover);return cover==53|| cover>=4&&cover<=7||cover==9||cover==10||cover==12||cover==14||cover==15||cover==17||cover==18||cover==20||cover>=21&&cover<=24||cover>=26&&cover<=52;}
    private static BlockState woodlandPlant(int cover) {
        if(blockbranch.realistic.LeafLitter.woodlandVariant(cover))return leafLitterState(blockbranch.realistic.LeafLitter.woodlandIndex(cover));
        cover=blockbranch.realistic.BambooSoil.vegetation(cover);
        if(cover>=37){int i=cover-37;var facing=switch(i/4){case 0->net.minecraft.core.Direction.NORTH;case 1->net.minecraft.core.Direction.EAST;case 2->net.minecraft.core.Direction.SOUTH;default->net.minecraft.core.Direction.WEST;};return Blocks.PINK_PETALS.defaultBlockState().setValue(net.minecraft.world.level.block.FlowerBedBlock.AMOUNT,1+i%4).setValue(net.minecraft.world.level.block.FlowerBedBlock.FACING,facing);}
        return switch(cover) { case 32->Blocks.ALLIUM.defaultBlockState();case 33->Blocks.AZURE_BLUET.defaultBlockState();case 34->Blocks.POPPY.defaultBlockState();case 35->Blocks.CORNFLOWER.defaultBlockState();case 36->Blocks.LILY_OF_THE_VALLEY.defaultBlockState();case 30->Blocks.DANDELION.defaultBlockState();case 31->Blocks.OXEYE_DAISY.defaultBlockState();case 20->Blocks.BLUE_ORCHID.defaultBlockState();case 4,14,21,26->Blocks.FERN.defaultBlockState();case 5,15,22,27->Blocks.BROWN_MUSHROOM.defaultBlockState();case 6,17,23,28->Blocks.SHORT_GRASS.defaultBlockState();case 7,9,18,24,29->Blocks.LEAF_LITTER.defaultBlockState();case 10->Blocks.FERN.defaultBlockState();case 12->Blocks.SHORT_GRASS.defaultBlockState();default->Blocks.AIR.defaultBlockState(); };
    }
    public static int meadowPlantHeight(int cover){return cover==9||cover==10?2:cover>=2&&cover<=6||cover==8||cover>=11&&cover<=13?1:0;}
    private static BlockState meadowPlant(int cover,int stem) {
        return switch(cover) {
            case 11 -> Blocks.ALLIUM.defaultBlockState();
            case 12 -> Blocks.AZURE_BLUET.defaultBlockState();
            case 13 -> Blocks.POPPY.defaultBlockState();
            case 2,8 -> Blocks.SHORT_GRASS.defaultBlockState();
            case 10 -> Blocks.SUNFLOWER.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,stem==2?net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER:net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            case 9 -> Blocks.TALL_GRASS.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,stem==2?net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER:net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            case 3 -> blockbranch.realistic.MeadowContent.MEADOW_FESCUE.defaultBlockState().setValue(blockbranch.realistic.MeadowFescueBlock.CROPPED,false);
            case 4 -> Blocks.OXEYE_DAISY.defaultBlockState();
            case 5 -> Blocks.CORNFLOWER.defaultBlockState();
            case 6 -> Blocks.DANDELION.defaultBlockState();
            default -> Blocks.AIR.defaultBlockState();
        };
    }
    // Natural spawn surface per column. The profile is immutable; racing initializations or
    // entry computations may duplicate work but always store the same value. MIN_VALUE marks an unset entry (top() never returns it).
    private volatile int[] spawnTops;
    /** {@code top(MOTION_BLOCKING_NO_LEAVES, x, z, ColumnLimits.MAX_Y-1)}, memoized for spawn attempts. */
    public int spawnSurfaceTop(int x,int z){
        var tops=spawnTops;
        if(tops==null){tops=new int[256];Arrays.fill(tops,Integer.MIN_VALUE);spawnTops=tops;}
        int i=(z&15)*16+(x&15),value=tops[i];
        if(value==Integer.MIN_VALUE)tops[i]=value=top(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque(),x,z,ColumnLimits.MAX_Y-1);
        return value;
    }
    /** Highest matching Y plus one, with an inclusive query limit (like ColumnQueries). */
    public int top(Predicate<BlockState> predicate,int x,int z,int limit) {return dripstone.top(predicate,x,z,limit,oreTop(predicate,x,z,limit),y->state(x,y,z));}
    private int oreTop(Predicate<BlockState> predicate,int x,int z,int limit) {
        int result=shallowOreTop(predicate,x,z,limit);if(!ores.deepAffectsPredicate(predicate))return result;
        int top=Math.min(limit,rockHeight(x,z)-blockbranch.metallurgy.UraniumVeins.MIN_DEPTH),bottom=Math.max(floor+1,rockHeight(x,z)-blockbranch.metallurgy.UraniumVeins.MAX_DEPTH);
        if(top<bottom||result>top+1)return result;
        // Skip the untouched gap; ordinary solid/fluid height predicates agree with their hosts.
        for(int y=top;y>=bottom;y--)if(predicate.test(state(x,y,z)))return y+1;
        return withoutOreTop(predicate,x,z,bottom-1);
    }
    private int shallowOreTop(Predicate<BlockState> predicate,int x,int z,int limit) {
        int original=withoutOreTop(predicate,x,z,limit);if(!ores.enabled())return original;
        int top=Math.min(limit,rockHeight(x,z)-OreLayer.MIN_DEPTH),bottom=Math.max(floor+1,rockHeight(x,z)-OreLayer.MAX_DEPTH);
        if(top<bottom||original>top+1||!ores.affectsPredicate(predicate))return original;
        for(int y=top;y>=bottom;y--)if(predicate.test(state(x,y,z)))return y+1;
        return withoutOreTop(predicate,x,z,bottom);
    }
    private int withoutOreTop(Predicate<BlockState> predicate,int x,int z,int limit) {
        int original=legacyTop(predicate,x,z,limit);
        int roof=rockHeight(x,z)-blockbranch.realistic.UndergroundStructure.ROOF_THICKNESS;
        if(!underground.enabled()||original>roof&&!underground.hasApproach())return original;
        int start=predicate.test(Blocks.AIR.defaultBlockState())?Math.min(limit,underground.hasApproach()?original-1:roof-1):original-1;
        if(start<floor)return original;
        for(int y=start;y>=floor;y--)if(predicate.test(state(x,y,z)))return y+1;
        return ColumnLimits.MIN_Y;
    }
    private int legacyTop(Predicate<BlockState> predicate,int x,int z,int limit) {
        int i=(z&15)*16+(x&15);
        int low=perennialIce.bottom(x,z),high=perennialIce.top(x,z);
        if(low!=Integer.MIN_VALUE&&perennialIce.grounded(x,z)&&limit>=low){
            int y=Math.min(limit,high);if(y==high&&predicate.test(Blocks.SNOW_BLOCK.defaultBlockState()))return y+1;
            if(y==high)y--;if(y>=low&&predicate.test(Blocks.PACKED_ICE.defaultBlockState()))return y+1;
            limit=Math.min(limit,low-1);
        }
        int overlay=Math.max(Math.max(canopy.topY(x,z),floatingIce.top(x,z)),perennialIce.grounded(x,z)?Integer.MIN_VALUE:high);
        if(overlay!=Integer.MIN_VALUE && limit>ground[i]) {
            // Submerged overlays must not hide the source-water surface above them.
            if(water[i]>ground[i]){int fluidTop=Math.min(limit,water[i]-1);if(fluidTop>overlay&&predicate.test(Blocks.WATER.defaultBlockState()))return fluidTop+1;}
            for(int y=Math.min(limit,overlay);y>ground[i];y--) if(predicate.test(state(x,y,z))) return y+1;
            limit=Math.min(limit,ground[i]);
        }
        return top(floor,ceiling,ground[i],water[i],scree[i],reedbed[i],riverMargin[i],sandyShore[i],meadow[i],woodland[i],steppe[i],boreal[i],warm[i],coldSteppe[i],desert[i],predicate,limit);
    }
    /** A bounded surface scan (at most four stems), then analytic water/rock intervals. */
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm,int coldSteppe,Predicate<BlockState> predicate,int limit) {
        return top(floor,ceiling,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,0,predicate,limit);
    }
    public static int top(int floor,int ceiling,int ground,int water,int scree,int reedbed,int riverMargin,int sandyShore,int meadow,int woodland,int steppe,int boreal,int warm,int coldSteppe,int desert,Predicate<BlockState> predicate,int limit){
        int end=Math.min(limit,ceiling-1);
        if(desertPlantHeight(desert)>0&&end>ground&&predicate.test(desertPlant(desert)))return Math.min(end,ground+desertPlantHeight(desert))+1;
        if(coldPlantPresent(coldSteppe) && end>ground && predicate.test(coldPlant(coldSteppe)))return ground+2;
        if(warmPlantPresent(warm) && end>ground && predicate.test(warmPlant(warm)))return ground+2;
        if(borealPlantPresent(boreal) && end>ground && predicate.test(borealPlant(boreal))) return ground+2;
        if(steppePlantPresent(steppe) && end>ground && predicate.test(steppePlant(steppe))) return ground+2;
        for(int stem=Math.min(woodlandPlantHeight(woodland),end-ground);stem>0;stem--)if(predicate.test(woodlandPlant(woodland,stem)))return ground+stem+1;
        for(int stem=Math.min(meadowPlantHeight(meadow),end-ground);stem>0;stem--)if(predicate.test(meadowPlant(meadow,stem)))return ground+stem+1;
        if(sandyShore==2 && end>ground && predicate.test(blockbranch.realistic.SandyShoreContent.BEACH_GRASS.defaultBlockState())) return ground+2;
        if(riverMargin==4 && end>ground && predicate.test(Blocks.SHORT_GRASS.defaultBlockState())) return ground+2;
        if(reedbed>1) for(int stem=Math.min(end,ground+reedbed);stem>ground;stem--)
            if(predicate.test(material(floor,ceiling,stem,ground,water,scree,reedbed,riverMargin,sandyShore))) return stem+1;
        int y=Math.min(end,water==Integer.MIN_VALUE ? Integer.MIN_VALUE : water-1);
        if(y>ground && y>=floor && predicate.test(material(floor,ceiling,y,ground,water,scree,reedbed,riverMargin,sandyShore))) return y+1;
        if(lakePlantPresent(riverMargin)&&end>ground&&predicate.test(Blocks.SEAGRASS.defaultBlockState()))return ground+2;
        y=Math.min(end,ground);
        if(blockbranch.realistic.Badlands.encoded(desert)){
            for(int surface=y;surface>ground-64;surface--)if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert)))return surface+1;
            y=Math.min(y,ground-64);
        }else if(desert>=8){
            for(int surface=y;surface>=ground-4;surface--)if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert)))return surface+1;
            y=Math.min(y,ground-5);
        }else if(desert>0){
            for(int surface=y;surface>=ground-1;surface--)if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert)))return surface+1;
            y=Math.min(y,ground-2);
        }
        if(coldSteppe>0){
            for(int surface=y;surface>ground-coldLayerDepth(coldSteppe);surface--)if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm,coldSteppe)))return surface+1;
            y=Math.min(y,ground-coldLayerDepth(coldSteppe));
        }
        if(warm>0) {
            for(int surface=y;surface>=ground-2;surface--)if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal,warm)))return surface+1;
            y=Math.min(y,ground-3);
        }
        if(boreal>0) {
            for(int surface=y;surface>=ground-2;surface--)
                if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe,boreal))) return surface+1;
            y=Math.min(y,ground-3);
        }
        if(steppe>0) {
            for(int surface=y;surface>=ground-2;surface--)
                if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland,steppe))) return surface+1;
            y=Math.min(y,ground-3);
        }
        if(woodland>0) {
            for(int surface=y;surface>=ground-2;surface--)
                if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow,woodland))) return surface+1;
            y=Math.min(y,ground-3);
        }
        if(meadow>0) {
            for(int surface=y;surface>=ground-2;surface--)
                if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore,meadow))) return surface+1;
            y=Math.min(y,ground-3);
        }
        if(sandyShore>0) {
            for(int surface=y;surface>=ground-4;surface--)
                if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin,sandyShore))) return surface+1;
            y=Math.min(y,ground-5);
        }
        if(riverMargin>0) {
            for(int surface=y;surface>=ground-1;surface--)
                if(predicate.test(material(floor,ceiling,surface,ground,water,scree,reedbed,riverMargin))) return surface+1;
            y=Math.min(y,ground-2);
        }
        if(reedbed>0 && y>ground-2 && predicate.test(Blocks.MUD.defaultBlockState())) return y+1;
        if(scree>0 && y>ground-scree && predicate.test(blockbranch.realistic.ScreeContent.LOOSE_SCREE.defaultBlockState())) return y+1;
        y=Math.min(y,ground-(reedbed>0 ? 2 : scree));
        if(y>=Math.max(floor+1,0) && predicate.test(Blocks.STONE.defaultBlockState())) return y+1;
        y=Math.min(y,-1);
        if(y>floor && predicate.test(Blocks.DEEPSLATE.defaultBlockState())) return y+1;
        if(end>=floor && predicate.test(Blocks.BEDROCK.defaultBlockState())) return floor+1;
        return ColumnLimits.MIN_Y;
    }
    /** Only discontinuities are needed for broad height and sky-source searches. */
    public Set<Integer> boundaries() {
        return boundaries;
    }
    public LevelChunkSection generate(int sectionY) {
        return new LevelChunkSection(generateStates(sectionY),sectionBiomes(sectionY));
    }
    /** Caller-owned block palette; snapshot copies do not need native section metadata or biomes. */
    public PalettedContainer<BlockState> generateStates(int sectionY) {
        // Reuse only exact immutable-base occupancy. Never scan merely to decide
        // whether to scan again; unknown/nonempty sections retain the full path.
        var empty=copyEmptyStatesIfKnown(sectionY);
        if(empty!=null)return empty;
        int bottom=sectionY<<4;
        var scratch=new SectionScratch(sectionY);
        var first=state(0,bottom,0,scratch);
        var blocks=new PalettedContainer<>(first,states);
        // The palette already contains first at every position. One pass both
        // detects variation and fills it, without evaluating a prefix twice.
        for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
            var value=state(x,bottom+y,z,scratch);
            if(value!=first)blocks.set(x,y,z,value);
        }
        return blocks;
    }
    /** Mushrooms emit light, but the supported plant palette is not section occupancy. */
    private boolean surfaceMushroom(int sectionY) {
        long bottom=(long)sectionY<<4;
        if(bottom>highestGround+1L||bottom+15<lowestGround+1L)return false;
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int y=ground[z*16+x]+1;
            if((y>>4)==sectionY&&state(x,y,z).is(Blocks.BROWN_MUSHROOM))return true;
        }
        return false;
    }
    private java.util.LinkedHashMap<Integer,Boolean> occupancyCache;
    private static final int OCCUPANCY_CACHE_LIMIT=256;
    private Boolean trivialEmptiness(int y){
        if((y<<4)>highest||(y<<4)+15<floor)return true;
        return !undergroundMayAffect(y)?Boolean.FALSE:null;
    }
    /** Exact immutable-base occupancy, shared by transient views and packet production. */
    public boolean sectionIsEmpty(int y){
        var known=knownSectionEmpty(y);if(known!=null)return known;
        boolean empty=scanEmptySection(y);rememberSectionEmpty(y,empty);return empty;
    }
    /** Exact cached/trivial base proof only; never triggers density evaluation. */
    public boolean sectionKnownEmpty(int y){return Boolean.TRUE.equals(knownSectionEmpty(y));}
    // One private template per used profile, never an externally rooted cache.
    private PalettedContainer<BlockState> emptyStates;
    public PalettedContainer<BlockState> copyEmptyStatesIfKnown(int y){
        if(!sectionKnownEmpty(y))return null;
        synchronized(this){
            if(emptyStates==null)emptyStates=new PalettedContainer<>(Blocks.AIR.defaultBlockState(),states);
            return emptyStates.copy();
        }
    }
    private Boolean knownSectionEmpty(int y){
        var trivial=trivialEmptiness(y);if(trivial!=null)return trivial;
        synchronized(this){return occupancyCache==null?null:occupancyCache.get(y);}
    }
    private boolean scanEmptySection(int y){
        var trivial=trivialEmptiness(y);if(trivial!=null)return trivial;
        var scratch=new SectionScratch(y);
        for(int ly=0;ly<16;ly++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(!state(x,(y<<4)+ly,z,scratch).isAir())return false;
        return true;
    }
    /** Trusted server's exact base result; never an edited section's occupancy. Not persisted. */
    public void rememberSectionEmpty(int y,boolean empty){
        if(y<(ColumnLimits.MIN_Y>>4)||y>=(ColumnLimits.MAX_Y>>4))throw new IllegalArgumentException("Occupancy section outside build bounds");
        var trivial=trivialEmptiness(y);if(trivial!=null){if(trivial!=empty)throw new IllegalArgumentException("Occupancy contradicts terrain bounds");return;}
        synchronized(this){
            if(occupancyCache==null)occupancyCache=new java.util.LinkedHashMap<>(32,.75f,true);
            var old=occupancyCache.get(y);if(old!=null&&old!=empty)throw new IllegalArgumentException("Conflicting immutable occupancy result");
            occupancyCache.put(y,empty);while(occupancyCache.size()>OCCUPANCY_CACHE_LIMIT)occupancyCache.remove(occupancyCache.keySet().iterator().next());
        }
    }
    public LevelChunkSection view(int y) { return new ProceduralSection(y); }
    private final class ProceduralSection extends LevelChunkSection {
        private final int y;
        ProceduralSection(int y) { super(new PalettedContainer<>(Blocks.AIR.defaultBlockState(),states),biomes); this.y=y; }
        @Override public BlockState getBlockState(int x,int ly,int z) { return state(x,(y<<4)+ly,z); }
        @Override public FluidState getFluidState(int x,int ly,int z) { return getBlockState(x,ly,z).getFluidState(); }
        @Override public boolean hasOnlyAir() { return sectionIsEmpty(y); }
        @Override public boolean isRandomlyTicking() { return randomTickSections.contains(y); }
        @Override public boolean maybeHas(Predicate<BlockState> p) {
            if(litCaves.hasSection(y)&&p.test(Blocks.MOSS_BLOCK.defaultBlockState()))return true;
            if(dripstone.maybeHas(y,p)||oresMayAffect(y)&&ores.maybeHas(p))return true;
            if(p.test(Blocks.SNOW_BLOCK.defaultBlockState())||p.test(Blocks.PACKED_ICE.defaultBlockState())||p.test(Blocks.RED_SAND.defaultBlockState())||p.test(Blocks.TERRACOTTA.defaultBlockState()))return true;
            for(var color:new net.minecraft.world.item.DyeColor[]{net.minecraft.world.item.DyeColor.ORANGE,net.minecraft.world.item.DyeColor.RED,net.minecraft.world.item.DyeColor.BROWN,net.minecraft.world.item.DyeColor.WHITE,net.minecraft.world.item.DyeColor.LIGHT_GRAY})if(p.test(Blocks.DYED_TERRACOTTA.pick(color).defaultBlockState()))return true;
            for(var half:net.minecraft.world.level.block.state.properties.DoubleBlockHalf.values())if(p.test(Blocks.TALL_GRASS.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,half))||p.test(Blocks.SUNFLOWER.defaultBlockState().setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF,half)))return true;
            if(p.test(Blocks.CACTUS.defaultBlockState()) || p.test(blockbranch.realistic.DesertContent.PAVEMENT.defaultBlockState()))return true;
            if(p.test(Blocks.MOSS_BLOCK.defaultBlockState()) || p.test(Blocks.MOSS_CARPET.defaultBlockState()) || p.test(Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE,3))) return true;
            if(p.test(Blocks.SHORT_DRY_GRASS.defaultBlockState()) || p.test(Blocks.DEAD_BUSH.defaultBlockState()) || p.test(blockbranch.realistic.SteppeContent.SILVER_SHRUB.defaultBlockState()) || p.test(blockbranch.realistic.SteppeContent.SILVER_SHRUB.defaultBlockState().setValue(blockbranch.realistic.SteppeShrubBlock.CLIPPED,false))) return true;
            for(var half:net.minecraft.world.level.block.state.properties.DoubleBlockHalf.values())if(p.test(Blocks.LARGE_FERN.defaultBlockState().setValue(DoublePlantBlock.HALF,half)))return true;
            if(canopy.maybeHas(y,p) || p.test(Blocks.ROOTED_DIRT.defaultBlockState()) || p.test(Blocks.PODZOL.defaultBlockState()) || p.test(Blocks.FERN.defaultBlockState()) || (p.test(Blocks.BROWN_MUSHROOM.defaultBlockState())&&surfaceMushroom(y)) || p.test(Blocks.LEAF_LITTER.defaultBlockState())) return true;
            if(p.test(Blocks.GRASS_BLOCK.defaultBlockState()) || p.test(Blocks.DIRT.defaultBlockState()) || p.test(Blocks.OXEYE_DAISY.defaultBlockState()) || p.test(Blocks.CORNFLOWER.defaultBlockState()) || p.test(Blocks.DANDELION.defaultBlockState()) || p.test(blockbranch.realistic.MeadowContent.MEADOW_FESCUE.defaultBlockState()) || p.test(blockbranch.realistic.MeadowContent.MEADOW_FESCUE.defaultBlockState().setValue(blockbranch.realistic.MeadowFescueBlock.CROPPED,false)) || p.test(Blocks.AIR.defaultBlockState()) || p.test(Blocks.STONE.defaultBlockState()) || p.test(Blocks.DEEPSLATE.defaultBlockState()) || p.test(Blocks.WATER.defaultBlockState()) || p.test(Blocks.BEDROCK.defaultBlockState()) || p.test(blockbranch.realistic.ScreeContent.LOOSE_SCREE.defaultBlockState()) || p.test(Blocks.MUD.defaultBlockState()) || p.test(Blocks.GRAVEL.defaultBlockState()) || p.test(Blocks.COARSE_DIRT.defaultBlockState()) || p.test(Blocks.SHORT_GRASS.defaultBlockState()) || p.test(blockbranch.realistic.RiverMarginContent.PACKED_RIVER_SHINGLE.defaultBlockState()) || p.test(Blocks.SAND.defaultBlockState()) || p.test(Blocks.SANDSTONE.defaultBlockState()) || p.test(blockbranch.realistic.SandyShoreContent.ROOT_BOUND_SAND.defaultBlockState()) || p.test(blockbranch.realistic.SandyShoreContent.BEACH_GRASS.defaultBlockState())) return true;
            for(boolean waterlogged:new boolean[]{false,true}) for(boolean tip:new boolean[]{false,true})
                if(p.test(blockbranch.realistic.ReedbedContent.LAKE_REED.defaultBlockState().setValue(blockbranch.realistic.LakeReedBlock.WATERLOGGED,waterlogged).setValue(blockbranch.realistic.LakeReedBlock.TIP,tip))) return true;
            return false;
        }
        @Override public PalettedContainer<BlockState> getStates() { return generateStates(y); }
        private volatile PalettedContainerRO<Holder<Biome>> sectionBiomes;
        @Override public PalettedContainerRO<Holder<Biome>> getBiomes() {var b=sectionBiomes;if(b==null){synchronized(this){b=sectionBiomes;if(b==null)sectionBiomes=b=TerrainProfile.this.sectionBiomes(y);}}return b;}
        @Override public Holder<Biome> getNoiseBiome(int x,int ly,int z) { return biome(x,(y<<2)+ly,z); }
        @Override public LevelChunkSection copy() { return generate(y); }
        @Override public BlockState setBlockState(int x,int ly,int z,BlockState value,boolean lock) { throw new IllegalStateException("Materialize a procedural section before editing"); }
        @Override public void write(FriendlyByteBuf buffer) { generate(y).write(buffer); }
    }
    /** NBT arrays are mutable: a serialized snapshot must not expose the immutable profile storage. */
    public CompoundTag encode(PalettedContainerFactory factory) {
        var tag=new CompoundTag(); tag.putInt("version",caveBiomes.enabled()?70:dripstone.definition()==2?68:litCaves.enabled()?66:dripstone.enabled()?64:ores.enabled()?(ores.definition()>=2?63:62):underground.enabled()?61:60);
        // Appended boreal codes retain the byte field and all saved legacy meanings.
        // A wrapper gates only profiles that need the new reader, independently
        // of the existing underground and cave-biome foundation versions.
        if(freshBoreal(boreal)){tag.putInt("base_profile_version",tag.getIntOr("version",-1));tag.putInt("version",72);}
        // Short stacks retain literal stem counts: 2/3 above depth-one/two beds.
        // Older readers reject that geometry, so gate only profiles that use it.
        if(shortReeds(ground,water,reedbed)){tag.putInt("reed_base_profile_version",tag.getIntOr("version",-1));tag.putInt("version",74);}
        if(benthicTraces(canopy.encode())){tag.putInt("benthic_base_profile_version",tag.getIntOr("version",-1));tag.putInt("version",75);}
        if(landIce(coldSteppe)){tag.putInt("land_ice_base_profile_version",tag.getIntOr("version",-1));tag.putInt("version",76);}
        if(perennialIce.enabled()){tag.putInt("perennial_ice_base_profile_version",tag.getIntOr("version",-1));tag.putInt("version",77);tag.putIntArray("perennial_ice",perennialIce.encode());}
        if(caveBiomes.enabled())tag.put("cave_biomes",caveBiomes.encode(factory));
        if(litCaves.enabled()){tag.put("lit_caves",litCaves.encode());tag.putInt("dripstone_definition",dripstone.definition());tag.putInt("ore_definition",ores.enabled()?ores.definition():0);}
        if(dripstone.definition()==2){tag.putInt("dripstone_definition",2);tag.putInt("lit_cave_definition",litCaves.enabled()?2:0);}
        if(dripstone.enabled()){tag.put("dripstone",dripstone.encode(factory));tag.putInt("ore_definition",ores.enabled()?ores.definition():0);}
        if(ores.enabled())tag.put("ores",ores.encode());
        if(underground.enabled())tag.put("underground",underground.encode()); tag.putInt("floor",floor); tag.putInt("ceiling",ceiling);
        tag.putIntArray("floating_ice",floatingIce.encode());
        tag.putIntArray("ground",ground.clone()); tag.putIntArray("water",water.clone());
        tag.putByteArray("scree",scree.clone());
        tag.putByteArray("reedbed",reedbed.clone());
        tag.putByteArray("river_margin",riverMargin.clone());
        tag.putByteArray("sandy_shore",sandyShore.clone());
        tag.putByteArray("meadow",meadow.clone());
        tag.putByteArray("woodland",woodland.clone());tag.putByteArray("steppe",steppe.clone());tag.putByteArray("boreal",boreal.clone());tag.putByteArray("warm",warm.clone());tag.putByteArray("cold_steppe",coldSteppe.clone());tag.putByteArray("desert",desert.clone());tag.putIntArray("canopy",canopy.encode());
        // Window sends and asynchronous saves may encode the same immutable
        // palette. Native pack() still owns a threading detector on read access.
        synchronized(biomes){tag.store("biomes",factory.biomeContainerCodec(),biomes);}
        return tag;
    }
    private static boolean freshBoreal(byte[] cover){for(byte value:cover)if(value>14)return true;return false;}
    private static boolean benthicTraces(int[] blocks){for(int value:blocks)if((value&CanopyLayer.STATE_MASK)>=blockbranch.realistic.AbyssalTraces.FIRST)return true;return false;}
    private static boolean landIce(byte[] roles){for(byte code:roles)if(blockbranch.realistic.LandIceField.encoded(code))return true;return false;}
    public static TerrainProfile decode(CompoundTag tag,PalettedContainerFactory factory) {return decode(tag,factory,PerennialIceLayer.EMPTY);}
    /** {@code ice} is the perennial-ice wrapper being unwrapped. The foundation is rebuilt with it attached from the start,
     * so every foundation layer (dripstone, cave biomes, lit caves) is validated against the same rock datum it was
     * planned with, never against the ice-inclusive surface. */
    private static TerrainProfile decode(CompoundTag tag,PalettedContainerFactory factory,PerennialIceLayer ice) {
        if(tag.getIntOr("version",-1)==77){
            int base=tag.getInt("perennial_ice_base_profile_version").orElseThrow(()->new IllegalArgumentException("Missing perennial-ice foundation"));
            if(base!=60&&base!=61&&base!=62&&base!=63&&base!=64&&base!=66&&base!=68&&base!=70&&base!=72&&base!=74&&base!=75&&base!=76)throw new IllegalArgumentException("Unsupported perennial-ice foundation");
            var wrapped=new PerennialIceLayer(tag.getIntArray("perennial_ice").orElseThrow());if(!wrapped.enabled())throw new IllegalArgumentException("Empty perennial-ice wrapper");
            var foundation=tag.copy();foundation.putInt("version",base);foundation.remove("perennial_ice_base_profile_version");foundation.remove("perennial_ice");return decode(foundation,factory,wrapped).withPerennialIce(wrapped);
        }
        if(tag.contains("perennial_ice_base_profile_version")||tag.contains("perennial_ice"))throw new IllegalArgumentException("Perennial ice on legacy profile");
        if(tag.getIntOr("version",-1)==76){
            int base=tag.getInt("land_ice_base_profile_version").orElseThrow(()->new IllegalArgumentException("Missing land-ice foundation"));
            if(base!=60&&base!=61&&base!=62&&base!=63&&base!=64&&base!=66&&base!=68&&base!=70&&base!=72&&base!=74&&base!=75)throw new IllegalArgumentException("Unsupported land-ice foundation");
            if(!landIce(tag.getByteArray("cold_steppe").orElseThrow()))throw new IllegalArgumentException("Land-ice wrapper requires ice roles");
            var foundation=tag.copy();foundation.putInt("version",base);foundation.remove("land_ice_base_profile_version");return decodeFoundation(foundation,factory,ice);
        }
        if(tag.contains("land_ice_base_profile_version")||landIce(tag.getByteArray("cold_steppe").orElse(new byte[0])))throw new IllegalArgumentException("Land ice on legacy profile");
        return decodeFoundation(tag,factory,ice);
    }
    private static TerrainProfile decodeFoundation(CompoundTag tag,PalettedContainerFactory factory,PerennialIceLayer ice) {
        // Appended environmental states are explicit reader barriers; profiles
        // without them keep their exact historical versions and descriptors.
        if(tag.getIntOr("version",-1)==75){
            int base=tag.getInt("benthic_base_profile_version").orElseThrow(()->new IllegalArgumentException("Missing benthic foundation"));
            if(base!=60&&base!=61&&base!=62&&base!=63&&base!=64&&base!=66&&base!=68&&base!=70&&base!=72&&base!=74)throw new IllegalArgumentException("Unsupported benthic foundation");
            if(!benthicTraces(tag.getIntArray("canopy").orElseThrow(()->new IllegalArgumentException("Missing benthic canopy"))))throw new IllegalArgumentException("Benthic wrapper requires traces");
            var foundation=tag.copy();foundation.putInt("version",base);foundation.remove("benthic_base_profile_version");return decode(foundation,factory,false,false,ice);
        }
        if(tag.contains("benthic_base_profile_version")||benthicTraces(tag.getIntArray("canopy").orElse(new int[0])))throw new IllegalArgumentException("Benthic states on legacy profile");
        return decode(tag,factory,false,false,ice);
    }
    private static boolean shortReeds(int[] ground,int[] water,byte[] cover) {
        if(ground.length!=256||water.length!=256||cover.length!=256)return false;
        for(int i=0;i<256;i++)if((cover[i]==2||cover[i]==3)&&water[i]-ground[i]==cover[i])return true;
        return false;
    }
    private static TerrainProfile decode(CompoundTag tag,PalettedContainerFactory factory,boolean freshBoreal,boolean shortReeds,PerennialIceLayer ice) {
        if(tag.getIntOr("version",-1)==74){
            if(shortReeds||freshBoreal)throw new IllegalArgumentException("Nested reed profile wrapper");
            int base=tag.getInt("reed_base_profile_version").orElseThrow(()->new IllegalArgumentException("Missing reed foundation version"));
            if(base!=60&&base!=61&&base!=62&&base!=63&&base!=64&&base!=66&&base!=68&&base!=70&&base!=72)throw new IllegalArgumentException("Unsupported reed foundation version");
            if(!shortReeds(tag.getIntArray("ground").orElseThrow(),tag.getIntArray("water").orElseThrow(),tag.getByteArray("reedbed").orElseThrow()))throw new IllegalArgumentException("Reed profile wrapper requires short emergent stems");
            var foundation=tag.copy();foundation.putInt("version",base);foundation.remove("reed_base_profile_version");
            return decode(foundation,factory,false,true,ice);
        }
        if(tag.contains("reed_base_profile_version"))throw new IllegalArgumentException("Reed foundation descriptor on legacy profile");
        if(!shortReeds&&shortReeds(tag.getIntArray("ground").orElseThrow(),tag.getIntArray("water").orElseThrow(),tag.getByteArray("reedbed").orElseThrow()))throw new IllegalArgumentException("Short emergent reeds on legacy profile");
        if(tag.getIntOr("version",-1)==72){
            if(freshBoreal)throw new IllegalArgumentException("Nested boreal profile wrapper");
            int base=tag.getInt("base_profile_version").orElseThrow(()->new IllegalArgumentException("Missing boreal foundation version"));
            if(base!=60&&base!=61&&base!=62&&base!=63&&base!=64&&base!=66&&base!=68&&base!=70)throw new IllegalArgumentException("Unsupported boreal foundation version");
            if(!freshBoreal(tag.getByteArray("boreal").orElseThrow()))throw new IllegalArgumentException("Boreal profile wrapper requires appended cover");
            var foundation=tag.copy();foundation.putInt("version",base);foundation.remove("base_profile_version");
            return decode(foundation,factory,true,shortReeds,ice);
        }
        if(tag.contains("base_profile_version"))throw new IllegalArgumentException("Boreal foundation descriptor on legacy profile");
        if(!freshBoreal&&freshBoreal(tag.getByteArray("boreal").orElseThrow()))throw new IllegalArgumentException("Appended boreal cover on legacy profile");
        if(tag.getIntOr("version",-1)==70){
            var foundation=tag.copy();foundation.putInt("version",68);foundation.remove("cave_biomes");
            var base=decode(foundation,factory,freshBoreal,shortReeds,ice);
            return base.withCaveBiomes(CaveBiomeLayer.decode(tag.getCompound("cave_biomes").orElseThrow(),factory,base.litCaves()));
        }
        if(tag.contains("cave_biomes"))throw new IllegalArgumentException("Cave biome descriptor on legacy profile");
        if(tag.getIntOr("version",-1)!=57&&tag.getIntOr("version",-1)!=58&&tag.getIntOr("version",-1)!=59&&tag.getIntOr("version",-1)!=60&&tag.getIntOr("version",-1)!=61&&tag.getIntOr("version",-1)!=62&&tag.getIntOr("version",-1)!=63&&tag.getIntOr("version",-1)!=64&&tag.getIntOr("version",-1)!=66&&tag.getIntOr("version",-1)!=68) throw new IllegalArgumentException("Unsupported procedural terrain version");
        var profile=new TerrainProfile(tag.getIntOr("floor",0),tag.getIntOr("ceiling",0),
            tag.getIntArray("ground").orElseThrow(),tag.getIntArray("water").orElseThrow(),tag.getByteArray("scree").orElseThrow(),tag.getByteArray("reedbed").orElseThrow(),tag.getByteArray("river_margin").orElseThrow(),tag.getByteArray("sandy_shore").orElseThrow(),tag.getByteArray("meadow").orElseThrow(),tag.getByteArray("woodland").orElseThrow(),tag.getByteArray("steppe").orElseThrow(),tag.getByteArray("boreal").orElseThrow(),tag.getByteArray("warm").orElseThrow(),tag.getByteArray("cold_steppe").orElseThrow(),tag.getByteArray("desert").orElseThrow(),new CanopyLayer(tag.getIntArray("canopy").orElseThrow()),tag.getIntOr("version",-1)==57?FloatingIceLayer.EMPTY:new FloatingIceLayer(tag.getIntArray("floating_ice").orElseThrow(()->new IllegalArgumentException("Missing floating ice layer"))),
            factory.biomeContainerCodec().parse(NbtOps.INSTANCE,tag.getCompoundOrEmpty("biomes")).getOrThrow(),factory.blockStatesStrategy());
        if(ice.enabled())profile=profile.withPerennialIce(ice);
        if(tag.getIntOr("version",-1)<66&&tag.contains("lit_caves"))throw new IllegalArgumentException("Lit cave descriptor on legacy profile");
        if(tag.getIntOr("version",-1)==64||tag.getIntOr("version",-1)==66||tag.getIntOr("version",-1)==68){
            var under=UndergroundLayer.decode(tag.getCompound("underground").orElseThrow(()->new IllegalArgumentException("Missing cave descriptor")));
            int expected=tag.getInt("ore_definition").orElseThrow(()->new IllegalArgumentException("Missing ore definition"));
            var ore=tag.contains("ores")?OreLayer.decode(tag.getCompoundOrEmpty("ores")):OreLayer.NONE;
            if(expected!=(ore.enabled()?ore.definition():0))throw new IllegalArgumentException("Foundation ore definition mismatch");
            var result=profile.withLayers(under,ore);
            if(tag.getIntOr("version",-1)==64){var layer=DripstoneLayer.decode(tag.getCompound("dripstone").orElseThrow(),factory);if(layer.definition()!=1)throw new IllegalArgumentException("Legacy dripstone definition mismatch");return result.withDripstone(layer);}
            int drip=tag.getInt("dripstone_definition").orElseThrow();if(drip<0||drip>(tag.getIntOr("version",-1)==68?2:1)||tag.contains("dripstone")!=(drip>0)||tag.getIntOr("version",-1)==68&&drip!=2)throw new IllegalArgumentException("Lit cave/dripstone definition mismatch");
            if(drip>0){var layer=DripstoneLayer.decode(tag.getCompoundOrEmpty("dripstone"),factory);if(layer.definition()!=drip)throw new IllegalArgumentException("Dripstone definition mismatch");result=result.withDripstone(layer);}
            if(tag.getIntOr("version",-1)==68){int growth=tag.getInt("lit_cave_definition").orElseThrow();if((growth!=0&&growth!=2)||tag.contains("lit_caves")!=(growth==2))throw new IllegalArgumentException("Formation/moss definition mismatch");}
            return tag.getIntOr("version",-1)==68&&!tag.contains("lit_caves")?result:result.withLitCaves(LitCaveLayer.decode(tag.getCompound("lit_caves").orElseThrow()));
        }
        if(tag.contains("lit_caves"))throw new IllegalArgumentException("Lit cave descriptor on legacy profile");
        if(tag.contains("dripstone"))throw new IllegalArgumentException("Dripstone descriptor on legacy profile");
        if(tag.getIntOr("version",-1)==62||tag.getIntOr("version",-1)==63){var ore=OreLayer.decode(tag.getCompound("ores").orElseThrow(()->new IllegalArgumentException("Missing ore descriptor")));if(tag.getIntOr("version",-1)==63?ore.definition()<2:ore.definition()!=1)throw new IllegalArgumentException("Ore/profile version mismatch");return profile.withLayers(tag.contains("underground")?UndergroundLayer.decode(tag.getCompoundOrEmpty("underground")):UndergroundLayer.NONE,ore);}
        if(tag.contains("ores"))throw new IllegalArgumentException("Ore descriptor on older profile");
        if(tag.getIntOr("version",-1)==61)return profile.withUnderground(UndergroundLayer.decode(tag.getCompound("underground").orElseThrow(()->new IllegalArgumentException("Missing underground descriptor"))));
        if(tag.contains("underground"))throw new IllegalArgumentException("Underground descriptor on legacy profile");
        return profile;
    }
}
