package blockbranch.cubic;

import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.storage.LevelResource;

/** Bounded native storage tests using synthetic columns, not terrain generation. */
public final class ColumnLifecycleSmoke {
    private static Path result(){return Path.of(System.getProperty("blockbranch.columnLifecycleResult"));}
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            try{
                var level=server.overworld();var factory=level.palettedContainerFactory();
                int[] water=new int[256];Arrays.fill(water,Integer.MIN_VALUE);
                var biomes=new PalettedContainer<>(level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS),factory.biomeStrategy());
                var base=new TerrainProfile(-16,64,new int[256],water,biomes,factory.blockStatesStrategy());
                for(int sy:new int[]{-500,0,200})exercise(level,base,sy);
                interruptedSave(level,base);
                clientOverlay(level,base);
                var horizon=blockbranch.horizon.HorizonLiveReadCheck.run(level,base);
                Files.writeString(result(),"PASS deferred surveys preserve live edits; repeat flush/evict/read, native snapshot/reopen, restoration, failed-save retry and read-only horizon reads\n");
                Blockbranch.LOGGER.info("COLUMN_LIFECYCLE PASS nativeLayers=3 repeats=4 failedSaveRetry=true {}",horizon);
            }catch(Throwable error){
                Blockbranch.LOGGER.error("COLUMN_LIFECYCLE FAIL",error);
                try{Files.writeString(result(),"FAIL "+error+"\n");}catch(Exception ignored){}
            }finally{server.halt(false);}
        });
    }
    private static ProtoChunk column(ServerLevel level,TerrainProfile base,ChunkPos pos,int[] modified){
        var chunk=new ProtoChunk(pos,UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
        ((SparseColumn)chunk).blockbranch$terrain(new ColumnTerrain(chunk,base,level.palettedContainerFactory(),SectionStore.of(level),modified));
        return chunk;
    }
    private static void exercise(ServerLevel level,TerrainProfile base,int sy)throws Exception{
        var chunk=column(level,base,new ChunkPos(sy,7),new int[0]);var sparse=(SparseColumn)chunk;var terrain=sparse.blockbranch$terrain();
        sparse.blockbranch$write(sy).setBlockState(1,1,1,Blocks.DIAMOND_BLOCK.defaultBlockState());terrain.flush();terrain.evictExcept(Set.of());
        for(int i=0;i<4;i++){
            var deferred=terrain.survey(sy); // Keep the traversal view while its section becomes resident and changes.
            require(!deferred.hasOnlyAir(),"saved edited layer materializes");
            var live=sparse.blockbranch$write(sy);var expected=(i%2==0?Blocks.GOLD_BLOCK:Blocks.EMERALD_BLOCK).defaultBlockState();
            live.setBlockState(1,1,1,expected);
            var observed=deferred.getBlockState(1,1,1);
            require(observed==expected,"deferred survey discarded unsaved live edit: expected="+expected+" observed="+observed);
            require(sparse.blockbranch$read(sy)==live,"deferred read replaced resident section identity");
            terrain.flush();terrain.evictExcept(Set.of());
            require(sparse.blockbranch$read(sy).getBlockState(1,1,1)==expected,"updated edit survives flush/eviction");
            terrain.evictExcept(Set.of());
        }
        var saved=SerializableChunkData.copyOf(level,chunk).write();
        require(Arrays.stream(saved.getIntArray("blockbranch:edited_sections").orElseThrow()).anyMatch(y->y==sy),"native snapshot retains edited index");
        SectionStore.closeAll();
        var parsed=SerializableChunkData.parse(level,level.palettedContainerFactory(),saved);
        var reloaded=parsed.read(level,level.getPoiManager(),new RegionStorageInfo("lifecycle",level.dimension(),"lifecycle"),chunk.getPos());
        var again=(SparseColumn)reloaded;
        require(again.blockbranch$read(sy).getBlockState(1,1,1).is(Blocks.EMERALD_BLOCK),"native snapshot and store reopen preserve latest edit");
        var next=again.blockbranch$terrain();var view=next.survey(sy);
        again.blockbranch$write(sy).setBlockState(1,1,1,base.state(1,sy*16+1,1));next.flush();next.evictExcept(Set.of());
        require(!next.modifiedView().contains(sy),"restoration removes overlay index");
        require(view.getBlockState(1,1,1)==base.state(1,sy*16+1,1),"old deferred view does not resurrect removed overlay");
        var restored=SerializableChunkData.copyOf(level,reloaded).write();
        require(restored.getIntArray("blockbranch:edited_sections").orElseThrow().length==0,"native snapshot preserves restored empty index");
        SectionStore.closeAll();
        var restoredChunk=SerializableChunkData.parse(level,level.palettedContainerFactory(),restored).read(level,level.getPoiManager(),new RegionStorageInfo("lifecycle",level.dimension(),"lifecycle"),chunk.getPos());
        require(((SparseColumn)restoredChunk).blockbranch$read(sy).getBlockState(1,1,1)==base.state(1,sy*16+1,1),"base restoration survives native reopen despite orphaned old section data");
        Blockbranch.LOGGER.info("COLUMN_LIFECYCLE_LAYER_PASS y={} repeats=4 nativeReopen=true",sy);
    }
    private static void interruptedSave(ServerLevel level,TerrainProfile base)throws Exception{
        var root=level.getServer().getWorldPath(LevelResource.ROOT).resolve("sections3d");
        int sy=257;while(Files.exists(root.resolve(Integer.toString(sy))))sy++;
        require(sy<300,"bounded fixture-only failure layers");
        var chunk=column(level,base,new ChunkPos(8,9),new int[0]);var sparse=(SparseColumn)chunk;var terrain=sparse.blockbranch$terrain();
        var live=sparse.blockbranch$write(sy);live.setBlockState(2,2,2,Blocks.DIAMOND_BLOCK.defaultBlockState());
        var obstacle=root.resolve(Integer.toString(sy));
        Files.createDirectories(obstacle.getParent());Files.writeString(obstacle,"intentional fixture-only save obstruction");
        boolean failed=false;try{terrain.flush();}catch(IllegalStateException expected){failed=true;}
        require(failed&&terrain.resident(sy)&&sparse.blockbranch$read(sy)==live,"failed save retains live resident section");
        Files.delete(obstacle);terrain.flush();terrain.evictExcept(Set.of());
        require(sparse.blockbranch$read(sy).getBlockState(2,2,2).is(Blocks.DIAMOND_BLOCK),"failed dirty write retries and survives eviction");
        Blockbranch.LOGGER.info("COLUMN_LIFECYCLE_FAILED_SAVE_RETRY_PASS");
    }
    private static void clientOverlay(ServerLevel level,TerrainProfile base){
        int sy=0;var chunk=column(level,base,new ChunkPos(11,12),new int[0]);var sparse=(SparseColumn)chunk;
        var terrain=new ColumnTerrain(chunk,base,level.palettedContainerFactory(),null,new int[]{sy});sparse.blockbranch$terrain(terrain);
        var deferred=terrain.survey(sy);require(deferred.hasOnlyAir(),"client edited overlay initially unavailable");
        sparse.blockbranch$write(sy).setBlockState(1,1,1,Blocks.DIAMOND_BLOCK.defaultBlockState());
        require(deferred.getBlockState(1,1,1).is(Blocks.DIAMOND_BLOCK),"retained client survey observes arrived overlay");
        Blockbranch.LOGGER.info("COLUMN_LIFECYCLE_CLIENT_OVERLAY_PASS");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private ColumnLifecycleSmoke(){}
}
