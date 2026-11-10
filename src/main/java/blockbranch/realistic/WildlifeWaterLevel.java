package blockbranch.realistic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Read-only package bridge; no generator API change or terrain materialization. */
public final class WildlifeWaterLevel {
    public static int at(ServerLevel level,BlockPos pos){
        if(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator)
            return generator.drainage(level.getChunkSource().randomState()).sample(pos.getX(),pos.getZ()).waterLevel();
        return DrainageNetwork.DRY;
    }
    private WildlifeWaterLevel(){}
}
