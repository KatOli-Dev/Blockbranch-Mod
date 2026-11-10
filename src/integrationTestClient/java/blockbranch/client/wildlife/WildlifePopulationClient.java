package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.WildlifeProbe;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
/** Keeps an actual client connected during the native unedited-terrain survey. */
public final class WildlifePopulationClient {
    public static void register(){
        long deadline=System.nanoTime()+2_400_000_000_000L;
        final boolean[] defaultsChecked={false},done={false};
        ClientPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.client().execute(()->{
            done[0]=true;
            try{Files.writeString(Path.of(System.getProperty("blockbranch.populationClientResult")),packet.stage()==0?"PASS\n":"FAIL "+packet.message()+"\n");}catch(Exception error){Blockbranch.LOGGER.error("Population result write",error);}context.client().stop();
        }));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            if(!done[0]){done[0]=true;var details=handler.getConnection().getDisconnectionDetails();
                String reason=details==null?"<no connection details>":details.reason().getString();
                Blockbranch.LOGGER.error("WILDLIFE_POPULATION_CLIENT disconnectReason={} memoryConnection={}",reason,handler.getConnection().isMemoryConnection());
                try{Files.writeString(Path.of(System.getProperty("blockbranch.populationClientResult")),"FAIL disconnected before survey completion: "+reason+"\n");}catch(Exception error){Blockbranch.LOGGER.error("Population disconnect result",error);}client.stop();}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.level!=null&&!defaultsChecked[0]){
                for(int i=0;i<64;i++){
                    var deer=new blockbranch.wildlife.RedDeer(blockbranch.wildlife.WildlifeRuntime.RED_DEER,client.level);
                    if(deer.hasAntlers())throw new AssertionError("Client deer default diverges from synced false default");
                }
                defaultsChecked[0]=true;Blockbranch.LOGGER.info("DEER_CLIENT_DEFAULT_SYNC PASS count=64 defaultAntlers=false");
            }
            if(System.nanoTime()>deadline){Blockbranch.LOGGER.error("WILDLIFE_POPULATION_CLIENT timeout");client.stop();}});
    }
    private WildlifePopulationClient(){}
}
