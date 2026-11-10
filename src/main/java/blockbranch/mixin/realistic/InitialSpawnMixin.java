package blockbranch.mixin.realistic;

import blockbranch.realistic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** This method is called only while ServerLevelData.isInitialized() is false. */
@Mixin(MinecraftServer.class)
abstract class InitialSpawnMixin {
    @Redirect(method="setInitialSpawn",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkGenerator;getOrigin(Lnet/minecraft/world/level/levelgen/RandomState;)Lnet/minecraft/world/level/ChunkPos;"))
    private static ChunkPos blockbranch$preferredRegion(ChunkGenerator generator,RandomState random,ServerLevel level,ServerLevelData data,boolean bonusChest,boolean debug,LevelLoadListener progress){
        return generator instanceof RealisticGenerator realistic?RealisticSpawn.origin(level,realistic,random):generator.getOrigin(random);
    }
    @Redirect(method="setInitialSpawn",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/PlayerSpawnFinder;getSpawnPosInChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos blockbranch$checkedNativeGround(ServerLevel level,ChunkPos chunk){
        boolean realistic=level.getChunkSource().getGenerator() instanceof RealisticGenerator;
        if(realistic&&!RealisticSpawn.selectionBounds(level).intersectsChunk(chunk.getMinBlockX(),chunk.getMinBlockZ()))return null;
        var found=PlayerSpawnFinder.getSpawnPosInChunk(level,chunk);
        return !realistic||found==null||RealisticSpawn.allowed(level,found)?found:null;
    }
    @Inject(method="setInitialSpawn",at=@At("TAIL"))
    private static void blockbranch$finalSpawn(ServerLevel level,ServerLevelData data,boolean bonusChest,boolean debug,LevelLoadListener progress,CallbackInfo ci){
        if(!debug)RealisticSpawn.validateInitial(level,data);
    }
}
