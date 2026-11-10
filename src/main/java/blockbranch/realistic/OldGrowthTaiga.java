package blockbranch.realistic;

/** Two vanilla mature boreal identities within the remaining mixed-boreal province. */
public final class OldGrowthTaiga {
    public static final int PINE=1, SPRUCE=2;
    /** Static composition/history proxy, not a simulated stand age or disturbance history. */
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return selected(seed,x,z,d,c,false);
    }
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return selected(seed,x,z,d,c,true);}
    private static int selected(long seed,int x,int z,DrainageNetwork d,ClimateField c,boolean physical){
        if(Taiga.provinceSelected(seed,x,z)||Taiga.province(seed^0x4F4C4447524F5754L,x,z)>.55)return 0;
        var climate=c.sample(x,z,d::sample);
        if(climate.meanTemperatureC()<-2||!(physical?BorealWoodland.surfaceContains(seed,x,z,d,c):BorealWoodland.contains(seed,x,z,d,c)))return 0;
        return climate.moisture()<.58?PINE:SPRUCE;
    }
    public static int family(int kind){return kind==PINE?7:8;}
    public static WoodlandTrees.Root root(TreeScatter.Point p,DrainageNetwork.Sample s,int kind) {
        long h=OakBirchWoodland.mix(p.priority()^0x4F4C44434F484F52L);
        int height=p.young()?8+(int)Long.remainderUnsigned(h,7):23+(int)Long.remainderUnsigned(h,9);
        if(kind==SPRUCE)height=p.young()?6+(int)Long.remainderUnsigned(h,7):Long.remainderUnsigned(h>>>16,10)<4?13+(int)Long.remainderUnsigned(h,9):22+(int)Long.remainderUnsigned(h,10);
        return new WoodlandTrees.Root(p.x(),s.height()+1,p.z(),false,height,(int)(h>>>32),family(kind));
    }
    public static boolean establishes(TreeScatter.Point p,int kind) {
        return kind==SPRUCE||(OakBirchWoodland.mix(p.priority()^0x50494E454F50454EL)>>>11)*0x1.0p-53<.68;
    }
    public record Point(int x,int z){}

    public record View(double x,double y,double z,float yaw,float pitch){}

    private OldGrowthTaiga(){}
}
