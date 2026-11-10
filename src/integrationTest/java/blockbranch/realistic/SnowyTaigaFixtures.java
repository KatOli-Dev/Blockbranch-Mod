package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.SnowyTaiga.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SnowyTaigaFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){if(!contains(seed,x,z,d,c))continue;var roots=BorealTrees.roots(seed,x-40,z-40,x+40,z+40,d,c);for(var r:roots)if(r.family()==5&&r.height()>=18&&contains(seed,r.x(),r.z(),d,c)&&roots.stream().filter(t->t.family()==5).count()>10)return new Point(r.x(),r.z());}throw new AssertionError("No natural cold snowy evergreen stand");}
    private SnowyTaigaFixtures() {}
}
