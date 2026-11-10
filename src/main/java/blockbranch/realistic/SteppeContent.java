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

public final class SteppeContent {
    public static final SteppeShrubBlock SILVER_SHRUB=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("silver_steppe_shrub"),
        new SteppeShrubBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id("silver_steppe_shrub")))
            .mapColor(MapColor.PLANT).noCollision().noOcclusion().replaceable().strength(0.2F).sound(SoundType.GRASS).pushReaction(PushReaction.POPPED)));
    public static final Item SHRUB_ITEM=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("silver_steppe_shrub"),
        new BlockItem(SILVER_SHRUB,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("silver_steppe_shrub"))).useBlockDescriptionPrefix()));
    public static void initialize() { CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->out.accept(SHRUB_ITEM)); }
    private SteppeContent() {}
}
