package blockbranch.realistic;
import java.util.*;
/** Material identity, physical snow support, existing biome protection and genuinely supported sparse trees. */
public final class ColdUplandsCheck {
 public static void main(String[]args){int snowCaps=0,iceCaps=0;
  for(long seed:new long[]{424242,1,-17}){var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var e=new GroundEcology.Field(d::sample,c);
   for(int kind:new int[]{1,2}){var p=blockbranch.realistic.ColdUplandsFixtures.fixture(d,seed,kind);int owned=0,gravel=0,soil=0,snow=0,ice=0,offQuart=0,layers=0;var codes=new TreeSet<Integer>();
    for(int cz=(p.z()>>4)-3;cz<=(p.z()>>4)+3;cz++)for(int cx=(p.x()>>4)-3;cx<=(p.x()>>4)+3;cx++){
     var overlay=OpenSnowCover.coldPeaksColumn(seed,cx,cz,new int[0],d,c,e);var at=new HashSet<Integer>();for(int b:overlay){require(SnowyTaiga.snow(b&blockbranch.cubic.CanopyLayer.STATE_MASK),"Only native snow-layer codes");at.add(b>>>blockbranch.cubic.CanopyLayer.STATE_BITS);}
     for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z,physical=ColdUplands.surfaceKind(seed,wx,wz,d,c);if(physical!=kind)continue;owned++;
      int code=ColdUplands.cover(seed,wx,wz,kind,d,e);require(ColdUplands.encoded(code),"Bounded cold-material encoding");codes.add(code);
      require(d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY&&d.channelAt(wx,wz).edge()<0,"No water/river displacement");
      require(MountainPeaks.kind(seed,wx,wz,d,c)==0&&WindsweptUplands.kind(seed,wx,wz,d,c)==0&&Badlands.kind(seed,wx,wz,d,c)==0,"Completed peaks, windswept and badlands retained");
      if(code<=10)gravel++;else if(code<=13)soil++;else if(ColdUplands.snowCap(code))snow++;else ice++;
      int key=((d.sample(wx,wz).height()+1-TerrainShape.FLOOR)<<8)|(z*16+x);boolean expected=kind==2&&ColdUplands.snowCap(code)&&OpenSnowCover.layers(seed,wx,wz,e.sample(wx,wz),true)>0;
      require(at.contains(key)==expected,"Snow layers require a physical snow-block cap; exposed packed ice stays bare");if(at.contains(key))layers++;
      if((wx&3)!=0&&ColdUplands.surfaceKind(seed,wx-1,wz,d,c)==kind&&ColdUplands.cover(seed,wx-1,wz,kind,d,e)!=code)offQuart++;
     }
    }
    require(owned>5000,"Natural representative interior");var roots=ColdUplands.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c);
    for(var r:roots){require(ColdUplands.surfaceKind(seed,r.x(),r.z(),d,c)==1&&ColdUplands.soil(seed,r.x(),r.z(),d),"Roots on actual new soil domain");require(WindsweptTrees.supportedFootprint(r.x(),r.z(),d,e.sample(r.x(),r.z()),true),"Physical root support");}
    if(kind==1)require(gravel>3000&&soil>0&&!roots.isEmpty(),"Gravel dominance, soil footholds and sparse supported trees");else{require(roots.isEmpty()&&snow+ice==owned,"Treeless high land-ice cover");snowCaps+=snow;iceCaps+=ice;}
    if(kind==2){var camera=blockbranch.realistic.ColdUplandsFixtures.view(p.x(),d.sample(p.x(),p.z()).height(),p.z(),kind,true,(x,z)->d.sample(x,z).height());require(Double.isFinite(camera.pitch())&&Math.abs(camera.pitch())<35&&camera.y()>=d.sample((int)Math.floor(camera.x()),(int)Math.floor(camera.z())).height()+8,"Frozen overview clear of terrain with bounded downward framing");}
    require(offQuart>0,"Material variations occur away from four-block masks");
    System.out.println("COLD_UPLAND_CHECK seed="+seed+" kind="+kind+" point="+p+" owned="+owned+" gravel="+gravel+" soil="+soil+" snow="+snow+" ice="+ice+" layers="+layers+" roots="+roots.size()+" offQuart="+offQuart+" codes="+codes);
   }
  }
  require(snowCaps>0&&iceCaps>0,"Retained snow and exposed ice both represented across natural witnesses");
  System.out.println("COLD_UPLAND_NATIVE_MATERIAL_SNOW_SUPPORT_SOIL_ROOTS_PHYSICAL_EDGES_PROTECTED_BIOMES_PASS");
 }
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private ColdUplandsCheck(){}
}
