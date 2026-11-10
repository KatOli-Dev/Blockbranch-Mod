package blockbranch.client.wildlife;
import blockbranch.wildlife.WildlifeRuntime;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
public final class WildlifeClient {
    public static void initialize() {
        PerchBucketDripSource.register();
        EntityRendererRegistry.register(WildlifeRuntime.WILD_BOAR, BoarRenderer::new);
        EntityRendererRegistry.register(WildlifeRuntime.RED_DEER, DeerRenderer::new);
        EntityRendererRegistry.register(WildlifeRuntime.AMERICAN_BISON,BisonRenderer::new);
        EntityRendererRegistry.register(WildlifeRuntime.BROWN_BEAR,BearRenderer::new);
        EntityRendererRegistry.register(WildlifeRuntime.EURASIAN_BEAVER,BeaverRenderer::new);
        EntityRendererRegistry.register(WildlifeRuntime.MALLARD,MallardRenderer::new);
        EntityRendererRegistry.register(WildlifeRuntime.HARBOR_SEAL,SealRenderer::new);
        EntityRendererRegistry.register(WildlifeRuntime.EUROPEAN_PERCH,PerchRenderer::new);
    }
    private WildlifeClient() {}
}
