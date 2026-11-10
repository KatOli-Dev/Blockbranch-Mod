package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;

/** Used only by Minecraft's new-world initial-spawn procedure; never moves an established spawn. */
public final class RealisticSpawn {
    public static SpawnRegions.Bounds bounds(ServerLevel level){
        var b=level.getWorldBorder();
        return new SpawnRegions.Bounds(Math.max(b.getMinX(),-ColumnLimits.BORDER_LIMIT),Math.max(b.getMinZ(),-ColumnLimits.BORDER_LIMIT),Math.min(b.getMaxX(),ColumnLimits.BORDER_LIMIT),Math.min(b.getMaxZ(),ColumnLimits.BORDER_LIMIT));
    }
    public static int playerRadius(ServerLevel level){return Math.max(0,level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.RESPAWN_RADIUS));}
    public static SpawnRegions.Bounds selectionBounds(ServerLevel level){return bounds(level).reserve(playerRadius(level));}
    public static ChunkPos origin(ServerLevel level,RealisticGenerator generator,RandomState random){
        var b=bounds(level);var selected=SpawnRegions.select(random.seed(),selectionBounds(level),generator.drainage(random),new ClimateField(random.seed()));
        Blockbranch.LOGGER.info("REALISTIC_SPAWN_PREFERRED seed={} border={} preferred={} clearance={} policy=128000m nativeSafeGround=true playerRadius={}",random.seed(),b,selected,b.clearance(selected.x(),selected.z()),playerRadius(level));
        return new ChunkPos(selected.x()>>4,selected.z()>>4);
    }
    public static boolean allowed(ServerLevel level,BlockPos pos){
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator g))return true;
        var random=level.getChunkSource().randomState();
        return selectionBounds(level).allows(pos.getX(),pos.getZ())&&SpawnRegions.eligible(pos.getX(),pos.getZ(),g.drainage(random),new ClimateField(random.seed()));
    }
    public static void validateInitial(ServerLevel level,ServerLevelData data){
        if(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator g))return;
        var p=data.getRespawnData().pos();var random=level.getChunkSource().randomState();
        if(!allowed(level,p)||p.getY()<=g.drainage(random).sample(p.getX(),p.getZ()).height())
            throw new IllegalStateException("The native safe-ground search did not find a valid mild spawn 128 km inside the border. No unsafe fallback is accepted.");
        var c=new ClimateField(random.seed()).sample(p.getX(),p.getZ(),g.drainage(random)::sample);
        Blockbranch.LOGGER.info("REALISTIC_SPAWN_FINAL_PASS seed={} feet={} clearance={} latitude={} climate={}",random.seed(),p,bounds(level).clearance(p.getX(),p.getZ()),Math.min(1,Math.abs((double)p.getZ())/2097152.),c);
    }
    private RealisticSpawn(){}
}
