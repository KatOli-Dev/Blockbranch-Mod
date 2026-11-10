package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
/** Native stainless foundation, persistent building states and independently frozen prior field. */
public final class StainlessSmoke {
 public static void verify(ServerLevel level,Path directory)throws Exception{recipes(level);dropsAndPick(level,directory);placement(level);edits(level,directory);legacy(level,directory);natural(level,directory);}
 private static void recipes(ServerLevel level){
  var manager=level.getServer().getRecipeManager();var cells=new ArrayList<ItemStack>();for(int k=0;k<6;k++)cells.add(new ItemStack(SteelContent.STEEL_INGOT));cells.add(new ItemStack(StainlessContent.CHROMIUM_INGOT));cells.add(new ItemStack(StainlessContent.CHROMIUM_INGOT));cells.add(new ItemStack(StainlessContent.NICKEL_INGOT));var input=CraftingInput.of(3,3,cells);var out=manager.getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);require(out.is(StainlessContent.STAINLESS_STEEL_INGOT)&&out.getCount()==9,"6 steel + 2 chromium + 1 nickel conserves 9 ingots");
  for(int k:new int[]{0,6,8}){var bad=new ArrayList<>(cells);bad.set(k,ItemStack.EMPTY);require(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,bad),level).isEmpty(),"cannot omit steel/chromium/nickel");}
  String[][] shapes={{"BBB","BBB","BBB"},{"BBB"},{"B  ","BB ","BBB"},{"BBB"," S "," S "}};var sources=List.of(StainlessContent.STAINLESS_STEEL_INGOT,StainlessContent.STAINLESS_STEEL_BLOCK.asItem(),StainlessContent.STAINLESS_STEEL_BLOCK.asItem(),StainlessContent.STAINLESS_STEEL_INGOT);var outputs=List.of(StainlessContent.STAINLESS_STEEL_BLOCK.asItem(),StainlessContent.STAINLESS_STEEL_SLAB.asItem(),StainlessContent.STAINLESS_STEEL_STAIRS.asItem(),StainlessContent.STAINLESS_STEEL_PICKAXE);int[] counts={1,6,4,1};
  for(int k=0;k<4;k++){var grid=new ArrayList<ItemStack>();for(String row:shapes[k])for(char c:row.toCharArray())grid.add(c=='B'?new ItemStack(sources.get(k)):c=='S'?new ItemStack(Items.STICK):ItemStack.EMPTY);var in=CraftingInput.of(3,shapes[k].length,grid);var result=manager.getRecipeFor(RecipeType.CRAFTING,in,level).orElseThrow().value().assemble(in);require(result.is(outputs.get(k))&&result.getCount()==counts[k],"stainless building/tool recipe counts");}
  var unpack=CraftingInput.of(1,1,List.of(new ItemStack(StainlessContent.STAINLESS_STEEL_BLOCK)));var ingots=manager.getRecipeFor(RecipeType.CRAFTING,unpack,level).orElseThrow().value().assemble(unpack);require(ingots.is(StainlessContent.STAINLESS_STEEL_INGOT)&&ingots.getCount()==9,"reversible stainless storage");
  for(var row:List.of(List.of(StainlessContent.RAW_NICKEL,StainlessContent.NICKEL_ORE.asItem(),StainlessContent.DEEPSLATE_NICKEL_ORE.asItem(),StainlessContent.NICKEL_INGOT),List.of(StainlessContent.RAW_CHROMIUM,StainlessContent.CHROMIUM_ORE.asItem(),StainlessContent.DEEPSLATE_CHROMIUM_ORE.asItem(),StainlessContent.CHROMIUM_INGOT)))for(int k=0;k<3;k++){var in=new SingleRecipeInput(new ItemStack(row.get(k)));require(manager.getRecipeFor(RecipeType.SMELTING,in,level).orElseThrow().value().assemble(in).is(row.get(3)),"smelting base metal");require(manager.getRecipeFor(RecipeType.BLASTING,in,level).orElseThrow().value().assemble(in).is(row.get(3)),"blasting base metal");}
 }
 private static void dropsAndPick(ServerLevel level,Path directory)throws Exception{
  var pick=new ItemStack(StainlessContent.STAINLESS_STEEL_PICKAXE);var steel=new ItemStack(SteelContent.STEEL_PICKAXE);var diamond=new ItemStack(Items.DIAMOND_PICKAXE);require(pick.getMaxDamage()==960&&pick.getMaxDamage()>steel.getMaxDamage()&&pick.getMaxDamage()<diamond.getMaxDamage(),"stainless wear life between steel and diamond");require(pick.getDestroySpeed(Blocks.STONE.defaultBlockState())<steel.getDestroySpeed(Blocks.STONE.defaultBlockState()),"wear life trades away steel speed");require(pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())&&!pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())&&pick.is(net.minecraft.tags.ItemTags.PICKAXES),"iron harvest access and native tool/enchantment tag");require(pick.isValidRepairItem(new ItemStack(StainlessContent.STAINLESS_STEEL_INGOT))&&!pick.isValidRepairItem(new ItemStack(SteelContent.STEEL_INGOT)),"own alloy repair");
  var silk=pick.copy();silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
  for(var row:List.of(List.of(StainlessContent.NICKEL_ORE,StainlessContent.DEEPSLATE_NICKEL_ORE),List.of(StainlessContent.CHROMIUM_ORE,StainlessContent.DEEPSLATE_CHROMIUM_ORE)))for(var ore:row){require(pick.isCorrectToolForDrops(ore.defaultBlockState())&&!new ItemStack(MetallurgyContent.BRONZE_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()),"iron harvest gate for new ores");var drops=Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,pick);require(drops.size()==1&&drops.getFirst().is(row.getFirst()==StainlessContent.NICKEL_ORE?StainlessContent.RAW_NICKEL:StainlessContent.RAW_CHROMIUM),"base metal raw drop");require(Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,silk).getFirst().is(ore.asItem()),"base metal silk touch");}
  var slab=StainlessContent.STAINLESS_STEEL_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE);var drops=Block.getDrops(slab,level,BlockPos.ZERO,null,null,pick);require(drops.size()==1&&drops.getFirst().is(StainlessContent.STAINLESS_STEEL_SLAB.asItem())&&drops.getFirst().getCount()==2,"double slab drops two");for(var b:List.of(StainlessContent.STAINLESS_STEEL_BLOCK,StainlessContent.STAINLESS_STEEL_STAIRS))require(Block.getDrops(b.defaultBlockState(),level,BlockPos.ZERO,null,null,pick).getFirst().is(b.asItem()),"building self drop");
  var file=directory.resolve("stainless-pick.dat");var ops=RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,level.registryAccess());if(Files.exists(file)){var restored=ItemStack.CODEC.parse(ops,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap())).getOrThrow();require(restored.is(StainlessContent.STAINLESS_STEEL_PICKAXE)&&restored.getDamageValue()==37,"pick identity and damage survive relaunch");}pick.setDamageValue(37);net.minecraft.nbt.NbtIo.writeCompressed((net.minecraft.nbt.CompoundTag)ItemStack.CODEC.encodeStart(ops,pick).getOrThrow(),file);
 }
    private static void placement(ServerLevel level){
        var player=new net.minecraft.world.entity.player.Player(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"StainlessBuilder")){
            @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.CREATIVE;}
        };player.getInventory().setSelectedSlot(0);player.setYRot(90);
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,304,0)+4;
        var slab=new BlockPos(304,y,0);var stair=new BlockPos(306,y,0);var left=new BlockPos(308,y,0);var right=left.east();
        for(var pos:List.of(slab,stair,left,right)){level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        place(level,player,slab.below(),StainlessContent.STAINLESS_STEEL_SLAB);require(level.getBlockState(slab).getValue(SlabBlock.TYPE)==SlabType.BOTTOM,"native lower slab placement");
        place(level,player,slab,StainlessContent.STAINLESS_STEEL_SLAB);require(level.getBlockState(slab).getValue(SlabBlock.TYPE)==SlabType.DOUBLE,"native slab combination");
        level.setBlock(stair,Blocks.WATER.defaultBlockState(),2);place(level,player,stair.below(),StainlessContent.STAINLESS_STEEL_STAIRS);
        require(level.getBlockState(stair).is(StainlessContent.STAINLESS_STEEL_STAIRS)&&level.getBlockState(stair).getValue(StairBlock.FACING)==Direction.WEST&&level.getBlockState(stair).getValue(StairBlock.WATERLOGGED),"native stair direction/waterlogging");
    }
    private static void place(ServerLevel level,net.minecraft.world.entity.player.Player player,BlockPos support,Block block){
        var stack=new ItemStack(block);player.getInventory().setItem(0,stack);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
    }
    private static void edits(ServerLevel level,Path directory)throws Exception{
        var marker=directory.resolve("stainless-edits.txt");int y;
        if(Files.exists(marker))y=Integer.parseInt(Files.readString(marker).trim());else{
            y=level.getHeight(Heightmap.Types.WORLD_SURFACE,288,0)+4;
            level.setBlock(new BlockPos(288,y,0),StainlessContent.STAINLESS_STEEL_BLOCK.defaultBlockState(),2);
            level.setBlock(new BlockPos(289,y,0),StainlessContent.STAINLESS_STEEL_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),2);
            level.setBlock(new BlockPos(290,y,0),StainlessContent.STAINLESS_STEEL_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.WEST).setValue(StairBlock.WATERLOGGED,true),2);Files.writeString(marker,Integer.toString(y));level.getServer().saveEverything(false,true,true);
        }
        require(level.getBlockState(new BlockPos(288,y,0)).is(StainlessContent.STAINLESS_STEEL_BLOCK),"saved stainless block");var slab=level.getBlockState(new BlockPos(289,y,0));require(slab.is(StainlessContent.STAINLESS_STEEL_SLAB)&&slab.getValue(SlabBlock.TYPE)==SlabType.DOUBLE,"saved double slab");
        var stair=level.getBlockState(new BlockPos(290,y,0));require(stair.is(StainlessContent.STAINLESS_STEEL_STAIRS)&&stair.getValue(StairBlock.FACING)==Direction.WEST&&stair.getValue(StairBlock.WATERLOGGED),"saved stair orientation and waterlogging");
    }
 private static void legacy(ServerLevel level,Path directory)throws Exception{
  long checked=0;int nickel=0,chromium=0;var predicate=(java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState>)(s->s.is(StainlessContent.NICKEL_ORE)||s.is(StainlessContent.CHROMIUM_ORE));require(!new OreLayer(1,0,0,6,0,0).maybeHas(predicate)&&new OreLayer(1,0,0,7,0,0).maybeHas(predicate)&&new OreLayer(1,0,0,7,0,0).affectsPredicate(predicate),"new ores participate only in current predicates");
  try(var loader=MetallurgySmoke.historicalReader(Path.of(System.getProperty("blockbranch.oldTungstenCatalogClasses")).toUri().toURL())){
   var old=loader.loadClass("blockbranch.realistic.OreLayer");var ctor=old.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);var bind=old.getMethod("bind",int.class,int.class);var state=old.getMethod("state",int.class,int.class,int.class,int.class,net.minecraft.world.level.block.state.BlockState.class,boolean.class);
   for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1})for(boolean caves:new boolean[]{false,true}){
    var frozen=bind.invoke(ctor.newInstance(seed,cx,-1,6,65535,65535),64,64);var retained=new OreLayer(seed,cx,-1,6,65535,65535).bind(64,64);var current=new OreLayer(seed,cx,-1,7,65535,65535).bind(64,64);
    for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=state.invoke(frozen,x,y,z,64,host,caves);require(retained.state(x,y,z,64,host,caves)==expected,"published tungsten field6 remains exact");var next=current.state(x,y,z,64,host,caves);if(expected!=host)require(next==expected,"new ores cannot replace existing materials");else if(next!=host){if(isNickel(next))nickel++;else{require(isChromium(next),"only nickel/chromium added");chromium++;}}checked++;}
   }
   try{old.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,7,0,0).encode());throw new AssertionError("old reader accepted7");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"prior tungsten reader safely rejects7");}
   var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);for(boolean decoration:new boolean[]{false,true}){var root=Files.createTempDirectory(directory,"old-tungsten-guard-");ColumnWorldFormat.check(root,false,decoration,true);var path=root.resolve("blockbranch-columns.txt");String before=Files.readString(path);try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("catalog6 reader accepted7");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"prior tungsten reader rejects catalog7");}require(Files.readString(path).equals(before),"older rejection leaves marker untouched");}
  }
  require(nickel>0&&chromium>0,"both new ores have positive witnesses");Blockbranch.LOGGER.info("STAINLESS frozenTungstenComparisons={} nickel={} chromium={} oldReaderRejects7=true",checked,nickel,chromium);
 }
 private static void natural(ServerLevel level,Path directory)throws Exception{
  if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator))return;
  if(generator.oreDefinition()<7){require(generator.oreDefinition()>=3&&generator.oreDefinition()<=6,"retained prior world");Blockbranch.LOGGER.info("STAINLESS prior ore definition retained; no nickel/chromium backfill");return;}
  require(generator.oreDefinition()>=7,"fresh stainless generation definition");var origin=generator.getOrigin(level.getChunkSource().randomState());
  for(boolean nickel:new boolean[]{true,false}){String name=nickel?"nickel":"chromium";var marker=directory.resolve(name+"-mined.txt");if(Files.exists(marker)){var v=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]));require(level.getBlockState(pos).isAir(),"natural base-metal mining edit survives relaunch");var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();require(nickel?isNickel(base.state(pos.getX(),pos.getY(),pos.getZ())):isChromium(base.state(pos.getX(),pos.getY(),pos.getZ())),"saved base retains natural ore");continue;}
   boolean found=false;search:for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++){var chunk=level.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();require(base.ores().definition()==generator.oreDefinition(),"actual FULL column retains generator descriptor");var restored=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=48;depth<=224;depth++){int y=base.groundHeight(x,z)-depth;var value=base.state(x,y,z);if(!(nickel?isNickel(value):isChromium(value)))continue;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);require(level.getBlockState(pos)==value&&restored.state(x,y,z)==value,"native ore agrees with profile roundtrip");level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());level.getServer().saveEverything(false,true,true);Blockbranch.LOGGER.info("STAINLESS natural {} witness={}",name,pos);found=true;break search;}}
   require(found,"natural base-metal witness in four columns: "+name);
  }
 }
 private static boolean isNickel(net.minecraft.world.level.block.state.BlockState s){return s.is(StainlessContent.NICKEL_ORE)||s.is(StainlessContent.DEEPSLATE_NICKEL_ORE);}
 private static boolean isChromium(net.minecraft.world.level.block.state.BlockState s){return s.is(StainlessContent.CHROMIUM_ORE)||s.is(StainlessContent.DEEPSLATE_CHROMIUM_ORE);}
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 private StainlessSmoke(){}
}
