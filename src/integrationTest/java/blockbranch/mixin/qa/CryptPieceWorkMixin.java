package blockbranch.mixin.qa;
import blockbranch.realistic.*;import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;import net.minecraft.world.level.*;import net.minecraft.world.level.chunk.*;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.core.registries.Registries;import net.minecraft.util.RandomSource;import org.spongepowered.asm.mixin.Mixin;
@Mixin(StructureStart.class)
abstract class CryptPieceWorkMixin {
 @WrapMethod(method="placeInChunk")private void placed(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox bounds,ChunkPos pos,Operation<Void> original){if(!AdmissionWorkTrace.ON){original.call(level,manager,generator,random,bounds,pos);return;}var self=(StructureStart)(Object)this;String id=String.valueOf(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(self.getStructure()));var span=AdmissionWorkTrace.begin("place."+id,pos.x(),pos.z());boolean success=false;try{original.call(level,manager,generator,random,bounds,pos);success=true;}finally{AdmissionWorkTrace.end(span,success);}}
}
