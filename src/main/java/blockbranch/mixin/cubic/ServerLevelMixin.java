package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Unique private boolean blockbranch$inactive(BlockPos pos) {
        var level=(ServerLevel)(Object)this;
        return level.getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator
            && !VerticalRuntime.active(level,pos.getX(),pos.getY(),pos.getZ());
    }
    @Inject(method="tickBlock",at=@At("HEAD"),cancellable=true)
    private void blockbranch$deferBlock(BlockPos pos,net.minecraft.world.level.block.Block block,CallbackInfo ci) {
        if(blockbranch$inactive(pos)) { ((ServerLevel)(Object)this).scheduleTick(pos,block,20); ci.cancel(); }
    }
    @Inject(method="tickFluid",at=@At("HEAD"),cancellable=true)
    private void blockbranch$deferFluid(BlockPos pos,net.minecraft.world.level.material.Fluid fluid,CallbackInfo ci) {
        if(blockbranch$inactive(pos)) { ((ServerLevel)(Object)this).scheduleTick(pos,fluid,20); ci.cancel(); }
    }
    @Inject(method="isPositionEntityTicking",at=@At("HEAD"),cancellable=true)
    private void blockbranch$verticalTicking(BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        if(blockbranch$inactive(pos)) cir.setReturnValue(false);
    }
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void blockbranch$verticalEntity(net.minecraft.world.entity.Entity entity,CallbackInfo ci) {
        if(!(entity instanceof net.minecraft.server.level.ServerPlayer) && blockbranch$inactive(entity.blockPosition())) ci.cancel();
    }
    @Redirect(method="tickChunk",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/LevelChunkSection;isRandomlyTicking()Z"))
    private boolean blockbranch$nativeRandomTicks(net.minecraft.world.level.chunk.LevelChunkSection section,LevelChunk chunk,int speed) {
        if(((SparseColumn)chunk).blockbranch$terrain()!=null) {
            var sections=chunk.getSections();
            for(int i=0;i<sections.length;i++) if(sections[i]==section && blockbranch$inactive(new BlockPos(chunk.getPos().getMinBlockX(),chunk.getSectionYFromSectionIndex(i)*16+8,chunk.getPos().getMinBlockZ()))) return false;
        }
        return section.isRandomlyTicking();
    }
    @Inject(method="getLogicalHeight",at=@At("HEAD"),cancellable=true)
    private void blockbranch$logical(CallbackInfoReturnable<Integer> cir) { if(ColumnLimits.expanded((ServerLevel)(Object)this)) cir.setReturnValue(ColumnLimits.HEIGHT); }
    @Inject(method="tickChunk",at=@At("TAIL"))
    private void blockbranch$randomTicks(LevelChunk chunk,int speed,CallbackInfo ci) {
        var sparse=(SparseColumn)chunk; if(!sparse.blockbranch$expanded() || speed<=0) return;
        var level=(ServerLevel)(Object)this;
        for(var e:new java.util.ArrayList<>(sparse.blockbranch$extraSections().entrySet())) {
            if(sparse.blockbranch$terrain()!=null && blockbranch$inactive(new BlockPos(chunk.getPos().getMinBlockX(),e.getKey()*16+8,chunk.getPos().getMinBlockZ()))) continue;
            if(!e.getValue().isRandomlyTicking()) continue;
            for(int i=0;i<speed;i++) {
                BlockPos pos=level.getBlockRandomPos(chunk.getPos().getMinBlockX(),e.getKey()<<4,chunk.getPos().getMinBlockZ(),15);
                var state=e.getValue().getBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
                if(state.isRandomlyTicking()) state.randomTick(level,pos,level.getRandom());
                if(state.getFluidState().isRandomlyTicking()) state.getFluidState().randomTick(level,pos,level.getRandom());
            }
        }
        var terrain=sparse.blockbranch$terrain();if(terrain==null)return;
        // Unresident procedural surfaces can lie inside the native array range too;
        // those array slots are empty placeholders. Resident slots were already ticked.
        for(int y:terrain.base().randomTickSections()){
            if(terrain.resident(y)||blockbranch$inactive(new BlockPos(chunk.getPos().getMinBlockX(),y*16+8,chunk.getPos().getMinBlockZ())))continue;
            var section=sparse.blockbranch$read(y);if(!section.isRandomlyTicking())continue;
            for(int i=0;i<speed;i++){var pos=level.getBlockRandomPos(chunk.getPos().getMinBlockX(),y<<4,chunk.getPos().getMinBlockZ(),15);var state=sparse.blockbranch$read(y).getBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15);if(state.isRandomlyTicking())state.randomTick(level,pos,level.getRandom());}
        }
    }
}
