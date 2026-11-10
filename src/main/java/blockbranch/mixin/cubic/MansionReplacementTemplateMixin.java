package blockbranch.mixin.cubic;

import blockbranch.realistic.MansionReplacementTemplateAccess;
import blockbranch.realistic.MansionReplacementTemplateProcessor;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WoodlandMansionPieces.WoodlandMansionPiece.class)
abstract class MansionReplacementTemplateMixin extends TemplateStructurePiece implements MansionReplacementTemplateAccess {
    @Unique private int blockbranch$mansionReplacement,blockbranch$mansionFloor;

    protected MansionReplacementTemplateMixin(StructurePieceType type,CompoundTag tag,StructureTemplateManager manager,Function<Identifier,StructurePlaceSettings> settings) {
        super(type,tag,manager,settings);
    }
    @Override public int blockbranch$mansionReplacementDefinition(){return blockbranch$mansionReplacement;}
    @Override public int blockbranch$mansionReplacementFloor(){return blockbranch$mansionFloor;}
    @Override public void blockbranch$configureMansionReplacement(int definition,int floor) {
        MansionReplacementTemplateProcessor.validateConfiguration(definition,floor,templatePosition.getY());
        if(blockbranch$mansionReplacement==1) {
            if(definition!=1||floor!=blockbranch$mansionFloor)throw new IllegalArgumentException("Native mansion replacement is already configured");
            return;
        }
        if(definition==0)return;
        if(placeSettings.getProcessors().stream().anyMatch(p->p instanceof MansionReplacementTemplateProcessor))throw new IllegalStateException("Duplicate mansion replacement processor");
        placeSettings.addProcessor(new MansionReplacementTemplateProcessor(templateName,floor));
        blockbranch$mansionReplacement=1;blockbranch$mansionFloor=floor;
    }
    @Inject(method="<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void blockbranch$readMansionReplacement(StructureTemplateManager manager,CompoundTag tag,CallbackInfo ci) {
        int definition=MansionReplacementTemplateProcessor.readDefinition(tag);
        if(definition==1)blockbranch$configureMansionReplacement(1,MansionReplacementTemplateProcessor.readFloor(tag,templatePosition.getY()));
    }
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void blockbranch$saveMansionReplacement(StructurePieceSerializationContext context,CompoundTag tag,CallbackInfo ci) {
        if(blockbranch$mansionReplacement==0)return;
        tag.putInt(MansionReplacementTemplateProcessor.DEFINITION_TAG,1);
        tag.putInt(MansionReplacementTemplateProcessor.FLOOR_TAG,blockbranch$mansionFloor);
    }
}
