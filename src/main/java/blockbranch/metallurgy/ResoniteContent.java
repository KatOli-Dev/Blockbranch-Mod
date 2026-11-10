package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
/** Resonite stays a crystal resource and placeable signaling material, without another alloy. */
public final class ResoniteContent {
    public static final SoundEvent CHIME=Registry.register(BuiltInRegistries.SOUND_EVENT,Blockbranch.id("resonite.chime"),SoundEvent.createVariableRangeEvent(Blockbranch.id("resonite.chime")));
    public static final Block ORE=block("resonite_ore",Blocks.IRON_ORE);
    public static final Block DEEPSLATE_ORE=block("deepslate_resonite_ore",Blocks.DEEPSLATE_IRON_ORE);
    public static final Block BLOCK=block("resonite_block",Blocks.AMETHYST_BLOCK);
    public static final Item CRYSTAL=Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("resonite_crystal"),new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("resonite_crystal")))));
    private static Block block(String name,Block base){var id=Blockbranch.id(name);var b=Registry.register(BuiltInRegistries.BLOCK,id,new ResoniteBlock(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return b;}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(ORE);out.accept(DEEPSLATE_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(out->out.accept(BLOCK));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->out.accept(CRYSTAL));
    }
    private ResoniteContent(){}
}
