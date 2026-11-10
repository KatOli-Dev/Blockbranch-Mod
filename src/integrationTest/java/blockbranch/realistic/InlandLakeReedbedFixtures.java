package blockbranch.realistic;
import java.util.function.BiFunction;
import static blockbranch.realistic.InlandLakeReedbed.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class InlandLakeReedbedFixtures {
    static ShorePoint fixture(DrainageNetwork drainage,long seed) {
        for(int i=0;i<drainage.size();i++) {
            if(drainage.parent(i)!=-1 || !drainage.closed(i) || drainage.waterLevel(i)>TerrainShape.SEA_LEVEL+600
                || Math.abs(drainage.nodeX(i))>1800000 || Math.abs(drainage.nodeZ(i))>1800000) continue;
            for(int angle=0;angle<64;angle++) {
                double radians=angle*Math.PI/32,radius=drainage.lakeShoreRadius(i,radians);
                for(double r=radius*0.55;r<radius;r+=4) {
                    int x=Math.floorDiv(drainage.nodeX(i)+(int)(Math.cos(radians)*r),4)*4;
                    int z=Math.floorDiv(drainage.nodeZ(i)+(int)(Math.sin(radians)*r),4)*4;
                    if(cover(seed,x,z,contains(x,z,(wx,wz)->site(drainage,wx,wz)),site(drainage,x,z))<=1) continue;
                    boolean interior=true;
                    for(int dz=-4;dz<=4;dz+=4) for(int dx=-4;dx<=4;dx+=4)
                        if(!contains(x+dx,z+dz,(wx,wz)->site(drainage,wx,wz))) interior=false;
                    if(interior) return new ShorePoint(x,z);
                }
            }
        }
        throw new AssertionError("No shallow terminal-lake reedbed found");
    }
    private InlandLakeReedbedFixtures() {}
}
