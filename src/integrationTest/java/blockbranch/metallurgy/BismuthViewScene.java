package blockbranch.metallurgy;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/** Disposable scene: client mines through native controls; solid stone hides two different ore types. */
public final class BismuthViewScene {
 public static void register(){
  java.util.UUID[] id={null};int[] ticks={0};boolean[] ready={false},gallery={false};
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{id[0]=handler.player.getUUID();ticks[0]=0;ready[0]=false;gallery[0]=false;});
  ServerTickEvents.END_SERVER_TICK.register(server->{if(id[0]==null||++ticks[0]<20)return;var p=server.getPlayerList().getPlayer(id[0]);if(p==null)return;var l=p.level();
   // Saved or late-synchronized hotbar selection cannot skip the native survival proof.
   if(ready[0]){if(Boolean.getBoolean("blockbranch.pickaxeArtView")&&!gallery[0]&&p.getInventory().getSelectedSlot()==8&&p.getInventory().getItem(6).isEmpty()&&l.getBlockState(new BlockPos(0,65,-3)).isAir()&&l.getBlockState(new BlockPos(1,65,-3)).isAir()&&l.getBlockState(new BlockPos(-1,65,-3)).isAir()){
    gallery[0]=true;Item[] picks={Items.IRON_PICKAXE,MetallurgyContent.BRONZE_PICKAXE,SteelContent.STEEL_PICKAXE,TungstenContent.TUNGSTEN_STEEL_PICKAXE,StainlessContent.STAINLESS_STEEL_PICKAXE,PreciousContent.SILVER_PICKAXE,PlatinumAlloyContent.PICKAXE,TitaniumEquipment.TITANIUM_ALLOY_PICKAXE,BismuthContent.PICKAXE};for(int i=0;i<picks.length;i++)p.getInventory().setItem(i,new ItemStack(picks[i]));p.containerMenu.broadcastChanges();
   }return;}ready[0]=true;
   l.getGameRules().set(GameRules.ADVANCE_TIME,false,server);l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
   for(int z=-8;z<=4;z++)for(int x=-8;x<=8;x++)l.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
   for(int z=-7;z<=3;z++)for(int y=65;y<=69;y++)for(int x=-7;x<=7;x++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
   for(int x=-4;x<=4;x++)for(int y=65;y<=68;y++)l.setBlock(new BlockPos(x,y,0),Blocks.STONE.defaultBlockState(),2);
   l.setBlock(new BlockPos(-2,66,1),Blocks.DIAMOND_ORE.defaultBlockState(),2);l.setBlock(new BlockPos(2,66,1),FoolsDiamondContent.ORE.defaultBlockState(),2);
   l.setBlock(new BlockPos(-6,65,0),BismuthContent.ORE.defaultBlockState(),2);l.setBlock(new BlockPos(-6,66,0),BismuthContent.DEEPSLATE_ORE.defaultBlockState(),2);
   l.setBlock(new BlockPos(0,65,-3),Blocks.STONE.defaultBlockState(),2);l.setBlock(new BlockPos(1,65,-3),Blocks.STONE.defaultBlockState(),2);l.setBlock(new BlockPos(-1,65,-3),Blocks.STONE.defaultBlockState(),2);
   p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.removeAllEffects();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
   p.getInventory().setItem(0,new ItemStack(BismuthContent.PICKAXE));p.getInventory().setItem(1,new ItemStack(BismuthContent.RAW));p.getInventory().setItem(2,new ItemStack(BismuthContent.INGOT));p.getInventory().setItem(3,new ItemStack(BismuthContent.ORE));p.getInventory().setItem(4,new ItemStack(BismuthContent.DEEPSLATE_ORE));p.getInventory().setItem(5,new ItemStack(SteelContent.STEEL_PICKAXE));var last=new ItemStack(BismuthContent.PICKAXE);last.setDamageValue(last.getMaxDamage()-1);p.getInventory().setItem(6,last);
   p.setHealth(20);server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" 0.0 65.0 -5.5 0 9");p.containerMenu.broadcastChanges();
  });
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
 }
 private BismuthViewScene(){}
}
