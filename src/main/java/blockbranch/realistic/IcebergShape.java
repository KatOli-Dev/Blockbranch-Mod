package blockbranch.realistic;

/** Static calved-ice solid. Shape and flotation are discrete, seed-local and query-order independent. */
public final class IcebergShape {
    public static final int RADIUS=128,SIDE=257;
    private final short[] bottoms=new short[SIDE*SIDE],tops=new short[SIDE*SIDE];
    public final int type,volume,submerged,freeboard,draft,footprint;
    public IcebergShape(long seed){
        long a=OakBirchWoodland.mix(seed),b=OakBirchWoodland.mix(a),c=OakBirchWoodland.mix(b);
        type=(int)Long.remainderUnsigned(a,3);double rx=36+50*unit(a),rz=24+42*unit(b),height=90+110*unit(c),angle=6.28318530718*unit(OakBirchWoodland.mix(c));
        java.util.Arrays.fill(bottoms,Short.MAX_VALUE);java.util.Arrays.fill(tops,Short.MIN_VALUE);
        int faces=5+(int)Long.remainderUnsigned(a>>>24,3);
        double[] nx=new double[faces],nz=new double[faces],offset=new double[faces],shoulder=new double[faces],cutCentre=new double[faces],cutWidth=new double[faces],cutDepth=new double[faces];
        for(int j=0;j<faces;j++){long h=OakBirchWoodland.mix(c+j*0x165667B19E3779F9L);double direction=j*Math.PI*2/faces+.28*(unit(h)-.5);nx[j]=Math.cos(direction);nz[j]=Math.sin(direction);offset[j]=.70+.25*unit(OakBirchWoodland.mix(h+71));
            // A minority of fresh calving faces; other margins have unequal
            // weathered shoulders. These are body-scale features, not voxel noise.
            shoulder[j]=j%3==0?0:.18+.42*unit(OakBirchWoodland.mix(h+83));
            cutCentre[j]=-.4+.8*unit(OakBirchWoodland.mix(h+97));
            cutWidth[j]=.14+.24*unit(OakBirchWoodland.mix(h+101));
            cutDepth[j]=j%2==0?.10+.24*unit(OakBirchWoodland.mix(h+103)):0;
        }
        int total=0,cells=0;
        for(int z=-RADIUS;z<=RADIUS;z++)for(int x=-RADIUS;x<=RADIUS;x++){
            if(x*x+z*z>RADIUS*RADIUS)continue;
            double u=(x*Math.cos(angle)+z*Math.sin(angle))/rx,v=(-x*Math.sin(angle)+z*Math.cos(angle))/rz;
            double radial=0;
            for(int j=0;j<faces;j++)radial=Math.max(radial,(nx[j]*u+nz[j]*v)/offset[j]);
            if(radial>=1)continue;
            double root=Math.sqrt(1-radial*radial);
            double cap=type==0?.30+.035*u-.025*v:type==1?.34+.16*u-.08*v:.39-.20*Math.abs(u+.18*v)+.04*v;
            cap=Math.min(cap,.43-.10*u+.06*v);
            for(int j=0;j<faces;j++){
                double inward=offset[j]-nx[j]*u-nz[j]*v;
                double along=-nz[j]*u+nx[j]*v;
                // A finite inward fracture lowers one localized margin into a
                // saddle. Its tapered ends avoid periodic full-height fluting.
                double notch=Math.max(0,1-Math.abs(along-cutCentre[j])/cutWidth[j]);
                double edge=inward-cutDepth[j]*notch*notch;
                if(shoulder[j]>0){
                    double t=Math.max(0,Math.min(1,edge/shoulder[j]));
                    double blend=t*t*(3-2*t);
                    cap=Math.min(cap,-.08+.55*blend);
                }else if(edge<0)cap=Math.min(cap,-.08+edge);
            }
            // A few unequal ablation hollows break the roof/shoulder silhouette.
            // Each has finite support at tens-of-metres scale; no periodic noise
            // or full-length trench is applied across every face.
            double weathering=0;
            for(int j=0;j<3;j++){
                long h=OakBirchWoodland.mix(seed+j*0xD1B54A32D192ED03L);
                double cx=-.65+1.3*unit(h),cz=-.65+1.3*unit(OakBirchWoodland.mix(h));
                double length=.22+.32*unit(OakBirchWoodland.mix(h+13));
                double width=.18+.22*unit(OakBirchWoodland.mix(h+29));
                double turn=6.28318530718*unit(OakBirchWoodland.mix(h+37));
                double du=u-cx,dv=v-cz;
                double along=(du*Math.cos(turn)+dv*Math.sin(turn))/length;
                double across=(-du*Math.sin(turn)+dv*Math.cos(turn))/width;
                double bowl=Math.max(0,1-along*along-across*across);
                weathering=Math.max(weathering,(.08+.12*unit(OakBirchWoodland.mix(h+41)))*bowl*bowl);
            }
            cap-=weathering;
            int low=(int)Math.ceil(-height*.68*root),high=(int)Math.floor(height*cap);
            if(low>high)continue;int i=(z+RADIUS)*SIDE+x+RADIUS;
            bottoms[i]=(short)low;tops[i]=(short)high;total+=high-low+1;cells++;
        }
        // Choose a static waterline by actual voxel volume, rather than inferring
        // draft from the visible height. Approximate dense glacial ice flotation.
        int bestShift=0,bestSubmerged=0;double error=Double.POSITIVE_INFINITY;
        for(int shift=-256;shift<=256;shift++){
            int wet=0;for(int i=0;i<tops.length;i++)if(bottoms[i]!=Short.MAX_VALUE)wet+=Math.max(0,Math.min(tops[i]+shift,-1)-(bottoms[i]+shift)+1);
            double e=Math.abs(wet/(double)total-.90);if(e<error){error=e;bestShift=shift;bestSubmerged=wet;}
        }
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(int i=0;i<tops.length;i++)if(bottoms[i]!=Short.MAX_VALUE){bottoms[i]+=bestShift;tops[i]+=bestShift;low=Math.min(low,bottoms[i]);high=Math.max(high,tops[i]);}
        footprint=cells;volume=total;submerged=bestSubmerged;draft=-low;freeboard=high+1;
        if(error>.025||draft>256||freeboard<1||freeboard>128)throw new IllegalStateException("Invalid iceberg flotation");
    }
    public int bottom(int x,int z){if(Math.abs(x)>RADIUS||Math.abs(z)>RADIUS)return Integer.MIN_VALUE;int v=bottoms[(z+RADIUS)*SIDE+x+RADIUS];return v==Short.MAX_VALUE?Integer.MIN_VALUE:v;}
    public int top(int x,int z){if(bottom(x,z)==Integer.MIN_VALUE)return Integer.MIN_VALUE;return tops[(z+RADIUS)*SIDE+x+RADIUS];}
    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
}
