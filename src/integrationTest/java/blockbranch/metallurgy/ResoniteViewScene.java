package blockbranch.metallurgy;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
/** Real native ticking cooldown and sculk response precede the client sound/capture checks. */
public final class ResoniteViewScene {
 public static void register(){
  java.util.UUID[] id={null};int[] ticks={0},stage={0},started={0};boolean[] sensor={false};
  var crystal=new BlockPos(6,65,2);var sculk=new BlockPos(6,65,0);
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{id[0]=handler.player.getUUID();ticks[0]=0;stage[0]=0;sensor[0]=false;});
  ServerTickEvents.END_SERVER_TICK.register(server->{if(id[0]==null||++ticks[0]<20)return;var p=server.getPlayerList().getPlayer(id[0]);if(p==null)return;var l=server.overworld();
   if(stage[0]==0){
    l.getGameRules().set(GameRules.ADVANCE_TIME,false,server);l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
    for(int z=-11;z<=6;z++)for(int x=-8;x<=8;x++)l.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
    var columns=java.util.List.of(Blocks.STONE,ResoniteContent.ORE,ResoniteContent.DEEPSLATE_ORE,ResoniteContent.BLOCK,Blocks.AMETHYST_BLOCK);for(int group=0;group<5;group++)for(int x=0;x<2;x++)for(int y=65;y<=67;y++)l.setBlock(new BlockPos(-5+group*2+x,y,1),columns.get(group).defaultBlockState(),2);
    l.setBlock(crystal,ResoniteContent.BLOCK.defaultBlockState(),2);l.setBlock(sculk,Blocks.SCULK_SENSOR.defaultBlockState(),2);
    p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" 0.0 67.25 -10.5 0 12");p.getInventory().clearContent();stage[0]=1;return;
   }
   if(stage[0]==1&&ticks[0]>=100){l.getBlockState(crystal).attack(l,crystal,p);if(!l.getBlockTicks().hasScheduledTick(crystal,ResoniteContent.BLOCK))throw new AssertionError("native cooldown not scheduled");started[0]=ticks[0];stage[0]=2;return;}
   if(stage[0]!=2)return;
   if(l.getBlockState(sculk).getValue(SculkSensorBlock.POWER)>0&&((SculkSensorBlockEntity)l.getBlockEntity(sculk)).getLastVibrationFrequency()==net.minecraft.world.level.gameevent.vibrations.VibrationSystem.getGameEventFrequency(net.minecraft.world.level.gameevent.GameEvent.BLOCK_ACTIVATE))sensor[0]=true;
   int elapsed=ticks[0]-started[0];if(elapsed<12)return;if(!sensor[0]){if(elapsed>60)throw new AssertionError("resonite did not activate native sculk sensor");return;}
   if(l.getBlockTicks().hasScheduledTick(crystal,ResoniteContent.BLOCK))throw new AssertionError("native ticking cooldown did not expire");l.getBlockState(crystal).attack(l,crystal,p);if(!l.getBlockTicks().hasScheduledTick(crystal,ResoniteContent.BLOCK))throw new AssertionError("native cooldown did not rearm");
   var mine=new BlockPos(-7,65,0);l.setBlock(mine,ResoniteContent.ORE.defaultBlockState(),2);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var tool=new ItemStack(Items.IRON_PICKAXE);p.getInventory().setItem(0,tool);p.getInventory().setSelectedSlot(0);if(!p.gameMode.destroyBlock(mine)||tool.getDamageValue()!=1)throw new AssertionError("native resonite mining");p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);var creative=new BlockPos(-7,65,2);l.setBlock(creative,ResoniteContent.BLOCK.defaultBlockState(),2);if(!p.gameMode.destroyBlock(creative)||!l.getBlockState(creative).isAir()||tool.getDamageValue()!=1)throw new AssertionError("native creative destruction/wear");
   net.minecraft.world.level.ItemLike[] items={Items.IRON_PICKAXE,ResoniteContent.ORE,ResoniteContent.DEEPSLATE_ORE,ResoniteContent.CRYSTAL,ResoniteContent.BLOCK};for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));p.getInventory().setSelectedSlot(0);p.containerMenu.broadcastChanges();blockbranch.Blockbranch.LOGGER.info("RESONITE live native survival/creative break, cooldown expired/rearmed and sculk activated; elapsedTicks={}",elapsed);stage[0]=3;
  });
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
 }
 private ResoniteViewScene(){}
}
