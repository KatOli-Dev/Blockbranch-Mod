package blockbranch.client.mixin.metallurgy;
import blockbranch.metallurgy.UraniumContent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Reuses native green-heart sprites, including hardcore/blink variants and resource-pack overrides. */
@Mixin(targets="net.minecraft.client.gui.Hud$HeartType")
public abstract class RadiationHeartMixin {
 // Native 26.3 retains this enum constant spelling: POISIONED.
 @SuppressWarnings({"rawtypes","unchecked"})
 @Inject(method="forPlayer",at=@At("RETURN"),cancellable=true)
 private static void radiation(Player player,CallbackInfoReturnable<Object> cir){if(player.hasEffect(UraniumContent.RADIATION)){var type=(Enum)cir.getReturnValue();cir.setReturnValue(Enum.valueOf(type.getDeclaringClass(),"POISIONED"));}}
}
