package blockbranch.realistic;

import java.util.LinkedHashMap;
import java.util.function.BiFunction;

/** Static, continuous vegetation-site proxies: regional moisture, local hollows, exposure and canopy shelter. */
public final class GroundEcology {
    public record Site(double wetness,double retention,double exposure,double dx,double dz,boolean shaded){
        public Site shade(boolean value){return new Site(Math.clamp(wetness+(value?.04:0),0,1),retention,exposure,dx,dz,value);}
    }
    public static final Site NEUTRAL=new Site(.55,0,0,0,0,false);
    /** Bounded cache stores immutable 32 m environmental samples; interpolation avoids chunk/grid steps. */
    public static final class Field {
        private final BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain;
        private final ClimateField climate;
        private final LinkedHashMap<Long,Site> samples=new LinkedHashMap<>(512,.75f,true);
        public Field(BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,ClimateField climate){this.terrain=terrain;this.climate=climate;}
        public Site sample(int x,int z){
            int gx=Math.floorDiv(x,32),gz=Math.floorDiv(z,32);double tx=Math.floorMod(x,32)/32.0,tz=Math.floorMod(z,32)/32.0;
            var a=corner(gx,gz);var b=corner(gx+1,gz);var c=corner(gx,gz+1);var d=corner(gx+1,gz+1);
            return new Site(blend(a.wetness(),b.wetness(),c.wetness(),d.wetness(),tx,tz),blend(a.retention(),b.retention(),c.retention(),d.retention(),tx,tz),blend(a.exposure(),b.exposure(),c.exposure(),d.exposure(),tx,tz),blend(a.dx(),b.dx(),c.dx(),d.dx(),tx,tz),blend(a.dz(),b.dz(),c.dz(),d.dz(),tx,tz),false);
        }
        private Site corner(int gx,int gz){
            long key=(gx&0xffffffffL)|((long)gz<<32);synchronized(samples){var found=samples.get(key);if(found!=null)return found;}
            int x=gx*32,z=gz*32,h=terrain.apply(x,z).height();var west=terrain.apply(x-16,z);var east=terrain.apply(x+16,z);var north=terrain.apply(x,z-16);var south=terrain.apply(x,z+16);
            double dx=(east.height()-west.height())/32.0,dz=(south.height()-north.height())/32.0;
            double hollow=((west.height()+east.height()+north.height()+south.height())/4.0-h)/4;
            double wet=0;for(var s:new DrainageNetwork.Sample[]{west,east,north,south})if(s.waterLevel()>s.height()+1)wet+=.25;
            double retention=Math.clamp(hollow+wet*.5,-1,1),slope=PlanarMath.length(dx,dz);
            // Fixed mean exposure toward the map's equatorial belt; no daily weather/soil-water simulation.
            double sun=slope==0?0:Math.max(0,Math.signum(z)*dz/slope)*Math.min(1,slope/.35)*Math.tanh(Math.abs(z)/400000.0);
            double exposure=Math.clamp(sun+Math.max(0,-retention)*.4,0,1);
            var site=new Site(Math.clamp(climate.sample(x,z,terrain).moisture()+.10*retention-.06*exposure,0,1),retention,exposure,dx,dz,false);
            synchronized(samples){samples.put(key,site);if(samples.size()>512)samples.remove(samples.keySet().iterator().next());}return site;
        }
        public int cachedSamples(){synchronized(samples){return samples.size();}}
    }
    /** Elliptical colonies have fully jittered centres and soft overlapping edges, rather than filled grid tiles. */
    static Colony colony(long seed,long salt,int x,int z,int scale,double minRadius,double maxRadius,Site site){
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double best=0;long selected=0;
        for(int oz=-1;oz<=1;oz++)for(int ox=-1;ox<=1;ox++){
            long h=OakBirchWoodland.mix(seed^salt^(gx+ox)*0x9E3779B97F4A7C15L^(gz+oz)*0xC2B2AE3D27D4EB4FL);
            double cx=(gx+ox)*scale+unit(h)*scale,cz=(gz+oz)*scale+unit(OakBirchWoodland.mix(h))*scale;
            double radius=minRadius+(maxRadius-minRadius)*((h>>>16)&65535)/65535.0;
            double angle=PlanarMath.length(site.dx(),site.dz())<.02?unit(OakBirchWoodland.mix(h^0xA1161EL))*Math.PI*2:Math.atan2(site.dz(),site.dx())+Math.PI/2+(((h>>>32)&65535)/65535.0-.5)*1.2;
            double dx=x-cx,dz=z-cz,along=dx*Math.cos(angle)+dz*Math.sin(angle),across=-dx*Math.sin(angle)+dz*Math.cos(angle);
            double q=along*along/(radius*radius*1.44)+across*across/(radius*radius*.64);
            double intensity=Math.max(0,1-q);if(intensity>best){best=intensity;selected=h;}
        }
        return new Colony(best,selected);
    }
    public static double colonyStrength(long seed,long salt,int x,int z,int scale,double minRadius,double maxRadius,Site site){return colony(seed,salt,x,z,scale,minRadius,maxRadius,site).strength();}
    record Colony(double strength,long seed){}
    public static int meadow(long seed,int x,int z,Site site){
        var grass=colony(seed,0x47524153534CL,x,z,24,10,18,site);
        double vigor=Math.clamp(.42+.65*site.wetness()+.10*site.retention()-.22*site.exposure()-(site.shaded()?.14:0),.15,.95);
        long cell=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        if(unit(cell)>(.08+.84*grass.strength())*vigor)return 1;
        var flowers=colony(seed,0x464C4F574552L,x,z,32,5,12,site);
        double flowerSite=Math.clamp(1-Math.abs(site.wetness()-.58)*1.5,.2,1)*(site.shaded()?.06:1)*Math.clamp(1-.45*site.exposure(),.3,1);
        if(unit(OakBirchWoodland.mix(cell^0xF10AE25L))<.75*flowers.strength()*flowerSite)return 4+(int)Long.remainderUnsigned(flowers.seed(),3);
        var tussock=colony(seed,0x464553435545L,x,z,20,4,10,site);
        double fescue=.12+.62*tussock.strength()*(1-.35*site.wetness());
        return unit(OakBirchWoodland.mix(cell^0xFE5C0EL))<fescue?3:2;
    }
    /** Boreal habitat eligibility, independent of colony shape or desired green color. */
    static boolean borealMossSite(Site site){
        // Normalized habitat policy: moist open mats or sheltered, moderately
        // moist ground. Moss is a plant community, not a green grass palette.
        return site.wetness()>=.55||(site.shaded()&&site.wetness()>=.35);
    }
    /** Existing floor codes, tied to actual canopy clearance, moisture and colonies. */
    public static int forest(long seed,int x,int z,int family,Site site,boolean clear){
        if(!clear)return family==1&&site.wetness()>.65?BorealSurface.SOIL:family==2?7:1;
        long cell=OakBirchWoodland.mix(seed^0x464F52455354464CL^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double moist=Math.clamp((site.wetness()-.25)*2,.2,1),slope=Math.min(1,PlanarMath.length(site.dx(),site.dz())/.4);
        double fern=colonyStrength(seed,0x4645524E434CL,x,z,20,5,12,site),fungus=colonyStrength(seed,0x46554E4749434CL,x,z,24,3,8,site);
        double grass=colonyStrength(seed,0x574F4F444752L,x,z,24,8,17,site),litter=colonyStrength(seed,0x4C4954544552L,x,z,28,8,17,site);
        if(family==0){
            if(site.shaded()&&site.wetness()>.48&&chance(cell,1)<.18*fungus*moist)return 5;
            if(chance(cell,2)<(.03+.65*fern)*moist*(site.shaded()?1:.12)*(1-.4*slope))return 4;
            if(chance(cell,3)<(.22+.65*litter)*(site.shaded()?1:.08))return 7;
            if(!site.shaded())return chance(cell,4)<(.08+.65*grass)*moist?6:chance(cell,5)<.04+.08*site.exposure()?2:3;
            return chance(cell,5)<(.03+.12*site.exposure())*(1-litter)?2:1;
        }
        if(family==1){
            double moss=colonyStrength(seed,0x4D4F5353434CL,x,z,24,8,17,site),berry=colonyStrength(seed,0x4245525259434CL,x,z,28,4,10,site);
            if(site.shaded()&&site.wetness()>.48&&chance(cell,1)<.16*fungus*moist)return 5;
            if(chance(cell,6)<.55*berry*moist*(site.shaded()?.12:1))return 7;
            if(chance(cell,2)<(.03+.55*fern)*moist*(site.shaded()?1:.2)*(1-.4*slope))return 4;
            if(chance(cell,7)<(.10+.65*moss)*moist*(site.shaded()?1:.45))return borealMossSite(site)?BorealSurface.CARPET:site.shaded()?1:8;
            if(!site.shaded()&&chance(cell,4)<(.07+.65*grass)*moist)return 8;
            if(chance(cell,5)<.025+.12*site.exposure())return 2;
            return chance(cell,8)<(.15+.65*moss)*moist?(borealMossSite(site)?BorealSurface.SOIL:site.shaded()?1:12):1;
        }
        if(site.shaded()&&site.wetness()>.50&&chance(cell,1)<.13*fungus*moist)return 6;
        if(chance(cell,2)<.60*fern*moist*(site.shaded()?1:.08)*(1-.5*slope))return 5;
        if(chance(cell,3)<(.20+.60*litter)*(site.shaded()?1:.12))return 3;
        if(chance(cell,4)<(.06+.65*grass)*(1-.35*site.wetness())*(site.shaded()?.12:1))return 4;
        if(!site.shaded()&&chance(cell,6)<.16*fungus*(1-site.wetness())*(.3+site.exposure()))return 8;
        return site.shaded()?7:chance(cell,8)<.15+.5*grass*moist?2:1;
    }
    private static double chance(long cell,long salt){return unit(OakBirchWoodland.mix(cell^salt*0x9E3779B97F4A7C15L));}
    private static double unit(long value){return(value>>>11)*0x1.0p-53;}
    private static double blend(double a,double b,double c,double d,double x,double z){return(a+(b-a)*x)*(1-z)+(c+(d-c)*x)*z;}
    private GroundEcology(){}
}
