package blockbranch.realistic;
import java.util.*;

/** World-coordinate placement/colonies, climate limits, retained meadow and deterministic section seams. */
public final class PlainsCheck {
    public static void main(String[]args){
        for(double t:new double[]{6,23})for(double m:new double[]{.27,.55})require(Plains.climateSuitable(new ClimateField.Sample(t,m)),"Inclusive Plains climate");
        for(var c:List.of(new ClimateField.Sample(5.999,.4),new ClimateField.Sample(23.001,.4),new ClimateField.Sample(15,.269999),new ClimateField.Sample(15,.550001)))require(!Plains.climateSuitable(c),"Specific cool/wet meadow and dryland retained");
        for(long seed:new long[]{424242,1,-17}){
            checkFlowerColonies(seed);
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var field=new GroundEcology.Field(d::sample,c);var p=blockbranch.realistic.PlainsFixtures.fixture(d,seed);int[] counts=new int[10];int adjacent=0,pairs=0,transitions=0;
            var patches=new int[256*256];
            for(int z=p.z()-128;z<p.z()+128;z++)for(int x=p.x()-128;x<p.x()+128;x++){
                boolean selected=Plains.contains(seed,x,z,d,c);int qx=Math.floorDiv(x,4)*4,qz=Math.floorDiv(z,4)*4;
                require(selected==Plains.contains(seed,qx,qz,d,c),"World-quart chunk seam");
                if(selected){require(LowlandMeadow.potential(x,z,d,c)&&!LowlandMeadow.contains(x,z,d,c),"Exclusive meadow partition");require(!OakBirchWoodland.contains(seed,x,z,d,c)&&!WarmWoodland.contains(seed,x,z,d,c)&&!LandVegetation.reserved(d,x,z),"Forest and specialist priorities retained");}
                var site=field.sample(x,z);int cover=Plains.cover(seed,x,z,site,true);counts[cover]++;patches[(z-p.z()+128)*256+x-p.x()+128]=cover;
                require(cover==Plains.cover(seed,x,z,site,true),"Seeded order independence");require(Plains.cover(seed,x,z,site,false)==7,"Canopy clearance suppresses all stems");require(Plains.cover(seed,x,z,site.shade(true),true)!=9,"Tall grass suppressed in shade");
                if(Math.floorMod(x,16)==15&&cover!=Plains.cover(seed,x+1,z,field.sample(x+1,z),true))transitions++;
            }
            // Tall stems must follow coherent colonies rather than independent uniform scatter.
            for(int z=0;z<255;z++)for(int x=0;x<255;x++)if(patches[z*256+x]==9){pairs+=2;if(patches[z*256+x+1]==9)adjacent++;if(patches[(z+1)*256+x]==9)adjacent++;}
            require(counts[7]>20000&&counts[8]>5000&&counts[9]>100&&counts[4]+counts[5]+counts[6]>20,"Mixed open sward,short/tall grass and occasional forbs");
            require(adjacent/(double)pairs>counts[9]/65536.0*1.3,"Measurable tall-grass colony adjacency");require(transitions>0,"Cover does not reset to repeated chunk borders");
            require(Math.floorMod(d.sample(p.x(),p.z()).height(),16)==14,"Two-block fixture crosses vertical section boundary");
            System.out.printf("PLAINS_PATTERN seed=%d point=%s climate=%s covers=%s tallNeighbour=%.3f globalTall=%.3f%n",seed,p,c.sample(p.x(),p.z(),d::sample),Arrays.toString(counts),adjacent/(double)pairs,counts[9]/65536.0);
        }
        System.out.println("PLAINS_CLIMATE_PARTITION_COLONIES_SEAMS_PASS");
    }
    private static void checkFlowerColonies(long seed){
        var site=new GroundEcology.Site(.5,0,0,0,0,false);
        int[] cover=new int[256*256],species=new int[3];int flowers=0,pairs=0,same=0;
        for(int z=0;z<256;z++)for(int x=0;x<256;x++){
            int value=Plains.cover(seed,x-128,z-128,site,true);cover[z*256+x]=value;
            if(value>=4&&value<=6){flowers++;species[value-4]++;}
            int shaded=Plains.cover(seed,x-128,z-128,site.shade(true),true);
            require(shaded<4||shaded>6,"Flowers require open sky");
        }
        for(int z=0;z<255;z++)for(int x=0;x<255;x++){
            int value=cover[z*256+x];if(value<4||value>6)continue;
            pairs+=2;if(cover[z*256+x+1]==value)same++;if(cover[(z+1)*256+x]==value)same++;
        }
        require(flowers>300&&flowers<2600,"Restrained visible flowers leave over 96% of open sward for grass");
        for(int count:species)require(count>30,"All three existing forb species retained");
        require(same/(double)pairs>.15,"Flowers form neighbouring groups of the same species");
        System.out.printf("PLAINS_FLOWER_COLONIES seed=%d flowers=%d species=%s sameSpeciesNeighbour=%.3f%n",seed,flowers,Arrays.toString(species),same/(double)pairs);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private PlainsCheck(){}
}
