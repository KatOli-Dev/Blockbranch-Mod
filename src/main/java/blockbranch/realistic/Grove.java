package blockbranch.realistic;
/** Vanilla Grove: sheltered cold montane spruce groups and snowy openings; static site proxies. */
public final class Grove {
 private static boolean terrain(long seed,int x,int z,DrainageNetwork d,ClimateField c){var cl=c.sample(x,z,d::sample);return cl.meanTemperatureC()>=-6&&cl.meanTemperatureC()<-2&&cl.moisture()>=.45&&BorealWoodland.contains(seed,x,z,d,c)&&MontaneTerrain.relief(d,x,z)>=180;}
 /** Regional shelter keeps a broad wooded habitat coherent; local ecological fields shape colonies and snow depth. */
 private static boolean sheltered(DrainageNetwork d,int x,int z){int h=d.sample(x,z).height(),w=d.sample(x-128,z).height(),e=d.sample(x+128,z).height(),n=d.sample(x,z-128).height(),s=d.sample(x,z+128).height();double dx=(e-w)/256.,dz=(s-n)/256.,slope=Math.hypot(dx,dz),sun=slope==0?0:Math.max(0,Math.signum(z)*dz/slope);return sun<.6&&(w+e+n+s)/4.-h>=0;}
 public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;return terrain(seed,x,z,d,c)&&sheltered(d,x,z);}
 public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c,GroundEcology.Field e){return contains(seed,x,z,d,c);}
 static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){var cl=c.sample(x,z,d::sample);return cl.meanTemperatureC()>=-6&&cl.meanTemperatureC()<-2&&cl.moisture()>=.45&&BorealWoodland.surfaceContains(seed,x,z,d,c)&&MontaneTerrain.relief(d,x,z)>=180&&sheltered(d,x,z);}
 public static boolean establishes(long seed,TreeScatter.Point p,GroundEcology.Site site){double group=GroundEcology.colonyStrength(seed,0x47524F5645535441L,p.x(),p.z(),64,18,42,site);double roll=(OakBirchWoodland.mix(p.priority()^0x47524F5645524543L)>>>11)*0x1.0p-53;return group>.08&&roll<(.25+.9*group)*(p.young()?.82:1);}
 public static WoodlandTrees.Root root(TreeScatter.Point p,DrainageNetwork.Sample s){long h=OakBirchWoodland.mix(p.priority()^0x47524F5645545245L);return new WoodlandTrees.Root(p.x(),s.height()+1,p.z(),false,p.young()?5+(int)Long.remainderUnsigned(h,6):11+(int)Long.remainderUnsigned(h,9),(int)(h>>>32),6);}
 public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,boolean covered){return SnowyTaiga.cover(seed^0x47524F564547524FL,x,z,site,clear,covered);}
 public static int[] snowColumn(long seed,int cx,int cz,int[]base,DrainageNetwork d,ClimateField c,GroundEcology.Field e){return SnowyTaiga.snowColumn(seed,cx,cz,base,d,c,e,true);}
 public record Point(int x,int z){}

 private Grove(){}
}
