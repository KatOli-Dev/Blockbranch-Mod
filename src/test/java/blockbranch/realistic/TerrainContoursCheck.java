package blockbranch.realistic;

/** Original reported sites and negative controls; bounded routed-edge changes and continuous landform response. */
public final class TerrainContoursCheck {
    private static final int[][] SITES={{1359388,-1694456},{-1332024,-1900024},{-17,-17}};
    // Short exact runs remain diagnostic: counting the fixed waterline as an artifact
    // rewarded the failed hard-clamp prototype for flattening banks to the datum.
    static int lines(int[][] h){
        int n=0;
        for(int z=9;z<119;z++)for(int x=9;x<119;x++)for(int[]v:new int[][]{{1,0},{0,1},{1,1},{1,-1}}){
            int dx=v[0],dz=v[1],a=h[z][x],b=h[z+dx][x-dz];if(a==b)continue;
            boolean same=true;for(int i=1;i<8;i++)if(h[z+dz*i][x+dx*i]!=a||h[z+dz*i+dx][x+dx*i-dz]!=b){same=false;break;}if(same)n++;
        }return n;
    }
    private static double energy(DrainageNetwork d,int[]p,boolean detailed){
        double ax=-.4289525460777618,az=.9033270245118351;
        if(p[0]<0){double gx=d.structuralSample(p[0]+64,p[1]).height()-d.structuralSample(p[0]-64,p[1]).height(),gz=d.structuralSample(p[0],p[1]+64).height()-d.structuralSample(p[0],p[1]-64).height(),length=Math.hypot(gx,gz);ax=gz/length;az=-gx/length;}
        double along=0,across=0;
        for(int z=-56;z<56;z+=2)for(int x=-56;x<56;x+=2){
            int wx=p[0]+(int)Math.round(ax*x-az*z),wz=p[1]+(int)Math.round(az*x+ax*z);var q=sample(d,wx,wz,detailed);boolean wet=q.waterLevel()>q.height(),pure=true;int[]h=new int[4];int j=0;
            for(double[]v:new double[][]{{ax*8,az*8},{-ax*8,-az*8},{-az*8,ax*8},{az*8,-ax*8}}){var t=sample(d,wx+(int)Math.round(v[0]),wz+(int)Math.round(v[1]),detailed);if((t.waterLevel()>t.height())!=wet)pure=false;h[j++]=t.height();}
            if(pure){along+=Math.pow(h[0]-h[1],2);across+=Math.pow(h[2]-h[3],2);}
        }
        require(across>1000,"Original sloping-site witness");return along/across;
    }
    private static DrainageNetwork.Sample sample(DrainageNetwork d,int x,int z,boolean detailed){return detailed?d.sample(x,z):d.structuralSample(x,z);}
    private static int nearby(int[][]sum,int x,int z){return sum[z+17][x+17]-sum[z][x+17]-sum[z+17][x]+sum[z][x];}
    private static void datum(DrainageNetwork d,TerrainShape shape){
        var field=new TerrainRelief(d::structuralSample);int size=272,legacyFlat=0,newFlat=0;
        int[][]oldH=new int[size][size],newH=new int[size][size],oldW=new int[size][size],newW=new int[size][size],oldWet=new int[size+1][size+1],newWet=new int[size+1][size+1];
        for(int z=0;z<size;z++)for(int x=0;x<size;x++){
            var a=d.structuralSample(1359252+x,-1694592+z);var b=d.sample(1359252+x,-1694592+z);
            oldH[z][x]=a.height();newH[z][x]=b.height();oldW[z][x]=a.waterLevel();newW[z][x]=b.waterLevel();
            oldWet[z+1][x+1]=oldWet[z+1][x]+oldWet[z][x+1]-oldWet[z][x]+(a.waterLevel()==64?1:0);
            newWet[z+1][x+1]=newWet[z+1][x]+newWet[z][x+1]-newWet[z][x]+(b.waterLevel()==64?1:0);
        }
        for(int z=8;z<264;z++)for(int x=8;x<264;x++){
            if(oldW[z][x]!=DrainageNetwork.DRY||oldH[z][x]<=63||oldH[z][x]>76)continue;
            double cap=Math.min(26,1+(oldH[z][x]-63)*.65);
            int legacy=Math.max(63,(int)Math.round(oldH[z][x]+Math.clamp(shape.surfaceRelief(1359252+x,-1694592+z,field.slope(1359252+x,-1694592+z)),-cap,cap)));
            // A narrow natural waterline bank is expected. The failed hard clamp
            // created new datum plateaus far from a wetted edge instead.
            if(legacy==63&&nearby(oldWet,x-8,z-8)==0)legacyFlat++;
            if(newW[z][x]==DrainageNetwork.DRY&&newH[z][x]==63&&nearby(newWet,x-8,z-8)==0)newFlat++;
        }
        require(legacyFlat>100,"Hard-datum-clamp negative control must reproduce broad inland shelves");
        require(newFlat==0,"No newly flattened dry datum plateau away from the actual waterline");
        System.out.printf("DATUM_SHELF_NEGATIVE_CONTROL legacyFarFromWater=%d current=%d%n",legacyFlat,newFlat);
    }
    private static void strengthTransition(DrainageNetwork d,TerrainShape shape){
        var field=new TerrainRelief(d::structuralSample);int unsafe=0,current=0;
        for(int x=-1640820;x<-1640804;x++){
            int z=-1702090;
            int a=(int)Math.round(d.structuralSample(x,z).height()+shape.surfaceRelief(x,z,field.slope(x,z)));
            int b=(int)Math.round(d.structuralSample(x+1,z).height()+shape.surfaceRelief(x+1,z,field.slope(x+1,z)));
            unsafe=Math.max(unsafe,Math.abs(a-b));current=Math.max(current,Math.abs(d.sample(x,z).height()-d.sample(x+1,z).height()));
        }
        require(unsafe>=5&&current<=4,"Nonlinear strength amplification negative control and fixed original witness");
        System.out.printf("STRENGTH_INTERPOLATION_NEGATIVE_CONTROL unsafeStep=%d currentStep=%d%n",unsafe,current);
    }
    private static void reservationEdges(DrainageNetwork d){
        int cases=0,seen=0,maxExtra=0;
        for(int i=0;i<d.size()&&cases<16;i++){
            if(!d.river(i)||Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000||seen++%997!=0)continue;
            for(double t:new double[]{.25,.5,.75}){
                double x=d.edgeX(i,t),z=d.edgeZ(i,t),ax=d.edgeX(i,t+.0001)-d.edgeX(i,t-.0001),az=d.edgeZ(i,t+.0001)-d.edgeZ(i,t-.0001),length=Math.hypot(ax,az);if(length==0)continue;ax/=length;az/=length;
                var channel=d.channelAt((int)Math.round(x),(int)Math.round(z));if(channel.edge()<0)continue;
                for(double radius:new double[]{channel.halfWidth()*3+48,channel.halfWidth()+128})for(int sign:new int[]{-1,1})for(int off=-2;off<=2;off++){
                    int wx=(int)Math.round(x-az*(radius+off)*sign),wz=(int)Math.round(z+ax*(radius+off)*sign);
                    for(int[]v:new int[][]{{1,0},{0,1}}){int old=Math.abs(d.structuralSample(wx,wz).height()-d.structuralSample(wx+v[0],wz+v[1]).height()),now=Math.abs(d.sample(wx,wz).height()-d.sample(wx+v[0],wz+v[1]).height());maxExtra=Math.max(maxExtra,now-old);require(now<=old+4,"New cliff at channel/guard reservation edge");}
                }
            }cases++;
        }
        require(cases>=8,"Representative routed reservation edges");System.out.printf("RESERVATION_EDGE_CONTINUITY cases=%d maxExtraStep=%d%n",cases,maxExtra);
    }
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            var shape=new TerrainShape(seed);var d=new DrainageNetwork(shape,seed);
            for(int[]p:SITES){
                int[][]old=new int[128][128],fresh=new int[128][128];int changed=0,maxDelta=0,maxOldStep=0,maxNewStep=0;long[]steps=new long[16];
                for(int z=0;z<128;z++)for(int x=0;x<128;x++){
                    double ax=p[0]>0?-.4289525460777618:1,az=p[0]>0?.9033270245118351:0;
                    int wx=p[0]+(int)Math.round(ax*(x-64)-az*(z-64)),wz=p[1]+(int)Math.round(az*(x-64)+ax*(z-64));
                    var a=d.structuralSample(wx,wz);var b=d.sample(wx,wz);
                    RiverMarginCheck.verifyWater(d,wx,wz,a,b);
                    require(b.equals(d.sample(wx,wz)),"Request order changed geometry");
                    old[z][x]=a.height();fresh[z][x]=b.height();if(a.height()!=b.height())changed++;maxDelta=Math.max(maxDelta,Math.abs(a.height()-b.height()));
                    require(b.height()>TerrainShape.FLOOR&&b.height()<TerrainShape.CEILING,"Terrain envelope");
                    for(int[]v:new int[][]{{1,0},{0,1}}){
                        int oldStep=Math.abs(a.height()-d.structuralSample(wx+v[0],wz+v[1]).height()),newStep=Math.abs(b.height()-d.sample(wx+v[0],wz+v[1]).height());
                        maxOldStep=Math.max(maxOldStep,oldStep);maxNewStep=Math.max(maxNewStep,newStep);steps[Math.min(15,newStep)]++;
                        require(newStep<=oldStep+4,"Excessive adjacent-block roughness at original site");
                    }
                }
                require(maxDelta<=120,"Landform plus bank profile bound");
                System.out.printf("SURFACE_STEP_DIAGNOSTIC seed=%d site=%d,%d oldMax=%d newMax=%d histogram=%s%n",seed,p[0],p[1],maxOldStep,maxNewStep,java.util.Arrays.toString(steps));
                if(seed==424242&&p[0]!=-17){
                    double before=energy(d,p,false),after=energy(d,p,true);
                    require(before<.025,"Original long-contour negative control missing");
                    require(after>before*2&&after>Math.pow(Math.tan(Math.toRadians(10)),2),"Contour-parallel regularity not reduced");
                    System.out.printf("CONTOUR_DIRECTIONAL_ENERGY site=%d,%d old=%.6f new=%.6f%n",p[0],p[1],before,after);
                }
                System.out.printf("CONTOUR_DIAGNOSTIC seed=%d site=%d,%d straight8 old=%d new=%d changed=%d maxDelta=%d%n",seed,p[0],p[1],lines(old),lines(fresh),changed,maxDelta);
            }
            reservationEdges(d);if(seed==424242){datum(d,shape);strengthTransition(d,shape);
                for(int[] p:new int[][]{{84000,-572000},{1428000,-428000}}){var old=d.structuralSample(p[0],p[1]);var current=d.sample(p[0],p[1]);require(old.waterLevel()==DrainageNetwork.DRY&&current.waterLevel()==DrainageNetwork.DRY&&old.basin()==current.basin()&&old.closed()==current.closed(),"Fixed dry coastal catchment labels survive shoulder changes");}
            }
        }
        System.out.println("TERRAIN_CONTOURS_NEGATIVE_CONTROLS_BOUNDED_WATER_EDGES_PASS");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
