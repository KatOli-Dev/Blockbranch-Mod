package blockbranch.realistic;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Optional flat field in the native map record. Completed and legacy maps retain the native shape. */
public final class PreviewMapCodec {
    private static final String FIELD = "blockbranch_pending_preview";
    private static final String ROWS = "blockbranch_preview_rows";
    public static Codec<MapItemSavedData> wrap(Codec<MapItemSavedData> nativeCodec) {
        return new Codec<>() {
            public <T> DataResult<Pair<MapItemSavedData,T>> decode(DynamicOps<T> ops,T input) {
                var marker=ops.get(input,FIELD).result();
                DataResult<Boolean> pending=marker.isEmpty()?DataResult.success(false):
                        ops.getNumberValue(marker.get()).flatMap(version -> version.longValue()==1&&version.doubleValue()==1.0?DataResult.success(true):
                                DataResult.error(()->"Unsupported Blockbranch map preview version: "+version));
                return pending.flatMap(value->{
                    var raw=ops.get(input,ROWS).result();
                    DataResult<Integer> rows=!value||raw.isEmpty()?DataResult.success(0):
                            ops.getNumberValue(raw.get()).flatMap(n->n.longValue()>=0&&n.longValue()<=127&&n.doubleValue()==(double)n.longValue()
                                    ?DataResult.success(n.intValue()):DataResult.error(()->"Invalid Blockbranch map preview rows: "+n));
                    return rows.flatMap(count->nativeCodec.decode(ops,input).map(pair->{
                        var preview=(PendingMapPreview)pair.getFirst();preview.blockbranch$pendingPreview(value);preview.blockbranch$previewRows(count);return pair;
                    }));
                });
            }
            public <T> DataResult<T> encode(MapItemSavedData data,DynamicOps<T> ops,T prefix) {
                return nativeCodec.encode(data,ops,prefix).flatMap(encoded->{
                    if(!DeferredMapPreview.pending(data))return DataResult.success(encoded);
                    return ops.mergeToMap(encoded,ops.createString(FIELD),ops.createInt(1)).flatMap(marked->
                            ops.mergeToMap(marked,ops.createString(ROWS),ops.createInt(((PendingMapPreview)data).blockbranch$previewRows())));
                });
            }
        };
    }
    private PreviewMapCodec() {}
}
