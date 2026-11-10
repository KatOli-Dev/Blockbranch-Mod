import jdk.jfr.consumer.*;
import java.nio.file.*;
import java.util.*;
/** Summarize terrain JFR recordings: FILE [START_UTC [END_UTC]], with ISO-8601 instants. */
public class TerrainJfrSummary {
 public static void main(String[] args)throws Exception{
  var from=args.length>1?java.time.Instant.parse(args[1]):java.time.Instant.MIN;
  var until=args.length>2?java.time.Instant.parse(args[2]):java.time.Instant.MAX;
  Map<String,Map<String,Integer>> groups=new TreeMap<>();Map<String,Long> waits=new TreeMap<>();long gc=0,gcMax=0;int gcCount=0;
  try(var f=new RecordingFile(Path.of(args[0]))){while(f.hasMoreEvents()){
   var e=f.readEvent();if(e.getStartTime().isBefore(from)||!e.getStartTime().isBefore(until))continue;String kind=e.getEventType().getName();
   if(kind.equals("jdk.GCPhasePause")){long ns=e.getDuration().toNanos();gc+=ns;gcMax=Math.max(gcMax,ns);gcCount++;}
   if(!Set.of("jdk.ExecutionSample","jdk.NativeMethodSample","jdk.ThreadPark","jdk.JavaMonitorEnter").contains(kind))continue;
   var thread=e.getThread(kind.equals("jdk.ExecutionSample")||kind.equals("jdk.NativeMethodSample")?"sampledThread":"eventThread");
   if(thread==null)continue;String name=thread.getJavaName();var st=e.getStackTrace();if(st==null)continue;
   List<String> stack=st.getFrames().stream().map(fr->fr.getMethod().getType().getName()+"."+fr.getMethod().getName()).toList();
   if(kind.equals("jdk.ThreadPark")||kind.equals("jdk.JavaMonitorEnter")){
    if(name.equals("Server thread")){
     String group=stack.stream().filter(s->s.startsWith("blockbranch.")||s.contains("getChunk")||s.contains("waitUntilNextTick")).findFirst().orElse("other");
     waits.merge(group,e.getDuration().toNanos(),Long::sum);
    }continue;
   }
   if(!name.startsWith("Worker-Main")&&!Set.of("Server thread","Render thread").contains(name))continue;
   String category=stack.stream().filter(s->s.contains("doCreateBiomes")||s.contains("RealisticGenerator.profile")||s.contains("RealisticGenerator.canopy")||s.contains("TerrainShape.<init>")||s.contains("DrainageNetwork.<init>")||s.contains("TerrainProfile.state")).findFirst().orElse("other");
   String key=(name.startsWith("Worker-Main")?"Worker-Main":name)+" "+category;
   String leaf=stack.stream().filter(s->s.startsWith("blockbranch.")).findFirst().orElse(stack.getFirst());
   groups.computeIfAbsent(key,k->new HashMap<>()).merge(leaf,1,Integer::sum);
  }}
  groups.forEach((k,v)->System.out.println(k+" total="+v.values().stream().mapToInt(i->i).sum()+" "+v.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(8).toList()));
  waits.forEach((k,v)->System.out.println("Server wait "+k+" ms="+v/1e6));
  System.out.println("GC count="+gcCount+" totalMs="+gc/1e6+" maxMs="+gcMax/1e6);
 }
}
