package blockbranch.realistic;

/** Static idealized precipitation climatology, not observed rainfall, PET, soil
 * water balance or gameplay weather. Only new dry-forest definitions consume it.
 * NOAA's migrating ITCZ motivates the seasonal shape, not its map calibration:
 * https://www.noaa.gov/jetstream/tropical/convergence-zone
 * The signed warm-belt coordinate is z/2097152, never astronomical degrees.
 * Migration .17, width .10 and background .20 are explicit design conventions:
 * at annual model rain 2100 the equatorial control has no dry months; at .12
 * and 1450 it has one five-month dry season; at .24 it has seven (excluded).
 * These are source-bounded synthetic controls, not fits to real station locations.
 */
public final class DrySeasonPolicy {
    public static final double MIGRATION=.17,WIDTH=.10,BACKGROUND=.20;
    public record Potential(double annualModelRainMm,int dryMonths,int longestDryRun) {
        public Potential {
            if(!Double.isFinite(annualModelRainMm)||annualModelRainMm<0||dryMonths<0||dryMonths>12||longestDryRun<0||longestDryRun>dryMonths)throw new IllegalArgumentException("Finite rain and bounded dry-month summary required");
        }
        /** Narrow regional SDTF precipitation envelope, not all tropical dry
         * forests or the broader FAO GEZ zone. Meir & Pennington (2011):
         * https://www.gov.uk/research-for-development-outputs/climatic-change-and-seasonally-dry-tropical-forests
         * Choose 4–6 consecutive months below 100 mm/model-month and annual
         * rain below 1600 mm/model-year. Disconnected equatorial dry spells,
         * wetter seasonal forest and longer droughts are outside this subtype.
         * No annual P-PET balance or fertility/fire inference is established. */
        public boolean regionalEnvelope(){return annualModelRainMm<1600&&dryMonths>=4&&dryMonths<=6&&longestDryRun==dryMonths;}
    }
    /** Assign a new model scale to unitless atmospheric suitability. .18/.55/.65
     * become approximately 305/1632/2096 mm/model-year: design anchors spanning
     * existing arid/open-warm/humid ranges, not a measured moisture conversion.
     * Local lakes cannot manufacture atmospheric rainfall through wet-ground paint. */
    public static double annualModelRain(double atmosphericSupply) {
        if(!Double.isFinite(atmosphericSupply)||atmosphericSupply<0||atmosphericSupply>1)throw new IllegalArgumentException("Unit atmospheric suitability required");
        return 4000*Math.pow(atmosphericSupply,1.5);
    }
    public static Potential potential(int x,int z,DrainageNetwork d,ClimateField c) {
        double latitude=Math.clamp(z/2097152.,-1,1);
        return summarize(monthly(latitude,annualModelRain(c.atmosphericSupply(x,z,d::sample))));
    }
    /** Twelve climatological months preserve annual total; a six-month phase
     * reversal mirrors hemispheres. No time, mutable weather state or new noise. */
    static double[] monthly(double warmBeltCoordinate,double annualModelRainMm) {
        return monthly(warmBeltCoordinate,annualModelRainMm,MIGRATION,WIDTH,BACKGROUND);
    }
    /** Package-local coefficient controls support sensitivity qualification;
     * saved definition 1 always uses the fixed constants above. */
    static double[] monthly(double warmBeltCoordinate,double annualModelRainMm,double migration,double width,double background) {
        if(!Double.isFinite(warmBeltCoordinate)||Math.abs(warmBeltCoordinate)>1||!Double.isFinite(annualModelRainMm)||annualModelRainMm<0
            ||!Double.isFinite(migration)||migration<0||migration>1||!Double.isFinite(width)||width<=0||width>1||!Double.isFinite(background)||background<0)throw new IllegalArgumentException("Finite bounded coordinate, coefficients and nonnegative model rain required");
        double[] rain=new double[12];double sum=0;
        for(int m=0;m<12;m++){
            double belt=migration*Math.cos(2*Math.PI*(m-5)/12),offset=(warmBeltCoordinate-belt)/width;
            rain[m]=background+Math.exp(-.5*offset*offset);sum+=rain[m];
        }
        if(!(sum>0)||!Double.isFinite(sum))throw new IllegalArgumentException("Nonzero finite rainfall shape required");
        for(int m=0;m<12;m++)rain[m]*=annualModelRainMm/sum;
        return rain;
    }
    static Potential summarize(double[] rain) {
        return summarize(rain,100);
    }
    static Potential summarize(double[] rain,double dryMonthThreshold) {
        if(!Double.isFinite(dryMonthThreshold)||dryMonthThreshold<=0)throw new IllegalArgumentException("Finite positive dry-month threshold required");
        if(rain.length!=12)throw new IllegalArgumentException("Twelve climatological months required");
        double sum=0;int dry=0,longest=0,run=0;
        for(double p:rain){if(!Double.isFinite(p)||p<0)throw new IllegalArgumentException("Finite nonnegative model rain required");sum+=p;if(p<dryMonthThreshold)dry++;}
        for(int m=0;m<24;m++){run=rain[m%12]<dryMonthThreshold?run+1:0;longest=Math.min(12,Math.max(longest,run));}
        return new Potential(sum,dry,longest);
    }
    private DrySeasonPolicy(){}
}
