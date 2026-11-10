package blockbranch.cubic;
import java.util.*;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
/** Read-only derived views. Reacquire after topology changes; native light snapshots own values. */
public interface LightColumnIndex {
    NavigableMap<Integer,DataLayer> blockbranch$layers(long zeroNode);
    default NavigableSet<Integer> blockbranch$sectionYs(long zeroNode){return blockbranch$layers(zeroNode).navigableKeySet();}
    default DataLayer blockbranch$layer(long node){return blockbranch$layers(SectionPos.getZeroNode(node)).get(SectionPos.y(node));}
}
