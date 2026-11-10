package blockbranch.realistic;
/** Mountain context measured from the existing heightfield,not a second terrain generator. */
public final class MontaneTerrain {
 public static int relief(DrainageNetwork d,int x,int z){int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;for(int oz=-512;oz<=512;oz+=512)for(int ox=-512;ox<=512;ox+=512){int h=d.sample(x+ox,z+oz).height();min=Math.min(min,h);max=Math.max(max,h);}return max-min;}
 public static double patch(long seed,int x,int z,int scale){int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double a=fade(Math.floorMod(x,scale)/(double)scale),b=fade(Math.floorMod(z,scale)/(double)scale);return(value(seed,gx,gz)*(1-a)+value(seed,gx+1,gz)*a)*(1-b)+(value(seed,gx,gz+1)*(1-a)+value(seed,gx+1,gz+1)*a)*b;}
 private static double value(long s,int x,int z){return(OakBirchWoodland.mix(s^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53;}
 private static double fade(double a){return a*a*a*(a*(a*6-15)+10);}
 private MontaneTerrain(){}
}
