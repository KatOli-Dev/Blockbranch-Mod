package blockbranch.realistic;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Native zoom and loaded-data access are server-thread operations. Only fallback() is worker-safe. */
public final class PreviewBiomeQueries {
    static final class Selector {
        private final BiomeManager zoom;
        private int x,y,z;
        Selector(ServerLevel level){
            var placeholder=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
            zoom=level.getBiomeManager().withDifferentSource((qx,qy,qz)->{x=qx;y=qy;z=qz;return placeholder;});
        }
        Point select(BlockPos pos){zoom.getBiome(pos);return new Point(x,y,z);}
    }
    public record Point(int x,int y,int z){}
    public record Read(boolean ready,Holder<Biome> value){}
    static Read loaded(ServerLevel level,Point point){
        // This is the native LevelReader.getNoiseBiome lookup, without its blocking wait.
        // false preserves native ticket ownership: the preview never asks for a new chunk ticket.
        // Public getChunkFuture itself calls managedBlock on the server thread, even with create=false.
        if(!level.getServer().isSameThread())throw new IllegalStateException("Map loaded-biome access must run on the server thread");
        var future=((blockbranch.mixin.cubic.PreviewChunkFutureAccessor)level.getChunkSource())
                .blockbranch$previewFuture(point.x()>>2,point.z()>>2,ChunkStatus.BIOMES,false);
        if(!future.isDone())return new Read(false,null);
        var chunk=future.join().orElse(null);
        return new Read(true,chunk==null?null:chunk.getNoiseBiome(point.x(),point.y(),point.z()));
    }
    static BiomeResolver workerResolver(ServerLevel level){
        // Do not assume an arbitrary mod's resolver is safe off-thread.
        if(level.getChunkSource().getGenerator().getBiomeSource() instanceof ScreeBiomeSource source){
            var seeded=source.createResolver(null);
            if(seeded==level.uncachedBiomeResolver())return seeded;
        }
        return null;
    }
    static Holder<Biome> fallback(BiomeResolver resolver,Point point){return resolver.getNoiseBiome(point.x(),point.y(),point.z());}
    private PreviewBiomeQueries(){}
}
