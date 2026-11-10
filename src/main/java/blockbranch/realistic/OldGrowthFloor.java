package blockbranch.realistic;

/** Continuous soil patches and localized understory; never quantizes physical cover to biome cells. */
public final class OldGrowthFloor {
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        int cover=pattern(seed,x,z,site,clear);
        if((cover==3||cover==6)&&!GroundEcology.borealMossSite(site))
            return site.shaded()?1:cover==6?8:12;
        return BorealSurface.fresh(cover);
    }
    /** Share colony shapes, not boreal block identities, with deciduous floors. */
    public static int deciduousCover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        int cover=pattern(seed,x,z,site,clear);
        return switch(cover){case 3,12->3;case 6->7;case 7->4;case 8->6;default->cover;};
    }
    private static int pattern(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        double moss=GroundEcology.colonyStrength(seed,0x4F4C444D4F535350L,x,z,48,14,30,site);
        long cell=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double fine=SurfacePatches.signed(seed^0x4F4C444544474553L,x,z,5);
        // Roughen and feather only transition bands; patch cores remain coherent.
        boolean mossBed=transition(moss*(.55+.6*site.wetness())-.42+.28*fine,.045,unit(OakBirchWoodland.mix(cell^0x4D4F535345444745L)));
        boolean coarse=transition(SurfacePatches.signed(seed^0x4F4C444C49545445L,x,z,40)-.20+.16*fine,.03,unit(OakBirchWoodland.mix(cell^0x534F494C45444745L)));
        int soil=mossBed?3:coarse&&!site.shaded()?2:1;
        if(!clear)return soil;
        double habitat=GroundEcology.colonyStrength(seed,0x4F4C44554E444552L,x,z,80,18,34,site);
        double tuft=GroundEcology.colonyStrength(seed,0x4F4C445455465453L,x,z,20,4,10,site);
        double fern=GroundEcology.colonyStrength(seed,0x4F4C444645524E53L,x,z,32,8,18,site);
        double berry=GroundEcology.colonyStrength(seed,0x4F4C444245525259L,x,z,64,10,22,site);
        double roll=unit(cell),vigor=.45+.55*habitat;
        // Broad patches increase vigor rather than vetoing every smaller colony
        // outside them. Tuft/fern/berry fields still leave genuinely bare gaps.
        if(mossBed)return roll<(.18+.5*tuft)*vigor?6:3;
        if(!site.shaded()&&berry>.45&&tuft>.25&&roll<.35*berry*vigor)return 7;
        if(site.wetness()>.45&&fern>.18&&tuft>.1&&roll<(.25+.65*tuft)*vigor)return 4;
        if(!site.shaded()&&tuft>.6&&fern<.35&&roll<(.16+.55*tuft)*vigor)return 8;
        return soil;
    }
    private static boolean transition(double signal,double width,double roll){return roll<Math.clamp((signal+width)/(2*width),0,1);}
    private static double unit(long value){return(value>>>11)*0x1.0p-53;}
    private OldGrowthFloor(){}
}
