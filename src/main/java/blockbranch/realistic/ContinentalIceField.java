package blockbranch.realistic;

/** Prescribed grounded polar-ice snapshot, not ice flow or a balance calculation.
 * https://nsidc.org/learn/parts-cryosphere/ice-sheets describes broad ice bodies
 * burying bed relief; the 16 km smoothing mesh, 768 m surface rise and 2048 m
 * maximum stored thickness below are generator geometry conventions, not estimates.
 * The thermal/supply screen assumes prior multiyear net deposition sufficient for
 * that snapshot. It does not predict accumulation, sublimation, equilibrium lines,
 * ablation tongues or the observed distribution of continental sheets.
 *
 * Land-terminating subtype: connected marine water is never occupied. A smooth
 * 64 m bed-height coastal taper retains an exposed shore, rather than fabricating
 * tidewater grounding lines, floating shelves or calving. Inland standing water
 * and channels inside a positive body are buried/replaced by solid ice; their
 * original solved graph/data survive in iceReference(), not as an exposed river.
 * Sheet admission uses the sea-level thermal regime (remove the existing lapse
 * term), so a cold mountain in otherwise nonpolar climate cannot masquerade as
 * a continental sheet. There is no elevation ceiling: polar plateaus may qualify.
 * Legacy alpine caps keep their separate original climate/geometry.
 * Bare summits above the prescribed surface remain nunataks. No dense radius
 * scan or additional drainage graph is built. Exact cache entries are bounded. */
public final class ContinentalIceField {
    public static final String KEY="continental_ice";
    public static final int BED_STEP=16384,RISE=768,MAX_DEPTH=2048;
    private record Cached(long key,int depth,boolean lid){}
    private final Cached[] cache=new Cached[1<<15];
    private final DrainageNetwork bed;
    private final ClimateField climate;
    private final int rise;private final double coolingScale;
    ContinentalIceField(long seed,DrainageNetwork bed){this(seed,bed,RISE,1);}
    /** Finite sensitivity controls only. Saved definition1 always uses the constants. */
    ContinentalIceField(long seed,DrainageNetwork bed,int rise,double coolingScale){
        if(rise<RISE/2||rise>RISE*2||!Double.isFinite(coolingScale)||coolingScale<.9||coolingScale>1.1)throw new IllegalArgumentException("Bounded ice sensitivity controls required");
        this.bed=bed;this.climate=new ClimateField(seed,true);this.rise=rise;this.coolingScale=coolingScale;
    }
    public int thickness(int x,int z){return PolarClimate.latitude(z)<=PolarClimate.START?0:entry(x,z).depth;}
    private Cached entry(int x,int z){
        long key=(x&0xffffffffL)|((long)z<<32);int slot=(int)OakBirchWoodland.mix(key)&(cache.length-1);var hit=cache[slot];
        if(hit!=null&&hit.key==key)return hit;
        int depth=calculate(x,z);var value=new Cached(key,depth,depth==0&&calculateLid(x,z));cache[slot]=value;return value;
    }
    private int calculate(int x,int z){
        var s=bed.sample(x,z);if(bed.habitatOceanWater(x,z,s))return 0;
        double lowlandMean=climate.sample(x,z,bed::sample).meanTemperatureC()+.006*Math.max(0,s.regionalHeight()+1-TerrainShape.SEA_LEVEL);
        if(coolingScale!=1)lowlandMean-=PolarClimate.cooling(PolarClimate.latitude(z))*(coolingScale-1);
        double strength=LandIceField.climateStrength(z,lowlandMean,climate.atmosphericSupply(x,z,bed::sample));
        if(strength==0)return 0;
        int surface=s.waterLevel()==DrainageNetwork.DRY?s.height():Math.max(s.height(),s.waterLevel()-1);
        if(surface-s.height()>=MAX_DEPTH||surface<TerrainShape.SEA_LEVEL-1)return 0;
        double coast=PolarClimate.smooth((surface-(TerrainShape.SEA_LEVEL-1))/64.);
        if(coast==0)return 0;
        double rise=Math.max(0,smoothedBed(x,z)+this.rise-surface)*strength*coast;
        int above=(int)Math.round(rise);if(above==0)return 0;
        return Math.min(Math.min(MAX_DEPTH,surface-s.height()+above),TerrainShape.CEILING-1-s.height());
    }
    private double smoothedBed(int x,int z){
        int gx=Math.floorDiv(x,BED_STEP)*BED_STEP,gz=Math.floorDiv(z,BED_STEP)*BED_STEP;
        double tx=PolarClimate.smooth(Math.floorMod(x,BED_STEP)/(double)BED_STEP),tz=PolarClimate.smooth(Math.floorMod(z,BED_STEP)/(double)BED_STEP);
        double a=bed.sample(gx,gz).height(),b=bed.sample(gx+BED_STEP,gz).height(),c=bed.sample(gx,gz+BED_STEP).height(),d=bed.sample(gx+BED_STEP,gz+BED_STEP).height();
        return (a+(b-a)*tx)*(1-tz)+(c+(d-c)*tx)*tz;
    }
    DrainageNetwork.Sample apply(int x,int z,DrainageNetwork.Sample old){int h=thickness(x,z);return h==0?old:new DrainageNetwork.Sample(old.height()+h,DrainageNetwork.DRY,old.basin(),old.closed(),false,old.regionalHeight(),old.regionalWaterLevel(),false);}
    /** A permanent one-block static lid on otherwise exposed inland water; no
     * lake-ice thickness estimate or annual freeze/thaw simulation is claimed. */
    boolean freshwaterLid(int x,int z){return PolarClimate.latitude(z)>PolarClimate.START&&entry(x,z).lid;}
    private boolean calculateLid(int x,int z){
        var s=bed.sample(x,z);if(s.waterLevel()<=s.height()+1||bed.habitatOceanWater(x,z,s))return false;
        return CoolDesert.season(z,climate.sample(x,z,bed::sample).meanTemperatureC()).warmestC()+4< -1;
    }
}
