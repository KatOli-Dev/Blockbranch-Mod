package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native material seams, biome independence, profile/render mirrors and disk edits at fixed boundaries. */
public final class SurfaceMaterialSmoke {
    private static final int[][] BOUNDARIES={
        // Full-scene capture exposed a coarse Plains refinement outside actual soil support here.
        {1359408,-1694640},
        {1359432,-1694412},{-1050028,-1700112},{11679,-756038},{-1079521,-244040},
        {-1679008,-480035},{-1727872,-452125},{-1390440,-1700424},{-927292,-1896744},
        {-1371995,-1883965},{386556,-1210560},{404238,-1211819},{-1318712,-1697764},
        {-798676,-1695080},{-1461100,-1698016},{-765600,-1700131},{-1310313,-1697025},
        {1284090,-1687201},{1359420,-1694424},{-1412024,-1844148},
        {-1611988,-1868041},{-795976,-1852048},{-765464,-1700209},{-1340916,-1687864}
    };
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);
        var resolver=g.getBiomeSource().createUncachedResolver(random);
        int checked=0;
        for(var anchor:BOUNDARIES){
            var chunk=level.getChunk(anchor[0]>>4,anchor[1]>>4);
            var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            var mirrors=new HashMap<Integer,LevelChunkSection>();
            for(int dz=0;dz<16;dz+=5)for(int dx=0;dx<16;dx+=5){
                int x=chunk.getPos().getMinBlockX()+dx,z=chunk.getPos().getMinBlockZ()+dz;var site=d.sample(x,z);
                for(int y=site.height()-6;y<=site.height()+3;y++){
                    var pos=new BlockPos(x,y,z);var expected=g.surfaceState(x,y,z,random);
                    require(chunk.getBlockState(pos)==expected&&decoded.state(x,y,z)==expected
                        &&mirrors.computeIfAbsent(y>>4,decoded::generate).getBlockState(dx,y&15,dz)==expected,"Native/profile/client material agreement at "+pos);
                }
                for(int qy:new int[]{-3000,0,site.height()>>2,2000}){
                    var expected=resolver.getNoiseBiome(x>>2,qy,z>>2);
                    require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(expected)&&decoded.biomes().get(dx>>2,qy&3,dz>>2).equals(expected),"Surface paint preserves routed biome at "+x+","+z);
                }
                for(var type:Heightmap.Types.values())require(chunk.getHeight(type,dx,dz)+1==g.getBaseHeight(x,z,type,level,random),"Native material heightmap "+type+" at "+x+","+z);
                if(site.waterLevel()!=DrainageNetwork.DRY){
                    var water=new BlockPos(x,site.waterLevel()-1,z);
                    require(chunk.getBlockState(water)==g.surfaceState(x,water.getY(),z,random),"Water plane unchanged");
                }
                checked++;
            }
            var edit=new BlockPos(anchor[0],d.sample(anchor[0],anchor[1]).height()-10,anchor[1]);
            if(reload)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Material fixture edit survives disk reopen at "+edit);
            level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),2);
            Blockbranch.LOGGER.info("SURFACE_MATERIAL_NATIVE_BOUNDARY_PASS x={} z={} reload={}",anchor[0],anchor[1],reload);
        }
        RiverSmoke.verify(level,g,reload);
        RiverBankSmoke.verify(level,g,reload);
        Blockbranch.LOGGER.info("SURFACE_MATERIAL_NATIVE_PROFILE_BIOME_HEIGHTMAP_SAVE_PASS columns={} samples={} reload={}",BOUNDARIES.length,checked,reload);
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private SurfaceMaterialSmoke(){}
}
