package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import blockbranch.cubic.*;
public final class WellInteraction {
 static Map<String,Object> run(ServerLevel level)throws Exception{
  var q=Files.readString(Path.of("well.txt")).trim().split(",");var p=new BlockPos(Integer.parseInt(q[0])-1,Integer.parseInt(q[1])-1,Integer.parseInt(q[2]));
  var marker=Path.of("brushed.txt");boolean reload=Files.exists(marker);
  if(reload){require(level.getBlockState(p).is(Blocks.SAND),"Brushed deposit remains sand after reopen");require(level.getBlockEntity(p)==null,"No recreated archaeology entity");return Map.of("reload",true,"persistentConsumedDeposit",true);}
  require(level.getBlockState(p).is(Blocks.SUSPICIOUS_SAND),"Natural deposit");
  var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WellBrush"),ClientInformation.createDefault());
  player.setPos(p.getX()+.5,p.getY()+2,p.getZ()+.5);player.setXRot(90);player.setYRot(0);player.xRotO=90;player.yRotO=0;var brush=new ItemStack(Items.BRUSH);player.setItemInHand(InteractionHand.MAIN_HAND,brush);player.startUsingItem(InteractionHand.MAIN_HAND);
  long original=level.getGameTime();int calls=0;var box=new AABB(p).inflate(4);int before=level.getEntitiesOfClass(ItemEntity.class,box).size();
  for(int i=0;i<12&&level.getBlockState(p).is(Blocks.SUSPICIOUS_SAND);i++){
   ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(original+i*10);
   Items.BRUSH.onUseTick(level,player,brush,196-i*10);calls++;
  }
  ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(original);
  require(level.getBlockState(p).is(Blocks.SAND),"Actual BrushItem raycast converts deposit to sand");require(brush.getDamageValue()==1,"Vanilla brush durability consumed");
  var drops=level.getEntitiesOfClass(ItemEntity.class,box);require(drops.size()==before+1,"Exactly one archaeology item entity");var item=drops.getLast().getItem();require(item.getCount()==1,"Vanilla archaeology stack size");
  var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();terrain.flush();terrain.evictExcept(Set.of());require(level.getBlockState(p).is(Blocks.SAND),"Consumed deposit survives eviction");
  level.getServer().saveEverything(false,true,true);Files.writeString(marker,p.toShortString());return Map.of("reload",false,"brushCalls",calls,"damage",brush.getDamageValue(),"drop",item.toString());
 }
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
