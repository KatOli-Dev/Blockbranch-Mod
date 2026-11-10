package blockbranch.realistic;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Bounded native outlines. World access/publication is server-owned; workers see only immutable biome inputs. */
public final class DeferredMapPreview {
    static final int MAX_JOBS=8, MAX_QUERIES=64;
    static final long BUDGET_NANOS=4_000_000L;
    private static final Map<MinecraftServer,State> STATES=new ConcurrentHashMap<>();
    private static final ThreadLocal<Job> RASTER=new ThreadLocal<>();

    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(DeferredMapPreview::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{var state=STATES.remove(server);if(state!=null)state.close();});
    }
    public static boolean pending(MapItemSavedData data){return ((PendingMapPreview)data).blockbranch$pendingPreview();}
    public static void mark(MapItemSavedData data,boolean value){
        var preview=(PendingMapPreview)data;
        if(!value||!preview.blockbranch$pendingPreview())preview.blockbranch$previewRows(0);
        preview.blockbranch$pendingPreview(value);data.setDirty();
    }
    public static boolean defer(ServerLevel level,ItemStack stack){
        if(RASTER.get()!=null)return false;
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator)||generator.discoveryDefinition()==0)return false;
        var data=MapItem.getSavedData(stack,level);
        if(data!=null&&data.dimension==level.dimension())mark(data,true);
        return true;
    }
    public static boolean writing(MapItemSavedData data){var job=RASTER.get();return job!=null&&job.data==data;}
    public static Holder<Biome> sampled(BlockPos pos){
        var job=RASTER.get();if(job==null)return null;
        int x=pos.getX()/job.scale-job.startX,z=pos.getZ()/job.scale-job.startZ;
        if(x<0||x>=128||z<0||z>=128)throw new IllegalStateException("Native preview sample coordinates changed");
        var value=job.samples[z*128+x];return value==null?job.samples[job.sampleStart]:value;
    }
    public static boolean writePixel(MapItemSavedData data,int z){
        var job=RASTER.get();return job==null||(job.data==data&&z>=job.firstRow&&z<job.readyRows-1);
    }

    private static void tick(MinecraftServer server){
        var state=STATES.computeIfAbsent(server,key->new State());
        var players=server.getPlayerList().getPlayers();
        state.jobs.removeIf(job->{
            if(!pending(job.data)||!held(players,job)){state.cancel(job);return true;}
            return false;
        });
        state.collect(); // Only observes a completed future; cancellation never pretends work has drained.
        int hands=players.size()*2,first=state.admission;
        for(int n=0;n<hands&&state.jobs.size()<MAX_JOBS;n++){
            int hand=Math.floorMod(first+n,hands);var player=players.get(hand/2);int before=state.jobs.size();
            admit(state,player,(hand&1)==0?player.getMainHandItem():player.getOffhandItem());
            if(state.jobs.size()>before)state.admission=hand+1;
        }
        long start=System.nanoTime();int attempts=0,visited=0;
        while(!state.jobs.isEmpty()&&attempts<MAX_QUERIES&&visited++<MAX_JOBS&&System.nanoTime()-start<BUDGET_NANOS){
            var job=state.jobs.get(Math.floorMod(state.cursor++,state.jobs.size()));
            try{
                if(!Arrays.equals(job.expected,job.data.colors)){mark(job.data,false);state.cancel(job);state.jobs.remove(job);continue;}
                if(state.flight!=null&&state.flight.job==job)continue;
                if(job.ready!=null){
                    // Loaded saved biomes can have arrived or changed during computation. Recheck on the server.
                    var batch=job.ready;
                    while(batch.applied<batch.count&&attempts<MAX_QUERIES&&System.nanoTime()-start<BUDGET_NANOS){
                        int i=batch.applied;attempts++;
                        var loaded=PreviewBiomeQueries.loaded(job.level,batch.points[i]);if(!loaded.ready())break;
                        job.samples[job.next++]=loaded.value()!=null?loaded.value():batch.values[i];batch.applied++;
                    }
                    if(batch.applied==batch.count)job.ready=null;
                    publish(job);
                    if(job.next==16384){mark(job.data,false);state.jobs.remove(job);continue;}
                    if(job.ready!=null)continue;
                }
                if(state.flight!=null)continue;
                var batch=new Batch(job.resolver,job.cache);
                while(batch.count<MAX_QUERIES&&job.next+batch.count<16384&&attempts<MAX_QUERIES&&System.nanoTime()-start<BUDGET_NANOS){
                    int index=job.next+batch.count,x=index%128,z=index/128;attempts++;
                    var point=job.selector.select(job.pos.set((job.startX+x)*job.scale,job.level.getSeaLevel(),(job.startZ+z)*job.scale));
                    var loaded=PreviewBiomeQueries.loaded(job.level,point);if(!loaded.ready())break;
                    int i=batch.count++;batch.points[i]=point;batch.values[i]=loaded.value();
                    if(loaded.value()==null){
                        if(job.resolver==null){
                            // Compatibility fallback for an unknown resolver: preserve native behavior, cooperatively.
                            try(var ignored=MapPreviewSamples.open(job.cache)){batch.values[i]=job.level.getUncachedNoiseBiome(point.x(),point.y(),point.z());}
                        }else batch.needsWorker=true;
                    }
                }
                if(batch.count==0)continue;
                if(batch.needsWorker)state.dispatch(job,batch);else job.ready=batch;
            }catch(RuntimeException failure){state.failed(job,failure);}
        }
    }
    private static void publish(Job job){
        int rows=job.next/128;
        if(rows!=128&&rows<job.readyRows+8)return;
        job.readyRows=rows;RASTER.set(job);
        try{
            MapItem.renderBiomePreviewMap(job.level,job.stack);
            ((PendingMapPreview)job.data).blockbranch$previewRows(rows-1);job.data.setDirty();
        }
        finally{RASTER.remove();System.arraycopy(job.data.colors,0,job.expected,0,16384);}
    }
    private static boolean held(List<ServerPlayer> players,Job job){
        for(var p:players)if(p.level()==job.level&&
                (MapItem.getSavedData(p.getMainHandItem(),job.level)==job.data||MapItem.getSavedData(p.getOffhandItem(),job.level)==job.data))return true;
        return false;
    }
    private static void admit(State state,ServerPlayer player,ItemStack stack){
        if(state.jobs.size()>=MAX_JOBS)return;
        var level=player.level();var data=MapItem.getSavedData(stack,level);
        if(data==null||!pending(data)||data.dimension!=level.dimension()||state.jobs.stream().anyMatch(j->j.data==data))return;
        long retry=((PendingMapPreview)data).blockbranch$previewRetry();if(retry!=0&&System.nanoTime()-retry<0)return;
        state.jobs.add(new Job(level,data,stack.copy()));
    }
    static int[] audit(MinecraftServer server){
        var s=STATES.get(server);return s==null?new int[]{0,0,0}:new int[]{s.jobs.size(),s.jobs.stream().mapToInt(j->j.next).sum(),s.flight==null?0:1};
    }
    private static final class State {
        int admission,cursor;final List<Job> jobs=new ArrayList<>();Flight flight;
        // One dispatched task and a bounded handoff slot; only one task computes at a time.
        final ExecutorService worker=new ThreadPoolExecutor(0,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),task->{
            var thread=new Thread(task,"Blockbranch map preview");thread.setDaemon(true);return thread;
        });
        void dispatch(Job job,Batch batch){flight=new Flight(job,batch,CompletableFuture.runAsync(batch::compute,worker));}
        void collect(){
            if(flight==null||!flight.future.isDone())return;
            var completed=flight;flight=null;
            try{
                completed.future.join();
                if(!completed.batch.cancelled.get()&&jobs.contains(completed.job))completed.job.ready=completed.batch;
            }catch(CompletionException failure){
                if(failure.getCause() instanceof Error error)throw error;
                if(!completed.batch.cancelled.get()&&jobs.contains(completed.job))failed(completed.job,failure);
            }
        }
        void cancel(Job job){if(flight!=null&&flight.job==job){flight.batch.cancelled.set(true);flight.job=null;}}
        void failed(Job job,RuntimeException failure){
            cancel(job);jobs.remove(job);((PendingMapPreview)job.data).blockbranch$previewRetry(System.nanoTime()+10_000_000_000L);
            blockbranch.Blockbranch.LOGGER.warn("Deferred map outline sampling failed; retrying after 10 seconds",failure);
        }
        void close(){if(flight!=null)flight.batch.cancelled.set(true);worker.shutdownNow();jobs.clear();flight=null;}
    }
    private static final class Flight {
        Job job;final Batch batch;final CompletableFuture<Void> future;
        Flight(Job job,Batch batch,CompletableFuture<Void> future){this.job=job;this.batch=batch;this.future=future;}
    }
    /** The worker captures this object only: no Level, server, chunk, player, map, selector or future callback. */
    private static final class Batch {
        final BiomeResolver resolver;final MapPreviewSamples.Cache cache;final AtomicBoolean cancelled=new AtomicBoolean();
        final PreviewBiomeQueries.Point[] points=new PreviewBiomeQueries.Point[MAX_QUERIES];
        @SuppressWarnings("unchecked") final Holder<Biome>[] values=(Holder<Biome>[])new Holder<?>[MAX_QUERIES];
        int count,applied;boolean needsWorker;
        Batch(BiomeResolver resolver,MapPreviewSamples.Cache cache){this.resolver=resolver;this.cache=cache;}
        void compute(){
            try(var ignored=MapPreviewSamples.open(cache)){
                for(int i=0;i<count&&!cancelled.get();i++)if(values[i]==null)values[i]=PreviewBiomeQueries.fallback(resolver,points[i]);
            }
        }
    }
    private static final class Job {
        final ServerLevel level;final MapItemSavedData data;final ItemStack stack;final int scale,startX,startZ,firstRow,sampleStart;
        final PreviewBiomeQueries.Selector selector;final BiomeResolver resolver;
        @SuppressWarnings("unchecked") final Holder<Biome>[] samples=(Holder<Biome>[])new Holder<?>[16384];
        final MapPreviewSamples.Cache cache=new MapPreviewSamples.Cache();final byte[] expected;
        final BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();int next,readyRows;Batch ready;
        Job(ServerLevel level,MapItemSavedData data,ItemStack stack){
            this.level=level;this.data=data;this.stack=stack;scale=1<<data.scale;startX=data.centerX/scale-64;startZ=data.centerZ/scale-64;
            firstRow=((PendingMapPreview)data).blockbranch$previewRows();
            readyRows=Math.max(0,firstRow-1);sampleStart=next=readyRows*128;
            expected=data.colors.clone();selector=new PreviewBiomeQueries.Selector(level);resolver=PreviewBiomeQueries.workerResolver(level);
        }
    }
    private DeferredMapPreview(){}
}
