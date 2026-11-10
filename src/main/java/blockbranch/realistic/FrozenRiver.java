package blockbranch.realistic;
import java.util.*;
/** Static conservative cold-reach cover; no water-temperature,season,flow or ice-breakup simulation. */
public final class FrozenRiver {
 public static final int ICE=91;
 static boolean point(int x,int z,DrainageNetwork d,ClimateField c){var s=RiverChannel.site(d,x,z);return RiverChannel.eligible(s)&&s.depth()>=2&&s.channel().gradient()<=.002&&c.sample(x,z,d::sample).meanTemperatureC()<-4;}
 public static boolean contains(int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;if(!RiverChannel.contains(x,z,d)||!point(x,z,d,c))return false;for(int oz=0;oz<4;oz++)for(int ox=0;ox<4;ox++)if(!point(x+ox,z+oz,d,c))return false;return true;}
 /** Surface ice follows each qualifying block; biome identity remains quart-sized. */
 public static int[] column(int cx,int cz,DrainageNetwork d,ClimateField c){
  var out=new TreeMap<Integer,Integer>();
  for(int z=0;z<16;z++)for(int x=0;x<16;x++){
   int wx=cx*16+x,wz=cz*16+z;
   if(!point(wx,wz,d,c))continue;
   int y=d.sample(wx,wz).waterLevel()-1;
   out.put(((y-TerrainShape.FLOOR)<<8)|(z*16+x),ICE);
  }
  return out.entrySet().stream().mapToInt(e->e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS|e.getValue()).toArray();
 }
 public record Point(int x,int z){}

 private FrozenRiver(){}
}
