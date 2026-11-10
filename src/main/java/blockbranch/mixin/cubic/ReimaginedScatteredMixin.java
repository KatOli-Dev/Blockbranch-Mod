package blockbranch.mixin.cubic;
import blockbranch.realistic.ReimaginedScatteredAccess;
import net.minecraft.world.level.levelgen.structure.*;
import org.spongepowered.asm.mixin.*;
@Mixin(ScatteredFeaturePiece.class)
abstract class ReimaginedScatteredMixin implements ReimaginedScatteredAccess {
 @Shadow protected int heightPosition;
 public void blockbranch$fitReimagined(int y){var self=(StructurePiece)(Object)this;self.move(0,y-self.getBoundingBox().minY(),0);heightPosition=y;}
}
