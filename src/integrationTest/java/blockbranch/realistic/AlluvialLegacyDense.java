package blockbranch.realistic;

import com.google.gson.*;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Replays the exact frozen original scalar footprint, including chunks whose
 * invalid profiles become valid. Selection never depends on the new result. */
final class AlluvialLegacyDense {
    static JsonObject capture(ServerLevel level,RealisticGenerator retained,JsonObject frozen)throws Exception{
        if(retained.alluvialDefinition()!=0)throw new AssertionError("Retained original terrain required");
        var random=level.getChunkSource().randomState();var d=retained.drainage(random);var hash=java.security.MessageDigest.getInstance("SHA-256");var visited=new HashSet<ChunkPos>();int reads=0;
        for(var entry:frozen.getAsJsonArray("profiles")){
            var p=entry.getAsJsonObject();var coord=p.getAsJsonArray("chunk");var pos=new ChunkPos(coord.get(0).getAsInt(),coord.get(1).getAsInt());
            var center=Published269Snapshot.POINTS[p.get("point").getAsInt()];boolean dense=Math.abs(pos.x()-(center[0]>>4))<=1&&Math.abs(pos.z()-(center[1]>>4))<=1;
            if((!dense&&p.get("valid").getAsBoolean())||!visited.add(pos))continue;
            for(int z=pos.getMinBlockZ();z<=pos.getMaxBlockZ();z++)for(int x=pos.getMinBlockX();x<=pos.getMaxBlockX();x++){
                int h=d.sample(x,z).height();for(int y=h-3;y<=h+40;y++){hash.update((x+","+y+","+z+":"+retained.surfaceState(x,y,z,random)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));reads++;}
            }
        }
        var report=new JsonObject();report.addProperty("seed",random.seed());report.addProperty("densePhysicalReads",reads);report.addProperty("densePhysicalSHA256",HexFormat.of().formatHex(hash.digest()));report.addProperty("scope","Exact frozen original dense chunk order, including original rejected profiles; not a new-result-selected probe set.");return report;
    }
    private AlluvialLegacyDense(){}
}
