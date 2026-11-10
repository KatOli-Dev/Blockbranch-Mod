package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
/** Native 26.3 weapon builders retain combat, spear controls, tool interactions and durability. */
public final class UraniumContent {
 public static final Block ORE=ore("uranium_ore",Blocks.DIAMOND_ORE),DEEPSLATE_ORE=ore("deepslate_uranium_ore",Blocks.DEEPSLATE_DIAMOND_ORE);
 public static final Item RAW=item("raw_uranium",exposed(new Item.Properties())),INGOT=item("uranium_ingot",exposed(new Item.Properties())),TIP=item("uranium_tip",exposed(new Item.Properties()));
 public static final Item SWORD=weapon("uranium_netherite_sword",new Item.Properties().sword(ToolMaterial.NETHERITE,3,-2.4F).fireResistant()),AXE=weapon("uranium_netherite_axe",new Item.Properties().axe(ToolMaterial.NETHERITE,5,-3).fireResistant()),SPEAR=weapon("uranium_netherite_spear",new Item.Properties().spear(ToolMaterial.NETHERITE,1.15F,1.2F,.4F,2.5F,9,5.5F,5.1F,8.75F,4.6F).fireResistant());
 public static final Holder.Reference<MobEffect> RADIATION=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Blockbranch.id("radiation"),new RadiationEffect());
 public static final ResourceKey<net.minecraft.world.damagesource.DamageType> RADIATION_DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,Blockbranch.id("radiation"));
 public static final TagKey<Item> RADIOACTIVE=tag("radioactive"),FRAGILE_ARMOR=tag("radiation_fragile_armor"),SHIELDING_ARMOR=tag("radiation_shielding_armor"),WEAPONS=tag("uranium_tipped_weapons");
 private static TagKey<Item> tag(String name){return TagKey.create(Registries.ITEM,Blockbranch.id(name));}
 private static Block ore(String name,Block base){var id=Blockbranch.id(name);var block=Registry.register(BuiltInRegistries.BLOCK,id,new Block(BlockBehaviour.Properties.ofFullCopy(base).setId(ResourceKey.create(Registries.BLOCK,id))));Registry.register(BuiltInRegistries.ITEM,id,new BlockItem(block,exposed(new Item.Properties()).setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix()));return block;}
 private static Item item(String name,Item.Properties properties){return Registry.register(BuiltInRegistries.ITEM,Blockbranch.id(name),new Item(properties.setId(ResourceKey.create(Registries.ITEM,Blockbranch.id(name)))));}
 private static Item.Properties exposed(Item.Properties p){return p.component(DataComponents.LORE,new ItemLore(java.util.List.of(net.minecraft.network.chat.Component.translatable("item.blockbranch.radioactive.effect"))));}
 // These are the native 26.3 registration parameters; native probes compare every bound component.
 private static Item weapon(String name,Item.Properties properties){properties.component(DataComponents.LORE,new ItemLore(java.util.List.of(net.minecraft.network.chat.Component.translatable("item.blockbranch.uranium_weapon.effect"),net.minecraft.network.chat.Component.translatable("item.blockbranch.radioactive.effect"))));return item(name,properties);}
 public static void initialize(){
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out->{out.accept(ORE);out.accept(DEEPSLATE_ORE);});
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out->{out.accept(RAW);out.accept(INGOT);out.accept(TIP);});
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out->{out.accept(SWORD);out.accept(AXE);out.accept(SPEAR);});
 }
 private UraniumContent(){}
}
