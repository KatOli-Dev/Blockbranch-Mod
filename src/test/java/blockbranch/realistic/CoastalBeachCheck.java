package blockbranch.realistic;

import java.util.Arrays;

public final class CoastalBeachCheck {
    public static void main(String[] args) {
        verifyPioneerClumps();
        if (!CoastalBeach.snowy(new ClimateField.Sample(1.999, .5))
                || CoastalBeach.snowy(new ClimateField.Sample(2, .5))) throw new AssertionError("Exact climate split");
        for (long seed : new long[] {424242, 1, -17}) {
            var d = new DrainageNetwork(new TerrainShape(seed), seed);
            var c = new ClimateField(seed);
            var e = new GroundEcology.Field(d::sample, c);
            for (boolean snow : new boolean[] {false, true}) {
                var scene = CoastalBeachScenes.review(seed, snow);
                if (scene != null) verifyReviewScene(scene, d, c);
                var p = blockbranch.realistic.CoastalBeachFixtures.fixture(d, seed, snow);
                int selected = 0, offQuart = 0, snowBlocks = 0;
                for (int cz = (p.z() >> 4) - 1; cz <= (p.z() >> 4) + 1; cz++)
                    for (int cx = (p.x() >> 4) - 1; cx <= (p.x() >> 4) + 1; cx++) {
                        var packed = OpenSnowCover.beachColumn(seed, cx, cz, new int[0], d, c, e);
                        if (!Arrays.equals(packed, OpenSnowCover.beachColumn(seed, cx, cz, new int[0], d, c, e)))
                            throw new AssertionError("Deterministic snow across repeated chunk queries");
                        for (int v : packed) {
                            int k = v >>>blockbranch.cubic.CanopyLayer.STATE_BITS, x = cx * 16 + (k & 15), z = cz * 16 + ((k >>> 4) & 15);
                            int y = (k >>> 8) + TerrainShape.FLOOR;
                            if (!snow || !CoastalBeach.surfaceContains(x, z, d, c, true)
                                    || y != d.sample(x, z).height() + 1 || (v &blockbranch.cubic.CanopyLayer.STATE_MASK) < SnowyTaiga.snowCode(1)
                                    || (v &blockbranch.cubic.CanopyLayer.STATE_MASK) > SnowyTaiga.snowCode(3)) throw new AssertionError("Thin dry snow on exact cold coast only");
                            snowBlocks++;
                        }
                        for (int z = cz * 16; z < cz * 16 + 16; z++) for (int x = cx * 16; x < cx * 16 + 16; x++) {
                            boolean actual = CoastalBeach.surfaceContains(x, z, d, c, snow);
                            if (actual) {
                                selected++;
                                var s = d.sample(x, z);
                                if (s.waterLevel() != DrainageNetwork.DRY || s.closed() || d.channelAt(x,z).edge() >= 0
                                        || CoastalBeach.surfaceContains(x,z,d,c,!snow)) throw new AssertionError("Exclusive dry ocean shore domain");
                            }
                            if (actual != CoastalBeach.contains(x,z,d,c,snow)) offQuart++;
                        }
                    }
                if (selected < 64 || (snow && snowBlocks < 64)) throw new AssertionError("Natural representative beach required");
                System.out.println("COASTAL_BEACH seed="+seed+" snow="+snow+" point="+p+" T="+c.sample(p.x(),p.z(),d::sample).meanTemperatureC()+" selected="+selected+" snowBlocks="+snowBlocks+" exactQuartDifferences="+offQuart);
            }
            verifyReducedPioneers(seed);
            var old = blockbranch.realistic.LowGradientSandyShoreFixtures.fixture(d, seed);
            if (CoastalBeach.contains(old.x(), old.z(), d, c, true)) throw new AssertionError("Legacy rooted shore fixture remains temperate");
        }
        System.out.println("COASTAL_BEACH_CLIMATE_DRY_DOMAIN_BLOCK_SNOW_PASS");
    }

    /** Species placement remains inside the historical upper-backshore eligibility mask. */
    private static void verifyPioneerClumps(){
        int n=256,windows=0;long[] seam=new long[2],interior=new long[2];
        var upper=new LowGradientSandyShore.Site(TerrainShape.SEA_LEVEL,DrainageNetwork.DRY,true,false,false);
        for(long seed:new long[]{424242,1,-17})for(int[]offset:new int[][]{{0,0},{-137,251},{1207,-2061}}){
            int[] eligible=new int[n*n],retained=new int[n*n];int before=0,after=0;
            for(int z=0;z<n;z++)for(int x=0;x<n;x++){
                int wx=offset[0]+x,wz=offset[1]+z,old=LowGradientSandyShore.cover(seed,wx,wz,true,upper);
                int now=CoastalBeach.pioneerCover(seed,wx,wz,upper,old);eligible[z*n+x]=old;retained[z*n+x]=now;
                if(old==2){before++;if(now==2)after++;}
                if(now!=old&&!(old==2&&now==1))throw new AssertionError("Only eligible rooted pioneers may become bare sand");
                for(int height:new int[]{TerrainShape.SEA_LEVEL-2,TerrainShape.SEA_LEVEL-1}){
                    var low=new LowGradientSandyShore.Site(height,DrainageNetwork.DRY,true,false,false);
                    if(CoastalBeach.pioneerCover(seed,wx,wz,low,2)!=1)throw new AssertionError("Low shore remains free of pioneers");
                }
                for(int code:new int[]{0,1,3})if(CoastalBeach.pioneerCover(seed,wx,wz,upper,code)!=code)throw new AssertionError("Other existing cover retained exactly");
            }
            double fraction=after/(double)before;
            if(fraction<.43||fraction>.57)throw new AssertionError("Retain sparse half-density within eligible upper backshore: "+fraction);
            double[] rates=new double[3];int k=0;
            for(int lag:new int[]{1,8,16}){
                long[] rate=new long[2];
                for(int z=0;z<n;z++)for(int x=0;x<n;x++){
                    int at=z*n+x;
                    if(x+lag<n){int next=at+lag;pioneerPair(rate,eligible,retained,at,next);if(lag==1)pioneerPair(Math.floorMod(offset[0]+x,16)==15?seam:interior,eligible,retained,at,next);}
                    if(z+lag<n){int next=at+lag*n;pioneerPair(rate,eligible,retained,at,next);if(lag==1)pioneerPair(Math.floorMod(offset[1]+z,16)==15?seam:interior,eligible,retained,at,next);}
                }
                rates[k++]=rate[0]/(double)rate[1];
            }
            if(rates[0]<fraction+.15||rates[0]<rates[1]+.15||rates[0]<rates[2]+.15)throw new AssertionError("Pioneer grouping must have fine-scale coherence");
            System.out.printf(java.util.Locale.ROOT,"BEACH_PIONEER_CLUMPS seed=%d offset=%d,%d fraction=%.5f lag1=%.5f lag8=%.5f lag16=%.5f%n",seed,offset[0],offset[1],fraction,rates[0],rates[1],rates[2]);windows++;
        }
        // Eligible seam pairs are sparse in each window, so pool independent
        // seeds/translated windows before assessing chunk-boundary continuity.
        if(Math.abs(seam[0]/(double)seam[1]-interior[0]/(double)interior[1])>.05)throw new AssertionError("Pioneer coherence continues across chunk seams");
        System.out.println("BEACH_PIONEER_SPARSE_CLUMPS_ELIGIBILITY_SEAMS_PASS windows="+windows);
    }
    private static void pioneerPair(long[]rate,int[]eligible,int[]retained,int a,int b){if(eligible[a]==2&&eligible[b]==2&&retained[a]==2){rate[1]++;if(retained[b]==2)rate[0]++;}}

    private static void verifyReducedPioneers(long seed) {
        var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true);var c=new ClimateField(seed);
        var scene=CoastalBeachScenes.forWorld(seed,false,d,c);int before=0,after=0,low=0,eligible=0;
        for(int z=scene.z()-64;z<=scene.z()+64;z++)for(int x=scene.x()-64;x<=scene.x()+64;x++){
            var site=LowGradientSandyShore.site(d,x,z);
            if(!CoastalBeach.surfaceContains(x,z,d,c,false))continue;
            eligible++;
            int old=LowGradientSandyShore.cover(seed,x,z,true,site),now=CoastalBeach.pioneerCover(seed,x,z,site,old);
            if(old==2)before++;if(now==2){after++;if(site.elevation()<1)low++;}
            if(now!=old && !(old==2&&now==1))throw new AssertionError("Only existing Beach grass/root sand becomes bare sand");
            if(now!=CoastalBeach.pioneerCover(seed,x,z,site,old))throw new AssertionError("Deterministic block-resolution pioneers");
            if(CoastalBeach.pioneerCover(seed,x,z,site,0)!=0||CoastalBeach.pioneerCover(seed,x,z,site,1)!=1)
                throw new AssertionError("Non-grass and wet bare sand retained");
        }
        if(before<50||after<10||after>before*.6||low!=0)throw new AssertionError("Reduced, retained upper-backshore colonies "+before+" -> "+after);
        System.out.println("BEACH_PIONEER_REDUCTION seed="+seed+" scene="+scene+" old="+before+" new="+after+" lowShoreGrass="+low+" eligibleDryCells="+eligible);
    }

    private static void verifyReviewScene(CoastalBeachScenes.Scene scene, DrainageNetwork d, ClimateField c) {
        if (!CoastalBeach.contains(scene.x(), scene.z(), d, c, scene.snow())) throw new AssertionError("Natural review witness");
        for (boolean elevated : new boolean[] {false, true}) {
            var a = CoastalBeachScenes.camera(scene, d, c, elevated);
            if (!a.equals(CoastalBeachScenes.camera(scene, d, c, elevated))) throw new AssertionError("Deterministic camera");
            var t = CoastalBeachScenes.transect(scene, d, c, elevated ? 0 : -36);
            double along = elevated ? 28 : 96;
            double waterX = t.x() + scene.seaX() * 16 - scene.seaZ() * along;
            double waterZ = t.z() + scene.seaZ() * 16 + scene.seaX() * along;
            double landX = t.x() - scene.seaX() * (t.width() + 16) - scene.seaZ() * along;
            double landZ = t.z() - scene.seaZ() * (t.width() + 16) + scene.seaX() * along;
            if (!CoastalBeachScenes.ocean(d, waterX, waterZ)) throw new AssertionError("Actual ocean in planned frame");
            int lx = (int)Math.round(landX), lz = (int)Math.round(landZ);
            if (d.sample(lx, lz).waterLevel() != DrainageNetwork.DRY
                    || LowGradientSandyShore.surfaceContains(lx, lz, (x,z) -> LowGradientSandyShore.site(d,x,z)))
                throw new AssertionError("Actual inland transition in planned frame");
            for (double[] target : new double[][] {{waterX,waterZ},{landX,landZ}}) {
                double dx = target[0] - a.x(), dz = target[1] - a.z(), distance = Math.hypot(dx,dz);
                double bearing = Math.toDegrees(Math.atan2(-dx,dz));
                double delta = Math.abs(Math.IEEEremainder(bearing - a.yaw(),360));
                double pitch = Math.toDegrees(Math.atan2(a.y()+1.62-TerrainShape.SEA_LEVEL,distance));
                if (distance > 224 || delta > 45 || Math.abs(pitch-a.pitch()) > 30)
                    throw new AssertionError("Water and inland witnesses must fit conservative view/distance bounds");
            }
            System.out.println("COASTAL_REVIEW_FRAME snow="+scene.snow()+" elevated="+elevated+" witness="+scene.x()+","+scene.z()+" width="+t.width()+" camera="+a);
        }
    }
}
