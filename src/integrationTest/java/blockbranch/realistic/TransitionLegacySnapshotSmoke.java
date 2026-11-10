package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.block.Blocks;

/** Runs unchanged on the frozen prior production sources and the candidate. */
public final class TransitionLegacySnapshotSmoke {
 public static void verify(ServerLevel level,RealisticGenerator g)throws Exception{
  Path dir=Path.of(System.getProperty("blockbranch.memorySmokeResult")).toAbsolutePath().getParent();boolean reload=Files.exists(dir.resolve("legacy-generator.json"));
  var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var json=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
  if(reload){var old=com.google.gson.JsonParser.parseString(Files.readString(dir.resolve("legacy-generator.json"))).getAsJsonObject();require(json.equals(old),"No generator promotion or backfill");}
  else{require(json.get("shape_version").getAsInt()==255&&!json.has("biome_transition_definition"),"Actual prior-main schema255 world");Files.writeString(dir.resolve("legacy-generator.json"),json.toString());}
  int cx=-629922>>4,cz=-1698398>>4;
  var chunk=level.getChunk(cx,cz);var profile=((SparseColumn)chunk).blockbranch$terrain().base();var tag=profile.encode(level.palettedContainerFactory());
  var edit=new BlockPos(-629922,416,-1698398);
  Path saved=dir.resolve("legacy-profile.nbt");
  if(reload){require(tag.equals(NbtIo.readCompressed(saved,NbtAccounter.unlimitedHeap())),"Legacy saved canopy/profile arrays unchanged");require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Legacy player edit retained");}
  else{NbtIo.writeCompressed(tag,saved);level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),2);}
  // A profile preview does not load its future destination chunk. The subsequent
  // candidate launch generates that coordinate using the saved legacy settings.
  Path future=dir.resolve("legacy-future.nbt");
  if(!reload){var proto=new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(cx+12,cz+12),net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);NbtIo.writeCompressed(g.profile(proto,level.getChunkSource().randomState()).encode(level.palettedContainerFactory()),future);}
  else{var next=level.getChunk(cx+12,cz+12);require(((SparseColumn)next).blockbranch$terrain().base().encode(level.palettedContainerFactory()).equals(NbtIo.readCompressed(future,NbtAccounter.unlimitedHeap())),"Previously unexplored chunk matches actual prior-main preview exactly");}
  Blockbranch.LOGGER.info("TRANSITION_LEGACY_SNAPSHOT_PASS reload={} schema={}",reload,json.get("shape_version"));
 }
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
