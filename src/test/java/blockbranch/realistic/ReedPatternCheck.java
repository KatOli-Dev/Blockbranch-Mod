package blockbranch.realistic;
/** Reject fixed eight-metre patch edges,with the prior generator as a negative control. */
public final class ReedPatternCheck{
 static long mix(long v){v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;v=(v^(v>>>27))*0x94D049BB133111EBL;return v^(v>>>31);}
 static boolean legacy(long seed,int x,int z){long p=mix(seed^Math.floorDiv(x,8)*0x9E3779B97F4A7C15L^Math.floorDiv(z,8)*0xC2B2AE3D27D4EB4FL),h=mix(p^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);return(p&3)!=0&&(h&3)!=0;}
 static double metric(long seed,boolean old){long[] edge=new long[2],count=new long[2];var site=new InlandLakeReedbed.Site(300,303,true);for(int z=-256;z<256;z++)for(int x=-256;x<256;x++)for(int axis=0;axis<2;axis++){boolean a=old?legacy(seed,x,z):InlandLakeReedbed.cover(seed,x,z,true,site)>1,b=old?legacy(seed,x+(axis==0?1:0),z+(axis==1?1:0)):InlandLakeReedbed.cover(seed,x+(axis==0?1:0),z+(axis==1?1:0),true,site)>1;int phase=Math.floorMod(axis==0?x:z,8)==7?1:0;count[phase]++;if(a!=b)edge[phase]++;}return Math.abs(edge[1]/(double)count[1]-edge[0]/(double)count[0]);}
 public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){double old=metric(seed,true),now=metric(seed,false);if(old<.05||now>.025)throw new AssertionError("Eight-metre phase imprint:old="+old+",new="+now);System.out.println("REED_PATCH_PHASE seed="+seed+" legacy="+old+" corrected="+now);}System.out.println("REED_FIXED_PATCH_PERIODICITY_NEGATIVE_CONTROL_PASS");}
}
