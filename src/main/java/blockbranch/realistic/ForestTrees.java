package blockbranch.realistic;
import java.util.*;

/** Temperate oak/birch cohorts with asymmetric scaffold crowns and connected, bark-oriented limbs. */
public final class ForestTrees {
    public static final int MAX_HEIGHT=40;
    private static final int R=WoodlandTrees.RADIUS,S=2*R+1,PLANE=S*S;
    private record Node(int x,int y,int z){}
    private record Crown(double y,double ry,double[] footprint,double[] tilt){}
    static WoodlandTrees.Root root(long seed,TreeScatter.Point point,DrainageNetwork.Sample terrain,GroundEcology.Site site,boolean birchDominant){
        long v=OakBirchWoodland.mix(point.priority()^0x464F524553544147L);boolean birch=birchDominant?Long.remainderUnsigned(v>>>16,100)<94:Long.remainderUnsigned(v>>>16,10)<2;
        int cohort=(int)Long.remainderUnsigned(v>>>20,10),height=point.young()?6+(int)Long.remainderUnsigned(v,5):cohort<2?(birch?11:12)+(int)Long.remainderUnsigned(v,birch?5:6):cohort<9?(birch?15:20)+(int)Long.remainderUnsigned(v,birch?7:8):(birch?22:28)+(int)Long.remainderUnsigned(v,birch?4:7);
        // In this birch-led composition oak associates remain subcanopy recruits;
        // a rare much broader veteran oak must not dominate the local canopy by volume.
        if(birchDominant&&!birch)height=Math.min(height,12);
        return new WoodlandTrees.Root(point.x(),terrain.height()+1,point.z(),birch,height,(int)(v>>>32),birchDominant?2:1);
    }
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){return tree(root,false);}
    /** {@code scaffold} selects {@link BroadleafCrowns} for ordinary-forest oaks only; every other caller and family keeps the published crown. */
    public static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root,boolean scaffold){
        int wood=root.birch()?4:1,h=root.height();long seed=OakBirchWoodland.mix(root.variant()^0x464F52455354464FL);double angle=unit(seed^11)*Math.PI*2;
        byte[] grid=new byte[PLANE*MAX_HEIGHT],distance=new byte[grid.length];Arrays.fill(distance,(byte)7);var crowns=new ArrayList<Crown>();
        int lean=(root.family()==2||root.family()==TemperateForestVariants.BIRCH_FAMILY)?(int)Long.remainderUnsigned(seed,3):1+(int)Long.remainderUnsigned(seed,2),fork=Math.max(2,(int)(h*(root.birch()?.42+.12*unit(seed^19):.30+.20*unit(seed^19))));
        var stem=new Node((int)Math.round(Math.cos(angle)*lean),fork,(int)Math.round(Math.sin(angle)*lean));
        segment(grid,new Node(0,0,0),new Node(0,Math.min(3,fork),0),wood);segment(grid,new Node(0,Math.min(3,fork),0),stem,wood);
        if((root.family()==2||root.family()==TemperateForestVariants.BIRCH_FAMILY)&&root.birch()){
            birchCrown(grid,crowns,stem,h,seed,angle,wood);
        }else if(root.birch()){
            var top=end(stem,angle,.7,h-2);segment(grid,stem,top,wood);
            int arms=h<11?4:4+(int)Long.remainderUnsigned(seed>>>8,3);
            for(int n=0;n<arms;n++){
                long v=OakBirchWoodland.mix(seed+n*1297);int by=Math.min(h-3,(int)(h*(h<11?.15+.55*(n+unit(v))/arms:.30+.45*(n+unit(v))/arms)));var start=attach(grid,Math.max(1,by));
                var tip=end(start,angle+n*2.39996+(unit(v^13)-.5),1.3+unit(v^29)*(h<11?1.2:1.8),Math.min(h-2,by+1+(root.family()==2&&h<11&&n==0?0:(int)Long.remainderUnsigned(v>>>8,3))));
                branch(grid,start,tip,wood,v);crowns.add(crown((tip.x()+start.x())*.5,tip.y()+.5,(tip.z()+start.z())*.5,1.8+unit(v)*.8,2+unit(v^43)*1.4,1.7+unit(v^79),v));
            }
            crowns.add(crown(top.x(),h-2,top.z(),2.0+unit(seed^41)*.6,2.6,1.9+unit(seed^73)*.7,seed^71));
            if(h>=15&&(seed&3)==0){var start=attach(grid,h/2);var tip=end(start,angle+1.7,2.5,h-4);branch(grid,start,tip,wood,seed^127);crowns.add(crown(tip.x(),h-4,tip.z(),2.3,3.1,2,seed^131));}
        }else if(h<11){
            var top=end(stem,angle,.6,h-2);segment(grid,stem,top,wood);int arms=3+(int)Long.remainderUnsigned(seed>>>8,3);
            for(int n=0;n<arms;n++){long v=OakBirchWoodland.mix(seed+n*997);int by=1+(int)((h-4)*(n+.3+.4*unit(v))/arms);var start=attach(grid,by);var tip=end(start,angle+n*2.39996,1.3+unit(v^17)*1.6,Math.min(h-2,by+1+(int)Long.remainderUnsigned(v>>>20,2)));branch(grid,start,tip,wood,v);crowns.add(crown(tip.x()*.8,tip.y()+.4,tip.z()*.8,1.9+unit(v)*.7,1.8+unit(v^37),1.8+unit(v^53),v));}
            crowns.add(crown(top.x(),h-2,top.z(),2.1,2.0,2.2,seed^151));
        }else if(scaffold&&root.family()==1){
            scaffoldOak(grid,crowns,stem,h,seed,angle,wood);
        }else{
            // Mature oaks have unequal scaffold limbs; their forks and crowns are not scaled sapling balls.
            var leader=end(stem,angle,.5,h-2);segment(grid,stem,leader,wood);int arms=2+(int)Long.remainderUnsigned(seed>>>8,h<17?2:4);double s=Math.max(1,h/22.0);
            for(int n=0;n<arms;n++){
                long v=OakBirchWoodland.mix(seed+n*1237);int by=Math.max(3,Math.min(h-5,(int)(h*(.28+.28*(n+unit(v))/arms))));var start=attach(grid,by);
                var tip=end(start,angle+n*2.39996+(unit(v^13)-.5)*.8,(2.5+unit(v^29)*(h<17?1.2:2.5))*s,Math.max(by+1,h-4-(int)Long.remainderUnsigned(v>>>16,h<17?3:6)));
                branch(grid,start,tip,wood,v);crowns.add(crown((tip.x()+start.x())*.48,tip.y()+.6,(tip.z()+start.z())*.48,(3+unit(v)*1.4)*s,(2.6+unit(v^43)*1.5)*Math.sqrt(s),(2.7+unit(v^79)*1.7)*s,v));
            }
            crowns.add(crown(leader.x()*.8,h-3,leader.z()*.8,(3.1+unit(seed^41))*s,(2.6+unit(seed^73))*Math.sqrt(s),(3+unit(seed^101))*s,seed^151));
            if(h>=20&&(seed&3)==1){var start=attach(grid,Math.max(3,h/3));var tip=end(start,angle+2,3.5,h/2+1);branch(grid,start,tip,wood,seed^181);crowns.add(crown(tip.x()*.8,tip.y()+.8,tip.z()*.8,2.8,2.5,3.0,seed^191));}
        }
        // Each crown writes the same leaf code. Its footprint and tilt are
        // already exact per horizontal cell; nonnegative dy^2 cannot reduce q.
        // Rasterize only that crown's conservative vertical interval, preserving
        // the original arithmetic and edge test. One-cell padding covers rounding.
        for(var crown:crowns)for(int z=-R;z<=R;z++)for(int x=-R;x<=R;x++){
            int horizontal=(z+R)*S+x+R;double footprint=crown.footprint()[horizontal];if(footprint>1)continue;
            double center=crown.y()+crown.tilt()[horizontal];
            int first=Math.max(1,(int)Math.floor(center-crown.ry())-1),last=Math.min(MAX_HEIGHT-1,(int)Math.ceil(center+crown.ry())+1);
            for(int y=first;y<=last;y++){
                int i=index(x,y,z);if(grid[i]!=0)continue;
                double q=footprint,dy=(y-crown.y()-crown.tilt()[horizontal])/crown.ry();q+=dy*dy;if(q>1)continue;
                long edge=OakBirchWoodland.mix(seed^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL^y*0x165667B19E3779F9L);if(q<.65||(edge&15)>0)grid[i]=19;
            }
        }
        if((root.family()==2||root.family()==TemperateForestVariants.BIRCH_FAMILY)&&root.birch())clotheBirchTips(grid,h,seed);
        int[] queue=new int[grid.length];int head=0,tail=0;for(int i=0;i<grid.length;i++)if(grid[i]>=1&&grid[i]<=6){distance[i]=0;queue[tail++]=i;}
        while(head<tail){int i=queue[head++],x=i%S-R,z=i/S%S-R,y=i/PLANE;if(distance[i]>=6)continue;for(var step:DIR){int nx=x+step[0],ny=y+step[1],nz=z+step[2];if(Math.abs(nx)>R||Math.abs(nz)>R||ny<0||ny>=MAX_HEIGHT)continue;int next=index(nx,ny,nz);if(grid[next]==19&&distance[next]>distance[i]+1){distance[next]=(byte)(distance[i]+1);queue[tail++]=next;}}}
        var result=new ArrayList<WoodlandTrees.Voxel>();for(int y=0;y<MAX_HEIGHT;y++)for(int z=-R;z<=R;z++)for(int x=-R;x<=R;x++){int i=index(x,y,z),code=grid[i];if(code==19)code=distance[i]>6?0:(root.birch()?12:6)+distance[i];if(code!=0)result.add(new WoodlandTrees.Voxel(root.x()+x,root.y()+y,root.z()+z,code));}return result;
    }
    /** Decurrent oak: the trunk divides below the apex into oblique limbs whose tips carry separate lobes.
     * Crown top, spread bounds and attachment rules match the published oak; only the arrangement differs. */
    private static void scaffoldOak(byte[] grid,List<Crown> crowns,Node stem,int h,long seed,double angle,int wood){
        double s=Math.max(1,h/22.0),r=Math.sqrt(s);
        int floor=Math.max(stem.y()+1,3),leaderTop=Math.max(floor+2,(int)Math.round(h*(.60+.10*unit(seed^307))));
        var leader=end(stem,angle+.6,.8,leaderTop);segment(grid,stem,leader,wood);
        int arms=3+(h>=17?1:0)+(int)Long.remainderUnsigned(seed>>>8,h<17?1:3);
        double lowest=Math.max(floor+2,h*.54);
        for(int n=0;n<arms;n++){
            long v=OakBirchWoodland.mix(seed+n*1237);double a=angle+1.2+n*2.39996+(unit(v^13)-.5)*1.1;
            int tipY=(int)Math.round(lowest+(h-5-lowest)*unit(v^7));
            double reach=(2.6+unit(v^29)*2.6)*r,rise=reach*(.55+unit(v^31)*1.0);
            int by=Math.clamp(tipY-(int)Math.round(rise),floor,leaderTop-1);var start=attach(grid,by);
            var tip=end(start,a,reach,Math.max(by+1,tipY));branch(grid,start,tip,wood,v);
            double lobe=1.0+.25*unit(v^47);
            crowns.add(crown(tip.x()*.9,tip.y()+.5,tip.z()*.9,(2.5+unit(v)*.9)*r*lobe,(2.2+unit(v^43)*.8)*r,(2.4+unit(v^79)*1.0)*r*lobe,v));
            if(reach>=3.7*r&&(v&3)!=0){
                double b=a+(((v>>>5)&1)==0?1:-1)*(.7+unit(v^53)*.6);var twig=end(tip,b,1.6,Math.min(h-4,tip.y()+((v&8)==0?0:1)));branch(grid,tip,twig,wood,v^277);
                crowns.add(crown(twig.x()*.9,twig.y()+.4,twig.z()*.9,1.9+unit(v^59)*.5,1.7+unit(v^61)*.4,1.9+unit(v^67)*.5,v^331));
            }
        }
        // The apex rides a short limb off the divided leader, so no straight pole runs through the crown.
        var apex=end(leader,angle+2.2+unit(seed^313)*2.0,1.0+unit(seed^317)*1.2,h-2);branch(grid,leader,apex,wood,seed^337);
        crowns.add(crown(apex.x()*.9,h-2.6,apex.z()*.9,(2.7+unit(seed^41)*.7)*r,(2.3+unit(seed^73)*.6)*r,(2.7+unit(seed^101)*.7)*r,seed^151));
    }
    /** Birch-led stands use one asymmetric oval envelope,not a vertical stack of limb-tip balls. */
    private static void birchCrown(byte[] grid,List<Crown> crowns,Node stem,int h,long seed,double angle,int wood){
        double bottom=h<11?1.0:h*(.32+.12*unit(seed^211)),top=h-1.0,cy=(bottom+top)*.5,ry=(top-bottom)*.5;
        double width=h<11?2.3+.7*unit(seed^223):3.0+Math.min(h,22)*.07+unit(seed^227);
        var leader=end(stem,angle,.6,h-2);segment(grid,stem,leader,wood);
        double cx=stem.x()*.6+Math.cos(angle+1.3)*.6,cz=stem.z()*.6+Math.sin(angle+1.3)*.6;
        crowns.add(crown(cx,cy,cz,width*.72,ry*.86,width*(.57+.20*unit(seed^229)),seed^233));
        // An overlapping side lobe changes balance and spread without making discrete tiers.
        double side=angle+1.1+unit(seed^239)*2.8;
        crowns.add(crown(cx+Math.cos(side)*2.5,cy-.08*ry,cz+Math.sin(side)*2.5,width*.60,ry*.68,width*.58,seed^241));
        int arms=h<11?3+(int)Long.remainderUnsigned(seed>>>8,2):4+(int)Long.remainderUnsigned(seed>>>8,3);
        for(int n=0;n<arms;n++){
            long v=OakBirchWoodland.mix(seed+n*1297);int by=Math.max(1,(int)Math.round(bottom+(top-bottom)*(.04+.42*unit(v^251))));
            var start=attach(grid,Math.min(h-3,by));double a=angle+n*2.39996+(unit(v^257)-.5)*1.4;
            int tipY=Math.min(h-2,by+1+(int)Math.round((h-by-3)*(.22+.44*unit(v^263))));
            var tip=end(start,a,width*(.44+.35*unit(v^269)),tipY);branch(grid,start,tip,wood,v);
            // Upper branch tips carry overlapping foliage,so each crown follows its scaffold.
            crowns.add(crown(tip.x()*.88,tip.y()+.4,tip.z()*.88,1.9+unit(v^281),2.4+unit(v^283)*1.1,1.8+unit(v^293)*.9,v^307));
            if(h>=15&&(v&1)==0){var twig=end(tip,a+.7+(unit(v^271)-.5),1.2,Math.min(h-2,tip.y()+1));branch(grid,tip,twig,wood,v^277);}
        }
    }
    /** Small terminal twig envelopes cover live cut ends;the main crown/scaffold is unchanged. */
    private static void clotheBirchTips(byte[] grid,int height,long seed){
        var tips=new ArrayList<Node>();
        for(int y=2;y<MAX_HEIGHT-1;y++)for(int z=-R;z<=R;z++)for(int x=-R;x<=R;x++){
            int code=grid[index(x,y,z)];if(code<4||code>6)continue;int woody=0;
            for(var d:DIR){int nx=x+d[0],ny=y+d[1],nz=z+d[2];if(Math.abs(nx)>R||Math.abs(nz)>R||ny<0||ny>=MAX_HEIGHT)continue;int near=grid[index(nx,ny,nz)];if(near>=1&&near<=6)woody++;}
            int[] axis=code==4?DIR[2]:code==5?DIR[0]:DIR[4];boolean cut=false;
            if(y>=Math.max(3,(int)(height*.45)))for(int sign:new int[]{-1,1}){
                int nx=x+axis[0]*sign,ny=y+axis[1]*sign,nz=z+axis[2]*sign;
                if(Math.abs(nx)<=R&&Math.abs(nz)<=R&&ny>=0&&ny<MAX_HEIGHT&&grid[index(nx,ny,nz)]==0)cut=true;
            }
            if(woody<=1||cut)tips.add(new Node(x,y,z));
        }
        for(var tip:tips){
            long v=OakBirchWoodland.mix(seed^tip.x()*1297L^tip.y()*1237L^tip.z()*997L);
            double rx=1.5+unit(v)*.4,ry=1.8+unit(v^313)*.4,rz=1.5+unit(v^317)*.4;
            for(int dy=-2;dy<=2;dy++)for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){
                int x=tip.x()+dx,y=tip.y()+dy,z=tip.z()+dz;if(Math.abs(x)>R||Math.abs(z)>R||y<1||y>=MAX_HEIGHT)continue;
                double q=dx*dx/(rx*rx)+(dy+.15)*(dy+.15)/(ry*ry)+dz*dz/(rz*rz);
                if(q<=1&&grid[index(x,y,z)]==0)grid[index(x,y,z)]=19;
            }
        }
    }
    private static Crown crown(double x,double y,double z,double rx,double ry,double rz,long seed){
        x=Math.clamp(x,-4,4);z=Math.clamp(z,-4,4);double cap=Math.min(R-Math.abs(x),R-Math.abs(z))-.6;rx=Math.min(rx,cap);rz=Math.min(rz,cap);y=Math.min(y,MAX_HEIGHT-ry-1);
        double a=unit(seed^59)*Math.PI*2,cs=Math.cos(a),sn=Math.sin(a),slopeX=(unit(seed^43)-.5)*.5,slopeZ=(unit(seed^79)-.5)*.5;double[] q=new double[PLANE],tilt=new double[PLANE];
        for(int iz=-R;iz<=R;iz++)for(int ix=-R;ix<=R;ix++){double dx=ix-x,dz=iz-z,u=dx*cs+dz*sn,v=-dx*sn+dz*cs;long lump=OakBirchWoodland.mix(seed^Math.floorDiv(ix,2)*0x165667B19E3779F9L^Math.floorDiv(iz,2)*0xD1B54A32D192ED03L);double lobe=.91+.16*unit(lump);int i=(iz+R)*S+ix+R;q[i]=(u*u/(rx*rx)+v*v/(rz*rz))/lobe;tilt[i]=dx*slopeX+dz*slopeZ;}
        return new Crown(y,ry,q,tilt);
    }
    private static Node end(Node a,double angle,double length,int y){return new Node((int)Math.clamp(Math.round(a.x()+Math.cos(angle)*length),-5,5),y,(int)Math.clamp(Math.round(a.z()+Math.sin(angle)*length),-5,5));}
    private static void branch(byte[] grid,Node a,Node b,int wood,long seed){var elbow=new Node((int)Math.round(a.x()+(b.x()-a.x())*.55),Math.min(b.y(),a.y()+Math.max(1,(b.y()-a.y())/2)+((seed&1)==0?1:0)),(int)Math.round(a.z()+(b.z()-a.z())*.55));segment(grid,a,elbow,wood);segment(grid,elbow,b,wood);}
    private static void segment(byte[] grid,Node a,Node b,int wood){int x=a.x(),y=a.y(),z=a.z(),dx=Math.abs(b.x()-x),dy=Math.abs(b.y()-y),dz=Math.abs(b.z()-z),steps=Math.max(dy,Math.max(dx,dz)),axis=wood+(dy>=dx&&dy>=dz?0:dx>=dz?1:2);put(grid,x,y,z,axis);for(int n=1;n<=steps;n++){int tx=(int)Math.round(a.x()+(b.x()-a.x())*n/(double)steps),ty=(int)Math.round(a.y()+(b.y()-a.y())*n/(double)steps),tz=(int)Math.round(a.z()+(b.z()-a.z())*n/(double)steps);while(y!=ty){y+=Integer.signum(ty-y);put(grid,x,y,z,axis);}while(x!=tx){x+=Integer.signum(tx-x);put(grid,x,y,z,axis);}while(z!=tz){z+=Integer.signum(tz-z);put(grid,x,y,z,axis);}}}
    private static void put(byte[] grid,int x,int y,int z,int code){int i=index(x,y,z);if(grid[i]==0)grid[i]=(byte)code;}
    private static Node attach(byte[] grid,int y){for(int r=0;r<=R;r++)for(int z=-r;z<=r;z++)for(int x=-r;x<=r;x++)if(Math.abs(x)+Math.abs(z)==r&&grid[index(x,y,z)]>0&&grid[index(x,y,z)]<=6)return new Node(x,y,z);throw new IllegalArgumentException("No trunk for forest branch");}
    private static double unit(long v){return(OakBirchWoodland.mix(v)>>>11)*0x1.0p-53;}
    private static int index(int x,int y,int z){return y*PLANE+(z+R)*S+x+R;}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private ForestTrees(){}
}
