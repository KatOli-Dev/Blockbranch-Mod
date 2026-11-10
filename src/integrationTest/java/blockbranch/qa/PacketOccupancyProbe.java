package blockbranch.qa;
import java.util.*;
/** Thread-owned QA observer; never retains worlds, chunks or profiles. */
public final class PacketOccupancyProbe {
 public static volatile Thread owner;
 public static long states;
 public static boolean legacy;
 public static int failAt=-1;
 public static long failStateAt=-1;
 public static final class Injected extends RuntimeException {}
 public static final List<String> notifications=new ArrayList<>();
 public static void begin(){states=0;notifications.clear();owner=Thread.currentThread();}
 public static void end(){owner=null;failAt=-1;failStateAt=-1;}
 public static void state(){if(Thread.currentThread()==owner){states++;if(states==failStateAt)throw new Injected();}}
 public static void event(String kind,int y,boolean empty){if(Thread.currentThread()==owner){notifications.add(kind+":"+y+":"+empty);if(notifications.size()==failAt)throw new Injected();}}
}
