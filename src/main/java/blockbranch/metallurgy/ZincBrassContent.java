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

/** Zinc/brass supplies building materials, with no additional equipment tier or machinery. */
public final class ZincBrassContent {
    public static final Block ZINC_ORE=block("zinc_ore",Blocks.IRON_ORE,Block::new);
    public static final Block DEEPSLATE_ZINC_ORE=block("deepslate_zinc_ore",Blocks.DEEPSLATE_IRON_ORE,Block::new);
    public static final Block BRASS_BLOCK=block("brass_block",Blocks.COPPER_BLOCK.weathering().unaffected(),Block::new);
    public static final Block BRASS_SLAB=block("brass_slab",BRASS_BLOCK,SlabBlock::new);
    public static final Block BRASS_STAIRS=block("brass_stairs",BRASS_BLOCK,p->new StairBlock(BRASS_BLOCK.defaultBlockState(),p){});
    public static final Block BRASS_BARS=block("brass_bars",Blocks.IRON_BARS,p->new IronBarsBlock(p){});
    public static final Item RAW_ZINC=item("raw_zinc");
    public static final Item ZINC_INGOT=item("zinc_ingot");
    public static final Item BRASS_INGOT=item("brass_ingot");
    private static Block block(String name,Block base,Function<BlockBehaviour.Properties,Block> factory){
        var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));
        Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;
    }
    private static Item item(String name){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(ZINC_ORE);out.accept(DEEPSLATE_ZINC_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->{out.accept(BRASS_BLOCK);out.accept(BRASS_SLAB);out.accept(BRASS_STAIRS);out.accept(BRASS_BARS);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW_ZINC);out.accept(ZINC_INGOT);out.accept(BRASS_INGOT);});
    }
    private ZincBrassContent(){}
}
