package blockbranch.realistic;

/** Original material-fix routing hashes remain frozen on the legacy coast; new routing changes require actual nearby coastal work. */
public final class SurfaceRoutingCheck {
    public static void main(String[] args){
        long[] seeds={424242,1,-17},expected={4829185425249317569L,-382732763971511487L,-3418010469850627423L};
        for(int n=0;n<seeds.length;n++){
            long seed=seeds[n],hash=1;var shape=new TerrainShape(seed);var d=new DrainageNetwork(shape,seed,false);var formed=new DrainageNetwork(shape,seed);var c=new ClimateField(seed);int intentional=0;
            for(int z=-1900000;z<=1900000;z+=64000)for(int x=-1900000;x<=1900000;x+=64000)for(int offset:new int[]{0,1}){
                int wx=x+offset,wz=z+offset;
                boolean[] domains=domains(seed,wx,wz,d,c),current=domains(seed,wx,wz,formed,c);
                if(!java.util.Arrays.equals(domains,current)){
                    if(!coastalChangeNearby(d,formed,wx,wz))throw new AssertionError("Unrelated routing changed away from actual coastal work at "+wx+","+wz+" seed="+seed);
                    intentional++;
                }
                for(boolean domain:domains)hash=31*hash+(domain?1:0);
            }
            if(hash!=expected[n])throw new AssertionError("Legacy material-fix control changed biome/vegetation routing for seed "+seed+": "+hash);
            System.out.printf("SURFACE_ROUTING_LEGACY_UNCHANGED_COASTAL_CHANGES_LOCAL seed=%d hash=%d intentionalQueries=%d%n",seed,hash,intentional);
        }
    }
    private static boolean[] domains(long seed,int wx,int wz,DrainageNetwork d,ClimateField c){
        return new boolean[]{LowlandMeadow.contains(wx,wz,d,c),OakBirchWoodland.contains(seed,wx,wz,d,c),
                    WarmWoodland.contains(seed,wx,wz,d,c),HumidWoodland.contains(seed,wx,wz,d,c),
                    Jungle.contains(seed,wx,wz,d,c),DrySteppe.contains(wx,wz,d,c),ColdSteppe.contains(wx,wz,d,c),
                    ColdTundra.contains(wx,wz,d,c),Savanna.contains(seed,wx,wz,d,c),GravelDesert.contains(wx,wz,d,c),
                    SandyDesert.contains(seed,wx,wz,d,c),LandVegetation.reserved(d,wx,wz),
                    RoutedRiver.contains(seed,wx,wz,d,c),Swamp.contains(seed,wx,wz,d,c),
                    MarineShelf.contains(wx,wz,d),DeepOcean.contains(wx,wz,d),LakeSediment.contains(wx,wz,d)};
    }
    private static boolean coastalChangeNearby(DrainageNetwork old,DrainageNetwork formed,int x,int z){
        // Neighbouring shore/slope predicates inspect beyond their centre. Require
        // an actual height/water/bedrock change, not merely a broad coast label.
        for(int dz=-256;dz<=256;dz+=16)for(int dx=-256;dx<=256;dx+=16){
            var a=old.sample(x+dx,z+dz);var b=formed.sample(x+dx,z+dz);
            if(a.height()!=b.height()||a.waterLevel()!=b.waterLevel()||a.bedrockCoast()!=b.bedrockCoast())return true;
        }
        return false;
    }
}
