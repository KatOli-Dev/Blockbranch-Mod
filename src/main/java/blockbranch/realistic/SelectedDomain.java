package blockbranch.realistic;

import java.util.function.BiPredicate;

/**
 * A biome domain clipped to exact ground: a quart-resolution selection (the biome source's choice) and a
 * block-resolution physical test. Separate bounded memos keep block-exact edges without repeating either
 * test in vertical reads.
 */
final class SelectedDomain {
    static BiPredicate<Integer, Integer> of(BiPredicate<Integer, Integer> selectedQuart, BiPredicate<Integer, Integer> physical) {
        var blocks = new ExactMemo<Long, Boolean>(256, 8192);
        var quarts = new ExactMemo<Long, Boolean>(256, 2048);
        return (x, z) -> {
            long key = ExactMemo.key(x, z);
            var cached = blocks.find(key);
            if (cached != null) return cached;
            int qx = x >> 2, qz = z >> 2;
            long quart = ExactMemo.key(qx, qz);
            var selected = quarts.find(quart);
            if (selected == null) selected = quarts.store(quart, selectedQuart.test(qx, qz));
            return blocks.store(key, selected && physical.test(x, z));
        };
    }

    private SelectedDomain() {}
}
