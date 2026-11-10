package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Durable general-purpose steel, retaining iron's harvest/combat/armor access. */
public final class SteelContent {
    public static final Block MANGANESE_ORE=block("manganese_ore",Blocks.IRON_ORE);
    public static final Block DEEPSLATE_MANGANESE_ORE=block("deepslate_manganese_ore",Blocks.DEEPSLATE_IRON_ORE);
    public static final Block STEEL_BLOCK=block("steel_block",Blocks.IRON_BLOCK);
    public static final Item RAW_MANGANESE=item("raw_manganese",new Item.Properties());
    public static final Item MANGANESE_INGOT=item("manganese_ingot",new Item.Properties());
    public static final Item STEEL_INGOT=item("steel_ingot",new Item.Properties());
    public static final TagKey<Item> STEEL_REPAIR=TagKey.create(Registries.ITEM,Blockbranch.id("steel_repair_materials"));
    public static final ToolMaterial STEEL=new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL,480,6.5f,2.0f,14,STEEL_REPAIR);
    public static final Item STEEL_PICKAXE=item("steel_pickaxe",new Item.Properties().pickaxe(STEEL,1,-2.8f));
    public static final Item STEEL_SWORD=item("steel_sword",new Item.Properties().sword(STEEL,3,-2.4f));
    public static final Item STEEL_AXE=item("steel_axe",new Item.Properties().axe(STEEL,6,-3.1f));
    public static final Item STEEL_SHOVEL=item("steel_shovel",new Item.Properties().shovel(STEEL,1.5f,-3));
    public static final Item STEEL_HOE=item("steel_hoe",new Item.Properties().hoe(STEEL,-2,-1));
    public static final ResourceKey<EquipmentAsset> STEEL_EQUIPMENT=ResourceKey.create(EquipmentAssets.ROOT_ID,Blockbranch.id("steel"));
    // Iron defense and zero toughness/knockback bonuses; improved wear life, below diamond.
    public static final ArmorMaterial STEEL_ARMOR=new ArmorMaterial(24,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,6,ArmorType.LEGGINGS,5,ArmorType.BOOTS,2),9,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,0,0,STEEL_REPAIR,STEEL_EQUIPMENT);
    public static final Item STEEL_HELMET=item("steel_helmet",new Item.Properties().humanoidArmor(STEEL_ARMOR,ArmorType.HELMET));
    public static final Item STEEL_CHESTPLATE=item("steel_chestplate",new Item.Properties().humanoidArmor(STEEL_ARMOR,ArmorType.CHESTPLATE));
    public static final Item STEEL_LEGGINGS=item("steel_leggings",new Item.Properties().humanoidArmor(STEEL_ARMOR,ArmorType.LEGGINGS));
    public static final Item STEEL_BOOTS=item("steel_boots",new Item.Properties().humanoidArmor(STEEL_ARMOR,ArmorType.BOOTS));
    private static Block block(String name,Block base){var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;}
    private static Item item(String name,Item.Properties p){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(p.setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(MANGANESE_ORE);out.accept(DEEPSLATE_MANGANESE_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->out.accept(STEEL_BLOCK));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW_MANGANESE);out.accept(MANGANESE_INGOT);out.accept(STEEL_INGOT);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(STEEL_PICKAXE);out.accept(STEEL_AXE);out.accept(STEEL_SHOVEL);out.accept(STEEL_HOE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{out.accept(STEEL_SWORD);out.accept(STEEL_AXE);out.accept(STEEL_HELMET);out.accept(STEEL_CHESTPLATE);out.accept(STEEL_LEGGINGS);out.accept(STEEL_BOOTS);});
    }
    private SteelContent(){}
}
