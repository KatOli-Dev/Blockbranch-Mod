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

public final class ShipwreckQualification {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();WellInteraction.require(g.shipwreckDefinition()==1,"New wreck definition");
  var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());var config=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();var base=config.deepCopy();base.remove("shipwrecks");base.remove("wetland_huts");base.remove("desert_waygates");base.remove("small_discovery_definition");base.remove("treasure_cache");
  for(int schema:new int[]{88,89,90,91,92,93}){
   var input=base.deepCopy();input.addProperty("shape_version",schema);boolean treasure=schema==89||schema==90||schema==92||schema==93,ship=schema==90||schema==93,gate=schema>=91;
   if(treasure)input.addProperty("small_discovery_definition",1);if(ship)input.add("shipwrecks",new com.google.gson.JsonObject());if(gate)input.add("desert_waygates",new com.google.gson.JsonObject());
   if(ship)input.getAsJsonObject("shipwrecks").addProperty("definition",1);if(gate)input.getAsJsonObject("desert_waygates").addProperty("definition",1);
   var parsed=RealisticGenerator.CODEC.codec().parse(ops,input).getOrThrow();WellInteraction.require(parsed.discoveryDefinition()==(treasure?1:0)&&parsed.shipwreckDefinition()==(ship?1:0)&&parsed.waygateDefinition()==(gate?1:0),"Exact saved feature defaults schema"+schema);
   WellInteraction.require(RealisticGenerator.CODEC.codec().encodeStart(ops,parsed).getOrThrow().equals(input),"Flat lossless metadata schema"+schema);
   var state=parsed.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),random,random.seed());var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
   WellInteraction.require(state.getPlacementsForStructure(registry.getOrThrow(BuiltinStructures.SHIPWRECK_BEACHED)).isEmpty()!=ship,"Exact shipwreck placement schema"+schema);
   WellInteraction.require(state.getPlacementsForStructure(registry.getOrThrow(BuiltinStructures.BURIED_TREASURE)).isEmpty()!=treasure,"Exact treasure placement schema"+schema);
   WellInteraction.require(state.getPlacementsForStructure(registry.getOrThrow(BuiltinStructures.RUINED_PORTAL_DESERT)).isEmpty()!=gate,"Exact waygate placement schema"+schema);
  }
  for(int schema:new int[]{88,89,91,92}){var invalid=config.deepCopy();invalid.addProperty("shape_version",schema);WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,invalid).error().isPresent(),"Older schema rejects shipwreck combination "+schema);}
  var malformed=config.deepCopy();malformed.remove("shipwrecks");WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,malformed).error().isPresent(),"Shipwreck schema requires wreck definition");
  StructureSchemaQualification.main(new String[0]);
  long rebuildBefore=System.nanoTime();var rebuilt=ShipwreckDiscovery.build(random.seed(),g.treasures(random));long rebuildNanos=System.nanoTime()-rebuildBefore;
  var planner=g.shipwrecks(random);long worst=0,total=0;int groups=0;for(var group:planner.orderedGroups(BlockPos.ZERO,6000000)){long before=System.nanoTime();group.plan();long elapsed=System.nanoTime()-before;worst=Math.max(worst,elapsed);total+=elapsed;groups++;}
  var plans=planner.plans();Files.writeString(Path.of("plans.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(plans));WellInteraction.require(!plans.isEmpty(),"Natural beached wreck plans");
  var p=plans.stream().filter(q->Math.abs(q.x())<1700000&&Math.abs(q.z())<1700000).findFirst().orElse(plans.getFirst());var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.SHIPWRECK_BEACHED);
  int hx=p.rotation()%2==0?4:15,hz=p.rotation()%2==0?15:4;
  for(int z=(p.z()+hz)>>4;z>=(p.z()-hz)>>4;z--)for(int x=(p.x()+hx)>>4;x>=(p.x()-hx)>>4;x--)level.getChunk(x,z);
  var origin=level.getChunk(p.x()>>4,p.z()>>4);var start=level.structureManager().getStartForStructure(holder.value(),origin);WellInteraction.require(start!=null&&start.isValid(),"Native shipwreck start");
  var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
  var pieceTag=start.getPieces().getFirst().createTag(context);var nativeTag=pieceTag.toString();WellInteraction.require(nativeTag.contains("blockbranch:"+planner.templatePath(p.variant()))&&pieceTag.getBooleanOr("height_adjusted",false),"Native fixed-height template piece save");
  ShipwreckBoundaryQualification.check(level,g,pieceTag);
  var templateManager=context.structureTemplateManager();
  for(int variant=0;variant<2;variant++){
   var other=pieceTag.copy();other.putString("Template","blockbranch:"+planner.templatePath(variant));ShipwreckBoundaryQualification.check(level,g,other);
   for(var turn:net.minecraft.world.level.block.Rotation.values()){
    var piece=new net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces.ShipwreckPiece(templateManager,blockbranch.Blockbranch.id(planner.templatePath(variant)),p.origin(),turn,true);piece.adjustPositionHeight(p.y());var bounds=piece.getBoundingBox();int r=turn.ordinal(),rx=r%2==0?4:15,rz=r%2==0?15:4;
    WellInteraction.require(bounds.minX()==p.x()-rx&&bounds.maxX()==p.x()+rx&&bounds.minZ()==p.z()-rz&&bounds.maxZ()==p.z()+rz,"Manual fit footprint matches native rotation bounds");
   }
  }
  if(Boolean.getBoolean("blockbranch.shipwreckSupport"))ShipwreckSupportQualification.check(level,g,plans);
  var legacyPiece=new net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces.ShipwreckPiece(templateManager,net.minecraft.resources.Identifier.withDefaultNamespace("shipwreck/with_mast"),BlockPos.ZERO,net.minecraft.world.level.block.Rotation.NONE,true);WellInteraction.require(!legacyPiece.createTag(context).getBooleanOr("height_adjusted",false),"Ordinary vanilla piece remains unadjusted");
  var chestResults=new ArrayList<Object>();boolean reload=Files.exists(Path.of("saved-edit.txt"));int index=0;
  for(int[] local:new int[][]{{3,3,6},{3,7,25},{5,3,25}}){
   var pos=new BlockPos(p.x()+ShipwreckDiscovery.rotateX(local[0]-4,local[2]-15,p.rotation()),p.y()+local[1],p.z()+ShipwreckDiscovery.rotateZ(local[0]-4,local[2]-15,p.rotation()));
   WellInteraction.require(level.getBlockState(pos).is(Blocks.CHEST)&&level.getBlockEntity(pos) instanceof ChestBlockEntity,"Native compartment chest "+index+" at "+pos);var chest=(ChestBlockEntity)level.getBlockEntity(pos);
   if(!reload)WellInteraction.require(chest.saveWithoutMetadata(level.registryAccess()).toString().contains("minecraft:chests/shipwreck_"+new String[]{"supply","map","treasure"}[index]),"Original vanilla loot compartment");
   chest.unpackLootTable(null);if(index==1)WellInteraction.require(java.util.stream.IntStream.range(0,chest.getContainerSize()).anyMatch(slot->chest.getItem(slot).get(net.minecraft.core.component.DataComponents.MAP_ID)!=null),"Native shipwreck map loot creates usable treasure map");
   if(index==0){if(reload)WellInteraction.require(chest.getItem(0).isEmpty(),"Saved player loot removal survives reopening");else{chest.setItem(0,ItemStack.EMPTY);chest.setChanged();}}
   chestResults.add(Map.of("position",pos.toShortString(),"items",java.util.stream.IntStream.range(0,chest.getContainerSize()).mapToObj(slot->chest.getItem(slot).toString()).toList()));index++;
  }
  var edit=new BlockPos(p.x()+ShipwreckDiscovery.rotateX(0,-6,p.rotation()),p.y()+3,p.z()+ShipwreckDiscovery.rotateZ(0,-6,p.rotation()));
  if(reload)WellInteraction.require(level.getBlockState(edit).is(Blocks.OAK_PLANKS),"Saved repair inside hull survives reopening");else level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);
  WellInteraction.require(level.findNearestMapStructure(HolderSet.direct(holder),p.center(),1,false).equals(p.center()),"Native wreck locate resolves actual start");
  var treasureHolder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.BURIED_TREASURE);WellInteraction.require(level.findNearestMapStructure(HolderSet.direct(holder,treasureHolder),p.center(),1,false).equals(p.center()),"Mixed native targets choose the nearest actual structure");
  if(g.waygateDefinition()>0&&start.getReferences()==0){
   var gateHolder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.RUINED_PORTAL_DESERT);int before=start.getReferences();
   var parent=g.findNearestMapStructure(level,HolderSet.direct(treasureHolder),p.center(),1,false);net.minecraft.world.level.levelgen.structure.StructureStart parentStart=parent==null?null:level.structureManager().getStartForStructure(treasureHolder.value(),level.getChunk(parent.getFirst().getX()>>4,parent.getFirst().getZ()>>4));int parentReferences=parentStart==null?0:parentStart.getReferences();
   WellInteraction.require(g.findNearestMapStructure(level,HolderSet.direct(holder,treasureHolder,gateHolder),p.center(),1,true).getFirst().equals(p.center()),"Mixed skip-known selects native wreck winner");
   WellInteraction.require(start.getReferences()==before+1&&(parentStart==null||parentStart.getReferences()==parentReferences),"Only winning native start gains reference");
  }
  var codec=ShipwreckDiscovery.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,planner).getOrThrow();var decoded=ShipwreckDiscovery.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,codec).getOrThrow();decoded.bind(g.drainage(random),new ClimateField(random.seed()),g.treasures(random));decoded.orderedGroups(BlockPos.ZERO,6000000).parallelStream().forEach(group->group.plan());WellInteraction.require(decoded.valid(random.seed(),g.treasures(random))&&decoded.plans().equals(plans),"Wreck cache concurrency/order and codec preserve plans");
  var broken=codec.deepCopy().getAsJsonObject();broken.getAsJsonObject("anchor_cache").addProperty("checksum","bad");WellInteraction.require(!ShipwreckDiscovery.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,broken).getOrThrow().valid(random.seed(),g.treasures(random)),"Corrupt derived cache invalidates");
  var badCache=codec.deepCopy().getAsJsonObject();badCache.addProperty("anchor_cache","invalid derived payload");WellInteraction.require(!ShipwreckDiscovery.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,badCache).getOrThrow().valid(random.seed(),g.treasures(random)),"Malformed optional wreck cache keeps definition but rebuilds");
  var nativeSet=level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(BuiltinStructures.SHIPWRECK_BEACHED))).findFirst().orElseThrow();Files.writeString(Path.of("native-placement.json"),net.minecraft.world.level.levelgen.structure.StructureSet.DIRECT_CODEC.encodeStart(ops,nativeSet.value()).getOrThrow().toString());
  Files.writeString(Path.of("view.txt"),p.x()+","+p.y()+","+p.z()+","+p.rotation()+","+p.variant());Files.writeString(Path.of("saved-edit.txt"),edit.toShortString());
  var report=new LinkedHashMap<String,Object>();report.put("plans",plans.size());report.put("groups",groups);report.put("anchors",planner.anchorCount());report.put("retainedIntBytes",planner.retainedDataBytes());report.put("initNanos",g.shipwreckInitializationNanos());report.put("rebuildAnchorNanos",rebuildNanos);report.put("cacheRestored",g.shipwreckCacheRestored());report.put("worstColdPlanNanos",worst);report.put("totalPlanNanos",total);report.put("reload",reload);report.put("witness",p);report.put("nativePiece",nativeTag);report.put("chests",chestResults);
  if(Boolean.getBoolean("blockbranch.shipwreckRegions"))ShipwreckRegionalQualification.run();
  server.saveEverything(false,true,true);Files.writeString(Path.of("shipwreck.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(Path.of("result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("SHIPWRECK_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
