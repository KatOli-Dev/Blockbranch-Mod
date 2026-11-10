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

/** Interlocked river aggregate is a stable block; the adjoining loose gravel still falls. */
public final class RiverMarginContent {
    public static final Block PACKED_RIVER_SHINGLE=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("packed_river_shingle"),
        new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id("packed_river_shingle")))
            .mapColor(MapColor.STONE).strength(1.1F,3F).sound(SoundType.GRAVEL)));
    public static final Item SHINGLE_ITEM=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("packed_river_shingle"),
        new BlockItem(PACKED_RIVER_SHINGLE,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("packed_river_shingle"))).useBlockDescriptionPrefix()));
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->out.accept(SHINGLE_ITEM));
    }
    private RiverMarginContent() {}
}
