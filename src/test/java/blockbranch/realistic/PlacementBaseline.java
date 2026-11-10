package blockbranch.realistic;

/** Frozen definition-14 placement rules for before/after comparison; no current slope/altitude extensions. */
final class PlacementBaseline {
    static String habitat(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        int e=d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
        return InlandLakeReedbed.contains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz))?"reedbed"
            :AlpineScree.contains(x,z,d::sample)?"scree"
            :GravelRiverMargin.contains(x,z,(wx,wz)->GravelRiverMargin.site(d,wx,wz))?"river"
            :LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(d,wx,wz))?"shore"
            :OakBirchWoodland.potential(seed,x,z,d,c)?"temperate-woodland"
            :warmBeforeJungle(seed,x,z,d,c)?"warm-woodland"
            :OakBirchWoodland.meadowClimateSuitable(c.sample(x,z,d::sample))&&LowlandMeadow.gentleContains(x,z,(wx,wz)->LowlandMeadow.site(d,wx,wz))?"meadow"
            :e>=30&&e<=1400&&DrySteppe.contains(x,z,d,c)?"steppe"
            :e>=40&&e<=1800&&BorealWoodland.contains(seed,x,z,d,c)?"boreal"
            :ColdSteppe.contains(x,z,d,c)?"cold-steppe"
            :gravelBeforeSand(x,z,d,c)?"gravel-desert":"placeholder";
    }
    static String previous(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        return InlandLakeReedbed.contains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz))?"reedbed"
            :AlpineScree.contains(x,z,d::sample)?"scree"
            :GravelRiverMargin.contains(x,z,(wx,wz)->GravelRiverMargin.site(d,wx,wz))?"river"
            :LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(d,wx,wz))?"shore"
            :OakBirchWoodland.potential(seed,x,z,d,c)?"temperate-woodland"
            :warmBeforeJungle(seed,x,z,d,c)?"warm-woodland"
            :LowlandMeadow.potential(x,z,d,c)?"meadow"
            :DrySteppe.contains(x,z,d,c)?"steppe":BorealWoodland.contains(seed,x,z,d,c)?"boreal"
            :ColdSteppe.contains(x,z,d,c)?"cold-steppe":gravelBeforeSand(x,z,d,c)?"gravel-desert":"placeholder";
    }
    static String definition18(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        String before=previous(seed,x,z,d,c);
        return (before.equals("placeholder")||before.equals("meadow"))&&humidBeforeJungle(seed,x,z,d,c)?"humid-woodland":before;
    }
    static String definition19(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        String before=definition18(seed,x,z,d,c);
        return before.equals("placeholder")&&ColdTundra.contains(x,z,d,c)?"tundra":before;
    }
    static String definition20(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        String before=definition19(seed,x,z,d,c);
        return before.equals("placeholder")&&LakeSediment.contains(x,z,d)?"lake":before;
    }
    static String definition21(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        String before=definition20(seed,x,z,d,c);
        return before.equals("placeholder")&&RiverChannel.contains(x,z,d)?"river-channel":before;
    }
    static String definition22(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        String before=definition21(seed,x,z,d,c);
        return before.equals("placeholder")&&MarineShelf.contains(x,z,d)?"marine-shelf":before;
    }
    static String actual(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        String before=definition22(seed,x,z,d,c);
        return before.equals("placeholder")&&DeepOcean.contains(x,z,d)?"deep-ocean":before;
    }
    static String definition24(long seed,int x,int z,DrainageNetwork d,ClimateField c){var old=actual(seed,x,z,d,c);return old.equals("placeholder")&&Savanna.contains(seed,x,z,d,c)?"savanna":old;}
    private static boolean warmBeforeJungle(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;if(LandVegetation.reserved(d,x,z)||!WarmWoodland.stand(seed,x,z))return false;
        double dx=(d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.0,dz=(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.0;
        return WarmWoodland.eligible(d.sample(x,z),Math.hypot(dx,dz),false,c.sample(x,z,d::sample));
    }
    static boolean humidBeforeJungle(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;if(LandVegetation.reserved(d,x,z))return false;
        return HumidWoodland.eligible(d.sample(x,z),SlopeCorrection.slope(d,x,z),false,c.sample(x,z,d::sample))&&LowlandMeadow.contains(x,z,(wx,wz)->LowlandMeadow.site(d,wx,wz));
    }
    static String definition25(long seed,int x,int z,DrainageNetwork d,ClimateField c){var old=definition24(seed,x,z,d,c);return Jungle.contains(seed,x,z,d,c)?"jungle":old;}
    static String definition28(long seed,int x,int z,DrainageNetwork d,ClimateField c){return SandyDesert.contains(seed,x,z,d,c)?"desert":definition25(seed,x,z,d,c);}
    static String definition30(long seed,int x,int z,DrainageNetwork d,ClimateField c){return SunflowerPlains.contains(seed,x,z,d,c)?"sunflower-plains":Plains.potential(seed,x,z,d,c)?"plains":definition28(seed,x,z,d,c);}
    static String definition31(long seed,int x,int z,DrainageNetwork d,ClimateField c){return Forest.potential(seed,x,z,d,c)?"forest":definition30(seed,x,z,d,c);}
    private static boolean gravelBeforeSand(int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;if(LandVegetation.reserved(d,x,z)||!GravelDesert.climateSuitable(c.sample(x,z,d::sample)))return false;
        if(!GravelDesert.eligible(d.sample(x,z),SlopeCorrection.slope(d,x,z),false,c.sample(x,z,d::sample)))return false;
        for(int[] delta:new int[][]{{2048,0},{-2048,0},{0,2048},{0,-2048}}){var t=d.sample(x+delta[0],z+delta[1]);if(!t.closed()&&t.waterLevel()==TerrainShape.SEA_LEVEL&&t.height()+1<TerrainShape.SEA_LEVEL)return false;}return true;
    }
    private PlacementBaseline(){}
}
