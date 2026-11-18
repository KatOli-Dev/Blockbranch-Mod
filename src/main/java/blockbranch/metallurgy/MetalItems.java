package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Registers a plain metallurgy item under {@code blockbranch:<name>}. Registration order is the caller's class-load order. */
final class MetalItems {
    static Item item(String name, Item.Properties properties) {
        var id = Blockbranch.id(name);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(properties.setId(ResourceKey.create(Registries.ITEM, id))));
    }

    static Item item(String name) { return item(name, new Item.Properties()); }

    private MetalItems() {}
}
