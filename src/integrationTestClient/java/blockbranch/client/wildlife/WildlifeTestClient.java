package blockbranch.client.wildlife;
public final class WildlifeTestClient {
    public static void initialize() {
        if(Boolean.getBoolean("blockbranch.wildlifeReview"))net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client->BoarModelCheck.verifyRevision());
        if(Boolean.getBoolean("blockbranch.approvedAudioCheck"))WildlifeApprovedAudioCheck.register();
        if(Boolean.getBoolean("blockbranch.populationAudit"))WildlifePopulationClient.register();
        if(Boolean.getBoolean("blockbranch.singleplayerSmoke"))WildlifeSingleplayerSmoke.register();
        if(Boolean.getBoolean("blockbranch.sealReview"))blockbranch.client.realistic.SealClientSmoke.register();
        if(Boolean.getBoolean("blockbranch.perchReview"))blockbranch.client.realistic.PerchClientSmoke.register();
        if(Boolean.getBoolean("blockbranch.beaverReview"))blockbranch.client.realistic.BeaverClientSmoke.register();
        if(Boolean.getBoolean("blockbranch.mallardReview"))blockbranch.client.realistic.MallardClientSmoke.register();
        if(Boolean.getBoolean("blockbranch.bearReview"))blockbranch.client.realistic.BearClientSmoke.register();
        if(Boolean.getBoolean("blockbranch.bisonReview"))blockbranch.client.realistic.BisonClientSmoke.register();
        if(Boolean.getBoolean("blockbranch.wildlifeReview"))blockbranch.client.realistic.WildlifeClientSmoke.register();
    }
}
