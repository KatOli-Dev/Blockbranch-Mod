package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
/** Iron-level magnetite-lined equipment; fragments do not introduce another alloy. */
public final class MagnetiteContent {
    public static final Block ORE=ore("magnetite_ore",Blocks.IRON_ORE);
    public static final Block DEEPSLATE_ORE=ore("deepslate_magnetite_ore",Blocks.DEEPSLATE_IRON_ORE);
    public static final Item FRAGMENT=item("magnetite_fragment",new Item.Properties());
    public static final ResourceKey<EquipmentAsset> EQUIPMENT=ResourceKey.create(EquipmentAssets.ROOT_ID,Blockbranch.id("magnetite"));
    public static final ArmorMaterial ARMOR=new ArmorMaterial(ArmorMaterials.IRON.durability(),java.util.Map.of(ArmorType.BOOTS,2),ArmorMaterials.IRON.enchantmentValue(),net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,0,0,ArmorMaterials.IRON.repairIngredient(),EQUIPMENT);
    public static final Item BOOTS=item("magnetite_boots",new Item.Properties().humanoidArmor(ARMOR,ArmorType.BOOTS).component(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(java.util.List.of(net.minecraft.network.chat.Component.translatable("item.blockbranch.magnetite_boots.effect")))));
    private static Block ore(String name,Block base){var id=Blockbranch.id(name);var b=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return b;}
    private static Item item(String name,Item.Properties properties){var id=Blockbranch.id(name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(properties.setId(ResourceKey.create(Registries.ITEM,id))));}
    public static void initialize(){
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(ORE);out.accept(DEEPSLATE_ORE);});
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->out.accept(FRAGMENT));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->out.accept(BOOTS));
        MagnetiteAttraction.register();
    }
    private MagnetiteContent(){}
}
