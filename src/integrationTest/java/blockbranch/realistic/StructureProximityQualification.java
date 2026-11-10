package blockbranch.realistic;
import java.util.*;
/** Exact distance oracle for signed bucket packing and strict exclusion thresholds. */
public final class StructureProximityQualification {
 public static void main(String[] args){
  long checks=0;
  for(int radius:new int[]{48,128}){
   var index=new StructureProximity(radius);var points=new ArrayList<int[]>();
   for(int x:new int[]{-2097100,-129,-128,-49,-48,-1,0,1,47,48,127,128,2097100})for(int z:new int[]{-129,-48,-1,0,48,128}){index.add(x,z);points.add(new int[]{x,z});}
   for(var p:points)for(int dx:new int[]{-radius-1,-radius,-radius+1,-1,0,1,radius-1,radius,radius+1})for(int dz:new int[]{-radius,0,radius}){
    int x=p[0]+dx,z=p[1]+dz;boolean expected=points.stream().anyMatch(q->Math.hypot(x-(double)q[0],z-(double)q[1])<radius);
    if(index.near(x,z)!=expected)throw new AssertionError("Exact radius/bucket mismatch at "+x+","+z);checks++;
   }
  }
  System.out.println("PASS exact signed proximity checks="+checks);
 }
}
