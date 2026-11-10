package blockbranch.realistic;

import blockbranch.cubic.SparseColumn;
import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;

/** Actual source propagation/removal in an isolated generated cave fixture. */
public final class CaveEmitterSmoke {
 public static void verify(ServerLevel level)throws Exception{
  var dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();var a=JsonParser.parseString(Files.readString(dir.resolve("dripstone-cameras.json"))).getAsJsonObject().getAsJsonArray("cameras").get(0).getAsJsonArray();var pos=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());
  require(level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir(),"Unmodified cave air fixture");var old=level.getBlockState(pos);var light=level.getChunkSource().getLightEngine();
  for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){var c=level.getChunk((pos.getX()>>4)+dx,(pos.getZ()>>4)+dz);for(int sy=(pos.getY()>>4)-1;sy<=(pos.getY()>>4)+1;sy++)light.updateSectionStatus(SectionPos.of(c.getPos(),sy),((SparseColumn)c).blockbranch$read(sy).hasOnlyAir());}
  var fence=light.waitForPendingTasks(pos.getX()>>4,pos.getZ()>>4);level.getServer().managedBlock(fence::isDone);fence.join();require(level.getBrightness(LightLayer.SKY,pos)==0,"Deep cave is naturally dark");
  try{level.setBlock(pos,Blocks.GLOWSTONE.defaultBlockState(),3);require(((SparseColumn)level.getChunkAt(pos)).blockbranch$read(pos.getY()>>4).maybeHas(s->s.getLightEmission()>0),"Genuine resident emitter is retained");light.checkBlock(pos);var on=light.waitForPendingTasks(pos.getX()>>4,pos.getZ()>>4);level.getServer().managedBlock(on::isDone);on.join();require(level.getBrightness(LightLayer.BLOCK,pos)==15&&level.getBrightness(LightLayer.BLOCK,pos.above())==14,"Native genuine emitter propagates in cave air");}
  finally{level.setBlock(pos,old,3);light.checkBlock(pos);var off=light.waitForPendingTasks(pos.getX()>>4,pos.getZ()>>4);level.getServer().managedBlock(off::isDone);off.join();}
  require(level.getBrightness(LightLayer.BLOCK,pos.above())==0,"Removing source clears native cave light");Files.writeString(dir.resolve("emitter-result.txt"),"PASS source15 adjacent14 removed0 sky0 at "+pos+"\n");
 }
 private static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
}
