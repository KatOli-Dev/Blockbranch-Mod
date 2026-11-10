package blockbranch.qa;

import blockbranch.Blockbranch;
import blockbranch.wildlife.WildlifeProbe;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Opt-in native traversal/teleport measurement. Does not preload or edit terrain. */
public final class TerrainLoadingAudit {
    private static ServerPlayer player;
    private static String phase="join";
    private static long start, previous, phaseStart;
    private static LoadingTickEvent tickEvent;
    private static final java.lang.management.ThreadMXBean CPU=java.lang.management.ManagementFactory.getThreadMXBean();
    private static long startCpu;
    private static long cpu(){return CPU.isCurrentThreadCpuTimeSupported()&&CPU.isThreadCpuTimeEnabled()?CPU.getCurrentThreadCpuTime():-1;}
    private static int ticks, loaded, generated;
    private static double x,y,z;
    private static final List<Map<String,Object>> samples=new ArrayList<>();
    private static Path directory(){return Path.of(System.getProperty("blockbranch.terrainAuditResult")).getParent();}
    public static void register(){
        if(System.getProperty("blockbranch.terrainAuditResult")==null)return; // Shared initializer also runs in the paired client.
        try{Files.writeString(directory().resolve("process-id.txt"),Long.toString(ProcessHandle.current().pid()));}catch(Exception e){throw new RuntimeException(e);}
        ServerChunkEvents.CHUNK_LOAD.register((level,chunk,fresh)->{loaded++;if(fresh)generated++;});
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            player=handler.player;
            try{
                var origin=directory().resolve("origin.txt");
                if(!Files.exists(origin))Files.writeString(origin,player.getX()+" "+(player.getY()+4)+" "+player.getZ());
                var values=Files.readString(origin).split(" ");x=Double.parseDouble(values[0]);y=Double.parseDouble(values[1]);z=Double.parseDouble(values[2]);
                player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                player.teleportTo(player.level(),x,y,z,Set.of(),-90,0,true);
                transition("local");
            }catch(Exception error){throw new RuntimeException(error);}
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.halt(false));
        ServerTickEvents.START_SERVER_TICK.register(server->{start=System.nanoTime();startCpu=cpu();tickEvent=new LoadingTickEvent();tickEvent.begin();});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(player==null)return;
            long now=System.nanoTime();
            tickEvent.end();long endCpu=cpu();tickEvent.threadCpuNanos=startCpu<0||endCpu<0?-1:endCpu-startCpu;tickEvent.phase=phase;tickEvent.loaded=loaded;tickEvent.generated=generated;tickEvent.commit();
            var row=new LinkedHashMap<String,Object>();row.put("threadCpuMs",tickEvent.threadCpuNanos<0?-1:tickEvent.threadCpuNanos/1e6);row.put("utcEpochMillis",System.currentTimeMillis());row.put("x",player.getX());row.put("y",player.getY());row.put("z",player.getZ());row.put("phase",phase);row.put("tickMs",(now-start)/1e6);row.put("intervalMs",previous==0?0:(now-previous)/1e6);row.put("elapsedMs",(now-phaseStart)/1e6);row.put("loaded",loaded);row.put("generated",generated);samples.add(row);previous=now;
            if(++ticks%100==0)Blockbranch.LOGGER.info("TERRAIN_AUDIT_PROGRESS phase={} ticks={} position={} loaded={} generated={}",phase,ticks,player.position(),loaded,generated);
            try{
                if(phase.equals("local")&&ticks>=200&&System.nanoTime()-phaseStart>=Long.getLong("blockbranch.auditStationarySeconds",0L)*1_000_000_000L&&player.level().isPositionEntityTicking(player.blockPosition())){
                    transition("traversal");ServerPlayNetworking.send(player,new WildlifeProbe(1,"forward"));
                }else if(phase.equals("traversal")&&player.getX()>=x+Double.parseDouble(System.getProperty("blockbranch.auditDistance","320"))){
                    ServerPlayNetworking.send(player,new WildlifeProbe(2,"stop"));transition("settle");
                }else if(phase.equals("settle")&&ticks>=200&&!Boolean.getBoolean("blockbranch.auditNoTeleport")){
                    transition("teleport");
                    player.teleportTo(player.level(),x+8192,y+256,z+8192,Set.of(),-90,0,true);
                }else if((phase.equals("teleport")&&ticks>=400||phase.equals("settle")&&ticks>=200&&Boolean.getBoolean("blockbranch.auditNoTeleport"))&&player.level().isPositionEntityTicking(player.blockPosition())){
                    Files.writeString(directory().resolve("ticks.json"),new GsonBuilder().setPrettyPrinting().create().toJson(samples));
                    Files.writeString(Path.of(System.getProperty("blockbranch.terrainAuditResult")),"PASS native creative flight "+System.getProperty("blockbranch.auditDistance","320")+"m; teleport="+!Boolean.getBoolean("blockbranch.auditNoTeleport")+"; no terrain edits\n");
                    Blockbranch.LOGGER.info("TERRAIN_AUDIT PASS position={} loaded={} generated={}",player.position(),loaded,generated);
                    ServerPlayNetworking.send(player,new WildlifeProbe(0,"done"));player=null;
                }
                if(ticks>3600)throw new AssertionError("phase failed to finish: "+phase);
            }catch(Exception error){throw new RuntimeException(error);}
        });
    }
    private static void transition(String next){phase=next;ticks=0;phaseStart=System.nanoTime();Blockbranch.LOGGER.info("TERRAIN_AUDIT_PHASE {} origin={},{},{}",next,x,y,z);}
    private TerrainLoadingAudit(){}
}
