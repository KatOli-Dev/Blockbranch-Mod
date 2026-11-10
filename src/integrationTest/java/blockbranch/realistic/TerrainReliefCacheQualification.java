package blockbranch.realistic;
import java.util.*;import java.util.concurrent.atomic.AtomicLong;
/** Exact old/new arithmetic, LRU eviction, zero values, order and concurrent misses. */
public final class TerrainReliefCacheQualification {
 private static DrainageNetwork.Sample sample(int x,int z,long seed,AtomicLong calls){calls.incrementAndGet();return new DrainageNetwork.Sample((int)(800*Math.sin(x*.001)+600*Math.cos(z*.0013)+(OakBirchWoodland.mix(seed^x*31L^z)&15)),DrainageNetwork.DRY,0,true);}
 private static void same(double a,double b){if(Double.doubleToRawLongBits(a)!=Double.doubleToRawLongBits(b))throw new AssertionError("Relief arithmetic changed "+a+" / "+b);}
 public static void main(String[] args)throws Exception{
  long checks=0;
  for(long seed:new long[]{424242,-17}){
   var oldCalls=new AtomicLong();var newCalls=new AtomicLong();var old=new TerrainReliefReference((x,z)->sample(x,z,seed,oldCalls));var candidate=new TerrainRelief((x,z)->sample(x,z,seed,newCalls));var shape=new TerrainShape(seed);
   for(int[] point:new int[][]{{0,0},{-1,-1},{2097100,-2097100},{64,64}}){same(old.offset(point[0],point[1],shape),candidate.offset(point[0],point[1],shape));same(old.bankFraction(point[0],point[1],shape),candidate.bankFraction(point[0],point[1],shape));checks+=2;}
   for(int i=0;i<19000;i++){int x=i*64-600000,z=(i*37)%1400000-700000;same(old.offset(x,z,shape),candidate.offset(x,z,shape));same(old.bankFraction(x,z,shape),candidate.bankFraction(x,z,shape));same(old.strength(x,z),candidate.strength(x,z));same(old.slope(x,z),candidate.slope(x,z));checks+=4;}
   for(int pass=0;pass<4;pass++)for(int i=600;i>=0;i--){int x=i*64-600000,z=(i*37)%1400000-700000;same(old.offset(x,z,shape),candidate.offset(x,z,shape));same(old.bankFraction(x,z,shape),candidate.bankFraction(x,z,shape));same(old.strength(x,z),candidate.strength(x,z));checks+=3;}
   if(oldCalls.get()!=newCalls.get())throw new AssertionError("LRU hit/miss/eviction order changed "+oldCalls+" / "+newCalls);
   java.util.stream.IntStream.range(0,8192).parallel().forEach(i->{int x=(i%1024)*79-40000,z=(i%1024)*31-20000;same(old.offset(x,z,shape),candidate.offset(x,z,shape));same(old.bankFraction(x,z,shape),candidate.bankFraction(x,z,shape));same(old.slope(x,z),candidate.slope(x,z));});checks+=8192*3;
   for(String field:new String[]{"offsets","bankFractions","gradients"}){var f=TerrainRelief.class.getDeclaredField(field);f.setAccessible(true);int size=((Map<?,?>)f.get(candidate)).size();if(size>(field.equals("gradients")?4096:16384))throw new AssertionError("Retained cache bound changed");}
  }
  System.out.println("PASS exact relief/cache checks="+checks+"; old/new bit identity, access-order LRU, eviction and concurrent pure misses");
 }
}
