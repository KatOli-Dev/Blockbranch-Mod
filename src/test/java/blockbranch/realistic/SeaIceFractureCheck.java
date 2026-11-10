package blockbranch.realistic;
/** Determinism and morphology bounds; actual shape quality also requires pixel review. */
public final class SeaIceFractureCheck {
 public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){int ice=0,water=0,thin=0,changed=0;
  for(int z=-512;z<=512;z+=4)for(int x=-512;x<=512;x+=4){boolean full=SeaIceFractures.ice(seed,x,z,1),half=SeaIceFractures.ice(seed,x,z,.5);require(full==SeaIceFractures.ice(seed,x,z,1),"Repeatable world-coordinate fracture field");require(!SeaIceFractures.ice(seed,x,z,0),"No ice at zero concentration");require(!half||full,"Opening fractures monotonically reduces cover");if(full)ice++;else water++;if(half)thin++;if(full!=SeaIceFractures.ice(seed,x+1,z+1,1))changed++;}
  System.out.println("FRACTURE_COUNTS seed="+seed+" ice="+ice+" leads="+water+" half="+thin+" edges="+changed);
  require(ice>30000&&water>1000&&water<20000&&thin>0&&thin<ice&&changed>100,"Dense interiors, actual open leads, thinning margin and non-quart edges");
  for(int x:new int[]{-321,-320,-319,-17,-16,-15,15,16,17,319,320,321})for(int z:new int[]{-321,-16,16,321}){
   double actual=SeaIceFractures.clearance(seed,x,z,false),reversed=SeaIceFractures.clearance(seed,x,z,true);require(actual==reversed,"Segment overlap union independent of enumeration order at chunk/cell seams");
   double reference=Double.POSITIVE_INFINITY;int gx=Math.floorDiv(x,320),gz=Math.floorDiv(z,320);
   for(int dz=-6;dz<=6;dz++)for(int dx=-6;dx<=6;dx++)for(var segment:SeaIceFractures.generate(seed,gx+dx,gz+dz))reference=Math.min(reference,segment.clearance(x,z));
   require(Math.min(640,actual)==Math.min(640,reference),"Bounded lookup agrees with larger direct reference at chunk/cell seams");
  }
  System.out.println("SEA_ICE_BRANCHING_FRACTURE_FIELD_PASS seed="+seed+" ice="+ice+" leads="+water+" half="+thin+" blockEdges="+changed);
 }}
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
}
