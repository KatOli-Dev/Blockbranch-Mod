package blockbranch.realistic;

import java.util.*;

/** Natural remaining-province partition, distinct supported crowns and seeded order controls. */
public final class OldGrowthTaigaCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void main(String[]args){
        for(int kind:new int[]{1,2}){
            var shapes=new HashSet<String>();double lower=0;int foliage=0;
            for(int i=0;i<64;i++){
                var root=new WoodlandTrees.Root(0,0,0,false,23+i%9,(int)OakBirchWoodland.mix(i*1297L),OldGrowthTaiga.family(kind));
                var tree=TaigaTrees.tree(root);var map=new HashMap<String,Integer>();shapes.add(tree.toString());
                for(var v:tree){require(Math.abs(v.x())<=8&&Math.abs(v.z())<=8&&v.y()<=31,"Bounded mature tree envelope");map.put(key(v.x(),v.y(),v.z()),v.code());}
                for(var v:tree)if(v.code()>=22&&v.code()<=27){
                    foliage++;if(v.y()<root.height()*.4)lower++;boolean support=false;int distance=WoodlandTrees.leafDistance(v.code());
                    for(var q:DIR){int n=map.getOrDefault(key(v.x()+q[0],v.y()+q[1],v.z()+q[2]),0);if(WoodlandTrees.logCode(n)&&distance==1||n>=22&&n<=27&&WoodlandTrees.leafDistance(n)<distance)support=true;}
                    require(support,"Native decreasing leaf distance to connected branch scaffold");
                }
            }
            require(shapes.size()>55,"Varied mature silhouettes rather than copied templates");
            require(kind==1?lower/foliage<.02:lower/foliage>.25,"Distinct raised pine versus low spruce crowns");
            System.out.println("OLD_GROWTH_SHAPES kind="+kind+" unique="+shapes.size()+" lowerFoliage="+lower/foliage);
        }
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);int[] counts=new int[3];int taiga=0,snowy=0,retained=0;
            for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
                int k=OldGrowthTaiga.kind(seed,x,z,d,c);counts[k]++;
                boolean t=Taiga.contains(seed,x,z,d,c),s=SnowyTaiga.contains(seed,x,z,d,c),g=Grove.contains(seed,x,z,d,c);
                require(k==0||!t&&!s&&!g&&BorealWoodland.contains(seed,x,z,d,c),"Completed taiga and grove domains retained");
                if(t)taiga++;if(s)snowy++;if(BorealWoodland.contains(seed,x,z,d,c)&&!t&&!s&&!g&&k==0)retained++;
            }
            require(counts[1]>20&&counts[2]>20&&retained>100&&taiga>300&&snowy>50,"Substantial two-species and retained boreal partitions");
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.OldGrowthTaigaFixtures.fixture(d,seed,kind);int family=OldGrowthTaiga.family(kind),mature=0,young=0,middle=0,minHeight=100,maxHeight=0;
                for(var r:BorealTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c))if(r.family()==family){if(r.height()>=22)mature++;else if(r.height()>=13)middle++;else young++;minHeight=Math.min(minHeight,r.height());maxHeight=Math.max(maxHeight,r.height());}
                require(mature>3&&young>0,"Old forest retains irregular recruitment cohorts");
                if(kind==2)require(middle>6&&young>6&&mature>6&&maxHeight-minHeight>=17,"Spruce has substantial young, intermediate and mature height cohorts");
                var saved=new LinkedHashMap<String,int[]>();for(int z=(p.z()>>4)-1;z<=(p.z()>>4)+1;z++)for(int x=(p.x()>>4)-1;x<=(p.x()>>4)+1;x++){var a=WoodlandTrees.column(seed,x,z,d,c);require(a.length<8192,"Bounded packed old-growth canopy");saved.put(x+","+z,a);}
                var order=new ArrayList<>(saved.keySet());Collections.reverse(order);for(var k:order){var q=k.split(",");require(Arrays.equals(saved.get(k),WoodlandTrees.column(seed,Integer.parseInt(q[0]),Integer.parseInt(q[1]),d,c)),"Cross-chunk negative-coordinate request-order stability");}
                System.out.println("OLD_GROWTH_NATURAL seed="+seed+" kind="+kind+" point="+p+" mature="+mature+" middle="+middle+" young="+young+" heightRange="+minHeight+".."+maxHeight);
            }
            System.out.println("OLD_GROWTH_COUNTS seed="+seed+" pine="+counts[1]+" spruce="+counts[2]+" retainedMixed="+retained);
        }
        System.out.println("OLD_GROWTH_TAIGA_PARTITION_CROWN_SUPPORT_ORDER_PASS");
    }
    private static String key(int x,int y,int z){return x+","+y+","+z;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private OldGrowthTaigaCheck(){}
}
