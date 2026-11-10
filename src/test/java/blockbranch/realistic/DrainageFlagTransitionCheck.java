package blockbranch.realistic;
/** A late-created preformation view may capture different QA-only flags. */
public final class DrainageFlagTransitionCheck {
 public static void main(String[]args)throws Exception{
  String a=System.getProperty("blockbranch.terrainStructuralComparison"),b=System.getProperty("blockbranch.realisticClientFixture");
  try{
   System.clearProperty("blockbranch.terrainStructuralComparison");System.clearProperty("blockbranch.realisticClientFixture");
   var d=new DrainageNetwork(new TerrainShape(424242),424242,true,true);
   System.setProperty("blockbranch.terrainStructuralComparison","true");System.setProperty("blockbranch.realisticClientFixture","true");
   var raw=DrainageNetwork.class.getDeclaredMethod("sample",int.class,int.class,boolean.class);raw.setAccessible(true);
   var view=d.preformation();int cases=0,differing=0,unprotectedFailures=0;
   for(int[]p:new int[][]{{228504,-267736},{-1318712,-1697680},{387960,-804006},{0,0}})for(int z=-16;z<=16;z+=8)for(int x=-16;x<=16;x+=8){
    int wx=p[0]+x,wz=p[1]+z;var original=(DrainageNetwork.Sample)raw.invoke(d,wx,wz,true);
    if(!original.equals(view.sample(wx,wz)))differing++;
    var expected=d.eroded().apply(wx,wz,original);
    if(!d.eroded().apply(wx,wz,view.sample(wx,wz)).equals(expected))unprotectedFailures++;
    if(!d.sample(wx,wz).equals(expected))throw new AssertionError("QA flag transition changes the old composition");cases++;
   }
   if(differing==0)throw new AssertionError("Fixture must exercise distinct base modes");
   if(unprotectedFailures==0)throw new AssertionError("Unguarded reuse must fail the frozen composition oracle");
   System.out.println("PASS captured QA flag transition cases="+cases+" distinctBaseModes="+differing+" unguardedFailures="+unprotectedFailures);
  }finally{
   if(a==null)System.clearProperty("blockbranch.terrainStructuralComparison");else System.setProperty("blockbranch.terrainStructuralComparison",a);
   if(b==null)System.clearProperty("blockbranch.realisticClientFixture");else System.setProperty("blockbranch.realisticClientFixture",b);
  }
 }
}
