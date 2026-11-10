package blockbranch.realistic;

import java.util.LinkedHashMap;

/** Static differential weathering approximation on pre-existing bare sedimentary outcrops.
 * No time integration, overhangs, drainage rerouting or simulated rock mechanics. */
final class ErodedBadlands {
    static final int MAX_INCISION=40;
    private final long seed;
    private final DrainageNetwork reference;
    private final ClimateField climate;
    private final LinkedHashMap<Long,Double> support=new LinkedHashMap<>(512,.75f,true);
    ErodedBadlands(long seed,DrainageNetwork reference){this.seed=seed;this.reference=reference;climate=new ClimateField(seed);}
    private double corner(int gx,int gz){
        long key=(gx&0xffffffffL)|((long)gz<<32);
        synchronized(support){var old=support.get(key);if(old!=null)return old;}
        int x=gx*64,z=gz*64;double value=1;
        // Erode only buffered BARE interiors. Existing soil-bearing woodland,
        // completed dry habitats and reserved water/banks cannot select a patch.
        for(int dz=-1;dz<=1&&value>0;dz++)for(int dx=-1;dx<=1;dx++){
            if(Badlands.surfaceKind(seed,x+dx*64,z+dz*64,reference,climate)!=Badlands.BARE){value=0;break;}
        }
        synchronized(support){support.put(key,value);if(support.size()>4096)support.remove(support.keySet().iterator().next());}return value;
    }
    double envelope(int x,int z){
        var old=reference.sample(x,z);
        if(old.waterLevel()!=DrainageNetwork.DRY||old.bedrockCoast()||old.height()<TerrainShape.SEA_LEVEL+150)return 0;
        // Identical dry-sedimentary province as Badlands; cheap rejection before habitat probes.
        if(SurfacePatches.sample(seed^0x424144534544494DL,x,z,24000)<.52)return 0;
        int gx=Math.floorDiv(x,64),gz=Math.floorDiv(z,64);
        double tx=fade(Math.floorMod(x,64)/64.),tz=fade(Math.floorMod(z,64)/64.);
        double a=corner(gx,gz)*(1-tx)+corner(gx+1,gz)*tx,b=corner(gx,gz+1)*(1-tx)+corner(gx+1,gz+1)*tx;
        double value=a*(1-tz)+b*tz;
        if(value==0||Badlands.surfaceKind(seed,x,z,reference,climate)!=Badlands.BARE)return 0;
        return value;
    }
    /** Sparse finite remnants of jointed walls, not enclosing tessellated plateau cells.
     * Each independently jittered anchor contributes a tapered fin; a minority
     * are short column remnants. The support halo follows the maximum reach. */
    double dose(int x,int z){
        int gx=Math.floorDiv(x,64),gz=Math.floorDiv(z,64);double retained=0;
        double fabric=.35+.7*((OakBirchWoodland.mix(seed^0x45524F44414E474CL)>>>11)*0x1.0p-53);
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){
            long h=OakBirchWoodland.mix(seed^0x45524F4446494E53L^(gx+dx)*0x165667B19E3779F9L^(gz+dz)*0xD1B54A32D192ED03L);
            if((h&15)==0)continue;
            double cx=(gx+dx)*64+8+48*unit(h),cz=(gz+dz)*64+8+48*unit(OakBirchWoodland.mix(h));
            long q=OakBirchWoodland.mix(h+31);
            double angle=fabric+(unit(q)-.5)*.9,half=(h&7)==1?3:12+26*unit(OakBirchWoodland.mix(q));
            double px=x-cx,pz=z-cz,along=px*Math.cos(angle)+pz*Math.sin(angle),across=-px*Math.sin(angle)+pz*Math.cos(angle);
            double width=2.5+3.5*unit(OakBirchWoodland.mix(q+71));
            // Low-frequency lateral bend is coherent along a fin, not block jitter.
            across-=1.4*Math.sin(along/13+6*unit(OakBirchWoodland.mix(q+92)));
            double end=fade(Math.clamp((half+7-Math.abs(along))/10,0,1));
            double ridge=fade(Math.clamp((width+8-Math.abs(across))/9,0,1))*end;
            double survival=(h&2)==0?1:.72+.28*unit(OakBirchWoodland.mix(q+44));
            retained=Math.max(retained,ridge*survival);
        }
        return (1-retained)*(30+10*SurfacePatches.sample(seed^0x45524F4444455054L,x,z,180));
    }
    private static double unit(long h){return (h>>>11)*0x1.0p-53;}
    int incision(int x,int z){
        double e=envelope(x,z);if(e==0)return 0;
        int h=reference.sample(x,z).height();
        int code=32+((int)Math.round(4*SurfacePatches.signed(seed^0x4241445354524154L,x,z,320))+8)*6;
        double budget=dose(x,z),removed=0;
        // A stated cemented-bed convention: the existing white/light-grey beds
        // resist weathering 3x more than the other beds. Pigment is not its cause.
        // Integrated resistance avoids height snapping to an absolute layer top.
        for(int dy=0;dy<MAX_INCISION&&budget>0;dy++){
            int band=Badlands.stratum(code,h-dy);double resistance=band==4||band==5?3:1;
            double step=Math.min(1,budget/resistance);removed+=step;budget-=step*resistance;
        }
        return (int)Math.round(e*removed);
    }
    boolean contains(int x,int z){return envelope(x,z)>0;}
    DrainageNetwork.Sample apply(int x,int z,DrainageNetwork.Sample old){
        int amount=incision(x,z);if(amount==0)return old;
        return new DrainageNetwork.Sample(old.height()-amount,old.waterLevel(),old.basin(),old.closed(),old.bedrockCoast(),old.regionalHeight(),old.regionalWaterLevel(),old.sandyBackshore());
    }
    static double fade(double t){return t*t*t*(t*(t*6-15)+10);}
}
