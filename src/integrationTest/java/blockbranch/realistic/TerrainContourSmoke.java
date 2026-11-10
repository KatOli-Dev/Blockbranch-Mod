package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
/** Original reported sites, independent of selecting a prettier biome witness. */
public final class TerrainContourSmoke {
 public static void verify(ServerLevel l,RealisticGenerator g){var r=l.getChunkSource().randomState();var d=g.drainage(r);
  for(int[]p:new int[][]{{1359388,-1694456},{-1332024,-1900024},{-1332081,-1900114},{-1332050,-1900063},{1860000,-1404000}}){int checked=0;for(int cz=(p[1]>>4)-1;cz<=(p[1]>>4)+1;cz++)for(int cx=(p[0]>>4)-1;cx<=(p[0]>>4)+1;cx++){var chunk=l.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(l.palettedContainerFactory());var copy=TerrainProfile.decode(tag,l.palettedContainerFactory());var heights=tag.getIntArray("ground").orElseThrow();var water=tag.getIntArray("water").orElseThrow();var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
   for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z;var sample=d.sample(wx,wz);RiverSmoke.require(heights[z*16+x]==sample.height()&&water[z*16+x]==sample.waterLevel(),"Per-block source/stored geometry");for(int y=sample.height()-4;y<=sample.height()+2;y++){var pos=new BlockPos(wx,y,wz);var expected=g.surfaceState(wx,y,wz,r);RiverSmoke.require(chunk.getBlockState(pos)==expected&&copy.state(wx,y,wz)==expected&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x,y&15,z)==expected,"Source/native/decoded/client geometry at "+pos);}checked++;}
  }Blockbranch.LOGGER.info("TERRAIN_CONTOUR_SOURCE_NATIVE_DECODED_PASS site={},{} samples={}",p[0],p[1],checked);}
 }
 private TerrainContourSmoke(){}
}
