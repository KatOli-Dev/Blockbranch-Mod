package blockbranch.realistic;
import java.util.*;
import blockbranch.cubic.CanopyLayer;

/** Climate ownership, natural domains, grass colonies and cross-chunk supported acacia crowns. */
public final class SavannaCheck {
    public static void main(String[] args){
        checkScaffolds();
        require(Savanna.climateSuitable(new ClimateField.Sample(23.001,.18)),"Warm/dry inclusive limits");
        for(var s:List.of(new ClimateField.Sample(23,.3),new ClimateField.Sample(30.01,.3),new ClimateField.Sample(25,.17999),new ClimateField.Sample(25,.55)))require(!Savanna.climateSuitable(s),"Reject other climates");
        var dry=new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false);var climate=new ClimateField.Sample(25,.35);
        require(Savanna.eligible(dry,.3,false,climate),"Supported moderate terrain");
        require(!Savanna.eligible(dry,.30001,false,climate)&&!Savanna.eligible(dry,0,true,climate)&&!Savanna.eligible(new DrainageNetwork.Sample(263,267,0,true),0,false,climate),"Steep, reserved and wet sites excluded");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var point=blockbranch.realistic.SavannaFixtures.fixture(d,seed);var ecology=new GroundEcology.Field(d::sample,c);
            int[] cover=new int[13];int used=0;
            for(int z=point.z()-128;z<point.z()+128;z++)for(int x=point.x()-128;x<point.x()+128;x++){
                boolean selected=Savanna.contains(seed,x,z,d,c);
                require(selected==Savanna.contains(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c),"Seeded quart seams");
                if(!selected)continue;used++;
                require(PlacementBaseline.actual(seed,x,z,d,c).equals("placeholder"),"All seventeen prior domains preserved");
                int code=Savanna.cover(seed,x,z,ecology.sample(x,z),true);cover[code]++;
                require(code==Savanna.cover(seed,x,z,ecology.sample(x,z),true),"Stable colonies");
            }
            require(used>1000&&cover[9]>100&&cover[10]+cover[11]>100&&cover[12]>10,"Real open soil/grass mosaic");
            var roots=SavannaTrees.roots(seed,point.x()-128,point.z()-128,point.x()+128,point.z()+128,d,c);
            require(roots.size()>4&&roots.size()<140,"Sparse natural acacia structure");
            var map=new HashMap<String,Integer>();int cx=point.x()>>4,cz=point.z()>>4;var chunks=new LinkedHashMap<String,int[]>();boolean crossing=false;
            for(int z=cz-1;z<=cz+1;z++)for(int x=cx-1;x<=cx+1;x++){
                int[] blocks=WoodlandTrees.column(seed,x,z,d,c);chunks.put(x+","+z,blocks);int last=-1;
                for(int packed:blocks){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;require(key>last&&((code>=28&&code<=36)||code==CanopyLayer.ACACIA_WOOD_CODE),"Sorted acacia-only canopy");last=key;map.put((x*16+(key&15))+","+((key>>>8)+TerrainShape.FLOOR)+","+(z*16+((key>>>4)&15)),code);}
            }
            for(var e:map.entrySet()){
                if(WoodlandTrees.logCode(e.getValue()))continue;var xyz=e.getKey().split(",");int x=Integer.parseInt(xyz[0]),y=Integer.parseInt(xyz[1]),z=Integer.parseInt(xyz[2]);
                if((x>>4)!=cx||(z>>4)!=cz)continue;int distance=WoodlandTrees.leafDistance(e.getValue());boolean support=false;
                for(int[] step:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){Integer near=map.get((x+step[0])+","+(y+step[1])+","+(z+step[2]));if(near!=null&&(WoodlandTrees.logCode(near)?distance==1:WoodlandTrees.leafDistance(near)<distance))support=true;}
                require(support,"Real connected leaf support across native chunk seams");
            }
            var order=new ArrayList<>(chunks.keySet());Collections.reverse(order);
            for(var key:order){var pos=key.split(",");require(Arrays.equals(chunks.get(key),WoodlandTrees.column(seed,Integer.parseInt(pos[0]),Integer.parseInt(pos[1]),d,c)),"Reverse chunk-order canopy equality");}
            for(var root:roots)for(var voxel:SavannaTrees.tree(root))crossing|=(voxel.x()>>4)!=(root.x()>>4)||(voxel.z()>>4)!=(root.z()>>4);
            require(crossing,"Natural crowns cross chunk boundaries");
            System.out.printf("SAVANNA seed=%d point=%s climate=%s roots=%d cover=%s%n",seed,point,c.sample(point.x(),point.z(),d::sample),roots.size(),Arrays.toString(cover));
        }
        System.out.println("SAVANNA_CLIMATE_LEGACY_DOMAINS_COLONIES_CANOPY_PASS");
    }
    private static void checkScaffolds(){
        int[][] directions={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for(int height:new int[]{6,9,12})for(int variant=0;variant<64;variant++){
            var root=new WoodlandTrees.Root(0,0,0,false,height,(int)OakBirchWoodland.mix(variant*1297L));
            var logs=new HashSet<List<Integer>>();
            for(var voxel:SavannaTrees.tree(root)){
                require(Math.abs(voxel.x())<=WoodlandTrees.RADIUS&&Math.abs(voxel.z())<=WoodlandTrees.RADIUS&&voxel.y()>=0&&voxel.y()<WoodlandTrees.MAX_HEIGHT,"Acacia geometry stays inside its declared scatter bounds");
                if(voxel.x()!=0||voxel.z()!=0)require(voxel.y()>=3,"Off-trunk geometry retains clearance over the establishment site's three-meter terrain tolerance");
                if(WoodlandTrees.logCode(voxel.code()))logs.add(List.of(voxel.x(),voxel.y(),voxel.z()));
            }
            var seen=new HashSet<List<Integer>>();var queue=new ArrayDeque<List<Integer>>();
            var origin=List.of(0,0,0);require(logs.contains(origin),"Acacia keeps its grounded root");queue.add(origin);seen.add(origin);
            while(!queue.isEmpty()){
                var pos=queue.removeFirst();
                for(var step:directions){var next=List.of(pos.get(0)+step[0],pos.get(1)+step[1],pos.get(2)+step[2]);if(logs.contains(next)&&seen.add(next))queue.add(next);}
            }
            require(seen.size()==logs.size(),"Every scaffold log connects to its rooted trunk through block faces");
        }
        System.out.println("SAVANNA_SCAFFOLD_CONNECTIVITY_BOUNDS_PASS variants=192");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
