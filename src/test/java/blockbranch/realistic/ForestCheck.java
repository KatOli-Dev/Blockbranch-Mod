package blockbranch.realistic;
import java.util.*;
import blockbranch.cubic.CanopyLayer;

/** Canopy architecture/cohorts, bounded real leaf support, seeded geometry and actual adjacent chunk seams. */
public final class ForestCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void main(String[]args){
        var silhouettes=new HashSet<String>();var fixedHeightShapes=new HashSet<String>();var tops=new HashSet<Integer>();int lowerYoung=0;
        for(int n=0;n<64;n++){
            var root=new WoodlandTrees.Root(0,0,0,false,6+Math.floorMod(n*7,23),(int)OakBirchWoodland.mix(n*997L),1);var tree=ForestTrees.tree(root);var map=new HashMap<String,Integer>();var shape=new TreeSet<String>();int lowest=99,highest=0;
            for(var v:tree){map.put(v.x()+","+v.y()+","+v.z(),v.code());if(v.code()>6){lowest=Math.min(lowest,v.y());highest=Math.max(highest,v.y());shape.add(v.x()+","+v.z());}require(Math.abs(v.x())<=8&&Math.abs(v.z())<=8&&v.y()<32,"Bounded crown/height");}
            var visited=new HashSet<String>();var queue=new ArrayDeque<String>();queue.add("0,0,0");while(!queue.isEmpty()){var key=queue.removeFirst();if(!visited.add(key))continue;var xyz=key.split(",");int x=Integer.parseInt(xyz[0]),y=Integer.parseInt(xyz[1]),z=Integer.parseInt(xyz[2]);for(var d:DIR){String next=(x+d[0])+","+(y+d[1])+","+(z+d[2]);Integer code=map.get(next);if(code!=null&&code<=6&&!visited.contains(next))queue.add(next);}}
            for(var v:tree)if(v.code()<=6)require(visited.contains(v.x()+","+v.y()+","+v.z()),"Every branch face-connected to rooted trunk");else{int distance=WoodlandTrees.leafDistance(v.code());boolean supported=false;for(var d:DIR){var near=map.get((v.x()+d[0])+","+(v.y()+d[1])+","+(v.z()+d[2]));if(near!=null&&(near<=6?distance==1:WoodlandTrees.leafDistance(near)<distance))supported=true;}require(supported,"Actual leaf-support path");}
            for(int y=0;y<Math.min(3,root.height());y++)require(map.get("0,"+y+",0")==1,"Bark-continuous lower stem,no alternating cut-end ladder");
            if(root.height()<11){require(lowest<=3,"Young tree foliage extends down branching stem,not bare pole with leaf ball");lowerYoung++;}silhouettes.add(shape.toString());tops.add(highest);
            var fixed=new TreeSet<String>();for(var v:ForestTrees.tree(new WoodlandTrees.Root(0,0,0,false,20,(int)OakBirchWoodland.mix(n*997L),1)))if(v.code()>6)fixed.add(v.x()+","+v.z());fixedHeightShapes.add(fixed.toString());
        }
        System.out.printf("FOREST_ARCHITECTURE_DIAGNOSTIC footprints=%d fixedHeightFootprints=%d tops=%d youngCases=%d%n",silhouettes.size(),fixedHeightShapes.size(),tops.size(),lowerYoung);
        require(silhouettes.size()>40&&fixedHeightShapes.size()>35&&tops.size()>16&&lowerYoung>8,"Varied coherent crown footprints and cohorts");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=blockbranch.realistic.ForestFixtures.fixture(d,seed);var roots=WoodlandTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c).stream().filter(r->r.family()==1&&Forest.contains(seed,r.x(),r.z(),d,c)).toList();int[] cohorts=new int[4];int birch=0;
            for(var r:roots){cohorts[r.height()<11?0:r.height()<17?1:r.height()<24?2:3]++;if(r.birch())birch++;require(Forest.contains(seed,r.x(),r.z(),d,c),"Roots belong to closed Forest");}
            require(roots.size()>12&&birch>0&&birch<roots.size()/2&&cohorts[0]>0&&cohorts[2]>0,"Oak-dominant stand with mixed species and age cohorts");
            int cx=p.x()>>4,cz=p.z()>>4;var chunks=new TreeMap<String,int[]>();var actual=new HashMap<String,Integer>();int max=0;
            for(int z=cz-1;z<=cz+1;z++)for(int x=cx-1;x<=cx+1;x++){var blocks=WoodlandTrees.column(seed,x,z,d,c);require(blocks.length<CanopyLayer.MAX_BLOCKS,"Bounded compact canopy");max=Math.max(max,blocks.length);chunks.put(x+","+z,blocks);for(int packed:blocks){int key=packed>>>CanopyLayer.STATE_BITS,wx=x*16+(key&15),wz=z*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR,code=packed&CanopyLayer.STATE_MASK;require(code>=1&&code<=18&&y>d.sample(wx,wz).height()&&d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY,"Valid dry supported temperate blocks");actual.put(wx+","+y+","+wz,code);}}
            for(int packed:chunks.get(cx+","+cz)){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;if(code<=6)continue;int x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;boolean supported=false;for(var v:DIR){Integer near=actual.get((x+v[0])+","+(y+v[1])+","+(z+v[2]));if(near!=null&&(near<=6?WoodlandTrees.leafDistance(code)==1:WoodlandTrees.leafDistance(near)<WoodlandTrees.leafDistance(code)))supported=true;}require(supported,"Merged clipped canopy supports across actual neighbouring chunks");}
            var order=new ArrayList<>(chunks.keySet());Collections.reverse(order);for(String key:order){var xy=key.split(",");require(Arrays.equals(chunks.get(key),WoodlandTrees.column(seed,Integer.parseInt(xy[0]),Integer.parseInt(xy[1]),d,c)),"Reverse chunk-order independence");}
            System.out.printf("FOREST_STRUCTURE seed=%d point=%s roots=%d birch=%d cohorts=%s maxColumnVoxels=%d%n",seed,p,roots.size(),birch,Arrays.toString(cohorts),max);
        }
        System.out.println("FOREST_ARCHITECTURE silhouettes="+silhouettes.size()+" tops="+tops.size());System.out.println("FOREST_COHORT_BRANCH_SUPPORT_SEAMS_PASS");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
