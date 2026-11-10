package blockbranch.realistic;

import java.util.*;

/**
 * A seeded irregular potential-stem process shared across woodland families, with local hard-core spacing.
 * <p>Candidates (about 260 per hectare) are thinned Matérn type-II: a stem is dropped when a higher-priority
 * candidate lies closer than the sum of both clearances. {@link Spacing#OPEN} caps the result near 65 stems per
 * hectare with at least 7 m between mature stems, which suits open woodland and savanna but leaves a closed forest
 * with a quarter of its real crown cover. Closed-canopy stands use a smaller hard core instead. Because the rule only
 * ever suppresses more as clearances grow, every {@code OPEN} survivor also survives the smaller core
 * ({@link Point#legacy()}), so a producer can keep exact open-woodland placement beside denser stands.
 */
public final class TreeScatter {
    private static final int CELL=32;
    /** Hard-core radii in blocks (one block is one metre) for mature and young stems; a pair needs the sum of its radii. */
    public enum Spacing{
        /** Open woodland and savanna: mature stems at least 7 m apart. */
        OPEN(3.5,2.2),
        /** Closed broadleaf canopy: mature stems at least 3.6 m apart, so neighbouring 7-8 m crowns touch and overlap. */
        CLOSED(1.8,1.2),
        /** Closed conifer stands, whose crowns are narrower: mature stems at least 2.8 m apart. */
        CONIFER(1.4,1.0);
        final double mature,young;
        Spacing(double mature,double young){this.mature=mature;this.young=young;}
    }
    /** {@code legacy}: the stem also survives {@link Spacing#OPEN} thinning, i.e. it is in the historical point set. */
    public record Point(int x,int z,long priority,boolean young,double clearance,boolean legacy){}
    public static List<Point> points(long seed,int minX,int minZ,int maxX,int maxZ){return points(seed,minX,minZ,maxX,maxZ,Spacing.OPEN);}
    public static List<Point> points(long seed,int minX,int minZ,int maxX,int maxZ,Spacing spacing){
        int ax=Math.floorDiv(minX,CELL),az=Math.floorDiv(minZ,CELL),bx=Math.floorDiv(maxX,CELL),bz=Math.floorDiv(maxZ,CELL);
        boolean open=spacing==Spacing.OPEN;
        var cells=new HashMap<Long,List<Point>>();
        for(int z=az-1;z<=bz+1;z++)for(int x=ax-1;x<=bx+1;x++)cells.put(key(x,z),cell(seed,x,z,spacing));
        var result=new ArrayList<Point>();
        for(int z=az;z<=bz;z++)for(int x=ax;x<=bx;x++)for(var p:cells.get(key(x,z))){
            if(p.x()<minX||p.x()>maxX||p.z()<minZ||p.z()>maxZ)continue;boolean keep=true,legacy=true;
            outer:for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(var q:cells.get(key(x+dx,z+dz))){
                if(Long.compareUnsigned(q.priority(),p.priority())<=0)continue;
                double squared=Math.pow(p.x()-q.x(),2)+Math.pow(p.z()-q.z(),2);
                double distance=p.clearance()+q.clearance();if(squared<distance*distance){keep=false;break outer;}
                if(!open&&legacy){double old=(p.young()?Spacing.OPEN.young:Spacing.OPEN.mature)+(q.young()?Spacing.OPEN.young:Spacing.OPEN.mature);if(squared<old*old)legacy=false;}
            }
            if(keep)result.add(open||legacy?p:new Point(p.x(),p.z(),p.priority(),p.young(),p.clearance(),false));
        }
        return result;
    }
    private static List<Point> cell(long seed,int x,int z,Spacing spacing){
        long cell=OakBirchWoodland.mix(seed^0x5354454D5041434BL^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL);int count=18+(int)Long.remainderUnsigned(cell,19);var result=new ArrayList<Point>(count);
        for(int i=0;i<count;i++){
            long h=OakBirchWoodland.mix(cell+i*0x165667B19E3779F9L);boolean young=Long.remainderUnsigned(h>>>24,10)<3;
            int px=x*CELL+(int)Long.remainderUnsigned(h,CELL),pz=z*CELL+(int)Long.remainderUnsigned(h>>>16,CELL);
            result.add(new Point(px,pz,h,young,young?spacing.young:spacing.mature,true));
        }
        return result;
    }
    public static boolean establishes(long seed,Point p,int family,GroundEcology.Site site){
        double recruitment=GroundEcology.colonyStrength(seed,0x52454352554954L,p.x(),p.z(),48,14,30,site);
        double moisture=Math.clamp(.78+.35*site.wetness()+.08*site.retention()-.14*site.exposure(),.55,1);
        double density=(family==1||family==3?1:family==2?.76:.92)*(p.young()?.60+.40*recruitment:.84+.16*recruitment)*moisture;
        return unit(OakBirchWoodland.mix(p.priority()^0x45535441424C4953L))<density;
    }
    private static long key(int x,int z){return(x&0xffffffffL)|((long)z<<32);}
    private static double unit(long value){return(value>>>11)*0x1.0p-53;}
    private TreeScatter(){}
}
