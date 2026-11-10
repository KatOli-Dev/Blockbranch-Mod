package blockbranch.client.realistic;

import net.fabricmc.api.ClientModInitializer;

public final class RealisticClientRuntime implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ColumnReceiveBatch.register();
    }
}
