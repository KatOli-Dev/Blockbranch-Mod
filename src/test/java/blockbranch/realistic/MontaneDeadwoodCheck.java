package blockbranch.realistic;

import java.util.*;

/** Isolate whole-log clipping with a synthetic one-cell owner over a real old
 * hardwood log. This tests the boundary algorithm, not ecological admission. */
public final class MontaneDeadwoodCheck {
    private record P(int x,int y,int z){}
    public static void main(String[] args){
        long seed=1;var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);
        Map<P,Integer> old=null;P selected=null;
        search:for(int[] site:new int[][]{{16000,-240000},{48000,-304000},{-1744000,-1040000}})
            for(int dz=-4;dz<=4;dz++)for(int dx=-4;dx<=4;dx++){
                int cx=(site[0]>>4)+dx,cz=(site[1]>>4)+dz;
                for(int packed:WoodlandDetails.logs(seed,cx,cz,d,c,63)){
                    int code=packed&255;if(code<1||code>6)continue;
                    int key=packed>>>8;selected=new P(cx*16+(key&15),(key>>>8)+TerrainShape.FLOOR,cz*16+((key>>>4)&15));old=new HashMap<>();
                    for(int oz=-1;oz<=1;oz++)for(int ox=-1;ox<=1;ox++)read(old,WoodlandDetails.logs(seed,cx+ox,cz+oz,d,c,63),cx+ox,cz+oz);
                    break search;
                }
            }
        require(selected!=null,"Actual old hardwood-log witness in fixed3x9x9chunk search");
        var component=component(old,selected);
        require(component.size()>=3,"Full nontrivial old log captured with neighbor halo");
        var legacy=new HashMap<P,Integer>();int legacyX=selected.x>>4,legacyZ=selected.z>>4;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)read(legacy,WoodlandDetails.logs(seed,legacyX+dx,legacyZ+dz,d,c,63,(x,z)->true),legacyX+dx,legacyZ+dz);
        require(legacy.equals(old),"Old saved bits ignore a newer supplied owner");
        var hit=selected;var changed=new HashMap<P,Integer>();int cx=selected.x>>4,cz=selected.z>>4;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)read(changed,WoodlandDetails.logs(seed,cx+dx,cz+dz,d,c,127,(x,z)->x==hit.x&&z==hit.z),cx+dx,cz+dz);
        for(var p:component)require(!changed.containsKey(p),"Complete old log rejected before chunk clipping; no retained stump");
        require(old.entrySet().stream().filter(e->!component.contains(e.getKey())).allMatch(e->Objects.equals(changed.get(e.getKey()),e.getValue())),"Other old deadwood retained");
        System.out.println("MONTANE_DEADWOOD_WHOLE_LOG_PASS witness="+selected+" fullLogCells="+component.size());
    }
    private static Set<P> component(Map<P,Integer> cells,P selected){
        var result=new HashSet<P>();var queue=new ArrayDeque<P>();queue.add(selected);
        while(!queue.isEmpty()){var p=queue.removeFirst();int code=cells.getOrDefault(p,0);if(code<1||code>6||!result.add(p))continue;queue.add(new P(p.x+1,p.y,p.z));queue.add(new P(p.x-1,p.y,p.z));queue.add(new P(p.x,p.y,p.z+1));queue.add(new P(p.x,p.y,p.z-1));}
        return result;
    }
    private static void read(Map<P,Integer> out,int[] values,int cx,int cz){for(int packed:values){int key=packed>>>8;out.put(new P(cx*16+(key&15),(key>>>8)+TerrainShape.FLOOR,cz*16+((key>>>4)&15)),packed&255);}}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private MontaneDeadwoodCheck(){}
}
