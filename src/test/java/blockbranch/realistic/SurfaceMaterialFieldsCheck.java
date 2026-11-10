package blockbranch.realistic;

/** Actual material fields, with their archived square samplers as directional negative controls. */
public final class SurfaceMaterialFieldsCheck {
    @FunctionalInterface interface Field { double sample(long seed,int x,int z); }
    record Selector(String name,int scale,Field actual,Field archived) {}
    record Stats(double fourfold,double variance,double change) {}

    static Stats measure(Field field,long seed,int scale) {
        double cs=0,sn=0,weight=0,sum=0,squares=0,change=0;
        int stride=Math.max(1,scale/16),n=384;
        for(int z=-n/2;z<n/2;z++)for(int x=-n/2;x<n/2;x++){
            int wx=x*stride,wz=z*stride;double value=field.sample(seed,wx,wz);
            double dx=field.sample(seed,wx+stride,wz)-field.sample(seed,wx-stride,wz);
            double dz=field.sample(seed,wx,wz+stride)-field.sample(seed,wx,wz-stride);
            double magnitude=Math.hypot(dx,dz),angle=4*Math.atan2(dz,dx);
            cs+=magnitude*Math.cos(angle);sn+=magnitude*Math.sin(angle);weight+=magnitude;
            sum+=value;squares+=value*value;change+=magnitude;
        }
        double count=n*n,mean=sum/count;
        return new Stats(Math.hypot(cs,sn)/weight,Math.sqrt(squares/count-mean*mean),change/count);
    }
    static double archivedSwamp(long seed,long salt,int x,int z,int scale){
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);
        double a=fade(Math.floorMod(x,scale)/(double)scale),b=fade(Math.floorMod(z,scale)/(double)scale);
        return (value(seed^salt,gx,gz)*(1-a)+value(seed^salt,gx+1,gz)*a)*(1-b)
            +(value(seed^salt,gx,gz+1)*(1-a)+value(seed^salt,gx+1,gz+1)*a)*b;
    }
    static double fade(double a){return a*a*a*(a*(a*6-15)+10);}
    static double value(long seed,int x,int z){return(OakBirchWoodland.mix(seed^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53*2-1;}
    public static void main(String[] args){
        Selector[] selectors={
            new Selector("lake",32,LakeSediment::materialPatch,(s,x,z)->OakBirchWoodland.stand(s^0x4C414B45424544L,x*12,z*12)),
            new Selector("shelf",64,MarineShelf::materialPatch,(s,x,z)->OakBirchWoodland.stand(s^0x5348454C46424544L,x*6,z*6)),
            new Selector("deep",192,DeepOcean::materialPatch,(s,x,z)->OakBirchWoodland.stand(s^0x444545504245444CL,x*2,z*2)),
            new Selector("ocean-rock",96,TemperateOcean::substrate,(s,x,z)->OakBirchWoodland.stand(s^0x4F4345414E524F43L,x*4,z*4)),
            new Selector("mangrove",24,(s,x,z)->MangroveSwamp.patch(s,x,z,24),(s,x,z)->{
                int gx=Math.floorDiv(x,24),gz=Math.floorDiv(z,24);double a=Math.floorMod(x,24)/24.,b=Math.floorMod(z,24)/24.;a=a*a*(3-2*a);b=b*b*(3-2*b);
                return(mangroveValue(s,gx,gz)*(1-a)+mangroveValue(s,gx+1,gz)*a)*(1-b)+(mangroveValue(s,gx,gz+1)*(1-a)+mangroveValue(s,gx+1,gz+1)*a)*b;
            }),
            swamp("swamp-soil",0x5357534F494CL,24),swamp("swamp-mud",0x53574D5544L,12),swamp("swamp-moss",0x53574D4F5353L,18)
        };
        for(long seed:new long[]{424242,1,-17})for(var selector:selectors){
            var old=measure(selector.archived(),seed,selector.scale());var fresh=measure(selector.actual(),seed,selector.scale());
            require(old.fourfold()>.12,"Archived square material control: "+selector.name());
            require(fresh.fourfold()<.10,"No four-direction material bias: "+selector.name()+" "+fresh);
            require(fresh.variance()>old.variance()*.5&&fresh.variance()<old.variance()*1.6,"Retain contrast: "+selector.name());
            require(fresh.change()>old.change()*.45&&fresh.change()<old.change()*1.8,"Retain patch scale: "+selector.name());
            for(int x:new int[]{-2097100,-33,-1,0,15,31,2097100}){
                double a=selector.actual().sample(seed,x,-x);selector.actual().sample(seed^17,x+16,-x-16);
                require(Double.isFinite(a)&&a==selector.actual().sample(seed,x,-x),"World/chunk/order consistency: "+selector.name());
            }
            System.out.printf("SURFACE_FIELD %s seed=%d fourfold=%.5f -> %.5f%n",selector.name(),seed,old.fourfold(),fresh.fourfold());
        }
        System.out.println("SURFACE_MATERIAL_FIELDS_ARCHIVED_CONTROLS_PASS");
    }
    static Selector swamp(String name,long salt,int scale){return new Selector(name,scale,(s,x,z)->Swamp.patch(s,salt,x,z,scale),(s,x,z)->archivedSwamp(s,salt,x,z,scale));}
    static double mangroveValue(long seed,int x,int z){return(OakBirchWoodland.mix(seed^0x4D414E47534F494CL^x*1297L^z*1237L)>>>11)*0x1.0p-53*2-1;}
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
