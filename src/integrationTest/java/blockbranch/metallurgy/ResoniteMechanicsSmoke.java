package blockbranch.metallurgy;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.gameevent.*;
import net.minecraft.world.phys.*;
/** Native reversible crafting, loot, and event dispatch, without replacing production sound or tick logic. */
public final class ResoniteMechanicsSmoke {
 public static void verify(ServerLevel l,Path d)throws Exception{
  var manager=l.getServer().getRecipeManager();var input=CraftingInput.of(2,2,Collections.nCopies(4,new ItemStack(ResoniteContent.CRYSTAL)));var block=manager.getRecipeFor(RecipeType.CRAFTING,input,l).orElseThrow().value().assemble(input);require(block.is(ResoniteContent.BLOCK.asItem())&&block.getCount()==1,"four crystals craft native placeable block");var back=CraftingInput.of(1,1,List.of(block));var crystals=manager.getRecipeFor(RecipeType.CRAFTING,back,l).orElseThrow().value().assemble(back);require(crystals.is(ResoniteContent.CRYSTAL)&&crystals.getCount()==4,"reversible crystal block");require(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(2,2,List.of(new ItemStack(ResoniteContent.CRYSTAL),new ItemStack(ResoniteContent.CRYSTAL),new ItemStack(ResoniteContent.CRYSTAL),ItemStack.EMPTY)),l).isEmpty(),"no underfilled recipe");
  var tool=new ItemStack(Items.IRON_PICKAXE);var silk=tool.copy();silk.enchant(l.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);for(var ore:List.of(ResoniteContent.ORE,ResoniteContent.DEEPSLATE_ORE)){require(tool.isCorrectToolForDrops(ore.defaultBlockState())&&!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()),"iron harvest");require(Block.getDrops(ore.defaultBlockState(),l,BlockPos.ZERO,null,null,tool).getFirst().is(ResoniteContent.CRYSTAL),"crystal loot");require(Block.getDrops(ore.defaultBlockState(),l,BlockPos.ZERO,null,null,silk).getFirst().is(ore.asItem()),"silk loot");}
  require(Block.getDrops(ResoniteContent.BLOCK.defaultBlockState(),l,BlockPos.ZERO,null,null,tool).getFirst().is(ResoniteContent.BLOCK.asItem()),"crafted block drops itself");
  var pos=new BlockPos(576,64,0);l.getChunkAt(pos);var saved=d.resolve("resonite-block.txt");if(Files.exists(saved)){require(l.getBlockState(pos).is(ResoniteContent.ORE),"placed signaling block persists before fixture writes");long time=Long.parseLong(Files.readString(saved));if(l.getGameTime()<time+ResoniteBlock.COOLDOWN){require(l.getBlockTicks().hasScheduledTick(pos,ResoniteContent.ORE),"native cooldown survives full relaunch");blockbranch.Blockbranch.LOGGER.info("RESONITE native saved cooldown retained before fixture reset");}}
  // Saved pending ticks enter the queue only when native chunk ticking begins. Unpack before resetting this disposable probe corridor.
  l.getChunkAt(pos).unpackTicks(l.getGameTime());l.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(576,64,0,580,64,2));int[] events={0};var registry=l.getChunkAt(pos).getListenerRegistry(pos.getY()>>4);var listener=new GameEventListener(){public PositionSource getListenerSource(){return new BlockPositionSource(pos);}public int getListenerRadius(){return 16;}public boolean handleGameEvent(ServerLevel level,Holder<GameEvent> event,GameEvent.Context context,Vec3 point){if(event.is(GameEvent.BLOCK_ACTIVATE.unwrapKey().orElseThrow()))events[0]++;return true;}};registry.register(listener);
  var player=new net.minecraft.world.entity.player.Player(l,new com.mojang.authlib.GameProfile(new UUID(0,201),"ResoniteProbe")){@Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}};var arrow=EntityTypes.ARROW.create(l,EntitySpawnReason.COMMAND);
  try{
   int index=0;for(var b:List.of(ResoniteContent.ORE,ResoniteContent.DEEPSLATE_ORE,ResoniteContent.BLOCK)){var p=pos.offset(index++*2,0,0);l.setBlock(p,b.defaultBlockState(),2);var state=l.getBlockState(p);int before=events[0];state.attack(l,p,player);require(events[0]==before+1&&l.getBlockTicks().hasScheduledTick(p,b),"native attack event and per-block scheduled cooldown");for(int i=0;i<20;i++){state.attack(l,p,player);b.stepOn(l,p,state,player);state.onProjectileHit(l,state,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false),arrow);}require(events[0]==before+1&&l.getBlockState(p)==state&&l.getBlockEntity(p)==null,"repeat disturbances debounced; state/edits unchanged; no custom block entity");}
   var step=pos.offset(0,0,2);l.setBlock(step,ResoniteContent.BLOCK.defaultBlockState(),2);ResoniteContent.BLOCK.stepOn(l,step,l.getBlockState(step),player);require(events[0]==4,"independent native footstep activation");var projectile=pos.offset(2,0,2);l.setBlock(projectile,ResoniteContent.BLOCK.defaultBlockState(),2);var state=l.getBlockState(projectile);state.onProjectileHit(l,state,new BlockHitResult(Vec3.atCenterOf(projectile),Direction.UP,projectile,false),arrow);require(events[0]==5,"native projectile hit activation");Blocks.STONE.defaultBlockState().attack(l,projectile,player);require(events[0]==5,"ordinary stone unchanged");
   var breaking=pos.offset(4,0,2);l.setBlock(breaking,ResoniteContent.BLOCK.defaultBlockState(),2);var unbroken=l.getBlockState(breaking);require(ResoniteContent.BLOCK.playerWillDestroy(l,breaking,unbroken,player)==unbroken&&events[0]==6,"native pre-break callback retains ordinary destruction state");unbroken.attack(l,breaking,player);require(events[0]==6,"pre-break/attack share cooldown");
   Files.writeString(saved,Long.toString(l.getGameTime()));l.getServer().saveEverything(false,true,true);
  }finally{registry.unregister(listener);}
  blockbranch.Blockbranch.LOGGER.info("RESONITE native recipes/loot/attack/step/projectile and debounce passed; events={}",events[0]);
 }
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 private ResoniteMechanicsSmoke(){}
}
