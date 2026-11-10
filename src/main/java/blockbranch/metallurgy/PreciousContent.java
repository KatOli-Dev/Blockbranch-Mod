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

/** Silver equipment/decor and rare platinum material/decor foundations. */
public final class PreciousContent {
    public static final Block SILVER_ORE=block("silver_ore",Blocks.IRON_ORE,Block::new);
    public static final Block DEEPSLATE_SILVER_ORE=block("deepslate_silver_ore",Blocks.DEEPSLATE_IRON_ORE,Block::new);
    public static final Block PLATINUM_BLOCK=block("platinum_block",Blocks.IRON_BLOCK,Block::new);
    public static final Block PLATINUM_SLAB=block("platinum_slab",PLATINUM_BLOCK,SlabBlock::new);
    public static final Block PLATINUM_STAIRS=block("platinum_stairs",PLATINUM_BLOCK,p->new StairBlock(PLATINUM_BLOCK.defaultBlockState(),p){});
    public static final Block SILVER_BLOCK=block("silver_block",Blocks.IRON_BLOCK,p->new Block(p.strength(4,6)));
    public static final Block SILVER_SLAB=block("silver_slab",SILVER_BLOCK,SlabBlock::new);
    public static final Block SILVER_STAIRS=block("silver_stairs",SILVER_BLOCK,p->new StairBlock(SILVER_BLOCK.defaultBlockState(),p){});
    public static final Block PLATINUM_ORE=block("platinum_ore",Blocks.IRON_ORE,p->new Block(p.strength(5,6)));
    public static final Block DEEPSLATE_PLATINUM_ORE=block("deepslate_platinum_ore",Blocks.DEEPSLATE_IRON_ORE,p->new Block(p.strength(7,6)));
    public static final Item RAW_PLATINUM=item("raw_platinum");
    public static final net.minecraft.tags.TagKey<Item> SILVER_REPAIR=net.minecraft.tags.TagKey.create(Registries.ITEM,Blockbranch.id("silver_repair_materials"));
    // Faster than iron/steel, shorter wear life; no higher harvest or attack tier.
    public static final ToolMaterial SILVER=new ToolMaterial(net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL,224,7,2,14,SILVER_REPAIR);
    public static final Item SILVER_PICKAXE=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("silver_pickaxe"),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("silver_pickaxe"))).pickaxe(SILVER,1,-2.8f)));
    public static final Item RAW_SILVER=item("raw_silver");
    public static final Item SILVER_INGOT=item("silver_ingot");
    public static final Item PLATINUM_INGOT=item("platinum_ingot");
    private static Block block(String name,Block base,Function<BlockBehaviour.Properties,Block> factory){
        var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));
        Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;
    }
    private static Item item(String name){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(SILVER_ORE);out.accept(DEEPSLATE_SILVER_ORE);out.accept(PLATINUM_ORE);out.accept(DEEPSLATE_PLATINUM_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->{out.accept(PLATINUM_BLOCK);out.accept(PLATINUM_SLAB);out.accept(PLATINUM_STAIRS);out.accept(SILVER_BLOCK);out.accept(SILVER_SLAB);out.accept(SILVER_STAIRS);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW_SILVER);out.accept(RAW_PLATINUM);out.accept(SILVER_INGOT);out.accept(PLATINUM_INGOT);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(SILVER_PICKAXE));
    }
    private PreciousContent(){}
}
