package blockbranch.realistic;

import static blockbranch.realistic.Grove.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class GroveFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);var e=new GroundEcology.Field(d::sample,c);for(int z=-1900000;z<=1900000;z+=4000)for(int x=-1900000;x<=1900000;x+=4000){if(!contains(seed,x,z,d,c,e))continue;var roots=BorealTrees.roots(seed,x-48,z-48,x+48,z+48,d,c);WoodlandTrees.Root best=null;long bestCount=0;for(var r:roots){if(r.family()!=6||r.height()<14||!contains(seed,r.x(),r.z(),d,c,e))continue;long near=roots.stream().filter(q->q.family()==6&&Math.hypot(q.x()-r.x(),q.z()-r.z())<=40).count();if(near>bestCount){best=r;bestCount=near;}}if(best!=null&&bestCount>=6)return new Point(best.x(),best.z());}throw new AssertionError("No natural snowy montane spruce Grove");}
    private GroveFixtures() {}
}
