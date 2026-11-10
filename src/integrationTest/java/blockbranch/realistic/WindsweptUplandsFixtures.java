package blockbranch.realistic;

import static blockbranch.realistic.WindsweptUplands.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class WindsweptUplandsFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);
        for(int z=-1700000;z<=1700000;z+=8000)for(int x=-1700000;x<=1700000;x+=8000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            for(var root:WindsweptTrees.roots(seed,x-64,z-64,x+64,z+64,d,c))if(kind(seed,root.x(),root.z(),d,c)==kind&&root.height()>=10)return new Point(root.x(),root.z());
        }
        throw new AssertionError("No natural windswept upland tree witness kind="+kind);
    }

    public static View view(int x,int y,int z,boolean elevated,java.util.function.IntBinaryOperator ground){
        double dx=ground.applyAsInt(x+32,z)-ground.applyAsInt(x-32,z),dz=ground.applyAsInt(x,z+32)-ground.applyAsInt(x,z-32),length=Math.hypot(dx,dz);
        if(length<.01){dx=18;dz=-27;length=Math.hypot(dx,dz);}
        // Cross the contour for ground views: downhill placement looks mostly uphill/sky.
        // The elevated view retains the same exposed face and full stone/soil context.
        double distance=elevated?55:33,cx=x-(elevated?dx:dz)/length*distance+.5,cz=z+(elevated?-dz:dx)/length*distance+.5;
        int local=ground.applyAsInt((int)Math.floor(cx),(int)Math.floor(cz));double cy=elevated?Math.max(y,local)+40:local+2,target=y+(elevated?4:5);
        return new View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(cx-x-.5,z+.5-cz)),(float)-Math.toDegrees(Math.atan2(target-(cy+1.62),Math.hypot(x+.5-cx,z+.5-cz))));
    }
    private WindsweptUplandsFixtures() {}
}
