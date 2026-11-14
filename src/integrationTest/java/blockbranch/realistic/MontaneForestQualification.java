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

/** Native/schema/clearing contracts for the independently gated montane structure. */
public final class MontaneForestQualification {
    public static int[][] points(long seed){return seed==1?new int[][]{{-1744000,-1040000},{1584000,-944000},{1552000,-912000},{16000,-1488000},{-630018,-1698028},{1488000,-304000},{1478348,-996764},{1468516,-675537},{-1552000,-1904000}}:new int[][]{{592000,-496000},{48000,-432000},{208000,720000},{16000,-1488000},{-630018,-1698028},{528000,-272000},{510208,-976057},{1885815,-980681},{-1552000,-1904000}};}
    private static Run active;
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{Path result=Path.of(System.getProperty("blockbranch.montaneForestResult"));try{active=new Run(server.overworld(),result);}catch(Throwable e){fail(server,result,e);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{var run=active;if(run==null)return;try{if(run.tick()){active=null;server.halt(false);}}catch(Throwable e){active=null;fail(server,run.result,e);}});
    }
    private static final class Run {
        final ServerLevel level;final RealisticGenerator g;final Path result,dir;final boolean reopen;final long seed,began=System.nanoTime();final JsonObject report=new JsonObject();final int[][] sites;final List<ChunkPos> required;final Map<ChunkPos,CompletableFuture<ChunkResult<ChunkAccess>>> pending=new LinkedHashMap<>();final RealisticGenerator retained;final ReimaginedTreeClearPiece piece;final Map<Long,Integer> plan;final Set<Long> edited=new HashSet<>();final BlockPos gold;final StructurePieceSerializationContext context;int requested;
        Run(ServerLevel level,Path result)throws Exception{
            this.level=level;this.result=result;dir=result.toAbsolutePath().getParent();reopen=Boolean.getBoolean("blockbranch.montaneForestReopen");seed=level.getSeed();g=(RealisticGenerator)level.getChunkSource().getGenerator();require(seed==1||seed==424242,"Disclosed original seed");
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,level.getServer());
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();require(encoded.get("shape_version").getAsInt()==278&&g.coastalOakCrownDefinition()==1&&g.pineCrownDefinition()==1&&g.birchCrownDefinition()==1&&g.seasonalCrownDefinition()==1&&g.broadleafCrownDefinition()==1&&g.mangroveRootsDefinition()==1&&g.borealSlopeDefinition()==1&&g.montaneForestDefinition()==1&&g.alluvialDefinition()==1,"Bundled coastal-over-pine-over-crowns-over-roots-over-slope-over-montane278 required");report.addProperty("seed",seed);report.add("generator",encoded);var current=encoded;encoded=encoded.deepCopy();MontaneForestFixtures.montane271(encoded);var montane=RealisticGenerator.CODEC.codec().parse(ops,encoded).getOrThrow();require(montane.borealSlopeDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,montane).getOrThrow().equals(encoded),"Complete montane271 reads with the slope gate absent and re-encodes exactly");
            var original=encoded.deepCopy();MontaneForestFixtures.published270(original);retained=RealisticGenerator.CODEC.codec().parse(ops,original).getOrThrow();
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(original),"Absent montane gate retains exact270 encoding");
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,encoded).getOrThrow()).getOrThrow().equals(encoded),"Exact271 encoding");
            for(int version:new int[]{264,265,266,267,268,269,270,272,273}){var bad=encoded.deepCopy();bad.addProperty("shape_version",version);require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Mismatched saved version rejects");}
            for(String missing:new String[]{"montane_forest_definition","alluvial_floodplain_definition","seasonal_forest_definition","continental_ice_definition","leaf_litter_definition"}){var bad=encoded.deepCopy();bad.remove(missing);require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Incomplete foundation rejects");}
            for(int definition:new int[]{-1,0,2}){var invalid=encoded.deepCopy();invalid.addProperty("montane_forest_definition",definition);require(RealisticGenerator.CODEC.codec().parse(ops,invalid).error().isPresent(),"Unknown/absent definition rejects271");}
            var bad=encoded.deepCopy();bad.getAsJsonObject("additional_biomes").remove(MontaneForest.KEY);require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Required needleleaf holder rejects");
            if(seed==424242&&!reopen)Files.writeString(dir.resolve("selectors.json"),new GsonBuilder().setPrettyPrinting().create().toJson(MontaneForestDiagnostics.selectors(HabitatPalette.CODEC.codec().parse(ops,encoded).getOrThrow()))+"\n");
            context=StructurePieceSerializationContext.fromLevel(level);Path saved=dir.resolve("original-clear.nbt");
            if(reopen)piece=BorealClearSmoke.load(context,NbtIo.readCompressed(saved,NbtAccounter.unlimitedHeap()));else{BlockPos stem=stem();piece=new ReimaginedTreeClearPiece(new BoundingBox(stem.getX(),stem.getY(),stem.getZ(),stem.getX(),stem.getY(),stem.getZ()),seed,g.borealCrownDefinition(),g.vegetationFlags()&~BorealSlope.FLAG&~BroadleafCrowns.FLAG&~SeasonalCrowns.FLAG&~BirchCrowns.FLAG&~PineCrowns.FLAG&~CoastalOakCrowns.FLAG);NbtIo.writeCompressed(piece.createTag(context),saved);}
            require(piece.createTag(context).getIntOr("ClearDefinition",0)==256,"Explicit montane saved vegetation algorithms without the slope flag");plan=BorealClearSmoke.plan(piece,g,level.getChunkSource().randomState());
            var construction=piece.createTag(context).getIntArray("Construction").orElseThrow();var box=new BoundingBox(construction[0],construction[1],construction[2],construction[3],construction[4],construction[5]);
            var root=MontaneForestTrees.roots(seed,construction[0],construction[2],construction[0],construction[2],g.drainage(level.getChunkSource().randomState()),g.climate(level.getChunkSource().randomState()),g.montaneForestOwner(level.getChunkSource().randomState())).stream().filter(r->r.x()==construction[0]&&r.z()==construction[2]&&r.y()==construction[1]).findFirst().orElseThrow();
            require(TaigaTrees.tree(root).stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))),"Entire actual conifer geometry belongs to saved clear footprint");
            for(int flags=0;flags<128;flags++)for(int crown=0;crown<2;crown++){var tag=new ReimaginedTreeClearPiece(box,seed,crown,flags).createTag(context);require(BorealClearSmoke.load(context,tag).createTag(context).equals(tag),"All256 saved plan flags roundtrip");}
            var oldTag=new ReimaginedTreeClearPiece(box,seed,1,63).createTag(context);
            require(BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),g,level.getChunkSource().randomState()).equals(BorealClearSmoke.plan(BorealClearSmoke.load(context,oldTag),retained,level.getChunkSource().randomState())),"Saved128 footprint ignores new roots in actual changed stand");require(plan.size()>50,"Nontrivial new whole-tree footprint");edited.addAll(plan.keySet());var first=points(seed)[0];int x=(first[0]&~15)+1,z=(first[1]&~15)+1;gold=new BlockPos(x,g.drainage(level.getChunkSource().randomState()).sample(x,z).height()-2,z);edited.add(gold.asLong());
            var originalSites=points(seed);sites=java.util.Arrays.copyOf(originalSites,originalSites.length+1);sites[sites.length-1]=MontaneForestDiagnostics.edge(level,g,originalSites);
            var chunks=new TreeSet<ChunkPos>(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));for(var p:sites)for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)chunks.add(new ChunkPos((p[0]>>4)+dx,(p[1]>>4)+dz));for(long key:plan.keySet()){var p=BlockPos.of(key);chunks.add(new ChunkPos(p.getX()>>4,p.getZ()>>4));}required=new ArrayList<>(chunks);Files.writeString(dir.resolve("preparation.json"),new Gson().toJson(Map.of("seed",seed,"chunks",required.size(),"clearCells",plan.size(),"construction",piece.createTag(context).getIntArray("Construction").orElseThrow()))+"\n");
        }
        private BlockPos stem(){
            var p=points(seed)[0];var random=level.getChunkSource().randomState();var d=g.drainage(random);var owner=g.montaneForestOwner(random);
            var roots=MontaneForestTrees.roots(seed,p[0]-96,p[1]-96,p[0]+96,p[1]+96,d,g.climate(random),owner);
            var root=roots.stream().filter(r->r.height()>=22).findFirst().orElseThrow(()->new AssertionError("No mature conifer in fixed96m witness window"));
            return new BlockPos(root.x(),root.y(),root.z());
        }
        boolean tick()throws Exception{
            require(System.nanoTime()-began<900_000_000_000L,"Bounded original native deadline");for(int n=0;n<8&&requested<required.size();n++,requested++){var p=required.get(requested);level.setChunkForced(p.x(),p.z(),true);pending.put(p,((blockbranch.mixin.cubic.PreviewChunkFutureAccessor)level.getChunkSource()).blockbranch$previewFuture(p.x(),p.z(),ChunkStatus.FULL,true));}
            if(requested<required.size()||pending.values().stream().anyMatch(f->!f.isDone()))return false;for(var f:pending.values())require(f.join().isSuccess(),"Original native generation failed: "+f.join().getError());finish();for(var p:required)level.setChunkForced(p.x(),p.z(),false);return true;
        }
        private void finish()throws Exception{
            report.add("structure",new Gson().toJsonTree(MontaneForestDiagnostics.structure(level,g,sites,edited)));var nativeStates=hash();var profiles=hash();var physical=hash();var random=level.getChunkSource().randomState();var d=g.drainage(random);int reads=0;
            for(var p:required){var chunk=level.getChunk(p.x(),p.z());var profile=((SparseColumn)chunk).blockbranch$terrain().base();add(profiles,p+":"+profile.encode(level.palettedContainerFactory()));for(int z=p.getMinBlockZ();z<=p.getMaxBlockZ();z++)for(int x=p.getMinBlockX();x<=p.getMaxBlockX();x++){var sample=d.sample(x,z);add(physical,x+","+z+":"+sample);for(int y=sample.height()-3;y<=sample.height()+48;y++){var pos=new BlockPos(x,y,z);if(edited.contains(pos.asLong()))continue;var actual=level.getBlockState(pos);require(actual.equals(g.surfaceState(x,y,z,random)),"Original native/scalar parity at "+pos);add(nativeStates,pos+":"+actual);reads++;}}}
            report.addProperty("nativeReads",reads);report.addProperty("nativeSHA256",hex(nativeStates));report.addProperty("profileCount",required.size());report.addProperty("profileSHA256",hex(profiles));report.addProperty("physicalSHA256",hex(physical));report.addProperty("clearCells",plan.size());report.addProperty("clearDefinition",256);String footprint=new TreeMap<>(plan).toString();
            if(reopen){require(report.equals(JsonParser.parseString(Files.readString(dir.resolve("baseline.json")))),"Montane271 observable outputs changed");require(footprint.equals(Files.readString(dir.resolve("clear-footprint.txt"))),"Original saved clear footprint changed");for(var value:JsonParser.parseString(Files.readString(dir.resolve("edits.json"))).getAsJsonArray()){var edit=value.getAsJsonObject();require(level.getBlockState(BlockPos.of(edit.get("position").getAsLong())).toString().equals(edit.get("state").getAsString()),"Original saved clear/player edit lost");}}
            else{
                var initial=new HashMap<Long,net.minecraft.world.level.block.state.BlockState>();for(long key:plan.keySet())initial.put(key,level.getBlockState(BlockPos.of(key)));long player=initial.entrySet().stream().filter(e->e.getValue().is(BlockTags.LOGS)).mapToLong(Map.Entry::getKey).findFirst().orElseThrow();initial.put(player,Blocks.GOLD_BLOCK.defaultBlockState());level.setBlock(BlockPos.of(player),Blocks.GOLD_BLOCK.defaultBlockState(),3);BorealClearSmoke.replay(level,g,context,piece.createTag(context),initial,false,true,dir,true);level.setBlock(gold,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                var edits=new JsonArray();for(long key:new TreeSet<>(edited)){var e=new JsonObject();e.addProperty("position",key);e.addProperty("state",level.getBlockState(BlockPos.of(key)).toString());edits.add(e);}Files.writeString(dir.resolve("edits.json"),edits.toString()+"\n");Files.writeString(dir.resolve("clear-footprint.txt"),footprint);Files.writeString(dir.resolve("baseline.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            }
            level.getServer().saveEverything(false,true,true);Files.writeString(result,"PASS complete montane271 native/profile/scalar/clear/edit snapshot\n");Blockbranch.LOGGER.info("MONTANE_FOREST_PASS seed={} chunks={} reads={} clear={}",seed,required.size(),reads,plan.size());
        }
    }
    private static MessageDigest hash()throws NoSuchAlgorithmException{return MessageDigest.getInstance("SHA-256");}
    private static void add(MessageDigest h,String s){h.update((s+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static String hex(MessageDigest h){return HexFormat.of().formatHex(h.digest());}
    private static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static void fail(net.minecraft.server.MinecraftServer server,Path result,Throwable e){Blockbranch.LOGGER.error("MONTANE_FOREST_FAIL",e);try{Files.writeString(result,"FAIL "+e+"\n");}catch(Exception ignored){}server.halt(false);}
    private MontaneForestQualification(){}
}
