package blockbranch.client.qa;

import blockbranch.Blockbranch;
import blockbranch.wildlife.WildlifeProbe;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;

/** Holds native forward input for the server's matched route. */
public final class TerrainLoadingClient {
    private static final java.util.List<java.util.Map<String,Object>> metrics=new java.util.ArrayList<>();
    public static void register(){
        try{Files.writeString(Path.of(System.getProperty("blockbranch.terrainAuditClientResult")).resolveSibling("process-id.txt"),Long.toString(ProcessHandle.current().pid()));}catch(Exception e){throw new RuntimeException(e);}
        boolean[] forward={false},done={false};
        long deadline=System.nanoTime()+600_000_000_000L;
        ClientPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.client().execute(()->{
            forward[0]=packet.stage()==1;
            if(packet.stage()==0){blockbranch.client.cubic.ProceduralSectionCopiesSmoke.beforeDisconnect();done[0]=true;write("PASS\n");context.client().stop();}
        }));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            client.options.keyUp.setDown(forward[0]);
            if(client.level!=null){var cache=blockbranch.client.cubic.ProceduralSectionCopies.statistics();
                if(cache[2]>8192||cache[3]>64L*1024*1024)throw new AssertionError("Snapshot cache exceeded its admission bounds");
                if(metrics.isEmpty()||System.currentTimeMillis()-(long)metrics.getLast().get("utcEpochMillis")>=1000)
                    metrics.add(java.util.Map.of("utcEpochMillis",System.currentTimeMillis(),"fps",client.getFps(),"hits",cache[0],"misses",cache[1],"entries",cache[2],"weightBytes",cache[3]));
            }
            if(System.nanoTime()>deadline){write("FAIL deadline\n");client.stop();}
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            if(!done[0]){var details=handler.getConnection().getDisconnectionDetails();write("FAIL disconnected: "+(details==null?"pending reason; inspect transport log":details.reason().getString())+"\n");client.stop();}
        });
    }
    private static void write(String text){try{var path=Path.of(System.getProperty("blockbranch.terrainAuditClientResult"));Files.writeString(path,text);Files.writeString(path.resolveSibling("client-metrics.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(metrics));}catch(Exception error){Blockbranch.LOGGER.error("Terrain audit result",error);}}
    private TerrainLoadingClient(){}
}
