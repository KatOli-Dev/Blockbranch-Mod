package blockbranch.cubic;
import net.minecraft.world.level.chunk.LevelChunkSection;
/** Only discard a legacy section after proving its blocks and biomes equal the base. */
public final class ColumnMigration {
    public static boolean matches(TerrainProfile base,int sy,LevelChunkSection section) {
        for(int y=0;y<4;y++) for(int z=0;z<4;z++) for(int x=0;x<4;x++)
            if(!section.getNoiseBiome(x,y,z).equals(base.biome(x,(sy<<2)+y,z))) return false;
        if(section.getStates().bitsPerEntry()==0 && !base.boundaries().contains(sy) && !base.undergroundMayAffect(sy) && !base.oresMayAffect(sy) && !base.dripstone().sections().contains(sy)) {
            var value=section.getBlockState(0,0,0);
            for(int z=0;z<16;z++) for(int x=0;x<16;x++)
                if(base.state(x,sy*16,z)!=value || base.state(x,sy*16+15,z)!=value) return false;
            return true;
        }
        for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++)
            if(section.getBlockState(x,y,z)!=base.state(x,sy*16+y,z)) return false;
        return true;
    }
    private ColumnMigration() {}
}
