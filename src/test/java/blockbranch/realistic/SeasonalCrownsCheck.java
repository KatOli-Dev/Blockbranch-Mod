package blockbranch.realistic;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Saved-gate numbering, the frozen published dry-forest canopy and the gated seasonal dry-forest crown morphology.
 * The published mature stem is a bare pole under one fused ovoid; the gated stem divides into oblique limbs that carry
 * separate, smaller lobes. Both variants are measured on the same stems. Metrics here are diagnostics for a bounded
 * change (same height and cohorts, comparable leaf mass and width, attached, every other family untouched), not visual approval.
 */
public final class SeasonalCrownsCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final int[][] SITES={{1488000,-304000},{1520000,-336000}};
    public static void main(String[] args)throws Exception{
        schema();
        published();
        otherFamilies();
        morphology();
        System.out.println("SEASONAL_CROWNS_GATED_PASS");
    }
    /** Schema 275 exists only as the complete broadleaf-crown 274 plus the explicit definition; bit 512 is single and distinct. */
    private static void schema(){
        for(int saved=0;saved<=300;saved++){
            require(SeasonalCrownSchema.baseSchema(saved,0)==(saved==275?Integer.MAX_VALUE:saved),"Absent definition never reads 275 ("+saved+")");
            require(SeasonalCrownSchema.matches(saved,1)==(saved==275),"Only 275 reads under the definition ("+saved+")");
        }
        require(SeasonalCrownSchema.schema(274,1)==275&&SeasonalCrownSchema.baseSchema(275,1)==274,"Complete independent wrapper");
        require(SeasonalCrownSchema.schema(273,0)==273&&SeasonalCrownSchema.schema(274,0)==274,"Absent definition leaves the schema");
        for(int base:new int[]{270,272,273,275})requireThrows(()->SeasonalCrownSchema.schema(base,1),"Wrapper rejects incomplete base "+base);
        requireThrows(()->SeasonalCrownSchema.schema(274,2),"Unknown definition rejected");
        require(SeasonalCrowns.FLAG==512&&SeasonalCrowns.enabled(512|3)&&!SeasonalCrowns.enabled(511)&&SeasonalCrowns.FLAG!=BroadleafCrowns.FLAG,"Flag bit 512 is its own meaning");
    }
    /** Every published window, with the seasonal bit absent, stays bit-identical to the frozen columns, and the bit changes only dry-forest stands. */
    private static void published()throws Exception{
        int rows=0,changed=0,regional=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(SeasonalCrownsCheck.class.getResourceAsStream("/fixtures/seasonal-crowns-274/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");
                long seed=Long.parseLong(f[1]);int x=Integer.parseInt(f[2]),z=Integer.parseInt(f[3]),flags=Integer.parseInt(f[4]),mode=Integer.parseInt(f[5]);
                var row=window(seed,x,z,flags,mode);
                require(row.equals(f[6]+" "+f[7]),"Published columns changed at "+f[0]+" flags "+flags+" owner "+mode+": "+row+" vs frozen "+f[6]+" "+f[7]);
                var gated=window(seed,x,z,flags|SeasonalCrowns.FLAG,mode);
                if(mode==2){require(!gated.equals(row),"The gate changed nothing at "+f[0]);changed++;}
                else if(mode==1&&!gated.equals(row))regional++;
                else if(mode==0)require(gated.equals(row),"The seasonal bit changed a window with no dry-forest owner at "+f[0]);
                rows++;
            }
        }
        require(rows==13&&changed==4&&regional>=1,"Every frozen window is checked: "+rows+" rows, "+changed+" seasonal stands, "+regional+" regional stands");
    }
    private static String window(long seed,int sx,int sz,int flags,int mode)throws Exception{
        var d=BorealSlopeOracle.terrain(seed);var c=new ClimateField(seed,true);var md=MessageDigest.getInstance("SHA-256");int n=0;
        var owner=mode==0?null:DryForest.domain(seed,d,c,(x,z)->true,mode==2);int cx=Math.floorDiv(sx,16),cz=Math.floorDiv(sz,16);
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){int[] col=WoodlandTrees.column(seed,cx+dx,cz+dz,d,c,true,flags,owner,null,null);n+=col.length;md.update(Arrays.toString(col).getBytes());}
        var hex=new StringBuilder();for(byte b:md.digest())hex.append(String.format("%02x",b));
        return n+" "+hex;
    }
    private static List<WoodlandTrees.Root> stems(){
        var out=new ArrayList<WoodlandTrees.Root>();var d=BorealSlopeOracle.terrain(1);var c=new ClimateField(1,true);var owner=DryForest.domain(1,d,c,(x,z)->true,true);
        for(var site:SITES)out.addAll(DryForestTrees.roots(1,site[0]-96,site[1]-96,site[0]+96,site[1]+96,d,c,owner));
        return out;
    }
    /** Only mature dry-forest stems change; young stems, the ordinary oak bit and every other family keep the published voxels. */
    private static void otherFamilies(){
        int mature=0,young=0;
        for(var root:stems()){
            var old=ForestTrees.tree(root,false,false);var gated=ForestTrees.tree(root,false,true);
            require(root.family()==DryForest.FAMILY,"Dry-forest owner yields family 18 only: "+root.family());
            if(root.height()>=11){mature++;require(!old.equals(gated),"Gate reshapes every mature stem: "+root);}
            else{young++;require(old.equals(gated),"Young stem changed: "+root);}
            require(ForestTrees.tree(root,true,true).equals(gated),"The ordinary-oak bit does not touch family 18");
        }
        require(mature>=800&&young>=100,"Enough stems: mature "+mature+" young "+young);
        // Every other family ignores the seasonal request, including the ordinary oak that the broadleaf bit reshapes.
        for(int family:new int[]{1,2,TemperateForestVariants.BIRCH_FAMILY,DappledForest.FAMILY,SummerDryWoodland.FAMILY,DarkCherryWoodland.DARK_FAMILY,DarkCherryWoodland.CHERRY_FAMILY,0})for(boolean broadleaf:new boolean[]{false,true})for(int height:new int[]{9,16,24,31}){
            var root=new WoodlandTrees.Root(0,0,0,false,height,1234567,family);
            if(family!=0)require(ForestTrees.tree(root,broadleaf,true).equals(ForestTrees.tree(root,broadleaf,false)),"ForestTrees seasonal request ignores family "+family+" broadleaf "+broadleaf+" h "+height);
        }
    }
    /** Same stems: crown top, leaf mass and width stay comparable, limbs attach, extents stay inside the shared grid, crowns are less ovoid. */
    private static void morphology(){
        var leaves=new ArrayList<Double>();var logsRatio=new ArrayList<Double>();var widthOld=new ArrayList<Double>();var widthNew=new ArrayList<Double>();var fillOld=new ArrayList<Double>();var fillNew=new ArrayList<Double>();
        int n=0;
        for(var root:stems()){
            if(root.height()<11)continue;
            var a=measure(root,ForestTrees.tree(root,false,false));var b=measure(root,ForestTrees.tree(root,false,true));n++;
            require(b.top>=root.height()-2&&b.top<=root.height()+1,"Crown top stays at the tree height: "+b.top+" vs "+root.height());
            require(b.top>=a.top-2&&b.top<=a.top+1,"Crown top near the published top");
            require(b.attached,"Every log of the gated stem is face-connected to its trunk base at "+root);
            require(b.maxExtent<=8,"Voxels stay inside the shared tree grid: "+b.maxExtent);
            require(b.lowestLeaf>=.35*root.height()-1,"No foliage below the lower crown: "+b.lowestLeaf+" of "+root.height());
            leaves.add(b.leaves/(double)a.leaves);logsRatio.add(b.logs/(double)a.logs);widthOld.add((double)a.width);widthNew.add((double)b.width);
            fillOld.add(a.leaves/(double)a.box);fillNew.add(b.leaves/(double)b.box);
        }
        require(n>=800,"Enough mature stems measured: "+n);
        double leafMedian=median(leaves),logMedian=median(logsRatio);
        require(leafMedian>.90&&leafMedian<1.10,"Leaf mass comparable: median ratio "+leafMedian);
        require(logMedian>1.0&&logMedian<1.5,"Limb wood bounded: median ratio "+logMedian);
        require(median(widthNew)-median(widthOld)<=2&&median(widthNew)>=median(widthOld),"Spread widens by at most two blocks: "+median(widthOld)+" -> "+median(widthNew));
        // A fused ovoid fills its bounding box; separate lobes leave clefts, so the filled fraction of the box falls.
        require(median(fillNew)<median(fillOld),"Lobed crowns fill less of their box: "+median(fillOld)+" -> "+median(fillNew));
        System.out.printf("SEASONAL_CROWNS_MORPHOLOGY stems=%d leafRatio=%.3f logRatio=%.3f width %.0f->%.0f boxFill %.3f->%.3f%n",n,leafMedian,logMedian,median(widthOld),median(widthNew),median(fillOld),median(fillNew));
    }
    private record Metrics(int top,int leaves,int logs,int width,int lowestLeaf,double maxExtent,int box,boolean attached){}
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
        return new Metrics(top,leaves,logs,width,lo,extent,(x1-x0+1)*(z1-z0+1)*(hi-lo+1),seen.size()==wood.size()&&!wood.isEmpty());
    }
    private static double median(List<Double> values){var v=new ArrayList<>(values);Collections.sort(v);return v.get(v.size()/2);}
    private record Pos(int x,int y,int z){}
    private static Pos key(int x,int y,int z){return new Pos(x,y,z);}
    private static void requireThrows(Runnable r,String message){try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SeasonalCrownsCheck(){}
}
