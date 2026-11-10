package blockbranch.realistic;

import java.util.*;

/** Tropical surface-climate shelf with native shallow-water coral colonies; no ocean circulation model. */
public final class WarmOcean {
    public static boolean climate(ClimateField.Sample q){return q.meanTemperatureC()>=24;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceContains(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return MarineShelf.surfaceContains(x,z,d)&&climate(c.sample(x,z,d::sample))&&!MangroveSwamp.exact(seed,x,z,d,c);}
    public static boolean aquatic(int code){return code>=150&&code<=168;}
    static double patch(long seed,int x,int z){return GroundEcology.colonyStrength(seed,0x5741524D52454546L,x,z,40,7,20,new GroundEcology.Site(.75,0,0,0,0,false));}
    static int coralHeight(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(!surfaceContains(seed,x,z,d,c))return 0;var s=MarineShelf.site(d,x,z);if(s.depth()<4||s.depth()>14||s.slope()>.18)return 0;
        double p=patch(seed,x,z),edge=SurfacePatches.sample(seed^0x434F52414C454447L,x,z,6);if(p<.2||edge<.35+.35*(1-p))return 0;
        return p>.65&&FlowerMicrosites.accepts(seed^0x434F52414C544F50L,x,z)?2:1;
    }
    static int[] column(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){
        var out=new TreeMap<Integer,Integer>();
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int wx=cx*16+x,wz=cz*16+z;if(!surfaceContains(seed,wx,wz,d,c))continue;var s=MarineShelf.site(d,wx,wz);
            int h=coralHeight(seed,wx,wz,d,c);long bits=OakBirchWoodland.mix(seed^wx*0x165667B19E3779F9L^wz*0xD1B54A32D192ED03L);
            int species=(int)Math.clamp(SurfacePatches.sample(seed^0x434F52414C545950L,wx,wz,24)*5,0,4);
            for(int dy=1;dy<=h;dy++)out.put(((s.height()+dy-TerrainShape.FLOOR)<<8)|(z*16+x),dy<h?164+species:150+species);
            if(h>0&&(bits&3)!=0)out.put(((s.height()+h+1-TerrainShape.FLOOR)<<8)|(z*16+x),155+species);
            else if(h==0&&s.depth()>=3&&s.depth()<=10&&s.slope()<=.12&&patch(seed^0x5049434B4C45L,wx,wz)>.45&&(bits&31)==0)
                out.put(((s.height()+1-TerrainShape.FLOOR)<<8)|(z*16+x),160+(int)Long.remainderUnsigned(bits>>>16,4));
        }
        return out.entrySet().stream().mapToInt(e->(e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS)|e.getValue()).toArray();
    }
    public record Point(int x,int z){}

    private WarmOcean(){}
}
