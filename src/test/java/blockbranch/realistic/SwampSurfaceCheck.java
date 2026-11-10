package blockbranch.realistic;
/** Ground mosaics form continuous wetness/substrate patches;plant scatter cannot change their material. */
public final class SwampSurfaceCheck{
 public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){
  int changes=0,edges=0,mud=0,moss=0,grass=0,isolated=0;var site=new GroundEcology.Site(.70,.1,0,0,0,false);
  for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){int here=Swamp.substrate(seed,x,z,site,1.5);if(here==0)mud++;else if(here==1)moss++;else grass++;int different=0;for(var q:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(Swamp.substrate(seed,x+q[0],z+q[1],site,1.5)!=here)different++;if(different==4)isolated++;for(var q:new int[][]{{1,0},{0,1}}){edges++;if(Swamp.substrate(seed,x+q[0],z+q[1],site,1.5)!=here)changes++;}
   if(Swamp.substrate(seed,x,z,site.shade(true),1.5)!=here)throw new AssertionError("Individual canopy cells do not tile the substrate");
  }
  if(changes>edges*.12||isolated>10||moss<1000||mud<100||grass<100)throw new AssertionError("Continuous diverse patches,not independent per-block material scatter");
  var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=blockbranch.realistic.SwampFixtures.fixture(d,seed);int exactEdge=0;for(int z=p.z()-64;z<=p.z()+64;z++)for(int x=p.x()-64;x<=p.x()+64;x++)if(Swamp.bank(x,z,d)!=null&&Swamp.climateSuitable(c.sample(x,z,d::sample))){if(!Swamp.soilContains(seed,x,z,d,c))throw new AssertionError("Exact dry bank cannot fall through a wet quart to bare stone");exactEdge++;}
  if(exactEdge<100)throw new AssertionError("Actual dry bank witness");System.out.println("SWAMP_COHERENT_SOIL seed="+seed+" materialEdgeFraction="+changes/(double)edges+" isolatedCells="+isolated+" mud="+mud+" moss="+moss+" grass="+grass+" exactDryBank="+exactEdge);
 }System.out.println("SWAMP_CONTINUOUS_SUBSTRATE_EXACT_DRY_EDGE_PASS");}
 private SwampSurfaceCheck(){}
}
