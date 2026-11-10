package blockbranch.realistic;

import java.util.*;

/** Native culms in irregular colonies and gaps, clipped against actual existing canopy cells. */
public final class BambooGroves {
    public static final int FIRST=124,LAST=127;
    public static boolean bamboo(int code){return code>=FIRST&&code<=LAST;}
    public static boolean accepts(long seed,int x,int z,GroundEcology.Site site){
        double colony=GroundEcology.colonyStrength(seed,0x42414D424F4F434CL,x,z,48,12,29,site);
        double inner=GroundEcology.colonyStrength(seed,0x42414D424F4F5455L,x,z,16,4,10,site);
        double roll=(OakBirchWoodland.mix(seed^0x42414D424F4F5354L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
        return colony>.18&&roll<Math.min(.95,.12+1.1*colony)*(.55+.45*inner)&&FlowerMicrosites.accepts(seed^0x42414D424F4F4D49L,x,z);
    }
    public static int height(long seed,int x,int z,GroundEcology.Site site){
        long h=OakBirchWoodland.mix(seed^0x42414D424F4F4849L^x*1297L^z*1237L);
        double cohort=GroundEcology.colonyStrength(seed,0x42414D424F4F4147L,x,z,32,8,19,site);
        return Math.min(16,5+(int)Math.floor(7*cohort)+(int)Long.remainderUnsigned(h,5));
    }
    public static int[] column(long seed,int cx,int cz,int[] base,DrainageNetwork d,ClimateField c,GroundEcology.Field ecology){
        var result=new ArrayList<Integer>();
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int wx=cx*16+x,wz=cz*16+z;if(TropicalForestVariants.surfaceKind(seed,wx,wz,d,c)!=2)continue;
            var site=ecology.sample(wx,wz);if(!accepts(seed,wx,wz,site))continue;
            int ground=d.sample(wx,wz).height(),height=height(seed,wx,wz,site);
            for(int dy=1;dy<=height;dy++)if(at(base,wx,ground+dy,wz)!=0){height=dy-1;break;}
            if(height<5)continue;
            for(int dy=1;dy<=height;dy++){
                int code=dy==height?126:dy==height-1?127:dy==height-2?125:124;
                int key=((ground+dy-TerrainShape.FLOOR)<<8)|(z*16+x);result.add((key<<blockbranch.cubic.CanopyLayer.STATE_BITS)|code);
            }
        }
        return result.stream().sorted().mapToInt(Integer::intValue).toArray();
    }
    private static int at(int[] base,int x,int y,int z){
        int key=((y-TerrainShape.FLOOR)<<8)|((z&15)*16+(x&15)),lo=0,hi=base.length-1;
        while(lo<=hi){int mid=(lo+hi)>>>1,k=base[mid]>>>blockbranch.cubic.CanopyLayer.STATE_BITS;if(k==key)return base[mid]&blockbranch.cubic.CanopyLayer.STATE_MASK;if(k<key)lo=mid+1;else hi=mid-1;}return 0;
    }
    private BambooGroves(){}
}
