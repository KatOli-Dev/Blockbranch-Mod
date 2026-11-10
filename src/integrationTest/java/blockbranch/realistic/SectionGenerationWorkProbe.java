package blockbranch.realistic;
/** QA-only work/failure seam, inactive outside the focused native regression. */
public final class SectionGenerationWorkProbe {
 private static final boolean ENABLED="section_generation_work".equals(System.getProperty("blockbranch.focusHabitat"));
 private static volatile Thread owner;private static long calls,failAt=-1;private static long palettes;
 public static final class InjectedFailure extends RuntimeException {InjectedFailure(){super("QA state failure");}}
 public static int count(int x){if(ENABLED&&Thread.currentThread()==owner){calls++;if(calls==failAt)throw new InjectedFailure();}return x;}
 public static void begin(){begin(-1);}
 public static void palette(){if(ENABLED&&Thread.currentThread()==owner)palettes++;}
 public static long palettes(){return palettes;}
 public static void begin(long fail){calls=0;palettes=0;failAt=fail;owner=Thread.currentThread();}
 public static long end(){owner=null;failAt=-1;return calls;}
}
