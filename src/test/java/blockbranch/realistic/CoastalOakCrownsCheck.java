package blockbranch.realistic;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Saved-gate numbering, the frozen published coastal-oak canopy and the gated mature coastal winter-rain oak morphology.
 * The published mature oak is a near-vertical pole under overlapping stacked crowns; the gated oak leans to a fork, usually divides into two
 * diverging stems, and carries 3..5 limbs that each end in a separate lobe. Both variants are measured on the same real stems (the pure owner
 * is {@link SummerDryWoodland#eligible}, the surface test the saved owner selects from; the generator's own owner is only exercised by the native
 * qualification). Metrics are diagnostics for a bounded change (same root heights, cohorts and positions, comparable leaf mass, attached
 * wood, inside the generation halo and below the root height), not visual approval and not an ecological claim.
 */
public final class CoastalOakCrownsCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    /** Coastal-oak windows of the frozen fixture: seed, x, z. */
    private static final long[][] SITES={{1,464000,-528000},{1,487552,-610176},{1,1401088,-532224},{424242,7328,-762720},{424242,285856,-746336}};
    public static void main(String[] args)throws Exception{
        schema();
        published();
        otherShapes();
        morphology();
        System.out.println("COASTAL_OAK_CROWNS_GATED_PASS");
    }
    /** Schema 278 exists only as the complete pine-crown 277 plus the explicit definition; bit 4096 is single and distinct. */
    private static void schema(){
        for(int saved=0;saved<=300;saved++){
            require(CoastalOakCrownSchema.baseSchema(saved,0)==(saved==278?Integer.MAX_VALUE:saved),"Absent definition never reads 278 ("+saved+")");
            require(CoastalOakCrownSchema.matches(saved,1)==(saved==278),"Only 278 reads under the definition ("+saved+")");
        }
        require(CoastalOakCrownSchema.schema(277,1)==278&&CoastalOakCrownSchema.baseSchema(278,1)==277,"Complete independent wrapper");
        require(CoastalOakCrownSchema.schema(276,0)==276&&CoastalOakCrownSchema.schema(277,0)==277,"Absent definition leaves the schema");
        for(int base:new int[]{270,272,275,276,278})requireThrows(()->CoastalOakCrownSchema.schema(base,1),"Wrapper rejects incomplete or future base "+base);
        requireThrows(()->CoastalOakCrownSchema.schema(277,2),"Unknown definition rejected");
        requireThrows(()->CoastalOakCrownSchema.schema(277,-1),"Negative definition rejected");
        require(PineCrownSchema.schema(276,1)==277&&BirchCrownSchema.schema(275,1)==276,"Earlier wrappers keep their numbers");
        int[] bits={PineCrowns.FLAG,BirchCrowns.FLAG,BroadleafCrowns.FLAG,SeasonalCrowns.FLAG,BorealSlope.FLAG,MontaneForest.FLAG,SeasonalForestExpansion.FLAG,DryForest.FLAG,SummerDryWoodland.FLAG,RedwoodForest.FLAG,16,1};
        require(CoastalOakCrowns.FLAG==4096&&CoastalOakCrowns.enabled(4096|3)&&!CoastalOakCrowns.enabled(4095)&&!CoastalOakCrowns.enabled(2048|1024|512|256),"Flag bit 4096 is its own meaning");
        for(int bit:bits)require((bit&CoastalOakCrowns.FLAG)==0,"Coastal-oak bit does not reuse bit "+bit);
        require(PineCrowns.FLAG==2048&&BirchCrowns.FLAG==1024&&SeasonalCrowns.FLAG==512&&BroadleafCrowns.FLAG==256&&SummerDryWoodland.FLAG==8,"Earlier bits keep their permanent meaning");
    }
    /** Every frozen published window and tree, with the coastal-oak bit absent, stays bit-identical; the bit changes only mature coastal oaks. */
    private static void published()throws Exception{
        int windows=0,changed=0,controls=0,treeRows=0,summerRows=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(CoastalOakCrownsCheck.class.getResourceAsStream("/fixtures/coastal-oak-277/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");
                if(f[0].equals("trees")){
                    int family=Integer.parseInt(f[1].substring(7));
                    var row=trees(family,false);
                    require(row.equals(f[2].substring(6)+" "+f[3].substring(7)+" "+f[4].substring(9)),"Published trees changed: "+line+" now "+row);
                    require(row.equals(trees(family,true)),"The explicit coastal-absent overloads are the published shape: family "+family);
                    treeRows++;continue;
                }
                if(f[0].equals("summerdry")){
                    var row=summerDry(false);
                    require(row.equals(f[1].substring(6)+" "+f[2].substring(5)),"Published summer-dry trees changed: "+line+" now "+row);
                    require(row.equals(summerDry(true)),"The explicit coastal-absent overload is the published summer-dry shape");
                    summerRows++;continue;
                }
                long seed=Long.parseLong(f[2]);int x=Integer.parseInt(f[3]),z=Integer.parseInt(f[4]),flags=Integer.parseInt(f[5]);
                var row=window(seed,x,z,flags);
                require(row.equals(f[6]+" "+f[7]),"Published columns changed at "+f[1]+" flags "+flags+": "+row+" vs frozen "+f[6]+" "+f[7]);
                var gated=window(seed,x,z,flags|CoastalOakCrowns.FLAG);
                if(f[1].startsWith("coast-")){require(!gated.equals(row),"The gate changed nothing at "+f[1]+" flags "+flags);changed++;}
                else{require(gated.equals(row),"The coastal-oak bit changed the unchanged control "+f[1]+" flags "+flags);require(Integer.parseInt(f[6])>3000,"Control window holds trees: "+f[1]);controls++;}
                windows++;
            }
        }
        require(windows==42&&changed==15&&controls==27&&treeRows==4&&summerRows==1,"Every frozen row is checked: "+windows+" windows, "+changed+" coastal, "+controls+" controls, "+treeRows+" tree rows, "+summerRows+" summer-dry row");
    }
    private static final Map<Long,DrainageNetwork> TERRAIN=new HashMap<>();private static final Map<Long,ClimateField> CLIMATE=new HashMap<>();
    private static DrainageNetwork terrain(long seed){return TERRAIN.computeIfAbsent(seed,BorealSlopeOracle::terrain);}
    private static ClimateField climate(long seed){return CLIMATE.computeIfAbsent(seed,k->new ClimateField(k,true));}
    private static java.util.function.BiPredicate<Integer,Integer> owner(long seed){var d=terrain(seed);var c=climate(seed);return (x,z)->SummerDryWoodland.eligible(seed,x,z,d,c);}
    private static String window(long seed,int sx,int sz,int flags)throws Exception{
        var d=terrain(seed);var c=climate(seed);var md=MessageDigest.getInstance("SHA-256");int n=0;var owner=owner(seed);
        int cx=Math.floorDiv(sx,16),cz=Math.floorDiv(sz,16);
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){int[] col=WoodlandTrees.column(seed,cx+dx,cz+dz,d,c,true,flags,null,(flags&SummerDryWoodland.FLAG)!=0?owner:null,null);n+=col.length;md.update(Arrays.toString(col).getBytes());}
        return n+" "+HexFormat.of().formatHex(md.digest());
    }
    /** Frozen recipe: 27 heights x 24 variants of unflagged roots through ForestTrees.tree and WoodlandTrees.tree; {@code explicit} calls the coastal-absent overload instead. */
    private static String trees(int family,boolean explicit)throws Exception{
        var f=MessageDigest.getInstance("SHA-256");var w=MessageDigest.getInstance("SHA-256");int roots=0,vf=0,vw=0;
        for(int h=5;h<=31;h++)for(int i=0;i<24;i++){
            var root=new WoodlandTrees.Root(0,0,0,family==9,h,(int)OakBirchWoodland.mix(i*7919L+h*104729L+family),family);
            var x=explicit?ForestTrees.tree(root,false,false,false,false):ForestTrees.tree(root);vf+=x.size();update(f,x);
            var y=WoodlandTrees.tree(root,0);vw+=y.size();update(w,y);roots++;
        }
        return roots+" "+vf+":"+HexFormat.of().formatHex(f.digest())+" "+vw+":"+HexFormat.of().formatHex(w.digest());
    }
    /** Frozen recipe: SummerDryTrees.tree over heights 3..18 x 24 variants of family-19 roots. */
    private static String summerDry(boolean explicit)throws Exception{
        var md=MessageDigest.getInstance("SHA-256");int roots=0,voxels=0;
        for(int h=3;h<=18;h++)for(int i=0;i<24;i++){
            var root=new WoodlandTrees.Root(0,0,0,false,h,(int)OakBirchWoodland.mix(i*7919L+h*104729L+19),SummerDryWoodland.FAMILY);
            var x=explicit?SummerDryTrees.tree(root,false):SummerDryTrees.tree(root);voxels+=x.size();update(md,x);roots++;
        }
        return roots+" "+voxels+":"+HexFormat.of().formatHex(md.digest());
    }
    private static void update(MessageDigest md,List<WoodlandTrees.Voxel> t){md.update((t.size()+":").getBytes());for(var v:t)md.update((v.x()+","+v.y()+","+v.z()+","+v.code()+";").getBytes());}
    private static List<WoodlandTrees.Root> stems(){
        var out=new ArrayList<WoodlandTrees.Root>();
        for(var s:SITES)for(var r:SummerDryTrees.roots(s[0],(int)s[1]-96,(int)s[2]-96,(int)s[1]+96,(int)s[2]+96,terrain(s[0]),climate(s[0]),owner(s[0])))out.add(r);
        return out;
    }
    /** Only mature family-19 oaks change; young oaks, the shrub layer and every other family keep the published voxels. */
    private static void otherShapes()throws Exception{
        int mature=0,young=0,shrub=0;var heights=new TreeSet<Integer>();
        for(var root:stems()){
            var old=SummerDryTrees.tree(root);var gated=SummerDryTrees.tree(root,true);
            require(SummerDryTrees.tree(root,false).equals(old),"The explicit coastal-absent overload is the published shape: "+root);
            require(root.family()==SummerDryWoodland.FAMILY&&!root.birch(),"Summer-dry stems are family 19: "+root);
            if(root.height()>=CoastalOakCrowns.MATURE_HEIGHT){mature++;require(!old.equals(gated),"Gate reshapes every mature oak: "+root);heights.add(root.height());}
            else{if(root.height()>5)young++;else shrub++;require(old.equals(gated),"Young stem or shrub changed: "+root);}
            require(WoodlandTrees.tree(root,CoastalOakCrowns.FLAG).equals(WoodlandTrees.tree(root,0)),"The generic WoodlandTrees entry never takes the coastal-oak architecture: "+root);
        }
        require(mature>=1000&&young>=300&&shrub>=300&&heights.equals(new TreeSet<>(List.of(12,13,14,15,16,17,18))),"Enough stems: mature "+mature+" young "+young+" shrub "+shrub+" mature heights "+heights);
        // Tree-level sweep over every height ForestTrees accepts: exactly the mature family-19 heights reshape, and no other family does.
        int reshaped=0,same=0;
        for(int family:new int[]{1,2,9,DryForest.FAMILY,SummerDryWoodland.FAMILY})for(int h=5;h<=31;h++){
            var root=new WoodlandTrees.Root(0,0,0,family==9,h,1234567+h,family);
            boolean differs=!ForestTrees.tree(root,false,false,false,false).equals(ForestTrees.tree(root,false,false,false,true));
            boolean expected=family==SummerDryWoodland.FAMILY&&h>=CoastalOakCrowns.MATURE_HEIGHT;
            require(differs==expected,"Coastal bit only reshapes mature family 19: family "+family+" h "+h+" differs "+differs);
            require(!ForestTrees.tree(root,false,false,false,true).isEmpty(),"Control is nonvacuous: family "+family+" h "+h);
            if(differs)reshaped++;else same++;
        }
        require(reshaped==31-CoastalOakCrowns.MATURE_HEIGHT+1&&same==5*27-reshaped,"Exactly the mature family-19 heights reshape: "+reshaped+" of "+(reshaped+same));
        // Roots never depend on the gate: the root list is a pure function of terrain, climate and owner, and the coastal oak has no height of its own.
        var first=stems();var second=stems();require(first.equals(second),"Roots are reproducible and gate-free");
    }
    /** Same real stems: root heights unchanged by construction; trunk and leaf mass comparable, wood attached, extents inside the halo, the crown top below the root height. */
    private static void morphology(){
        var leaves=new ArrayList<Double>();var logsRatio=new ArrayList<Double>();var widthOld=new ArrayList<Double>();var widthNew=new ArrayList<Double>();var fillOld=new ArrayList<Double>();var fillNew=new ArrayList<Double>();var ratioOld=new ArrayList<Double>();var ratioNew=new ArrayList<Double>();var lowOld=new ArrayList<Double>();var lowNew=new ArrayList<Double>();
        var reach=new ArrayList<Double>();var reachOld=new ArrayList<Double>();var woodReach=new ArrayList<Double>();var leafOldN=new ArrayList<Double>();var leafNewN=new ArrayList<Double>();var logOldN=new ArrayList<Double>();var logNewN=new ArrayList<Double>();var axisOld=new ArrayList<Double>();var axisNew=new ArrayList<Double>();var topOld=new ArrayList<Double>();var topNew=new ArrayList<Double>();var heights=new ArrayList<Double>();
        var silhouettes=new HashSet<Integer>();var baseDelta=new TreeMap<Integer,Integer>();int n=0,lowerBase=0;
        for(var root:stems()){
            if(root.height()<CoastalOakCrowns.MATURE_HEIGHT)continue;
            var a=measure(root,SummerDryTrees.tree(root));var b=measure(root,SummerDryTrees.tree(root,true));n++;
            require(b.top<=root.height(),"Crown top never above the root height: "+b.top+" of "+root.height());
            require(Math.abs(b.top-a.top)<=3,"Crown top stays within three blocks of the published voxel top: "+b.top+" vs "+a.top+" of "+root.height());
            require(b.attached,"Every log of the gated stem is face-connected to its trunk base at "+root);
            require(b.maxExtent<=CoastalOakCrowns.EXTENT&&b.woodExtent<=5,"Voxels stay inside the 6-block generation halo: "+b.maxExtent+" wood "+b.woodExtent+" vs published "+a.maxExtent);
            require(b.lowestLeaf>=a.lowestLeaf-4,"Diagnostic guard, not a target: no foliage more than four blocks below the published crown base: "+b.lowestLeaf+" vs "+a.lowestLeaf+" of "+root.height());
            if(b.lowestLeaf<a.lowestLeaf)lowerBase++;
            baseDelta.merge(b.lowestLeaf-a.lowestLeaf,1,Integer::sum);
            heights.add((double)root.height());topOld.add((double)(a.top-root.height()));topNew.add((double)(b.top-root.height()));
            reach.add(b.maxExtent);reachOld.add(a.maxExtent);woodReach.add(b.woodExtent);leafOldN.add((double)a.leaves);leafNewN.add((double)b.leaves);logOldN.add((double)a.logs);logNewN.add((double)b.logs);
            leaves.add(b.leaves/(double)a.leaves);logsRatio.add(b.logs/(double)a.logs);widthOld.add((double)a.width);widthNew.add((double)b.width);
            fillOld.add(a.leaves/(double)a.box);fillNew.add(b.leaves/(double)b.box);axisOld.add(a.axis);axisNew.add(b.axis);
            ratioOld.add(a.depth/(double)a.width);ratioNew.add(b.depth/(double)b.width);lowOld.add(a.lowestLeaf/(double)root.height());lowNew.add(b.lowestLeaf/(double)root.height());
            silhouettes.add(Objects.hash(b.leaves,b.logs,b.width,b.lowestLeaf,b.top,b.axis));
        }
        require(n>=1000,"Enough mature stems measured: "+n);
        double leafMedian=median(leaves),logMedian=median(logsRatio);
        require(leafMedian>.9&&leafMedian<1.12,"Leaf mass comparable, not a blanket reduction: median per-stem ratio "+leafMedian);
        require(logMedian>1.0&&logMedian<1.5,"Limb wood bounded: median per-stem ratio "+logMedian);
        require(median(widthNew)-median(widthOld)<=4&&median(widthNew)>=median(widthOld),"Spread widens by at most four blocks: "+median(widthOld)+" -> "+median(widthNew));
        require(median(fillNew)<median(fillOld),"Lobed crowns fill less of their box: "+median(fillOld)+" -> "+median(fillNew));
        require(Math.abs(median(lowNew)-median(lowOld))<=.06,"The crown base stays near its published height fraction: "+median(lowOld)+" -> "+median(lowNew));
        require(median(axisNew)-median(axisOld)>=1.5,"The woody axis leans further from the root than the published pole: "+median(axisOld)+" -> "+median(axisNew));
        require(silhouettes.size()>n*.9,"Stems differ individually: "+silhouettes.size()+" distinct silhouettes of "+n);
        int capped=0;for(double r:reach)if(r>=CoastalOakCrowns.EXTENT)capped++;
        System.out.printf("COASTAL_OAK_CROWNS_CAP boundary-touch fraction: %d of %d gated stems have a voxel on the 6-block halo plane (a touch count, not the number of clipped cells)%n",capped,n);
        System.out.printf("COASTAL_OAK_CROWNS_BASE lowest-leaf height, gated minus published, blocks over %d stems (the crown base moves in both directions; it is not unchanged): %s; lower on %d%n",n,baseDelta,lowerBase);
        cost();
        System.out.printf("COASTAL_OAK_CROWNS_MORPHOLOGY stems=%d root heights min %.0f median %.0f max %.0f (unchanged); leaf-count medians %.0f->%.0f, median per-stem ratio %.3f; log-count medians %.0f->%.0f, median per-stem ratio %.3f; crown-top voxel minus root height median %.0f->%.0f min %.0f->%.0f max %.0f->%.0f; width %.0f->%.0f; depth/width %.2f->%.2f; boxFill %.3f->%.3f; lowestLeaf/h %.3f->%.3f (lower base on %d stems); voxel extent median %.0f->%.0f max %.0f->%.0f; wood extent max %.0f; top-log offset from root median %.1f->%.1f; distinct silhouettes %d%n",n,Collections.min(heights),median(heights),Collections.max(heights),median(leafOldN),median(leafNewN),leafMedian,median(logOldN),median(logNewN),logMedian,median(topOld),median(topNew),Collections.min(topOld),Collections.min(topNew),Collections.max(topOld),Collections.max(topNew),median(widthOld),median(widthNew),median(ratioOld)/1.0,median(ratioNew)/1.0,median(fillOld),median(fillNew),median(lowOld),median(lowNew),lowerBase,median(reachOld),median(reach),Collections.max(reachOld),Collections.max(reach),Collections.max(woodReach),median(axisOld),median(axisNew),silhouettes.size());
    }
    /** Diagnostic only (never asserted): warm-JIT construction time per mature real stem, published versus gated, median of five passes. */
    private static void cost(){
        var stems=new ArrayList<WoodlandTrees.Root>();for(var r:stems())if(r.height()>=CoastalOakCrowns.MATURE_HEIGHT)stems.add(r);
        long sink=0;for(int w=0;w<3;w++)for(var r:stems)sink+=SummerDryTrees.tree(r,false).size()+SummerDryTrees.tree(r,true).size();
        double[] medians=new double[2];
        for(int variant=0;variant<2;variant++){var t=new double[5];
            for(int k=0;k<5;k++){long begin=System.nanoTime();for(var r:stems)sink+=SummerDryTrees.tree(r,variant==1).size();t[k]=(System.nanoTime()-begin)/1e3/stems.size();}
            Arrays.sort(t);medians[variant]=t[2];}
        System.out.printf("COASTAL_OAK_CROWNS_COST us/stem published %.0f gated %.0f over %d mature stems (warm JIT, median of 5; diagnostic only, no frame-rate claim) %d%n",medians[0],medians[1],stems.size(),sink&1);
    }
    private record Metrics(int top,int leaves,int logs,int width,int lowestLeaf,int depth,double maxExtent,double woodExtent,int box,double axis,boolean attached){}
    private static Metrics measure(WoodlandTrees.Root root,List<WoodlandTrees.Voxel> tree){
        int top=0,leaves=0,logs=0,lo=99,hi=-1,x0=99,x1=-99,z0=99,z1=-99,logTop=-1;double extent=0,woodExtent=0,axis=0;var wood=new HashSet<Pos>();var minX=new int[64];var maxX=new int[64];Arrays.fill(minX,99);Arrays.fill(maxX,-99);
        for(var v:tree){
            int x=v.x()-root.x(),y=v.y()-root.y(),z=v.z()-root.z();top=Math.max(top,y);double e=Math.max(Math.abs(x),Math.abs(z));extent=Math.max(extent,e);
            if(WoodlandTrees.logCode(v.code())){logs++;wood.add(key(x,y,z));woodExtent=Math.max(woodExtent,e);if(y>logTop){logTop=y;axis=Math.hypot(x,z);}continue;}
            leaves++;lo=Math.min(lo,y);hi=Math.max(hi,y);minX[y]=Math.min(minX[y],x);maxX[y]=Math.max(maxX[y],x);x0=Math.min(x0,x);x1=Math.max(x1,x);z0=Math.min(z0,z);z1=Math.max(z1,z);
        }
        int width=0;for(int y=0;y<minX.length;y++)if(maxX[y]>=minX[y])width=Math.max(width,maxX[y]-minX[y]+1);
        var seen=new HashSet<Pos>();var queue=new ArrayDeque<int[]>();
        if(wood.contains(key(0,0,0))){queue.add(new int[]{0,0,0});seen.add(key(0,0,0));}
        while(!queue.isEmpty()){var p=queue.poll();for(var s:DIR){int x=p[0]+s[0],y=p[1]+s[1],z=p[2]+s[2];var k=key(x,y,z);if(wood.contains(k)&&seen.add(k))queue.add(new int[]{x,y,z});}}
        return new Metrics(top,leaves,logs,width,lo,hi-lo+1,extent,woodExtent,(x1-x0+1)*(z1-z0+1)*(hi-lo+1),axis,seen.size()==wood.size()&&!wood.isEmpty());
    }
    private static double median(List<Double> values){var v=new ArrayList<>(values);Collections.sort(v);return v.get(v.size()/2);}
    private record Pos(int x,int y,int z){}
    private static Pos key(int x,int y,int z){return new Pos(x,y,z);}
    private static void requireThrows(Runnable r,String message){try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private CoastalOakCrownsCheck(){}
}
