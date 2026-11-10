package blockbranch.client.metallurgy;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Worn-armor layers recolored from the installed iron armor. They are registered at the same texture
 * IDs as our packaged PNGs, so the texture manager prefers them and reloads them with resource packs.
 * Worn layers are not atlas sprites, so registration must wait for the client texture manager.
 * Direct pack replacements retain ordinary resource loading; missing/unusable donors or failed
 * registration leave that loading path intact, including the retained packaged PNG fallback.
 */
public final class ArmorLayerTexture extends ReloadableTexture {
    static final List<String> LAYERS=List.of("humanoid","humanoid_leggings","humanoid_baby");
    /** Equipment asset name to palette. Magnetite and titanium_alloy are boots-only sheets. */
    static final Map<String,int[]> PALETTES=Map.of(
        "bronze",PickaxeSpriteSource.PALETTES.get("bronze"),"steel",PickaxeSpriteSource.PALETTES.get("steel"),
        "stainless_steel",PickaxeSpriteSource.PALETTES.get("stainless_steel"),"silver",PickaxeSpriteSource.PALETTES.get("silver"),
        "platinum_alloy",PickaxeSpriteSource.PALETTES.get("platinum_alloy"),"titanium_alloy",PickaxeSpriteSource.PALETTES.get("titanium_alloy"),
        "titanium_alloy_gear",PickaxeSpriteSource.PALETTES.get("titanium_alloy"),"magnetite",ArmorSpriteSource.MAGNETITE);
    /** Packaged sheets that exist; the others are never requested by an equipment asset. */
    static boolean packaged(String layer,String name){return ArmorLayerTexture.class.getResource("/assets/blockbranch/"+path(layer,name))!=null;}
    static String path(String layer,String name){return "textures/entity/equipment/"+layer+"/"+name+".png";}
    private final String layer,name;
    private ArmorLayerTexture(String layer,String name){super(Blockbranch.id(path(layer,name)));this.layer=layer;this.name=name;}
    /** Call once the client has started; later resource reloads re-run {@link #loadContents}. */
    public static void registerAll(){
        var textures=Minecraft.getInstance().getTextureManager();int count=0;
        for(var layer:LAYERS)for(var name:PALETTES.keySet())if(packaged(layer,name)){
            try{textures.registerAndLoad(Blockbranch.id(path(layer,name)),new ArmorLayerTexture(layer,name));count++;}
            catch(RuntimeException e){Blockbranch.LOGGER.warn("Worn armor {}/{} keeps its packaged texture: {}",layer,name,e.toString());}
        }
        Blockbranch.LOGGER.info("Registered {} native-derived worn armor textures",count);
    }
    static Identifier donor(String layer){return Identifier.withDefaultNamespace("textures/entity/equipment/"+layer+"/iron.png");}
    static NativeImage derive(ResourceManager resources,String layer,String name)throws IOException{
        try(var iron=PickaxeSpriteSource.read(resources,donor(layer))){return IngotSpriteSource.recolor(iron,PALETTES.get(name),false,1);}
    }
    @Override public TextureContents loadContents(ResourceManager resources)throws IOException{
        var id=resourceId();
        if(!PickaxeSpriteSource.directOverride(resources,id))try{
            var image=derive(resources,layer,name);
            if(image!=null){
                // Sampler metadata (blur/clamp/mipmaps) stays that of our packaged sheet.
                try(var packaged=TextureContents.load(resources,id)){return new TextureContents(image,packaged.metadata());}
                catch(IOException|RuntimeException e){image.close();throw e;}
            }
        }catch(IOException|RuntimeException e){Blockbranch.LOGGER.debug("Worn armor {}/{} retains packaged texture: {}",layer,name,e.toString());}
        return TextureContents.load(resources,id);
    }
}
