package blockbranch.client.horizon;
import static blockbranch.client.horizon.HorizonTileChecks.*;
import static blockbranch.horizon.HorizonTiles.key;
import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
/**
 * Client half of the loopback dedicated-server reconnect check; the server half,
 * {@code blockbranch.horizon.HorizonReconnectCheck}, describes the sequence. This side checks
 * that the target's tile is drawn, that disconnecting clears every client tile, and that
 * rejoining the still-running server draws the gold layer placed and saved while away.
 * Writes {@code result.txt} and a {@code HORIZON_RECONNECT} log line per step.
 */
public final class HorizonReconnectReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.horizonReconnectDirectory"));
    private static final String ADDRESS=System.getProperty("blockbranch.horizonReconnectAddress");
    private static final long BEGAN=System.nanoTime(),DEADLINE=780_000_000_000L,STEP_DEADLINE=240_000_000_000L;
    private static int targetX,targetZ,goldY,phase;
    private static long phaseBegan=System.nanoTime(),lastLog;
    private static boolean finished;

    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(HorizonReconnectReview::tick);}

    private static void tick(Minecraft client) {
        if(finished)return;
        try {
            long now=System.nanoTime();
            if(now-BEGAN>DEADLINE)throw new AssertionError("Bounded reconnect deadline; phase "+phase);
            if(now-phaseBegan>STEP_DEADLINE)throw new AssertionError("Phase "+phase+" deadline; tile "+describe(tile()));
            var server=DIR.resolve("server.txt");
            if(Files.exists(server)&&Files.readString(server).startsWith("FAIL"))throw new AssertionError("Server: "+Files.readString(server));
            var screen=client.gui.screen();String name=screen==null?"":screen.getClass().getSimpleName();
            if(name.equals("DisconnectedScreen"))throw new AssertionError("Disconnected by the server");
            if(name.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
            if(now-lastLog>15_000_000_000L){lastLog=now;log("phase=%d screen=%s tile=%s stats=%s",phase,name,describe(tile()),HorizonClient.stats());}
            switch(phase) {
                case 0->{
                    // First connection: the target's tile is cached and drawn.
                    if(client.level==null||!Files.exists(DIR.resolve("target.txt")))return;
                    if(targetX==0&&targetZ==0){var t=Files.readString(DIR.resolve("target.txt")).trim().split(" ");targetX=Integer.parseInt(t[0]);targetZ=Integer.parseInt(t[1]);}
                    var t=tile();
                    if(!settled(t))return;
                    if(count(t.data(),layer("gold"))!=0)throw new AssertionError("Gold before the edit");
                    log("first connection drew the target: %s, %s; disconnecting",describe(t),HorizonClient.stats());
                    client.disconnect(new TitleScreen(),false);next();
                }
                case 1->{
                    // Disconnected: no tiles survive. Rejoin once the server has edited and saved the target.
                    if(!(screen instanceof TitleScreen)||client.level!=null||client.getConnection()!=null)return;
                    var stats=HorizonClient.stats();
                    if(stats.tiles()!=0)throw new AssertionError("Client tiles left after disconnecting: "+stats);
                    var edited=DIR.resolve("edited.txt");
                    if(!Files.exists(edited))return;
                    goldY=Integer.parseInt(Files.readString(edited).trim());
                    log("disconnected with %s; rejoining %s",stats,ADDRESS);
                    ConnectScreen.startConnecting(new TitleScreen(),client,ServerAddress.parseString(ADDRESS),new ServerData("Horizon reconnect",ADDRESS,ServerData.Type.OTHER),false,null);
                    next();
                }
                case 2->{
                    // Rejoined: the tile comes from the saved, edited chunk.
                    if(client.level==null)return;
                    var t=tile();
                    if(!settled(t))return;
                    if(!uniform(t.data(),layer("gold"),goldY)) {
                        if(System.nanoTime()-phaseBegan>STEP_DEADLINE/2)throw new AssertionError("Rejoin drew stale data: "+describe(t));
                        return;
                    }
                    log("rejoined and drew the edit: %s",describe(t));
                    Files.writeString(DIR.resolve("drawn.txt"),"drawn");next();
                }
                case 3->{
                    if(!Files.exists(server))return;
                    String s=Files.readString(server);
                    finished=true;
                    Files.writeString(DIR.resolve("result.txt"),"PASS horizon dedicated reconnect: client tiles cleared on disconnect, saved edit drawn after rejoining; "+s);
                    log("finished: %s",s);
                    client.disconnect(new TitleScreen(),false);client.stop();
                }
                default->{}
            }
        } catch(Throwable t) {
            finished=true;Blockbranch.LOGGER.error("HORIZON_RECONNECT_FAIL",t);
            try{Files.writeString(DIR.resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}
            client.stop();
        }
    }
    private static void next(){phase++;phaseBegan=System.nanoTime();}
    private static HorizonClient.TileState tile(){return targetX==0&&targetZ==0?null:HorizonClient.tile(key(0,targetX,targetZ));}
    private static void log(String format,Object... args){Blockbranch.LOGGER.info("HORIZON_RECONNECT {}",String.format(Locale.ROOT,format,args));}
    private HorizonReconnectReview(){}
}
