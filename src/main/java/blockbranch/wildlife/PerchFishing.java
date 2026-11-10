package blockbranch.wildlife;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
/** Existing rod fishing: replace a modest share of freshwater cod/salmon catches, never add extra drops. */
final class PerchFishing {
    static boolean freshwater(String id){return switch(id){case "minecraft:river","minecraft:swamp","blockbranch:open_inland_lake_sediment_bed","blockbranch:open_routed_river_channel","blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"->true;default->false;};}
    static void initialize(){LootTableEvents.MODIFY_DROPS.register((table,context,drops)->{
        if(!table.is(BuiltInLootTables.FISHING_FISH))return;
        var origin=context.getOptional(LootContextParams.ORIGIN);if(origin==null)return;
        var biome=context.getLevel().getBiome(BlockPos.containing(origin)).unwrapKey();
        if(biome.isEmpty()||!freshwater(biome.get().identifier().toString()))return;
        for(int i=0;i<drops.size();i++){var stack=drops.get(i);if((stack.is(Items.COD)||stack.is(Items.SALMON))&&context.getRandom().nextInt(5)==0)drops.set(i,new ItemStack(WildlifeItems.PERCH,stack.getCount()));}
    });}
    private PerchFishing(){}
}
