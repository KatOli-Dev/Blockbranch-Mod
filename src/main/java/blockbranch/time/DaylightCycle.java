package blockbranch.time;

import blockbranch.Blockbranch;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Install a longer daylight default through the native saved/synchronized clock. */
public final class DaylightCycle {
    public static final float RATE=1.0f/15.0f;
    public static final SavedDataType<Applied> TYPE=new SavedDataType<>(Blockbranch.id("daylight_cycle"),Applied::new,
        Codec.BOOL.fieldOf("applied").xmap(Applied::new,state->state.applied).codec(),null);

    public static void initialize(){ServerLifecycleEvents.SERVER_STARTED.register(DaylightCycle::applyDefault);}

    public static void applyDefault(MinecraftServer server){
        var marker=server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if(marker.applied)return;
        var clock=server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        var instance=server.clockManager().getInstance(clock);
        // Preserve explicit non-default rates, clock position/fraction and pause state.
        // The marker also preserves a later operator choice of the vanilla rate on reload.
        if(instance.rate()==1.0f){
            server.clockManager().setRate(clock,RATE);
            Blockbranch.LOGGER.info("Installed five-hour Overworld daylight default: rate={}",RATE);
        }else Blockbranch.LOGGER.info("Preserved existing Overworld clock rate {}",instance.rate());
        marker.applied=true;
        marker.setDirty();
    }

    public static final class Applied extends SavedData {
        private boolean applied;
        public Applied(){this(false);}
        private Applied(boolean applied){this.applied=applied;}
        public boolean applied(){return applied;}
    }
    private DaylightCycle(){}
}
