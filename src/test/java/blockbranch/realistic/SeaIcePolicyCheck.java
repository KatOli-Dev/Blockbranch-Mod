package blockbranch.realistic;

/** Inexpensive thermal audit before any biome or ice generation is changed. */
public final class SeaIcePolicyCheck {
 public static void main(String[]args){
  require(SeaIcePolicy.potential(0,12).coldQuarterC()==12,"No universal winter offset at central belt");
  for(int z:new int[]{0,500000,1500000,2097152})for(double mean:new double[]{-1,0,4,8,18}){
   var p=SeaIcePolicy.potential(z,mean);require(p.equals(SeaIcePolicy.potential(-z,mean)),"Static hemispheres symmetric");
   double sum=0;for(int n=0;n<10000;n++){double phase=(n+.5)/10000.*Math.PI/2-Math.PI/4;sum+=mean-p.halfRangeC()*Math.cos(phase);}
   require(Math.abs(sum/10000-p.coldQuarterC())<1e-6,"Quarter integration matches analytic sinusoid");
   if(mean>=8)require(p.freezingMarginC()<0,"Completed temperate/warm marine climate bands remain outside potential");
  }
  for(int z:new int[]{500000,1500000,2097152}){double critical=-1.8+(2*Math.sqrt(2)/Math.PI)*10*Math.pow(z/2097152.,2);require(SeaIcePolicy.potential(z,critical-.001).freezingMarginC()>0&&SeaIcePolicy.potential(z,critical+.001).freezingMarginC()<0,"Thermal boundary has correct monotonic sign without changing mean climate");}
  for(long seed:new long[]{424242,1,-17}){
   var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);int[] shelf=new int[3],deep=new int[3];int marine=0;double min=100;
   for(int z=-2080000;z<=2080000;z+=32000)for(int x=-2080000;x<=2080000;x+=32000){var s=d.sample(x,z);if(!d.oceanWater(x,z,s)||s.waterLevel()-s.height()-1<2)continue;marine++;double mean=c.sample(x,z,d::sample).meanTemperatureC();min=Math.min(min,mean);
    for(int j=0;j<3;j++)if(SeaIcePolicy.potential(z,mean,8+2*j).freezingMarginC()>0){if(s.waterLevel()-s.height()-1<=200)shelf[j]++;else deep[j]++;}
   }
   require(shelf[1]>0&&deep[1]>0,"Both connected marine depth classes have cold-season potential");
   System.out.println("SEA_ICE_THERMAL_SENSITIVITY seed="+seed+" marine="+marine+" minMeanC="+min+" amplitudes=[8,10,12] shelf="+java.util.Arrays.toString(shelf)+" deep="+java.util.Arrays.toString(deep));
  }
  System.out.println("SEA_ICE_POLICY_UNCHANGED_MEAN_LATITUDE_QUARTER_INTEGRATION_PRESERVED_WARM_BANDS_PASS");
 }
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
