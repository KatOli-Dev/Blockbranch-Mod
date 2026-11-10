package blockbranch.mixin.qa;
import blockbranch.realistic.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RealisticGenerator.class)
abstract class StructureLoadAuditMixin {
 @Shadow @Final @Mutable private int structureDefinition;
 @Inject(method="createState",at=@At("HEAD"))
 private void begin(CallbackInfoReturnable<net.minecraft.world.level.chunk.ChunkGeneratorStructureState> ci){
  if(!Boolean.getBoolean("blockbranch.structureLoadAudit"))return;
  if(Boolean.getBoolean("blockbranch.structureLoadControl"))structureDefinition=0;
  StructureLoadAudit.begin();
 }
 @Inject(method="createState",at=@At("RETURN"))
 private void end(CallbackInfoReturnable<net.minecraft.world.level.chunk.ChunkGeneratorStructureState> ci){
  if(Boolean.getBoolean("blockbranch.structureLoadAudit"))StructureLoadAudit.end();
 }
}
