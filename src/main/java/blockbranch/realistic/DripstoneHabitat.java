package blockbranch.realistic;

/** Definition1: a bounded static province proxy, not a hydrology/geology simulation. */
public final class DripstoneHabitat {
    public static final int DEFINITION=1,MIN_DEPTH=48,MAX_DEPTH=320;
    private DripstoneHabitat(){}
    public static double province(long seed,int x,int z){return noise(seed,x,0,z,192,0x647269705f70726fL);}
    public static boolean contains(long seed,int x,int y,int z,int surface,double province){
        int depth=surface-y;
        return province>.2&&depth>=MIN_DEPTH&&depth<=MAX_DEPTH&&y>TerrainShape.FLOOR+32
            &&noise(seed,x,y,z,64,0x647269705f766572L)>.05;
    }
    public static boolean contains(long seed,int x,int y,int z,int surface){return contains(seed,x,y,z,surface,province(seed,x,z));}
    private static double noise(long seed,int x,int y,int z,int scale,long salt){
        int gx=Math.floorDiv(x,scale),gy=Math.floorDiv(y,scale),gz=Math.floorDiv(z,scale);
        double tx=fade(Math.floorMod(x,scale)/(double)scale),ty=fade(Math.floorMod(y,scale)/(double)scale),tz=fade(Math.floorMod(z,scale)/(double)scale);
        double a=lerp(ty,lerp(tx,value(seed,gx,gy,gz,salt),value(seed,gx+1,gy,gz,salt)),lerp(tx,value(seed,gx,gy+1,gz,salt),value(seed,gx+1,gy+1,gz,salt)));
        double b=lerp(ty,lerp(tx,value(seed,gx,gy,gz+1,salt),value(seed,gx+1,gy,gz+1,salt)),lerp(tx,value(seed,gx,gy+1,gz+1,salt),value(seed,gx+1,gy+1,gz+1,salt)));
        return lerp(tz,a,b);
    }
    private static double value(long seed,int x,int y,int z,long salt){return (mix(seed^(long)x*0x9E3779B97F4A7C15L^(long)y*0xD6E8FEB86659FD93L^(long)z*0xA24BAED4963EE407L^salt)>>>11)*0x1.0p-53*2-1;}
    public static long mix(long h){h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;return h^(h>>>31);}
    private static double fade(double t){return t*t*t*(t*(t*6-15)+10);}
    private static double lerp(double t,double a,double b){return a+t*(b-a);}
}
