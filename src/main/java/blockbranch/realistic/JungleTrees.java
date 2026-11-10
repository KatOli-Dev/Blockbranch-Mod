package blockbranch.realistic;
import java.util.*;

/** Tropical stand cohorts with several crown/trunk architectures, not scaled copies of one tree. */
public final class JungleTrees {
    public static final int MAX_HEIGHT=60;
    private static final int R=WoodlandTrees.RADIUS,S=R*2+1,PLANE=S*S;
    private record Node(int x,int y,int z){}
    private record Crown(double x,double y,double z,double ry,double[] footprint,double[] tilt){}
    public static List<WoodlandTrees.Root> roots(long seed,int minX,int minZ,int maxX,int maxZ,DrainageNetwork d,ClimateField c){
        var result=new ArrayList<WoodlandTrees.Root>();var field=new GroundEcology.Field(d::sample,c);
        for(var point:TreeScatter.points(seed,minX-R,minZ-R,maxX+R,maxZ+R,TreeScatter.Spacing.CLOSED)){
            int x=point.x(),z=point.z();if(Swamp.contains(seed,x,z,d,c))continue;if(!Jungle.contains(seed,x,z,d,c))continue;
            var site=field.sample(x,z);var center=d.sample(x,z);if(center.waterLevel()!=DrainageNetwork.DRY||d.channelAt(x,z).edge()>=0)continue;
            double gap=gap(seed,x,z),recruit=GroundEcology.colonyStrength(seed,0x4A554E4752454352L,x,z,64,20,42,site);
            long h=OakBirchWoodland.mix(point.priority()^0x4A554E474C454147L);
            double density=point.young()?Math.clamp(.55+.20*gap+.35*recruit,.2,.95):(.97+.03*recruit)*(1-.25*gap);
            if(gap>.90&&!point.young()||unit(h^0x44454E53495459L)>density||!TreeScatter.establishes(seed,point,3,site))continue;
            boolean safe=true;for(int dz=-R;dz<=R;dz+=R)for(int dx=-R;dx<=R;dx+=R)if(Math.abs(d.sample(x+dx,z+dz).height()-center.height())>3||LandVegetation.reserved(d,x+dx,z+dz))safe=false;
            if(!safe)continue;
            int cohort=(int)Long.remainderUnsigned(h>>>20,10),height=point.young()?(gap>.55?10+(int)Long.remainderUnsigned(h,5):6+(int)Long.remainderUnsigned(h,7)):cohort<2?15+(int)Long.remainderUnsigned(h,8):cohort<9?28+(int)Long.remainderUnsigned(h,12):42+(int)Long.remainderUnsigned(h,13);
            // Subcanopy leans toward nearby openings. Direction is stored in the low 12 variant bits.
            double gx=gap(seed,x+8,z)-gap(seed,x-8,z),gz=gap(seed,x,z+8)-gap(seed,x,z-8);
            double angle=Math.hypot(gx,gz)>.025?Math.atan2(gz,gx):unit(h^0x414E474C45L)*Math.PI*2;
            int direction=(int)Math.floorMod(Math.round(angle/(Math.PI*2)*4096),4096);
            int variant=((int)(h>>>32)&~4095)|direction;
            result.add(new WoodlandTrees.Root(x,center.height()+1,z,false,height,variant));
        }
        return result;
    }
    private static double gap(long seed,int x,int z){return GroundEcology.colonyStrength(seed,0x4A554E4747415053L,x,z,64,18,34,GroundEcology.NEUTRAL);}
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){
        byte[] grid=new byte[PLANE*MAX_HEIGHT],distance=new byte[grid.length];Arrays.fill(distance,(byte)7);
        int h=root.height(),ceiling=Math.min(MAX_HEIGHT,h+8),form=h<15?0:h<23?1:h<44?2:3;long seed=OakBirchWoodland.mix(root.variant()^0x43524F574E534850L);
        double facing=(root.variant()&4095)/4096.0*Math.PI*2,s=Math.max(1,h/30.0);
        int lean=form==3?1:1+(int)Long.remainderUnsigned(seed,2);
        int fork=Math.max(3,(int)Math.round(h*(form==2?.40+.18*unit(seed^13):form==3?.68+.13*unit(seed^13):.38+.22*unit(seed^13))));
        var stem=new Node((int)Math.round(Math.cos(facing)*lean),fork,(int)Math.round(Math.sin(facing)*lean));var base=new Node(0,form>=2?4:0,0);segment(grid,new Node(0,0,0),base);segment(grid,base,stem);
        if(h>=23&&unit(seed^0x4255545452455353L)<.55){int sx=(seed&1)==0?1:-1,sz=(seed&2)==0?1:-1,b=1+(int)Long.remainderUnsigned(seed>>>8,4);for(int z:new int[]{0,sz})for(int x:new int[]{0,sx})segment(grid,new Node(x,0,z),new Node(x,b,z));}
        var crowns=new ArrayList<Crown>();
        if(form==0){
            // Shade-tolerant recruits carry ascending leafy branches through much of their height.
            // They are not miniatures of an adult crown on a long empty pole.
            var top=new Node(stem.x(),h-2,stem.z());segment(grid,stem,top);
            int arms=3+(int)Long.remainderUnsigned(seed>>>6,4);
            for(int n=0;n<arms;n++){
                long v=OakBirchWoodland.mix(seed+n*911);int by=1+(int)((h-4)*(n+.2+.6*unit(v))/(double)arms);
                var start=attach(grid,by);double angle=facing+n*2.39996+(unit(v^17)-.5);
                var end=endpoint(start,angle,1.3+unit(v^29)*1.5,Math.min(h-2,by+1+(int)Long.remainderUnsigned(v>>>20,3)));
                branch(grid,start,end,v);crowns.add(crown(end.x()*.8,end.y()+.4,end.z()*.8,1.6+unit(v)*.8,1.7+unit(v^19)*.9,1.5+unit(v^43)*.9,v));
            }
            crowns.add(crown(top.x(),h-2,top.z(),2.1+unit(seed)*.7,2.3,2.1,seed^71));
        }else if(form==1){
            // An unequal leader and staggered ascending limbs form a vertically extended subcanopy.
            var top=endpoint(stem,facing,.7,h-2);segment(grid,stem,top);
            int arms=3+(int)Long.remainderUnsigned(seed>>>6,3);
            for(int n=0;n<arms;n++){
                long v=OakBirchWoodland.mix(seed+n*997);int by=Math.min(h-4,(int)(h*(n==0?.20+.12*unit(v):.38+.38*(n+unit(v))/arms)));
                var start=attach(grid,by);double angle=facing+n*2.39996+(unit(v^17)-.5)*.9;
                var end=endpoint(start,angle,2+unit(v^29)*2,Math.min(h-3,by+1+(int)Long.remainderUnsigned(v>>>20,3)));
                branch(grid,start,end,v);crowns.add(crown((end.x()+start.x())*.45,end.y()+1,(end.z()+start.z())*.45,3.2+unit(v)*1.2,2.7+unit(v^19)*1.2,3+unit(v^43)*1.4,v));
            }
            crowns.add(crown(top.x()*.8,h-3,top.z()*.8,3.4+unit(seed^113),3.1+unit(seed^127),3.2+unit(seed^149),seed^101));
        }else if(form==2){
            int arms=2+(int)Long.remainderUnsigned(seed>>>6,3);
            for(int n=0;n<arms;n++){
                long v=OakBirchWoodland.mix(seed+n*997);double angle=facing+n*(Math.PI*2/arms)+(.8*unit(v)-.4);
                var end=endpoint(stem,angle,n==0?1.2+unit(v):2.5+unit(v^17)*2,h-3-(int)Long.remainderUnsigned(v>>>16,5));
                branch(grid,stem,end,v);
                crowns.add(crown((end.x()+stem.x())*.32,end.y()+1,(end.z()+stem.z())*.32,(4.5+unit(v)*1.5)*s,(3.2+unit(v^19)*1.3)*Math.sqrt(s),(4.2+unit(v^29)*1.8)*s,v));
            }
            if((seed&3)==0){var lower=attach(grid,Math.max(4,h/2-3));var end=endpoint(lower,facing+2.2,3,h/2+1);branch(grid,lower,end,seed^61);crowns.add(crown(end.x()*.7,end.y()+1,end.z()*.7,3.2,2.8,3.4,seed^71));}
        }else{
            var leader=endpoint(stem,facing,.7,h-2);segment(grid,stem,leader);
            int arms=3+(int)Long.remainderUnsigned(seed>>>6,3);
            for(int n=0;n<arms;n++){long v=OakBirchWoodland.mix(seed+n*1237);double angle=facing+n*(Math.PI*2/arms)+(unit(v)-.5);var end=endpoint(stem,angle,2.5+unit(v^17)*2.5,h-4-(int)Long.remainderUnsigned(v>>>16,4));branch(grid,stem,end,v);crowns.add(crown(end.x()*.7,end.y()+1,end.z()*.7,(3.5+unit(v)*1.4)*s,(2.6+unit(v^19)*1.3)*Math.sqrt(s),(3.5+unit(v^29)*1.5)*s,v));}
            crowns.add(crown(leader.x(),h-3,leader.z(),(5.2+unit(seed^113))*s,(4.0+unit(seed^127))*Math.sqrt(s),(5+unit(seed^149))*s,seed^101));
        }
        for(int y=1;y<ceiling;y++)for(int z=-R;z<=R;z++)for(int x=-R;x<=R;x++){
            int i=index(x,y,z);if(grid[i]!=0)continue;int horizontal=(z+R)*S+x+R;
            for(var crown:crowns){double q=crown.footprint()[horizontal];if(q>1.18)continue;double dy=(y-crown.y()-crown.tilt()[horizontal])/crown.ry();q+=dy*dy;if(q>1)continue;long edge=OakBirchWoodland.mix(seed^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL^y*0x165667B19E3779F9L);if(q<.64||(edge&15)>1){grid[i]=40;break;}}
        }
        int[] queue=new int[grid.length];int head=0,tail=0;for(int i=0;i<grid.length;i++)if(grid[i]>=37&&grid[i]<=39){distance[i]=0;queue[tail++]=i;}
        while(head<tail){int i=queue[head++],x=i%S-R,z=i/S%S-R,y=i/PLANE;if(distance[i]>=6)continue;for(var step:DIR){int nx=x+step[0],ny=y+step[1],nz=z+step[2];if(Math.abs(nx)>R||Math.abs(nz)>R||ny<0||ny>=MAX_HEIGHT)continue;int next=index(nx,ny,nz);if(grid[next]==40&&distance[next]>distance[i]+1){distance[next]=(byte)(distance[i]+1);queue[tail++]=next;}}}
        var result=new ArrayList<WoodlandTrees.Voxel>();for(int y=0;y<ceiling;y++)for(int z=-R;z<=R;z++)for(int x=-R;x<=R;x++){int i=index(x,y,z),code=grid[i];if(code==40)code=distance[i]>6?0:39+distance[i];if(code!=0)result.add(new WoodlandTrees.Voxel(root.x()+x,root.y()+y,root.z()+z,code));}return result;
    }
    private static Crown crown(double x,double y,double z,double rx,double ry,double rz,long seed){
        x=Math.clamp(x,-4,4);z=Math.clamp(z,-4,4);double cap=Math.min(R-Math.abs(x),R-Math.abs(z))-.8;rx=Math.min(rx,cap);rz=Math.min(rz,cap);y=Math.min(y,MAX_HEIGHT-ry-1);
        double a=unit(seed^0x524F54415445L)*Math.PI*2,cs=Math.cos(a),sn=Math.sin(a),slopeX=(unit(seed^43)-.5)*.45,slopeZ=(unit(seed^79)-.5)*.45;double[] q=new double[PLANE],tilt=new double[PLANE];
        for(int iz=-R;iz<=R;iz++)for(int ix=-R;ix<=R;ix++){double dx=ix-x,dz=iz-z,u=dx*cs+dz*sn,v=-dx*sn+dz*cs;long lump=OakBirchWoodland.mix(seed^Math.floorDiv(ix,2)*0x165667B19E3779F9L^Math.floorDiv(iz,2)*0xD1B54A32D192ED03L);double scallop=.90+.18*unit(lump);int i=(iz+R)*S+ix+R;q[i]=(u*u/(rx*rx)+v*v/(rz*rz))/scallop;tilt[i]=dx*slopeX+dz*slopeZ;}
        return new Crown(x,y,z,ry,q,tilt);
    }
    private static Node endpoint(Node start,double angle,double length,int y){return new Node((int)Math.clamp(Math.round(start.x()+Math.cos(angle)*length),-5,5),y,(int)Math.clamp(Math.round(start.z()+Math.sin(angle)*length),-5,5));}
    private static void branch(byte[] grid,Node start,Node end,long seed){var elbow=new Node((int)Math.round(start.x()+(end.x()-start.x())*.58),Math.min(end.y(),start.y()+Math.max(1,(end.y()-start.y())/2)+((seed&1)==0?1:0)),(int)Math.round(start.z()+(end.z()-start.z())*.58));segment(grid,start,elbow);segment(grid,elbow,end);}
    private static void segment(byte[] grid,Node a,Node b){
        int x=a.x(),y=a.y(),z=a.z(),dx=Math.abs(b.x()-x),dy=Math.abs(b.y()-y),dz=Math.abs(b.z()-z),steps=Math.max(dy,Math.max(dx,dz));
        // Orient the whole woody segment by its dominant direction. Horizontal correction
        // blocks in a steep leader keep vertical bark rather than exposing repeated cut ends.
        int axis=dy>=dx&&dy>=dz?37:dx>=dz?38:39;putLog(grid,x,y,z,axis);
        for(int n=1;n<=steps;n++){
            int tx=(int)Math.round(a.x()+(b.x()-a.x())*n/(double)steps),ty=(int)Math.round(a.y()+(b.y()-a.y())*n/(double)steps),tz=(int)Math.round(a.z()+(b.z()-a.z())*n/(double)steps);
            while(y!=ty){y+=Integer.signum(ty-y);putLog(grid,x,y,z,axis);}
            while(x!=tx){x+=Integer.signum(tx-x);putLog(grid,x,y,z,axis);}
            while(z!=tz){z+=Integer.signum(tz-z);putLog(grid,x,y,z,axis);}
        }
    }
    private static void putLog(byte[] grid,int x,int y,int z,int axis){int i=index(x,y,z);if(grid[i]==0)grid[i]=(byte)axis;}
    private static Node attach(byte[] grid,int y){for(int distance=0;distance<=R;distance++)for(int z=-distance;z<=distance;z++)for(int x=-distance;x<=distance;x++)if(Math.abs(x)+Math.abs(z)==distance&&grid[index(x,y,z)]>=37&&grid[index(x,y,z)]<=39)return new Node(x,y,z);throw new IllegalArgumentException("Missing trunk attachment");}
    private static double unit(long value){return(OakBirchWoodland.mix(value)>>>11)*0x1.0p-53;}
    private static int index(int x,int y,int z){return y*PLANE+(z+R)*S+x+R;}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private JungleTrees(){}
}
