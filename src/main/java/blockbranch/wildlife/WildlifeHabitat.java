package blockbranch.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/** Woodland animals accept real forest soils, without changing terrain or global tags. */
public final class WildlifeHabitat {
    public static boolean woodlandSpawn(EntityType<? extends Animal> type, LevelAccessor level,
                                        EntitySpawnReason reason, BlockPos position, RandomSource random) {
        var floor=level.getBlockState(position.below());
        boolean soil=floor.is(BlockTags.ANIMALS_SPAWNABLE_ON)||floor.is(Blocks.PODZOL)
            ||floor.is(Blocks.COARSE_DIRT)||floor.is(Blocks.ROOTED_DIRT)||floor.is(Blocks.DIRT)||floor.is(Blocks.MOSS_BLOCK);
        return soil&&(EntitySpawnReason.ignoresLightRequirements(reason)||level.getRawBrightness(position,0)>8);
    }
    public static boolean grasslandSpawn(EntityType<? extends Animal> type,LevelAccessor level,
                                         EntitySpawnReason reason,BlockPos position,RandomSource random){
        var floor=level.getBlockState(position.below());
        boolean soil=floor.is(BlockTags.ANIMALS_SPAWNABLE_ON)||floor.is(Blocks.COARSE_DIRT)||floor.is(Blocks.DIRT)||floor.is(Blocks.MOSS_BLOCK);
        return soil&&(EntitySpawnReason.ignoresLightRequirements(reason)||level.getRawBrightness(position,0)>8);
    }
    public static boolean wetlandSpawn(EntityType<? extends Animal> type,LevelAccessor level,
                                       EntitySpawnReason reason,BlockPos position,RandomSource random){
        var floor=level.getBlockState(position.below());
        boolean bank=floor.is(BlockTags.ANIMALS_SPAWNABLE_ON)||floor.is(Blocks.DIRT)||floor.is(Blocks.COARSE_DIRT)
            ||floor.is(Blocks.MUD)||floor.is(Blocks.MUDDY_MANGROVE_ROOTS)||floor.is(Blocks.CLAY)
            ||floor.is(Blocks.GRAVEL)||floor.is(Blocks.SAND)||floor.is(Blocks.MOSS_BLOCK);
        if(!bank||!EntitySpawnReason.ignoresLightRequirements(reason)&&level.getRawBrightness(position,0)<=8)return false;
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=0;y++){
            var candidate=position.offset(x,y,z);
            if(readable(level,candidate)&&level.getFluidState(candidate).is(net.minecraft.tags.FluidTags.WATER))return true;
        }
        return false;
    }
    public static boolean beaverSpawn(EntityType<? extends Animal> type,LevelAccessor level,EntitySpawnReason reason,BlockPos position,RandomSource random){
        if(!wetlandSpawn(type,level,reason,position,random))return false;
        for(int r=0;r<=6;r++)for(int z=-r;z<=r;z++)for(int x=-r;x<=r;x++)if(r==0||Math.abs(x)==r||Math.abs(z)==r)for(int y=0;y<=3;y++){
            var pos=position.offset(x,y,z);if(readable(level,pos)&&level.getBlockState(pos).is(BlockTags.LOGS))return true;
        }
        return false;
    }
    /** Freshwater biome registration supplies geography; local water checks work at any terrain elevation. */
    public static boolean perchSpawn(EntityType<? extends EuropeanPerch> type,LevelAccessor level,EntitySpawnReason reason,BlockPos position,RandomSource random){
        return level.hasChunkAt(position)&&level.getBlockState(position).is(Blocks.WATER)
            &&level.getBlockState(position.below()).is(Blocks.WATER)&&level.getBlockState(position.above()).is(Blocks.WATER);
    }
    public static boolean sealSpawn(EntityType<? extends HarborSeal> type,LevelAccessor level,EntitySpawnReason reason,BlockPos pos,RandomSource random){
        var floor=level.getBlockState(pos.below());
        if(!floor.is(BlockTags.BASE_STONE_OVERWORLD)&&!floor.is(Blocks.SNOW_BLOCK))return wetlandSpawn(type,level,reason,pos,random);
        if(!EntitySpawnReason.ignoresLightRequirements(reason)&&level.getRawBrightness(pos,0)<=8)return false;
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=0;y++){var p=pos.offset(x,y,z);if(readable(level,p)&&level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER))return true;}return false;}
    /** Null preserves the complete native predicate outside these procedural freshwater cases. */
    public static Boolean vanillaFreshwaterSpawn(EntityType<?> type,net.minecraft.world.level.ServerLevelAccessor accessor,BlockPos pos){
        if(type!=EntityTypes.SALMON&&type!=EntityTypes.SQUID)return null;
        var level=accessor.getLevel();
        if(!(level.getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator generator))return null;
        String biome=accessor.getBiome(pos).unwrapKey().orElseThrow().identifier().toString();
        boolean freshwater=switch(biome){
            case "minecraft:river","minecraft:frozen_river","minecraft:swamp","minecraft:mangrove_swamp",
                "blockbranch:open_inland_lake_sediment_bed","blockbranch:open_routed_river_channel",
                "blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"->true;
            default->false;
        };
        if(!freshwater)return null;
        int water=blockbranch.realistic.WildlifeWaterLevel.at(level,pos);
        if(water==blockbranch.realistic.DrainageNetwork.DRY)return null;
        // Preserve the installed native 13-block surface band and water predicates.
        return pos.getY()>=water-13&&pos.getY()<=water
            &&accessor.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER)
            &&accessor.getBlockState(pos.above()).is(Blocks.WATER);
    }
    /** Null keeps every other rabbit/biome predicate intact. No entity ticking or terrain edits. */
    public static Boolean desertRabbitSpawn(EntityType<?> type,net.minecraft.world.level.ServerLevelAccessor level,BlockPos pos){
        if(type!=EntityTypes.RABBIT||!(level.getLevel().getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator)
            ||!level.getBiome(pos).is(blockbranch.Blockbranch.id("continental_gravel_desert")))return null;
        if(level.getRawBrightness(pos,0)<=8)return false;
        var floor=level.getBlockState(pos.below());
        if(floor.is(Blocks.GRASS_BLOCK))return true;
        if(!floor.is(Blocks.SAND)&&!floor.is(Blocks.DIRT)&&!floor.is(Blocks.COARSE_DIRT))return false;
        // Vegetated pockets, not sterile pavement/gravel. At most 13×13×5 local reads.
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)for(int dy=-2;dy<=2;dy++){
            var candidate=pos.offset(dx,dy,dz);if(!readable(level,candidate))continue;
            if(level instanceof net.minecraft.server.level.ServerLevel server&&!blockbranch.cubic.VerticalRuntime.active(server,candidate.getX(),candidate.getY(),candidate.getZ()))continue;
            var state=level.getBlockState(candidate);
            if(state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.SHORT_GRASS)||state.is(Blocks.SHORT_DRY_GRASS)
                ||state.is(blockbranch.realistic.SteppeContent.SILVER_SHRUB)&&!state.getValue(blockbranch.realistic.SteppeShrubBlock.CLIPPED))return true;
        }
        return false;
    }
    private static boolean readable(LevelAccessor level,BlockPos pos){
        if(level instanceof net.minecraft.server.level.WorldGenRegion region){
            var center=region.getCenter();
            if((pos.getX()>>4)!=center.x()||(pos.getZ()>>4)!=center.z())return false;
        }
        return level.hasChunkAt(pos);
    }
    private WildlifeHabitat() {}
}
