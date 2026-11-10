package blockbranch.qa;
import java.nio.file.*;
import java.util.concurrent.atomic.LongAdder;
/** Counts actual compacting returns and actual shared-cache entries; never retains arguments. */
public final class UniformBiomeReuseProbe {
 private static final boolean ON=Boolean.getBoolean("blockbranch.uniformBiomeReuseProbe");
 private static final LongAdder TOTAL=new LongAdder(),COLD=new LongAdder();
 public static void returned(Object source,Object result){if(ON&&source!=result)TOTAL.increment();}
 public static void shared(){if(ON)COLD.increment();}
 public static void write(Path dir)throws Exception{if(ON){if(COLD.sum()<=0||TOTAL.sum()<=COLD.sum())throw new AssertionError("Native workload must exercise shared and last-result paths");Files.writeString(dir.resolve("uniform-biome-reuse.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(java.util.Map.of("compactingReturns",TOTAL.sum(),"sharedCacheEntries",COLD.sum(),"lastResultHits",TOTAL.sum()-COLD.sum()))+"\n");}}
 private UniformBiomeReuseProbe(){}
}
