package blockbranch.realistic;
import java.util.*;
public final class IcebergFieldCheck {
 public static void main(String[]args){
  for(long seed:new long[]{424242,1,-17}){
   var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var f=new IcebergField(seed,d,c);var b=f.fixture();
   int count=0,visible=0;long hash=1;
   for(int z=b.z()-144;z<=b.z()+144;z++)for(int x=b.x()-144;x<=b.x()+144;x++){
    var layer=f.column(x>>4,z>>4);int low=layer.bottom(x,z),high=layer.top(x,z);if(low==Integer.MIN_VALUE)continue;
    require(IcebergField.habitat(seed,x,z,d,c),"Marine cold province owns all solid ice");require(low>d.sample(x,z).height()+8,"Keel clear of physical seabed");
    count++;if(high>=64)visible++;hash=31*hash+low;hash=31*hash+high;
   }
   var reverse=new IcebergField(seed,d,c);long again=1;
   // Store request-order samples independently from the geometry's anchor cache.
   var points=new ArrayList<int[]>();for(int z=b.z()-144;z<=b.z()+144;z++)for(int x=b.x()-144;x<=b.x()+144;x++)points.add(new int[]{x,z});
   Collections.reverse(points);for(var p:points){var a=f.column(p[0]>>4,p[1]>>4);var q=reverse.column(p[0]>>4,p[1]>>4);require(a.bottom(p[0],p[1])==q.bottom(p[0],p[1])&&a.top(p[0],p[1])==q.top(p[0],p[1]),"Seams and reverse request order agree");}
   require(count>500&&visible>100,"Actual partly submerged marine body");
   System.out.println("ICEBERG_FIELD seed="+seed+" center="+b.x()+","+b.z()+" type="+b.shape().type+" footprint="+count+" visible="+visible+" freeboard="+b.shape().freeboard+" draft="+b.shape().draft+" hash="+hash);
  }
  System.out.println("ICEBERG_MARINE_KEEL_SEAMS_ORDER_PASS");
 }
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
