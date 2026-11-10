package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @Inject(method="getRandomPosWithin",at=@At("HEAD"),cancellable=true)
    private static void blockbranch$spawn(Level level,LevelChunk chunk,CallbackInfoReturnable<BlockPos> cir) {
        var sparse=(SparseColumn)chunk;
        // Procedural columns use the active-window surface sampler. Do not override
        // its result with the legacy resident-section search at the same HEAD.
        if(sparse.blockbranch$terrain()!=null || !sparse.blockbranch$expanded()) return;
        var random=level.getRandom(); int x=random.nextInt(16),z=random.nextInt(16);
        var sections=sparse.blockbranch$sections(); var ranges=new ArrayList<int[]>();
        int terrainTop=-64;
        for(var entry:sections.subMap(-4,true,19,true).descendingMap().entrySet()) {
            for(int y=15;y>=0;y--) if(!entry.getValue().getBlockState(x,y,z).isAir()) { terrainTop=(entry.getKey()<<4)+y+1; break; }
            if(terrainTop>-64) break;
        }
        ranges.add(new int[]{-64,terrainTop});
        for(var entry:sections.entrySet()) if((entry.getKey() < -4 || entry.getKey()>19) && !entry.getValue().hasOnlyAir())
            ranges.add(new int[]{entry.getKey()<<4,Math.min(ColumnLimits.MAX_Y-1,(entry.getKey()<<4)+16)});
        ranges.sort(Comparator.comparingInt(a->a[0]));
        var merged=new ArrayList<int[]>();
        for(var range:ranges) {
            if(!merged.isEmpty() && range[0]<=merged.getLast()[1]+1) merged.getLast()[1]=Math.max(merged.getLast()[1],range[1]);
            else merged.add(range);
        }
        int total=0; for(var range:merged) total+=range[1]-range[0]+1;
        int choice=random.nextInt(total),selected=-64;
        for(var range:merged) { int size=range[1]-range[0]+1; if(choice<size) { selected=range[0]+choice; break; } choice-=size; }
        cir.setReturnValue(new BlockPos(chunk.getPos().getMinBlockX()+x,selected,chunk.getPos().getMinBlockZ()+z));
    }
}
