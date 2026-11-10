package blockbranch.realistic;

import java.util.*;

/** Remaining woodland partitions, species palettes, varied supported crowns and native petal colonies. */
public final class DarkCherryWoodlandCheck {
    public static void main(String[]args){
        for(int kind:new int[]{1,2}){
            var shapes=new HashSet<String>();
            for(int i=0;i<64;i++){
                int height=kind==2?12+i%7:18+i%10;var root=new WoodlandTrees.Root(0,0,0,false,height,(int)OakBirchWoodland.mix(i*1297L),DarkCherryWoodland.family(kind));var tree=WoodlandTrees.tree(root);shapes.add(tree.toString());var cells=new HashMap<String,Integer>();
                for(var v:tree){require(Math.abs(v.x())<=8&&Math.abs(v.z())<=8&&v.y()<32,"Bounded native crown");cells.put(v.x()+","+v.y()+","+v.z(),v.code());require(kind==2?v.code()>=101&&v.code()<=109:(v.code()>=92&&v.code()<=100||DarkOakBranches.branch(v.code())),"Species-specific native wood/leaf palette");}
                for(var v:tree)if(!WoodlandTrees.logCode(v.code())){int distance=WoodlandTrees.leafDistance(v.code());boolean support=false;for(int[]q:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){int n=cells.getOrDefault((v.x()+q[0])+","+(v.y()+q[1])+","+(v.z()+q[2]),0);if(WoodlandTrees.logCode(n)&&distance==1||n>0&&!WoodlandTrees.logCode(n)&&WoodlandTrees.leafDistance(n)<distance)support=true;}require(support&&distance>=1&&distance<=6,"Connected decreasing native leaf distance");}
            }
            require(shapes.size()>55,"Varied connected crown silhouettes");System.out.println("DARK_CHERRY_SHAPES kind="+kind+" unique="+shapes.size());
        }
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);int[] counts=new int[3];int retained=0,physical=0;
            for(int z=-1700000;z<=1700000;z+=16000)for(int x=-1700000;x<=1700000;x+=16000){int k=DarkCherryWoodland.kind(seed,x,z,d,c);counts[k]++;require(k==0||OakBirchWoodland.contains(seed,x,z,d,c)&&!Forest.contains(seed,x,z,d,c)&&!BirchForest.contains(seed,x,z,d,c)&&!Swamp.contains(seed,x,z,d,c)&&TemperateForestVariants.kind(seed,x,z,d,c)==0&&!MontaneMeadow.contains(seed,x,z,d,c)&&!Grove.contains(seed,x,z,d,c),"Only remaining domains; completed forests, meadow and grove retained");if(k==0&&OakBirchWoodland.contains(seed,x,z,d,c)&&TemperateForestVariants.kind(seed,x,z,d,c)==0&&!Swamp.contains(seed,x,z,d,c))retained++;}
            require(counts[1]>20&&counts[2]>20&&retained>100,"Substantial distinct stands and retained woodland");
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.DarkCherryWoodlandFixtures.fixture(d,seed,kind);int young=0,middle=0,mature=0;for(var r:WoodlandTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c))if(r.family()==DarkCherryWoodland.family(kind)){if(r.height()<(kind==2?10:16))young++;else if(r.height()<(kind==2?14:21))middle++;else mature++;}
                require(young>0&&middle>0&&mature>0,"Three actual recruitment cohorts");
                for(int dz=-96;dz<=96;dz+=3)for(int dx=-96;dx<=96;dx+=3)if(DarkCherryWoodland.surfaceKind(seed,p.x()+dx,p.z()+dz,d,c)!=DarkCherryWoodland.kind(seed,p.x()+dx,p.z()+dz,d,c))physical++;
                int cx=p.x()>>4,cz=p.z()>>4;int[] a=WoodlandTrees.column(seed,cx,cz,d,c);require(a.length<8192,"Bounded native packed canopy");WoodlandTrees.column(seed,cx+1,cz,d,c);require(Arrays.equals(a,WoodlandTrees.column(seed,cx,cz,d,c)),"Cross-chunk request order stability");
                System.out.println("DARK_CHERRY_NATURAL seed="+seed+" kind="+kind+" point="+p+" cohorts="+young+","+middle+","+mature);
            }
            require(physical>0,"Physical material selections do not reuse quart boundaries");
            var site=new GroundEcology.Site(.65,.2,.1,0,0,false);var states=new HashSet<Integer>();int petals=0;int[] tiles=new int[1024];
            for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
                int cover=DarkCherryWoodland.cover(seed,x,z,site,true,2);require(cover==DarkCherryWoodland.cover(seed,x,z,site,true,2),"Stable absolute-coordinate petal floor");require(DarkCherryWoodland.cover(seed,x,z,site,false,2)==3,"No floor plants under occupied native stems");if(cover>=37){petals++;states.add(cover);tiles[((z+128)>>3)*32+((x+128)>>3)]++;}
            }
            int empty=0;for(int n:tiles)if(n==0)empty++;require(petals>1000&&petals<10000&&empty>100&&states.size()==16,"Irregular petal colonies, bare gaps, four amounts and four rotations");
            System.out.println("DARK_CHERRY_COUNTS seed="+seed+" dark="+counts[1]+" cherry="+counts[2]+" retained="+retained+" physical="+physical+" petals="+petals+" empty8="+empty);
        }
        System.out.println("DARK_CHERRY_PARTITION_SPECIES_COHORT_PETAL_SUPPORT_ORDER_PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private DarkCherryWoodlandCheck(){}
}
