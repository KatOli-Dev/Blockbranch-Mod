package blockbranch.realistic;

import java.util.function.BiFunction;

/** Flat diagonal and natural bank controls for surface painting independent of quart biomes. */
public final class ColdSurfaceCheck {
    public static void main(String[] args) {
        long seed=424242;var climate=new ClimateField(seed);int z0=1900000;
        BiFunction<Integer,Integer,DrainageNetwork.Sample> initial=(x,z)->new DrainageNetwork.Sample(1000,DrainageNetwork.DRY,0,false);
        int height=1000+(int)Math.round(climate.sample(0,z0,initial).meanTemperatureC()/.006);
        BiFunction<Integer,Integer,DrainageNetwork.Sample> flat=(x,z)->new DrainageNetwork.Sample(height,DrainageNetwork.DRY,0,false);
        BiFunction<Integer,Integer,Boolean> reserved=(x,z)->x+2*(z-z0)>=13;
        int wrong=0;
        for(int z=z0-16;z<z0+16;z++)for(int x=-16;x<16;x++) {
            boolean expected=!reserved.apply(x,z);
            boolean before=BorealWoodland.contains(seed,x,z,flat,reserved,climate);
            boolean after=BorealWoodland.surfaceContains(seed,x,z,flat,reserved,climate);
            require(after==expected,"A flat diagonal surface boundary must follow the actual block");
            if(before!=expected)wrong++;
        }
        require(wrong>40,"Quart-mask negative control must reproduce incorrect painted columns");

        var drainage=new DrainageNetwork(new TerrainShape(seed),seed);
        int size=96,x0=1064536,nz0=1284120,changed=0,oldEdges=0,oldOffGrid=0,newEdges=0,newOffGrid=0;
        boolean[][] old=new boolean[size][size],fresh=new boolean[size][size];
        long geometry=1;
        for(int z=0;z<size;z++)for(int x=0;x<size;x++){
            int wx=x0+x,wz=nz0+z;var before=drainage.sample(wx,wz);
            old[z][x]=SnowyTaiga.contains(seed,wx,wz,drainage,climate);
            fresh[z][x]=SnowyTaiga.surfaceContains(seed,wx,wz,drainage,climate);
            if(old[z][x]!=fresh[z][x])changed++;
            require(before.equals(drainage.sample(wx,wz)),"Surface selection must not change geometry/water");
            geometry=31*geometry+before.height();geometry=31*geometry+before.waterLevel();
        }
        for(int z=1;z<size;z++)for(int x=1;x<size;x++)for(int axis=0;axis<2;axis++){
            int px=x-(axis==0?1:0),pz=z-(axis==1?1:0),coordinate=axis==0?x0+x:nz0+z;
            if(old[z][x]!=old[pz][px]){oldEdges++;if(Math.floorMod(coordinate,4)!=0)oldOffGrid++;}
            if(fresh[z][x]!=fresh[pz][px]){newEdges++;if(Math.floorMod(coordinate,4)!=0)newOffGrid++;}
        }
        require(oldEdges>100&&oldOffGrid==0,"Natural bank control has exclusively four-block paint boundaries");
        require(changed>100&&newOffGrid>newEdges*.4,"Natural bank material boundaries must resolve individual blocks");
        for(int z=size-1;z>=0;z--)for(int x=size-1;x>=0;x--)require(fresh[z][x]==SnowyTaiga.surfaceContains(seed,x0+x,nz0+z,drainage,climate),"Surface selection independent of sampling order");
        System.out.printf("COLD_SURFACE flatWrongBefore=%d flatWrongAfter=0 naturalChanged=%d oldEdges=%d oldOffGrid=%d newEdges=%d newOffGrid=%d geometry=%d%n",wrong,changed,oldEdges,oldOffGrid,newEdges,newOffGrid,geometry);
        System.out.println("COLD_SURFACE_FLAT_DIAGONAL_NATURAL_BANK_ORDER_PASS");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private ColdSurfaceCheck() {}
}
