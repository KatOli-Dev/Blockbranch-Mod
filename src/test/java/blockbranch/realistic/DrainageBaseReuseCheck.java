package blockbranch.realistic;
import java.util.*;
import java.util.concurrent.*;
/** Frozen pre-reuse miss composition, exact owner parity and immutable concurrent consumers. */
public final class DrainageBaseReuseCheck {
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  var raw=DrainageNetwork.class.getDeclaredMethod("sample",int.class,int.class,boolean.class);raw.setAccessible(true);
  int checked=0,weathered=0;
  for(long seed:new long[]{424242,1,-17})for(int mode=0;mode<(seed==424242?3:1);mode++){
   var d=new DrainageNetwork(new TerrainShape(seed),seed,mode!=2,mode==0);var expected=new LinkedHashMap<Long,DrainageNetwork.Sample>();
   var points=new ArrayList<int[]>();
   for(int[]center:new int[][]{{0,0},{228504,-267736},{-1318712,-1697680},{-701960,270984},{387960,-804006},{-2097104,2097103}})
    for(int z=-16;z<=16;z+=8)for(int x=-16;x<=16;x+=8)points.add(new int[]{center[0]+x,center[1]+z});
   var random=new Random(seed);for(int i=0;i<128;i++)points.add(new int[]{random.nextInt(4_000_000)-2_000_000,random.nextInt(4_000_000)-2_000_000});
   int rivers=0;for(int i=0;i<d.size()&&rivers<8;i++)if(d.river(i)){points.add(new int[]{(int)Math.round(d.edgeX(i,.5)),(int)Math.round(d.edgeZ(i,.5))});rivers++;}
   for(var p:points){
    var original=(DrainageNetwork.Sample)raw.invoke(d,p[0],p[1],true);
    var pre=(DrainageNetwork.Sample)raw.invoke(d.preformation(),p[0],p[1],true);
    check(original.equals(pre),"Same exact base field for source/preformation seed="+seed+" mode="+mode);
    var old=d.eroded().apply(p[0],p[1],original);var actual=d.sample(p[0],p[1]);
    check(old.equals(actual),"Frozen old miss composition preserves every Sample field");
    check(d.preformation().sample(p[0],p[1]).equals(pre),"Non-weathered owner stays a base view");
    if(!original.equals(actual))weathered++;
    expected.put((p[0]&0xffffffffL)|((long)p[1]<<32),old);checked++;
   }
   var cache=DrainageNetwork.class.getDeclaredField("sampleCache");cache.setAccessible(true);Arrays.fill((Object[])cache.get(d),null);Arrays.fill((Object[])cache.get(d.preformation()),null);
   var start=new CountDownLatch(1);var ready=new CountDownLatch(4);var jobs=Executors.newFixedThreadPool(4);
   try{
    var tasks=new ArrayList<Future<?>>();for(int worker=0;worker<4;worker++)tasks.add(jobs.submit(()->{
     ready.countDown();try{start.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
     for(int round=0;round<3;round++)for(var e:expected.entrySet()){long key=e.getKey();check(d.sample((int)key,(int)(key>>32)).equals(e.getValue()),"Concurrent immutable sample consumer");}
    }));check(ready.await(5,TimeUnit.SECONDS),"Cold consumers ready");start.countDown();for(var job:tasks)job.get(30,TimeUnit.SECONDS);
   }finally{jobs.shutdownNow();check(jobs.awaitTermination(5,TimeUnit.SECONDS),"Consumer jobs release");}
  }
  String priorComparison=System.getProperty("blockbranch.terrainStructuralComparison"),priorFixture=System.getProperty("blockbranch.realisticClientFixture");
  try{
   System.setProperty("blockbranch.terrainStructuralComparison","true");System.setProperty("blockbranch.realisticClientFixture","true");
   var control=new DrainageNetwork(new TerrainShape(424242),424242,true,true);
   for(int i=-8;i<=8;i++){int x=228504+i*8,z=-267736-i*4;check(control.sample(x,z).equals(raw.invoke(control,x,z,false)),"QA structural comparison remains the raw negative control");checked++;}
  }finally{
   if(priorComparison==null)System.clearProperty("blockbranch.terrainStructuralComparison");else System.setProperty("blockbranch.terrainStructuralComparison",priorComparison);
   if(priorFixture==null)System.clearProperty("blockbranch.realisticClientFixture");else System.setProperty("blockbranch.realisticClientFixture",priorFixture);
  }
  check(weathered>0,"Actual weathered interior, not only unchanged coast");
  System.out.println("PASS exact old drainage composition cases="+checked+" weathered="+weathered+" three seeds, legacy coastal flags, four concurrent consumers");
 }
}
