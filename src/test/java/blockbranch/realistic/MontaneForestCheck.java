package blockbranch.realistic;

import java.util.*;

/** Saved-wrapper rejection and actual geometry support; no fitted sample counts. */
public final class MontaneForestCheck {
    private record P(int x,int y,int z){}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void main(String[] args){
        for(int old=TerrainShape.SHAPE_VERSION;old<=270;old++){
            require(MontaneForestSchema.schema(old,0)==old&&MontaneForestSchema.baseSchema(old,0)==old,"Absent retains original version");
            if(old!=270)require(!MontaneForestSchema.matches(old,1),"No incomplete foundation promotion");
        }
        require(MontaneForestSchema.schema(270,1)==271&&MontaneForestSchema.baseSchema(271,1)==270,"Complete saved wrapper");
        require(!MontaneForestSchema.matches(271,0)&&!MontaneForestSchema.matches(272,1)&&!MontaneForestSchema.matches(271,2),"Malformed/future gates reject");
        require(MontaneForest.climateSuitable(1048576,800,8,1000),"Source-context lower bounds");
        require(!MontaneForest.climateSuitable(1048576,799,8,1500),"Below mountain convention excluded");
        require(!MontaneForest.climateSuitable(1048576,1000,8,999)&&!MontaneForest.climateSuitable(1048576,1000,8,2501),"Bounded first rain context");
        require(!MontaneForest.climateSuitable(0,1000,8,1500),"Nonfreezing winter excluded");
        require(!MontaneForest.climateSuitable(1048576,1000,-6,1500),"Too little warm season excluded");
        int shapes=0,leaves=0;
        for(int height=10;height<=31;height++)for(int variant=0;variant<12;variant++){
            var root=new WoodlandTrees.Root(0,0,0,false,height,variant,MontaneForest.FAMILY);
            var voxels=TaigaTrees.tree(root);var cells=new HashMap<P,Integer>();var wood=new HashSet<P>();
            for(var v:voxels){
                require(Math.abs(v.x())<=8&&Math.abs(v.z())<=8&&v.y()>=0&&v.y()<=height+1,"Bounded clear/canopy halo");
                var p=new P(v.x(),v.y(),v.z());require(cells.put(p,v.code())==null,"Unique geometry cells");
                if(WoodlandTrees.logCode(v.code()))wood.add(p);
            }
            var connected=new HashSet<P>();var queue=new ArrayDeque<P>();queue.add(new P(0,0,0));
            while(!queue.isEmpty()){var p=queue.removeFirst();if(!wood.contains(p)||!connected.add(p))continue;for(var d:DIR)queue.add(new P(p.x+d[0],p.y+d[1],p.z+d[2]));}
            require(connected.equals(wood),"All wood connected to ground stem");
            var distance=new HashMap<P,Integer>();queue.addAll(wood);for(var p:wood)distance.put(p,0);
            while(!queue.isEmpty()){var p=queue.removeFirst();int n=distance.get(p);if(n>=6)continue;for(var d:DIR){var q=new P(p.x+d[0],p.y+d[1],p.z+d[2]);if(cells.containsKey(q)&&!distance.containsKey(q)){distance.put(q,n+1);queue.add(q);}}}
            for(var e:cells.entrySet())if(!wood.contains(e.getKey())){require(distance.getOrDefault(e.getKey(),7)==WoodlandTrees.leafDistance(e.getValue()),"Every actual leaf has exact valid six-neighbor support");leaves++;}
            shapes++;
        }
        System.out.println("MONTANE_SCHEMA_GEOMETRY_PASS shapes="+shapes+" leaves="+leaves);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private MontaneForestCheck(){}
}
