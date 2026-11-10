package blockbranch.cubic;

import blockbranch.realistic.DripstoneHabitat;
import blockbranch.realistic.DeepDarkHabitat;
import blockbranch.realistic.TerrainShape;
import java.util.Arrays;
import net.minecraft.core.Holder;
import net.minecraft.nbt.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.*;

/** Definition 1 changes biome identity only. No native feature lists or spawning are applied. */
public final class CaveBiomeLayer {
    public static final CaveBiomeLayer NONE=new CaveBiomeLayer();
    private final long seed;
    private final int cx,cz;
    private final Holder<Biome> lush,sulfur;
    private final Holder<Biome> dark;
    private final int darkMask;
    private final int[] lushCells;
    private final double[] provinces;
    private final boolean sulfurPossible;
    private CaveBiomeLayer(){seed=0;cx=cz=0;lush=sulfur=dark=null;darkMask=0;lushCells=new int[0];provinces=new double[0];sulfurPossible=false;}
    public CaveBiomeLayer(long seed,int cx,int cz,Holder<Biome> lush,Holder<Biome> sulfur,LitCaveLayer growth){
        if(!growth.enabled()||!growth.owns(cx,cz)||!lush.is(Biomes.LUSH_CAVES)||!sulfur.is(Biomes.SULFUR_CAVES))throw new IllegalArgumentException("Invalid cave biome foundation");
        this.seed=seed;this.cx=cx;this.cz=cz;this.lush=lush;this.sulfur=sulfur;this.dark=null;this.darkMask=0;
        int[] moss=growth.placements(),cells=new int[moss.length/2];
        for(int i=0;i<moss.length;i+=2)cells[i/2]=key((moss[i]&15)>>2,(moss[i+1]+1)>>2,(moss[i]>>4)>>2);
        lushCells=Arrays.stream(cells).distinct().sorted().toArray();
        provinces=new double[16];
        boolean possible=false;for(int z=0;z<4;z++)for(int x=0;x<4;x++){provinces[z*4+x]=DripstoneHabitat.province(seed^0x73756c6675725f31L,cx*16+x*4,cz*16+z*4);possible|=provinces[z*4+x]>.35;}sulfurPossible=possible;
    }
    private CaveBiomeLayer(CaveBiomeLayer base,Holder<Biome> dark,int mask){
        seed=base.seed;cx=base.cx;cz=base.cz;lush=base.lush;sulfur=base.sulfur;lushCells=base.lushCells;provinces=base.provinces;sulfurPossible=base.sulfurPossible;this.dark=dark;darkMask=mask;
    }
    public CaveBiomeLayer withDeepDark(Holder<Biome> dark,int mask){
        if(!enabled()||!dark.is(Biomes.DEEP_DARK)||(mask&~65535)!=0)throw new IllegalArgumentException("Invalid deep-dark layer");
        return mask==0?this:new CaveBiomeLayer(this,dark,mask);
    }
    private static int key(int x,int y,int z){return (y<<4)|(z&3)*4|(x&3);}
    public boolean enabled(){return lush!=null;}
    public void validate(TerrainProfile profile){
        if(!enabled()||!profile.underground().enabled()||profile.underground().seed()!=seed||!profile.underground().owns(cx,cz)||!profile.litCaves().enabled()||profile.dripstone().definition()!=2)throw new IllegalArgumentException("Cave biome/profile mismatch");
    }
    public Holder<Biome> biome(int x,int qy,int z,int surface,Holder<Biome> original){
        if(!enabled())return original;
        long y=(long)qy*4;if(y<TerrainShape.FLOOR||y>=TerrainShape.CEILING)return original;
        if(dark!=null&&(darkMask&(1<<((z&3)*4+(x&3))))!=0&&DeepDarkHabitat.atHeight(surface,y))return dark;
        if(Arrays.binarySearch(lushCells,key(x,qy,z))>=0)return lush;
        // Keep the existing dripstone province intact. Sulfur is a separate, rocky
        // deep province; vanilla sulfur resources, fluids and features stay disabled.
        long depth=(long)surface-y;
        return !original.is(Biomes.DRIPSTONE_CAVES)&&provinces[(z&3)*4+(x&3)]>.35&&depth>=96&&depth<=384&&y>TerrainShape.FLOOR+32?sulfur:original;
    }
    public boolean mayHaveBiome(int sy,int low,int high){
        if(!enabled())return false;
        for(int cell:lushCells)if((cell>>6)==sy)return true;
        long bottom=(long)sy*16,top=bottom+12;
        return darkMask!=0&&bottom<=TerrainProfile.DEEPSLATE_TOP&&top>=TerrainProfile.DEEPSLATE_TOP-DeepDarkHabitat.BAND_DEPTH&&bottom<=high-DeepDarkHabitat.MIN_DEPTH||sulfurPossible&&bottom<=high-96&&top>=low-384&&top>TerrainShape.FLOOR+32;
    }
    public CompoundTag encode(PalettedContainerFactory factory){
        var tag=new CompoundTag();tag.putInt("definition",dark==null?1:2);tag.putLong("seed",seed);tag.putInt("column_x",cx);tag.putInt("column_z",cz);
        var holders=new PalettedContainer<>(lush,factory.biomeStrategy());holders.set(1,0,0,sulfur);if(dark!=null){holders.set(2,0,0,dark);tag.putInt("deep_dark_mask",darkMask);}tag.store("holders",factory.biomeContainerCodec(),holders);return tag;
    }
    public static CaveBiomeLayer decode(CompoundTag tag,PalettedContainerFactory factory,LitCaveLayer growth){
        int definition=tag.getIntOr("definition",0);
        if(definition!=1&&definition!=2)throw new IllegalArgumentException("Unsupported cave biome definition");
        var holders=factory.biomeContainerCodec().parse(NbtOps.INSTANCE,tag.getCompoundOrEmpty("holders")).getOrThrow();
        var lush=holders.get(0,0,0);var sulfur=holders.get(1,0,0);
        var dark=definition==2?holders.get(2,0,0):null;
        for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)if(!holders.get(x,y,z).equals(x==1&&y==0&&z==0?sulfur:x==2&&y==0&&z==0&&dark!=null?dark:lush))throw new IllegalArgumentException("Malformed cave biome holders");
        var layer=new CaveBiomeLayer(tag.getLong("seed").orElseThrow(),tag.getInt("column_x").orElseThrow(),tag.getInt("column_z").orElseThrow(),lush,sulfur,growth);
        if(dark==null)return layer;
        int mask=tag.getInt("deep_dark_mask").orElseThrow();if(mask==0)throw new IllegalArgumentException("Empty deep-dark definition2 mask");
        return layer.withDeepDark(dark,mask);
    }
}
