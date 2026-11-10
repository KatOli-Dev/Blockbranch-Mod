package blockbranch.realistic;

import blockbranch.cubic.FloatingIceLayer;
import java.util.*;

/** Static standing stock of calved glacial ice; no calving, drift or melt simulation. */
public final class IcebergField {
    private static final int CELL=320,SEPARATION=257;
    private final long seed;private final DrainageNetwork drainage;private final ClimateField climate;
    private record Site(int x,int z,long priority){}
    public record Berg(int x,int z,IcebergShape shape){}
    private final LinkedHashMap<Long,Optional<Berg>> cache=new LinkedHashMap<>(128,.75f,true);
    public IcebergField(long seed,DrainageNetwork d,ClimateField c){this.seed=seed;drainage=d;climate=c;}
    public static boolean habitat(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(SurfacePatches.sample(seed^0x4943454245524750L,x,z,16000)<.58)return false;
        var s=d.sample(x,z);return s.waterLevel()==64&&64-s.height()-1>=160&&FrozenOcean.surfaceKind(seed,x,z,d,c)>0;
    }
    private Site site(int gx,int gz){long h=OakBirchWoodland.mix(seed^0x4943454245524753L^gx*0x165667B19E3779F9L^gz*0xD1B54A32D192ED03L);return new Site(gx*CELL+48+(int)(224*unit(h)),gz*CELL+48+(int)(224*unit(OakBirchWoodland.mix(h))),h);}
    private Berg berg(int gx,int gz){
        long key=(gx&0xffffffffL)|((long)gz<<32);synchronized(cache){var old=cache.get(key);if(old!=null)return old.orElse(null);}
        var p=site(gx,gz);boolean valid=habitat(seed,p.x,p.z,drainage,climate);
        // Matérn-style priority rejection, independent of habitat and request order.
        // Accepted bounding discs do not overlap, so merging cannot invent solid ice.
        if(valid)for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            if(dx==0&&dz==0)continue;var q=site(gx+dx,gz+dz);
            if(Math.hypot(p.x-q.x,p.z-q.z)<SEPARATION&&(Long.compareUnsigned(q.priority,p.priority)<0||q.priority==p.priority&&(dz<0||dz==0&&dx<0)))valid=false;
        }
        Berg value=null;
        if(valid){var shape=new IcebergShape(p.priority);check:for(int dz=-IcebergShape.RADIUS;dz<=IcebergShape.RADIUS;dz++)for(int dx=-IcebergShape.RADIUS;dx<=IcebergShape.RADIUS;dx++){
            int bottom=shape.bottom(dx,dz);if(bottom==Integer.MIN_VALUE)continue;
            var s=drainage.sample(p.x+dx,p.z+dz);
            if(!habitat(seed,p.x+dx,p.z+dz,drainage,climate)||s.height()>=64+bottom-8){valid=false;break check;}
        }if(valid)value=new Berg(p.x,p.z,shape);}
        synchronized(cache){cache.put(key,Optional.ofNullable(value));if(cache.size()>128)cache.remove(cache.keySet().iterator().next());}return value;
    }
    public Berg near(int x,int z){int gx=Math.floorDiv(x,CELL),gz=Math.floorDiv(z,CELL);
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){var b=berg(gx+dx,gz+dz);if(b!=null&&b.shape.bottom(x-b.x,z-b.z)!=Integer.MIN_VALUE)return b;}return null;
    }
    private final LinkedHashMap<Long,FloatingIceLayer> columns=new LinkedHashMap<>(128,.75f,true);
    public FloatingIceLayer column(int cx,int cz){
        long key=(cx&0xffffffffL)|((long)cz<<32);synchronized(columns){var old=columns.get(key);if(old!=null)return old;}
        var layer=createColumn(cx,cz);synchronized(columns){columns.put(key,layer);if(columns.size()>128)columns.remove(columns.keySet().iterator().next());}return layer;
    }
    private FloatingIceLayer createColumn(int cx,int cz){
        int[] spans=new int[256];
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int wx=cx*16+x,wz=cz*16+z;var b=near(wx,wz);if(b==null)continue;
            int low=64+b.shape.bottom(wx-b.x,wz-b.z),high=64+b.shape.top(wx-b.x,wz-b.z);
            boolean snow=high>=64&&SurfacePatches.sample(seed^b.x^((long)b.z<<32),wx,wz,8)>.2;
            spans[z*16+x]=FloatingIceLayer.pack(low,high,snow);
        }
        return new FloatingIceLayer(spans);
    }
    private final java.util.concurrent.atomic.AtomicReferenceArray<Berg> witnesses=new java.util.concurrent.atomic.AtomicReferenceArray<>(4);
    public Berg fixture(){return fixture(-1);}
    public Berg fixture(int form){
        if(form< -1||form>2)throw new IllegalArgumentException("Iceberg form");
        var cached=witnesses.get(form+1);if(cached!=null)return cached;
        for(int i=0;i<drainage.size();i++){
            int x=drainage.nodeX(i),z=drainage.nodeZ(i);
            if(Math.abs(x)>2040000||Math.abs(z)>2040000||!habitat(seed,x,z,drainage,climate))continue;
            int gx=Math.floorDiv(x,CELL),gz=Math.floorDiv(z,CELL);
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){var b=berg(gx+dx,gz+dz);if(b!=null&&(form<0||b.shape.type==form)&&b.shape.freeboard>=(form==0?12:24)&&b.shape.footprint>=6400){
                int neighbours=0;var forms=new java.util.HashSet<Integer>();int bx=Math.floorDiv(b.x,CELL),bz=Math.floorDiv(b.z,CELL);
                for(int nz=-3;nz<=3;nz++)for(int nx=-3;nx<=3;nx++){var q=berg(bx+nx,bz+nz);if(q!=null&&Math.hypot(q.x-b.x,q.z-b.z)<=448){neighbours++;forms.add(q.shape.type);}}
                if(neighbours>=3&&forms.size()>=2){witnesses.set(form+1,b);return b;}
            }}
        }throw new AssertionError("No natural iceberg fixture seed="+seed);
    }
    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
}
