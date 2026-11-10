package blockbranch.realistic;

/** Existing summit zones and their native rock/scree; no uplift, erosion or glacier simulation. */
public final class MountainPeaks {
    public static final int STONY=1,JAGGED=2;
    public static String key(int kind){return kind==JAGGED?"jagged_peaks":"stony_peaks";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceKind(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var q=d.sample(x,z);int h=q.height(),e=h+1-TerrainShape.SEA_LEVEL;
        if(e<2200||e>4200||q.waterLevel()!=DrainageNetwork.DRY||d.channelAt(x,z).edge()>=0)return 0;
        double slope=SlopeCorrection.slope(d,x,z);if(slope<=.30||slope>2.4)return 0;
        var cl=c.sample(x,z,d::sample);int kind=cl.meanTemperatureC()>0?STONY:cl.meanTemperatureC()>=-20&&cl.meanTemperatureC()<-2&&cl.moisture()>=.30?JAGGED:0;
        if(kind==0||SnowySlopes.contains(x,z,d,c)||SnowySlopes.surfaceContains(x,z,d,c)||RoutedRiver.bankContains(seed,x,z,d,c)||RoutedRiver.surfaceBankContains(seed,x,z,d,c))return 0;
        int lower=0,max=h;
        for(int dz=-512;dz<=512;dz+=512)for(int dx=-512;dx<=512;dx+=512){
            if(dx==0&&dz==0)continue;int near=d.sample(x+dx,z+dz).height();max=Math.max(max,near);if(near<=h-40)lower++;
        }
        return lower>=5&&max-h<=150?kind:0;
    }
    static int interiorCount(long seed,int x,int z,int kind,DrainageNetwork d,ClimateField c){int count=0;for(int dz=-64;dz<=64;dz+=16)for(int dx=-64;dx<=64;dx+=16)if(kind(seed,x+dx,z+dz,d,c)==kind)count++;return count;}
    public record Point(int x,int z){}

    public record View(double x,double y,double z,float yaw,float pitch){}

    /** Fixture-only overview: include the actual nearby crest instead of aiming into its flank. */
    public static View overview(int x,int y,int z,java.util.function.IntBinaryOperator ground){
        int sx=x,sz=z,top=y;
        for(int dz=-192;dz<=192;dz+=16)for(int dx=-192;dx<=192;dx+=16){
            if(dx*dx+dz*dz>192*192)continue;
            int h=ground.applyAsInt(x+dx,z+dz);
            if(h>top){sx=x+dx;sz=z+dz;top=h;}
        }
        double gx=ground.applyAsInt(x+32,z)-ground.applyAsInt(x-32,z),gz=ground.applyAsInt(x,z+32)-ground.applyAsInt(x,z-32),length=Math.hypot(gx,gz);
        if(length<.01){gx=1;gz=0;length=1;}
        double cx=sx-gx/length*180+.5,cz=sz-gz/length*180+.5;
        // Keep the sampling circle within the client's existing 16-chunk view radius.
        double ox=cx-(x+.5),oz=cz-(z+.5),offset=Math.hypot(ox,oz);
        if(offset>48){cx=x+.5+ox/offset*48;cz=z+.5+oz/offset*48;}
        // A high aerial shifts the finite section window above the visible lower flank.
        double cy=Math.max(Math.max(y+65,top-30),ground.applyAsInt((int)Math.floor(cx),(int)Math.floor(cz))+65);
        double tx=sx+.5,tz=sz+.5,ty=top-35;
        return new View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(cx-tx,tz-cz)),(float)-Math.toDegrees(Math.atan2(ty-(cy+1.62),Math.hypot(tx-cx,tz-cz))));
    }
    private MountainPeaks(){}
}
