package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
/** Completes platinum-alloy equipment using its published native tool material. */
public final class PlatinumEquipment {
    public static final Item PLATINUM_ALLOY_SWORD=gear("platinum_alloy_sword",new Item.Properties().sword(PlatinumAlloyContent.MATERIAL,3,-2.4f));
    public static final Item PLATINUM_ALLOY_AXE=gear("platinum_alloy_axe",new Item.Properties().axe(PlatinumAlloyContent.MATERIAL,6,-3.1f));
    public static final Item PLATINUM_ALLOY_SHOVEL=gear("platinum_alloy_shovel",new Item.Properties().shovel(PlatinumAlloyContent.MATERIAL,1.5f,-3));
    public static final Item PLATINUM_ALLOY_HOE=gear("platinum_alloy_hoe",new Item.Properties().hoe(PlatinumAlloyContent.MATERIAL,-2,-1));
    public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> EQUIPMENT=ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,Blockbranch.id("platinum_alloy"));
    // Iron defense with higher enchantability; diamond keeps defense and wear-life advantages.
    public static final net.minecraft.world.item.equipment.ArmorMaterial ARMOR=new net.minecraft.world.item.equipment.ArmorMaterial(20,java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.HELMET,2,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE,6,net.minecraft.world.item.equipment.ArmorType.LEGGINGS,5,net.minecraft.world.item.equipment.ArmorType.BOOTS,2),22,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,0,0,PlatinumAlloyContent.REPAIR,EQUIPMENT);
    public static final Item PLATINUM_ALLOY_HELMET=gear("platinum_alloy_helmet",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final Item PLATINUM_ALLOY_CHESTPLATE=gear("platinum_alloy_chestplate",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final Item PLATINUM_ALLOY_LEGGINGS=gear("platinum_alloy_leggings",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    public static final Item PLATINUM_ALLOY_BOOTS=gear("platinum_alloy_boots",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.BOOTS));
    private static Item gear(String name,Item.Properties properties){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(properties.setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(PLATINUM_ALLOY_AXE);out.accept(PLATINUM_ALLOY_SHOVEL);out.accept(PLATINUM_ALLOY_HOE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{out.accept(PLATINUM_ALLOY_SWORD);out.accept(PLATINUM_ALLOY_AXE);out.accept(PLATINUM_ALLOY_HELMET);out.accept(PLATINUM_ALLOY_CHESTPLATE);out.accept(PLATINUM_ALLOY_LEGGINGS);out.accept(PLATINUM_ALLOY_BOOTS);});
    }
    private PlatinumEquipment(){}
}
