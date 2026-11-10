package blockbranch.realistic;

import static blockbranch.realistic.MontaneMeadow.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class MontaneMeadowFixtures {
    /** Stable scenery anchor,independent of the interaction-witness flower's placement. */
 static Point sceneAnchor(DrainageNetwork d,long seed){var c=new ClimateField(seed);for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){if(!contains(seed,x,z,d,c))continue;boolean interior=true;for(int oz=-64;oz<=64;oz+=32)for(int ox=-64;ox<=64;ox+=32)if(!contains(seed,x+ox,z+oz,d,c))interior=false;if(interior)return new Point(x+15,z-18);}throw new AssertionError("No Meadow scenery anchor");}

    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);var e=new GroundEcology.Field(d::sample,c);for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){if(!contains(seed,x,z,d,c))continue;boolean interior=true;for(int oz=-64;oz<=64;oz+=32)for(int ox=-64;ox<=64;ox+=32)if(!contains(seed,x+ox,z+oz,d,c))interior=false;if(!interior)continue;for(int oz=-24;oz<=24;oz++)for(int ox=-24;ox<=24;ox++){int wx=x+ox,wz=z+oz;if(cover(seed,wx,wz,e.sample(wx,wz),true)==11)return new Point(wx,wz);}}throw new AssertionError("No natural montane Meadow allium interior");}
    private MontaneMeadowFixtures() {}
}
