package blockbranch.realistic;

import java.util.*;

/** Finite branching traces. No plate ownership, closed-cell tessellation or dynamics. */
final class SeaIceFractures {
    private static final int CELL=320,RADIUS=5,LIMIT=2048;
    private record Key(long seed,int x,int z){}
    record Segment(double ax,double az,double bx,double bz,double aw,double bw){
        double clearance(double x,double z){double dx=bx-ax,dz=bz-az;
            double t=Math.clamp(((x-ax)*dx+(z-az)*dz)/(dx*dx+dz*dz),0,1);
            return Math.hypot(x-ax-t*dx,z-az-t*dz)-(aw+t*(bw-aw));}
    }
    private static final LinkedHashMap<Key,List<Segment>> BUNDLES=new LinkedHashMap<>(128,.75f,true);
    private static final LinkedHashMap<Key,List<Segment>> CACHE=new LinkedHashMap<>(128,.75f,true);
    static boolean ice(long seed,int x,int z,double concentration){
        if(concentration<=0)return false;
        return clearance(seed,x,z,false)>640*Math.pow(1-concentration,3);
    }
    static double clearance(long seed,int x,int z,boolean reverse){
        int gx=Math.floorDiv(x,CELL),gz=Math.floorDiv(z,CELL);double best=Double.POSITIVE_INFINITY;
        // Include trace support plus the maximum640m margin opening. The5-cell
        // halo is independent of chunk boundaries and includes every influence.
        var traces=bundle(seed,gx,gz);
        for(int i=0;i<traces.size();i++){var s=traces.get(reverse?traces.size()-1-i:i);
            double pad=Math.max(s.aw(),s.bw())+Math.max(0,best);
            if(Double.isFinite(best)&&(x<Math.min(s.ax(),s.bx())-pad||x>Math.max(s.ax(),s.bx())+pad||z<Math.min(s.az(),s.bz())-pad||z>Math.max(s.az(),s.bz())+pad))continue;
            best=Math.min(best,s.clearance(x,z));
        }
        return best;
    }
    private static List<Segment> bundle(long seed,int gx,int gz){
        var key=new Key(seed,gx,gz);synchronized(BUNDLES){var found=BUNDLES.get(key);if(found!=null)return found;}
        var all=new ArrayList<Segment>();for(int dz=-RADIUS;dz<=RADIUS;dz++)for(int dx=-RADIUS;dx<=RADIUS;dx++)all.addAll(cell(seed,gx+dx,gz+dz));var result=List.copyOf(all);
        synchronized(BUNDLES){BUNDLES.put(key,result);if(BUNDLES.size()>512)BUNDLES.remove(BUNDLES.keySet().iterator().next());}return result;
    }
    private static List<Segment> cell(long seed,int gx,int gz){
        var key=new Key(seed,gx,gz);synchronized(CACHE){var found=CACHE.get(key);if(found!=null)return found;}
        var result=generate(seed,gx,gz);
        synchronized(CACHE){CACHE.put(key,result);if(CACHE.size()>LIMIT)CACHE.remove(CACHE.keySet().iterator().next());}return result;
    }
    static List<Segment> generate(long seed,int gx,int gz){
        long h=OakBirchWoodland.mix(seed^gx*0x9E3779B97F4A7C15L^gz*0xC2B2AE3D27D4EB4FL);
        double x=(gx+.15+.7*unit(h))*CELL,z=(gz+.15+.7*unit(h^17))*CELL;
        // A regional stress-direction convention, not a calculated stress field.
        double angle=Math.PI*unit(seed^0x535452455353L)+.35*SurfacePatches.signed(seed^29,(int)x,(int)z,4000);
        if(Long.remainderUnsigned(h,7)==0)angle+=1.25;
        double length=440+400*unit(h^31);var out=new ArrayList<Segment>();
        double px=x-.5*length*Math.cos(angle),pz=z-.5*length*Math.sin(angle),pw=0;
        for(int i=1;i<=8;i++){
            long v=OakBirchWoodland.mix(h+i*0x165667B19E3779F9L);
            double heading=angle+(.5*unit(v)-.25),step=length/8*(.8+.4*unit(v^41));
            double nx=px+step*Math.cos(heading),nz=pz+step*Math.sin(heading);
            double nw=i==8?0:(1.4+3.6*unit(v^47))*Math.sqrt(Math.sin(Math.PI*i/8));
            out.add(new Segment(px,pz,nx,nz,pw,nw));
            if((i==3||i==6)&&Long.remainderUnsigned(v^53,3)!=0)branch(out,nx,nz,nw,heading+(i==3?1:-1)*(.6+.65*unit(v^59)),v);
            px=nx;pz=nz;pw=nw;
        }
        for(var s:out)for(double[] p:new double[][]{{s.ax(),s.az()},{s.bx(),s.bz()}})if(Math.abs(p[0]-x)>900||Math.abs(p[1]-z)>900)throw new IllegalStateException("Fracture support exceeds lookup bound");
        return List.copyOf(out);
    }
    private static void branch(List<Segment> out,double x,double z,double width,double angle,long h){
        int count=3+(int)Long.remainderUnsigned(h,3);double previous=width*.65;
        for(int i=1;i<=count;i++){
            long v=OakBirchWoodland.mix(h+i*0xD1B54A32D192ED03L);double heading=angle+.4*(unit(v)-.5),length=35+30*unit(v^67);
            double nx=x+length*Math.cos(heading),nz=z+length*Math.sin(heading),next=i==count?0:width*.65*(1-i/(double)count)*(.8+.4*unit(v^71));
            out.add(new Segment(x,z,nx,nz,previous,next));x=nx;z=nz;previous=next;
        }
    }
    private static double unit(long h){return(OakBirchWoodland.mix(h)>>>11)*0x1.0p-53;}
    private SeaIceFractures(){}
}
