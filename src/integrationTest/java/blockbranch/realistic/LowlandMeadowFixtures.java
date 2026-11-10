package blockbranch.realistic;
import java.util.function.BiFunction;
import static blockbranch.realistic.LowlandMeadow.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class LowlandMeadowFixtures {
    static MeadowPoint fixture(DrainageNetwork drainage,long seed) {
        var climate=new ClimateField(seed);var ecology=new GroundEcology.Field(drainage::sample,climate);
        for(int z=-1700000;z<=1700000;z+=2000) for(int x=-1700000;x<=1700000;x+=2000) {
            int e=drainage.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
            if(e<20 || e>900)continue; // Retain the original regression witness while production placement expands.
            if(!gentleContains(x,z,(wx,wz)->site(drainage,wx,wz))) continue;
            for(int oz=-32;oz<=32;oz+=4) for(int ox=-32;ox<=32;ox+=4) {
                int wx=x+ox,wz=z+oz;
                if(!retained(wx,wz,drainage,climate))continue;
                if(cover(seed,wx,wz,gentleContains(wx,wz,(px,pz)->site(drainage,px,pz)),site(drainage,wx,wz),ecology.sample(wx,wz))!=3) continue;
                if(!OakBirchWoodland.meadowClimateSuitable(climate.sample(wx,wz,drainage::sample)) || OakBirchWoodland.stand(seed,wx,wz)>0.45) continue;
                boolean interior=true;
                for(int dz=-4;dz<=4;dz+=4) for(int dx=-4;dx<=4;dx+=4)
                    if(!gentleContains(wx+dx,wz+dz,(px,pz)->site(drainage,px,pz))) interior=false;
                if(interior) return new MeadowPoint(wx,wz);
            }
        }
        throw new AssertionError("No rolling lowland meadow found");
    }
    private LowlandMeadowFixtures() {}
}
