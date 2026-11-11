package blockbranch.realistic;
import java.util.*;
/** Bounded source/terrain assessment only; no production generation changes. */
public final class LandIceAssessment {
    public static void main(String[] args){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true);var c=new ClimateField(seed);
            int cold=0,supply=0;var roughness=new TreeMap<Integer,Integer>();var candidates=new ArrayList<int[]>();
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)continue;
                if(CoolDesert.season(z,c.sample(x,z,d::sample).meanTemperatureC()).warmestC()+4>=-1)continue;cold++;
                if(c.atmosphericSupply(x,z,d::sample)<.45)continue;supply++;candidates.add(new int[]{x,z});
            }
            int witnesses=0;var first=new ArrayList<Object>();
            // Only existing fixed-grid cold/supply candidates, ±1024m stride128.
            // This is not a search for a desired Earth fraction or occupancy.
            var seen=new HashSet<Long>();
            for(var p:candidates)for(int dz=-1024;dz<=1024;dz+=128)for(int dx=-1024;dx<=1024;dx+=128){
                int x=p[0]+dx,z=p[1]+dz;long key=(x&0xffffffffL)|((long)z<<32);if(!seen.add(key))continue;
                var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY||CoolDesert.season(z,c.sample(x,z,d::sample).meanTemperatureC()).warmestC()+4>=-1||c.atmosphericSupply(x,z,d::sample)<.45)continue;
                double sx=(d.sample(x+64,z).height()-d.sample(x-64,z).height())/128.,sz=(d.sample(x,z+64).height()-d.sample(x,z-64).height())/128.;if(Math.hypot(sx,sz)>.20)continue;
                double residual=0;boolean dry=true;
                for(int oz=-128;oz<=128;oz+=32)for(int ox=-128;ox<=128;ox+=32){var q=d.sample(x+ox,z+oz);dry&=q.waterLevel()==DrainageNetwork.DRY&&!LandVegetation.surfaceReserved(d,x+ox,z+oz);residual=Math.max(residual,Math.abs(q.height()-s.height()-sx*ox-sz*oz));}
                if(!dry)continue;roughness.merge((int)Math.ceil(residual/8)*8,1,Integer::sum);
                if(residual<=16){witnesses++;if(first.size()<5)first.add(Map.of("x",x,"z",z,"height",s.height(),"maxPlaneResidual",residual,"slope",Math.hypot(sx,sz),"atmosphericSupply",c.atmosphericSupply(x,z,d::sample)));}
            }
            System.out.println("ICE_ASSESSMENT seed="+seed+" fixedGrid=14400 thermalCold="+cold+" atmosphericSupply45="+supply+" boundedNeighborhoodSamples="+seen.size()+" gentleDryResidualBins="+roughness+" fit16="+witnesses+" first="+first);
        }
    }
}
