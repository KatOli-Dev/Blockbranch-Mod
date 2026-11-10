package blockbranch.realistic;

import java.util.function.BiFunction;

/** Habitat boundaries, metre scale, seeded patchiness and world-coordinate stability. */
public final class AlpineScreeCheck {
    public static void main(String[] args) {
        BiFunction<Integer,Integer,DrainageNetwork.Sample> slope = (x,z) -> sample(3063 + Math.floorDiv(x,2), DrainageNetwork.DRY);
        require(AlpineScree.contains(0,0,slope), "Dry 27 degree high slope");
        require(!AlpineScree.contains(0,0,(x,z)->sample(3063,DrainageNetwork.DRY)), "Exclude flat summits");
        require(!AlpineScree.contains(0,0,(x,z)->sample(3063+x*2,DrainageNetwork.DRY)), "Exclude cliffs");
        require(!AlpineScree.contains(0,0,(x,z)->sample(1063+x/2,DrainageNetwork.DRY)), "Exclude lowland");
        require(!AlpineScree.contains(0,0,(x,z)->sample(5063+x/2,DrainageNetwork.DRY)), "Exclude high nival zone");
        require(!AlpineScree.contains(0,0,(x,z)->sample(3063+x/2,3200)), "Exclude inland water");
        BiFunction<Integer,Integer,DrainageNetwork.Sample> wetBoundary=(x,z)->sample(3063+Math.floorDiv(x,2),x==1?3200:DrainageNetwork.DRY);
        require(AlpineScree.contains(1,0,wetBoundary),"Biome cell retains its dry origin classification");
        require(!AlpineScree.surfaceContains(1,0,wetBoundary),"Material eligibility checks actual wet boundary block");
        for (int elevation : new int[]{2200,4200}) require(AlpineScree.contains(0,0,(x,z)->sample(63+elevation+x/2,DrainageNetwork.DRY)), "Inclusive elevation limit");
        for (int elevation : new int[]{2199,4201}) require(!AlpineScree.contains(0,0,(x,z)->sample(63+elevation+x/2,DrainageNetwork.DRY)), "Outside elevation limit");
        for (int x : new int[]{-2097152,-17,-16,-1,0,15,16,2097151}) for (int z : new int[]{-17,-1,0,15,16}) {
            boolean habitat=AlpineScree.contains(x,z,slope);
            require(habitat==AlpineScree.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,slope),"Quart alignment including negative coordinates");
            require(AlpineScree.depth(424242,x,z,false)==0,"No debris outside habitat");
        }
        int[] depths=new int[4]; int changed=0;
        for(int z=-256;z<=256;z+=12) for(int x=-256;x<=256;x+=12) {
            int d=AlpineScree.depth(424242,x,z,true);
            require(d==AlpineScree.depth(424242,x,z,true),"Deterministic cover"); depths[d]++;
            if(d!=AlpineScree.depth(1,x,z,true)) changed++;
        }
        for(int count:depths) require(count>100,"Bedrock and each debris thickness present");
        require(changed>500,"Seed-sensitive bedrock ribs");
        // The reported Snow overlook: isolate material assignment from height.
        // A cell hash changes only at multiples of twelve; white noise would
        // evade that check, so also require spatially coherent debris thickness.
        int gridEdges=0, interiorEdges=0, neighbors=0;
        for(int z=-1900140;z<-1900012;z++)for(int x=-1332108;x<-1331980;x++) {
            int depth=AlpineScree.depth(424242,x,z,true);
            for(int[] offset:new int[][]{{1,0},{0,1}}) {
                int nx=x+offset[0],nz=z+offset[1];
                int next=AlpineScree.depth(424242,nx,nz,true);
                require(Math.abs(next-depth)<=1,"Continuous debris thickness, not per-block dithering");
                neighbors++;
                if(depth==next)continue;
                if(Math.floorMod(offset[0]==1?nx:nz,12)==0)gridEdges++;else interiorEdges++;
            }
        }
        require(interiorEdges>4*gridEdges,"Material boundaries must not follow 12 m tiles");
        require(gridEdges+interiorEdges<neighbors/4,"Coherent material patches rather than white noise");
        for(long seed:new long[]{0,1,-17,424242}) {
            var shape=new TerrainShape(seed);
            BiFunction<Integer,Integer,DrainageNetwork.Sample> raw=(x,z)->sample(shape.height(x,z),DrainageNetwork.DRY);
            int count=0;
            for(int z=-1700000;z<=1700000;z+=16000) for(int x=-1700000;x<=1700000;x+=16000)
                if(AlpineScree.contains(x,z,raw)) count++;
            require(count>10,"Habitat exists in multiple mountain regions for seed "+seed);
            System.out.println("Alpine scree seed="+seed+" sampled habitat cells="+count);
        }
        System.out.println("ALPINE_SCREE_SHAPE_PASS");
    }
    private static DrainageNetwork.Sample sample(int height,int water) { return new DrainageNetwork.Sample(height,water,0,false); }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private AlpineScreeCheck() {}
}
