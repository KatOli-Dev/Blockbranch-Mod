package blockbranch.realistic;
import java.util.*;
import blockbranch.cubic.CanopyLayer;

/** Grounded variable scaffolds,real vine chains,unchanged water and chunk-independent wet overlays. */
public final class SwampCheck{
 static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
 public static void main(String[]args){var silhouettes=new HashSet<String>();for(int n=0;n<64;n++){
  var t=SwampTrees.tree(new WoodlandTrees.Root(0,0,0,false,5+n%10,(int)OakBirchWoodland.mix(n*1297L),3));var map=new HashMap<String,Integer>();var shape=new TreeSet<String>();for(var v:t){map.put(v.x()+","+v.y()+","+v.z(),v.code());if(v.code()>=7)shape.add(v.x()+","+v.y()+","+v.z());}silhouettes.add(shape.toString());
  var visited=new HashSet<String>();var queue=new ArrayDeque<String>();queue.add("0,0,0");while(!queue.isEmpty()){var k=queue.removeFirst();if(!visited.add(k))continue;var p=k.split(",");for(var d:DIR){String q=(Integer.parseInt(p[0])+d[0])+","+(Integer.parseInt(p[1])+d[1])+","+(Integer.parseInt(p[2])+d[2]);if(map.getOrDefault(q,0)>0&&map.get(q)<=3&&!visited.contains(q))queue.add(q);}}
  for(var v:t)if(v.code()<=3)require(visited.contains(v.x()+","+v.y()+","+v.z()),"Connected rooted low oak scaffold");else{int dist=WoodlandTrees.leafDistance(v.code());boolean support=false;for(var d:DIR){int near=map.getOrDefault((v.x()+d[0])+","+(v.y()+d[1])+","+(v.z()+d[2]),0);if(near>0&&(near<=3?dist==1:WoodlandTrees.leafDistance(near)<dist))support=true;}require(support,"Real leaf support");}
 }require(silhouettes.size()>48,"Architecture variation beyond one silhouette");
 for(long seed:new long[]{424242,1,-17}){
  var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=blockbranch.realistic.SwampFixtures.fixture(d,seed);require(Swamp.contains(seed,p.x(),p.z(),d,c)&&d.sample(p.x(),p.z()).waterLevel()==DrainageNetwork.DRY,"Trunk anchor is real dry bank");var actual=new HashMap<String,Integer>();var chunks=new TreeMap<String,int[]>();int cx=p.x()>>4,cz=p.z()>>4,leaves=0,vines=0,lilies=0;
  for(int z=cz-2;z<=cz+2;z++)for(int x=cx-2;x<=cx+2;x++){var a=WoodlandTrees.column(seed,x,z,d,c);require(a.length<CanopyLayer.MAX_BLOCKS,"Bounded wetland overlays");chunks.put(x+","+z,a);for(int v:a){int key=v>>>CanopyLayer.STATE_BITS,code=v&CanopyLayer.STATE_MASK,wx=x*16+(key&15),wz=z*16+((key>>>4)&15),wy=(key>>>8)+TerrainShape.FLOOR;actual.put(wx+","+wy+","+wz,code);var s=d.sample(wx,wz);require(wy>s.height()&&(s.waterLevel()==DrainageNetwork.DRY||wy>=s.waterLevel()),"No submerged wood/leaf/vine cells");if(code>=7&&code<=12)leaves++;if(code>=46&&code<=60)vines++;if(code==61){lilies++;require(wy==s.waterLevel()&&d.terminalLakeWater(wx,wz,s),"Lily rests on confirmed freshwater surface");require(InlandLakeReedbed.cover(seed,wx,wz,InlandLakeReedbed.contains(wx,wz,(xx,zz)->InlandLakeReedbed.site(d,xx,zz)),InlandLakeReedbed.site(d,wx,wz))<=1,"Emergent reed stems preserved");}}}
  require(leaves>500&&vines>10&&lilies>0,"Characteristic actual oak/vine/lily structure");
  for(int cz2=cz-1;cz2<=cz+1;cz2++)for(int cx2=cx-1;cx2<=cx+1;cx2++)for(int v:chunks.get(cx2+","+cz2)){int key=v>>>CanopyLayer.STATE_BITS,code=v&CanopyLayer.STATE_MASK,x=cx2*16+(key&15),z=cz2*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;
   if(code>=7&&code<=12){boolean support=false;for(var a:DIR){int near=actual.getOrDefault((x+a[0])+","+(y+a[1])+","+(z+a[2]),0);if(near>0&&near<=45&&(WoodlandTrees.logCode(near)?WoodlandTrees.leafDistance(code)==1:WoodlandTrees.leafDistance(near)<WoodlandTrees.leafDistance(code)))support=true;}require(support,"Clipped merged leaf support");}
   if(code>=46&&code<=60){int mask=code-45;int[][]face={{0,-1},{1,0},{0,1},{-1,0}};for(int bit=0;bit<4;bit++)if((mask&(1<<bit))!=0){int near=actual.getOrDefault((x+face[bit][0])+","+y+","+(z+face[bit][1]),0),above=actual.getOrDefault(x+","+(y+1)+","+z,0);require(near>0&&near<=45||above>=46&&above<=60&&((above-45)&(1<<bit))!=0,"Actual vine face/vertical-chain support across chunks");}}
  }
  var order=new ArrayList<>(chunks.keySet());Collections.reverse(order);for(var key:order){var q=key.split(",");require(Arrays.equals(chunks.get(key),WoodlandTrees.column(seed,Integer.parseInt(q[0]),Integer.parseInt(q[1]),d,c)),"Chunk request order independence");}
  int dead=0;for(int z=cz-8;z<=cz+8;z++)for(int x=cx-8;x<=cx+8;x++)for(int v:WoodlandDetails.logs(seed,x,z,d,c)){dead++;int key=v>>>CanopyLayer.STATE_BITS,wx=x*16+(key&15),wz=z*16+((key>>>4)&15),wy=(key>>>8)+TerrainShape.FLOOR;var q=d.sample(wx,wz);require(wy==q.height()+1&&q.waterLevel()==DrainageNetwork.DRY&&d.channelAt(wx,wz).edge()<0,"Deadwood rests on supported dry ground,not water/channels");}
  System.out.println("SWAMP_STRUCTURE seed="+seed+" point="+p+" leaves="+leaves+" vines="+vines+" lilies="+lilies+" nearbyDeadwoodCells="+dead);
 }
 System.out.println("SWAMP_VARIABLE_SCAFFOLD_VINE_WATER_BOUNDARY_ORDER_PASS");
 }
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
