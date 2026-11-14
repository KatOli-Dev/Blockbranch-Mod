package blockbranch.realistic;

import java.util.*;

/**
 * Saved vegetation flag (clear definitions 16385..32768) for mature humid-woodland stems: {@link HumidTrees} roots of at least
 * {@link #MATURE_HEIGHT} replace the published upright pole under a ring of equal cardinal lobes and two fixed upper and lower tiers with a
 * leader that leans slightly, 7..8 limbs at golden-ratio-spaced heights and golden-angle azimuths, and a separate unequal, elongated and tilted foliage
 * lobe on each limb tip and the leader top. Heights, roots, cohorts, climate, placement and young stems (8..12) are unchanged. Every
 * voxel is held within {@link #EXTENT} blocks of the root horizontally (the generation halo is 6) and at or below the published structural top
 * (root height + 1): lobes are scaled toward that room (never below half size) and any remaining cells outside it are cut by a hard clip, so some
 * lobes end flat at the bound. A local scratch measurement over the 1918 mature stems of five sites found 3562 of 1,353,960 placed lobe
 * cells (0.26%) beyond it, on 417 of those stems (about 22%); it is a finite uncommitted sample, not a bound. Tree-clear plans replay these trees from the saved generator, so the bit travels with the saved vegetation flags; plans
 * without it replay the published crown and never-explored chunks of an older world keep it.
 */
public final class HumidCrowns {
    public static final int FLAG=8192,MATURE_HEIGHT=21,EXTENT=6;
    public static boolean enabled(int flags){return (flags&FLAG)!=0;}
    private static double unit(long v){return (OakBirchWoodland.mix(v)>>>11)*0x1.0p-53;}

    static List<WoodlandTrees.Voxel> tree(WoodlandTrees.Root root){
        if(root.height()<MATURE_HEIGHT||root.height()>WoodlandTrees.HUMID_HEIGHT-2)throw new IllegalArgumentException("Humid crown height outside "+MATURE_HEIGHT+".."+(WoodlandTrees.HUMID_HEIGHT-2)+": "+root.height());
        int h=root.height(),size=WoodlandTrees.RADIUS*2+1,height=WoodlandTrees.HUMID_HEIGHT;
        byte[] blocks=new byte[size*size*height];boolean[] claimed=new boolean[blocks.length];
        long seed=OakBirchWoodland.mix(root.variant()^0x48554D4944435257L);
        double angle=unit(seed^601)*Math.PI*2,lean=.6+1.4*unit(seed^602);
        int fork=Math.max(10,Math.min(h-9,(int)Math.round(h*(.50+.08*unit(seed^603)))));
        int topY=h-3;
        int[] ax=new int[topY+1],az=new int[topY+1];
        for(int y=fork;y<=topY;y++){double t=(y-fork)/(double)(topY-fork);ax[y]=(int)Math.round(Math.cos(angle)*lean*t);az[y]=(int)Math.round(Math.sin(angle)*lean*t);}
        WoodlandTrees.line(blocks,0,0,0,0,fork,0,1);
        for(int y=fork;y<topY;y++)WoodlandTrees.line(blocks,ax[y],y,az[y],ax[y+1],y+1,az[y+1],1);
        var lobes=new ArrayList<double[]>();
        // Leader top: the broadest, tallest lobe, offset by the lean.
        double ut=unit(seed^611);
        lobes.add(new double[]{ax[topY]+Math.cos(angle)*.4,h-2.4,az[topY]+Math.sin(angle)*.4,4.0+.9*ut,3.6+.8*unit(seed^612),3.0+.4*ut,angle+1.2*(unit(seed^613)-.5),.25*(unit(seed^614)-.5)});
        int limbs=7+(int)Long.remainderUnsigned(seed>>>9,2);
        for(int n=0;n<limbs;n++){
            long v=OakBirchWoodland.mix(seed+n*1237L);double u1=unit(v^13),u2=unit(v^29),u3=unit(v^47),u4=unit(v^71);
            double f=(.137+n*.6180339887+.3*(unit(v^3)-.5))%1.0;f=f<0?f+1:f;double a=angle+.9+n*2.39996+(u1-.5)*.9;
            int by=Math.max(fork,Math.min(topY-3,fork+(int)Math.round((topY-3-fork)*f)));
            double reach=(2.8+1.2*u2)*(.75+.5*Math.sin(Math.PI*f));
            int tipX=Math.max(-5,Math.min(5,(int)Math.round(ax[by]+Math.cos(a)*reach))),tipZ=Math.max(-5,Math.min(5,(int)Math.round(az[by]+Math.sin(a)*reach)));
            int tipY=Math.min(topY-1,by+(int)Math.round(reach*(.15+.45*u3)));
            WoodlandTrees.line(blocks,ax[by],by,az[by],tipX,tipY,tipZ,1);
            lobes.add(new double[]{tipX+Math.cos(a)*.6,tipY+.5,tipZ+Math.sin(a)*.6,3.4+1.1*u4,3.0+1.0*unit(v^89),2.7+.8*unit(v^97),a+(u1-.5)*.8,(u3-.5)*.9});
        }
        for(var l:lobes)lobe(blocks,claimed,root.variant(),h+1,l);
        return WoodlandTrees.support(root,size,height,blocks);
    }

    /** Elongated, tilted ellipsoid {cx,cy,cz,alongRadius,acrossRadius,verticalRadius,azimuth,tilt}, scaled toward the remaining halo room but never below half size ({@code Math.max(.5,room)}), so a lobe far off centre can still
     *  extend past {@link #EXTENT}; those cells are cut by the loop bounds and {@code maxY}, not avoided. Each cell is claimed once. */
    private static void lobe(byte[] blocks,boolean[] claimed,int variant,int maxY,double[] l){
        double cx=l[0],cy=l[1],cz=l[2],a=l[3],b=l[4],ry=l[5],az=l[6],tilt=l[7],cos=Math.cos(az),sin=Math.sin(az);
        double hx=Math.hypot(a*cos,b*sin),hz=Math.hypot(a*sin,b*cos),room=Math.min(1,Math.min((EXTENT+.4-Math.abs(cx))/hx,(EXTENT+.4-Math.abs(cz))/hz));
        if(room<1){a*=Math.max(.5,room);b*=Math.max(.5,room);}
        // The horizontal part of the distance and the along-axis coordinate do not depend on y: compute them once per (x,z), keep only the
        // cells whose horizontal part alone is at most 1, and add the vertical term per y.
        int x0=Math.max(-EXTENT,(int)Math.floor(cx-hx)),x1=Math.min(EXTENT,(int)Math.ceil(cx+hx)),z0=Math.max(-EXTENT,(int)Math.floor(cz-hz)),z1=Math.min(EXTENT,(int)Math.ceil(cz+hz));
        int[] cellX=new int[(x1-x0+1)*(z1-z0+1)],cellZ=new int[cellX.length];double[] horizontal=new double[cellX.length],along=new double[cellX.length];int cells=0;
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++){
            double dx=x-cx,dz=z-cz,u=dx*cos+dz*sin,v=-dx*sin+dz*cos,h=(u/a)*(u/a)+(v/b)*(v/b);
            if(h>1)continue;
            cellX[cells]=x;cellZ[cells]=z;horizontal[cells]=h;along[cells]=u;cells++;
        }
        for(int y=Math.max(1,(int)Math.floor(cy-ry-Math.abs(tilt)*a));y<=Math.min(maxY,(int)Math.ceil(cy+ry+Math.abs(tilt)*a));y++)
            for(int k=0;k<cells;k++){
                double dy=y-cy-tilt*along[k],d=horizontal[k]+(dy/ry)*(dy/ry);
                if(d>1)continue;
                int x=cellX[k],z=cellZ[k],index=WoodlandTrees.index(x,y,z);if(blocks[index]!=0||claimed[index])continue;
                claimed[index]=true;
                long edge=OakBirchWoodland.mix(variant ^ x*0x9E3779B97F4A7C15L ^ y*0xC2B2AE3D27D4EB4FL ^ z*0x165667B19E3779F9L);
                if(d<0.72||(edge&7)!=0)blocks[index]=19;
            }
    }
    private HumidCrowns(){}
}
