package blockbranch.wildlife;
public final class WildlifeTestRuntime {
    public static void initialize() {
        if(Boolean.getBoolean("blockbranch.wolfDeerSmoke"))WolfDeerSmoke.register();
        if(Boolean.getBoolean("blockbranch.utilitySmoke"))VanillaUtilitySmoke.register();
        if(Boolean.getBoolean("blockbranch.integrationSmoke"))WildlifeIntegrationSmoke.register();
        if(Boolean.getBoolean("blockbranch.sealSmoke"))SealSmoke.register();
        if(Boolean.getBoolean("blockbranch.perchSmoke"))PerchSmoke.register();
        if(Boolean.getBoolean("blockbranch.beaverSmoke"))BeaverSmoke.register();
        if(Boolean.getBoolean("blockbranch.mallardSmoke"))MallardSmoke.register();
        if(Boolean.getBoolean("blockbranch.itemSmoke"))WildlifeItemSmoke.register();
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(WildlifeProbe.TYPE,WildlifeProbe.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(WildlifeProbe.TYPE,WildlifeProbe.CODEC);
        if(Boolean.getBoolean("blockbranch.populationAudit"))blockbranch.realistic.WildlifePopulationAudit.register();
        if(Boolean.getBoolean("blockbranch.beaverReview"))blockbranch.realistic.BeaverReviewScene.register();
        if(Boolean.getBoolean("blockbranch.sealReview"))blockbranch.realistic.SealReviewScene.register();
        if(Boolean.getBoolean("blockbranch.perchReview"))blockbranch.realistic.PerchReviewScene.register();
        if(Boolean.getBoolean("blockbranch.mallardReview"))blockbranch.realistic.MallardReviewScene.register();
        if(Boolean.getBoolean("blockbranch.bearReview"))blockbranch.realistic.BearReviewScene.register();
        if(Boolean.getBoolean("blockbranch.bearSmoke"))BrownBearSmoke.register();
        if(Boolean.getBoolean("blockbranch.bisonSmoke"))BisonSmoke.register();
        if(Boolean.getBoolean("blockbranch.bisonReview"))blockbranch.realistic.BisonReviewScene.register();
        if (Boolean.getBoolean("blockbranch.wildlifeSmoke")) WildlifeSmoke.register();
        if (Boolean.getBoolean("blockbranch.deerSmoke")) RedDeerSmoke.register();
        if (Boolean.getBoolean("blockbranch.wildlifeReview")) blockbranch.realistic.WildlifeReviewScene.register();
        if (Boolean.getBoolean("blockbranch.wildlifeSurvey")) blockbranch.realistic.WildlifeSpawnSurvey.register();
    }
}
