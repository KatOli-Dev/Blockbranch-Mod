package blockbranch.realistic;
/** Original noise oracle checks exact doubles at every cached salt and fallback boundaries. */
public final class TerrainPhaseCheck {
 private final long seed;
 private TerrainPhaseCheck(long seed){this.seed=seed;}
 public static void main(String[] args)throws Exception{
  var noise=TerrainShape.class.getDeclaredMethod("noise",double.class,double.class,long.class);noise.setAccessible(true);int checked=0;
  for(long seed:new long[]{424242,1,-17,Long.MIN_VALUE,Long.MAX_VALUE}){
   var original=new TerrainPhaseCheck(seed);var actual=new TerrainShape(seed);var random=new java.util.Random(seed);
   for(long salt=-1;salt<=1152;salt++)for(int i=0;i<8;i++){
    double x=i<4?new double[]{0,-1,Math.nextDown(0.),2097152}[i]:random.nextDouble()*4_000_000-2_000_000;
    double z=i<4?new double[]{-0.,Math.nextDown(-1.),1,-2097152}[i]:random.nextDouble()*4_000_000-2_000_000;
    double expected=original.noise(x,z,salt),found=(double)noise.invoke(actual,x,z,salt);
    if(Double.doubleToRawLongBits(expected)!=Double.doubleToRawLongBits(found))throw new AssertionError("Noise phase parity seed="+seed+" salt="+salt+" at "+x+","+z);checked++;
   }
   for(long salt:new long[]{Long.MIN_VALUE,Long.MAX_VALUE})if(Double.doubleToRawLongBits(original.noise(-17.5,33.25,salt))!=Double.doubleToRawLongBits((double)noise.invoke(actual,-17.5,33.25,salt)))throw new AssertionError("Long salt fallback");
  }
  System.out.println("PASS original noise bits="+checked+" across five seeds, salt range, fractional/lattice/boundary coordinates and long fallbacks");
 }
    private double noise(double x, double z, long salt) {
        // Seeded fractional phases avoid forcing every octave through zero at
        // the world origin, and avoid a seed-independent landscape there.
        x += (mix(seed ^ salt) >>> 12) * 0x1.0p-40;
        z += (mix(seed ^ salt ^ 0xD1B54A32D192ED03L) >>> 12) * 0x1.0p-40;
        int ix = (int)Math.floor(x), iz = (int)Math.floor(z);
        double dx = x - ix, dz = z - iz;
        double u = fade(dx), v = fade(dz);
        return 1.4142135623730951 * lerp(v,
            lerp(u, gradient(ix, iz, dx, dz, salt), gradient(ix + 1, iz, dx - 1, dz, salt)),
            lerp(u, gradient(ix, iz + 1, dx, dz - 1, salt), gradient(ix + 1, iz + 1, dx - 1, dz - 1, salt)));
    }

    private double gradient(int x, int z, double dx, double dz, long salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = mix(h);
        return switch ((int)h & 7) {
            case 0 -> dx;
            case 1 -> -dx;
            case 2 -> dz;
            case 3 -> -dz;
            case 4 -> (dx + dz) * 0.7071067811865476;
            case 5 -> (dx - dz) * 0.7071067811865476;
            case 6 -> (-dx + dz) * 0.7071067811865476;
            default -> (-dx - dz) * 0.7071067811865476;
        };
    }

    private static long mix(long h) {
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double smooth(double a, double b, double value) {
        double t = Math.clamp((value - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
