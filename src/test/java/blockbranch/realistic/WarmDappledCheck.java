package blockbranch.realistic;
import java.util.*;
import blockbranch.cubic.CanopyLayer;

/** Actual habitat identities, palette width, bounded native populations and deterministic negative-coordinate seams. */
public final class WarmDappledCheck {
 public static void main(String[]args){var colors=new TreeSet<Integer>();
  require(CanopyLayer.STATE_BITS==8&&CanopyLayer.LAST_CODE<256,"Native population requires explicit eight-bit palette");
  for(int y:new int[]{TerrainShape.FLOOR+1,-1,0,TerrainShape.CEILING-1})for(int code:new int[]{1,88,127,128,149,150,163,168}){
   int key=((y-TerrainShape.FLOOR)<<8)|255,packed=(key<<CanopyLayer.STATE_BITS)|code;
   require(packed>=0&&packed>>>CanopyLayer.STATE_BITS==key&&(packed&CanopyLayer.STATE_MASK)==code,"Whole supported vertical envelope packs without overflow");
  }
  for(long seed:new long[]{424242,1,-17}){var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var ecology=new GroundEcology.Field(d::sample,c);
   var p=blockbranch.realistic.DappledForestFixtures.fixture(d,seed);int poplar=0,leaves=0,shrubs=0,interior=0;var heights=new TreeSet<Integer>();var columns=new LinkedHashMap<String,int[]>();var world=new HashMap<String,Integer>();
   for(int dz=-32;dz<=32;dz+=8)for(int dx=-32;dx<=32;dx+=8)if(DappledForest.contains(seed,p.x()+dx,p.z()+dz,d,c))interior++;
   for(var r:WoodlandTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c))if(r.family()==DappledForest.FAMILY){heights.add(r.height());require(DappledForest.contains(seed,r.x(),r.z(),d,c)&&d.sample(r.x(),r.z()).waterLevel()==DrainageNetwork.DRY,"Poplar roots in actual dry habitat");}
   for(int cz=(p.z()>>4)-2;cz<=(p.z()>>4)+2;cz++)for(int cx=(p.x()>>4)-2;cx<=(p.x()>>4)+2;cx++){
    var a=DappledForest.shrubs(seed,cx,cz,WoodlandTrees.column(seed,cx,cz,d,c),d,c,ecology);columns.put(cx+","+cz,a);require(a.length<CanopyLayer.MAX_BLOCKS,"Bounded eight-bit canopy");int previous=-1;
    for(int v:a){int key=v>>>CanopyLayer.STATE_BITS,code=v&CanopyLayer.STATE_MASK,wx=cx*16+(key&15),wz=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;require(key>previous,"Sorted unique positions");previous=key;world.put(wx+","+y+","+wz,code);
     if(code>=128&&code<=130)poplar++;if(code>=131&&code<=148){leaves++;colors.add((code-131)/6);}if(code==149){shrubs++;require(DappledForest.surfaceContains(seed,wx,wz,d,c)&&y==d.sample(wx,wz).height()+1,"Physical native shrub placement");}
    }
   }
   for(int cz=(p.z()>>4)-1;cz<=(p.z()>>4)+1;cz++)for(int cx=(p.x()>>4)-1;cx<=(p.x()>>4)+1;cx++)for(int v:columns.get(cx+","+cz)){
    int code=v&CanopyLayer.STATE_MASK;if(code<131||code>148)continue;int key=v>>>CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;boolean support=false;
    for(int[]s:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){int near=world.getOrDefault((x+s[0])+","+(y+s[1])+","+(z+s[2]),0);if(WoodlandTrees.logCode(near)&&WoodlandTrees.leafDistance(code)==1||near>=131&&near<=148&&WoodlandTrees.leafDistance(near)<WoodlandTrees.leafDistance(code))support=true;}
    require(support,"Merged clipped native poplar leaf support");
   }
   var reverse=new ArrayList<>(columns.keySet());Collections.reverse(reverse);for(String k:reverse){var a=k.split(",");int x=Integer.parseInt(a[0]),z=Integer.parseInt(a[1]);require(Arrays.equals(columns.get(k),DappledForest.shrubs(seed,x,z,WoodlandTrees.column(seed,x,z,d,c),d,c,ecology)),"Request order/negative-coordinate seams");}
   require(poplar>100&&leaves>1000&&shrubs>0&&heights.size()>=3&&interior>=40,"Meaningful poplar cohorts, native shrubs and natural interior");
   var w=blockbranch.realistic.WarmOceanFixtures.fixture(d,seed);int coral=0,fans=0,pickles=0,open=0,offQuart=0;var species=new TreeSet<Integer>();
   for(int cz=(w.z()>>4)-3;cz<=(w.z()>>4)+3;cz++)for(int cx=(w.x()>>4)-3;cx<=(w.x()>>4)+3;cx++){
    var a=WarmOcean.column(seed,cx,cz,d,c);require(Arrays.equals(a,WarmOcean.column(seed,cx,cz,d,c))&&a.length<=768,"Bounded deterministic reef overlay");var occupied=new HashSet<Integer>();
    for(int v:a){int key=v>>>CanopyLayer.STATE_BITS,code=v&CanopyLayer.STATE_MASK,wx=cx*16+(key&15),wz=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;occupied.add(key&255);var site=MarineShelf.site(d,wx,wz);
     require(WarmOcean.aquatic(code)&&WarmOcean.surfaceContains(seed,wx,wz,d,c)&&y>site.height()&&y<site.water(),"Native coral/fans/pickles in real existing source water");
     require(!LukewarmOcean.surfaceContains(seed,wx,wz,d,c)&&!TemperateOcean.surfaceContains(seed,wx,wz,d,c)&&!MangroveSwamp.exact(seed,wx,wz,d,c),"Retained marine communities protected");
     if(code<155){coral++;species.add(code-150);}else if(code<160)fans++;else if(code<=163)pickles++;
     if(Math.floorMod(wx,4)!=0||Math.floorMod(wz,4)!=0)offQuart++;
    }
    open+=256-occupied.size();
   }
   require(coral>50&&fans>20&&pickles>0&&open>1000&&offQuart>0&&species.size()>=2,"Irregular reef colonies, native fans/pickles and meaningful open water/sand gaps");
   System.out.println("WARM_DAPPLED seed="+seed+" forest="+p+" interior="+interior+" poplar="+poplar+" leaves="+leaves+" shrubs="+shrubs+" heights="+heights+" warm="+w+" coral="+coral+" fans="+fans+" pickles="+pickles+" open="+open+" offQuart="+offQuart+" species="+species);
  }
  require(colors.size()==3,"All native poplar foliage identities occur across natural stands");System.out.println("WARM_DAPPLED_NATIVE_IDENTITIES_EIGHT_BIT_SUPPORT_SEAMS_REEF_GAPS_PASS");
 }
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private WarmDappledCheck(){}
}
