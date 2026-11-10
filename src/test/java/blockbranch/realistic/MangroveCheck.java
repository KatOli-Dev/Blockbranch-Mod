package blockbranch.realistic;
import java.util.*;

/** Actual three-seed coastal partition,root fluids,leaf support and negative-coordinate chunk seams. */
public final class MangroveCheck{
 static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
 public static void main(String[] args){for(long seed:new long[]{424242,1,-17}){
  var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=blockbranch.realistic.MangroveSwampFixtures.fixture(d,seed,false);int cx=p.x()>>4,cz=p.z()>>4,leaves=0,roots=0,wet=0,props=0;var map=new HashMap<String,Integer>();var columns=new LinkedHashMap<String,int[]>();
  int changed=0,sand=0,shelf=0,land=0;long hydro=0;
  for(int z=p.z()-96;z<=p.z()+96;z+=4)for(int x=p.x()-96;x<=p.x()+96;x+=4){var s=d.sample(x,z);hydro=hydro*31+s.hashCode();if(!MangroveSwamp.contains(seed,x,z,d,c))continue;changed++;require(!d.terminalLakeWater(x,z,s)&&d.channelAt(x,z).edge()<0,"No lake or routed channel takeover");if(LowGradientSandyShore.contains(x,z,(xx,zz)->LowGradientSandyShore.site(d,xx,zz)))sand++;else if(MarineShelf.contains(x,z,d))shelf++;else land++;}
  for(int z=cz-2;z<=cz+2;z++)for(int x=cx-2;x<=cx+2;x++){var a=MangroveTrees.column(seed,x,z,d,c);require(a.length<8192,"Bounded canopy");columns.put(x+","+z,a);for(int v:a){int key=v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,code=v&blockbranch.cubic.CanopyLayer.STATE_MASK,wx=x*16+(key&15),wz=z*16+((key>>>4)&15),wy=(key>>>8)+TerrainShape.FLOOR;map.put(wx+","+wy+","+wz,code);var s=d.sample(wx,wz);require(wy>s.height(),"No buried roots or crown");if(s.waterLevel()!=DrainageNetwork.DRY&&wy<s.waterLevel())require(code==72,"Only waterlogged roots replace retained source fluid");if(code>=65&&code<=70)leaves++;if(code==71||code==72)roots++;if(code==72)wet++;if(code>=73)props++;}}
  for(int z=cz-1;z<=cz+1;z++)for(int x=cx-1;x<=cx+1;x++)for(int v:columns.get(x+","+z)){int k=v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,code=v&blockbranch.cubic.CanopyLayer.STATE_MASK,wx=x*16+(k&15),wz=z*16+((k>>>4)&15),wy=(k>>>8)+TerrainShape.FLOOR;if(code>=65&&code<=70){boolean support=false;for(var q:DIR){int n=map.getOrDefault((wx+q[0])+","+(wy+q[1])+","+(wz+q[2]),0);if(n>=62&&n<=64&&code==65||n>=65&&n<code)support=true;}require(support,"Actual merged cross-chunk leaf distance");}if(code>=73){int above=map.getOrDefault(wx+","+(wy+1)+","+wz,0);require(above>=65&&above<=70,"Hanging propagule below actual leaves");}}
  // Flood structural cells from actual bed contact;roots are not artificial log-tag leaf supports.
  var anchored=new HashSet<String>();var queue=new ArrayDeque<String>();
  for(var e:map.entrySet())if(e.getValue()>=62&&e.getValue()<=64||e.getValue()==71||e.getValue()==72){var q=e.getKey().split(",");int x=Integer.parseInt(q[0]),y=Integer.parseInt(q[1]),z=Integer.parseInt(q[2]);if(y==d.sample(x,z).height()+1){anchored.add(e.getKey());queue.add(e.getKey());}}
  while(!queue.isEmpty()){var p0=queue.removeFirst().split(",");for(var q:DIR){String key=(Integer.parseInt(p0[0])+q[0])+","+(Integer.parseInt(p0[1])+q[1])+","+(Integer.parseInt(p0[2])+q[2]);int code=map.getOrDefault(key,0);if((code>=62&&code<=64||code==71||code==72)&&anchored.add(key))queue.add(key);}}
  for(var e:map.entrySet()){var q=e.getKey().split(",");int x=Integer.parseInt(q[0]),z=Integer.parseInt(q[2]);if((x>>4)>=cx-1&&(x>>4)<=cx+1&&(z>>4)>=cz-1&&(z>>4)<=cz+1&&(e.getValue()>=62&&e.getValue()<=64||e.getValue()==71||e.getValue()==72))require(anchored.contains(e.getKey()),"Every interior structural cell connects to a grounded root contact");}
  var order=new ArrayList<>(columns.keySet());Collections.reverse(order);for(var k:order){var q=k.split(",");require(Arrays.equals(columns.get(k),MangroveTrees.column(seed,Integer.parseInt(q[0]),Integer.parseInt(q[1]),d,c)),"Chunk-order determinism");}
  require(leaves>500&&roots>50&&props>0,"Distinct actual mangrove stand");System.out.println("MANGROVE seed="+seed+" point="+p+" local49x49partition="+changed+" sand="+sand+" shelf="+shelf+" otherLand="+land+" leaves="+leaves+" roots="+roots+" wetRoots="+wet+" propagules="+props+" hydrology="+hydro);
 }System.out.println("MANGROVE_COAST_ROOT_FLUID_LEAF_SEAM_ORDER_PASS");}
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
