package blockbranch.realistic;

/** Check raw height bits against the retained field before zero-weight work removal. */
public final class CoastalShapeQualification {
 public static void main(String[] args) {
  long count=0;var random=new java.util.Random(918273L);
  for(long seed:new long[]{424242,1,-17}){
   var before=new TerrainShapeReference(seed);var after=new TerrainShape(seed);
   for(int i=0;i<65536;i++){
    double x,z;
    if(i<32768){x=-504768+random.nextDouble(-512,512);z=-1657792+random.nextDouble(-512,512);}
    else{x=random.nextDouble(-2097100,2097100);z=random.nextDouble(-2097100,2097100);}
    require(before,after,x,z);count++;
   }
   // Search exact original blend cutoffs, then compare neighboring representable coordinates.
   for(double target:new double[]{-.03,-.11,0})for(double z:new double[]{-1657792,0,1000000}){
    double left=-2097100,lv=before.continentality(left,z)-target;
    for(double right=left+16384;right<=2097100;right+=16384){
     double rv=before.continentality(right,z)-target;
     if((lv<0)!=(rv<0)){
      double lo=left,hi=right;for(int n=0;n<52;n++){double mid=(lo+hi)/2;if((before.continentality(mid,z)-target<0)==(lv<0))lo=mid;else hi=mid;}
      for(double x:new double[]{Math.nextDown(lo),lo,hi,Math.nextUp(hi)}){require(before,after,x,z);count++;}
      break;
     }
     left=right;lv=rv;
    }
   }
  }
  System.out.println("PASS raw-bit field compatibility samples="+count);
 }
 private static void require(TerrainShapeReference before,TerrainShape after,double x,double z){
  double a=before.continuousHeight(x,z),b=after.continuousHeight(x,z);
  if(Double.doubleToRawLongBits(a)!=Double.doubleToRawLongBits(b))throw new AssertionError("Field changed at "+x+","+z+": "+a+" != "+b);
 }
}
