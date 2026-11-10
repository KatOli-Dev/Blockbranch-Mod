package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Titanium/nickel alloy foundation with native, feet-only mobility equipment. */
public final class TitaniumContent {
    public static final Block ORE=block("titanium_ore",Blocks.IRON_ORE,5);
    public static final Block DEEPSLATE_ORE=block("deepslate_titanium_ore",Blocks.DEEPSLATE_IRON_ORE,7);
    public static final Block ALLOY_BLOCK=block("titanium_alloy_block",Blocks.IRON_BLOCK,5);
    public static final Item RAW=item("raw_titanium",new Item.Properties());
    public static final Item INGOT=item("titanium_ingot",new Item.Properties());
    public static final Item ALLOY_INGOT=item("titanium_alloy_ingot",new Item.Properties());
    public static final net.minecraft.tags.TagKey<Item> REPAIR=net.minecraft.tags.TagKey.create(Registries.ITEM,Blockbranch.id("titanium_alloy_repair_materials"));
    public static final ResourceKey<EquipmentAsset> EQUIPMENT=ResourceKey.create(EquipmentAssets.ROOT_ID,Blockbranch.id("titanium_alloy"));
    // Iron boot defense, below diamond wear life; the mobility niche uses the native movement attribute.
    public static final ArmorMaterial ARMOR=new ArmorMaterial(20,java.util.Map.of(ArmorType.BOOTS,2),12,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,0,0,REPAIR,EQUIPMENT);
    public static final Item BOOTS=item("titanium_alloy_boots",new Item.Properties().humanoidArmor(ARMOR,ArmorType.BOOTS).attributes(ARMOR.createAttributes(ArmorType.BOOTS).withModifierAdded(Attributes.MOVEMENT_SPEED,new AttributeModifier(Blockbranch.id("titanium_alloy_boots_speed"),.05,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.FEET)));
    private static Block block(String name,Block base,float hardness){var id=Blockbranch.id(name);var b=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).strength(hardness,6).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return b;}
    private static Item item(String name,Item.Properties properties){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(properties.setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(ORE);out.accept(DEEPSLATE_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW);out.accept(INGOT);out.accept(ALLOY_INGOT);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->out.accept(ALLOY_BLOCK));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->out.accept(BOOTS));
    }
    private TitaniumContent(){}
}
