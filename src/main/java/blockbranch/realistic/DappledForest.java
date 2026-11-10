package blockbranch.realistic;

import java.util.*;

/** Native poplar community in remaining mesic woodland; a static autumn palette, not a season simulation. */
public final class DappledForest {
    public static final int FAMILY=16,LOG=128,SHRUB=149;
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return selected(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c,false);}
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return selected(seed,x,z,d,c,true);}
    private static boolean selected(long seed,int x,int z,DrainageNetwork d,ClimateField c,boolean physical){
        if(!(physical?OakBirchWoodland.surfacePotential(seed,x,z,d,c)&&!Forest.surfacePotential(seed,x,z,d,c):OakBirchWoodland.contains(seed,x,z,d,c)))return false;
        if(Swamp.contains(seed,x,z,d,c)||Swamp.surfaceSoilContains(seed,x,z,d,c)||BirchForest.contains(seed,x,z,d,c)||BirchForest.surfaceContains(seed,x,z,d,c)||TemperateForestVariants.kind(seed,x,z,d,c)>0||TemperateForestVariants.surfaceKind(seed,x,z,d,c)>0||DarkCherryWoodland.kind(seed,x,z,d,c)>0||DarkCherryWoodland.surfaceKind(seed,x,z,d,c)>0)return false;
        var q=c.sample(x,z,d::sample);return q.meanTemperatureC()>=7&&q.meanTemperatureC()<=18&&q.moisture()<=.8&&BirchForest.province(seed^0x504F504C41525052L,x,z)>.35;
    }
    static WoodlandTrees.Root root(TreeScatter.Point p,DrainageNetwork.Sample terrain){
        long h=OakBirchWoodland.mix(p.priority()^0x504F504C41524147L);int height=p.young()?7+(int)Long.remainderUnsigned(h,5):Long.remainderUnsigned(h>>>24,10)<3?20+(int)Long.remainderUnsigned(h,6):13+(int)Long.remainderUnsigned(h,7);
        return new WoodlandTrees.Root(p.x(),terrain.height()+1,p.z(),true,height,(int)(h>>>32),FAMILY);
    }
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){
        // Reuse the asymmetric slender deciduous scaffold, with poplar identity throughout.
        var shape=new WoodlandTrees.Root(root.x(),root.y(),root.z(),true,root.height(),root.variant(),2);
        int color=(int)Long.remainderUnsigned(OakBirchWoodland.mix(root.variant()^0x504F504C41524CL),3),leaf=130+6*color;
        return ForestTrees.tree(shape).stream().map(v->new WoodlandTrees.Voxel(v.x(),v.y(),v.z(),v.code()>=4&&v.code()<=6?LOG+v.code()-4:leaf+WoodlandTrees.leafDistance(v.code()))).toList();
    }
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){return TemperateForestVariants.birchCover(seed^0x504F504C4152464CL,x,z,site,clear);}
    static int[] shrubs(long seed,int cx,int cz,int[] base,DrainageNetwork d,ClimateField c,GroundEcology.Field ecology){
        int[] tops=new int[256];Arrays.fill(tops,Integer.MIN_VALUE);var occupied=new HashSet<Integer>();
        for(int v:base){int key=v>>>blockbranch.cubic.CanopyLayer.STATE_BITS;occupied.add(key);tops[key&255]=(key>>>8)+TerrainShape.FLOOR;}
        var result=new TreeMap<Integer,Integer>();
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int wx=cx*16+x,wz=cz*16+z;if(!surfaceContains(seed,wx,wz,d,c))continue;int y=d.sample(wx,wz).height()+1;
            if(occupied.contains(((y-TerrainShape.FLOOR)<<8)|(z*16+x)))continue;var site=ecology.sample(wx,wz).shade(tops[z*16+x]>y+1);
            double colony=GroundEcology.colonyStrength(seed,0x504F504C41525348L,wx,wz,32,5,14,site);
            double roll=(OakBirchWoodland.mix(seed^wx*1297L^wz*1237L^0x5348525542L)>>>11)*0x1.0p-53;
            if(!site.shaded()&&roll<.35*colony&&FlowerMicrosites.accepts(seed^0x504F505348525542L,wx,wz))result.put(((y-TerrainShape.FLOOR)<<8)|(z*16+x),SHRUB);
        }
        return SwampCanopy.merge(base,result.entrySet().stream().mapToInt(e->(e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS)|e.getValue()).toArray());
    }
    public record Point(int x,int z){}

    private DappledForest(){}
}
