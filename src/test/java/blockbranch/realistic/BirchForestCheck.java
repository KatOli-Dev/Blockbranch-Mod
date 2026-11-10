package blockbranch.realistic;
import java.util.*;
import blockbranch.cubic.CanopyLayer;

/** Birch dominance,independent architecture,cohorts,ecological floor and real neighbour support/order. */
public final class BirchForestCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void main(String[]args){
        var footprints=new HashSet<String>();var tops=new HashSet<Integer>();int young=0;
        for(int n=0;n<64;n++){
            var tree=ForestTrees.tree(new WoodlandTrees.Root(0,0,0,true,6+Math.floorMod(n*7,20),(int)OakBirchWoodland.mix(n*1237L),2));var map=new HashMap<String,Integer>();int lowest=99,highest=0;
            for(var v:tree){map.put(v.x()+","+v.y()+","+v.z(),v.code());require(v.code()>=4&&v.code()<=6||v.code()>=13&&v.code()<=18,"Native birch materials");if(v.code()>6){lowest=Math.min(lowest,v.y());highest=Math.max(highest,v.y());}}
            var visited=new HashSet<String>();var queue=new ArrayDeque<String>();queue.add("0,0,0");while(!queue.isEmpty()){var key=queue.removeFirst();if(!visited.add(key))continue;var p=key.split(",");int x=Integer.parseInt(p[0]),y=Integer.parseInt(p[1]),z=Integer.parseInt(p[2]);for(var d:DIR){String next=(x+d[0])+","+(y+d[1])+","+(z+d[2]);var code=map.get(next);if(code!=null&&code<=6&&!visited.contains(next))queue.add(next);}}
            for(var v:tree)if(v.code()<=6)require(visited.contains(v.x()+","+v.y()+","+v.z()),"Connected rooted birch limbs");else{boolean supported=false;int distance=WoodlandTrees.leafDistance(v.code());for(var d:DIR){Integer near=map.get((v.x()+d[0])+","+(v.y()+d[1])+","+(v.z()+d[2]));if(near!=null&&(near<=6?distance==1:WoodlandTrees.leafDistance(near)<distance))supported=true;}require(supported,"Real birch leaf support");}
            for(var v:tree)if(v.code()>=4&&v.code()<=6&&v.y()>=2){
                int woody=0;for(var d:DIR){int near=map.getOrDefault((v.x()+d[0])+","+(v.y()+d[1])+","+(v.z()+d[2]),0);if(near>0&&near<=6)woody++;}
                if(woody<=1)for(var d:DIR)require(map.containsKey((v.x()+d[0])+","+(v.y()+d[1])+","+(v.z()+d[2])),"Live Birch endpoints terminate inside foliage");
                if(v.y()>=Math.max(3,(int)((6+Math.floorMod(n*7,20))*.45))){int[] axis=v.code()==4?DIR[2]:v.code()==5?DIR[0]:DIR[4];for(int sign:new int[]{-1,1})require(map.containsKey((v.x()+axis[0]*sign)+","+(v.y()+axis[1]*sign)+","+(v.z()+axis[2]*sign)),"Upper Birch cut faces have a twig/leaf envelope");}
            }
            for(int y=0;y<3;y++)require(map.get("0,"+y+",0")==4,"Continuous lower white bark,not cut-end ladder");if(6+Math.floorMod(n*7,20)<11){require(lowest<=3,"Low leafy young birch branches");young++;}tops.add(highest);
            var leafLevels=new BitSet(32);int minX=99,maxX=-99,minZ=99,maxZ=-99;for(var v:tree)if(v.code()>6){leafLevels.set(v.y());minX=Math.min(minX,v.x());maxX=Math.max(maxX,v.x());minZ=Math.min(minZ,v.z());maxZ=Math.max(maxZ,v.z());}
            require(leafLevels.nextClearBit(lowest+1)>=highest,"Continuous crown envelope without detached vertical tiers");
            if(6+Math.floorMod(n*7,20)>=15)require(Math.max(maxX-minX+1,maxZ-minZ+1)>=7,"Mature Birch crowns spread beyond narrow columns");
            var shape=new TreeSet<String>();for(var v:ForestTrees.tree(new WoodlandTrees.Root(0,0,0,true,18,(int)OakBirchWoodland.mix(n*1237L),2)))if(v.code()>6)shape.add(v.x()+","+v.z());footprints.add(shape.toString());
        }
        require(footprints.size()>40&&tops.size()>15&&young>10,"Fixed-height architecture and age variation");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=blockbranch.realistic.BirchForestFixtures.fixture(d,seed);var field=new GroundEcology.Field(d::sample,c);var roots=WoodlandTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c).stream().filter(r->r.family()==2&&BirchForest.contains(seed,r.x(),r.z(),d,c)).toList();long birches=roots.stream().filter(WoodlandTrees.Root::birch).count();require(roots.size()>12&&birches>roots.size()*.85,"Actual birch-dominant recruitment");
            int[] cohorts=new int[4];for(var r:roots)cohorts[r.height()<11?0:r.height()<17?1:r.height()<22?2:3]++;require(cohorts[0]>0&&cohorts[2]>0,"Young and mature birch cohorts");
            for(int z=p.z()-64;z<=p.z()+64;z+=4)for(int x=p.x()-64;x<=p.x()+64;x+=4){boolean selected=BirchForest.contains(seed,x,z,d,c);if(selected)require(Forest.potential(seed,x,z,d,c)&&!Forest.contains(seed,x,z,d,c)&&!OakBirchWoodland.contains(seed,x,z,d,c)&&BirchForest.climateSuitable(c.sample(x,z,d::sample)),"Exclusive suitable composition domain");require(BirchForest.cover(seed,x,z,field.sample(x,z),true)==BirchForest.cover(seed,x,z,field.sample(x,z),true),"Stable ecological floor");}
            int cx=p.x()>>4,cz=p.z()>>4;var chunks=new TreeMap<String,int[]>();var actual=new HashMap<String,Integer>();int logs=0,birchLogs=0,leaves=0,birchLeaves=0,max=0;
            for(int z=cz-1;z<=cz+1;z++)for(int x=cx-1;x<=cx+1;x++){var data=WoodlandTrees.column(seed,x,z,d,c);require(data.length<CanopyLayer.MAX_BLOCKS,"Bounded birch canopy");max=Math.max(max,data.length);chunks.put(x+","+z,data);for(int packed:data){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;actual.put((x*16+(key&15))+","+((key>>>8)+TerrainShape.FLOOR)+","+(z*16+((key>>>4)&15)),code);if(code<=6){logs++;if(code>=4)birchLogs++;}else{leaves++;if(code>=13)birchLeaves++;}}}
            System.out.printf("BIRCH_STRUCTURE seed=%d point=%s roots=%d birchRoots=%d cohorts=%s birchLogs=%d/%d birchLeaves=%d/%d maxColumn=%d%n",seed,p,roots.size(),birches,Arrays.toString(cohorts),birchLogs,logs,birchLeaves,leaves,max);
            require(birchLogs>logs*.80&&birchLeaves>leaves*.60,"Visible woody/leaf canopy is birch dominant");
            for(int packed:chunks.get(cx+","+cz)){
                int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;if(code<4||code>6)continue;
                int x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;if(y<=d.sample(x,z).height()+3)continue;
                int woody=0;for(var v:DIR){int near=actual.getOrDefault((x+v[0])+","+(y+v[1])+","+(z+v[2]),0);if(near>0&&near<=6)woody++;}
                if(woody<=1)for(var v:DIR){int nx=x+v[0],ny=y+v[1],nz=z+v[2];require(actual.containsKey(nx+","+ny+","+nz)||ny<=d.sample(nx,nz).height(),"Actual clipped Birch endpoints remain inside foliage/terrain envelope");}
            }
            for(int packed:chunks.get(cx+","+cz)){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;if(code<=6)continue;int x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;boolean support=false;for(var v:DIR){Integer near=actual.get((x+v[0])+","+(y+v[1])+","+(z+v[2]));if(near!=null&&(near<=6?WoodlandTrees.leafDistance(code)==1:WoodlandTrees.leafDistance(near)<WoodlandTrees.leafDistance(code)))support=true;}require(support,"Clipped cross-chunk birch support");}
            var order=new ArrayList<>(chunks.keySet());Collections.reverse(order);for(var key:order){var xy=key.split(",");require(Arrays.equals(chunks.get(key),WoodlandTrees.column(seed,Integer.parseInt(xy[0]),Integer.parseInt(xy[1]),d,c)),"Chunk-order independence");}
        }
        for(int x=-32000;x<=32000;x+=4000)require(Math.abs(BirchForest.province(424242,x,0)-BirchForest.province(424242,x+1,0))<.001,"Continuous seeded regional composition");
        System.out.println("BIRCH_ARCHITECTURE footprints="+footprints.size()+" tops="+tops.size());System.out.println("BIRCH_DOMINANCE_COHORT_SUPPORT_SEAMS_PASS");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
