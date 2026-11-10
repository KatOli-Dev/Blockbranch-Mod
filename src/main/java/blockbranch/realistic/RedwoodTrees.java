package blockbranch.realistic;

import java.util.*;

/**
 * Redwood stands at one block per metre. Two species share the stand:
 * <ul>
 * <li><b>Coast redwood</b> (<i>Sequoia sempervirens</i>): 70-110 m giants with a 3x3 flared base, a long
 * branch-free bole and a narrow conical crown of drooping whorled boughs; a 45-70 m intermediate cohort and
 * 10-30 m saplings that are crowned to near the ground.</li>
 * <li><b>Giant sequoia</b> (<i>Sequoiadendron giganteum</i>, stored as {@code Root.birch}): 60-95 m with a 4x4
 * buttressed base, a heavier taper and a high, rounded crown. It grows only at montane elevations.</li>
 * </ul>
 * Wood is native mangrove bark on every face and foliage is native spruce leaves; every leaf keeps a six-block
 * support path to wood so no crown decays. Geometry stays within {@link #REACH} blocks of the root column
 * (the root search margin is {@link WoodlandTrees#RADIUS}, so every chunk window sees every stem that can touch
 * it) and below {@link #MAX_HEIGHT} blocks above the root.
 */
public final class RedwoodTrees {
    /** Tallest stem. Canopy may rise 128 blocks above local ground (TerrainProfile window); stems on 4 blocks of relief stay well inside. */
    public static final int MAX_STEM=110,MAX_HEIGHT=MAX_STEM+14,ROOT_DEPTH=4,REACH=8;
    /** Giants keep this clear distance from any higher-priority giant candidate (Matern type-II thinning). */
    private static final int GIANT_GAP=10;
    /** Canopy codes 170/171/172: mangrove bark on the Y/X/Z axis. */
    public static final int WOOD=blockbranch.cubic.CanopyLayer.REDWOOD_WOOD_CODE,WOOD_LAST=WOOD+2;
    public static boolean woodCode(int code){return code>=WOOD&&code<=WOOD_LAST;}
    private record P(int x,int y,int z){}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final long SALT=0x5245445745524F4FL;

    public static boolean sequoia(WoodlandTrees.Root r){return r.birch();}

    /** Elevation above sea level where giant sequoia begins to replace coast redwood, and how fast. */
    private static double sequoiaShare(int elevation){return Math.clamp((elevation-250)/700.0,0,.55);}
    private static boolean giantCandidate(TreeScatter.Point p){
        long h=OakBirchWoodland.mix(p.priority()^SALT);
        return !p.young()&&Long.remainderUnsigned(h>>>20,100)>=22&&unit(OakBirchWoodland.mix(h^0x4B454550L))<.6;
    }

    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();int radius=WoodlandTrees.RADIUS;
        var ecology=new GroundEcology.Field(d::sample,c);
        var wide=TreeScatter.points(seed,minX-radius-GIANT_GAP,minZ-radius-GIANT_GAP,maxX+radius+GIANT_GAP,maxZ+radius+GIANT_GAP);
        var giants=new ArrayList<TreeScatter.Point>();for(var p:wide)if(giantCandidate(p))giants.add(p);
        for(var p:TreeScatter.points(seed,minX-radius,minZ-radius,maxX+radius,maxZ+radius)){
            int x=p.x(),z=p.z();
            if(!RedwoodForest.contains(seed,x,z,d,c)||RedwoodForest.glade(seed,x,z))continue;
            long h=OakBirchWoodland.mix(p.priority()^SALT);
            if(!TreeScatter.establishes(seed,p,1,ecology.sample(x,z)))continue;
            boolean giant=giantCandidate(p);
            if(giant){
                // Whether or not the neighbour itself survives, so the outcome is independent of the request window.
                boolean crowded=false;
                for(var q:giants)if(q!=p&&Long.compareUnsigned(q.priority(),p.priority())>0&&Math.hypot(q.x()-x,q.z()-z)<GIANT_GAP){crowded=true;break;}
                if(crowded)continue;
            }else if(unit(OakBirchWoodland.mix(h^0x4B454550L))>=(p.young()?.95:.9))continue;
            var center=d.sample(x,z);boolean safe=true;
            for(int dz=-5;dz<=5;dz+=5)for(int dx=-5;dx<=5;dx+=5){
                var s=d.sample(x+dx,z+dz);
                if(s.waterLevel()!=DrainageNetwork.DRY||Math.abs(s.height()-center.height())>4||LandVegetation.reserved(d,x+dx,z+dz))safe=false;
            }
            if(!safe)continue;
            int elevation=center.height()+1-TerrainShape.SEA_LEVEL;
            boolean sequoia=unit(OakBirchWoodland.mix(h^0x53455155L))<sequoiaShare(elevation);
            int roll=(int)Long.remainderUnsigned(h,1000);
            int height;
            if(p.young())height=10+roll%21;
            else if(!giant)height=(sequoia?40:45)+roll%(sequoia?20:25);
            else height=sequoia?60+roll%36:72+roll%(MAX_STEM-71);
            result.add(new WoodlandTrees.Root(x,center.height()+1,z,sequoia,height,(int)(h>>>32),RedwoodForest.FAMILY));
        }
        return result;
    }

    /** Trunk footprint at height y as {lo,hi}: a square spanning lo..hi on both axes (always contains 0..1 until the leader). */
    private static int[] footprint(boolean sequoia,int h,int y){
        if(h<24)return new int[]{0,0};
        if(sequoia){
            int narrow=Math.max(10,h*16/100);
            if(y>h-1-narrow)return y>h-1-narrow/3?new int[]{0,0}:new int[]{0,1};
            if(y<h*8/100)return new int[]{-1,2};
            if(y<h*35/100)return new int[]{-1,1};
            return new int[]{0,1};
        }
        int narrow=Math.max(10,h*18/100);
        if(y>h-1-narrow)return y>h-1-narrow/3?new int[]{0,0}:new int[]{0,1};
        if(h>=60&&y<h*10/100)return new int[]{-1,1};
        return h>=30?new int[]{0,1}:new int[]{0,0};
    }

    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root r){
        long seed=OakBirchWoodland.mix(r.variant()^0x5245445747494E54L);
        boolean sequoia=sequoia(r);int h=r.height(),top=h-1;
        var wood=new HashMap<P,Integer>();var leaves=new HashSet<P>();
        // Bole: extended below the root so a slope never leaves it floating (blocks at or under the terrain
        // are clipped by the caller).
        for(int y=-ROOT_DEPTH;y<=top;y++){
            var f=footprint(sequoia,h,Math.max(0,y));
            for(int z=f[0];z<=f[1];z++)for(int x=f[0];x<=f[1];x++)wood.put(new P(x,y,z),WOOD);
        }
        // Buttress lobes around the base: tall, ragged ridges, taller on a sequoia.
        var base=footprint(sequoia,h,0);
        if(h>=30)for(int side=0;side<4;side++){
            if(unit(seed^(0x464C415245L+side))>=.9)continue;
            int lobe=(sequoia?4:3)+(int)Long.remainderUnsigned(OakBirchWoodland.mix(seed^side*0x9E3779B97F4A7C15L),sequoia?5:4);
            int mid=(base[0]+base[1])/2,outer=side%2==0?base[0]-1:base[1]+1;boolean alongX=side<2;
            for(int y=-ROOT_DEPTH;y<=lobe;y++){
                wood.putIfAbsent(alongX?new P(outer,y,mid):new P(mid,y,outer),WOOD);
                if(y<=lobe-2)wood.putIfAbsent(alongX?new P(outer,y,mid+1):new P(mid+1,y,outer),WOOD);
            }
        }
        double crownFraction=h<20?.70:h<45?.58:sequoia?.34:.46+.08*unit(seed^11);
        int bole=Math.max(2,(int)(h*(1-crownFraction)));
        double rmax=sequoia?Math.min(5,3.6+.02*h+unit(seed^13)):h<20?Math.min(3.2,1.6+.09*h):h<45?3.2+.03*(h-20):Math.min(5,3.9+.012*(h-45)+.4*unit(seed^13));
        double angle0=unit(seed)*Math.PI*2;
        int whorl=0;
        for(int y=bole;y<top-1;whorl++){
            long w=OakBirchWoodland.mix(seed+whorl*0x9E3779B97F4A7C15L);
            double f=(top-y)/(double)Math.max(1,top-bole);
            double len=rmax*(sequoia?dome(1-f):(.18+.82*Math.pow(f,.8))*(f>.88?1-(f-.88)*1.5:1));
            int arms=h<20?4:sequoia?5+(int)Long.remainderUnsigned(w,3):4+(int)Long.remainderUnsigned(w,3);
            double phase=angle0+unit(w^7)*Math.PI*2;
            var fp=footprint(sequoia,h,y);
            for(int arm=0;arm<arms;arm++){
                long b=OakBirchWoodland.mix(w^arm*0x165667B19E3779F9L);
                double a=phase+arm*Math.PI*2/arms+(unit(b)-.5)*.5,length=Math.max(1,len*(.65+.35*unit(b^3)));
                bough(wood,leaves,a,length,y,fp,unit(b^5),sequoia);
            }
            // A sparse collar of foliage veils the trunk at each whorl without hiding the bark.
            for(int cz=fp[0]-1;cz<=fp[1]+1;cz++)for(int cx=fp[0]-1;cx<=fp[1]+1;cx++){
                boolean ring=cx==fp[0]-1||cx==fp[1]+1||cz==fp[0]-1||cz==fp[1]+1;boolean corner=(cx==fp[0]-1||cx==fp[1]+1)&&(cz==fp[0]-1||cz==fp[1]+1);
                if(ring&&!corner&&unit(OakBirchWoodland.mix(w^cx*131L^cz*977L))<.4)leaves.add(new P(cx,y,cz));
            }
            y+=(sequoia?4:3)+(int)(w&1);
        }
        crownTop(leaves,top,sequoia);
        return finish(r,wood,leaves);
    }
    /** Dome width profile: t=0 at the lowest limb, 1 at the apex; widest about two fifths of the way up. */
    private static double dome(double t){return t<.4?.55+.45*(t/.4):Math.max(.12,1-.88*Math.pow((t-.4)/.6,2));}

    /** A drooping bough with a leafy spray along its outer part and a pendant tip curtain. */
    private static void bough(Map<P,Integer> wood,Set<P> leaves,double angle,double length,int y,int[] fp,double droopRoll,boolean sequoia){
        double dx=Math.cos(angle),dz=Math.sin(angle);
        int x=dx>.34?fp[1]:dx<-.34?fp[0]:(fp[0]+fp[1]+(dx>=0?1:0))/2;
        int z=dz>.34?fp[1]:dz<-.34?fp[0]:(fp[0]+fp[1]+(dz>=0?1:0))/2;
        // Keep start + length + one block of foliage inside the reach.
        length=Math.min(length,REACH-1-Math.max(Math.abs(x),Math.abs(z)));
        int steps=Math.max(1,(int)Math.round(length)),droop=(int)Math.round(length*(sequoia?.1+.12*droopRoll:.22+.2*droopRoll));
        int cx=x,cy=y,cz=z,sx=x,sz=z;
        var path=new ArrayList<P>();path.add(new P(cx,cy,cz));
        for(int n=1;n<=steps;n++){
            int tx=sx+(int)Math.round(dx*n),tz=sz+(int)Math.round(dz*n),ty=y-(int)Math.round(droop*n/(double)steps);
            while(cy>ty){cy--;wood.putIfAbsent(new P(cx,cy,cz),WOOD);path.add(new P(cx,cy,cz));}
            while(cx!=tx){cx+=Integer.signum(tx-cx);wood.putIfAbsent(new P(cx,cy,cz),WOOD+1);path.add(new P(cx,cy,cz));}
            while(cz!=tz){cz+=Integer.signum(tz-cz);wood.putIfAbsent(new P(cx,cy,cz),WOOD+2);path.add(new P(cx,cy,cz));}
        }
        int first=Math.max(1,(int)(path.size()*(sequoia?.5:.35)));
        for(int i=first;i<path.size();i++){
            var p=path.get(i);
            for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,-1,0}})leaves.add(new P(p.x()+d[0],p.y()+d[1],p.z()+d[2]));
            if(sequoia)leaves.add(new P(p.x(),p.y()+1,p.z()));
        }
        var tip=path.getLast();
        for(int[] d:new int[][]{{1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1},{0,-2,0},{0,1,0}})leaves.add(new P(tip.x()+d[0],tip.y()+d[1],tip.z()+d[2]));
    }
    /** A tapering spire: the living leader stays inside foliage that narrows to one block. */
    private static void crownTop(Set<P> leaves,int top,boolean sequoia){
        int depth=sequoia?5:Math.min(8,Math.max(4,top/12)),cap=sequoia?3:2;
        for(int y=top-depth;y<=top+1;y++){
            int radius=(int)Math.round(cap*(top+1-y)/(double)(depth+1));
            for(int z=-radius;z<=radius;z++)for(int x=-radius;x<=radius;x++)if(Math.abs(x)+Math.abs(z)<=radius)leaves.add(new P(x,y,z));
        }
    }
    private static List<WoodlandTrees.Voxel> finish(WoodlandTrees.Root r,Map<P,Integer> wood,Set<P> leaves){
        var distance=new HashMap<P,Integer>();var queue=new ArrayDeque<P>();
        for(var p:wood.keySet()){distance.put(p,0);queue.add(p);}
        while(!queue.isEmpty()){
            var p=queue.removeFirst();int n=distance.get(p);if(n>=6)continue;
            for(int[] d:DIR){
                var q=new P(p.x()+d[0],p.y()+d[1],p.z()+d[2]);
                if(leaves.contains(q)&&!wood.containsKey(q)&&distance.getOrDefault(q,7)>n+1){distance.put(q,n+1);queue.add(q);}
            }
        }
        var out=new ArrayList<WoodlandTrees.Voxel>();
        for(var e:wood.entrySet()){var p=e.getKey();if(inside(p))out.add(new WoodlandTrees.Voxel(r.x()+p.x(),r.y()+p.y(),r.z()+p.z(),e.getValue()));}
        for(var p:leaves){
            int n=distance.getOrDefault(p,7);
            if(n>6||wood.containsKey(p)||!inside(p))continue;
            out.add(new WoodlandTrees.Voxel(r.x()+p.x(),r.y()+p.y(),r.z()+p.z(),21+n));
        }
        out.sort(Comparator.comparingInt(WoodlandTrees.Voxel::y).thenComparingInt(WoodlandTrees.Voxel::z).thenComparingInt(WoodlandTrees.Voxel::x));
        return out;
    }
    private static boolean inside(P p){return Math.abs(p.x())<=REACH&&Math.abs(p.z())<=REACH;}
    private static double unit(long v){return(OakBirchWoodland.mix(v)>>>11)*0x1.0p-53;}
    private RedwoodTrees(){}
}
