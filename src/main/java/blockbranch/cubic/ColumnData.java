package blockbranch.cubic;

import java.util.*;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.lighting.LevelLightEngine;

public final class ColumnData {
    public static SerializableChunkData withSections(SerializableChunkData data, List<SerializableChunkData.SectionData> sections) {
        var result=new SerializableChunkData(data.containerFactory(), data.chunkPos(), data.minSectionY(), data.lastUpdateTime(), data.inhabitedTime(), data.chunkStatus(), data.blendingData(), data.belowZeroRetrogen(), data.upgradeData(), data.heightmaps(), data.packedTicks(), data.postProcessingSections(), data.lightCorrect(), sections, data.entities(), data.blockEntities(), data.structureData());
        var source=(TerrainSnapshotAccess)(Object)data;
        ((TerrainSnapshotAccess)(Object)result).blockbranch$base(source.blockbranch$base(),source.blockbranch$edited());
        ((SparsePostProcessingSnapshot)(Object)result).blockbranch$postSnapshot(((SparsePostProcessingSnapshot)(Object)data).blockbranch$postSnapshot());
        return result;
    }
    public static List<SerializableChunkData.SectionData> snapshot(ChunkAccess chunk, LevelLightEngine light) {
        var sparse=(SparseColumn)chunk;
        if(sparse.blockbranch$terrain()!=null) { sparse.blockbranch$terrain().flush(); return List.of(); }
        var populated=sparse.blockbranch$sections();
        var ys=new TreeSet<Integer>(populated.keySet());
        ys.addAll(((SparseLight)light).blockbranch$lightSections(chunk.getPos().pack()));
        var result=new ArrayList<SerializableChunkData.SectionData>();
        for(int y:ys) {
            var position=SectionPos.of(chunk.getPos(),y);
            var block=light.getLayerListener(LightLayer.BLOCK).getDataLayerData(position);
            var sky=light.getLayerListener(LightLayer.SKY).getDataLayerData(position);
            var section=populated.get(y);
            result.add(new SerializableChunkData.SectionData(y, section==null ? null : blockbranch.realistic.UniformSnapshots.copyOf(section), block==null ? null : block.copy(), sky==null ? null : sky.copy()));
        }
        return result;
    }
    private ColumnData() {}
}
