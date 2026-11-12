package blockbranch.realistic;

import static blockbranch.realistic.BirchForest.*;

/** Development-only fixture selection and camera helpers; no production registration. The climate follows the drainage network's polar gate so a gated world is searched with the climate it generates. */
public final class BirchForestFixtures {
    static Point fixture(DrainageNetwork d,long seed){return fixture(d,seed,false);}

    static Point fixture(DrainageNetwork d,long seed,boolean second){
        var c=new ClimateField(seed,d.polarClimateEnabled());Point first=null;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(first!=null&&Math.abs(x-first.x())<16000&&Math.abs(z-first.z())<16000)continue;
            if(!contains(seed,x,z,d,c))continue;var roots=WoodlandTrees.roots(seed,x-48,z-48,x+48,z+48,d,c);
            for(var root:roots){
                if(root.family()!=2||!root.birch()||root.height()<17)continue;
                boolean interior=true;for(int dz=-32;dz<=32;dz+=16)for(int dx=-32;dx<=32;dx+=16)if(!contains(seed,root.x()+dx,root.z()+dz,d,c))interior=false;
                if(!interior)continue;
                var local=WoodlandTrees.roots(seed,root.x()-32,root.z()-32,root.x()+32,root.z()+32,d,c);
                if(local.size()>12&&local.stream().filter(r->r.family()==2&&r.birch()).count()>local.size()*.90){var found=new Point(root.x(),root.z());if(!second||first!=null)return found;first=found;break;}
            }
        }
        throw new AssertionError("No natural birch-dominant Forest fixture");
    }
    private BirchForestFixtures() {}
}
