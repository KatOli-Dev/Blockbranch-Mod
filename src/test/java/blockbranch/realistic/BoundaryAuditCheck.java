package blockbranch.realistic;

import java.util.*;

/** Compare existing cutoffs with climate-led common vegetation before changing production selection. */
public final class BoundaryAuditCheck {
    static boolean meadow(DrainageNetwork d,int x,int z) { int e=d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;if(e<20 || e>900)return false;return LowlandMeadow.contains(x,z,(wx,wz)->{var t=d.sample(wx,wz);return new LowlandMeadow.Site(t.height(),t.waterLevel(),d.channelAt(wx,wz).edge()>=0);}); }
    static boolean ground(DrainageNetwork d,int x,int z,double maxSlope) {
        var t=d.sample(x,z);if(t.waterLevel()!=DrainageNetwork.DRY || d.channelAt(x,z).edge()>=0)return false;
        return Math.hypot((d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.0,(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.0)<=maxSlope;
    }
    public static void main(String[] args) {
        for(long seed:new long[]{424242,1,-17}) {
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);int land=0,base=0,corrected=0,low=0,upland=0,warmGap=0,coldGap=0,dryGap=0,warmCandidate=0,warmPreviouslyAssigned=0;
            var residual=new TreeMap<String,Integer>();
            for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000) {
                var t=d.sample(x,z);if(t.waterLevel()!=DrainageNetwork.DRY)continue;land++;var climate=c.sample(x,z,d::sample);
                boolean niche=InlandLakeReedbed.contains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz)) || AlpineScree.contains(x,z,d::sample) || GravelRiverMargin.contains(x,z,(wx,wz)->GravelRiverMargin.site(d,wx,wz)) || LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(d,wx,wz));
                int elevation=t.height()+1-TerrainShape.SEA_LEVEL;
                boolean existing=niche || elevation>=60 && elevation<=900 && OakBirchWoodland.potential(seed,x,z,d,c) || OakBirchWoodland.meadowClimateSuitable(climate) && meadow(d,x,z) || DrySteppe.contains(x,z,d,c) || BorealWoodland.contains(seed,x,z,d,c);
                boolean next=existing || OakBirchWoodland.climateSuitable(climate) && OakBirchWoodland.stand(seed,x,z)>=0.55 && ground(d,x,z,0.35) || OakBirchWoodland.meadowClimateSuitable(climate) && ground(d,x,z,0.20);
                if(existing)base++;if(next)corrected++;
                boolean selected=OakBirchWoodland.potential(seed,x,z,d,c) || WarmWoodland.contains(seed,x,z,d,c) || OakBirchWoodland.meadowClimateSuitable(climate) && LowlandMeadow.contains(x,z,(wx,wz)->LowlandMeadow.site(d,wx,wz)) || DrySteppe.contains(x,z,d,c) || BorealWoodland.contains(seed,x,z,d,c);
                if(!niche && existing && !selected)throw new AssertionError("Production selectors lost previously covered land");
                if(niche && WarmWoodland.contains(seed,x,z,d,c))throw new AssertionError("Warm woodland overwrote a specialist");
                if(next && !existing) {int e=t.height()+1-TerrainShape.SEA_LEVEL;if(e<20)low++;if(e>900)upland++;}
                boolean candidate=climate.meanTemperatureC()>19 && climate.meanTemperatureC()<=27 && climate.moisture()>=0.35 && climate.moisture()<=0.72 && ground(d,x,z,0.35) && !niche && !DrySteppe.contains(x,z,d,c);
                if(candidate) {warmCandidate++;if(next)warmPreviouslyAssigned++;}
                if(next)continue;
                if(climate.meanTemperatureC()>19)warmGap++;else if(climate.meanTemperatureC()<3)coldGap++;if(climate.moisture()<0.27)dryGap++;
                String key=climate.meanTemperatureC()>19 ? "warm" : climate.meanTemperatureC()<3 ? "cold" : "temperate";key+="/"+(climate.moisture()<0.27 ? "dry" : climate.moisture()>0.85 ? "wet" : "intermediate");residual.merge(key,1,Integer::sum);
            }
            System.out.printf(Locale.ROOT,"BOUNDARY_AUDIT seed=%d land=%d baselineAssigned=%d correctedAssigned=%d gained=%d gainedLow=%d gainedUpland=%d remainingWarm=%d remainingCold=%d remainingDry=%d warmWoodCandidate=%d alreadyAssignedCandidate=%d residual=%s%n",seed,land,base,corrected,corrected-base,low,upland,warmGap,coldGap,dryGap,warmCandidate,warmPreviouslyAssigned,residual);
        }
    }
    private BoundaryAuditCheck() {}
}
