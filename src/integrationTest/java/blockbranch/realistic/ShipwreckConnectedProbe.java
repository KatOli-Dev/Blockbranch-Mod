package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

/** Native shipwreck loot map through a registered connection; normal inventory ticking owns drawing. */
public final class ShipwreckConnectedProbe {
 public static final boolean ON=Boolean.getBoolean("blockbranch.shipwreckConnected"),REOPEN=Boolean.getBoolean("blockbranch.shipwreckConnectedReopen");
 public static volatile String phase="startup",serverHash="";public static volatile Throwable failure;public static volatile int mapId=-1;public static volatile boolean sawMenu,sawMap,clientAtWreck,done;public static volatile long heartbeat=System.nanoTime();
 private static ServerPlayer player;private static ShipwreckDiscovery.Plan plan;private static byte[] initial;private static long ticks,phaseTick,columns,maxTick,maxUpdate,updates,beginTick;private static long[] peak=new long[6];
 private static Path dir(){return Path.of(System.getProperty("blockbranch.shipwreckConnectedDir"));}
 public static void note(ItemStack item,long nanos){if(ON&&item.get(DataComponents.MAP_ID)!=null&&item.get(DataComponents.MAP_ID).id()==mapId){maxUpdate=Math.max(maxUpdate,nanos);updates++;}}
 public static String hash(byte[] bytes)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
 public static void register(){
  ServerTickEvents.START_SERVER_TICK.register(server->{beginTick=System.nanoTime();});
  ServerLifecycleEvents.SERVER_STOPPED.register(server->{try{WellInteraction.require(Arrays.stream(ProgressiveMapDrawing.audit(server)).allMatch(n->n==0),"Scheduler released after shutdown");Files.writeString(dir().resolve(REOPEN?"reopen-cleanup.json":"cleanup.json"),"[0,0,0,0,0,0]");}catch(Exception e){throw new RuntimeException(e);}});
  ServerTickEvents.END_SERVER_TICK.register(server->{heartbeat=System.nanoTime();if(server.isDedicatedServer()||done)return;try{
   ticks++;if(server.getPlayerList().getPlayers().isEmpty())return;var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
   if(player==null){
    player=server.getPlayerList().getPlayers().getFirst();WellInteraction.require(g.shipwreckDefinition()==1&&g.discoveryDefinition()==1&&g.waygateDefinition()==1,"Native schema93 all definitions");
    plan=g.shipwrecks(level.getChunkSource().randomState()).plans().stream().filter(p->Math.abs(p.x())<1700000&&Math.abs(p.z())<1700000).findFirst().orElseThrow();
    var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());WellInteraction.require(RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject().get("shape_version").getAsInt()==93,"Actual schema93 world");
    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode creative "+player.getName().getString());server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
    if(REOPEN){
     var saved=com.google.gson.JsonParser.parseString(Files.readString(dir().resolve("server-results.json"))).getAsJsonObject();mapId=saved.get("mapId").getAsInt();var stack=player.getInventory().getItem(1);WellInteraction.require(stack.get(DataComponents.MAP_ID)!=null&&stack.get(DataComponents.MAP_ID).id()==mapId,"Actual player inventory map ID restored");
     var data=MapItem.getSavedData(stack,level);WellInteraction.require(hash(data.colors).equals(saved.get("finalHash").getAsString()),"Partial native pixels retained exactly");
     WellInteraction.require(level.getBlockState(plan.center().offset(0,2,0)).is(Blocks.WOOL.red()),"Saved hull edit retained");
     initial=data.colors.clone();player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.getInventory().setItem(1,ItemStack.EMPTY);phase="drawing";columns=ProgressiveMapDrawing.audit(server)[0];phaseTick=ticks;return;
    }
    var mapPos=local(3,7,25);chestPosition=mapPos;level.getChunk(mapPos.getX()>>4,mapPos.getZ()>>4);teleport(level,mapPos.getX()+2.5,mapPos.getY()+1,mapPos.getZ()+.5,0,30);player.getAbilities().flying=true;player.onUpdateAbilities();
    phase="arrive-wreck";phaseTick=ticks;return;
   }
   if(phase.equals("arrive-wreck")){
    if(!clientAtWreck||ticks-phaseTick<40)return;var mapPos=local(3,7,25);
    player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(mapPos),Direction.UP,mapPos,false));WellInteraction.require(player.containerMenu!=player.inventoryMenu,"Native shipwreck map chest opens");phase="chest";phaseTick=ticks;return;
   }
   if(phase.equals("chest")){
    if(!sawMenu)return;var chest=(ChestBlockEntity)level.getBlockEntity(local(3,7,25));ItemStack map=ItemStack.EMPTY;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).get(DataComponents.MAP_ID)!=null){map=chest.removeItem(i,1);break;}
    WellInteraction.require(!map.isEmpty(),"Native map compartment supplied actual buried-treasure loot map");chest.setChanged();player.closeContainer();var data=MapItem.getSavedData(map,level);mapId=map.get(DataComponents.MAP_ID).id();initial=data.colors.clone();
    WellInteraction.require(map.get(DataComponents.MAP_DECORATIONS)!=null,"Native loot item carries exploration decoration before its first inventory tick");
    // An unexplored edge of this actual loot map, away from the already generated wreck.
    double x=data.centerX+126.5,z=data.centerZ+.5;teleport(level,x,100,z,-90,35);player.getAbilities().flying=true;player.onUpdateAbilities();player.setItemInHand(InteractionHand.MAIN_HAND,map);
    level.setBlock(plan.center().offset(0,2,0),Blocks.WOOL.red().defaultBlockState(),3);columns=ProgressiveMapDrawing.audit(server)[0];phase="drawing";phaseTick=ticks;return;
   }
   if(phase.equals("drawing")){
    maxTick=Math.max(maxTick,System.nanoTime()-beginTick);var audit=ProgressiveMapDrawing.audit(server);for(int i=0;i<6;i++)peak[i]=Math.max(peak[i],audit[i]);WellInteraction.require(audit[1]<=64&&audit[2]<=18&&audit[3]<=1&&audit[4]<=1&&audit[5]<=1,"Native scheduler bounds");
    var data=MapItem.getSavedData(player.getMainHandItem(),level);serverHash=hash(data.colors);
    if(ticks-phaseTick>120&&audit[0]-columns>=16&&sawMap&&!serverHash.equals(hash(initial))){
     var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.SHIPWRECK_BEACHED);var gate=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.RUINED_PORTAL_DESERT);var treasure=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.BURIED_TREASURE);
     WellInteraction.require(g.findNearestMapStructure(level,HolderSet.direct(holder,gate,treasure),plan.center(),1,false).getFirst().equals(plan.center()),"All three mixed targets resolve nearest wreck");
     var out=new LinkedHashMap<String,Object>();out.put("schema",93);out.put("mapId",mapId);out.put("nativeShipwreckLootMap",true);out.put("registeredPlayer",true);out.put("reopen",REOPEN);out.put("position",List.of(player.getX(),player.getY(),player.getZ()));out.put("initialHash",hash(initial));out.put("finalHash",serverHash);out.put("completedColumns",audit[0]-columns);out.put("maxInventoryTickNanos",maxUpdate);out.put("inventorySamples",updates);out.put("maxDrawingTickNanos",maxTick);out.put("peakAudit",peak);out.put("witness",plan);
     player.getInventory().setItem(1,player.getMainHandItem());player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);phase="paused";phaseTick=ticks;Files.writeString(dir().resolve(REOPEN?"reopen-server-results.json":"server-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
   }else if(phase.equals("paused")&&ticks-phaseTick>=20){
    var audit=ProgressiveMapDrawing.audit(server);WellInteraction.require(audit[2]==0&&audit[3]==0&&audit[4]==0&&audit[5]==0,"Putting map away releases tickets and jobs");
    server.saveEverything(false,true,true);done=true;phase="done";
   }
   if(ticks%100==0)Files.writeString(dir().resolve("phase.json"),"{\"phase\":\""+phase+"\",\"ticks\":"+ticks+"}");
  }catch(Throwable t){failure=t;done=true;blockbranch.Blockbranch.LOGGER.error("SHIPWRECK_CONNECTED_FAIL",t);}});
 }
 private static void teleport(ServerLevel level,double x,double y,double z,float yaw,float pitch){level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+x+" "+y+" "+z+" "+yaw+" "+pitch);}
 public static volatile BlockPos chestPosition;
 private static BlockPos local(int x,int y,int z){return new BlockPos(plan.x()+ShipwreckDiscovery.rotateX(x-4,z-15,plan.rotation()),plan.y()+y,plan.z()+ShipwreckDiscovery.rotateZ(x-4,z-15,plan.rotation()));}
}
