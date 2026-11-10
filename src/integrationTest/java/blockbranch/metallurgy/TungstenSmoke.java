package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
/** Native tungsten tradeoffs, recipes, actions, persistence and independently frozen prior field. */
public final class TungstenSmoke {
 public static void verify(ServerLevel level,Path directory)throws Exception{recipes(level);tools(level,directory);legacy(level,directory);natural(level,directory);}
 private static void recipes(ServerLevel level){
  var manager=level.getServer().getRecipeManager();var cells=List.of(new ItemStack(SteelContent.STEEL_INGOT),new ItemStack(SteelContent.STEEL_INGOT),new ItemStack(SteelContent.STEEL_INGOT),new ItemStack(TungstenContent.TUNGSTEN_INGOT));
  var input=CraftingInput.of(2,2,cells);var out=manager.getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);require(out.is(TungstenContent.TUNGSTEN_STEEL_INGOT)&&out.getCount()==4,"three steel + one tungsten conserves four alloy ingots");
  for(int k:new int[]{0,3}){var bad=new ArrayList<>(cells);bad.set(k,ItemStack.EMPTY);require(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(2,2,bad),level).isEmpty(),"cannot omit steel/tungsten");}
  for(var source:List.of(TungstenContent.RAW_TUNGSTEN,TungstenContent.TUNGSTEN_ORE.asItem(),TungstenContent.DEEPSLATE_TUNGSTEN_ORE.asItem())){var in=new SingleRecipeInput(new ItemStack(source));require(manager.getRecipeFor(RecipeType.SMELTING,in,level).orElseThrow().value().assemble(in).is(TungstenContent.TUNGSTEN_INGOT),"smelting tungsten");require(manager.getRecipeFor(RecipeType.BLASTING,in,level).orElseThrow().value().assemble(in).is(TungstenContent.TUNGSTEN_INGOT),"blasting tungsten");}
 }
 private static void tools(ServerLevel level,Path directory)throws Exception{
  var items=List.of(TungstenContent.TUNGSTEN_STEEL_PICKAXE,TungstenContent.TUNGSTEN_STEEL_AXE,TungstenContent.TUNGSTEN_STEEL_SHOVEL,TungstenContent.TUNGSTEN_STEEL_HOE);var tags=List.of(net.minecraft.tags.ItemTags.PICKAXES,net.minecraft.tags.ItemTags.AXES,net.minecraft.tags.ItemTags.SHOVELS,net.minecraft.tags.ItemTags.HOES);
  String[] names={"pickaxe","axe","shovel","hoe"};String[][] patterns={{"BBB"," S "," S "},{"BB","BS"," S"},{"B","S","S"},{"BB"," S"," S"}};
  var path=directory.resolve("tungsten-tools.dat");var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());var before=Files.exists(path)?net.minecraft.nbt.NbtIo.readCompressed(path,net.minecraft.nbt.NbtAccounter.unlimitedHeap()):null;var after=new net.minecraft.nbt.CompoundTag();
  for(int k=0;k<4;k++){var cells=new ArrayList<ItemStack>();for(String row:patterns[k])for(char c:row.toCharArray())cells.add(c=='B'?new ItemStack(TungstenContent.TUNGSTEN_STEEL_INGOT):c=='S'?new ItemStack(Items.STICK):ItemStack.EMPTY);var input=CraftingInput.of(patterns[k][0].length(),3,cells);var stack=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
   require(stack.is(items.get(k))&&stack.is(tags.get(k))&&stack.getMaxDamage()==2048,"native tool recipe/tag/durability");require(stack.isValidRepairItem(new ItemStack(TungstenContent.TUNGSTEN_STEEL_INGOT))&&!stack.isValidRepairItem(new ItemStack(SteelContent.STEEL_INGOT)),"alloy-only repair");
   if(before!=null){var old=ItemStack.CODEC.parse(ops,before.getCompoundOrEmpty(names[k])).getOrThrow();require(old.is(items.get(k))&&old.getDamageValue()==37,"tool stack identity/damage persists");}stack.setDamageValue(37);after.put(names[k],ItemStack.CODEC.encodeStart(ops,stack).getOrThrow());
  }net.minecraft.nbt.NbtIo.writeCompressed(after,path);
  var pick=new ItemStack(items.getFirst());var iron=new ItemStack(Items.IRON_PICKAXE);var diamond=new ItemStack(Items.DIAMOND_PICKAXE);var steel=new ItemStack(SteelContent.STEEL_PICKAXE);
  require(pick.getMaxDamage()>diamond.getMaxDamage()&&pick.getDestroySpeed(Blocks.STONE.defaultBlockState())<iron.getDestroySpeed(Blocks.STONE.defaultBlockState())&&pick.getDestroySpeed(Blocks.STONE.defaultBlockState())<steel.getDestroySpeed(Blocks.STONE.defaultBlockState()),"long life trades away mining speed");require(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"diamond harvest access");
  var silk=pick.copy();silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
  for(var ore:List.of(TungstenContent.TUNGSTEN_ORE,TungstenContent.DEEPSLATE_TUNGSTEN_ORE)){require(!iron.isCorrectToolForDrops(ore.defaultBlockState())&&!steel.isCorrectToolForDrops(ore.defaultBlockState())&&diamond.isCorrectToolForDrops(ore.defaultBlockState())&&pick.isCorrectToolForDrops(ore.defaultBlockState()),"tungsten requires diamond");require(ore.defaultBlockState().getDestroySpeed(level,BlockPos.ZERO)>Blocks.DEEPSLATE_IRON_ORE.defaultBlockState().getDestroySpeed(level,BlockPos.ZERO),"tungsten is hard to mine");var drops=Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,pick);require(drops.size()==1&&drops.getFirst().is(TungstenContent.RAW_TUNGSTEN),"raw tungsten drop");require(Block.getDrops(ore.defaultBlockState(),level,BlockPos.ZERO,null,null,silk).getFirst().is(ore.asItem()),"silk touch");}
  var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TungstenActions");var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,224,0)+4;Block[] sources={Blocks.OAK_LOG,Blocks.GRASS_BLOCK,Blocks.GRASS_BLOCK},results={Blocks.STRIPPED_OAK_LOG,Blocks.DIRT_PATH,Blocks.FARMLAND};
  for(int k=0;k<3;k++){var pos=new BlockPos(224+k,y,0);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,sources[k].defaultBlockState(),2);var tool=new ItemStack(items.get(k+1));player.getInventory().setItem(0,tool);player.getInventory().setSelectedSlot(0);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));require(level.getBlockState(pos).is(results[k])&&tool.getDamageValue()==1,"native tool action/durability");level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));require(level.getBlockState(pos).is(Blocks.STONE)&&tool.getDamageValue()==1,"negative action control");}
  Blockbranch.LOGGER.info("TUNGSTEN tools, recipes, durability/speed tradeoff, gates, native actions and stack reload={}",before!=null);
 }
    private static void legacy(ServerLevel level,Path directory)throws Exception{
        var url=Path.of(System.getProperty("blockbranch.oldSteelCatalogClasses")).toUri().toURL();long checked=0;int tungsten=0;
        try(var loader=MetallurgySmoke.historicalReader(url)){
            var old=loader.loadClass("blockbranch.realistic.OreLayer");var ctor=old.getConstructor(long.class,int.class,int.class,int.class,int.class,int.class);var bind=old.getMethod("bind",int.class,int.class);var state=old.getMethod("state",int.class,int.class,int.class,int.class,net.minecraft.world.level.block.state.BlockState.class,boolean.class);
            for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1})for(boolean caves:new boolean[]{false,true}){
                var frozen=bind.invoke(ctor.newInstance(seed,cx,-1,5,65535,65535),64,64);var retained=new OreLayer(seed,cx,-1,5,65535,65535).bind(64,64);var current=new OreLayer(seed,cx,-1,6,65535,65535).bind(64,64);
                for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=224;depth+=2){
                    int y=64-depth;var host=(y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();var expected=state.invoke(frozen,x,y,z,64,host,caves);require(retained.state(x,y,z,64,host,caves)==expected,"published steel definition5 remains exact");var next=current.state(x,y,z,64,host,caves);
                    if(expected!=host)require(next==expected,"tungsten cannot replace prior ores, tin or zinc");else if(next!=host){require(next.is(TungstenContent.TUNGSTEN_ORE)||next.is(TungstenContent.DEEPSLATE_TUNGSTEN_ORE),"only tungsten added to remaining hosts");tungsten++;}checked++;
                }
            }
            try{old.getMethod("decode",net.minecraft.nbt.CompoundTag.class).invoke(null,new OreLayer(1,0,0,6,0,0).encode());throw new AssertionError("old ore reader accepted6");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalArgumentException,"prior steel reader safely rejects6");}
            var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);
            for(boolean decoration:new boolean[]{false,true}){
                var root=Files.createTempDirectory(directory,"old-steel-guard-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);
                try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("catalog5 reader accepted6");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"previous steel reader rejects catalog6");}require(Files.readString(marker).equals(before),"older marker rejection leaves data untouched");
            }
        }
        require(tungsten>0,"positive definition6 tungsten witnesses");Blockbranch.LOGGER.info("TUNGSTEN frozenSteelComparisons={} tungstenWitnesses={} oldReaderRejects6=true",checked,tungsten);
    }
    private static void natural(ServerLevel level,Path directory)throws Exception{
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator))return;
        if(generator.oreDefinition()<6){require((generator.oreDefinition()>=3&&generator.oreDefinition()<=5),"retained prior tin world definition");Blockbranch.LOGGER.info("TUNGSTEN_STEEL legacy world keeps prior ore definition; no tungsten backfill");return;}
        require(generator.oreDefinition()>=6,"current tungsten generation definition");var marker=directory.resolve("tungsten-mined.txt");
        if(Files.exists(marker)){
            var values=Files.readString(marker).trim().split(",");var pos=new BlockPos(Integer.parseInt(values[0]),Integer.parseInt(values[1]),Integer.parseInt(values[2]));require(level.getBlockState(pos).isAir(),"natural tungsten mining edit survives relaunch");var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();require(isTungsten(base.state(pos.getX(),pos.getY(),pos.getZ())),"saved tungsten remains in procedural base");return;
        }
        var origin=generator.getOrigin(level.getChunkSource().randomState());
        for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++){
            var chunk=level.getChunk(origin.x()+dx,origin.z()+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();require(base.ores().definition()==generator.oreDefinition(),"actual FULL column carries its saved ore definition");var restored=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=TungstenVeins.MIN_DEPTH;depth<=TungstenVeins.MAX_DEPTH;depth++){
                int y=base.groundHeight(x,z)-depth;var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);var value=base.state(x,y,z);if(!isTungsten(value))continue;
                require(level.getBlockState(pos)==value&&restored.state(x,y,z)==value,"native tungsten agrees with snapshot");level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);Files.writeString(marker,pos.getX()+","+pos.getY()+","+pos.getZ());level.getServer().saveEverything(false,true,true);Blockbranch.LOGGER.info("TUNGSTEN_STEEL naturally generated tungsten witness={}",pos);return;
            }
        }
        throw new AssertionError("no natural tungsten in four native columns");
    }
    private static boolean isTungsten(net.minecraft.world.level.block.state.BlockState state){return state.is(TungstenContent.TUNGSTEN_ORE)||state.is(TungstenContent.DEEPSLATE_TUNGSTEN_ORE);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private TungstenSmoke(){}
}
