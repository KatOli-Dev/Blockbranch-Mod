package blockbranch.client.realistic;
public final class RealisticTestClientRuntime {
    public static void initialize() {
        if(Boolean.getBoolean("blockbranch.meadowForbClientOnly"))MeadowForbClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.tundraFloorClientOnly"))TundraFloorClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.gravelDesertClientOnly"))GravelDesertClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.icebergClientOnly"))IcebergClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.seaIceClientOnly"))SeaIceClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.stonyShoreClientOnly"))StonyShoreClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.warmDappledClientOnly"))WarmDappledClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.coldUplandsClientOnly"))ColdUplandsClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.badlandsClientOnly"))BadlandsClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.peaksClientOnly"))MountainPeaksClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.savannaUplandsClientOnly"))SavannaUplandsClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.windsweptClientOnly"))WindsweptUplandsClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.tropicalClientOnly"))TropicalForestClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.darkCherryClientOnly"))DarkCherryClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.temperateVariantClientOnly"))TemperateForestVariantClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.oldGrowthClientOnly"))OldGrowthTaigaClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.lukewarmOceanClientOnly")||Boolean.getBoolean("blockbranch.deepLukewarmOceanClientOnly"))LukewarmOceanClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.beachClientOnly"))CoastalBeachClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.riverClientOnly")||Boolean.getBoolean("blockbranch.frozenRiverClientOnly"))RiverClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.montaneMeadowClientOnly"))MontaneMeadowClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.snowPlainsClientOnly")||Boolean.getBoolean("blockbranch.snowSlopesClientOnly"))OpenSnowClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.oceanClientOnly"))OceanClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.groveClientOnly"))ForestClientSmoke.register(false,false);
        else if(Boolean.getBoolean("blockbranch.snowyTaigaClientOnly"))ForestClientSmoke.register(false,false);
        else if(Boolean.getBoolean("blockbranch.taigaClientOnly"))ForestClientSmoke.register(false,false);
        else if(Boolean.getBoolean("blockbranch.mangroveClientOnly"))ForestClientSmoke.register(false,false);
        else if(Boolean.getBoolean("blockbranch.swampClientOnly"))ForestClientSmoke.register(false,true);
        else if(Boolean.getBoolean("blockbranch.birchForestClientOnly"))ForestClientSmoke.register(true);
        else if(Boolean.getBoolean("blockbranch.forestClientOnly"))ForestClientSmoke.register(false);
        else if(Boolean.getBoolean("blockbranch.sunflowerClientOnly"))PlainsClientSmoke.register(true);
        else if(Boolean.getBoolean("blockbranch.plainsClientOnly"))PlainsClientSmoke.register(false);
        else if(Boolean.getBoolean("blockbranch.desertClientOnly"))SandyDesertClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.jungleClientOnly"))JungleClientSmoke.register();
        else if(Boolean.getBoolean("blockbranch.savannaClientOnly"))SavannaClientSmoke.register();
        else if (Boolean.getBoolean("blockbranch.realisticClientSmoke")) RealisticClientSmoke.register();
    }
}
