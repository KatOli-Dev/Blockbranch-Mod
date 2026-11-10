package blockbranch.realistic;

import blockbranch.cubic.SparseColumn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;

/** Only the biome contribution to mob attributes uses the prior surface column. */
public final class DripstoneSpawnRouting {
    private DripstoneSpawnRouting(){}
    public static BiomeResolver surfaceMobs(BiomeResolver original){
        if(!(original instanceof ServerLevel level))return original;
        return (x,y,z)->{
            var actual=original.getNoiseBiome(x,y,z);
            if(level.getChunkSource().getGenerator() instanceof RealisticGenerator g&&((g.dripstoneDefinition()>0&&actual.is(Biomes.DRIPSTONE_CAVES))||(g.caveBiomeDefinition()>0&&(actual.is(Biomes.LUSH_CAVES)||actual.is(Biomes.SULFUR_CAVES))))){
                var chunk=level.getChunk(x>>2,z>>2);var terrain=((SparseColumn)chunk).blockbranch$terrain();
                if(terrain!=null)return terrain.base().biomes().get(x&3,y&3,z&3);
                var source=(ScreeBiomeSource)g.getBiomeSource();if(source.bound())return source.surfaceResolver().getNoiseBiome(x,y,z);
            }
            return actual;
        };
    }
}
