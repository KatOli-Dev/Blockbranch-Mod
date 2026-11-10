package blockbranch.realistic;

/** Flat-material controls isolate paint patterns from terrain, biome selection and lighting. */
public final class SurfacePatchesCheck {
    @FunctionalInterface private interface Field { double sample(int x, int z); }
    private record Stats(double fourfold, double contourFourfold, double variation, double neighborChange, double mean) {}

    private static Stats measure(Field field) {
        double cosine=0,sine=0,weight=0,edgeCosine=0,edgeSine=0,edgeWeight=0,sum=0,squares=0,change=0;
        int size=512;
        for(int z=-size/2;z<size/2;z++)for(int x=-size/2;x<size/2;x++) {
            double value=field.sample(x,z),east=field.sample(x+1,z),south=field.sample(x,z+1);
            double dx=east-field.sample(x-1,z),dz=south-field.sample(x,z-1);
            double magnitude=Math.hypot(dx,dz),angle=4*Math.atan2(dz,dx);
            cosine+=magnitude*Math.cos(angle);sine+=magnitude*Math.sin(angle);weight+=magnitude;
            if(band(value)!=band(east)||band(value)!=band(south)){
                edgeCosine+=magnitude*Math.cos(angle);edgeSine+=magnitude*Math.sin(angle);edgeWeight+=magnitude;
            }
            sum+=value;squares+=value*value;change+=Math.abs(east-value)+Math.abs(south-value);
        }
        int count=size*size;double mean=sum/count;
        return new Stats(Math.hypot(cosine,sine)/weight,Math.hypot(edgeCosine,edgeSine)/edgeWeight,Math.sqrt(squares/count-mean*mean),change/(2*count),mean);
    }
    private static int band(double value){return value<.28?0:value<.46?1:value<.62?2:3;}

    // Former samplers are retained only as failing controls. Rotating a square
    // lattice moves its preferred directions; the fourfold magnitude detects both.
    private static double fade(double t){return t*t*t*(t*(t*6-15)+10);}
    private static double value(long seed,int x,int z){return(OakBirchWoodland.mix(seed^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53;}
    private static double oldNoise(long seed,double x,double z){int ix=(int)Math.floor(x),iz=(int)Math.floor(z);double u=fade(x-ix),v=fade(z-iz);return(value(seed,ix,iz)*(1-u)+value(seed,ix+1,iz)*u)*(1-v)+(value(seed,ix,iz+1)*(1-u)+value(seed,ix+1,iz+1)*u)*v;}
    private static double oldPatch(long seed,int x,int z,double scale){double u=(.8*x+.6*z)/scale,v=(-.6*x+.8*z)/scale;return .75*oldNoise(seed,u,v)+.25*oldNoise(seed^0xD1B54A32D192ED03L,1.9*v+7.3,-1.9*u+2.8);}

    public static void main(String[] args) {
        for(long seed:new long[]{424242,1,-17}) {
            for(double scale:new double[]{12,18,28,32}) {
                var before=measure((x,z)->oldPatch(seed,x,z,scale));
                var after=measure((x,z)->SurfacePatches.sample(seed,x,z,scale));
                require(before.fourfold()>.15,"Square-lattice negative control must be detected");
                require(after.fourfold()<.08,"Material boundaries must not prefer four grid directions");
                require(before.contourFourfold()>.12&&after.contourFourfold()<.08,"Thresholded material boundaries retain no square-lattice signature");
                require(after.variation()>.12&&after.variation()<.23,"Retain material contrast");
                require(after.neighborChange()>before.neighborChange()*.5&&after.neighborChange()<before.neighborChange()*1.5,"Retain patch size without blurring or speckling");
                require(Math.abs(after.mean()-.5)<.04,"Retain balanced material field");
                System.out.printf("MATERIAL_PATCH seed=%d scale=%.0f fourfold=%.5f -> %.5f change=%.5f -> %.5f%n",seed,scale,before.fourfold(),after.fourfold(),before.neighborChange(),after.neighborChange());
            }
            var oldSnow=measure((x,z)->2*oldNoise(seed,x/32.,z/32.)-1);
            var snow=measure((x,z)->Taiga.groundPatch(seed,x,z));
            require(oldSnow.fourfold()>.3&&snow.fourfold()<.08,"Snow-depth square-lattice control");
            require(snow.variation()>.3&&snow.variation()<.55,"Snow depth keeps spatial contrast");
            for(int z:new int[]{-2097100,-33,-1,0,15,31,2097100})for(int x:new int[]{-2097100,-33,-1,0,15,31,2097100}) {
                double a=SurfacePatches.sample(seed,x,z,18);
                require(a>=0&&a<=1&&Double.isFinite(a),"Finite bounded surface variation");
                SurfacePatches.sample(seed^17,z,x,28);
                require(a==SurfacePatches.sample(seed,x,z,18),"Seed/order independent sampling at world and chunk boundaries");
            }
            System.out.printf("SNOW_PATCH seed=%d fourfold=%.5f -> %.5f%n",seed,oldSnow.fourfold(),snow.fourfold());
        }
        System.out.println("SURFACE_PATCH_FLAT_CONTROLS_SCALE_SEED_ORDER_PASS");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private SurfacePatchesCheck() {}
}
