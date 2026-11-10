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

public final class DarkOakBranchContent {
    public static final DarkOakBranchBlock BRANCH=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("dark_oak_branch"),new DarkOakBranchBlock(
        BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id("dark_oak_branch")))
            .mapColor(MapColor.WOOD).strength(2).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("dark_oak_branch"),new BlockItem(BRANCH,
        new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("dark_oak_branch"))).useBlockDescriptionPrefix()));
    public static void initialize(){
        net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(BRANCH,5,5);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->out.accept(ITEM));
    }
    private DarkOakBranchContent(){}
}
