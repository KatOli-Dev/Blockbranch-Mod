package blockbranch.realistic;
import java.util.*;

/** Static cold-forest snow retention proxies;no seasons,snowfall or basin-wide freezing simulation. */
public final class SnowyTaiga {
 public static final int SNOW_FIRST=81,SNOW_LAST=88;
 public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return Taiga.snowyCandidate(seed,x,z,d,c);}
 static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return Taiga.snowySurfaceCandidate(seed,x,z,d,c);}
 public static int snowCode(int layers){return SNOW_FIRST+layers-1;}
 public static boolean snow(int code){return code>=SNOW_FIRST&&code<=SNOW_LAST;}
 private static int at(int[]base,int x,int y,int z){int key=((y-TerrainShape.FLOOR)<<8)|((z&15)*16+(x&15)),lo=0,hi=base.length-1;while(lo<=hi){int mid=(lo+hi)>>>1,k=base[mid]>>>blockbranch.cubic.CanopyLayer.STATE_BITS;if(k==key)return base[mid]&blockbranch.cubic.CanopyLayer.STATE_MASK;if(k<key)lo=mid+1;else hi=mid-1;}return 0;}
 private static boolean support(int code){return WoodlandTrees.logCode(code)||code>=7&&code<=18||code>=22&&code<=27||code>=31&&code<=36||code>=40&&code<=45;}
 public static int[] snowColumn(long seed,int cx,int cz,int[]base,DrainageNetwork d,ClimateField c,GroundEcology.Field ecology){return snowColumn(seed,cx,cz,base,d,c,ecology,false);}
 static int[] snowColumn(long seed,int cx,int cz,int[]base,DrainageNetwork d,ClimateField c,GroundEcology.Field ecology,boolean grove){
  boolean[] selected=new boolean[256];boolean any=false;for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z;boolean in=grove?Grove.surfaceContains(seed,wx,wz,d,c):surfaceContains(seed,wx,wz,d,c)&&!Grove.surfaceContains(seed,wx,wz,d,c);selected[z*16+x]=in;any|=in;}if(!any)return base;
  var snow=new TreeMap<Integer,Integer>();int[] top=new int[256],code=new int[256];Arrays.fill(top,Integer.MIN_VALUE);for(int v:base){int k=v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,i=k&255;top[i]=(k>>>8)+TerrainShape.FLOOR;code[i]=v&blockbranch.cubic.CanopyLayer.STATE_MASK;}
  for(int z=0;z<16;z++)for(int x=0;x<16;x++){
   if(!selected[z*16+x])continue;int wx=cx*16+x,wz=cz*16+z,i=z*16+x;var sample=d.sample(wx,wz);if(sample.waterLevel()!=DrainageNetwork.DRY||LandVegetation.surfaceReserved(d,wx,wz))continue;
   var site=ecology.sample(wx,wz).shade(top[i]>sample.height()+2);double patch=Taiga.groundPatch(seed^0x534E4F575245544EL,wx,wz);
   // Broad exposure-scoured openings;spatial field and terrain determine snow depth,not independent cell scatter.
   int layers=site.exposure()>.72&&patch<-.1?0:site.shaded()?1+(patch>.3?1:0):2+(patch>.15?1:0)+(site.retention()>.4?1:0);
   if(grove&&layers>0)layers=Math.min(8,layers+1);
   if(layers>0&&at(base,wx,sample.height()+1,wz)==0){int key=((sample.height()+1-TerrainShape.FLOOR)<<8)|i;snow.put(key,snowCode(layers));}
   if(top[i]>sample.height()+2&&support(code[i])){int key=((top[i]+1-TerrainShape.FLOOR)<<8)|i;snow.put(key,snowCode(patch>.45&&site.exposure()<.4?2:1));}
  }
  return SwampCanopy.merge(base,snow.entrySet().stream().mapToInt(e->(e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS)|e.getValue()).toArray());
 }
 public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,boolean covered){int base=Taiga.cover(seed^0x534E4F57534F494CL,x,z,site,clear||covered);if(!covered)return base;return BorealSurface.mossSite(base)?13:base==8||base==9||base==12?14:13;}
 public record Point(int x,int z){}

 private SnowyTaiga(){}
}
