package blockbranch.realistic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;

/** Analytic fixture selection only: generated terrain is never edited or populated by this helper. */
final class WildlifeNaturalSites {
    static BlockPos beachEdge(RealisticGenerator generator,RandomState random,boolean snow){
        return beachEdge(generator,random,snow,0);
    }
    static BlockPos beachEdge(RealisticGenerator generator,RandomState random,boolean snow,int along){
        var drainage=generator.drainage(random);var climate=new ClimateField(random.seed());
        var scene=CoastalBeachScenes.forWorld(random.seed(),snow,drainage,climate);
        var shore=CoastalBeachScenes.transect(scene,drainage,climate,along);
        int x=(int)Math.floor(shore.x()-scene.seaX()*2),z=(int)Math.floor(shore.z()-scene.seaZ()*2);
        return new BlockPos(x,drainage.sample(x,z).height(),z);
    }
    private WildlifeNaturalSites(){}
}
