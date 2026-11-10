package blockbranch.client.metallurgy;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Recolors installed tool pixels only in memory; no vanilla artwork is bundled or written to disk. */
public final class PickaxeSpriteSource implements SpriteSource {
    public static final MapCodec<PickaxeSpriteSource> CODEC=MapCodec.unit(PickaxeSpriteSource::new);
    static final Map<String,int[]> PALETTES=Map.of(
        "bronze",new int[]{0x382313,0x75451e,0xb98145,0xdaa35d,0xf3d3a0},
        "steel",new int[]{0x1b2026,0x434c55,0x929da7,0xc8d1d8,0xf0f4f7},
        "tungsten_steel",new int[]{0x151c24,0x313d4b,0x637486,0x92a3b6,0xc5d2df},
        "stainless_steel",new int[]{0x20272b,0x4f5c64,0xb2c2ca,0xd5e2e8,0xf7fdff},
        "silver",new int[]{0x242834,0x606a7b,0xcbd5e1,0xe1e9f2,0xf9fbff},
        "platinum_alloy",new int[]{0x2b2830,0x625b69,0xc8bfce,0xe6dce7,0xfff6fb},
        "titanium_alloy",new int[]{0x272736,0x53556b,0xa1a6c0,0xc9cee3,0xf1f3ff});
    private static final int MAX_SIZE=256,MAX_BYTES=1024*1024;
    private static final double[] SHADE_ANCHORS={0,44/231.0,169/231.0,192/231.0,1};
    private static final int[] TEAL={0x294b50,0x467d85,0x74a7aa,0xa4c8c7,0xd5e9df},VIOLET={0x443249,0x755779,0xa282a9,0xc4a7c7,0xe9d6e7},COPPER={0x563c2c,0x8b6143,0xb48c62,0xd3b28a,0xead7b6};
    private static final Identifier IRON=Identifier.withDefaultNamespace("textures/item/iron_pickaxe.png"),DIAMOND=Identifier.withDefaultNamespace("textures/item/diamond_pickaxe.png"),GOLD=Identifier.withDefaultNamespace("textures/item/golden_pickaxe.png");
    public static void register(){SpriteSourceRegistry.register(Blockbranch.id("native_pickaxes"),CODEC);}
    @Override public MapCodec<? extends SpriteSource> codec(){return CODEC;}
    @Override public void run(ResourceManager resources,Output output){
        for(var material:PALETTES.keySet()){var name=material+"_pickaxe";output.add(Blockbranch.id("item/"+name),loader->load(resources,loader,name,material,false));}
        output.add(Blockbranch.id("item/bismuth_pickaxe_inlay"),loader->load(resources,loader,"bismuth_pickaxe_inlay","steel",true));
    }
    private static SpriteContents load(ResourceManager resources,SpriteResourceLoader loader,String name,String material,boolean inlay){
        var sprite=Blockbranch.id("item/"+name);var texture=Blockbranch.id("textures/item/"+name+".png");
        // Compare only our own packaged original against the selected resource. This honors full
        // replacements even when a pack changes its ID/order, filters lower packs or adds animation.
        if(directOverride(resources,texture))return resources.getResource(texture).map(r->loader.loadSprite(sprite,r)).orElse(null);
        NativeImage image=null;
        try{
            if(inlay&&directOverride(resources,Blockbranch.id("textures/item/steel_pickaxe.png")))image=new NativeImage(16,16,true);
            else image=derive(resources,material,inlay);
            if(image!=null){var result=new SpriteContents(sprite,new FrameSize(image.getWidth(),image.getHeight()),image);image=null;Blockbranch.LOGGER.debug("Native pickaxe sprite {} sampled from installed resources",name);return result;}
        }catch(IOException|RuntimeException e){Blockbranch.LOGGER.debug("Native pickaxe {} retains unmodified resource loading: {}",name,e.toString());}
        finally{if(image!=null)image.close();}
        if(inlay)return new SpriteContents(sprite,new FrameSize(16,16),new NativeImage(16,16,true));
        // Unknown/animated donor art uses the pack's native iron sprite, not a guessed mask.
        var fallback=resources.getResource(IRON).or(()->resources.getResource(texture));
        return fallback.map(resource->loader.loadSprite(sprite,resource)).orElse(null);
    }
    static boolean directOverride(ResourceManager resources,Identifier texture){
        if(resources.getResource(Identifier.fromNamespaceAndPath(texture.getNamespace(),texture.getPath()+".mcmeta")).isPresent())return true;
        var resource=resources.getResource(texture);if(resource.isEmpty())return false;
        try(var selected=resource.get().open();var packaged=PickaxeSpriteSource.class.getResourceAsStream("/assets/"+texture.getNamespace()+"/"+texture.getPath())){
            if(packaged==null)return true;
            byte[] a=selected.readNBytes(MAX_BYTES+1),b=packaged.readNBytes(MAX_BYTES+1);return a.length>MAX_BYTES||!Arrays.equals(a,b);
        }catch(IOException e){return true;}
    }
    static NativeImage derive(ResourceManager resources,String material,boolean inlay)throws IOException{
        try(var iron=read(resources,IRON);var diamond=read(resources,DIAMOND);var gold=read(resources,GOLD)){return sample(iron,diamond,gold,PALETTES.get(material),inlay);}
    }
    static NativeImage read(ResourceManager resources,Identifier id)throws IOException{
        if(resources.getResource(Identifier.fromNamespaceAndPath(id.getNamespace(),id.getPath()+".mcmeta")).isPresent())throw new IOException("animated donor requires native loading");
        var resource=resources.getResource(id).orElseThrow(()->new IOException("missing "+id));byte[] bytes;
        try(var stream=resource.open()){bytes=stream.readNBytes(MAX_BYTES+1);}
        if(bytes.length>MAX_BYTES||bytes.length<33)throw new IOException("texture byte bound");var h=ByteBuffer.wrap(bytes);
        if(h.getLong()!=0x89504e470d0a1a0aL||h.getInt()!=13||h.getInt()!=0x49484452)throw new IOException("PNG header");
        int w=h.getInt(),height=h.getInt();if(w<16||w>MAX_SIZE||w%16!=0||height!=w)throw new IOException("unsupported texture dimensions");
        return NativeImage.read(bytes);
    }
    /** Differing metal pixels against two donors, with a shared handle and matching alpha geometry. */
    static NativeImage sample(NativeImage iron,NativeImage diamond,NativeImage gold,int[] palette,boolean inlay){return sample(iron,diamond,gold,palette,inlay,false);}
    /** A blade is nearly all metal and keeps only a short grip, so swords accept a larger differing share. */
    static NativeImage sample(NativeImage iron,NativeImage diamond,NativeImage gold,int[] palette,boolean inlay,boolean blade){
        int n=iron.getWidth();if(n<16||n>MAX_SIZE||n%16!=0||iron.getHeight()!=n||diamond.getWidth()!=n||diamond.getHeight()!=n||gold.getWidth()!=n||gold.getHeight()!=n||palette==null)return null;
        var metal=new boolean[n*n];int solid=0,common=0,head=0,top=n;double min=256,max=-1;
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){
            int a=iron.getPixel(x,y),b=diamond.getPixel(x,y),c=gold.getPixel(x,y),alpha=a>>>24;
            if(alpha!=(b>>>24)||alpha!=(c>>>24))return null;
            if(alpha==0)continue;if(alpha!=255)return null;solid++;
            if(a==b&&a==c){common++;continue;}
            metal[y*n+x]=true;head++;top=Math.min(top,y);double l=luminance(a);min=Math.min(min,l);max=Math.max(max,l);
        }
        if(solid==0||common<(blade?4:solid/4)||head<solid/4||head>(blade?solid*95/100:solid*3/4)||max-min<32)return null;
        var result=new NativeImage(n,n,true);int scale=n/16;
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){
            int color=iron.getPixel(x,y);if(!metal[y*n+x]){if(!inlay)result.setPixel(x,y,color);continue;}
            double light=(luminance(color)-min)/(max-min);
            if(inlay){
                // A single contiguous crown stripe stays inside the shaded head, away from the handle.
                if(y<top+scale||y>=top+2*scale||light<.2)continue;
                int[] hue=x<n/2?TEAL:x<n*5/8?VIOLET:COPPER;
                result.setPixel(x,y,0xff000000|shade(hue,light));
            }else result.setPixel(x,y,0xff000000|shade(palette,light));
        }
        return result;
    }
    private static double luminance(int color){return .2126*((color>>>16)&255)+.7152*((color>>>8)&255)+.0722*(color&255);}
    private static int shade(int[] palette,double light){
        int i=0;while(i<3&&light>SHADE_ANCHORS[i+1])i++;double t=Math.clamp((light-SHADE_ANCHORS[i])/(SHADE_ANCHORS[i+1]-SHADE_ANCHORS[i]),0,1);int a=palette[i],b=palette[i+1],out=0;for(int shift:new int[]{16,8,0})out|=(int)Math.round(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t)<<shift;return out;
    }
}
