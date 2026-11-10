package blockbranch.realistic;
/** Vanilla Meadow:cool mesic flower-rich open grassland in actual mountain relief. */
public final class MontaneMeadow {
 public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;var cl=c.sample(x,z,d::sample);if(cl.meanTemperatureC()<3||cl.meanTemperatureC()>=12||cl.moisture()<.4||cl.moisture()>.8)return false;return LowlandMeadow.contains(x,z,d,c)&&!OakBirchWoodland.potential(seed,x,z,d,c)&&!WarmWoodland.contains(seed,x,z,d,c)&&!HumidWoodland.contains(seed,x,z,d,c)&&!Swamp.soilContains(seed,x,z,d,c)&&MontaneTerrain.relief(d,x,z)>=180;}
 public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){if(!clear)return 7;long h=OakBirchWoodland.mix(seed^0x4D4F4E54464C4F57L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);double roll=(h>>>11)*0x1.0p-53;double grass=GroundEcology.colonyStrength(seed,0x4D4F4E5447524153L,x,z,28,12,23,site),flowers=GroundEcology.colonyStrength(seed,0x4D4F4E54464C4F52L,x,z,40,9,23,site);double vigor=Math.clamp(.7+.35*site.wetness()+.1*site.retention()-.25*site.exposure(),.35,1);if(roll<(.025+.64*flowers)*vigor/FlowerMicrosites.AVAILABLE_FRACTION&&FlowerMicrosites.accepts(seed,x,z)){int[] species={4,5,6,11,12,13};int dominant=Math.min(5,(int)(MontaneTerrain.patch(seed^0x53504543494553L,x,z,96)*6));int i=Long.remainderUnsigned(h>>>20,5)==0?(int)Long.remainderUnsigned(h>>>28,6):dominant;return species[i];}if(roll<(.2+.7*grass)*vigor){return site.wetness()>.55&&grass>.4&&Long.remainderUnsigned(h>>>32,6)==0?9:8;}return 7;}

 public record Point(int x,int z){}

 private MontaneMeadow(){}
}
