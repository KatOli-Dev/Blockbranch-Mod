package blockbranch.realistic;

import java.util.*;

/** Seeded narrow evergreen spires with lower whorls and scattered lighter birch crowns. */
public final class BorealTrees {
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork drainage,ClimateField climate) {
        return roots(seed,minX,minZ,maxX,maxZ,drainage,climate,BorealSlope.PUBLISHED_RELIEF);
    }
    /** {@code relief} bounds each footprint sample's height difference from the stem. */
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork drainage,ClimateField climate,int relief) {
        var result=new ArrayList<WoodlandTrees.Root>();int radius=WoodlandTrees.RADIUS;
        var ecology=new GroundEcology.Field(drainage::sample,climate);
        for(var point:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius,TreeScatter.Spacing.CONIFER)){
            int x=point.x(),z=point.z();long v=OakBirchWoodland.mix(point.priority()^0x62B04EA9L);
            boolean grove=Grove.contains(seed,x,z,drainage,climate,ecology);
            if(grove?!Grove.establishes(seed,point,ecology.sample(x,z)):BorealWoodland.glade(seed,x,z) || !BorealWoodland.contains(seed,x,z,drainage,climate) || !TreeScatter.establishes(seed,point,1,ecology.sample(x,z)))continue;
            var center=drainage.sample(x,z);boolean safe=true;
            for(int dz=-radius;dz<=radius;dz+=radius) for(int dx=-radius;dx<=radius;dx+=radius) {
                var s=drainage.sample(x+dx,z+dz);
                if(s.waterLevel()!=DrainageNetwork.DRY || Math.abs(s.height()-center.height())>relief || drainage.channelAt(x+dx,z+dz).edge()>=0) safe=false;
            }
            if(!safe) continue;
            if(grove){result.add(Grove.root(point,center));continue;}
            int oldGrowth=OldGrowthTaiga.kind(seed,x,z,drainage,climate);
            if(oldGrowth>0){if(OldGrowthTaiga.establishes(point,oldGrowth))result.add(OldGrowthTaiga.root(point,center,oldGrowth));continue;}
            if(Taiga.contains(seed,x,z,drainage,climate)){result.add(TaigaTrees.root(point,center));continue;}
            if(SnowyTaiga.contains(seed,x,z,drainage,climate)){result.add(TaigaTrees.snowyRoot(point,center));continue;}
            boolean birch=Long.remainderUnsigned(v>>>16,5)==0;
            int height=point.young()?9+(int)Long.remainderUnsigned(v>>>24,4):(birch?12:16)+(int)Long.remainderUnsigned(v>>>24,birch?5:7);
            result.add(new WoodlandTrees.Root(x,center.height()+1,z,birch,height,(int)(v>>>32)));
        }
        return result;
    }
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root) { return tree(root,true,false); }
    /** {@code pine} selects the saved mature-pine crown architecture ({@link PineCrowns}); it changes only family-7 stems of mature height. */
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root,boolean pine) { return tree(root,true,pine); }
    /** Historical saved-world crowns and young warm-climate pines retain this exact shape. */
    static List<WoodlandTrees.Voxel> legacyTree(WoodlandTrees.Root root) { return tree(root,false,false); }
    private static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root,boolean whorled,boolean pine) {
        if(root.family()==4||root.family()==5||root.family()==6||root.family()==7||root.family()==8)return TaigaTrees.tree(root,pine);
        if(root.birch()) return WoodlandTrees.tree(root);
        var raw=new HashMap<Long,Integer>();int h=root.height();
        for(int y=0;y<h-1;y++) raw.put(key(0,y,0),19);
        int skirt=3+Math.floorMod(root.variant(),3);
        whorled&=root.family()==0;
        double spread=2.9+((root.variant()>>>8)&255)/255.0*1.3;
        for(int y=skirt;y<h;y++) {
            double fraction=(h-1-y)/(double)(h-1-skirt);
            // Broad boughs alternate with narrower collars. Crown width varies
            // by tree, while the established woody scaffold stays unchanged.
            double inset=whorled&&Math.floorMod(y-skirt,3)!=1?.95:0;
            int radius=Math.min(4,(int)Math.ceil((whorled?spread:3.5)*fraction-inset));
            if(whorled)radius=Math.max(1,radius);
            if(y==h-1) radius=0;
            for(int z=-radius;z<=radius;z++) for(int x=-radius;x<=radius;x++) {
                if(Math.abs(x)+Math.abs(z)>radius+1) continue;
                long v=OakBirchWoodland.mix(root.variant() ^ x*0x9E3779B97F4A7C15L ^ z*0xC2B2AE3D27D4EB4FL ^ y*0x165667B19E3779F9L);
                // Layered whorls and slight asymmetric edges, supported by the central stem.
                if(Math.max(Math.abs(x),Math.abs(z))==radius && (v&7)==0 && radius>1) continue;
                raw.putIfAbsent(key(x,y,z),22);
            }
        }
        // Short woody whorls connect the broader bottom skirt without persistent foliage.
        for(int y=skirt+1;y<h-4;y+=3) for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
            raw.put(key(d[0],y,d[1]),d[0]!=0 ? 20 : 21);
        // A native leaf cap conceals short branch ends without changing timber
        // materials or the support scaffold. Historical warm crowns are exact.
        if(whorled)for(int y=skirt+1;y<h-4;y+=3)for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
            raw.putIfAbsent(key(d[0]*2,y,d[1]*2),22);
        var distance=new HashMap<Long,Integer>();var queue=new ArrayDeque<Long>();
        for(var e:raw.entrySet()) if(WoodlandTrees.logCode(e.getValue())) { distance.put(e.getKey(),0);queue.add(e.getKey()); }
        while(!queue.isEmpty()) {
            long k=queue.removeFirst();int dist=distance.get(k);if(dist>=6) continue;
            int x=x(k),y=y(k),z=z(k);
            for(int[] d:DIRECTIONS) {
                long n=key(x+d[0],y+d[1],z+d[2]);
                if(raw.getOrDefault(n,0)==22 && distance.getOrDefault(n,7)>dist+1) { distance.put(n,dist+1);queue.add(n); }
            }
        }
        var result=new ArrayList<WoodlandTrees.Voxel>();
        for(var e:raw.entrySet()) {
            int code=e.getValue(),dist=distance.getOrDefault(e.getKey(),7);if(code==22) { if(dist>6) continue;code=21+dist; }
            long k=e.getKey();result.add(new WoodlandTrees.Voxel(root.x()+x(k),root.y()+y(k),root.z()+z(k),code));
        }
        result.sort(Comparator.comparingInt(WoodlandTrees.Voxel::y).thenComparingInt(WoodlandTrees.Voxel::z).thenComparingInt(WoodlandTrees.Voxel::x));return result;
    }
    private static long key(int x,int y,int z) { return ((long)(y+32)<<16)|((long)(z+32)<<8)|(x+32); }
    private static int x(long k) { return (int)(k&255)-32; }
    private static int z(long k) { return (int)((k>>>8)&255)-32; }
    private static int y(long k) { return (int)(k>>>16)-32; }
    private static final int[][] DIRECTIONS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private BorealTrees() {}
}
