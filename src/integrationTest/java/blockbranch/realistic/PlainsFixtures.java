package blockbranch.realistic;

import static blockbranch.realistic.Plains.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class PlainsFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);var ecology=new GroundEcology.Field(d::sample,c);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(!contains(seed,x,z,d,c))continue;
            boolean interior=true;for(int dz:new int[]{-48,0,48})for(int dx:new int[]{-48,0,48})if(!contains(seed,x+dx,z+dz,d,c))interior=false;if(!interior)continue;
            for(int dz=-24;dz<=24;dz++)for(int dx=-24;dx<=24;dx++){
                int wx=x+dx,wz=z+dz;if(Math.floorMod(d.sample(wx,wz).height(),16)==14&&contains(seed,wx,wz,d,c)&&cover(seed,wx,wz,ecology.sample(wx,wz),true)==9)return new Point(wx,wz);
            }
        }
        throw new AssertionError("No natural Plains tall-grass section-seam fixture");
    }
    private PlainsFixtures() {}
}
