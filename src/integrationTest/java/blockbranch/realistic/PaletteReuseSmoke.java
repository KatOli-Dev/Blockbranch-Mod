package blockbranch.realistic;

import blockbranch.cubic.*;
import com.google.gson.GsonBuilder;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;

/** Exact old-selector comparison plus bounded same-JVM palette construction observation. */
public final class PaletteReuseSmoke {
 private record Input(TerrainProfile profile,int sy){}
 public static void verify(ServerLevel level,RealisticGenerator g)throws Exception{
  var r=level.getChunkSource().randomState();var origin=g.getOrigin(r);var coords=new ArrayList<int[]>();for(int z=-12;z<=12;z+=2)for(int x=-12;x<=12;x+=2)coords.add(new int[]{origin.x()+x,origin.z()+z});coords.sort(Comparator.comparingDouble((int[] c)->-DripstoneHabitat.province(r.seed(),c[0]*16,c[1]*16)));
  var inputs=new ArrayList<Input>();int checks=0,reused=0,changed=0;
  for(int n=0;n<16;n++){var c=coords.get(n<12?n:coords.size()-1-(n-12));var p=((SparseColumn)level.getChunk(c[0],c[1])).blockbranch$terrain().base();int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;for(int z=0;z<16;z++)for(int x=0;x<16;x++){low=Math.min(low,p.groundHeight(x,z));high=Math.max(high,p.groundHeight(x,z));}
   for(int sy=(low-352)>>4;sy<=(high+32)>>4;sy++){var old=new OldView(p,sy,level.palettedContainerFactory()).getBiomes();var actual=p.view(sy).getBiomes();boolean differs=false;for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++){require(old.get(x,y,z).equals(actual.get(x,y,z)),"Exact old/new quartet at section "+sy);differs|=!old.get(x,y,z).equals(p.biomes().get(x,y,z));checks++;}if(!p.dripstone().mayHaveBiome(sy,low,high)){require(!differs&&actual==p.biomes(),"Rejected palette reuses original immutable surface palette");reused++;}else if(differs)changed++;inputs.add(new Input(p,sy));}
  }
  require(reused>0&&changed>0,"Both retained underground overrides and rejected sections exercised");
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);var rows=new ArrayList<Map<String,Object>>();
  for(int warm=0;warm<2;warm++){probe(inputs,level,true,bean);probe(inputs,level,false,bean);}
  for(int pass=0;pass<3;pass++)for(boolean old:pass%2==0?new boolean[]{true,false}:new boolean[]{false,true}){var row=probe(inputs,level,old,bean);row.put("pass",pass);row.put("original",old);rows.add(row);}
  var report=new LinkedHashMap<String,Object>();report.put("seed",r.seed());report.put("quartChecks",checks);report.put("reusedSections",reused);report.put("changedOverrideSections",changed);report.put("inputsPerPass",inputs.size());report.put("measurements",rows);report.put("limits","Same-JVM fresh view construction micro-observation, not live frame/tick allocation or end-to-end loading. OldView reproduces old palette loop; wrapper object size may differ slightly. Full palette equality is checked separately.");
  var dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();Files.writeString(dir.resolve("palette-reuse.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
 }
 private static LinkedHashMap<String,Object> probe(List<Input> inputs,ServerLevel level,boolean old,com.sun.management.ThreadMXBean bean){long thread=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(thread),start=System.nanoTime(),checksum=0;for(var in:inputs){var palette=old?new OldView(in.profile,in.sy,level.palettedContainerFactory()).getBiomes():in.profile.view(in.sy).getBiomes();checksum+=palette.get(0,0,0).hashCode();}long elapsed=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(thread)-before;var row=new LinkedHashMap<String,Object>();row.put("elapsedNs",elapsed);row.put("allocatedBytes",bytes);row.put("checksum",checksum);return row;}
 private static final class OldView extends LevelChunkSection {
  private final TerrainProfile p;private final int sy;private PalettedContainerRO<Holder<Biome>> cached;
  OldView(TerrainProfile p,int sy,PalettedContainerFactory factory){super(new PalettedContainer<>(Blocks.AIR.defaultBlockState(),factory.blockStatesStrategy()),p.biomes());this.p=p;this.sy=sy;}
  @Override public synchronized PalettedContainerRO<Holder<Biome>> getBiomes(){if(cached!=null)return cached;if(!p.dripstone().enabled())return cached=p.biomes();var result=p.biomes().recreate();boolean changed=false;for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++){var value=p.biome(x,(sy<<2)+y,z);result.set(x,y,z,value);changed|=!value.equals(p.biomes().get(x,y,z));}return cached=changed?UniformSnapshots.compactBiomes(result):p.biomes();}
 }
 private static void require(boolean v,String s){if(!v)throw new AssertionError(s);}
}
