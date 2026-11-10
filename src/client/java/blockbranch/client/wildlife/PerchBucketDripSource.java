package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/** Atlas-time sampling only: no native pixels stored in the mod or written to disk. */
public final class PerchBucketDripSource implements SpriteSource {
    public static final MapCodec<PerchBucketDripSource> CODEC=MapCodec.unit(PerchBucketDripSource::new);
    private static final Identifier SPRITE=Blockbranch.id("item/perch_bucket_overlay");
    private static final Identifier OVERLAY=Blockbranch.id("textures/item/perch_bucket_overlay.png");
    private static final Identifier WATER=Identifier.withDefaultNamespace("textures/item/water_bucket.png");
    private static final Identifier COD=Identifier.withDefaultNamespace("textures/item/cod_bucket.png");
    private static final Identifier SALMON=Identifier.withDefaultNamespace("textures/item/salmon_bucket.png");
    private static final int MAX_SIZE=256,MAX_BYTES=1024*1024;
    static volatile int reloads,sampledPixels;
    static volatile String status="not-loaded";

    public static void register(){SpriteSourceRegistry.register(Blockbranch.id("perch_bucket_drip"),CODEC);}
    @Override public MapCodec<? extends SpriteSource> codec(){return CODEC;}
    @Override public void run(ResourceManager resources,Output output){output.add(SPRITE,loader->load(resources,loader));}

    private SpriteContents load(ResourceManager resources,SpriteResourceLoader loader){
        NativeImage combined=null;
        try(var water=read(resources,WATER);var cod=read(resources,COD);var salmon=read(resources,SALMON);var overlay=read(resources,OVERLAY)){
            combined=sample(water,cod,salmon,overlay);
            if(combined!=null){
                int count=changedPixels(combined,overlay);
                sampledPixels=count;status="sampled";reloads++;
                Blockbranch.LOGGER.info("PERCH_BUCKET_DRIP reload={} status={} pixels={} size={}",reloads,status,count,combined.getWidth());
                var sprite=new SpriteContents(SPRITE,new FrameSize(combined.getWidth(),combined.getHeight()),combined);
                combined=null; // SpriteContents/atlas owns and closes the image from here.
                return sprite;
            }
        }catch(IOException|RuntimeException e){
            Blockbranch.LOGGER.debug("Perch bucket drip unavailable; retaining original overlay: {}",e.toString());
        }finally{if(combined!=null)combined.close();}
        sampledPixels=0;status="original-overlay-fallback";reloads++;
        Blockbranch.LOGGER.info("PERCH_BUCKET_DRIP reload={} status={}",reloads,status);
        return resources.getResource(OVERLAY).map(resource->loader.loadSprite(SPRITE,resource))
            .orElseGet(()->new SpriteContents(SPRITE,new FrameSize(16,16),new NativeImage(16,16,true)));
    }

    /** Reject oversized/animated/unexpected inputs before native image allocation. */
    private static NativeImage read(ResourceManager resources,Identifier id)throws IOException{
        if(resources.getResource(Identifier.fromNamespaceAndPath(id.getNamespace(),id.getPath()+".mcmeta")).isPresent())
            throw new IOException("texture metadata requires unmodified native loading");
        var resource=resources.getResource(id).orElseThrow(()->new IOException("missing "+id));
        byte[] bytes;
        try(var stream=resource.open()){bytes=stream.readNBytes(MAX_BYTES+1);}
        if(bytes.length>MAX_BYTES||bytes.length<33)throw new IOException("texture byte bound");
        var header=ByteBuffer.wrap(bytes);
        if(header.getLong()!=0x89504e470d0a1a0aL||header.getInt()!=13||header.getInt()!=0x49484452)
            throw new IOException("unexpected PNG header");
        int width=header.getInt(),height=header.getInt();
        if(width<16||width>MAX_SIZE||width!=height)throw new IOException("unsupported texture dimensions");
        return NativeImage.read(bytes);
    }

    /** Shared lower water colors only; donor fish/metal and unrelated differences are excluded. */
    static NativeImage sample(NativeImage water,NativeImage cod,NativeImage salmon,NativeImage overlay){
        int n=water.getWidth(),m=overlay.getWidth();
        if(n<16||n>MAX_SIZE||n%16!=0||water.getHeight()!=n||cod.getWidth()!=n||cod.getHeight()!=n
            ||salmon.getWidth()!=n||salmon.getHeight()!=n||overlay.getHeight()!=m||m<16||m>MAX_SIZE||m%16!=0)return null;
        Set<Integer> waterColors=new HashSet<>();int bottom=-1;
        for(int y=0;y<n;y++)for(int x=0;x<n;x++){
            int color=water.getPixel(x,y);
            int r=(color>>>16)&255,g=(color>>>8)&255,b=color&255;
            if((color>>>24)==255&&b>r*1.2&&b>g*1.1){waterColors.add(color);bottom=Math.max(bottom,y);}
        }
        // A blue mark already low on the bucket is not an unambiguous surface waterline.
        if(bottom<0||bottom>=n/2||waterColors.size()>64)return null;
        var result=new NativeImage(n,n,true);int copied=0;
        for(int y=0;y<n;y++)for(int x=0;x<n;x++)result.setPixel(x,y,overlay.getPixel(x*m/n,y*m/n));
        for(int y=bottom+1;y<n;y++)for(int x=0;x<n;x++){
            int c=cod.getPixel(x,y),o=result.getPixel(x,y);
            if(c!=water.getPixel(x,y)&&c==salmon.getPixel(x,y)&&waterColors.contains(c)
                &&(water.getPixel(x,y)>>>24)==255&&(o>>>24)==0){result.setPixel(x,y,c);copied++;}
        }
        // No clear drip or widespread changes: retain the original unmodified overlay.
        if(copied==0||copied>n*n/8){result.close();return null;}
        return result;
    }
    private static int changedPixels(NativeImage combined,NativeImage overlay){
        int n=combined.getWidth(),m=overlay.getWidth(),count=0;
        for(int y=0;y<n;y++)for(int x=0;x<n;x++)if(combined.getPixel(x,y)!=overlay.getPixel(x*m/n,y*m/n))count++;
        return count;
    }
}
