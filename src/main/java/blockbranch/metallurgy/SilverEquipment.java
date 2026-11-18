package blockbranch.metallurgy;
import static blockbranch.metallurgy.MetalItems.item;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
/** Completes silver equipment using its published tool material and own repair ingredient. */
public final class SilverEquipment {
    public static final Item SILVER_SWORD=item("silver_sword",new Item.Properties().sword(PreciousContent.SILVER,3,-2.4f));
    public static final Item SILVER_AXE=item("silver_axe",new Item.Properties().axe(PreciousContent.SILVER,6,-3.1f));
    public static final Item SILVER_SHOVEL=item("silver_shovel",new Item.Properties().shovel(PreciousContent.SILVER,1.5f,-3));
    public static final Item SILVER_HOE=item("silver_hoe",new Item.Properties().hoe(PreciousContent.SILVER,-2,-1));
    public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> EQUIPMENT=ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,Blockbranch.id("silver"));
    // Iron defense with shorter wear life and higher enchantability; no toughness or knockback bonus.
    public static final net.minecraft.world.item.equipment.ArmorMaterial ARMOR=new net.minecraft.world.item.equipment.ArmorMaterial(13,java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.HELMET,2,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE,6,net.minecraft.world.item.equipment.ArmorType.LEGGINGS,5,net.minecraft.world.item.equipment.ArmorType.BOOTS,2),14,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,0,0,PreciousContent.SILVER_REPAIR,EQUIPMENT);
    public static final Item SILVER_HELMET=item("silver_helmet",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final Item SILVER_CHESTPLATE=item("silver_chestplate",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final Item SILVER_LEGGINGS=item("silver_leggings",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    public static final Item SILVER_BOOTS=item("silver_boots",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.BOOTS));
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(SILVER_AXE);out.accept(SILVER_SHOVEL);out.accept(SILVER_HOE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{out.accept(SILVER_SWORD);out.accept(SILVER_AXE);out.accept(SILVER_HELMET);out.accept(SILVER_CHESTPLATE);out.accept(SILVER_LEGGINGS);out.accept(SILVER_BOOTS);});
    }
    private SilverEquipment(){}
}
