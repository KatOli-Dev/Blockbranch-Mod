package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
/** Deep, diamond-gated tungsten and slow, long-wearing mining tools. */
public final class TungstenContent {
 public static final Block TUNGSTEN_ORE=ore("tungsten_ore",Blocks.IRON_ORE,6);
 public static final Block DEEPSLATE_TUNGSTEN_ORE=ore("deepslate_tungsten_ore",Blocks.DEEPSLATE_IRON_ORE,8);
 public static final Item RAW_TUNGSTEN=item("raw_tungsten",new Item.Properties());
 public static final Item TUNGSTEN_INGOT=item("tungsten_ingot",new Item.Properties());
 public static final Item TUNGSTEN_STEEL_INGOT=item("tungsten_steel_ingot",new Item.Properties());
 public static final TagKey<Item> REPAIR=TagKey.create(Registries.ITEM,Blockbranch.id("tungsten_steel_repair_materials"));
 // Diamond harvest access, iron attack strength, slower than iron mining; no sword/armor tier.
 public static final ToolMaterial MATERIAL=new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,2048,5,2,10,REPAIR);
 public static final Item TUNGSTEN_STEEL_PICKAXE=item("tungsten_steel_pickaxe",new Item.Properties().pickaxe(MATERIAL,1,-2.8f));
 public static final Item TUNGSTEN_STEEL_AXE=item("tungsten_steel_axe",new Item.Properties().axe(MATERIAL,6,-3.1f));
 public static final Item TUNGSTEN_STEEL_SHOVEL=item("tungsten_steel_shovel",new Item.Properties().shovel(MATERIAL,1.5f,-3));
 public static final Item TUNGSTEN_STEEL_HOE=item("tungsten_steel_hoe",new Item.Properties().hoe(MATERIAL,-2,-1));
 private static Block ore(String name,Block base,float hardness){var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).strength(hardness,6).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;}
 private static Item item(String name,Item.Properties p){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(p.setId(ResourceKey.create(Registries.ITEM,id))));}
 public static void initialize(){
 CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(TUNGSTEN_ORE);out.accept(DEEPSLATE_TUNGSTEN_ORE);});
 CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW_TUNGSTEN);out.accept(TUNGSTEN_INGOT);out.accept(TUNGSTEN_STEEL_INGOT);});
 CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{out.accept(TUNGSTEN_STEEL_PICKAXE);out.accept(TUNGSTEN_STEEL_AXE);out.accept(TUNGSTEN_STEEL_SHOVEL);out.accept(TUNGSTEN_STEEL_HOE);});
 }
 private TungstenContent(){}
}
