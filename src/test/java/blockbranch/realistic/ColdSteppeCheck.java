package blockbranch.realistic;

/** Dryland climate limits, prior-biome separation, actual terrain and stable transitions. */
public final class ColdSteppeCheck {
    public static void main(String[] args) throws Exception {
        checkColonies();
        for(double t:new double[]{-6,4.999}) for(double m:new double[]{0.12,0.319999}) require(ColdSteppe.climateSuitable(new ClimateField.Sample(t,m)),"Inclusive temperature/lower moisture limits");
        for(var s:new ClimateField.Sample[]{new ClimateField.Sample(-6.01,0.2),new ClimateField.Sample(5,0.2),new ClimateField.Sample(0,0.1199),new ClimateField.Sample(0,0.32)}) require(!ColdSteppe.climateSuitable(s),"Exclude cold, hot, truly arid and mesic climate");
        for(int t=-10;t<=35;t++) for(int m=0;m<=100;m++) {
            var s=new ClimateField.Sample(t,m/100.0);
            if(ColdSteppe.climateSuitable(s)) require(!OakBirchWoodland.climateSuitable(s) && !BorealWoodland.climateSuitable(s) && !DrySteppe.climateSuitable(s) && !WarmWoodland.climateSuitable(s),"Other woodland and warmer steppe climates retained");
        }
        var good=new ClimateField.Sample(0,0.2);var soil=new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false);
        require(ColdSteppe.eligible(soil,0.3,false,false,good),"Inclusive dry slope");
        require(!ColdSteppe.eligible(soil,0.30001,false,false,good),"Reject steep terrain");
        require(!ColdSteppe.eligible(soil,0,true,false,good) && !ColdSteppe.eligible(soil,0,false,true,good),"Specialists and existing meadow retained");
        require(!ColdSteppe.eligible(new DrainageNetwork.Sample(263,266,0,true),0,false,false,good),"Exclude water");
        int[] covers=new int[8];int changed=0;
        for(int z=-128;z<128;z++) for(int x=-128;x<128;x++) {
            int c=ColdSteppe.cover(424242,x,z,true,true);covers[c]++;
            require(c==ColdSteppe.cover(424242,x,z,true,true),"Stable clumps");
            require(ColdSteppe.cover(424242,x,z,true,false)==0 && ColdSteppe.cover(424242,x,z,false,true)==0,"No dry cover in wet or excluded cells");
            if(c!=ColdSteppe.cover(1,x,z,true,true)) changed++;
        }
        for(int c=1;c<=7;c++) require(covers[c]>100,"Every soil/sward/live/dead component present");
        require(covers[4]>18000 && covers[3]>8000 && covers[6]<1000 && covers[7]<1000 && changed>20000,"Dominant bunchgrass, sparse shrubs/dead stems and seeded structure");
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var p=blockbranch.realistic.ColdSteppeFixtures.fixture(drainage,seed);
            require(ColdSteppe.contains(p.x(),p.z(),drainage,climate),"Natural selected steppe");
            int[] actual=new int[8];long started=System.nanoTime();
            for(int z=p.z()-128;z<p.z()+128;z+=2) for(int x=p.x()-128;x<p.x()+128;x+=2) {
                boolean habitat=ColdSteppe.contains(x,z,drainage,climate);
                require(habitat==ColdSteppe.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,drainage,climate),"World-coordinate quart boundaries");
                var s=drainage.sample(x,z);int cover=ColdSteppe.cover(seed,x,z,habitat,s.waterLevel()==DrainageNetwork.DRY && drainage.channelAt(x,z).edge()<0);actual[cover]++;
                if(cover>0)require(!LowlandMeadow.contains(x,z,drainage,climate),"Actual preexisting meadow preserved");
                if(cover>0) require(s.waterLevel()==DrainageNetwork.DRY && ColdSteppe.climateSuitable(climate.sample(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,drainage::sample)),"Actual dry climate and hydrology");
            }
            for(int c=1;c<=7;c++) require(actual[c]>0,"Natural component for seed="+seed+" code="+c);
            for(boolean climateOnly:new boolean[]{false,true}) {
                var transition=blockbranch.realistic.ColdSteppeFixtures.transition(drainage,seed,climateOnly);
                boolean inside=climateOnly ? ColdSteppe.climateSuitable(climate.sample(transition.insideX(),transition.insideZ(),drainage::sample)) : ColdSteppe.contains(transition.insideX(),transition.insideZ(),drainage,climate);
                boolean outside=climateOnly ? ColdSteppe.climateSuitable(climate.sample(transition.outsideX(),transition.outsideZ(),drainage::sample)) : ColdSteppe.contains(transition.outsideX(),transition.outsideZ(),drainage,climate);
                require(inside && !outside && Math.max(Math.abs(transition.insideX()-transition.outsideX()),Math.abs(transition.insideZ()-transition.outsideZ()))<=4,"Adjacent actual climate/terrain transition");
                require(transition.equals(blockbranch.realistic.ColdSteppeFixtures.transition(drainage,seed,climateOnly)),"Deterministic transition search");
            }
            System.out.printf("Cold steppe seed=%d fixture=%s climate=%s cover=%s sampleMillis=%d%n",seed,p,climate.sample(p.x(),p.z(),drainage::sample),java.util.Arrays.toString(actual),(System.nanoTime()-started)/1000000);
        }
        System.out.println("COLD_CONTINENTAL_FESCUE_STEPPE_SHAPE_CLIMATE_PASS");
    }
    /** Frozen main masks protect every non-species cell, including planted density. */
    private static void checkColonies() throws Exception {
        // Main 6a06e49, row-major 256x256 windows: normalize 4 to 3,
        // retain every other code, hash one byte per cell with SHA-256.
        String[] expected={
            "14d278cfe467afada265745fd49763d9a46810abcba0b5c4f5c482edb4e0ecc9",
            "ac3b889977c9a871a1bf3d1ddd58cbea54e7dfe161262af57c9776d0559651ef",
            "e30b92e68cc692badb6275ed4c1f35ccb81df41f4879a956316724b76a4a733f",
            "90b26ba0a36c97934079b1146718c287baa74c653191df7d584f6b8d6ae0448f",
            "1746fedc2adc6bbe845686ce2a7c190d161c3b036681a1b1a668c846def13289",
            "125df341072bcd99fea9df2a75ec42b1917e08467c2f77db640a813fee3b2233",
            "e0f955185316a99593434a3e21c7229d6175aaee3cd6efb43ebb0d47125c5b60",
            "58e973f9bc8c3617775867281a0d47b2c27fa1840717cb90ac9d7c248a2424f4",
            "ad74fdffedc509522bf0837c24deb7c2f5671068f69ad4e3cb41981fee6fa6dc"
        };
        int fixture=0,n=256;
        for(long seed:new long[]{424242,1,-17})for(int[]offset:new int[][]{{0,0},{-137,251},{1207,-2061}}){
            var digest=java.security.MessageDigest.getInstance("SHA-256");int[] covers=new int[n*n];int planted=0,fescue=0;
            for(int z=0;z<n;z++)for(int x=0;x<n;x++){
                int code=ColdSteppe.cover(seed,offset[0]+x,offset[1]+z,true,true);covers[z*n+x]=code;
                if(code==3||code==4){planted++;if(code==4)fescue++;}
                digest.update((byte)(code==4?3:code));
            }
            require(java.util.HexFormat.of().formatHex(digest.digest()).equals(expected[fixture++]),"Frozen main soil, shrub, dead-stem and total planted footprints remain exact");
            double fraction=fescue/(double)planted;require(fraction>.55&&fraction<.67,"Fescue share stays near the established bunchgrass balance");
            double[] rates=new double[3];long[] seam=new long[2],interior=new long[2];int k=0;
            for(int lag:new int[]{1,8,16}){
                long[] rate=new long[2];
                for(int z=0;z<n;z++)for(int x=0;x<n;x++){
                    int from=covers[z*n+x];
                    if(x+lag<n){int to=covers[z*n+x+lag];pair(rate,from,to);if(lag==1)pair(Math.floorMod(offset[0]+x,16)==15?seam:interior,from,to);}
                    if(z+lag<n){int to=covers[(z+lag)*n+x];pair(rate,from,to);if(lag==1)pair(Math.floorMod(offset[1]+z,16)==15?seam:interior,from,to);}
                }
                rates[k++]=rate[0]/(double)rate[1];
            }
            require(rates[0]>fraction+.15&&rates[0]>rates[1]+.15&&rates[0]>rates[2]+.15,"Neighboring fescue forms fine colonies rather than independent scatter or one broad repaint");
            require(Math.abs(seam[0]/(double)seam[1]-interior[0]/(double)interior[1])<.05,"Colony adjacency continues across native chunk seams");
            System.out.printf(java.util.Locale.ROOT,"COLD_STEPPE_COLONY seed=%d offset=%d,%d fraction=%.5f lag1=%.5f lag8=%.5f lag16=%.5f%n",seed,offset[0],offset[1],fraction,rates[0],rates[1],rates[2]);
        }
        System.out.println("COLD_STEPPE_FROZEN_FOOTPRINT_FINE_COLONIES_PASS windows="+fixture);
    }
    private static void pair(long[]rate,int from,int to){if(from==4&&(to==3||to==4)){rate[1]++;if(to==4)rate[0]++;}}
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
