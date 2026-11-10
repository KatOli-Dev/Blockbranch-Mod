package blockbranch.realistic;

import blockbranch.cubic.TerrainProfile;

/** Coverage-only habitat tied to the actual definition82 rock strata. */
public final class DeepDarkHabitat {
    public static final int MIN_DEPTH=192, BAND_DEPTH=256, ROOF_CLEARANCE=32;
    private DeepDarkHabitat(){}
    public static boolean atHeight(int surface,long y){
        return y<=TerrainProfile.DEEPSLATE_TOP&&y>=TerrainProfile.DEEPSLATE_TOP-BAND_DEPTH
            &&(long)surface-y>=MIN_DEPTH&&y>TerrainShape.FLOOR+32;
    }
    public static int proposalY(int surface){return Math.min(surface-320,TerrainProfile.DEEPSLATE_TOP-64);}
    public static int cityFloor(int surface,int span){
        // Reserve the complete native envelope and the cavern roof inside deepslate.
        return Math.min(surface-320,TerrainProfile.DEEPSLATE_TOP-ROOF_CLEARANCE-span);
    }
    public static boolean eligibleColumn(long seed,int x,int z,DrainageNetwork terrain){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;var site=terrain.sample(x,z);
        return site.waterLevel()==DrainageNetwork.DRY&&site.height()>384&&
            DripstoneHabitat.province(seed^0x444545504441524bL,x/4,z/4)>.1;
    }
    public static boolean contains(long seed,int x,int y,int z,DrainageNetwork terrain){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;y=Math.floorDiv(y,4)*4;
        return eligibleColumn(seed,x,z,terrain)&&atHeight(terrain.sample(x,z).height(),y);
    }
    public static int mask(long seed,int cx,int cz,DrainageNetwork terrain){
        int mask=0;for(int z=0;z<4;z++)for(int x=0;x<4;x++)
            if(eligibleColumn(seed,cx*16+x*4,cz*16+z*4,terrain))mask|=1<<(z*4+x);
        return mask;
    }
}
