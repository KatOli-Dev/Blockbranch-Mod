package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
/** Diamond-like ore that always crumbles into inert dust, including with Silk Touch. */
public final class FoolsDiamondContent {
 public static final Block ORE=ore("fools_diamond_ore",Blocks.DIAMOND_ORE);
 public static final Block DEEPSLATE_ORE=ore("deepslate_fools_diamond_ore",Blocks.DEEPSLATE_DIAMOND_ORE);
 public static final Item DUST=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("fools_diamond_dust"),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("fools_diamond_dust")))));
 private static Block ore(String name,Block base){var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;}
 public static void initialize(){CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(ORE);out.accept(DEEPSLATE_ORE);});CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->out.accept(DUST));}
 private FoolsDiamondContent(){}
}
