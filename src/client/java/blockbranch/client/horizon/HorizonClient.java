package blockbranch.client.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import blockbranch.Blockbranch;
import blockbranch.horizon.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.*;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.*;
import java.util.*;
import java.util.concurrent.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4fc;
/**
 * Client half of the visual-only horizon: requests tiles, meshes them off-thread and
 * draws them with the native block atlas, lightmap and fog after the solid terrain.
 * All state is owned by the render thread; the mesher only reads immutable tile data.
 * Disconnects, dimension changes and level swaps release every GPU buffer.
 */
public final class HorizonClient {
    public static final boolean ENABLED=HorizonService.ENABLED;
    /** Horizon radius in blocks from the camera, and the detail-falloff distance. */
    static final int RADIUS=Integer.getInteger("blockbranch.horizon.radius",2048),QUALITY=Integer.getInteger("blockbranch.horizon.quality",64);
    static final int MAX_TILES=4096,UPLOADS_PER_FRAME=12;

    static final VertexFormat FORMAT=VertexFormat.builder(0).addAttribute("Position",GpuFormat.RGB32_FLOAT).addAttribute("Color",GpuFormat.RGBA8_UNORM)
        .addAttribute("UV0",GpuFormat.RG32_FLOAT).addAttribute("UV2",GpuFormat.RG16_SINT).addAttribute("Sprite",GpuFormat.RGBA32_FLOAT).build();
    private static final RenderPipeline.Snippet SNIPPET=RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS).withBindGroupLayout(BindGroupLayouts.FOG).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER2).withBindGroupLayout(BindGroupLayouts.PROJECTION).withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
        .withBindGroupLayout(BindGroupLayouts.TERRAIN_INFO).withVertexBinding(0,FORMAT).withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withVertexShader(Blockbranch.id("core/horizon")).withFragmentShader(Blockbranch.id("core/horizon")).buildSnippet();
    static final RenderPipeline SOLID=RenderPipeline.builder(SNIPPET).withLocation(Blockbranch.id("pipeline/horizon_solid"))
        .withDepthStencilState(DepthStencilState.DEFAULT).withColorTargetState(ColorTargetState.DEFAULT).build();
    static final RenderPipeline WATER=RenderPipeline.builder(SNIPPET).withLocation(Blockbranch.id("pipeline/horizon_water"))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL,false)).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();

    static final class Entry {
        final int[] data;GpuBuffer buffer;int solid,water,version=-1,lastDrawn;
        /** Tile data the uploaded mesh was built from; differs from {@link #data} until a refreshed tile's mesh uploads. */
        int[] meshed;
        Entry(int[] data){this.data=data;}
    }
    private record Built(int generation,int version,HorizonMesher.Mesh mesh,int[] data) {}
    private record Failed(int generation,long key) {}
    /** Meshing failures, retried with backoff and given up after three; the tile's parent is drawn instead. */
    private static final ConcurrentLinkedQueue<Failed> FAILED=new ConcurrentLinkedQueue<>();
    private static final HorizonRetry MESH_RETRY=new HorizonRetry(20,400,3,1024);
    private static final it.unimi.dsi.fastutil.longs.LongOpenHashSet REMESH=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    private static final HorizonResend RESEND=new HorizonResend();

    private static final Long2ObjectOpenHashMap<Entry> ENTRIES=new Long2ObjectOpenHashMap<>();
    private static final ConcurrentLinkedQueue<Built> BUILT=new ConcurrentLinkedQueue<>();
    private static ExecutorService mesher;
    private static ClientLevel level;
    private static HorizonMaterials materials;
    private static HorizonMesher mesherState;
    private static int generation,version,ticks,frame;
    private static GpuBufferSlice terrainUbo;
    private static GpuBufferSlice[] sectionUbos=new GpuBufferSlice[0];
    private static Entry[] drawList=new Entry[0];
    /** Horizon-owned per-frame uniforms: writing into the shared instance would resize or
     * replace buffers native terrain has already bound for this frame. */
    private static DynamicGpuData uniforms;

    public static void initialize() {
        if(!ENABLED)return;
        ClientPlayNetworking.registerGlobalReceiver(HorizonTile.TYPE,(packet,context)->receive(packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(HorizonClient::reset));
        ClientTickEvents.END_CLIENT_TICK.register(HorizonClient::tick);
    }

    /** Radius the camera and fog must reach while horizon tiles are drawn, else 0. */
    public static int activeRadius(){return ENABLED&&level!=null&&drawList.length>0?RADIUS:0;}
    /** Counts for matched captures and diagnostics; read on the render thread. */
    public record Stats(int tiles,int meshed,int drawn,int missing,int meshing,int quads,int failed) {}
    public static Stats stats() {
        int meshed=0,meshing=0,failed=0;long quads=0;
        for(var e:ENTRIES.long2ObjectEntrySet()) {
            var v=e.getValue();
            if(v.version>=0){meshed++;quads+=v.solid+v.water;}
            else if(MESH_RETRY.exhausted(e.getLongKey()))failed++;
            else meshing++;
        }
        return new Stats(ENTRIES.size(),meshed,drawList.length,RESEND.last().length,BUILT.size()+meshing,(int)quads,failed);
    }
    /**
     * A cached tile for lifecycle checks, or null: its data, whether the uploaded mesh was
     * built from that data, and whether that mesh was drawn in the last frame. Render thread.
     */
    record TileState(int[] data,boolean meshCurrent,boolean drawn) {}
    static TileState tile(long key){var e=ENTRIES.get(key);return e==null?null:new TileState(e.data,e.meshed==e.data,e.buffer!=null&&e.lastDrawn==frame);}
    /** Drops a cached tile as eviction does, so the next request asks for it again; lifecycle checks, render thread. */
    static void drop(long key){var e=ENTRIES.remove(key);if(e!=null&&e.buffer!=null)e.buffer.close();}
    /** Pauses requests and drawing, keeping cached tiles, so a capture can compare against native terrain alone. */
    public static volatile boolean suppressed;

    static void reset() {
        RenderSystem.assertOnRenderThread();
        for(var e:ENTRIES.values())if(e.buffer!=null)e.buffer.close();
        ENTRIES.clear();BUILT.clear();FAILED.clear();REMESH.clear();MESH_RETRY.clear();RESEND.reset();drawList=new Entry[0];sectionUbos=new GpuBufferSlice[0];
        generation++;level=null;materials=null;mesherState=null;
        if(mesher!=null){mesher.shutdownNow();mesher=null;}
    }

    private static void receive(HorizonTile tile) {
        if(tile.generation()!=generation||level==null)return;
        var entry=new Entry(tile.data());
        var old=ENTRIES.put(tile.key(),entry);
        // A refreshed tile keeps drawing its old mesh until the new one is uploaded.
        if(old!=null){entry.buffer=old.buffer;entry.solid=old.solid;entry.water=old.water;entry.version=old.version;entry.lastDrawn=old.lastDrawn;entry.meshed=old.meshed;}
        // New data gets fresh mesh attempts.
        MESH_RETRY.succeeded(tile.key());REMESH.remove(tile.key());
        submit(tile.key(),entry);
    }
    private static void submit(long key,Entry e) {
        if(mesher==null){mesher=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Blockbranch Horizon Mesher");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});}
        int g=generation,v=version;var m=mesherState;var data=e.data;
        mesher.execute(()->{
            try{BUILT.add(new Built(g,v,m.build(key,data),data));}
            catch(Throwable t){FAILED.add(new Failed(g,key));Blockbranch.LOGGER.error("Horizon mesh {} failed",key,t);}
        });
    }

    private static void tick(Minecraft client) {
        if(client.level!=level){if(level!=null||!ENTRIES.isEmpty())reset();level=client.level;}
        if(level==null||client.player==null||level.dimension()!=Level.OVERWORLD||!ClientPlayNetworking.canSend(HorizonRequest.TYPE))return;
        var models=client.getModelManager().getBlockStateModelSet();
        if(materials==null||materials.models!=models) {
            materials=new HorizonMaterials(client);var biomes=level.registryAccess().lookupOrThrow(Registries.BIOME);
            mesherState=new HorizonMesher(materials,biomes::byId);version++;
            for(var e:ENTRIES.long2ObjectEntrySet())submit(e.getLongKey(),e.getValue());
        }
        if(suppressed)return;
        ++ticks;
        retryMeshes();
        if(ticks%10!=0)return;
        var view=view(client);var coverage=coverage(client);
        long[] wanted=HorizonSelection.wanted(view,coverage);var wantedSet=new LongOpenHashSet(wanted);
        // Evict tiles that are out of range, or neither wanted nor drawn for 30 seconds.
        for(var it=ENTRIES.long2ObjectEntrySet().iterator();it.hasNext();) {
            var e=it.next();long k=e.getLongKey();
            if(distance(k,view.x(),view.z())>RADIUS+span(level(k))||(!wantedSet.contains(k)&&frame-e.getValue().lastDrawn>1800&&ENTRIES.size()>MAX_TILES/4)) {
                if(e.getValue().buffer!=null)e.getValue().buffer.close();it.remove();
            }
        }
        // Each request replaces the server queue, so it always lists every missing tile in
        // priority order; the server skips tiles it is already sampling.
        var missing=new LongArrayList();
        for(long k:wanted)if(!ENTRIES.containsKey(k)&&missing.size()<HorizonRequest.MAX_KEYS)missing.add(k);
        // An unchanged list is repeated with backoff: the server drops failed and stale tiles.
        long[] send=RESEND.next(missing.toLongArray(),ticks);
        if(send!=null)ClientPlayNetworking.send(new HorizonRequest(generation,send));
    }
    /** Resubmits failed meshes once their backoff has passed, at most three attempts per tile data. */
    private static void retryMeshes() {
        for(Failed f;(f=FAILED.poll())!=null;)
            if(f.generation()==generation){MESH_RETRY.failed(f.key(),ticks);if(!MESH_RETRY.exhausted(f.key()))REMESH.add(f.key());}
        for(var it=REMESH.iterator();it.hasNext();) {
            long k=it.nextLong();var e=ENTRIES.get(k);
            if(e==null){it.remove();continue;}
            if(MESH_RETRY.due(k,ticks)){it.remove();submit(k,e);}
        }
    }

    private static HorizonSelection.View view(Minecraft client) {
        var camera=client.gameRenderer.mainCamera().position();
        return new HorizonSelection.View(camera.x,camera.z,client.options.getEffectiveRenderDistance()*16.0,RADIUS,QUALITY);
    }
    /**
     * Native terrain covers a chunk once it is loaded and the sections holding its lowest
     * ground and its highest block have compiled, provided both pass the same view-distance
     * test as the native section graph: horizontally within the view distance, and at most
     * that many sections above or below the camera. A camera high above a forest therefore
     * leaves the chunks beneath it to the horizon even where native terrain still draws the
     * crowns; testing one column would treat a chunk as covered by a crown in its middle.
     */
    private static HorizonSelection.NativeCoverage coverage(Minecraft client) {
        var area=client.levelRenderer.viewArea();
        if(area==null||level==null)return (x,z)->false;
        var camera=area.getCameraSectionPos();int distance=area.getViewDistance();var chunks=level.getChunkSource();var world=level;
        int cameraY=(int)Math.floor(client.gameRenderer.mainCamera().position().y),cameraSection=SectionPos.blockToSectionCoord(cameraY);
        return new HorizonSelection.NativeCoverage() {
            /** Lowest ground (leaves excluded) and highest block in the chunk; the camera height while the height maps are unknown. */
            private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<int[]> surfaces=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
            private int[] surface(int x,int z){return surfaces.computeIfAbsent(net.minecraft.world.level.ChunkPos.pack(x,z),k->measure(x,z));}
            private int[] measure(int x,int z) {
                var chunk=world.getChunk(x,z);int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
                for(int i=0;i<256;i++) {
                    low=Math.min(low,chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,i&15,i>>4));
                    high=Math.max(high,chunk.getHeight(Heightmap.Types.MOTION_BLOCKING,i&15,i>>4));
                }
                return new int[]{low>world.getMinY()?low-1:cameraY,high>world.getMinY()?high-1:cameraY};
            }
            private boolean reachable(int y){return Math.abs(SectionPos.blockToSectionCoord(y)-cameraSection)<=distance;}
            private boolean compiled(int x,int y,int z) {
                var section=area.getRenderSectionAt(new BlockPos(x*16+8,y,z*16+8));
                return section!=null&&section.getSectionMesh()!=CompiledSectionMesh.UNCOMPILED;
            }
            @Override public boolean covers(int x,int z) {
                if(!ChunkTrackingView.isInViewDistance(camera.x(),camera.z(),distance,x,z)||!chunks.hasChunk(x,z))return false;
                int[] y=surface(x,z);
                return reachable(y[0])&&reachable(y[1])&&compiled(x,y[0],z)&&compiled(x,y[1],z);
            }
            @Override public boolean expected(int x,int z) {
                if(!ChunkTrackingView.isInViewDistance(camera.x(),camera.z(),distance,x,z))return false;
                if(!chunks.hasChunk(x,z))return true;
                int[] y=surface(x,z);return reachable(y[0])&&reachable(y[1]);
            }
        };
    }

    /** Before the frame graph runs: upload finished meshes, choose tiles and write their uniforms. */
    public static void prepare(Matrix4fc modelView) {
        frame++;
        var client=Minecraft.getInstance();
        if(!ENABLED||level==null||client.player==null||level.dimension()!=Level.OVERWORLD){drawList=new Entry[0];return;}
        for(int n=0;n<UPLOADS_PER_FRAME;n++) {
            var b=BUILT.poll();if(b==null)break;
            var e=ENTRIES.get(b.mesh().key());
            if(b.generation()!=generation||e==null||b.version()<e.version)continue;
            MESH_RETRY.succeeded(b.mesh().key());
            if(e.buffer!=null)e.buffer.close();e.buffer=null;e.version=b.version();e.meshed=b.data();e.solid=b.mesh().solidQuads();e.water=b.mesh().waterQuads();
            if(e.solid+e.water>0)e.buffer=RenderSystem.getDevice().createBuffer(()->"Blockbranch horizon tile",GpuBuffer.USAGE_VERTEX,b.mesh().vertices());
        }
        if(suppressed){drawList=new Entry[0];return;}
        if(uniforms==null)uniforms=new DynamicGpuData();else uniforms.reset();
        var keys=HorizonSelection.drawn(view(client),k->{var e=ENTRIES.get(k);return e!=null&&e.version>=0;},coverage(client));
        var list=new ArrayList<Entry>(keys.length);var infos=new ArrayList<DynamicGpuData.ChunkSectionInfo>(keys.length);int maxQuads=0;
        for(long k:keys) {
            var e=ENTRIES.get(k);e.lastDrawn=frame;if(e.buffer==null)continue;
            list.add(e);infos.add(new DynamicGpuData.ChunkSectionInfo(originX(k),0,originZ(k),1f));maxQuads=Math.max(maxQuads,e.solid+e.water);
        }
        drawList=list.toArray(Entry[]::new);
        if(drawList.length==0)return;
        var atlas=client.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        terrainUbo=uniforms.writeTerrainTransform(modelView,atlas.getWidth(0),atlas.getHeight(0));
        sectionUbos=uniforms.writeChunkSections(infos.toArray(DynamicGpuData.ChunkSectionInfo[]::new));
        RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(maxQuads*6);
    }

    /** Inside the main pass after native solid terrain: opaque tiles, then their water. */
    public static void draw(RenderPass pass,GpuSampler atlasSampler) {
        if(drawList.length==0)return;
        var client=Minecraft.getInstance();
        var atlas=client.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        var indices=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        pass.pushDebugGroup(()->"Blockbranch horizon");
        pass.setPipeline(RenderSystem.getCompiledPipeline(SOLID));
        pass.setUniform("TerrainUniform",terrainUbo);
        pass.setUniform("Sampler0",atlas,atlasSampler);
        pass.setUniform("Sampler2",client.gameRenderer.lightmap(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        pass.setIndexBuffer(indices.getBuffer(),indices.type());
        for(int i=0;i<drawList.length;i++){var e=drawList[i];if(e.solid==0)continue;pass.setUniform("ChunkSection",sectionUbos[i]);pass.setVertexBuffer(0,e.buffer.slice());pass.drawIndexed(e.solid*6,1,0,0,0);}
        pass.setPipeline(RenderSystem.getCompiledPipeline(WATER));
        for(int i=0;i<drawList.length;i++){var e=drawList[i];if(e.water==0)continue;pass.setUniform("ChunkSection",sectionUbos[i]);pass.setVertexBuffer(0,e.buffer.slice());pass.drawIndexed(e.water*6,1,e.solid*6,0,0);}
        pass.popDebugGroup();
    }
    private HorizonClient(){}
}
