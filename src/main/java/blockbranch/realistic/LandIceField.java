package blockbranch.realistic;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

/** Prescribed cold-alpine perennial snow/ice snapshots, not mass balance or flow.
 * Warm-season bounds reuse the existing idealized LAND cycle, with an additional
 * bounded +4C warm-spell convention (not a measured daily range or fixed sigma).
 * Unitless atmospheric supply is only a conservative availability screen. Net
 * multiyear deposition sufficient for the prescribed body is assumed; sublimation,
 * radiation, wind redistribution, firn age, meltwater and ablation tongues are not
 * calculated. NSIDC: https://nsidc.org/learn/parts-cryosphere/glaciers/science-glaciers
 * Monthly means alone undercount thaw: https://doi.org/10.3189/2015JoG14J116
 * No rainfall units, empirical calibration, complete T6.1 ecology or ice-sheet
 * coverage follows from these controls. No arbitrary elevation band is imposed. */
public final class LandIceField {
    public static final String KEY="land_ice";
    public static final int RADIUS=128,RISE=32,MAX_DEPTH=64,FIRST_CODE=32,LAST_CODE=FIRST_CODE+MAX_DEPTH-1;
    private record Cap(int height,double dx,double dz,boolean present){}
    private static final Cap ABSENT=new Cap(0,0,0,false);
    private final DrainageNetwork original;
    private final ClimateField climate;
    private final double warmSpell,supplyFloor,maxSlope,maxResidual;
    private final Long2ObjectLinkedOpenHashMap<Cap> caps=new Long2ObjectLinkedOpenHashMap<>(512);
    private final Long2IntLinkedOpenHashMap depths=new Long2IntLinkedOpenHashMap(1024);
    LandIceField(long seed,DrainageNetwork original){this(seed,original,4,.45,.20,16);}
    /** Bounded sensitivity controls; saved definition1 always uses the defaults. */
    LandIceField(long seed,DrainageNetwork original,double warmSpell,double supplyFloor,double maxSlope,double maxResidual){
        if(!Double.isFinite(warmSpell)||warmSpell<0||!Double.isFinite(supplyFloor)||supplyFloor<0||supplyFloor>.9||!Double.isFinite(maxSlope)||maxSlope<=0||maxSlope>1||!Double.isFinite(maxResidual)||maxResidual<=0||maxResidual>RISE)throw new IllegalArgumentException("Bounded ice-model controls required");
        // Preserve this alpine definition's original climate, also in polar worlds.
        this.original=original;climate=new ClimateField(seed);this.warmSpell=warmSpell;this.supplyFloor=supplyFloor;this.maxSlope=maxSlope;this.maxResidual=maxResidual;
    }
    /** Thresholds and ramps are generator conventions, never an accumulation or
     * ablation estimate. Even the added warm spell must remain below -1C; full
     * strength at -3C. Supply .45..55 rejects cold-arid rock without interpreting
     * atmospheric suitability as snowfall or proving a perennial water balance. */
    public static double climateStrength(int z,double mean,double supply){return climateStrength(z,mean,supply,4,.45);}
    static double climateStrength(int z,double mean,double supply,double warmSpell,double supplyFloor){
        if(!Double.isFinite(mean)||!Double.isFinite(supply)||supply<0||supply>1||!Double.isFinite(warmSpell)||warmSpell<0||!Double.isFinite(supplyFloor)||supplyFloor<0||supplyFloor>.9)throw new IllegalArgumentException("Finite bounded climate controls required");
        return smooth((-1-CoolDesert.season(z,mean).warmestC()-warmSpell)/2)*smooth((supply-supplyFloor)/.10);
    }
    private double climateStrength(int x,int z){return climateStrength(z,climate.sample(x,z,original::sample).meanTemperatureC(),climate.atmosphericSupply(x,z,original::sample),warmSpell,supplyFloor);}
    private Cap cap(int gx,int gz){
        long key=(gx&0xffffffffL)|((long)gz<<32);synchronized(caps){var value=caps.getAndMoveToLast(key);if(value!=null)return value;}
        var value=fit(gx*RADIUS,gz*RADIUS);
        synchronized(caps){caps.putAndMoveToLast(key,value);if(caps.size()>4096)caps.removeFirst();}return value;
    }
    private Cap fit(int x,int z){
        var s=original.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY||climateStrength(x,z)==0)return ABSENT;
        double dx=(original.sample(x+64,z).height()-original.sample(x-64,z).height())/128.,dz=(original.sample(x,z+64).height()-original.sample(x,z-64).height())/128.;
        if(Math.hypot(dx,dz)>maxSlope)return ABSENT;
        // A local bed-plane fit, not a glaciological thickness formula. Dense
        // admission retains gentle dry supported footprints rather than using
        // cold mean alone. The16m residual is half the32m prescribed core rise.
        for(int oz=-RADIUS;oz<=RADIUS;oz+=4)for(int ox=-RADIUS;ox<=RADIUS;ox+=4){
            if(ox*ox+oz*oz>RADIUS*RADIUS)continue;var near=original.sample(x+ox,z+oz);
            if(near.height()+MAX_DEPTH>=TerrainShape.CEILING||near.waterLevel()!=DrainageNetwork.DRY||LandVegetation.surfaceReserved(original,x+ox,z+oz)||Math.abs(near.height()-s.height()-dx*ox-dz*oz)>maxResidual)return ABSENT;
        }
        for(int oz=-64;oz<=64;oz+=64)for(int ox=-64;ox<=64;ox+=64)if(climateStrength(x+ox,z+oz)==0)return ABSENT;
        return new Cap(s.height(),dx,dz,true);
    }
    public int thickness(int x,int z){
        long key=(x&0xffffffffL)|((long)z<<32);synchronized(depths){int v=depths.getAndMoveToLast(key);if(v!=0||depths.containsKey(key))return v;}
        var old=original.sample(x,z);double depth=0;
        if(old.waterLevel()==DrainageNetwork.DRY&&!LandVegetation.surfaceReserved(original,x,z)){
            double strength=climateStrength(x,z);
            if(strength>0){int gx=Math.floorDiv(x,RADIUS),gz=Math.floorDiv(z,RADIUS);
                for(int cz=gz-1;cz<=gz+1;cz++)for(int cx=gx-1;cx<=gx+1;cx++){
                    int dx=x-cx*RADIUS,dz=z-cz*RADIUS;double r=Math.hypot(dx,dz)/RADIUS;if(r>=1)continue;var cap=cap(cx,cz);if(!cap.present)continue;
                    // Smooth taper reaches zero at its original bed edge; the
                    // interior approaches a fitted plane32m above the center.
                    // Overlapping snapshots use their union, not additive ice.
                    depth=Math.max(depth,(cap.height+cap.dx*dx+cap.dz*dz+RISE-old.height())*(1-smooth(r))*strength);
                }
            }
        }
        int value=(int)Math.round(Math.clamp(depth,0,MAX_DEPTH));
        synchronized(depths){depths.putAndMoveToLast(key,value);if(depths.size()>16384)depths.removeFirstInt();}return value;
    }
    DrainageNetwork.Sample apply(int x,int z,DrainageNetwork.Sample old){int depth=thickness(x,z);return depth==0?old:new DrainageNetwork.Sample(old.height()+depth,old.waterLevel(),old.basin(),old.closed(),old.bedrockCoast(),old.regionalHeight(),old.regionalWaterLevel(),old.sandyBackshore());}
    public static boolean encoded(int code){return code>=FIRST_CODE&&code<=LAST_CODE;}
    public static int depth(int code){if(!encoded(code))throw new IllegalArgumentException("Invalid land-ice role");return code-FIRST_CODE+1;}
    public static int code(int depth){if(depth<1||depth>MAX_DEPTH)throw new IllegalArgumentException("Invalid land-ice thickness");return FIRST_CODE+depth-1;}
    private static double smooth(double value){double t=Math.clamp(value,0,1);return t*t*t*(t*(t*6-15)+10);}
}
