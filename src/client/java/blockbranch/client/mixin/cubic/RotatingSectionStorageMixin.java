package blockbranch.client.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(RotatingSectionStorage.class)
abstract class RotatingSectionStorageMixin<T extends RotatingSectionStorage.Value> {
    @Shadow @Final @Mutable private int minY;
    @Shadow @Final @Mutable private int maxY;
    @Shadow @Final private int sectionGridSizeY;
    @Shadow private SectionPos centerSectionPos;
    @Shadow @Final private int radius;
    @Shadow public abstract T getValue(int x,int y,int z);
    @Unique private boolean blockbranch$transitionReady;
    @Unique private int blockbranch$worldMin;
    @Unique private int blockbranch$worldMax;
    @Unique private boolean blockbranch$vertical;
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true,ordinal=2)
    private static int blockbranch$limitHeight(int max,int radius,int min,int originalMax,RotatingSectionStorage.ValueCreator<?> creator) {
        return originalMax-min>1024 ? min+2*(radius+1) : max;
    }
    @Inject(method="<init>",at=@At(value="FIELD",target="Lnet/minecraft/client/RotatingSectionStorage;sectionGridSizeXZ:I",opcode=org.objectweb.asm.Opcodes.PUTFIELD,shift=At.Shift.AFTER))
    private void blockbranch$init(int radius,int min,int max,RotatingSectionStorage.ValueCreator<?> creator,CallbackInfo ci) {
        blockbranch$vertical=min==(ColumnLimits.MIN_Y>>4);
        blockbranch$worldMin=min;
        blockbranch$worldMax=blockbranch$vertical ? (ColumnLimits.MAX_Y>>4)-1 : max;
    }
    // Keep absolute section elevations in stable ring slots. Relative Y indexing
    // shifts every surviving mesh when minY moves, turning one layer into a full rebuild.
    @ModifyVariable(method="getSectionIndex",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private int blockbranch$ringY(int y) {
        return blockbranch$vertical ? Math.floorMod(minY+y,sectionGridSizeY) : y;
    }
    @Inject(method="repositionCenter",at=@At("HEAD"),cancellable=true)
    private void blockbranch$reposition(SectionPos center,CallbackInfoReturnable<Boolean> cir) {
        if(blockbranch$vertical) {
            boolean wasReady=blockbranch$transitionReady;
            if(!center.equals(centerSectionPos))blockbranch$transitionReady=false;
            int oldMin=minY;
            minY=Math.clamp(center.y()-sectionGridSizeY/2,blockbranch$worldMin,blockbranch$worldMax-sectionGridSizeY+1);
            maxY=minY+sectionGridSizeY-1;
            var old=centerSectionPos;
            int dx=center.x()-old.x(),dz=center.z()-old.z();
            int width=2*radius+1;
            if(wasReady && oldMin==minY && old.y()==center.y()
                    && (dx!=0||dz!=0) && Math.abs((long)dx)<=radius && Math.abs((long)dz)<=radius
                    && (long)(width-Math.abs(dx))*(width-Math.abs(dz))*4>=(long)width*width*3) {
                // A failed callback must retry through the full native scan.
                blockbranch$transitionReady=false;
                int baseX=center.x()-radius,baseZ=center.z()-radius;
                int oldX=old.x()-radius,oldZ=old.z()-radius;
                // Native callback order is local X, local Z, ascending absolute Y.
                for(int ix=0;ix<width;ix++) {
                    int x=baseX+Math.floorMod(ix-baseX,width);
                    for(int iz=0;iz<width;iz++) {
                        int z=baseZ+Math.floorMod(iz-baseZ,width);
                        if(x>=oldX&&x<oldX+width&&z>=oldZ&&z<oldZ+width)continue;
                        int priorX=oldX+Math.floorMod(ix-oldX,width);
                        int priorZ=oldZ+Math.floorMod(iz-oldZ,width);
                        for(int y=minY;y<=maxY;y++) {
                            T value=getValue(priorX,y,priorZ);
                            long node=SectionPos.asLong(x,y,z);
                            if(value.getSectionNode()!=node)value.setSectionNode(node);
                        }
                    }
                }
                centerSectionPos=center;
                blockbranch$transitionReady=true;
                cir.setReturnValue(true);
            }
        }
    }
    @Inject(method="repositionCenter",at=@At("RETURN"))
    private void blockbranch$completed(SectionPos center,CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValueZ())blockbranch$transitionReady=true;
    }
}
