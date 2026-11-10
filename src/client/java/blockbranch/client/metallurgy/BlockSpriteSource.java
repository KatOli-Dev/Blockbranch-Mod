package blockbranch.client.metallurgy;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Metal storage blocks and bars recolored in memory from installed native blocks, so each metal
 * keeps a vanilla-style construction (plates, facets, rings) without shipping native artwork.
 * Contrast is compressed around the donor's own average tone so bright donors do not turn harsh.
 */
public final class BlockSpriteSource implements SpriteSource {
    public static final MapCodec<BlockSpriteSource> CODEC=MapCodec.unit(BlockSpriteSource::new);
    static final double CONTRAST=.7,FLOOR=.2;
    private static final int[] RESONITE={0x132730,0x234758,0x4caaa4,0xb2e8ca,0xdcf5e7};
    /** Sprite name to {palette material, native donor block, optional contrast override}. */
    static final Map<String,String[]> BLOCKS=Map.ofEntries(
        Map.entry("steel_block",new String[]{"steel","iron_block"}),
        Map.entry("stainless_steel_block",new String[]{"stainless_steel","diamond_block"}),
        Map.entry("silver_block",new String[]{"silver","polished_andesite","0.5"}),
        Map.entry("platinum_block",new String[]{"platinum","emerald_block"}),
        Map.entry("platinum_alloy_block",new String[]{"platinum_alloy","chiseled_copper","0.5"}),
        Map.entry("titanium_alloy_block",new String[]{"titanium_alloy","cut_copper","0.55"}),
        Map.entry("brass_block",new String[]{"brass","gold_block"}),
        Map.entry("bronze_block",new String[]{"bronze","copper_block"}),
        Map.entry("lead_block",new String[]{"lead","netherite_block"}),
        Map.entry("lead_alloy_block",new String[]{"lead_alloy","polished_blackstone"}),
        Map.entry("resonite_block",new String[]{"resonite","amethyst_block"}),
        Map.entry("brass_bars",new String[]{"brass","iron_bars"}));
    static int[] palette(String material){return material.equals("resonite")?RESONITE:IngotSpriteSource.PALETTES.get(material);}
    static double contrast(String name){var entry=BLOCKS.get(name);return entry.length>2?Double.parseDouble(entry[2]):CONTRAST;}
    static Identifier donor(String name){return Identifier.withDefaultNamespace("textures/block/"+BLOCKS.get(name)[1]+".png");}
    public static void register(){SpriteSourceRegistry.register(Blockbranch.id("native_blocks"),CODEC);}
    @Override public MapCodec<? extends SpriteSource> codec(){return CODEC;}
    @Override public void run(ResourceManager resources,Output output){
        for(var name:BLOCKS.keySet())output.add(Blockbranch.id("block/"+name),loader->load(resources,loader,name));
    }
    private static SpriteContents load(ResourceManager resources,SpriteResourceLoader loader,String name){
        var sprite=Blockbranch.id("block/"+name);var texture=Blockbranch.id("textures/block/"+name+".png");
        if(!PickaxeSpriteSource.directOverride(resources,texture)){
            NativeImage image=null;
            try{
                image=derive(resources,name);
                if(image!=null){var result=new SpriteContents(sprite,new FrameSize(image.getWidth(),image.getHeight()),image);image=null;return result;}
            }catch(IOException|RuntimeException e){Blockbranch.LOGGER.debug("Native block {} retains packaged sprite: {}",name,e.toString());}
            finally{if(image!=null)image.close();}
        }
        return resources.getResource(texture).map(r->loader.loadSprite(sprite,r)).orElse(null);
    }
    static NativeImage derive(ResourceManager resources,String name)throws IOException{
        try(var donor=PickaxeSpriteSource.read(resources,donor(name))){return recolor(donor,palette(BLOCKS.get(name)[0]),contrast(name));}
    }
    /** Maps luminance, offset from the donor's mean and scaled by contrast, onto the palette. Null if unusable. */
    static NativeImage recolor(NativeImage donor,int[] palette,double contrast){
        int n=donor.getWidth();if(palette==null||n<16||n>256||n%16!=0||donor.getHeight()!=n)return null;
        double min=256,max=-1,sum=0;int solid=0;var tones=new HashSet<Integer>();
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){int c=donor.getPixel(x,y),a=c>>>24;if(a==0)continue;if(a!=255)return null;tones.add(c);double l=lum(c);min=Math.min(min,l);max=Math.max(max,l);sum+=l;solid++;}
        if(solid==0||tones.size()<3||max-min<24)return null;
        double mean=(sum/solid-min)/(max-min);var result=new NativeImage(n,n,true);
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){
            int c=donor.getPixel(x,y);if((c>>>24)==0)continue;
            result.setPixel(x,y,0xff000000|IngotSpriteSource.shade(palette,Math.max(FLOOR,.5+((lum(c)-min)/(max-min)-mean)*contrast)));
        }
        return result;
    }
    private static double lum(int c){return .2126*((c>>>16)&255)+.7152*((c>>>8)&255)+.0722*(c&255);}
}
