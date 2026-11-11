package blockbranch.realistic;

import java.util.*;
import blockbranch.cubic.CanopyLayer;

/** Static fine-sediment trace analog, not animals, bioturbation dynamics, food
 * supply or population estimates. IUCN GET M3.3 p.150 describes habitat-forming
 * tracks/burrows/mounds. Swanborn et al.2026 DOI:10.1002/lno.70420 documents
 * traces with presence-only observations, not universal density calibration.
 * Depth 3–6 km bounds this first analog; it does not classify every site as a
 * geomorphic abyssal plain. Existing surface-climate aliases stay unchanged. */
public final class AbyssalTraces {
    public static final int FIRST=173,LAST=184;
    public record Point(int x,int z,int form,int rotation){}
    /** One jittered candidate per 12 m cell, accepted with probability .35.
     * Shape, spacing and abundance are generator conventions; no extra noise
     * is interpreted as carbon supply or ecological productivity. */
    public static Point candidate(long seed,int cellX,int cellZ){
        long h=OakBirchWoodland.mix(seed^0x4142595353545243L^cellX*0x165667B19E3779F9L^cellZ*0xD1B54A32D192ED03L);
        if((h>>>11)*0x1.0p-53>=.35)return null;
        return new Point(cellX*12+3+(int)((h>>>4)&7)%6,cellZ*12+3+(int)((h>>>8)&7)%6,(int)Long.remainderUnsigned(h>>>24,3),(int)((h>>>30)&3));
    }
    static int bed(long seed,int x,int z,DrainageNetwork d,ClimateField c,boolean icebergs){return FrozenSeabed.replacement(DeepOcean.cover(seed,x,z,true,DeepOcean.site(d,x,z)),FrozenSeabed.frozenCover(seed,x,z,d,c,icebergs));}
    public static boolean eligible(int depth,double slope,int bed,boolean marine,boolean supported){return marine&&depth>=3000&&depth<=6000&&slope<=.08&&(bed==5||bed==6)&&supported;}
    static boolean eligible(long seed,int x,int z,DrainageNetwork d,ClimateField c,boolean icebergs){
        var s=d.sample(x,z);int depth=s.waterLevel()==DrainageNetwork.DRY?0:s.waterLevel()-s.height()-1;
        if(depth<3000||depth>6000||!d.habitatOceanWater(x,z,s)||d.terminalLakeWater(x,z,s)||d.channelAt(x,z).edge()>=0)return false;
        double slope=SlopeCorrection.slope(d,x,z);int bed=bed(seed,x,z,d,c,icebergs);if(!eligible(depth,slope,bed,true,true))return false;
        // Every model lies inside this one column (2..14/16 horizontally).
        // The selected mud/clay block supports its entire footprint. Requiring
        // equal heights in neighbouring columns would reject valid individual
        // terraces in the existing relief without improving physical support.
        return true;
    }
    static int[] column(long seed,int cx,int cz,DrainageNetwork d,ClimateField c,boolean icebergs){
        var out=new TreeMap<Integer,Integer>();int minX=cx*16,minZ=cz*16;
        for(int gz=Math.floorDiv(minZ,12);gz<=Math.floorDiv(minZ+15,12);gz++)for(int gx=Math.floorDiv(minX,12);gx<=Math.floorDiv(minX+15,12);gx++){
            var p=candidate(seed,gx,gz);if(p==null||p.x()<minX||p.x()>minX+15||p.z()<minZ||p.z()>minZ+15||!eligible(seed,p.x(),p.z(),d,c,icebergs))continue;
            int y=d.sample(p.x(),p.z()).height()+1,key=((y-TerrainShape.FLOOR)<<8)|((p.z()&15)*16+(p.x()&15));out.put(key,FIRST+p.form()*4+p.rotation());
        }
        return out.entrySet().stream().mapToInt(e->e.getKey()<<CanopyLayer.STATE_BITS|e.getValue()).toArray();
    }
    private AbyssalTraces(){}
}
