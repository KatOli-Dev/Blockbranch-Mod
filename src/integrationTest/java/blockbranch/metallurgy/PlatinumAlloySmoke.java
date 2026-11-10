package blockbranch.metallurgy;

import blockbranch.cubic.ColumnWorldFormat;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native alloy conservation, pick tradeoffs/enchanting and versioned saved-state qualification. */
public final class PlatinumAlloySmoke {
    public static void verify(ServerLevel level,Path directory)throws Exception{
        recipes(level);pick(level,directory);storage(level,directory);oldReader(directory);
    }
    private static void recipes(ServerLevel level){
        var manager=level.getServer().getRecipeManager();var cells=new ArrayList<ItemStack>();
        for(int i=0;i<8;i++)cells.add(new ItemStack(PreciousContent.PLATINUM_INGOT));cells.add(new ItemStack(PreciousContent.SILVER_INGOT));
        var input=CraftingInput.of(3,3,cells);var output=manager.getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
        require(output.is(PlatinumAlloyContent.INGOT)&&output.getCount()==9,"8 platinum + 1 silver conserves nine alloy ingots");
        for(int index:new int[]{0,8}){var bad=new ArrayList<>(cells);bad.set(index,ItemStack.EMPTY);require(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,bad),level).isEmpty(),"cannot omit platinum or silver");}
        var packed=CraftingInput.of(3,3,java.util.stream.IntStream.range(0,9).mapToObj(i->new ItemStack(PlatinumAlloyContent.INGOT)).toList());
        require(manager.getRecipeFor(RecipeType.CRAFTING,packed,level).orElseThrow().value().assemble(packed).is(PlatinumAlloyContent.BLOCK_ITEM),"native storage recipe");
        var unpack=CraftingInput.of(1,1,List.of(new ItemStack(PlatinumAlloyContent.BLOCK)));var ingots=manager.getRecipeFor(RecipeType.CRAFTING,unpack,level).orElseThrow().value().assemble(unpack);
        require(ingots.is(PlatinumAlloyContent.INGOT)&&ingots.getCount()==9,"reversible alloy storage");
        var head=List.of(new ItemStack(PlatinumAlloyContent.INGOT),new ItemStack(PlatinumAlloyContent.INGOT),new ItemStack(PlatinumAlloyContent.INGOT),ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY);
        var tool=CraftingInput.of(3,3,head);require(manager.getRecipeFor(RecipeType.CRAFTING,tool,level).orElseThrow().value().assemble(tool).is(PlatinumAlloyContent.PICKAXE),"native alloy pick recipe");
        var missing=new ArrayList<>(head);missing.set(1,ItemStack.EMPTY);require(manager.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,missing),level).isEmpty(),"pick requires complete head");
    }
    private static void pick(ServerLevel level,Path directory)throws Exception{
        var pick=new ItemStack(PlatinumAlloyContent.PICKAXE);var diamond=new ItemStack(Items.DIAMOND_PICKAXE);var iron=new ItemStack(Items.IRON_PICKAXE);
        require(pick.is(net.minecraft.tags.ItemTags.PICKAXES)&&pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())&&pick.isCorrectToolForDrops(PreciousContent.PLATINUM_ORE.defaultBlockState()),"diamond harvest access and native tool tag");
        require(pick.getMaxDamage()==896&&pick.getMaxDamage()<diamond.getMaxDamage()&&pick.getDestroySpeed(Blocks.STONE.defaultBlockState())<diamond.getDestroySpeed(Blocks.STONE.defaultBlockState()),"diamond keeps wear life and speed advantages");
        var attribute=net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;var slot=net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        require(pick.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot)==iron.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot),"iron attack strength");
        require(pick.isValidRepairItem(new ItemStack(PlatinumAlloyContent.INGOT))&&!pick.isValidRepairItem(new ItemStack(PreciousContent.PLATINUM_INGOT))&&!pick.isValidRepairItem(new ItemStack(PreciousContent.SILVER_INGOT)),"only alloy repairs equipment");
        require(pick.isEnchantable()&&pick.get(DataComponents.ENCHANTABLE).value()==22&&pick.get(DataComponents.ENCHANTABLE).value()>diamond.get(DataComponents.ENCHANTABLE).value(),"native enchantability exceeds diamond");
        var registry=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for(var key:List.of(Enchantments.EFFICIENCY,Enchantments.FORTUNE,Enchantments.SILK_TOUCH))require(registry.getOrThrow(key).value().canEnchant(pick),"native pick enchantment eligibility");
        var table=registry.getOrThrow(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE);
        var enchanted=EnchantmentHelper.enchantItem(net.minecraft.util.RandomSource.create(424242),pick.copy(),30,table.stream());
        require(enchanted.is(PlatinumAlloyContent.PICKAXE)&&enchanted.isEnchanted(),"native table enchantment selection");
        require(!new ItemStack(PlatinumAlloyContent.BLOCK).isEnchantable(),"storage has no equipment enchanting behavior");
        var silk=pick.copy();silk.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH),1);
        var ore=PreciousContent.DEEPSLATE_PLATINUM_ORE.defaultBlockState();require(Block.getDrops(ore,level,BlockPos.ZERO,null,null,pick).getFirst().is(PreciousContent.RAW_PLATINUM)&&Block.getDrops(ore,level,BlockPos.ZERO,null,null,silk).getFirst().is(PreciousContent.DEEPSLATE_PLATINUM_ORE.asItem()),"native harvest and silk drops");
        enchanted.setDamageValue(37);var ops=RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,level.registryAccess());var path=directory.resolve("platinum-alloy-pick.dat");boolean reload=Files.exists(path);
        if(reload){var restored=ItemStack.CODEC.parse(ops,net.minecraft.nbt.NbtIo.readCompressed(path,net.minecraft.nbt.NbtAccounter.unlimitedHeap())).getOrThrow();require(ItemStack.isSameItemSameComponents(enchanted,restored),"identity, wear and selected enchants survive relaunch");}
        net.minecraft.nbt.NbtIo.writeCompressed((net.minecraft.nbt.CompoundTag)ItemStack.CODEC.encodeStart(ops,enchanted).getOrThrow(),path);
        blockbranch.Blockbranch.LOGGER.info("PLATINUM_ALLOY native recipes/harvest/enchantability/selectedEnchants={} reload={}",enchanted.getEnchantments(),reload);
    }
    private static void storage(ServerLevel level,Path directory)throws Exception{
        var marker=directory.resolve("platinum-alloy-storage.txt");boolean prior=Files.exists(marker);int y=prior?Integer.parseInt(Files.readString(marker).trim()):level.getHeight(Heightmap.Types.WORLD_SURFACE,512,0)+4;var pos=new BlockPos(512,y,0);
        if(!prior){
            var player=new net.minecraft.world.entity.player.Player(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PlatinumBuilder")){@Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.CREATIVE;}};
            level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);var stack=new ItemStack(PlatinumAlloyContent.BLOCK);player.getInventory().setSelectedSlot(0);player.getInventory().setItem(0,stack);
            stack.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false)));
            require(level.getBlockState(pos).is(PlatinumAlloyContent.BLOCK),"native storage placement");Files.writeString(marker,Integer.toString(y));level.getServer().saveEverything(false,true,true);
        }
        require(level.getBlockState(pos).is(PlatinumAlloyContent.BLOCK),"saved storage block retained");
        var drops=Block.getDrops(PlatinumAlloyContent.BLOCK.defaultBlockState(),level,pos,null,null,new ItemStack(PlatinumAlloyContent.PICKAXE));require(drops.size()==1&&drops.getFirst().is(PlatinumAlloyContent.BLOCK_ITEM),"storage self drop");
    }
    private static void oldReader(Path directory)throws Exception{
        try(var loader=MetallurgySmoke.historicalReader(Path.of(System.getProperty("blockbranch.oldSilverCatalogClasses")).toUri().toURL())){
            var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);
            for(boolean decoration:new boolean[]{false,true}){var root=Files.createTempDirectory(directory,"old-silver-guard-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);
                try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("catalog12 reader accepted13");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"prior reader safely refuses new alloy catalog");}
                require(Files.readString(marker).equals(before),"old rejection preserves marker");
            }
        }
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PlatinumAlloySmoke(){}
}
