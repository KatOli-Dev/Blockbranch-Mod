package blockbranch.realistic;

import java.util.*;

/** Climate suitability, retained meadow space, supported natural crowns and order-independent chunk clipping. */
public final class WarmWoodlandCheck {
    public static void main(String[] args) {
        for(var c:List.of(new ClimateField.Sample(19.001,0.35),new ClimateField.Sample(27,0.72)))require(WarmWoodland.climateSuitable(c),"Warm inclusive/exclusive endpoints");
        for(var c:List.of(new ClimateField.Sample(19,0.6),new ClimateField.Sample(27.01,0.6),new ClimateField.Sample(22,0.3499),new ClimateField.Sample(22,0.7201)))require(!WarmWoodland.climateSuitable(c),"Reject cool, excessively hot, dry and wetter climates");
        for(int t=-15;t<=35;t++)for(int m=0;m<=100;m++) {
            var c=new ClimateField.Sample(t,m/100.0);
            if(WarmWoodland.climateSuitable(c))require(!OakBirchWoodland.climateSuitable(c) && !BorealWoodland.climateSuitable(c) && !DrySteppe.climateSuitable(c),"Other woodland and steppe climate identities preserved");
        }
        var good=new ClimateField.Sample(22,0.6);
        require(WarmWoodland.eligible(new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false),0.35,false,good),"Inclusive dry slope");
        require(!WarmWoodland.eligible(new DrainageNetwork.Sample(263,266,0,true),0,false,good),"Water excluded");
        require(!WarmWoodland.eligible(new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false),0,true,good),"Specialist domain excluded");
        require(!WarmWoodland.eligible(new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false),0.35001,false,good),"Steep ground excluded");
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var point=blockbranch.realistic.WarmWoodlandFixtures.fixture(drainage,seed);
            require(WarmWoodland.contains(seed,point.x(),point.z(),drainage,climate),"Natural cold woodland");
            for(int sign:new int[]{-1,1}) for(int dz=-8;dz<=8;dz++) for(int dx=-8;dx<=8;dx++) {
                int wx=sign*Math.abs(point.x())+dx,wz=sign*Math.abs(point.z())+dz;
                require(WarmWoodland.contains(seed,wx,wz,drainage,climate)==WarmWoodland.contains(seed,Math.floorDiv(wx,4)*4,Math.floorDiv(wz,4)*4,drainage,climate),"Positive and negative global quart seams");
            }
            int wood=0,open=0;int[] cover=new int[9];
            for(int z=point.z()-128;z<point.z()+128;z++) for(int x=point.x()-128;x<point.x()+128;x++) {
                if(WarmWoodland.glade(seed,x,z)) open++;else wood++;
                int c=WarmWoodland.cover(seed,x,z,true,true);cover[c]++;
                require(c==WarmWoodland.cover(seed,x,z,true,true) && WarmWoodland.cover(seed,x,z,true,false)==0 && WarmWoodland.cover(seed,x,z,false,true)==0,"Stable, dry-only floor");
            }
            for(int c=1;c<=8;c++) require(cover[c]>0,"Every boreal floor component");
            require(wood>500 && open>100,"Retain openings among cold stands");
            for(boolean climateOnly:new boolean[]{false,true}) {
                var edge=blockbranch.realistic.WarmWoodlandFixtures.transition(drainage,seed,climateOnly);
                boolean in=climateOnly ? WarmWoodland.climateSuitable(climate.sample(edge.insideX(),edge.insideZ(),drainage::sample)) : WarmWoodland.contains(seed,edge.insideX(),edge.insideZ(),drainage,climate);
                boolean out=climateOnly ? WarmWoodland.climateSuitable(climate.sample(edge.outsideX(),edge.outsideZ(),drainage::sample)) : WarmWoodland.contains(seed,edge.outsideX(),edge.outsideZ(),drainage,climate);
                require(in && !out && Math.max(Math.abs(edge.insideX()-edge.outsideX()),Math.abs(edge.insideZ()-edge.outsideZ()))<=4,"Adjacent climate/terrain transition");
                require(edge.equals(blockbranch.realistic.WarmWoodlandFixtures.transition(drainage,seed,climateOnly)),"Deterministic transition");
            }
            var roots=WarmTrees.roots(seed,point.x()-32,point.z()-32,point.x()+32,point.z()+32,drainage,climate);
            require(roots.stream().anyMatch(WarmTrees.Root::pine) && roots.stream().anyMatch(r->!r.pine()),"Both tree architectures in stand");
            int maxLeaves=0,branches=0;boolean crossing=false;
            for(var root:roots) {
                var tree=WarmTrees.tree(root);var map=new HashMap<String,Integer>();
                for(var v:tree) map.put(v.x()+","+v.y()+","+v.z(),v.code());
                int leaves=0;
                for(var v:tree) {
                    if(WoodlandTrees.logCode(v.code())) { if(v.code()%3!=1) branches++;continue; }
                    leaves++;int distance=WoodlandTrees.leafDistance(v.code());boolean supported=false;
                    for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}) {
                        Integer near=map.get((v.x()+d[0])+","+(v.y()+d[1])+","+(v.z()+d[2]));
                        if(near!=null && (WoodlandTrees.logCode(near) ? distance==1 : WoodlandTrees.leafDistance(near)<distance)) supported=true;
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
                for(int packed:blocks) { int position=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS;require(position>previous && (packed&blockbranch.cubic.CanopyLayer.STATE_MASK)>=1 && (packed&blockbranch.cubic.CanopyLayer.STATE_MASK)<=27,"Sorted unique valid tree block codes");previous=position; }
            }
            var actual=new HashMap<String,Integer>();
            for(var entry:baseline.entrySet()) {
                var p=entry.getKey().split(",");int chunkX=Integer.parseInt(p[0]),chunkZ=Integer.parseInt(p[1]);
                for(int packed:entry.getValue()) { int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS;actual.put((chunkX*16+(key&15))+","+((key>>>8)+TerrainShape.FLOOR)+","+(chunkZ*16+((key>>>4)&15)),packed&blockbranch.cubic.CanopyLayer.STATE_MASK); }
            }
            for(int packed:baseline.get(cx+","+cz)) {
                int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,code=packed&blockbranch.cubic.CanopyLayer.STATE_MASK;if(WoodlandTrees.logCode(code)) continue;
                int x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR,distance=WoodlandTrees.leafDistance(code);boolean support=false;
                for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}) {
                    Integer near=actual.get((x+d[0])+","+(y+d[1])+","+(z+d[2]));
                    if(near!=null && (WoodlandTrees.logCode(near) ? distance==1 : WoodlandTrees.leafDistance(near)<distance)) support=true;
                }
                require(support,"Merged and terrain-clipped canopy has consistent leaf support across native seams");
            }
            Collections.reverse(order);
            for(String key:order) {var p=key.split(",");require(Arrays.equals(baseline.get(key),WoodlandTrees.column(seed,Integer.parseInt(p[0]),Integer.parseInt(p[1]),drainage,climate)),"Reverse chunk request order produces identical canopy");}
            for(var root:roots) for(var voxel:WarmTrees.tree(root)) {
                int x=voxel.x()>>4,z=voxel.z()>>4;var stored=baseline.get(x+","+z);if(stored==null) continue;
                var terrain=drainage.sample(voxel.x(),voxel.z());if(terrain.waterLevel()!=DrainageNetwork.DRY || drainage.channelAt(voxel.x(),voxel.z()).edge()>=0 || voxel.y()<=terrain.height()) continue;
                int position=((voxel.y()-TerrainShape.FLOOR)<<8)|((voxel.z()&15)*16+(voxel.x()&15));
                boolean found=false;for(int packed:stored) if((packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS)==position) { found=true;break; }
                require(found,"Whole-tree voxels survive clipping on both sides of native seams");
            }
            System.out.println("Warm climate="+climate.sample(point.x(),point.z(),drainage::sample)+" cover="+Arrays.toString(cover));
            System.out.printf("Warm seed=%d fixture=%s roots=%d standSamples=%d openSamples=%d maxLeaves=%d clipMillis=%d%n",seed,point,roots.size(),wood,open,maxLeaves,(System.nanoTime()-started)/1000000);
        }
        System.out.println("WARM_OAK_PINE_SHAPE_CLIMATE_CANOPY_PASS");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
