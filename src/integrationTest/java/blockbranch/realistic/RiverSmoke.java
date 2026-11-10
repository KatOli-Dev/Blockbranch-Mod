package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;

/** Actual vanilla River identity, source fluids, bed profiles and independent saved edits. */
public final class RiverSmoke {
 private static final Map<DrainageNetwork,BlockPos> SCENES=Collections.synchronizedMap(new WeakHashMap<>());
 private static final Map<DrainageNetwork,BlockPos> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
 public static BlockPos fixture(RealisticGenerator g,RandomState r){return FIXTURES.computeIfAbsent(g.drainage(r),d->{if(r.seed()==424242)return new BlockPos(-944752,d.sample(-944752,-1698384).height(),-1698384);var p=blockbranch.realistic.RoutedRiverFixtures.fixture(d,r.seed());return new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());});}
 /** Additional lowland visual site; the original gorge native witness is never substituted. */
 public static BlockPos scene(RealisticGenerator g,RandomState r){if(r.seed()==424242)return new BlockPos(1359388,g.drainage(r).sample(1359388,-1694456).height(),-1694456);return SCENES.computeIfAbsent(g.drainage(r),d->{var c=new ClimateField(r.seed());
  for(int i=0;i<d.size();i++){if(!d.river(i)||Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000)continue;
   for(double t:new double[]{.3,.5,.7}){int x=Math.floorDiv((int)Math.round(d.edgeX(i,t)),4)*4,z=Math.floorDiv((int)Math.round(d.edgeZ(i,t)),4)*4;if(!RoutedRiver.contains(r.seed(),x,z,d,c))continue;var site=RiverChannel.site(d,x,z);var cl=c.sample(x,z,d::sample);if(site.depth()<3||site.depth()>9||site.channel().halfWidth()<16||site.channel().halfWidth()>48||cl.meanTemperatureC()<8||cl.meanTemperatureC()>22||RoutedRiver.cover(r.seed(),x,z,d,c)!=7)continue;var a=RoutedRiver.axis(d,x,z);boolean suitable=true;int max=Integer.MIN_VALUE;
    for(int side:new int[]{-1,1})for(int along:new int[]{-64,-32,0,32,64}){int bx=x+(int)Math.round(-a[1]*site.channel().halfWidth()*1.6*side+a[0]*along),bz=z+(int)Math.round(a[0]*site.channel().halfWidth()*1.6*side+a[1]*along);var q=d.sample(bx,bz);if(q.waterLevel()!=DrainageNetwork.DRY||q.height()>site.water()+18||q.height()<site.water()-1||!LowlandMeadow.contains(bx,bz,d,c))suitable=false;}
    if(!suitable)continue;for(int oz=-128;oz<=128;oz+=64)for(int ox=-128;ox<=128;ox+=64)max=Math.max(max,d.sample(x+ox,z+oz).height());if(max<=site.water()+45)return new BlockPos(x,site.height(),z);
   }
  }throw new AssertionError("No natural vegetated lowland River scene");});}
 public static void restore(ServerLevel l,BlockPos p){var b=((SparseColumn)l.getChunkAt(p)).blockbranch$terrain().base();l.setBlock(p,b.state(p.getX(),p.getY(),p.getZ()),3);}
 public static double[] camera(DrainageNetwork d,BlockPos p,boolean elevated){if(Boolean.getBoolean("blockbranch.terrainContourComparison"))return elevated?new double[]{1359373.4677450322,119.,-1694501.3838525992,-3.600717690984339,38.97316934921043}:new double[]{1359339.189791282,d.sample(1359339,-1694493).height()+1.,-1694492.199599013,-33.05958511842924,4.446124403060553};var s=RiverChannel.site(d,p.getX(),p.getZ());var a=RoutedRiver.axis(d,p.getX(),p.getZ());double side=elevated?.8:1.45,along=elevated?-35:-12,x=p.getX()-a[1]*s.channel().halfWidth()*side+a[0]*along+.5,z=p.getZ()+a[0]*s.channel().halfWidth()*side+a[1]*along+.5,y=elevated?s.water()+55:Math.max(s.water()+1.,d.sample((int)Math.floor(x),(int)Math.floor(z)).height()+1.);double tx=p.getX()+a[0]*25+.5,tz=p.getZ()+a[1]*25+.5,dx=tx-x,dz=tz-z,dy=s.water()-.5-y;return new double[]{x,y,z,Math.toDegrees(Math.atan2(-dx,dz)),-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)))};}
 public static void verify(ServerLevel l,RealisticGenerator g,boolean reload){var r=l.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());var p=fixture(g,r);var saved=p.below(8);
  if(reload)require(l.getBlockState(p).is(Blocks.CLAY)&&l.getBlockState(saved).is(Blocks.GOLD_BLOCK),"Saved River bed and deep edit survive reopen");restore(l,p);
  require(l.getBiome(p).is(net.minecraft.world.level.biome.Biomes.RIVER)&&l.getBlockState(p).is(Blocks.SAND),"Actual vanilla River and natural sand-bar witness");
  int[] covers=new int[15];var resolver=g.getBiomeSource().createUncachedResolver(r);
  for(int cz=(p.getZ()>>4)-1;cz<=(p.getZ()>>4)+1;cz++)for(int cx=(p.getX()>>4)-1;cx<=(p.getX()>>4)+1;cx++){var chunk=l.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(l.palettedContainerFactory());var copy=TerrainProfile.decode(tag,l.palettedContainerFactory());var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
   for(int z=0;z<16;z+=4)for(int x=0;x<16;x+=4){int wx=cx*16+x,wz=cz*16+z;var s=d.sample(wx,wz);int top=Math.max(s.height()+3,s.waterLevel()==DrainageNetwork.DRY?s.height()+3:s.waterLevel()+1);int cover=g.riverMarginCover(wx,wz,r);covers[cover]++;
    for(int y=s.height()-3;y<=top;y++){var q=new BlockPos(wx,y,wz);var expected=g.surfaceState(wx,y,wz,r);require(chunk.getBlockState(q)==expected&&copy.state(wx,y,wz)==expected&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x,y&15,z)==expected,"Native/encoded/client River bed and water agreement");if(cover>=5&&cover<=12&&y>s.height()&&y<s.waterLevel())require(l.getFluidState(q).isSource(),"All River liquid remains source water");}
    for(var bounds:new LevelHeightAccessor[]{l,LevelHeightAccessor.create(s.height()-2,3),LevelHeightAccessor.create(s.height()+1,3),LevelHeightAccessor.create(top-3,3)}){var col=g.getBaseColumn(wx,wz,bounds,r);for(var type:Heightmap.Types.values()){int h=bounds.getMinY();for(int y=Math.min(bounds.getMaxY(),TerrainShape.CEILING-1);y>=Math.max(bounds.getMinY(),TerrainShape.FLOOR);y--)if(type.isOpaque().test(col.getBlock(y))){h=y+1;break;}require(g.getBaseHeight(wx,wz,type,bounds,r)==h,"Clipped River heightmaps "+type);if(bounds==l)require(chunk.getHeight(type,x,z)+1==h,"Actual River native heightmaps");}}
    for(int y:new int[]{-3000,0,s.height()>>2,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(resolver.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(resolver.getNoiseBiome(wx>>2,y,wz>>2)),"All-height River source/stored/decoded identity");
    if(RoutedRiver.contains(r.seed(),wx,wz,d,c))require(!d.terminalLakeWater(wx,wz,s)&&!d.oceanWater(wx,wz,s),"River never claims lake or sea");
   }
   byte[] original=tag.getByteArray("river_margin").orElseThrow();if(original.length>0){byte old=original[0];original[0]=13;require(base.encode(l.palettedContainerFactory()).getByteArray("river_margin").orElseThrow()[0]==old,"River serialized snapshot isolation");}
  }
  boundary(l,g,p);
  var terrain=((SparseColumn)l.getChunkAt(p)).blockbranch$terrain();l.setBlock(p,Blocks.CLAY.defaultBlockState(),3);l.setBlock(saved,Blocks.GOLD_BLOCK.defaultBlockState(),2);l.getChunkAt(p).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(l.getBlockState(p).is(Blocks.CLAY)&&l.getBlockState(saved).is(Blocks.GOLD_BLOCK),"River bed/deep edits survive lazy eviction");
  RiverBankSmoke.verify(l,g,reload);
  Blockbranch.LOGGER.info("BROAD_RIVER_NATIVE_PASS {} bed={} codes={}",reload?"RELOAD":"NEW",p,Arrays.toString(covers));
 }
 private static void boundary(ServerLevel l,RealisticGenerator g,BlockPos p){var r=l.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());for(int[]a:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int off=0;off<=1024;off+=16){int cx=(p.getX()+a[0]*off)>>4,cz=(p.getZ()+a[1]*off)>>4,n=0;for(int z=0;z<4;z++)for(int x=0;x<4;x++)if(RoutedRiver.contains(r.seed(),cx*16+x*4,cz*16+z*4,d,c))n++;if(n==0||n==16)continue;var chunk=l.getChunk(cx,cz);var copy=TerrainProfile.decode(((SparseColumn)chunk).blockbranch$terrain().base().encode(l.palettedContainerFactory()),l.palettedContainerFactory());for(int z=0;z<4;z++)for(int x=0;x<4;x++){boolean in=RoutedRiver.contains(r.seed(),cx*16+x*4,cz*16+z*4,d,c);require(chunk.getNoiseBiome(cx*4+x,0,cz*4+z).is(net.minecraft.world.level.biome.Biomes.RIVER)==in&&copy.biomes().get(x,0,z).is(net.minecraft.world.level.biome.Biomes.RIVER)==in,"Mixed River boundary and encoded identity");}Blockbranch.LOGGER.info("BROAD_RIVER_BOUNDARY_PASS chunk={} selectedQuartCells={}",chunk.getPos(),n);return;}throw new AssertionError("No mixed River core/fringe boundary");}
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private RiverSmoke(){}
}
