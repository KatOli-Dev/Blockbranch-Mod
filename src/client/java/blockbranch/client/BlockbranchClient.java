package blockbranch.client;

import net.fabricmc.api.ClientModInitializer;

public class BlockbranchClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		blockbranch.client.performance.LowEndProfile.register();
		blockbranch.client.cubic.ClientColumnRuntime.initialize();
		blockbranch.client.wildlife.WildlifeClient.initialize();
        blockbranch.client.metallurgy.OrePulseClient.initialize();
        blockbranch.client.horizon.HorizonClient.initialize();
        blockbranch.client.metallurgy.PickaxeSpriteSource.register();
        blockbranch.client.metallurgy.IngotSpriteSource.register();
        blockbranch.client.metallurgy.ToolSpriteSource.register();
        blockbranch.client.metallurgy.ArmorSpriteSource.register();
        blockbranch.client.metallurgy.BlockSpriteSource.register();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client->blockbranch.client.metallurgy.ArmorLayerTexture.registerAll());
	}
}
