import jdk.jfr.consumer.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
/** Interval-aligned scalar/class-name summary; no raw thread identities or paths. */
public class AdmissionJfrSummary {
 record Window(int phase,Instant start,Instant end){}
 public static void main(String[] args)throws Exception{
  var events=RecordingFile.readAllEvents(Path.of(args[0]));var windows=new ArrayList<Window>();
  for(var e:events)if(e.getEventType().getName().equals("blockbranch.qa.Admission"))windows.add(new Window(e.getInt("phase"),e.getStartTime(),e.getEndTime()));
  windows.sort(Comparator.comparingInt(Window::phase));
  for(var w:windows){
   var leaves=new HashMap<String,Integer>();var groups=new TreeMap<String,Integer>();var threads=new TreeMap<String,Integer>();
   var waits=new TreeMap<String,long[]>();
   int samples=0,truncated=0,compilations=0,gcs=0;long compileOverlap=0,compileMax=0,pauseSum=0,pauseMax=0;
   for(var e:events){
    String type=e.getEventType().getName();var t=e.getStartTime();
    if(Set.of("jdk.JavaMonitorEnter","jdk.ThreadPark","jdk.ExecuteVMOperation").contains(type)&&e.getEndTime().isAfter(w.start)&&t.isBefore(w.end)){
     var thread=e.getThread();String name=thread==null||thread.getJavaName()==null?"":thread.getJavaName();String role=name.startsWith("Worker-Main")?"worker":name.equals("Server thread")?"server":"other";
     long overlap=Duration.between(t.isAfter(w.start)?t:w.start,e.getEndTime().isBefore(w.end)?e.getEndTime():w.end).toNanos();
     var a=waits.computeIfAbsent(type+":"+role,k->new long[3]);a[0]++;a[1]+=overlap;a[2]=Math.max(a[2],overlap);
    }
    if(type.equals("jdk.Compilation")&&e.getEndTime().isAfter(w.start)&&t.isBefore(w.end)){
     compilations++;long overlap=Duration.between(t.isAfter(w.start)?t:w.start,e.getEndTime().isBefore(w.end)?e.getEndTime():w.end).toNanos();compileOverlap+=overlap;compileMax=Math.max(compileMax,overlap);
    }
    if(t.isBefore(w.start)||!t.isBefore(w.end))continue;
    if(type.equals("jdk.GarbageCollection")){gcs++;pauseSum+=e.getDuration("sumOfPauses").toNanos();pauseMax=Math.max(pauseMax,e.getDuration("longestPause").toNanos());}
    if(!type.equals("jdk.ExecutionSample"))continue;var st=e.getStackTrace();if(st==null)continue;samples++;if(st.isTruncated())truncated++;
    var stack=st.getFrames().stream().map(f->f.getMethod().getType().getName().replace('/','.')+"."+f.getMethod().getName()).toList();leaves.merge(stack.getFirst(),1,Integer::sum);
    for(var category:Map.of("profile","profile","canopy","canopy","oceanWater","oceanWater","terrainShape","TerrainShape.","drainage","DrainageNetwork.").entrySet())
     if(stack.stream().anyMatch(s->s.contains(category.getValue())))groups.merge(category.getKey(),1,Integer::sum);
    if(stack.getFirst().contains("AdmissionWorkTrace")||stack.getFirst().contains("AdmissionOceanTrace"))groups.merge("instrumentationLeaf",1,Integer::sum);
    var thread=e.getThread("sampledThread");String name=thread==null||thread.getJavaName()==null?"":thread.getJavaName();threads.merge(name.startsWith("Worker-Main")?"worker":name.equals("Server thread")?"server":"other",1,Integer::sum);
   }
   System.out.println("phase="+w.phase+" samples="+samples+" truncated="+truncated+" inclusiveOverlapping="+groups+" threads="+threads);
   System.out.println("leaves="+leaves.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(12).toList());
   System.out.println("GCs="+gcs+" pauseSumMs="+pauseSum/1e6+" longestPauseMs="+pauseMax/1e6+" overlappingCompilations="+compilations+" summedCompilationOverlapMs="+compileOverlap/1e6+" maximumCompilationOverlapMs="+compileMax/1e6);
   waits.forEach((k,a)->System.out.println("overlappingWait="+k+" count="+a[0]+" summedMs="+a[1]/1e6+" maxMs="+a[2]/1e6));
  }
 }
}
