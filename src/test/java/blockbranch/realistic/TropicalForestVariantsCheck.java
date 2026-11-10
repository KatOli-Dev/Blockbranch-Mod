package blockbranch.realistic;

import java.util.*;

/** Remaining domain gains, open recruitment and native culm continuity at physical coordinates. */
public final class TropicalForestVariantsCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var e=new GroundEcology.Field(d::sample,c);int[] counts=new int[3];
            for(int z=-1700000;z<=1700000;z+=16000)for(int x=-1700000;x<=1700000;x+=16000){
                int kind=TropicalForestVariants.kind(seed,x,z,d,c);counts[kind]++;if(kind==0)continue;
                require(!Jungle.contains(seed,x,z,d,c)&&!Savanna.contains(seed,x,z,d,c)&&!Forest.contains(seed,x,z,d,c)&&!BirchForest.contains(seed,x,z,d,c)&&!Swamp.contains(seed,x,z,d,c)&&!MangroveSwamp.contains(seed,x,z,d,c)&&!Plains.contains(seed,x,z,d,c)&&!MontaneMeadow.contains(seed,x,z,d,c),"Completed native domains retained");
                require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY&&!LandVegetation.reserved(d,x,z)&&SlopeCorrection.slope(d,x,z)<=.30,"Actual dry suitable terrain, not altitude painting");
            }
            require(counts[1]>10&&counts[2]>10,"Both natural warm forest selections present");
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.TropicalForestVariantsFixtures.fixture(d,seed,kind);int[] cohorts=new int[3];
                var trees=TropicalVariantTrees.roots(seed,p.x()-128,p.z()-128,p.x()+128,p.z()+128,d,c);
                for(var root:trees)if(root.family()==(kind==1?12:13)){cohorts[root.height()<12?0:root.height()<18?1:2]++;require(root.y()==d.sample(root.x(),root.z()).height()+1,"Native soil-supported roots");}
                if(kind==1)require(cohorts[0]>0&&cohorts[1]>0&&cohorts[2]>0&&trees.size()<TreeScatter.points(seed,p.x()-128,p.z()-128,p.x()+128,p.z()+128).size()*.65,"Sparse stand retains gaps and three cohorts");
                System.out.println("TROPICAL_TREE_COHORT seed="+seed+" kind="+kind+" anchor="+p+" cohorts="+Arrays.toString(cohorts));
            }
            var p=blockbranch.realistic.TropicalForestVariantsFixtures.fixture(d,seed,2);int blocks=0,stems=0,offQuart=0;var heights=new HashSet<Integer>();var stored=new LinkedHashMap<String,int[]>();
            for(int cz=(p.z()>>4)-2;cz<=(p.z()>>4)+2;cz++)for(int cx=(p.x()>>4)-2;cx<=(p.x()>>4)+2;cx++){
                int[] base=WoodlandTrees.column(seed,cx,cz,d,c),bamboo=BambooGroves.column(seed,cx,cz,base,d,c,e);require(base.length+bamboo.length<=8192,"Existing bounded canopy capacity");stored.put(cx+","+cz,bamboo);var byColumn=new HashMap<Integer,List<Integer>>();
                for(int v:bamboo){int key=v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,code=v&blockbranch.cubic.CanopyLayer.STATE_MASK,wx=cx*16+(key&15),wz=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;require(BambooGroves.bamboo(code)&&!WoodlandTrees.logCode(code),"Native bamboo stays plant wood, not leaf-support logs");require(TropicalForestVariants.surfaceKind(seed,wx,wz,d,c)==2&&y>d.sample(wx,wz).height(),"Block-resolution supported bamboo domain");byColumn.computeIfAbsent(key&255,k->new ArrayList<>()).add(v);blocks++;}
                for(var list:byColumn.values()){stems++;int first=list.getFirst()>>>blockbranch.cubic.CanopyLayer.STATE_BITS,last=list.getLast()>>>blockbranch.cubic.CanopyLayer.STATE_BITS;require(list.size()>=5&&list.size()<=16&&(list.getLast()&blockbranch.cubic.CanopyLayer.STATE_MASK)==126,"Native finished growth cap and height range");require((last>>>8)-(first>>>8)+1==list.size(),"No floating or broken culms");heights.add(list.size());if((first&3)!=0||((first>>>4)&3)!=0)offQuart++;}
            }
            require(blocks>200&&stems>20&&heights.size()>3&&offQuart>stems*.6,"Meaningful variable-height colonies use all physical cells");
            var keys=new ArrayList<>(stored.keySet());Collections.reverse(keys);for(String key:keys){var q=key.split(",");int cx=Integer.parseInt(q[0]),cz=Integer.parseInt(q[1]);require(Arrays.equals(stored.get(key),BambooGroves.column(seed,cx,cz,WoodlandTrees.column(seed,cx,cz,d,c),d,c,e)),"Chunk/request order stable");}
            System.out.println("TROPICAL_VARIANTS seed="+seed+" sparse="+counts[1]+" bamboo="+counts[2]+" stems="+stems+" blocks="+blocks+" heights="+heights+" offQuart="+offQuart);
        }
        System.out.println("TROPICAL_REMAINING_DOMAIN_COHORT_BAMBOO_CONTINUITY_ORDER_PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private TropicalForestVariantsCheck(){}
}
