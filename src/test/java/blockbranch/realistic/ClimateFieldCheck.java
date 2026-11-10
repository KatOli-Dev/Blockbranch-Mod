package blockbranch.realistic;

import java.util.function.BiFunction;

/** Regional variation, explicit lapse/exposure/water signals, seams and deterministic reads. */
public final class ClimateFieldCheck {
    public static void main(String[] args) {
        BiFunction<Integer,Integer,DrainageNetwork.Sample> flat=(x,z)->site(63,DrainageNetwork.DRY,false);
        var field=new ClimateField(424242);var sea=field.sample(0,0,flat);
        var elevated=field.sample(0,0,(x,z)->site(1063,DrainageNetwork.DRY,false));
        require(Math.abs(sea.meanTemperatureC()-elevated.meanTemperatureC()-6)<1e-9,"Six degrees per kilometre lapse rate");
        double open=field.sample(1000,2000,flat).moisture();
        double wet=field.sample(1000,2000,(x,z)->site(63,66,true)).moisture();
        require(wet>open,"Nearby actual inland water raises moisture proxy");
        double maritime=field.sample(1000,2000,(x,z)->site(60,64,false)).moisture();
        require(maritime>wet,"Ocean adds maritime contribution as well as wet ground");
        double lee=field.sample(1000,2000,(x,z)->site(x<0 ? 3063 : 63,DrainageNetwork.DRY,false)).moisture();
        require(lee<open,"Western high barrier dries the lee side");
        for(long seed:new long[]{424242,1,-17,Long.MIN_VALUE}) {
            var climate=new ClimateField(seed);double minT=100,maxT=-100,minM=1,maxM=0;int temperate=0,dry=0,cold=0;
            for(int z=-1920000;z<=1920000;z+=32000) for(int x=-1920000;x<=1920000;x+=32000) {
                var s=climate.sample(x,z,flat);require(s.equals(climate.sample(x,z,flat)),"Seeded repeated sample");
                minT=Math.min(minT,s.meanTemperatureC());maxT=Math.max(maxT,s.meanTemperatureC());minM=Math.min(minM,s.moisture());maxM=Math.max(maxM,s.moisture());
                if(s.meanTemperatureC()>=6 && s.meanTemperatureC()<=19 && s.moisture()>=0.48 && s.moisture()<=0.85) temperate++;
                if(s.moisture()<0.35) dry++;if(s.meanTemperatureC()<7) cold++;
                var next=climate.sample(x+1,z+1,flat);
                require(Math.abs(next.meanTemperatureC()-s.meanTemperatureC())<0.01 && Math.abs(next.moisture()-s.moisture())<0.001,"Smooth regional sampling including cell seams");
            }
            require(maxT-minT>15 && maxM-minM>0.4 && temperate>500 && dry>100 && cold>100,"Distinct suitability regions across seeds");
            for(int seam:new int[]{-240000,-64000,-256,-16,0,16,256,64000,240000}) {
                var a=climate.sample(seam-1,seam,flat);var b=climate.sample(seam,seam,flat);
                require(Math.abs(a.meanTemperatureC()-b.meanTemperatureC())<0.01 && Math.abs(a.moisture()-b.moisture())<0.001,"No chunk or negative-grid climate seam");
            }
            System.out.printf("Climate seed=%d temperature=[%.2f,%.2f] moisture=[%.3f,%.3f] temperate=%d dry=%d cool=%d%n",seed,minT,maxT,minM,maxM,temperate,dry,cold);
        }
        BiFunction<Integer,Integer,DrainageNetwork.Sample> wetEdge=(x,z)->site(63,x<0 ? 66 : DrainageNetwork.DRY,true);
        for(int x=-258;x<=258;x++) {
            var a=field.sample(x,32,wetEdge);var b=field.sample(x+1,32,wetEdge);
            require(Math.abs(a.moisture()-b.moisture())<0.002,"Interpolated wet-ground transition across negative and native chunk seams");
        }
        var expected=new SampleHolder[512];
        for(int i=0;i<expected.length;i++) expected[i]=new SampleHolder(field.sample(i*113-30000,i*71-10000,flat));
        java.util.stream.IntStream.range(0,expected.length).parallel().forEach(i->require(expected[i].value().equals(field.sample(i*113-30000,i*71-10000,flat)),"Parallel immutable climate reads"));
        require(!sea.equals(new ClimateField(1).sample(0,0,flat)),"Different seed changes regional climate");
        System.out.println("REGIONAL_CLIMATE_FIELD_PASS");
    }
    private record SampleHolder(ClimateField.Sample value) {}
    private static DrainageNetwork.Sample site(int height,int water,boolean closed) { return new DrainageNetwork.Sample(height,water,0,closed); }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
