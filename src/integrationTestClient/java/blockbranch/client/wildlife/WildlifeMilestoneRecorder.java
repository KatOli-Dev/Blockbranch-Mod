package blockbranch.client.wildlife;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;

/** Opt-in 5fps capture of actual native framebuffer frames, without audio. */
final class WildlifeMilestoneRecorder {
    private static boolean started,busy,finished;
    private static long start,next;
    private static final List<Map<String,Object>> frames=new ArrayList<>();
    static void start(){started=true;start=System.nanoTime();next=start;}
    static void tick(Minecraft client)throws Exception{
        long now=System.nanoTime();if(!started||finished||busy||now<next)return;
        Files.createDirectories(client.gameDirectory.toPath().resolve("screenshots/milestone-frames"));
        String name=String.format(Locale.ROOT,"frame-%05d.png",frames.size());
        frames.add(Map.of("file",name,"seconds",(now-start)/1e9));next=now+200_000_000L;busy=true;
        net.minecraft.client.Screenshot.grab(client.gameDirectory,"milestone-frames/"+name,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->busy=false));
    }
    static boolean finish(Minecraft client)throws Exception{
        if(busy)return false;
        if(!finished){
            var directory=client.gameDirectory.toPath().resolve("screenshots/milestone-frames");
            for(var frame:frames)if(!Files.isReadable(directory.resolve((String)frame.get("file"))))throw new AssertionError("native video frame missing");
            Files.writeString(directory.resolve("frames.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("kind","actual native framebuffer samples; silent, staged, at most5fps","frames",frames))+"\n");
            finished=true;
        }
        return true;
    }
    private WildlifeMilestoneRecorder(){}
}
