package blockbranch.mixin.metallurgy;
import blockbranch.metallurgy.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class UraniumLivingMixin {
 @Inject(method="tick",at=@At("TAIL")) private void exposure(CallbackInfo ci){Radiation.tick((LivingEntity)(Object)this);}
 @ModifyVariable(method="heal",at=@At("HEAD"),argsOnly=true,ordinal=0) private float halfHealing(float value){return((LivingEntity)(Object)this).hasEffect(UraniumContent.RADIATION)?value*.5F:value;}
 @ModifyArg(method="doHurtEquipment",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"),index=0)
 private int armorCost(int value,@Local(argsOnly=true)DamageSource source){return UraniumCombat.weaponHit(source)?(int)Math.min(Integer.MAX_VALUE,(long)value*3):value;}
 @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
 private void onProcessedHit(LivingEntity target,ServerLevel level,DamageSource source,float amount,Operation<Void> original){float health=target.getHealth(),absorption=target.getAbsorptionAmount();original.call(target,level,source,amount);if(amount>0&&(target.getHealth()<health||target.getAbsorptionAmount()<absorption))UraniumCombat.afterHit(target,source);}
}
