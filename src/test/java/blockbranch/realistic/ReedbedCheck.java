package blockbranch.realistic;

/** Habitat exclusions, seed/order stability, shallow margins and bounded sampling cost. */
public final class ReedbedCheck {
    public static void main(String[] args) {
        var shallow=new InlandLakeReedbed.Site(300,303,true);
        require(InlandLakeReedbed.shallow(shallow),"Two-metre lake water");
        for(var site:new InlandLakeReedbed.Site[]{new InlandLakeReedbed.Site(300,303,false),
            new InlandLakeReedbed.Site(300,DrainageNetwork.DRY,true),new InlandLakeReedbed.Site(300,301,true),
            new InlandLakeReedbed.Site(300,305,true),new InlandLakeReedbed.Site(60,64,true),new InlandLakeReedbed.Site(700,703,true)})
            require(!InlandLakeReedbed.shallow(site),"Exclude rivers, ocean, dry, deep or high sites");
        require(!InlandLakeReedbed.contains(0,0,(x,z)->new InlandLakeReedbed.Site(300+x,303+x,true)),"Exclude steep scarps");
        int[] covers=new int[5]; int changed=0;
        for(int z=-128;z<128;z++) for(int x=-128;x<128;x++) {
            int c=InlandLakeReedbed.cover(424242,x,z,true,shallow); covers[c]++;
            require(c==InlandLakeReedbed.cover(424242,x,z,true,shallow),"Deterministic cover");
            require(InlandLakeReedbed.cover(424242,x,z,false,shallow)==0,"No plants outside habitat");
            require(InlandLakeReedbed.cover(424242,x,z,true,new InlandLakeReedbed.Site(300,304,true))==1,"Three-metre water remains open");
            if(c!=InlandLakeReedbed.cover(1,x,z,true,shallow)) changed++;
        }
        require(covers[1]>1000 && covers[3]>1000 && covers[4]>1000 && changed>1000,"Seeded clumps, mixed heights and open water");
        verifyLegacyFootprint();
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);
            var point=blockbranch.realistic.InlandLakeReedbedFixtures.fixture(drainage,seed);
            int reeds=0,mud=0,clear=0; long started=System.nanoTime();
            for(int z=point.z()-32;z<point.z()+32;z++) for(int x=point.x()-32;x<point.x()+32;x++) {
                var site=InlandLakeReedbed.site(drainage,x,z);
                boolean habitat=InlandLakeReedbed.contains(x,z,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz));
                require(habitat==InlandLakeReedbed.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,(wx,wz)->InlandLakeReedbed.site(drainage,wx,wz)),"Quart grid across negative chunk edges");
                int cover=InlandLakeReedbed.cover(seed,x,z,habitat,site);
                if(cover>1) { reeds++; require(site.depth()<=2 && (cover==site.depth()+1||cover==site.depth()+2),"Stems span root to one or two metres above water"); }
                else if(cover==1) mud++; else clear++;
            }
            require(reeds>0 && mud>0 && clear>0,"Natural margin includes reeds, mud and unassigned water");
            require(point.equals(blockbranch.realistic.InlandLakeReedbedFixtures.fixture(drainage,seed)),"Stable fixture search");
            System.out.printf("Reedbed seed=%d fixture=%s reeds=%d mud=%d clear=%d sampleMillis=%d%n",seed,point,reeds,mud,clear,(System.nanoTime()-started)/1000000);
        }
        System.out.println("INLAND_LAKE_REEDBED_SHAPE_PASS");
    }
    /** Prior selector is the negative control for placement, not the new height policy. */
    private static void verifyLegacyFootprint(){
        for(long seed:new long[]{424242,1,-17}){
            int shorter=0,taller=0;
            for(int depth=1;depth<=3;depth++)for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
                var site=new InlandLakeReedbed.Site(300,301+depth,true);
                double colony=GroundEcology.colonyStrength(seed,0x52454544434F4CL,x,z,32,9,22,new GroundEcology.Site(.85,0,0,0,0,false));
                long cell=mix(seed^0x5354414C4B53414CL^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
                int old=depth>2||(cell>>>11)*0x1.0p-53>.68*colony?1:depth+2;
                int now=InlandLakeReedbed.cover(seed,x,z,true,site);
                require(old>1?now==old||now==old-1:now==old,"Legacy plant/mud footprint and maximum height preserved");
                if(old>1){if(now==old) taller++;else shorter++;}
            }
            require(shorter>1000&&taller>shorter,"Both heights present; tall stems remain dominant");
            System.out.printf("REED_HEIGHT_FOOTPRINT seed=%d short=%d tall=%d%n",seed,shorter,taller);
        }
    }
    private static long mix(long v){v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;v=(v^(v>>>27))*0x94D049BB133111EBL;return v^(v>>>31);}
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private ReedbedCheck() {}
}
