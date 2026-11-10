package blockbranch.client.metallurgy;

import blockbranch.metallurgy.*;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.*;
import net.minecraft.world.phys.AABB;

/** Native always-on-top pale marks convey location, never the ore's model, color or name. */
public final class OrePulseClient {
    private static ClientLevel owner;
    private static List<BlockPos> positions=List.of();
    private static int remaining;
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(OrePulsePacket.TYPE,(packet,context)->receive(packet,context.client()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear());
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(client.level!=owner||client.player==null||!client.player.isAlive()){clear();return;}if(remaining>0&&--remaining==0)clear();});
        LevelExtractionEvents.END_EXTRACTION.register(context->{
            if(context.level()!=owner||remaining<=0)return;
            var collector=new SimpleGizmoCollector();
            try(var ignored=Gizmos.withCollector(collector)){
                int alpha=Math.min(210,remaining*14);int color=(alpha<<24)|0xe5e4d4;
                for(var pos:positions){double x=pos.getX()+.5,y=pos.getY()+.5,z=pos.getZ()+.5;Gizmos.cuboid(new AABB(x-.18,y-.18,z-.18,x+.18,y+.18,z+.18),GizmoStyle.strokeAndFill(color,1.5F,(Math.min(32,alpha)<<24)|0xe5e4d4)).setAlwaysOnTop();}
            }
            context.levelRenderer().addMainThreadGizmos(collector.drainGizmos());
        });
    }
    private static void receive(OrePulsePacket packet,Minecraft client){
        if(client.level==null||client.player==null||!client.level.dimension().identifier().equals(packet.dimension())||client.player.blockPosition().distSqr(packet.center())>16*16)return;
        owner=client.level;positions=packet.positions();remaining=positions.isEmpty()?0:OrePulse.DURATION;
    }
    private static void clear(){owner=null;positions=List.of();remaining=0;}
    private OrePulseClient(){}
}
