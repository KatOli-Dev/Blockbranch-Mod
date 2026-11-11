package blockbranch.realistic;

/** Finite current-field assessment, before adding a new biome or changing terrain. */
public final class CoolDesertSiteSurvey {
    public static void main(String[] args) {
        System.out.println("seed,x,z,height,temperature,moisture,slope,reserved,supported,inland");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true);var c=new ClimateField(seed);
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var s=d.sample(x,z);var q=c.sample(x,z,d::sample);
                if(s.waterLevel()!=DrainageNetwork.DRY||q.meanTemperatureC()<-6||q.meanTemperatureC()>=5||q.moisture()>=.12)continue;
                double dx=(d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.,dz=(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.;
                boolean support=SlopeStability.supportsSoil(x,z,s.height(),dx,dz,(wx,wz)->{var p=d.sample(wx,wz);return new SlopeStability.Site(p.height(),p.waterLevel()==DrainageNetwork.DRY&&!LandVegetation.surfaceReserved(d,wx,wz));});
                boolean inland=true;for(int[] p:new int[][]{{2048,0},{-2048,0},{0,2048},{0,-2048}}){var near=d.sample(x+p[0],z+p[1]);if(d.habitatOceanWater(x+p[0],z+p[1],near))inland=false;}
                System.out.println(seed+","+x+","+z+","+s.height()+","+q.meanTemperatureC()+","+q.moisture()+","+Math.hypot(dx,dz)+","+LandVegetation.surfaceReserved(d,x,z)+","+support+","+inland);
            }
        }
    }
    private CoolDesertSiteSurvey(){}
}
