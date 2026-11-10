package blockbranch.metallurgy;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
/** Read-only loaded-world and native inventory exposure checks. Shulker contents are an explicit shielding boundary. */
public final class Radiation {
 public static final int RADIUS=4,LINGER=100;
 public static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
 private static final int[][] OFFSETS=offsets();
 private static int[][] offsets(){var values=new ArrayList<int[]>();for(int y=-RADIUS;y<=RADIUS;y++)for(int z=-RADIUS;z<=RADIUS;z++)for(int x=-RADIUS;x<=RADIUS;x++)if(x*x+y*y+z*z<=RADIUS*RADIUS)values.add(new int[]{x,y,z});return values.toArray(int[][]::new);}
 public static int unprotectedPieces(LivingEntity entity){int missing=0;for(var slot:ARMOR)if(!entity.getItemBySlot(slot).is(UraniumContent.SHIELDING_ARMOR))missing++;return missing;}
 public static boolean radioactive(ItemStack root){
  if(root.isEmpty())return false;
  if(root.is(UraniumContent.RADIOACTIVE))return true;
  if(root.getItem() instanceof BlockItem b&&b.getBlock() instanceof ShulkerBoxBlock)return false;
  if(!root.has(DataComponents.CONTAINER)&&!root.has(DataComponents.BUNDLE_CONTENTS))return false;
  // Lazy depth-first traversal bounds retained iterators by component depth, not total item count.
  // Immutable shared subtrees need only one visit. Never infer radiation from a scan limit.
  var pending=new ArrayDeque<Iterator<ItemStack>>();pending.push(List.of(root).iterator());
  Set<Object> visited=Collections.newSetFromMap(new IdentityHashMap<>());
  while(!pending.isEmpty()){
   var iterator=pending.peek();if(!iterator.hasNext()){pending.pop();continue;}
   var stack=iterator.next();if(stack.isEmpty())continue;
   if(stack.is(UraniumContent.RADIOACTIVE))return true;
   if(stack.getItem() instanceof BlockItem b&&b.getBlock() instanceof ShulkerBoxBlock)continue;
   var container=stack.get(DataComponents.CONTAINER);if(container!=null&&container.size()>0&&visited.add(container))pending.push(container.itemCopies().iterator());
   var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);if(bundle!=null&&!bundle.isEmpty()&&visited.add(bundle))pending.push(bundle.itemCopies().iterator());
  }
  return false;
 }
 public static boolean carried(LivingEntity entity){
  if(entity instanceof Player p){
   for(int i=0;i<p.getInventory().getContainerSize();i++)if(radioactive(p.getInventory().getItem(i)))return true;
   // Cursor and the personal crafting grid remain carried items; open world-container slots do not.
   if(radioactive(p.containerMenu.getCarried()))return true;
   var crafting=p.inventoryMenu.getCraftSlots();for(int i=0;i<crafting.getContainerSize();i++)if(radioactive(crafting.getItem(i)))return true;
  }
  for(var slot:EquipmentSlot.values())if(radioactive(entity.getItemBySlot(slot)))return true;
  return false;
 }
 public static boolean nearby(ServerLevel level,BlockPos center){
  var probe=new BlockPos.MutableBlockPos();for(var d:OFFSETS){probe.set(center.getX()+d[0],center.getY()+d[1],center.getZ()+d[2]);if(!level.isInWorldBounds(probe))continue;var chunk=level.getChunkSource().getChunkNow(probe.getX()>>4,probe.getZ()>>4);if(chunk==null)continue;
   if(chunk instanceof blockbranch.cubic.SparseColumn sparse){var terrain=sparse.blockbranch$terrain();if(terrain!=null&&!terrain.hasEdits(probe.getY()>>4)){var base=terrain.base();int depth=base.groundHeight(probe.getX(),probe.getZ())-probe.getY();if(base.ores().definition()<14||depth<UraniumVeins.MIN_DEPTH||depth>UraniumVeins.MAX_DEPTH)continue;}}
   var state=chunk.getBlockState(probe);if(state.is(UraniumContent.ORE)||state.is(UraniumContent.DEEPSLATE_ORE))return true;}return false;
 }
 public static void tick(LivingEntity entity){
  if(!(entity.level() instanceof ServerLevel level)||!entity.isAlive()||entity.isSpectator())return;
  if(unprotectedPieces(entity)==0){if(entity.hasEffect(UraniumContent.RADIATION))entity.removeEffect(UraniumContent.RADIATION);return;}
  if(Math.floorMod(entity.tickCount+entity.getId(),20)!=0)return;
  if(carried(entity)||nearby(level,entity.blockPosition()))irradiate(entity,LINGER);
 }
 public static boolean irradiate(LivingEntity entity,int ticks){return unprotectedPieces(entity)>0&&entity.addEffect(new MobEffectInstance(UraniumContent.RADIATION,ticks,0,false,false,true));}
 private Radiation(){}
}
