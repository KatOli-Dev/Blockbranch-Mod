package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.security.MessageDigest;
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

/** Native geometry, exact legacy footprint, conservative stage certificates,
 * base APIs, compressed profiles, saved clear plans and explicit edits. */
public final class AlluvialQualification {
    private static Run run;
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{Path result=Path.of(System.getProperty("blockbranch.alluvialResult"));try{run=new Run(server.overworld(),result);}catch(Throwable e){fail(server,result,e);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{var r=run;if(r==null)return;try{if(r.tick()){run=null;server.halt(false);}}catch(Throwable e){run=null;fail(server,r.result,e);}});
    }
    private static final class Scene {
        final int x,z,radius;final boolean junction;int minX,maxX,minZ,maxZ;
        Scene(int x,int z,int radius,boolean junction){this.x=x;this.z=z;this.radius=radius;this.junction=junction;minX=x-radius;maxX=x+radius;minZ=z-radius;maxZ=z+radius;}
    }
    private static final class Run {
        final ServerLevel level;final RealisticGenerator g;final long seed;final Path result,dir;final boolean reopen;
        final List<Scene> scenes=new ArrayList<>();final List<ChunkPos> required;final Map<ChunkPos,CompletableFuture<ChunkResult<ChunkAccess>>> pending=new LinkedHashMap<>();
        final Map<String,Object> report=new LinkedHashMap<>();final long began=System.nanoTime();final Set<Long> edited=new HashSet<>();
        final StructurePieceSerializationContext context;final ReimaginedTreeClearPiece piece;final Map<Long,Integer> clear;final BlockPos soilEdit;int requested;
        Run(ServerLevel level,Path result)throws Exception{
            this.level=level;this.result=result;dir=result.toAbsolutePath().getParent();reopen=Boolean.getBoolean("blockbranch.alluvialReopen");seed=level.getSeed();
            require(seed==Long.getLong("blockbranch.alluvialSeed",1),"Recorded seed");g=(RealisticGenerator)level.getChunkSource().getGenerator();
            require(g.alluvialDefinition()==1,"Actual saved alluvial gate");
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,level.getServer());
            var random=level.getChunkSource().randomState();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var fresh=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
            require(fresh.get("shape_version").getAsInt()==(g.pineCrownDefinition()==1?277:g.birchCrownDefinition()==1?276:g.seasonalCrownDefinition()==1?275:g.broadleafCrownDefinition()==1?274:g.mangroveRootsDefinition()==1?273:g.borealSlopeDefinition()==1?272:g.montaneForestDefinition()==1?271:270)&&RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow()).getOrThrow().equals(fresh),"Exact current codec roundtrip");
            for(int version:new int[]{264,265,266,267,268,269,270,271,272,273}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);reject(ops,bad);}
            for(int definition:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("alluvial_floodplain_definition",definition);reject(ops,bad);}
            for(String key:new String[]{"boreal_slope_definition","montane_forest_definition","alluvial_floodplain_definition","seasonal_forest_definition","gravel_relief_definition","leaf_litter_definition"}){var bad=fresh.deepCopy();bad.remove(key);reject(ops,bad);}
            var old=fresh.deepCopy();AlluvialFixtures.published269(old);var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();require(retained.alluvialDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent retains269 exactly");
            report.put("oldFoundationCodecCases",LeafLitterAudit.gateMatrix(ops,g));report.put("seed",seed);
            if(!reopen){
                JsonObject preset;try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(AlluvialQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/alluvial_baseline.json")),java.nio.charset.StandardCharsets.UTF_8)){preset=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");}
                var original=RealisticGenerator.CODEC.codec().parse(ops,preset).getOrThrow();var frozen=AlluvialLegacyVerify.frozen(seed);var scoped=AlluvialLegacyDense.capture(level,original,frozen);
                Files.writeString(dir.resolve("legacy-original-scope.json"),scoped.toString()+"\n");
                var observed=JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.alluvialLegacyAudit")))).getAsJsonObject();report.put("legacyExactFootprint",AlluvialLegacyVerify.verify(frozen,observed,scoped));
            }
            int[][] centers=seed==1?new int[][]{{1478348,-996764},{1468516,-675537}}:new int[][]{{510208,-976057},{1885815,-980681}};
            var d=g.drainage(random);for(var p:centers)scenes.add(new Scene(p[0],p[1],48,false));
            int[] children=new int[d.size()];for(int i=0;i<d.size();i++)if(d.river(i))children[d.parent(i)]++;
            int junction=-1;double nearest=Double.POSITIVE_INFINITY;
            for(int node=0;node<d.size();node++)if(children[node]>=2&&d.waterLevel(node)>TerrainShape.SEA_LEVEL+8){double dx=d.nodeX(node)-centers[0][0],dz=d.nodeZ(node)-centers[0][1],distance=dx*dx+dz*dz;if(distance<nearest){nearest=distance;junction=node;}}
            require(junction>=0,"Nearest actual tributary confluence in finite solved graph");scenes.add(new Scene(d.nodeX(junction),d.nodeZ(junction),24,true));report.put("confluenceIncomingRivers",children[junction]);
            for(var s:scenes)prepareScene(s);
            context=StructurePieceSerializationContext.fromLevel(level);
            if(seed==1){
            Path saved=dir.resolve("clear-plan.nbt");
            if(reopen)piece=BorealClearSmoke.load(context,NbtIo.readCompressed(saved,NbtAccounter.unlimitedHeap()));
            else{var point=woodedBench();piece=new ReimaginedTreeClearPiece(new BoundingBox(point.getX(),point.getY(),point.getZ(),point.getX(),point.getY(),point.getZ()),seed,g.borealCrownDefinition(),g.vegetationFlags());NbtIo.writeCompressed(piece.createTag(context),saved);}
            require(piece.createTag(context).getIntOr("ClearDefinition",0)==1+g.borealCrownDefinition()+2*g.vegetationFlags(),"Saved current vegetation algorithms with immutable270 terrain");clear=BorealClearSmoke.plan(piece,g,random);require(clear.size()>50,"Nontrivial whole-tree clear footprint on changed ground");edited.addAll(clear.keySet());
            var construction=piece.createTag(context).getIntArray("Construction").orElseThrow();require(d.alluvialPlan(construction[0],construction[2]).changed(),"Construction still lies on affected bench after reopen");
            }else{piece=null;clear=Map.of();report.put("savedClearScope","Affected wooded-bench replay is qualified on seed1; seed424242 qualifies terrain/connectivity/profile and a changed-ground edit only.");}
            var first=scenes.getFirst();BlockPos chosen=null;search:for(int z=first.z-24;z<=first.z+24;z++)for(int x=first.x-24;x<=first.x+24;x++)if(d.alluvialPlan(x,z).changed()){chosen=new BlockPos(x,d.sample(x,z).height()-2,z);break search;}require(chosen!=null,"Saved soil edit uses affected terrain");soilEdit=chosen;edited.add(chosen.asLong());
            var chunks=new LinkedHashSet<ChunkPos>();for(var s:scenes)for(int z=s.minZ>>4;z<=s.maxZ>>4;z++)for(int x=s.minX>>4;x<=s.maxX>>4;x++)chunks.add(new ChunkPos(x,z));
            for(long key:clear.keySet()){var p=BlockPos.of(key);chunks.add(new ChunkPos(p.getX()>>4,p.getZ()>>4));}chunks.add(new ChunkPos(soilEdit.getX()>>4,soilEdit.getZ()>>4));required=new ArrayList<>(chunks);
            Files.writeString(dir.resolve("preparation.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("seed",seed,"chunks",required.size(),"scenes",scenes,"clearConstruction",piece==null?new int[0]:piece.createTag(context).getIntArray("Construction").orElseThrow(),"clearCells",clear.size()))+"\n");
            report.put("negativeControls",controls());
        }
        private void prepareScene(Scene s){
            var d=g.drainage(level.getChunkSource().randomState());var reference=d.alluvialReference();int changed=0,extended=0;
            for(int z=s.z-s.radius;z<=s.z+s.radius;z++)for(int x=s.x-s.radius;x<=s.x+s.radius;x++){
                var p=d.alluvialPlan(x,z);if(!p.changed())continue;changed++;if(reference.channelAt(x,z).edge()<0)extended++;
                var reach=reference.alluvialReachAt(x,z);double t=reference.closestT(reach.edge(),x,z);int cx=(int)Math.round(reference.edgeX(reach.edge(),t)),cz=(int)Math.round(reference.edgeZ(reach.edge(),t));
                s.minX=Math.min(s.minX,cx);s.maxX=Math.max(s.maxX,cx);s.minZ=Math.min(s.minZ,cz);s.maxZ=Math.max(s.maxZ,cz);
            }
            require(s.maxX-s.minX<=256&&s.maxZ-s.minZ<=256,"Native certificate footprint stays within explicit256m bound");
            if(!s.junction)require(changed>0&&extended>0,"Real morphology beyond historical channel query, not merely bank repainting");
            Blockbranch.LOGGER.info("ALLUVIAL_SCENE_PREP center={},{} changed={} extended={}",s.x,s.z,changed,extended);
        }
        private BlockPos woodedBench()throws Exception{
            var random=level.getChunkSource().randomState();var d=g.drainage(random);var candidates=new ArrayList<int[]>();for(var s:scenes)candidates.add(new int[]{s.x,s.z});
            var design=JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.alluvialDesignInput")))).getAsJsonObject();
            int appended=0;for(var r:design.getAsJsonArray("seeds"))if(r.getAsJsonObject().get("seed").getAsLong()==seed)for(var w:r.getAsJsonObject().getAsJsonArray("witnesses"))for(var change:w.getAsJsonObject().getAsJsonArray("changes")){var p=change.getAsJsonObject();int x=p.get("x").getAsInt(),z=p.get("z").getAsInt();if(Math.abs(z)<1000000&&appended++<2048)candidates.add(new int[]{x,z});}
            int probes=0;for(var center:candidates)for(int z=center[1]-8;z<=center[1]+8;z++)for(int x=center[0]-8;x<=center[0]+8;x++){
                probes++;var p=d.alluvialPlan(x,z);if(!p.changed())continue;int h=d.sample(x,z).height();var canopy=g.canopy(x,z,random);
                for(int y=h+1;y<=h+8;y++){var state=canopy.state(x,y,z);if(state!=null&&state.is(BlockTags.LOGS)&&state.hasProperty(RotatedPillarBlock.AXIS)&&state.getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Y){report.put("woodedBenchDiscovery",Map.of("probes",probes,"candidatePointBound",2051,"point",List.of(x,y,z)));return new BlockPos(x,y,z);}}
            }
            throw new AssertionError("No affected wooded bench inside fixed scenes plus2048 recorded changed central samples");
        }
        private Object controls()throws Exception{
            var d=g.drainage(level.getChunkSource().randomState());int steep=0,gorge=0,wet=0,ice=0;
            var sections=JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.alluvialSectionInput")))).getAsJsonObject();
            for(var r:sections.getAsJsonArray("seeds"))if(r.getAsJsonObject().get("seed").getAsLong()==seed)for(var w:r.getAsJsonObject().getAsJsonArray("sections"))for(var v:w.getAsJsonObject().getAsJsonArray("crossSection")){
                if(steep>=8&&gorge>=8)break;var p=v.getAsJsonObject();int x=p.get("x").getAsInt(),z=p.get("z").getAsInt();var reach=d.alluvialReference().alluvialReachAt(x,z);if(reach.edge()<0)continue;var old=d.alluvialReference().sample(x,z);
                if(reach.gradient()>=.003&&steep<8){require(!d.alluvialPlan(x,z).changed(),"Steep reach retained");steep++;}
                if(reach.gradient()<.003&&old.height()+1-reach.waterLevel()>14&&gorge<8){require(!d.alluvialPlan(x,z).changed(),"High confined valley wall retained");gorge++;}
            }
            for(var p:new int[][]{{-1552000,-1904000},{-1456000,-1904000},{304000,784000},{432000,-784000}}){var old=d.alluvialReference().sample(p[0],p[1]);if(old.waterLevel()!=DrainageNetwork.DRY){require(d.sample(p[0],p[1]).equals(old),"Original wet columns retained");wet++;}if(d.continentalIceThickness(p[0],p[1])>0){require(d.sample(p[0],p[1]).equals(old),"Grounded ice retained");ice++;}}
            for(var scene:scenes){var old=d.alluvialReference().sample(scene.x,scene.z);if(old.waterLevel()!=DrainageNetwork.DRY){require(d.sample(scene.x,scene.z).equals(old),"Actual scene/confluence water retained");wet++;}}
            require(steep>0&&gorge>0&&wet>0,"Nonvacuous terrain/water controls: steep="+steep+" walls="+gorge+" wet="+wet);
return Map.of("steep",steep,"highWalls",gorge,"wet",wet,"ice",ice);
        }
        boolean tick()throws Exception{
            if(System.nanoTime()-began>900_000_000_000L)throw new AssertionError("Bounded native qualification deadline");
            for(int n=0;n<8&&requested<required.size();n++,requested++){var p=required.get(requested);level.setChunkForced(p.x(),p.z(),true);pending.put(p,((blockbranch.mixin.cubic.PreviewChunkFutureAccessor)level.getChunkSource()).blockbranch$previewFuture(p.x(),p.z(),ChunkStatus.FULL,true));}
            if(requested<required.size()||pending.values().stream().anyMatch(f->!f.isDone()))return false;
            for(var e:pending.entrySet())require(e.getValue().join().isSuccess(),"Native full future "+e.getKey()+": "+e.getValue().join().getError());
            qualify();for(var p:required)level.setChunkForced(p.x(),p.z(),false);return true;
        }
        private void qualify()throws Exception{
            var random=level.getChunkSource().randomState();var d=g.drainage(random);var nativeHash=MessageDigest.getInstance("SHA-256");var profiles=MessageDigest.getInstance("SHA-256");var nativeWater=new HashMap<ChunkPos,int[]>();var seen=new HashSet<ChunkPos>();var rows=new ArrayList<Object>();int reads=0,columns=0,api=0;
            for(var s:scenes){int changed=0,core=0,extended=0,fines=0;int sizeX=s.maxX-s.minX+1,sizeZ=s.maxZ-s.minZ+1;int[] ground=new int[sizeX*sizeZ],water=new int[ground.length];var groups=new TreeSet<Integer>();
                for(int z=s.minZ;z<=s.maxZ;z++)for(int x=s.minX;x<=s.maxX;x++){
                    var chunk=level.getChunk(x>>4,z>>4);var profile=((SparseColumn)chunk).blockbranch$terrain().base();int idx=(z-s.minZ)*sizeX+x-s.minX;ground[idx]=profile.groundHeight(x,z);water[idx]=nativeWater.computeIfAbsent(chunk.getPos(),key->profile.encode(level.palettedContainerFactory()).getIntArray("water").orElseThrow())[(z&15)*16+(x&15)];
                    require(ground[idx]==d.sample(x,z).height()&&water[idx]==d.sample(x,z).waterLevel(),"Native stored terrain agrees with realized geometry");
                    if(seen.add(chunk.getPos())){var tag=profile.encode(level.palettedContainerFactory());require(blockbranch.cubic.TerrainProfile.decode(tag,level.palettedContainerFactory()).encode(level.palettedContainerFactory()).equals(tag),"Exact native profile roundtrip");add(profiles,chunk.getPos()+":"+tag);}
                    var plan=d.alluvialPlan(x,z);if(x>=s.x-s.radius&&x<=s.x+s.radius&&z>=s.z-s.radius&&z<=s.z+s.radius){columns++;if(plan.changed()){changed++;groups.add(plan.water());if(plan.connectedCore())core++;if(d.alluvialReference().channelAt(x,z).edge()<0)extended++;if(g.riverMarginCover(x,z,random)>=13)fines++;}
                        int h=ground[idx];for(int y=h-3;y<=h+40;y++){var pos=new BlockPos(x,y,z);if(edited.contains(pos.asLong()))continue;var state=level.getBlockState(pos);require(state.equals(g.surfaceState(x,y,z,random)),"Native/base surface mismatch "+pos);add(nativeHash,pos+":"+state);reads++;}
                        if((x-s.x)%8==0&&(z-s.z)%8==0){api+=baseApis(x,z,h,chunk);}
                    }
                }
                int edgePairs=0,maxCutDifference=0,maxAddedRise=0;
                for(int z=0;z<sizeZ;z++)for(int x=0;x<sizeX;x++)for(var offset:new int[][]{{1,0},{0,1}}){
                    int nx=x+offset[0],nz=z+offset[1];if(nx>=sizeX||nz>=sizeZ)continue;
                    int i=z*sizeX+x,j=nz*sizeX+nx;
                    int original=d.alluvialReference().sample(s.minX+x,s.minZ+z).height(),neighbor=d.alluvialReference().sample(s.minX+nx,s.minZ+nz).height();
                    int cut=original-ground[i],otherCut=neighbor-ground[j];require(cut>=0&&cut<=8&&otherCut>=0&&otherCut<=8,"Native edge cut stays bounded and conservative");
                    int difference=Math.abs(cut-otherCut),added=Math.abs(ground[i]-ground[j])-Math.abs(original-neighbor);
                    require(difference<=1&&added<=1,"Bench filtering introduced a multi-block cliff at "+(s.minX+x)+","+(s.minZ+z));
                    maxCutDifference=Math.max(maxCutDifference,difference);maxAddedRise=Math.max(maxAddedRise,added);edgePairs++;
                }
                var stageRows=new ArrayList<Object>();
                for(int ordinary:groups)for(int rise:new int[]{3,4,5,6}){
                    boolean[] reached=flood(ground,water,sizeX,sizeZ,ordinary,rise);int claimed=0,reachable=0;
                    for(int z=s.z-s.radius;z<=s.z+s.radius;z++)for(int x=s.x-s.radius;x<=s.x+s.radius;x++){var p=d.alluvialPlan(x,z);if(!p.changed()||p.water()!=ordinary)continue;int idx=(z-s.minZ)*sizeX+x-s.minX;if(reached[idx])reachable++;if(d.alluvialPotential(x,z,rise)){require(reached[idx],"Certified core is not face-connected in native terrain at positive depth: "+x+","+z+" rise"+rise);claimed++;}}
                    stageRows.add(Map.of("ordinaryWater",ordinary,"rise",rise,"certified",claimed,"nativeReachableModified",reachable));
                }
                if(!s.junction)require(changed>0&&core>0&&extended>0,"Nonvacuous native bench and extended core");
                var sceneRow=new LinkedHashMap<String,Object>();sceneRow.put("center",List.of(s.x,s.z));sceneRow.put("nativeBounds",List.of(s.minX,s.maxX,s.minZ,s.maxZ));sceneRow.put("junction",s.junction);sceneRow.put("changed",changed);sceneRow.put("certifiedCore4",core);sceneRow.put("extended",extended);sceneRow.put("fineSediment",fines);sceneRow.put("stageSensitivity",stageRows);sceneRow.put("edgePairs",edgePairs);sceneRow.put("maxAdjacentCutDifference",maxCutDifference);sceneRow.put("maxAddedFaceRise",maxAddedRise);rows.add(sceneRow);
            }
            String stateHash=HexFormat.of().formatHex(nativeHash.digest()),profileHash=HexFormat.of().formatHex(profiles.digest());
            if(reopen){var prior=JsonParser.parseString(Files.readString(dir.resolve("result.txt.json"))).getAsJsonObject();require(prior.get("naturalNativeSHA256").getAsString().equals(stateHash)&&prior.get("profileSHA256").getAsString().equals(profileHash),"Natural states and profiles survive reopen");for(var entry:JsonParser.parseString(Files.readString(dir.resolve("edits.json"))).getAsJsonArray()){var e=entry.getAsJsonObject();require(level.getBlockState(BlockPos.of(e.get("position").getAsLong())).toString().equals(e.get("state").getAsString()),"Saved native clear/player edit lost");}if(!clear.isEmpty())require(Files.readString(dir.resolve("clear-footprint.txt")).equals(new TreeMap<>(clear).toString()),"Exact saved clearing footprint");}
            else{
                if(!clear.isEmpty()){Files.writeString(dir.resolve("clear-footprint.txt"),new TreeMap<>(clear).toString());var initial=new HashMap<Long,net.minecraft.world.level.block.state.BlockState>();for(long key:clear.keySet())initial.put(key,level.getBlockState(BlockPos.of(key)));
                long player=initial.entrySet().stream().filter(e->e.getValue().is(BlockTags.LOGS)).mapToLong(Map.Entry::getKey).findFirst().orElseThrow();initial.put(player,Blocks.GOLD_BLOCK.defaultBlockState());level.setBlock(BlockPos.of(player),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                var tag=piece.createTag(context);var forward=BorealClearSmoke.replay(level,g,context,tag,initial,false,true,dir);var reverse=BorealClearSmoke.replay(level,g,context,tag,initial,true,true,dir);require(forward.equals(reverse)&&forward.get(player).is(Blocks.GOLD_BLOCK),"Clipped replay order/non-tree edit retained");var expected=BorealClearSmoke.replay(level,g,context,tag,initial,false,true,dir,true);for(long key:clear.keySet())require(level.getBlockState(BlockPos.of(key)).equals(expected.get(key)),"Actual native clipped clear matches immutable plan");
                }
                level.setBlock(soilEdit,Blocks.GOLD_BLOCK.defaultBlockState(),3);var edits=new JsonArray();for(long key:edited){var state=level.getBlockState(BlockPos.of(key));var e=new JsonObject();e.addProperty("position",key);e.addProperty("state",state.toString());edits.add(e);}Files.writeString(dir.resolve("edits.json"),edits.toString()+"\n");
            }
            level.getServer().saveEverything(false,true,true);report.put("scenes",rows);report.put("naturalNativeSHA256",stateHash);report.put("profileSHA256",profileHash);report.put("nativeReads",reads);report.put("targetColumns",columns);report.put("baseApiReads",api);report.put("nativeProfileCount",seen.size());report.put("savedClearCells",clear.size());report.put("scope","Finite native same-datum terrain reachability at prescribed stages; conservative radial certificates, not hydroperiod, flood frequency, biological ecology, dynamic flooding or Earth area.");
            Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(result,"PASS native alluvial geometry/connectivity/APIs/profiles/clearing/edits\n");Blockbranch.LOGGER.info("ALLUVIAL_NATIVE_PASS seed={} reads={} profiles={}",seed,reads,seen.size());
        }
        private int baseApis(int x,int z,int h,LevelChunk chunk){
            var random=level.getChunkSource().randomState();int reads=0;var bounds=net.minecraft.world.level.LevelHeightAccessor.create(h-3,12);var column=g.getBaseColumn(x,z,bounds,random);
            for(int y=h-3;y<=h+8;y++){if(!edited.contains(new BlockPos(x,y,z).asLong()))require(column.getBlock(y).equals(level.getBlockState(new BlockPos(x,y,z))),"Clipped base column/native parity");reads++;}
            boolean columnEdited=edited.stream().map(BlockPos::of).anyMatch(p->p.getX()==x&&p.getZ()==z);if(!columnEdited)for(var type:net.minecraft.world.level.levelgen.Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(level.getBlockState(new BlockPos(x,y,z)))){expected=y+1;break;}require(g.getBaseHeight(x,z,type,bounds,random)==expected,"Clipped native base height");require(g.getBaseHeight(x,z,type,level,random)==chunk.getHeight(type,x&15,z&15)+1,"Full native base height");reads+=2;}return reads;
        }
    }
    private static boolean[] flood(int[] ground,int[] water,int width,int height,int ordinary,int rise){
        boolean[] seen=new boolean[ground.length];int[] queue=new int[ground.length];int head=0,tail=0;for(int i=0;i<ground.length;i++)if(water[i]==ordinary&&ground[i]+1<ordinary){seen[i]=true;queue[tail++]=i;}
        while(head<tail){int i=queue[head++],x=i%width,z=i/width;for(var o:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+o[0],nz=z+o[1];if(nx<0||nx>=width||nz<0||nz>=height)continue;int n=nz*width+nx;if(!seen[n]&&ground[n]+1<ordinary+rise&&(water[n]==DrainageNetwork.DRY||water[n]==ordinary)){seen[n]=true;queue[tail++]=n;}}}return seen;
    }
    private static void reject(RegistryOps<JsonElement> ops,JsonObject json){require(RealisticGenerator.CODEC.codec().parse(ops,json).error().isPresent(),"Incomplete/malformed/future generator rejected");}
    private static void add(MessageDigest hash,String text){hash.update((text+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void fail(net.minecraft.server.MinecraftServer server,Path result,Throwable e){Blockbranch.LOGGER.error("ALLUVIAL_NATIVE_FAIL",e);try{Files.writeString(result,"FAIL "+e+"\n");}catch(Exception ignored){}server.halt(false);}
    private AlluvialQualification(){}
}
