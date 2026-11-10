package blockbranch.realistic;
import java.util.concurrent.atomic.AtomicInteger;
public final class SurfaceReservationQualification {
 static boolean value(int x,int z){return ((x*7919L+z*13L)&7)<3;}
 static void check(boolean yes){if(!yes)throw new AssertionError("Exact reservation cache");}
 public static void main(String[]args)throws Exception{
  var calls=new AtomicInteger();var cache=new SurfaceReservationCache((x,z)->{calls.incrementAndGet();return value(x,z);});
  for(int[]p:new int[][]{{0,0},{-1,-1},{Integer.MIN_VALUE,Integer.MAX_VALUE},{Integer.MAX_VALUE,Integer.MIN_VALUE},{1,0}}){check(cache.reserved(p[0],p[1])==value(p[0],p[1]));int before=calls.get();check(cache.reserved(p[0],p[1])==value(p[0],p[1]));check(calls.get()==before);}
  for(int i=0;i<50000;i++)check(cache.reserved(i-25000,i*31)==value(i-25000,i*31));
  java.util.stream.IntStream.range(0,50000).parallel().forEach(i->check(cache.reserved(i-25000,i*31)==value(i-25000,i*31)));
  var other=new SurfaceReservationCache((x,z)->!value(x,z));for(int i=0;i<5000;i++)check(other.reserved(i,i)==!cache.reserved(i,i));
  var d=new DrainageNetwork(new TerrainShape(424242),424242,true,true);
  int checked=0;for(int[]c:new int[][]{{228504,-267736},{0,0},{-701960,270984}})for(int z=-12;z<12;z++)for(int x=-12;x<12;x++){
   int wx=c[0]+x,wz=c[1]+z;var sample=d.sample(wx,wz);boolean expected=sample.bedrockCoast()||sample.waterLevel()!=DrainageNetwork.DRY||d.channelAt(wx,wz).edge()>=0;
   int elevation=sample.height()+1-TerrainShape.SEA_LEVEL;
   if(!expected)expected=elevation>=2200&&elevation<=4200&&AlpineScree.surfaceContains(wx,wz,d::sample);
   if(!expected)expected=elevation>=-8&&elevation<=4&&LowGradientSandyShore.surfaceContains(wx,wz,(a,b)->LowGradientSandyShore.site(d,a,b));
   check(expected==LandVegetation.surfaceReserved(d,wx,wz));checked++;
  }
  System.out.println("PASS reservation cache: 105000 collision/concurrent/isolation probes, true/false/zero/extreme keys; "+checked+" frozen physical exclusion probes");
 }
}
