package blockbranch.realistic;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Server-thread-only, ephemeral scheduling. Pixels and dirtiness remain native saved map data. */
public final class ProgressiveMapDrawing {
    private static final int MIN_SESSIONS = 64, COLUMNS_PER_TICK = 4;
    private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE,
            blockbranch.Blockbranch.id("map_drawing"), new TicketType(0, TicketType.FLAG_LOADING));
    private static final Map<MinecraftServer, State> SERVERS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    public static void initialize() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            State s = SERVERS.computeIfAbsent(server, key -> new State());
            s.tick++; s.used.clear();
            if(s.flight!=null&&s.flight.isDone())s.clearFlight();
            if(s.pending!=null && s.tick-s.pending.started>600) {Job timedOut=s.pending;s.release(timedOut);timedOut.retry=s.tick+200;}
            var players = server.getPlayerList().getPlayers();
            s.allowed.clear();
            for (int n=0; n<Math.min(COLUMNS_PER_TICK, players.size()); n++)
                s.allowed.add(players.get(grantIndex(s.tick,n,players.size())).getUUID());
            s.jobs.removeIf(job -> {if (s.tick-job.seen > 2) {s.release(job); return true;} return false;});
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            State s = SERVERS.remove(server);
            if (s != null) s.close();
        });
    }
    public static Context begin(Level world, Entity entity, MapItemSavedData data) {
        if (!(world instanceof ServerLevel level) || !(entity instanceof ServerPlayer player)
                || !(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator)
                || generator.discoveryDefinition() == 0 || data.dimension != world.dimension()
                || !level.getServer().getPlayerList().getPlayers().contains(player)) return null;
        State state = SERVERS.computeIfAbsent(level.getServer(), key -> new State());
        var holding=data.getHoldingPlayer(player);
        Job job = state.find(level,player.getUUID(),data,holding);
        int scale = 1 << data.scale;
        int px = Mth.floor(player.getX()-data.centerX)/scale+64;
        int pz = Mth.floor(player.getZ()-data.centerZ)/scale+64;
        int radius = 128/scale;
        if (level.dimensionType().hasCeiling()) radius /= 2;
        int start = Math.max(0, px-radius+1), end = Math.min(128, px+radius);
        // Holding is distinct from admission: a busy server must not evict a still-held owner.
        if (job != null) {
            job.seen=state.tick;
            if (job.px != px || job.pz != pz) {state.release(job);job.column=start;job.px=px;job.pz=pz;}
        }
        var main=net.minecraft.world.item.MapItem.getSavedData(player.getMainHandItem(),level);
        var off=net.minecraft.world.item.MapItem.getSavedData(player.getOffhandItem(),level);
        boolean both=main!=null&&!main.locked&&off!=null&&!off.locked&&main.dimension==world.dimension()&&off.dimension==world.dimension();
        boolean handEligible=!both || main==off || data==(offhandTurn(state.tick,level.getServer().getPlayerList().getPlayers().size())?off:main);
        boolean granted = handEligible && state.allowed.contains(player.getUUID()) && state.used.add(player.getUUID());
        if (!granted) return new Context(state, null, false);
        if (job == null) {
            state.reserveSlot(level.getServer().getPlayerList().getPlayers().size());
            job = new Job(level, player.getUUID(), data, holding);job.column=job.holding.step;job.px=px;job.pz=pz;state.jobs.add(job);
        }
        job.seen=job.granted=state.tick;
        if (job.column < start || job.column >= end) job.column=start;
        // Do not resample a partial prefix on every tick while its requested FULL chunk is still pending.
        boolean allowed = start < end && state.tick >= job.retry && (job.future == null || job.future.isDone());
        return new Context(state, job, allowed);
    }
    public static Context current() {return CURRENT.get();}
    public static final class Context implements AutoCloseable {
        private final State state; private final Job job; public final boolean allowed;
        private final Context previous; private boolean complete=true;
        private final Set<ChunkPos> visited=new LinkedHashSet<>();
        private Context(State state, Job job, boolean allowed) {this.state=state;this.job=job;this.allowed=allowed;previous=CURRENT.get();CURRENT.set(this);}
        public int column() {return job.column;}
        public LevelChunk chunk(int x,int z,int column) {
            if (column != job.column) throw Yield.INSTANCE;
            var source=job.level.getChunkSource();
            ChunkPos pos=new ChunkPos(x,z); visited.add(pos);
            if (visited.size()>18) throw new IllegalStateException("Native map column exceeded chunk bound");
            var loaded=source.getChunkNow(x,z);
            if (loaded != null) return loaded;
            complete=false;
            if (state.pending != job) {
                state.waiting.add(job);
                if (state.pending != null || state.waiting.iterator().next() != job || !state.canRequest()) throw Yield.INSTANCE;
                state.waiting.remove(job);state.pending=job;job.started=state.tick;
            }
            for (var seen:visited) if(job.tickets.add(seen)) {if(job.tickets.size()>18)throw new IllegalStateException("Map ticket bound");source.addTicketWithRadius(TICKET,seen,0);}
            if (job.future != null && !pos.equals(job.pos)) {job.future=null;job.pos=null;}
            if (job.future != null && job.future.isDone()) {
                state.release(job); job.retry=state.tick+200;
            } else if (job.future == null) {
                job.pos=pos;
                state.flightLevel=job.level;state.flightPos=pos;
                try {state.flight=job.future=source.addTicketAndLoadWithRadius(TICKET,pos,0);}
                catch (RuntimeException failure) {
                    state.release(job);
                    // No returned future means submission state is unknown. Do not retry/flood it.
                    state.flight=new CompletableFuture<>();
                    throw failure;
                }
            }
            throw Yield.INSTANCE;
        }
        public void failed() {complete=false;state.release(job);}
        @Override public void close() {
            try {if (allowed && complete) {state.release(job); state.completed++; job.column++;job.holding.step=job.column;}}
            finally {if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
        }
    }
    /** Control transfer only; never escapes MapItem.update. No stack allocation per deferred pixel. */
    public static final class Yield extends RuntimeException {
        private static final Yield INSTANCE=new Yield();
        private Yield(){super(null,null,false,false);}
    }
    private static final class Job {
        final ServerLevel level;final UUID player;final MapItemSavedData data;final MapItemSavedData.HoldingPlayer holding;
        int px=Integer.MIN_VALUE,pz,column;long seen,retry,started,granted;final Set<ChunkPos> tickets=new LinkedHashSet<>();ChunkPos pos;CompletableFuture<?> future;
        Job(ServerLevel level,UUID player,MapItemSavedData data,MapItemSavedData.HoldingPlayer holding){this.level=level;this.player=player;this.data=data;this.holding=holding;}
    }
    private static final class State {
        long tick,completed;final Set<Job> waiting=new LinkedHashSet<>();final List<Job> jobs=new ArrayList<>();final Set<UUID> allowed=new HashSet<>(),used=new HashSet<>();Job pending;CompletableFuture<?> flight;ServerLevel flightLevel;ChunkPos flightPos;boolean draining;
        void reserveSlot(int players){
            if(jobs.size()<descriptorLimit(players))return;
            Job old=jobs.stream().filter(j->j!=pending).min(Comparator.comparingLong(j->j.granted)).orElseThrow();
            release(old);jobs.remove(old);
        }
        Job find(ServerLevel level,UUID player,MapItemSavedData data,MapItemSavedData.HoldingPlayer holding){return jobs.stream().filter(j->j.level==level&&j.player.equals(player)&&j.data==data&&j.holding==holding).findFirst().orElse(null);}
        boolean canRequest(){if(flight!=null&&flight.isDone())clearFlight();return flight==null;}
        void release(Job job) {
            waiting.remove(job);
            if (pending != job) return;
            try {for(var pos:job.tickets){
                // Keep the single dispatched target alive until FULL completion. Unloading it could
                // complete its public future early while cancelled native generation is still winding down.
                if(flight!=null&&!flight.isDone()&&job.level==flightLevel&&pos.equals(flightPos)){draining=true;continue;}
                job.level.getChunkSource().removeTicketWithRadius(TICKET,pos,0);
            }}
            finally {pending=null;job.pos=null;job.future=null;job.tickets.clear();}
        }
        void clearFlight(){
            try {if(draining&&flightLevel!=null)flightLevel.getChunkSource().removeTicketWithRadius(TICKET,flightPos,0);}
            finally {flight=null;flightLevel=null;flightPos=null;draining=false;}
        }
        void close(){try{if(pending!=null)release(pending);}finally{clearFlight();jobs.clear();waiting.clear();}}
    }
    static int grantIndex(long tick,int slot,int players){return (int)((tick*COLUMNS_PER_TICK+slot)%players);}
    // Two held maps through the three-tick liveness window. A fixed smaller LRU can
    // continually evict the FIFO head before its player's next drawing grant.
    // Only descriptors scale with participants; the global request/ticket bounds do not.
    static int descriptorLimit(int players){return (int)Math.min(Integer.MAX_VALUE,Math.max(MIN_SESSIONS,6L*players));}
    static boolean offhandTurn(long tick,int players) {return (tick/Math.max(1,(players+3)/4)&1)!=0;}
    static long[] audit(MinecraftServer server) {var s=SERVERS.get(server);return s==null?new long[]{0,0,0,0,0,0}:new long[]{s.completed,s.jobs.size(),s.pending==null?0:s.pending.tickets.size(),s.pending==null||s.pending.future==null?0:1,s.flight==null||s.flight.isDone()?0:1,s.draining?1:0};}
    private ProgressiveMapDrawing() {}
}
