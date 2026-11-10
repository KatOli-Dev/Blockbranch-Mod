package blockbranch.realistic;

/** Static marine cold-season suitability, not seawater temperature or weather.
 * The 10 C half-range and squared latitude scaling are bounded design conventions,
 * not measured water temperatures. The coldest-quarter mean integrates a sinusoid
 * (2*sqrt(2)/pi); -1.8 C is the normal-salinity seawater freezing reference.
 * Both hemispheres depict their local cold season; annual mean climate is unchanged. */
public final class SeaIcePolicy {
    public static final double SEAWATER_FREEZING_C=-1.8;
    public static final double POLAR_HALF_RANGE_C=10;
    private static final double COLD_QUARTER_FACTOR=2*Math.sqrt(2)/Math.PI;
    public record Potential(double latitudeProxy,double halfRangeC,double coldQuarterC,double freezingMarginC){}
    public static Potential potential(int z,double meanC){return potential(z,meanC,POLAR_HALF_RANGE_C);}
    static Potential potential(int z,double meanC,double polarHalfRange){
        if(!Double.isFinite(meanC)||!Double.isFinite(polarHalfRange)||polarHalfRange<0)throw new IllegalArgumentException("Finite climate and nonnegative seasonal range required");
        double latitude=Math.min(1,Math.abs((double)z)/2097152.0),amplitude=polarHalfRange*latitude*latitude;
        double quarter=meanC-COLD_QUARTER_FACTOR*amplitude;
        return new Potential(latitude,amplitude,quarter,SEAWATER_FREEZING_C-quarter);
    }
    private SeaIcePolicy(){}
}
