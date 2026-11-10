package blockbranch.cubic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Compare the same vanilla generator using ordinary and expanded chunk height accessors. */
public final class ColumnGenerationCheck {
    public static void check(ServerLevel level) {
        var generator=level.getChunkSource().getGenerator();
        var random=level.getChunkSource().randomState();
        var normal=terrainChunk(level,ColumnLimits.TERRAIN);
        var expanded=terrainChunk(level,level);
        for(var chunk:new ProtoChunk[]{normal,expanded}) {
            var biomes=generator.createBiomes(random,Blender.empty(),level.structureManager(),chunk);
            level.getServer().managedBlock(biomes::isDone); biomes.join();
            chunk.setPersistedStatus(ChunkStatus.BIOMES);
            var terrain=generator.buildTerrain(chunk,Blender.empty(),random,level.structureManager(),level.getBiomeManager(),null,generator.getBiomeSource().possibleBiomes());
            level.getServer().managedBlock(terrain::isDone); terrain.join();
        }
        var position=new net.minecraft.core.BlockPos.MutableBlockPos();
        for(int y=-64;y<320;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
            position.set(x,y,z);
            if(normal.getBlockState(position)!=expanded.getBlockState(position)) throw new AssertionError("Terrain differs at "+position);
        }
        for(int y=-16;y<80;y++) for(int z=0;z<4;z++) for(int x=0;x<4;x++)
            if(!normal.getNoiseBiome(x,y,z).equals(expanded.getNoiseBiome(x,y,z))) throw new AssertionError("Biome differs at "+x+","+y+","+z);
        for(var type:Heightmap.Types.values()) for(int z=0;z<16;z++) for(int x=0;x<16;x++)
            if(normal.getHeight(type,x,z)!=expanded.getHeight(type,x,z)) throw new AssertionError("Heightmap differs: "+type);
        blockbranch.Blockbranch.LOGGER.info("COLUMN_GENERATION_PASS seed={} blocks=98304 biomes=1536",random.seed());
    }
    private static ProtoChunk terrainChunk(ServerLevel level,net.minecraft.world.level.LevelHeightAccessor bounds) {
        return new ProtoChunk(new ChunkPos(0,0),UpgradeData.EMPTY,bounds,level.palettedContainerFactory(),null) {
            @Override public net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos) {
                // Surface lookahead may read the block immediately below terrain.
                // A deeper read reveals an accidental scan toward the build floor.
                if(pos.getY()<ColumnLimits.TERRAIN.getMinY()-1) throw new AssertionError("Generation read below terrain padding: "+pos);
                return super.getBlockState(pos);
            }
        };
    }
    private ColumnGenerationCheck() {}
}
