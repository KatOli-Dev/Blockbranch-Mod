package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.RandomState;
public final class GroveSmoke {
 private static final Map<DrainageNetwork,BlockPos> FIXTURE=Collections.synchronizedMap(new WeakHashMap<>());
 public static BlockPos fixture(RealisticGenerator g,RandomState r){return FIXTURE.computeIfAbsent(g.drainage(r),d->{if(Boolean.getBoolean("blockbranch.groveComparison")){if(r.seed()!=424242||!Grove.contains(r.seed(),-1359947,-1900027,d,new ClimateField(r.seed())))throw new AssertionError("Original Grove comparison stand no longer suitable");return new BlockPos(-1359947,d.sample(-1359947,-1900027).height(),-1900027);}var p=blockbranch.realistic.GroveFixtures.fixture(d,r.seed());return new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());});}
 public static void verify(ServerLevel l,RealisticGenerator g,boolean reload){SnowyTaigaSmoke.verify(l,g,reload,true);var r=l.getChunkSource().randomState();var p=fixture(g,r);var d=g.drainage(r);var c=new ClimateField(r.seed());var e=new GroundEcology.Field(d::sample,c);var resolver=g.getBiomeSource().createUncachedResolver(r);for(int axis=0;axis<2;axis++)for(int off=-4096;off<=4096;off+=16){int cx=(p.getX()+(axis==0?off:0))>>4,cz=(p.getZ()+(axis==1?off:0))>>4,n=0;for(int z=0;z<4;z++)for(int x=0;x<4;x++)if(Grove.contains(r.seed(),cx*16+x*4,cz*16+z*4,d,c,e))n++;if(n==0||n==16)continue;var chunk=l.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(base.encode(l.palettedContainerFactory()),l.palettedContainerFactory());for(int z=0;z<4;z++)for(int x=0;x<4;x++)for(int y:new int[]{-3000,0,p.getY()>>2,2000})require(chunk.getNoiseBiome(cx*4+x,y,cz*4+z).equals(resolver.getNoiseBiome(cx*4+x,y,cz*4+z))&&copy.biomes().get(x,y&3,z).equals(resolver.getNoiseBiome(cx*4+x,y,cz*4+z)),"Grove mixed source/stored/decoded identity at every height");for(int x=0;x<20;x++){int wx=cx*16+x,wz=cz*16,h=d.sample(wx,wz).height();for(int y=h-3;y<=h+3;y++)require(l.getBlockState(new BlockPos(wx,y,wz))==g.surfaceState(wx,y,wz,r),"Grove native boundary material/fluid/seam continuity");}Blockbranch.LOGGER.info("GROVE_BOUNDARY_PASS chunk={} selectedQuartCells={}",chunk.getPos(),n);return;}throw new AssertionError("No mixed Grove boundary near stand");}
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private GroveSmoke(){}
}
