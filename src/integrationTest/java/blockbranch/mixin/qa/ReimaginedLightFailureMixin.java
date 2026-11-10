package blockbranch.mixin.qa;
import blockbranch.cubic.LightColumnIndex;import net.minecraft.core.*;import net.minecraft.world.level.chunk.DataLayer;import net.minecraft.world.level.lighting.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Diagnose the native missing-layer failure; never substitutes a light value or suppresses failure. */
@Mixin(LayerLightSectionStorage.class)
abstract class ReimaginedLightFailureMixin {
 @Shadow protected DataLayerStorageMap<?> updatingSectionData;
 @Shadow protected abstract DataLayer getDataLayer(long section,boolean updating);
 @Inject(method="getStoredLevel",at=@At("HEAD")) private void probe(long node,CallbackInfoReturnable<Integer> cir){if(!(Boolean.getBoolean("blockbranch.reimaginedQualification")||Boolean.getBoolean("blockbranch.lightQueueDiagnostic")))return;long section=SectionPos.blockToSection(node);if(getDataLayer(section,true)==null){var index=(LightColumnIndex)updatingSectionData;blockbranch.Blockbranch.LOGGER.error("REIMAGINED_MISSING_LIGHT storage={} block={} section={} exact={} ys={}",getClass().getName(),BlockPos.of(node),SectionPos.of(section),index.blockbranch$layer(section),index.blockbranch$sectionYs(SectionPos.getZeroNode(section)));}}
}
