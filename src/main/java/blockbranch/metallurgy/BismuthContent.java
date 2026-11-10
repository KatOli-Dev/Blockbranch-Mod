package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Bismuth refines an existing steel pickaxe; it does not replace the published steel recipe or material. */
public final class BismuthContent {
    public static final Block ORE=ore("bismuth_ore",Blocks.IRON_ORE),DEEPSLATE_ORE=ore("deepslate_bismuth_ore",Blocks.DEEPSLATE_IRON_ORE);
    public static final Item RAW=item("raw_bismuth"),INGOT=item("bismuth_ingot");
    public static final Item PICKAXE=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("bismuth_steel_pickaxe"),new BismuthPickaxe(new Item.Properties().pickaxe(SteelContent.STEEL,1,-2.8F).component(DataComponents.LORE,new ItemLore(java.util.List.of(Component.translatable("item.blockbranch.bismuth_steel_pickaxe.effect")))).setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("bismuth_steel_pickaxe")))));
    private static Block ore(String name,Block base){var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;}
    private static Item item(String name){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        PayloadTypeRegistry.clientboundPlay().register(OrePulsePacket.TYPE,OrePulsePacket.CODEC);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(ORE);out.accept(DEEPSLATE_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW);out.accept(INGOT);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(PICKAXE));
    }
    private BismuthContent(){}
}
