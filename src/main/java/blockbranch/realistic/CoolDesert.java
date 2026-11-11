package blockbranch.realistic;

import java.util.LinkedHashMap;
import java.util.function.BiPredicate;

/** A cool continental stony-desert structural analog, not a quantified aridity
 * class. IUCN GET T5.4 describes low grasses/dwarf shrubs on open stony ground;
 * extreme polar/alpine cold deserts are separate. Moisture is the existing
 * unitless suitability proxy, never precipitation/PET, salinity or fertility.
 * https://doi.org/10.2305/IUCN.CH.2020.13.en (profile T5.4, p.65)
 */
public final class CoolDesert {
    public static final String KEY="cool_desert";
    public record Season(double coldestC,double warmestC,int warmMonths) {
        public Season {
            if(!Double.isFinite(coldestC)||!Double.isFinite(warmestC)||coldestC>warmestC||warmMonths<0||warmMonths>12)throw new IllegalArgumentException("Finite ordered temperatures and bounded month count required");
        }
        public boolean growingSeason(){return coldestC<0&&warmMonths>=3;}
    }
    /** Candidate-local idealized land temperature cycle, not SeaIcePolicy's
     * marine range or a daily temperature range. Half-range 4+16*|z/2097152|
     * is an explicit design convention; aggregates describe each local season.
     * A three-month >10 C growing season rejects year-round alpine cold.
     * These thresholds/coefficients are not empirical calibration or an IUCN
     * climate classifier. They never alter annual climate or gameplay weather. */
    public static Season season(int z,double meanC){return season(z,meanC,16);}
    static Season season(int z,double meanC,double gradient){
        if(!Double.isFinite(meanC)||!Double.isFinite(gradient)||gradient<0)throw new IllegalArgumentException("Finite mean and nonnegative range required");
        double amplitude=4+gradient*Math.min(1,Math.abs((double)z)/2097152.);
        int warm=0;for(int m=0;m<12;m++)if(meanC+amplitude*Math.cos(2*Math.PI*m/12)>10)warm++;
        return new Season(meanC-amplitude,meanC+amplitude,warm);
    }
    /** -6/5 C and .12 are existing rockland/steppe/warm-desert seams, chosen
     * to leave their definitions intact, not Earth-derived desert boundaries. */
    public static boolean climateSuitable(int z,ClimateField.Sample c){
        return c.meanTemperatureC()>=-6&&c.meanTemperatureC()<5&&c.moisture()<.12&&season(z,c.meanTemperatureC()).growingSeason();
    }
    static boolean eligible(int x,int z,DrainageNetwork d,ClimateField c){
        return climateSuitable(z,c.sample(x,z,d::sample))&&physicalSupport(x,z,d);
    }
    static boolean physicalSupport(int x,int z,DrainageNetwork d){
        if(LandVegetation.surfaceReserved(d,x,z))return false;
        var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)return false;
        double dx=(d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.,dz=(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.;
        if(Math.hypot(dx,dz)>.30)return false;
        // Support is a geometric proxy, not a soil-type/fertility inference.
        if(!SlopeStability.supportsSoil(x,z,s.height(),dx,dz,(wx,wz)->{var p=d.sample(wx,wz);return new SlopeStability.Site(p.height(),p.waterLevel()==DrainageNetwork.DRY&&!LandVegetation.surfaceReserved(d,wx,wz));}))return false;
        for(int[] p:new int[][]{{2048,0},{-2048,0},{0,2048},{0,-2048}}){var near=d.sample(x+p[0],z+p[1]);if(d.habitatOceanWater(x+p[0],z+p[1],near))return false;}
        return true;
    }
    /** Final-fallback quart ownership is intersected with physical support.
     * Immutable seed fields and bounded caches survive concurrent column reads. */
    static BiPredicate<Integer,Integer> domain(DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> selectedQuart){
        var blocks=new LinkedHashMap<Long,Boolean>(256,.75f,true);var quarts=new LinkedHashMap<Long,Boolean>(256,.75f,true);
        return (x,z)->{
            long key=(x&0xffffffffL)|((long)z<<32);synchronized(blocks){var v=blocks.get(key);if(v!=null)return v;}
            int qx=x>>2,qz=z>>2;long qkey=(qx&0xffffffffL)|((long)qz<<32);Boolean selected;
            synchronized(quarts){selected=quarts.get(qkey);}
            if(selected==null){selected=selectedQuart.test(qx,qz);synchronized(quarts){quarts.put(qkey,selected);if(quarts.size()>2048)quarts.remove(quarts.keySet().iterator().next());}}
            boolean value=selected&&eligible(x,z,d,c);
            synchronized(blocks){blocks.put(key,value);if(blocks.size()>8192)blocks.remove(blocks.keySet().iterator().next());}return value;
        };
    }
    /** Existing desert roles 1..7, including <1 m live shrubs and short grasses.
     * Fine-soil pockets retain vegetation; exposed gravel/pavement remains bare.
     * Retention grades local shelter only, never rainfall, nutrients or salinity.
     * This is a static growing-season mosaic, without snowmelt/fire/grazing or
     * biological soil-crust ecology. No cactus or warm-desert sand mantle. */
    static int cover(long seed,int x,int z,GroundEcology.Site site){
        double colony=GroundEcology.colonyStrength(seed,0x434F4F4C53485255L,x,z,28,8,17,site);
        double shelter=Math.clamp(.55+.30*site.retention()-.35*site.exposure(),.15,.85);
        double roll=(OakBirchWoodland.mix(seed^0x434F4F4C44455345L^x*1297L^z*1237L)>>>11)*0x1.0p-53;
        if(roll<.13*colony*shelter)return 7;
        if(roll<.18*colony*shelter)return 6;
        if(roll<.20*colony*shelter)return 5;
        double pocket=SurfacePatches.sample(seed^0x434F4F4C534F494CL,x,z,24)+.12*site.retention();
        if(pocket>.63)return 4;
        return SurfacePatches.sample(seed^0x434F4F4C47524156L,x,z,13)>.48?2:1;
    }
    private CoolDesert(){}
}
