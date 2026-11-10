package blockbranch.cubic;
import java.util.*;
/** Canonical window+boundary order; one authoritative occupancy bit per target. */
public final class SectionOccupancy {
 public static final int MAX_BYTES=((ColumnLimits.HEIGHT>>4)+7)>>3;
 private final int[] sections;
 private final byte[] bits;
 private SectionOccupancy(int[] sections,byte[] bits){this.sections=sections;this.bits=bits.clone();}
 public static int[] targets(TerrainProfile base,int min,int max){
  if(min<(ColumnLimits.MIN_Y>>4)||max>=(ColumnLimits.MAX_Y>>4)||min>max||max-min>128)throw new IllegalArgumentException("Invalid occupancy window");
  var set=new TreeSet<>(base.boundaries());for(int y=min;y<=max;y++)set.add(y);
  if(set.size()>(ColumnLimits.HEIGHT>>4))throw new IllegalArgumentException("Too many occupancy sections");
  for(int y:set)if(y<(ColumnLimits.MIN_Y>>4)||y>=(ColumnLimits.MAX_Y>>4))throw new IllegalArgumentException("Occupancy boundary outside build bounds");
  return set.stream().mapToInt(Integer::intValue).toArray();
 }
 public static byte[] create(TerrainProfile base,int min,int max,int[] edited){
  var ys=targets(base,min,max);var pending=new HashSet<Integer>();for(int y:edited)pending.add(y);byte[] bits=new byte[(ys.length+7)>>3];
  for(int i=0;i<ys.length;i++)if(pending.contains(ys[i])||base.sectionIsEmpty(ys[i]))bits[i>>3]|=(byte)(1<<(i&7));
  return bits;
 }
 public static SectionOccupancy decode(TerrainProfile base,int min,int max,int[] edited,byte[] bits){
  var ys=targets(base,min,max);
  if(bits.length!=((ys.length+7)>>3)||bits.length>MAX_BYTES)throw new IllegalArgumentException("Invalid occupancy summary length");
  if((ys.length&7)!=0&&(bits[bits.length-1]&255)>>> (ys.length&7)!=0)throw new IllegalArgumentException("Noncanonical occupancy padding");
  var result=new SectionOccupancy(ys,bits);var pending=new HashSet<Integer>();for(int y:edited)pending.add(y);
  // An edited layer is unavailable until its ordered overlay packet arrives,
  // matching the existing client sentinel. It must never seed the base cache.
  for(int y:ys){if(pending.contains(y)){if(!result.empty(y))throw new IllegalArgumentException("Pending overlay must retain empty sentinel status");}else base.rememberSectionEmpty(y,result.empty(y));}
  return result;
 }
 public boolean empty(int y){int i=Arrays.binarySearch(sections,y);if(i<0)throw new IllegalArgumentException("Section absent from occupancy summary");return (bits[i>>3]&(1<<(i&7)))!=0;}
}
