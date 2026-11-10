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

public final class SandyShoreContent {
    public static final RootBoundSandBlock ROOT_BOUND_SAND=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("root_bound_sand"),
        new RootBoundSandBlock(properties("root_bound_sand").mapColor(MapColor.SAND).strength(0.6F).sound(SoundType.SAND)));
    public static final BeachGrassBlock BEACH_GRASS=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("beach_grass"),
        new BeachGrassBlock(properties("beach_grass").mapColor(MapColor.PLANT).noCollision().noOcclusion().replaceable()
            .strength(0.1F).sound(SoundType.GRASS).pushReaction(PushReaction.POPPED)));
    public static final Item ROOT_SAND_ITEM=item("root_bound_sand",ROOT_BOUND_SAND);
    public static final Item GRASS_ITEM=item("beach_grass",BEACH_GRASS);
    private static BlockBehaviour.Properties properties(String name) { return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id(name))); }
    private static Item item(String name,Block block) {
        return Registry.register(BuiltInRegistries.ITEM,Blockbranch.id(name),new BlockItem(block,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id(name))).useBlockDescriptionPrefix()));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{ out.accept(ROOT_SAND_ITEM);out.accept(GRASS_ITEM); });
    }
    private SandyShoreContent() {}
}
