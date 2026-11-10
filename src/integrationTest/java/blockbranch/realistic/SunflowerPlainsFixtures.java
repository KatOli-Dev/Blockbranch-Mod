package blockbranch.realistic;

import static blockbranch.realistic.SunflowerPlains.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SunflowerPlainsFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);var field=new GroundEcology.Field(d::sample,c);
        String fixed=System.getProperty("blockbranch.sunflowerFixturePoint","");
        if(!fixed.isBlank()){
            var parts=fixed.split(",");if(parts.length!=2)throw new IllegalArgumentException("sunflowerFixturePoint requires x,z");
            int x=Integer.parseInt(parts[0].trim()),z=Integer.parseInt(parts[1].trim());
            if(!contains(seed,x,z,d,c)||Math.floorMod(d.sample(x,z).height(),16)!=14||cover(seed,x,z,field.sample(x,z),true)!=10)
                throw new AssertionError("Requested sunflower fixture is not a natural paired-flower section seam");
            return new Point(x,z);
        }
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(!contains(seed,x,z,d,c))continue;
            boolean interior=true;for(int dz:new int[]{-48,0,48})for(int dx:new int[]{-48,0,48})if(!contains(seed,x+dx,z+dz,d,c))interior=false;if(!interior)continue;
            for(int dz=-24;dz<=24;dz++)for(int dx=-24;dx<=24;dx++){
                int wx=x+dx,wz=z+dz;if(Math.floorMod(d.sample(wx,wz).height(),16)==14&&contains(seed,wx,wz,d,c)&&cover(seed,wx,wz,field.sample(wx,wz),true)==10)return new Point(wx,wz);
            }
        }
        throw new AssertionError("No natural Sunflower Plains section-seam fixture");
    }
    private SunflowerPlainsFixtures() {}
}
