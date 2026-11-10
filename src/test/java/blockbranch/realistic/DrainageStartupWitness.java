package blockbranch.realistic;
/** Opt-in one-build-per-seed graph timing and full immutable-array signatures. */
import java.nio.file.*;import java.security.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
public final class DrainageStartupWitness {
 public static void main(String[]args)throws Exception{
  var shape=Class.forName("blockbranch.realistic.TerrainShape");var drainage=Class.forName("blockbranch.realistic.DrainageNetwork");var ctor=drainage.getDeclaredConstructor(shape,long.class,boolean.class,boolean.class);ctor.setAccessible(true);
  var cpu=java.lang.management.ManagementFactory.getThreadMXBean();
  for(long seed:Arrays.stream(args).mapToLong(Long::parseLong).toArray()){
   long wall=System.nanoTime(),before=cpu.isCurrentThreadCpuTimeSupported()?cpu.getCurrentThreadCpuTime():-1;var s=shape.getConstructor(long.class).newInstance(seed);var d=ctor.newInstance(s,seed,true,true);long time=System.nanoTime()-wall,used=before<0?-1:cpu.getCurrentThreadCpuTime()-before;
   var digest=MessageDigest.getInstance("SHA-256");var out=new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(),digest));
   for(String name:new String[]{"ground","parent","root","area","water","ocean","tangentX","tangentZ"}){var f=drainage.getDeclaredField(name);f.setAccessible(true);Object values=f.get(d);out.writeUTF(name);if(values instanceof int[]v)for(int n:v)out.writeInt(n);else if(values instanceof short[]v)for(short n:v)out.writeShort(n);else if(values instanceof boolean[]v)for(boolean n:v)out.writeBoolean(n);else if(values instanceof float[]v)for(float n:v)out.writeInt(Float.floatToRawIntBits(n));else throw new AssertionError(values);}
   String graph=HexFormat.of().formatHex(digest.digest());
   var random=new Random(seed);var height=shape.getMethod("height",int.class,int.class);var continuous=shape.getDeclaredMethod("continuousHeight",double.class,double.class);continuous.setAccessible(true);
   for(int i=0;i<10000;i++){int x=random.nextInt(4194304)-2097152,z=random.nextInt(4194304)-2097152;out.writeInt((int)height.invoke(s,x,z));out.writeLong(Double.doubleToRawLongBits((double)continuous.invoke(s,x+.25,z-.5)));}
   System.out.println("seed="+seed+" graphWallNs="+time+" graphCpuNs="+used+" graph="+graph+" shape="+HexFormat.of().formatHex(digest.digest()));
  }
 }
}
