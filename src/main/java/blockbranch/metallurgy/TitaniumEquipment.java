package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
/** Titanium tool/body-armor completion; published mobility boots retain their own asset and behavior. */
public final class TitaniumEquipment {
    // Diamond harvest access, below diamond speed/wear life; movement remains on the published boots.
    public static final net.minecraft.world.item.ToolMaterial MATERIAL=new ToolMaterial(net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL,1024,7.5f,2,12,TitaniumContent.REPAIR);
    public static final Item TITANIUM_ALLOY_PICKAXE=gear("titanium_alloy_pickaxe",new Item.Properties().pickaxe(MATERIAL,1,-2.8f));
    public static final Item TITANIUM_ALLOY_SWORD=gear("titanium_alloy_sword",new Item.Properties().sword(MATERIAL,3,-2.4f));
    public static final Item TITANIUM_ALLOY_AXE=gear("titanium_alloy_axe",new Item.Properties().axe(MATERIAL,6,-3.1f));
    public static final Item TITANIUM_ALLOY_SHOVEL=gear("titanium_alloy_shovel",new Item.Properties().shovel(MATERIAL,1.5f,-3));
    public static final Item TITANIUM_ALLOY_HOE=gear("titanium_alloy_hoe",new Item.Properties().hoe(MATERIAL,-2,-1));
    public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> EQUIPMENT=ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,Blockbranch.id("titanium_alloy_gear"));
    // Iron defense and moderate wear life; no additional movement modifiers on body armor.
    public static final net.minecraft.world.item.equipment.ArmorMaterial ARMOR=new net.minecraft.world.item.equipment.ArmorMaterial(20,java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.HELMET,2,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE,6,net.minecraft.world.item.equipment.ArmorType.LEGGINGS,5),12,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,0,0,TitaniumContent.REPAIR,EQUIPMENT);
    public static final Item TITANIUM_ALLOY_HELMET=gear("titanium_alloy_helmet",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final Item TITANIUM_ALLOY_CHESTPLATE=gear("titanium_alloy_chestplate",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final Item TITANIUM_ALLOY_LEGGINGS=gear("titanium_alloy_leggings",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    private static Item gear(String name,Item.Properties properties){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(properties.setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(TITANIUM_ALLOY_PICKAXE);out.accept(TITANIUM_ALLOY_AXE);out.accept(TITANIUM_ALLOY_SHOVEL);out.accept(TITANIUM_ALLOY_HOE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{out.accept(TITANIUM_ALLOY_SWORD);out.accept(TITANIUM_ALLOY_AXE);out.accept(TITANIUM_ALLOY_HELMET);out.accept(TITANIUM_ALLOY_CHESTPLATE);out.accept(TITANIUM_ALLOY_LEGGINGS);});
    }
    private TitaniumEquipment(){}
}
