package blockbranch.realistic;

import static blockbranch.realistic.SnowyPlains.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SnowyPlainsFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);var e=new GroundEcology.Field(d::sample,c);for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){if(!contains(x,z,d,c))continue;boolean interior=true;for(int oz=-64;oz<=64;oz+=32)for(int ox=-64;ox<=64;ox+=32)if(!contains(x+ox,z+oz,d,c))interior=false;if(!interior)continue;for(int oz=-24;oz<=24;oz+=4)for(int ox=-24;ox<=24;ox+=4){int wx=x+ox,wz=z+oz;if(OpenSnowCover.layers(seed,wx,wz,e.sample(wx,wz),false)>=4)return new Point(wx,wz);}}throw new AssertionError("No natural cold snowy plain interior");}
    private SnowyPlainsFixtures() {}
}
