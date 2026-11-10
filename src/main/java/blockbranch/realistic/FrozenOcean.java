package blockbranch.realistic;
import java.util.*;
/** Connected marine cold-season ice; one voxel represents surface cover, not predicted thickness. */
public final class FrozenOcean {
 public static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
  var s=d.sample(x,z);int depth=s.waterLevel()==DrainageNetwork.DRY?0:s.waterLevel()-s.height()-1;
  if(depth<2||!d.oceanWater(x,z,s)||RiverChannel.contains(x,z,d)||RoutedRiver.contains(seed,x,z,d,c))return 0;
  if(SeaIcePolicy.potential(z,c.sample(x,z,d::sample).meanTemperatureC()).freezingMarginC()<=0)return 0;
  return depth<=200?1:2;
 }
 public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceKind(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
 public static String key(int kind){if(kind<1||kind>2)throw new IllegalArgumentException("Frozen marine kind");return kind==1?"frozen_ocean":"deep_frozen_ocean";}
 public static boolean ice(long seed,int x,int z,DrainageNetwork d,ClimateField c){
  if(surfaceKind(seed,x,z,d,c)==0)return false;
  double margin=SeaIcePolicy.potential(z,c.sample(x,z,d::sample).meanTemperatureC()).freezingMarginC();
  double concentration=Math.clamp(margin/3,0,1);
  return SeaIceFractures.ice(seed,x,z,concentration);
 }
 public static int[] column(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){return column(seed,cx,cz,d,c,false);}
 public static int[] column(long seed,int cx,int cz,DrainageNetwork d,ClimateField c,boolean openIcebergHabitat){var blocks=new TreeMap<Integer,Integer>();
  for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(!(openIcebergHabitat&&IcebergField.habitat(seed,cx*16+x,cz*16+z,d,c))&&ice(seed,cx*16+x,cz*16+z,d,c))blocks.put(((TerrainShape.SEA_LEVEL-1-TerrainShape.FLOOR)<<8)|(z*16+x),FrozenRiver.ICE);
  return blocks.entrySet().stream().mapToInt(e->e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS|e.getValue()).toArray();
 }
 public record Point(int x,int z){}



 private FrozenOcean(){}
}
