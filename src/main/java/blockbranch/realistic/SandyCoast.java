package blockbranch.realistic;

import java.util.LinkedHashMap;

/** Local static berm/backshore approximation over the original sandy coast.
 * Does not change the continental field, drainage solve or ocean datum. */
final class SandyCoast {
    private record Frame(double distance,double width,double advance,double weight) {}
    private static final Frame NONE=new Frame(0,0,0,0);
    private final long seed;private final TerrainShape shape;private final DrainageNetwork drainage;private final ClimateField climate;
    private final LinkedHashMap<Long,Frame> frames=new LinkedHashMap<>(256,.75f,true);
    SandyCoast(long seed,TerrainShape shape,DrainageNetwork drainage){this.seed=seed;this.shape=shape;this.drainage=drainage;climate=new ClimateField(seed);}

    private Frame frame(int x,int z){
        long key=(x&0xffffffffL)|((long)z<<32);
        synchronized(frames){var found=frames.get(key);if(found!=null)return found;}
        var value=createFrame(x,z);
        synchronized(frames){frames.put(key,value);if(frames.size()>16384)frames.remove(frames.keySet().iterator().next());}
        return value;
    }
    private Frame createFrame(int x,int z){
        double dx=(shape.continuousHeight(x+32,z)-shape.continuousHeight(x-32,z))/64,
            dz=(shape.continuousHeight(x,z+32)-shape.continuousHeight(x,z-32))/64,length=Math.hypot(dx,dz);
        if(length<1e-9||length>.06)return NONE;
        double nx=dx/length,nz=dz/length;
        double lo=-256,hi=256;
        if(shape.continuousHeight(x+nx*lo,z+nz*lo)>=TerrainShape.SEA_LEVEL-1
                ||shape.continuousHeight(x+nx*hi,z+nz*hi)<TerrainShape.SEA_LEVEL-1)return NONE;
        // Root the original continuous coast, rather than dividing by a tiny
        // derivative across its asymmetric land/shelf curves. No modified samples.
        for(int n=0;n<12;n++){
            double mid=(lo+hi)/2;
            if(shape.continuousHeight(x+nx*mid,z+nz*mid)<TerrainShape.SEA_LEVEL-1)lo=mid;else hi=mid;
        }
        double distance=-(lo+hi)/2;
        if(distance< -64||distance>192)return NONE;
        // A conservative climate guard leaves all existing mangrove-suitable
        // regions alone; no wetland classifier is evaluated on modified terrain.
        if(MangroveSwamp.climateSuitable(climate.sample(x,z,drainage::structuralSample)))return NONE;
        int sx=(int)Math.round(x-dx/length*(Math.max(0,distance)+64)),sz=(int)Math.round(z-dz/length*(Math.max(0,distance)+64));
        var sea=drainage.structuralSample(sx,sz);
        if(!drainage.oceanWater(sx,sz,sea))return NONE;
        // Fade out before resistant-rock provinces can carry an approved bluff.
        var rock=CoastalLandforms.frame(seed,shape,x,z);
        double rockBand=smooth(-512,-256,rock.distance())*(1-smooth(768,1024,rock.distance()));
        // Province-zero frames use +infinity as a sentinel; no rock band there.
        if(rock.province()==0)rockBand=0;
        double weight=(1-rockBand*smooth(0,.10,rock.province()))
            *smooth(0,256,drainage.openCatchmentClearance(x,z))*smooth(0,256,drainage.oceanClearance(sx,sz));
        if(weight==0)return NONE;
        double width=75+30*Math.tanh(3*SurfacePatches.signed(seed^0x53414E4457494454L,x,z,220));
        width+=7*SurfacePatches.signed(seed^0x53414E4445444745L,x,z,24);
        double advance=34+22*Math.tanh(3*SurfacePatches.signed(seed^0x53414E4446524F4EL,x,z,180));
        return new Frame(distance,width,advance,weight);
    }
    DrainageNetwork.Sample apply(int x,int z,DrainageNetwork.Sample old,double riverClearance){
        int elevation=old.height()+1-TerrainShape.SEA_LEVEL;
        // At elevation 4 the later blend is exactly zero, so no frame is needed.
        if(old.closed()||elevation< -2||elevation>=4||riverClearance<=0
                ||old.waterLevel()!=DrainageNetwork.DRY&&old.waterLevel()!=TerrainShape.SEA_LEVEL)return old;
        var f=frame(x,z);double weight=f.weight*smooth(0,256,riverClearance)*smooth(-3,-1,elevation)*(1-smooth(0,4,elevation));
        if(weight==0)return old;
        double front=1.6*smooth(-f.advance,60-f.advance,f.distance);
        double crest=(f.distance-f.width*.65)/(f.width*.95);
        double berm=1.6*Math.exp(-crest*crest)*( .85+.15*SurfacePatches.signed(seed^0x53414E444245524DL,x,z,110));
        berm*=smooth(-f.advance,0,f.distance);
        double taper=1-smooth(f.width*.85,Math.min(192,f.width+80),f.distance);
        // Quantize the continuous base plus profile together. Adding separately
        // rounded offsets to a floored base can create a two-block step at the
        // old shoreline even when both underlying functions are smooth.
        double raw=shape.continuousHeight(x,z),fraction=raw-Math.floor(raw);
        int rise=(int)Math.floor(fraction+Math.clamp(weight*(front+berm)*taper,0,4));
        int height=old.height()+rise,water=old.waterLevel();
        if(water!=DrainageNetwork.DRY&&height+1>=water)water=DrainageNetwork.DRY;
        // Region references remain original so the macroclimate cannot feed back
        // through the beach profile. Width is a separate physical sand-domain flag.
        boolean band=f.distance<=192+(f.width-192)*weight;
        return new DrainageNetwork.Sample(height,water,old.basin(),old.closed(),old.bedrockCoast(),old.regionalHeight(),old.regionalWaterLevel(),band);
    }
    private static double smooth(double lo,double hi,double v){double t=Math.clamp((v-lo)/(hi-lo),0,1);return t*t*(3-2*t);}
}
