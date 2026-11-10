package blockbranch.mixin.qa;
import blockbranch.client.cubic.ClientColumnRuntime;
import blockbranch.qa.PacketOccupancyProbe;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.client.multiplayer.ClientChunkCache;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ClientColumnRuntime.class)
abstract class PacketOccupancyObserverMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method={"receiveWindow","receiveColumn"},at=@At("HEAD"))
 private static void blockbranch$protocol(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){blockbranch.client.cubic.ColumnProtocolAudit.receivedState();}
 @WrapOperation(method="receiveWindow",at=@At(value="INVOKE",target="Lblockbranch/client/cubic/ClientColumnRuntime;windowEmpty(Lblockbranch/cubic/SparseColumn;ILblockbranch/cubic/SectionOccupancy;)Z"))
 private static boolean blockbranch$legacy(blockbranch.cubic.SparseColumn sparse,int y,blockbranch.cubic.SectionOccupancy summary,Operation<Boolean> original){return PacketOccupancyProbe.legacy&&Thread.currentThread()==PacketOccupancyProbe.owner?sparse.blockbranch$read(y).hasOnlyAir():original.call(sparse,y,summary);}
 @WrapOperation(method={"receiveWindow","receiveColumn"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/lighting/LevelLightEngine;updateSectionStatus(Lnet/minecraft/core/SectionPos;Z)V"))
 private static void blockbranch$light(LevelLightEngine engine,SectionPos pos,boolean empty,Operation<Void> original){PacketOccupancyProbe.event("light",pos.y(),empty);original.call(engine,pos,empty);}
 @WrapOperation(method={"receiveWindow","receiveColumn"},at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientChunkCache;onSectionEmptinessChanged(IIIZ)V"))
 private static void blockbranch$empty(ClientChunkCache cache,int x,int y,int z,boolean empty,Operation<Void> original){PacketOccupancyProbe.event("occupancy",y,empty);original.call(cache,x,y,z,empty);}
 @WrapOperation(method="receiveWindow",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/lighting/LevelLightEngine;queueSectionData(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/DataLayer;)V"))
 private static void blockbranch$queue(LevelLightEngine engine,net.minecraft.world.level.LightLayer layer,SectionPos pos,net.minecraft.world.level.chunk.DataLayer data,Operation<Void> original){PacketOccupancyProbe.event("queue-"+layer,pos.y(),false);original.call(engine,layer,pos,data);}
 @WrapOperation(method={"receiveWindow","receiveColumn"},at=@At(value="INVOKE",target="Lblockbranch/client/realistic/ColumnReceiveBatch;changed(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;I)V"))
 private static void blockbranch$dirty(net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.world.level.chunk.ChunkAccess chunk,int y,Operation<Void> original){PacketOccupancyProbe.event("dirty",y,false);original.call(level,chunk,y);}
}
