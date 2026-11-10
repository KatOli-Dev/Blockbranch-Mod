package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.item.*;
public final class TreasureQualification {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
  WellInteraction.require(g.discoveryDefinition()==1,"New discovery definition");
  var readCount=new java.util.concurrent.atomic.AtomicInteger();
  var empty=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(TreasureQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(proxy,method,args)->{
   if(method.getName().equals("getHeight"))return 70;
   if(method.getName().equals("getMinY"))return TerrainShape.FLOOR;
   if(method.getName().equals("getBlockState")){readCount.incrementAndGet();return Blocks.DEEPSLATE.defaultBlockState();}
   return method.invoke(level,args);
  });
  new net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces.BuriedTreasurePiece(new BlockPos(9,90,9)).postProcess(empty,level.structureManager(),g,net.minecraft.util.RandomSource.create(1),new net.minecraft.world.level.levelgen.structure.BoundingBox(0,TerrainShape.FLOOR,0,15,TerrainShape.CEILING-1,15),new net.minecraft.world.level.ChunkPos(0,0),BlockPos.ZERO);
  WellInteraction.require(readCount.get()==140,"Native failed piece search is bounded to positive coastal levels");
  var codecOps=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  var config=RealisticGenerator.CODEC.codec().encodeStart(codecOps,g).getOrThrow().getAsJsonObject();
  var legacy=config.deepCopy();legacy.remove("small_discovery_definition");legacy.remove("treasure_cache");legacy.remove("desert_waygates");legacy.addProperty("shape_version",88);
  WellInteraction.require(RealisticGenerator.CODEC.codec().parse(codecOps,legacy).getOrThrow().discoveryDefinition()==0,"Existing well worlds remain opted out of treasure");
  var malformed=config.deepCopy();malformed.remove("small_discovery_definition");WellInteraction.require(RealisticGenerator.CODEC.codec().parse(codecOps,malformed).error().isPresent(),"New schema rejects missing discovery definition");
  malformed=config.deepCopy();malformed.addProperty("treasure_cache","invalid optional cache");WellInteraction.require(RealisticGenerator.CODEC.codec().parse(codecOps,malformed).result().isPresent(),"Malformed derived cache does not reject world");var planner=g.treasures(random);long worst=0,total=0;int groups=0;
  for(var group:planner.orderedGroups(BlockPos.ZERO,4194304)){long start=System.nanoTime();group.site();long elapsed=System.nanoTime()-start;worst=Math.max(worst,elapsed);total+=elapsed;groups++;}
  var sites=planner.sites();WellInteraction.require(!sites.isEmpty(),"Natural coastal treasure plans");
  var marker=Path.of("chest.txt");boolean reload=Files.exists(marker);BlockPos chestPos=null;
  if(reload){var q=Files.readString(marker).trim().split(",");chestPos=new BlockPos(Integer.parseInt(q[0]),Integer.parseInt(q[1]),Integer.parseInt(q[2]));WellInteraction.require(g.treasureRestored(),"Native saved treasure cache restored");}
  else{var p=sites.getFirst();level.getChunk(p.x()>>4,p.z()>>4);for(int y=80;y>=0;y--)if(level.getBlockState(new BlockPos(p.x(),y,p.z())).is(Blocks.CHEST)){chestPos=new BlockPos(p.x(),y,p.z());break;}}
  WellInteraction.require(chestPos!=null,"Scheduled native treasure chest generated");var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);WellInteraction.require(chest!=null,"Native chest block entity");
  var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.BURIED_TREASURE);
  var nearest=level.findNearestMapStructure(HolderSet.direct(holder),chestPos,1,false);WellInteraction.require(nearest!=null&&nearest.getX()==chestPos.getX()&&nearest.getZ()==chestPos.getZ(),"Native map lookup resolves chest chunk");
  var report=new LinkedHashMap<String,Object>();report.put("resolvedGroups",groups);report.put("worstColdGroupNanos",worst);report.put("totalGroupResolutionNanos",total);report.put("reload",reload);report.put("plans",sites.size());report.put("preparationNanos",g.treasureInitializationNanos());report.put("cacheRestored",g.treasureRestored());report.put("chest",chestPos.toShortString());report.put("surface",g.drainage(random).sample(chestPos.getX(),chestPos.getZ()).height());
  if(!reload){
   WellInteraction.require(chest.saveWithoutMetadata(level.registryAccess()).toString().contains("minecraft:chests/buried_treasure"),"Vanilla buried treasure loot table");
   chest.unpackLootTable(null);int hearts=0,slot=-1;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.HEART_OF_THE_SEA)){hearts+=chest.getItem(i).getCount();slot=i;}
   WellInteraction.require(hearts==1,"Exactly one vanilla heart of the sea");chest.setItem(slot,ItemStack.EMPTY);chest.setChanged();
   var first=level.findNearestMapStructure(HolderSet.direct(holder),chestPos,1,true);WellInteraction.require(first!=null&&first.getX()==chestPos.getX()&&first.getZ()==chestPos.getZ(),"Map can claim unreferenced start");
   var second=level.findNearestMapStructure(HolderSet.direct(holder),chestPos,1,true);WellInteraction.require(second==null||second.getX()!=chestPos.getX()||second.getZ()!=chestPos.getZ(),"Skip-known map lookup does not return same treasure");
   Files.writeString(marker,chestPos.getX()+","+chestPos.getY()+","+chestPos.getZ());report.put("vanillaHearts",hearts);
  }else{
   for(int i=0;i<chest.getContainerSize();i++)WellInteraction.require(!chest.getItem(i).is(Items.HEART_OF_THE_SEA),"Player loot removal survives reopening");
   var skip=level.findNearestMapStructure(HolderSet.direct(holder),chestPos,1,true);WellInteraction.require(skip==null||skip.getX()!=chestPos.getX()||skip.getZ()!=chestPos.getZ(),"Known treasure reference survives reopening");
  }
  int checked=0;for(var site:sites){if(Math.abs(site.x())>1700000||Math.abs(site.z())>1700000)continue;
   level.getChunk(site.x()>>4,site.z()>>4);BlockPos found=null;for(int y=80;y>=0;y--)if(level.getBlockState(new BlockPos(site.x(),y,site.z())).is(Blocks.CHEST)){found=new BlockPos(site.x(),y,site.z());break;}
   WellInteraction.require(found!=null,"Representative interior native treasure");
   WellInteraction.require(level.getBlockEntity(found) instanceof ChestBlockEntity,"Representative native chest entity");
   if(checked==0)Files.writeString(Path.of("view.txt"),found.getX()+","+found.getY()+","+found.getZ());
   if(++checked==3)break;
  }
  WellInteraction.require(checked==3,"Three interior native sites");report.put("additionalNativeSites",checked);
  var encoded=BuriedTreasureDiscovery.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,planner).getOrThrow();Files.writeString(Path.of("treasure-catalog.json"),encoded.toString());var decoded=BuriedTreasureDiscovery.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
  var corrupted=encoded.deepCopy().getAsJsonObject();corrupted.addProperty("checksum","bad");WellInteraction.require(!BuriedTreasureDiscovery.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,corrupted).getOrThrow().valid(random.seed(),true),"Corrupt checksum invalidates derived cache");
  decoded.bind(g.drainage(random),new ClimateField(random.seed()));
  decoded.orderedGroups(BlockPos.ZERO,4194304).parallelStream().forEach(group->group.site());
  WellInteraction.require(decoded.valid(random.seed(),true)&&decoded.sites().equals(sites),"Catalog codec, concurrent resolution and different request order preserve all sites");WellInteraction.require(!decoded.valid(random.seed()+1,true),"Seed mismatch invalidates cache");
  server.saveEverything(false,true,true);Files.writeString(Path.of("treasure.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(Path.of("treasure-result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("TREASURE_QUALIFICATION_FAIL",t);try{Files.writeString(Path.of("treasure-result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
