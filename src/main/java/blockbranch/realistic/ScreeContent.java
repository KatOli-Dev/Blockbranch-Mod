package blockbranch.realistic;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Loose angular debris falls when undermined and reduces walking speed. */
public final class ScreeContent {
    public static final Block LOOSE_SCREE = Registry.register(BuiltInRegistries.BLOCK, Blockbranch.id("loose_scree"),
        new ColoredFallingBlock(new ColorRGBA(0xFF92918C), BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, Blockbranch.id("loose_scree")))
            .mapColor(MapColor.STONE).strength(0.7F).sound(SoundType.GRAVEL).speedFactor(0.65F)));
    public static final Item SCREE_ITEM = register("loose_scree", true);
    public static final Item ANGULAR_ROCK_FRAGMENT = register("angular_rock_fragment", false);

    private static Item register(String name, boolean block) {
        var properties = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Blockbranch.id(name)));
        return Registry.register(BuiltInRegistries.ITEM, Blockbranch.id(name),
            block ? new BlockItem(LOOSE_SCREE, properties.useBlockDescriptionPrefix()) : new Item(properties));
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> entries.accept(SCREE_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.accept(ANGULAR_ROCK_FRAGMENT));
    }
    private ScreeContent() {}
}
