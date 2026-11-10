package blockbranch.realistic;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
/** Focused combined-definition interaction on a disposable world and an actual connected player. */
public final class CombinedMapWitness {
 public static volatile boolean clientSawGate,clientSawTreasure;public static volatile BlockPos gateInside;
 public static final BlockPos TREASURE=new BlockPos(-504775,61,-1657815);
 public static final boolean ON=Boolean.getBoolean("blockbranch.combinedMapWitness");
 public static void check(ServerLevel level,ServerPlayer player,boolean reopen)throws Exception{
  var g=(RealisticGenerator)level.getChunkSource().getGenerator();var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  WellInteraction.require(g.discoveryDefinition()==1&&g.waygateDefinition()==1&&RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject().get("shape_version").getAsInt()==92,"Actual connected world uses combined schema92");
  var plan=g.waygates(level.getChunkSource().randomState()).plans().getFirst();
  for(int z=(plan.z()-6)>>4;z<=(plan.z()+6)>>4;z++)for(int x=(plan.x()-6)>>4;x<=(plan.x()+6)>>4;x++)level.getChunk(x,z);
  var chestPos=TREASURE;level.getChunk(chestPos.getX()>>4,chestPos.getZ()>>4);var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);
  WellInteraction.require(chest!=null,"Combined native buried treasure chest generated");var edit=chestPos.offset(2,3,2);var inside=plan.local(6,4,7);gateInside=inside;
  if(!reopen){
   WellInteraction.require(chest.saveWithoutMetadata(level.registryAccess()).toString().contains("buried_treasure"),"Original native treasure loot table");
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"gamemode survival "+player.getName().getString());
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+(inside.getX()+1.5)+" "+(inside.getY()+1)+" "+(inside.getZ()+1.5));
   WellInteraction.require(!level.getBlockState(inside).is(Blocks.NETHER_PORTAL),"Combined damaged waygate starts inactive");
   for(int x=5;x<=8;x++)for(int y=3;y<=7;y++)if(x==5||x==8||y==3||y==7)level.setBlock(plan.local(x,y,7),Blocks.OBSIDIAN.defaultBlockState(),3);
   var flint=new ItemStack(Items.FLINT_AND_STEEL);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,flint);var bottom=plan.local(6,3,7);
   player.gameMode.useItemOn(player,level,flint,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(bottom),Direction.UP,bottom,false));
   WellInteraction.require(flint.getDamageValue()==1,"Registered player's native ignition consumes durability");player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  }else{WellInteraction.require(chest.getItem(0).isEmpty()&&level.getBlockState(edit).is(Blocks.WOOL.red()),"Combined treasure loot removal and edit persist");}
  WellInteraction.require(level.getBlockState(inside).is(Blocks.NETHER_PORTAL),"Combined repaired waygate remains active");
  Files.writeString(TreasureConnectedProbe.dir().resolve(reopen?"combined-reopen.json":"combined-fresh.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(java.util.Map.of("schema",92,"registeredPlayer",level.getServer().getPlayerList().getPlayers().contains(player),"gate",plan,"treasure",chestPos.toShortString(),"nativeIgnition",true,"reopened",reopen)));
 }
 public static void openTreasure(ServerLevel level,ServerPlayer player){
  level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"gamemode creative "+player.getName().getString());
  level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+(TREASURE.getX()+.5)+" "+(TREASURE.getY()+2)+" "+(TREASURE.getZ()+.5));
  var chest=(ChestBlockEntity)level.getBlockEntity(TREASURE);player.openMenu(chest);chest.setItem(0,ItemStack.EMPTY);chest.setChanged();level.setBlock(TREASURE.offset(2,3,2),Blocks.WOOL.red().defaultBlockState(),3);
 }
 public static void prepare(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{var g=(RealisticGenerator)server.overworld().getChunkSource().getGenerator();WellInteraction.require(g.discoveryDefinition()==1&&g.waygateDefinition()==1,"Fresh combined default");server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS fresh combined92 native world prepared\n");}catch(Throwable t){throw new RuntimeException(t);}finally{server.halt(false);}});}
}
