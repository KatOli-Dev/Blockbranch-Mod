package blockbranch.time;

import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Files coordinate QA only; clock traffic uses the unmodified native connection. */
public final class DaylightSyncSmoke {
    public static void register(){
        Path directory=Path.of(System.getProperty("blockbranch.daylightDirectory"));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
        int[] last={-1},ticks={0};
        boolean[] sleeping={false},done={false};
        ServerTickEvents.END_SERVER_TICK.register(server->{if(done[0])return;try{
            if(++ticks[0]>2400){Files.writeString(directory.resolve("result.txt"),"FAIL server timeout");server.halt(false);return;}
            if(sleeping[0]){
                var player=server.getPlayerList().getPlayers().getFirst();
                var clock=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK).getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
                var instance=server.clockManager().getInstance(clock);
                if(!player.isSleeping()&&instance.totalTicks()>=24000){
                    if(instance.rate()!=DaylightCycle.RATE)throw new AssertionError("sleep changed clock rate");
                    Files.writeString(directory.resolve("sleep.txt"),"PASS native player bed sleep skipped to morning at unchanged daylight rate");sleeping[0]=false;
                }
            }
            var request=directory.resolve("request.txt");if(!Files.exists(request))return;
            int phase=Integer.parseInt(Files.readString(request).trim());if(phase==last[0])return;last[0]=phase;
            var source=server.createCommandSourceStack();var dispatcher=server.getCommands().getDispatcher();
            if(phase==0){server.getPlayerList().getPlayers().getFirst().setGameMode(net.minecraft.world.level.GameType.CREATIVE);dispatcher.execute("time set 6000",source);}
            if(phase==1)dispatcher.execute("time pause",source);
            if(phase==2){dispatcher.execute("time set 18000",source);dispatcher.execute("time rate 0.25",source);dispatcher.execute("time resume",source);}
            if(phase==3){
                dispatcher.execute("time rate "+DaylightCycle.RATE,source);dispatcher.execute("time set 18000",source);
                var level=server.overworld();var head=new net.minecraft.core.BlockPos(0,65,0);var foot=head.south();
                for(int z=-2;z<=3;z++)for(int x=-2;x<=2;x++)level.setBlock(new net.minecraft.core.BlockPos(x,64,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                var bed=(net.minecraft.world.level.block.AbstractBedBlock)net.minecraft.world.level.block.Blocks.BED.red();
                var state=bed.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,net.minecraft.core.Direction.NORTH);
                level.setBlock(foot,state.setValue(net.minecraft.world.level.block.AbstractBedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.FOOT),2);
                state=state.setValue(net.minecraft.world.level.block.AbstractBedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD);level.setBlock(head,state,2);
                var player=server.getPlayerList().getPlayers().getFirst();dispatcher.execute("tp DaylightAudit 0.5 65 1.5",source);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var result=player.startSleepInBed(bed,state,bed.getBedRule(level,head),head);
                if(result.left().isPresent()||!player.isSleeping())throw new AssertionError("native bed entry failed: "+result);
                sleeping[0]=true;
            }
        }catch(Throwable error){done[0]=true;error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}server.halt(false);}});
    }
    private DaylightSyncSmoke(){}
}
