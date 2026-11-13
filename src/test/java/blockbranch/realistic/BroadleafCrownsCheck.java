package blockbranch.realistic;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Saved-gate numbering, the frozen published ordinary-forest canopy and the gated oak crown morphology.
 * The published oak is a bare pole under one fused ovoid; the gated oak divides into oblique limbs that carry
 * separate lobes. Both variants are measured on the same stems. Metrics here are diagnostics for a bounded
 * change (same height, comparable leaf mass, attached, other families untouched), not visual approval.
 */
public final class BroadleafCrownsCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final int[][] SITES={{48000,-304000},{-1744000,-1040000},{720000,-464000},{16000,-240000}};
    public static void main(String[] args)throws Exception{
        schema();
        published();
        otherFamilies();
        badlandOaks();
        morphology();
        deadwoodClear();
        System.out.println("BROADLEAF_CROWNS_GATED_PASS");
    }
    /** Schema 274 exists only as the complete mangrove-roots 273 plus the explicit definition; bit 256 is single and distinct. */
    private static void schema(){
        for(int saved=0;saved<=300;saved++){
            require(BroadleafCrownSchema.baseSchema(saved,0)==(saved==274?Integer.MAX_VALUE:saved),"Absent definition never reads 274 ("+saved+")");
            require(BroadleafCrownSchema.matches(saved,1)==(saved==274),"Only 274 reads under the definition ("+saved+")");
        }
        require(BroadleafCrownSchema.schema(273,1)==274&&BroadleafCrownSchema.baseSchema(274,1)==273,"Complete independent wrapper");
        require(BroadleafCrownSchema.schema(272,0)==272&&BroadleafCrownSchema.schema(273,0)==273,"Absent definition leaves the schema");
        for(int base:new int[]{270,271,272,274})requireThrows(()->BroadleafCrownSchema.schema(base,1),"Wrapper rejects incomplete base "+base);
        requireThrows(()->BroadleafCrownSchema.schema(273,2),"Unknown definition rejected");
        require(BroadleafCrowns.FLAG==256&&BroadleafCrowns.enabled(256|3)&&!BroadleafCrowns.enabled(255),"Flag bit 256 is its own meaning");
    }
    /** Every published window, with the broadleaf bit absent, stays bit-identical to the frozen columns. */
    private static void published()throws Exception{
        int rows=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(BroadleafCrownsCheck.class.getResourceAsStream("/fixtures/broadleaf-crowns-273/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");
                var row=window(Long.parseLong(f[1]),Integer.parseInt(f[2]),Integer.parseInt(f[3]),Integer.parseInt(f[4]));
                require(row.equals(f[5]+" "+f[6]),"Published ordinary-forest columns changed at "+f[0]+" flags "+f[4]+": "+row+" vs frozen "+f[5]+" "+f[6]);
                // The gate must change the forest windows (there are oaks) and nothing may throw with it set.
                var gated=window(Long.parseLong(f[1]),Integer.parseInt(f[2]),Integer.parseInt(f[3]),Integer.parseInt(f[4])|BroadleafCrowns.FLAG);
                if(f[0].startsWith("forest"))require(!gated.equals(row),"The gate changed nothing at "+f[0]);
                rows++;
            }
        }
        require(rows==15,"Every frozen window is checked");
    }
    private static String window(long seed,int cx,int cz,int flags)throws Exception{
        var d=BorealSlopeOracle.terrain(seed);var c=new ClimateField(seed,true);var md=MessageDigest.getInstance("SHA-256");int n=0;
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){int[] col=WoodlandTrees.column(seed,cx+dx,cz+dz,d,c,true,flags,null,null,null);n+=col.length;md.update(Arrays.toString(col).getBytes());}
        var hex=new StringBuilder();for(byte b:md.digest())hex.append(String.format("%02x",b));
        return n+" "+hex;
    }
    /** Only mature non-birch family-1 oaks change; every other family, young stems and birches keep the published voxels. */
    private static void otherFamilies(){
        int oaks=0,changed=0,others=0;
        for(var site:SITES){
            var d=BorealSlopeOracle.terrain(1);var c=new ClimateField(1,true);
            for(var root:WoodlandTrees.roots(1,site[0]-96,site[1]-96,site[0]+96,site[1]+96,d,c,0)){
                var old=WoodlandTrees.tree(root,0);var gated=WoodlandTrees.tree(root,BroadleafCrowns.FLAG);
                boolean target=root.family()==1&&!root.birch()&&root.height()>=11;
                if(target){oaks++;if(!old.equals(gated))changed++;}
                else{others++;require(old.equals(gated),"Non-target tree changed: family "+root.family()+" birch "+root.birch()+" h "+root.height());}
            }
        }
        require(oaks>=200&&changed==oaks&&others>=100,"Gate reshapes every target oak and no other tree: oaks "+oaks+" changed "+changed+" others "+others);
        // Families handed straight to the forest tree by other generators keep the published crown.
        for(int family:new int[]{2,TemperateForestVariants.BIRCH_FAMILY,DappledForest.FAMILY,DryForest.FAMILY,SummerDryWoodland.FAMILY,DarkCherryWoodland.DARK_FAMILY,DarkCherryWoodland.CHERRY_FAMILY,0}){
            var root=new WoodlandTrees.Root(0,0,0,false,24,1234567,family);
            if(family!=0)require(ForestTrees.tree(root,true).equals(ForestTrees.tree(root,false)),"ForestTrees scaffold ignores family "+family);
        }
    }
    /** Badland oaks are family 1 too but are not ordinary forest: their canopy columns must not change under the gate. */
    private static void badlandOaks()throws Exception{
        var d=BorealSlopeOracle.terrain(1);var c=new ClimateField(1,true);int x=96000,z=-180000,badland=0;
        for(var root:Badlands.roots(1,x-48,z-48,x+48,z+48,d,c))if(root.family()==1&&!root.birch()&&root.height()>=11)badland++;
        require(badland>=5,"Badland family-1 oaks exist at the witness: "+badland);
        int cx=Math.floorDiv(x,16),cz=Math.floorDiv(z,16);
        require(window(1,cx,cz,0).equals(window(1,cx,cz,BroadleafCrowns.FLAG)),"The gate leaves badland oak columns unchanged");
    }
    /** Same stems: crown top and leaf mass stay comparable, limbs attach, extents stay inside the shared grid. */
    private static void morphology(){
        var leaves=new ArrayList<Double>();var logsRatio=new ArrayList<Double>();var widthOld=new ArrayList<Double>();var widthNew=new ArrayList<Double>();var depthOld=new ArrayList<Double>();var depthNew=new ArrayList<Double>();
        int n=0,highTop=0;
        for(var site:SITES){
            var d=BorealSlopeOracle.terrain(1);var c=new ClimateField(1,true);
            for(var root:WoodlandTrees.roots(1,site[0]-96,site[1]-96,site[0]+96,site[1]+96,d,c,0)){
                if(root.family()!=1||root.birch()||root.height()<11)continue;
                var a=measure(root,ForestTrees.tree(root,false));var b=measure(root,ForestTrees.tree(root,true));n++;
                require(b.top>=root.height()-2&&b.top<=root.height()+1,"Crown top stays at the tree height: "+b.top+" vs "+root.height());
                require(b.top>=a.top-2&&b.top<=a.top+1,"Crown top near the published top");
                require(b.attached,"Every log of the gated oak is face-connected to its trunk base at "+root);
                require(b.maxExtent<=8.5,"Voxels stay inside the shared tree grid: "+b.maxExtent);
                require(b.lowestLeaf>=.40*root.height()-1,"No foliage below the lower crown: "+b.lowestLeaf+" of "+root.height());
                leaves.add(b.leaves/(double)a.leaves);logsRatio.add(b.logs/(double)a.logs);widthOld.add((double)a.width);widthNew.add((double)b.width);
                depthOld.add(a.depth/(double)a.width);depthNew.add(b.depth/(double)b.width);
            }
        }
        require(n>=200,"Enough mature oaks measured: "+n);
        double leafMedian=median(leaves),logMedian=median(logsRatio);
        require(leafMedian>.92&&leafMedian<1.12,"Leaf mass comparable: median ratio "+leafMedian);
        require(logMedian>1.0&&logMedian<1.4,"Limb wood bounded: median ratio "+logMedian);
        // The published crown is taller than wide (columnar); the gated crown is broader than deep on median.
        require(median(depthOld)>1.0&&median(depthNew)<.95,"Crown depth/width: "+median(depthOld)+" -> "+median(depthNew));
        require(median(widthNew)-median(widthOld)<=3&&median(widthNew)>=median(widthOld),"Spread widens by at most three blocks: "+median(widthOld)+" -> "+median(widthNew));
    }
    private record Metrics(int top,int leaves,int logs,int width,int depth,int lowestLeaf,double maxExtent,boolean attached){}
    private static Metrics measure(WoodlandTrees.Root root,List<WoodlandTrees.Voxel> tree){
        int top=0,leaves=0,logs=0,lo=99,hi=-1;double extent=0;var wood=new HashSet<Pos>();var minX=new int[ForestTrees.MAX_HEIGHT];var maxX=new int[ForestTrees.MAX_HEIGHT];Arrays.fill(minX,99);Arrays.fill(maxX,-99);
        for(var v:tree){
            int x=v.x()-root.x(),y=v.y()-root.y(),z=v.z()-root.z();top=Math.max(top,y);extent=Math.max(extent,Math.max(Math.abs(x),Math.abs(z)));
            if(v.code()<7){logs++;wood.add(key(x,y,z));continue;}
            leaves++;lo=Math.min(lo,y);hi=Math.max(hi,y);minX[y]=Math.min(minX[y],x);maxX[y]=Math.max(maxX[y],x);
        }
        int width=0;for(int y=0;y<minX.length;y++)if(maxX[y]>=minX[y])width=Math.max(width,maxX[y]-minX[y]+1);
        var seen=new HashSet<Pos>();var queue=new ArrayDeque<int[]>();
        if(wood.contains(key(0,0,0))){queue.add(new int[]{0,0,0});seen.add(key(0,0,0));}
        while(!queue.isEmpty()){var p=queue.poll();for(var s:DIR){int x=p[0]+s[0],y=p[1]+s[1],z=p[2]+s[2];var k=key(x,y,z);if(wood.contains(k)&&seen.add(k))queue.add(new int[]{x,y,z});}}
        return new Metrics(top,leaves,logs,width,hi-lo+1,lo,extent,seen.size()==wood.size()&&!wood.isEmpty());
    }
    /** Fallen deadwood is rejected against the same tree geometry the chunk uses, so it never lies inside a gated oak. */
    private static void deadwoodClear(){
        int logs=0;
        for(var site:SITES)for(int flags:new int[]{0,BroadleafCrowns.FLAG}){
            var d=BorealSlopeOracle.terrain(1);var c=new ClimateField(1,true);int cx0=Math.floorDiv(site[0],16),cz0=Math.floorDiv(site[1],16);
            for(int cz=cz0-6;cz<=cz0+6;cz++)for(int cx=cx0-6;cx<=cx0+6;cx++){
                var dead=WoodlandDetails.logs(1,cx,cz,d,c,flags);if(dead.length==0)continue;
                var occupied=new HashSet<Pos>();
                for(var root:WoodlandTrees.roots(1,cx*16-14,cz*16-14,cx*16+29,cz*16+29,d,c,flags))for(var v:WoodlandTrees.tree(root,flags))occupied.add(key(v.x(),v.y(),v.z()));
                for(int packed:dead){
                    int k=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS;int x=cx*16+(k&15),z=cz*16+((k>>>4)&15),y=(k>>>8)+TerrainShape.FLOOR;
                    require(!occupied.contains(key(x,y,z)),"Deadwood inside a live tree at "+x+","+y+","+z+" flags "+flags);logs++;
                }
            }
        }
        require(logs>0,"Deadwood was sampled");
    }
    private static double median(List<Double> values){var v=new ArrayList<>(values);Collections.sort(v);return v.get(v.size()/2);}
    private record Pos(int x,int y,int z){}
    private static Pos key(int x,int y,int z){return new Pos(x,y,z);}
    private static void requireThrows(Runnable r,String message){try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private BroadleafCrownsCheck(){}
}
