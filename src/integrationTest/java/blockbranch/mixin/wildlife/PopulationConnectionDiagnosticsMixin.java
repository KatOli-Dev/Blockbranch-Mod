package blockbranch.mixin.wildlife;

import blockbranch.Blockbranch;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opt-in evidence for QA connections; never changes timeout or packet behavior. */
@Mixin(Connection.class)
abstract class PopulationConnectionDiagnosticsMixin {
    @Shadow public abstract net.minecraft.network.protocol.PacketFlow getReceiving();
    @Shadow public abstract boolean isMemoryConnection();

    @Inject(method="exceptionCaught",at=@At("HEAD"))
    private void blockbranch$exception(io.netty.channel.ChannelHandlerContext context,Throwable failure,CallbackInfo callback){
        if((Boolean.getBoolean("blockbranch.terrainAudit")||Boolean.getBoolean("blockbranch.populationAudit")||Boolean.getBoolean("blockbranch.singleplayerSmoke")))
            Blockbranch.LOGGER.error("WILDLIFE_CONNECTION_EXCEPTION receiving={} memory={}",getReceiving(),isMemoryConnection(),failure);
    }
    @Inject(method="disconnect(Lnet/minecraft/network/DisconnectionDetails;)V",at=@At("HEAD"))
    private void blockbranch$disconnect(DisconnectionDetails details,CallbackInfo callback){
        if((Boolean.getBoolean("blockbranch.terrainAudit")||Boolean.getBoolean("blockbranch.populationAudit")||Boolean.getBoolean("blockbranch.singleplayerSmoke")))
            Blockbranch.LOGGER.info("WILDLIFE_CONNECTION_DISCONNECT receiving={} memory={} reason={}",getReceiving(),isMemoryConnection(),details.reason().getString());
    }
}
