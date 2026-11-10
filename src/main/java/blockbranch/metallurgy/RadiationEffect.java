package blockbranch.metallurgy;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
/** Armor material shielding is proportional; normal damage/Unbreaking and effect immunities remain native. */
public final class RadiationEffect extends MobEffect {
 public RadiationEffect(){super(MobEffectCategory.HARMFUL,0x77be43);}
 @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return duration%20==0;}
 @Override public boolean applyEffectTick(ServerLevel level,LivingEntity entity,int amplifier){
  int missing=Radiation.unprotectedPieces(entity);if(missing==0)return false;
  var source=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(UraniumContent.RADIATION_DAMAGE));entity.hurtServer(level,source,.5F*missing);
  for(var slot:Radiation.ARMOR){var stack=entity.getItemBySlot(slot);if(stack.is(UraniumContent.FRAGILE_ARMOR))stack.hurtAndBreak(1,entity,slot);}
  return true;
 }
}
