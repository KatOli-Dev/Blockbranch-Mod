package blockbranch.realistic;
/** Variable-clearance flowering microsites inside ecological colonies. Uses native26.3 XZ offsets
 * so competition is measured between rendered stems,not a fixed two-block planting lattice.
 * This is deterministic spatial sampling,not a root-growth simulation; vanilla blocks remain unchanged. */
public final class FlowerMicrosites {
 public static final double AVAILABLE_FRACTION=.254;
 static long priority(long s,int x,int z){return OakBirchWoodland.mix(s^0x464C4F5745525350L^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL);}
 static double radius(long h){return .42+.32*((OakBirchWoodland.mix(h^0x524F4F5453504143L)>>>11)*0x1.0p-53);}
 static long visual(int x,int z){long h=x*3129871^z*116129781L;h=h*h*42317861L+h*11;return h>>16;}
 static double ox(long h){return((float)(h&15)/15F-.5)*.5;}static double oz(long h){return((float)((h>>8)&15)/15F-.5)*.5;}
 public static boolean accepts(long s,int x,int z){long h=priority(s,x,z),v=visual(x,z);double r=radius(h),px=x+ox(v),pz=z+oz(v);for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){if(dx==0&&dz==0)continue;long q=priority(s,x+dx,z+dz);if(Long.compareUnsigned(q,h)<=0)continue;long w=visual(x+dx,z+dz);double rr=r+radius(q),ax=x+dx+ox(w)-px,az=z+dz+oz(w)-pz;if(ax*ax+az*az<rr*rr)return false;}return true;}
 public static double offsetX(int x,int z){return ox(visual(x,z));}
 public static double offsetZ(int x,int z){return oz(visual(x,z));}
 private FlowerMicrosites(){}
}
