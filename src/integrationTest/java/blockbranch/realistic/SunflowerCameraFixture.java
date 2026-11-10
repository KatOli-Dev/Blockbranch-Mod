package blockbranch.realistic;

import net.minecraft.core.BlockPos;

/** Optional fixed photo anchor, independent of the naturally selected interaction flower. */
public final class SunflowerCameraFixture {
    private SunflowerCameraFixture() {}
    public static boolean fixed(){return !System.getProperty("blockbranch.sunflowerCameraPoint","").isEmpty();}
    public static BlockPos anchor(BlockPos fallback){
        String point=System.getProperty("blockbranch.sunflowerCameraPoint","");
        if(point.isEmpty())return fallback;
        String[] coordinates=point.split(",");
        if(coordinates.length!=3)throw new IllegalArgumentException("Sunflower camera point requires x,y,z");
        return new BlockPos(Integer.parseInt(coordinates[0]),Integer.parseInt(coordinates[1]),Integer.parseInt(coordinates[2]));
    }
}
