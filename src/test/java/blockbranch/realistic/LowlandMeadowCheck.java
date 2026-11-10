package blockbranch.realistic;

/** Topographic suitability, explicit water/channel exclusions, seeded vegetation and seams. */
public final class LowlandMeadowCheck {
    public static void main(String[] args) {
        LowlandMeadowForbsCheck.verify();
        require(LowlandMeadow.contains(0,0,(x,z)->site(263+x/8,DrainageNetwork.DRY,false)),"Rolling dry lowland");
        for(int e:new int[]{-10,0,19,20,900,901,2500}) require(LowlandMeadow.contains(0,0,(x,z)->site(63+e,DrainageNetwork.DRY,false)),"No arbitrary altitude cutoff in topographic suitability; climate is separate");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263+x/2,DrainageNetwork.DRY,false)),"Exclude steep slopes");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263,265,false)),"Exclude lakes");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263,DrainageNetwork.DRY,true)),"Exclude routed river banks even when dry");
        for(int x:new int[]{-17,-16,-1,0,3,4,15,16}) for(int z:new int[]{-17,-1,0,15,16})
            require(LowlandMeadow.contains(x,z,(wx,wz)->site(263+wx/8,DrainageNetwork.DRY,false))==LowlandMeadow.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,(wx,wz)->site(263+wx/8,DrainageNetwork.DRY,false)),"Global quart seams");
        int[] covers=new int[7];int changed=0;
        for(int z=-128;z<128;z++) for(int x=-128;x<128;x++) {
            int c=LowlandMeadow.cover(424242,x,z,true,site(263,DrainageNetwork.DRY,false));covers[c]++;
            require(c==LowlandMeadow.cover(424242,x,z,true,site(263,DrainageNetwork.DRY,false)),"Stable cover");
            if(c!=LowlandMeadow.cover(1,x,z,true,site(263,DrainageNetwork.DRY,false))) changed++;
            require(LowlandMeadow.cover(424242,x,z,true,site(263,265,false))==0,"Water remains unchanged within a biome cell");
            require(LowlandMeadow.cover(424242,x,z,false,site(263,DrainageNetwork.DRY,false))==0,"No meadow outside selected cells");
        }
        for(int c=1;c<=6;c++) require(covers[c]>100,"Every sward/grass/forb component present");
        require(covers[1]>20000 && changed>10000,"Open sward and seed-sensitive clumps");
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var point=blockbranch.realistic.LowlandMeadowFixtures.fixture(drainage,seed);
            var ecology=new GroundEcology.Field(drainage::sample,new ClimateField(seed));
            int[] actual=new int[7];long started=System.nanoTime();
            for(int z=point.z()-64;z<point.z()+64;z++) for(int x=point.x()-64;x<point.x()+64;x++) {
                var s=LowlandMeadow.site(drainage,x,z);int c=LowlandMeadow.cover(seed,x,z,LowlandMeadow.contains(x,z,(wx,wz)->LowlandMeadow.site(drainage,wx,wz)),s,ecology.sample(x,z));actual[c]++;
                if(c>0) require(s.water()==DrainageNetwork.DRY && !s.river(),"Actual drainage retained");
            }
            for(int c=1;c<=6;c++) require(actual[c]>0,"Natural diverse meadow seed="+seed+" cover="+c);
            require(point.equals(blockbranch.realistic.LowlandMeadowFixtures.fixture(drainage,seed)),"Deterministic fixture search");
            System.out.printf("Lowland meadow seed=%d fixture=%s cover=%s sampleMillis=%d%n",seed,point,java.util.Arrays.toString(actual),(System.nanoTime()-started)/1000000);
        }
        System.out.println("ROLLING_LOWLAND_MEADOW_SHAPE_PASS (topographic selector and climate-compatible retained meadow fixtures)");
    }
    private static LowlandMeadow.Site site(int h,int w,boolean river) { return new LowlandMeadow.Site(h,w,river); }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
