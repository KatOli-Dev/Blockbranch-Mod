package blockbranch.realistic;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Bounded exact memo of a pure function: least-recently-used eviction at a fixed capacity, never an
 * approximation. Callers compute a miss without holding the lock ({@code find}, compute, {@code store}), so
 * concurrent misses for one key may both compute; the first stored value wins and every caller returns it.
 * Values must be non-null; wrap absent results in {@link java.util.Optional}.
 */
final class ExactMemo<K, V> {
    private final LinkedHashMap<K, V> entries;
    private final int capacity;

    ExactMemo(int initialCapacity, int capacity) {
        this.entries = new LinkedHashMap<>(initialCapacity, .75f, true);
        this.capacity = capacity;
    }

    /** The cached value, or null on a miss. */
    V find(K key) {
        synchronized (entries) { return entries.get(key); }
    }

    /** Stores a computed value unless another caller stored one first; returns the stored value. */
    V store(K key, V value) {
        Objects.requireNonNull(value);
        synchronized (entries) {
            var previous = entries.putIfAbsent(key, value);
            if (previous != null) return previous;
            if (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
            return value;
        }
    }

    int size() {
        synchronized (entries) { return entries.size(); }
    }

    /** Packs two coordinates into one key. */
    static long key(int x, int z) { return (x & 0xffffffffL) | ((long) z << 32); }
}
