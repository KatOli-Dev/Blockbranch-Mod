package blockbranch.qa;

import blockbranch.cubic.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;

/** Isolated fixture world: actual native block-light propagation through procedural and edited sections. */
public final class LoadingLightSmoke {
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.loadingLightsResult"));
        try{
            if(Boolean.getBoolean("blockbranch.sectionPublicationSmoke")){SectionPublicationSmoke.verify(server.overworld());Files.writeString(result,"PASS deterministic edited-section eviction/reload publication\n");return;}
            LightIndexSmoke.verify();LightIndexSmoke.measure(server.overworld());
            var level=server.overworld();var factory=level.palettedContainerFactory();
            var biomes=new PalettedContainer<>(level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS),factory.biomeStrategy());
            var activation=VerticalRuntime.class.getDeclaredMethod("activate",ServerLevel.class,LevelChunk.class);activation.setAccessible(true);
            for(int kind=0;kind<3;kind++){
                var chunk=level.getChunk(kind*2,0);var sparse=(SparseColumn)chunk;
                int[] water=new int[256];Arrays.fill(water,kind==1?4:Integer.MIN_VALUE);
                var plain=new TerrainProfile(-16,64,new int[256],water,biomes,factory.blockStatesStrategy());
                var tag=plain.encode(factory);
                if(kind==0){byte[] warm=new byte[256];warm[8*16+8]=6;tag.putByteArray("warm",warm);}
                if(kind==1)tag.putIntArray("canopy",new int[]{((1-blockbranch.realistic.TerrainShape.FLOOR)<<16)|((8*16+8)<<8)|163});
                var base=TerrainProfile.decode(tag,factory);
                sparse.blockbranch$terrain(new ColumnTerrain(chunk,base,factory,SectionStore.of(level),new int[0]));
                var source=new BlockPos(chunk.getPos().getMinBlockX()+8,1,8);
                // This replacement profile is attached to an already FULL flat chunk.
                // Activate its layers before live edits: native checkBlock drops an
                // update when its section has no layer, and activation scans the base.
                activation.invoke(null,level,chunk);fence(level,source);
                if(kind==2){
                    check(!base.view(0).maybeHas(s->s.getLightEmission()>0),"plain procedural palette has no emitter");
                    check(level.getChunkSource().getLightEngine().getLayerListener(LightLayer.BLOCK).getDataLayerData(net.minecraft.core.SectionPos.of(source))!=null,"live-edit fixture has an initialized light layer");
                    level.setBlock(source,Blocks.GLOWSTONE.defaultBlockState(),3);
                }
                fence(level,source);
                int expected=kind==0?1:15;
                check(level.getBlockState(source).getLightEmission()==expected,"actual source state emission");
                check(level.getBrightness(LightLayer.BLOCK,source)==expected,"native source block light "+kind);
                if(kind>0)check(level.getBrightness(LightLayer.BLOCK,source.east())>=13,"native light propagated to neighbor "+kind);
                if(kind==2){
                    level.setBlock(source,Blocks.AIR.defaultBlockState(),3);fence(level,source);
                    check(level.getBrightness(LightLayer.BLOCK,source)==0,"removing edited emitter removes native light");
                    ((SparseColumn)chunk).blockbranch$terrain().flush();
                }
            }
            // Initial generation must allocate edits outside the immutable boundary
            // set before native findBlockLightSources enqueues those emitters.
            var editedChunk=level.getChunk(8,0);var edited=(SparseColumn)editedChunk;
            var editedBase=new TerrainProfile(-16,64,new int[256],java.util.stream.IntStream.generate(()->Integer.MIN_VALUE).limit(256).toArray(),biomes,factory.blockStatesStrategy());
            edited.blockbranch$terrain(new ColumnTerrain(editedChunk,editedBase,factory,SectionStore.of(level),new int[0]));
            int editedY=64;
            check(!editedBase.boundaries().contains(editedY),"regression edit is outside procedural boundaries");
            var editedSource=new BlockPos(editedChunk.getPos().getMinBlockX()+8,(editedY<<4)+8,8);
            edited.blockbranch$write(editedY).setBlockState(8,8,8,Blocks.GLOWSTONE.defaultBlockState());
            var engine=level.getChunkSource().getLightEngine();
            var initialize=engine.initializeLight(editedChunk,false);level.getServer().managedBlock(initialize::isDone);initialize.join();
            var propagate=engine.lightChunk(editedChunk,false);level.getServer().managedBlock(propagate::isDone);propagate.join();fence(level,editedSource);
            check(level.getBrightness(LightLayer.BLOCK,editedSource)==15&&level.getBrightness(LightLayer.BLOCK,editedSource.east())==14,"native initialization includes edited non-boundary section");
            edited.blockbranch$write(editedY).setBlockState(8,8,8,Blocks.AIR.defaultBlockState());
            var removed=engine.initializeLight(editedChunk,false);level.getServer().managedBlock(removed::isDone);removed.join();
            var dark=engine.lightChunk(editedChunk,false);level.getServer().managedBlock(dark::isDone);dark.join();fence(level,editedSource);
            check(level.getBrightness(LightLayer.BLOCK,editedSource)==0,"native reinitialization uses current empty occupancy after removal");
            // Adjacent FEATURES may add emitters after this column's INITIALIZE_LIGHT.
            // A solid interior stays nonempty, so vanilla emptiness notifications
            // cannot allocate a sparse light layer for that late feature.
            var lateChunk=level.getChunk(10,0);var late=(SparseColumn)lateChunk;
            int[] solidGround=new int[256];Arrays.fill(solidGround,1535);
            var solidBase=new TerrainProfile(-16,2048,solidGround,java.util.stream.IntStream.generate(()->Integer.MIN_VALUE).limit(256).toArray(),biomes,factory.blockStatesStrategy());
            late.blockbranch$terrain(new ColumnTerrain(lateChunk,solidBase,factory,SectionStore.of(level),new int[0]));
            int lateY=64;check(!solidBase.boundaries().contains(lateY),"late emitter lies inside an unindexed solid interior");
            var initialized=engine.initializeLight(lateChunk,false);level.getServer().managedBlock(initialized::isDone);initialized.join();
            var lateSource=new BlockPos(lateChunk.getPos().getMinBlockX()+8,(lateY<<4)+8,8);
            check(level.getBlockState(lateSource).is(Blocks.STONE),"late-feature control starts as solid stone");
            for(int y=0;y<=2;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++)level.setBlock(lateSource.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(lateSource,Blocks.AMETHYST_CLUSTER.defaultBlockState(),3);
            var edgeSource=lateSource.offset(7,0,0);level.getChunk(11,0);
            for(int y=0;y<=2;y++)for(int z=-1;z<=1;z++)for(int x=-1;x<=0;x++)level.setBlock(edgeSource.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(edgeSource,Blocks.AMETHYST_CLUSTER.defaultBlockState(),3);
            fence(level,lateSource);
            check(engine.getLayerListener(LightLayer.BLOCK).getDataLayerData(net.minecraft.core.SectionPos.of(lateSource))==null,"late source has no preexisting light layer before discovery");
            var lateLight=engine.lightChunk(lateChunk,false);level.getServer().managedBlock(lateLight::isDone);lateLight.join();fence(level,lateSource);
            check(level.getBrightness(LightLayer.BLOCK,lateSource)==5&&level.getBrightness(LightLayer.BLOCK,lateSource.above())==4,"late interior amethyst source and native propagation");
            check(level.getBrightness(LightLayer.BLOCK,edgeSource)==5&&level.getBrightness(LightLayer.BLOCK,edgeSource.east())==4,"late source propagates through native padding into the neighboring chunk");
            var repeated=engine.lightChunk(lateChunk,false);level.getServer().managedBlock(repeated::isDone);repeated.join();fence(level,lateSource);
            check(level.getBrightness(LightLayer.BLOCK,edgeSource.east())==4,"repeat discovery preserves native light values");
            late.blockbranch$terrain().flush();late.blockbranch$terrain().evictExcept(Set.of());
            check(level.getBlockState(lateSource).is(Blocks.AMETHYST_CLUSTER),"late feature survives persisted-section eviction/reload");
            var unload=net.minecraft.server.level.ThreadedLevelLightEngine.class.getDeclaredMethod("updateChunkStatus",net.minecraft.world.level.ChunkPos.class);unload.setAccessible(true);unload.invoke(engine,lateChunk.getPos());fence(level,lateSource);
            check(engine.getLayerListener(LightLayer.BLOCK).getDataLayerData(net.minecraft.core.SectionPos.of(lateSource))==null,"native lighting unload releases the source layer after repeat discovery");
            check(engine.getLayerListener(LightLayer.BLOCK).getDataLayerData(net.minecraft.core.SectionPos.of(edgeSource.east()))==null,"native lighting unload releases the neighboring halo without leaked references");
            var reinitialized=engine.initializeLight(lateChunk,false);level.getServer().managedBlock(reinitialized::isDone);reinitialized.join();
            var relit=engine.lightChunk(lateChunk,false);level.getServer().managedBlock(relit::isDone);relit.join();fence(level,lateSource);
            check(level.getBrightness(LightLayer.BLOCK,lateSource)==5&&level.getBrightness(LightLayer.BLOCK,edgeSource.east())==4,"native lighting reinitialization restores persisted emitters and cross-chunk propagation");
            level.setBlock(lateSource,Blocks.AIR.defaultBlockState(),3);fence(level,lateSource);
            level.setBlock(edgeSource,Blocks.AIR.defaultBlockState(),3);fence(level,edgeSource);
            check(level.getBrightness(LightLayer.BLOCK,lateSource)==0,"late source removal clears native light");
            check(level.getBrightness(LightLayer.BLOCK,edgeSource.east())==0,"removing the edge source clears neighboring-chunk light");
            Files.writeString(result,"PASS native mushroom/pickle/glowstone emission; late amethyst discovery, repeated discovery, cross-chunk propagation, native lighting unload/reinitialization, removal and persisted-section eviction/reload\n");
        }catch(Throwable error){blockbranch.Blockbranch.LOGGER.error("LOADING_LIGHTS FAIL",error);try{Files.writeString(result,"FAIL "+error);}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static void fence(ServerLevel level,BlockPos pos){var future=level.getChunkSource().getLightEngine().waitForPendingTasks(pos.getX()>>4,pos.getZ()>>4);level.getServer().managedBlock(future::isDone);future.join();}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
