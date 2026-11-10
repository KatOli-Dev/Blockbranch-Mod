package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Approved metallurgy content; later materials do not register unfinished behavior. */
public final class MetallurgyContent {
    public static final Block TIN_ORE=block("tin_ore",Blocks.IRON_ORE);
    public static final Block DEEPSLATE_TIN_ORE=block("deepslate_tin_ore",Blocks.DEEPSLATE_IRON_ORE);
    public static final Block BRONZE_BLOCK=block("bronze_block",Blocks.COPPER_BLOCK.weathering().unaffected());
    public static final Item RAW_TIN=item("raw_tin",new Item.Properties());
    public static final Item TIN_INGOT=item("tin_ingot",new Item.Properties());
    public static final Item BRONZE_INGOT=item("bronze_ingot",new Item.Properties());
    public static final TagKey<Item> BRONZE_REPAIR=TagKey.create(Registries.ITEM,Blockbranch.id("bronze_repair_materials"));
    // Between copper and iron in speed/durability; copper mining access preserves the need for iron.
    public static final ToolMaterial BRONZE=new ToolMaterial(BlockTags.INCORRECT_FOR_COPPER_TOOL,220,5.5f,1.5f,13,BRONZE_REPAIR);
    public static final Item BRONZE_PICKAXE=item("bronze_pickaxe",new Item.Properties().pickaxe(BRONZE,1.0f,-2.8f));
    public static final Item BRONZE_SWORD=item("bronze_sword",new Item.Properties().sword(BRONZE,3.0f,-2.4f));
    public static final Item BRONZE_AXE=item("bronze_axe",new Item.Properties().axe(BRONZE,6.0f,-3.15f));
    public static final Item BRONZE_SHOVEL=item("bronze_shovel",new Item.Properties().shovel(BRONZE,1.5f,-3.0f));
    public static final Item BRONZE_HOE=item("bronze_hoe",new Item.Properties().hoe(BRONZE,-1.5f,-1.5f));
    public static final ResourceKey<EquipmentAsset> BRONZE_EQUIPMENT=ResourceKey.create(EquipmentAssets.ROOT_ID,Blockbranch.id("bronze"));
    // Full-set defense 12 and durability multiplier 13 sit below iron (15/15), above copper (10/11).
    public static final ArmorMaterial BRONZE_ARMOR=new ArmorMaterial(13,java.util.Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,5,ArmorType.LEGGINGS,4,ArmorType.BOOTS,1),9,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_COPPER,0,0,BRONZE_REPAIR,BRONZE_EQUIPMENT);
    public static final Item BRONZE_HELMET=item("bronze_helmet",new Item.Properties().humanoidArmor(BRONZE_ARMOR,ArmorType.HELMET));
    public static final Item BRONZE_CHESTPLATE=item("bronze_chestplate",new Item.Properties().humanoidArmor(BRONZE_ARMOR,ArmorType.CHESTPLATE));
    public static final Item BRONZE_LEGGINGS=item("bronze_leggings",new Item.Properties().humanoidArmor(BRONZE_ARMOR,ArmorType.LEGGINGS));
    public static final Item BRONZE_BOOTS=item("bronze_boots",new Item.Properties().humanoidArmor(BRONZE_ARMOR,ArmorType.BOOTS));

    private static Block block(String name,Block base){
        var id=Blockbranch.id(name);
        var block=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));
        item(name,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));
        return block;
    }
    private static Item item(String name,Item.Properties properties){return item(name,new Item(properties.setId(ResourceKey.create(Registries.ITEM,Blockbranch.id(name)))));}
    private static Item item(String name,Item item){return Registry.register(BuiltInRegistries.ITEM,Blockbranch.id(name),item);}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(TIN_ORE);out.accept(DEEPSLATE_TIN_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->out.accept(BRONZE_BLOCK));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW_TIN);out.accept(TIN_INGOT);out.accept(BRONZE_INGOT);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(BRONZE_PICKAXE);out.accept(BRONZE_SHOVEL);out.accept(BRONZE_HOE);out.accept(BRONZE_AXE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{out.accept(BRONZE_SWORD);out.accept(BRONZE_AXE);out.accept(BRONZE_HELMET);out.accept(BRONZE_CHESTPLATE);out.accept(BRONZE_LEGGINGS);out.accept(BRONZE_BOOTS);});
    }
    private MetallurgyContent(){}
}
