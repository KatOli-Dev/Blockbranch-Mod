package blockbranch.metallurgy;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.item.equipment.trim.*;
import net.minecraft.world.item.enchantment.Enchantments;

/** Native armor equip/damage, smithing and persisted component qualification. */
public final class TitaniumArmorSmoke {
    public static void verify(ServerLevel level,Path directory)throws Exception{
        var armor=List.of(TitaniumEquipment.TITANIUM_ALLOY_HELMET,TitaniumEquipment.TITANIUM_ALLOY_CHESTPLATE,TitaniumEquipment.TITANIUM_ALLOY_LEGGINGS);
        var types=List.of(ArmorType.HELMET,ArmorType.CHESTPLATE,ArmorType.LEGGINGS);
        var tags=List.of(net.minecraft.tags.ItemTags.HEAD_ARMOR,net.minecraft.tags.ItemTags.CHEST_ARMOR,net.minecraft.tags.ItemTags.LEG_ARMOR);
        String[][] patterns={{"BBB","B B"},{"B B","BBB","BBB"},{"BBB","B B","B B"}};
        var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());var path=directory.resolve("titanium-body-armor.dat");
        var before=Files.exists(path)?NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap()):null;var saved=new CompoundTag();var player=player(level);
        var protection=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        for(int i=0;i<armor.size();i++){
            var type=types.get(i);var cells=new ArrayList<ItemStack>();for(var row:patterns[i])for(char c:row.toCharArray())cells.add(c=='B'?new ItemStack(TitaniumContent.ALLOY_INGOT):ItemStack.EMPTY);
            var input=CraftingInput.of(3,patterns[i].length,cells);var crafted=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
            require(crafted.is(armor.get(i))&&crafted.is(tags.get(i))&&crafted.is(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR),"native armor recipe and tags");
            require(crafted.getMaxStackSize()==1&&crafted.get(DataComponents.ENCHANTABLE).value()==12&&crafted.getMaxDamage()==type.getDurability(20),"native armor stack and durability");
            require(crafted.isValidRepairItem(new ItemStack(TitaniumContent.ALLOY_INGOT))&&!crafted.isValidRepairItem(new ItemStack(SteelContent.STEEL_INGOT)),"titanium_alloy-only armor repair");
            var equip=crafted.get(DataComponents.EQUIPPABLE);require(equip!=null&&equip.slot()==type.getSlot()&&equip.assetId().orElseThrow().equals(TitaniumEquipment.EQUIPMENT),"native slot and custom equipment asset");
            require(protection.value().canEnchant(crafted),"native protection enchanting eligibility");
            player.getInventory().setSelectedSlot(0);player.getInventory().setItem(0,crafted);
            crafted.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);
            require(player.getItemBySlot(type.getSlot()).is(armor.get(i))&&player.getMainHandItem().isEmpty(),"native use equips into the correct slot");
            var smithing=new SmithingRecipeInput(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE),new ItemStack(armor.get(i)),new ItemStack(Items.QUARTZ));
            var trimmed=level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING,smithing,level).orElseThrow().value().assemble(smithing);
            require(trimmed.is(armor.get(i))&&trimmed.get(DataComponents.TRIM).material().is(TrimMaterials.QUARTZ)&&trimmed.get(DataComponents.TRIM).pattern().is(TrimPatterns.SENTRY),"native smithing trim");
            trimmed.setDamageValue(37);trimmed.enchant(protection,2);saved.put(type.getName(),ItemStack.CODEC.encodeStart(ops,trimmed).getOrThrow());
            var restored=ItemStack.CODEC.parse(ops,before==null?saved.getCompoundOrEmpty(type.getName()):before.getCompoundOrEmpty(type.getName())).getOrThrow();
            require(ItemStack.isSameItemSameComponents(trimmed,restored),"identity, damage, enchantments and trim persist across launch");
        }
        require(TitaniumEquipment.ARMOR.durability()<net.minecraft.world.item.equipment.ArmorMaterials.DIAMOND.durability()&&TitaniumEquipment.ARMOR.enchantmentValue()>net.minecraft.world.item.equipment.ArmorMaterials.DIAMOND.enchantmentValue(),"armor wear-life/enchantability tradeoff");
        var bad=new SmithingRecipeInput(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE),new ItemStack(TitaniumContent.ALLOY_INGOT),new ItemStack(Items.QUARTZ));
        require(level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING,bad,level).isEmpty(),"non-armor cannot be trimmed");
        NbtIo.writeCompressed(saved,path);
        var full=new ArrayList<Item>(armor);full.add(TitaniumContent.BOOTS);mobility(level,armor,full);
        float bare=damage(level,List.of(),0),copper=damage(level,List.of(Items.COPPER_HELMET,Items.COPPER_CHESTPLATE,Items.COPPER_LEGGINGS,Items.COPPER_BOOTS),10);
        float titanium_alloy=damage(level,full,15),iron=damage(level,List.of(Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS),15);
        float diamond=damage(level,List.of(Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS),20);
        require(bare>copper&&copper>titanium_alloy&&Math.abs(titanium_alloy-iron)<.0001f&&titanium_alloy>diamond&&diamond>0,"titanium_alloy retains iron defense and diamond stays stronger");
        blockbranch.Blockbranch.LOGGER.info("TITANIUM_ALLOY_ARMOR native equip/recipes/repair/enchants/trims reload={} healthLoss bare={} copper={} titanium_alloy={} iron={}",before!=null,bare,copper,titanium_alloy,iron);
    }
    private static void mobility(ServerLevel level,List<Item> body,List<Item> full)throws Exception{
        var p=player(level);var speed=net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;double base=p.getAttributeValue(speed);var slots=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);var update=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");update.setAccessible(true);
        for(int i=0;i<body.size();i++)p.setItemSlot(slots.get(i),new ItemStack(body.get(i)));update.invoke(p);require(p.getAttributeValue(speed)==base&&p.getArmorValue()==13,"body armor adds defense without extra movement");
        p.setItemSlot(EquipmentSlot.FEET,new ItemStack(TitaniumContent.BOOTS));update.invoke(p);require(Math.abs(p.getAttributeValue(speed)-base*1.05)<1e-8&&p.getArmorValue()==15,"full set retains published single boot bonus");update.invoke(p);require(Math.abs(p.getAttributeValue(speed)-base*1.05)<1e-8,"full set cannot stack movement");
        p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);update.invoke(p);require(p.getAttributeValue(speed)==base&&p.getArmorValue()==13,"removing boots clears bonus while body armor remains");
    }
    private static Player player(ServerLevel level){return new Player(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ArmorTester")){
        @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
    };}
    private static float damage(ServerLevel level,List<Item> pieces,int defense)throws Exception{
        var target=player(level);var slots=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
        for(int i=0;i<pieces.size();i++)target.setItemSlot(slots.get(i),new ItemStack(pieces.get(i)));
        // Run the native equipment update normally performed by an entity tick.
        var update=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");update.setAccessible(true);update.invoke(target);
        require(target.getArmorValue()==defense,"native equipped armor attributes");
        var attacker=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);float health=target.getHealth();
        require(target.hurtServer(level,level.damageSources().mobAttack(attacker),8),"native damaging hit");
        for(int i=0;i<pieces.size();i++)require(target.getItemBySlot(slots.get(i)).getDamageValue()>0,"native armor durability cost");
        return health-target.getHealth();
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private TitaniumArmorSmoke(){}
}
