package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Observable-output collector, also compiled against the complete original schema-269 public
 * production tree in a separate JVM. No copied selection or hydrology algorithms. */
public final class Published269Snapshot {
    public static final int[][] POINTS={{499704,-969009},{1542252,-915875},{1521994,-866602},{1720835,-907590},{1525275,-839951},{1468516,-675537},{-1726231,-647124},{-1514188,-472896},{1488000,-304000},{1520000,-336000},{432000,-784000},{-1552000,-1904000},{304000,784000}};
    public static JsonObject capture(ServerLevel level,RealisticGenerator generator)throws Exception {
        var random=level.getChunkSource().randomState();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().getAsJsonObject();
        if(encoded.get("shape_version").getAsInt()!=269)throw new AssertionError("Original complete schema269 settings required");
        var palette=HabitatPalette.CODEC.codec().parse(ops,encoded).getOrThrow();
        var report=new JsonObject();var grids=new JsonArray();
        for(long seed:random.seed()==424242?new long[]{424242,1,-17}:new long[0]){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);
            var m=palette.margins();var sentinel=Holder.direct(m.fallback().value());
            var tagged=new HabitatPalette(new HabitatPalette.Margins(sentinel,m.scree(),m.reedbed(),m.riverMargin(),m.sandyShore(),m.lake(),m.riverChannel(),m.shelf(),m.deepOcean()),palette.land(),palette.additional());
            var source=new ScreeBiomeSource(tagged,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true).withSummerDryWoodlands(true).withWarmXeric(true).withSeasonalForests(true);source.bind(seed,d,c);
            var terrain=digest();var biomes=digest();int samples=0,fallback=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var s=d.sample(x,z);var q=c.sample(x,z,d::sample);
                add(terrain,x+","+z+":"+s+":"+d.channelAt(x,z)+":"+d.oceanWater(x,z,s)+":"+d.terminalLakeWater(x,z,s)+":"+Double.toHexString(q.meanTemperatureC())+":"+Double.toHexString(q.moisture()));
                var biome=source.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);boolean isFallback=biome==sentinel;
                add(biomes,x+","+z+":"+(isFallback?"fallback":biome.unwrapKey().orElseThrow().identifier().toString()));
                if(isFallback)fallback++;samples++;
            }
            var entry=new JsonObject();entry.addProperty("seed",seed);entry.addProperty("samples",samples);entry.addProperty("fallback",fallback);
            entry.addProperty("terrainClimateOwnershipSHA256",hex(terrain));entry.addProperty("selectorSHA256",hex(biomes));grids.add(entry);
        }
        report.add("grids",grids);report.addProperty("nativeSeed",random.seed());
        var physical=digest();var drainage=generator.drainage(random);int physicalCount=0;
        for(var p:POINTS){
            Blockbranch.LOGGER.info("PUBLISHED269_POINT_BEGIN seed={} x={} z={}",random.seed(),p[0],p[1]);
            for(int dz=-8;dz<=8;dz+=4)for(int dx=-8;dx<=8;dx+=4){
                int x=p[0]+dx,z=p[1]+dz;var s=drainage.sample(x,z);
                for(int y=s.height()-4;y<=Math.max(s.height()+40,s.waterLevel()+1);y++){
                    add(physical,x+","+y+","+z+":"+generator.surfaceState(x,y,z,random));physicalCount++;
                }
            }
        }
        report.addProperty("physicalStates",physicalCount);report.addProperty("physicalSHA256",hex(physical));
        return report;
    }
    /** Recording is explicit and refuses any newer schema. Provenance must also
     * verify the entire production source tree before using a recording as an oracle. */
    private static NativeRecording recording;
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            Path result=Path.of(System.getProperty("blockbranch.published269SnapshotResult"));
            try{recording=new NativeRecording(server.overworld(),result);}
            catch(Throwable failure){fail(server,result,failure);}
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{var r=recording;if(r==null)return;try{if(r.tick()){recording=null;server.halt(false);}}catch(Throwable failure){recording=null;fail(server,r.result,failure);}});
    }
    /** The existing nonblocking future accessor lets normal ticks process
     * distance-manager/lighting work; forced QA tickets retain each requested chunk.
     * One native request at a time; the60s deadline detects a stall, not a speed target. */
    private static final class NativeRecording {
        final ServerLevel level;final RealisticGenerator generator;final Path result;final JsonObject snapshot;
        final MessageDigest states=digest(),profiles=digest();final Set<Integer> excluded;int point,count;long requested;
        java.util.concurrent.CompletableFuture<net.minecraft.server.level.ChunkResult<net.minecraft.world.level.chunk.ChunkAccess>> future;
        NativeRecording(ServerLevel level,Path result)throws Exception{
            this.level=level;this.result=result;generator=(RealisticGenerator)level.getChunkSource().getGenerator();
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,level.getServer());
            if(Boolean.getBoolean("blockbranch.published269Diagnostic")){snapshot=new JsonObject();excluded=Set.of();point=5;return;}
            snapshot=capture(level,generator);
            var observed=Published269CoverAudit.capture(level,generator,result.resolveSibling(Boolean.getBoolean("blockbranch.published269Reopen")?"reopen-cover-audit.json":"original-cover-audit.json"));
            excluded=new TreeSet<>();
            if(Boolean.getBoolean("blockbranch.published269Reopen")){
                var baseline=JsonParser.parseString(Files.readString(result.resolveSibling("baseline.json"))).getAsJsonObject();
                baseline.getAsJsonArray("nativeExcludedPoints").forEach(v->excluded.add(v.getAsInt()));
            }else excluded.addAll(observed);
            snapshot.add("nativeExcludedPoints",new Gson().toJsonTree(excluded));

            Files.writeString(result.resolveSibling(Boolean.getBoolean("blockbranch.published269Reopen")?"reopen-physical.json":"physical-baseline.json"),snapshot.toString()+"\n");
        }
        boolean tick()throws Exception{
            while(point<POINTS.length&&excluded.contains(point)){Blockbranch.LOGGER.warn("PUBLISHED269_NATIVE_EXCLUDED_ORIGINAL_PROFILE point={}",point);point++;}
            if(point>=POINTS.length)return finish();
            if(future==null){var p=POINTS[point];requested=System.nanoTime();
                Blockbranch.LOGGER.info("PUBLISHED269_NATIVE_REQUEST seed={} point={} x={} z={}",level.getSeed(),point,p[0],p[1]);
                level.setChunkForced(p[0]>>4,p[1]>>4,true);
                future=((blockbranch.mixin.cubic.PreviewChunkFutureAccessor)level.getChunkSource()).blockbranch$previewFuture(p[0]>>4,p[1]>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);return false;}
            if(!future.isDone()){if(System.nanoTime()-requested>60_000_000_000L)throw new AssertionError("Native chunk future stalled at point"+point+" "+Arrays.toString(POINTS[point]));return false;}
            var read=future.join();var chunk=read.orElseThrow(()->new AssertionError("Native full chunk unavailable at point"+point+": "+read.getError()));
            var p=POINTS[point];var profile=((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain().base();
            add(profiles,p[0]+","+p[1]+":"+profile.encode(level.palettedContainerFactory()));
            var drainage=generator.drainage(level.getChunkSource().randomState());
            for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                int x=(p[0]&~15)+dx,z=(p[1]&~15)+dz;var sample=drainage.sample(x,z);
                for(int y=sample.height()-3;y<=Math.max(sample.height()+40,sample.waterLevel());y++){
                    if(x==(POINTS[0][0]&~15)+1&&z==(POINTS[0][1]&~15)+1&&y==sample.height()-2)continue;
                    add(states,x+","+y+","+z+":"+chunk.getBlockState(new BlockPos(x,y,z)));count++;
                }
            }
            if(point==0){int x=(p[0]&~15)+1,z=(p[1]&~15)+1;var edit=new BlockPos(x,drainage.sample(x,z).height()-2,z);
                if(Boolean.getBoolean("blockbranch.published269Reopen")){if(!level.getBlockState(edit).is(Blocks.GOLD_BLOCK))throw new AssertionError("Saved original edit lost");}
                else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            }
            Blockbranch.LOGGER.info("PUBLISHED269_NATIVE_COMPLETE point={}",point);future=null;point++;
            if(Boolean.getBoolean("blockbranch.published269Diagnostic")){Files.writeString(result,"PASS bounded native diagnostic only\n");return true;}
            if(point<POINTS.length)return false;
            return finish();
        }
        private boolean finish()throws Exception{
            for(var site:POINTS)level.setChunkForced(site[0]>>4,site[1]>>4,false);
            snapshot.addProperty("nativeStates",count);snapshot.addProperty("nativeSHA256",hex(states));snapshot.addProperty("nativeProfileSHA256",hex(profiles));
            if(Boolean.getBoolean("blockbranch.published269Reopen")){
                var expected=JsonParser.parseString(Files.readString(result.resolveSibling("baseline.json")));
                if(!snapshot.equals(expected))throw new AssertionError("Published269 observable hashes changed");
                Files.writeString(result,"PASS authentic269 original hashes and saved edit\n");
            }else{
                level.getServer().saveEverything(false,true,true);
                Files.writeString(result.resolveSibling("baseline.json"),new GsonBuilder().setPrettyPrinting().create().toJson(snapshot)+"\n");
                Files.writeString(result,"PASS original production snapshot, recorded native subset and saved edit\n");
            }
            return true;
        }
    }
    private static void fail(net.minecraft.server.MinecraftServer server,Path result,Throwable failure){Blockbranch.LOGGER.error("PUBLISHED_269_SNAPSHOT_FAIL",failure);try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}server.halt(false);}
    private static MessageDigest digest()throws NoSuchAlgorithmException{return MessageDigest.getInstance("SHA-256");}
    private static void add(MessageDigest digest,String text){digest.update((text+"\n").getBytes(StandardCharsets.UTF_8));}
    private static String hex(MessageDigest digest){return HexFormat.of().formatHex(digest.digest());}
    private Published269Snapshot(){}
}
