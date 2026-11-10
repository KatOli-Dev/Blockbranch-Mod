package blockbranch.realistic;
import java.util.*;
/** Explicit dry-bank partition;wet reedbeds,river margins and water geometry remain owned by prior habitats. */
public final class SwampCoverageCheck{
 static String before(long seed,int x,int z,DrainageNetwork d,ClimateField c){return BirchForest.contains(seed,x,z,d,c)?"birch-forest":Forest.potential(seed,x,z,d,c)?"forest":PlacementBaseline.definition30(seed,x,z,d,c);}
 public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){
  var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var gains=new TreeMap<String,Integer>();int coarse=0;
  for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000)if(Swamp.contains(seed,x,z,d,c)){coarse++;gains.merge(before(seed,x,z,d,c),1,Integer::sum);check(seed,x,z,d,c);}
  var p=blockbranch.realistic.SwampFixtures.fixture(d,seed);int local=0,retainedReed=0,retainedRiver=0;var localGains=new TreeMap<String,Integer>();for(int z=p.z()-128;z<=p.z()+128;z+=4)for(int x=p.x()-128;x<=p.x()+128;x+=4){boolean swamp=Swamp.contains(seed,x,z,d,c);if(swamp){local++;localGains.merge(before(seed,x,z,d,c),1,Integer::sum);check(seed,x,z,d,c);}if(InlandLakeReedbed.contains(x,z,(xx,zz)->InlandLakeReedbed.site(d,xx,zz))){retainedReed++;require(!swamp,"Protected reed biome not relabelled");}if(GravelRiverMargin.contains(x,z,(xx,zz)->GravelRiverMargin.site(d,xx,zz))){retainedRiver++;require(!swamp,"Protected gravel river margin not relabelled");}}
  require(local>20&&retainedReed>20,"Real contiguous bank and retained reedbelt witness");System.out.println("SWAMP_PARTITION seed="+seed+" coarse="+coarse+" gains="+gains+" localSwamp="+local+" localGains="+localGains+" retainedReed="+retainedReed+" retainedRiver="+retainedRiver);
 }System.out.println("SWAMP_DRY_BANK_PARTITION_HYDROLOGY_REED_RIVER_PASS");}
 static void check(long seed,int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;var a=d.sample(x,z);var bank=Swamp.bank(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d);require(bank!=null&&bank.above()>=0&&bank.above()<=3&&a.waterLevel()==DrainageNetwork.DRY&&d.channelAt(x,z).edge()<0,"Only saturated low dry banks,not active water/channel");String old=before(seed,x,z,d,c);require(!Set.of("lake","reedbed","river","river-channel","shore","marine-shelf","deep-ocean","scree").contains(old),"Only explicit prior land reallocation");require(a.equals(d.sample(x,z)),"Eligibility does not alter hydrology");}
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
}
