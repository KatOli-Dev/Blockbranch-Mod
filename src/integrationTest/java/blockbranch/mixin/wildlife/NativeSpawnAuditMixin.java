package blockbranch.mixin.wildlife;

import blockbranch.wildlife.WildlifeSpawnAudit;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Observes existing spawn decisions only, with no changed return value or extra attempt. */
@Mixin(NaturalSpawner.class)
abstract class NativeSpawnAuditMixin {
    @Inject(method="getFilteredSpawningCategories",at=@At("RETURN"))
    private static void categories(NaturalSpawner.SpawnState state,boolean enemies,boolean persistent,CallbackInfoReturnable<List<MobCategory>> result){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.categories(state,persistent,result.getReturnValue());}
    @Inject(method="spawnForChunk",at=@At("HEAD"))
    private static void chunk(ServerLevel level,LevelChunk chunk,NaturalSpawner.SpawnState state,List<MobCategory> categories,CallbackInfo callback){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.candidateChunk(level,chunk.getPos(),categories);}
    @Inject(method="spawnCategoryForChunk",at=@At("HEAD"))
    private static void enter(MobCategory category,ServerLevel level,LevelChunk chunk,NaturalSpawner.SpawnPredicate predicate,NaturalSpawner.AfterSpawnCallback after,CallbackInfo callback){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.enter(level,chunk.getPos(),category);}
    @Inject(method="spawnCategoryForChunk",at=@At("RETURN"))
    private static void leave(MobCategory category,ServerLevel level,LevelChunk chunk,NaturalSpawner.SpawnPredicate predicate,NaturalSpawner.AfterSpawnCallback after,CallbackInfo callback){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.leave();}
    @Inject(method="getRandomSpawnMobAt",at=@At("RETURN"))
    private static void select(ServerLevel level,StructureManager structures,ChunkGenerator generator,MobCategory category,RandomSource random,BlockPos pos,CallbackInfoReturnable<Optional<MobSpawnSettings.SpawnerData>> result){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.selection(level,pos,category,result.getReturnValue());}
    @Inject(method="isValidSpawnPostitionForType",at=@At("RETURN"))
    private static void type(ServerLevel level,MobCategory category,StructureManager structures,ChunkGenerator generator,MobSpawnSettings.SpawnerData data,BlockPos.MutableBlockPos pos,double distance,CallbackInfoReturnable<Boolean> result){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.validType(level,pos,data,result.getReturnValue());}
    @Inject(method="isValidPositionForMob",at=@At("RETURN"))
    private static void mob(ServerLevel level,Mob mob,double distance,CallbackInfoReturnable<Boolean> result){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.validMob(level,mob,result.getReturnValue());}
}
