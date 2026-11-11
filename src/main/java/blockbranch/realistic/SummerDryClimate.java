package blockbranch.realistic;

/** Candidate-local winter-rain climatology; not measured rain, PET, weather or a
 * global circulation model. FAO's Mediterranean description motivates the
 * timing, not the coefficients: https://www.fao.org/4/ad652e/ad652e21.htm
 * Warm-belt coordinates are map conventions, not astronomical latitude.
 * Atmospheric supply retains the published annual model-rain scale. No local
 * wet-ground moisture is promoted to rainfall or soil fertility. */
public final class SummerDryClimate {
    static final double BACKGROUND=.08,LOWER_BELT=.25,UPPER_BELT=.65;
    /** Thermal screens 0/18/22C bound this first mild-winter, hot-summer
     * subtype by design; they are not asserted FAO ecological-zone limits. */
    public record Potential(double annualModelRainMm,int dryMonths,boolean summerDry,
                            double coldestC,double warmestC,double winterRain,double summerRain){
        public boolean suitable(){return coldestC>0&&coldestC<18&&warmestC>=22
            &&summerDry&&dryMonths>=3&&dryMonths<=6&&winterRain>summerRain;}
    }
    /** Reuse the existing fixed-westerly 20/60km horizons as finite upwind
     * marine-fetch witnesses. This is neither wind transport nor a rain gauge;
     * unsampled bays, inland Mediterranean climates and east-coast analogs are
     * excluded from this first coastal subtype. */
    static boolean coastalRegime(int x,int z,DrainageNetwork d){
        double belt=Math.abs(z/2097152.);
        if(belt<LOWER_BELT||belt>UPPER_BELT)return false;
        for(int west:new int[]{20000,60000}){var s=d.sample(x-west,z);if(d.habitatOceanWater(x-west,z,s))return true;}
        return false;
    }
    static Potential potential(int x,int z,DrainageNetwork d,ClimateField c){
        return summarize(monthly(DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample)),BACKGROUND),
            temperatures(z,c.sample(x,z,d::sample).meanTemperatureC()),2);
    }
    /** Local phase zero is the warmest climatological month in either
     * hemisphere. Land amplitude 4+16*|z/2097152| is the published convention,
     * not a daily range, a marine seasonal cycle or an empirical station fit. */
    static double[] temperatures(int z,double mean){
        if(!Double.isFinite(mean))throw new IllegalArgumentException("Finite mean required");
        double amplitude=CoolDesert.season(z,mean).warmestC()-mean;
        var t=new double[12];for(int m=0;m<12;m++)t[m]=mean+amplitude*Math.cos(2*Math.PI*m/12);
        return t;
    }
    /** A squared cold-season lobe with .08 background conserves annual rain.
     * Shape/background/belt bounds are generator choices, not observations.
     * It represents a winter-rain regime where the model has marine fetch;
     * neither subtropical highs nor migrating westerlies are simulated. */
    static double[] monthly(double annual,double background){
        if(!Double.isFinite(annual)||annual<0||!Double.isFinite(background)||background<=0)
            throw new IllegalArgumentException("Finite nonnegative total and positive background required");
        var p=new double[12];double sum=0;
        for(int m=0;m<12;m++){double winter=(1-Math.cos(2*Math.PI*m/12))/2;p[m]=background+winter*winter;sum+=p[m];}
        for(int m=0;m<12;m++)p[m]*=annual/sum;
        return p;
    }
    /** FAO's P<=2T dry-month convention, kept separate from the tropical
     * dry-forest definition's <100mm convention. Three to six connected dry
     * months must include the warmest month and remain in the warm half-year.
     * This is a suitability envelope, not a full FAO or Koppen classifier,
     * mass balance, aridity index or a claim of empirical calibration. */
    static Potential summarize(double[] rain,double[] temperature,double dryFactor){
        if(rain.length!=12||temperature.length!=12||!Double.isFinite(dryFactor)||dryFactor<=0)
            throw new IllegalArgumentException("Twelve months and positive finite dry threshold required");
        double annual=0,coldest=Double.POSITIVE_INFINITY,warmest=Double.NEGATIVE_INFINITY,summer=0,winter=0;
        int dry=0,longest=0,run=0;boolean[] isDry=new boolean[12];boolean summerOnly=true;
        for(int m=0;m<12;m++){
            double p=rain[m],t=temperature[m];if(!Double.isFinite(p)||p<0||!Double.isFinite(t))throw new IllegalArgumentException("Finite temperature and nonnegative finite rain required");
            annual+=p;coldest=Math.min(coldest,t);warmest=Math.max(warmest,t);
            boolean warmHalf=m<=2||m>=10;
            // Months 3 and 9 sit at the seasonal mean. Give one to each half
            // explicitly instead of letting floating-point cos(pi/2) decide.
            if(warmHalf||m==3)summer+=p;else winter+=p;
            if(isDry[m]=p<=dryFactor*t){dry++;summerOnly&=warmHalf||m==3;}
        }
        for(int m=0;m<24;m++){run=isDry[m%12]?run+1:0;longest=Math.min(12,Math.max(longest,run));}
        return new Potential(annual,dry,isDry[0]&&summerOnly&&longest==dry,coldest,warmest,winter,summer);
    }
    private SummerDryClimate(){}
}
