package blockbranch.client.realistic;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;

/** Bounded read-only timeout observation of missing sections and immediate graph neighbors. */
final class CaveSectionStateSnapshot {
    private static Object field(Object object,String name)throws Exception{
        for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException absent){}
        throw new NoSuchFieldException(name);
    }
    static Map<String,Object> capture(Minecraft client,Map<String,Object> audit)throws Exception{
        var result=new LinkedHashMap<String,Object>();var graph=client.levelRenderer.sectionOcclusionGraph();
        var area=client.levelRenderer.viewArea();if(area==null)return Map.of("error","no view area");
        var tracker=(net.minecraft.client.SectionUpdateTracker)field(field(client.level,"levelExtractor"),"sectionUpdateTracker");
        var dispatcher=client.levelRenderer.sectionRenderDispatcher();
        result.put("lightWork",client.level.getLightEngine().hasLightWork());result.put("graphNeedsFullUpdate",field(graph,"needsFullUpdate"));
        var full=field(graph,"fullUpdateTask");result.put("fullUpdateDone",full==null||((java.util.concurrent.Future<?>)full).isDone());
        if(dispatcher!=null){result.put("dispatcher",dispatcher.getStats());result.put("compileQueue",dispatcher.getCompileQueueSize());}
        var nodes=new LinkedHashSet<Long>();var pattern=java.util.regex.Pattern.compile("SectionPos\\{x=(-?\\d+), y=(-?\\d+), z=(-?\\d+)\\}");
        String probe=System.getProperty("blockbranch.sectionStateProbe","");
        if(!probe.isEmpty()){
            var xyz=probe.split(",");var pos=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            nodes.add(SectionPos.asLong(pos.getX(),pos.getY(),pos.getZ()));
            for(var direction:Direction.values()){var q=pos.relative(direction);nodes.add(SectionPos.asLong(q.getX(),q.getY(),q.getZ()));}
        }
        for(var reason:(List<?>)audit.getOrDefault("reasons",List.of())){
            var match=pattern.matcher(reason.toString());if(!match.find())continue;
            var pos=new BlockPos(Integer.parseInt(match.group(1)),Integer.parseInt(match.group(2)),Integer.parseInt(match.group(3)));
            nodes.add(SectionPos.asLong(pos.getX(),pos.getY(),pos.getZ()));
            for(var direction:Direction.values()){var q=pos.relative(direction);nodes.add(SectionPos.asLong(q.getX(),q.getY(),q.getZ()));}
        }
        var rows=new ArrayList<Map<String,Object>>();
        for(long node:nodes.stream().limit(32).toList()){
            var row=new LinkedHashMap<String,Object>();rows.add(row);var section=SectionPos.of(node);row.put("section",section.toString());
            try{
                var chunk=client.level.getChunkSource().getChunk(section.x(),section.z(),net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
                row.put("chunkPresent",chunk!=null);row.put("allNeighborsReady",tracker.hasAllNeighbors(client.level,node));
                var dirty=tracker.getDirtyState(node);row.put("dirty",dirty!=null&&dirty.isDirty());
                row.put("graphEmpty",((it.unimi.dsi.fastutil.longs.LongSet)field(graph,"emptySections")).contains(node));
                row.put("graphLoaded",((it.unimi.dsi.fastutil.longs.LongSet)field(graph,"loadedChunks")).contains(net.minecraft.world.level.ChunkPos.pack(section.x(),section.z())));
                var mesh=area.getRenderSectionAt(section.origin());row.put("slotPresent",mesh!=null);if(mesh==null)continue;
                row.put("slotMatches",mesh.getSectionNode()==node);row.put("renderable",mesh.getSectionMesh().hasRenderableLayers());
                var connections=new ArrayList<String>();for(var from:Direction.values())for(var to:Direction.values())if(mesh.getSectionMesh().facesCanSeeEachother(from,to))connections.add(from+"->"+to);row.put("visibleFacePairs",connections);
                var ingress=graph.getNode(mesh);row.put("graphNode",ingress!=null);
                if(ingress!=null){row.put("sourceDirections",field(ingress,"sourceDirections"));row.put("directions",field(ingress,"directions"));}
                var task=field(mesh,"lastCompileTask");row.put("compileTask",task==null?"none":task.getClass().getName());
                if(task!=null){row.put("cancelled",field(task,"isCancelled").toString());row.put("completed",field(task,"isCompleted").toString());row.put("regionNull",field(task,"region")==null);}
                row.put("frameUpdates",client.gameRenderer.gameRenderState().levelRenderState.sectionUpdateRenderStates.stream().filter(e->e.sectionNode()==node).map(e->Map.of("regionNull",e.region()==null,"playerChanged",e.playerChanged())).toList());
            }catch(Exception error){row.put("observationError",error.toString());}
        }
        result.put("sections",rows);return result;
    }
}
