package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    @Inject(method="postProcessGeneration",at=@At("HEAD"))
    private void blockbranch$sparsePost(ServerLevel level,CallbackInfo ci){SparsePostProcessing.process((LevelChunk)(Object)this,level);}
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void blockbranch$prune(BlockPos pos,BlockState state,int flags,CallbackInfoReturnable<BlockState> cir) {
        var sparse=(SparseColumn)this; int y=pos.getY()>>4;
        if(sparse.blockbranch$terrain()==null && sparse.blockbranch$expanded() && (y < -4 || y>19) && sparse.blockbranch$read(y).hasOnlyAir()) sparse.blockbranch$replace(y,null);
    }
    @Inject(method="replaceWithPacketData",at=@At("HEAD"))
    private void blockbranch$clear(int x,int z,net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData data,CallbackInfo ci) {
        var sparse=(SparseColumn)this;
        if(sparse.blockbranch$expanded()) {
            var chunk=(LevelChunk)(Object)this;
            var level=chunk.getLevel();
            var light=level.getChunkSource().getLightEngine();
            for(int y:java.util.List.copyOf(sparse.blockbranch$sections().keySet())) if(y < -4 || y>19) {
                var section=net.minecraft.core.SectionPos.of(chunk.getPos(),y);
                light.updateSectionStatus(section,true);
                level.getChunkSource().onSectionEmptinessChanged(x,y,z,true);
                for(int padding=-1;padding<=1;padding++) {
                    var location=net.minecraft.core.SectionPos.of(chunk.getPos(),y+padding);
                    light.queueSectionData(net.minecraft.world.level.LightLayer.BLOCK,location,null);
                    light.queueSectionData(net.minecraft.world.level.LightLayer.SKY,location,null);
                }
                sparse.blockbranch$replace(y,null);
            }
            sparse.blockbranch$terrain(null);
        }
    }
    @Inject(method="<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ProtoChunk;getBlockEntities()Ljava/util/Map;"))
    private void blockbranch$promote(ServerLevel level, ProtoChunk proto, LevelChunk.PostLoadProcessor processor, CallbackInfo ci) {
        var updates=((SparsePostProcessingAccess)proto).blockbranch$postProcessing(false);
        if(updates!=null)((SparsePostProcessingAccess)this).blockbranch$postProcessing(true).restore(updates.snapshot());
        // Publish sparse states before vanilla validates and attaches block entities.
        var from=(SparseColumn)proto; var to=(SparseColumn)this;
        if(from.blockbranch$expanded()) from.blockbranch$sections().forEach(to::blockbranch$replace);
        if(from.blockbranch$terrain()!=null) to.blockbranch$terrain(from.blockbranch$terrain().rebind((ChunkAccess)(Object)this));
    }
    @Inject(method="getBlockState", at=@At("HEAD"), cancellable=true)
    private void blockbranch$read(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        var self=(SparseColumn)this;
        if(self.blockbranch$expanded()) cir.setReturnValue(ColumnLimits.containsY(pos.getY()) ? self.blockbranch$read(pos.getY()>>4).getBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15) : Blocks.VOID_AIR.defaultBlockState());
    }
    @Inject(method="getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", at=@At("HEAD"), cancellable=true)
    private void blockbranch$fluid(int x,int y,int z,CallbackInfoReturnable<FluidState> cir) {
        var self=(SparseColumn)this;
        if(self.blockbranch$expanded()) cir.setReturnValue(ColumnLimits.containsY(y) ? self.blockbranch$read(y>>4).getFluidState(x&15,y&15,z&15) : Fluids.EMPTY.defaultFluidState());
    }
    @Redirect(method="setBlockState", at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/LevelChunk;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;"))
    private LevelChunkSection blockbranch$write(LevelChunk chunk,int index) { var sparse=(SparseColumn)chunk; return sparse.blockbranch$expanded() ? sparse.blockbranch$write(chunk.getSectionYFromSectionIndex(index)) : chunk.getSection(index); }
}
