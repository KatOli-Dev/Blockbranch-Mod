package blockbranch.realistic;
/** Cold identities reuse physical substrate; temperature is an air-climate map proxy. */
public final class ColdOceanCheck {
 public static void main(String[]args){
  for(long seed:new long[]{424242,1,-17}){
   var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var shallow=blockbranch.realistic.ColdOceanFixtures.fixture(d,seed);var deep=blockbranch.realistic.DeepColdOceanFixtures.fixture(d,seed);
   int selected=0,grass=0;
   for(int z=shallow.z()-32;z<=shallow.z()+32;z++)for(int x=shallow.x()-32;x<=shallow.x()+32;x++){
    if(!ColdOcean.surfaceContains(seed,x,z,d,c))continue;selected++;
    var site=MarineShelf.site(d,x,z);int code=ColdOcean.cover(seed,x,z,d,c);if(code==12)grass++;
    require(code==MarineShelf.cover(seed,x,z,true,site,c.sample(x,z,d::sample)),"Existing material/plant semantics retained");
    require(!TemperateOcean.surfaceContains(seed,x,z,d,c)&&FrozenOcean.surfaceKind(seed,x,z,d,c)==0,"Completed marine classes excluded");
   }
   require(selected>100&&grass>10,"Natural supported photic vegetation and bare gaps");
   require(DeepColdOcean.contains(deep.x(),deep.z(),d,c)&&!TemperateDeepOcean.contains(deep.x(),deep.z(),d,c)&&FrozenOcean.kind(seed,deep.x(),deep.z(),d,c)==0,"Cold deep domain excludes temperate and ice");
   System.out.println("COLD_MARINE seed="+seed+" shelf="+shallow+" deep="+deep+" selected="+selected+" grass="+grass);
   for(int kind:new int[]{1,2}){var frozen=blockbranch.realistic.FrozenOceanFixtures.fixture(d,seed,kind);require(!ColdOcean.contains(seed,frozen.x(),frozen.z(),d,c)&&!DeepColdOcean.contains(frozen.x(),frozen.z(),d,c),"Completed frozen ocean domains remain excluded");}
   System.out.println("COLD_DEPTH_EDGE "+DeepColdOcean.shelfTransition(d,seed));System.out.println("COLD_THERMAL_EDGE "+DeepColdOcean.thermalTransition(d,seed,false));
  }
  System.out.println("COLD_MARINE_DOMAIN_MATERIAL_DEPTH_THERMAL_PASS");
 }
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
