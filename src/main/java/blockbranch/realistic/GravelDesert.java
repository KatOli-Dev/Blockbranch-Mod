package blockbranch.realistic;

/** Warm/cool-temperate arid gravel desert; moisture is a suitability proxy, not a physical aridity index. */
public final class GravelDesert {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=5&&c.meanTemperatureC()<=30&&c.moisture()<0.12;}
    public static boolean eligible(DrainageNetwork.Sample terrain,double slope,boolean reserved,ClimateField.Sample climate){return terrain.waterLevel()==DrainageNetwork.DRY&&!reserved&&slope<=0.30&&climateSuitable(climate);}
    public static boolean contains(int x,int z,DrainageNetwork drainage,ClimateField climate){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,drainage,climate);
    }
    /** 1=pavement; 2=loose gravel; 3=sand lens; 4=bare fine-soil pocket; 5=dead stems; 6=dry grass; 7=live shrub. */
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry){
        if(!habitat||!dry)return 0;
        long patch=patch(seed,x,z);
        int group=(int)(patch&15);
        if(group>=14){
            int plant=plant(patch,x,z);
            if(plant==0)return 7;if(plant==1||plant==65)return 5;if(plant==2)return 6;
            if(soilPocket(seed,patch,x,z))return 4;
        }
        // Broad gravel lenses and smaller sand pockets interrupt a connected pavement
        // matrix. Existing plants keep their exact locations and soil/support states.
        // A narrow mixed edge avoids a sharply cut material boundary.
        double grain=((OakBirchWoodland.mix(seed^0x475241494E454447L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53-.5)*.12;
        if(SurfacePatches.signed(seed^0x534544494D454E54L,x,z,32)+grain>.28)return 2;
        return SurfacePatches.signed(seed^0x53414E444C454E53L,x,z,16)+grain<-.35?3:1;
    }
    private static int plant(long patch,int x,int z){return (int)(OakBirchWoodland.mix(patch^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)&127);}
    /** Retain fine soil near seeded plants, rather than exposing whole 24 m soil cells.
     * Test cheap hashes first; only a plant candidate needs another patch lookup.
     * No neighbour terrain reads, caches, plants added or support states changed.
     */
    private static boolean soilPocket(long seed,long soilPatch,int x,int z){
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){
            if(dx*dx+dz*dz>4||(dx==0&&dz==0))continue;
            int plant=plant(soilPatch,x+dx,z+dz);
            if((plant<=2||plant==65)&&patch(seed,x+dx,z+dz)==soilPatch)return true;
        }
        return false;
    }
    /** Original plant footprint; saved material meanings and vegetation supports stay fixed. */
    private static long patch(long seed,int x,int z){
        int cx=Math.floorDiv(x,24),cz=Math.floorDiv(z,24);long nearest=0,second=0;double firstDistance=Double.POSITIVE_INFINITY,secondDistance=Double.POSITIVE_INFINITY;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            long h=OakBirchWoodland.mix(seed^0xDE5E47A11L^(cx+dx)*0x9E3779B97F4A7C15L^(cz+dz)*0xC2B2AE3D27D4EB4FL);
            double px=(cx+dx)*24+3.6+16.8*((h>>>16)&65535)/65535.0,pz=(cz+dz)*24+3.6+16.8*((h>>>32)&65535)/65535.0;
            double distance=(x-px)*(x-px)+(z-pz)*(z-pz);
            if(distance<firstDistance){secondDistance=firstDistance;second=nearest;firstDistance=distance;nearest=h;}
            else if(distance<secondDistance){secondDistance=distance;second=h;}
        }
        double gap=Math.sqrt(secondDistance)-Math.sqrt(firstDistance);
        long edge=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        return gap<2&&(edge&1023)<(2-gap)*256?second:nearest;
    }
    public record Point(int x,int z){}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ){}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork drainage,ClimateField climate){
        drainage=drainage.gravelReference();
        if(LandVegetation.surfaceReserved(drainage,x,z))return false;
        if(SandyDesert.surfaceContains(climate.seed(),x,z,drainage,climate))return false;
        var c=climate.sample(x,z,drainage::sample);if(!climateSuitable(c))return false;
        double dx=(drainage.sample(x+32,z).height()-drainage.sample(x-32,z).height())/64.0,dz=(drainage.sample(x,z+32).height()-drainage.sample(x,z-32).height())/64.0;
        if(!eligible(drainage.sample(x,z),Math.hypot(dx,dz),false,c))return false;
        for(int[] d:new int[][]{{2048,0},{-2048,0},{0,2048},{0,-2048}}){var t=drainage.sample(x+d[0],z+d[1]);if(!t.closed()&&t.waterLevel()==TerrainShape.SEA_LEVEL&&t.height()+1<TerrainShape.SEA_LEVEL)return false;}
        return true;
    }
    private GravelDesert(){}
}
