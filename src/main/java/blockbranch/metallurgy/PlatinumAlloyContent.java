package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Rare platinum/silver alloy with native high-enchantability equipment. */
public final class PlatinumAlloyContent {
    public static final Block BLOCK=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("platinum_alloy_block"),new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id("platinum_alloy_block")))));
    public static final Item BLOCK_ITEM=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("platinum_alloy_block"),new BlockItem(BLOCK,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("platinum_alloy_block"))).useBlockDescriptionPrefix()));
    public static final Item INGOT=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("platinum_alloy_ingot"),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("platinum_alloy_ingot")))));
    public static final net.minecraft.tags.TagKey<Item> REPAIR=net.minecraft.tags.TagKey.create(Registries.ITEM,Blockbranch.id("platinum_alloy_repair_materials"));
    // High enchantability and diamond harvest access trade away diamond speed/wear life; iron attack strength.
    public static final ToolMaterial MATERIAL=new ToolMaterial(net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL,896,7,2,22,REPAIR);
    public static final Item PICKAXE=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("platinum_alloy_pickaxe"),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("platinum_alloy_pickaxe"))).pickaxe(MATERIAL,1,-2.8f)));
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->out.accept(INGOT));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->out.accept(BLOCK));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(PICKAXE));
    }
    private PlatinumAlloyContent(){}
}
