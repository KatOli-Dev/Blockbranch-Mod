package blockbranch.realistic;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;

/** Shares immutable uniform save snapshots; live chunk sections remain independently mutable. */
public final class UniformSnapshots {
    private static final Strategy<BlockState> STATES = Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY);
    // An outer weak-key entry can remain stale until the next use of that cache.
    // Its inner biome keys must also be weak, so an idle cache cannot retain a
    // closed world's registry while waiting for normal stale-entry expunging.
    private static final Map<Strategy<Holder<Biome>>, Map<BlockState, Map<Holder<Biome>, WeakReference<LevelChunkSection>>>> CACHE = new WeakHashMap<>();
    private static final Map<Strategy<Holder<Biome>>, Map<Holder<Biome>, WeakReference<PalettedContainerRO<Holder<Biome>>>>> BIOMES = new WeakHashMap<>();

    // One weak last-result hint per calling thread; never retain a world/registry.
    private static final ThreadLocal<WeakReference<PalettedContainerRO<Holder<Biome>>>> LAST_BIOME = new ThreadLocal<>();

    /** Section biome storage is read-only; edits recreate an independent palette. */
    @SuppressWarnings("unchecked")
    public static PalettedContainerRO<Holder<Biome>> compactBiomes(PalettedContainerRO<Holder<Biome>> source) {
        if (source instanceof FrozenPalette<?> || source.bitsPerEntry() != 0 || !(source instanceof PaletteStrategyAccess<?> access)) return source;
        var strategy = (Strategy<Holder<Biome>>)access.realistic$strategy();
        var value = source.get(0, 0, 0);
        var lastRef = LAST_BIOME.get();
        var last = lastRef == null ? null : lastRef.get();
        if (last instanceof FrozenPalette<?> frozen && frozen.strategy == strategy && java.util.Objects.equals(value, frozen.value)) return last;
        WeakReference<PalettedContainerRO<Holder<Biome>>> ref;
        PalettedContainerRO<Holder<Biome>> palette;
        synchronized (BIOMES) {
            var entries = BIOMES.computeIfAbsent(strategy, ignored -> new WeakHashMap<>());
            ref = entries.get(value);
            palette = ref == null ? null : ref.get();
            if (palette == null) {
                palette = new FrozenPalette<>(value, strategy);
                ref = new WeakReference<>(palette);
                entries.put(value, ref);
            }
        }
        LAST_BIOME.set(ref);
        return palette;
    }

    @SuppressWarnings("unchecked")
    public static LevelChunkSection copyOf(LevelChunkSection source) {
        if (source.getStates().bitsPerEntry() != 0 || source.getBiomes().bitsPerEntry() != 0) return source.copy();
        if (!(source.getBiomes() instanceof PaletteStrategyAccess<?> access)) return source.copy();
        var strategy = (Strategy<Holder<Biome>>)access.realistic$strategy();
        var state = source.getBlockState(0, 0, 0);
        var biome = source.getNoiseBiome(0, 0, 0);
        synchronized (CACHE) {
            var states = CACHE.computeIfAbsent(strategy, ignored -> new HashMap<>());
            var entries = states.computeIfAbsent(state, ignored -> new WeakHashMap<>());
            var ref = entries.get(biome);
            var snapshot = ref == null ? null : ref.get();
            if (snapshot == null) {
                snapshot = new Snapshot(state, biome, strategy);
                entries.put(biome, new WeakReference<>(snapshot));
            }
            return snapshot;
        }
    }

    private static final class FrozenPalette<T> extends PalettedContainer<T> {
        private final T value;
        private final Strategy<T> strategy;
        FrozenPalette(T value, Strategy<T> strategy) { super(value, strategy); this.value = value; this.strategy = strategy; }
        // Sharing is safe only because every public mutation entry point rejects writes.
        @Override public void acquire() {}
        @Override public void release() {}
        @Override public void set(int x, int y, int z, T value) { throw frozen(); }
        @Override public T getAndSet(int x, int y, int z, T value) { throw frozen(); }
        @Override public T getAndSetUnchecked(int x, int y, int z, T value) { throw frozen(); }
        @Override public int onResize(int bits, T value) { throw frozen(); }
        @Override public void read(FriendlyByteBuf buffer) { throw frozen(); }
        @Override public PalettedContainer<T> copy() { return new PalettedContainer<>(value, strategy); }
        @Override public PalettedContainer<T> recreate() { return copy(); }
    }
    private static final class Snapshot extends LevelChunkSection {
        Snapshot(BlockState state, Holder<Biome> biome, Strategy<Holder<Biome>> strategy) {
            super(new FrozenPalette<>(state, STATES), new FrozenPalette<>(biome, strategy));
        }
        @Override public BlockState setBlockState(int x, int y, int z, BlockState state) { throw frozen(); }
        @Override public BlockState setBlockState(int x, int y, int z, BlockState state, boolean lock) { throw frozen(); }
        @Override public void read(FriendlyByteBuf buffer) { throw frozen(); }
        @Override public void readBiomes(FriendlyByteBuf buffer) { throw frozen(); }
        @Override public void fillBiomesFromNoise(BiomeResolver resolver, int x, int y, int z) { throw frozen(); }
        @Override public LevelChunkSection copy() { return new LevelChunkSection(getStates().copy(), getBiomes().copy()); }
    }
    private static UnsupportedOperationException frozen() { return new UnsupportedOperationException("Immutable uniform save snapshot"); }
    private UniformSnapshots() {}
}
