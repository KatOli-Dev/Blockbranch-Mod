package blockbranch.mixin.cubic;

import blockbranch.realistic.*;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MapItemSavedData.class)
abstract class PendingMapPreviewMixin implements PendingMapPreview {
    @Shadow @Final @Mutable public static Codec<MapItemSavedData> CODEC;
    @Unique private long blockbranch$retry;
    public long blockbranch$previewRetry(){return blockbranch$retry;}
    public void blockbranch$previewRetry(long nanos){blockbranch$retry=nanos;}
    @Unique private boolean blockbranch$preview;
    @Unique private int blockbranch$rows;
    public int blockbranch$previewRows(){return blockbranch$rows;}
    public void blockbranch$previewRows(int rows){blockbranch$rows=rows;}
    public boolean blockbranch$pendingPreview(){return blockbranch$preview;}
    public void blockbranch$pendingPreview(boolean value){blockbranch$preview=value;}
    @Inject(method="<clinit>",at=@At("TAIL"))
    private static void previewCodec(CallbackInfo ci){CODEC=PreviewMapCodec.wrap(CODEC);}
    @Inject(method="setColor",at=@At("HEAD"))
    private void preserveExternalEdits(int x,int z,byte color,CallbackInfo ci){
        var self=(MapItemSavedData)(Object)this;
        if(blockbranch$preview&&!DeferredMapPreview.writing(self))DeferredMapPreview.mark(self,false);
    }
}
