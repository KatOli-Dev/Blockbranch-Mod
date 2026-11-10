package blockbranch.cubic;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/** Player vertical windows own lighting and resident overlays, independently of column depth. */
public final class VerticalRuntime {
    private static final Map<ServerGamePacketListenerImpl,View> VIEWS=new IdentityHashMap<>();
    private static final Map<LevelChunk,ServerLevel> LOADED=new IdentityHashMap<>();
    private static int ticks;
    private static final class View {
        final ServerLevel level;
        int y, radius;
        final Set<Long> columns=new HashSet<>();
        View(ServerLevel level,int y,int radius) { this.level=level; this.y=y; this.radius=radius; }
    }
    private static int radius(ServerPlayer player) { return Math.clamp(player.clientInformation().viewDistance()+2,4,34); }
    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register((level,chunk,newlyGenerated)->{ if(((SparseColumn)chunk).blockbranch$terrain()!=null) LOADED.put(chunk,level); });
        ServerChunkEvents.CHUNK_UNLOAD.register((level,chunk)->LOADED.remove(chunk));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            var touched=new IdentityHashMap<LevelChunk,ServerLevel>();
            var refreshes=new ArrayList<Runnable>();
            if(++ticks%20==0) touched.putAll(LOADED);
            var disconnected=new ArrayList<ServerGamePacketListenerImpl>();
            for(var entry:VIEWS.entrySet()) {
                var connection=entry.getKey(); var player=connection.player; var view=entry.getValue();
                if(!server.getPlayerList().getPlayers().contains(player) || player.level()!=view.level) { disconnected.add(connection); continue; }
                int center=player.blockPosition().getY()>>4, radius=radius(player);
                boolean moved=center!=view.y || radius!=view.radius;
                view.y=center; view.radius=radius;
                var iterator=view.columns.iterator();
                while(iterator.hasNext()) {
                    long key=iterator.next(); int x=net.minecraft.world.level.ChunkPos.getX(key),z=net.minecraft.world.level.ChunkPos.getZ(key);
                    var chunk=(LevelChunk)view.level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
                    if(chunk==null || Math.abs(x-player.chunkPosition().x())>radius || Math.abs(z-player.chunkPosition().z())>radius) { iterator.remove(); if(chunk!=null) touched.put(chunk,view.level); continue; }
                    if(moved) { sendWindow(connection,chunk,view); touched.put(chunk,view.level); refreshes.add(()->sendLight(connection,chunk,view)); }
                }
            }
            for(var connection:disconnected) {
                var view=VIEWS.remove(connection);
                for(long key:view.columns) {
                    var chunk=(LevelChunk)view.level.getChunkSource().getChunk(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),ChunkStatus.FULL,false);
                    if(chunk!=null) touched.put(chunk,view.level);
                }
            }
            touched.forEach((chunk,level)->activate(level,chunk));
            refreshes.forEach(Runnable::run);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{ VIEWS.clear(); LOADED.clear(); ticks=0; SectionStore.closeAll(); });
    }
    public static void send(ServerGamePacketListenerImpl connection,LevelChunk chunk) {
        var level=(ServerLevel)chunk.getLevel();
        var view=VIEWS.computeIfAbsent(connection,c->new View(level,c.player.blockPosition().getY()>>4,radius(c.player)));
        if(view.level!=level) { VIEWS.remove(connection); send(connection,chunk); return; }
        view.columns.add(chunk.getPos().pack());
        sendWindow(connection,chunk,view); activate(level,chunk); sendLight(connection,chunk,view);
    }
    private static void sendLight(ServerGamePacketListenerImpl connection,LevelChunk chunk,View view) {
        int center=view.y,radius=view.radius;
        var terrain=((SparseColumn)chunk).blockbranch$terrain(); if(terrain==null) return;
        var engine=view.level.getChunkSource().getLightEngine();
        // Full-section overlays do not create vanilla per-block client light
        // updates. Fence the lighting executor, then send this vertical window.
        engine.waitForPendingTasks(chunk.getPos().x(),chunk.getPos().z()).thenRun(()->view.level.getServer().execute(()->{
            if(VIEWS.get(connection)!=view || view.y!=center || view.radius!=radius || !view.columns.contains(chunk.getPos().pack())) return;
            var mask=new BitSet(); int offset=engine.getMinLightSection();
            for(int y=Math.max(ColumnLimits.MIN_Y>>4,center-radius);y<=Math.min((ColumnLimits.MAX_Y>>4)-1,center+radius);y++) mask.set(y-offset);
            for(int y:terrain.base().boundaries()) mask.set(y-offset);
            connection.send(new net.minecraft.network.protocol.game.ClientboundLightUpdatePacket(chunk.getPos(),engine,mask,mask));
        }));
    }
    private static void sendWindow(ServerGamePacketListenerImpl connection,LevelChunk chunk,View view) {
        var terrain=((SparseColumn)chunk).blockbranch$terrain(); if(terrain==null) return;
        int min=Math.max(ColumnLimits.MIN_Y>>4,view.y-view.radius),max=Math.min((ColumnLimits.MAX_Y>>4)-1,view.y+view.radius);
        var definition=terrain.base().encode(terrain.factory());
        int[] sources=new int[256];
        for(int z=0;z<16;z++) for(int x=0;x<16;x++) sources[z*16+x]=chunk.getSkyLightSources().getLowestSourceY(x,z);
        definition.putIntArray("sky_sources",sources);
        var edits=terrain.modified();
        ServerPlayNetworking.send(connection.player,new TerrainWindowPacket(chunk.getPos().x(),chunk.getPos().z(),min,max,definition,edits,SectionOccupancy.create(terrain.base(),min,max,edits)));
        for(int y:terrain.modifiedView().subSet(min,true,max,true))
            ServerPlayNetworking.send(connection.player,ColumnPacket.create(chunk.getPos().x(),chunk.getPos().z(),y,((SparseColumn)chunk).blockbranch$read(y)));
        for(var entity:chunk.getBlockEntities().values()) if((entity.getBlockPos().getY()>>4)>=min && (entity.getBlockPos().getY()>>4)<=max)
            connection.send(ClientboundBlockEntityDataPacket.create(entity));
    }
    private static void activate(ServerLevel level,LevelChunk chunk) {
        var terrain=((SparseColumn)chunk).blockbranch$terrain(); if(terrain==null) return;
        var active=new HashSet<Integer>();
        for(var view:VIEWS.values()) if(view.level==level && view.columns.contains(chunk.getPos().pack()))
            for(int y=Math.max(ColumnLimits.MIN_Y>>4,view.y-view.radius);y<=Math.min((ColumnLimits.MAX_Y>>4)-1,view.y+view.radius);y++) active.add(y);
        terrain.evictExcept(active);
        var lit=new HashSet<>(active); lit.addAll(terrain.base().boundaries());
        var engine=level.getChunkSource().getLightEngine();
        for(int y:terrain.lit) if(!lit.contains(y)) engine.updateSectionStatus(SectionPos.of(chunk.getPos(),y),true);
        for(int y:lit) if(!terrain.lit.contains(y)) {
            var section=active.contains(y) ? ((SparseColumn)chunk).blockbranch$read(y) : terrain.base().view(y);
            engine.updateSectionStatus(SectionPos.of(chunk.getPos(),y),section.hasOnlyAir());
            if(section.maybeHas(state->state.getLightEmission()>0))
                for(int ly=0;ly<16;ly++) for(int z=0;z<16;z++) for(int x=0;x<16;x++)
                    if(section.getBlockState(x,ly,z).getLightEmission()>0)
                        engine.checkBlock(new net.minecraft.core.BlockPos(chunk.getPos().getMinBlockX()+x,y*16+ly,chunk.getPos().getMinBlockZ()+z));
        }
        terrain.lit.clear(); terrain.lit.addAll(lit);
    }
    public static boolean active(ServerLevel level,int x,int y,int z) {
        long key=new net.minecraft.world.level.ChunkPos(x>>4,z>>4).pack();
        for(var view:VIEWS.values()) if(view.level==level && view.columns.contains(key) && Math.abs((y>>4)-view.y)<=Math.min(view.radius,level.getServer().getPlayerList().getSimulationDistance()+1)) return true;
        return false;
    }
    /** Read-only union of the existing simulation windows; querying creates no tickets. */
    public record SectionRange(int min, int max) {}
    public static List<SectionRange> activeSectionRanges(ServerLevel level,int x,int z) {
        long key=new net.minecraft.world.level.ChunkPos(x>>4,z>>4).pack();
        var ranges=new ArrayList<SectionRange>();
        for(var view:VIEWS.values()) if(view.level==level && view.columns.contains(key)) {
            int radius=Math.min(view.radius,level.getServer().getPlayerList().getSimulationDistance()+1);
            ranges.add(new SectionRange(Math.max(ColumnLimits.MIN_Y>>4,view.y-radius),
                Math.min((ColumnLimits.MAX_Y>>4)-1,view.y+radius)));
        }
        ranges.sort(Comparator.comparingInt(SectionRange::min));
        var merged=new ArrayList<SectionRange>();
        for(var range:ranges) {
            if(!merged.isEmpty() && range.min<=merged.getLast().max+1) {
                var last=merged.removeLast();merged.add(new SectionRange(last.min,Math.max(last.max,range.max)));
            } else merged.add(range);
        }
        return List.copyOf(merged);
    }
    public static boolean visible(ServerPlayer player,int sectionY) {
        var view=VIEWS.get(player.connection);
        return view!=null && Math.abs(sectionY-view.y)<=view.radius;
    }
    private VerticalRuntime() {}
}
