package blockbranch.wildlife;
import blockbranch.cubic.*;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** Surface/water attempts in existing active windows, including local edited floors. */
public final class WildlifeSpawnPosition {
    public static BlockPos sample(ServerLevel level,LevelChunk chunk) {
        var terrain=((SparseColumn)chunk).blockbranch$terrain();
        var random=level.getRandom();
        int x=chunk.getPos().getMinBlockX()+random.nextInt(16),z=chunk.getPos().getMinBlockZ()+random.nextInt(16);
        var windows=VerticalRuntime.activeSectionRanges(level,x,z);
        if(windows.isEmpty())return new BlockPos(x,level.getMinY(),z);
        var opaque=Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque();
        int naturalTop=terrain.base().top(opaque,x,z,ColumnLimits.MAX_Y-1);
        var heights=new HashSet<Integer>();
        for(var window:windows) {
            int min=window.min()<<4,max=(window.max()<<4)+15;
            for(int y=Math.max(min,naturalTop-16);y<=Math.min(max,naturalTop);y++)heights.add(y);
            // Only edited sections inside existing active ranges are read/materialized.
            for(int sectionY:terrain.modifiedView().subSet(window.min(),true,window.max(),true)) {
                int bottom=sectionY<<4;
                for(int localY=15;localY>=0;localY--) {
                    int y=bottom+localY;
                    if(y+1>max)continue;
                    var pos=new BlockPos(x,y,z);
                    if(opaque.test(chunk.getBlockState(pos))&&!opaque.test(chunk.getBlockState(pos.above())))
                        for(int candidate=Math.max(min,y-15);candidate<=y+1;candidate++)heights.add(candidate);
                }
            }
        }
        if(heights.isEmpty())return new BlockPos(x,level.getMinY(),z);
        int selected=0,count=0;
        for(int height:heights)if(random.nextInt(++count)==0)selected=height;
        return new BlockPos(x,selected,z);
    }
    private WildlifeSpawnPosition() {}
}
