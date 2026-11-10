package blockbranch.realistic;

import java.util.*;

/** Climate suitability, retained meadow space, supported natural crowns and order-independent chunk clipping. */
public final class WoodlandCheck {
    public static void main(String[] args) {
        for(var climate:List.of(new ClimateField.Sample(5,0.6),new ClimateField.Sample(20,0.6),new ClimateField.Sample(12,0.4),new ClimateField.Sample(12,0.9)))
            require(!OakBirchWoodland.climateSuitable(climate),"Reject unsuitable temperature or moisture");
        require(OakBirchWoodland.climateSuitable(new ClimateField.Sample(12,0.6)),"Mesic temperate suitability");
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var point=blockbranch.realistic.OakBirchWoodlandFixtures.fixture(drainage,seed);
            int wood=0,open=0;
            for(int z=point.z()-768;z<=point.z()+768;z+=16) for(int x=point.x()-768;x<=point.x()+768;x+=16) {
                if(OakBirchWoodland.stand(seed,x,z)>=0.55) wood++;else open++;
                var a=OakBirchWoodland.stand(seed,x,z);var b=OakBirchWoodland.stand(seed,x+1,z+1);
                require(Math.abs(a-b)<0.02,"Continuous stand patches across native and negative seams");
            }
            require(wood>500 && open>500,"Retain substantial open meadow space around stands");
            var roots=WoodlandTrees.roots(seed,point.x()-32,point.z()-32,point.x()+32,point.z()+32,drainage,climate);
            require(roots.stream().anyMatch(WoodlandTrees.Root::birch) && roots.stream().anyMatch(r->!r.birch()),"Both tree architectures in stand");
            int maxLeaves=0,branches=0;boolean crossing=false;
            for(var root:roots) {
                var tree=WoodlandTrees.tree(root);var map=new HashMap<String,Integer>();
                for(var v:tree) map.put(v.x()+","+v.y()+","+v.z(),v.code());
                int leaves=0;
                for(var v:tree) {
                    if(v.code()<=6) { if(v.code()%3!=1) branches++;continue; }
                    leaves++;int distance=(v.code()-7)%6+1;boolean supported=false;
                    for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}) {
                        Integer near=map.get((v.x()+d[0])+","+(v.y()+d[1])+","+(v.z()+d[2]));
                        if(near!=null && (near<=6 ? distance==1 : (near-7)%6+1<distance)) supported=true;
                    }
                    require(supported,"Every natural leaf has a real connected support path");
                    crossing|=(v.x()>>4)!=(root.x()>>4) || (v.z()>>4)!=(root.z()>>4);
                }
                maxLeaves=Math.max(maxLeaves,leaves);
            }
            require(maxLeaves>150 && branches>20 && crossing,"Branching nontrivial crowns crossing native chunk borders");
            int cx=point.x()>>4,cz=point.z()>>4;var order=new ArrayList<String>();var baseline=new HashMap<String,int[]>();long started=System.nanoTime();
            for(int z=cz-1;z<=cz+1;z++) for(int x=cx-1;x<=cx+1;x++) {
                String key=x+","+z;order.add(key);var blocks=WoodlandTrees.column(seed,x,z,drainage,climate);baseline.put(key,blocks);
                require(blocks.length<8192,"Bounded compact tree overlay");int previous=-1;
                for(int packed:blocks) { int position=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS;require(position>previous && (packed&blockbranch.cubic.CanopyLayer.STATE_MASK)>=1 && (packed&blockbranch.cubic.CanopyLayer.STATE_MASK)<=18,"Sorted unique valid tree block codes");previous=position; }
            }
            var actual=new HashMap<String,Integer>();
            for(var entry:baseline.entrySet()) {
                var p=entry.getKey().split(",");int chunkX=Integer.parseInt(p[0]),chunkZ=Integer.parseInt(p[1]);
                for(int packed:entry.getValue()) { int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS;actual.put((chunkX*16+(key&15))+","+((key>>>8)+TerrainShape.FLOOR)+","+(chunkZ*16+((key>>>4)&15)),packed&blockbranch.cubic.CanopyLayer.STATE_MASK); }
            }
            for(int packed:baseline.get(cx+","+cz)) {
                int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,code=packed&blockbranch.cubic.CanopyLayer.STATE_MASK;if(code<=6) continue;
                int x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR,distance=(code-7)%6+1;boolean support=false;
                for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}) {
                    Integer near=actual.get((x+d[0])+","+(y+d[1])+","+(z+d[2]));
                    if(near!=null && (near<=6 ? distance==1 : (near-7)%6+1<distance)) support=true;
                }
                require(support,"Merged and terrain-clipped canopy has consistent leaf support across native seams");
            }
            Collections.reverse(order);
            for(String key:order) {var p=key.split(",");require(Arrays.equals(baseline.get(key),WoodlandTrees.column(seed,Integer.parseInt(p[0]),Integer.parseInt(p[1]),drainage,climate)),"Reverse chunk request order produces identical canopy");}
            for(var root:roots) for(var voxel:WoodlandTrees.tree(root)) {
                int x=voxel.x()>>4,z=voxel.z()>>4;var stored=baseline.get(x+","+z);if(stored==null) continue;
                var terrain=drainage.sample(voxel.x(),voxel.z());if(terrain.waterLevel()!=DrainageNetwork.DRY || drainage.channelAt(voxel.x(),voxel.z()).edge()>=0 || voxel.y()<=terrain.height()) continue;
                int position=((voxel.y()-TerrainShape.FLOOR)<<8)|((voxel.z()&15)*16+(voxel.x()&15));
                boolean found=false;for(int packed:stored) if((packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS)==position) { found=true;break; }
                require(found,"Whole-tree voxels survive clipping on both sides of native seams");
            }
            System.out.printf("Woodland seed=%d fixture=%s roots=%d standSamples=%d openSamples=%d maxLeaves=%d clipMillis=%d%n",seed,point,roots.size(),wood,open,maxLeaves,(System.nanoTime()-started)/1000000);
        }
        System.out.println("DAPPLED_OAK_BIRCH_WOODLAND_SHAPE_CANOPY_PASS");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
