package blockbranch.mixin.cubic;

import blockbranch.realistic.ProgressiveMapDrawing;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(net.minecraft.world.item.MapItem.class)
abstract class ProgressiveMapDrawingMixin {
    @WrapMethod(method="update")
    private void drawProgressively(Level level, Entity entity, MapItemSavedData data, Operation<Void> original) {
        if(blockbranch.realistic.DeferredMapPreview.pending(data))return;
        var context=ProgressiveMapDrawing.begin(level,entity,data);
        if(context==null){original.call(level,entity,data);return;}
        try(context){
            if(!context.allowed)return;
            try {original.call(level,entity,data);}
            catch(ProgressiveMapDrawing.Yield deferred) { /* partial native pixels remain dirty and saved */ }
            catch(RuntimeException|Error failure){context.failed();throw failure;}
        }
    }
    // Minecraft 26.3 update: local15 is the outer pixel-X cursor (initial STORE only).
    @ModifyVariable(method="update",at=@At("STORE"),index=15)
    private int startColumn(int nativeStart) {
        var c=ProgressiveMapDrawing.current();return c==null?nativeStart:c.column();
    }
    @WrapOperation(method="update",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;getHoldingPlayer(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData$HoldingPlayer;"))
    private MapItemSavedData.HoldingPlayer selectColumn(MapItemSavedData data,net.minecraft.world.entity.player.Player player,Operation<MapItemSavedData.HoldingPlayer> original){
        var holding=original.call(data,player);var c=ProgressiveMapDrawing.current();if(c!=null)holding.step=c.column()-1;return holding;
    }
    @WrapOperation(method="update",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk readyChunk(Level level,int x,int z,Operation<LevelChunk> original,@Local(index=15)int column){
        var c=ProgressiveMapDrawing.current();return c==null?original.call(level,x,z):c.chunk(x,z,column);
    }
}
