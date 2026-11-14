package blockbranch.realistic;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Saved-gate numbering, the frozen published boreal canopy and the gated mature old-growth-pine crown morphology.
 * The published mature pine is a continuous layered ovoid around a bare lower trunk; the gated pine carries 6..11 lateral limbs ending in
 * separate flattened pads around a leader crown. Both variants are measured on the same real stems. Metrics are diagnostics for a bounded
 * change (same height, cohorts and trunk, comparable leaf mass, attached limbs, inside the generation halo, every other family untouched),
 * not visual approval and not an ecological claim.
 */
public final class PineCrownsCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    /** Old-growth-pine windows of the frozen fixture: seed, x, z. */
    private static final long[][] SITES={{424242,1520000,-1584000},{424242,-756000,-1600000},{1,87552,-1591808},{1,595456,-1600000}};
    private static final int[] FAMILIES={4,5,6,7,8,15,MontaneForest.FAMILY};
    public static void main(String[] args)throws Exception{
        schema();
        published();
        otherFamilies();
        morphology();
        System.out.println("PINE_CROWNS_GATED_PASS");
    }
    /** Schema 277 exists only as the complete birch-crown 276 plus the explicit definition; bit 2048 is single and distinct. */
    private static void schema(){
        for(int saved=0;saved<=300;saved++){
            require(PineCrownSchema.baseSchema(saved,0)==(saved==277?Integer.MAX_VALUE:saved),"Absent definition never reads 277 ("+saved+")");
            require(PineCrownSchema.matches(saved,1)==(saved==277),"Only 277 reads under the definition ("+saved+")");
        }
        require(PineCrownSchema.schema(276,1)==277&&PineCrownSchema.baseSchema(277,1)==276,"Complete independent wrapper");
        require(PineCrownSchema.schema(275,0)==275&&PineCrownSchema.schema(276,0)==276,"Absent definition leaves the schema");
        for(int base:new int[]{270,272,273,274,275,277})requireThrows(()->PineCrownSchema.schema(base,1),"Wrapper rejects incomplete or future base "+base);
        requireThrows(()->PineCrownSchema.schema(276,2),"Unknown definition rejected");
        requireThrows(()->PineCrownSchema.schema(276,-1),"Negative definition rejected");
        require(BirchCrownSchema.schema(275,1)==276&&SeasonalCrownSchema.schema(274,1)==275,"Earlier wrappers keep their numbers");
        int[] bits={BirchCrowns.FLAG,BroadleafCrowns.FLAG,SeasonalCrowns.FLAG,BorealSlope.FLAG,MontaneForest.FLAG,SeasonalForestExpansion.FLAG,DryForest.FLAG,SummerDryWoodland.FLAG,RedwoodForest.FLAG,16,1};
        require(PineCrowns.FLAG==2048&&PineCrowns.enabled(2048|3)&&!PineCrowns.enabled(2047)&&!PineCrowns.enabled(1024|512|256),"Flag bit 2048 is its own meaning");
        for(int bit:bits)require((bit&PineCrowns.FLAG)==0,"Pine bit does not reuse bit "+bit);
        require(BirchCrowns.FLAG==1024&&BroadleafCrowns.FLAG==256&&SeasonalCrowns.FLAG==512,"Earlier bits keep their permanent meaning");
    }
    /** Every frozen published window and tree, with the pine bit absent, stays bit-identical; the bit changes only mature old-growth pine. */
    private static void published()throws Exception{
        int windows=0,changed=0,controls=0,treeRows=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(PineCrownsCheck.class.getResourceAsStream("/fixtures/pine-crowns-276/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");
                if(f[0].equals("trees")){
                    int family=Integer.parseInt(f[1].substring(7));
                    var row=trees(family,0);
                    require(row.equals(f[2].substring(6)+" "+f[3].substring(6)+" "+f[4].substring(7)+" "+f[5].substring(7)),"Published trees changed: "+line+" now "+row);
                    require(row.equals(trees(family,1)),"The explicit pine-absent overloads are the published shape: family "+family);
                    treeRows++;continue;
                }
                long seed=Long.parseLong(f[2]);int x=Integer.parseInt(f[3]),z=Integer.parseInt(f[4]),flags=Integer.parseInt(f[5]);
                var row=window(seed,x,z,flags);
                require(row.equals(f[6]+" "+f[7]),"Published columns changed at "+f[1]+" flags "+flags+": "+row+" vs frozen "+f[6]+" "+f[7]);
                var gated=window(seed,x,z,flags|PineCrowns.FLAG);
                if(f[1].startsWith("pine-")){require(!gated.equals(row),"The gate changed nothing at "+f[1]+" flags "+flags);changed++;}
                else{require(gated.equals(row),"The pine bit changed the unchanged control "+f[1]+" flags "+flags);require(Integer.parseInt(f[6])>3000,"Control window holds trees: "+f[1]);controls++;}
                windows++;
            }
        }
        require(windows==55&&changed==20&&controls==35&&treeRows==7,"Every frozen row is checked: "+windows+" windows, "+changed+" pine, "+controls+" controls, "+treeRows+" tree rows");
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
    /**
     * The frozen tree digests recipe: 27 heights x 24 variants of unflagged roots through TaigaTrees.tree, BorealTrees.tree and
     * BorealTrees.legacyTree. {@code extra} nonzero calls the explicit pine-absent overloads instead.
     */
    private static String trees(int family,int extra)throws Exception{
        var t=MessageDigest.getInstance("SHA-256");var b=MessageDigest.getInstance("SHA-256");var l=MessageDigest.getInstance("SHA-256");int roots=0,vt=0,vb=0,vl=0;
        for(int h=5;h<=31;h++)for(int i=0;i<24;i++){
            var root=new WoodlandTrees.Root(0,0,0,false,h,(int)OakBirchWoodland.mix(i*7919L+h*104729L+family),family);
            var x=extra==0?TaigaTrees.tree(root):TaigaTrees.tree(root,false);vt+=x.size();update(t,x);
            var y=extra==0?BorealTrees.tree(root):BorealTrees.tree(root,false);vb+=y.size();update(b,y);
            var z=BorealTrees.legacyTree(root);vl+=z.size();update(l,z);roots++;
        }
        return roots+" "+vt+":"+HexFormat.of().formatHex(t.digest())+" "+vb+":"+HexFormat.of().formatHex(b.digest())+" "+vl+":"+HexFormat.of().formatHex(l.digest());
    }
    private static void update(MessageDigest md,List<WoodlandTrees.Voxel> t){md.update((t.size()+":").getBytes());for(var v:t)md.update((v.x()+","+v.y()+","+v.z()+","+v.code()+";").getBytes());}
    private static List<WoodlandTrees.Root> stems(){
        var out=new ArrayList<WoodlandTrees.Root>();
        for(var s:SITES){var d=terrain(s[0]);var c=climate(s[0]);
            for(var r:BorealTrees.roots(s[0],(int)s[1]-96,(int)s[2]-96,(int)s[1]+96,(int)s[2]+96,d,c))if(r.family()==PineCrowns.FAMILY)out.add(r);}
        return out;
    }
    /** Only mature family-7 pine changes; young pines, spruce, the other taiga families, montane needleleaf and the legacy path keep the published voxels. */
    private static void otherFamilies()throws Exception{
        int mature=0,young=0;
        for(var root:stems()){
            var old=TaigaTrees.tree(root);var gated=TaigaTrees.tree(root,true);
            if(root.height()>=PineCrowns.MATURE_HEIGHT){mature++;require(!old.equals(gated),"Gate reshapes every mature pine: "+root);}
            else{young++;require(old.equals(gated),"Young pine changed: "+root);}
            require(BorealTrees.tree(root).equals(old)&&BorealTrees.tree(root,false).equals(old)&&BorealTrees.tree(root,true).equals(gated),"BorealTrees routes the pine request: "+root);
            require(BorealTrees.legacyTree(root).equals(old),"The legacy crown definition never takes the pine architecture: "+root);
            require(root.height()>=8&&root.height()<=31&&!root.birch(),"Old-growth pine stems are family 7 conifers of the generated heights: "+root);
        }
        require(mature>=400&&young>=100,"Enough stems: mature "+mature+" young "+young);
        // Real roots of the unchanged controls: spruce (8), taiga (4), snowy (5), grove (6) never differ whatever the request.
        int controls=0;long[][] others={{424242,-944000,-1616000},{1,24704,-1200000},{1,106624,-1167232},{1,110720,-1163136}};
        var seen=new TreeSet<Integer>();
        for(var s:others){var d=terrain(s[0]);var c=climate(s[0]);
            for(var r:BorealTrees.roots(s[0],(int)s[1]-48,(int)s[2]-48,(int)s[1]+48,(int)s[2]+48,d,c)){
                require(r.family()!=PineCrowns.FAMILY,"Control windows hold no pines: "+r);
                require(BorealTrees.tree(r,true).equals(BorealTrees.tree(r,false)),"The pine bit leaves family "+r.family()+" alone: "+r);seen.add(r.family());controls++;
            }}
        require(controls>=300&&seen.containsAll(List.of(4,5,6,8)),"Real control stems: "+controls+" families "+seen);
        // Tree-level sweep over every height the taiga shape accepts: exactly the nine mature family-7 heights reshape.
        int reshaped=0,same=0;
        for(int family:FAMILIES)for(int h=5;h<=31;h++){
            var root=new WoodlandTrees.Root(0,0,0,false,h,1234567+h,family);
            boolean differs=!TaigaTrees.tree(root,false).equals(TaigaTrees.tree(root,true));
            boolean expected=family==PineCrowns.FAMILY&&h>=PineCrowns.MATURE_HEIGHT;
            require(differs==expected,"Pine bit only reshapes mature family 7: family "+family+" h "+h+" differs "+differs);
            require(!TaigaTrees.tree(root,true).isEmpty(),"Control is nonvacuous: family "+family+" h "+h);
            if(differs)reshaped++;else same++;
        }
        require(reshaped==31-PineCrowns.MATURE_HEIGHT+1&&same==FAMILIES.length*27-reshaped,"Exactly the mature family-7 heights reshape: "+reshaped+" of "+(reshaped+same));
        // A pine-flagged column request for a window with no pines is unchanged (frozen above); the birch bit stays independent.
        var root=new WoodlandTrees.Root(0,0,0,false,27,99,PineCrowns.FAMILY);
        require(WoodlandTrees.tree(new WoodlandTrees.Root(0,0,0,true,27,99,TemperateForestVariants.BIRCH_FAMILY),PineCrowns.FLAG).equals(WoodlandTrees.tree(new WoodlandTrees.Root(0,0,0,true,27,99,TemperateForestVariants.BIRCH_FAMILY),0)),"The pine bit leaves old-growth birch alone");
        require(!BorealTrees.tree(root,true).equals(BorealTrees.tree(root)),"A mature pine request is not vacuous");
    }
    /** Same real stems: height, trunk and leaf mass stay comparable, limbs attach, extents stay inside the halo, crowns are more open and lobed. */
    private static void morphology(){
        var leaves=new ArrayList<Double>();var logsRatio=new ArrayList<Double>();var widthOld=new ArrayList<Double>();var widthNew=new ArrayList<Double>();var fillOld=new ArrayList<Double>();var fillNew=new ArrayList<Double>();var ratioOld=new ArrayList<Double>();var ratioNew=new ArrayList<Double>();var lowOld=new ArrayList<Double>();var lowNew=new ArrayList<Double>();
        var sectorOld=new ArrayList<Double>();var sectorNew=new ArrayList<Double>();var reach=new ArrayList<Double>();var reachOld=new ArrayList<Double>();var woodReach=new ArrayList<Double>();var leafOldN=new ArrayList<Double>();var leafNewN=new ArrayList<Double>();var logOldN=new ArrayList<Double>();var logNewN=new ArrayList<Double>();int n=0,lowerBase=0;
        for(var root:stems()){
            if(root.height()<PineCrowns.MATURE_HEIGHT)continue;
            var a=measure(root,TaigaTrees.tree(root));var b=measure(root,TaigaTrees.tree(root,true));n++;
            require(b.top<=root.height()&&Math.abs(b.top-a.top)<=2,"Crown top stays within two blocks of the published top and never above the tree height: "+b.top+" vs "+a.top+" of "+root.height());
            require(b.attached,"Every log of the gated stem is face-connected to its trunk base at "+root);
            require(b.maxExtent<=6&&b.woodExtent<=5,"Voxels stay inside the 6-block generation halo: "+b.maxExtent+" wood "+b.woodExtent+" vs published "+a.maxExtent);
            require(b.lowestLeaf>=a.lowestLeaf-4,"No foliage far below the published crown base: "+b.lowestLeaf+" vs "+a.lowestLeaf+" of "+root.height());
            if(b.lowestLeaf<a.lowestLeaf)lowerBase++;
            reach.add(b.maxExtent);reachOld.add(a.maxExtent);woodReach.add(b.woodExtent);leafOldN.add((double)a.leaves);leafNewN.add((double)b.leaves);logOldN.add((double)a.logs);logNewN.add((double)b.logs);
            leaves.add(b.leaves/(double)a.leaves);logsRatio.add(b.logs/(double)a.logs);widthOld.add((double)a.width);widthNew.add((double)b.width);
            fillOld.add(a.leaves/(double)a.box);fillNew.add(b.leaves/(double)b.box);sectorOld.add(a.sector);sectorNew.add(b.sector);
            ratioOld.add(a.depth/(double)a.width);ratioNew.add(b.depth/(double)b.width);lowOld.add(a.lowestLeaf/(double)root.height());lowNew.add(b.lowestLeaf/(double)root.height());
        }
        require(n>=400,"Enough mature stems measured: "+n);
        double leafMedian=median(leaves),logMedian=median(logsRatio);
        require(leafMedian>.9&&leafMedian<1.15,"Leaf mass comparable, not a blanket reduction: median ratio "+leafMedian);
        require(logMedian>1.0&&logMedian<1.4,"Limb wood bounded: median ratio "+logMedian);
        require(median(widthNew)-median(widthOld)<=4&&median(widthNew)>=median(widthOld),"Spread widens by at most four blocks: "+median(widthOld)+" -> "+median(widthNew));
        require(median(fillNew)<median(fillOld),"Lobed crowns fill less of their box: "+median(fillOld)+" -> "+median(fillNew));
        require(median(ratioNew)<median(ratioOld),"Crowns are less column-like (depth over width): "+median(ratioOld)+" -> "+median(ratioNew));
        require(median(sectorNew)<median(sectorOld),"Crowns leave more azimuth gaps per layer: "+median(sectorOld)+" -> "+median(sectorNew));
        require(Math.abs(median(lowNew)-median(lowOld))<=.06,"The crown base stays near its published height fraction: "+median(lowOld)+" -> "+median(lowNew));
        int capped=0;for(double r:reach)if(r>=6)capped++;
        System.out.printf("PINE_CROWNS_CAP boundary-touch fraction: %d of %d gated stems have a voxel on the 6-block halo plane (a touch count, not the number of clipped cells)%n",capped,n);
        cost();
        System.out.printf("PINE_CROWNS_MORPHOLOGY stems=%d leaves median %.0f->%.0f (ratio %.3f) logs median %.0f->%.0f (ratio %.3f) width %.0f->%.0f depth/width %.2f->%.2f boxFill %.3f->%.3f sectorFill %.3f->%.3f lowestLeaf/h %.2f->%.2f (lower base on %d stems) maxReach median %.0f->%.0f max %.0f->%.0f woodReach max %.0f%n",n,median(leafOldN),median(leafNewN),leafMedian,median(logOldN),median(logNewN),logMedian,median(widthOld),median(widthNew),median(ratioOld),median(ratioNew),median(fillOld),median(fillNew),median(sectorOld),median(sectorNew),median(lowOld),median(lowNew),lowerBase,median(reachOld),median(reach),Collections.max(reachOld),Collections.max(reach),Collections.max(woodReach));
    }
    /** Diagnostic only (never asserted): warm-JIT construction time per mature real stem, published versus gated, median of five passes. */
    private static void cost(){
        var stems=new ArrayList<WoodlandTrees.Root>();for(var r:stems())if(r.height()>=PineCrowns.MATURE_HEIGHT)stems.add(r);
        long sink=0;for(int w=0;w<3;w++)for(var r:stems)sink+=TaigaTrees.tree(r,false).size()+TaigaTrees.tree(r,true).size();
        double[] medians=new double[2];
        for(int variant=0;variant<2;variant++){var t=new double[5];
            for(int k=0;k<5;k++){long begin=System.nanoTime();for(var r:stems)sink+=TaigaTrees.tree(r,variant==1).size();t[k]=(System.nanoTime()-begin)/1e3/stems.size();}
            Arrays.sort(t);medians[variant]=t[2];}
        System.out.printf("PINE_CROWNS_COST us/stem published %.0f gated %.0f over %d mature stems (warm JIT, median of 5; diagnostic only, no frame-rate claim) %d%n",medians[0],medians[1],stems.size(),sink&1);
    }
    private record Metrics(int top,int leaves,int logs,int width,int lowestLeaf,int depth,double maxExtent,double woodExtent,int box,double sector,boolean attached){}
    private static boolean log(int code){return code>=TaigaTrees.WOOD&&code<=TaigaTrees.WOOD+2;}
    private static Metrics measure(WoodlandTrees.Root root,List<WoodlandTrees.Voxel> tree){
        int top=0,leaves=0,logs=0,lo=99,hi=-1,x0=99,x1=-99,z0=99,z1=-99;double extent=0,woodExtent=0;var wood=new HashSet<Pos>();var minX=new int[64];var maxX=new int[64];Arrays.fill(minX,99);Arrays.fill(maxX,-99);var occupied=new boolean[64][16];
        for(var v:tree){
            int x=v.x()-root.x(),y=v.y()-root.y(),z=v.z()-root.z();top=Math.max(top,y);double e=Math.max(Math.abs(x),Math.abs(z));extent=Math.max(extent,e);
            if(log(v.code())){logs++;wood.add(key(x,y,z));woodExtent=Math.max(woodExtent,e);continue;}
            leaves++;lo=Math.min(lo,y);hi=Math.max(hi,y);minX[y]=Math.min(minX[y],x);maxX[y]=Math.max(maxX[y],x);x0=Math.min(x0,x);x1=Math.max(x1,x);z0=Math.min(z0,z);z1=Math.max(z1,z);
            occupied[y][(int)((Math.atan2(z,x)+Math.PI)/(2*Math.PI)*16)%16]=true;
        }
        int width=0;for(int y=0;y<minX.length;y++)if(maxX[y]>=minX[y])width=Math.max(width,maxX[y]-minX[y]+1);
        double sector=0;for(int y=lo;y<=hi;y++){int k=0;for(boolean s:occupied[y])if(s)k++;sector+=k/16.0;}sector/=hi-lo+1;
        var seen=new HashSet<Pos>();var queue=new ArrayDeque<int[]>();
        if(wood.contains(key(0,0,0))){queue.add(new int[]{0,0,0});seen.add(key(0,0,0));}
        while(!queue.isEmpty()){var p=queue.poll();for(var s:DIR){int x=p[0]+s[0],y=p[1]+s[1],z=p[2]+s[2];var k=key(x,y,z);if(wood.contains(k)&&seen.add(k))queue.add(new int[]{x,y,z});}}
        return new Metrics(top,leaves,logs,width,lo,hi-lo+1,extent,woodExtent,(x1-x0+1)*(z1-z0+1)*(hi-lo+1),sector,seen.size()==wood.size()&&!wood.isEmpty());
    }
    private static double median(List<Double> values){var v=new ArrayList<>(values);Collections.sort(v);return v.get(v.size()/2);}
    private record Pos(int x,int y,int z){}
    private static Pos key(int x,int y,int z){return new Pos(x,y,z);}
    private static void requireThrows(Runnable r,String message){try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private PineCrownsCheck(){}
}
