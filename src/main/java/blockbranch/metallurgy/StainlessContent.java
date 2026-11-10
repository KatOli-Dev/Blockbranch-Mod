package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Nickel/chromium alloying, polished building pieces and durable native equipment. */
public final class StainlessContent {
    public static final Block NICKEL_ORE=block("nickel_ore",Blocks.IRON_ORE,Block::new);
    public static final Block DEEPSLATE_NICKEL_ORE=block("deepslate_nickel_ore",Blocks.DEEPSLATE_IRON_ORE,Block::new);
    public static final Block STAINLESS_STEEL_BLOCK=block("stainless_steel_block",Blocks.IRON_BLOCK,Block::new);
    public static final Block STAINLESS_STEEL_SLAB=block("stainless_steel_slab",STAINLESS_STEEL_BLOCK,SlabBlock::new);
    public static final Block STAINLESS_STEEL_STAIRS=block("stainless_steel_stairs",STAINLESS_STEEL_BLOCK,p->new StairBlock(STAINLESS_STEEL_BLOCK.defaultBlockState(),p){});
    public static final Block CHROMIUM_ORE=block("chromium_ore",Blocks.IRON_ORE,p->new Block(p.strength(4,6)));
    public static final Block DEEPSLATE_CHROMIUM_ORE=block("deepslate_chromium_ore",Blocks.DEEPSLATE_IRON_ORE,p->new Block(p.strength(6,6)));
    public static final Item RAW_NICKEL=item("raw_nickel");
    public static final Item NICKEL_INGOT=item("nickel_ingot");
    public static final Item STAINLESS_STEEL_INGOT=item("stainless_steel_ingot");
    public static final Item RAW_CHROMIUM=item("raw_chromium");
    public static final Item CHROMIUM_INGOT=item("chromium_ingot");
    public static final net.minecraft.tags.TagKey<Item> REPAIR=net.minecraft.tags.TagKey.create(Registries.ITEM,Blockbranch.id("stainless_steel_repair_materials"));
    // Longer wear life than steel, at iron speed/access/attack strength; diamond remains faster/higher-tier.
    public static final ToolMaterial MATERIAL=new ToolMaterial(net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL,960,6,2,12,REPAIR);
    public static final Item STAINLESS_STEEL_PICKAXE=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("stainless_steel_pickaxe"),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("stainless_steel_pickaxe"))).pickaxe(MATERIAL,1,-2.8f)));
    public static final Item STAINLESS_STEEL_SWORD=gear("stainless_steel_sword",new Item.Properties().sword(MATERIAL,3,-2.4f));
    public static final Item STAINLESS_STEEL_AXE=gear("stainless_steel_axe",new Item.Properties().axe(MATERIAL,6,-3.1f));
    public static final Item STAINLESS_STEEL_SHOVEL=gear("stainless_steel_shovel",new Item.Properties().shovel(MATERIAL,1.5f,-3));
    public static final Item STAINLESS_STEEL_HOE=gear("stainless_steel_hoe",new Item.Properties().hoe(MATERIAL,-2,-1));
    public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> EQUIPMENT=ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,Blockbranch.id("stainless_steel"));
    // Iron defense, longer wear life and lower enchantability than steel; no toughness or knockback bonus.
    public static final net.minecraft.world.item.equipment.ArmorMaterial ARMOR=new net.minecraft.world.item.equipment.ArmorMaterial(30,java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.HELMET,2,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE,6,net.minecraft.world.item.equipment.ArmorType.LEGGINGS,5,net.minecraft.world.item.equipment.ArmorType.BOOTS,2),8,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,0,0,REPAIR,EQUIPMENT);
    public static final Item STAINLESS_STEEL_HELMET=gear("stainless_steel_helmet",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.HELMET));
    public static final Item STAINLESS_STEEL_CHESTPLATE=gear("stainless_steel_chestplate",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
    public static final Item STAINLESS_STEEL_LEGGINGS=gear("stainless_steel_leggings",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
    public static final Item STAINLESS_STEEL_BOOTS=gear("stainless_steel_boots",new Item.Properties().humanoidArmor(ARMOR,net.minecraft.world.item.equipment.ArmorType.BOOTS));
    private static Item gear(String name,Item.Properties properties){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(properties.setId(ResourceKey.create(Registries.ITEM,id))));}
    private static Block block(String name,Block base,Function<BlockBehaviour.Properties,Block> factory){
        var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));
        Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;
    }
    private static Item item(String name){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(NICKEL_ORE);out.accept(DEEPSLATE_NICKEL_ORE);out.accept(CHROMIUM_ORE);out.accept(DEEPSLATE_CHROMIUM_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->{out.accept(STAINLESS_STEEL_BLOCK);out.accept(STAINLESS_STEEL_SLAB);out.accept(STAINLESS_STEEL_STAIRS);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW_NICKEL);out.accept(NICKEL_INGOT);out.accept(STAINLESS_STEEL_INGOT);out.accept(RAW_CHROMIUM);out.accept(CHROMIUM_INGOT);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(STAINLESS_STEEL_PICKAXE);out.accept(STAINLESS_STEEL_AXE);out.accept(STAINLESS_STEEL_SHOVEL);out.accept(STAINLESS_STEEL_HOE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{out.accept(STAINLESS_STEEL_SWORD);out.accept(STAINLESS_STEEL_AXE);out.accept(STAINLESS_STEEL_HELMET);out.accept(STAINLESS_STEEL_CHESTPLATE);out.accept(STAINLESS_STEEL_LEGGINGS);out.accept(STAINLESS_STEEL_BOOTS);});
    }
    private StainlessContent(){}
}
