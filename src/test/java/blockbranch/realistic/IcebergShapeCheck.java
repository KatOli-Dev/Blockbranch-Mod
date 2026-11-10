package blockbranch.realistic;
import blockbranch.cubic.FloatingIceLayer;
import java.nio.file.*;
import java.util.*;

public final class IcebergShapeCheck {
 public static void main(String[]args)throws Exception{
  boolean exceedsSparse=false;var signatures=new HashSet<Integer>();var types=new HashSet<Integer>();
  Path out=Path.of("build/iceberg-diagnostics");Files.createDirectories(out);
  for(int n=0;n<64;n++){
   var b=new IcebergShape(424242L+n);types.add(b.type);double wet=b.submerged/(double)b.volume;
   require(wet>.875&&wet<.925,"Glacial ice submerged volume");require(b.draft>0&&b.freeboard>0,"Both keel and visible ice");
   int[] chunkVolumes=new int[289];int hash=1,volume=0;var rows=new StringBuilder("x,z,bottom,top\n");
   for(int z=-128;z<=128;z++)for(int x=-128;x<=128;x++){
    int low=b.bottom(x,z),high=b.top(x,z);if(low==Integer.MIN_VALUE)continue;
    int packed=FloatingIceLayer.pack(64+low,64+high,high>=0);int[] a=new int[256];a[0]=packed;var layer=new FloatingIceLayer(a);a[0]=0;
    require(layer.bottom(0,0)==64+low&&layer.top(0,0)==64+high,"Immutable compact signed-Y span");
    var encoded=layer.encode();encoded[0]=0;require(layer.encode()[0]==packed,"Encode snapshot isolation");
    hash=31*hash+packed;volume+=high-low+1;chunkVolumes[(Math.floorDiv(z,16)+8)*17+Math.floorDiv(x,16)+8]+=high-low+1;
    if(n<6)rows.append(x).append(',').append(z).append(',').append(low).append(',').append(high).append('\n');
   }
   for(int v:chunkVolumes)if(v>8192)exceedsSparse=true;
   // A whole perimeter of equally high walls is the rejected extrusion.
   // Require both low shoulders and a surviving calving face, plus a range
   // of shoreline elevations, independently of the shape construction.
   var shore=new HashSet<Integer>();int highestEdge=-1;
   for(int z=-128;z<=128;z++)for(int x=-128;x<=128;x++){
    int y=b.top(x,z);if(y<0)continue;
    if(b.top(x-1,z)<0||b.top(x+1,z)<0||b.top(x,z-1)<0||b.top(x,z+1)<0){shore.add(y);highestEdge=Math.max(highestEdge,y);}
   }
   require(shore.size()>=5&&shore.contains(0),"Visible shoulders reach water with varied perimeter elevations");
   require(highestEdge>=b.freeboard/4,"Some broad calving relief remains");
   require(volume==b.volume,"Exact voxel mass tally");signatures.add(hash);
   if(n<6){Files.writeString(out.resolve("shape-"+n+".csv"),rows);System.out.println("ICEBERG_SHAPE n="+n+" type="+b.type+" volume="+volume+" submerged="+wet+" draft="+b.draft+" freeboard="+b.freeboard);}
  }
  require(exceedsSparse,"Compact spans represent bodies beyond the sparse block limit");
  require(types.size()==3&&signatures.size()==64,"Varied bounded bodies");
  for(int[] bad:new int[][]{new int[255],new int[257]}){boolean rejected=false;try{new FloatingIceLayer(bad);}catch(IllegalArgumentException e){rejected=true;}require(rejected,"Malformed span array rejected");}
  System.out.println("ICEBERG_SHAPES_FLOTATION_SPAN_SNAPSHOT_VARIATION_PASS");
 }
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
