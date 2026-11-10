package blockbranch.realistic;

/** Real coastal bluffs and their shallow bedrock platform; no stone-only substitute for an absent landform. */
public final class StonyShore {
    public static boolean contains(int x,int z,DrainageNetwork d){return surfaceContains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d);}
    static boolean surfaceContains(int x,int z,DrainageNetwork d){return d.sample(x,z).bedrockCoast();}
    /** Legacy coast byte:3 exposed native stone;4 thin supported gravel on gentler foot ledges. */
    static int cover(long seed,int x,int z,DrainageNetwork d){
        if(!surfaceContains(x,z,d))return 0;
        return SlopeCorrection.slope(d,x,z)<.55&&SurfacePatches.sample(seed^0x53544F4E59534852L,x,z,23)>.58?4:3;
    }
    public record Point(int x,int z){}


    private StonyShore(){}
}
