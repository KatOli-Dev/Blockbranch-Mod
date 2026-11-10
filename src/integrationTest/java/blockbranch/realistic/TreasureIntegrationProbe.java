package blockbranch.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;
public final class TreasureIntegrationProbe {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed());
  var shore=LowGradientSandyShoreFixtures.fixture(d,random.seed());ChunkPos chunk=null;
  outer:for(int dz=-64;dz<=64;dz+=16)for(int dx=-64;dx<=64;dx+=16){var candidate=new ChunkPos((shore.x()+dx)>>4,(shore.z()+dz)>>4);int x=candidate.getMinBlockX()+9,z=candidate.getMinBlockZ()+9;
   if(CoastalBeach.contains(x,z,d,c,false)||CoastalBeach.contains(x,z,d,c,true)){chunk=candidate;break outer;}}
  WellInteraction.require(chunk!=null,"Natural aligned beach found");
  var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.BURIED_TREASURE);
  var start=holder.value().generate(holder,level.dimension(),level.registryAccess(),g,g.getBiomeSource(),random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED),random,server.getStructureTemplateManager(),random.seed(),chunk,0,level,h->true);
  WellInteraction.require(start.isValid(),"Native buried treasure start");
  net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(level.getChunk(chunk.x(),chunk.z()),java.util.Set.of(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG));
  start.placeInChunk(level,level.structureManager(),g,net.minecraft.util.RandomSource.create(19),new BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ()),chunk);
  BlockPos found=null;for(int y=80;y>=0;y--){var pos=new BlockPos(chunk.getMinBlockX()+9,y,chunk.getMinBlockZ()+9);if(level.getBlockState(pos).is(Blocks.CHEST)){found=pos;break;}}
  if(found==null){var states=new java.util.ArrayList<String>();for(int y=80;y>=40;y--)states.add(y+":"+level.getBlockState(new BlockPos(chunk.getMinBlockX()+9,y,chunk.getMinBlockZ()+9)));throw new AssertionError("Native piece missing; height="+level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,chunk.getMinBlockX()+9,chunk.getMinBlockZ()+9)+" states="+states);}
  WellInteraction.require(found!=null,"Native piece placed chest");var entity=level.getBlockEntity(found);WellInteraction.require(entity!=null&&entity.saveWithoutMetadata(level.registryAccess()).toString().contains("buried_treasure"),"Native buried treasure loot table");
  server.saveEverything(false,true,true);Files.writeString(Path.of("treasure-probe.txt"),"PASS chest="+found+" surface="+d.sample(found.getX(),found.getZ()).height()+" biome="+level.getBiome(found)+" loot="+entity.saveWithoutMetadata(level.registryAccess()));
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("TREASURE_PROBE_FAIL",t);try{Files.writeString(Path.of("treasure-probe.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
