package blockbranch.metallurgy;

import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Native ingredients/components, observation bounds and independently frozen published generation. */
public final class BismuthSmoke {
 static void require(boolean b,String m){if(!b)throw new AssertionError("BISMUTH "+m);}
 static boolean bismuth(BlockState state){return state.is(BismuthContent.ORE)||state.is(BismuthContent.DEEPSLATE_ORE);}
 public static void verify(ServerLevel level,Path directory)throws Exception{materials(level,directory);scan(level);packets(level);legacy(level,directory);natural(level,directory);}
 private static void materials(ServerLevel l,Path d)throws Exception{
  var manager=l.getServer().getRecipeManager();for(var material:List.of(BismuthContent.RAW,BismuthContent.ORE.asItem(),BismuthContent.DEEPSLATE_ORE.asItem()))for(var type:List.of(RecipeType.SMELTING,RecipeType.BLASTING))require(manager.getRecipeFor(type,new SingleRecipeInput(new ItemStack(material)),l).orElseThrow().value().assemble(new SingleRecipeInput(new ItemStack(material))).is(BismuthContent.INGOT),"native processing");
  var registry=l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var unbreaking=registry.getOrThrow(Enchantments.UNBREAKING);
  var base=new ItemStack(SteelContent.STEEL_PICKAXE);base.setDamageValue(31);base.enchant(unbreaking,2);base.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Seeker"));var before=base.copy();var input=new SmithingRecipeInput(ItemStack.EMPTY,base,new ItemStack(BismuthContent.INGOT));var result=manager.getRecipeFor(RecipeType.SMITHING,input,l).orElseThrow().value().assemble(input);
  require(result.is(BismuthContent.PICKAXE)&&result.getDamageValue()==31&&result.getEnchantments().getLevel(unbreaking)==2&&result.get(DataComponents.CUSTOM_NAME).getString().equals("Seeker")&&ItemStack.isSameItemSameComponents(base,before),"native no-template smithing preserves damage/name/enchantments and input");
  require(manager.getRecipeFor(RecipeType.SMITHING,new SmithingRecipeInput(ItemStack.EMPTY,new ItemStack(Items.IRON_PICKAXE),new ItemStack(BismuthContent.INGOT)),l).isEmpty(),"only existing steel target");
  for(var c:SteelContent.STEEL_PICKAXE.components())if(c.type()!=DataComponents.ITEM_NAME&&c.type()!=DataComponents.ITEM_MODEL&&c.type()!=DataComponents.LORE)require(Objects.equals(BismuthContent.PICKAXE.components().get(c.type()),c.value()),"same steel native component "+c.type());
  for(var e:registry.listElements().toList())require(e.value().canEnchant(base)==e.value().canEnchant(new ItemStack(BismuthContent.PICKAXE)),"steel enchant eligibility "+e.key());
  require(result.isValidRepairItem(new ItemStack(SteelContent.STEEL_INGOT))&&!result.isValidRepairItem(new ItemStack(BismuthContent.INGOT)),"unchanged steel repair");
  var ops=RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,l.registryAccess());var save=d.resolve("bismuth-pickaxe.dat");if(Files.exists(save)){var prior=ItemStack.CODEC.parse(ops,net.minecraft.nbt.NbtIo.readCompressed(save,net.minecraft.nbt.NbtAccounter.unlimitedHeap())).getOrThrow();require(ItemStack.isSameItemSameComponents(prior,result),"refined tool relaunch");}net.minecraft.nbt.NbtIo.writeCompressed((net.minecraft.nbt.CompoundTag)ItemStack.CODEC.encodeStart(ops,result).getOrThrow(),save);
  for(var ore:List.of(BismuthContent.ORE,BismuthContent.DEEPSLATE_ORE)){
   require(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState())&&!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()),"iron harvest gate");
   for(boolean silk:new boolean[]{false,true}){var pick=new ItemStack(Items.DIAMOND_PICKAXE);pick.enchant(registry.getOrThrow(silk?Enchantments.SILK_TOUCH:Enchantments.FORTUNE),silk?1:3);var drops=Block.getDrops(ore.defaultBlockState(),l,BlockPos.ZERO,null,null,pick);require(drops.size()==1&&drops.getFirst().is(silk?ore.asItem():BismuthContent.RAW),"native silk/fortune ore drop");}
  }
  blockbranch.Blockbranch.LOGGER.info("BISMUTH native processing, smithing/component/enchant parity, iron gate and silk/fortune passed");
 }
 private static void scan(ServerLevel l){
  var center=new BlockPos(5000,80,5000);for(int z=-6;z<=6;z++)for(int x=-6;x<=6;x++)l.getChunk((center.getX()+x)>>4,(center.getZ()+z)>>4);for(int y=-6;y<=6;y++)for(int z=-6;z<=6;z++)for(int x=-6;x<=6;x++)l.setBlock(center.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
  var gold=center.offset(2,0,0);var fake=center.offset(-2,0,0);var deep=center.offset(0,0,6);var outside=center.offset(5,0,5);l.setBlock(gold,Blocks.GOLD_ORE.defaultBlockState(),2);l.setBlock(fake,FoolsDiamondContent.ORE.defaultBlockState(),2);l.setBlock(deep,UraniumContent.DEEPSLATE_ORE.defaultBlockState(),2);l.setBlock(outside,Blocks.DIAMOND_ORE.defaultBlockState(),2);
  var found=OrePulse.nearby(l,center);require(found.size()==3&&found.containsAll(List.of(gold,fake,deep))&&!found.contains(outside),"uniform observations include fake/uranium but exclude outside sphere");require(l.getBlockState(gold).is(Blocks.GOLD_ORE),"scan never modifies ore");l.setBlock(gold,Blocks.AIR.defaultBlockState(),2);require(!OrePulse.nearby(l,center).contains(gold),"mined player edit removes marker");
  var far=new BlockPos(2_000_000,90,2_000_000);require(l.getChunkSource().getChunkNow(far.getX()>>4,far.getZ()>>4)==null,"far control unloaded");require(OrePulse.nearby(l,far).isEmpty()&&l.getChunkSource().getChunkNow(far.getX()>>4,far.getZ()>>4)==null,"scan never loads chunks");
  for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)for(int x=-4;x<=4;x++)l.setBlock(center.offset(x,y,z),Blocks.IRON_ORE.defaultBlockState(),2);var capped=OrePulse.nearby(l,center);require(capped.size()==64&&capped.getFirst().equals(center),"nearest-first marker cap");
  var nether=l.getServer().getLevel(net.minecraft.world.level.Level.NETHER);var netherPos=new BlockPos(4000,80,4000);nether.getChunkAt(netherPos);nether.setBlock(netherPos,Blocks.NETHER_QUARTZ_ORE.defaultBlockState(),2);require(OrePulse.nearby(nether,netherPos).contains(netherPos),"same observation works in native Nether dimension");
  blockbranch.Blockbranch.LOGGER.info("BISMUTH read-only loaded scans, fake/generic identity, radius/64cap and native Nether passed");
 }
 private static void packets(ServerLevel l){
  var center=new BlockPos(1,-9000,2);var p=new OrePulsePacket(l.dimension().identifier(),center,List.of(center.offset(1,0,0),center.offset(0,6,0)));var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),l.registryAccess());try{OrePulsePacket.CODEC.encode(b,p);require(b.readableBytes()<100,"bounded identity-free payload");require(OrePulsePacket.CODEC.decode(b).equals(p)&&b.readableBytes()==0,"native packet codec");}finally{b.release();}
  try{new OrePulsePacket(l.dimension().identifier(),center,List.of(center.offset(7,0,0)));throw new AssertionError("outside accepted");}catch(IllegalArgumentException expected){}
  try{new OrePulsePacket(l.dimension().identifier(),center,Collections.nCopies(65,center));throw new AssertionError("overcount accepted");}catch(IllegalArgumentException expected){}
  for(int n:new int[]{-1,65}){var bad=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),l.registryAccess());try{bad.writeIdentifier(l.dimension().identifier());bad.writeBlockPos(center);bad.writeVarInt(n);try{OrePulsePacket.CODEC.decode(bad);throw new AssertionError("invalid count accepted");}catch(IllegalArgumentException expected){}}finally{bad.release();}}
 }
 private static void legacy(ServerLevel l,Path d)throws Exception{
  long count=0,deepCount=0;int added=0;
  require(!new OreLayer(1,0,0,14,0,0).maybeHas(BismuthSmoke::bismuth)&&new OreLayer(1,0,0,15,0,0).maybeHas(BismuthSmoke::bismuth)&&new OreLayer(1,0,0,15,0,0).affectsPredicate(BismuthSmoke::bismuth),"versioned bismuth predicates");
  try(var loader=MetallurgySmoke.historicalReader(Path.of(System.getProperty("blockbranch.oldUraniumClasses")).toUri().toURL())){
   var old=loader.loadClass("blockbranch.realistic.OreLayer");var ctor=old.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);var bind=old.getMethod("bind",int.class,int.class);var state=old.getMethod("state",int.class,int.class,int.class,int.class,BlockState.class,boolean.class);
   for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1}){var frozen=bind.invoke(ctor.newInstance(seed,cx,-1,14,65535,65535),64,64);var retained=new OreLayer(seed,cx,-1,14,65535,65535).bind(64,64);var next=new OreLayer(seed,cx,-1,15,65535,65535).bind(64,64);
    for(boolean caves:new boolean[]{false,true})for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=(BlockState)state.invoke(frozen,x,y,z,64,host,caves);var actual=next.state(x,y,z,64,host,caves);require(retained.state(x,y,z,64,host,caves)==expected,"retained14 exact");if(actual!=expected){require(expected==host&&bismuth(actual),"only surviving-host bismuth addition");added++;}count++;}
    for(int z=0;z<16;z+=2)for(int x=0;x<16;x+=2)for(int depth=8000;depth<=10000;depth+=16){int y=64-depth;var host=Blocks.DEEPSLATE.defaultBlockState();require(next.state(x,y,z,64,host)==state.invoke(frozen,x,y,z,64,host,false),"deep uranium unchanged under15");deepCount++;}
   }
   try{old.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,15,0,0).encode());throw new AssertionError("old14 accepted15");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"old descriptor refusal");}
   var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);for(boolean decoration:new boolean[]{false,true}){var root=Files.createTempDirectory(d,"old-uranium-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("old19 accepted20");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"old catalog refusal");}require(Files.readString(marker).equals(before),"old refusal preserves marker");}
  }
  require(added>0&&OreLayer.MAX_DEPTH==224,"new positive witnesses and unchanged depth index");blockbranch.Blockbranch.LOGGER.info("BISMUTH frozen14={} deepUnchanged={} additions={} old15/20 refusal=true",count,deepCount,added);
 }
 private static void natural(ServerLevel l,Path d)throws Exception{
  if(!(l.getChunkSource().getGenerator() instanceof RealisticGenerator g)||g.oreDefinition()<15)return;var marker=d.resolve("bismuth-mined.txt");if(Files.exists(marker)){var values=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(values[0]),Integer.parseInt(values[1]),Integer.parseInt(values[2]));var base=((SparseColumn)l.getChunkAt(pos)).blockbranch$terrain().base();require(l.getBlockState(pos).isAir()&&bismuth(base.state(pos.getX(),pos.getY(),pos.getZ())),"mined edit survives relaunch with retained procedural base");require(base.top(BismuthSmoke::bismuth,pos.getX(),pos.getZ(),base.groundHeight(pos.getX(),pos.getZ()))==pos.getY()+1,"reopened bismuth-only height query");return;}
  var origin=g.getOrigin(l.getChunkSource().randomState());boolean found=false;search:for(int dz=0;dz<4;dz++)for(int dx=0;dx<4;dx++){var chunk=l.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=24;depth<=192;depth++){int y=base.groundHeight(x,z)-depth;if(!bismuth(base.state(x,y,z)))continue;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);require(l.getBlockState(pos)==base.state(x,y,z),"native natural state agrees");var restored=TerrainProfile.decode(base.encode(l.palettedContainerFactory()),l.palettedContainerFactory());require(restored.state(x,y,z)==base.state(x,y,z),"profile roundtrip");require(base.top(BismuthSmoke::bismuth,x,z,base.groundHeight(x,z))==y+1,"native bismuth-only height query");l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());l.getServer().saveEverything(false,true,true);blockbranch.Blockbranch.LOGGER.info("BISMUTH native natural witness={} depth={}",pos,depth);found=true;break search;}}require(found,"native natural witness");
 }
 private BismuthSmoke(){}
}
