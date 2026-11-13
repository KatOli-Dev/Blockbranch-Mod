package blockbranch.realistic;
import java.util.*;

/** World-coordinate stilt-root networks and unequal connected crowns;native source-water roots. */
public final class MangroveTrees {
    public static final int LOG=62,LEAF=65,ROOT=71,WET_ROOT=72,PROPAGULE=73,RADIUS=8;
    public record Stem(int x,int y,int z,int height,long variant){}
    private record P(int x,int y,int z){}
    private record Crown(double x,double y,double z,double rx,double ry,double rz){}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static List<Stem> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var out=new ArrayList<Stem>();var ecology=new GroundEcology.Field(d::sample,c);
        for(var p:TreeScatter.points(seed^0x4D414E475354454DL,minX-RADIUS,minZ-RADIUS,maxX+RADIUS,maxZ+RADIUS)){
            if(!MangroveSwamp.exact(seed,p.x(),p.z(),d,c))continue;
            double colony=GroundEcology.colonyStrength(seed,0x4D414E475354414EL,p.x(),p.z(),64,18,38,ecology.sample(p.x(),p.z()));
            if(unit(p.priority())>.55+.45*colony)continue;var s=d.sample(p.x(),p.z());long v=OakBirchWoodland.mix(p.priority()^0x4D414E47524F4F54L);
            int y=Math.max(s.height()+2,TerrainShape.SEA_LEVEL+1)+(int)Long.remainderUnsigned(v>>>9,3),h=p.young()?6+(int)Long.remainderUnsigned(v>>>16,4):11+(int)Long.remainderUnsigned(v>>>16,7);
            out.add(new Stem(p.x(),y,p.z(),h,v));
        }return out;
    }
    public static List<WoodlandTrees.Voxel> tree(Stem r,DrainageNetwork d){return tree(r,d,false);}
    /** {@code arched} selects the gated finer prop roots; false reproduces the published root shapes exactly. */
    public static List<WoodlandTrees.Voxel> tree(Stem r,DrainageNetwork d,boolean arched){
        var wood=new HashMap<P,Integer>();var roots=new HashMap<P,Integer>();var crowns=new ArrayList<Crown>();long seed=r.variant();double angle=unit(seed^7)*Math.PI*2;
        P base=new P(r.x(),r.y(),r.z()),fork=new P(r.x()+(int)Math.round(Math.cos(angle)*1.5),r.y()+Math.max(3,r.height()/2),r.z()+(int)Math.round(Math.sin(angle)*1.5));
        limb(wood,base,fork,LOG);int arms=2+(int)Long.remainderUnsigned(seed>>>23,r.height()<10?2:4);
        for(int n=0;n<arms;n++){
            long v=OakBirchWoodland.mix(seed+n*1297L);double a=angle+n*2.39996+(unit(v^11)-.5)*.8,len=2+unit(v^13)*(r.height()<10?2:4);
            P tip=new P(r.x()+(int)Math.round(Math.cos(a)*len),r.y()+r.height()-2-(int)Long.remainderUnsigned(v>>>15,3),r.z()+(int)Math.round(Math.sin(a)*len));
            P elbow=new P((fork.x()+tip.x())/2,fork.y()+Math.max(1,(tip.y()-fork.y())/2),(fork.z()+tip.z())/2);limb(wood,fork,elbow,LOG);limb(wood,elbow,tip,LOG);
            crowns.add(new Crown(tip.x()-.3*Math.cos(a),tip.y()+.5,tip.z()-.3*Math.sin(a),2.4+unit(v^17)*1.3,1.8+unit(v^19)*1.2,2.3+unit(v^23)*1.4));
        }
        // Each tree chooses its own root count,direction,reach and elbow;no copied radial template.
        int feet=3+(int)Long.remainderUnsigned(seed>>>33,4),anchored=0;
        for(int n=0;n<feet;n++){
            long v=OakBirchWoodland.mix(seed^n*997L);double a=angle+n*2.39996+(unit(v^31)-.5),reach=arched?3+unit(v^37)*2.5:2+unit(v^37)*3;
            int x=r.x()+(int)Math.round(Math.cos(a)*reach),z=r.z()+(int)Math.round(Math.sin(a)*reach);var s=d.sample(x,z);
            if(d.channelAt(x,z).edge()>=0||s.height()>=r.y()||s.height()<TerrainShape.SEA_LEVEL-4)continue;
            P foot=new P(x,s.height()+1,z),knee=new P((x+r.x())/2,Math.max(s.height()+2,r.y()-1),(z+r.z())/2);var path=new HashMap<P,Integer>();
            if(arched)arch(path,r,foot,v);else{limb(path,base,knee,ROOT);limb(path,knee,foot,ROOT);}
            boolean safe=true;for(var p:path.keySet())if(p.y()<=d.sample(p.x(),p.z()).height())safe=false;
            if(!safe)continue;roots.putAll(path);anchored++;
        }
        if(anchored<2)return List.of();
        var raw=new HashMap<P,Integer>(wood);for(var e:roots.entrySet())raw.putIfAbsent(e.getKey(),e.getValue());
        for(var crown:crowns)for(int y=(int)Math.floor(crown.y()-crown.ry());y<=crown.y()+crown.ry();y++)for(int z=(int)Math.floor(crown.z()-crown.rz());z<=crown.z()+crown.rz();z++)for(int x=(int)Math.floor(crown.x()-crown.rx());x<=crown.x()+crown.rx();x++){
            if(Math.abs(x-r.x())>RADIUS||Math.abs(z-r.z())>RADIUS)continue;double dx=(x-crown.x())/crown.rx(),dz=(z-crown.z())/crown.rz(),dy=(y-crown.y()-.10*(x-crown.x()))/crown.ry();if(dx*dx+dy*dy+dz*dz<1)raw.putIfAbsent(new P(x,y,z),LEAF);
        }
        // Terminal wood ends remain in living foliage;lower buttresses remain exposed.
        for(var p:wood.keySet())if(p.y()>r.y()+3){int neighbours=0;for(var q:DIR)if(wood.containsKey(new P(p.x()+q[0],p.y()+q[1],p.z()+q[2])))neighbours++;if(neighbours<=1)for(var q:DIR)raw.putIfAbsent(new P(p.x()+q[0],p.y()+q[1],p.z()+q[2]),LEAF);}
        raw.entrySet().removeIf(e->{var p=e.getKey();var s=d.sample(p.x(),p.z());return p.y()<=s.height()||e.getValue()!=ROOT&&s.waterLevel()!=DrainageNetwork.DRY&&p.y()<s.waterLevel()||d.channelAt(p.x(),p.z()).edge()>=0;});
        var distance=new HashMap<P,Integer>();var queue=new ArrayDeque<P>();for(var p:wood.keySet())if(raw.containsKey(p)){distance.put(p,0);queue.add(p);}
        while(!queue.isEmpty()){var p=queue.removeFirst();int n=distance.get(p);if(n>=6)continue;for(var q:DIR){var near=new P(p.x()+q[0],p.y()+q[1],p.z()+q[2]);if(raw.getOrDefault(near,0)==LEAF&&distance.getOrDefault(near,7)>n+1){distance.put(near,n+1);queue.add(near);}}}
        var out=new ArrayList<WoodlandTrees.Voxel>();for(var e:raw.entrySet()){
            var p=e.getKey();int code=e.getValue();if(code==LEAF){int n=distance.getOrDefault(p,7);if(n>6)continue;code=LEAF+n-1;}else if(code==ROOT){var s=d.sample(p.x(),p.z());if(s.waterLevel()!=DrainageNetwork.DRY&&p.y()<s.waterLevel())code=WET_ROOT;}
            out.add(new WoodlandTrees.Voxel(p.x(),p.y(),p.z(),code));
        }
        // Sparse native hanging propagules grow by their normal age ticks;never unsupported ground saplings.
        var occupied=new HashSet<P>();for(var v:out)occupied.add(new P(v.x(),v.y(),v.z()));var additions=new ArrayList<WoodlandTrees.Voxel>();
        for(var v:out)if(v.code()>=LEAF&&v.code()<ROOT){var p=new P(v.x(),v.y()-1,v.z());if(p.y()>TerrainShape.SEA_LEVEL&&!occupied.contains(p)&&unit(seed^p.x()*1297L^p.z()*1237L^p.y()*997L)<.018)additions.add(new WoodlandTrees.Voxel(p.x(),p.y(),p.z(),PROPAGULE+(int)Long.remainderUnsigned(OakBirchWoodland.mix(seed^p.x()^p.z()),5)));}out.addAll(additions);return out;
    }
    public static int[] column(long seed,int cx,int cz,DrainageNetwork d,ClimateField c){return column(seed,cx,cz,d,c,false);}
    public static int[] column(long seed,int cx,int cz,DrainageNetwork d,ClimateField c,boolean arched){
        // A six-block halo contains every path for a retained distance<=6 leaf.
        var merged=new HashMap<P,Integer>();for(var r:roots(seed,cx*16-6,cz*16-6,cx*16+21,cz*16+21,d,c))for(var v:tree(r,d,arched)){
            if(v.x()<cx*16-6||v.x()>cx*16+21||v.z()<cz*16-6||v.z()>cz*16+21)continue;
            merged.merge(new P(v.x(),v.y(),v.z()),v.code(),(a,b)->priority(a)<=priority(b)?a:b);
        }
        var distance=new HashMap<P,Integer>();var queue=new ArrayDeque<P>();for(var e:merged.entrySet())if(e.getValue()>=LOG&&e.getValue()<LEAF){distance.put(e.getKey(),0);queue.add(e.getKey());}
        while(!queue.isEmpty()){var p=queue.removeFirst();int n=distance.get(p);if(n>=6)continue;for(var q:DIR){var near=new P(p.x()+q[0],p.y()+q[1],p.z()+q[2]);int code=merged.getOrDefault(near,0);if(code>=LEAF&&code<ROOT&&distance.getOrDefault(near,7)>n+1){distance.put(near,n+1);queue.add(near);}}}
        var sorted=new TreeMap<Integer,Integer>();for(var e:merged.entrySet()){
            var p=e.getKey();if((p.x()>>4)!=cx||(p.z()>>4)!=cz)continue;int code=e.getValue();
            if(code>=LEAF&&code<ROOT){int n=distance.getOrDefault(p,7);if(n>6)continue;code=LEAF+n-1;}
            if(code>=PROPAGULE){var above=new P(p.x(),p.y()+1,p.z());int support=merged.getOrDefault(above,0);if(support<LEAF||support>=ROOT||distance.getOrDefault(above,7)>6)continue;}
            int key=((p.y()-TerrainShape.FLOOR)<<8)|((p.z()&15)*16+(p.x()&15));sorted.put(key,code);
        }
        return sorted.entrySet().stream().mapToInt(e->(e.getKey()<<blockbranch.cubic.CanopyLayer.STATE_BITS)|e.getValue()).toArray();
    }
    private static int priority(int code){return code<LEAF?0:code==ROOT||code==WET_ROOT?1:code<ROOT?2:3;}
    private static void limb(Map<P,Integer> map,P a,P b,int base){int x=a.x(),y=a.y(),z=a.z(),steps=Math.max(Math.abs(a.x()-b.x()),Math.max(Math.abs(a.y()-b.y()),Math.abs(a.z()-b.z())));map.putIfAbsent(a,base);for(int n=1;n<=steps;n++){
        int tx=(int)Math.round(a.x()+(b.x()-a.x())*n/(double)steps),ty=(int)Math.round(a.y()+(b.y()-a.y())*n/(double)steps),tz=(int)Math.round(a.z()+(b.z()-a.z())*n/(double)steps);
        while(y!=ty){y+=Integer.signum(ty-y);map.putIfAbsent(new P(x,y,z),base);}while(x!=tx){x+=Integer.signum(tx-x);map.putIfAbsent(new P(x,y,z),base==ROOT?ROOT:base+1);}while(z!=tz){z+=Integer.signum(tz-z);map.putIfAbsent(new P(x,y,z),base==ROOT?ROOT:base+2);}
    }}
    /** One prop root as a thin arch. It leaves the trunk at its own height (stem base to two blocks up, so a stand's
     * roots do not share one shelf), swings outward with a small rise, then drops steeply to the ground: a quadratic
     * curve sampled four times per block and joined face to face. The published roots reached 2 to 5 blocks yet
     * piled against the trunk; these reach 3 to 5.5 (inside the 8-block stem radius), keep the published stems,
     * count, angles, ground foot and grounded-path test, and are skipped as before when any block would be buried. */
    private static void arch(Map<P,Integer> map,Stem r,P foot,long v){
        int y0=r.y()+(int)Long.remainderUnsigned(v>>>11,3);double lift=.5+unit(v^41)*1.5,out=.45+unit(v^43)*.25;
        double cx=r.x()+(foot.x()-r.x())*out,cy=y0+lift,cz=r.z()+(foot.z()-r.z())*out;
        int span=Math.abs(foot.x()-r.x())+Math.abs(foot.y()-y0)+Math.abs(foot.z()-r.z()),samples=Math.max(4,span*4);P prev=new P(r.x(),y0,r.z());map.putIfAbsent(prev,ROOT);
        for(int i=1;i<=samples;i++){
            double t=i/(double)samples,u=1-t;
            P cur=new P((int)Math.round(u*u*r.x()+2*u*t*cx+t*t*foot.x()),(int)Math.round(u*u*y0+2*u*t*cy+t*t*foot.y()),(int)Math.round(u*u*r.z()+2*u*t*cz+t*t*foot.z()));
            if(cur.equals(prev))continue;
            // Diagonal samples are bridged y, x then z so every root block touches the previous one by a face.
            int x=prev.x(),y=prev.y(),z=prev.z();while(y!=cur.y()){y+=Integer.signum(cur.y()-y);map.putIfAbsent(new P(x,y,z),ROOT);}
            while(x!=cur.x()){x+=Integer.signum(cur.x()-x);map.putIfAbsent(new P(x,y,z),ROOT);}while(z!=cur.z()){z+=Integer.signum(cur.z()-z);map.putIfAbsent(new P(x,y,z),ROOT);}
            prev=cur;
        }
    }
    private static double unit(long v){return(OakBirchWoodland.mix(v)>>>11)*0x1.0p-53;}
    private MangroveTrees(){}
}
