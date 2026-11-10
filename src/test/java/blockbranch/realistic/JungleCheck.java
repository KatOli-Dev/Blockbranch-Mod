package blockbranch.realistic;
import java.util.*;
import blockbranch.cubic.CanopyLayer;

/** Tropical ownership, bounded reassignment, layered crowns and deterministic supported seams. */
public final class JungleCheck {
    public static void main(String[]args){
        require(Jungle.climateSuitable(new ClimateField.Sample(23.001,.65))&&Jungle.climateSuitable(new ClimateField.Sample(30,.95)),"Tropical endpoints");
        for(var c:List.of(new ClimateField.Sample(23,.8),new ClimateField.Sample(30.001,.8),new ClimateField.Sample(25,.64999),new ClimateField.Sample(25,.95001)))require(!Jungle.climateSuitable(c),"Reject cooler/drier/excessively wet climates");
        var dry=new DrainageNetwork.Sample(100,DrainageNetwork.DRY,0,false);var climate=new ClimateField.Sample(25,.8);
        require(Jungle.eligible(dry,.30,false,climate)&&!Jungle.eligible(dry,.30001,false,climate)&&!Jungle.eligible(dry,0,true,climate)&&!Jungle.eligible(new DrainageNetwork.Sample(100,104,0,true),0,false,climate),"Dry supported terrain only");
        int[] cohortWitness=new int[4];
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var point=blockbranch.realistic.JungleFixtures.fixture(d,seed);var ecology=new GroundEcology.Field(d::sample,c);int[] covers=new int[13];int count=0;
            for(int z=point.z()-96;z<point.z()+96;z++)for(int x=point.x()-96;x<point.x()+96;x++){
                boolean selected=Jungle.contains(seed,x,z,d,c);require(selected==Jungle.contains(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c),"Global quart boundaries");if(!selected)continue;count++;
                var old=PlacementBaseline.definition24(seed,x,z,d,c);require(Set.of("warm-woodland","humid-woodland","placeholder").contains(old),"Only tropical prior analogues or bare land reallocated");
                require(!WarmWoodland.contains(seed,x,z,d,c)&&!HumidWoodland.contains(seed,x,z,d,c)&&!Savanna.contains(seed,x,z,d,c),"Unique final ownership");
                var site=ecology.sample(x,z).shade(!Jungle.glade(seed,x,z));int cover=Jungle.cover(seed,x,z,site,true);covers[cover]++;require(cover==Jungle.cover(seed,x,z,site,true)&&Jungle.cover(seed,x,z,site,false)==8,"Stable supported floor colonies");
            }
            require(count>1000&&covers[8]>100&&covers[9]>100&&covers[10]>100,"Rooted floor/litter/fern structure");
            var roots=JungleTrees.roots(seed,point.x()-64,point.z()-64,point.x()+64,point.z()+64,d,c);require(roots.size()>8&&roots.stream().anyMatch(r->r.height()>=25)&&roots.stream().anyMatch(r->r.height()<20),"Mature canopy and young lower cohort");
            int maxLeaves=0;boolean thick=false;
            for(var root:roots){cohortWitness[root.height()<15?0:root.height()<23?1:root.height()<31?2:3]++;var tree=JungleTrees.tree(root);int leaves=0;for(var v:tree)if(!WoodlandTrees.logCode(v.code()))leaves++;maxLeaves=Math.max(maxLeaves,leaves);if(root.height()>=24)thick|=tree.stream().filter(v->WoodlandTrees.logCode(v.code())&&v.y()==root.y()).count()>1;}
            System.out.printf("JUNGLE_FORM_DIAGNOSTIC seed=%d roots=%d maxLeaves=%d basalFlare=%s%n",seed,roots.size(),maxLeaves,thick);
            require(maxLeaves>700&&thick,"Substantial mature crowns and real basal flares");
            int cx=point.x()>>4,cz=point.z()>>4;var chunks=new LinkedHashMap<String,int[]>();var blocks=new HashMap<String,Integer>();
            for(int z=cz-1;z<=cz+1;z++)for(int x=cx-1;x<=cx+1;x++){
                int[] data=WoodlandTrees.column(seed,x,z,d,c);chunks.put(x+","+z,data);require(data.length<CanopyLayer.MAX_BLOCKS,"Compact canopy bound");int previous=-1;
                for(int packed:data){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;require(key>previous&&code>=37&&code<=45,"Sorted Jungle materials");previous=key;int wx=x*16+(key&15),wz=z*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;require(d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY&&y>d.sample(wx,wz).height(),"No submerged or buried canopy");blocks.put(wx+","+y+","+wz,code);}
            }
            for(var e:blocks.entrySet()){
                int code=e.getValue();if(WoodlandTrees.logCode(code))continue;var xyz=e.getKey().split(",");int x=Integer.parseInt(xyz[0]),y=Integer.parseInt(xyz[1]),z=Integer.parseInt(xyz[2]);if((x>>4)!=cx||(z>>4)!=cz)continue;int distance=WoodlandTrees.leafDistance(code);boolean support=false;
                for(var v:DIR){var near=blocks.get((x+v[0])+","+(y+v[1])+","+(z+v[2]));if(near!=null&&(WoodlandTrees.logCode(near)?distance==1:WoodlandTrees.leafDistance(near)<distance))support=true;}require(support,"Leaf support through actual neighbouring chunk states");
            }
            var order=new ArrayList<>(chunks.keySet());Collections.reverse(order);for(var key:order){var pos=key.split(",");require(Arrays.equals(chunks.get(key),WoodlandTrees.column(seed,Integer.parseInt(pos[0]),Integer.parseInt(pos[1]),d,c)),"Chunk order independence");}
            System.out.printf("JUNGLE seed=%d point=%s climate=%s roots=%d maxLeaves=%d floor=%s%n",seed,point,c.sample(point.x(),point.z(),d::sample),roots.size(),maxLeaves,Arrays.toString(covers));
        }
        for(int count:cohortWitness)require(count>2,"Every natural stand cohort represented across surveyed seeds");
        System.out.println("JUNGLE_COHORT_WITNESSES "+Arrays.toString(cohortWitness));
        System.out.println("JUNGLE_CLIMATE_OWNERSHIP_ROOTED_FLOOR_LAYERED_CANOPY_PASS");
    }
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static void require(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
}
