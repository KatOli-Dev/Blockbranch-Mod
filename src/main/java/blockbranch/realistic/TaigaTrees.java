package blockbranch.realistic;
import java.util.*;

/** Continuous asymmetric evergreen envelopes around connected,unequal branch scaffolds,with young and mature cohorts. */
public final class TaigaTrees {
    public static final int WOOD=78;
    private record P(int x,int y,int z){}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static WoodlandTrees.Root root(TreeScatter.Point p,DrainageNetwork.Sample s){long v=OakBirchWoodland.mix(p.priority()^0x5441494741545245L);int height=p.young()?7+(int)Long.remainderUnsigned(v>>>8,6):16+(int)Long.remainderUnsigned(v>>>8,13);return new WoodlandTrees.Root(p.x(),s.height()+1,p.z(),false,height,(int)(v>>>32),4);}
    public static WoodlandTrees.Root snowyRoot(TreeScatter.Point p,DrainageNetwork.Sample s){long v=OakBirchWoodland.mix(p.priority()^0x534E4F5754524545L);return new WoodlandTrees.Root(p.x(),s.height()+1,p.z(),false,p.young()?7+(int)Long.remainderUnsigned(v,5):13+(int)Long.remainderUnsigned(v,11),(int)(v>>>32),5);}
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root r){
        long seed=OakBirchWoodland.mix(r.variant()^0x5441494741424F55L);int h=r.height(),form=(int)Long.remainderUnsigned(seed,3),skirt=h<14?1+(int)(seed&1):3+(int)Long.remainderUnsigned(seed>>>8,5);double angle=unit(seed^3)*Math.PI*2,lean=.3+unit(seed^7)*1.6,width=h<14?1.5+unit(seed^11):2.8+unit(seed^11)*2;
        if(r.family()==5){width*=.82;skirt=Math.max(1,skirt-1);}
        if(r.family()==6){
            // Grove age ranges overlap at 11..13m: avoid classifying every short mature
            // stem as a narrow juvenile. Continuous width gives saplings real lower boughs.
            width=Math.min(5.2,.18*h+.75+.60*unit(seed^11));lean*=.65;skirt=Math.max(1,skirt-2);
        }
        if(r.family()==7){width=Math.min(5.6,.16*h+.35);skirt=Math.max(3,(int)(h*.45));lean*=.7;}
        if(r.family()==8){width=Math.min(5.8,(.11+.07*unit(seed^0x5749445448L))*h+.4+unit(seed^0x43524F574EL));skirt=(h<15?1:2)+(int)Long.remainderUnsigned(seed,5);lean*=.55+.4*unit(seed^0x4C45414EL);}
        var wood=new HashMap<P,Integer>();var centers=new ArrayList<P>();var sprays=new ArrayList<P[]>();
        for(int y=0;y<h;y++){double t=y/(double)h;centers.add(new P((int)Math.round(Math.cos(angle)*lean*t*t),y,(int)Math.round(Math.sin(angle)*lean*t*t)));if(y>0&&y<h-2)limb(wood,centers.get(y-1),centers.get(y));else if(y==0)wood.put(centers.getFirst(),WOOD);}
        for(int y=skirt+2,n=0;y<h-2;){long v=OakBirchWoodland.mix(seed+n++*1297L);double f=(h-1-y)/(double)(h-skirt),reach=Math.max(0,width*envelope(r.family(),f,form==1?.65:form==2?1.1:.9)*.52-.45);int count=3+(int)Long.remainderUnsigned(v,4);var center=centers.get(y);
            for(int arm=0;arm<count;arm++){long b=OakBirchWoodland.mix(v+arm*997L);double a=angle+arm*2.39996+unit(b^13),length=reach*(.65+.35*unit(b^17));P end=new P(center.x()+(int)Math.round(Math.cos(a)*length),Math.max(skirt+1,y-(int)Long.remainderUnsigned(b>>>12,3)),center.z()+(int)Math.round(Math.sin(a)*length));limb(wood,center,end);
                double outer=Math.max(1,width*envelope(r.family(),f,form==1?.65:form==2?1.1:.9))*(.7+.3*unit(b^17));P foliageTip=new P(center.x()+(int)Math.round(Math.cos(a)*outer),Math.max(skirt,y-(int)Long.remainderUnsigned(b>>>12,3)),center.z()+(int)Math.round(Math.sin(a)*outer));sprays.add(new P[]{end,foliageTip});}
            y+=2+(int)Long.remainderUnsigned(v>>>16,3);
        }
        var raw=new HashMap<P,Integer>(wood);
        for(int y=skirt;y<=h;y++){double f=Math.max(0,(h-y)/(double)(h-skirt));var center=centers.get(Math.min(y,h-1));double base=width*envelope(r.family(),f,form==1?.65:form==2?1.1:.85),phase=unit(seed^23)*Math.PI*2;
            for(int z=-7;z<=7;z++)for(int x=-7;x<=7;x++){
                double dx=x-center.x(),dz=z-center.z(),a=Math.atan2(dz,dx),asym=1+.14*Math.sin(a+angle)+.10*Math.cos(3*a+phase),bough=.90+.10*Math.sin(y*.9+phase+a*.5),radius=Math.max((r.family()==6?.62:1.15)*Math.min(1,(h-y)/2.0),base*asym*bough);
                if(dx*dx+dz*dz<=radius*radius)raw.putIfAbsent(new P(x,y,z),22);
            }
        }
        for(var spray:sprays)foliage(raw,spray[0],spray[1]);
        // Living leaders and terminal branch ends stay inside real foliage.
        for(var p:wood.keySet())if(p.y()>skirt){int neighbours=0;for(var q:DIR)if(wood.containsKey(new P(p.x()+q[0],p.y()+q[1],p.z()+q[2])))neighbours++;if(neighbours<=1)for(var q:DIR)raw.putIfAbsent(new P(p.x()+q[0],p.y()+q[1],p.z()+q[2]),22);}
        var distance=new HashMap<P,Integer>();var queue=new ArrayDeque<P>();for(var p:wood.keySet()){distance.put(p,0);queue.add(p);}
        while(!queue.isEmpty()){var p=queue.removeFirst();int n=distance.get(p);if(n>=6)continue;for(var q:DIR){var near=new P(p.x()+q[0],p.y()+q[1],p.z()+q[2]);if(raw.getOrDefault(near,0)==22&&distance.getOrDefault(near,7)>n+1){distance.put(near,n+1);queue.add(near);}}}
        var out=new ArrayList<WoodlandTrees.Voxel>();for(var e:raw.entrySet()){var p=e.getKey();int code=e.getValue();if(code==22){int n=distance.getOrDefault(p,7);if(n>6)continue;code=21+n;}out.add(new WoodlandTrees.Voxel(r.x()+p.x(),r.y()+p.y(),r.z()+p.z(),code));}out.sort(Comparator.comparingInt(WoodlandTrees.Voxel::y).thenComparingInt(WoodlandTrees.Voxel::z).thenComparingInt(WoodlandTrees.Voxel::x));return out;
    }
    private static double envelope(int family,double f,double exponent){return family==7?Math.min(1,f*4)*(.55+.45*Math.sin(Math.PI*f)):Math.pow(f,exponent);}
    private static void limb(Map<P,Integer> map,P a,P b){int x=a.x(),y=a.y(),z=a.z(),steps=Math.max(Math.abs(a.x()-b.x()),Math.max(Math.abs(a.y()-b.y()),Math.abs(a.z()-b.z())));map.putIfAbsent(a,WOOD);for(int n=1;n<=steps;n++){int tx=(int)Math.round(a.x()+(b.x()-a.x())*n/(double)steps),ty=(int)Math.round(a.y()+(b.y()-a.y())*n/(double)steps),tz=(int)Math.round(a.z()+(b.z()-a.z())*n/(double)steps);while(y!=ty){y+=Integer.signum(ty-y);map.putIfAbsent(new P(x,y,z),WOOD);}while(x!=tx){x+=Integer.signum(tx-x);map.putIfAbsent(new P(x,y,z),WOOD+1);}while(z!=tz){z+=Integer.signum(tz-z);map.putIfAbsent(new P(x,y,z),WOOD+2);}}}
    private static void foliage(Map<P,Integer> raw,P a,P b){int x=a.x(),y=a.y(),z=a.z(),steps=Math.max(Math.abs(a.x()-b.x()),Math.max(Math.abs(a.y()-b.y()),Math.abs(a.z()-b.z())));for(int n=0;n<=steps;n++){int tx=steps==0?x:(int)Math.round(a.x()+(b.x()-a.x())*n/(double)steps),ty=steps==0?y:(int)Math.round(a.y()+(b.y()-a.y())*n/(double)steps),tz=steps==0?z:(int)Math.round(a.z()+(b.z()-a.z())*n/(double)steps);while(x!=tx){x+=Integer.signum(tx-x);raw.putIfAbsent(new P(x,y,z),22);}while(y!=ty){y+=Integer.signum(ty-y);raw.putIfAbsent(new P(x,y,z),22);}while(z!=tz){z+=Integer.signum(tz-z);raw.putIfAbsent(new P(x,y,z),22);}raw.putIfAbsent(new P(x,y,z),22);if(n<steps*.55)raw.putIfAbsent(new P(x,y+1,z),22);}}
    private static double unit(long v){return(OakBirchWoodland.mix(v)>>>11)*0x1.0p-53;}
    private TaigaTrees(){}
}
