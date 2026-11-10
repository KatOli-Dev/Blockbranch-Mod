package blockbranch.realistic;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;

public final class ReedbedContent {
    public static final LakeReedBlock LAKE_REED=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("lake_reed"),
        new LakeReedBlock(properties("lake_reed").mapColor(MapColor.PLANT).noCollision().noOcclusion()
            .offsetType(BlockBehaviour.OffsetType.XZ).strength(0.1F).sound(SoundType.GRASS).pushReaction(PushReaction.POPPED)));
    public static final Block WOVEN_REED_MAT=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("woven_reed_mat"),
        new CarpetBlock(properties("woven_reed_mat").mapColor(MapColor.COLOR_YELLOW).strength(0.2F).sound(SoundType.GRASS).ignitedByLava()));
    public static final Item REED_ITEM=blockItem("lake_reed",LAKE_REED);
    public static final Item MAT_ITEM=blockItem("woven_reed_mat",WOVEN_REED_MAT);
    public static final Item REED_FIBRE=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("reed_fibre"),
        new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("reed_fibre")))));
    private static BlockBehaviour.Properties properties(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id(name)));
    }
    private static Item blockItem(String name,Block block) {
        return Registry.register(BuiltInRegistries.ITEM,Blockbranch.id(name),new BlockItem(block,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id(name))).useBlockDescriptionPrefix()));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->out.accept(REED_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register(out->out.accept(MAT_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->out.accept(REED_FIBRE));
    }
    private ReedbedContent() {}
}
