package blockbranch.client.metallurgy;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Native tool, armor icon and worn-layer derivation checks against the installed donors. */
final class GearSpriteCheck {
 private static void require(boolean b,String m){if(!b)throw new AssertionError("GEAR "+m);}
 private static Identifier item(String name){return Identifier.withDefaultNamespace("textures/item/"+name+".png");}
 static void verify(ResourceManager resources)throws Exception{
  int tools=0,icons=0,layers=0;
  for(var material:PickaxeSpriteSource.PALETTES.keySet())for(var kind:ToolSpriteSource.KINDS)if(ToolSpriteSource.exists(material,kind))
   try(var iron=PickaxeSpriteSource.read(resources,item("iron_"+kind));var diamond=PickaxeSpriteSource.read(resources,item("diamond_"+kind));var gold=PickaxeSpriteSource.read(resources,item("golden_"+kind));var result=ToolSpriteSource.derive(resources,material,kind)){
    require(result!=null&&result.getWidth()==iron.getWidth(),"tool derivation "+material+"_"+kind);int metal=0,common=0;
    for(int y=0;y<iron.getHeight();y++)for(int x=0;x<iron.getWidth();x++){int a=iron.getPixel(x,y),r=result.getPixel(x,y);require((a>>>24)==(r>>>24),"tool alpha retained "+material+"_"+kind);
     if((a>>>24)==0)continue;if(a==diamond.getPixel(x,y)&&a==gold.getPixel(x,y)){require(a==r,"shared handle pixels exact "+material+"_"+kind);common++;}else{if(a!=r)metal++;}}
    require(metal>0&&common>0,"tool head recolored, handle kept "+material+"_"+kind);tools++;
   }
  for(var name:ArmorSpriteSource.PALETTES.keySet())try(var iron=PickaxeSpriteSource.read(resources,ArmorSpriteSource.donor(name));var result=ArmorSpriteSource.derive(resources,name)){
   require(result!=null&&result.getWidth()==iron.getWidth(),"armor icon derivation "+name);alphaSame(iron,result,"armor icon "+name);icons++;
  }
  for(var layer:ArmorLayerTexture.LAYERS)for(var name:ArmorLayerTexture.PALETTES.keySet())if(ArmorLayerTexture.packaged(layer,name))try(var iron=PickaxeSpriteSource.read(resources,ArmorLayerTexture.donor(layer));var result=ArmorLayerTexture.derive(resources,layer,name)){
   require(result!=null&&result.getWidth()==iron.getWidth()&&result.getHeight()==iron.getHeight(),"worn layer derivation "+layer+"/"+name);alphaSame(iron,result,"worn layer "+layer+"/"+name);layers++;
  }
  int blocks=0;
  for(var name:BlockSpriteSource.BLOCKS.keySet())try(var donor=PickaxeSpriteSource.read(resources,BlockSpriteSource.donor(name));var result=BlockSpriteSource.derive(resources,name)){
   require(result!=null&&result.getWidth()==donor.getWidth(),"block derivation "+name);alphaSame(donor,result,"block "+name);blocks++;
  }
  require(blocks==12,"expected 12 block derivations, got "+blocks);
  require(tools==27&&icons==25&&layers==22,"expected sprite counts, got "+tools+"/"+icons+"/"+layers);
  Blockbranch.LOGGER.info("GEAR_CHECK PASS {} native tool, {} armor icon and {} worn-layer derivations with exact silhouettes and shared handles",tools,icons,layers);
 }
 private static void alphaSame(NativeImage a,NativeImage b,String m){for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)require((a.getPixel(x,y)>>>24)==(b.getPixel(x,y)>>>24),m+" alpha retained");}
 private GearSpriteCheck(){}
}
