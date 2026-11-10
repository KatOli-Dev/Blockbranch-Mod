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

/** Lead and lead/tin alloy building sets, using native mining/blast and shape behavior. */
public final class LeadContent {
    public static final Block LEAD_ORE=block("lead_ore",Blocks.IRON_ORE,Block::new);
    public static final Block DEEPSLATE_LEAD_ORE=block("deepslate_lead_ore",Blocks.DEEPSLATE_IRON_ORE,Block::new);
    public static final Block LEAD_ALLOY_BLOCK=block("lead_alloy_block",Blocks.IRON_BLOCK,p->new Block(p.strength(6,12)));
    public static final Block LEAD_ALLOY_SLAB=block("lead_alloy_slab",LEAD_ALLOY_BLOCK,SlabBlock::new);
    public static final Block LEAD_ALLOY_STAIRS=block("lead_alloy_stairs",LEAD_ALLOY_BLOCK,p->new StairBlock(LEAD_ALLOY_BLOCK.defaultBlockState(),p){});
    public static final Block LEAD_BLOCK=block("lead_block",Blocks.IRON_BLOCK,p->new Block(p.strength(4,6)));
    public static final Block LEAD_SLAB=block("lead_slab",LEAD_BLOCK,SlabBlock::new);
    public static final Block LEAD_STAIRS=block("lead_stairs",LEAD_BLOCK,p->new StairBlock(LEAD_BLOCK.defaultBlockState(),p){});
    public static final Item RAW_LEAD=item("raw_lead");
    public static final Item LEAD_INGOT=item("lead_ingot");
    public static final Item LEAD_ALLOY_INGOT=item("lead_alloy_ingot");
    private static Block block(String name,Block base,Function<BlockBehaviour.Properties,Block> factory){
        var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));
        Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;
    }
    private static Item item(String name){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(LEAD_ORE);out.accept(DEEPSLATE_LEAD_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->{out.accept(LEAD_ALLOY_BLOCK);out.accept(LEAD_ALLOY_SLAB);out.accept(LEAD_ALLOY_STAIRS);out.accept(LEAD_BLOCK);out.accept(LEAD_SLAB);out.accept(LEAD_STAIRS);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW_LEAD);out.accept(LEAD_INGOT);out.accept(LEAD_ALLOY_INGOT);});
    }
    private LeadContent(){}
}
