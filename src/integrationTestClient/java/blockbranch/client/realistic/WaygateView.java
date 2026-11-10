package blockbranch.client.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import blockbranch.realistic.DesertWaygates;
/** Two native views of an unrepaired, naturally generated site in a disposable copied save. */
public final class WaygateView {
 private static volatile boolean posed,rearRequested,rearPosed;
 private static volatile String failure;
 private static boolean opened,saving;
 private static int settled,capture,diagnosticTicks,lastContext;
 private static long began;
 private static DesertWaygates.Plan target;
 private static void pose(net.minecraft.server.MinecraftServer server,net.minecraft.server.level.ServerPlayer player,boolean rear){
  int lx=rear?-12:10,lz=rear?15:-14;
  double x=target.x()+DesertWaygates.rotateX(lx,lz,target.rotation())+.5,z=target.z()+DesertWaygates.rotateZ(lx,lz,target.rotation())+.5,y=target.y()+10,dx=target.x()+.5-x,dz=target.z()+.5-z,dy=target.y()+3.5-y;
  server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+x+" "+y+" "+z+" "+Math.toDegrees(Math.atan2(-dx,dz))+" "+Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz))));
 }
 public static void register(){
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(server.isDedicatedServer()||failure!=null||server.getPlayerList().getPlayers().isEmpty())return;
   try{
    var player=server.getPlayerList().getPlayers().getFirst();
    if(rearRequested){pose(server,player,true);rearRequested=false;rearPosed=true;return;}
    if(posed)return;
    target=new com.google.gson.Gson().fromJson(Files.readString(Path.of(System.getProperty("blockbranch.waygateMarker"))),DesertWaygates.Plan.class);
    var level=server.overworld();var commands=server.getCommands();var source=server.createCommandSourceStack();commands.performPrefixedCommand(source,"difficulty peaceful");commands.performPrefixedCommand(source,"time set 6000");commands.performPrefixedCommand(source,"weather clear");commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());
    for(int z=(target.z()-6)>>4;z<=(target.z()+6)>>4;z++)for(int x=(target.x()-6)>>4;x<=(target.x()+6)>>4;x++)level.getChunk(x,z);
    double ax=target.x()+DesertWaygates.rotateX(0,-7,target.rotation())+.5,az=target.z()+DesertWaygates.rotateZ(0,-7,target.rotation())+.5,floor=Double.NaN;
    for(int y=target.y()+5;y>=target.y()-6;y--){var pos=new BlockPos((int)Math.floor(ax),y,(int)Math.floor(az));var shape=level.getBlockState(pos).getCollisionShape(level,pos);if(!shape.isEmpty()){floor=y+shape.max(Direction.Axis.Y);break;}}
    if(!Double.isFinite(floor))throw new AssertionError("Native approach surface");player.setPos(ax,floor,az);player.getAbilities().flying=false;player.onUpdateAbilities();
    for(int i=0;i<16;i++)player.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(DesertWaygates.rotateX(0,1,target.rotation())*.25,-.08,DesertWaygates.rotateZ(0,1,target.rotation())*.25));
    if(Math.hypot(player.getX()-ax,player.getZ()-az)<3.7)throw new AssertionError("Native collision walks forecourt entrance");
    var chest=target.local(3,3,4);player.setPos(chest.getX()+1.5,chest.getY(),chest.getZ()+1.5);player.gameMode.useItemOn(player,level,player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chest),Direction.UP,chest,false));
    if(player.containerMenu==player.inventoryMenu)throw new AssertionError("Native chest opens");player.closeContainer();player.getAbilities().flying=true;player.onUpdateAbilities();pose(server,player,false);posed=true;
   }catch(Throwable t){failure=t.toString();blockbranch.Blockbranch.LOGGER.error("WAYGATE_VIEW_FAIL",t);}
  });
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{
   if(failure!=null||opened&&System.nanoTime()-began>600_000_000_000L){Files.writeString(client.gameDirectory.toPath().resolve("result.txt"),"FAIL "+failure+" capture="+capture+" context="+lastContext);client.stop();return;}
   if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;began=System.nanoTime();client.createWorldOpenFlows().openWorld("WaygateReview",()->client.gui.setScreen(new TitleScreen()));return;}
   if(!posed||capture==1&&!rearPosed||client.level==null||client.player==null||saving)return;
   client.options.fov().set(50);if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();
   int context=0;for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)if(client.level.getChunkSource().hasChunk(Math.floorDiv(target.x()+x*16,16),Math.floorDiv(target.z()+z*16,16)))context++;
   lastContext=context;
   boolean blocks=client.level.getBlockState(target.local(5,3,7)).is(Blocks.OBSIDIAN)&&client.level.getBlockState(target.local(3,3,4)).is(Blocks.CHEST),rendered=client.levelRenderer.hasRenderedAllSections();
   if(++diagnosticTicks%200==0)blockbranch.Blockbranch.LOGGER.info("WAYGATE_CAPTURE variant={} angle={} contextChunks={} blocks={} rendered={} queue={}",target.variant(),capture,context,blocks,rendered,client.levelRenderer.sectionRenderDispatcher().getStats());
   if(context<25||!blocks||!rendered){settled=0;return;}if(++settled<160)return;saving=true;
   net.minecraft.client.Screenshot.grab(client.gameDirectory,"desert-waygate-"+target.variant()+(capture==0?"-front.png":"-rear.png"),client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{
    if(capture==0){capture=1;settled=0;saving=false;rearRequested=true;return;}
    Files.writeString(client.gameDirectory.toPath().resolve("result.txt"),"PASS two native unrepaired views; collision entrance walk; chest opened; frame/chest data, 25 context chunks and native renderer completion");client.stop();
   }catch(Exception e){throw new RuntimeException(e);}}));
  }catch(Exception e){throw new RuntimeException(e);}});
 }
}
