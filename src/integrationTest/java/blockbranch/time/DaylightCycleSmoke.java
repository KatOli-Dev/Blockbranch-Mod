package blockbranch.time;

import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/** Native clock duration, command, tick isolation and three-launch persistence regression. */
public final class DaylightCycleSmoke {
    private static int ticks,phase;
    private static boolean initialized,done;
    private static long startGame,startClock;
    private static Path directory;
    public static void register(){
        ServerTickEvents.START_SERVER_TICK.register(server->{if(initialized||done)return;initialized=true;try{
            directory=Path.of(System.getProperty("blockbranch.daylightDirectory"));
            var phaseFile=directory.resolve("phase.txt");
            phase=Files.exists(phaseFile)?Integer.parseInt(Files.readString(phaseFile).trim()):0;
            var clock=clock(server);var instance=server.clockManager().getInstance(clock);
            require(server.overworld().getDataStorage().computeIfAbsent(DaylightCycle.TYPE).applied(),"migration marker");
            require(instance.rate()==(phase==2?1.0f:DaylightCycle.RATE),"saved/default rate phase "+phase);
            if(phase>0){
                var expected=ClockState.CODEC.parse(JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(Files.readString(directory.resolve("clock.json")))).getOrThrow();
                require(instance.packState().equals(expected),"exact native clock save/reload including fraction and pause");
            }
            require(server.clockManager().getInstance(server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.THE_END)).rate()==1.0f,"End rate unchanged");
            require(server.getLevel(Level.NETHER).dimensionType().defaultClock().isEmpty(),"Nether remains clockless");
            duration();
            if(phase==0){
                instance.loadFrom(new ClockState(43210,0.375f,1.0f,true));
                server.overworld().getDataStorage().set(DaylightCycle.TYPE,new DaylightCycle.Applied());
                DaylightCycle.applyDefault(server);
                require(instance.totalTicks()==43210&&instance.partialTick()==0.375f&&instance.isPaused()&&instance.rate()==DaylightCycle.RATE,"migration retains native time/fraction/pause");
                instance.loadFrom(new ClockState(43210,0.375f,0.5f,true));
                server.overworld().getDataStorage().set(DaylightCycle.TYPE,new DaylightCycle.Applied());
                DaylightCycle.applyDefault(server);
                require(instance.packState().equals(new ClockState(43210,0.375f,0.5f,true)),"pre-existing custom rate preserved");
            }
            command(server,"time resume");command(server,"time set 1000");command(server,"time add 250");
            require(instance.totalTicks()==1250,"set/add retain clock tick units");
            require(command(server,"time query time")==1250,"native daytime query");
            command(server,"time pause");require(instance.isPaused(),"pause command");
            command(server,"time resume");require(!instance.isPaused(),"resume command");
            command(server,"time rate 0.25");DaylightCycle.applyDefault(server);require(instance.rate()==0.25f,"operator rate preserved");
            command(server,"time rate "+DaylightCycle.RATE);
            command(server,"time set 14000");server.clockManager().moveToTimeMarker(clock,ClockTimeMarkers.WAKE_UP_FROM_SLEEP);
            require(instance.totalTicks()>14000&&instance.totalTicks()%24000<2000,"native sleep wake marker");
            command(server,"time set 12345");
            server.overworld().getGameRules().set(GameRules.ADVANCE_TIME,false,server);
            startGame=server.overworld().getGameTime();startClock=instance.totalTicks();
        }catch(Throwable error){fail(server,error);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(done||!initialized)return;try{
            ticks++;
            var instance=server.clockManager().getInstance(clock(server));
            if(ticks==20){
                require(instance.totalTicks()==startClock,"advance_time false freezes clock");
                require(server.overworld().getGameTime()-startGame==20,"game time still advances at normal tick cadence");
                server.overworld().getGameRules().set(GameRules.ADVANCE_TIME,true,server);
                startGame=server.overworld().getGameTime();startClock=instance.totalTicks();
            }
            if(ticks==220){
                require(server.overworld().getGameTime()-startGame==200,"200 independent simulation ticks");
                require(instance.totalTicks()-startClock==13,"200 simulation ticks advance daylight by 13 whole ticks");
                require(server.tickRateManager().tickrate()==20.0f,"simulation tick rate unchanged");
                command(server,"time pause");
                if(phase==1)command(server,"time rate 1");
                DaylightCycle.applyDefault(server);
                var encoded=ClockState.CODEC.encodeStart(JsonOps.INSTANCE,instance.packState()).getOrThrow();
                Files.writeString(directory.resolve("clock.json"),encoded.toString());
                Files.writeString(directory.resolve("phase.txt"),Integer.toString(phase+1));
                Files.writeString(directory.resolve("result.txt"),"PASS phase="+phase+" duration=360000 ticks commands,pause,gamerule,dimensions,simulation,persistence\n");
                done=true;server.saveEverything(false,true,true);server.halt(false);
            }
        }catch(Throwable error){fail(server,error);}});
    }
    private static net.minecraft.core.Holder<WorldClock> clock(MinecraftServer server){return server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);}
    private static int command(MinecraftServer server,String command)throws Exception{return server.getCommands().getDispatcher().execute(command,server.createCommandSourceStack());}
    private static void duration(){
        var clock=new ServerClockManager.ServerClockInstance();clock.loadFrom(new ClockState(0,0,DaylightCycle.RATE,false));
        for(int i=0;i<359999;i++)clock.tick();require(clock.totalTicks()==23999,"not a complete day before five hours");
        clock.tick();require(clock.totalTicks()==24000,"full day at five hours at 20 TPS");
        var saved=clock.packState();clock.loadFrom(new ClockState(saved.totalTicks(),saved.partialTick(),saved.rate(),true));
        for(int i=0;i<30;i++)clock.tick();require(clock.totalTicks()==saved.totalTicks()&&clock.partialTick()==saved.partialTick(),"paused fractional clock");
    }
    private static void require(boolean pass,String description){if(!pass)throw new AssertionError(description);}
    private static void fail(MinecraftServer server,Throwable error){done=true;error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}server.halt(false);}
    private DaylightCycleSmoke(){}
}
