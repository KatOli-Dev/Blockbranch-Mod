package blockbranch.client.time;

import blockbranch.time.DaylightCycle;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.clock.WorldClocks;

/** Real TCP join, pause and changed-rate synchronization with native client clocks. */
public final class DaylightSyncClient {
    public static void register(){
        Path directory=Path.of(System.getProperty("blockbranch.daylightDirectory"));
        long deadline=System.nanoTime()+120_000_000_000L;int[] phase={-1},ticks={0};boolean[] done={false};
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(done[0])return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("client clock sync timeout at phase "+phase[0]);
            if(client.level==null||client.player==null)return;
            var holder=client.level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
            var clock=client.level.clockManager().getInstance(holder);
            if(phase[0]==-1){if(clock.rate()!=DaylightCycle.RATE)return;phase[0]=0;ticks[0]=0;Files.writeString(directory.resolve("request.txt"),"0");}
            if(++ticks[0]<20)return;
            if(phase[0]==0&&clock.totalTicks()>=6000&&clock.totalTicks()<6100&&clock.rate()==DaylightCycle.RATE){phase[0]=1;ticks[0]=0;Files.writeString(directory.resolve("request.txt"),"1");}
            else if(phase[0]==1&&clock.rate()==0.0f){phase[0]=2;ticks[0]=0;Files.writeString(directory.resolve("request.txt"),"2");}
            else if(phase[0]==2&&clock.rate()==0.25f&&clock.totalTicks()>=18000&&clock.totalTicks()<18100){
                phase[0]=3;ticks[0]=0;Files.writeString(directory.resolve("request.txt"),"3");
            }
            else if(phase[0]==3&&clock.rate()==DaylightCycle.RATE&&clock.totalTicks()>=24000&&Files.exists(directory.resolve("sleep.txt"))){
                Files.writeString(directory.resolve("result.txt"),"PASS real TCP native clock join rate, time set, pause, resumed custom rate and actual player bed sleep\n");done[0]=true;client.stop();
            }
        }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}});
    }
    private DaylightSyncClient(){}
}
