package blockbranch.realistic;

/** Static resistant-bedrock bluffs in selected coastal provinces, not a wave/erosion simulation. */
public final class CoastalLandforms {
    public static final int MAX_RISE=64;
    public record Frame(double distance,double province,double amplitude,double nx,double nz){}
    static Frame frame(long seed,TerrainShape shape,int x,int z){
        double province=smooth(.55,.80,SurfacePatches.sample(seed^0x434F415354524F43L,x,z,18000));
        if(province==0)return new Frame(Double.POSITIVE_INFINITY,0,0,0,0);
        double center=shape.continentality(x,z),dx=(shape.continentality(x+256,z)-shape.continentality(x-256,z))/512,
            dz=(shape.continentality(x,z+256)-shape.continentality(x,z-256))/512,length=Math.hypot(dx,dz);
        if(length<1e-10)return new Frame(Double.POSITIVE_INFINITY,0,0,0,0);
        return new Frame(center/length,province,province*(44+20*SurfacePatches.sample(seed^0x434F415354484754L,x,z,600)),dx/length,dz/length);
    }
    static DrainageNetwork.Sample apply(long seed,TerrainShape shape,DrainageNetwork d,int x,int z,DrainageNetwork.Sample old,double riverClearance){
        int elevation=old.height()+1-TerrainShape.SEA_LEVEL;
        if(old.closed()||elevation< -8||elevation>128||(old.waterLevel()!=DrainageNetwork.DRY&&old.waterLevel()!=TerrainShape.SEA_LEVEL)||riverClearance<=0)return old;
        var f=frame(seed,shape,x,z);double distance=f.distance();
        if(distance< -256||distance>=768||f.amplitude()<.01)return old;
        // Apply one continuous relief field across the shallow land/sea interface.
        // A dry-only mask would cut a tall rise off at the original secondary
        // shorelines. Water stays at its original datum until displaced by rock.
        int seaX=(int)Math.round(x-f.nx()*(Math.max(0,distance)+64)),seaZ=(int)Math.round(z-f.nz()*(Math.max(0,distance)+64));
        double guard=smooth(0,256,riverClearance)*smooth(0,512,d.openCatchmentClearance(x,z))*smooth(0,512,d.oceanClearance(seaX,seaZ));
        if(guard==0)return old;
        var form=d.headland(x,z);if(form.rise()<=0)return old;
        double rise=form.rise()*guard;
        int height=old.height()+(int)Math.round(rise);
        boolean rock=form.exposed()&&form.strength()*guard>=24;
        if(height==old.height()&&!rock)return old;
        int water=old.waterLevel();if(water!=DrainageNetwork.DRY&&height+1>=water)water=DrainageNetwork.DRY;
        return new DrainageNetwork.Sample(height,water,old.basin(),old.closed(),rock,old.regionalHeight(),old.regionalWaterLevel(),old.sandyBackshore());
    }
    private static double smooth(double low,double high,double v){double t=Math.clamp((v-low)/(high-low),0,1);return t*t*(3-2*t);}
    private CoastalLandforms(){}
}
