package blockbranch.mixin.qa;
import blockbranch.cubic.*;import blockbranch.mixin.cubic.LightStorageAccessor;import net.minecraft.core.*;import net.minecraft.world.level.lighting.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LightEngine.class)
abstract class ReimaginedLightQueueMixin {
 @Shadow @Final protected LayerLightSectionStorage<?> storage;
 @Unique private int blockbranch$missing;
 @Inject(method="enqueueIncrease",at=@At("HEAD")) private void probe(long node,long entry,CallbackInfo ci){if(!(Boolean.getBoolean("blockbranch.reimaginedQualification")||Boolean.getBoolean("blockbranch.lightQueueDiagnostic"))||blockbranch$missing>=8)return;long section=SectionPos.blockToSection(node);var map=((LightStorageAccessor)storage).blockbranch$updating();if(((LightColumnIndex)map).blockbranch$layer(section)==null){blockbranch$missing++;blockbranch.Blockbranch.LOGGER.error("REIMAGINED_ENQUEUE_MISSING engine={} block={} entry={}",getClass().getName(),BlockPos.of(node),entry,new Exception("Enqueue origin"));}}
}
