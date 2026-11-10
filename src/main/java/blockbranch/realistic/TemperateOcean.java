package blockbranch.realistic;

import java.util.*;

/** Temperate surface-climate class over the existing marine shelf; no seawater-temperature simulation. */
public final class TemperateOcean {
    public static final int KELP_BODY=89,KELP_HEAD=90;
    public static boolean climate(ClimateField.Sample c){return c.meanTemperatureC()>=8&&c.meanTemperatureC()<18;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,d,c);
    }
    public static boolean deepCandidate(int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;return DeepOcean.contains(x,z,d)&&climate(c.sample(x,z,d::sample));}
    static double substrate(long seed,int x,int z){return SurfacePatches.sample(seed^0x4F4345414E524F43L,x,z,96);}
    // Preserve the established kelp colonies while correcting the independent bare-bed paint field.
    private static double kelpSubstrate(long seed,int x,int z){return OakBirchWoodland.stand(seed^0x4F4345414E524F43L,x*4,z*4);}
    public static int cover(long seed,int x,int z,DrainageNetwork d,ClimateField c){var s=MarineShelf.site(d,x,z);if(!MarineShelf.eligible(s))return 0;
        double patch=substrate(seed,x,z);if(s.depth()<=16&&s.slope()<=.25){if(patch>.62||kelpHeight(seed,x,z,d,c)>0)return 9;if(patch>.52)return 8;}
        return MarineShelf.cover(seed,x,z,true,s,c.sample(x,z,d::sample));
    }
    public static int kelpHeight(long seed,int x,int z,DrainageNetwork d,ClimateField c){if(!contains(seed,x,z,d,c))return 0;var s=MarineShelf.site(d,x,z);
        if(!MarineShelf.eligible(s)||s.depth()<4||s.depth()>10||s.slope()>.18||kelpSubstrate(seed,x,z)<=.62)return 0;
        if(!colony(seed,x,z))return 0;long v=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        return 2+(int)Long.remainderUnsigned(OakBirchWoodland.mix(v^0x484549474854L),Math.min(6,s.depth()-1)-1);
    }
    static boolean colony(long seed,int x,int z){double strength=GroundEcology.colonyStrength(seed,0x4F4345414E4B454CL,x,z,32,7,15,new GroundEcology.Site(.75,0,0,0,0,false));return(OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53<.35*strength;}
    public static int[] column(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){boolean any=false;for(int z=0;z<4;z++)for(int x=0;x<4;x++)any|=contains(seed,cx*16+x*4,cz*16+z*4,d,c);if(!any)return new int[0];var blocks=new TreeMap<Integer,Integer>();
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z,h=kelpHeight(seed,wx,wz,d,c);if(h==0)continue;int ground=d.sample(wx,wz).height();for(int n=1;n<=h;n++){int key=((ground+n-TerrainShape.FLOOR)<<8)|(z*16+x);blocks.put(key,n==h?KELP_HEAD:KELP_BODY);}}
        return blocks.entrySet().stream().mapToInt(e->(e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS)|e.getValue()).toArray();
    }
    public record Point(int x,int z){}

    /** Block-resolution material eligibility; the original selector retains biome routing. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return MarineShelf.surfaceContains(x,z,d)&&climate(c.sample(x,z,d::sample))&&!MangroveSwamp.exact(seed,x,z,d,c);}
    private TemperateOcean(){}
}
