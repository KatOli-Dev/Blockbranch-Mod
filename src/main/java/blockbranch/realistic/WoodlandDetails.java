package blockbranch.realistic;
import java.util.*;

/** Sparse world-seeded deadwood on supported dry ground;shared woodland details stay biome-qualified. */
public final class WoodlandDetails {
    public static int[] logs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){return logs(seed,cx,cz,d,c,0);}
    public static int[] logs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c,int transitions){
        return logs(seed,cx,cz,d,c,transitions,null);
    }
    public static int[] logs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c,int transitions,java.util.function.BiPredicate<Integer,Integer> montane){
        if((transitions&MontaneForest.FLAG)!=0&&montane==null)throw new IllegalArgumentException("Montane deadwood requires saved ownership");
        // Saved vegetation bits own suppression even if a caller supplies a newer owner.
        if((transitions&MontaneForest.FLAG)==0)montane=null;
        var base=SwampCanopy.merge(wetLogs(seed,cx,cz,d,c),temperateLogs(seed,cx,cz,d,c,transitions,montane));
        return RedwoodForest.enabled(transitions)?SwampCanopy.merge(base,redwoodLogs(seed,cx,cz,d,c)):base;
    }
    /**
     * Fallen redwood boles: every cell of a log lies in the redwood province on level dry ground and clear of
     * every standing stem (checked in world coordinates before clipping to the chunk). Lengths stay below the
     * +/-10 search margin so a log that reaches into this chunk is always found from its cell.
     */
    static int[] redwoodLogs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){
        var out=new TreeMap<Integer,Integer>();int minX=cx*16,minZ=cz*16,scale=40;
        for(int gz=Math.floorDiv(minZ-10,scale);gz<=Math.floorDiv(minZ+25,scale);gz++)for(int gx=Math.floorDiv(minX-10,scale);gx<=Math.floorDiv(minX+25,scale);gx++){
            long h=OakBirchWoodland.mix(seed^0x52454446414C4C4EL^gx*0x9E3779B97F4A7C15L^gz*0xC2B2AE3D27D4EB4FL);if((h>>>11)*0x1.0p-53>.6)continue;
            int x=gx*scale+(int)Long.remainderUnsigned(h>>>8,scale),z=gz*scale+(int)Long.remainderUnsigned(h>>>16,scale),length=5+(int)Long.remainderUnsigned(h>>>24,6);boolean east=(h&1)==0;int direction=(h&2)==0?1:-1;
            int endX=x+(east?(length-1)*direction:0),endZ=z+(east?0:(length-1)*direction);
            if(Math.max(x,endX)<minX||Math.min(x,endX)>minX+15||Math.max(z,endZ)<minZ||Math.min(z,endZ)>minZ+15)continue;
            int y=d.sample(x,z).height()+1;boolean valid=true;
            for(int n=0;n<length&&valid;n++){int wx=x+(east?n*direction:0),wz=z+(east?0:n*direction);var q=d.sample(wx,wz);valid=q.height()+1==y&&q.waterLevel()==DrainageNetwork.DRY&&d.channelAt(wx,wz).edge()<0&&!LandVegetation.reserved(d,wx,wz)&&RedwoodForest.contains(seed,wx,wz,d,c);}
            if(!valid)continue;
            for(var root:RedwoodTrees.roots(seed,x-16,z-16,x+16,z+16,d,c)){
                for(int n=0;n<length&&valid;n++)if(Math.hypot(root.x()+.5-x-(east?n*direction:0),root.z()+.5-z-(east?0:n*direction))<(root.height()>=60?5.5:root.height()>=30?4.5:3))valid=false;
                if(valid)for(var v:RedwoodTrees.tree(root))if(v.y()==y)for(int n=0;n<length;n++)if(v.x()==x+(east?n*direction:0)&&v.z()==z+(east?0:n*direction))valid=false;
                if(!valid)break;
            }
            if(!valid)continue;int code=RedwoodTrees.WOOD+(east?1:2);
            for(int n=0;n<length;n++){int wx=x+(east?n*direction:0),wz=z+(east?0:n*direction);if((wx>>4)!=cx||(wz>>4)!=cz)continue;int key=((y-TerrainShape.FLOOR)<<8)|((wz&15)*16+(wx&15));out.put(key,(key<<blockbranch.cubic.CanopyLayer.STATE_BITS)|code);}
        }
        return out.values().stream().mapToInt(Integer::intValue).toArray();
    }
    private static int[] wetLogs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){
        var out=new TreeMap<Integer,Integer>();int minX=cx*16,minZ=cz*16,scale=32;
        for(int gz=Math.floorDiv(minZ-7,scale);gz<=Math.floorDiv(minZ+22,scale);gz++)for(int gx=Math.floorDiv(minX-7,scale);gx<=Math.floorDiv(minX+22,scale);gx++){
            long h=OakBirchWoodland.mix(seed^0x5745544445414457L^gx*0x9E3779B97F4A7C15L^gz*0xC2B2AE3D27D4EB4FL);if((h>>>11)*0x1.0p-53>.22)continue;
            int x=gx*scale+(int)Long.remainderUnsigned(h>>>8,scale),z=gz*scale+(int)Long.remainderUnsigned(h>>>16,scale),length=3+(int)Long.remainderUnsigned(h>>>24,5);boolean east=(h&1)==0;
            if(!Swamp.contains(seed,x,z,d,c))continue;int y=d.sample(x,z).height()+1;boolean valid=true;
            for(int n=0;n<length;n++){int wx=x+(east?n:0),wz=z+(east?0:n);var s=d.sample(wx,wz);if(s.height()+1!=y||s.waterLevel()!=DrainageNetwork.DRY||d.channelAt(wx,wz).edge()>=0||!Swamp.contains(seed,wx,wz,d,c))valid=false;}
            if(!valid)continue;
            // Actual wetland stems leave real gaps;outside-domain potential stems are conservative.
            for(var root:SwampTrees.roots(seed,x-6,z-6,x+(east?length:0)+6,z+(east?0:length)+6,d,c))for(int n=0;n<length;n++)if(Math.hypot(root.x()-x-(east?n:0),root.z()-z-(east?0:n))<4)valid=false;
            for(var p:TreeScatter.points(seed,x-6,z-6,x+(east?length:0)+6,z+(east?0:length)+6)){
                if(d.sample(p.x(),p.z()).waterLevel()!=DrainageNetwork.DRY||Swamp.contains(seed,p.x(),p.z(),d,c))continue;
                for(int n=0;n<length;n++)if(Math.hypot(p.x()-x-(east?n:0),p.z()-z-(east?0:n))<6)valid=false;
            }
            if(!valid)continue;
            for(int n=0;n<length;n++){int wx=x+(east?n:0),wz=z+(east?0:n);if((wx>>4)!=cx||(wz>>4)!=cz)continue;int key=((y-TerrainShape.FLOOR)<<8)|((wz&15)*16+(wx&15));out.put(key,(key<<blockbranch.cubic.CanopyLayer.STATE_BITS)|(east?2:3));}
        }
        return out.values().stream().mapToInt(Integer::intValue).toArray();
    }
    private static boolean temperate(long seed,int x,int z,DrainageNetwork d,ClimateField c){return Forest.potential(seed,x,z,d,c)&&!Swamp.soilContains(seed,x,z,d,c);}
    /** Rare fallen stems;validate the entire log in world coordinates before clipping to a chunk. */
    static int[] temperateLogs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){
        return temperateLogs(seed,cx,cz,d,c,0);
    }
    static int[] temperateLogs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c,int transitions){
        return temperateLogs(seed,cx,cz,d,c,transitions,null);
    }
    private static int[] temperateLogs(long seed,int cx,int cz,DrainageNetwork d,ClimateField c,int transitions,java.util.function.BiPredicate<Integer,Integer> montane){
        var out=new TreeMap<Integer,Integer>();int minX=cx*16,minZ=cz*16,scale=48;
        for(int gz=Math.floorDiv(minZ-8,scale);gz<=Math.floorDiv(minZ+23,scale);gz++)for(int gx=Math.floorDiv(minX-8,scale);gx<=Math.floorDiv(minX+23,scale);gx++){
            long h=OakBirchWoodland.mix(seed^0x46414C4C454E4C4FL^gx*0x9E3779B97F4A7C15L^gz*0xC2B2AE3D27D4EB4FL);if((h>>>11)*0x1.0p-53>.65)continue;
            int x=gx*scale+(int)Long.remainderUnsigned(h>>>8,scale),z=gz*scale+(int)Long.remainderUnsigned(h>>>16,scale),length=3+(int)Long.remainderUnsigned(h>>>24,5);boolean east=(h&1)==0;int direction=(h&2)==0?1:-1;
            int endX=x+(east?(length-1)*direction:0),endZ=z+(east?0:(length-1)*direction);
            if(Math.max(x,endX)<minX||Math.min(x,endX)>minX+15||Math.max(z,endZ)<minZ||Math.min(z,endZ)>minZ+15)continue;
            boolean crowded=false;
            for(int oz=-1;oz<=1;oz++)for(int ox=-1;ox<=1;ox++){if(ox==0&&oz==0)continue;long q=OakBirchWoodland.mix(seed^0x46414C4C454E4C4FL^(gx+ox)*0x9E3779B97F4A7C15L^(gz+oz)*0xC2B2AE3D27D4EB4FL);if((q>>>11)*0x1.0p-53>.65||Long.compareUnsigned(q,h)>=0)continue;int qx=(gx+ox)*scale+(int)Long.remainderUnsigned(q>>>8,scale),qz=(gz+oz)*scale+(int)Long.remainderUnsigned(q>>>16,scale);if(Math.hypot(qx-x,qz-z)<14)crowded=true;}
            if(crowded||!temperate(seed,x,z,d,c))continue;int y=d.sample(x,z).height()+1;boolean valid=true;
            for(int n=0;n<length;n++){int wx=x+(east?n*direction:0),wz=z+(east?0:n*direction);var q=d.sample(wx,wz);if(q.height()+1!=y||q.waterLevel()!=DrainageNetwork.DRY||d.channelAt(wx,wz).edge()>=0||LandVegetation.reserved(d,wx,wz)||!temperate(seed,wx,wz,d,c))valid=false;}
            // This old hardwood-deadwood sampler knows only old standing roots.
            // Reject a complete log touching new needleleaf ownership before
            // chunk clipping, rather than intersect new stems or leave stubs.
            // Dedicated conifer deadwood remains an unrepresented floor detail.
            if(montane!=null)for(int n=0;n<length;n++)if(montane.test(x+(east?n*direction:0),z+(east?0:n*direction)))valid=false;
            if(!valid)continue;
            var roots=WoodlandTrees.roots(seed,x-14,z-14,x+14,z+14,d,c,transitions);
            for(var root:roots){for(int n=0;n<length;n++)if(Math.hypot(root.x()-x-(east?n*direction:0),root.z()-z-(east?0:n*direction))<(root.height()<10?6:3.5))valid=false;
                if(valid)for(var v:WoodlandTrees.tree(root))if(v.y()==y)for(int n=0;n<length;n++)if(v.x()==x+(east?n*direction:0)&&v.z()==z+(east?0:n*direction))valid=false;
            }
            if(!valid)continue;boolean birch=BirchForest.contains(seed,x,z,d,c)?(h&15)!=0:(h&7)==0;int code=(birch?3:0)+(east?2:3);
            for(int n=0;n<length;n++){int wx=x+(east?n*direction:0),wz=z+(east?0:n*direction);if((wx>>4)!=cx||(wz>>4)!=cz)continue;int key=((y-TerrainShape.FLOOR)<<8)|((wz&15)*16+(wx&15));out.put(key,(key<<blockbranch.cubic.CanopyLayer.STATE_BITS)|code);}
        }
        return out.values().stream().mapToInt(Integer::intValue).toArray();
    }
    /** Tiny disturbance patches and sparse blooms;existing live-tree geometry is not sampled or changed. */
    static int temperateFloor(long seed,int x,int z,GroundEcology.Site site,boolean clear,int cover){
        if(!clear)return cover;
        double slope=Math.hypot(site.dx(),site.dz()),disturbance=GroundEcology.colonyStrength(seed,0x534F494C5455524EL,x,z,48,2,4.5,site);
        if(slope<.25&&site.retention()>-.2&&disturbance>.55)return 2;
        if(cover==2)cover=site.shaded()?1:3;
        if(cover==1&&site.shaded()&&GroundEcology.colonyStrength(seed,0x4C49545445525041L,x,z,40,7,14,site)>.3)cover=7;
        if((cover==3||cover==6)&&!site.shaded()&&site.wetness()>=.4&&site.wetness()<=.8&&slope<.3&&site.exposure()<.6){long h=OakBirchWoodland.mix(seed^0x574F4F44424C4F4FL^x*1297L^z*1237L);if((h>>>11)*0x1.0p-53<.004)return(h&3)==0?31:30;}
        return cover;
    }
    /** Background blooms supplement coherent colonies only in suitable lit marsh gaps. */
    public static boolean marshFlower(long seed,int x,int z,GroundEcology.Site s){
        if(s.shaded()||s.wetness()<.60||Math.hypot(s.dx(),s.dz())>.15)return false;
        double colony=GroundEcology.colonyStrength(seed,0x53574F5243484944L,x,z,32,4,9,s),v=(OakBirchWoodland.mix(seed^0x464C4F574552L^x*1297L^z*1237L)>>>11)*0x1.0p-53;
        return v<.0008+.025*colony;
    }
    private WoodlandDetails(){}
}
