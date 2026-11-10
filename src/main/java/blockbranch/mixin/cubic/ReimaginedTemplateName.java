package blockbranch.mixin.cubic;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Accessor;import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
@Mixin(TemplateStructurePiece.class)
public interface ReimaginedTemplateName {@Accessor("templateName")String blockbranch$reimaginedTemplate();}
