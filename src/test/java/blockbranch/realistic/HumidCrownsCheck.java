package blockbranch.realistic;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Saved-gate numbering, the frozen published humid-woodland canopy and the gated mature humid-woodland crown morphology.
 * The published mature humid stem is a straight pole under a ring of three equal cardinal lobes, a top lobe and, from height 18, two further
 * equal lobes on opposite cardinals; the gated stem keeps its root height and position, leans its leader slightly and carries 7..8 limbs at
 * golden-angle azimuths and graded heights, each ending in an unequal, elongated, tilted lobe. Both variants are measured on the same real
 * stems from {@link HumidTrees#roots}. Metrics are diagnostics for a bounded change (same roots, comparable leaf mass, attached wood, inside
 * the generation halo and at or below the published structural top), not visual approval and not an ecological claim.
 */
public final class HumidCrownsCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    /** Humid windows of the frozen fixture: seed, x, z. */
    private static final long[][] SITES={{1,512000,-512000},{1,1424000,-560000},{1,1392000,-496000},{424242,-1072000,-240000},{424242,-1056000,-224000}};
    public static void main(String[] args)throws Exception{
        schema();
        published();
        otherShapes();
        morphology();
        System.out.println("HUMID_CROWNS_GATED_PASS");
    }
    /** Schema 279 exists only as the complete coastal-oak 278 plus the explicit definition; bit 8192 is single and distinct. */
    private static void schema(){
        for(int saved=0;saved<=300;saved++){
            require(HumidCrownSchema.baseSchema(saved,0)==(saved==279?Integer.MAX_VALUE:saved),"Absent definition never reads 279 ("+saved+")");
            require(HumidCrownSchema.matches(saved,1)==(saved==279),"Only 279 reads under the definition ("+saved+")");
        }
        require(HumidCrownSchema.schema(278,1)==279&&HumidCrownSchema.baseSchema(279,1)==278,"Complete independent wrapper");
        require(HumidCrownSchema.schema(277,0)==277&&HumidCrownSchema.schema(278,0)==278,"Absent definition leaves the schema");
        for(int base:new int[]{270,272,275,276,277,279})requireThrows(()->HumidCrownSchema.schema(base,1),"Wrapper rejects incomplete or future base "+base);
        requireThrows(()->HumidCrownSchema.schema(278,2),"Unknown definition rejected");
        requireThrows(()->HumidCrownSchema.schema(278,-1),"Negative definition rejected");
        require(CoastalOakCrownSchema.schema(277,1)==278&&PineCrownSchema.schema(276,1)==277,"Earlier wrappers keep their numbers");
        int[] bits={CoastalOakCrowns.FLAG,PineCrowns.FLAG,BirchCrowns.FLAG,BroadleafCrowns.FLAG,SeasonalCrowns.FLAG,BorealSlope.FLAG,MontaneForest.FLAG,SeasonalForestExpansion.FLAG,DryForest.FLAG,SummerDryWoodland.FLAG,RedwoodForest.FLAG,16,1};
        require(HumidCrowns.FLAG==8192&&HumidCrowns.enabled(8192|3)&&!HumidCrowns.enabled(8191)&&!HumidCrowns.enabled(4096|2048|1024|512|256),"Flag bit 8192 is its own meaning");
        for(int bit:bits)require((bit&HumidCrowns.FLAG)==0,"Humid bit does not reuse bit "+bit);
        require(CoastalOakCrowns.FLAG==4096&&PineCrowns.FLAG==2048&&BirchCrowns.FLAG==1024&&BroadleafCrowns.FLAG==256,"Earlier bits keep their permanent meaning");
    }
    /** Every frozen published window and tree, with the humid bit absent, stays bit-identical; the bit changes only mature humid stems. */
    private static void published()throws Exception{
        int windows=0,changed=0,controls=0,treeRows=0,humidRows=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(HumidCrownsCheck.class.getResourceAsStream("/fixtures/humid-crowns-278/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");
                if(f[0].equals("trees")){
                    int family=Integer.parseInt(f[1].substring(7));
                    var row=trees(family);
                    require(row.equals(f[2].substring(6)+" "+f[3].substring(9)),"Published trees changed: "+line+" now "+row);
                    require(row.equals(treesWith(family,HumidCrowns.FLAG)),"The generic WoodlandTrees entry never takes the humid architecture: family "+family);
                    treeRows++;continue;
                }
                if(f[0].equals("humid")){
                    int low=Integer.parseInt(f[1].substring(8,f[1].indexOf('-',8))),high=Integer.parseInt(f[1].substring(f[1].indexOf('-',8)+1));
                    var row=humid(low,high,false);
                    require(row.equals(f[2].substring(6)+" "+f[3].substring(5)+" "+f[4].substring(10)),"Published humid trees changed: "+line+" now "+row);
                    require(row.equals(humid(low,high,false)),"Stable");
                    var gated=humid(low,high,true);
                    if(low>=HumidCrowns.MATURE_HEIGHT)require(!gated.equals(row),"The gate reshapes the mature humid band");
                    else require(gated.equals(row),"The gate left the young humid band unchanged");
                    humidRows++;continue;
                }
                long seed=Long.parseLong(f[2]);int x=Integer.parseInt(f[3]),z=Integer.parseInt(f[4]),flags=Integer.parseInt(f[5]);
                var row=window(seed,x,z,flags);
                require(row.equals(f[6]+" "+f[7]),"Published columns changed at "+f[1]+" flags "+flags+": "+row+" vs frozen "+f[6]+" "+f[7]);
                var gated=window(seed,x,z,flags|HumidCrowns.FLAG);
                if(f[1].startsWith("humid-")){require(!gated.equals(row),"The gate changed nothing at "+f[1]+" flags "+flags);changed++;}
                else{require(gated.equals(row),"The humid bit changed the unchanged control "+f[1]+" flags "+flags);require(Integer.parseInt(f[6])>3000,"Control window holds trees: "+f[1]);controls++;}
                windows++;
            }
        }
        require(windows==47&&changed==15&&controls==32&&treeRows==4&&humidRows==2,"Every frozen row is checked: "+windows+" windows, "+changed+" humid, "+controls+" controls, "+treeRows+" tree rows, "+humidRows+" humid rows");
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
    /** Frozen recipe: 24 variants per height of unflagged roots through WoodlandTrees.tree; family 0 uses the actual published heights 8..24. */
    private static String trees(int family)throws Exception{return treesWith(family,0);}
    private static String treesWith(int family,int flags)throws Exception{
        var w=MessageDigest.getInstance("SHA-256");int roots=0,vw=0;
        for(int h=family==0?8:5;h<=(family==0?24:31);h++)for(int i=0;i<24;i++){
            var root=new WoodlandTrees.Root(0,0,0,family==9,h,(int)OakBirchWoodland.mix(i*7919L+h*104729L+family),family);
            var y=WoodlandTrees.tree(root,flags);vw+=y.size();update(w,y);roots++;
        }
        return roots+" "+vw+":"+HexFormat.of().formatHex(w.digest());
    }
    /** Frozen recipe: HumidTrees.tree and WoodlandTrees.humidTree over one actual height band x 24 variants; {@code gated} selects the saved gate. */
    private static String humid(int low,int high,boolean gated)throws Exception{
        var md=MessageDigest.getInstance("SHA-256");var hm=MessageDigest.getInstance("SHA-256");int roots=0,voxels=0;
        for(int h=low;h<=high;h++)for(int i=0;i<24;i++){
            var root=new WoodlandTrees.Root(0,0,0,false,h,(int)OakBirchWoodland.mix(i*7919L+h*104729L+7L),0);
            var x=gated?HumidTrees.tree(root,true):HumidTrees.tree(root);voxels+=x.size();update(md,x);update(hm,gated?WoodlandTrees.humidTree(root,true):WoodlandTrees.humidTree(root));roots++;
        }
        return roots+" "+voxels+":"+HexFormat.of().formatHex(md.digest())+" "+HexFormat.of().formatHex(hm.digest());
    }
    private static void update(MessageDigest md,List<WoodlandTrees.Voxel> t){md.update((t.size()+":").getBytes());for(var v:t)md.update((v.x()+","+v.y()+","+v.z()+","+v.code()+";").getBytes());}
    private static List<WoodlandTrees.Root> stems(){
        var out=new ArrayList<WoodlandTrees.Root>();
        for(var s:SITES)for(var r:HumidTrees.roots(s[0],(int)s[1]-96,(int)s[2]-96,(int)s[1]+96,(int)s[2]+96,terrain(s[0]),climate(s[0])))out.add(r);
        return out;
    }
    /** Only mature humid stems change; young stems, the generic family-0 path and every other family keep the published voxels. */
    private static void otherShapes()throws Exception{
        int mature=0,young=0;var heights=new TreeSet<Integer>();
        for(var root:stems()){
            var old=HumidTrees.tree(root);var gated=HumidTrees.tree(root,true);
            require(HumidTrees.tree(root,false).equals(old),"The explicit gate-absent overload is the published shape: "+root);
            require(root.family()==0&&!root.birch(),"Humid stems are family 0: "+root);
            heights.add(root.height());
            if(root.height()>=HumidCrowns.MATURE_HEIGHT){mature++;require(!old.equals(gated),"Gate reshapes every mature humid stem: "+root);}
            else{young++;require(old.equals(gated),"Young humid stem changed: "+root);}
            // Mature humid roots exceed the generic family-0 shape's height bound by design, so the generic entry is only valid for young stems.
            if(root.height()<=WoodlandTrees.MAX_HEIGHT)require(WoodlandTrees.tree(root,HumidCrowns.FLAG).equals(WoodlandTrees.tree(root,0)),"The generic WoodlandTrees entry never takes the humid architecture: "+root);
        }
        require(mature>=1000&&young>=300&&heights.equals(new TreeSet<>(List.of(8,9,10,11,12,21,22,23,24,25,26,27))),"Enough stems: mature "+mature+" young "+young+" heights "+heights);
        for(int h=8;h<=27;h++){
            var root=new WoodlandTrees.Root(0,0,0,false,h,1234567+h,0);
            boolean differs=!HumidTrees.tree(root,false).equals(HumidTrees.tree(root,true));
            require(differs==(h>=HumidCrowns.MATURE_HEIGHT),"Humid bit only reshapes mature stems: height "+h+" differs "+differs);
            require(!HumidTrees.tree(root,true).isEmpty(),"Control is nonvacuous: height "+h);
        }
        var first=stems();var second=stems();require(first.equals(second),"Roots are reproducible and gate-free");
    }
    /** Same real stems: root heights unchanged by construction; leaf mass comparable, wood attached, extents inside the halo, the crown no higher than before. */
    private static void morphology(){
        var leafRatio=new ArrayList<Double>();var logRatio=new ArrayList<Double>();var widthOld=new ArrayList<Double>();var widthNew=new ArrayList<Double>();var fillOld=new ArrayList<Double>();var fillNew=new ArrayList<Double>();
        var lowOld=new ArrayList<Double>();var lowNew=new ArrayList<Double>();var reach=new ArrayList<Double>();var reachOld=new ArrayList<Double>();var woodReach=new ArrayList<Double>();
        var leafOldN=new ArrayList<Double>();var leafNewN=new ArrayList<Double>();var logOldN=new ArrayList<Double>();var logNewN=new ArrayList<Double>();var topOld=new ArrayList<Double>();var topNew=new ArrayList<Double>();var heights=new ArrayList<Double>();
        var peaksOld=new ArrayList<Double>();var peaksNew=new ArrayList<Double>();var cardOld=new ArrayList<Double>();var cardNew=new ArrayList<Double>();var sectorsOld=new ArrayList<Double>();var sectorsNew=new ArrayList<Double>();var offOld=new ArrayList<Double>();var offNew=new ArrayList<Double>();
        var silhouettes=new HashSet<Integer>();var baseDelta=new TreeMap<Integer,Integer>();int n=0,lowerBase=0;
        for(var root:stems()){
            if(root.height()<HumidCrowns.MATURE_HEIGHT)continue;
            var a=measure(root,HumidTrees.tree(root));var b=measure(root,HumidTrees.tree(root,true));n++;
            require(b.top<=root.height()+1,"Crown top never above the published structural top (root height + 1): "+b.top+" of "+root.height());
            require(b.attached,"Every log of the gated stem is face-connected to its trunk base at "+root);
            require(b.maxExtent<=HumidCrowns.EXTENT&&b.woodExtent<=5,"Voxels stay inside the 6-block generation halo: "+b.maxExtent+" wood "+b.woodExtent+" vs published "+a.maxExtent);
            if(b.lowestLeaf<a.lowestLeaf)lowerBase++;
            baseDelta.merge(b.lowestLeaf-a.lowestLeaf,1,Integer::sum);
            heights.add((double)root.height());topOld.add((double)(a.top-root.height()));topNew.add((double)(b.top-root.height()));
            reach.add(b.maxExtent);reachOld.add(a.maxExtent);woodReach.add(b.woodExtent);leafOldN.add((double)a.leaves);leafNewN.add((double)b.leaves);logOldN.add((double)a.logs);logNewN.add((double)b.logs);
            leafRatio.add(b.leaves/(double)a.leaves);logRatio.add(b.logs/(double)a.logs);widthOld.add((double)a.width);widthNew.add((double)b.width);
            fillOld.add(a.leaves/(double)a.box);fillNew.add(b.leaves/(double)b.box);lowOld.add(a.lowestLeaf/(double)root.height());lowNew.add(b.lowestLeaf/(double)root.height());
            peaksOld.add((double)a.peaks);peaksNew.add((double)b.peaks);cardOld.add(a.cardinal);cardNew.add(b.cardinal);sectorsOld.add((double)a.sectors);sectorsNew.add((double)b.sectors);offOld.add(a.centroidOffset);offNew.add(b.centroidOffset);
            silhouettes.add(Objects.hash(b.leaves,b.logs,b.width,b.lowestLeaf,b.top,b.sectors,b.peaks));
        }
        require(n>=1000,"Enough mature stems measured: "+n);
        double leafMedian=median(leafRatio),logMedian=median(logRatio);
        int capped=0;for(double r:reach)if(r>=HumidCrowns.EXTENT)capped++;
        System.out.printf("HUMID_CROWNS_CAP boundary-touch fraction: %d of %d gated stems have a voxel on the 6-block halo plane (a touch count, not the number of clipped cells); published voxel extent exceeded 6 on %d of %d stems%n",capped,n,countAbove(reachOld,6),n);
        System.out.printf("HUMID_CROWNS_BASE lowest-leaf height, gated minus published, blocks over %d stems (the crown base moves in both directions; it is not unchanged): %s; lower on %d%n",n,baseDelta,lowerBase);
        cost();
        System.out.printf("HUMID_CROWNS_MORPHOLOGY stems=%d root heights min %.0f median %.0f max %.0f (unchanged); leaf-count medians %.0f->%.0f, median per-stem ratio %.3f; log-count medians %.0f->%.0f, median per-stem ratio %.3f; crown-top voxel minus root height median %.0f->%.0f min %.0f->%.0f max %.0f->%.0f; width %.0f->%.0f; boxFill %.3f->%.3f; lowestLeaf/h %.3f->%.3f (lower base on %d stems); voxel extent median %.0f->%.0f max %.0f->%.0f; wood extent max %.0f; leaf-profile peaks median %.0f->%.0f; outer leaves within 22.5 degrees of a cardinal axis (isotropy 0.50) median %.3f->%.3f; occupied outer azimuth octants (of 8) median %.0f->%.0f; layer-centroid offset from the stem median %.2f->%.2f; distinct metric hashes %d%n",n,Collections.min(heights),median(heights),Collections.max(heights),median(leafOldN),median(leafNewN),leafMedian,median(logOldN),median(logNewN),logMedian,median(topOld),median(topNew),Collections.min(topOld),Collections.min(topNew),Collections.max(topOld),Collections.max(topNew),median(widthOld),median(widthNew),median(fillOld),median(fillNew),median(lowOld),median(lowNew),lowerBase,median(reachOld),median(reach),Collections.max(reachOld),Collections.max(reach),Collections.max(woodReach),median(peaksOld),median(peaksNew),median(cardOld),median(cardNew),median(sectorsOld),median(sectorsNew),median(offOld),median(offNew),silhouettes.size());
        require(leafMedian>.8&&leafMedian<1.2,"Leaf mass comparable, not a blanket reduction: median per-stem ratio "+leafMedian);
        require(logMedian>.8&&logMedian<2.0,"Limb wood bounded: median per-stem ratio "+logMedian);
        require(Math.abs(median(lowNew)-median(lowOld))<=.06,"The crown base stays near its published height fraction (it still moves per stem, see HUMID_CROWNS_BASE): "+median(lowOld)+" -> "+median(lowNew));
        require(silhouettes.size()>n*.9,"Stems differ individually by their metric hash: "+silhouettes.size()+" distinct of "+n);
        require(median(cardNew)<median(cardOld),"Outer foliage is less aligned to the four cardinal axes: "+median(cardOld)+" -> "+median(cardNew));
    }
    private static int countAbove(List<Double> values,double limit){int c=0;for(double v:values)if(v>limit)c++;return c;}
    /** Diagnostic only (never asserted): warm-JIT construction time per mature real stem, published versus gated, median of five passes. */
    private static void cost(){
        var stems=new ArrayList<WoodlandTrees.Root>();for(var r:stems())if(r.height()>=HumidCrowns.MATURE_HEIGHT)stems.add(r);
        long sink=0;for(int w=0;w<3;w++)for(var r:stems)sink+=HumidTrees.tree(r,false).size()+HumidTrees.tree(r,true).size();
        double[] medians=new double[2];
        for(int variant=0;variant<2;variant++){var t=new double[5];
            for(int k=0;k<5;k++){long begin=System.nanoTime();for(var r:stems)sink+=HumidTrees.tree(r,variant==1).size();t[k]=(System.nanoTime()-begin)/1e3/stems.size();}
            Arrays.sort(t);medians[variant]=t[2];}
        System.out.printf("HUMID_CROWNS_COST us/stem published %.0f gated %.0f over %d mature stems (warm JIT, median of 5; diagnostic only, no frame-rate claim) %d%n",medians[0],medians[1],stems.size(),sink&1);
    }
    private record Metrics(int top,int leaves,int logs,int width,int lowestLeaf,int depth,double maxExtent,double woodExtent,int box,boolean attached,int peaks,double cardinal,int sectors,double centroidOffset){}
    private static Metrics measure(WoodlandTrees.Root root,List<WoodlandTrees.Voxel> tree){
        int top=0,leaves=0,logs=0,lo=99,hi=-1,x0=99,x1=-99,z0=99,z1=-99;double extent=0,woodExtent=0;var wood=new HashSet<Pos>();var minX=new int[64];var maxX=new int[64];Arrays.fill(minX,99);Arrays.fill(maxX,-99);
        var layer=new int[64];var layerX=new double[64];var layerZ=new double[64];int outer=0,outerCardinal=0;var sector=new boolean[8];
        for(var v:tree){
            int x=v.x()-root.x(),y=v.y()-root.y(),z=v.z()-root.z();top=Math.max(top,y);double e=Math.max(Math.abs(x),Math.abs(z));extent=Math.max(extent,e);
            if(WoodlandTrees.logCode(v.code())){logs++;wood.add(key(x,y,z));woodExtent=Math.max(woodExtent,e);continue;}
            leaves++;lo=Math.min(lo,y);hi=Math.max(hi,y);minX[y]=Math.min(minX[y],x);maxX[y]=Math.max(maxX[y],x);x0=Math.min(x0,x);x1=Math.max(x1,x);z0=Math.min(z0,z);z1=Math.max(z1,z);
            layer[y]++;layerX[y]+=x;layerZ[y]+=z;
            double r=Math.hypot(x,z);
            if(r>=3){outer++;double ang=Math.atan2(z,x);double q=Math.abs(((ang/(Math.PI/2))%1+1)%1);double dist=Math.min(q,1-q)*90;if(dist<=22.5)outerCardinal++;sector[(int)(((ang+Math.PI)/(2*Math.PI))*8)%8]=true;}
        }
        int width=0;for(int y=0;y<minX.length;y++)if(maxX[y]>=minX[y])width=Math.max(width,maxX[y]-minX[y]+1);
        int peaks=0;for(int y=lo+1;y<hi;y++)if(layer[y]>layer[y-1]&&layer[y]>=layer[y+1]&&layer[y]>0)peaks++;
        double off=0;int layers=0;var offs=new ArrayList<Double>();for(int y=lo;y<=hi;y++)if(layer[y]>0){offs.add(Math.hypot(layerX[y]/layer[y],layerZ[y]/layer[y]));layers++;}
        Collections.sort(offs);double centroid=offs.isEmpty()?0:offs.get(offs.size()/2);
        int sectors=0;for(boolean s:sector)if(s)sectors++;
        var seen=new HashSet<Pos>();var queue=new ArrayDeque<int[]>();
        if(wood.contains(key(0,0,0))){queue.add(new int[]{0,0,0});seen.add(key(0,0,0));}
        while(!queue.isEmpty()){var p=queue.poll();for(var s:DIR){int x=p[0]+s[0],y=p[1]+s[1],z=p[2]+s[2];var k=key(x,y,z);if(wood.contains(k)&&seen.add(k))queue.add(new int[]{x,y,z});}}
        return new Metrics(top,leaves,logs,width,lo,hi-lo+1,extent,woodExtent,(x1-x0+1)*(z1-z0+1)*(hi-lo+1),seen.size()==wood.size()&&!wood.isEmpty(),peaks,outer==0?0:outerCardinal/(double)outer,sectors,centroid);
    }
    private static double median(List<Double> values){var v=new ArrayList<>(values);Collections.sort(v);return v.get(v.size()/2);}
    private record Pos(int x,int y,int z){}
    private static Pos key(int x,int y,int z){return new Pos(x,y,z);}
    private static void requireThrows(Runnable r,String message){try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private HumidCrownsCheck(){}
}
