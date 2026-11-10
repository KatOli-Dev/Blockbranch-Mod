package blockbranch.realistic;

import java.lang.management.ManagementFactory;
import java.util.*;

/** QA-only bounded counts. Never retains a world, chunk, generator or profile. */
public final class AdmissionWorkTrace {
    public static final boolean ON = Boolean.getBoolean("blockbranch.admissionWorkTrace");
    private static final java.lang.management.ThreadMXBean CPU = ManagementFactory.getThreadMXBean();
    private static final Map<Key, long[]> rows = new HashMap<>();
    private static final WeakHashMap<net.minecraft.world.level.chunk.ChunkAccess,Integer> owners = new WeakHashMap<>();
    private static int nextOwner;
    private static volatile long seed;
    private static volatile int phase;
    private static final List<Object> intervals = new ArrayList<>();
    @jdk.jfr.Name("blockbranch.qa.Admission") @jdk.jfr.StackTrace(false)
    public static final class Interval extends jdk.jfr.Event { public int phase; }
    public static Map<String,Long> quota() {
        var result = new TreeMap<String,Long>();
        try { for (var line : java.nio.file.Files.readAllLines(java.nio.file.Path.of("/sys/fs/cgroup/cpu.stat"))) {
            var parts=line.split(" "); result.put(parts[0],Long.parseLong(parts[1]));
        }} catch (java.io.IOException ignored) {}
        return result;
    }
    public static synchronized void interval(Map<String,Long> before, Map<String,Long> after) {
        intervals.add(Map.of("phase",phase,"quotaBefore",before,"quotaAfter",after));
    }
    public static synchronized List<Object> intervals() { return List.copyOf(intervals); }
    private record Key(int phase, String kind, int x, int z, int owner) {}
    public record Scope(int phase, String kind, int x, int z, int owner, long wall, long cpu) {}
    public static void seed(long value) { if(ON)seed=value; }
    private static synchronized int owner(net.minecraft.world.level.chunk.ChunkAccess c) {
        try { if(c.getClass().getMethod("equals",Object.class).getDeclaringClass()!=Object.class
                ||c.getClass().getMethod("hashCode").getDeclaringClass()!=Object.class)
            throw new AssertionError("Weak owner keys must use identity equality");
        } catch (NoSuchMethodException e) { throw new AssertionError(e); }
        return owners.computeIfAbsent(c,ignored->{if(++nextOwner>65536)throw new AssertionError("Owner limit");return nextOwner;});
    }
    public static Scope beginProfile(net.minecraft.world.level.chunk.ChunkAccess chunk,long actualSeed) {
        if(!ON)return null;
        if(actualSeed!=seed)throw new AssertionError("Unexpected second seed in fixed admission audit");
        return new Scope(phase,"profile",chunk.getPos().x(),chunk.getPos().z(),owner(chunk),System.nanoTime(),CPU.getCurrentThreadCpuTime());
    }
    public static void phase(int value) { phase = value; }
    public static int currentPhase() { return phase; }
    public static Scope begin(String kind, int x, int z) {
        return ON ? new Scope(phase, kind, x, z, 0, System.nanoTime(), CPU.getCurrentThreadCpuTime()) : null;
    }
    public static void end(Scope s, boolean success) {
        if (s == null) return;
        long wall = System.nanoTime() - s.wall, cpu = CPU.getCurrentThreadCpuTime() - s.cpu;
        add(s.phase, s.kind, s.x, s.z, s.owner, wall, cpu, success);
    }
    public static void count(String kind, int x, int z) {
        if (ON) add(phase, kind, x, z, 0, 0, 0, true);
    }
    private static synchronized void add(int phase, String kind, int x, int z, int owner, long wall, long cpu, boolean success) {
        var key = new Key(phase, kind, x, z, owner);
        if (rows.size() >= 65536 && !rows.containsKey(key)) throw new AssertionError("Admission trace key limit");
        var a = rows.computeIfAbsent(key, ignored -> new long[5]);
        a[0]++; a[1] += wall; a[2] += cpu; if (!success) a[3]++; a[4] = Math.max(a[4], wall);
    }
    public static void cache(String kind, Object key) {
        if (!ON) return;
        if (key instanceof net.minecraft.world.level.chunk.ChunkAccess c) add(phase,kind,c.getPos().x(),c.getPos().z(),owner(c),0,0,true);
        else if (key instanceof Long l) count(kind, l.intValue(), (int)(l >> 32));
        else throw new AssertionError("Unexpected cache key");
    }
    public static synchronized List<Object> snapshot() {
        var result = new ArrayList<Object>();
        rows.entrySet().stream().sorted(Comparator.comparing((Map.Entry<Key,long[]> e)->e.getKey().phase)
            .thenComparing(e->e.getKey().kind).thenComparing(e->e.getKey().x).thenComparing(e->e.getKey().z).thenComparing(e->e.getKey().owner))
            .forEach(e->{var k=e.getKey();var a=e.getValue();var row=new TreeMap<String,Object>();
                row.put("seed",seed);row.put("phase",k.phase);row.put("kind",k.kind);row.put("x",k.x);row.put("z",k.z);row.put("owner",k.owner);
                row.put("calls",a[0]);row.put("wallNanos",a[1]);row.put("cpuNanos",a[2]);row.put("failures",a[3]);row.put("maxWallNanos",a[4]);result.add(row);});
        return result;
    }
    private AdmissionWorkTrace() {}
}
