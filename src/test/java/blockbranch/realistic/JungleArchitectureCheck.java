package blockbranch.realistic;
import java.util.*;

/** Geometry-level guards against the former repeated four-arm / two-tier motif. */
public final class JungleArchitectureCheck {
    public static void main(String[]args){
        var silhouettes=new HashSet<Long>();int leaning=0,diagonal=0,trees=0;var heights=new TreeSet<Integer>();
        for(int h:new int[]{8,12,18,21,25,28,33,36})for(int n=0;n<8;n++){
            int variant=(int)OakBirchWoodland.mix(h*911L+n*1237L);var root=new WoodlandTrees.Root(0,0,0,false,h,variant);var voxels=JungleTrees.tree(root);var all=new HashMap<Integer,Integer>();var logs=new HashSet<Integer>();long footprint=0;int top=0,lowestLeaf=JungleTrees.MAX_HEIGHT;
            for(var v:voxels){require(Math.abs(v.x())<=8&&Math.abs(v.z())<=8&&v.y()>=0&&v.y()<JungleTrees.MAX_HEIGHT,"Bounded tree geometry");int key=index(v.x(),v.y(),v.z());all.put(key,v.code());top=Math.max(top,v.y());if(WoodlandTrees.logCode(v.code())){logs.add(key);if(v.y()==h/2&&(v.x()!=0||v.z()!=0)&&h>=23)leaning++;if(v.y()>h/2&&Math.abs(v.x())>=2&&Math.abs(v.z())>=2)diagonal++;}else{lowestLeaf=Math.min(lowestLeaf,v.y());footprint|=1L<<(((v.z()+8)/3)*6+(v.x()+8)/3);}}
            var reached=new HashSet<Integer>();var queue=new ArrayDeque<Integer>();queue.add(index(0,0,0));reached.add(index(0,0,0));
            while(!queue.isEmpty()){int key=queue.removeFirst(),x=key%17-8,z=key/17%17-8,y=key/289;for(var d:DIR){int nx=x+d[0],ny=y+d[1],nz=z+d[2];if(Math.abs(nx)>8||Math.abs(nz)>8||ny<0||ny>=JungleTrees.MAX_HEIGHT)continue;int next=index(nx,ny,nz);if(logs.contains(next)&&reached.add(next))queue.add(next);}}
            require(reached.size()==logs.size(),"Every branch and stem connects to its real root");
            for(var e:all.entrySet()){if(WoodlandTrees.logCode(e.getValue()))continue;int key=e.getKey(),x=key%17-8,z=key/17%17-8,y=key/289,distance=WoodlandTrees.leafDistance(e.getValue());boolean support=false;for(var d:DIR){int nx=x+d[0],ny=y+d[1],nz=z+d[2];if(Math.abs(nx)>8||Math.abs(nz)>8||ny<0||ny>=JungleTrees.MAX_HEIGHT)continue;var near=all.get(index(nx,ny,nz));if(near!=null&&(WoodlandTrees.logCode(near)?distance==1:WoodlandTrees.leafDistance(near)<distance))support=true;}require(support,"Every varied crown retains a real leaf support path");}
            if(h<23)require(lowestLeaf<=Math.max(3,h/2),"Juvenile/subcanopy foliage occupies real lower branches, not a ball on a bare pole: height="+h+" variant="+n+" lowestLeaf="+lowestLeaf);
            for(int y=0;y<h/3;y++){int nearest=Integer.MAX_VALUE,axis=0;for(var v:voxels)if(v.y()==y&&WoodlandTrees.logCode(v.code())&&Math.abs(v.x())+Math.abs(v.z())<nearest){nearest=Math.abs(v.x())+Math.abs(v.z());axis=v.code();}require(axis==37,"Steep leader retains continuous bark without a ladder of lateral cut ends");}
            require(voxels.size()>25&&top>=h-5,"Nondegenerate cohort architecture");silhouettes.add(footprint);heights.add(top);trees++;
        }
        System.out.printf("JUNGLE_ARCHITECTURE_DIAGNOSTIC silhouettes=%d topHeights=%d upperLean=%d diagonal=%d%n",silhouettes.size(),heights.size(),leaning,diagonal);
        require(silhouettes.size()>=20&&heights.size()>=12&&leaning>10&&diagonal>100,"Several silhouettes, tilted stems and non-cardinal asymmetric branches");
        System.out.printf("JUNGLE_ARCHITECTURE_PASS trees=%d coarseSilhouettes=%d topHeights=%d leaningWitnesses=%d diagonalBranchLogs=%d%n",trees,silhouettes.size(),heights.size(),leaning,diagonal);
    }
    private static int index(int x,int y,int z){return y*289+(z+8)*17+x+8;}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
}
