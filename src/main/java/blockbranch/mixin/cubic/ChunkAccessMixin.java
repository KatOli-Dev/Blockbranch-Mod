package blockbranch.mixin.cubic;

import blockbranch.cubic.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ChunkAccess.class)
abstract class ChunkAccessMixin implements SparseColumn, SparsePostProcessingAccess, LevelHeightAccessor {
    @Unique private SparsePostProcessing blockbranch$post;
    public synchronized SparsePostProcessing blockbranch$postProcessing(boolean create){if(blockbranch$post==null&&create)blockbranch$post=new SparsePostProcessing();return blockbranch$post;}
    @Shadow @Final protected LevelHeightAccessor levelHeightAccessor;
    @Shadow @Final protected LevelChunkSection[] sections;
    @Unique private boolean blockbranch$expanded;
    @Unique private PalettedContainerFactory blockbranch$factory;
    @Unique private LevelChunkSection blockbranch$empty;
    @Unique private ColumnTerrain blockbranch$terrain;
    public ColumnTerrain blockbranch$terrain() { return blockbranch$terrain; }
    public void blockbranch$terrain(ColumnTerrain terrain) { blockbranch$terrain = terrain; }
    public PalettedContainerFactory blockbranch$factory() { return blockbranch$factory; }
    @Unique private final NavigableMap<Integer, LevelChunkSection> blockbranch$extra = new java.util.concurrent.ConcurrentSkipListMap<>();

    @Unique private final NavigableMap<Integer,LevelChunkSection> blockbranch$extraView=Collections.unmodifiableNavigableMap(blockbranch$extra);

    @ModifyVariable(method="<init>", at=@At("HEAD"), argsOnly=true)
    private static LevelHeightAccessor blockbranch$terrainAllocation(LevelHeightAccessor height) {
        return ColumnLimits.expanded(height) ? new TerrainWindow() : height;
    }
    @ModifyArg(method="<init>", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/lighting/ChunkSkyLightSources;<init>(Lnet/minecraft/world/level/LevelHeightAccessor;)V"))
    private LevelHeightAccessor blockbranch$skyBounds(LevelHeightAccessor height) { return height instanceof TerrainWindow ? ColumnLimits.BUILD : height; }
    @Inject(method="<init>", at=@At("RETURN"))
    private void blockbranch$init(ChunkPos pos, UpgradeData upgrade, LevelHeightAccessor height, PalettedContainerFactory factory,
        long time, LevelChunkSection[] initial, BlendingData blending, CallbackInfo ci) {
        // The marker preserves expanded-column identity while native arrays use terrain bounds.
        blockbranch$expanded = levelHeightAccessor instanceof TerrainWindow;
        blockbranch$factory = factory;
        blockbranch$empty = new EmptyColumnSection(factory);
    }
    @Override public boolean blockbranch$expanded() { return blockbranch$expanded; }
    @Override public int blockbranch$baseMinSection() { return levelHeightAccessor.getMinSectionY(); }
    @Override public NavigableMap<Integer, LevelChunkSection> blockbranch$sections() {
        var result = new TreeMap<Integer, LevelChunkSection>(blockbranch$extra);
        for (int i=0; i<sections.length; i++) if(blockbranch$terrain == null || blockbranch$terrain.resident(i + blockbranch$baseMinSection())) result.put(i + blockbranch$baseMinSection(), sections[i]);
        return result;
    }
    @Override public NavigableMap<Integer,LevelChunkSection> blockbranch$extraSections() { return blockbranch$extraView; }
    @Override public LevelChunkSection blockbranch$read(int y) {
        if(blockbranch$terrain != null) synchronized(blockbranch$terrain) {
            if(!blockbranch$terrain.resident(y)) return blockbranch$terrain.read(y);
            return blockbranch$storedSection(y);
        }
        return blockbranch$storedSection(y);
    }
    @Unique private LevelChunkSection blockbranch$storedSection(int y) {
        int i = y - blockbranch$baseMinSection();
        return i >= 0 && i < sections.length ? sections[i] : blockbranch$extra.isEmpty() ? blockbranch$empty : blockbranch$extra.getOrDefault(y, blockbranch$empty);
    }
    @Override public LevelChunkSection blockbranch$write(int y) {
        if (blockbranch$expanded && !ColumnLimits.containsY(y << 4)) throw new IndexOutOfBoundsException("Section Y " + y);
        if(blockbranch$terrain != null) {
            synchronized(blockbranch$terrain) {
                if(!blockbranch$terrain.resident(y)) blockbranch$replace(y, blockbranch$terrain.materialize(y));
                blockbranch$terrain.modified(y);
            }
        }
        int i = y - blockbranch$baseMinSection();
        return i >= 0 && i < sections.length ? sections[i] : blockbranch$extra.computeIfAbsent(y, key -> new LevelChunkSection(blockbranch$factory.createForBlockStates(), blockbranch$read(Math.clamp(key, blockbranch$baseMinSection(), blockbranch$baseMinSection()+sections.length-1)).getBiomes()));
    }
    @Override public void blockbranch$replace(int y, LevelChunkSection value) {
        if(blockbranch$terrain != null) synchronized(blockbranch$terrain) {
            blockbranch$replaceStoredSection(y,value);
            blockbranch$terrain.resident(y,value!=null);
            return;
        }
        blockbranch$replaceStoredSection(y,value);
    }
    @Unique private void blockbranch$replaceStoredSection(int y,LevelChunkSection value) {
        int i = y - blockbranch$baseMinSection();
        if (i >= 0 && i < sections.length) sections[i] = value == null ? new LevelChunkSection(blockbranch$factory) : value;
        else if (value == null) blockbranch$extra.remove(y);
        else blockbranch$extra.put(y, value);
    }
    // Native arrays and generation use terrain-relative indices. Sparse lookups translate those indices to absolute Y.
    @Override public int getSectionIndexFromSectionY(int y) { return y - blockbranch$baseMinSection(); }
    @Override public int getSectionYFromSectionIndex(int index) { return index + blockbranch$baseMinSection(); }
    @Override public int getSectionsCount() { return sections.length; }
    @Inject(method="getMinY", at=@At("HEAD"), cancellable=true)
    private void blockbranch$min(CallbackInfoReturnable<Integer> cir) { if(blockbranch$expanded) cir.setReturnValue(ColumnLimits.MIN_Y); }
    @Inject(method="getHeight()I", at=@At("HEAD"), cancellable=true)
    private void blockbranch$height(CallbackInfoReturnable<Integer> cir) { if(blockbranch$expanded) cir.setReturnValue(ColumnLimits.HEIGHT); }
    @Inject(method="getSection", at=@At("HEAD"), cancellable=true)
    private void blockbranch$section(int index, CallbackInfoReturnable<LevelChunkSection> cir) {
        if(blockbranch$expanded) cir.setReturnValue(blockbranch$read(getSectionYFromSectionIndex(index)));
    }
    @Inject(method="getHeightAccessorForGeneration", at=@At("HEAD"), cancellable=true)
    private void blockbranch$generation(CallbackInfoReturnable<LevelHeightAccessor> cir) { if(blockbranch$expanded) cir.setReturnValue(levelHeightAccessor); }
    @Inject(method="getNoiseBiome", at=@At("HEAD"), cancellable=true)
    private void blockbranch$biome(int x, int y, int z, CallbackInfoReturnable<Holder<Biome>> cir) {
        if(blockbranch$expanded) {
            int clamped = Math.clamp(y, ColumnLimits.MIN_Y >> 2, (ColumnLimits.MAX_Y-1) >> 2);
            cir.setReturnValue(blockbranch$read(clamped >> 2).getNoiseBiome(x & 3, clamped & 3, z & 3));
        }
    }
    @Inject(method="getHighestSectionPosition", at=@At("HEAD"), cancellable=true)
    private void blockbranch$highest(CallbackInfoReturnable<Integer> cir) {
        if(blockbranch$expanded) { int top = blockbranch$terrain == null ? ColumnLimits.MIN_Y : (blockbranch$terrain.base().highest()>>4)<<4;
            if(blockbranch$terrain!=null && !blockbranch$terrain.modifiedView().isEmpty()) top=Math.max(top,blockbranch$terrain.modifiedView().last()<<4);
            for(var e : blockbranch$extra.descendingMap().entrySet()) if(!e.getValue().hasOnlyAir()) { top=e.getKey()<<4; break; }
            for(int i=sections.length-1;i>=0;i--) if(!sections[i].hasOnlyAir()) { top=Math.max(top,(i+blockbranch$baseMinSection())<<4); break; }
            cir.setReturnValue(top);
        }
    }
    @Inject(method="isYSpaceEmpty",at=@At("HEAD"),cancellable=true)
    private void blockbranch$emptyRange(int min,int max,CallbackInfoReturnable<Boolean> cir) {
        if(!blockbranch$expanded) return;
        if(blockbranch$terrain != null) {
            boolean empty=true;
            for(int z=0;z<16 && empty;z++) for(int x=0;x<16 && empty;x++)
                empty=ColumnQueries.top((ChunkAccess)(Object)this, s -> !s.isAir(), x,z,max) <= min;
            cir.setReturnValue(empty); return;
        }
        if(min>max) { cir.setReturnValue(true); return; }
        int base=blockbranch$baseMinSection();
        for(int i=Math.max(0,(min>>4)-base);i<=Math.min(sections.length-1,(max>>4)-base);i++)
            if(!sections[i].hasOnlyAir()) { cir.setReturnValue(false); return; }
        for(var value:blockbranch$extra.subMap(min>>4,true,max>>4,true).values())
            if(!value.hasOnlyAir()) { cir.setReturnValue(false); return; }
        cir.setReturnValue(true);
    }
    @Inject(method="findBlocks", at=@At("HEAD"), cancellable=true)
    private void blockbranch$find(Predicate<BlockState> predicate, BiConsumer<BlockPos, BlockState> consumer, CallbackInfo ci) {
        if(!blockbranch$expanded) return;
        var chunk = (ChunkAccess)(Object)this;
        for(var e : blockbranch$sections().entrySet()) {
            var section=e.getValue(); if(section.hasOnlyAir() || !section.maybeHas(predicate)) continue;
            for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                var state=section.getBlockState(x,y,z);
                if(predicate.test(state)) consumer.accept(new BlockPos(chunk.getPos().getMinBlockX()+x,(e.getKey()<<4)+y,chunk.getPos().getMinBlockZ()+z),state);
            }
        }
        ci.cancel();
    }
}
