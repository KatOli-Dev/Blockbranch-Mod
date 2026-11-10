package blockbranch.realistic;

/** Coastal expression of the existing shoulder/ravine landform field.
 * No isolated geometric hill primitives, height summation or capped plateaux. */
final class CoastalHeadlands {
    record Relief(double rise,boolean exposed,double strength) {}
    private final long seed;
    private final TerrainShape shape;
    CoastalHeadlands(long seed,TerrainShape shape){this.seed=seed;this.shape=shape;}
    Relief sample(int x,int z){
        var f=CoastalLandforms.frame(seed,shape,x,z);
        double rise=rise(x,z,f);
        if(rise<.01)return new Relief(0,false,0);
        double dx=(rise(x+4,z,CoastalLandforms.frame(seed,shape,x+4,z))-rise(x-4,z,CoastalLandforms.frame(seed,shape,x-4,z)))/8;
        double dz=(rise(x,z+4,CoastalLandforms.frame(seed,shape,x,z+4))-rise(x,z-4,CoastalLandforms.frame(seed,shape,x,z-4)))/8;
        return new Relief(rise,Math.hypot(dx,dz)>=.32,f.amplitude());
    }
    private double rise(int x,int z,CoastalLandforms.Frame f){
        if(f.amplitude()<.01||f.distance()< -256||f.distance()>=768)return 0;
        // The same intermediate shoulders/ridges/ravines already used inland
        // become the primary rock relief here. Their hollows remain coves,
        // rather than perturbing a pre-existing ramp or radial dome.
        double relief=Math.max(0,shape.surfaceReliefStrength(x,z,1));
        double shift=70*SurfacePatches.signed(seed^0x434F41535446524EL,x,z,280);
        double envelope=smooth(-96+shift,-8+shift,f.distance())*(1-smooth(120,640,f.distance()));
        return f.amplitude()*Math.tanh(relief/48)*envelope;
    }
    private static double smooth(double a,double b,double x){double t=Math.clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);}
}
