package blockbranch.realistic;

import java.util.*;

/** Keep a full lower leader; taper crown wood without moving any canopy cell. */
public final class DarkOakBranches {
    public static final int FIRST=110,LAST=123;
    private record Cell(int x,int y,int z){Cell offset(int[] d){return new Cell(x+d[0],y+d[1],z+d[2]);}}
    private static final int[][] DIRECTIONS={{0,1,0},{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,-1,0}};
    public static boolean branch(int code){return code>=FIRST&&code<=LAST;}
    public static int axes(int code){return (code-FIRST)%7+1;}
    public static boolean thick(int code){return code>=FIRST+7;}
    public static List<WoodlandTrees.Voxel> taper(WoodlandTrees.Root root,List<WoodlandTrees.Voxel> tree){
        var wood=new HashSet<Cell>();
        for(var v:tree)if(v.code()>=92&&v.code()<=94)wood.add(new Cell(v.x(),v.y(),v.z()));
        var start=new Cell(root.x(),root.y(),root.z());
        if(!wood.contains(start))throw new IllegalArgumentException("Dark oak needs a grounded leader");
        var parent=new HashMap<Cell,Cell>();var queue=new ArrayDeque<Cell>();parent.put(start,null);queue.add(start);
        while(!queue.isEmpty()){
            var cell=queue.removeFirst();
            for(var d:DIRECTIONS){var next=cell.offset(d);if(wood.contains(next)&&!parent.containsKey(next)){parent.put(next,cell);queue.addLast(next);}}
        }
        if(parent.size()!=wood.size())throw new IllegalArgumentException("Disconnected dark-oak scaffold");
        var tip=wood.stream().max(Comparator.comparingInt(Cell::y)
            .thenComparingInt(c->-Math.abs(c.x-root.x())-Math.abs(c.z-root.z()))
            .thenComparingInt(Cell::x).thenComparingInt(Cell::z)).orElseThrow();
        var leader=new HashSet<Cell>();for(var cell=tip;cell!=null;cell=parent.get(cell))leader.add(cell);
        var distance=new HashMap<Cell,Integer>();
        for(var cell:leader){distance.put(cell,0);queue.addLast(cell);}
        while(!queue.isEmpty()){
            var cell=queue.removeFirst();
            for(var d:DIRECTIONS){var next=cell.offset(d);if(wood.contains(next)&&!distance.containsKey(next)){distance.put(next,distance.get(cell)+1);queue.addLast(next);}}
        }
        var result=new ArrayList<WoodlandTrees.Voxel>(tree.size());
        for(var v:tree){
            var cell=new Cell(v.x(),v.y(),v.z());int code=v.code();
            int relativeY=v.y()-root.y(),trunkTop=Math.max(3,(int)(root.height()*.45));
            if(wood.contains(cell)&&!(leader.contains(cell)&&relativeY<=trunkTop)){
                int axes=0;
                for(var d:DIRECTIONS)if(wood.contains(cell.offset(d)))axes|=d[1]!=0?1:d[0]!=0?2:4;
                boolean thick=(leader.contains(cell)||distance.get(cell)==1)&&relativeY<=root.height()*.72;
                code=FIRST+(thick?7:0)+axes-1;
            }
            result.add(new WoodlandTrees.Voxel(v.x(),v.y(),v.z(),code));
        }
        return List.copyOf(result);
    }
    private DarkOakBranches(){}
}
