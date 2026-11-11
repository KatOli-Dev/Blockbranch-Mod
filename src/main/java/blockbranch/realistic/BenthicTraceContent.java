package blockbranch.realistic;

import blockbranch.Blockbranch;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.block.SoundType;

public final class BenthicTraceContent {
    public static final BenthicTraceBlock TRACE=Registry.register(BuiltInRegistries.BLOCK,Blockbranch.id("sediment_burrow_trace"),new BenthicTraceBlock(
        BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,Blockbranch.id("sediment_burrow_trace")))
            .mapColor(MapColor.COLOR_BROWN).noCollision().noOcclusion().strength(.1F).sound(SoundType.MUD).pushReaction(PushReaction.POPPED)));
    public static void initialize(){}
    private BenthicTraceContent(){}
}
