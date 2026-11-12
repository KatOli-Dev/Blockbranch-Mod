package blockbranch.realistic;

import java.util.Arrays;

/** Source-bounded climate counterexamples, finite physical witnesses and
 * structural separation from grass steppe, not empirical biome calibration. */
public final class WarmXericCheck {
    public static void main(String[] args){
        require(WarmXericShrubland.climateSuitable(100,.01),"Lower model rain bound and mild winter");
        require(WarmXericShrubland.climateSuitable(299.99,15),"Warm semidesert control");
        require(!WarmXericShrubland.climateSuitable(99.99,15),"Very dry desert retains ownership");
        require(!WarmXericShrubland.climateSuitable(300,15),"Wetter steppe/savanna retains ownership");
        require(!WarmXericShrubland.climateSuitable(200,0),"Freezing winter excluded");
        require(!WarmXericShrubland.climateSuitable(Double.NaN,15),"Invalid model rain rejected");
        require(!WarmXericShrubland.climateSuitable(200,Double.POSITIVE_INFINITY),"Invalid thermal input rejected");
        for(int old=TerrainShape.SHAPE_VERSION;old<=267;old++){
            require(WarmXericSchema.schema(old,0)==old&&WarmXericSchema.baseSchema(old,0)==old,"Absent retains old schema");
            require(!WarmXericSchema.matches(old,1),"No incomplete/old foundation promoted");
        }
        require(WarmXericSchema.schema(267,1)==268&&WarmXericSchema.baseSchema(268,1)==267,"Complete saved wrapper");
        require(!WarmXericSchema.matches(268,0)&&!WarmXericSchema.matches(269,1)&&!WarmXericSchema.matches(268,2),"Malformed/future definitions reject");
        var d=new DrainageNetwork(new TerrainShape(424242),424242,true,true,true,true,true,true,true);
        var c=new ClimateField(424242,true);
        require(WarmXericShrubland.eligible(432000,-784000,d,c),"Existing warm dry steppe witness supported");
        require(!WarmXericShrubland.eligible(400000,-944000,d,c),"Marine outlet cannot become shrubland");
        require(!WarmXericShrubland.eligible(-1552000,-1904000,d,c),"Polar sheet cannot become shrubland");
        require(!WarmXericShrubland.domain(d,c,(x,z)->false).test(432000,-784000),"Physical eligibility cannot override quart ownership");
        var selected=WarmXericShrubland.domain(d,c,(x,z)->true);
        require(!selected.test(400000,-944000),"Quart ownership cannot override marine exclusion");
        for(int[] center:new int[][]{{432000,-784000},{399776,-784512},{400000,-944000},{-1552000,-1904000}})
            for(int z=center[1]-16;z<=center[1]+16;z+=4)for(int x=center[0]-16;x<=center[0]+16;x+=4)
                require(CoolDesert.physicalSupport(x,z,d)==CoolDesert.physicalSupport(x,z,d,true),"Marine negative bound matches full physical support in dry, mixed, marine and polar windows");
        int[] count=new int[8];int samples=257*257;
        for(int z=-128;z<=128;z++)for(int x=-128;x<=128;x++){
            int code=WarmXericShrubland.cover(424242,x,z,GroundEcology.NEUTRAL);
            require(code==WarmXericShrubland.cover(424242,x,z,GroundEcology.NEUTRAL),"Deterministic cover");count[code]++;
        }
        require(count[0]==0&&count[7]>samples*.10&&count[7]<samples*.30,"Open low shrub cover");
        require(count[7]>count[6]*4,"Shrubs dominate live cover over dry grass");
        require(count[1]+count[2]+count[3]+count[4]>samples*.65,"Exposed mineral matrix");
        System.out.println("WARM_XERIC_CLIMATE_PHYSICAL_SCHEMA_COVER_PASS "+Arrays.toString(count));
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private WarmXericCheck(){}
}
