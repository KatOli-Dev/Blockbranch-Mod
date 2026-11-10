package blockbranch.metallurgy;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
/** Real connected player, ticking dropped-item motion and normal pickup before unedited captures. */
public final class MagnetiteViewScene {
 public static void register(){
  java.util.UUID[] id={null};int[] ticks={0},stage={0};net.minecraft.world.entity.item.ItemEntity[] probe={null};boolean[] approached={false};
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{id[0]=handler.player.getUUID();ticks[0]=0;stage[0]=0;approached[0]=false;});
  ServerTickEvents.END_SERVER_TICK.register(server->{if(id[0]==null||++ticks[0]<20)return;var p=server.getPlayerList().getPlayer(id[0]);if(p==null)return;var l=server.overworld();
   if(stage[0]==0){
    l.getGameRules().set(GameRules.ADVANCE_TIME,false,server);l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
    for(int z=-11;z<=6;z++)for(int x=-8;x<=8;x++)l.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
    var columns=java.util.List.of(Blocks.STONE,MagnetiteContent.ORE,MagnetiteContent.DEEPSLATE_ORE,Blocks.COAL_ORE,Blocks.IRON_ORE);for(int group=0;group<5;group++)for(int x=0;x<2;x++)for(int y=65;y<=67;y++)l.setBlock(new BlockPos(-5+group*2+x,y,1),columns.get(group).defaultBlockState(),2);
    var pos=new BlockPos(7,65,3);l.setBlock(pos,MagnetiteContent.ORE.defaultBlockState(),2);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var tool=new ItemStack(Items.IRON_PICKAXE);p.getInventory().setItem(0,tool);p.getInventory().setSelectedSlot(0);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(MagnetiteContent.BOOTS));server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" 7.5 65 5.5");
    if(!p.gameMode.destroyBlock(pos)||tool.getDamageValue()!=1)throw new AssertionError("actual iron mining failed");
    probe[0]=new net.minecraft.world.entity.item.ItemEntity(l,7.5,65.2,3,new ItemStack(Items.GOLD_NUGGET));probe[0].setNoPickUpDelay();probe[0].setTarget(p.getUUID());probe[0].setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);l.addFreshEntity(probe[0]);stage[0]=1;return;
   }
   if(stage[0]!=1)return;if(!probe[0].isRemoved()&&probe[0].getZ()>3.2)approached[0]=true;
   if(!probe[0].isRemoved()){if(ticks[0]>220)throw new AssertionError("live magnetite item did not reach native pickup: "+probe[0].position()+" motion="+probe[0].getDeltaMovement());return;}
   boolean collected=false;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(Items.GOLD_NUGGET))collected=true;if(!approached[0]||!collected)throw new AssertionError("live attraction must move item then use native pickup");blockbranch.Blockbranch.LOGGER.info("MAGNETITE live native ticking item approached and was collected normally; elapsedTicks={}",ticks[0]-20);
   p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" 0.0 67.25 -10.5 0 12");
   net.minecraft.world.level.ItemLike[] items={Items.IRON_PICKAXE,MagnetiteContent.ORE,MagnetiteContent.DEEPSLATE_ORE,MagnetiteContent.FRAGMENT,MagnetiteContent.BOOTS,Items.IRON_INGOT};for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));
   var boots=new ItemStack(MagnetiteContent.BOOTS);boots.set(net.minecraft.core.component.DataComponents.TRIM,new net.minecraft.world.item.equipment.trim.ArmorTrim(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRIM_MATERIAL).getOrThrow(net.minecraft.world.item.equipment.trim.TrimMaterials.QUARTZ),l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRIM_PATTERN).getOrThrow(net.minecraft.world.item.equipment.trim.TrimPatterns.SENTRY)));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,boots);p.getInventory().setItem(4,boots.copy());
   var babyId=java.util.UUID.fromString("591b1a99-09eb-4bce-9569-a088e271ba7e");var old=l.getEntity(babyId);if(old!=null)old.discard();var baby=net.minecraft.world.entity.EntityTypes.HUSK.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);baby.setUUID(babyId);baby.setBaby(true);baby.setNoAi(true);baby.setNoGravity(true);baby.setPermanentlyInvulnerable(true);baby.setSilent(true);baby.setPos(1.5,67.25,-10.5);baby.setYRot(0);baby.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(MagnetiteContent.BOOTS));l.addFreshEntity(baby);p.getInventory().setSelectedSlot(0);p.containerMenu.broadcastChanges();stage[0]=2;
  });
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
 }
 private MagnetiteViewScene(){}
}
