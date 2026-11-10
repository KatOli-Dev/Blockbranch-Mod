package blockbranch.realistic;

/** Original graph/height goldens remain frozen. Current channel edges may move
 * inside the declared corridor; node datums and catchment classes stay fixed. */
public final class RiverMarginCheck {
    static long graph(DrainageNetwork d) {
        long h=1;
        for(int i=0;i<d.size();i++) {
            h=h*31+d.nodeX(i); h=h*31+d.nodeZ(i); h=h*31+d.parent(i);
            h=h*31+d.outlet(i); h=h*31+d.waterLevel(i); h=h*31+d.accumulation(i);
        }
        return h;
    }
    record SampleAudit(long structuralHash,int points,int changedHeights) {}
    // Retain the original combined golden against the untouched structural field,
    // then compare every current water/catchment sample independently of height.
    static SampleAudit samples(DrainageNetwork d) {
        long[] audit={1,0,0,0,0};
        for(int z=-1700000;z<=1700000;z+=64000) for(int x=-1700000;x<=1700000;x+=64000) sample(audit,d,x,z);
        for(int i=0;i<d.size();i+=197) if(d.river(i)) for(double t:new double[]{0.15,0.5,0.85}) {
            double x=d.edgeX(i,t),z=d.edgeZ(i,t);
            double dx=d.edgeX(i,Math.min(1,t+0.001))-d.edgeX(i,Math.max(0,t-0.001));
            double dz=d.edgeZ(i,Math.min(1,t+0.001))-d.edgeZ(i,Math.max(0,t-0.001));
            double len=Math.hypot(dx,dz),width=(3+Math.min(400,Math.sqrt(d.accumulation(i))*1.8))*(1-t)+(3+Math.min(400,Math.sqrt(d.accumulation(d.parent(i)))*1.8))*t;
            for(double across:new double[]{-1.3,-0.75,0,0.75,1.3}) sample(audit,d,(int)Math.round(x-dz/len*width*across),(int)Math.round(z+dx/len*width*across));
        }
        System.out.printf("INTENTIONAL_ROUTE_CHANGES mergedDatums=%d wetEdges=%d%n",audit[3],audit[4]);return new SampleAudit(audit[0],(int)audit[1],(int)audit[2]);
    }
    private static void sample(long[] audit,DrainageNetwork d,int x,int z) {
        var structural=d.structuralSample(x,z);var current=d.sample(x,z);
        verifyWater(d,x,z,structural,current);
        if(current.waterLevel()!=DrainageNetwork.DRY&&structural.waterLevel()!=DrainageNetwork.DRY&&current.waterLevel()!=structural.waterLevel())audit[3]++;
        if((current.waterLevel()>current.height())!=(structural.waterLevel()>structural.height()))audit[4]++;
        long h=audit[0];h=h*31+structural.height();h=h*31+structural.waterLevel();h=h*31+structural.basin();audit[0]=h*31+(structural.closed()?1:0);
        audit[1]++;if(current.height()!=structural.height())audit[2]++;
    }
    static void verifyWater(DrainageNetwork d,int x,int z,DrainageNetwork.Sample structural,DrainageNetwork.Sample current){
        require(current.basin()==structural.basin()&&current.closed()==structural.closed(),"Catchment/outlet class changed at "+x+","+z);
        boolean oldWet=structural.waterLevel()!=DrainageNetwork.DRY,newWet=current.waterLevel()!=DrainageNetwork.DRY;
        if(oldWet!=newWet)require(routedCorridor(d,x,z)||coastalDisplacement(structural,current),"Wet edge changed outside bounded routed corridor at "+x+","+z);
        if(oldWet&&newWet&&current.waterLevel()!=structural.waterLevel())require(Math.abs(current.waterLevel()-structural.waterLevel())<=2&&routedCorridor(d,x,z),"Unbounded overlapping route merge datum change at "+x+","+z);
        require(Math.abs(current.height()-structural.height())<=120,"Surface change exceeds explicit bound");
        // Existing values are anchored by the unchanged combined golden below.
        // Only new or changed water needs an additional node-datum witness.
        if(newWet&&(!oldWet||current.waterLevel()!=structural.waterLevel()))require(validWaterDatum(d,x,z,current),"New water has no unchanged source datum at "+x+","+z);
    }
    /** Only original shallow sea displaced by bounded coastal solid ground.
     * Regional references are set by the coastal stage before water removal. */
    static boolean coastalDisplacement(DrainageNetwork.Sample before,DrainageNetwork.Sample after){
        return before.waterLevel()==TerrainShape.SEA_LEVEL&&after.waterLevel()==DrainageNetwork.DRY
            &&!before.closed()&&!after.closed()&&before.basin()==after.basin()
            &&after.regionalWaterLevel()==TerrainShape.SEA_LEVEL&&before.height()<TerrainShape.SEA_LEVEL-1
            &&after.regionalHeight()<TerrainShape.SEA_LEVEL-1&&after.height()>=TerrainShape.SEA_LEVEL-1
            &&after.height()>after.regionalHeight()&&after.height()-after.regionalHeight()<=CoastalLandforms.MAX_RISE;
    }
    private static boolean validWaterDatum(DrainageNetwork d,int x,int z,DrainageNetwork.Sample sample){
        if(d.terminalLakeWater(x,z,sample)||d.oceanWater(x,z,sample))return true;
        int cx=Math.clamp(Math.floorDiv(x-DrainageNetwork.MIN,DrainageNetwork.STEP),0,DrainageNetwork.SIDE-2),cz=Math.clamp(Math.floorDiv(z-DrainageNetwork.MIN,DrainageNetwork.STEP),0,DrainageNetwork.SIDE-2);
        for(int nz=Math.max(0,cz-1);nz<=Math.min(DrainageNetwork.SIDE-1,cz+2);nz++)for(int nx=Math.max(0,cx-1);nx<=Math.min(DrainageNetwork.SIDE-1,cx+2);nx++){
            int i=nz*DrainageNetwork.SIDE+nx;if(!d.river(i))continue;int p=d.parent(i);double t=d.closestT(i,x,z),width=d.riverHalfWidth(i)*(1-t)+d.riverHalfWidth(p)*t;
            if(Math.hypot(x-d.edgeX(i,t),z-d.edgeZ(i,t))>2.1*width+4)continue;
            int level=(int)Math.floor(d.waterLevel(i)+t*(d.waterLevel(p)-d.waterLevel(i)));
            if(Math.hypot(x-d.nodeX(i),z-d.nodeZ(i))<d.riverHalfWidth(i)*4)level=d.waterLevel(i);
            if(Math.hypot(x-d.nodeX(p),z-d.nodeZ(p))<d.riverHalfWidth(p)*4)level=d.waterLevel(p);
            if(level==sample.waterLevel())return true;
        }return false;
    }
    // Width <=1.38 original; lateral shift <=.5 width. The2.1 factor
    // conservatively covers1.38*1.5=2.07, plus4m projection/voxel tolerance.
    static boolean routedCorridor(DrainageNetwork d,int x,int z){int cx=Math.clamp(Math.floorDiv(x-DrainageNetwork.MIN,DrainageNetwork.STEP),0,DrainageNetwork.SIDE-2),cz=Math.clamp(Math.floorDiv(z-DrainageNetwork.MIN,DrainageNetwork.STEP),0,DrainageNetwork.SIDE-2);for(int nz=Math.max(0,cz-1);nz<=Math.min(DrainageNetwork.SIDE-1,cz+2);nz++)for(int nx=Math.max(0,cx-1);nx<=Math.min(DrainageNetwork.SIDE-1,cx+2);nx++){int i=nz*DrainageNetwork.SIDE+nx;if(!d.river(i))continue;double t=d.closestT(i,x,z),width=d.riverHalfWidth(i)*(1-t)+d.riverHalfWidth(d.parent(i))*t;if(Math.hypot(x-d.edgeX(i,t),z-d.edgeZ(i,t))<=width*2.1+4)return true;}return false;}
    public static void main(String[] args) {
        var sea=new DrainageNetwork.Sample(62,64,1,false);
        require(coastalDisplacement(sea,new DrainageNetwork.Sample(63,DrainageNetwork.DRY,1,false,false,62,64)),"One metre shallow sea displacement carries original coastal references");
        for(var invalid:new DrainageNetwork.Sample[]{new DrainageNetwork.Sample(63,DrainageNetwork.DRY,1,false),new DrainageNetwork.Sample(62,DrainageNetwork.DRY,1,false,false,62,64),new DrainageNetwork.Sample(127,DrainageNetwork.DRY,1,false,false,62,64),new DrainageNetwork.Sample(63,DrainageNetwork.DRY,2,false,false,62,64),new DrainageNetwork.Sample(63,DrainageNetwork.DRY,1,true,false,62,64),new DrainageNetwork.Sample(63,64,1,false,false,62,64)})require(!coastalDisplacement(sea,invalid),"Reject missing coastal references, absent/excess uplift, changed basin, closed water or retained/new water");
        require(!coastalDisplacement(new DrainageNetwork.Sample(62,65,1,false),new DrainageNetwork.Sample(63,DrainageNetwork.DRY,1,false,false,62,64)),"Lake datum cannot use coastal exception");
        var route=new DrainageNetwork.Channel(1,300,20,20,0.005);
        require(GravelRiverMargin.eligible(new GravelRiverMargin.Site(298,300,route)),"Shallow routed flank");
        require(GravelRiverMargin.eligible(new GravelRiverMargin.Site(302,DrainageNetwork.DRY,route)),"Low dry bank");
        for(var c:new DrainageNetwork.Channel[]{DrainageNetwork.Channel.NONE,
            new DrainageNetwork.Channel(1,64,20,20,0.005),new DrainageNetwork.Channel(1,1800,20,20,0.005),
            new DrainageNetwork.Channel(1,300,7,7,0.005),new DrainageNetwork.Channel(1,300,120,120,0.005),
            new DrainageNetwork.Channel(1,300,20,9,0.005),new DrainageNetwork.Channel(1,300,20,27,0.005),
            new DrainageNetwork.Channel(1,300,20,20,0),new DrainageNetwork.Channel(1,300,20,20,0.03)})
            require(!GravelRiverMargin.eligible(new GravelRiverMargin.Site(298,300,c)),"Reject absent, coastal, high, undersize/oversize, deep-centre, distant or wrong-grade routes");
        require(!GravelRiverMargin.eligible(new GravelRiverMargin.Site(290,300,route)),"Deep thalweg excluded");
        require(!GravelRiverMargin.eligible(new GravelRiverMargin.Site(310,DrainageNetwork.DRY,route)),"High valley side excluded");
        require(!GravelRiverMargin.eligible(new GravelRiverMargin.Site(298,299,route)),"Unrelated overlapping water excluded");
        int[] counts=new int[5];int changed=0;
        for(int z=-64;z<64;z++) for(int x=-64;x<64;x++) {
            var site=new GravelRiverMargin.Site(302,DrainageNetwork.DRY,route);
            int cover=GravelRiverMargin.cover(424242,x,z,true,site); counts[cover]++;
            require(cover==GravelRiverMargin.cover(424242,x,z,true,site),"Stable sediment pockets");
            require(GravelRiverMargin.cover(424242,x,z,false,site)==0,"No cover outside habitat");
            require(GravelRiverMargin.cover(424242,x,z,true,new GravelRiverMargin.Site(298,300,route))==1,"Lower flank is armoured");
            require(GravelRiverMargin.cover(424242,x,z,true,new GravelRiverMargin.Site(300,DrainageNetwork.DRY,route))==2,"Waterline bank is loose gravel");
            if(cover!=GravelRiverMargin.cover(1,x,z,true,site)) changed++;
        }
        require(counts[2]>1000 && counts[3]>500 && counts[4]>500 && changed>1000,"Seeded loose gravel, soil and pioneer-grass mosaic");
        long[] seeds={424242,1,-17};
        long[] graphs={2359322262386042341L,-2756514231954350449L,3526067613403680944L};
        long[] waters={-3097498960539301074L,-2998127741080058364L,-1184975752539771226L};
        for(int index=0;index<seeds.length;index++) {
            long seed=seeds[index];var drainage=new DrainageNetwork(new TerrainShape(seed),seed);
            require(graph(drainage)==graphs[index],"Drainage graph matches pre-biome baseline for seed "+seed);
            var audit=samples(drainage);
            require(audit.structuralHash()==waters[index],"Structural height/water/catchment samples match the unchanged pre-biome baseline for seed "+seed);
            require(audit.points()>4000&&audit.changedHeights()>100,"Meaningful current-height versus strict hydrology comparison");
            require(graph(drainage)==graphs[index],"Sampling leaves the original drainage graph unchanged");
            System.out.printf("BASELINE_GRAPH_BOUNDED_ROUTE_CHANGES seed=%d points=%d intentionalHeightChanges=%d originalCombinedGolden=%d%n",seed,audit.points(),audit.changedHeights(),waters[index]);
            var point=blockbranch.realistic.GravelRiverMarginFixtures.fixture(drainage,seed); int[] natural=new int[5]; long started=System.nanoTime();
            for(int z=point.z()-32;z<point.z()+32;z++) for(int x=point.x()-32;x<point.x()+32;x++) {
                boolean habitat=GravelRiverMargin.contains(x,z,(wx,wz)->GravelRiverMargin.site(drainage,wx,wz));
                require(habitat==GravelRiverMargin.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,(wx,wz)->GravelRiverMargin.site(drainage,wx,wz)),"Quart alignment on negative chunk seams");
                int cover=GravelRiverMargin.cover(seed,x,z,habitat,GravelRiverMargin.site(drainage,x,z));natural[cover]++;
                if(cover>0) require(GravelRiverMargin.eligible(GravelRiverMargin.site(drainage,x,z)),"Per-block hydrological validity");
            }
            require(natural[0]>0 && natural[1]>0 && natural[2]>0,"Natural margin is bounded and has graded gravel");
            for(int i=0;i<drainage.size();i++) if(drainage.parent(i)==-1 && drainage.closed(i)) {
                require(drainage.channelAt(drainage.nodeX(i),drainage.nodeZ(i)).edge()<0,"Terminal lake footprint excluded");break;
            }
            require(point.equals(blockbranch.realistic.GravelRiverMarginFixtures.fixture(drainage,seed)),"Stable bank search");
            System.out.printf("River margin seed=%d fixture=%s cover=%s sampleMillis=%d graph=%d water=%d%n",seed,point,java.util.Arrays.toString(natural),(System.nanoTime()-started)/1000000,graphs[index],waters[index]);
        }
        System.out.println("GRAVEL_RIVER_MARGIN_SHAPE_HYDROLOGY_PASS");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private RiverMarginCheck() {}
}
