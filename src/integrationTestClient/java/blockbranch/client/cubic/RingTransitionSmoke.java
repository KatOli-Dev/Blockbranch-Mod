package blockbranch.client.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
/** Both arms invoke actual mixed-in storage; oracle disables only the new fast path. */
public final class RingTransitionSmoke {
 static final class V implements RotatingSectionStorage.Value {
  final int id;final Trace trace;long node;V(int id,long node,Trace trace){this.id=id;this.node=node;this.trace=trace;}
  public long getSectionNode(){trace.reads++;return node;}
  public void setSectionNode(long n){if(trace.owner!=null)check(trace.owner.centerSectionPos().equals(trace.before),"callback observes prior center");if(trace.failAt==trace.count)throw new IllegalStateException("controlled callback failure");trace.ids[trace.count]=id;trace.nodes[trace.count++]=n;node=n;}
 }
 static final class Trace {RotatingSectionStorage<V> owner;SectionPos before;int count,failAt=-1;long reads;final int[] ids;final long[] nodes;Trace(int size){ids=new int[size];nodes=new long[size];}void reset(){if(owner!=null)before=owner.centerSectionPos();count=0;reads=0;failAt=-1;}}
 static java.lang.reflect.Field ready;
 static void check(boolean x,String m){if(!x)throw new AssertionError(m);}
 static boolean nativeMove(RotatingSectionStorage<V> s,SectionPos p)throws Exception{ready.setBoolean(s,false);return s.repositionCenter(p);}
 public static void register(){
  try{
   ready=RotatingSectionStorage.class.getDeclaredField("blockbranch$transitionReady");ready.setAccessible(true);
   for(int r:new int[]{2,8,16,32})verify(r);
   verifyNormal();
   measure();
   Files.writeString(Path.of(System.getProperty("blockbranch.ringResult")),"PASS actual native oracle: ordered callbacks, identities, bounds, fallback, failure/retry; fixed work\n");
  }catch(Throwable t){throw new RuntimeException(t);}
  ClientTickEvents.END_CLIENT_TICK.register(c->c.stop());
 }
 static void verify(int r)throws Exception{
  int size=(2*r+1)*(2*r+1)*(2*r+3);var a=new Trace(size);var b=new Trace(size);
  var fast=new RotatingSectionStorage<V>(r,ColumnLimits.MIN_Y>>4,(ColumnLimits.MAX_Y>>4)-1,(i,n)->new V(i,n,a));
  var nativeStorage=new RotatingSectionStorage<V>(r,ColumnLimits.MIN_Y>>4,(ColumnLimits.MAX_Y>>4)-1,(i,n)->new V(i,n,b));
  a.owner=fast;b.owner=nativeStorage;
  List<SectionPos> route=new ArrayList<>();route.add(SectionPos.of(-10,10,-10));
  for(int i=0;i<10;i++)route.add(SectionPos.of(-9+i,10,-10+i%3));
  route.add(SectionPos.of(r,r,0));route.add(SectionPos.of(2*r,r,r));
  int edge=ColumnLimits.BORDER_LIMIT>>4;
  route.add(SectionPos.of(edge,10,-edge));route.add(SectionPos.of(edge-1,10,-edge+1));route.add(SectionPos.of(-edge,10,edge));route.add(SectionPos.of(-edge+1,10,edge-1));
  route.add(route.getLast());route.add(SectionPos.of(0,11,0));route.add(SectionPos.of(1,11,0));route.add(SectionPos.of(1000,-100,1000));
  for(int y:new int[]{ColumnLimits.MIN_Y>>4,(ColumnLimits.MAX_Y>>4)-1}){route.add(SectionPos.of(-1000,y,1000));route.add(SectionPos.of(-999,y,999));}
  for(var p:route){a.reset();b.reset();check(fast.repositionCenter(p)==nativeMove(nativeStorage,p),"return value");compare(fast,nativeStorage,a,b,r,p);}
  // Failure after a partial entering plane, then retry the identical transition.
  var old=fast.centerSectionPos();var p=SectionPos.of(old.x()+1,old.y(),old.z());a.reset();b.reset();a.failAt=b.failAt=7;
  try{fast.repositionCenter(p);throw new AssertionError("missing fast failure");}catch(IllegalStateException expected){}
  try{nativeMove(nativeStorage,p);throw new AssertionError("missing native failure");}catch(IllegalStateException expected){}
  a.reset();b.reset();check(!fast.repositionCenter(old)&&!nativeMove(nativeStorage,old),"unchanged center after interrupted placement");
  check(!ready.getBoolean(fast),"no-op cannot certify an interrupted placement");
  a.reset();b.reset();check(fast.repositionCenter(p)==nativeMove(nativeStorage,p),"return value");compare(fast,nativeStorage,a,b,r,p);
  blockbranch.Blockbranch.LOGGER.info("RING_ORACLE_PASS radius={} transitions={} exceptionRetry=true",r,route.size()+1);
 }
 static void compare(RotatingSectionStorage<V> a,RotatingSectionStorage<V> b,Trace x,Trace y,int r,SectionPos p){
  check(x.count==y.count,"callback count");for(int i=0;i<x.count;i++)check(x.ids[i]==y.ids[i]&&x.nodes[i]==y.nodes[i],"callback identity/order");
  check(a.centerSectionPos().equals(b.centerSectionPos())&&a.minY()==b.minY()&&a.maxY()==b.maxY(),"center and bounds");
  for(int xx=p.x()-r;xx<=p.x()+r;xx++)for(int zz=p.z()-r;zz<=p.z()+r;zz++)for(int yy=a.minY();yy<=a.maxY();yy++){
   V v=a.getValue(xx,yy,zz),w=b.getValue(xx,yy,zz);check(v.id==w.id&&v.node==w.node&&v.node==SectionPos.asLong(xx,yy,zz),"absolute identity/content");
  }
  check(a.getValue(p.x(),a.minY()-1,p.z())==null&&a.getValue(p.x(),a.maxY()+1,p.z())==null,"Y exclusion");
  check(a.getValue(p.x()+r+1,p.y(),p.z())==null,"X exclusion");
 }
 static void verifyNormal()throws Exception {
  var a=new Trace(5*5*16);var b=new Trace(5*5*16);
  var x=new RotatingSectionStorage<V>(2,0,15,(i,n)->new V(i,n,a));var y=new RotatingSectionStorage<V>(2,0,15,(i,n)->new V(i,n,b));
  for(var p:List.of(SectionPos.of(0,5,0),SectionPos.of(1,5,0),SectionPos.of(1,6,0),SectionPos.of(-8,6,8))){a.reset();b.reset();x.repositionCenter(p);nativeMove(y,p);compare(x,y,a,b,2,p);}
 }
 static void measure()throws Exception{
  int r=32,size=65*65*67;var a=new Trace(size);var b=new Trace(size);
  var fast=new RotatingSectionStorage<V>(r,ColumnLimits.MIN_Y>>4,(ColumnLimits.MAX_Y>>4)-1,(i,n)->new V(i,n,a));
  var oracle=new RotatingSectionStorage<V>(r,ColumnLimits.MIN_Y>>4,(ColumnLimits.MAX_Y>>4)-1,(i,n)->new V(i,n,b));
  fast.repositionCenter(SectionPos.of(0,10,0));nativeMove(oracle,SectionPos.of(0,10,0));
  var cpu=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long tid=Thread.currentThread().threadId();
  for(int warm=1;warm<=20;warm++){a.reset();b.reset();fast.repositionCenter(SectionPos.of(warm,10,0));nativeMove(oracle,SectionPos.of(warm,10,0));}
  for(int arm=0;arm<4;arm++){
   var storage=arm%2==0?oracle:fast;var trace=arm%2==0?b:a;int start=storage.centerSectionPos().x();long elapsed=System.nanoTime(),time=cpu.getCurrentThreadCpuTime(),bytes=cpu.getThreadAllocatedBytes(tid),reads=0;
   for(int n=1;n<=80;n++){trace.reset();var p=SectionPos.of(start+n,10,0);if(arm%2==0)nativeMove(storage,p);else storage.repositionCenter(p);reads+=trace.reads;check(trace.count==65*67,"one entering plane");}
   blockbranch.Blockbranch.LOGGER.info("RING_FIXED_WORK arm={} transitions=80 reads={} cpuNs={} wallNs={} bytes={}",arm,reads,cpu.getCurrentThreadCpuTime()-time,System.nanoTime()-elapsed,cpu.getThreadAllocatedBytes(tid)-bytes);
   check(reads==(arm%2==0?80L*size:80L*65*67),"work bound");
  }
 }
}
