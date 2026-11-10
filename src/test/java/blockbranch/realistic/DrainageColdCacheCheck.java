package blockbranch.realistic;

import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual public lookups with controlled cold entry and same-slot replacement. No production hooks. */
public final class DrainageColdCacheCheck {
 private record Point(int x,int z) {long key(){return (x&0xffffffffL)|((long)z<<32);}}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 private static Field field(String name)throws Exception{var f=DrainageNetwork.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private static DrainageNetwork.Sample original(DrainageNetwork oracle,Method raw,Point p)throws Exception{
  return oracle.eroded().apply(p.x,p.z,(DrainageNetwork.Sample)raw.invoke(oracle,p.x,p.z,true));
 }
 private static void blocked(Collection<Thread> threads,Object monitor)throws Exception {
  long deadline=System.nanoTime()+5_000_000_000L;var bean=ManagementFactory.getThreadMXBean();
  while(System.nanoTime()<deadline){boolean all=threads.size()==4;
   for(var t:threads){var info=bean.getThreadInfo(t.threadId());var lock=info==null?null:info.getLockInfo();
    all&=info!=null&&info.getThreadState()==Thread.State.BLOCKED&&lock!=null&&lock.getIdentityHashCode()==System.identityHashCode(monitor);
   }
   if(all)return;java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
  }throw new AssertionError("Four actual calls must reach the held production monitor");
 }
 private static void coldFactory(DrainageNetwork oracle,Field view)throws Exception {
  var threads=new ConcurrentLinkedQueue<Thread>();var ready=new CountDownLatch(4);var start=new CountDownLatch(1);
  var jobs=Executors.newFixedThreadPool(4,r->{var t=new Thread(r,"drainage-cold-view");threads.add(t);return t;});
  try{
   var tasks=new ArrayList<Future<DrainageNetwork>>();
   for(int i=0;i<4;i++)tasks.add(jobs.submit(()->{ready.countDown();check(start.await(5,TimeUnit.SECONDS),"View callers released");return oracle.preformation();}));
   check(ready.await(5,TimeUnit.SECONDS),"Four cold view callers ready");
   synchronized(oracle){check(view.get(oracle)==null,"View factory starts cold");start.countDown();blocked(threads,oracle);check(view.get(oracle)==null,"No initialization before releasing real factory monitor");}
   var identity=tasks.getFirst().get(30,TimeUnit.SECONDS);for(var task:tasks)check(task.get(30,TimeUnit.SECONDS)==identity,"Actual cold factory returns the same fully initialized view");
  }finally{start.countDown();jobs.shutdownNow();check(jobs.awaitTermination(5,TimeUnit.SECONDS),"View callers release");}
 }
 public static void main(String[]args)throws Exception{
  var target=new DrainageNetwork(new TerrainShape(424242),424242,true,true);
  var oracle=new DrainageNetwork(new TerrainShape(424242),424242,true,true);
  var raw=DrainageNetwork.class.getDeclaredMethod("sample",int.class,int.class,boolean.class);raw.setAccessible(true);
  var memo=(DrainageNetwork.Sample[])field("sampleCache").get(target);var keys=(long[])field("sampleKeys").get(target);
  var view=field("preformation");var weathering=field("eroded");var cold=new Point(387960,-804006);coldFactory(oracle,view);var expected=original(oracle,raw,cold);
  check(view.get(target)==null&&weathering.get(target)==null,"Target lazy views remain cold after separate oracle computation");
  check(Arrays.stream(memo).allMatch(Objects::isNull),"Target sample cache starts empty");
  var threads=new ConcurrentLinkedQueue<Thread>();var ready=new CountDownLatch(4);var start=new CountDownLatch(1);
  var owners=ConcurrentHashMap.newKeySet();var jobs=Executors.newFixedThreadPool(4,r->{var t=new Thread(r,"drainage-cold-reader");threads.add(t);return t;});
  try{
   var tasks=new ArrayList<Future<DrainageNetwork.Sample>>();
   for(int i=0;i<4;i++)tasks.add(jobs.submit(()->{ready.countDown();check(start.await(5,TimeUnit.SECONDS),"Cold start released");var answer=target.sample(cold.x,cold.z);owners.add(view.get(target));return answer;}));
   check(ready.await(5,TimeUnit.SECONDS),"All dedicated callers are ready");
   synchronized(memo){
    start.countDown();blocked(threads,memo);
    check(view.get(target)==null&&weathering.get(target)==null,"No lazy initialization before cold callers enter");
   }
   for(var task:tasks)check(task.get(30,TimeUnit.SECONDS).equals(expected),"Concurrent cold result matches frozen composition");
   check(owners.size()==1&&owners.contains(view.get(target))&&weathering.get(target)!=null,"Caller completion observes one fully published lazy owner");
  }finally{start.countDown();jobs.shutdownNow();check(jobs.awaitTermination(5,TimeUnit.SECONDS),"Cold callers release");}
  // Duplication of pure computation is legal. We require value parity and one
  // lazy view, not a single Sample allocation or suppression of safe duplicate work.
  var mix=DrainageNetwork.class.getDeclaredMethod("mix",long.class);mix.setAccessible(true);
  var buckets=new HashMap<Integer,List<Point>>();List<Point> collision=null;int slot=-1;
  for(int i=0;i<60_000&&collision==null;i++){
   var p=new Point(i-30_000,i*31-900_000);int at=(int)(long)mix.invoke(null,p.key())&(memo.length-1);
   var bucket=buckets.computeIfAbsent(at,k->new ArrayList<>());bucket.add(p);if(bucket.size()==3){collision=List.copyOf(bucket);slot=at;}
  }
  check(collision!=null,"Three distinct valid coordinates collide under the production hash and capacity");
  var wanted=new HashMap<Long,DrainageNetwork.Sample>();for(var p:collision)wanted.put(p.key(),original(oracle,raw,p));
  for(int pass=0;pass<3;pass++)for(var p:collision){
   var actual=target.sample(p.x,p.z);check(actual.equals(wanted.get(p.key())),"Sequential collision/reload preserves exact value");
   synchronized(memo){check(keys[slot]==p.key()&&memo[slot].equals(wanted.get(p.key())),"Eviction replaces full key and associated value together");}
  }
  final int sharedSlot=slot;var points=collision;var barrier=new CyclicBarrier(3);var writers=Executors.newFixedThreadPool(3);
  try{
   var tasks=new ArrayList<Future<?>>();for(var p:points)tasks.add(writers.submit(()->{
    for(int round=0;round<32;round++){
     barrier.await(10,TimeUnit.SECONDS);var held=target.sample(p.x,p.z);
     check(held.equals(wanted.get(p.key())),"Concurrent colliding lookup matches independent composition");
     barrier.await(10,TimeUnit.SECONDS);
     check(held.equals(wanted.get(p.key())),"Held immutable result survives other-key eviction");
     synchronized(memo){var correct=wanted.get(keys[sharedSlot]);check(correct!=null&&correct.equals(memo[sharedSlot]),"Concurrent cache publication never pairs a key with another value");}
    }return null;
   }));for(var task:tasks)task.get(60,TimeUnit.SECONDS);
  }finally{writers.shutdownNow();check(writers.awaitTermination(5,TimeUnit.SECONDS),"Collision workers release");}
  System.out.println("PASS cold view factory callers=4 returnedIdentity=1; cold sample callers=4 allEnteredPublicLookup=true; productionHashCollisions=3 sequentialEvictions=9 concurrentRounds=32 callers=3");
 }
}
