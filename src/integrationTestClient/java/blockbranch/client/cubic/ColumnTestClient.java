package blockbranch.client.cubic;
import blockbranch.cubic.SparseColumn;
public final class ColumnTestClient {
    public static void initialize() {
        if(Boolean.getBoolean("blockbranch.columnRenderSmoke"))RenderWindowSmoke.register();
        if(Boolean.getBoolean("blockbranch.columnClientSmoke"))ClientColumnSmoke.register();
        if(Boolean.getBoolean("blockbranch.sectionCopiesSmoke"))net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.level==null||client.player==null)return;
            // Observe an actual received profile; no test call remains in the packet handler.
            var pos=client.player.blockPosition();
            for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){
                var chunk=client.level.getChunk((pos.getX()>>4)+dx,(pos.getZ()>>4)+dz,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
                if(chunk==null)continue;
                var terrain=((SparseColumn)chunk).blockbranch$terrain();
                if(terrain!=null)ProceduralSectionCopiesSmoke.verify(client.level,terrain.base(),chunk.getPos());
            }
        });
    }
}
