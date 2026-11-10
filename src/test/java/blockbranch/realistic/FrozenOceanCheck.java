package blockbranch.realistic;
public final class FrozenOceanCheck {
 public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);
  for(int k:new int[]{1,2}){var p=blockbranch.realistic.FrozenOceanFixtures.fixture(d,seed,k);int ice=0,leads=0,offQuart=0;var saved=new java.util.HashMap<Long,DrainageNetwork.Sample>();
   for(int z=p.z()-256;z<=p.z()+256;z+=4)for(int x=p.x()-256;x<=p.x()+256;x+=4){var s=d.sample(x,z);long key=(x&0xffffffffL)|((long)z<<32);saved.put(key,s);boolean frozen=FrozenOcean.ice(seed,x,z,d,c);if(frozen){ice++;require(s.waterLevel()==64&&64-s.height()-1>=2&&d.oceanWater(x,z,s)&&!RiverChannel.contains(x,z,d)&&!RoutedRiver.contains(seed,x,z,d,c),"Ice only on connected source marine water outside rivers");require(c.sample(x,z,d::sample).meanTemperatureC()<8,"Completed warm/temperate marine classes protected");}else if(FrozenOcean.surfaceKind(seed,x,z,d,c)>0)leads++;if(frozen!=FrozenOcean.ice(seed,x+1,z+1,d,c))offQuart++;}
   require(ice>100&&leads>0&&offQuart>0,"Coherent floes with real leads and block-resolution edges");
   for(var e:saved.entrySet()){int x=(int)(long)e.getKey(),z=(int)(e.getKey()>>32);require(e.getValue().equals(d.sample(x,z)),"Ice classification cannot mutate terrain/water");}
   require(k==FrozenOcean.kind(seed,p.x(),p.z(),d,c),"Actual depth-specific quart witness");
   System.out.println("FROZEN_OCEAN_FLOES seed="+seed+" kind="+k+" point="+p+" ice="+ice+" leads="+leads+" offQuart="+offQuart+" climate="+c.sample(p.x(),p.z(),d::sample)+" potential="+SeaIcePolicy.potential(p.z(),c.sample(p.x(),p.z(),d::sample).meanTemperatureC()));
  }
  int[] edge=seed==424242?new int[]{-1576272,-2047924,-1576268,-2047924}:seed==1?new int[]{-1314420,-2047876,-1314416,-2047876}:new int[]{-1736900,-2048280,-1736896,-2048280};
  int a=FrozenOcean.kind(seed,edge[0],edge[1],d,c),b=FrozenOcean.kind(seed,edge[2],edge[3],d,c);require(a>0&&b>0&&a!=b,"Adjacent native shelf/deep ice identities across200m depth");
  var sa=d.sample(edge[0],edge[1]);var sb=d.sample(edge[2],edge[3]);require(sa.waterLevel()==64&&sb.waterLevel()==64&&sa.basin()==sb.basin(),"Depth identity transition preserves connected water datum/catchment");
  System.out.println("FROZEN_MARINE_DEPTH_BOUNDARY_PASS seed="+seed+" edge="+java.util.Arrays.toString(edge)+" a="+a+" b="+b);
  for(int z=-1900000;z<=1900000;z+=160000)for(int x=-1900000;x<=1900000;x+=160000){var s=d.sample(x,z);if(s.waterLevel()==DrainageNetwork.DRY||s.closed()||!d.oceanWater(x,z,s))require(!FrozenOcean.ice(seed,x,z,d,c),"Dry land/closed lakes/inland water never marine ice");}
 }
 System.out.println("FROZEN_OCEAN_CONNECTED_WATER_RIVER_WARM_DOMAINS_BLOCK_EDGES_UNCHANGED_TERRAIN_PASS");}
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
