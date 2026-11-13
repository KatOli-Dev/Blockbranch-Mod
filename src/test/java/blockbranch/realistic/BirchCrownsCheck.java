package blockbranch.realistic;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Saved-gate numbering, the frozen published old-growth-birch canopy and the gated mature-birch crown morphology.
 * The published mature stem is one tall fused ovoid, a side lobe and a stack of tip balls around an upright pole; the gated stem
 * carries graded ascending limbs with separate, flatter lobes. Both variants are measured on the same stems. Metrics are diagnostics for a
 * bounded change (same height and cohorts, comparable leaf mass, attached, inside the shared grid, every other family untouched),
 * not visual approval and not an ecological claim.
 */
public final class BirchCrownsCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    /** Old-growth-birch windows of the frozen fixture: seed, x, z. */
    private static final long[][] SITES={{424242,1360000,-1328000},{424242,1264000,-1296000},{1,-684000,1364000},{1,1492000,-940000}};
    public static void main(String[] args)throws Exception{
        schema();
        published();
        otherFamilies();
        morphology();
        System.out.println("BIRCH_CROWNS_GATED_PASS");
    }
    /** Schema 276 exists only as the complete seasonal-crown 275 plus the explicit definition; bit 1024 is single and distinct. */
    private static void schema(){
        for(int saved=0;saved<=300;saved++){
            require(BirchCrownSchema.baseSchema(saved,0)==(saved==276?Integer.MAX_VALUE:saved),"Absent definition never reads 276 ("+saved+")");
            require(BirchCrownSchema.matches(saved,1)==(saved==276),"Only 276 reads under the definition ("+saved+")");
        }
        require(BirchCrownSchema.schema(275,1)==276&&BirchCrownSchema.baseSchema(276,1)==275,"Complete independent wrapper");
        require(BirchCrownSchema.schema(274,0)==274&&BirchCrownSchema.schema(275,0)==275,"Absent definition leaves the schema");
        for(int base:new int[]{270,272,273,274,276})requireThrows(()->BirchCrownSchema.schema(base,1),"Wrapper rejects incomplete or future base "+base);
        requireThrows(()->BirchCrownSchema.schema(275,2),"Unknown definition rejected");
        requireThrows(()->BirchCrownSchema.schema(275,-1),"Negative definition rejected");
        // The schema chain: each earlier wrapper keeps its own number and its own bit.
        require(SeasonalCrownSchema.schema(274,1)==275&&BroadleafCrownSchema.schema(273,1)==274,"Earlier wrappers keep their numbers");
        int[] bits={BroadleafCrowns.FLAG,SeasonalCrowns.FLAG,BorealSlope.FLAG,MontaneForest.FLAG,SeasonalForestExpansion.FLAG,DryForest.FLAG,SummerDryWoodland.FLAG,RedwoodForest.FLAG};
        require(BirchCrowns.FLAG==1024&&BirchCrowns.enabled(1024|3)&&!BirchCrowns.enabled(1023)&&!BirchCrowns.enabled(2048|512|256),"Flag bit 1024 is its own meaning");
        for(int bit:bits)require((bit&BirchCrowns.FLAG)==0,"Birch bit does not reuse bit "+bit);
        require(BroadleafCrowns.FLAG==256&&SeasonalCrowns.FLAG==512,"Earlier bits keep their permanent meaning");
    }
    /** Every frozen published window and tree, with the birch bit absent, stays bit-identical; the bit changes only mature old-growth birch. */
    private static void published()throws Exception{
        int windows=0,changed=0,controls=0,treeRows=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(BirchCrownsCheck.class.getResourceAsStream("/fixtures/birch-crowns-275/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");
                if(f[0].equals("trees")){
                    int family=Integer.parseInt(f[1].substring(7));boolean birch=Boolean.parseBoolean(f[2].substring(6));
                    var row=trees(family,birch,0);
                    require(row.equals(f[3].substring(6)+" "+f[4].substring(7)+" "+f[5]),"Published trees changed: "+line+" now "+row);
                    treeRows++;continue;
                }
                long seed=Long.parseLong(f[1]);int x=Integer.parseInt(f[2]),z=Integer.parseInt(f[3]),flags=Integer.parseInt(f[4]);
                var row=window(seed,x,z,flags);
                require(row.equals(f[5]+" "+f[6]),"Published columns changed at "+f[0]+" flags "+flags+": "+row+" vs frozen "+f[5]+" "+f[6]);
                var gated=window(seed,x,z,flags|BirchCrowns.FLAG);
                if(f[0].startsWith("birch-")){require(!gated.equals(row),"The gate changed nothing at "+f[0]+" flags "+flags);changed++;}
                else{require(gated.equals(row),"The birch bit changed the unchanged control "+f[0]+" flags "+flags);require(Integer.parseInt(f[5])>3000,"Control window holds trees: "+f[0]);controls++;}
                windows++;
            }
        }
        require(windows==32&&changed==16&&controls==16&&treeRows==6,"Every frozen row is checked: "+windows+" windows, "+changed+" birch, "+controls+" controls, "+treeRows+" tree rows");
    }
    private static final Map<Long,DrainageNetwork> TERRAIN=new HashMap<>();private static final Map<Long,ClimateField> CLIMATE=new HashMap<>();
    private static DrainageNetwork terrain(long seed){return TERRAIN.computeIfAbsent(seed,BorealSlopeOracle::terrain);}
    private static ClimateField climate(long seed){return CLIMATE.computeIfAbsent(seed,k->new ClimateField(k,true));}
    private static String window(long seed,int sx,int sz,int flags)throws Exception{
        var d=terrain(seed);var c=climate(seed);var md=MessageDigest.getInstance("SHA-256");int n=0;
        int cx=Math.floorDiv(sx,16),cz=Math.floorDiv(sz,16);
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){int[] col=WoodlandTrees.column(seed,cx+dx,cz+dz,d,c,true,flags,null,null,null);n+=col.length;md.update(Arrays.toString(col).getBytes());}
        return n+" "+HexFormat.of().formatHex(md.digest());
    }
    /** The frozen tree digest recipe: 27 heights x 24 variants x flags 0/256/512/768 (plus {@code extra} added to each flag set). */
    private static String trees(int family,boolean birch,int extra)throws Exception{
        var md=MessageDigest.getInstance("SHA-256");int voxels=0,roots=0;
        for(int h=5;h<=31;h++)for(int i=0;i<24;i++){
            var root=new WoodlandTrees.Root(0,0,0,birch,h,(int)OakBirchWoodland.mix(i*7919L+h*104729L+family),family);
            for(int flags:new int[]{0,256,512,768}){var t=WoodlandTrees.tree(root,flags|extra);md.update((flags+":"+t.size()+":").getBytes());for(var v:t)md.update((v.x()+","+v.y()+","+v.z()+","+v.code()+";").getBytes());voxels+=t.size();}
            roots++;
        }
        return roots+" "+voxels+" "+HexFormat.of().formatHex(md.digest());
    }
    private static List<WoodlandTrees.Root> stems(){
        var out=new ArrayList<WoodlandTrees.Root>();
        for(var s:SITES){var d=terrain(s[0]);var c=climate(s[0]);
            for(var r:WoodlandTrees.roots(s[0],(int)s[1]-96,(int)s[2]-96,(int)s[1]+96,(int)s[2]+96,d,c,0))if(r.family()==TemperateForestVariants.BIRCH_FAMILY)out.add(r);}
        return out;
    }
    /** Only mature old-growth birch changes; young stems, oak recruits, other forest birch/oak and every other family keep the published voxels. */
    private static void otherFamilies()throws Exception{
        int mature=0,young=0,recruits=0;
        for(var root:stems()){
            var old=ForestTrees.tree(root,false,false);var gated=ForestTrees.tree(root,false,false,true);
            require(root.family()==TemperateForestVariants.BIRCH_FAMILY,"Old-growth sites yield family 9 only: "+root.family());
            if(root.birch()&&root.height()>=BirchCrowns.MATURE_HEIGHT){mature++;require(!old.equals(gated),"Gate reshapes every mature birch: "+root);}
            else{if(root.birch())young++;else recruits++;require(old.equals(gated),"Unchanged stem changed: "+root);}
            for(int flags:new int[]{0,256,512,768})require(WoodlandTrees.tree(root,flags).equals(old)&&WoodlandTrees.tree(root,flags|BirchCrowns.FLAG).equals(gated),"Saved flags route through WoodlandTrees: "+root);
            require(ForestTrees.tree(root,true,true,true).equals(gated),"The oak and dry-forest requests do not touch family 9");
        }
        require(mature>=300&&young>=100&&recruits>=20,"Enough stems: mature "+mature+" young "+young+" oak recruits "+recruits);
        // Tree-level controls over each family's own supported dimensions. Family 0 (the historical generic woodland shape) only exists at
        // oak heights 10..12 and 14..20 and birch heights 9..11 and 12..16 (shape() does not terminate below its generated range), so
        // it is exercised there; ForestTrees itself takes any height, so the other forest families are swept over 5..31 directly.
        int reshaped=0,controls=0;
        for(int family:new int[]{1,2,TemperateForestVariants.BIRCH_FAMILY})for(boolean birch:new boolean[]{false,true})for(int height=5;height<=31;height++){
            var root=new WoodlandTrees.Root(0,0,0,birch,height,1234567+height,family);
            boolean differs=!WoodlandTrees.tree(root,0).equals(WoodlandTrees.tree(root,BirchCrowns.FLAG));
            boolean expected=family==TemperateForestVariants.BIRCH_FAMILY&&birch&&height>=BirchCrowns.MATURE_HEIGHT;
            require(differs==expected,"Birch bit only reshapes mature family 9 birch: family "+family+" birch "+birch+" h "+height+" differs "+differs);
            if(differs)reshaped++;else controls++;
        }
        require(reshaped==31-BirchCrowns.MATURE_HEIGHT+1&&controls==3*2*27-reshaped,"Exactly the mature family-9 birch heights reshape: "+reshaped+" of "+(reshaped+controls));
        int generic=0;
        for(boolean birch:new boolean[]{false,true})for(int height=birch?9:10;height<=(birch?16:20);height++){
            if(!birch&&height==13)continue;
            var root=new WoodlandTrees.Root(0,0,0,birch,height,1234567+height,0);
            require(WoodlandTrees.tree(root,0).equals(WoodlandTrees.tree(root,BirchCrowns.FLAG|BroadleafCrowns.FLAG|SeasonalCrowns.FLAG)),"Family 0 ignores every crown bit: birch "+birch+" h "+height);
            require(!WoodlandTrees.tree(root,0).isEmpty(),"Family 0 control is nonvacuous");generic++;
        }
        require(generic==18,"Family 0 controls: "+generic);
        int others=0;
        for(int family:new int[]{DappledForest.FAMILY,SummerDryWoodland.FAMILY,DarkCherryWoodland.DARK_FAMILY,DarkCherryWoodland.CHERRY_FAMILY,DryForest.FAMILY})for(boolean birch:new boolean[]{false,true})for(int height:new int[]{9,16,24,31}){
            var root=new WoodlandTrees.Root(0,0,0,birch,height,1234567,family);
            require(ForestTrees.tree(root,false,false,true).equals(ForestTrees.tree(root,false,false,false)),"ForestTrees birch request ignores family "+family+" birch "+birch+" h "+height);others++;
        }
        require(others==40,"Other-family controls: "+others);
    }
    /** Same stems: crown top, leaf mass stay comparable, limbs attach, extents stay inside the shared grid, crowns are less columnar. */
    private static void morphology(){
        var leaves=new ArrayList<Double>();var logsRatio=new ArrayList<Double>();var widthOld=new ArrayList<Double>();var widthNew=new ArrayList<Double>();var fillOld=new ArrayList<Double>();var fillNew=new ArrayList<Double>();var ratioOld=new ArrayList<Double>();var ratioNew=new ArrayList<Double>();var lowOld=new ArrayList<Double>();var lowNew=new ArrayList<Double>();
        var reach=new ArrayList<Double>();var reachOld=new ArrayList<Double>();int n=0;
        for(var root:stems()){
            if(!root.birch()||root.height()<BirchCrowns.MATURE_HEIGHT)continue;
            var a=measure(root,ForestTrees.tree(root,false,false));var b=measure(root,ForestTrees.tree(root,false,false,true));n++;
            require(b.top>=root.height()-2&&b.top<=root.height()+1,"Crown top stays at the tree height: "+b.top+" vs "+root.height());
            require(b.top>=a.top-2&&b.top<=a.top+1,"Crown top near the published top");
            require(b.attached,"Every log of the gated stem is face-connected to its trunk base at "+root);
            require(b.maxExtent<=7,"Voxels stay inside the shared tree grid (radius 8) with a margin: "+b.maxExtent+" vs published "+a.maxExtent);reach.add(b.maxExtent);reachOld.add(a.maxExtent);
            require(b.lowestLeaf>=a.lowestLeaf-3,"No foliage far below the published crown base: "+b.lowestLeaf+" vs "+a.lowestLeaf+" of "+root.height());
            leaves.add(b.leaves/(double)a.leaves);logsRatio.add(b.logs/(double)a.logs);widthOld.add((double)a.width);widthNew.add((double)b.width);
            fillOld.add(a.leaves/(double)a.box);fillNew.add(b.leaves/(double)b.box);
            ratioOld.add(a.depth/(double)a.width);ratioNew.add(b.depth/(double)b.width);lowOld.add(a.lowestLeaf/(double)root.height());lowNew.add(b.lowestLeaf/(double)root.height());
        }
        require(n>=300,"Enough mature stems measured: "+n);
        double leafMedian=median(leaves),logMedian=median(logsRatio);
        require(leafMedian>.95&&leafMedian<1.15,"Leaf mass comparable, not a blanket reduction: median ratio "+leafMedian);
        require(logMedian>1.0&&logMedian<1.6,"Limb wood bounded: median ratio "+logMedian);
        require(median(widthNew)-median(widthOld)<=3&&median(widthNew)>=median(widthOld),"Spread widens by at most three blocks: "+median(widthOld)+" -> "+median(widthNew));
        require(median(fillNew)<median(fillOld),"Lobed crowns fill less of their box: "+median(fillOld)+" -> "+median(fillNew));
        require(median(ratioNew)<median(ratioOld),"Crowns are less column-like (depth over width): "+median(ratioOld)+" -> "+median(ratioNew));
        require(Math.abs(median(lowNew)-median(lowOld))<=.06,"The crown base keeps its published height fraction: "+median(lowOld)+" -> "+median(lowNew));
        System.out.printf("BIRCH_CROWNS_MORPHOLOGY stems=%d leafRatio=%.3f logRatio=%.3f width %.0f->%.0f boxFill %.3f->%.3f depth/width %.2f->%.2f lowestLeaf/h %.2f->%.2f maxReach median %.0f->%.0f max %.0f->%.0f%n",n,leafMedian,logMedian,median(widthOld),median(widthNew),median(fillOld),median(fillNew),median(ratioOld),median(ratioNew),median(lowOld),median(lowNew),median(reachOld),median(reach),Collections.max(reachOld),Collections.max(reach));
    }
    private record Metrics(int top,int leaves,int logs,int width,int lowestLeaf,int depth,double maxExtent,int box,boolean attached){}
    private static Metrics measure(WoodlandTrees.Root root,List<WoodlandTrees.Voxel> tree){
        int top=0,leaves=0,logs=0,lo=99,hi=-1,x0=99,x1=-99,z0=99,z1=-99;double extent=0;var wood=new HashSet<Pos>();var minX=new int[ForestTrees.MAX_HEIGHT];var maxX=new int[ForestTrees.MAX_HEIGHT];Arrays.fill(minX,99);Arrays.fill(maxX,-99);
        for(var v:tree){
            int x=v.x()-root.x(),y=v.y()-root.y(),z=v.z()-root.z();top=Math.max(top,y);extent=Math.max(extent,Math.max(Math.abs(x),Math.abs(z)));
            if(v.code()<7){logs++;wood.add(key(x,y,z));continue;}
            leaves++;lo=Math.min(lo,y);hi=Math.max(hi,y);minX[y]=Math.min(minX[y],x);maxX[y]=Math.max(maxX[y],x);x0=Math.min(x0,x);x1=Math.max(x1,x);z0=Math.min(z0,z);z1=Math.max(z1,z);
        }
        int width=0;for(int y=0;y<minX.length;y++)if(maxX[y]>=minX[y])width=Math.max(width,maxX[y]-minX[y]+1);
        var seen=new HashSet<Pos>();var queue=new ArrayDeque<int[]>();
        if(wood.contains(key(0,0,0))){queue.add(new int[]{0,0,0});seen.add(key(0,0,0));}
        while(!queue.isEmpty()){var p=queue.poll();for(var s:DIR){int x=p[0]+s[0],y=p[1]+s[1],z=p[2]+s[2];var k=key(x,y,z);if(wood.contains(k)&&seen.add(k))queue.add(new int[]{x,y,z});}}
        return new Metrics(top,leaves,logs,width,lo,hi-lo+1,extent,(x1-x0+1)*(z1-z0+1)*(hi-lo+1),seen.size()==wood.size()&&!wood.isEmpty());
    }
    private static double median(List<Double> values){var v=new ArrayList<>(values);Collections.sort(v);return v.get(v.size()/2);}
    private record Pos(int x,int y,int z){}
    private static Pos key(int x,int y,int z){return new Pos(x,y,z);}
    private static void requireThrows(Runnable r,String message){try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private BirchCrownsCheck(){}
}
