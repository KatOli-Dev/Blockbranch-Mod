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
import net.minecraft.world.level.levelgen.Heightmap;
/** Native titanium_alloy hand-tool actions, combat strength and saved stack qualification. */
public final class TitaniumGearSmoke {
 public static void verify(ServerLevel level,Path directory)throws Exception{tools(level,directory);pick(level,directory);try(var loader=MetallurgySmoke.historicalReader(Path.of(System.getProperty("blockbranch.oldTitaniumCatalogClasses")).toUri().toURL())){var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);for(boolean decoration:new boolean[]{false,true}){var root=Files.createTempDirectory(directory,"old-titanium_alloy-guard-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("prior catalog15 reader accepted16");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"prior foundation reader rejects equipment16");}require(Files.readString(marker).equals(before),"old reader rejection preserves marker");}}}
    private static void tools(ServerLevel level,Path directory)throws Exception{
        var tools=List.of(TitaniumEquipment.TITANIUM_ALLOY_SWORD,TitaniumEquipment.TITANIUM_ALLOY_AXE,TitaniumEquipment.TITANIUM_ALLOY_SHOVEL,TitaniumEquipment.TITANIUM_ALLOY_HOE);
        var tags=List.of(net.minecraft.tags.ItemTags.SWORDS,net.minecraft.tags.ItemTags.AXES,net.minecraft.tags.ItemTags.SHOVELS,net.minecraft.tags.ItemTags.HOES);
        String[] names={"sword","axe","shovel","hoe"};String[][] patterns={{"B","B","S"},{"BB","BS"," S"},{"B","S","S"},{"BB"," S"," S"}};
        var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());var saved=directory.resolve("titanium_alloy-tools.dat");
        var previous=Files.exists(saved)?net.minecraft.nbt.NbtIo.readCompressed(saved,net.minecraft.nbt.NbtAccounter.unlimitedHeap()):null;
        var next=new net.minecraft.nbt.CompoundTag();
        for(int i=0;i<tools.size();i++){
            var cells=new ArrayList<ItemStack>();for(String row:patterns[i])for(char ch:row.toCharArray())cells.add(ch=='B'?new ItemStack(TitaniumContent.ALLOY_INGOT):ch=='S'?new ItemStack(Items.STICK):ItemStack.EMPTY);
            var input=CraftingInput.of(patterns[i][0].length(),patterns[i].length,cells);
            var output=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
            require(output.is(tools.get(i))&&output.is(tags.get(i)),"native titanium_alloy "+names[i]+" recipe and tool/enchantment tag");
            require(output.getMaxDamage()==1024&&output.get(net.minecraft.core.component.DataComponents.ENCHANTABLE).value()==12&&output.isValidRepairItem(new ItemStack(TitaniumContent.ALLOY_INGOT))&&!output.isValidRepairItem(new ItemStack(SteelContent.STEEL_INGOT)),"shared titanium_alloy durability/repair");
            if(previous!=null){var restored=ItemStack.CODEC.parse(ops,previous.getCompoundOrEmpty(names[i])).getOrThrow();require(restored.is(tools.get(i))&&restored.getDamageValue()==37,"saved tool identity and damage survive relaunch");}
            output.setDamageValue(37);next.put(names[i],ItemStack.CODEC.encodeStart(ops,output).getOrThrow());
        }
        net.minecraft.nbt.NbtIo.writeCompressed(next,saved);
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SilverActions");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,48,0)+4;
        Block[] sources={Blocks.OAK_LOG,Blocks.GRASS_BLOCK,Blocks.GRASS_BLOCK};Block[] results={Blocks.STRIPPED_OAK_LOG,Blocks.DIRT_PATH,Blocks.FARMLAND};
        for(int i=0;i<3;i++){
            var pos=new BlockPos(48+i,y,0);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,sources[i].defaultBlockState(),2);
            var tool=new ItemStack(tools.get(i+1));player.getInventory().setItem(0,tool);player.getInventory().setSelectedSlot(0);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);
            tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            require(level.getBlockState(pos).is(results[i])&&tool.getDamageValue()==1,"native "+names[i+1]+" action and durability");
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            require(level.getBlockState(pos).is(Blocks.STONE)&&tool.getDamageValue()==1,"unsupported action leaves blocks and durability unchanged");
        }
        var sword=new ItemStack(TitaniumEquipment.TITANIUM_ALLOY_SWORD);player.getInventory().setItem(0,sword);
        var target=net.minecraft.world.entity.EntityTypes.PIG.create(level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        require(target!=null&&sword.hurtEnemy(target,player),"native sword weapon component");sword.postHurtEnemy(target,player);require(sword.getDamageValue()==1,"native sword hit durability");
        var attribute=net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;var slot=net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        double titanium_alloy=sword.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double iron=new ItemStack(Items.IRON_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double diamond=new ItemStack(Items.DIAMOND_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        require(titanium_alloy==iron&&titanium_alloy<diamond,"titanium_alloy retains iron attack strength and diamond advantage");
        Blockbranch.LOGGER.info("TITANIUM_ALLOY_TOOLS native recipes/actions/negative controls/sword durability and saved-stack reload={}",previous!=null);
    }
 private static void pick(ServerLevel level,Path directory)throws Exception{
  var pick=new ItemStack(TitaniumEquipment.TITANIUM_ALLOY_PICKAXE);var diamond=new ItemStack(Items.DIAMOND_PICKAXE);var iron=new ItemStack(Items.IRON_PICKAXE);var stone=Blocks.STONE.defaultBlockState();require(pick.getMaxDamage()==1024&&pick.getMaxDamage()<diamond.getMaxDamage()&&pick.getDestroySpeed(stone)>iron.getDestroySpeed(stone)&&pick.getDestroySpeed(stone)<diamond.getDestroySpeed(stone),"titanium mining tradeoff preserves diamond advantage");require(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())&&pick.is(net.minecraft.tags.ItemTags.PICKAXES)&&pick.get(net.minecraft.core.component.DataComponents.ENCHANTABLE).value()==12,"native diamond access/tag/enchantability");require(pick.isValidRepairItem(new ItemStack(TitaniumContent.ALLOY_INGOT))&&!pick.isValidRepairItem(new ItemStack(TitaniumContent.INGOT)),"pick needs alloy repair");
  var grid=List.of(new ItemStack(TitaniumContent.ALLOY_INGOT),new ItemStack(TitaniumContent.ALLOY_INGOT),new ItemStack(TitaniumContent.ALLOY_INGOT),ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY);var in=CraftingInput.of(3,3,grid);require(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,in,level).orElseThrow().value().assemble(in).is(TitaniumEquipment.TITANIUM_ALLOY_PICKAXE),"native pick recipe");var bad=new ArrayList<>(grid);bad.set(1,ItemStack.EMPTY);require(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,bad),level).isEmpty(),"pick cannot omit head material");
  for(var tool:List.of(TitaniumEquipment.TITANIUM_ALLOY_PICKAXE,TitaniumEquipment.TITANIUM_ALLOY_SWORD,TitaniumEquipment.TITANIUM_ALLOY_AXE,TitaniumEquipment.TITANIUM_ALLOY_SHOVEL,TitaniumEquipment.TITANIUM_ALLOY_HOE))require(new ItemStack(tool).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,.1,net.minecraft.world.entity.EquipmentSlot.MAINHAND)==.1,"held tools add no mobility");
  pick.setDamageValue(37);pick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY),3);var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());var path=directory.resolve("titanium-pick.dat");if(Files.exists(path)){var restored=ItemStack.CODEC.parse(ops,net.minecraft.nbt.NbtIo.readCompressed(path,net.minecraft.nbt.NbtAccounter.unlimitedHeap())).getOrThrow();require(ItemStack.isSameItemSameComponents(pick,restored),"saved pick identity/wear/enchant persist");}net.minecraft.nbt.NbtIo.writeCompressed((net.minecraft.nbt.CompoundTag)ItemStack.CODEC.encodeStart(ops,pick).getOrThrow(),path);
 }
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 private TitaniumGearSmoke(){}
}
