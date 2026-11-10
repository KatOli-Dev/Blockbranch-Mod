package blockbranch.realistic;

/** Early morphology/preservation diagnostic; not visual acceptance. */
public final class ErodedBadlandsCheck {
 public static void main(String[] args){
  for(long seed:new long[]{424242,1,-17}){
   var d=new DrainageNetwork(new TerrainShape(seed),seed);var old=d.preformation();var c=new ClimateField(seed);var f=d.eroded();
   Badlands.Point found=null;
   search:for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){
    if(f.envelope(x,z)<.99)continue;
    found=new Badlands.Point(x,z);break search;
   }
   require(found!=null,"Natural preformation interior");
   int changed=0,retained=0,max=0,jump=0;int[] saved=new int[193*193];
   for(int dz=-96;dz<=96;dz++)for(int dx=-96;dx<=96;dx++){
    int x=found.x()+dx,z=found.z()+dz;var a=old.sample(x,z);var b=d.sample(x,z);int cut=a.height()-b.height();saved[(dz+96)*193+dx+96]=b.height();
    require(cut>=0&&cut<=40,"Bounded incision");require(a.waterLevel()==b.waterLevel()&&a.basin()==b.basin()&&a.closed()==b.closed()&&a.regionalHeight()==b.regionalHeight()&&a.regionalWaterLevel()==b.regionalWaterLevel(),"Hydrology/climate reference unchanged");
    require(a.waterLevel()==DrainageNetwork.DRY,"Dry formation");
    if(cut>0){changed++;require(Badlands.surfaceKind(seed,x,z,old,c)==1,"Only preformation bare badlands altered");}
    // Weathered remnant tops may lose a few metres; exact outside preservation is tested separately.
    if(cut<=5)retained++;
    max=Math.max(max,cut);jump=Math.max(jump,Math.abs(cut-(old.sample(x+1,z).height()-d.sample(x+1,z).height())));jump=Math.max(jump,Math.abs(cut-(old.sample(x,z+1).height()-d.sample(x,z+1).height())));
   }
   System.out.println("ERODED_COUNTS seed="+seed+" changed="+changed+" remnantTopsWithin5m="+retained+" max="+max+" jump="+jump);
   require(changed>1000&&retained>100&&max>=20&&jump<=12,"Sparse remnant tops, incision and bounded flanks");
   // The region exceeds the exact sample cache. A coprime permutation crosses
   // positive/negative chunk seams in a different order after eviction.
   for(int i=0;i<saved.length;i++){int index=(i*8191)%saved.length,x=found.x()+index%193-96,z=found.z()+index/193-96;require(d.sample(x,z).height()==saved[index],"Order/cache/seam determinism");}
   int edges=0;
   for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
    for(int distance=1;distance<=8192;distance++){
     int x=found.x()+direction[0]*distance,z=found.z()+direction[1]*distance;
     double strength=f.envelope(x,z);
     int cut=old.sample(x,z).height()-d.sample(x,z).height();
     int prior=old.sample(x-direction[0],z-direction[1]).height()-d.sample(x-direction[0],z-direction[1]).height();
     require(Math.abs(cut-prior)<=12,"Bounded incision along outward support transect x="+x+" z="+z+" cut="+cut+" prior="+prior+" envelope="+strength+" previousEnvelope="+f.envelope(x-direction[0],z-direction[1]));
     if(strength==0){require(cut==0,"Exact zero at formation support boundary");edges++;break;}
    }
   }
   require(edges==4,"Witness has four bounded support transitions");
   var wooded=blockbranch.realistic.BadlandsFixtures.fixture(old,seed,Badlands.WOODED);
   for(int dz=-64;dz<=64;dz+=4)for(int dx=-64;dx<=64;dx+=4){int x=wooded.x()+dx,z=wooded.z()+dz;require(old.sample(x,z).equals(d.sample(x,z)),"Existing woodland interior preserved exactly");}
   for(int i=0;i<2000;i++){int x=-2000000+(int)Long.remainderUnsigned(OakBirchWoodland.mix(seed+i),4000000),z=-2000000+(int)Long.remainderUnsigned(OakBirchWoodland.mix(seed-i-30),4000000);var a=old.sample(x,z);var b=d.sample(x,z);if(f.envelope(x,z)==0)require(a.equals(b),"Exact outside preservation");}
   System.out.println("ERODED_DIAGNOSTIC seed="+seed+" point="+found+" changed="+changed+" remnantTopsWithin5m="+retained+" max="+max+" jump="+jump);
  }
  System.out.println("ERODED_BADLANDS_PREFORMATION_SEAMS_ORDER_BOUNDARIES_PRESERVATION_PASS");
 }
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
