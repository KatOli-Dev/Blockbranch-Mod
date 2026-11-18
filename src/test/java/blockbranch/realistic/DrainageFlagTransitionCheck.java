package blockbranch.realistic;
/**
 * The QA structural-comparison mode is fixed per network family. Views (preformation, references) inherit
 * it from their owner, so a normal network keeps the old eroded composition and a comparison network stays raw
 * all the way down; no late view can capture a different mode.
 */
public final class DrainageFlagTransitionCheck {
 public static void main(String[]args)throws Exception{
  var normal=new DrainageNetwork(new TerrainShape(424242),424242,DrainageNetwork.Options.DEFAULT.withSandyCoasts(true));
  var comparison=new DrainageNetwork(new TerrainShape(424242),424242,DrainageNetwork.Options.DEFAULT.withSandyCoasts(true).withStructuralComparison(true));
  var raw=DrainageNetwork.class.getDeclaredMethod("sample",int.class,int.class,boolean.class);raw.setAccessible(true);
  var view=normal.preformation();int cases=0,weathered=0;
  for(int[]p:new int[][]{{228504,-267736},{-1318712,-1697680},{387960,-804006},{0,0}})for(int z=-16;z<=16;z+=8)for(int x=-16;x<=16;x+=8){
   int wx=p[0]+x,wz=p[1]+z;var original=(DrainageNetwork.Sample)raw.invoke(normal,wx,wz,true);
   if(!original.equals(view.sample(wx,wz)))throw new AssertionError("Preformation view inherits the detailed base mode");
   var expected=normal.eroded().apply(wx,wz,original);
   if(!normal.sample(wx,wz).equals(expected))throw new AssertionError("Normal network keeps the old eroded composition");
   if(!expected.equals(original))weathered++;
   if(!comparison.sample(wx,wz).equals(raw.invoke(comparison,wx,wz,false)))throw new AssertionError("Comparison network stays raw");
   if(comparison.preformation()!=comparison&&!comparison.preformation().sample(wx,wz).equals(raw.invoke(comparison,wx,wz,false)))throw new AssertionError("Comparison view inherits the raw mode");
   cases++;
  }
  if(weathered==0)throw new AssertionError("Fixture must exercise weathered columns");
  System.out.println("PASS fixed QA comparison mode cases="+cases+" weathered="+weathered);
 }
}
