package blockbranch.realistic;

/** Dryland climate limits, prior-biome separation, actual terrain and stable transitions. */
public final class DrySteppeCheck {
    public static void main(String[] args) {
        for(double t:new double[]{5,24}) for(double m:new double[]{0.12,0.269999}) require(DrySteppe.climateSuitable(new ClimateField.Sample(t,m)),"Inclusive temperature/lower moisture limits");
        for(var s:new ClimateField.Sample[]{new ClimateField.Sample(4.99,0.2),new ClimateField.Sample(24.01,0.2),new ClimateField.Sample(12,0.1199),new ClimateField.Sample(12,0.27)}) require(!DrySteppe.climateSuitable(s),"Exclude cold, hot, truly arid and mesic climate");
        for(int t=-10;t<=35;t++) for(int m=0;m<=100;m++) {
            var s=new ClimateField.Sample(t,m/100.0);
            if(DrySteppe.climateSuitable(s)) require(!OakBirchWoodland.climateSuitable(s) && !OakBirchWoodland.meadowClimateSuitable(s),"No climate overlap with prior woodland/meadow");
        }
        var seam=new ClimateField.Sample(12,0.27);require(OakBirchWoodland.meadowClimateSuitable(seam),"The moisture transition meets meadow without a gap");
        int[] covers=new int[7];int changed=0;
        for(int z=-128;z<128;z++) for(int x=-128;x<128;x++) {
            int c=DrySteppe.cover(424242,x,z,true,true);covers[c]++;
            require(c==DrySteppe.cover(424242,x,z,true,true),"Stable clumps");
            require(DrySteppe.cover(424242,x,z,true,false)==0 && DrySteppe.cover(424242,x,z,false,true)==0,"No dry cover in wet or excluded cells");
            if(c!=DrySteppe.cover(1,x,z,true,true)) changed++;
        }
        for(int c=1;c<=6;c++) require(covers[c]>100,"Every soil/sward/live/dead component present");
        require(covers[1]>20000 && covers[4]<3000 && changed>20000,"Open soil, sparse shrubs and seed-sensitive structure");
        verifyGrassComposition();
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var p=blockbranch.realistic.DrySteppeFixtures.fixture(drainage,seed);
            require(DrySteppe.contains(p.x(),p.z(),drainage,climate),"Natural selected steppe");
            require(!DrySteppe.contains(p.x(),p.z(),(x,z)->new DrainageNetwork.Sample(263,266,0,true),(x,z)->false,climate),"Exclude lakes");
            require(!DrySteppe.contains(p.x(),p.z(),drainage::sample,(x,z)->true,climate),"Exclude routed channels");
            require(!DrySteppe.contains(p.x(),p.z(),(x,z)->new DrainageNetwork.Sample(263+x-p.x(),DrainageNetwork.DRY,0,false),(x,z)->false,climate),"Exclude steep ground");
            int[] actual=new int[7];long started=System.nanoTime();
            for(int z=p.z()-32;z<p.z()+32;z++) for(int x=p.x()-32;x<p.x()+32;x++) {
                boolean habitat=DrySteppe.contains(x,z,drainage,climate);
                require(habitat==DrySteppe.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,drainage,climate),"World-coordinate quart boundaries");
                var s=drainage.sample(x,z);int cover=DrySteppe.cover(seed,x,z,habitat,s.waterLevel()==DrainageNetwork.DRY && drainage.channelAt(x,z).edge()<0);actual[cover]++;
                if(cover>0) require(s.waterLevel()==DrainageNetwork.DRY && DrySteppe.climateSuitable(climate.sample(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,drainage::sample)),"Actual dry climate and hydrology");
            }
            for(int c=1;c<=6;c++) require(actual[c]>0,"Natural component for seed="+seed+" code="+c);
            for(boolean climateOnly:new boolean[]{false,true}) {
                var transition=blockbranch.realistic.DrySteppeFixtures.transition(drainage,seed,climateOnly);
                boolean inside=climateOnly ? DrySteppe.climateSuitable(climate.sample(transition.insideX(),transition.insideZ(),drainage::sample)) : DrySteppe.contains(transition.insideX(),transition.insideZ(),drainage,climate);
                boolean outside=climateOnly ? DrySteppe.climateSuitable(climate.sample(transition.outsideX(),transition.outsideZ(),drainage::sample)) : DrySteppe.contains(transition.outsideX(),transition.outsideZ(),drainage,climate);
                require(inside && !outside && Math.max(Math.abs(transition.insideX()-transition.outsideX()),Math.abs(transition.insideZ()-transition.outsideZ()))<=4,"Adjacent actual climate/terrain transition");
                require(transition.equals(blockbranch.realistic.DrySteppeFixtures.transition(drainage,seed,climateOnly)),"Deterministic transition search");
            }
            System.out.printf("Steppe seed=%d fixture=%s climate=%s cover=%s sampleMillis=%d%n",seed,p,climate.sample(p.x(),p.z(),drainage::sample),java.util.Arrays.toString(actual),(System.nanoTime()-started)/1000000);
        }
        System.out.println("DRY_CONTINENTAL_SCRUB_STEPPE_SHAPE_CLIMATE_PASS");
    }
    /** Frozen prior selector is a placement and uniform-scatter negative control. */
    private static int legacy(long seed,int x,int z){
        double patch=SurfacePatches.sample(seed^0x4452595354455050L,x,z,18);
        if(patch<.28)return 2;if(patch>.72)return 5;
        long cell=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);int group=(int)(cell&31);
        return group<10?3:group==10?4:group==11&&((cell>>>8)&1)==0?6:1;
    }
    private static boolean grassSite(int code){return code==1||code==3;}
    private static double clustering(double[] edges){return edges[3]*edges[0]/(edges[1]*edges[2]);}
    private static void verifyGrassComposition(){
        for(long seed:new long[]{424242,1,-17}){
            int oldGrass=0,newGrass=0,reassigned=0;double[][] edges=new double[2][4];
            for(int z=-192;z<192;z++)for(int x=-192;x<192;x++){
                int old=legacy(seed,x,z),now=DrySteppe.cover(seed,x,z,true,true);
                require(grassSite(old)?grassSite(now):old==now,"Only existing bare-soil and bunchgrass roles may change");
                if(old==3)oldGrass++;if(now==3)newGrass++;if(old!=now)reassigned++;
                for(int axis=0;axis<2;axis++){
                    int nx=x+(axis==0?1:0),nz=z+(axis==1?1:0),oldNext=legacy(seed,nx,nz);
                    if(!grassSite(old)||!grassSite(oldNext))continue;
                    int next=DrySteppe.cover(seed,nx,nz,true,true);
                    for(int variant=0;variant<2;variant++){
                        boolean a=(variant==0?old:now)==3,b=(variant==0?oldNext:next)==3;
                        var e=edges[variant];e[0]++;if(a)e[1]++;if(b)e[2]++;if(a&&b)e[3]++;
                    }
                }
            }
            double before=clustering(edges[0]),after=clustering(edges[1]);
            require(reassigned>10000&&newGrass>oldGrass*.8&&newGrass<oldGrass*1.2,"Meaningful redistribution retains overall grass/soil balance");
            require(before<1.08&&after>before+.15,"Adjacent grass forms clumps beyond the uniform-scatter control");
            System.out.printf("DRY_STEPPE_GRASS_COMPOSITION seed=%d oldGrass=%d newGrass=%d reassigned=%d adjacentRatio=%.3f->%.3f%n",seed,oldGrass,newGrass,reassigned,before,after);
        }
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
