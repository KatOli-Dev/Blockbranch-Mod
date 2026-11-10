package blockbranch.realistic;
import it.unimi.dsi.fastutil.longs.*;
/** Temporary exact exclusion index; no world, block or generator references. */
final class StructureProximity {
 private final int radius;
 private final Long2ObjectOpenHashMap<LongArrayList> cells=new Long2ObjectOpenHashMap<>();
 StructureProximity(int radius){this.radius=radius;}
 private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
 void add(int x,int z){long k=key(Math.floorDiv(x,radius),Math.floorDiv(z,radius));var a=cells.get(k);if(a==null){a=new LongArrayList(1);cells.put(k,a);}a.add(key(x,z));}
 boolean near(int x,int z){int bx=Math.floorDiv(x,radius),bz=Math.floorDiv(z,radius);for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){var a=cells.get(key(bx+dx,bz+dz));if(a==null)continue;for(int i=0;i<a.size();i++){long p=a.getLong(i);if(Math.hypot(x-(double)(int)(p>>32),z-(double)(int)p)<radius)return true;}}return false;}
}
