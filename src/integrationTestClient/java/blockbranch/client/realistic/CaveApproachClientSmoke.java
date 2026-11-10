package blockbranch.client.realistic;

import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;

/** Actual normal automatic rendering of a copied native entrance world; no mesh intervention. */
public final class CaveApproachClientSmoke {
    private static final int[] POINTS=Arrays.stream(System.getProperty("blockbranch.caveApproachPoints").split(",")).mapToInt(Integer::parseInt).toArray();
    private static final int[] ENTRY=System.getProperty("blockbranch.caveEntryPoints","").isEmpty()?new int[0]:Arrays.stream(System.getProperty("blockbranch.caveEntryPoints").split(",")).mapToInt(Integer::parseInt).toArray();
    private static final boolean DRIPSTONE_REVIEW=Boolean.getBoolean("blockbranch.dripstoneReview");
    private static final int[] TARGETS=Arrays.stream(System.getProperty("blockbranch.dripstoneTargets","0,0,0").split(",")).mapToInt(Integer::parseInt).toArray();
    private static final CaveApproach APPROACH=DRIPSTONE_REVIEW?CaveApproach.NONE:new CaveApproach(POINTS,new CaveEntryRoute(ENTRY));
    private static final int LAST=ENTRY.length==0?3:4;
    private static volatile int request=-1,applied=-2;
    private static volatile BlockPos camera;
    private static volatile blockbranch.cubic.TerrainProfile serverProfile;
    private static final boolean LIT_REVIEW=Boolean.getBoolean("blockbranch.litCaveReview");
    private static volatile Map<BlockPos,net.minecraft.world.level.block.state.BlockState> mossStates=Map.of();
    private static final boolean ORE_REVIEW=Boolean.getBoolean("blockbranch.oreReview");
    private static final blockbranch.client.qa.BaselineRetentionAudit RETENTION=Boolean.getBoolean("blockbranch.baselineRetentionAudit")?new blockbranch.client.qa.BaselineRetentionAudit(Path.of(System.getProperty("blockbranch.caveApproachClientResult")).resolveSibling("baseline-retention.json")):null;
    private static volatile Throwable failure;
    private static boolean opened,busy,done;
    private static int stage,ticks,good;
    private static long stageStart,settleStart;
    private static int settleChecks;
    private static final List<Map<String,Object>> observations=new ArrayList<>();
    public static void register(){
        long deadline=System.nanoTime()+(RETENTION==null?300_000_000_000L:900_000_000_000L);
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(done||server.isDedicatedServer()||request<0||applied==request||failure!=null)return;
            try{
                var level=server.overworld();var players=server.getPlayerList().getPlayers();if(players.isEmpty())return;var player=players.getFirst();
                require(level.getSeed()==Long.getLong("blockbranch.caveApproachSeed",424242L),"Copied native world seed matches review fixture");
                var g=(RealisticGenerator)level.getChunkSource().getGenerator();require(g.approachesEnabled(),"Actual saved generator opted into approaches");
                if(request==0&&!DRIPSTONE_REVIEW)CaveApproachSmoke.restoreReviewWitness(level,g,APPROACH);
                int i=Math.min(request,3)*3;camera=request==4?new BlockPos(ENTRY[ENTRY.length-3],ENTRY[ENTRY.length-2],ENTRY[ENTRY.length-1]):new BlockPos(POINTS[i],POINTS[i+1],POINTS[i+2]);
                require(level.getBlockState(camera).isAir()&&level.getBlockState(camera.above()).isAir(),"Actual camera clear");
                int next=request==3?6:i+3;double dx=request==4?ENTRY[ENTRY.length-3]-ENTRY[ENTRY.length-6]:POINTS[next]-POINTS[i],dz=request==4?ENTRY[ENTRY.length-1]-ENTRY[ENTRY.length-4]:POINTS[next+2]-POINTS[i+2];
                if(DRIPSTONE_REVIEW){dx=TARGETS[i]-camera.getX();dz=TARGETS[i+2]-camera.getZ();}
                double yaw=Math.toDegrees(Math.atan2(-dx,dz));int pitch=DRIPSTONE_REVIEW?(int)Math.toDegrees(Math.atan2(camera.getY()+1.62-TARGETS[i+1]-.5,Math.hypot(dx,dz))):request==0?35:request==3?-15:18;
                var source=server.createCommandSourceStack();String name=player.getName().getString();
                server.getCommands().performPrefixedCommand(source,"time set 6000");
                server.getCommands().performPrefixedCommand(source,"gamemode creative "+name);player.getAbilities().flying=true;player.onUpdateAbilities();
                server.getCommands().performPrefixedCommand(source,"tp "+name+" "+(camera.getX()+.5)+" "+camera.getY()+" "+(camera.getZ()+.5)+" "+yaw+" "+pitch);
                serverProfile=((SparseColumn)level.getChunkAt(camera)).blockbranch$terrain().base();
                if(LIT_REVIEW&&request==0){var states=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();for(int mz=-1;mz<=1;mz++)for(int mx=-1;mx<=1;mx++){var c=level.getChunk((camera.getX()>>4)+mx,(camera.getZ()>>4)+mz);var p=((SparseColumn)c).blockbranch$terrain().base();require(p.litCaves().enabled(),"Saved generator retains growth opt-in");var pairs=p.litCaves().placements();for(int n=0;n<pairs.length;n+=2){var pos=new BlockPos(c.getPos().getMinBlockX()+(pairs[n]&15),pairs[n+1],c.getPos().getMinBlockZ()+(pairs[n]>>4));states.put(pos,level.getBlockState(pos));}}require(states.size()>=2,"Actual moss snapshot witnesses");mossStates=Map.copyOf(states);}

                server.getCommands().performPrefixedCommand(source,request==0&&!DRIPSTONE_REVIEW?"effect clear "+name+" minecraft:night_vision":"effect give "+name+" minecraft:night_vision 999999 0 true");applied=request;
            }catch(Throwable error){failure=error;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(done)return;
            try{
                require(System.nanoTime()<deadline,"Bounded approach review startup timeout");
                if(failure!=null)throw new AssertionError("Approach render server",failure);
                if(RETENTION!=null&&RETENTION.waiting()){
                    if(RETENTION.poll(client)){
                        if(RETENTION.completed()==blockbranch.client.qa.BaselineRetentionAudit.TARGET){Files.writeString(Path.of(System.getProperty("blockbranch.caveApproachClientResult")),"PASS automatic cave rendering and baseline retention cycles\n");done=true;client.stop();return;}
                        opened=false;busy=false;stage=ticks=good=0;settleStart=0;settleChecks=0;request=-1;applied=-2;camera=null;serverProfile=null;observations.clear();
                    }
                    return;
                }
                var screen=client.gui.screen();String screenName=screen==null?"":screen.getClass().getSimpleName();
                if(screenName.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
                if(!opened&&screen instanceof TitleScreen){opened=true;require(Files.exists(client.gameDirectory.toPath().resolve("saves/CaveApproachReview/level.dat")),"Isolated copied world exists");client.createWorldOpenFlows().openWorld("CaveApproachReview",()->client.gui.setScreen(new TitleScreen()));return;}
                if(screenName.equals("ConfirmExperimentalFeaturesScreen")||screenName.equals("BackupConfirmScreen")){press(screen,"Proceed");return;}
                if(client.level==null||client.player==null)return;
                if(request<0){stageStart=System.nanoTime();request=0;return;}
                CaveApproachTimeoutSnapshot.observe(client,camera,stage,applied,request,busy);
                if(System.nanoTime()-stageStart>=90_000_000_000L){CaveApproachTimeoutSnapshot.write(client,camera,stage,applied,request,busy);throw new AssertionError("Bounded approach capture timeout stage="+stage);}
                if(busy||camera==null||applied!=stage||client.player.position().distanceToSqr(camera.getX()+.5,camera.getY(),camera.getZ()+.5)>.02)return;
                ticks++;
                var terrain=((SparseColumn)client.level.getChunkAt(camera)).blockbranch$terrain();
                if(terrain==null||(DRIPSTONE_REVIEW?!terrain.base().dripstone().enabled():!terrain.base().underground().hasApproach())){
                    if(ticks%100==0)blockbranch.Blockbranch.LOGGER.info("CAVE_APPROACH_PROFILE_WAIT stage={} camera={} received={}",stage,camera,terrain!=null);
                    return;
                }
                if(ENTRY.length>0)require(terrain.base().underground().encode().getIntOr("version",0)==3,"Client receives versioned entry route");
                if(ticks%10!=0)return;
                if(DRIPSTONE_REVIEW){for(int qz=0;qz<4;qz++)for(int qx=0;qx<4;qx++)for(int depth=serverProfile.caveBiomes().enabled()?-16:48;depth<=400;depth+=serverProfile.caveBiomes().enabled()?4:16){int qy=Math.floorDiv(serverProfile.groundHeight(qx*4,qz*4)-depth,4);require(terrain.base().biome(qx,qy,qz).equals(serverProfile.biome(qx,qy,qz)),"Actual deep client/server biome snapshot agreement");}}
                if(ORE_REVIEW){require(terrain.base().ores().enabled()&&serverProfile.ores().enabled(),"Client and server receive opted-in ore definition");int matches=0;for(int z=0;z<16;z+=3)for(int x=0;x<16;x+=3)for(int depth=4;depth<=224;depth+=3){int y=serverProfile.groundHeight(x,z)-depth;require(terrain.base().state(x,y,z)==serverProfile.state(x,y,z),"Actual client/server ore snapshot agreement");matches++;}}
                if(LIT_REVIEW){require(terrain.base().litCaves().enabled(),"Client growth descriptor received");if(stage==0){boolean ready=true;for(var pos:mossStates.keySet())if(((SparseColumn)client.level.getChunkAt(pos)).blockbranch$terrain()==null)ready=false;if(!ready)return;for(var e:mossStates.entrySet())require(client.level.getBlockState(e.getKey())==e.getValue(),"Actual client/server moss, mined air and backfill agree");}}
                var audit=UndergroundAutomaticRenderSmoke.audit(client);
                if(ticks%100==0)blockbranch.Blockbranch.LOGGER.info("CAVE_APPROACH_RENDER_WAIT stage={} audit={}",stage,audit);
                if(((Number)audit.get("missing")).intValue()!=0||((Number)audit.get("hits")).intValue()<40){good=0;settleStart=0;settleChecks=0;return;}
                if(++good<2)return;
                int settlingSeconds=Integer.getInteger("blockbranch.settlingSeconds",0);
                if(stage==LAST&&settlingSeconds>0){
                    if(settleStart==0){settleStart=System.nanoTime();blockbranch.Blockbranch.LOGGER.info("CAVE_STATIONARY_SETTLE_BEGIN seconds={}",settlingSeconds);}
                    settleChecks++;if(System.nanoTime()-settleStart<settlingSeconds*1_000_000_000L)return;
                }
                if(stage==0&&Boolean.getBoolean("blockbranch.packetOccupancySmoke")){
                    blockbranch.client.cubic.PacketOccupancySmoke.verify(client,camera);
                    Path result=Path.of(System.getProperty("blockbranch.caveApproachClientResult"));Files.writeString(result,"PASS native terrain-window receiver replay\n");done=true;client.disconnect(new TitleScreen(),false);client.stop();return;
                }
                if(stage==0&&Boolean.getBoolean("blockbranch.poseDiagnosticSmoke"))CaveApproachTimeoutSnapshot.verify(client,camera,stage);
                if(stage==0&&RETENTION!=null)RETENTION.sample(client,terrain.base(),serverProfile);
                var item=new LinkedHashMap<String,Object>();item.put("stage",stage);if(stage==LAST&&settleStart!=0){item.put("stationarySettlingNanos",System.nanoTime()-settleStart);item.put("stationaryVisibilityChecks",settleChecks);}item.put("pixelsCaptured",!Boolean.getBoolean("blockbranch.meshOnlySmoke"));item.put("litCaveSnapshotAgreement",LIT_REVIEW);item.put("mossAndEditWitnesses",mossStates.size());item.put("dripstoneSnapshotAgreement",DRIPSTONE_REVIEW);item.put("oreSnapshotAgreement",ORE_REVIEW);item.put("camera",camera.toShortString());if(DRIPSTONE_REVIEW){int ti=Math.min(stage,3)*3;item.put("targetBiome",client.level.getNoiseBiome(TARGETS[ti]>>2,TARGETS[ti+1]>>2,TARGETS[ti+2]>>2).unwrapKey().orElseThrow().identifier().toString());item.put("caveBiomeSnapshotAgreement",terrain.base().caveBiomes().enabled()==serverProfile.caveBiomes().enabled());}item.put("nightVisionDiagnostic",DRIPSTONE_REVIEW||stage>0);item.put("entryRouteEndpoint",stage==4);item.put("sky",client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,camera));item.putAll(audit);observations.add(item);
                busy=true;Runnable captured=()->{
                    try{
                        if(stage==(RETENTION==null||Boolean.getBoolean("blockbranch.baselineFullViews")?LAST:0)){
                            var report=Map.of("renderDistance",client.options.getEffectiveRenderDistance(),"explicitMeshSubmissions",0,"renderCacheMutations",0,"observations",observations);
                            Path result=Path.of(System.getProperty("blockbranch.caveApproachClientResult"));Files.writeString(result.resolveSibling("render-evidence.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                            blockbranch.client.cubic.ProceduralSectionCopiesSmoke.beforeDisconnect();
                            if(RETENTION!=null){serverProfile=null;camera=null;RETENTION.beginClose();client.disconnect(new TitleScreen(),false);}
                            else{client.disconnect(new TitleScreen(),false);Files.writeString(result,"PASS actual automatic native entrance rendering\n");done=true;client.stop();}
                        }else{stage++;ticks=0;good=0;busy=false;stageStart=System.nanoTime();request=stage;}
                    }catch(Throwable error){fail(client,error);}
                };
                if(Boolean.getBoolean("blockbranch.meshOnlySmoke"))captured.run();
                else net.minecraft.client.Screenshot.grab(client.gameDirectory,"approach-stage-"+stage+".png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(captured));
            }catch(Throwable error){fail(client,error);}
        });
    }
    private static void fail(net.minecraft.client.Minecraft client,Throwable error){done=true;blockbranch.Blockbranch.LOGGER.error("CAVE_APPROACH_CLIENT_FAIL",error);try{Files.writeString(Path.of(System.getProperty("blockbranch.caveApproachClientResult")),"FAIL "+error+"\n");}catch(Exception ignored){}client.stop();}
    private static boolean press(net.minecraft.client.gui.screens.Screen screen,String label){for(var child:screen.children())if(child instanceof Button button&&button.active&&button.getMessage().getString().equals(label)){button.onPress(new KeyEvent(257,0,0));return true;}return false;}
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
