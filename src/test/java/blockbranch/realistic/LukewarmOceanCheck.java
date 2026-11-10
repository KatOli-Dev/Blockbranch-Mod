package blockbranch.realistic;

/** Natural marine thermal/depth partitions and block-resolution photic meadow controls. */
public final class LukewarmOceanCheck {
    public static void main(String[] args) {
        for (double temperature : new double[] {-2,8,17.999,18,23.999,24,27}) {
            var c = new ClimateField.Sample(temperature,.6);
            require(LukewarmOcean.climate(c)==(temperature>=18&&temperature<24),"Exact half-open warm band");
            require(!(LukewarmOcean.climate(c)&&TemperateOcean.climate(c)),"Prior temperate band retained");
        }
        for (long seed : new long[] {424242,1,-17}) {
            var d=new DrainageNetwork(new TerrainShape(seed),seed); var c=new ClimateField(seed);
            var p=blockbranch.realistic.LukewarmOceanFixtures.fixture(d,seed); int oldPlants=0,plants=0,added=0,thinned=0,offQuart=0;
            long water=1;
            for(int z=p.z()-64;z<=p.z()+64;z++) for(int x=p.x()-64;x<=p.x()+64;x++) {
                var site=MarineShelf.site(d,x,z);var climate=c.sample(x,z,d::sample);
                int before=MarineShelf.cover(seed,x,z,MarineShelf.surfaceContains(x,z,d),site,climate);
                int after=LukewarmOcean.cover(seed,x,z,d,c);
                boolean domain=LukewarmOcean.surfaceContains(seed,x,z,d,c);
                if(!domain){require(after==0,"No warm paint outside exact marine domain");continue;}
                require(!TemperateOcean.surfaceContains(seed,x,z,d,c),"Completed Ocean material domain unchanged");
                require(after==before||(after==12&&before==7)||(after==7&&before==12),"Only grass presence changes; sandy substrate is identical");
                if(before==12){oldPlants++;if(after==7)thinned++;}
                if(after==12){plants++;require(MarineShelf.plantSuitable(site,climate),"Exact photic depth, slope and climate");}
                if(after==12&&before==7)added++;
                if(domain!=LukewarmOcean.contains(seed,x,z,d,c))offQuart++;
                var sample=d.sample(x,z);water=31*water+sample.height();water=31*water+sample.waterLevel();
                require(after==LukewarmOcean.cover(seed,x,z,d,c),"Deterministic cache/query order");
            }
            require(added>20&&thinned>20&&plants>20,"Varied meadow with thinned overlapping colonies and open sand");
            placement(seed);
            var deep=blockbranch.realistic.DeepLukewarmOceanFixtures.fixture(d,seed);int fine=0,clay=0;
            for(int z=deep.z()-64;z<=deep.z()+64;z+=8)for(int x=deep.x()-64;x<=deep.x()+64;x+=8){
                require(DeepLukewarmOcean.contains(x,z,d,c)&&!LukewarmOcean.contains(seed,x,z,d,c),"Deep and shallow identity exclusive");
                int code=DeepOcean.cover(seed,x,z,true,DeepOcean.site(d,x,z));
                require(code>=5&&code<=9&&code!=7,"Unchanged aphotic sediment/rock without plants");
                if(code==5)fine++;if(code==6)clay++;
            }
            require(fine>0&&clay>0,"Natural deep sediment contrast");
            var edge=blockbranch.realistic.DeepLukewarmOceanFixtures.shelfTransition(d,seed);
            require(LukewarmOcean.contains(seed,edge.insideX(),edge.insideZ(),d,c)
                &&DeepLukewarmOcean.contains(edge.outsideX(),edge.outsideZ(),d,c),"Natural adjacent canonical 200m depth edge");
            for(boolean upper:new boolean[]{false,true}){
                var t=DeepLukewarmOcean.thermalTransition(d,seed,upper);
                require(DeepLukewarmOcean.contains(t.insideX(),t.insideZ(),d,c)&&!DeepLukewarmOcean.contains(t.outsideX(),t.outsideZ(),d,c),"Actual adjacent thermal edge");
                require(Math.max(Math.abs(t.insideX()-t.outsideX()),Math.abs(t.insideZ()-t.outsideZ()))<=4,"Adjacent quart cells");
                if(!upper)require(TemperateDeepOcean.contains(t.outsideX(),t.outsideZ(),d,c),"Existing Deep Ocean retained across 18C edge");
            }
            var lake=blockbranch.realistic.LakeSedimentFixtures.fixture(d,seed);var river=blockbranch.realistic.RiverChannelFixtures.fixture(d,seed);
            require(!LukewarmOcean.contains(seed,lake.x(),lake.z(),d,c)&&!DeepLukewarmOcean.contains(lake.x(),lake.z(),d,c),"Lake exclusion");
            require(!LukewarmOcean.contains(seed,river.x(),river.z(),d,c)&&!DeepLukewarmOcean.contains(river.x(),river.z(),d,c),"River exclusion");
            System.out.println("LUKEWARM_OCEAN seed="+seed+" shallow="+p+" T="+c.sample(p.x(),p.z(),d::sample).meanTemperatureC()+" priorGrass="+oldPlants+" grass="+plants+" added="+added+" thinned="+thinned+" offQuart="+offQuart+" deep="+deep+" fine/clay="+fine+"/"+clay+" edge="+edge+" sampledHydrology="+water);
        }
        System.out.println("LUKEWARM_OCEAN_THERMAL_PHOTIC_MEADOW_DEEP_BOUNDARY_PASS");
    }
    private static void placement(long seed) {
        int plants=0,neighborPairs=0;int[] cells={4,12,16,40},edges=new int[4],edgeChanges=new int[4],inner=new int[4],innerChanges=new int[4];
        for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
            boolean a=LukewarmOcean.seagrass(seed,x,z),east=LukewarmOcean.seagrass(seed,x+1,z),south=LukewarmOcean.seagrass(seed,x,z+1);
            require(a==LukewarmOcean.seagrass(seed,x,z),"Deterministic absolute-coordinate tuft placement");
            if(a){plants++;if(east)neighborPairs++;if(south)neighborPairs++;}
            for(int i=0;i<cells.length;i++)for(int axis=0;axis<2;axis++){
                boolean boundary=Math.floorMod(axis==0?x+1:z+1,cells[i])==0,changed=a!=(axis==0?east:south);
                if(boundary){edges[i]++;if(changed)edgeChanges[i]++;}else{inner[i]++;if(changed)innerChanges[i]++;}
            }
        }
        double density=plants/65536.0;
        require(density>.04&&density<.20,"Sparse variable colonies retain open water rather than a dense carpet");
        require(neighborPairs/(2.0*plants)>density*1.2,"Tufts cluster locally rather than uniform independent scatter");
        for(int i=0;i<cells.length;i++)require(Math.abs(edgeChanges[i]/(double)edges[i]-innerChanges[i]/(double)inner[i])<.035,"No excess placement edges at quart/chunk/colony lattice scale "+cells[i]);
        System.out.println("LUKEWARM_TUFT_PLACEMENT seed="+seed+" density="+density+" neighbor="+neighborPairs/(2.0*plants));
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
