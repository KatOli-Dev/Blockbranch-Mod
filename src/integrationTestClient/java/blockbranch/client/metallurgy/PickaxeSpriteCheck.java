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

/** Native resource loading plus synthetic pack/scale regressions; no native art is encoded in fixtures. */
final class PickaxeSpriteCheck {
 static void require(boolean b,String m){if(!b)throw new AssertionError("PICKAXE "+m);}
 private static NativeImage image(ResourceManager resources,String name)throws IOException{try(var in=resources.getResource(Identifier.withDefaultNamespace("textures/item/"+name+"_pickaxe.png")).orElseThrow().open()){return NativeImage.read(in);}}
 static void verify(ResourceManager resources)throws Exception{
  try(var iron=image(resources,"iron");var diamond=image(resources,"diamond");var gold=image(resources,"golden")){
   for(var material:PickaxeSpriteSource.PALETTES.keySet())try(var result=PickaxeSpriteSource.derive(resources,material,false)){
    require(result!=null&&result.getWidth()==iron.getWidth(),"installed native derivation "+material);int changed=0,wood=0;
    for(int y=0;y<iron.getHeight();y++)for(int x=0;x<iron.getWidth();x++){int a=iron.getPixel(x,y),b=result.getPixel(x,y);require((a>>>24)==(b>>>24),"native alpha silhouette retained");if(a==diamond.getPixel(x,y)&&a==gold.getPixel(x,y)){require(a==b,"shared native handle/transparent pixels exact");if((a>>>24)>0)wood++;}else if(a!=b)changed++;}
    require(changed>0&&wood>0,"native head recolored; real handle retained");
   }
   try(var inlay=PickaxeSpriteSource.derive(resources,"steel",true)){
    int colored=0;for(int y=0;y<inlay.getHeight();y++)for(int x=0;x<inlay.getWidth();x++)if(inlay.getPixel(x,y)!=0){require((iron.getPixel(x,y)>>>24)>0&&(iron.getPixel(x,y)!=diamond.getPixel(x,y)||iron.getPixel(x,y)!=gold.getPixel(x,y)),"bismuth inlay stays inside metal head");colored++;}require(colored>0&&colored<16,"restrained contiguous native crown stripe");
   }
  }
  for(int n:new int[]{16,32,64})try(var iron=new NativeImage(n,n,true);var a=new NativeImage(n,n,true);var b=new NativeImage(n,n,true)){
   for(int y=n/2;y<n;y++)for(int x=n/4;x<n/2;x++){iron.setPixel(x,y,0xff805530);a.setPixel(x,y,0xff805530);b.setPixel(x,y,0xff805530);}
   for(int y=n/8;y<n*3/8;y++)for(int x=n/4;x<n*3/4;x++){iron.setPixel(x,y,0xff000000|((x%3==0?40:x%3==1?160:230)*0x010101));a.setPixel(x,y,0xff307077);b.setPixel(x,y,0xffd0b040);}
   try(var result=PickaxeSpriteSource.sample(iron,a,b,PickaxeSpriteSource.PALETTES.get("steel"),false)){require(result!=null&&result.getWidth()==n,"scaled pack "+n);require(result.getPixel(n/4,n-1)==0xff805530,"scaled wood unchanged");require(result.getPixel(n/4,n/4)!=iron.getPixel(n/4,n/4),"scaled metal recolored");}
   b.setPixel(0,0,0xffffffff);require(PickaxeSpriteSource.sample(iron,a,b,PickaxeSpriteSource.PALETTES.get("steel"),false)==null,"incompatible donor alpha is not guessed");
  }
  var steel=Blockbranch.id("textures/item/steel_pickaxe.png");var inlayId=Blockbranch.id("textures/item/bismuth_pickaxe_inlay.png");var ironId=Identifier.withDefaultNamespace("textures/item/iron_pickaxe.png");var diamondId=Identifier.withDefaultNamespace("textures/item/diamond_pickaxe.png");
  require(!PickaxeSpriteSource.directOverride(resources,steel),"default own resource recognized without pack-ID assumptions");
  byte[] custom;try(var i=new NativeImage(32,32,true)){i.setPixel(3,7,0xffe52ab7);custom=png(i);}
  var original=resources.getResource(steel).orElseThrow();var replacement=new Resource(original.source(),()->new ByteArrayInputStream(custom));
  var replaced=proxy(resources,Map.of(steel,Optional.of(replacement)));require(PickaxeSpriteSource.directOverride(replaced,steel),"same-pack-ID direct replacement detected");
  var result=load(replaced,"item/steel_pickaxe");try{require(result.width()==32&&pixels(result).getPixel(3,7)==0xffe52ab7,"native exact direct override");}finally{result.close();}
  result=load(replaced,"item/bismuth_pickaxe_inlay");try{int filled=0;for(int y=0;y<result.height();y++)for(int x=0;x<result.width();x++)if(pixels(result).getPixel(x,y)!=0)filled++;require(filled==0,"default inlay does not cross custom steel geometry");}finally{result.close();}
  var replacedInlay=proxy(resources,Map.of(inlayId,Optional.of(replacement)));result=load(replacedInlay,"item/bismuth_pickaxe_inlay");try{require(result.width()==32&&pixels(result).getPixel(3,7)==0xffe52ab7,"explicit inlay replacement wins");}finally{result.close();}
  var missing=proxy(resources,Map.of(diamondId,Optional.empty()));result=load(missing,"item/steel_pickaxe");try(var iron=image(resources,"iron")){try{require(result.width()==iron.getWidth(),"missing donor retains native iron dimensions");for(int y=0;y<iron.getHeight();y++)for(int x=0;x<iron.getWidth();x++)require(pixels(result).getPixel(x,y)==iron.getPixel(x,y),"unrecognized donor fallback exact native art");}finally{result.close();}}
  byte[] frames;try(var i=new NativeImage(16,32,true)){i.setPixel(2,2,0xff8293a4);i.setPixel(2,18,0xffa49382);frames=png(i);}
  var animated=new Resource(original.source(),()->new ByteArrayInputStream(frames),()->ResourceMetadata.fromJsonStream(new ByteArrayInputStream("{\"animation\":{\"frametime\":2}}".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  var meta=new Resource(original.source(),()->new ByteArrayInputStream("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  var animation=proxy(resources,Map.of(ironId,Optional.of(animated),Identifier.withDefaultNamespace("textures/item/iron_pickaxe.png.mcmeta"),Optional.of(meta)));result=load(animation,"item/steel_pickaxe");try{require(result.isAnimated()&&result.width()==16&&result.height()==16,"animated native fallback preserved");}finally{result.close();}
  Blockbranch.LOGGER.info("PICKAXE_CHECK PASS seven native masks/handles, head-only inlay, scaled16/32/64, direct/custom-inlay overrides, missing-donor and animation fallback");
 }
 private static byte[] png(NativeImage i)throws IOException{var b=new java.awt.image.BufferedImage(i.getWidth(),i.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);b.setRGB(0,0,i.getWidth(),i.getHeight(),i.makePixelArray(),0,i.getWidth());var out=new ByteArrayOutputStream();javax.imageio.ImageIO.write(b,"png",out);return out.toByteArray();}
 private static ResourceManager proxy(ResourceManager base,Map<Identifier,Optional<Resource>> changes){return(ResourceManager)Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,m,args)->m.getName().equals("getResource")&&changes.containsKey(args[0])?changes.get(args[0]):m.invoke(base,args));}
 private static NativeImage pixels(SpriteContents s)throws Exception{var f=SpriteContents.class.getDeclaredField("originalImage");f.setAccessible(true);return(NativeImage)f.get(s);}
 private static SpriteContents load(ResourceManager r,String name){SpriteContents[] out={null};var target=Blockbranch.id(name);new PickaxeSpriteSource().run(r,new SpriteSource.Output(){public void add(Identifier id,SpriteSource.DiscardableLoader loader){if(id.equals(target))out[0]=loader.get(SpriteResourceLoader.create(Set.of()));}public void removeAll(java.util.function.Predicate<Identifier> f){}});require(out[0]!=null,"sprite source load "+name);return out[0];}
 private PickaxeSpriteCheck(){}
}
