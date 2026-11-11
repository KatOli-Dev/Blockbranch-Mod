package blockbranch.realistic;

import java.util.*;

/** Fixed physical witnesses and transition controls, never a trace-occupancy target. */
public final class DeepBasinReliefCheck {
    static final int[][] SITES={{-1744000,-1904000},{-1904000,-1904000},{-1872000,-1904000},
        {-1648000,-1904000},{-1328000,-1904000},{-1168000,-1904000},{-1040000,-1904000},
        {-336000,-1904000},{-400000,-1744000},{-1008000,-1680000},{1904000,-1552000},{-1136000,-1776000}};
    static final String[] NAMES={"previousFineBed","gradientTransition","gentleBasin","ruggedMargin","upland","shelf","coast","ruggedDeep","trench","river","lake","gentleDepthTransition"};
    public static void main(String[] args){
        require(DeepBasinSchema.schema(261,1)==262&&DeepBasinSchema.baseSchema(262,1)==261,"Complete wrapper");
        for(int version:new int[]{82,257,258,259,260,261,263})require(!DeepBasinSchema.matches(version,1),"Partial/future schema");
        require(!DeepBasinSchema.matches(262,0)&&!DeepBasinSchema.matches(262,2),"Absent/unknown definition rejects262");
        require(DeepBasinRelief.response(1000,0)==0&&DeepBasinRelief.response(3000,.02)==1&&DeepBasinRelief.response(6000,.10)==0,"Named blend anchors");
        double previous=-1;
        for(int depth=900;depth<=3100;depth++){
            double w=DeepBasinRelief.response(depth,.01);require(w>=previous&&w>=0&&w<=1,"Monotone bounded depth blend");previous=w;
            require(Math.abs(w-DeepBasinRelief.response(depth+.001,.01))<1e-6,"Continuous depth response");
        }
        for(double g:new double[]{.02,.10}){
            double a=DeepBasinRelief.response(4000,g-1e-6),b=DeepBasinRelief.response(4000,g+1e-6);
            require(Math.abs(a-b)<1e-10,"Smooth gradient endpoint, no hard contour");
        }
        int totalChanged=0;
        for(long seed:new long[]{424242,1,-17}){
            var shape=new TerrainShape(seed);var old=new DrainageNetwork(shape,seed,true,true,true,true);var now=new DrainageNetwork(shape,seed,true,true,true,true,true);var c=new ClimateField(seed);int n=0,changed=0,unchanged=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                checkInvariant(x,z,old,now,c);var before=old.sample(x,z);var after=now.sample(x,z);
                if(before.height()!=after.height())changed++;else unchanged++;n++;
            }
            totalChanged+=changed;System.out.println("GRID seed="+seed+" denominator="+n+" changedPhysical="+changed+" unchanged="+unchanged+" bounds=[-1920000,1920000) centeredStride=32000");
            if(seed==424242)windows(shape,old,now,c);
        }
        require(totalChanged>0,"Physical change is exercised");System.out.println("PASS fixed physical, unchanged ownership/climate, exclusions, deterministic blend and finite transition bounds");
    }
    private static void windows(TerrainShape shape,DrainageNetwork old,DrainageNetwork now,ClimateField climate){
        var policy=new DeepBasinRelief(shape);
        for(int i=0;i<SITES.length;i++){
            int cx=SITES[i][0],cz=SITES[i][1];double oldSq=0,newSq=0,weightRate=0;int oldMin=99,oldMax=-99,newMin=99,newMax=-99,structMin=99999,structMax=-99999,changed=0,exactBank=0,n=0;
            for(int dz=-128;dz<=128;dz+=4)for(int dx=-128;dx<=128;dx+=4){
                int x=cx+dx,z=cz+dz;checkInvariant(x,z,old,now,climate);var s=old.structuralSample(x,z);var a=old.sample(x,z);var b=now.sample(x,z);
                require(s.equals(now.structuralSample(x,z)),"Original structural field remains exact");
                int oa=a.height()-s.height(),nb=b.height()-s.height();oldSq+=(double)oa*oa;newSq+=(double)nb*nb;oldMin=Math.min(oldMin,oa);oldMax=Math.max(oldMax,oa);newMin=Math.min(newMin,nb);newMax=Math.max(newMax,nb);structMin=Math.min(structMin,s.height());structMax=Math.max(structMax,s.height());if(a.height()!=b.height())changed++;if(oa==Math.round(52*shape.bankFraction(x,z)))exactBank++;n++;
                if(i>=3&&i!=11)require(a.equals(b),"Negative control "+NAMES[i]);
            }
            // Two crossing 4097-point native-scale transects, including corners
            // of the old 64m cache lattice, expose depth/gradient blend contours.
            for(int axis=0;axis<2;axis++)for(int t=-2048;t<=2048;t++){
                int x=cx+(axis==0?t:0),z=cz+(axis==1?t:0);double w=policy.strength(x,z);
                weightRate=Math.max(weightRate,23.4*Math.abs(w-policy.strength(x+(axis==0?1:0),z+(axis==1?1:0))));
                checkInvariant(x,z,old,now,climate);
            }
            require(weightRate<.25,"Blend alone introduces <.25m/block component change in finite transects");
            if(i==0||i==2)require(newSq<oldSq*.03&&exactBank==n&&changed>0,"Bank component is reduced, not structural terrain");
            System.out.printf(Locale.ROOT,"SITE %s (%d,%d) samples=%d structure=%d..%d oldDetail=%d..%d RMS=%.5f newDetail=%d..%d RMS=%.5f changed=%d exactOldBank=%d blendInducedMaxMPerBlock=%.6f window=+/-128 stride=4 transects=+/-2048 stride=1 axes=2%n",NAMES[i],cx,cz,n,structMin,structMax,oldMin,oldMax,Math.sqrt(oldSq/n),newMin,newMax,Math.sqrt(newSq/n),changed,exactBank,weightRate);
        }
        // Request order and negative coordinates use identical exact samples.
        for(int i=SITES.length-1;i>=0;i--){var p=SITES[i];require(now.sample(p[0],p[1]).equals(policy.apply(old,p[0],p[1],old.sample(p[0],p[1]))),"Order-independent final composition");}
    }
    private static void checkInvariant(int x,int z,DrainageNetwork old,DrainageNetwork now,ClimateField c){
        var a=old.sample(x,z);var b=now.sample(x,z);
        require(a.waterLevel()==b.waterLevel()&&a.basin()==b.basin()&&a.closed()==b.closed()&&a.bedrockCoast()==b.bedrockCoast()&&a.sandyBackshore()==b.sandyBackshore()&&a.regionalHeight()==b.regionalHeight()&&a.regionalWaterLevel()==b.regionalWaterLevel(),"Only physical height may change");
        require(old.channelAt(x,z).equals(now.channelAt(x,z))&&old.habitatOceanWater(x,z,a)==now.habitatOceanWater(x,z,b)&&old.terminalLakeWater(x,z,a)==now.terminalLakeWater(x,z,b),"Water ownership and routes unchanged");
        require(c.sample(x,z,old::sample).equals(c.sample(x,z,now::sample)),"Mean climate references are exact");
        if(a.height()!=b.height())require(old.habitatOceanWater(x,z,a)&&!old.terminalLakeWater(x,z,a)&&old.channelAt(x,z).edge()<0&&a.waterLevel()-1-a.height()>1000&&Math.abs(a.height()-b.height())<=24,"Bounded confirmed offshore change");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private DeepBasinReliefCheck(){}
}
