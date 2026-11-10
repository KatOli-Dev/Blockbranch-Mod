package blockbranch.client.metallurgy;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;

/** Native ingot derivation plus synthetic pack regressions; no native art is encoded in fixtures. */
final class IngotSpriteCheck {
 private static void require(boolean b,String m){if(!b)throw new AssertionError("INGOT "+m);}
 private static final Identifier IRON=Identifier.withDefaultNamespace("textures/item/iron_ingot.png");
 static void verify(ResourceManager resources)throws Exception{
  require(IngotSpriteSource.DONORS.keySet().equals(IngotSpriteSource.PALETTES.keySet()),"every palette has a donor");
  for(var material:IngotSpriteSource.PALETTES.keySet())try(var iron=PickaxeSpriteSource.read(resources,IngotSpriteSource.donor(material))){
   try(var result=IngotSpriteSource.derive(resources,material)){
    require(result!=null&&result.getWidth()==iron.getWidth(),"native derivation "+material);var palette=IngotSpriteSource.PALETTES.get(material);boolean dark=false,glint=false;var seen=new HashSet<Integer>();
    for(int y=0;y<iron.getHeight();y++)for(int x=0;x<iron.getWidth();x++){int a=iron.getPixel(x,y),b=result.getPixel(x,y);require((a>>>24)==(b>>>24),"alpha silhouette retained "+material);if((b>>>24)>0){seen.add(b);int rgb=b&0xffffff;dark|=rgb==palette[0];glint|=rgb==palette[4];}}
    require(dark&&glint&&seen.size()>=3,"full palette range used "+material);
   }
  }
  for(int n:new int[]{16,32,64})try(var donor=new NativeImage(n,n,true)){
   for(int y=n/4;y<n*3/4;y++)for(int x=n/8;x<n*7/8;x++)donor.setPixel(x,y,0xff000000|((x%3==0?30:x%3==1?140:240)*0x010101));
   try(var result=IngotSpriteSource.recolor(donor,"tin")){require(result!=null&&result.getWidth()==n,"scaled donor "+n);require(result.getPixel(0,0)==0,"clear pixels stay clear");require((result.getPixel(n/2,n/2)>>>24)==255,"solid pixels stay opaque");}
   donor.setPixel(n/2,n/2,0x80ffffff);require(IngotSpriteSource.recolor(donor,"tin")==null,"translucent donor is not guessed");
  }
  try(var flat=new NativeImage(16,16,true)){flat.setPixel(1,1,0xff808080);flat.setPixel(2,1,0xff909090);require(IngotSpriteSource.recolor(flat,"tin")==null,"low-tone donor falls back");}
  var tin=Blockbranch.id("textures/item/tin_ingot.png");require(!PickaxeSpriteSource.directOverride(resources,tin),"default own ingot resource recognized");
  byte[] custom;try(var i=new NativeImage(32,32,true)){i.setPixel(3,7,0xffe52ab7);custom=png(i);}
  var original=resources.getResource(tin).orElseThrow();var replacement=new Resource(original.source(),()->new ByteArrayInputStream(custom));
  var result=load(proxy(resources,Map.of(tin,Optional.of(replacement))),"item/tin_ingot");try{require(result.width()==32&&pixels(result).getPixel(3,7)==0xffe52ab7,"direct ingot override wins");}finally{result.close();}
  result=load(proxy(resources,Map.of(IRON,Optional.empty())),"item/tin_ingot");try(var packaged=NativeImage.read(original.open())){require(result.width()==16&&Arrays.equals(pixels(result).makePixelArray(),packaged.makePixelArray()),"missing donor falls back to packaged sprite");}finally{result.close();}
  result=load(resources,"item/bismuth_ingot");try{require(result!=null&&result.width()==iron16(resources),"bismuth derives from its native donor");}finally{result.close();}
  Blockbranch.LOGGER.info("INGOT_CHECK PASS {} native ingot derivations, scaled16/32/64, translucent/low-tone rejection, direct override, missing-donor fallback",IngotSpriteSource.PALETTES.size());
 }
 private static int iron16(ResourceManager r)throws IOException{try(var i=PickaxeSpriteSource.read(r,IRON)){return i.getWidth();}}
 private static byte[] png(NativeImage i)throws IOException{var b=new java.awt.image.BufferedImage(i.getWidth(),i.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);b.setRGB(0,0,i.getWidth(),i.getHeight(),i.makePixelArray(),0,i.getWidth());var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(b,"png",out);return out.toByteArray();}
 private static ResourceManager proxy(ResourceManager base,Map<Identifier,Optional<Resource>> changes){return(ResourceManager)Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,m,args)->m.getName().equals("getResource")&&changes.containsKey(args[0])?changes.get(args[0]):m.invoke(base,args));}
 private static NativeImage pixels(SpriteContents s)throws Exception{var f=SpriteContents.class.getDeclaredField("originalImage");f.setAccessible(true);return(NativeImage)f.get(s);}
 private static SpriteContents load(ResourceManager r,String name){SpriteContents[] out={null};var target=Blockbranch.id(name);new IngotSpriteSource().run(r,new SpriteSource.Output(){public void add(Identifier id,SpriteSource.DiscardableLoader loader){if(id.equals(target))out[0]=loader.get(SpriteResourceLoader.create(Set.of()));}public void removeAll(java.util.function.Predicate<Identifier> f){}});return out[0];}
 private IngotSpriteCheck(){}
}
