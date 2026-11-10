package blockbranch.realistic;

import java.lang.management.ManagementFactory;
import java.util.*;

/** Bounded experiment. Analytic collision envelope is not a native movement test. */
public final class NetworkCaveExperiment {
    private static volatile long sink;
    private static Object retainedField,retainedTerrain;
    static void require(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        if(args.length>0&&args[0].equals("bench")){bench(args[1]);return;}
        for(long seed:new long[]{424242,1,-17})check(seed);
        System.out.println("PROTOTYPE_ANALYTIC_PASS native generation/loading/player physics remain untested");
    }
    static void check(long seed){
        var field=new NetworkCavePrototype(seed,(x,z)->80);
        double total=0,designedWalk=0,clearWalk=0;int networks=0,entrances=0,regionalMin=16,regionalMax=0;
        for(int rz=-1;rz<=1;rz++)for(int rx=-1;rx<=1;rx++){
            int regional=0;
            for(int dz=0;dz<4;dz++)for(int dx=0;dx<4;dx++){
                int nx=rx*4+dx,nz=rz*4+dz;networks++;if(field.entranceNetwork(nx,nz)){entrances++;regional++;}
                for(int band=0;band<3;band++)for(var s:field.band(nx,band,nz)){
                    // Connection declared by both owners counts once: canonical western owner.
                    if(!s.entrance()&&s.walkableDesign()&&s.a().x()<nx*256)continue;
                    total+=s.length();if(s.walkableDesign()){
                        designedWalk+=s.length();int n=(int)Math.ceil(s.length()),ok=0;
                        for(int k=0;k<n;k++){
                            double t=(k+.5)/n;int x=(int)Math.round(s.a().x()+(s.b().x()-s.a().x())*t),z=(int)Math.round(s.a().z()+(s.b().z()-s.a().z())*t);
                            int y=(int)Math.round(s.a().y()+(s.b().y()-s.a().y())*t);
                            boolean usable=false;
                            for(int dy=-1;dy<=1;dy++)if(air(field,x,y+dy,z)&&air(field,x,y+dy+1,z)&&!air(field,x,y+dy-1,z))usable=true;
                            if(usable)ok++;
                        }
                        clearWalk+=s.length()*ok/n;
                    }
                }
            }
            regionalMin=Math.min(regionalMin,regional);regionalMax=Math.max(regionalMax,regional);
            require(regional==4||regional==8||regional==12,"Exact entrance assignment quota per sixteen IDs");
        }
        require(designedWalk/total>.65&&designedWalk/total<.8,"Prototype length mix near target");
        var fresh=new NetworkCavePrototype(seed,(x,z)->80);
        int probes=0,voids=0;
        for(int z=-273;z<273;z+=7)for(int x=-273;x<273;x+=7)for(int y=-210;y<85;y+=11){
            boolean a=air(field,x,y,z);require(a==air(fresh,x,y,z),"Fresh descriptor/query-order repeatability");
            if(a)voids++;probes++;
            if(y>=56&&a){
                var section=field.section(Math.floorDiv(x,16),Math.floorDiv(y,16),Math.floorDiv(z,16));
                boolean deliberate=false;
                for(var segment:section.segments())deliberate|=segment.entrance()&&segment.contains(x,y,z);
                require(deliberate,"Protected roof changes only in explicit entrance footprint");
            }
            if(Math.floorMod(x,16)==0)require(a==reference(field,x,y,z),"Negative/positive section clipping seam");
        }
        require(voids>0&&voids<probes*.25,"Rock-dominant bounded sample");
        require(field.peakDescriptors<=NetworkCavePrototype.CACHE_LIMIT,"Hard descriptor retention bound");
        for(int nz=-3;nz<=3;nz++)for(int nx=-3;nx<=3;nx++)if(field.entranceNetwork(nx,nz)){
            int mx=nx*256+48,mz=nz*256+48;
            require(air(field,mx,81,mz)&&air(field,mx,82,mz),"Assigned dry network has standing-clear mouth");
        }
        System.out.printf(Locale.ROOT,"METRIC seed=%d networks=%d entranceNetworks=%d regionMin=%d/16 regionMax=%d/16 totalLength=%.3f designedWalkLength=%.3f supportedCentrelineLength=%.3f designedFraction=%.6f supportedFraction=%.6f voids=%d/%d peakDescriptors=%d%n",seed,networks,entrances,regionalMin,regionalMax,total,designedWalk,clearWalk,designedWalk/total,clearWalk/total,voids,probes,field.peakDescriptors);
    }
    static boolean air(NetworkCavePrototype field,int x,int y,int z){return field.section(Math.floorDiv(x,16),Math.floorDiv(y,16),Math.floorDiv(z,16)).voidAt(x,y,z,80);}
    // Independent of section bounding-box clipping, covering exactly the bounded local candidates.
    static boolean reference(NetworkCavePrototype field,int x,int y,int z){
        if(y<=TerrainShape.FLOOR+32||y>85)return false;
        int nx=Math.floorDiv(x,256),nz=Math.floorDiv(z,256);
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            int base=Math.floorDiv(field.anchorSurface(nx+dx,nz+dz)-64-y,192);
            for(int db=-1;db<=1;db++)for(var s:field.band(nx+dx,base+db,nz+dz))
                if((s.entrance()||y<56)&&s.contains(x,y,z))return true;
        }
        return false;
    }
    static void bench(String mode){
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
        int[][] witnesses={{237582,-277,-262158},{57336,-214,-57376},{-122868,-285,180198}};int witnessIndex=0;
        for(long seed:new long[]{424242,1,-17}){
            int[] witness=witnesses[witnessIndex++];
            int nx=Math.floorDiv(witness[0],256),nz=Math.floorDiv(witness[2],256);
            int sx0=nx*16,sz0=nz*16;
            // Real shared terrain sampling, prepared outside either timed geometry path.
            var terrain=new DrainageNetwork(new TerrainShape(seed),seed,true,true);
            int[] ground=new int[256*256];
            for(int z=0;z<256;z++)for(int x=0;x<256;x++)ground[z*256+x]=terrain.sample(x+sx0*16,z+sz0*16).height();
            for(int trial=-2;trial<4;trial++){
                var baseline=new UndergroundStructure(seed);
                var proto=new NetworkCavePrototype(seed,(x,z)->terrain.sample(x,z).height());
                long allocated0=bean.getThreadAllocatedBytes(Thread.currentThread().threadId()),start=System.nanoTime(),count=0;
                // Same three requested bands per seed, same block and section iteration.
                for(int origin:new int[]{ground[128*256+128]-48,witness[1],proto.anchorSurface(nx,nz)-64-192})for(int sz=sz0;sz<sz0+16;sz++)for(int sx=sx0;sx<sx0+16;sx++)for(int dy=0;dy<3;dy++){
                    int sy=Math.floorDiv(origin,16)-dy;
                    var section=mode.equals("prototype")?proto.section(sx,sy,sz):null;
                    for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                        int wx=sx*16+x,wy=sy*16+y,wz=sz*16+z,surface=ground[(wz-sz0*16)*256+wx-sx0*16];
                        if(section==null?baseline.voidAt(wx,wy,wz,surface):section.voidAt(wx,wy,wz,surface))count++;
                    }
                }
                long elapsed=System.nanoTime()-start,allocated=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocated0;sink=count;
                require(count>0,"Benchmark must actually intersect generated cave space");
                if(trial>=0)System.out.printf(Locale.ROOT,"BENCH mode=%s seed=%d trial=%d ns=%d allocatedBytes=%d voids=%d blocks=9437184 peakDescriptors=%d builtDescriptors=%d%n",mode,seed,trial,elapsed,allocated,count,proto.peakDescriptors,proto.constructedDescriptors);
                retainedField=mode.equals("prototype")?proto:baseline;retainedTerrain=terrain;
            }
        }
        System.out.println("BENCH_SINK "+sink);
        try{
            for(String line:java.nio.file.Files.readAllLines(java.nio.file.Path.of("/proc/self/status")))
                if(line.startsWith("VmHWM:")||line.startsWith("VmRSS:"))System.out.println("PROCESS_MEMORY "+line);
        }catch(java.io.IOException e){System.out.println("PROCESS_MEMORY unavailable");}
        System.gc();
        System.out.println("POST_GC_HEAP_BYTES "+ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
    }
}
