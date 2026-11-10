package blockbranch.realistic;
import java.util.*;
/** Coastal geometry, fixed climate/water/drainage, legacy controls and bounded physical bedrock. */
public final class CoastalLandformsCheck {
 public static void main(String[]args){
  for(long seed:new long[]{424242,1,-17}){
   var shape=new TerrainShape(seed);var d=new DrainageNetwork(shape,seed);var old=new DrainageNetwork(shape,seed,false);var climate=new ClimateField(seed);
   int changed=0,rock=0,wet=0,unchanged=0,river=0;var p=blockbranch.realistic.StonyShoreFixtures.fixture(d,seed);var codes=new TreeSet<Integer>();int maxRise=0;
   for(int z=p.z()-192;z<=p.z()+192;z+=4)for(int x=p.x()-192;x<=p.x()+192;x+=4){var a=old.sample(x,z);var b=d.sample(x,z);int delta=b.height()-a.height();
    require(delta>=0&&delta<=CoastalLandforms.MAX_RISE,"Bounded positive coastal relief");maxRise=Math.max(maxRise,delta);require(a.waterLevel()==b.regionalWaterLevel()&&a.basin()==b.basin()&&a.closed()==b.closed(),"Original regional water reference and catchment preserved");
    require(b.regionalHeight()==a.height()&&climate.sample(x,z,d::sample).equals(climate.sample(x,z,old::sample)),"Exact original macroclimate reference despite local bluff relief");
    if(delta>0){changed++;require((a.waterLevel()==DrainageNetwork.DRY||a.waterLevel()==TerrainShape.SEA_LEVEL)&&!a.closed(),"Only coastal land/shallow sea in ocean catchments rises");if(a.waterLevel()!=b.waterLevel())require(a.waterLevel()==TerrainShape.SEA_LEVEL&&b.waterLevel()==DrainageNetwork.DRY&&b.height()+1>=TerrainShape.SEA_LEVEL,"Only uplifted shallow marine cells become land; datum stays fixed");}else unchanged++;
    if(b.bedrockCoast()){rock++;codes.add(StonyShore.cover(seed,x,z,d));if(b.waterLevel()!=DrainageNetwork.DRY)wet++;}
    if(old.channelAt(x,z).edge()>=0){river++;require(delta==0&&!b.bedrockCoast(),"Actual rivers untouched by coastal landform/material");}
   }
   require(changed>300&&rock>300&&wet>0&&unchanged>300&&maxRise>=24&&codes.equals(Set.of(3,4)),"Genuine bluffs, foreshore platform, rock/gravel and untouched sea/shore samples");
   int fixed=0;for(int z=-1900000;z<=1900000;z+=32000)for(int x=-1900000;x<=1900000;x+=32000){var a=old.sample(x,z);var b=d.sample(x,z);require(a.waterLevel()==b.regionalWaterLevel()&&a.basin()==b.basin()&&a.closed()==b.closed(),"Global regional water reference and routing unchanged");if(a.waterLevel()!=b.waterLevel())require(a.waterLevel()==TerrainShape.SEA_LEVEL&&b.waterLevel()==DrainageNetwork.DRY&&b.height()>a.height()&&b.height()+1>=TerrainShape.SEA_LEVEL,"Only intentional shallow marine displacement changes the global wet footprint");require(climate.sample(x,z,d::sample).equals(climate.sample(x,z,old::sample)),"Global macroclimate unchanged");
    if(a.height()+1-TerrainShape.SEA_LEVEL>128||a.height()+1-TerrainShape.SEA_LEVEL< -8||a.closed())require(a.equals(b),"Unrelated inland/deep water/closed basins byte-value unchanged");fixed++;}
   for(int i=0;i<d.size();i+=127)require(d.parent(i)==old.parent(i)&&d.waterLevel(i)==old.waterLevel(i)&&d.accumulation(i)==old.accumulation(i),"Drainage topology, datums and flow accumulation exact");
   int[][] seams=seed==424242?new int[][]{{-1242762,-1699947},{1306220,-1694839},{1306160,-1694903},{1339762,-1675377}}:seed==1?new int[][]{{160982,-1674959},{1550571,-1687895},{-1399134,-1691780}}:new int[][]{{-1080737,-1582864},{-975785,-1655260},{-976728,-1662874}};int worstJump=0;
   for(int[]seam:seams)for(int z=seam[1]-32;z<=seam[1]+32;z++)for(int x=seam[0]-32;x<=seam[0]+32;x++){
    int a=d.sample(x,z).height()-old.sample(x,z).height();
    for(int[] step:new int[][]{{1,0},{0,1}}){int b=d.sample(x+step[0],z+step[1]).height()-old.sample(x+step[0],z+step[1]).height();worstJump=Math.max(worstJump,Math.abs(a-b));}
   }
   require(worstJump<=4,"Actual secondary shoreline has bounded per-block uplift transition");
   System.out.println("COASTAL_SECONDARY_SHORELINE_PASS seed="+seed+" maxNeighbourOffsetJump="+worstJump);
   int guardedOutlets=0,guardedCells=0;
   outlets:for(int i=0;i<d.size();i++)if(old.river(i)&&old.waterLevel(i)==TerrainShape.SEA_LEVEL){
    for(int n=0;n<=64;n++){double t=n/64.;int x=(int)Math.round(old.edgeX(i,t)),z=(int)Math.round(old.edgeZ(i,t));var frame=CoastalLandforms.frame(seed,shape,x,z);
     if(frame.amplitude()<24||frame.distance()<0||frame.distance()>180||d.headland(x,z).rise()<8)continue;
     for(int dz=-64;dz<=64;dz+=16)for(int dx=-64;dx<=64;dx+=16){var a=old.sample(x+dx,z+dz);var b=d.sample(x+dx,z+dz);require(a.equals(b),"Coastally eligible river mouth and protected bank remain exact");guardedCells++;}
     if(++guardedOutlets>=3)break outlets;break;
    }
   }
   require(guardedOutlets>0,"River guard exercised in a positive coastal-bedrock province");
   System.out.println("COASTAL_RIVER_BUFFER_PASS seed="+seed+" outlets="+guardedOutlets+" unchangedCells="+guardedCells);
   headlandMorphology(seed,p,d,old,shape);
   for(boolean snowy:new boolean[]{false,true}){var beach=blockbranch.realistic.CoastalBeachFixtures.fixture(d,seed,snowy);require(CoastalBeach.contains(beach.x(),beach.z(),d,climate,snowy)&&!StonyShore.contains(beach.x(),beach.z(),d),"Retained true sandy/snowy coast interiors");}
   var low=blockbranch.realistic.StonyShoreFixtures.view(p.x(),d.sample(p.x(),p.z()).height(),p.z(),false,(x,z)->d.sample(x,z).height());var high=blockbranch.realistic.StonyShoreFixtures.view(p.x(),d.sample(p.x(),p.z()).height(),p.z(),true,(x,z)->d.sample(x,z).height());require(Double.isFinite(low.pitch())&&Double.isFinite(high.pitch())&&high.y()>low.y()+50,"Actual-relief camera poses");
   System.out.println("COASTAL_FOUNDATION seed="+seed+" point="+p+" changed="+changed+" rock="+rock+" wetPlatform="+wet+" unchanged="+unchanged+" maxRise="+maxRise+" riverSamples="+river+" globalSamples="+fixed+" codes="+codes+" low="+low+" high="+high);
  }
  System.out.println("COASTAL_BLUFF_BEDROCK_FIXED_CLIMATE_WATER_RIVERS_INLAND_SANDY_SHORES_PASS");
 }
 private static void headlandMorphology(long seed,StonyShore.Point p,DrainageNetwork d,DrainageNetwork old,TerrainShape shape){
  var f=CoastalLandforms.frame(seed,shape,p.x(),p.z());double ax=p.x()-f.nx()*f.distance(),az=p.z()-f.nz()*f.distance();int low=0,raised=0,wet=0,peak=0,total=0;
  for(int along=-1024;along<=1024;along+=8){
   int x=(int)Math.round(ax+f.nx()*40-f.nz()*along),z=(int)Math.round(az+f.nz()*40+f.nx()*along),offset=offset(d,old,x,z);total++;if(offset<=1)low++;if(offset>=12)raised++;peak=Math.max(peak,offset);
   int wx=(int)Math.round(ax-f.nx()*24-f.nz()*along),wz=(int)Math.round(az-f.nz()*24+f.nx()*along);if(d.sample(wx,wz).waterLevel()==TerrainShape.SEA_LEVEL)wet++;
  }
  System.out.println("COASTAL_TRANSECT seed="+seed+" low="+low+" raised="+raised+" wet="+wet+" peak="+peak+" total="+total);
  require(low>total*.25&&raised>0&&raised<total*.65&&wet>total*.20&&peak>=24,"Separated headlands and low/wet coves replace the continuous extrusion");
  int cx=p.x(),cz=p.z(),localMax=0;
  for(int z=cz-200;z<=cz+200;z+=8)for(int x=cx-200;x<=cx+200;x+=8)localMax=Math.max(localMax,offset(d,old,x,z));
  int upper=0,flat=0;for(int z=cz-200;z<=cz+200;z+=8)for(int x=cx-200;x<=cx+200;x+=8){if(offset(d,old,x,z)<localMax*.8)continue;upper++;double dx=(offset(d,old,x+8,z)-offset(d,old,x-8,z))/16.,dz=(offset(d,old,x,z+8)-offset(d,old,x,z-8))/16.;if(Math.hypot(dx,dz)<.08)flat++;}
  require(upper>10&&flat<upper*.20,"Curved crest has no broad planar cap");
  System.out.println("COASTAL_HEADLAND_MORPHOLOGY_PASS seed="+seed+" transect="+total+" lowCoves="+low+" raised="+raised+" wetCoves="+wet+" peak="+peak+" upperCells="+upper+" flatUpperCells="+flat);
 }
 private static int offset(DrainageNetwork d,DrainageNetwork old,int x,int z){return d.sample(x,z).height()-old.sample(x,z).height();}
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private CoastalLandformsCheck(){}
}
