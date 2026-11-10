package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.FrozenOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class FrozenOceanFixtures {
    public static Point fixture(DrainageNetwork d,long seed,int kind){return fixture(d,seed,kind,(x,z)->false);}

    public static Point fixture(DrainageNetwork d,long seed,int kind,java.util.function.BiPredicate<Integer,Integer> excluded){var c=new ClimateField(seed);
  for(int i=0;i<d.size();i++){int x=d.nodeX(i),z=d.nodeZ(i);if(Math.abs(x)>2050000||Math.abs(z)>2050000||excluded.test(x,z)||kind(seed,x,z,d,c)!=kind)continue;
   if(SeaIcePolicy.potential(z,c.sample(x,z,d::sample).meanTemperatureC()).freezingMarginC()<2)continue;
   for(int dz=-32;dz<=32;dz+=4)for(int dx=-32;dx<=32;dx+=4)if(!excluded.test(x+dx,z+dz)&&ice(seed,x+dx,z+dz,d,c)&&kind(seed,x+dx,z+dz,d,c)==kind)return new Point(x+dx,z+dz);
  }throw new AssertionError("No cold-season marine fixture seed="+seed+" kind="+kind);
 }

    public static MountainPeaks.View view(int x,int y,int z,boolean elevated,java.util.function.IntBinaryOperator ignored){return new MountainPeaks.View(x+.5,TerrainShape.SEA_LEVEL+(elevated?65:2),z+.5,35,elevated?55:12);}
    private FrozenOceanFixtures() {}
}
