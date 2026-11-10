package blockbranch.realistic;

import java.util.*;

/** A connected full leader with smaller supported limbs, unchanged foliage and other species. */
public final class DarkOakBranchCheck {
    private record Cell(int x,int y,int z){Cell offset(int[]d){return new Cell(x+d[0],y+d[1],z+d[2]);}}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void main(String[]args){
        int fine=0,thick=0;var silhouettes=new HashSet<String>();
        for(int i=0;i<96;i++){
            var root=new WoodlandTrees.Root(-33,71,17,false,7+i%21,(int)OakBirchWoodland.mix(i*1297L),10);
            var before=ForestTrees.tree(root);var after=DarkCherryWoodland.tree(root);var original=new HashMap<Cell,Integer>();var cells=new HashMap<Cell,Integer>();
            for(var v:before)original.put(new Cell(v.x(),v.y(),v.z()),v.code());
            for(var v:after)cells.put(new Cell(v.x(),v.y(),v.z()),v.code());
            require(original.keySet().equals(cells.keySet()),"Taper moves no wood or foliage cell");
            var trunk=new HashSet<Cell>();
            for(var entry:cells.entrySet()){
                var c=entry.getKey();int code=entry.getValue(),old=original.get(c);
                if(old>3){require(code==94+WoodlandTrees.leafDistance(old),"Leaf positions/support distances unchanged");continue;}
                if(!DarkOakBranches.branch(code)){require(code==92+old-1,"Preserved native trunk axes");trunk.add(c);continue;}
                if(DarkOakBranches.thick(code))thick++;else fine++;
                require(WoodlandTrees.logCode(code),"Branch is structural wood for native leaf support");
                int mask=0;boolean touchesTrunk=false;
                for(var d:DIR){Integer neighbor=original.get(c.offset(d));if(neighbor!=null&&neighbor<=3){mask|=d[1]!=0?1:d[0]!=0?2:4;int next=cells.get(c.offset(d));touchesTrunk|=next>=92&&next<=94;}}
                require(mask==DarkOakBranches.axes(code),"Rendered branch axes join every original wood neighbor");
                require(!touchesTrunk||DarkOakBranches.thick(code),"First transition from the full trunk uses the wider branch section");
            }
            var start=new Cell(root.x(),root.y(),root.z());require(trunk.contains(start),"Grounded full trunk");
            var connected=new HashSet<Cell>();var queue=new ArrayDeque<Cell>();queue.add(start);connected.add(start);
            while(!queue.isEmpty()){var c=queue.removeFirst();for(var d:DIR){var n=c.offset(d);if(trunk.contains(n)&&connected.add(n))queue.addLast(n);}}
            require(connected.equals(trunk),"Full trunk remains connected to its highest leader");
            int maximum=original.entrySet().stream().filter(e->e.getValue()<=3).mapToInt(e->e.getKey().y).max().orElseThrow();
            int trunkTop=trunk.stream().mapToInt(Cell::y).max().orElseThrow();
            require(trunkTop>=root.y()+Math.max(3,root.height()/3)&&trunkTop<maximum,"Sound lower trunk reaches the crown entrance and tapers below the woody top");
            silhouettes.add(after.toString());
            var cherry=new WoodlandTrees.Root(root.x(),root.y(),root.z(),false,root.height(),root.variant(),11);
            var expected=ForestTrees.tree(cherry).stream().map(v->new WoodlandTrees.Voxel(v.x(),v.y(),v.z(),v.code()<=3?101+v.code()-1:103+WoodlandTrees.leafDistance(v.code()))).toList();
            require(DarkCherryWoodland.tree(cherry).equals(expected),"Cherry remains byte-for-byte native scaffold geometry");
        }
        require(fine>500&&thick>300&&silhouettes.size()==96,"Both taper sections and all varied trees retained");
        System.out.println("DARK_OAK_TAPER_LEADER_SUPPORT_CHERRY_CONTROL_PASS fine="+fine+" thick="+thick+" shapes="+silhouettes.size());
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private DarkOakBranchCheck(){}
}
