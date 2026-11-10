package blockbranch.mixin.cubic;

import blockbranch.cubic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.nbt.NbtOps;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(SerializableChunkData.class)
abstract class SerializableChunkDataMixin implements TerrainSnapshotAccess, SparsePostProcessingSnapshot {
    @Unique private int[] blockbranch$postSnapshot=new int[0];
    public int[] blockbranch$postSnapshot(){return blockbranch$postSnapshot.clone();}
    public void blockbranch$postSnapshot(int[] values){blockbranch$postSnapshot=SparsePostProcessing.validate(values);}
    @Unique private TerrainProfile blockbranch$base;
    @Unique private int[] blockbranch$edited=new int[0];
    public TerrainProfile blockbranch$base() { return blockbranch$base; }
    public int[] blockbranch$edited() { return blockbranch$edited.clone(); }
    public void blockbranch$base(TerrainProfile base,int[] edited) { blockbranch$base=base; blockbranch$edited=edited.clone(); }
    @Redirect(method="copyOf",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/lighting/LevelLightEngine;getMinLightSection()I"))
    private static int blockbranch$saveMin(LevelLightEngine engine,ServerLevel level,ChunkAccess chunk) { return ((SparseColumn)chunk).blockbranch$terrain()!=null ? 1 : engine.getLightSectionCount()>1024 ? -5 : engine.getMinLightSection(); }
    @Redirect(method="copyOf",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/lighting/LevelLightEngine;getMaxLightSection()I"))
    private static int blockbranch$saveMax(LevelLightEngine engine,ServerLevel level,ChunkAccess chunk) { return ((SparseColumn)chunk).blockbranch$terrain()!=null ? 0 : engine.getLightSectionCount()>1024 ? 21 : engine.getMaxLightSection(); }
    @Inject(method="copyOf",at=@At("RETURN"),cancellable=true)
    private static void blockbranch$snapshot(ServerLevel level,ChunkAccess chunk,CallbackInfoReturnable<SerializableChunkData> cir) {
        if(((SparseColumn)chunk).blockbranch$expanded()) {
            var saved=ColumnData.withSections(cir.getReturnValue(),ColumnData.snapshot(chunk,level.getLightEngine()));
            var terrain=((SparseColumn)chunk).blockbranch$terrain();
            if(terrain!=null) ((TerrainSnapshotAccess)(Object)saved).blockbranch$base(terrain.base(),terrain.modified());
            var updates=((SparsePostProcessingAccess)chunk).blockbranch$postProcessing(false);
            if(updates!=null)((SparsePostProcessingSnapshot)(Object)saved).blockbranch$postSnapshot(updates.snapshot());
            cir.setReturnValue(saved);
        }
    }
    @Inject(method="write",at=@At("RETURN"))
    private void blockbranch$write(CallbackInfoReturnable<CompoundTag> cir) {
        var data=(SerializableChunkData)(Object)this;
        var tag=cir.getReturnValue(); tag.putInt("blockbranch:column_format",ColumnLimits.FORMAT_VERSION);
        if(blockbranch$postSnapshot.length>0){tag.putInt("blockbranch:column_format",SparsePostProcessing.FORMAT);tag.putIntArray(SparsePostProcessing.KEY,blockbranch$postSnapshot);}
        if(blockbranch$base!=null) {
            tag.put("blockbranch:terrain",blockbranch$base.encode(data.containerFactory()));
            tag.putIntArray("blockbranch:edited_sections",blockbranch$edited);
            tag.putBoolean("isLightOn",false);
        }
        var tags=tag.getListOrEmpty("sections");
        // Vanilla narrows section Y to a byte. Replace it with the original absolute integer.
        int index=0;
        for(var section:data.sectionData()) {
            if(section.chunkSection()!=null || section.blockLight()!=null || section.skyLight()!=null) tags.getCompound(index++).orElseThrow().putInt("Y",section.y());
        }
    }
    @WrapMethod(method="parse")
    private static SerializableChunkData blockbranch$parse(LevelHeightAccessor height,PalettedContainerFactory factory,CompoundTag tag,Operation<SerializableChunkData> original) {
        try {
        if(tag.contains("blockbranch:terrain")!=tag.contains("blockbranch:edited_sections"))
            throw new ColumnProfileException(new IllegalArgumentException("Incomplete Blockbranch profile/edit descriptor"));
        if(tag.getString("Status").isEmpty()) {
            if(ColumnLimits.expanded(height)||tag.contains("blockbranch:terrain"))throw new IllegalArgumentException("Missing Blockbranch chunk status");
            return original.call(height,factory,tag);
        }
        int format=tag.getIntOr("blockbranch:column_format",-1);
        boolean sparsePost=tag.contains(SparsePostProcessing.KEY);
        if(format!=1&&format!=ColumnLimits.FORMAT_VERSION&&format!=SparsePostProcessing.FORMAT)throw new IllegalArgumentException("Unsupported Blockbranch column format");
        if((format==SparsePostProcessing.FORMAT)!=sparsePost)throw new IllegalArgumentException("Incomplete sparse post-processing format");
        int[] updates=sparsePost?SparsePostProcessing.validate(tag.getIntArray(SparsePostProcessing.KEY).orElseThrow()):new int[0];
        if(sparsePost&&updates.length==0)throw new IllegalArgumentException("Empty sparse post-processing format");
        var clean=tag.copy(); var base=new ListTag(); var extras=new ArrayList<SerializableChunkData.SectionData>();
        for(var section:tag.getListOrEmpty("sections").compoundStream().toList()) {
            int y=section.getIntOr("Y",Integer.MAX_VALUE);
            if(y < (height.getMinY()>>4)-1 || y > (height.getMaxY()>>4)+1) throw new IllegalArgumentException("Invalid section Y: "+y);
            if(y>=-128 && y<=127) base.add(section);
            else {
                LevelChunkSection content=null;
                if(section.contains("block_states")) content=new LevelChunkSection(factory.blockStatesContainerCodec().parse(NbtOps.INSTANCE,section.getCompoundOrEmpty("block_states")).getOrThrow(),factory.biomeContainerCodec().parse(NbtOps.INSTANCE,section.getCompoundOrEmpty("biomes")).getOrThrow());
                extras.add(new SerializableChunkData.SectionData(y,content,section.getByteArray("BlockLight").map(DataLayer::new).orElse(null),section.getByteArray("SkyLight").map(DataLayer::new).orElse(null)));
            }
        }
        clean.put("sections",base);
        var parsed=original.call(height,factory,clean);
        if(parsed==null) {
            if(ColumnLimits.expanded(height)||tag.contains("blockbranch:terrain"))throw new IllegalArgumentException("Unreadable Blockbranch chunk");
            return null;
        }
        var all=new ArrayList<>(parsed.sectionData()); all.addAll(extras);
        var result=ColumnData.withSections(parsed,all);
        ((SparsePostProcessingSnapshot)(Object)result).blockbranch$postSnapshot(updates);
        if(tag.contains("blockbranch:terrain")) ((TerrainSnapshotAccess)(Object)result).blockbranch$base(TerrainProfile.decode(tag.getCompoundOrEmpty("blockbranch:terrain"),factory),tag.getIntArray("blockbranch:edited_sections").orElseThrow());
        return result;
        } catch(RuntimeException failure) {
            if((tag.contains("blockbranch:terrain")||tag.contains("blockbranch:edited_sections"))&&!(failure instanceof ColumnProfileException))throw new ColumnProfileException(failure);
            throw failure;
        }
    }
    @Inject(method="read",at=@At("RETURN"))
    private void blockbranch$restorePost(ServerLevel level,PoiManager poi,RegionStorageInfo info,ChunkPos pos,CallbackInfoReturnable<ProtoChunk> cir){
        if(blockbranch$postSnapshot.length==0)return;
        ((SparsePostProcessingAccess)cir.getReturnValue()).blockbranch$postProcessing(true).restore(blockbranch$postSnapshot);
    }
    @Redirect(method="read",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getSectionsCount()I"))
    private int blockbranch$readCount(ServerLevel level) { return level.dimensionType().height()>>4; }
    @Redirect(method="read",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getSectionIndexFromSectionY(I)I"))
    private int blockbranch$readIndex(ServerLevel level,int y) { return y-(level.dimensionType().minY()>>4); }
    @WrapMethod(method="read")
    private ProtoChunk blockbranch$read(ServerLevel level,PoiManager poi,RegionStorageInfo info,ChunkPos pos,Operation<ProtoChunk> original) {
        var data=(SerializableChunkData)(Object)this;
        if(!ColumnLimits.expanded(level)) return original.call(level,poi,info,pos);
        if(blockbranch$base!=null) {
            var result=original.call(level,poi,info,pos);
            ChunkAccess owner=result instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : result;
            ((SparseColumn)owner).blockbranch$terrain(new ColumnTerrain(owner,blockbranch$base,level.palettedContainerFactory(),SectionStore.of(level),blockbranch$edited));
            owner.initializeLightSources();
            return result;
        }
        var base=data.sectionData().stream().filter(s->s.y()>=-4 && s.y()<=19).toList();
        if(base.size()==data.sectionData().size()) {
            var result=original.call(level,poi,info,pos);
            if(base.size()==0) blockbranch$migrate(level,result);
            return result;
        }
        var nativeData=ColumnData.withSections(data,base);
        // A filtered recursive call reaches the vanilla reader without building a full-height array.
        var result=nativeData.read(level,poi,info,pos);
        ChunkAccess chunk=result instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : result;
        var sparse=(SparseColumn)chunk;
        for(var section:data.sectionData()) if(section.y() < -4 || section.y()>19) {
            if(section.chunkSection()!=null) { sparse.blockbranch$replace(section.y(),section.chunkSection()); poi.checkConsistencyWithBlocks(SectionPos.of(pos,section.y()),section.chunkSection()); }
            var location=SectionPos.of(pos,section.y());
            if(!(level.getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator)) {
                if(section.blockLight()!=null) level.getLightEngine().queueSectionData(LightLayer.BLOCK,location,section.blockLight());
                if(section.skyLight()!=null) level.getLightEngine().queueSectionData(LightLayer.SKY,location,section.skyLight());
            }
        }
        chunk.initializeLightSources();
        blockbranch$migrate(level,result);
        return result;
    }
    @Unique private static void blockbranch$migrate(ServerLevel level,ProtoChunk result) {
        if(!(level.getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator generator)) return;
        ChunkAccess owner=result instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : result;
        var sparse=(SparseColumn)owner;
        if(sparse.blockbranch$terrain()!=null) return;
        var old=sparse.blockbranch$sections();
        var base=generator.profile(owner,level.getChunkSource().randomState());
        var terrain=new ColumnTerrain(owner,base,level.palettedContainerFactory(),SectionStore.of(level),new int[0]);
        sparse.blockbranch$terrain(terrain);
        for(var entry:old.entrySet()) {
            if(ColumnMigration.matches(base,entry.getKey(),entry.getValue())) sparse.blockbranch$replace(entry.getKey(),null);
            else { sparse.blockbranch$replace(entry.getKey(),entry.getValue()); terrain.modified(entry.getKey()); }
        }
        owner.markUnsaved(); owner.setLightCorrect(false); owner.initializeLightSources();
    }
}
