package blockbranch.realistic;
import java.util.*;
public final class RoutedRiverCheck {
 public static void main(String[]a){for(long seed:new long[]{424242,1,-17}){
  var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);int selected=0,retained=0;long before=1,after=1;var types=new HashSet<Integer>();
  for(int i=0;i<d.size();i++){if(i%23!=0||!d.river(i)||Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000)continue;
   for(double t:new double[]{.2,.5,.8}){int x=(int)Math.round(d.edgeX(i,t)),z=(int)Math.round(d.edgeZ(i,t));var ch=d.channelAt(x,z);if(ch.edge()<0)continue;var axis=RoutedRiver.axis(d,x,z);
    for(double lateral:new double[]{-.8,-.3,0,.3,.8}){int wx=Math.floorDiv((int)Math.round(x-axis[1]*ch.halfWidth()*lateral),4)*4,wz=Math.floorDiv((int)Math.round(z+axis[0]*ch.halfWidth()*lateral),4)*4;var s=d.sample(wx,wz);before=31*before+s.height();before=31*before+s.waterLevel();
     boolean broad=RoutedRiver.contains(seed,wx,wz,d,c);if(RiverChannel.contains(wx,wz,d)){if(broad)selected++;else retained++;}if(broad&&!RoutedRiver.bankContains(seed,wx,wz,d,c)){var site=RiverChannel.site(d,wx,wz);require(RiverChannel.eligible(site)&&site.lateral()<.5&&site.channel().gradient()<=.003&&c.sample(wx,wz,d::sample).meanTemperatureC()>=-4,"Audited inner-channel/climate suitability");require(!d.terminalLakeWater(wx,wz,s)&&!d.oceanWater(wx,wz,s),"No lake/marine reclassification");types.add(RoutedRiver.cover(seed,wx,wz,d,c));}var q=d.sample(wx,wz);after=31*after+q.height();after=31*after+q.waterLevel();
    }
   }
  }
  require(before==after&&selected>1000&&retained>selected*.5,"Hydrology unchanged and substantial specific river retention");
  var p=blockbranch.realistic.RoutedRiverFixtures.fixture(d,seed);require(RoutedRiver.cover(seed,p.x(),p.z(),d,c)==7,"Actual sandy inner-channel witness");
  var old=blockbranch.realistic.RiverChannelFixtures.fixture(d,seed);require(!RoutedRiver.contains(seed,old.x(),old.z(),d,c),"Original planted specific-river witness retained");
  if(seed==1){
   int x=1613850,z=-1800885,qx=Math.floorDiv(x,4)*4,qz=Math.floorDiv(z,4)*4;
   var fringe=RiverChannel.site(d,x,z);var origin=RiverChannel.site(d,qx,qz);
   require(RiverChannel.plantSuitable(fringe,c.sample(x,z,d::sample)),"Original mixed-cell site remains suitable for plants");
   require(origin.lateral()<.5&&origin.channel().gradient()<=.003&&RiverChannel.eligible(origin),"Origin-only negative control reproduces inner-core classification");
   require(!RoutedRiver.contains(seed,x,z,d,c),"Whole-cell inner core retains planted fringe");
   // Material fields can change an individual site's substrate. Retain the
   // original geometry control above and exercise an actual planted mixed cell.
   int px=1613819,pz=-1800853;
   var planted=RiverChannel.site(d,px,pz);
   var plantedOrigin=RiverChannel.site(d,Math.floorDiv(px,4)*4,Math.floorDiv(pz,4)*4);
   require(RiverChannel.cover(seed,px,pz,true,planted,c.sample(px,pz,d::sample))>=10,"Actual mixed-cell plant witness");
   require(plantedOrigin.lateral()<.5&&plantedOrigin.channel().gradient()<=.003&&RiverChannel.eligible(plantedOrigin),"Origin-only control misclassifies the actual planted cell");
   require(!RoutedRiver.contains(seed,px,pz,d,c),"Whole-cell inner core preserves actual planted fringe");
  }
  for(int[] q:new int[][]{{p.x(),p.z()},{p.x()+16,p.z()},{p.x()-16,p.z()-16}}){int[] first=new int[256];for(int z=0;z<16;z++)for(int x=0;x<16;x++)first[z*16+x]=RoutedRiver.cover(seed,q[0]+x,q[1]+z,d,c);for(int z=15;z>=0;z--)for(int x=15;x>=0;x--)require(first[z*16+x]==RoutedRiver.cover(seed,q[0]+x,q[1]+z,d,c),"Order-independent bed patches and negative-coordinate seams");}
  System.out.println("BROAD_RIVER seed="+seed+" selectedRouteSamples="+selected+" retainedSpecific="+retained+" hydrology="+after+" codes="+types+" fixture="+p);
 }System.out.println("RIVER_ROUTED_CORE_FRINGE_PARTITION_HYDROLOGY_ORDER_PASS");}
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private RoutedRiverCheck(){}
}
