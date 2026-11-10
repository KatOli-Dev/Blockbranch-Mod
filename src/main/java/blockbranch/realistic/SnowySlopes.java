package blockbranch.realistic;

/** Cold moist mountain faces, with a mean equator-facing exposure proxy rather than seasons. */
public final class SnowySlopes {
 public static boolean contains(int x,int z,DrainageNetwork d,ClimateField c){return surfaceContains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
 /** Material eligibility is evaluated at the actual block, independently of the quart biome palette. */
 static boolean surfaceContains(int x,int z,DrainageNetwork d,ClimateField c){var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)return false;var cl=c.sample(x,z,d::sample);if(cl.meanTemperatureC()<-14||cl.meanTemperatureC()>=-2||cl.moisture()<.35)return false;double dx=(d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.,dz=(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.,slope=Math.hypot(dx,dz);if(slope<=.4||slope>.9)return false;double equatorExposure=Math.max(0,Math.signum(z)*dz/slope);return equatorExposure<=.6&&d.channelAt(x,z).edge()<0;}
 public record Point(int x,int z){}

 private SnowySlopes(){}
}
