package blockbranch.realistic;

import java.util.function.BiFunction;

/** Ocean authority, sea-level crossing, shelf gradient and deterministic pioneer cover. */
public final class SandyShoreCheck {
    public static void main(String[] args) {
        BiFunction<Integer,Integer,LowGradientSandyShore.Site> coast=(x,z)->x<0 ? site(62,64,true,true,false) : site(63,DrainageNetwork.DRY,true,false,false);
        require(LowGradientSandyShore.contains(0,0,coast),"Gentle ocean wet/dry crossing");
        require(!LowGradientSandyShore.contains(0,0,(x,z)->site(62,64,true,true,false)),"Exclude open shelf away from land");
        require(!LowGradientSandyShore.contains(0,0,(x,z)->site(63,DrainageNetwork.DRY,true,false,false)),"Exclude dry lowlands without ocean");
        require(!LowGradientSandyShore.contains(0,0,(x,z)->site(62,64,false,false,false)),"Exclude inland lake water at the sea datum");
        require(!LowGradientSandyShore.contains(0,0,(x,z)->site(62,64,true,true,true)),"Exclude river mouths");
        require(!LowGradientSandyShore.contains(0,0,(x,z)->x<0 ? site(50,64,true,true,false) : site(63,DrainageNetwork.DRY,true,false,false)),"Exclude cliff coast");
        require(!LowGradientSandyShore.contains(0,0,(x,z)->x<0 ? site(x<=-256 ? 30 : 62,64,true,true,false) : site(63,DrainageNetwork.DRY,true,false,false)),"Exclude steep offshore shelf");
        for(var s:new LowGradientSandyShore.Site[]{site(54,64,true,true,false),site(68,DrainageNetwork.DRY,true,false,false),site(62,70,true,false,false)})
            require(!LowGradientSandyShore.eligible(s),"Exclude deep, high or non-sea water");
        for(int x:new int[]{-17,-16,-1,0,3,4,15,16,17}) for(int z:new int[]{-17,-1,0,15,16})
            require(LowGradientSandyShore.contains(x,z,coast)==LowGradientSandyShore.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,coast),"Quart alignment across negative chunk seams");
        int[] count=new int[3];int changed=0;
        for(int z=-128;z<128;z++) for(int x=-128;x<128;x++) {
            int c=LowGradientSandyShore.cover(424242,x,z,true,site(63,DrainageNetwork.DRY,true,false,false));count[c]++;
            require(c==LowGradientSandyShore.cover(424242,x,z,true,site(63,DrainageNetwork.DRY,true,false,false)),"Stable root patches");
            require(LowGradientSandyShore.cover(424242,x,z,true,site(62,64,true,true,false))==1,"Submerged shore stays bare sand");
            require(LowGradientSandyShore.cover(424242,x,z,false,site(63,DrainageNetwork.DRY,true,false,false))==0,"No cover outside habitat");
            if(c!=LowGradientSandyShore.cover(1,x,z,true,site(63,DrainageNetwork.DRY,true,false,false))) changed++;
        }
        require(count[1]>40000 && count[2]>1000 && changed>1000,"Sparse seeded pioneer clumps");
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var point=blockbranch.realistic.LowGradientSandyShoreFixtures.fixture(drainage,seed);
            int[] covers=new int[3];boolean wet=false,dry=false,boundary=false;long started=System.nanoTime();
            for(int z=point.z()-32;z<point.z()+32;z++) for(int x=point.x()-32;x<point.x()+32;x++) {
                var s=LowGradientSandyShore.site(drainage,x,z);boolean habitat=LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz));
                int c=LowGradientSandyShore.cover(seed,x,z,habitat,s);covers[c]++;
                if(c==2) require(s.dry() && s.elevation()>=0,"Natural grass remains above water");
            }
            for(int z=point.z()-256;z<=point.z()+256;z+=16) for(int x=point.x()-256;x<=point.x()+256;x+=16) {
                var s=LowGradientSandyShore.site(drainage,x,z);
                wet|=s.oceanWater();dry|=s.dry() && s.elevation()>=0;
                boundary|=!LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz));
            }
            require(covers[1]>0 && covers[2]>0 && wet && dry && boundary,"Bounded natural beach, water and backshore");
            require(point.equals(blockbranch.realistic.LowGradientSandyShoreFixtures.fixture(drainage,seed)),"Deterministic coastal search");
            System.out.printf("Sandy shore seed=%d fixture=%s cover=%s sampleMillis=%d%n",seed,point,java.util.Arrays.toString(covers),(System.nanoTime()-started)/1000000);
        }
        System.out.println("LOW_GRADIENT_SANDY_SHORE_SHAPE_PASS");
    }
    private static LowGradientSandyShore.Site site(int height,int water,boolean catchment,boolean ocean,boolean river) { return new LowGradientSandyShore.Site(height,water,catchment,ocean,river); }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private SandyShoreCheck() {}
}
