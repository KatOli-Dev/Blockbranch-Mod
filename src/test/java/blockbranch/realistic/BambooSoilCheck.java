package blockbranch.realistic;

import java.util.TreeSet;

/** Retain every floor material and non-litter plant while thinning only literal leaf litter. */
public final class BambooSoilCheck {
    public static void main(String[] args) {
        for (long seed : new long[] {424242, 1, -17}) {
            var d = new DrainageNetwork(new TerrainShape(seed), seed);
            var c = new ClimateField(seed);
            var ecology = new GroundEcology.Field(d::sample, c);
            var p = blockbranch.realistic.TropicalForestVariantsFixtures.fixture(d, seed, TropicalForestVariants.BAMBOO);
            var soils = new TreeSet<Integer>();
            int planted = 0, unchangedOpenings = 0, restoredGrass = 0, oldLitter = 0, newLitter = 0, removedLitter = 0;
            for (int z = p.z() - 64; z <= p.z() + 64; z++) for (int x = p.x() - 64; x <= p.x() + 64; x++) {
                var site = ecology.sample(x, z);
                for (boolean clear : new boolean[] {false, true}) {
                    int before = TropicalForestVariants.plantCover(seed, x, z, site, clear, TropicalForestVariants.BAMBOO);
                    int after = TropicalForestVariants.cover(seed, x, z, site, clear, TropicalForestVariants.BAMBOO);
                    int actualPlant=BambooSoil.vegetation(after);
                    require(actualPlant==before||before==9&&(actualPlant==8||actualPlant==11), "Only literal leaf litter may be removed");
                    int original=BambooSoil.encode(seed,x,z,site,clear,before);
                    require(floor(after)==floor(original),"Every version-1 soil and grass underlay remains byte-identical");
                    if(before==9){oldLitter++;if(actualPlant==9)newLitter++;else removedLitter++;}
                    boolean grass=after==11||BambooSoil.encoded(after)&&BambooSoil.soil(after)==3;
                    require(grass==(clear&&TropicalForestVariants.bambooGrass(seed,x,z,site)),"Physical grass underlay cannot reuse the fern/litter placement mask");
                    if(clear&&(before==9||before==10)&&TropicalForestVariants.bambooGrass(seed,x,z,site)){
                        require(floor(after)==3,"Grass remains under a fern/litter position even if litter is removed");restoredGrass++;
                    }
                    if (BambooSoil.encoded(after)) {
                        soils.add(BambooSoil.soil(after));
                        if (!clear) require(BambooSoil.soil(after) == 2, "Actual culm/root contact retains rooted soil");
                    } else {
                        require(before == after||before==9&&after==11, "Only removed grass-underlay litter becomes an empty grass opening");
                        unchangedOpenings++;
                    }
                    if (before == 9 || before == 10 || before == 12) planted++;
                    require(TropicalForestVariants.cover(seed, x, z, site, clear, TropicalForestVariants.SPARSE)
                        == TropicalForestVariants.plantCover(seed, x, z, site, clear, TropicalForestVariants.SPARSE), "Sparse Jungle remains unchanged");
                }
            }
            require(soils.equals(java.util.Set.of(0, 1, 2, 3)) && planted > 100 && unchangedOpenings > 100 && restoredGrass>100,
                "Meaningful actual soil variety with retained colonies/openings");
            require(newLitter>50&&newLitter<oldLitter*.35&&removedLitter>500,"Restrained litter colonies replace the broad blanket");
            checkBlockResolution(seed);
            System.out.println("BAMBOO_SOIL_FERNS_FLOOR_PRESERVED seed=" + seed + " anchor=" + p + " soils=" + soils + " plantsBefore=" + planted + " grass=" + unchangedOpenings+" plantMaskHolesRemoved="+restoredGrass+" oldLitter="+oldLitter+" newLitter="+newLitter+" removedLitter="+removedLitter);
        }
        System.out.println("BAMBOO_SOIL_GRASS_FERN_LITTER_SUBSET_PASS");
    }

    private static void checkBlockResolution(long seed) {
        int[] actual=new int[4],square=new int[4];
        for(int z=-192;z<192;z++)for(int x=-192;x<192;x++)for(boolean quantized:new boolean[]{false,true}){
            int here=soil(seed,x,z,quantized),west=soil(seed,x-1,z,quantized),north=soil(seed,x,z-1,quantized);
            var counts=quantized?square:actual;
            // Exclude root-colony edges, so a smooth root mask cannot hide a
            // coarse four-block repaint specifically in the dirt/coarse selector.
            if(here<2&&west<2&&here!=west)counts[Math.floorMod(x,4)]++;
            if(here<2&&north<2&&here!=north)counts[Math.floorMod(z,4)]++;
        }
        int total=java.util.Arrays.stream(actual).sum(),control=java.util.Arrays.stream(square).sum();
        require(total>100&&control>100,"Meaningful actual and synthetic material boundaries");
        for(int n:actual)require(n>0&&n<total*.4,"Actual soil boundaries use every block residue rather than quart masks");
        require(square[0]>control*.95,"Synthetic four-block repaint is detected");
        System.out.println("BAMBOO_SOIL_BLOCK_RESOLUTION seed="+seed+" actual="+java.util.Arrays.toString(actual)+" squareControl="+java.util.Arrays.toString(square));
    }

    private static int floor(int cover){return cover==11?3:BambooSoil.soil(cover);}

    private static int soil(long seed,int x,int z,boolean quantized) {
        if(quantized){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;}
        return BambooSoil.soil(BambooSoil.encode(seed,x,z,GroundEcology.NEUTRAL,true,8));
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private BambooSoilCheck() {}
}
