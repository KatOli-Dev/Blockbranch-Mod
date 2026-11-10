package blockbranch.mixin.cubic;

import blockbranch.cubic.SparseColumn;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockLightEngine.class)
abstract class BlockLightEngineMixin extends LightEngine {
    protected BlockLightEngineMixin(LightChunkGetter getter,LayerLightSectionStorage storage){super(getter,storage);}

    @WrapOperation(method="propagateLightSources",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/LightChunk;findBlockLightSources(Ljava/util/function/BiConsumer;)V"))
    private void blockbranch$sourceLayers(LightChunk chunk,BiConsumer<BlockPos,BlockState> consumer,Operation<Void> original){
        if(!(chunk instanceof SparseColumn sparse)||!sparse.blockbranch$expanded()||sparse.blockbranch$terrain()==null){original.call(chunk,consumer);return;}
        // Neighbor FEATURES can write a previously unindexed solid interior after
        // INITIALIZE_LIGHT. Solid-to-solid edits emit no emptiness notification.
        // Native discovery enqueues emitters without a layer check. This native
        // callback runs on the lighting executor: register each actual source
        // section immediately before its consumer, including the native halo.
        var registered=new LongOpenHashSet();
        BiConsumer<BlockPos,BlockState> initialized=(pos,state)->{
            var section=SectionPos.of(pos);
            if(registered.add(section.asLong()))super.updateSectionStatus(section,false);
            consumer.accept(pos,state);
        };
        original.call(chunk,initialized);
    }
}
