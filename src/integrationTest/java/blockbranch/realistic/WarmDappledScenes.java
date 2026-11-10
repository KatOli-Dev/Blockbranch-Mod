package blockbranch.realistic;
/** Shared deterministic camera poses; no influence on generation. */
public final class WarmDappledScenes {
 public static MountainPeaks.View view(int x,int y,int z,boolean forest,boolean elevated,java.util.function.IntBinaryOperator ground){
  if(forest){var v=blockbranch.realistic.DarkCherryWoodlandFixtures.view(x,y,z,elevated,DarkCherryWoodland.CHERRY,ground);return new MountainPeaks.View(v.x(),v.y(),v.z(),v.yaw(),v.pitch());}
  double cx=x+(elevated?-18:-7)+.5,cz=z+(elevated?24:11)+.5;
  double cy=elevated?TerrainShape.SEA_LEVEL+6:ground.applyAsInt((int)Math.floor(cx),(int)Math.floor(cz))+2.5;
  double yaw=Math.toDegrees(Math.atan2(cx-x-.5,z+.5-cz)),pitch=-Math.toDegrees(Math.atan2(y+2-(cy+1.62),Math.hypot(x+.5-cx,z+.5-cz)));
  return new MountainPeaks.View(cx,cy,cz,(float)yaw,(float)pitch);
 }
 private WarmDappledScenes(){}
}
