package blockbranch.client.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Read-only prerequisite and rendering evidence, including when the pose gate rejects a frame. */
final class CaveApproachTimeoutSnapshot {
    private static final Deque<Map<String,Object>> history = new ArrayDeque<>();
    private static long last;
    static Map<String,Object> prerequisites(Minecraft client, BlockPos target, int stage, int applied, int request, boolean busy) {
        var row = new LinkedHashMap<String,Object>();
        var failed = new ArrayList<String>();
        row.put("stage",stage); row.put("applied",applied); row.put("request",request); row.put("busy",busy);
        row.put("toleranceSquared",.02);
        if(busy)failed.add("capture busy");
        if(applied!=stage)failed.add("server pose not applied");
        if(target==null)failed.add("target absent");
        else row.put("target",List.of(target.getX()+.5,(double)target.getY(),target.getZ()+.5));
        if(client.level==null)failed.add("client world absent");
        else if(target!=null){
            var chunk=client.level.getChunkSource().getChunk(target.getX()>>4,target.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
            var terrain=chunk==null?null:((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain();
            boolean ready=terrain!=null&&(Boolean.getBoolean("blockbranch.dripstoneReview")?terrain.base().dripstone().enabled():terrain.base().underground().hasApproach());
            row.put("targetProfileReady",ready);if(!ready)failed.add("target profile not ready");
        }
        var player=client.player;
        if(player==null)failed.add("client player absent");
        else {
            var pos=player.position();var velocity=player.getDeltaMovement();
            row.put("position",List.of(pos.x,pos.y,pos.z));row.put("velocity",List.of(velocity.x,velocity.y,velocity.z));
            row.put("flying",player.getAbilities().flying);row.put("onGround",player.onGround());
            row.put("keys",Map.of("forward",client.options.keyUp.isDown(),"back",client.options.keyDown.isDown(),"left",client.options.keyLeft.isDown(),"right",client.options.keyRight.isDown(),"jump",client.options.keyJump.isDown(),"shift",client.options.keyShift.isDown()));
            if(target!=null){double distance=pos.distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5);row.put("distanceSquared",distance);if(distance>.02)failed.add("pose outside tolerance");}
            if(client.level!=null)row.put("nearbyEntities",client.level.getEntities(player,player.getBoundingBox().inflate(3)).stream().limit(16).map(e->Map.of("type",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(),"position",List.of(e.getX(),e.getY(),e.getZ()))).toList());
        }
        row.put("failedPrerequisites",failed);return row;
    }
    static void observe(Minecraft client,BlockPos target,int stage,int applied,int request,boolean busy){
        long now=System.nanoTime();if(now-last<1_000_000_000L)return;last=now;
        var row=prerequisites(client,target,stage,applied,request,busy);row.put("epochMillis",System.currentTimeMillis());
        if(history.size()==16)history.removeFirst();history.addLast(row);
    }
    static Map<String,Object> capture(Minecraft client,BlockPos target,int stage,int applied,int request,boolean busy){
        var row=prerequisites(client,target,stage,applied,request,busy);
        if(client.level!=null&&client.player!=null)try{row.put("meshAudit",UndergroundAutomaticRenderSmoke.audit(client));}catch(Throwable error){row.put("meshAuditError",error.toString());}
        if(row.get("meshAudit") instanceof Map<?,?> audit)try{row.put("sectionStates",CaveSectionStateSnapshot.capture(client,(Map<String,Object>)audit));}catch(Exception error){row.put("sectionStatesError",error.toString());}
        row.put("recentMovement",new ArrayList<>(history));return row;
    }
    static void write(Minecraft client,BlockPos target,int stage,int applied,int request,boolean busy)throws Exception{
        var path=Path.of(System.getProperty("blockbranch.caveApproachClientResult")).resolveSibling("timeout-snapshot.json");
        Files.writeString(path,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(capture(client,target,stage,applied,request,busy))+"\n");
    }
    static void verify(Minecraft client,BlockPos target,int stage)throws Exception{
        // Change only the diagnostic target; no player, input, world or render state mutation.
        var valid=capture(client,target,stage,stage,stage,false);
        var displaced=capture(client,target.offset(8,0,0),stage,stage,stage,false);
        if(!((List<?>)valid.get("failedPrerequisites")).isEmpty()||!((List<?>)displaced.get("failedPrerequisites")).contains("pose outside tolerance"))throw new AssertionError("Pose diagnostic failed to discriminate targets");
        if(!valid.get("meshAudit").equals(displaced.get("meshAudit")))throw new AssertionError("Diagnostic target altered mesh audit");
        if(((Number)((Map<?,?>)valid.get("meshAudit")).get("missing")).intValue()!=0)throw new AssertionError("Diagnostic validation requires ready native meshes");
        Files.writeString(Path.of(System.getProperty("blockbranch.caveApproachClientResult")).resolveSibling("pose-diagnostic-check.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("valid",valid,"displacedTarget",displaced))+"\n");
    }
}
