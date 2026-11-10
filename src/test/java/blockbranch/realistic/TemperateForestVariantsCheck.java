package blockbranch.realistic;

import java.util.*;

/** Remaining-domain partition, natural cohorts, supported shapes and patch/spacing controls. */
public final class TemperateForestVariantsCheck {
    public static void main(String[]args){
        var shapes=new HashSet<String>();
        for(int i=0;i<64;i++){
            var tree=WoodlandTrees.tree(new WoodlandTrees.Root(0,0,0,true,24+i%7,(int)OakBirchWoodland.mix(i*1297L),TemperateForestVariants.BIRCH_FAMILY));
            shapes.add(tree.toString());var cells=new HashMap<String,Integer>();
            for(var v:tree){require(Math.abs(v.x())<=8&&Math.abs(v.z())<=8&&v.y()<32,"Bounded mature birch envelope");cells.put(v.x()+","+v.y()+","+v.z(),v.code());}
            for(var v:tree)if(v.code()>=7&&v.code()<=18){boolean supported=false;int distance=WoodlandTrees.leafDistance(v.code());for(int[]q:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){int n=cells.getOrDefault((v.x()+q[0])+","+(v.y()+q[1])+","+(v.z()+q[2]),0);if(n>=1&&n<=6&&distance==1||n>=7&&n<=18&&WoodlandTrees.leafDistance(n)<distance)supported=true;}require(supported,"Decreasing native leaf distance to connected birch scaffold");}
        }
        require(shapes.size()>55,"Varied old birch silhouettes");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);int[] counts=new int[3];int retained=0,physicalWitnesses=0;
            for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
                int kind=TemperateForestVariants.kind(seed,x,z,d,c);counts[kind]++;
                require(kind==0||OakBirchWoodland.contains(seed,x,z,d,c)&&!Forest.contains(seed,x,z,d,c)&&!BirchForest.contains(seed,x,z,d,c)&&!Swamp.contains(seed,x,z,d,c),"Only remaining woodland; completed forest/wetland domains preserved");
                if(kind==0&&OakBirchWoodland.contains(seed,x,z,d,c)&&!Swamp.contains(seed,x,z,d,c))retained++;
            }
            require(counts[1]>100&&counts[2]>50&&retained>200,"Substantial natural partitions with retained woodland");
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.TemperateForestVariantsFixtures.fixture(d,seed,kind);int young=0,middle=0,mature=0,birch=0,all=0;
                for(var r:WoodlandTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c))if(kind==2?r.family()==9:r.family()==1){all++;if(r.birch())birch++;if(r.height()<14)young++;else if(r.height()<24)middle++;else mature++;}
                require(young>0&&middle>0&&mature>0,"Varied recruitment cohorts");if(kind==2)require(birch>all*.85,"Old-growth birch dominance");
                for(int dz=-96;dz<=96;dz+=3)for(int dx=-96;dx<=96;dx+=3)if(TemperateForestVariants.surfaceKind(seed,p.x()+dx,p.z()+dz,d,c)!=TemperateForestVariants.kind(seed,p.x()+dx,p.z()+dz,d,c))physicalWitnesses++;
                int cx=p.x()>>4,cz=p.z()>>4;int[] first=WoodlandTrees.column(seed,cx,cz,d,c);WoodlandTrees.column(seed,cx+1,cz,d,c);require(Arrays.equals(first,WoodlandTrees.column(seed,cx,cz,d,c)),"Packed canopy request-order stability");
                System.out.println("TEMPERATE_VARIANT_NATURAL seed="+seed+" kind="+kind+" point="+p+" cohorts="+young+","+middle+","+mature+" birch="+birch+"/"+all);
            }
            require(physicalWitnesses>0,"Physical material eligibility is independent of the quart biome mask");
            var site=new GroundEcology.Site(.65,.2,.1,0,0,false);var palette=new HashSet<Integer>();int flowers=0,empty=0;int[] tiles=new int[1024];
            for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
                int code=TemperateForestVariants.flowerCover(seed,x,z,site,true);palette.add(code);
                require(code==TemperateForestVariants.flowerCover(seed,x,z,site,true),"Stable absolute-coordinate floor");
                require(TemperateForestVariants.flowerCover(seed,x,z,site,false)==3,"No plants without stem clearance");
                if(code>=30){flowers++;tiles[((z+128)>>3)*32+((x+128)>>3)]++;require(FlowerMicrosites.accepts(seed^0x464C4F5745525350L,x,z),"Existing rendered-stem variable spacing retained");}
            }
            for(int n:tiles)if(n==0)empty++;
            require(flowers>65536*.025&&flowers<65536*.18&&empty>128,"Flower colonies retain meaningful gaps rather than uniform confetti");
            require(palette.stream().filter(n->n>=30).count()>=5,"Several natural native flower species");
            System.out.println("TEMPERATE_VARIANT_COUNTS seed="+seed+" flower="+counts[1]+" oldBirch="+counts[2]+" retained="+retained+" physicalBoundaryWitnesses="+physicalWitnesses+" flowers="+flowers+" empty8="+empty+" palette="+palette);
        }
        System.out.println("TEMPERATE_VARIANTS_PARTITION_COHORT_SUPPORT_FLOWER_SPACING_PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private TemperateForestVariantsCheck(){}
}
