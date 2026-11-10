package blockbranch.realistic;
import java.util.function.BiFunction;
import static blockbranch.realistic.GravelRiverMargin.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class GravelRiverMarginFixtures {
    static BankPoint fixture(DrainageNetwork drainage,long seed) {
        for(int i=0;i<drainage.size();i++) {
            if(!drainage.river(i) || drainage.waterLevel(i)<TerrainShape.SEA_LEVEL+8 || drainage.waterLevel(i)>TerrainShape.SEA_LEVEL+1500
                || Math.abs(drainage.nodeX(i))>1700000 || Math.abs(drainage.nodeZ(i))>1700000) continue;
            for(double t:new double[]{0.3,0.5,0.7}) {
                double x=drainage.edgeX(i,t),z=drainage.edgeZ(i,t);
                double dx=drainage.edgeX(i,t+0.001)-drainage.edgeX(i,t-0.001),dz=drainage.edgeZ(i,t+0.001)-drainage.edgeZ(i,t-0.001);
                double length=Math.hypot(dx,dz),width=drainage.riverHalfWidth(i)*(1-t)+drainage.riverHalfWidth(drainage.parent(i))*t;
                for(double side:new double[]{-1,1}) for(double offset=0.65;offset<=1.25;offset+=0.05) {
                    int wx=Math.floorDiv((int)Math.round(x-dz/length*width*offset*side),4)*4;
                    int wz=Math.floorDiv((int)Math.round(z+dx/length*width*offset*side),4)*4;
                    if(cover(seed,wx,wz,contains(wx,wz,(bx,bz)->site(drainage,bx,bz)),site(drainage,wx,wz))!=1) continue;
                    // Keep the client witness away from biome-zoom boundaries.
                    boolean interior=true;
                    for(int oz=-4;oz<=4;oz+=4) for(int ox=-4;ox<=4;ox+=4)
                        if(!contains(wx+ox,wz+oz,(bx,bz)->site(drainage,bx,bz))) interior=false;
                    if(interior) return new BankPoint(wx,wz);
                }
            }
        }
        throw new AssertionError("No coarse river-margin fixture found");
    }
    private GravelRiverMarginFixtures() {}
}
