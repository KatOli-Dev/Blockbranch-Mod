package blockbranch.realistic;

/** Coherent substrate/understory patches, empty gaps and physical coordinate continuity. */
public final class OldGrowthFloorCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17})for(boolean shade:new boolean[]{false,true}){
            var site=new GroundEcology.Site(.65,.2,.1,0,0,shade);int moss=0,mossPairs=0,plants=0,plantPairs=0,emptyTiles=0,empty8=0;int[] tilePlants=new int[256],tile8=new int[1024];
            int[] scales={4,16,20,48},edges=new int[4],edgeChanges=new int[4],inner=new int[4],innerChanges=new int[4];
            for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
                int a=OldGrowthFloor.cover(seed,x,z,site,true),east=OldGrowthFloor.cover(seed,x+1,z,site,true),south=OldGrowthFloor.cover(seed,x,z+1,site,true);
                require(a==OldGrowthFloor.cover(seed,x,z,site,true),"Stable absolute-coordinate floor");
                require(a>=1&&a<=8&&a!=3&&a!=6||a==12||BorealSurface.mossSite(a),"Existing native floor palette only");
                int blocked=OldGrowthFloor.cover(seed,x,z,site,false);require(blocked>=1&&blocked<=2||blocked==BorealSurface.SOIL,"Stem clearance suppresses all low plants");
                if(moss(a)){moss++;if(moss(east))mossPairs++;if(moss(south))mossPairs++;}
                if(plant(a)){plants++;if(plant(east))plantPairs++;if(plant(south))plantPairs++;tilePlants[((z+128)>>4)*16+((x+128)>>4)]++;tile8[((z+128)>>3)*32+((x+128)>>3)]++;}
                for(int i=0;i<scales.length;i++)for(int axis=0;axis<2;axis++){
                    boolean boundary=Math.floorMod((axis==0?x:z)+1,scales[i])==0,changed=moss(a)!=moss(axis==0?east:south);
                    if(boundary){edges[i]++;if(changed)edgeChanges[i]++;}else{inner[i]++;if(changed)innerChanges[i]++;}
                }
            }
            for(int n:tilePlants)if(n==0)emptyTiles++;for(int n:tile8)if(n==0)empty8++;
            require(moss>1000&&moss<32768,"Meaningful moss patches and substantial bare soil");
            require(mossPairs/(2.0*moss)>.85,"Moss forms contiguous patches rather than independent marks");
            require(plants>65536*.04&&plants<65536*.16&&empty8>128&&empty8<768&&emptyTiles<154,"Balanced sparse understory between confetti and almost-bare extremes");
            require(plantPairs/(2.0*plants)>plants/65536.0*1.4,"Low plants cluster rather than uniform independent scatter");
            for(int i=0;i<scales.length;i++)require(Math.abs(edgeChanges[i]/(double)edges[i]-innerChanges[i]/(double)inner[i])<.025,"No excess moss edges at lattice scale "+scales[i]);
            System.out.println("OLD_GROWTH_FLOOR seed="+seed+" shade="+shade+" moss="+moss+" neighbor="+mossPairs/(2.0*moss)+" plants="+plants+" empty8="+empty8+" empty16="+emptyTiles);
        }
        System.out.println("OLD_GROWTH_FLOOR_PATCH_GAP_SEAM_PASS");
    }
    private static boolean moss(int code){return BorealSurface.mossSite(code);}
    private static boolean plant(int code){return code>=4&&code<=8||code==BorealSurface.CARPET;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
