package blockbranch.client.realistic;

import java.util.IdentityHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Coalesce column-wide work when hundreds of sparse sections arrive together. */
public final class ColumnReceiveBatch {
    private static final boolean MEASURE = Boolean.getBoolean("blockbranch.realisticClientSmoke");
    static long received, refreshed;
    private static final IdentityHashMap<ChunkAccess, Pending> PENDING = new IdentityHashMap<>();
    private static final class Pending {
        final ClientLevel level;
        int min, max;
        Pending(ClientLevel level, int y) { this.level = level; min = max = y; }
    }
    public static void changed(ClientLevel level, ChunkAccess chunk, int sectionY) {
        if (MEASURE) received++;
        var pending = PENDING.computeIfAbsent(chunk, ignored -> new Pending(level, sectionY));
        pending.min = Math.min(pending.min, sectionY);
        pending.max = Math.max(pending.max, sectionY);
    }
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (PENDING.isEmpty()) return;
            var view = client.levelRenderer.viewArea();
            PENDING.forEach((chunk, pending) -> {
                var level = pending.level;
                if (client.level != level || level.getChunkSource().getChunk(chunk.getPos().x(), chunk.getPos().z(), ChunkStatus.FULL, false) != chunk) return;
                chunk.initializeLightSources();
                if (MEASURE) refreshed++;
                if (view == null) return;
                int min = Math.max(pending.min - 1, view.minSectionY());
                int max = Math.min(pending.max + 1, view.maxSectionY());
                if (min <= max) level.setSectionRangeDirty(chunk.getPos().x() - 1, min, chunk.getPos().z() - 1,
                    chunk.getPos().x() + 1, max, chunk.getPos().z() + 1);
            });
            PENDING.clear();
        });
    }
    private ColumnReceiveBatch() {}
}
