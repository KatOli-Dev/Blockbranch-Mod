package blockbranch.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
/** Horizon payload codecs, server worker lifecycle and cactus column reading (run with -Dblockbranch.horizon=true). */
public final class HorizonProtocolCheck {
    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        require(HorizonService.ENABLED,"check runs with the horizon enabled");
        // Tile roundtrip with steep terrain, water, negative heights and several palettes.
        int[] d=new int[HorizonTile.LENGTH];var r=new java.util.Random(7);
        for(int o=0;o<HorizonTile.LENGTH;o+=FIELDS) {
            int g=-60+r.nextInt(400);d[o+GROUND]=g;d[o+TOP]=g+(r.nextBoolean()?0:r.nextInt(30));
            d[o+WATER]=r.nextInt(4)==0?g+1+r.nextInt(40):NONE;
            d[o+CANOPY_BOTTOM]=d[o+TOP]-(d[o+TOP]>g?r.nextInt(d[o+TOP]-g):0);d[o+TOP_STATE]=r.nextInt(50);d[o+GROUND_STATE]=r.nextInt(5);d[o+DEEP_STATE]=1;d[o+BIOME]=r.nextInt(60);
        }
        var tile=new HorizonTile(3,key(4,-17,9),d);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        HorizonTile.CODEC.encode(buf,tile);int bytes=buf.readableBytes();var back=HorizonTile.CODEC.decode(buf);
        require(back.generation()==3&&back.key()==tile.key()&&java.util.Arrays.equals(back.data(),d)&&buf.readableBytes()==0,"tile roundtrip");
        require(bytes<HorizonTile.LENGTH*4/2,"tile encoding is compact: "+bytes+" bytes");
        System.out.println("HORIZON_PROTOCOL tileBytes="+bytes);
        // Malformed input is rejected rather than read out of bounds.
        var bad=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);bad.writeVarInt(0);bad.writeLong(-1L);
        rejects(()->HorizonTile.CODEC.decode(bad),"invalid tile key");
        var badPalette=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);badPalette.writeVarInt(0);badPalette.writeLong(key(0,0,0));badPalette.writeVarInt(0);
        rejects(()->HorizonTile.CODEC.decode(badPalette),"empty palette");
        int[] wet=d.clone();wet[WATER]=wet[GROUND];
        rejects(()->new HorizonTile(0,key(0,0,0),wet),"water at ground level");
        rejects(()->new HorizonTile(0,key(0,0,0),new int[5]),"short tile");
        // Requests.
        var request=new HorizonRequest(5,new long[]{key(0,1,-1),key(6,-3,2)});
        var rb=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);HorizonRequest.CODEC.encode(rb,request);var rback=HorizonRequest.CODEC.decode(rb);
        require(rback.generation()==5&&java.util.Arrays.equals(rback.keys(),request.keys()),"request roundtrip");
        var big=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);big.writeVarInt(0);big.writeVarInt(HorizonRequest.MAX_KEYS+1);
        rejects(()->HorizonRequest.CODEC.decode(big),"oversized request");
        // Worker lifecycle: idempotent start, stop releases workers and queues, restart works.
        require(!HorizonService.running(),"idle before a server starts");
        HorizonService.start();require(HorizonService.running(),"started");
        HorizonService.start();require(HorizonService.running(),"start is idempotent");
        HorizonService.stop();require(!HorizonService.running()&&HorizonService.queuedPlayers()==0,"stopped and cleared");
        HorizonService.stop();require(!HorizonService.running(),"stop is idempotent");
        HorizonService.start();require(HorizonService.running(),"restarted");HorizonService.stop();
        recovery();
        cactus();
        System.out.println("PASS");
    }
    /**
     * Column reading on a desert stack (stone, sand to y 64, then the given blocks): a cactus
     * stands on the sand, so its sides are drawn as cactus, while bare sand, a sandstone block
     * on the sand and a cactus on air (no ground to stand on) read as before.
     */
    private static void cactus() {
        var cactus=Blocks.CACTUS.defaultBlockState();var sand=Blocks.SAND.defaultBlockState();
        var sandstone=Blocks.SANDSTONE.defaultBlockState();var air=Blocks.AIR.defaultBlockState();
        int[] tall=column(y->y<=60?sandstone:y<=64?sand:y<=67?cactus:air);
        require(tall[TOP]==67&&tall[TOP_STATE]==Block.getId(cactus)&&tall[GROUND]==64&&tall[GROUND_STATE]==Block.getId(sand)&&tall[CANOPY_BOTTOM]==65&&!floating(tall,0),"three-block cactus stands on sand: "+Arrays.toString(tall));
        int[] one=column(y->y<=64?sand:y==65?cactus:air);
        require(one[TOP]==65&&one[GROUND]==64&&one[GROUND_STATE]==Block.getId(sand)&&!floating(one,0),"one-block cactus stands on sand: "+Arrays.toString(one));
        int[] bare=column(y->y<=64?sand:air);
        require(bare[TOP]==64&&bare[GROUND]==64&&bare[GROUND_STATE]==Block.getId(sand),"bare sand unchanged: "+Arrays.toString(bare));
        int[] block=column(y->y<=64?sand:y==65?sandstone:air);
        require(block[TOP]==65&&block[GROUND]==65&&block[GROUND_STATE]==Block.getId(sandstone),"a solid block on sand stays ground: "+Arrays.toString(block));
        int[] hanging=column(y->y<=64?sand:y==70?cactus:air);
        require(hanging[TOP]==70&&hanging[GROUND]==64&&floating(hanging,0),"a cactus over air floats as before: "+Arrays.toString(hanging));
        System.out.println("HORIZON_PROTOCOL cactus "+Arrays.toString(tall));
    }
    /** Reads one column of a stack described by y, with stone below y 0 and nothing above y 200. */
    private static int[] column(java.util.function.IntFunction<BlockState> stack) {
        var stone=Blocks.STONE.defaultBlockState();
        java.util.function.IntFunction<BlockState> at=y->y<0?stone:stack.apply(y);
        var source=new HorizonColumns.Source() {
            public BlockState state(int x,int y,int z){return at.apply(y);}
            public int top(java.util.function.Predicate<BlockState> p,int x,int z,int limit){for(int y=Math.min(limit,200);y>=-64;y--)if(p.test(at.apply(y)))return y+1;return -64;}
        };
        int[] d=new int[FIELDS];
        require(HorizonColumns.read(source,0,0,-64,200,d,0),"column has a surface");
        return d;
    }
    /** A tile the server and client accept: dry columns. */
    private static int[] tileData(){int[] d=new int[HorizonTile.LENGTH];for(int o=0;o<d.length;o+=FIELDS)d[o+WATER]=NONE;return d;}
    /**
     * Drives the real queue steps (enqueue, take, abandon, collect) with the client's resend
     * policy, one server and client tick per step, the camera still and no reconnect.
     * {@code fail} decides per attempt whether sampling throws. Returns the tick by which every
     * key arrived, or -1, and counts attempts per key.
     */
    private static int simulate(long[] keys,java.util.function.BiPredicate<Long,Integer> fail,Map<Long,Integer> attempts,int ticks) {
        var q=new HorizonService.Queue();var resend=new HorizonResend();var received=new HashSet<Long>();
        for(int t=0;t<ticks;t++) {
            var missing=Arrays.stream(keys).filter(k->!received.contains(k)).toArray();
            long[] send=resend.next(missing,t);
            if(send!=null)HorizonService.enqueue(q,1,send,k->true,t);
            var deferred=new ArrayList<Long>();
            for(HorizonService.Task task;q.outstanding.get()<HorizonService.IN_FLIGHT&&(task=HorizonService.take(q,deferred))!=null;) {
                int n=attempts.merge(task.key(),1,Integer::sum);
                if(fail.test(task.key(),n))HorizonService.abandon(q,task,new IllegalStateException("injected horizon failure"),t);
                else q.done.add(new HorizonTile(task.generation(),task.key(),tileData()));
            }
            for(HorizonTile tile;(tile=HorizonService.collect(q))!=null;)if(tile.generation()==q.generation)received.add(tile.key());
            if(received.size()==keys.length&&q.outstanding.get()==0)return t;
        }
        return -1;
    }
    /** Failed, stale and refreshed tiles recover without the camera moving; failures stay bounded. */
    private static void recovery() {
        long[] keys={key(0,0,0),key(0,1,0),key(1,0,1),key(2,-1,0)};long last=keys[keys.length-1];
        // The final outstanding tile fails once and is fetched again by the repeated request.
        var attempts=new HashMap<Long,Integer>();
        int done=simulate(keys,(k,n)->k==last&&n==1,attempts,5000);
        require(done>=0&&done<=HorizonResend.FIRST+HorizonService.RETRY_TICKS+1,"final tile recovers after one failure: tick "+done);
        require(attempts.get(last)==2&&attempts.get(keys[0])==1,"one retry, no resampling of delivered tiles: "+attempts);
        // A tile that always fails is retried with backoff, not every tick, and never blocks the others.
        attempts.clear();
        require(simulate(keys,(k,n)->k==last,attempts,72_000)==-1,"always-failing tile stays missing");
        int tries=attempts.get(last);
        require(tries>=5&&tries<=72_000/HorizonService.RETRY_MAX_TICKS+12,"bounded server retries per hour: "+tries);
        require(attempts.get(keys[0])==1,"other tiles delivered once: "+attempts);
        // A newer request generation resamples a tile still in flight for an older one; the stale result is dropped.
        var q=new HorizonService.Queue();var deferred=new ArrayList<Long>();long k=keys[0];
        HorizonService.enqueue(q,1,new long[]{k},x->true,0);var old=HorizonService.take(q,deferred);
        HorizonService.enqueue(q,1,new long[]{k},x->true,1);
        require(HorizonService.take(q,deferred)==null,"same generation does not resample an in-flight tile");
        HorizonService.enqueue(q,2,new long[]{k},x->true,2);var fresh=HorizonService.take(q,deferred);
        require(fresh!=null&&fresh.generation()==2,"newer generation resamples the in-flight tile");
        q.done.add(new HorizonTile(old.generation(),k,tileData()));var stale=HorizonService.collect(q);
        require(stale.generation()!=q.generation&&Objects.equals(q.inFlight.get(k),2),"stale completion keeps the newer task in flight");
        q.done.add(new HorizonTile(2,k,tileData()));
        require(HorizonService.collect(q).generation()==2&&q.inFlight.isEmpty()&&q.outstanding.get()==0,"newer result delivered and accounted");
        // A refresh queued while its tile is in flight waits for that result instead of being dropped.
        q=new HorizonService.Queue();deferred.clear();
        HorizonService.enqueue(q,1,new long[]{k},x->true,0);var running=HorizonService.take(q,deferred);
        require(HorizonService.refreshable(q,k),"a change during a tile's first sample refreshes it");
        q.refresh.add(k);
        require(HorizonService.take(q,deferred)==null&&deferred.equals(List.of(k))&&q.refresh.isEmpty(),"in-flight refresh deferred");
        q.refresh.addAll(deferred);deferred.clear();
        q.done.add(new HorizonTile(1,k,tileData()));HorizonService.collect(q);
        var refreshed=HorizonService.take(q,deferred);
        require(refreshed!=null&&refreshed.key()==k&&deferred.isEmpty(),"deferred refresh sampled after the earlier result");
        // Unchanged requests repeat with doubling intervals; a changed list is sent at once.
        var resend=new HorizonResend();long[] a={k};int sends=0;
        for(int t=0;t<20_000;t++)if(resend.next(a,t)!=null)sends++;
        require(sends>=5&&sends<=20_000/HorizonResend.MAX+6,"bounded resends: "+sends);
        require(resend.next(new long[]{k,last},20_000)!=null&&resend.next(new long[0],20_001)!=null&&resend.next(new long[0],30_000)==null,"changed lists sent at once, empty lists not repeated");
        // Meshing: the client resubmits a failed mesh when due and stops after three failures.
        var mesh=new HorizonRetry(20,400,3,1024);int submissions=1;boolean scheduled=false;
        for(int t=0;t<10_000;t++) {
            if(submissions>0&&!scheduled&&!mesh.exhausted(k)){mesh.failed(k,t);scheduled=!mesh.exhausted(k);}
            if(scheduled&&mesh.due(k,t)){scheduled=false;submissions++;}
        }
        require(submissions==3&&mesh.exhausted(k)&&!mesh.due(k,10_000),"mesh retries stop after three failures: "+submissions);
        mesh.succeeded(k);require(mesh.due(k,0)&&!mesh.exhausted(k),"new tile data restores mesh attempts");
        // Source cache: a load overlapping an invalidation is not cached; a later load is.
        var cache=new HorizonSourceCache<String>(4);long c=net.minecraft.world.level.ChunkPos.pack(3,-9);
        int stamp=cache.stamp(c);cache.forget(c);
        require(!cache.commit(c,stamp,"before edit")&&cache.get(c)==null,"load overlapping forget is discarded");
        require(cache.commit(c,cache.stamp(c),"after edit")&&"after edit".equals(cache.get(c)),"fresh load cached");
        int other=cache.stamp(net.minecraft.world.level.ChunkPos.pack(4,-9));
        require(cache.commit(net.minecraft.world.level.ChunkPos.pack(4,-9),other,"x"),"other chunks unaffected");
        System.out.println("HORIZON_RECOVERY finalTileRecoveredAt="+done+" serverRetriesPerHour="+tries+" resends="+sends+" meshSubmissions="+submissions);
    }
    private static void rejects(Runnable r,String what){try{r.run();}catch(RuntimeException expected){return;}throw new AssertionError("accepted "+what);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
