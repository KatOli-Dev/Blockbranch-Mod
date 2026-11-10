package blockbranch.horizon;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
/**
 * Real blocks of generated chunks for horizon tiles, so structures, cave openings and edits
 * appear wherever the world has been generated. A chunk the server holds is read in place;
 * otherwise its saved data is read through the level's own chunk I/O worker and decoded
 * without creating, loading or ticking a chunk. Elsewhere {@link HorizonStructures} places
 * planned structures in detached columns; remaining chunks are left to the analytic sampler.
 * The level is never asked to generate or load a chunk for the horizon.
 *
 * <p>Reads never change a chunk: a live chunk's resident sections are read in place, and an
 * edited section that is not resident is loaded into a private copy instead of the chunk's
 * working set. A block read races a concurrent edit only as one {@code PalettedContainer.get}
 * (it reads the volatile data reference once), so a tile can mix blocks from before and after
 * an edit; the edit marks its chunk changed and the tile is sampled again.
 *
 * <p>Saved chunks are cached by position; {@link #forget} drops one when it may have changed,
 * and a read that overlapped the drop is not cached. A region file that does not exist is
 * never opened, because reading through the storage would create it.
 */
final class HorizonSources {
    private static final int CACHED=512;
    private final ServerLevel level;
    private final Path regions;
    private final PalettedContainerFactory factory;
    private final Set<Long> presentRegions=ConcurrentHashMap.newKeySet();
    private final HorizonStructures structures;
    private final HorizonSourceCache<Optional<HorizonColumns.Source>> saved=new HorizonSourceCache<>(CACHED);
    private volatile boolean warned;

    HorizonSources(ServerLevel level) {
        this.level=level;this.factory=level.palettedContainerFactory();
        this.structures=new HorizonStructures(level);
        this.regions=DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(LevelResource.ROOT)).resolve("region");
    }
    ServerLevel level(){return level;}
    void forget(long chunk){saved.forget(chunk);}

    /**
     * A chunk's blocks, read without changing the chunk, or null when it has no procedural
     * terrain. {@code store} holds its edited sections that are not resident (null for a
     * detached chunk, whose edits are all resident).
     */
    static HorizonColumns.Source loaded(ChunkAccess chunk,SectionStore store) {
        var terrain=((SparseColumn)chunk).blockbranch$terrain();
        return terrain==null?null:new Live(chunk,terrain,store);
    }

    /**
     * Source for a chunk: the loaded chunk, else its saved data, else (for a chunk never
     * fully generated) its planned structures placed in a sandbox, else null. Call off the server thread.
     */
    HorizonColumns.Source column(int cx,int cz,Long2ObjectMap<LevelChunk> loaded) {
        long key=ChunkPos.pack(cx,cz);
        var chunk=loaded.get(key);
        if(chunk!=null)return loaded(chunk,SectionStore.of(level));
        var known=saved.get(key);if(known!=null)return known.orElse(null);
        int stamp=saved.stamp(key);
        var found=read(cx,cz);
        var source=Optional.ofNullable(found!=null?found:structures.column(cx,cz));
        saved.commit(key,stamp,source);
        return source.orElse(null);
    }

    private HorizonColumns.Source read(int cx,int cz) {
        long region=ChunkPos.pack(cx>>5,cz>>5);
        if(!presentRegions.contains(region)) {
            if(!Files.isRegularFile(regions.resolve("r."+(cx>>5)+"."+(cz>>5)+".mca")))return null;
            presentRegions.add(region);
        }
        try {
            var pos=new ChunkPos(cx,cz);
            CompoundTag tag=level.getChunkSource().chunkMap.read(pos).get(30,TimeUnit.SECONDS).orElse(null);
            if(tag==null||!tag.contains("blockbranch:terrain"))return null;
            var status=ChunkStatus.byName(tag.getStringOr("Status",""));
            if(status==null||!status.isOrAfter(ChunkStatus.FULL))return null;
            var base=TerrainProfile.decode(tag.getCompoundOrEmpty("blockbranch:terrain"),factory);
            var edited=new TreeSet<Integer>();for(int y:tag.getIntArray("blockbranch:edited_sections").orElse(new int[0]))edited.add(y);
            return new Saved(pos,base,edited);
        } catch(InterruptedException stopped) {
            Thread.currentThread().interrupt();throw new CancellationException("Horizon stopped");
        } catch(Exception failure) {
            if(!warned){warned=true;Blockbranch.LOGGER.warn("Horizon could not read saved chunk {},{}; using generated terrain there",cx,cz,failure);}
            return null;
        }
    }

    /**
     * A procedural base with edited sections layered over it. {@link #section} supplies an
     * edited section; every other section is the base.
     */
    private abstract static class Layered implements HorizonColumns.Source {
        final TerrainProfile base;
        Layered(TerrainProfile base){this.base=base;}
        abstract NavigableSet<Integer> edited();
        abstract LevelChunkSection section(int sy);
        @Override public BlockState state(int x,int y,int z) {
            int sy=y>>4;return edited().contains(sy)?section(sy).getBlockState(x&15,y&15,z&15):base.state(x&15,y,z&15);
        }
        /** Same search as {@link ColumnQueries#top}: edited sections from the top down, then the base. */
        @Override public int top(Predicate<BlockState> predicate,int x,int z,int limit) {
            x&=15;z&=15;
            for(int sy:edited().descendingSet()) {
                int bottom=sy<<4;if(bottom>limit)continue;
                int generated=base.top(predicate,x,z,limit);
                if(bottom+15<generated-1)return generated;
                var section=section(sy);
                for(int y=Math.min(limit,bottom+15);y>=bottom;y--)if(predicate.test(section.getBlockState(x,y&15,z)))return y+1;
                limit=Math.min(limit,bottom-1);
            }
            return base.top(predicate,x,z,limit);
        }
    }

    /**
     * A chunk in memory, read in place. A resident section is the chunk's own (checked and
     * fetched under the terrain lock, so eviction cannot turn the fetch into a load into the
     * chunk); an edited section that is not resident is loaded privately from the store.
     */
    private static final class Live extends Layered {
        private final SparseColumn chunk;
        private final ColumnTerrain terrain;
        private final ChunkPos pos;
        private final SectionStore store;
        private final Map<Integer,LevelChunkSection> loaded=new ConcurrentHashMap<>();
        Live(ChunkAccess chunk,ColumnTerrain terrain,SectionStore store){super(terrain.base());this.chunk=(SparseColumn)chunk;this.terrain=terrain;this.pos=chunk.getPos();this.store=store;}
        @Override NavigableSet<Integer> edited(){return terrain.modifiedView();}
        @Override LevelChunkSection section(int sy) {
            synchronized(terrain){if(terrain.resident(sy))return chunk.blockbranch$read(sy);}
            return loaded.computeIfAbsent(sy,y->{
                if(store==null)return base.generate(y);
                try{return store.load(pos,y,terrain.factory());}
                catch(RuntimeException failure){Blockbranch.LOGGER.debug("Horizon could not read edited section {} / {}",pos,y,failure);return base.generate(y);}
            });
        }
    }

    /** A saved column: its procedural base plus the sections edited since generation, loaded on demand. */
    private final class Saved extends Layered {
        private final ChunkPos pos;
        private final NavigableSet<Integer> edited;
        private final Map<Integer,LevelChunkSection> sections=new ConcurrentHashMap<>();
        Saved(ChunkPos pos,TerrainProfile base,NavigableSet<Integer> edited){super(base);this.pos=pos;this.edited=edited;}
        @Override NavigableSet<Integer> edited(){return edited;}
        @Override LevelChunkSection section(int y) {
            return sections.computeIfAbsent(y,sy->{
                try{return SectionStore.of(level).load(pos,sy,factory);}
                catch(RuntimeException failure){
                    if(!warned){warned=true;Blockbranch.LOGGER.warn("Horizon could not read edited section {} / {}; using generated terrain there",pos,sy,failure);}
                    return base.generate(sy);
                }
            });
        }
    }
}
