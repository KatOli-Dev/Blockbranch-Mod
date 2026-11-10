package blockbranch.metallurgy;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
/** Successful melee damage is handled before native death/loot, retaining native combat calculations. */
public final class UraniumCombat {
 public static final float STRIP_CHANCE=.15F,IRRADIATE_CHANCE=.8F;
 public static boolean weaponHit(DamageSource source){var item=source.getWeaponItem();return(source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK)||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)||source.is(DamageTypes.SPEAR))&&item!=null&&item.is(UraniumContent.WEAPONS);}
 private record EnchantedGear(ItemStack stack,Holder<Enchantment> enchantment){}
 public static void afterHit(LivingEntity victim,DamageSource source){
  if(!weaponHit(source))return;var random=source.getEntity() instanceof LivingEntity attacker?attacker.getRandom():victim.getRandom();
  if(random.nextFloat()<STRIP_CHANCE){var choices=new ArrayList<EnchantedGear>();for(var slot:EquipmentSlot.values()){var stack=victim.getItemBySlot(slot);for(var enchantment:stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).keySet())choices.add(new EnchantedGear(stack,enchantment));}if(!choices.isEmpty()){var choice=choices.get(random.nextInt(choices.size()));var changed=new ItemEnchantments.Mutable(choice.stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY));changed.removeIf(choice.enchantment::equals);choice.stack.set(DataComponents.ENCHANTMENTS,changed.toImmutable());}}
  if(victim.isAlive()&&random.nextFloat()<IRRADIATE_CHANCE)Radiation.irradiate(victim,200);
 }
 private UraniumCombat(){}
}
