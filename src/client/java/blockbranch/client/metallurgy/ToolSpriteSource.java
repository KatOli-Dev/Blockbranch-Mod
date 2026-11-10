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
 * Axes, shovels, hoes and swords recolored from the installed iron, diamond and golden tools, with
 * the pickaxe source's palettes and fallbacks. No native artwork is bundled or written to disk.
 */
public final class ToolSpriteSource implements SpriteSource {
    public static final MapCodec<ToolSpriteSource> CODEC=MapCodec.unit(ToolSpriteSource::new);
    static final List<String> KINDS=List.of("axe","shovel","hoe","sword");
    /** Tungsten steel has no sword, matching the packaged item set. */
    static boolean exists(String material,String kind){return PickaxeSpriteSource.PALETTES.containsKey(material)&&!(kind.equals("sword")&&material.equals("tungsten_steel"));}
    public static void register(){SpriteSourceRegistry.register(Blockbranch.id("native_tools"),CODEC);}
    @Override public MapCodec<? extends SpriteSource> codec(){return CODEC;}
    @Override public void run(ResourceManager resources,Output output){
        for(var material:PickaxeSpriteSource.PALETTES.keySet())for(var kind:KINDS)if(exists(material,kind)){var name=material+"_"+kind;output.add(Blockbranch.id("item/"+name),loader->load(resources,loader,name,material,kind));}
    }
    private static Identifier donor(String prefix,String kind){return Identifier.withDefaultNamespace("textures/item/"+prefix+"_"+kind+".png");}
    private static SpriteContents load(ResourceManager resources,SpriteResourceLoader loader,String name,String material,String kind){
        var sprite=Blockbranch.id("item/"+name);var texture=Blockbranch.id("textures/item/"+name+".png");
        if(PickaxeSpriteSource.directOverride(resources,texture))return resources.getResource(texture).map(r->loader.loadSprite(sprite,r)).orElse(null);
        NativeImage image=null;
        try{
            image=derive(resources,material,kind);
            if(image!=null){var result=new SpriteContents(sprite,new FrameSize(image.getWidth(),image.getHeight()),image);image=null;return result;}
        }catch(IOException|RuntimeException e){Blockbranch.LOGGER.debug("Native tool {} retains unmodified resource loading: {}",name,e.toString());}
        finally{if(image!=null)image.close();}
        // Unknown or animated donor art uses the pack's own iron tool rather than a guessed mask.
        return resources.getResource(donor("iron",kind)).or(()->resources.getResource(texture)).map(r->loader.loadSprite(sprite,r)).orElse(null);
    }
    static NativeImage derive(ResourceManager resources,String material,String kind)throws IOException{
        try(var iron=PickaxeSpriteSource.read(resources,donor("iron",kind));var diamond=PickaxeSpriteSource.read(resources,donor("diamond",kind));var gold=PickaxeSpriteSource.read(resources,donor("golden",kind))){
            return PickaxeSpriteSource.sample(iron,diamond,gold,PickaxeSpriteSource.PALETTES.get(material),false,kind.equals("sword"));
        }
    }
}
