package blockbranch.mixin.cubic;
import blockbranch.cubic.LightColumnIndex;
import net.minecraft.core.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(SkyLightSectionStorage.class)
abstract class SkyLightSectionStorageMixin extends LayerLightSectionStorage {
    protected SkyLightSectionStorageMixin(LightChunkGetter getter,DataLayerStorageMap initial) { super(LightLayer.SKY,getter,initial); }
    @Shadow protected abstract int getTopSectionY(long zeroNode);
    @Shadow private static DataLayer repeatFirstLayer(DataLayer value) { throw new AssertionError(); }
    @Inject(method="getLightValue(JZ)I",at=@At("HEAD"),cancellable=true)
    private void blockbranch$value(long block,boolean updating,CallbackInfoReturnable<Integer> cir) {
        long section=SectionPos.blockToSection(block);
        var index=(LightColumnIndex)(updating ? updatingSectionData : visibleSectionData);
        // Most probes address a stored section. Its exact native value needs no
        // ceiling search or temporary entry; gaps still use sparse topology.
        var exact=index.blockbranch$layer(section);
        if(exact!=null){cir.setReturnValue(exact.get(BlockPos.getX(block)&15,BlockPos.getY(block)&15,BlockPos.getZ(block)&15));return;}
        var height=index.blockbranch$sectionYs(SectionPos.getZeroNode(section)).ceiling(SectionPos.y(section));
        if(height==null) cir.setReturnValue(updating && !lightOnInSection(section) ? 0 : 15);
        else cir.setReturnValue(index.blockbranch$layer(SectionPos.asLong(SectionPos.x(section),height,SectionPos.z(section))).get(BlockPos.getX(block)&15,height==SectionPos.y(section) ? BlockPos.getY(block)&15 : 0,BlockPos.getZ(block)&15));
    }
    @Inject(method="createDataLayer",at=@At("HEAD"),cancellable=true)
    private void blockbranch$create(long section,CallbackInfoReturnable<DataLayer> cir) {
        var queued=(DataLayer)queuedSections.get(section); if(queued!=null) { cir.setReturnValue(queued); return; }
        var index=(LightColumnIndex)updatingSectionData;
        var upper=index.blockbranch$sectionYs(SectionPos.getZeroNode(section)).higher(SectionPos.y(section));
        if(upper!=null) cir.setReturnValue(repeatFirstLayer(index.blockbranch$layer(SectionPos.asLong(SectionPos.x(section),upper,SectionPos.z(section)))));
        else cir.setReturnValue(lightOnInSection(section) ? new DataLayer(15) : new DataLayer());
    }
}
