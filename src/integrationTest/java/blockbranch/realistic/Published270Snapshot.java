package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Observable outputs of complete original270 production. No copied selector,
 * climate, tree or hydrology algorithms. Record only against the entire original
 * production tree; failed native targets are failures, never silent exclusions. */
public final class Published270Snapshot {
    public static int[][] points(long seed){return seed==1?new int[][]{{-1744000,-1040000},{1584000,-944000},{1552000,-912000},{16000,-1488000},{-630018,-1698028},{1488000,-304000},{1478348,-996764},{1468516,-675537},{-1552000,-1904000}}:new int[][]{{592000,-496000},{48000,-432000},{208000,720000},{16000,-1488000},{-630018,-1698028},{528000,-272000},{510208,-976057},{1885815,-980681},{-1552000,-1904000}};}
    public static JsonObject grids(HabitatPalette palette)throws Exception{
        var entries=new JsonArray();
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);var m=palette.margins();var sentinel=Holder.direct(m.fallback().value());
            var tagged=new HabitatPalette(new HabitatPalette.Margins(sentinel,m.scree(),m.reedbed(),m.riverMargin(),m.sandyShore(),m.lake(),m.riverChannel(),m.shelf(),m.deepOcean()),palette.land(),palette.additional());
            var source=new ScreeBiomeSource(tagged,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true).withSummerDryWoodlands(true).withWarmXeric(true).withSeasonalForests(true);source.bind(seed,d,c);
            var physical=hash();var identities=hash();int count=0,fallback=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){var s=d.sample(x,z);var q=c.sample(x,z,d::sample);add(physical,x+","+z+":"+s+":"+d.channelAt(x,z)+":"+d.oceanWater(x,z,s)+":"+d.terminalLakeWater(x,z,s)+":"+Double.toHexString(q.meanTemperatureC())+":"+Double.toHexString(q.moisture()));var id=source.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);boolean f=id==sentinel;add(identities,x+","+z+":"+(f?"fallback":id.unwrapKey().orElseThrow().identifier()));if(f)fallback++;count++;}
            var row=new JsonObject();row.addProperty("seed",seed);row.addProperty("samples",count);row.addProperty("fallback",fallback);row.addProperty("physicalClimateSHA256",hex(physical));row.addProperty("selectorSHA256",hex(identities));entries.add(row);
        }
        var result=new JsonObject();result.add("grids",entries);return result;
    }
    private static Run active;
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{Path result=Path.of(System.getProperty("blockbranch.published270Result"));try{active=new Run(server.overworld(),result);}catch(Throwable e){fail(server,result,e);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{var run=active;if(run==null)return;try{if(run.tick()){active=null;server.halt(false);}}catch(Throwable e){active=null;fail(server,run.result,e);}});
    }
    private static final class Run {
        final ServerLevel level;final RealisticGenerator g;final Path result,dir;final boolean reopen;final long seed,began=System.nanoTime();final JsonObject report=new JsonObject();final List<ChunkPos> required;final Map<ChunkPos,CompletableFuture<ChunkResult<ChunkAccess>>> pending=new LinkedHashMap<>();final ReimaginedTreeClearPiece piece;final Map<Long,Integer> plan;final Set<Long> edited=new HashSet<>();final BlockPos gold;final StructurePieceSerializationContext context;int requested;
        Run(ServerLevel level,Path result)throws Exception{
            this.level=level;this.result=result;dir=result.toAbsolutePath().getParent();reopen=Boolean.getBoolean("blockbranch.published270Reopen");seed=level.getSeed();g=(RealisticGenerator)level.getChunkSource().getGenerator();require(seed==1||seed==424242,"Disclosed original seed");
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,level.getServer());
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();require(encoded.get("shape_version").getAsInt()==270&&g.alluvialDefinition()==1,"Complete original270 required");report.addProperty("seed",seed);report.add("generator",encoded);
            if(seed==424242)report.add("global",grids(HabitatPalette.CODEC.codec().parse(ops,encoded).getOrThrow()));
            context=StructurePieceSerializationContext.fromLevel(level);Path saved=dir.resolve("original-clear.nbt");
            if(reopen)piece=BorealClearSmoke.load(context,NbtIo.readCompressed(saved,NbtAccounter.unlimitedHeap()));else{BlockPos stem=stem();piece=new ReimaginedTreeClearPiece(new BoundingBox(stem.getX(),stem.getY(),stem.getZ(),stem.getX(),stem.getY(),stem.getZ()),seed,g.borealCrownDefinition(),g.vegetationFlags());NbtIo.writeCompressed(piece.createTag(context),saved);}
            require(piece.createTag(context).getIntOr("ClearDefinition",0)==128,"Original saved vegetation algorithms");plan=BorealClearSmoke.plan(piece,g,level.getChunkSource().randomState());require(plan.size()>50,"Nontrivial old whole-tree footprint");edited.addAll(plan.keySet());var first=points(seed)[0];int x=(first[0]&~15)+1,z=(first[1]&~15)+1;gold=new BlockPos(x,g.drainage(level.getChunkSource().randomState()).sample(x,z).height()-2,z);edited.add(gold.asLong());
            var chunks=new TreeSet<ChunkPos>(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));for(var p:points(seed))for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)chunks.add(new ChunkPos((p[0]>>4)+dx,(p[1]>>4)+dz));for(long key:plan.keySet()){var p=BlockPos.of(key);chunks.add(new ChunkPos(p.getX()>>4,p.getZ()>>4));}required=new ArrayList<>(chunks);Files.writeString(dir.resolve("preparation.json"),new Gson().toJson(Map.of("seed",seed,"chunks",required.size(),"clearCells",plan.size(),"construction",piece.createTag(context).getIntArray("Construction").orElseThrow()))+"\n");
        }
        private BlockPos stem(){var p=points(seed)[0];var random=level.getChunkSource().randomState();var d=g.drainage(random);for(int z=p[1]-48;z<=p[1]+48;z++)for(int x=p[0]-48;x<=p[0]+48;x++){int h=d.sample(x,z).height();var canopy=g.canopy(x,z,random);for(int y=h+1;y<=h+8;y++){var s=canopy.state(x,y,z);if(s!=null&&s.is(BlockTags.LOGS)&&s.hasProperty(RotatedPillarBlock.AXIS)&&s.getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Y)return new BlockPos(x,y,z);}}throw new AssertionError("No original tree in bounded affected-site search");}
        boolean tick()throws Exception{
            require(System.nanoTime()-began<900_000_000_000L,"Bounded original native deadline");for(int n=0;n<8&&requested<required.size();n++,requested++){var p=required.get(requested);level.setChunkForced(p.x(),p.z(),true);pending.put(p,((blockbranch.mixin.cubic.PreviewChunkFutureAccessor)level.getChunkSource()).blockbranch$previewFuture(p.x(),p.z(),ChunkStatus.FULL,true));}
            if(requested<required.size()||pending.values().stream().anyMatch(f->!f.isDone()))return false;for(var f:pending.values())require(f.join().isSuccess(),"Original native generation failed: "+f.join().getError());finish();for(var p:required)level.setChunkForced(p.x(),p.z(),false);return true;
        }
        private void finish()throws Exception{
            var nativeStates=hash();var profiles=hash();var physical=hash();var random=level.getChunkSource().randomState();var d=g.drainage(random);int reads=0;
            for(var p:required){var chunk=level.getChunk(p.x(),p.z());var profile=((SparseColumn)chunk).blockbranch$terrain().base();add(profiles,p+":"+profile.encode(level.palettedContainerFactory()));for(int z=p.getMinBlockZ();z<=p.getMaxBlockZ();z++)for(int x=p.getMinBlockX();x<=p.getMaxBlockX();x++){var sample=d.sample(x,z);add(physical,x+","+z+":"+sample);for(int y=sample.height()-3;y<=sample.height()+48;y++){var pos=new BlockPos(x,y,z);if(edited.contains(pos.asLong()))continue;var actual=level.getBlockState(pos);require(actual.equals(g.surfaceState(x,y,z,random)),"Original native/scalar parity at "+pos);add(nativeStates,pos+":"+actual);reads++;}}}
            report.addProperty("nativeReads",reads);report.addProperty("nativeSHA256",hex(nativeStates));report.addProperty("profileCount",required.size());report.addProperty("profileSHA256",hex(profiles));report.addProperty("physicalSHA256",hex(physical));report.addProperty("clearCells",plan.size());report.addProperty("clearDefinition",128);String footprint=new TreeMap<>(plan).toString();
            if(reopen){require(report.equals(JsonParser.parseString(Files.readString(dir.resolve("baseline.json")))),"Original270 observable outputs changed");require(footprint.equals(Files.readString(dir.resolve("clear-footprint.txt"))),"Original saved clear footprint changed");for(var value:JsonParser.parseString(Files.readString(dir.resolve("edits.json"))).getAsJsonArray()){var edit=value.getAsJsonObject();require(level.getBlockState(BlockPos.of(edit.get("position").getAsLong())).toString().equals(edit.get("state").getAsString()),"Original saved clear/player edit lost");}}
            else{
                var initial=new HashMap<Long,net.minecraft.world.level.block.state.BlockState>();for(long key:plan.keySet())initial.put(key,level.getBlockState(BlockPos.of(key)));long player=initial.entrySet().stream().filter(e->e.getValue().is(BlockTags.LOGS)).mapToLong(Map.Entry::getKey).findFirst().orElseThrow();initial.put(player,Blocks.GOLD_BLOCK.defaultBlockState());level.setBlock(BlockPos.of(player),Blocks.GOLD_BLOCK.defaultBlockState(),3);BorealClearSmoke.replay(level,g,context,piece.createTag(context),initial,false,true,dir,true);level.setBlock(gold,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                var edits=new JsonArray();for(long key:new TreeSet<>(edited)){var e=new JsonObject();e.addProperty("position",key);e.addProperty("state",level.getBlockState(BlockPos.of(key)).toString());edits.add(e);}Files.writeString(dir.resolve("edits.json"),edits.toString()+"\n");Files.writeString(dir.resolve("clear-footprint.txt"),footprint);Files.writeString(dir.resolve("baseline.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            }
            level.getServer().saveEverything(false,true,true);Files.writeString(result,"PASS complete original270 native/profile/scalar/clear/edit snapshot\n");Blockbranch.LOGGER.info("PUBLISHED270_PASS seed={} chunks={} reads={} clear={}",seed,required.size(),reads,plan.size());
        }
    }
    private static MessageDigest hash()throws NoSuchAlgorithmException{return MessageDigest.getInstance("SHA-256");}
    private static void add(MessageDigest h,String s){h.update((s+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static String hex(MessageDigest h){return HexFormat.of().formatHex(h.digest());}
    private static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static void fail(net.minecraft.server.MinecraftServer server,Path result,Throwable e){Blockbranch.LOGGER.error("PUBLISHED270_FAIL",e);try{Files.writeString(result,"FAIL "+e+"\n");}catch(Exception ignored){}server.halt(false);}
    private Published270Snapshot(){}
}
