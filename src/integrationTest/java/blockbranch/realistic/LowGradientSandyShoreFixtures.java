package blockbranch.realistic;
import java.util.function.BiFunction;
import static blockbranch.realistic.LowGradientSandyShore.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class LowGradientSandyShoreFixtures {
    static ShorePoint fixture(DrainageNetwork drainage,long seed) {
        for(int i=0;i<drainage.size()-DrainageNetwork.SIDE;i++) {
            if(Math.abs(drainage.nodeX(i))>1700000 || Math.abs(drainage.nodeZ(i))>1700000) continue;
            for(int step:new int[]{1,DrainageNetwork.SIDE}) {
                int n=i+step;
                if(step==1 && i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1) continue;
                if((drainage.nodeGround(i)<63)==(drainage.nodeGround(n)<63)) continue;
                int ax=drainage.nodeX(i),az=drainage.nodeZ(i),bx=drainage.nodeX(n),bz=drainage.nodeZ(n);
                boolean aw=site(drainage,ax,az).oceanWater(),bw=site(drainage,bx,bz).oceanWater();
                if(aw==bw) continue;
                for(int iteration=0;iteration<16;iteration++) {
                    int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);
                    if(site(drainage,mx,mz).oceanWater()==aw) { ax=mx;az=mz; } else { bx=mx;bz=mz; }
                }
                int cx=aw ? bx : ax,cz=aw ? bz : az;
                for(int oz=-48;oz<=48;oz+=4) for(int ox=-48;ox<=48;ox+=4) {
                    int x=Math.floorDiv(cx+ox,4)*4,z=Math.floorDiv(cz+oz,4)*4;
                    if(cover(seed,x,z,contains(x,z,(wx,wz)->site(drainage,wx,wz)),site(drainage,x,z))!=2) continue;
                    boolean interior=true;
                    for(int dz=-4;dz<=4;dz+=4) for(int dx=-4;dx<=4;dx+=4)
                        if(!contains(x+dx,z+dz,(wx,wz)->site(drainage,wx,wz))) interior=false;
                    if(interior) return new ShorePoint(x,z);
                }
            }
        }
        throw new AssertionError("No gentle ocean shore with rooted beach grass found");
    }
    private LowGradientSandyShoreFixtures() {}
}
