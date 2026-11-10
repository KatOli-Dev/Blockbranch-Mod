package blockbranch.realistic;
import java.util.*;

/** Low crooked oak scaffolds and broad unequal crowns on solid saturated banks. */
/** Frozen d61ff42 geometry oracle; QA only. */
public final class SwampTreesReference {
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();var ecology=new GroundEcology.Field(d::sample,c);
        for(var point:TreeScatter.points(seed,minX-8,minZ-8,maxX+8,maxZ+8)){
            int x=point.x(),z=point.z();if(!Swamp.contains(seed,x,z,d,c)||!TreeScatter.establishes(seed,point,0,ecology.sample(x,z))||OakBirchWoodland.glade(seed^0x5357414D50474150L,x,z))continue;
            var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)continue;boolean safe=true;
            for(int dz=-2;dz<=2;dz+=2)for(int dx=-2;dx<=2;dx+=2){var q=d.sample(x+dx,z+dz);if(q.waterLevel()!=DrainageNetwork.DRY||Math.abs(q.height()-s.height())>1||d.channelAt(x+dx,z+dz).edge()>=0)safe=false;}
            if(!safe)continue;long v=OakBirchWoodland.mix(point.priority()^0x5357414D50545245L);int h=point.young()?5+(int)Long.remainderUnsigned(v,4):9+(int)Long.remainderUnsigned(v,6);
            result.add(new WoodlandTrees.Root(x,s.height()+1,z,false,h,(int)(v>>>32),3));
        }
        return result;
    }
    private record P(int x,int y,int z){}
    private record Crown(double x,double y,double z,double rx,double ry,double rz,long seed){}
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){
        long seed=OakBirchWoodland.mix(root.variant()^0x5357414D50464F52L);int h=root.height();var wood=new HashMap<P,Integer>();var crowns=new ArrayList<Crown>();double angle=unit(seed)*Math.PI*2;
        P base=new P(0,0,0),straight=new P(0,Math.min(3,h/2),0),fork=new P((int)Math.round(Math.cos(angle)*(1+unit(seed^7))),Math.max(3,h/2),(int)Math.round(Math.sin(angle)*(1+unit(seed^7))));
        limb(wood,base,straight);limb(wood,straight,fork);
        int arms=h<9?2+(int)(seed&1):3+(int)Long.remainderUnsigned(seed>>>8,3);
        for(int n=0;n<arms;n++){
            long v=OakBirchWoodland.mix(seed+n*1297L);double a=angle+n*2.39996+(unit(v^17)-.5)*.9,length=(h<9?2:3)+unit(v^23)*(h<9?1:2);
            P tip=new P((int)Math.clamp(Math.round(fork.x()+Math.cos(a)*length),-5,5),Math.max(fork.y()+1,h-2-(int)Long.remainderUnsigned(v>>>12,h<9?2:4)),(int)Math.clamp(Math.round(fork.z()+Math.sin(a)*length),-5,5));
            P elbow=new P((fork.x()+tip.x())/2,fork.y()+Math.max(1,(tip.y()-fork.y())/2),(fork.z()+tip.z())/2);limb(wood,fork,elbow);limb(wood,elbow,tip);
            crowns.add(new Crown(tip.x()*.8,tip.y()+.5,tip.z()*.8,h<9?2.1:2.8+unit(v^29),1.8+unit(v^31),h<9?2.0:2.7+unit(v^37),v));
        }
        var raw=new HashMap<P,Integer>(wood);
        for(int y=1;y<24;y++)for(int z=-8;z<=8;z++)for(int x=-8;x<=8;x++){
            var p=new P(x,y,z);if(raw.containsKey(p))continue;
            for(var c:crowns){double dx=x-c.x(),dz=z-c.z(),q=dx*dx/(c.rx()*c.rx())+dz*dz/(c.rz()*c.rz())+(y-c.y()-.12*dx)*(y-c.y()-.12*dx)/(c.ry()*c.ry());if(q<1){raw.put(p,7);break;}}
        }
        // Real terminal twigs stay inside foliage,without changing the broad crown architecture.
        for(var p:wood.keySet())if(p.y()>2){int neighbours=0;for(var d:DIR)if(wood.containsKey(new P(p.x()+d[0],p.y()+d[1],p.z()+d[2])))neighbours++;if(neighbours<=1)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)raw.putIfAbsent(new P(p.x()+dx,p.y()+dy,p.z()+dz),7);}
        var distance=new HashMap<P,Integer>();var queue=new ArrayDeque<P>();for(var p:wood.keySet()){distance.put(p,0);queue.add(p);}
        while(!queue.isEmpty()){var p=queue.removeFirst();int v=distance.get(p);if(v>=6)continue;for(var d:DIR){var q=new P(p.x()+d[0],p.y()+d[1],p.z()+d[2]);if(raw.containsKey(q)&&!wood.containsKey(q)&&distance.getOrDefault(q,7)>v+1){distance.put(q,v+1);queue.add(q);}}}
        var result=new ArrayList<WoodlandTrees.Voxel>();for(var e:raw.entrySet()){var p=e.getKey();int code=e.getValue();if(code==7)code=distance.getOrDefault(p,7)>6?0:6+distance.get(p);if(code>0)result.add(new WoodlandTrees.Voxel(root.x()+p.x(),root.y()+p.y(),root.z()+p.z(),code));}
        result.sort(Comparator.comparingInt(WoodlandTrees.Voxel::y).thenComparingInt(WoodlandTrees.Voxel::z).thenComparingInt(WoodlandTrees.Voxel::x));return result;
    }
    private static void limb(Map<P,Integer> wood,P a,P b){int x=a.x(),y=a.y(),z=a.z(),dy=Math.abs(b.y()-y),dx=Math.abs(b.x()-x),dz=Math.abs(b.z()-z),axis=dy>=dx&&dy>=dz?1:dx>=dz?2:3,steps=Math.max(dy,Math.max(dx,dz));wood.putIfAbsent(a,axis);for(int n=1;n<=steps;n++){int tx=(int)Math.round(a.x()+(b.x()-a.x())*n/(double)steps),ty=(int)Math.round(a.y()+(b.y()-a.y())*n/(double)steps),tz=(int)Math.round(a.z()+(b.z()-a.z())*n/(double)steps);while(y!=ty){y+=Integer.signum(ty-y);wood.putIfAbsent(new P(x,y,z),axis);}while(x!=tx){x+=Integer.signum(tx-x);wood.putIfAbsent(new P(x,y,z),axis);}while(z!=tz){z+=Integer.signum(tz-z);wood.putIfAbsent(new P(x,y,z),axis);}}}
    private static double unit(long v){return(OakBirchWoodland.mix(v)>>>11)*0x1.0p-53;}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private SwampTreesReference(){}
}
