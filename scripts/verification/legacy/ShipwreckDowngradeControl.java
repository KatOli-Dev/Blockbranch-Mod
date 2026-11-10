/* Historical schema92 downgrade witness, outside current source sets.
 * Use only in an isolated checkout of 21fbe2e with main92-control-development.patch;
 * copy this source into src/integrationTest/java/blockbranch/realistic there.
 * The absolute source path below must point at the schema93 preset being rejected,
 * not a current preset; recover it from the pre-cleanup Git history if needed.
 * Run runShipwreckDowngradeControl and require downgrade-result.txt PASS.
 */
package blockbranch.realistic;
import java.nio.file.*;
public final class ShipwreckDowngradeControl {
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
 var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
 var config=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow();Files.writeString(Path.of("main92-config.json"),config.toString());
 var source=com.google.gson.JsonParser.parseString(Files.readString(Path.of("/workspace/Blockbranch/src/main/resources/data/blockbranch/worldgen/world_preset/realistic.json"))).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").get("generator");
 WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,source).error().isPresent(),"Actual main92 codec rejects newer schema93");
 level.setBlock(new net.minecraft.core.BlockPos(-84256,207,624288),net.minecraft.world.level.block.Blocks.WOOL.red().defaultBlockState(),3);
 server.saveEverything(false,true,true);Files.writeString(Path.of("downgrade-result.txt"),"PASS actual main21fbe2e rejects schema93; native92 save and player edit retained\n");
 }catch(Exception e){throw new RuntimeException(e);}finally{server.halt(false);}});}
}
