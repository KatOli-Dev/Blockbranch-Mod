package blockbranch.mixin.qa;
import blockbranch.realistic.UniformSnapshots;
import blockbranch.qa.SnapshotHoldProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Field ordinal1 is the first map access after monitorenter in the frozen source. */
@Mixin(value=UniformSnapshots.class,remap=false)
abstract class UniformSnapshotHoldMixin {
 @Inject(method="compactBiomes",at=@At("HEAD")) private static void beginBiome(CallbackInfoReturnable<?> ci){SnapshotHoldProbe.begin(0,false);}
 @Inject(method="compactBiomes",at=@At(value="FIELD",target="Lblockbranch/realistic/UniformSnapshots;BIOMES:Ljava/util/Map;",ordinal=1)) private static void acquiredBiome(CallbackInfoReturnable<?> ci){SnapshotHoldProbe.acquired(0);}
 @Inject(method="compactBiomes",at=@At("RETURN")) private static void endBiome(net.minecraft.world.level.chunk.PalettedContainerRO<?> source,CallbackInfoReturnable<?> ci){SnapshotHoldProbe.end(0);blockbranch.qa.UniformBiomeReuseProbe.returned(source,ci.getReturnValue());}
 @Inject(method="compactBiomes",at=@At(value="FIELD",target="Lblockbranch/realistic/UniformSnapshots;LAST_BIOME:Ljava/lang/ThreadLocal;",ordinal=1),require=0) private static void sharedBiome(CallbackInfoReturnable<?> ci){blockbranch.qa.UniformBiomeReuseProbe.shared();}
 @Inject(method="copyOf",at=@At("HEAD")) private static void beginSnapshot(CallbackInfoReturnable<?> ci){SnapshotHoldProbe.begin(1,false);}
 @Inject(method="copyOf",at=@At(value="FIELD",target="Lblockbranch/realistic/UniformSnapshots;CACHE:Ljava/util/Map;",ordinal=1)) private static void acquiredSnapshot(CallbackInfoReturnable<?> ci){SnapshotHoldProbe.acquired(1);}
 @Inject(method="copyOf",at=@At("RETURN")) private static void endSnapshot(CallbackInfoReturnable<?> ci){SnapshotHoldProbe.end(1);}
}
