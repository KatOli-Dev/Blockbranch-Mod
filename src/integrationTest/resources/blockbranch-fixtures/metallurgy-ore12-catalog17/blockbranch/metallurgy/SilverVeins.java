package blockbranch.metallurgy;

/** Definition 10 silver: immutable local proposals in world coordinates, independent of mining/chunk order. */
public final class SilverVeins {
    public static final int MIN_DEPTH=40, PEAK_DEPTH=112, MAX_DEPTH=208;
    private static final int CELL=12;
    private SilverVeins(){}

    public static boolean contains(long seed,int x,int y,int z,int depth){
        if(depth<=MIN_DEPTH||depth>=MAX_DEPTH)return false;
        double density=depth<=PEAK_DEPTH?(depth-MIN_DEPTH)/(double)(PEAK_DEPTH-MIN_DEPTH):(MAX_DEPTH-depth)/(double)(MAX_DEPTH-PEAK_DEPTH);
        int gx=Math.floorDiv(x,CELL),gy=Math.floorDiv(y,CELL),gz=Math.floorDiv(z,CELL);
        int fx=Math.floorMod(x,CELL),fy=Math.floorMod(y,CELL),fz=Math.floorMod(z,CELL);
        // Every radius is below 2.04. Only the two boundary cells can reach a neighbor.
        for(int a=gx-(fx<2?1:0);a<=gx+(fx>9?1:0);a++)
            for(int b=gy-(fy<2?1:0);b<=gy+(fy>9?1:0);b++)
                for(int c=gz-(fz<2?1:0);c<=gz+(fz>9?1:0);c++){
                    long h=mix(seed^0x41475F7631000001L^(long)a*0x9E3779B97F4A7C15L^(long)b*0xD6E8FEB86659FD93L^(long)c*0xA24BAED4963EE407L);
                    if(unit(h)>=.28*density)continue;
                    long shape=mix(h^0x7665696eL);
                    double rx=1.6*(.9+.2*unit(shape)),ry=1.6*(.6+.2*unit(mix(shape^0x59524144495553L))),rz=1.6*(.75+.2*unit(mix(shape^0x5A524144495553L)));
                    double dx=(x+.5-((long)a*CELL+CELL*unit(mix(shape))))/rx;
                    double dy=(y+.5-((long)b*CELL+CELL*unit(mix(shape^17))))/ry;
                    double dz=(z+.5-((long)c*CELL+CELL*unit(mix(shape^37))))/rz;
                    if(dx*dx+dy*dy+dz*dz<1)return true;
                }
        return false;
    }
    private static double unit(long value){return(value>>>11)*0x1.0p-53;}
    private static long mix(long value){value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;value=(value^(value>>>27))*0x94D049BB133111EBL;return value^(value>>>31);}
}
