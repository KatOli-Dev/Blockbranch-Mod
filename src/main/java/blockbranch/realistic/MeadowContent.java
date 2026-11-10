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

public final class MeadowContent {
    public static final MeadowFescueBlock MEADOW_FESCUE=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("meadow_fescue"),
        new MeadowFescueBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id("meadow_fescue")))
            .mapColor(MapColor.PLANT).noCollision().noOcclusion().replaceable().offsetType(net.minecraft.world.level.block.state.BlockBehaviour.OffsetType.XZ).strength(0.1F).sound(SoundType.GRASS).pushReaction(PushReaction.POPPED)));
    public static final Item FESCUE_ITEM=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("meadow_fescue"),
        new BlockItem(MEADOW_FESCUE,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("meadow_fescue"))).useBlockDescriptionPrefix()));
    public static void initialize() { CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->out.accept(FESCUE_ITEM)); }
    private MeadowContent() {}
}
