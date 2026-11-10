package blockbranch.realistic;

import java.util.LinkedHashMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;

/** Exact, height-independent surface results owned only by one native map preview. */
public final class MapPreviewSamples implements AutoCloseable {
    private static final ThreadLocal<MapPreviewSamples> CURRENT = new ThreadLocal<>();
    private static final int LIMIT = 8192;
    private record Key(BiomeResolver owner, long column) {}
    private final MapPreviewSamples previous;
    static final class Cache {
        final LinkedHashMap<Key, Holder<Biome>> samples = new LinkedHashMap<>(256, .75f, true);
    }
    private final LinkedHashMap<Key, Holder<Biome>> samples;
    private final boolean clearOnClose;
    private MapPreviewSamples(Cache cache, boolean clear) {
        samples=cache.samples;clearOnClose=clear;previous=CURRENT.get();CURRENT.set(this);
    }
    public static MapPreviewSamples open() { return new MapPreviewSamples(new Cache(),true); }
    // A preview job owns this bounded cache; only one executing batch may bind it at a time.
    // The thread-local binding is removed when that server operation or worker batch returns.
    static MapPreviewSamples open(Cache cache) { return new MapPreviewSamples(cache,false); }
    static Holder<Biome> surface(int x, int y, int z, BiomeResolver original) {
        var scope = CURRENT.get();
        if (scope == null) return original.getNoiseBiome(x, y, z);
        var key = new Key(original, (x & 0xffffffffL) | ((long) z << 32));
        var found = scope.samples.get(key);
        if (found != null) return found;
        var result = original.getNoiseBiome(x, y, z);
        scope.samples.put(key, result);
        if (scope.samples.size() > LIMIT) scope.samples.remove(scope.samples.keySet().iterator().next());
        return result;
    }
    int retainedSamples() { return samples.size(); }
    @Override public void close() {
        if(clearOnClose)samples.clear();
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
