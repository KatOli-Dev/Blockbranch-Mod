package blockbranch.realistic;

import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Frozen 212 heights, unchanged drainage, shorter terraces and exact tile clearance. */
public final class GravelReliefCheck {
    private static final int SIDE=128;
    private record Site(long seed,int x,int z,String legacyDigest) {}
    // Authentic pre-opt-in compiled DrainageNetwork; z then x, big-endian x/z/height int tuples.
    private static final Site[] SITES={
        new Site(424242,388028,-1212032,"cf2c3ba161044e4ebafab1d3c53f7122be65f1c6b5be5965f831e1bdbf3a0eb4"),
        new Site(1,-1484024,-1900032,"b4ed737aca909a1b417be46ed90025110180f28fb467c2e2eed9c736ee25e35f"),
        new Site(-17,-1212032,-1756032,"8b157ca786932bc60747ee0c80fbf305e1e9a7b2d2679cc464c8ee71f054a94a")
    };
    public static void main(String[] args)throws Exception {
        clearance();
        centeredWater();
        for(var site:SITES)verify(site);
        System.out.println("GRAVEL_RELIEF_LEGACY_HEIGHT_METADATA_GRAPH_CLEARANCE_ORDER_PASS seeds=3");
    }
    private static void verify(Site site)throws Exception {
        long started=System.nanoTime();
        var legacy=new DrainageNetwork(new TerrainShape(site.seed),site.seed,true,true);
        var fresh=new DrainageNetwork(new TerrainShape(site.seed),site.seed,true,true,true);
        var climate=new ClimateField(site.seed);
        var before=new DrainageNetwork.Sample[SIDE*SIDE];var after=new DrainageNetwork.Sample[before.length];
        int changed=0,minimum=0,maximum=0;
        var digest=MessageDigest.getInstance("SHA-256");
        for(int z=0;z<SIDE;z++)for(int x=0;x<SIDE;x++) {
            int wx=site.x+x-SIDE/2,wz=site.z+z-SIDE/2,i=z*SIDE+x;
            var a=legacy.sample(wx,wz);var b=fresh.sample(wx,wz);before[i]=a;after[i]=b;
            integer(digest,wx);integer(digest,wz);integer(digest,a.height());
            metadata(a,b);int delta=b.height()-a.height();minimum=Math.min(minimum,delta);maximum=Math.max(maximum,delta);
            require(Math.abs(delta)<=3,"Shallow relief exceeds three-block contract");if(delta!=0)changed++;
            require(a.equals(fresh.gravelReference().sample(wx,wz)),"Eligibility reference changed original complete surface");
            require(GravelDesert.contains(wx,wz,legacy,climate)==GravelDesert.contains(wx,wz,fresh,climate),"Gravel biome routing feeds back from new slopes");
            require(GravelDesert.surfaceContains(wx,wz,legacy,climate)==GravelDesert.surfaceContains(wx,wz,fresh,climate),"Physical gravel identity changed");
            if((x&15)==0&&(z&15)==0)require(legacy.channelAt(wx,wz).equals(fresh.channelAt(wx,wz)),"Routed channel geometry changed");
        }
        require(HexFormat.of().formatHex(digest.digest()).equals(site.legacyDigest),"Ordinary constructor changed authentic212 ground");
        graph(legacy,fresh);
        require(changed>before.length/3,"Disabled or ineffective relief negative control");
        int oldRuns=runP90(before),newRuns=runP90(after);
        require(newRuns*3<oldRuns*2,"Relief does not meaningfully shorten long equal-height terraces");
        for(int i=after.length-1;i>=0;i--){int x=site.x+i%SIDE-SIDE/2,z=site.z+i/SIDE-SIDE/2;require(fresh.sample(x,z).equals(after[i]),"Reverse query changed cached surface");}
        wetControls(site.seed,legacy,fresh);
        priorReader(site,before);
        if(site.seed==424242)concurrentCold(site,after);
        System.out.printf(Locale.ROOT,"Gravel relief seed=%d changed=%d/%d offsets=%d..%d runP90=%d->%d elapsedMs=%d%n",site.seed,changed,before.length,minimum,maximum,oldRuns,newRuns,(System.nanoTime()-started)/1_000_000);
    }
    private static void metadata(DrainageNetwork.Sample a,DrainageNetwork.Sample b) {
        require(a.waterLevel()==b.waterLevel()&&a.basin()==b.basin()&&a.closed()==b.closed()
            &&a.bedrockCoast()==b.bedrockCoast()&&a.regionalHeight()==b.regionalHeight()
            &&a.regionalWaterLevel()==b.regionalWaterLevel()&&a.sandyBackshore()==b.sandyBackshore(),"Non-height sample metadata changed");
    }
    private static void graph(DrainageNetwork legacy,DrainageNetwork fresh)throws Exception {
        for(String name:new String[]{"ground","parent","root","area","water","ocean","tangentX","tangentZ"}) {
            var field=DrainageNetwork.class.getDeclaredField(name);field.setAccessible(true);
            require(Objects.deepEquals(field.get(legacy),field.get(fresh)),"Solved drainage graph changed: "+name);
        }
    }
    private static int runP90(DrainageNetwork.Sample[] samples) {
        var runs=new ArrayList<Integer>();
        for(int axis=0;axis<2;axis++)for(int line=0;line<SIDE;line++) {
            int run=1;
            for(int p=1;p<SIDE;p++) {
                int index=axis==0?line*SIDE+p:p*SIDE+line,previous=axis==0?index-1:index-SIDE;
                if(samples[index].height()==samples[previous].height())run++;
                else{runs.add(run);run=1;}
            }
            runs.add(run);
        }
        Collections.sort(runs);return runs.get((runs.size()-1)*9/10);
    }
    private static void concurrentCold(Site site,DrainageNetwork.Sample[] expected)throws Exception {
        var cold=new DrainageNetwork(new TerrainShape(site.seed),site.seed,true,true,true);
        var indices=new ArrayList<Integer>();for(int z=0;z<SIDE;z+=8)for(int x=0;x<SIDE;x+=8)indices.add(z*SIDE+x);
        Collections.reverse(indices);
        try(var pool=Executors.newFixedThreadPool(3)) {
            var jobs=new ArrayList<Callable<Void>>();
            for(int i:indices)jobs.add(()->{int x=site.x+i%SIDE-SIDE/2,z=site.z+i/SIDE-SIDE/2;require(cold.sample(x,z).equals(expected[i]),"Independent cold network/concurrent tile order changed surface");return null;});
            for(var result:pool.invokeAll(jobs))result.get();
        }
    }
    private static void wetControls(long seed,DrainageNetwork legacy,DrainageNetwork fresh) {
        // Actual ocean, lake and shore witnesses, independent of the desert eligibility implementation.
        int[][] points=seed==424242?new int[][]{{-1900000,-1900000},{-765612,-1700131},{-1310210,-1695566}}
            :seed==1?new int[][]{{-1836000,-1900000}}:new int[][]{{-1900000,-1900000},{-1756000,-1900000}};
        for(var p:points)require(legacy.sample(p[0],p[1]).equals(fresh.sample(p[0],p[1])),"Wet/coastal control changed");
    }
    private static void priorReader(Site site,DrainageNetwork.Sample[] expected)throws Exception {
        String archive=System.getProperty("blockbranch.gravelReliefPriorReader","");if(archive.isEmpty())return;
        try(var loader=new URLClassLoader(new java.net.URL[]{Path.of(archive).toUri().toURL()},GravelReliefCheck.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        }) {
            var shape=loader.loadClass("blockbranch.realistic.TerrainShape");var drainage=loader.loadClass("blockbranch.realistic.DrainageNetwork");
            require(drainage!=DrainageNetwork.class,"Historical drainage must be independently loaded");
            var constructor=drainage.getDeclaredConstructor(shape,long.class,boolean.class,boolean.class);constructor.setAccessible(true);
            var old=constructor.newInstance(shape.getConstructor(long.class).newInstance(site.seed),site.seed,true,true);
            var sample=drainage.getMethod("sample",int.class,int.class);
            for(int z=0;z<SIDE;z+=8)for(int x=0;x<SIDE;x+=8)require(sample.invoke(old,site.x+x-SIDE/2,site.z+z-SIDE/2).toString().equals(expected[z*SIDE+x].toString()),"Authentic212 height/metadata control changed");
        }
    }
    private static void clearance() {
        for(int[][] blocked:new int[][][]{new int[0][2],{{0,0}},{{31,31}},{{7,15}},{{8,8}},{{24,16}},{{0,31},{31,0},{15,15}}}) {
            byte[] distance=new byte[32*32];Arrays.fill(distance,(byte)8);for(var p:blocked)distance[p[1]*32+p[0]]=0;
            GravelDesertRelief.clearance(distance);
            for(int z=0;z<32;z++)for(int x=0;x<32;x++)require(distance[z*32+x]==brute(x,z,blocked),"Single-pixel/corner clearance differs from capped Chebyshev distance");
        }
        int[][] obstacles={{-1,0},{16,16},{24,-8},{-50,-50}};
        for(int tz=-1;tz<=1;tz++)for(int tx=-1;tx<=1;tx++) {
            byte[] distance=new byte[32*32];Arrays.fill(distance,(byte)8);
            for(var p:obstacles){int x=p[0]-tx*16+8,z=p[1]-tz*16+8;if(x>=0&&x<32&&z>=0&&z<32)distance[z*32+x]=0;}
            GravelDesertRelief.clearance(distance);
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)require(distance[(z+8)*32+x+8]==brute(tx*16+x,tz*16+z,obstacles),"Tile halo/seam omitted a specialist pixel");
        }
    }
    private static void centeredWater() {
        // World-coordinate point datums are independent of the production tile scan.
        // A halo-wide maximum would incorrectly constrain dry output columns far from a point.
        int[][][] scenes={new int[0][3],{{-1,0,101}},{{16,16,180}},{{23,8,200}},{{-25,-8,-7}},
            {{-25,-8,-7},{-16,-16,143},{-1,0,101},{16,16,180},{23,8,200},{24,-8,220}}};
        int scalarCounterexamples=0;
        for(var points:scenes)for(int tz=-2;tz<=1;tz++)for(int tx=-2;tx<=1;tx++) {
            int[] water=new int[32*32];Arrays.fill(water,DrainageNetwork.DRY);int scalar=DrainageNetwork.DRY;
            for(var p:points) {
                int x=p[0]-tx*16+8,z=p[1]-tz*16+8;
                if(x>=0&&x<32&&z>=0&&z<32){water[z*32+x]=Math.max(water[z*32+x],p[2]);scalar=Math.max(scalar,p[2]);}
            }
            var snapshot=water.clone();int[] actual=GravelDesertRelief.centeredWaterLevels(water);
            require(actual.length==256&&Arrays.equals(water,snapshot),"Centered water query changes input or output dimensions");
            for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                int wx=tx*16+x,wz=tz*16+z,expected=DrainageNetwork.DRY;
                for(var p:points)if(Math.max(Math.abs(wx-p[0]),Math.abs(wz-p[1]))<=8)expected=Math.max(expected,p[2]);
                require(actual[z*16+x]==expected,"Centered wet datum differs across a tile seam or radius boundary at "+wx+","+wz);
                if(expected==DrainageNetwork.DRY&&scalar!=DrainageNetwork.DRY)scalarCounterexamples++;
            }
        }
        require(scalarCounterexamples>0,"Water fixtures no longer reject the halo-wide scalar negative control");
        int[] single=new int[32*32];Arrays.fill(single,DrainageNetwork.DRY);single[8*32+7]=101;
        var levels=GravelDesertRelief.centeredWaterLevels(single);
        require(levels[0]==101&&levels[15]==DrainageNetwork.DRY,"One wet halo pixel must not cap every column in its tile");
    }
    private static int brute(int x,int z,int[][] blocked){int distance=8;for(var p:blocked)distance=Math.min(distance,Math.max(Math.abs(x-p[0]),Math.abs(z-p[1])));return distance;}
    private static void integer(MessageDigest hash,int value){hash.update((byte)(value>>>24));hash.update((byte)(value>>>16));hash.update((byte)(value>>>8));hash.update((byte)value);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private GravelReliefCheck() {}
}
