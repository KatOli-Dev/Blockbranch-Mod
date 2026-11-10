package blockbranch.realistic;

/** Bounded analytic preferred-region search; no chunks, origin bias or geographic-degree claim. */
public final class SpawnRegions {
    public static final int CLEARANCE=128_000,REGIONS=64,RINGS=5,STEP=16_384;
    public static final int MAX_PROBES=REGIONS*(RINGS*2+1)*(RINGS*2+1);
    public record Bounds(double minX,double minZ,double maxX,double maxZ){
        public Bounds {
            if(!Double.isFinite(minX)||!Double.isFinite(minZ)||!Double.isFinite(maxX)||!Double.isFinite(maxZ)||minX>=maxX||minZ>=maxZ)throw new IllegalArgumentException("Invalid world border bounds");
        }
        public double clearance(int x,int z){return Math.min(Math.min(x+.5-minX,maxX-x-.5),Math.min(z+.5-minZ,maxZ-z-.5));}
        public boolean allows(int x,int z){return clearance(x,z)>=CLEARANCE;}
        public boolean intersectsChunk(int x,int z){return x+15.5>=minX+CLEARANCE&&x+.5<=maxX-CLEARANCE&&z+15.5>=minZ+CLEARANCE&&z+.5<=maxZ-CLEARANCE;}
        public Bounds reserve(int radius){
            if(radius<0)throw new IllegalArgumentException("Negative spawn radius");
            if(maxX-minX<=2.*(CLEARANCE+radius)||maxZ-minZ<=2.*(CLEARANCE+radius))
                throw new IllegalArgumentException("World border is too small for 128 km spawn clearance plus the configured respawn radius. Enlarge it; a reduced-margin centre fallback needs a separate decision.");
            var reserved=new Bounds(minX+radius,minZ+radius,maxX-radius,maxZ-radius);reserved.range();return reserved;
        }
        private Range range(){
            int ax=(int)Math.ceil(minX+CLEARANCE-.5),az=(int)Math.ceil(minZ+CLEARANCE-.5),bx=(int)Math.floor(maxX-CLEARANCE-.5),bz=(int)Math.floor(maxZ-CLEARANCE-.5);
            if(maxX-minX<2.*CLEARANCE||maxZ-minZ<2.*CLEARANCE||ax>bx||az>bz)
                throw new IllegalArgumentException("World border is too small for 128 km spawn clearance on every side. Enlarge the border beyond 256 km across; a reduced-margin centre fallback requires an explicit separate decision. No unsafe fallback was selected.");
            return new Range(ax,az,bx,bz);
        }
    }
    private record Range(int minX,int minZ,int maxX,int maxZ){}
    public record Selection(int x,int z,int region,int probes,double latitude,double temperatureC,double moisture){}
    public static boolean mild(ClimateField.Sample c){return c.meanTemperatureC()>=4&&c.meanTemperatureC()<=26&&c.moisture()>=.12&&c.moisture()<=.88;}
    public static boolean eligible(int x,int z,DrainageNetwork d,ClimateField c){
        var s=d.sample(x,z);int h=s.height();
        if(s.waterLevel()!=DrainageNetwork.DRY||d.channelAt(x,z).edge()>=0||h<TerrainShape.SEA_LEVEL+4||h>TerrainShape.SEA_LEVEL+250)return false;
        // Keep the old lowland/16m gentle-ground test, now against final drainage terrain on both sides.
        if(Math.abs(h-d.sample(x+16,z).height())>4||Math.abs(h-d.sample(x-16,z).height())>4
                ||Math.abs(h-d.sample(x,z+16).height())>4||Math.abs(h-d.sample(x,z-16).height())>4)return false;
        return mild(c.sample(x,z,d::sample));
    }
    public static Selection select(long seed,Bounds bounds,DrainageNetwork d,ClimateField c){
        var range=bounds.range();int probes=0;
        for(int region=0;region<REGIONS;region++){
            long h=OakBirchWoodland.mix(seed^0x535041574E524547L^region*0x9E3779B97F4A7C15L);
            int px=coordinate(OakBirchWoodland.mix(h^0x58535041574EL),range.minX,range.maxX),pz=coordinate(OakBirchWoodland.mix(h^0x5A535041574EL),range.minZ,range.maxZ);
            int turn=(int)(h>>>32)&3;
            for(int ring=0;ring<=RINGS;ring++)for(int dz=-ring;dz<=ring;dz++)for(int dx=-ring;dx<=ring;dx++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
                int ox=dx,oz=dz;for(int n=0;n<turn;n++){int old=ox;ox=-oz;oz=old;}
                long lx=(long)px+ox*STEP,lz=(long)pz+oz*STEP;
                if(lx<range.minX||lx>range.maxX||lz<range.minZ||lz>range.maxZ)continue;
                int x=((int)lx>>4)*16+8,z=((int)lz>>4)*16+8;
                if(!bounds.allows(x,z))continue;
                probes++;if(!eligible(x,z,d,c))continue;
                var climate=c.sample(x,z,d::sample);
                return new Selection(x,z,region,probes,Math.min(1,Math.abs((double)z)/2097152.),climate.meanTemperatureC(),climate.moisture());
            }
        }
        throw new IllegalStateException("No mild dry spawn region found within the bounded "+MAX_PROBES+"-probe search. No unsafe or near-border fallback was selected.");
    }
    private static int coordinate(long value,int min,int max){return (int)(min+Long.remainderUnsigned(value,(long)max-min+1));}
    private SpawnRegions(){}
}
