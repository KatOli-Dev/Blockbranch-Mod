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

/** Armor inventory icons recolored in memory from the installed iron armor; nothing native is bundled. */
public final class ArmorSpriteSource implements SpriteSource {
    public static final MapCodec<ArmorSpriteSource> CODEC=MapCodec.unit(ArmorSpriteSource::new);
    static final List<String> SLOTS=List.of("helmet","chestplate","leggings","boots");
    static final int[] MAGNETITE={0x16191c,0x282d33,0x51565d,0x8f9aa2,0xcdd2d5};
    /** Sprite name to palette; magnetite supplies boots only. */
    static final Map<String,int[]> PALETTES;
    static{
        var map=new LinkedHashMap<String,int[]>();
        for(var material:List.of("bronze","steel","stainless_steel","silver","platinum_alloy","titanium_alloy"))for(var slot:SLOTS)map.put(material+"_"+slot,PickaxeSpriteSource.PALETTES.get(material));
        map.put("magnetite_boots",MAGNETITE);
        PALETTES=Collections.unmodifiableMap(map);
    }
    static String slot(String name){return name.substring(name.lastIndexOf('_')+1);}
    static Identifier donor(String name){return Identifier.withDefaultNamespace("textures/item/iron_"+slot(name)+".png");}
    public static void register(){SpriteSourceRegistry.register(Blockbranch.id("native_armor"),CODEC);}
    @Override public MapCodec<? extends SpriteSource> codec(){return CODEC;}
    @Override public void run(ResourceManager resources,Output output){
        for(var name:PALETTES.keySet())output.add(Blockbranch.id("item/"+name),loader->load(resources,loader,name));
    }
    private static SpriteContents load(ResourceManager resources,SpriteResourceLoader loader,String name){
        var sprite=Blockbranch.id("item/"+name);var texture=Blockbranch.id("textures/item/"+name+".png");
        if(!PickaxeSpriteSource.directOverride(resources,texture)){
            NativeImage image=null;
            try{
                image=derive(resources,name);
                if(image!=null){var result=new SpriteContents(sprite,new FrameSize(image.getWidth(),image.getHeight()),image);image=null;return result;}
            }catch(IOException|RuntimeException e){Blockbranch.LOGGER.debug("Native armor {} retains packaged sprite: {}",name,e.toString());}
            finally{if(image!=null)image.close();}
        }
        return resources.getResource(texture).map(r->loader.loadSprite(sprite,r)).orElse(null);
    }
    static NativeImage derive(ResourceManager resources,String name)throws IOException{
        try(var iron=PickaxeSpriteSource.read(resources,donor(name))){return IngotSpriteSource.recolor(iron,PALETTES.get(name),false);}
    }
}
