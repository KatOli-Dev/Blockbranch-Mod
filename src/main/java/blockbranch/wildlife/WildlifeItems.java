package blockbranch.wildlife;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
public final class WildlifeItems {
    public static final Item VENISON = meat("venison", 3, .3F);
    public static final Item COOKED_VENISON = meat("cooked_venison", 8, .8F);
    public static final Item DUCK = meat("duck", 2, .3F);
    public static final Item COOKED_DUCK = meat("cooked_duck", 6, .6F);
    public static final Item PERCH = meat("perch",2,.1F);
    public static final Item COOKED_PERCH = meat("cooked_perch",6,.6F);
    public static final Item PERCH_BUCKET = Registry.register(BuiltInRegistries.ITEM,Blockbranch.id("perch_bucket"),
        new MobBucketItem(WildlifeRuntime.EUROPEAN_PERCH,net.minecraft.world.level.material.Fluids.WATER,
            net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH,new Item.Properties().stacksTo(1)
                .setId(ResourceKey.create(Registries.ITEM,Blockbranch.id("perch_bucket")))));
    public static final java.util.Map<EntityType<?>,Item> SPAWN_EGGS = spawnEggs();
    private static java.util.Map<EntityType<?>,Item> spawnEggs() {
        var eggs=new java.util.LinkedHashMap<EntityType<?>,Item>();
        for(var type:java.util.List.of(WildlifeRuntime.WILD_BOAR,WildlifeRuntime.RED_DEER,WildlifeRuntime.AMERICAN_BISON,WildlifeRuntime.BROWN_BEAR,
                WildlifeRuntime.MALLARD,WildlifeRuntime.EURASIAN_BEAVER,WildlifeRuntime.EUROPEAN_PERCH,WildlifeRuntime.HARBOR_SEAL)) {
            var id=Blockbranch.id(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()+"_spawn_egg");
            eggs.put(type,Registry.register(BuiltInRegistries.ITEM,id,new SpawnEggItem(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM,id)).spawnEgg(type))));
        }
        return java.util.Collections.unmodifiableMap(eggs);
    }
    private static Item meat(String path, int nutrition, float saturation) {
        var id = Blockbranch.id(path);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id))
            .food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build())));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries->SPAWN_EGGS.values().forEach(entries::accept));
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(PERCH_BUCKET,java.util.Objects.requireNonNull(
            net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(Items.COD_BUCKET),"native fish bucket dispenser behavior"));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(VENISON);entries.accept(COOKED_VENISON);entries.accept(DUCK);entries.accept(COOKED_DUCK);entries.accept(PERCH);entries.accept(COOKED_PERCH);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries->entries.accept(PERCH_BUCKET));
    }
    private WildlifeItems() {}
}
