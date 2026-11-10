package blockbranch.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import blockbranch.Blockbranch;
import blockbranch.realistic.RealisticGenerator;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
/**
 * Server half of the visual-only horizon (opt-in with {@code -Dblockbranch.horizon=true}).
 *
 * <p>Tiles are sampled on a small daemon pool, never by generating, loading or ticking
 * chunks, so nothing beyond the normal view distance is simulated. Where the world has been
 * generated, columns come from its real blocks (structures, cave openings and edits, through
 * {@link HorizonSources}); elsewhere from the realistic generator's analytic terrain. Each
 * player has one replaceable wanted list and a bounded number of tiles in flight; a newer
 * list, a disconnect or a server stop drops queued work and any result whose generation is
 * stale. A tile that fails is held back with backoff; the client repeats an unchanged request
 * with backoff too, so failed and dropped tiles return without the camera moving.
 *
 * <p>Block changes and newly generated chunks mark their chunk changed. Once a changed chunk
 * is outside a player's view distance, every tile sent to that player or being sampled with a
 * sample in it is sampled again, after any sample already in flight, and resent if its data
 * differs, so leaving an area shows what was built, broken or generated there.
 */
public final class HorizonService {
    public static final boolean ENABLED=Boolean.getBoolean("blockbranch.horizon");
    /** Farthest tile edge served, in blocks from the player; bounds client-requested work. */
    public static final int MAX_RADIUS=Integer.getInteger("blockbranch.horizon.maxRadius",4096);
    static final int IN_FLIGHT=6,SENDS_PER_TICK=8;
    /** Sent tiles remembered per player for refreshes; older ones are simply not refreshed. */
    static final int REMEMBERED=16384;
    /** Ticks between passes over changed chunks. */
    static final int CHANGE_INTERVAL=20;
    /** Backoff after a failed tile, doubling per failure; the client's repeated request retries it. */
    static final int RETRY_TICKS=20,RETRY_MAX_TICKS=1200;
    private static final Map<UUID,Queue> QUEUES=new ConcurrentHashMap<>();
    /** Chunks changed since the last pass; touched only on the server thread. */
    private static final it.unimi.dsi.fastutil.longs.LongOpenHashSet CHANGED=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    private static long lastChanged=Long.MIN_VALUE;
    private static volatile HorizonSources sources;
    private static int ticks;
    private static final LongAdder TILES=new LongAdder(),NANOS=new LongAdder();
    private static volatile ExecutorService workers;

    static final class Queue {
        volatile int generation;
        final ArrayDeque<Long> pending=new ArrayDeque<>();
        final AtomicInteger outstanding=new AtomicInteger();
        final ConcurrentLinkedQueue<HorizonTile> done=new ConcurrentLinkedQueue<>();
        /**
         * Keys submitted to the pool and not yet sent, with their request generation, so a
         * repeated request does not resample them while a newer generation does.
         */
        final ConcurrentHashMap<Long,Integer> inFlight=new ConcurrentHashMap<>();
        /** Tiles whose sampling failed, kept back with backoff until the client asks again. */
        final HorizonRetry failures=new HorizonRetry(RETRY_TICKS,RETRY_MAX_TICKS,Integer.MAX_VALUE,4096);
        /** Sent tiles and a hash of their data, oldest first; server thread only. */
        final LinkedHashMap<Long,Integer> sent=new LinkedHashMap<>(256,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<Long,Integer> eldest){return size()>REMEMBERED;}
        };
        /** Changed chunks still within this player's view; server thread only. */
        final it.unimi.dsi.fastutil.longs.LongOpenHashSet changed=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        /** Sent tiles to sample again, served after the wanted list. */
        final ArrayDeque<Long> refresh=new ArrayDeque<>();
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(HorizonRequest.TYPE,HorizonRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HorizonTile.TYPE,HorizonTile.CODEC);
        // Payload types are always registered so both sides agree on the protocol; without the
        // receiver a client reports the server as unable to serve tiles and never asks.
        if(!ENABLED)return;
        ServerPlayNetworking.registerGlobalReceiver(HorizonRequest.TYPE,(packet,context)->request(context.player(),packet));
        ServerLifecycleEvents.SERVER_STARTED.register(server->start());
        ServerLifecycleEvents.SERVER_STOPPING.register(server->stop());
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->QUEUES.remove(handler.player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(HorizonService::pump);
        ServerChunkEvents.CHUNK_GENERATE.register((level,chunk)->changed(level,chunk.getPos().x(),chunk.getPos().z()));
        ServerChunkEvents.CHUNK_UNLOAD.register((level,chunk)->{var s=sources;if(s!=null&&s.level()==level)s.forget(chunk.getPos().pack());});
    }

    /** Marks a chunk changed after a block change or generation; server thread. */
    public static void changed(ServerLevel level,int chunkX,int chunkZ) {
        if(!ENABLED||workers==null||level.dimension()!=Level.OVERWORLD)return;
        long key=ChunkPos.pack(chunkX,chunkZ);
        if(key==lastChanged)return;
        lastChanged=key;CHANGED.add(key);
    }

    static synchronized void start() {
        if(!ENABLED||workers!=null)return;
        int threads=Math.max(1,Math.min(2,Runtime.getRuntime().availableProcessors()/4));
        var count=new AtomicInteger();
        workers=Executors.newFixedThreadPool(threads,r->{var t=new Thread(r,"Blockbranch Horizon "+count.incrementAndGet());t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
    }
    static synchronized void stop() {
        if(workers!=null){workers.shutdownNow();workers=null;}
        QUEUES.clear();CHANGED.clear();lastChanged=Long.MIN_VALUE;sources=null;
    }
    /** Queued players, for lifecycle tests. */
    static int queuedPlayers(){return QUEUES.size();}
    static boolean running(){return workers!=null;}

    /** Handles a wanted list; Fabric runs play receivers on the server thread. */
    static void request(ServerPlayer player,HorizonRequest packet) {
        if(!ENABLED)return;
        var q=QUEUES.computeIfAbsent(player.getUUID(),u->new Queue());
        double px=player.getX(),pz=player.getZ();
        enqueue(q,packet.generation(),packet.keys(),key->valid(key)&&distance(key,px,pz)<=MAX_RADIUS,ticks);
    }
    /**
     * Replaces a queue's wanted list. Tiles already in flight for this generation and tiles
     * still in failure backoff are left out; server thread.
     */
    static void enqueue(Queue q,int generation,long[] keys,java.util.function.LongPredicate allowed,int now) {
        var seen=new HashSet<Long>();
        synchronized(q) {
            q.generation=generation;q.pending.clear();
            for(long key:keys)if(allowed.test(key)&&!Objects.equals(q.inFlight.get(key),generation)&&q.failures.due(key,now)&&seen.add(key))q.pending.add(key);
        }
        // The client asks only for tiles it lacks, so a requested tile is sent even if unchanged.
        for(long key:keys)q.sent.remove(key);
    }
    record Task(long key,int generation) {}
    /** Takes the next wanted tile, else the next refresh not already in flight, and marks it in flight; server thread. */
    static Task take(Queue q,List<Long> deferred) {
        synchronized(q) {
            Long next=q.pending.poll();
            if(next==null)next=nextRefresh(q.refresh,q.inFlight::containsKey,deferred);
            if(next==null)return null;
            q.outstanding.incrementAndGet();q.inFlight.put(next,q.generation);
            return new Task(next,q.generation);
        }
    }
    /** Ends a task without a tile: abandoned, cancelled, rejected or failed (then kept back with backoff). */
    static void abandon(Queue q,Task task,Throwable failure,int submitted) {
        q.outstanding.decrementAndGet();q.inFlight.remove(task.key(),task.generation());
        // Stopping the server cancels saved-chunk reads; that is not a failure.
        if(failure==null||failure instanceof CancellationException)return;
        if(q.failures.failed(task.key(),submitted)==1)Blockbranch.LOGGER.error("Horizon tile {} failed; retrying with backoff",task.key(),failure);
        else Blockbranch.LOGGER.debug("Horizon tile {} failed again",task.key(),failure);
    }
    /** Next finished tile, no longer in flight, or null; the caller drops it if its generation is stale. */
    static HorizonTile collect(Queue q) {
        var tile=q.done.poll();if(tile==null)return null;
        q.outstanding.decrementAndGet();q.inFlight.remove(tile.key(),tile.generation());q.failures.succeeded(tile.key());
        return tile;
    }

    private static void pump(MinecraftServer server) {
        if(!ENABLED)return;
        var pool=workers;if(pool==null)return;
        if(++ticks%CHANGE_INTERVAL==0&&!CHANGED.isEmpty()) {
            long[] changed=CHANGED.toLongArray();CHANGED.clear();lastChanged=Long.MIN_VALUE;
            var s=sources;if(s!=null)for(long c:changed)s.forget(c);
            for(var q:QUEUES.values())q.changed.addAll(it.unimi.dsi.fastutil.longs.LongArrayList.wrap(changed));
        }
        if(QUEUES.isEmpty())return;
        for(var entry:QUEUES.entrySet()) {
            var player=server.getPlayerList().getPlayer(entry.getKey());var q=entry.getValue();
            if(player==null){QUEUES.remove(entry.getKey());continue;}
            int sent=0;
            for(HorizonTile tile;sent<SENDS_PER_TICK&&(tile=collect(q))!=null;) {
                if(tile.generation()!=q.generation||!ServerPlayNetworking.canSend(player,HorizonTile.TYPE))continue;
                int hash=Arrays.hashCode(tile.data());var previous=q.sent.put(tile.key(),hash);
                if(previous!=null&&previous==hash)continue;
                ServerPlayNetworking.send(player,tile);sent++;
            }
            var level=player.level();
            if(level.dimension()!=Level.OVERWORLD||!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator)){synchronized(q){q.pending.clear();q.refresh.clear();}q.changed.clear();continue;}
            var source=sources;
            if(source==null||source.level()!=level)sources=source=new HorizonSources(level);
            if(ticks%CHANGE_INTERVAL==0)refresh(player,q,server.getPlayerList().getViewDistance());
            var random=level.getChunkSource().randomState();var biomes=server.registryAccess().lookupOrThrow(Registries.BIOME);
            var reader=source;var deferred=new ArrayList<Long>();
            try {
                for(Task task;q.outstanding.get()<IN_FLIGHT&&(task=take(q,deferred))!=null;) {
                    var loaded=loadedChunks(level,task.key());int submitted=ticks;var t=task;
                    try {
                        pool.execute(()->{
                            if(t.generation()!=q.generation){abandon(q,t,null,submitted);return;}
                            try{q.done.add(new HorizonTile(t.generation(),t.key(),sample(generator,random,t.key(),h->biomes.getId(h.value()),reader,loaded)));}
                            catch(Throwable failure){abandon(q,t,failure,submitted);}
                        });
                    } catch(RejectedExecutionException stopped){abandon(q,t,null,submitted);return;}
                }
            } finally {
                if(!deferred.isEmpty())synchronized(q){q.refresh.addAll(deferred);}
            }
        }
    }

    /**
     * Queues every remembered tile sampling a changed chunk that has left this player's view
     * (with a one-chunk margin for chunks still unloading); nearer changes wait.
     */
    private static void refresh(ServerPlayer player,Queue q,int viewDistance) {
        if(q.changed.isEmpty())return;
        int px=player.chunkPosition().x(),pz=player.chunkPosition().z();
        var keys=new LinkedHashSet<Long>();
        for(var it=q.changed.iterator();it.hasNext();) {
            long c=it.nextLong();int cx=ChunkPos.getX(c),cz=ChunkPos.getZ(c);
            if(Math.max(Math.abs(cx-px),Math.abs(cz-pz))<=viewDistance+1)continue;
            it.remove();
            for(long k:sampling(cx,cz))if(refreshable(q,k))keys.add(k);
        }
        if(!keys.isEmpty())synchronized(q){for(long k:keys)if(!q.refresh.contains(k))q.refresh.add(k);}
    }

    /**
     * Whether a changed chunk resamples tile {@code k}: it was sent, or it is being sampled
     * now and its result may predate the change.
     */
    static boolean refreshable(Queue q,long k){return q.sent.containsKey(k)||q.inFlight.containsKey(k);}
    /**
     * Next refresh to sample, or null. A tile still in flight is set aside in {@code deferred}
     * (for the caller to queue again) rather than dropped: its result may predate the change
     * that queued the refresh.
     */
    static Long nextRefresh(ArrayDeque<Long> refresh,java.util.function.Predicate<Long> inFlight,List<Long> deferred) {
        for(Long r;(r=refresh.poll())!=null;){if(inFlight.test(r))deferred.add(r);else return r;}
        return null;
    }

    /** Loaded full chunks under a tile's samples, gathered on the server thread for the worker. */
    private static it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<LevelChunk> loadedChunks(ServerLevel level,long key) {
        var out=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<LevelChunk>();
        int l=level(key),ox=originX(key),oz=originZ(key);var cache=level.getChunkSource();
        for(int j=0;j<SAMPLES;j++)for(int i=0;i<SAMPLES;i++) {
            int cx=sampleCoordinate(ox,l,i)>>4,cz=sampleCoordinate(oz,l,j)>>4;long c=ChunkPos.pack(cx,cz);
            if(out.containsKey(c))continue;
            out.put(c,cache.getChunkNow(cx,cz));
        }
        out.values().removeIf(Objects::isNull);
        return out;
    }

    /** Samples every column of one tile, including its one-cell border, from analytic terrain only. */
    public static int[] sample(RealisticGenerator generator,net.minecraft.world.level.levelgen.RandomState random,long key,java.util.function.ToIntFunction<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> biomeIds) {
        return sample(generator,random,key,biomeIds,null,null);
    }
    /**
     * Samples every column of one tile, including its one-cell border: real blocks where
     * {@code sources} has a generated chunk, the analytic terrain elsewhere.
     */
    static int[] sample(RealisticGenerator generator,net.minecraft.world.level.levelgen.RandomState random,long key,java.util.function.ToIntFunction<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> biomeIds,
            HorizonSources sources,it.unimi.dsi.fastutil.longs.Long2ObjectMap<LevelChunk> loaded) {
        long began=System.nanoTime();int level=level(key),ox=originX(key),oz=originZ(key);int[] data=new int[HorizonTile.LENGTH];
        var columns=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<HorizonColumns.Source>();
        for(int j=0;j<SAMPLES;j++)for(int i=0;i<SAMPLES;i++) {
            if(Thread.currentThread().isInterrupted())throw new CancellationException("Horizon stopped");
            int x=sampleCoordinate(ox,level,i),z=sampleCoordinate(oz,level,j),o=(j*SAMPLES+i)*FIELDS;
            HorizonColumns.Source column=null;
            if(sources!=null){long c=ChunkPos.pack(x>>4,z>>4);column=columns.get(c);if(column==null&&!columns.containsKey(c)){column=sources.column(x>>4,z>>4,loaded);columns.put(c,column);}}
            if(column!=null&&HorizonColumns.read(column,x,z,blockbranch.cubic.ColumnLimits.MIN_Y,blockbranch.cubic.ColumnLimits.MAX_Y-1,data,o))
                data[o+BIOME]=generator.horizonBiome(x,Math.max(data[o+GROUND],data[o+WATER]),z,biomeIds);
            else generator.horizonColumn(x,z,cell(level),random,data,o,biomeIds);
        }
        TILES.increment();NANOS.add(System.nanoTime()-began);
        long n=TILES.sum();if(n%256==0){long d=HorizonStructures.DECORATED.sum();Blockbranch.LOGGER.info("HORIZON_SAMPLER tiles={} meanTileMs={} structureChunks={} meanStructureMs={}",n,String.format(Locale.ROOT,"%.2f",NANOS.sum()/1e6/n),d,String.format(Locale.ROOT,"%.2f",d==0?0:HorizonStructures.DECORATE_NANOS.sum()/1e6/d));}
        return data;
    }
    private HorizonService(){}
}
