package blockbranch.realistic;

import java.util.*;
import java.util.function.BiPredicate;

/** Definition 1: cardinal connected macro-habitat components, never chunk identities. */
public final class BiomeRegionPlacement {
    public static final int GRID=2048, MAX_CELLS=1024;
    public record Cell(int x,int z) {
        public int blockX(){return x*GRID+GRID/2;}
        public int blockZ(){return z*GRID+GRID/2;}
    }
    public record Region(Cell owner,int cells,boolean overflow) {}
    private final long seed;
    private final BiPredicate<Integer,Integer> habitat;
    private final LinkedHashMap<Cell,Region> cache=new LinkedHashMap<>(128,.75f,true);
    public BiomeRegionPlacement(long seed,BiPredicate<Integer,Integer> habitat){this.seed=seed;this.habitat=habitat;}
    private boolean eligible(Cell c){return Math.abs((long)c.blockX())<2096000&&Math.abs((long)c.blockZ())<2096000&&habitat.test(c.blockX(),c.blockZ());}
    public synchronized Region region(Cell start){
        var cached=cache.get(start);if(cached!=null)return cached;
        if(!eligible(start))return new Region(null,0,false);
        var visited=new HashSet<Cell>();var queue=new ArrayDeque<Cell>();visited.add(start);queue.add(start);Cell owner=start;
        while(!queue.isEmpty()){
            var c=queue.remove();if(compare(c,owner)<0)owner=c;
            for(var n:List.of(new Cell(c.x+1,c.z),new Cell(c.x-1,c.z),new Cell(c.x,c.z+1),new Cell(c.x,c.z-1))){
                if(visited.contains(n)||!eligible(n))continue;
                if(visited.size()==MAX_CELLS)return new Region(null,MAX_CELLS,true);
                visited.add(n);queue.add(n);
            }
        }
        var result=new Region(owner,visited.size(),false);
        for(var c:visited){cache.put(c,result);while(cache.size()>4096)cache.remove(cache.keySet().iterator().next());}
        return result;
    }
    private int compare(Cell a,Cell b){int order=Long.compareUnsigned(hash(a),hash(b));return order!=0?order:a.x!=b.x?Integer.compare(a.x,b.x):Integer.compare(a.z,b.z);}
    public long hash(Cell c){return OakBirchWoodland.mix(seed^0x57454c4c524547L^c.x*0x9e3779b97f4a7c15L^c.z*0xc2b2ae3d27d4eb4fL);}
}
