package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.ColumnLimits;
import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

/** Actual new-world initialization plus preservation of an explicitly established saved spawn. */
public final class RealisticSpawnSmoke {
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception{
        var r=level.getChunkSource().randomState();var b=RealisticSpawn.bounds(level);var wb=level.getWorldBorder();
        Blockbranch.LOGGER.info("SPAWN_ACTUAL_BORDER minX={} minZ={} maxX={} maxZ={} size={} effective={}",wb.getMinX(),wb.getMinZ(),wb.getMaxX(),wb.getMaxZ(),wb.getSize(),b);
        require(wb.getMinX()==-ColumnLimits.BORDER_LIMIT&&wb.getMaxX()==ColumnLimits.BORDER_LIMIT&&wb.getMinZ()==-ColumnLimits.BORDER_LIMIT&&wb.getMaxZ()==ColumnLimits.BORDER_LIMIT,"Actual default border matches configured 4194272m square");
        var actual=level.getRespawnData();var record=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).resolveSibling("established-spawn.json");
        if(reload){
            var saved=JsonParser.parseString(Files.readString(record)).getAsJsonObject();var expected=new BlockPos(saved.get("x").getAsInt(),saved.get("y").getAsInt(),saved.get("z").getAsInt());
            require(actual.pos().equals(expected)&&actual.yaw()==47&&actual.pitch()==-12,"Existing saved spawn coordinates/yaw/pitch unchanged");
            Blockbranch.LOGGER.info("REALISTIC_SPAWN_EXISTING_RELOAD_PASS seed={} spawn={}",r.seed(),actual);return;
        }
        var p=actual.pos();require(RealisticSpawn.allowed(level,p),"Actual native safe-ground result is moderate, dry and at least128000m inside every edge");
        require(level.getBlockState(p).getCollisionShape(level,p).isEmpty()&&level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()&&level.getFluidState(p).isEmpty(),"Native standing/head space stays clear and dry");
        require(!level.getBlockState(p.below()).getCollisionShape(level,p.below()).isEmpty(),"Native solid support retained");
        var preferred=SpawnRegions.select(r.seed(),RealisticSpawn.selectionBounds(level),g.drainage(r),new ClimateField(r.seed()));
        require(Math.abs(p.getX()-preferred.x())<=96&&Math.abs(p.getZ()-preferred.z())<=96,"Original native bounded local safe-ground search retained");
        Blockbranch.LOGGER.info("REALISTIC_SPAWN_NEW_NATIVE_PASS seed={} preferred={} final={} clearance={} latitude={}",r.seed(),preferred,p,b.clearance(p.getX(),p.getZ()),Math.abs((double)p.getZ())/2097152.);
        // Fixture-only manual establishment: reopening must preserve the user's chosen position/orientation.
        var established=LevelData.RespawnData.of(level.dimension(),p.offset(37,0,-19),47,-12);level.setRespawnData(established);
        require(level.getRespawnData().equals(established),"Established spawn stored");var data=new JsonObject();data.addProperty("seed",r.seed());data.addProperty("x",established.pos().getX());data.addProperty("y",established.pos().getY());data.addProperty("z",established.pos().getZ());Files.writeString(record,data.toString()+"\n");
        Blockbranch.LOGGER.info("REALISTIC_SPAWN_ESTABLISHED_CHECKPOINT {}",established);
    }
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
    private RealisticSpawnSmoke(){}
}
