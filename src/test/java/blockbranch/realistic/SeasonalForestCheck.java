package blockbranch.realistic;

/** Conservative source-inspired envelope controls, never empirical calibration. */
public final class SeasonalForestCheck {
    public static void main(String[] args){
        require(suitable(20,1300,4,4),"Regional lower thermal/rain bounds");
        require(suitable(30,2000,5,5),"Regional upper thermal/rain bounds");
        require(!suitable(19.99,1600,5,5)&&!suitable(30.01,1600,5,5),"Annual-mean envelope retained");
        require(!suitable(24,1299.99,5,5)&&!suitable(24,2000.01,5,5),"Rain range retained");
        require(!suitable(24,1600,3,3)&&!suitable(24,1600,6,6),"Shorter/longer droughts outside this refinement");
        require(!suitable(24,1600,5,3),"Disconnected dry spells are not one dry season");
        require(!SeasonalForestExpansion.climateSuitable(2097152,new ClimateField.Sample(20,.5),new DrySeasonPolicy.Potential(1600,5,5)),"Freezing winter excluded");
        for(int old=TerrainShape.SHAPE_VERSION;old<=268;old++){
            require(SeasonalForestSchema.schema(old,0)==old&&SeasonalForestSchema.baseSchema(old,0)==old,"Absent retains old definitions");
            require(!SeasonalForestSchema.matches(old,1),"Incomplete/old foundations never promoted");
        }
        require(SeasonalForestSchema.schema(268,1)==269&&SeasonalForestSchema.baseSchema(269,1)==268,"New saved wrapper");
        require(!SeasonalForestSchema.matches(269,0)&&!SeasonalForestSchema.matches(270,1)&&!SeasonalForestSchema.matches(269,2),"Unknown/future/mismatched fields reject");
        System.out.println("SEASONAL_FOREST_ENVELOPE_SCHEMA_PASS");
    }
    private static boolean suitable(double t,double rain,int months,int run){return SeasonalForestExpansion.climateSuitable(0,new ClimateField.Sample(t,.5),new DrySeasonPolicy.Potential(rain,months,run));}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SeasonalForestCheck(){}
}
