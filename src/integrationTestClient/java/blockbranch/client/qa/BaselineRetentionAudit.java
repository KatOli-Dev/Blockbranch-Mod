package blockbranch.client.qa;

import blockbranch.client.cubic.ProceduralSectionCopies;
import blockbranch.cubic.TerrainProfile;
import com.google.gson.GsonBuilder;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;

/** Bounded ordinary-world close/reopen check; explicit GC is confined to this QA fixture. */
public final class BaselineRetentionAudit {
    public static final int TARGET=Integer.getInteger("blockbranch.baselineRetentionCycles",5);
    private record Witness(int cycle,String kind,WeakReference<Object> reference) {}
    private final List<Witness> witnesses=new ArrayList<>();
    private final List<Map<String,Object>> rows=new ArrayList<>();
    private final Path result;
    private long closing,lastGc;
    private int completed;
    public BaselineRetentionAudit(Path result){if(TARGET<2||TARGET>5)throw new IllegalArgumentException("Expected 2..5 bounded cycles");this.result=result;}
    private void add(String kind,Object object){witnesses.add(new Witness(completed+1,kind,new WeakReference<>(object)));}
    public void sample(Minecraft client,TerrainProfile clientProfile,TerrainProfile serverProfile){
        var server=client.getSingleplayerServer();var world=server.overworld();
        add("client-profile",clientProfile);add("server-profile",serverProfile);
        add("client-level",client.level);add("server-level",world);
        add("generator",world.getChunkSource().getGenerator());
        add("server-registry",world.registryAccess().lookupOrThrow(Registries.BIOME));
        add("client-registry",client.level.registryAccess().lookupOrThrow(Registries.BIOME));
    }
    public void beginClose(){closing=System.nanoTime();lastGc=0;}
    public boolean waiting(){return closing!=0;}
    public boolean poll(Minecraft client)throws Exception{
        long elapsed=System.nanoTime()-closing;
        if(elapsed>20_000_000_000L)throw new AssertionError("Closed-world retention timeout: "+status());
        if(client.level!=null||client.getSingleplayerServer()!=null)return false;
        if(elapsed>1_000_000_000L&&(lastGc==0||elapsed-lastGc>3_000_000_000L)){
            System.gc();lastGc=elapsed;return false;
        }
        if(elapsed<4_000_000_000L)return false;
        var cache=ProceduralSectionCopies.statistics();
        if(cache[2]!=0||cache[3]!=0||client.levelRenderer.viewArea()!=null||!client.levelRenderer.visibleSections().isEmpty()||client.levelRenderer.sectionRenderDispatcher()!=null)
            throw new AssertionError("Closed-world renderer retains sections or snapshots");
        var state=client.gameRenderer.gameRenderState().levelRenderState;
        if(!state.sectionUpdateRenderStates.isEmpty()||state.playerCompiledSectionCallback!=null)
            throw new AssertionError("Closed-world extracted render state retains work");
        for(var w:witnesses)if(w.cycle<completed+1&&w.reference.get()!=null)return false;
        for(var w:witnesses)if((w.kind.equals("client-profile")||w.kind.equals("server-profile")||w.kind.endsWith("level"))&&w.cycle==completed+1&&w.reference.get()!=null)return false;
        long heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        rows.add(Map.of("cycle",++completed,"postGcHeapBytes",heap,"cacheEntries",cache[2],"cacheWeightBytes",cache[3],"earlierOwnersAlive",earlierAlive()));
        if(rows.size()>1){long before=((Number)rows.get(rows.size()-2).get("postGcHeapBytes")).longValue();
            if(heap>before+32L*1024*1024&&heap>before*1.10)throw new AssertionError("Post-GC heap growth gate");}
        Files.writeString(result,new GsonBuilder().setPrettyPrinting().create().toJson(rows)+"\n");
        closing=0;return true;
    }
    public int completed(){return completed;}
    private long earlierAlive(){return witnesses.stream().filter(w->w.cycle<completed&&w.reference.get()!=null).count();}
    private String status(){return witnesses.stream().filter(w->w.reference.get()!=null).map(w->w.kind+"@"+w.cycle).toList().toString();}
}
