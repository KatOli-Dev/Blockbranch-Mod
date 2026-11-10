package blockbranch.realistic;

import java.util.*;

/** Climate suitability, retained meadow space, supported natural crowns and order-independent chunk clipping. */
public final class HumidWoodlandCheck {
    public static void main(String[] args) {
        for(var c:List.of(new ClimateField.Sample(19.001,0.72001),new ClimateField.Sample(29,0.95)))require(HumidWoodland.climateSuitable(c),"Warm inclusive/exclusive endpoints");
        for(var c:List.of(new ClimateField.Sample(19,0.8),new ClimateField.Sample(29.01,0.8),new ClimateField.Sample(22,0.72),new ClimateField.Sample(22,0.95001)))require(!HumidWoodland.climateSuitable(c),"Reject cool, excessively hot, dry and wetter climates");
        for(int t=-15;t<=35;t++)for(int m=0;m<=100;m++) {
            var c=new ClimateField.Sample(t,m/100.0);
            if(HumidWoodland.climateSuitable(c))require(!OakBirchWoodland.climateSuitable(c) && !BorealWoodland.climateSuitable(c) && !DrySteppe.climateSuitable(c) && !WarmWoodland.climateSuitable(c),"Other woodland and steppe climate identities preserved");
        }
        var good=new ClimateField.Sample(22,0.8);
        require(HumidWoodland.eligible(new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false),0.35,false,good),"Inclusive dry slope");
        require(!HumidWoodland.eligible(new DrainageNetwork.Sample(263,266,0,true),0,false,good),"Water excluded");
        require(!HumidWoodland.eligible(new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false),0,true,good),"Specialist domain excluded");
        require(!HumidWoodland.eligible(new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false),0.35001,false,good),"Steep ground excluded");
        int shadedFerns=0,openFerns=0,fungi=0;
        for(int z=-96;z<96;z++)for(int x=-96;x<96;x++){
            int shade=HumidWoodland.cover(424242,x,z,new GroundEcology.Site(.85,.4,.1,0,0,true),true);
            int open=HumidWoodland.cover(424242,x,z,new GroundEcology.Site(.85,.4,.1,0,0,false),true);
            if(shade==4)shadedFerns++;if(open==4)openFerns++;if(shade==5)fungi++;
            require(open!=5,"Fungi stay in moist canopy shade");
        }
        require(shadedFerns>openFerns*3&&fungi>5,"Humid fern/fungal colonies follow canopy conditions");
        for(long seed:new long[]{424242,1,-17}) {
            var drainage=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var point=blockbranch.realistic.HumidWoodlandFixtures.fixture(drainage,seed);
            require(HumidWoodland.contains(seed,point.x(),point.z(),drainage,climate),"Natural humid woodland");
            for(int sign:new int[]{-1,1}) for(int dz=-8;dz<=8;dz++) for(int dx=-8;dx<=8;dx++) {
                int wx=sign*Math.abs(point.x())+dx,wz=sign*Math.abs(point.z())+dz;
                require(HumidWoodland.contains(seed,wx,wz,drainage,climate)==HumidWoodland.contains(seed,Math.floorDiv(wx,4)*4,Math.floorDiv(wz,4)*4,drainage,climate),"Positive and negative global quart seams");
            }
            int wood=0,open=0;int[] cover=new int[8];
            for(int z=point.z()-128;z<point.z()+128;z++) for(int x=point.x()-128;x<point.x()+128;x++) {
                if(HumidWoodland.glade(seed,x,z)) open++;else wood++;
                var site=new GroundEcology.Site(.85,.3,.1,0,0,!HumidWoodland.glade(seed,x,z));
                int c=HumidWoodland.cover(seed,x,z,site,true);cover[c]++;
                require(c==HumidWoodland.cover(seed,x,z,site,true),"Stable floor colonies");
                require(HumidWoodland.cover(seed,x,z,site,false)==1,"Clearance under low stems");
            }
            for(int c=1;c<=7;c++) require(cover[c]>0,"Every humid floor component");
            require(wood>500 && open>100,"Retain openings among humid stands");
            for(boolean climateOnly:new boolean[]{false,true}) {
                var edge=blockbranch.realistic.HumidWoodlandFixtures.transition(drainage,seed,climateOnly);
                boolean in=climateOnly ? HumidWoodland.climateSuitable(climate.sample(edge.insideX(),edge.insideZ(),drainage::sample)) : HumidWoodland.contains(seed,edge.insideX(),edge.insideZ(),drainage,climate);
                boolean out=climateOnly ? HumidWoodland.climateSuitable(climate.sample(edge.outsideX(),edge.outsideZ(),drainage::sample)) : HumidWoodland.contains(seed,edge.outsideX(),edge.outsideZ(),drainage,climate);
                require(in && !out && Math.max(Math.abs(edge.insideX()-edge.outsideX()),Math.abs(edge.insideZ()-edge.outsideZ()))<=4,"Adjacent climate/terrain transition");
                require(edge.equals(blockbranch.realistic.HumidWoodlandFixtures.transition(drainage,seed,climateOnly)),"Deterministic transition");
            }
            var roots=HumidTrees.roots(seed,point.x()-32,point.z()-32,point.x()+32,point.z()+32,drainage,climate);
            require(roots.stream().anyMatch(r->r.height()<=12) && roots.stream().anyMatch(r->r.height()>=19),"Young and mature broadleaf layers");
            int maxLeaves=0,branches=0;boolean crossing=false;
            for(var root:roots) {
                var tree=HumidTrees.tree(root);var map=new HashMap<String,Integer>();
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
            for(var root:roots) for(var voxel:HumidTrees.tree(root)) {
                int x=voxel.x()>>4,z=voxel.z()>>4;var stored=baseline.get(x+","+z);if(stored==null) continue;
                var terrain=drainage.sample(voxel.x(),voxel.z());if(terrain.waterLevel()!=DrainageNetwork.DRY || drainage.channelAt(voxel.x(),voxel.z()).edge()>=0 || voxel.y()<=terrain.height()) continue;
                int position=((voxel.y()-TerrainShape.FLOOR)<<8)|((voxel.z()&15)*16+(voxel.x()&15));
                boolean found=false;for(int packed:stored) if((packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS)==position) { found=true;break; }
                require(found,"Whole-tree voxels survive clipping on both sides of native seams");
            }
            System.out.println("Humid climate="+climate.sample(point.x(),point.z(),drainage::sample)+" cover="+Arrays.toString(cover));
            System.out.printf("Humid seed=%d fixture=%s roots=%d standSamples=%d openSamples=%d maxLeaves=%d clipMillis=%d%n",seed,point,roots.size(),wood,open,maxLeaves,(System.nanoTime()-started)/1000000);
        }
        System.out.println("HUMID_OAK_FERN_SHAPE_CLIMATE_CANOPY_PASS");
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
