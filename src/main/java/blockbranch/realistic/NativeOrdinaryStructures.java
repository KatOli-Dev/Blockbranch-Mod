package blockbranch.realistic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.*;

/** Schema101 dispatch only. All earlier saved definitions keep their original bodies. */
public final class NativeOrdinaryStructures {
 private NativeOrdinaryStructures(){}
 public static void placeWell(DesertWell wells,WorldGenLevel world,ChunkPos chunk,RealisticGenerator generator){
  wells.forChunk(chunk).filter(p->p.x()>>4==chunk.x()&&p.z()>>4==chunk.z()).ifPresent(p->{
   // The registered vanilla overlay owns its exact template, both randomly selected
   // archaeology sites, loot processors and RNG. Placement density remains regional.
   var feature=world.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(ResourceKey.create(Registries.FEATURE,Identifier.parse("minecraft:desert_well"))).value();
   feature.place(world,generator,net.minecraft.util.RandomSource.create(p.seed()),new BlockPos(p.x(),p.y(),p.z()));
  });
 }
 public static int ruinFloor(RealisticGenerator generator,RandomState random,BoundingBox box){
  // The old footprint predicate has already passed. Embed the native floor at the
  // lowest seabed column so there is no custom pedestal, arcade or retaining wall.
  var terrain=generator.hutPlanningTerrain(random).drainage();int floor=Integer.MAX_VALUE;
  for(int z=box.minZ();z<=box.maxZ();z++)for(int x=box.minX();x<=box.maxX();x++)floor=Math.min(floor,terrain.sample(x,z).height());
  return floor+1;
 }
 public static Structure.GenerationContext portalContext(Structure.GenerationContext c,int surface){
  // Native partly-buried portals use four base columns. A local height window
  // contains their complete search while avoiding half-million-cell columns.
  var bounds=LevelHeightAccessor.create(Math.max(TerrainShape.FLOOR,surface-96),192);
  return new Structure.GenerationContext(c.registryAccess(),c.chunkGenerator(),c.biomeSource(),c.climateSampler(),c.biomeResolver(),c.randomState(),c.structureTemplateManager(),c.random(),c.seed(),c.chunkPos(),bounds,c.validBiome());
 }
}
