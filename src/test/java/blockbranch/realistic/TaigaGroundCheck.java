package blockbranch.realistic;
public final class TaigaGroundCheck{
 static int base(int c){return BorealSurface.mossSite(c)?1:c==8||c==9||c==11||c==12?2:c==2?3:0;}
 public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){var site=new GroundEcology.Site(.65,.2,.1,.08,0,true);int old=0,now=0,n=0;for(int z=-128;z<128;z++)for(int x=-128;x<128;x++)for(int[]q:new int[][]{{1,0},{0,1}}){int a=GroundEcology.forest(seed^0x5441494741464C52L,x,z,1,site,true),b=GroundEcology.forest(seed^0x5441494741464C52L,x+q[0],z+q[1],1,site,true);if(base(a)!=base(b))old++;if(base(Taiga.cover(seed,x,z,site,true))!=base(Taiga.cover(seed,x+q[0],z+q[1],site,true)))now++;n++;}if(now>=old*.2||old<n*.1)throw new AssertionError("Ground negative control must detect former checkerboard");System.out.println("TAIGA_GROUND_NEGATIVE_CONTROL_PASS seed="+seed+" formerAdjacentColorChanges="+old/(double)n+" corrected="+now/(double)n);}}
}
