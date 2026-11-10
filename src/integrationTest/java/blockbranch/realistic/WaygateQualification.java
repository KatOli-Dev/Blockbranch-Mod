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
public final class WaygateQualification {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var gates=g.waygates(random);var plans=gates.plans();WellInteraction.require(!plans.isEmpty(),"Natural desert waygates");
  var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());var config=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();var old=config.deepCopy();old.remove("desert_waygates");old.remove("small_discovery_definition");old.remove("treasure_cache");old.addProperty("shape_version",88);WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow().waygateDefinition()==0,"Saved main schema 88 does not gain waygates");
  WaygateMetadataAudit.run(ops,config);
  var bad=old.deepCopy();bad.addProperty("shape_version",91);WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"New schema requires definition");
  var p=plans.stream().filter(q->((q.x()-6)>>4)!=((q.x()+6)>>4)||((q.z()-6)>>4)!=((q.z()+6)>>4)).findFirst().orElse(plans.getFirst());
  for(int z=(p.z()+6)>>4;z>=(p.z()-6)>>4;z--)for(int x=(p.x()+6)>>4;x>=(p.x()-6)>>4;x--)level.getChunk(x,z);
  var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.RUINED_PORTAL_DESERT);var chunk=level.getChunk(p.x()>>4,p.z()>>4);var start=level.structureManager().getStartForStructure(holder.value(),chunk);WellInteraction.require(start!=null&&start.isValid(),"Registered native ruined portal start");
  var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);var tag=start.getPieces().getFirst().createTag(context);WellInteraction.require(tag.toString().contains("blockbranch:desert_waygate_v1"),"Registered saved piece ID");
  for(int variant=0;variant<2;variant++)for(int r=0;r<4;r++){
   var test=new DesertWaygates.Plan(p.x(),p.y(),p.z(),r,variant,p.owner());var piece=new DesertWaygatePiece(context.structureTemplateManager(),test);var bounds=piece.getBoundingBox();WellInteraction.require(bounds.minX()==p.x()-6&&bounds.maxX()==p.x()+6&&bounds.minZ()==p.z()-6&&bounds.maxZ()==p.z()+6,"Native rotated bounding box equals fitted footprint");WaygateBoundaryQualification.check(level,g,piece.createTag(context));
  }
  var chestPos=p.local(3,3,4);WellInteraction.require(level.getBlockEntity(chestPos) instanceof ChestBlockEntity,"Native chest block entity");var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);boolean reopen=Files.exists(Path.of("edit.txt"));var edit=p.local(9,3,3);
  if(!reopen){WellInteraction.require(chest.saveWithoutMetadata(level.registryAccess()).toString().contains("minecraft:chests/ruined_portal"),"Unchanged native loot table");chest.unpackLootTable(null);chest.setItem(0,ItemStack.EMPTY);chest.setChanged();level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);}
  else{WellInteraction.require(chest.getItem(0).isEmpty(),"Player loot removal persisted");WellInteraction.require(level.getBlockState(edit).is(Blocks.OAK_PLANKS),"Player repair persisted");}
  WellInteraction.require(java.util.stream.IntStream.range(0,27).noneMatch(i->chest.getItem(i).get(net.minecraft.core.component.DataComponents.MAP_ID)!=null),"Native ruined portal loot contains no maps");
  WellInteraction.require(level.findNearestMapStructure(HolderSet.direct(holder),p.center(),1,false).equals(p.center()),"Native locate resolves fitted start");
  var axis=p.rotation()%2==0?Direction.Axis.X:Direction.Axis.Z;var inside=p.local(6,4,7);
  if(!reopen){
   WellInteraction.require(net.minecraft.world.level.portal.PortalShape.findEmptyPortalShape(level,inside,axis).isEmpty(),"Damaged frame cannot activate without repairs");
   for(int x=5;x<=8;x++)for(int y=3;y<=7;y++)if(x==5||x==8||y==3||y==7)level.setBlock(p.local(x,y,7),Blocks.OBSIDIAN.defaultBlockState(),3);
   var player=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"WaygateIgnition"),net.minecraft.server.level.ClientInformation.createDefault());player.setPos(inside.getX()+2,inside.getY()+1,inside.getZ()+2);var flint=new ItemStack(Items.FLINT_AND_STEEL);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,flint);
   var bottom=p.local(6,3,7);player.gameMode.useItemOn(player,level,flint,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(bottom),Direction.UP,bottom,false));
   WellInteraction.require(level.getBlockState(inside).is(Blocks.NETHER_PORTAL),"Native flint and steel activates repaired portal");WellInteraction.require(flint.getDamageValue()==1,"Native ignition durability");
  }else WellInteraction.require(level.getBlockState(inside).is(Blocks.NETHER_PORTAL),"Repaired active portal survives reopen");
  var codec=DesertWaygates.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,gates).getOrThrow();var restored=DesertWaygates.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,codec).getOrThrow();WellInteraction.require(restored.valid(random.seed(),true)&&restored.plans().equals(plans),"Cache codec preserves placements");
  WellInteraction.require(!restored.valid(random.seed()+1,true)&&!restored.valid(random.seed(),false),"Seed and terrain-option mismatch invalidate only derived cache");
  var malformedCache=codec.deepCopy().getAsJsonObject();malformedCache.addProperty("plan_cache","invalid optional cache");WellInteraction.require(!DesertWaygates.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,malformedCache).getOrThrow().valid(random.seed(),true),"Malformed optional waygate cache preserves definition for rebuild");
  var corrupt=codec.deepCopy().getAsJsonObject();corrupt.getAsJsonObject("plan_cache").addProperty("checksum","bad");WellInteraction.require(!DesertWaygates.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,corrupt).getOrThrow().valid(random.seed(),true),"Corrupt disposable cache invalidates");
  plans.parallelStream().forEach(q->WellInteraction.require(gates.forStart(q.x()>>4,q.z()>>4).orElseThrow().equals(q),"Concurrent start routes"));
  Files.writeString(Path.of("view.json"),new com.google.gson.Gson().toJson(plans.stream().filter(q->!q.equals(p)).findFirst().orElseThrow()));
  for(int v=0;v<2;v++){int variant=v;var view=plans.stream().filter(q->q.variant()==variant&&!q.equals(p)&&q.y()<220).findFirst().orElseThrow();Files.writeString(Path.of("view-"+v+".json"),new com.google.gson.Gson().toJson(view));}Files.writeString(Path.of("edit.txt"),edit.toShortString());
  if(Boolean.getBoolean("blockbranch.waygateRegions"))WaygateRegions.run(g,gates,random,level);
  var report=Map.of("plans",plans,"witness",p,"initNanos",g.waygateInitializationNanos(),"cacheRestored",g.waygateCacheRestored(),"retainedIntBytes",gates.retainedIntBytes(),"reload",reopen,"piece",tag.toString(),"nativeIgnition",true);
  Files.writeString(Path.of("native.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("WAYGATE_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
