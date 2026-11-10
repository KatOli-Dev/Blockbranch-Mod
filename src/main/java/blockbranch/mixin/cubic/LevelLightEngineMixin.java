package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.*;
import org.spongepowered.asm.mixin.*;
@Mixin(LevelLightEngine.class)
abstract class LevelLightEngineMixin implements SparseLight {
    @Shadow @Final private LightEngine<?,?> blockEngine;
    @Shadow @Final private LightEngine<?,?> skyEngine;
    public Set<Integer> blockbranch$lightSections(long chunk) {
        return blockbranch$sections(chunk,false);
    }
    public Set<Integer> blockbranch$updatingLightSections(long chunk) { return blockbranch$sections(chunk,true); }
    @Unique private Set<Integer> blockbranch$sections(long chunk,boolean updating) {
        long zero=SectionPos.getZeroNode(ChunkPos.getX(chunk),ChunkPos.getZ(chunk));
        var result=new TreeSet<Integer>();
        blockbranch$collect(blockEngine,zero,result,updating);
        blockbranch$collect(skyEngine,zero,result,updating);
        return result;
    }
    @Unique private static void blockbranch$collect(LightEngine<?,?> engine,long zero,Set<Integer> result,boolean updating) {
        if(engine==null) return;
        var storage=(LightStorageAccessor)((LightEngineAccessor)engine).blockbranch$storage();
        result.addAll(((LightColumnIndex)storage.blockbranch$visible()).blockbranch$sectionYs(zero));
        if(updating) result.addAll(((LightColumnIndex)storage.blockbranch$updating()).blockbranch$sectionYs(zero));
        var queued=storage.blockbranch$queued();
        synchronized(queued) { for(long node:queued.keySet()) if(SectionPos.getZeroNode(node)==zero) result.add(SectionPos.y(node)); }
    }
}
