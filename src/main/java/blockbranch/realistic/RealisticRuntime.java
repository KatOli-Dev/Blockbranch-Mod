package blockbranch.realistic;

import blockbranch.Blockbranch;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class RealisticRuntime implements ModInitializer {
    @Override public void onInitialize() {
        blockbranch.horizon.HorizonService.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{
            // 26.3 stores world generation settings as SavedData. Only a rebuilt optional cache
            // needs a write; the native save path owns writing and error handling.
            var settings=server.getWorldGenSettings();
            for(var stem:settings.dimensions().dimensions().values())
                if(stem.generator() instanceof RealisticGenerator g && g.structureCacheNeedsSave())settings.setDirty();
        });
        // A demand-built catalog may finish after SERVER_STARTED. Saving reads
        // either a complete immutable cache or the existing no-cache marker;
        // this notification never starts planning or waits for a region.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.BEFORE_SAVE.register((server,flush,force)->{
            var settings=server.getWorldGenSettings();
            for(var stem:settings.dimensions().dimensions().values())
                if(stem.generator() instanceof RealisticGenerator g&&g.completedWetlandHutCacheNeedsSave())settings.setDirty();
        });
        ScreeContent.initialize();
        ReedbedContent.initialize();
        RiverMarginContent.initialize();
        SandyShoreContent.initialize();
        MeadowContent.initialize();
        SteppeContent.initialize();
        DesertContent.initialize();
        DarkOakBranchContent.initialize();
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, Blockbranch.id("realistic"), RealisticGenerator.CODEC);
        Registry.register(BuiltInRegistries.BIOME_SOURCE, Blockbranch.id("scree"), ScreeBiomeSource.CODEC);
    }
}
