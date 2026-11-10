package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.DappledForest.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class DappledForestFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);
        for(int z=-1700000;z<=1700000;z+=16000)for(int x=-1700000;x<=1700000;x+=16000){if(!contains(seed,x,z,d,c))continue;
            for(var root:WoodlandTrees.roots(seed,x-64,z-64,x+64,z+64,d,c))if(root.family()==FAMILY&&root.height()>=20&&contains(seed,root.x(),root.z(),d,c))return new Point(root.x(),root.z());
        }throw new AssertionError("No natural Dappled Forest interior seed="+seed);
    }
    private DappledForestFixtures() {}
}
