package blockbranch.realistic;
import java.util.*;

/** Diagnose the previous tile artifact, test ecological response, clonal grouping, cache bounds and seams. */
public final class GroundFoliageCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            int[] cover=new int[7];int edgeChanges=0,edges=0,innerChanges=0,inner=0,legacyInner=0,fullTiles=0,flowerPairs=0,flowers=0;
            var neutral=GroundEcology.NEUTRAL;var wet=new GroundEcology.Site(.75,.7,.05,.16,0,false);var dry=new GroundEcology.Site(.30,-.7,.9,.16,0,false);int wetPlants=0,dryPlants=0,sunFlowers=0,shadeFlowers=0;
            for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
                int c=GroundEcology.meadow(seed,x,z,neutral);cover[c]++;
                require(c==GroundEcology.meadow(seed,x,z,neutral),"Stable absolute-coordinate colonies");
                int near=GroundEcology.meadow(seed,x-1,z,neutral);if(Math.floorMod(x,12)==0){edges++;if((c>1)!=(near>1))edgeChanges++;}else{inner++;if((c>1)!=(near>1))innerChanges++;if(legacy(seed,x,z)!=legacy(seed,x-1,z))legacyInner++;}
                if(c>=4){flowers++;if(near==c)flowerPairs++;}
                if(GroundEcology.meadow(seed,x,z,wet)>1)wetPlants++;if(GroundEcology.meadow(seed,x,z,dry)>1)dryPlants++;
                if(c>=4)sunFlowers++;if(GroundEcology.meadow(seed,x,z,neutral.shade(true))>=4)shadeFlowers++;
            }
            for(int tz=-120;tz<=108;tz+=12)for(int tx=-120;tx<=108;tx+=12){int plants=0;for(int z=tz;z<tz+12;z++)for(int x=tx;x<tx+12;x++)if(GroundEcology.meadow(seed,x,z,neutral)>1)plants++;if(plants==144)fullTiles++;}
            double edgeRate=edgeChanges/(double)edges,innerRate=innerChanges/(double)inner;
            require(legacyInner==0,"Previous standing vegetation was binary inside every 12 m tile");
            require(fullTiles==0&&innerRate>.15&&edgeRate/innerRate>.75&&edgeRate/innerRate<1.25,"Old 12 m boundaries no longer control occupancy");
            for(int c=1;c<=6;c++)require(cover[c]>50,"All existing meadow components retained");
            require(wetPlants>dryPlants*1.35,"Moister sheltered soil supports denser standing vegetation");
            require(shadeFlowers<sunFlowers*.25,"Shade strongly suppresses sunlit meadow flowers");
            require(flowerPairs/(double)flowers>flowers/65536.0*2,"Flowers group by colony rather than independent uniform scatter");
            System.out.printf(Locale.ROOT,"GROUND_FOLIAGE_PATTERN seed=%d cover=%s edgeRate=%.4f interiorRate=%.4f legacyInteriorChanges=%d filled12mTiles=%d wetPlants=%d dryPlants=%d flowers=%d shadedFlowers=%d sameFlowerNeighbours=%d%n",seed,Arrays.toString(cover),edgeRate,innerRate,legacyInner,fullTiles,wetPlants,dryPlants,sunFlowers,shadeFlowers,flowerPairs);
        }
        var climate=new ClimateField(424242);var flat=new GroundEcology.Field((x,z)->new DrainageNetwork.Sample(263,DrainageNetwork.DRY,0,false),climate);
        var bowl=new GroundEcology.Field((x,z)->new DrainageNetwork.Sample(263-(int)Math.round(4*Math.exp(-((double)x*x+(double)z*z)/512)),DrainageNetwork.DRY,0,false),climate);
        var crest=new GroundEcology.Field((x,z)->new DrainageNetwork.Sample(263+(int)Math.round(4*Math.exp(-((double)x*x+(double)z*z)/512)),DrainageNetwork.DRY,0,false),climate);
        require(bowl.sample(0,0).wetness()>flat.sample(0,0).wetness()+.03&&crest.sample(0,0).wetness()<flat.sample(0,0).wetness()-.03,"Actual local relief modifies regional moisture continuously");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var field=new GroundEcology.Field(d::sample,c);var p=blockbranch.realistic.LowlandMeadowFixtures.fixture(d,seed);var recorded=new HashMap<String,Integer>();
            for(int z=p.z()-32;z<p.z()+32;z++)for(int x=p.x()-32;x<p.x()+32;x++)recorded.put(x+","+z,GroundEcology.meadow(seed,x,z,field.sample(x,z)));
            for(int z=p.z()+31;z>=p.z()-32;z--)for(int x=p.x()+31;x>=p.x()-32;x--)require(recorded.get(x+","+z)==GroundEcology.meadow(seed,x,z,field.sample(x,z)),"Reverse native-chunk requests preserve all foliage states");
            for(int x:new int[]{-33,-32,-17,-16,-1,0,15,16,31,32}){var a=field.sample(x,0);var b=field.sample(x+1,0);require(Math.abs(a.wetness()-b.wetness())<.04,"No 16/32 m environmental cache boundary jump");}
            for(int i=0;i<2048;i++)field.sample(i*97-100000,i*53-70000);require(field.cachedSamples()<=512,"Environmental cache remains bounded");
        }
        System.out.println("ECOLOGICAL_MEADOW_FOLIAGE_CLUSTER_SEAM_PASS");
    }
    private static boolean legacy(long seed,int x,int z){long v=OakBirchWoodland.mix(seed^Math.floorDiv(x,12)*0x9E3779B97F4A7C15L^Math.floorDiv(z,12)*0xC2B2AE3D27D4EB4FL);int group=(int)(v&15);return group>=4&&group<10||group>=14;}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private GroundFoliageCheck(){}
}
