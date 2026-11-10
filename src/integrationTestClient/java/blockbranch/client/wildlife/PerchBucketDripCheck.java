package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Opt-in native regressions; synthetic colors/shapes do not encode Minecraft artwork. */
final class PerchBucketDripCheck {
    private static void require(boolean v,String message){if(!v)throw new AssertionError("perch drip: "+message);}
    static void verify(ResourceManager resources){
        for(int n:new int[]{16,32,64}){
            try(var water=new NativeImage(n,n,true);var cod=new NativeImage(n,n,true);var salmon=new NativeImage(n,n,true);var overlay=new NativeImage(16,16,true)){
                int blue=0xff1844b0,metal=0xffaaaaaa,fish=0xff447722;
                for(int y=0;y<n;y++)for(int x=0;x<n;x++)water.setPixel(x,y,metal);
                water.setPixel(n/2,n/4,blue);cod.copyFrom(water);salmon.copyFrom(water);
                cod.setPixel(n/2,n*3/4,blue);salmon.setPixel(n/2,n*3/4,blue);
                cod.setPixel(1,n*3/4,fish);salmon.setPixel(1,n*3/4,fish); // shared donor fish is not water
                cod.setPixel(2,n*3/4,blue); // water-colored donor disagreement is excluded
                overlay.setPixel(1,1,fish);
                try(var sampled=PerchBucketDripSource.sample(water,cod,salmon,overlay)){
                    require(sampled!=null&&sampled.getWidth()==n,"scaled resource pack "+n);
                    int colored=0;for(int y=0;y<n;y++)for(int x=0;x<n;x++)if(sampled.getPixel(x,y)!=0)colored++;
                    require(colored==(n/16)*(n/16)+1,"only original fish + consensus drip");
                    require(sampled.getPixel(n/2,n*3/4)==blue,"exact runtime sample");
                    require(sampled.getPixel(1,n*3/4)==0&&sampled.getPixel(2,n*3/4)==0,"donor fish/disagreement excluded");
                }
                cod.copyFrom(water);salmon.copyFrom(water);
                require(PerchBucketDripSource.sample(water,cod,salmon,overlay)==null,"no drip fallback");
                water.setPixel(n/2,n-1,blue);
                require(PerchBucketDripSource.sample(water,cod,salmon,overlay)==null,"ambiguous low waterline fallback");
            }
        }
        try(var water=new NativeImage(16,16,true);var a=new NativeImage(32,32,true);var b=new NativeImage(16,16,true);var o=new NativeImage(16,16,true)){
            require(PerchBucketDripSource.sample(water,a,b,o)==null,"dimension mismatch fallback");
        }
        var missing=(ResourceManager)Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,method,args)->{
            if(method.getName().equals("getResource")&&args[0].equals(Identifier.withDefaultNamespace("textures/item/cod_bucket.png")))return Optional.empty();
            return method.invoke(resources,args);
        });
        final SpriteContents[] result={null};
        new PerchBucketDripSource().run(missing,new SpriteSource.Output(){
            @Override public void add(Identifier id,SpriteSource.DiscardableLoader loader){result[0]=loader.get(SpriteResourceLoader.create(Set.of()));}
            @Override public void removeAll(java.util.function.Predicate<Identifier> filter){}
        });
        require(result[0]!=null&&PerchBucketDripSource.status.equals("original-overlay-fallback"),"missing donor loads existing overlay");
        result[0].close();
        Blockbranch.LOGGER.info("PERCH_BUCKET_DRIP_CHECK PASS scaled16/32/64 donorFishExcluded dimensionMismatch noDrip lowWaterline missingDonorFallback");
    }
    private PerchBucketDripCheck(){}
}
