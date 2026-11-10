package blockbranch.client.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import blockbranch.realistic.*;

/** Connected views of the actual saved native cores and approved additions; no visual stand-ins. */
public final class ReimaginedRemainingView {
 private static boolean opened,saving,finished;
 private static volatile boolean posed;
 private static volatile int view;
 private static int settled,mechanismTick,leverStage,initialArrows;
 private static long began;
 private static volatile BlockPos anchor;
 private static StructureStart jungle;
 private static List<BlockPos> levers,wires;
 private static List<DispenserBlockEntity> dispensers;
 private static boolean pistonChanged,tripwireTriggered;
 private static Set<BlockPos> initialPistons;
 private static final List<Object> records=new ArrayList<>();
 private static Path dir(){return Path.of(System.getProperty("blockbranch.reimaginedRemainingViewDir"));}
 public static void register(){
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.isDedicatedServer()||server.getPlayerList().getPlayers().isEmpty()||finished)return;try{
   var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
   if(view==6){finish("PASS six native views and original jungle lever/piston/tripwire/dispenser interaction");return;}
   if(view==2&&leverStage<10){
    if(levers==null){levers=new ArrayList<>();wires=new ArrayList<>();dispensers=new ArrayList<>();var core=jungle.getPieces().stream().filter(p->p instanceof JungleTemplePiece).findFirst().orElseThrow().getBoundingBox();for(var p:BlockPos.betweenClosed(core.minX(),core.minY()-5,core.minZ(),core.maxX(),core.maxY(),core.maxZ())){var s=level.getBlockState(p);if(s.is(Blocks.LEVER))levers.add(p.immutable());if(s.is(Blocks.TRIPWIRE))wires.add(p.immutable());if(level.getBlockEntity(p) instanceof DispenserBlockEntity d){d.unpackLootTable(null);dispensers.add(d);}}levers.sort(Comparator.comparingLong(BlockPos::asLong));if(levers.size()!=3||dispensers.size()!=2||wires.isEmpty())throw new AssertionError("Original jungle mechanism witnesses");initialPistons=pistonHeads(level,core);initialArrows=arrows();}
    if(++mechanismTick%15!=0)return;
    var core=jungle.getPieces().stream().filter(p->p instanceof JungleTemplePiece).findFirst().orElseThrow().getBoundingBox();pistonChanged|=!initialPistons.equals(pistonHeads(level,core));
    if(leverStage<8){int mask=leverStage^(leverStage>>1);for(int i=0;i<3;i++){var p=levers.get(i);boolean desired=(mask&(1<<i))!=0;if(level.getBlockState(p).getValue(LeverBlock.POWERED)!=desired){player.setPos(p.getX()+.5,p.getY(),p.getZ()+1.5);player.gameMode.useItemOn(player,level,net.minecraft.world.item.ItemStack.EMPTY,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),Direction.NORTH,p,false));if(level.getBlockState(p).getValue(LeverBlock.POWERED)!=desired)throw new AssertionError("Actual native lever use");}}leverStage++;return;}
    if(leverStage==8){var p=wires.getFirst();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+(p.getX()+.5)+" "+(p.getY()+.1)+" "+(p.getZ()+.5));player.getAbilities().flying=false;player.onUpdateAbilities();leverStage++;mechanismTick=0;return;}
    tripwireTriggered|=wires.stream().anyMatch(p->level.getBlockState(p).is(Blocks.TRIPWIRE)&&level.getBlockState(p).getValue(TripWireBlock.POWERED));if(mechanismTick<90)return;
    if(!pistonChanged||!tripwireTriggered||arrows()>=initialArrows)throw new AssertionError("Native mechanism response piston="+pistonChanged+" tripwire="+tripwireTriggered+" arrows="+initialArrows+" -> "+arrows());records.add(Map.of("nativeJungleInteraction",true,"pistonMoved",true,"tripwirePowered",true,"arrowsBefore",initialArrows,"arrowsAfter",arrows()));Files.writeString(dir().resolve("interaction.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(records));leverStage=10;player.getAbilities().flying=true;player.onUpdateAbilities();
   }
   if(posed)return;
   int kind=3+view/2;var g=(RealisticGenerator)level.getChunkSource().getGenerator();var plan=g.reimagined(level.getChunkSource().randomState()).ordered(kind,BlockPos.ZERO,6000000).stream().limit(12).map(ReimaginedSites.Region::plan).flatMap(Optional::stream).findFirst().orElseThrow();var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(ReimaginedStructures.key(kind));var start=level.structureManager().getStartForStructure(holder.value(),level.getChunk(plan.x()>>4,plan.z()>>4));if(start==null||!start.isValid())throw new AssertionError("Actual remaining start");var all=start.getBoundingBox();for(int z=all.minZ()>>4;z<=all.maxZ()>>4;z++)for(int x=all.minX()>>4;x<=all.maxX()>>4;x++)level.setChunkForced(x,z,true);
   var commands=server.getCommands();var source=server.createCommandSourceStack();commands.performPrefixedCommand(source,"time set 6000");commands.performPrefixedCommand(source,"weather clear");commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());commands.performPrefixedCommand(source,"effect give "+player.getName().getString()+" minecraft:night_vision 100000 0 true");player.getAbilities().flying=true;player.onUpdateAbilities();
   BlockPos camera,aim;
   if(kind==3){jungle=start;var piece=(ReimaginedLayoutPiece)start.getPieces().stream().filter(p->p instanceof ReimaginedLayoutPiece).findFirst().orElseThrow();camera=view%2==0?piece.local(-13,28,-12):piece.local(33,20,30);aim=view%2==0?piece.local(3,13,9):piece.local(17,14,16);}
   else if(kind==4){var piece=(ReimaginedMansionPiece)start.getPieces().stream().filter(p->p instanceof ReimaginedMansionPiece).findFirst().orElseThrow();var b=piece.getBoundingBox();camera=view%2==0?new BlockPos(b.minX()+1,b.minY()+41,b.minZ()+1):new BlockPos(b.maxX()-2,b.minY()+31,b.maxZ()-2);aim=new BlockPos((b.minX()+b.maxX())/2,b.minY()+21,(b.minZ()+b.maxZ())/2);}
   else{var piece=(ReimaginedSanctuaryPiece)start.getPieces().get(1);camera=view%2==0?piece.local(19,13,-21):piece.local(-20,22,-12);aim=view%2==0?piece.local(28,9,3):piece.local(-8,15,-7);}
   anchor=aim;double cx=camera.getX()+.5,cy=camera.getY()+.2,cz=camera.getZ()+.5,dx=aim.getX()+.5-cx,dz=aim.getZ()+.5-cz,dy=aim.getY()+.5-(cy+player.getEyeHeight());double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)));commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+cx+" "+cy+" "+cz+" "+yaw+" "+pitch);records.add(Map.of("kind",kind,"view",view,"camera",List.of(cx,cy,cz),"aim",aim.toShortString(),"nightVision",true));blockbranch.Blockbranch.LOGGER.info("REIMAGINED_REMAINING_VIEW_POSED {}",view);posed=true;
  }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("REIMAGINED_REMAINING_VIEW_FAIL",t);try{finish("FAIL "+t);}catch(Exception ignored){}}});
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{if(opened&&System.nanoTime()-began>600_000_000_000L){finish("FAIL view timeout");client.stop();return;}if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;began=System.nanoTime();client.createWorldOpenFlows().openWorld("ReimaginedRemainingReview",()->client.gui.setScreen(new TitleScreen()));return;}if(Files.exists(dir().resolve("result.txt"))){client.stop();return;}if(!posed||client.level==null||client.player==null||saving)return;client.options.fov().set(75);if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();var area=client.levelRenderer.viewArea();if(area==null)return;var section=area.getRenderSectionAt(anchor);if(section==null||!section.getSectionMesh().hasRenderableLayers()){settled=0;return;}if(++settled<160)return;saving=true;String file=new String[]{"jungle","mansion","monument"}[view/2]+"-"+(view%2+1)+".png";net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{view++;posed=false;saving=false;settled=0;}));}catch(Throwable t){try{finish("FAIL "+t);}catch(Exception ignored){}client.stop();}});
 }
 private static Set<BlockPos> pistonHeads(net.minecraft.server.level.ServerLevel l,BoundingBox b){var result=new HashSet<BlockPos>();for(var p:BlockPos.betweenClosed(b.minX(),b.minY()-5,b.minZ(),b.maxX(),b.maxY(),b.maxZ()))if(l.getBlockState(p).is(Blocks.PISTON_HEAD)||l.getBlockState(p).is(Blocks.MOVING_PISTON))result.add(p.immutable());return result;}
 private static int arrows(){return dispensers.stream().mapToInt(d->java.util.stream.IntStream.range(0,d.getContainerSize()).map(i->d.getItem(i).getCount()).sum()).sum();}
 private static void finish(String outcome)throws Exception{finished=true;Files.writeString(dir().resolve("views.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(records));Files.writeString(dir().resolve("result.txt"),outcome);}
}
