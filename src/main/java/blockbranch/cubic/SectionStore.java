package blockbranch.cubic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.storage.LevelResource;

/** Independent section region files. Only edited sections ever enter this store. */
public final class SectionStore implements AutoCloseable {
    private static final Map<ServerLevel,SectionStore> WORLDS=new IdentityHashMap<>();
    public static synchronized SectionStore of(ServerLevel level) {
        return WORLDS.computeIfAbsent(level,l->new SectionStore(l.getServer().getWorldPath(LevelResource.ROOT).resolve("sections3d"),new RegionStorageInfo(l.getServer().getWorldData().getLevelName(),l.dimension(),"sections3d")));
    }
    public static synchronized void closeAll() {
        for(var store:WORLDS.values()) store.close(); WORLDS.clear();
    }
    private final Path root;
    private final RegionStorageInfo info;
    private final LinkedHashMap<Integer,RegionFileStorage> layers=new LinkedHashMap<>(16,.75f,true);
    private SectionStore(Path root,RegionStorageInfo info) { this.root=root; this.info=info; }
    private RegionFileStorage layer(int y) throws IOException {
        var storage=layers.get(y);
        if(storage==null) {
            if(layers.size()>=8) { var oldest=layers.entrySet().iterator(); oldest.next().getValue().close(); oldest.remove(); }
            var path=root.resolve(Integer.toString(y)); java.nio.file.Files.createDirectories(path);
            storage=new RegionFileStorage(info,path,true); layers.put(y,storage);
        }
        return storage;
    }
    public synchronized void save(ChunkPos pos,int y,LevelChunkSection section,PalettedContainerFactory factory) {
        try {
            var tag=new CompoundTag(); tag.putInt("version",1); tag.putInt("x",pos.x()); tag.putInt("y",y); tag.putInt("z",pos.z());
            tag.store("block_states",factory.blockStatesContainerCodec(),section.getStates());
            tag.store("biomes",factory.biomeContainerCodec(),section.getBiomes());
            var layer=layer(y); layer.write(pos,tag); layer.flush();
        } catch(IOException failure) { throw new IllegalStateException("Cannot save section "+pos+" / "+y,failure); }
    }
    public synchronized LevelChunkSection load(ChunkPos pos,int y,PalettedContainerFactory factory) {
        try {
            var tag=layer(y).read(pos);
            if(tag==null || tag.getIntOr("version",-1)!=1 || tag.getIntOr("x",Integer.MIN_VALUE)!=pos.x() || tag.getIntOr("y",Integer.MIN_VALUE)!=y || tag.getIntOr("z",Integer.MIN_VALUE)!=pos.z())
                throw new IllegalStateException("Missing or invalid edited section "+pos+" / "+y);
            return new LevelChunkSection(factory.blockStatesContainerCodec().parse(NbtOps.INSTANCE,tag.getCompoundOrEmpty("block_states")).getOrThrow(),
                factory.biomeContainerCodec().parse(NbtOps.INSTANCE,tag.getCompoundOrEmpty("biomes")).getOrThrow());
        } catch(IOException failure) { throw new IllegalStateException("Cannot load section "+pos+" / "+y,failure); }
    }
    @Override public synchronized void close() {
        try { for(var storage:layers.values()) storage.close(); layers.clear(); }
        catch(IOException failure) { throw new IllegalStateException("Cannot close section store",failure); }
    }
}
