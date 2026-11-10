package blockbranch.realistic;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class DesertContent {
    public static final DesertPavementBlock PAVEMENT=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("desert_pavement"),
        new DesertPavementBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id("desert_pavement"))).mapColor(MapColor.TERRACOTTA_BROWN).strength(1F,3F).sound(SoundType.GRAVEL)));
    public static final Item PAVEMENT_ITEM=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("desert_pavement"),
        new BlockItem(PAVEMENT,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("desert_pavement"))).useBlockDescriptionPrefix()));
    public static void initialize(){CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->out.accept(PAVEMENT_ITEM));}
    private DesertContent(){}
}
