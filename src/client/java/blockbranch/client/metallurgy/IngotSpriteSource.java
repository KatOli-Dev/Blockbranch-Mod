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
 * Recolors installed ingots and bricks in memory so mod ingots share the active pack's native item
 * silhouettes and shading. No native artwork is bundled; our packaged PNGs remain the fallback.
 */
public final class IngotSpriteSource implements SpriteSource {
    public static final MapCodec<IngotSpriteSource> CODEC=MapCodec.unit(IngotSpriteSource::new);
    /** Bismuth: diagonal oxide bands share shadow and glint stops so the sprite stays one metal. */
    private static final int[][] BISMUTH={
        {0x2f2838,0x6a5a70,0xb09bb4,0xd9c4d6,0xf7f1fa},{0x2f2838,0x2f6b72,0x5fb0a6,0xa9dccb,0xf7f1fa},
        {0x2f2838,0x5b4a86,0x9a82c4,0xc9b7e6,0xf7f1fa},{0x2f2838,0x8a6a34,0xcfa65a,0xedd197,0xf7f1fa}};
    /** Palette stops: shadow, dark, mid, light, glint. */
    static final Map<String,int[]> PALETTES=Map.ofEntries(
        Map.entry("tin",new int[]{0x384448,0x657c82,0x93abae,0xc8d6d5,0xe6edec}),
        Map.entry("lead",new int[]{0x25222f,0x443d55,0x79738d,0xb7acc5,0xdfdae5}),
        Map.entry("lead_alloy",new int[]{0x1f1d2a,0x3a3550,0x625d80,0x9b94b8,0xcbc6dc}),
        Map.entry("chromium",new int[]{0x29373e,0x4a6470,0x96bac6,0xe0f0f2,0xf1f8f9}),
        Map.entry("manganese",new int[]{0x2b2830,0x4f4858,0x6c6477,0xa29ca8,0xd5d2d8}),
        Map.entry("nickel",new int[]{0x36382b,0x62654e,0xa7aa81,0xd9d9ae,0xeeeedb}),
        Map.entry("platinum",new int[]{0x484336,0x827963,0xc6bda6,0xeee5cc,0xf7f3e8}),
        Map.entry("silver",new int[]{0x242834,0x606a7b,0xcbd5e1,0xe1e9f2,0xf9fbff}),
        Map.entry("titanium",new int[]{0x2d3741,0x526477,0x9bafc2,0xdfebf2,0xf1f6f9}),
        Map.entry("tungsten",new int[]{0x17191d,0x292d35,0x555e6b,0xa0abb6,0xd4d9de}),
        Map.entry("uranium",new int[]{0x161c14,0x283325,0x687944,0xb5d360,0xdeebb7}),
        Map.entry("zinc",new int[]{0x413e46,0x77717f,0xa39aaa,0xd2cbd4,0xebe8ec}),
        Map.entry("brass",new int[]{0x4a3516,0x8a6a24,0xc9a43c,0xe6c85c,0xfaeaa0}),
        Map.entry("bronze",new int[]{0x382313,0x75451e,0xb98145,0xdaa35d,0xf3d3a0}),
        Map.entry("steel",new int[]{0x1b2026,0x434c55,0x929da7,0xc8d1d8,0xf0f4f7}),
        Map.entry("tungsten_steel",new int[]{0x151c24,0x313d4b,0x637486,0x92a3b6,0xc5d2df}),
        Map.entry("stainless_steel",new int[]{0x20272b,0x4f5c64,0xb2c2ca,0xd5e2e8,0xf7fdff}),
        Map.entry("platinum_alloy",new int[]{0x2b2830,0x625b69,0xc8bfce,0xe6dce7,0xfff6fb}),
        Map.entry("titanium_alloy",new int[]{0x272736,0x53556b,0xa1a6c0,0xc9cee3,0xf1f3ff}),
        Map.entry("bismuth",BISMUTH[0]));
    /** Native item each metal borrows its silhouette and shading from, spreading seven shapes around. */
    static final Map<String,String> DONORS=Map.ofEntries(
        Map.entry("steel","iron_ingot"),Map.entry("stainless_steel","iron_ingot"),Map.entry("silver","iron_ingot"),
        Map.entry("chromium","gold_ingot"),Map.entry("platinum","gold_ingot"),Map.entry("platinum_alloy","gold_ingot"),
        Map.entry("zinc","copper_ingot"),Map.entry("nickel","copper_ingot"),Map.entry("titanium_alloy","copper_ingot"),
        Map.entry("tungsten","netherite_ingot"),Map.entry("tungsten_steel","netherite_ingot"),
        Map.entry("lead","brick"),Map.entry("tin","brick"),Map.entry("manganese","brick"),
        Map.entry("titanium","nether_brick"),Map.entry("lead_alloy","nether_brick"),
        Map.entry("brass","resin_brick"),Map.entry("bronze","resin_brick"),Map.entry("bismuth","resin_brick"),Map.entry("uranium","resin_brick"));
    private static final double BODY_CURVE=1.35;
    static Identifier donor(String material){return Identifier.withDefaultNamespace("textures/item/"+DONORS.get(material)+".png");}
    public static void register(){SpriteSourceRegistry.register(Blockbranch.id("native_ingots"),CODEC);}
    @Override public MapCodec<? extends SpriteSource> codec(){return CODEC;}
    @Override public void run(ResourceManager resources,Output output){
        for(var material:PALETTES.keySet()){var name=material+"_ingot";output.add(Blockbranch.id("item/"+name),loader->load(resources,loader,name,material));}
    }
    private static SpriteContents load(ResourceManager resources,SpriteResourceLoader loader,String name,String material){
        var sprite=Blockbranch.id("item/"+name);var texture=Blockbranch.id("textures/item/"+name+".png");
        // A pack that replaces our texture (or animates it) keeps native loading, as for pickaxes.
        if(!PickaxeSpriteSource.directOverride(resources,texture)){
            NativeImage image=null;
            try{
                image=derive(resources,material);
                if(image!=null){var result=new SpriteContents(sprite,new FrameSize(image.getWidth(),image.getHeight()),image);image=null;return result;}
            }catch(IOException|RuntimeException e){Blockbranch.LOGGER.debug("Native ingot {} retains packaged sprite: {}",name,e.toString());}
            finally{if(image!=null)image.close();}
        }
        return resources.getResource(texture).map(r->loader.loadSprite(sprite,r)).orElse(null);
    }
    static NativeImage derive(ResourceManager resources,String material)throws IOException{
        try(var donor=PickaxeSpriteSource.read(resources,donor(material))){return recolor(donor,material);}
    }
    /**
     * Normalizes the donor's luminance between its own darkest and lightest tone, bends it so the body
     * sits in the palette's middle stops and only the donor's brightest pixels reach the glint stop.
     * Normalizing per donor keeps contrast for dark or restyled packs. Null when the donor is not a
     * plain opaque-or-clear square with a usable tonal range, so callers use the packaged sprite.
     */
    static NativeImage recolor(NativeImage donor,String material){return recolor(donor,PALETTES.get(material),material.equals("bismuth"));}
    /** Same normalization for any all-metal donor, such as armor icons and worn armor layers. */
    static NativeImage recolor(NativeImage donor,int[] palette,boolean iridescent){return recolor(donor,palette,iridescent,BODY_CURVE);}
    /** A curve of 1 keeps the donor's tonal balance; larger values push the body toward the dark stops. */
    static NativeImage recolor(NativeImage donor,int[] palette,boolean iridescent,double curve){
        int w=donor.getWidth(),h=donor.getHeight();if(palette==null||w<16||h<16||w>256||h>256||w%16!=0||h%16!=0)return null;
        double min=256,max=-1;var tones=new HashSet<Integer>();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int c=donor.getPixel(x,y),a=c>>>24;if(a==0)continue;if(a!=255)return null;tones.add(c);double l=luminance(c);min=Math.min(min,l);max=Math.max(max,l);}
        if(tones.size()<3||max-min<32)return null;
        var result=new NativeImage(w,h,true);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int c=donor.getPixel(x,y);if((c>>>24)==0)continue;
            result.setPixel(x,y,0xff000000|shade(iridescent?BISMUTH[Math.min(3,(x+y)*4/(w+h-1))]:palette,Math.pow((luminance(c)-min)/(max-min),curve)));
        }
        return result;
    }
    private static double luminance(int c){return .2126*((c>>>16)&255)+.7152*((c>>>8)&255)+.0722*(c&255);}
    static int shade(int[] palette,double light){
        double p=Math.clamp(light,0,1)*(palette.length-1);int i=Math.min((int)p,palette.length-2);double t=p-i;int a=palette[i],b=palette[i+1],out=0;
        for(int shift:new int[]{16,8,0})out|=(int)Math.round(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t)<<shift;
        return out;
    }
}
