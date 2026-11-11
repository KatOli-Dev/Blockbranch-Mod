package blockbranch.realistic;

/** Specialist habitat protection for climate-led common vegetation, independent of arbitrary altitude bands. */
public final class LandVegetation {
    public static boolean reserved(DrainageNetwork drainage,int x,int z) {
        if(drainage.continentalIceThickness(x,z)>0||drainage.landIceThickness(x,z)>0)return true;
        if(drainage.sample(x,z).bedrockCoast())return true;
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        var sample=drainage.sample(x,z);if(sample.bedrockCoast()||sample.waterLevel()!=DrainageNetwork.DRY || drainage.channelAt(x,z).edge()>=0)return true;
        int elevation=sample.height()+1-TerrainShape.SEA_LEVEL;
        if(elevation>=2200 && elevation<=4200 && AlpineScree.contains(x,z,drainage::sample))return true;
        return elevation>=-8 && elevation<=4 && LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz));
    }
    /** Water and exposed debris protect their actual columns independently of the biome grid. */
    static boolean surfaceReserved(DrainageNetwork drainage,int x,int z) {
        return drainage.surfaceReserved(x,z);
    }
    static boolean uncachedSurfaceReserved(DrainageNetwork drainage,int x,int z) {
        if(drainage.continentalIceThickness(x,z)>0||drainage.landIceThickness(x,z)>0)return true;
        var sample=drainage.sample(x,z);if(sample.bedrockCoast()||sample.waterLevel()!=DrainageNetwork.DRY || drainage.channelAt(x,z).edge()>=0)return true;
        int elevation=sample.height()+1-TerrainShape.SEA_LEVEL;
        if(elevation>=2200 && elevation<=4200 && AlpineScree.surfaceContains(x,z,drainage::sample))return true;
        return elevation>=-8 && elevation<=4 && LowGradientSandyShore.surfaceContains(x,z,(wx,wz)->LowGradientSandyShore.site(drainage,wx,wz));
    }
    private LandVegetation() {}
}
