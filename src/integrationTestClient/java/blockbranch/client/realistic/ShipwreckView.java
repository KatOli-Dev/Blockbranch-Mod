package blockbranch.client.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import blockbranch.realistic.ShipwreckDiscovery;

/** Native generated wreck, no review excavation or structure replacement. */
public final class ShipwreckView {
 private static final boolean ALL_VIEWS=Boolean.getBoolean("blockbranch.shipwreckAllViews");private static volatile int viewIndex;
 private static boolean rear(){return ALL_VIEWS?viewIndex==1:Boolean.getBoolean("blockbranch.shipwreckRearView");}
 private static boolean stairs(){return ALL_VIEWS?viewIndex==2:Boolean.getBoolean("blockbranch.shipwreckStairView");}
 private static volatile boolean posed;private static boolean opened,saving;private static int settled;private static long began;private static ShipwreckDiscovery.Plan target;
 public static void register(){
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.isDedicatedServer()||posed||server.getPlayerList().getPlayers().isEmpty())return;try{
   var q=Files.readString(Path.of(System.getProperty("blockbranch.shipwreckMarker"))).trim().split(",");target=new ShipwreckDiscovery.Plan(Integer.parseInt(q[0]),Integer.parseInt(q[1]),Integer.parseInt(q[2]),Integer.parseInt(q[3]),Integer.parseInt(q[4]),0);
   var player=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();var commands=server.getCommands();var source=server.createCommandSourceStack();commands.performPrefixedCommand(source,"time set 6000");commands.performPrefixedCommand(source,"weather clear");commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());player.getAbilities().flying=true;player.onUpdateAbilities();
   if(ALL_VIEWS&&viewIndex==3){
    var generator=(blockbranch.realistic.RealisticGenerator)level.getChunkSource().getGenerator();int firstRotation=target.rotation();
    target=generator.shipwrecks(level.getChunkSource().randomState()).plans().stream().filter(p->p.rotation()!=firstRotation&&Math.abs(p.x())<1700000&&Math.abs(p.z())<1700000).findFirst().orElseThrow();
    int hx=target.rotation()%2==0?4:15,hz=target.rotation()%2==0?15:4;
    for(int cz=(target.z()-hz)>>4;cz<=(target.z()+hz)>>4;cz++)for(int cx=(target.x()-hx)>>4;cx<=(target.x()+hx)>>4;cx++)level.getChunk(cx,cz);
   }
   double ax=target.x()+ShipwreckDiscovery.rotateX(5,-3,target.rotation())+.5,az=target.z()+ShipwreckDiscovery.rotateZ(5,-3,target.rotation())+.5;
   double floor=Double.NaN;for(int y=target.y()+6;y>=target.y()-6;y--){var pos=new BlockPos((int)Math.floor(ax),y,(int)Math.floor(az));var shape=level.getBlockState(pos).getCollisionShape(level,pos);if(!shape.isEmpty()){floor=y+shape.max(Direction.Axis.Y);break;}}
   if(!Double.isFinite(floor))throw new AssertionError("Native solid approach surface exists");
   player.setPos(ax,floor,az);player.getAbilities().flying=false;player.onUpdateAbilities();
   for(int step=0;step<12;step++)player.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(ShipwreckDiscovery.rotateX(-1,0,target.rotation())*.25,-.08,ShipwreckDiscovery.rotateZ(-1,0,target.rotation())*.25));
   if(Math.hypot(player.getX()-ax,player.getZ()-az)<2.7||player.getY()<target.y()+2.9)throw new AssertionError("Native collision can walk up boarding steps into hold");
   player.getAbilities().flying=true;player.onUpdateAbilities();
   for(int[] local:new int[][]{{3,3,6},{3,7,25},{5,3,25}}){
    var chest=new BlockPos(target.x()+ShipwreckDiscovery.rotateX(local[0]-4,local[2]-15,target.rotation()),target.y()+local[1],target.z()+ShipwreckDiscovery.rotateZ(local[0]-4,local[2]-15,target.rotation()));
    player.setPos(chest.getX()+2.5,chest.getY()+1,chest.getZ()+.5);player.gameMode.useItemOn(player,level,player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chest),Direction.UP,chest,false));
    if(player.containerMenu==player.inventoryMenu)throw new AssertionError("Native shipwreck chest opens at "+chest);player.closeContainer();
   }
   int side=rear()?-1:1;double x=target.x()+ShipwreckDiscovery.rotateX(18*side,-18*side,target.rotation())+.5,z=target.z()+ShipwreckDiscovery.rotateZ(18*side,-18*side,target.rotation())+.5,y=target.y()+(side==1?24:19);
   double aimX=target.x()+.5,aimZ=target.z()+.5,aimY=target.y()+5;
   if(stairs()){
    x=target.x()+ShipwreckDiscovery.rotateX(2,1,target.rotation())+.5;z=target.z()+ShipwreckDiscovery.rotateZ(2,1,target.rotation())+.5;y=target.y()+6;
    aimX=target.x()+ShipwreckDiscovery.rotateX(0,4,target.rotation())+.5;aimZ=target.z()+ShipwreckDiscovery.rotateZ(0,4,target.rotation())+.5;aimY=target.y()+4.3;
   }
   double dx=aimX-x,dz=aimZ-z,dy=aimY-y;double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)));
   commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+x+" "+y+" "+z+" "+yaw+" "+pitch);posed=true;
  }catch(Exception e){throw new RuntimeException(e);}});
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{
   if(opened&&System.nanoTime()-began>600_000_000_000L){Files.writeString(client.gameDirectory.toPath().resolve("result.txt"),"FAIL native wreck view timeout");client.stop();return;}
   if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;began=System.nanoTime();client.createWorldOpenFlows().openWorld("ShipwreckReview",()->client.gui.setScreen(new TitleScreen()));return;}
   if(!posed||client.level==null||client.player==null||saving)return;
   client.options.fov().set(50);
   if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();var area=client.levelRenderer.viewArea();if(area==null)return;int hx=target.rotation()%2==0?4:15,hz=target.rotation()%2==0?15:4;
   for(int z=(target.z()-hz)>>4;z<=(target.z()+hz)>>4;z++)for(int x=(target.x()-hx)>>4;x<=(target.x()+hx)>>4;x++){var section=area.getRenderSectionAt(new BlockPos(x*16+8,target.y()+5,z*16+8));if(section==null||!section.getSectionMesh().hasRenderableLayers()){settled=0;return;}}
   int context=0;for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++){boolean visible=false;for(int y:new int[]{48,64,80}){var section=area.getRenderSectionAt(new BlockPos(target.x()+x*16,y,target.z()+z*16));visible|=section!=null&&section.getSectionMesh().hasRenderableLayers();}if(visible)context++;}
   if(context<20){settled=0;return;}
   if(++settled<160)return;saving=true;net.minecraft.client.Screenshot.grab(client.gameDirectory,ALL_VIEWS&&viewIndex==3?"coastal-shipwreck-alternate.png":stairs()?"coastal-shipwreck-stairs.png":rear()?"coastal-shipwreck-rear.png":"coastal-shipwreck.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{if(ALL_VIEWS&&viewIndex<3){viewIndex++;settled=0;saving=false;posed=false;return;}Files.writeString(client.gameDirectory.toPath().resolve("result.txt"),"PASS native generated wreck view; boarding steps walked with native collision; all three chests opened by connected player; all affected surface chunks plus 20 surrounding context columns rendered");client.stop();}catch(Exception e){throw new RuntimeException(e);}}));
  }catch(Exception e){throw new RuntimeException(e);}});
 }
}
