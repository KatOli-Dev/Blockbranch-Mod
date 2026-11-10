package blockbranch.realistic;
import java.util.*;

/** Native vine faces and sparse lily colonies overlay existing water;no water datum is changed. */
public final class SwampCanopy {
    public static final int VINE_FIRST=46,VINE_LAST=60,LILY=61;
    @FunctionalInterface public interface Lookup{int code(int x,int y,int z);}
    public static int[] decorate(long seed,int cx,int cz,Map<WoodlandTrees.Root,List<WoodlandTrees.Voxel>> trees,Lookup lookup,DrainageNetwork d,ClimateField c){
        var out=new TreeMap<Integer,Integer>();int[][] directions={{0,-1},{1,0},{0,1},{-1,0}};
        for(var entry:trees.entrySet()){
            var root=entry.getKey();long tree=OakBirchWoodland.mix(root.variant()^0x535756494E4553L);if(root.height()<8||(tree&3)==0)continue;
            var anchors=new ArrayList<WoodlandTrees.Voxel>();for(var v:entry.getValue())if(v.code()>=7&&v.y()>root.y()+3&&lookup.code(v.x(),v.y(),v.z())>=7&&lookup.code(v.x(),v.y(),v.z())<=12)anchors.add(v);
            anchors.sort(Comparator.comparingLong(v->OakBirchWoodland.mix(tree^v.x()*1297L^v.y()*1237L^v.z()*997L)));
            var used=new ArrayList<int[]>();int limit=2+(int)Long.remainderUnsigned(tree>>>8,4);
            for(var v:anchors){if(used.size()>=limit)break;boolean near=false;for(var p:used)if(Math.hypot(p[0]-v.x(),p[1]-v.z())<3)near=true;if(near)continue;
                for(int n=0;n<4;n++){int dx=directions[n][0],dz=directions[n][1],x=v.x()+dx,z=v.z()+dz;if(lookup.code(x,v.y(),z)!=0)continue;var s=d.sample(x,z);int bottom=Math.max(s.height()+2,s.waterLevel()==DrainageNetwork.DRY?Integer.MIN_VALUE:s.waterLevel());
                    var reed=InlandLakeReedbed.site(d,x,z);if(InlandLakeReedbed.cover(seed,x,z,InlandLakeReedbed.contains(x,z,(xx,zz)->InlandLakeReedbed.site(d,xx,zz)),reed)>1)bottom=Math.max(bottom,s.waterLevel()+2);
                    int length=2+(int)Long.remainderUnsigned(OakBirchWoodland.mix(tree^v.x()*131L^v.z()*137L),5),mask=1<<((n+2)%4);boolean placed=false;
                    for(int y=v.y();y>=Math.max(bottom,v.y()-length);y--){if(lookup.code(x,y,z)!=0)break;if((x>>4)==cx&&(z>>4)==cz){int key=((y-TerrainShape.FLOOR)<<8)|((z&15)*16+(x&15));int old=out.getOrDefault(key,45);out.put(key,45+((old-45)|mask));}placed=true;}
                    if(placed){used.add(new int[]{x,z});break;}
                }
            }
        }
        // Check all quart cells before per-block work;large tree-free oceans do not receive a plant scan.
        boolean possible=false;for(int z=0;z<16;z+=4)for(int x=0;x<16;x+=4){var s=d.sample(cx*16+x,cz*16+z);int dep=s.waterLevel()==DrainageNetwork.DRY?0:s.waterLevel()-s.height()-1;if(dep>=1&&dep<=3&&d.terminalLakeWater(cx*16+x,cz*16+z,s))possible=true;}
        if(possible&&Swamp.climateSuitable(c.sample(cx*16+8,cz*16+8,d::sample))){
            var site=new GroundEcology.Site(.85,0,0,0,0,false);
            for(int z=cz*16;z<cz*16+16;z++)for(int x=cx*16;x<cx*16+16;x++){
                var s=d.sample(x,z);int dep=s.waterLevel()==DrainageNetwork.DRY?0:s.waterLevel()-s.height()-1;if(dep<1||dep>3||!d.terminalLakeWater(x,z,s)||d.channelAt(x,z).edge()>=0||lookup.code(x,s.waterLevel(),z)!=0)continue;
                if(InlandLakeReedbed.cover(seed,x,z,InlandLakeReedbed.contains(x,z,(xx,zz)->InlandLakeReedbed.site(d,xx,zz)),InlandLakeReedbed.site(d,x,z))>1)continue;
                double colony=GroundEcology.colonyStrength(seed,0x53574C494C595041L,x,z,32,5,13,site),v=(OakBirchWoodland.mix(seed^x*1297L^z*1237L)>>>11)*0x1.0p-53;if(v>.45*colony)continue;
                boolean bank=false;for(var q:directions)if(Swamp.bank(x+q[0]*24,z+q[1]*24,d)!=null)bank=true;if(!bank)continue;
                int key=((s.waterLevel()-TerrainShape.FLOOR)<<8)|((z&15)*16+(x&15));out.putIfAbsent(key,LILY);
            }
        }
        return out.entrySet().stream().mapToInt(e->(e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS)|e.getValue()).toArray();
    }
    public static int[] merge(int[] base,int[] decor){if(decor.length==0)return base;if(base.length==0)return decor;var sorted=new TreeMap<Integer,Integer>();for(int v:base)sorted.put(v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,v);for(int v:decor)sorted.putIfAbsent(v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,v);return sorted.values().stream().mapToInt(Integer::intValue).toArray();}
    private SwampCanopy(){}
}
