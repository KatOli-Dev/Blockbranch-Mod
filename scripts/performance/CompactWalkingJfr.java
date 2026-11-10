import java.nio.file.*;import java.util.*;import java.io.*;import jdk.jfr.consumer.*;
class CompactWalkingJfr {
 static String q(String s){if(s==null)return "null";return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
 public static void main(String[] args)throws Exception{
  Set<String> types=Set.of("jdk.ObjectAllocationSample","jdk.ExecutionSample","jdk.NativeMethodSample","jdk.JavaMonitorEnter","jdk.JavaMonitorWait","jdk.ThreadPark","jdk.ThreadSleep","jdk.GCPhasePause","jdk.GarbageCollection","jdk.CPULoad","jdk.ThreadCPULoad","blockbranch.SnapshotHold","blockbranch.IntegratedSlowFrame");
  Map<List<String>,Integer> ids=new LinkedHashMap<>();Map<Long,String> names=new TreeMap<>();
  try(var in=new RecordingFile(Path.of(args[0]));var out=new PrintWriter(args[1])){
   out.print("{\"events\":[");boolean comma=false;
   while(in.hasMoreEvents()){
    var e=in.readEvent();String type=e.getEventType().getName();if(!types.contains(type))continue;
    String threadField=type.endsWith("ExecutionSample")||type.endsWith("NativeMethodSample")?"sampledThread":"eventThread";var thread=e.hasField(threadField)?e.getThread(threadField):null;long tid=thread==null?0:thread.getJavaThreadId();if(thread!=null)names.put(tid,thread.getJavaName());
    var st=e.getStackTrace();var frames=st==null?List.<String>of():st.getFrames().stream().map(f->f.getMethod().getType().getName()+"."+f.getMethod().getName()+":"+f.getLineNumber()).toList();int id=ids.computeIfAbsent(frames,k->ids.size());
    if(comma)out.print(',');comma=true;
    out.print("{\"type\":"+q(type)+",\"ms\":"+(e.getStartTime().getEpochSecond()*1000d+e.getStartTime().getNano()/1e6)+",\"durationNs\":"+e.getDuration().toNanos()+",\"thread\":"+tid+",\"stack\":"+id);
    if(thread!=null)out.print(",\"osThread\":"+thread.getOSThreadId());
    if(type.equals("jdk.ObjectAllocationSample"))out.print(",\"weight\":"+e.getLong("weight")+",\"objectClass\":"+q(e.getClass("objectClass").getName()));
    if(type.equals("jdk.JavaMonitorEnter")){var owner=e.getThread("previousOwner");out.print(",\"monitorClass\":"+q(e.getClass("monitorClass").getName())+",\"previousOwner\":"+(owner==null?0:owner.getJavaThreadId()));}
    if(type.equals("blockbranch.SnapshotHold")){for(String k:List.of("kind","phase"))out.print(","+q(k)+":"+q(e.getString(k)));for(String k:List.of("methodNs","preAcquireNs","approximateHoldNs","holdCpuNs"))out.print(","+q(k)+":"+e.getLong(k));}
    if(type.equals("jdk.ThreadCPULoad")){out.print(",\"user\":"+e.getFloat("user")+",\"system\":"+e.getFloat("system"));}
    if(type.equals("jdk.CPULoad")){out.print(",\"jvmUser\":"+e.getFloat("jvmUser")+",\"jvmSystem\":"+e.getFloat("jvmSystem")+",\"machineTotal\":"+e.getFloat("machineTotal"));}
    out.print('}');
   }
   out.print("],\"stacks\":[");comma=false;for(var s:ids.keySet()){if(comma)out.print(',');comma=true;out.print('[');for(int i=0;i<s.size();i++){if(i>0)out.print(',');out.print(q(s.get(i)));}out.print(']');}
   out.print("],\"threads\":{");comma=false;for(var e:names.entrySet()){if(comma)out.print(',');comma=true;out.print(q(e.getKey().toString())+":"+q(e.getValue()));}out.println("}}");
  }
 }
}
