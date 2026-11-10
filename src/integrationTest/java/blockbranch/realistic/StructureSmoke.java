package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import blockbranch.cubic.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
public final class StructureSmoke {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();if(g.structureDefinition()==3)require(g.structureAtlasPrepared(),"Atlas prepared during native createState before chunk admission");
  var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  var config=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
  require(config.get("shape_version").getAsInt()==(g.waygateDefinition()>0?(g.discoveryDefinition()>0?92:91):g.discoveryDefinition()>0?89:g.structureDefinition()==3?88:g.structureDefinition()==2?87:86),"New world schema barrier");
  var cacheMalformed=config.deepCopy();cacheMalformed.addProperty("structure_cache","broken optional data");
  require(RealisticGenerator.CODEC.codec().parse(ops,cacheMalformed).getOrThrow().cachedStructureBytes()==0,"Malformed optional cache does not prevent opening world");
  var legacy=config.deepCopy();legacy.remove("small_discovery_definition");legacy.remove("treasure_cache");legacy.remove("structure_definition");legacy.addProperty("shape_version",85);
  require(RealisticGenerator.CODEC.codec().parse(ops,legacy).getOrThrow().structureDefinition()==0,"Existing generators stay opted out");
  var malformed=config.deepCopy();malformed.remove("structure_definition");require(RealisticGenerator.CODEC.codec().parse(ops,malformed).error().isPresent(),"Reject missing definition on new schema");
  malformed=config.deepCopy();malformed.addProperty("shape_version",85);require(RealisticGenerator.CODEC.codec().parse(ops,malformed).error().isPresent(),"Reject structure definition under old schema");
  for(int definition=1;definition<=3;definition++){
   var saved=config.deepCopy();saved.remove("small_discovery_definition");saved.remove("treasure_cache");saved.addProperty("shape_version",85+definition);saved.addProperty("structure_definition",definition);
   require(RealisticGenerator.CODEC.codec().parse(ops,saved).getOrThrow().structureDefinition()==definition,"Explicit saved definitions remain intact");
  }
  var directory=Path.of(System.getProperty("blockbranch.structureResult")).getParent();var marker=directory.resolve("well.txt");
  var cacheMarker=directory.resolve("cache-written.txt");if(g.structureDefinition()==3&&Files.exists(cacheMarker))require(g.wells(random).restoredFromSave(),"Actual native world generation settings restore optional placement cache");DesertWell.Plan plan=null;boolean reload=Files.exists(marker);
  // Synthetic topology checks exercise components and budgets independently of biome noise.
  var field=new BiomeRegionPlacement(7,(x,z)->Math.abs(x)<6000&&Math.abs(z)<6000);
  var a=field.region(new BiomeRegionPlacement.Cell(0,0));var b=field.region(new BiomeRegionPlacement.Cell(-1,-1));
  require(a.equals(b)&&a.cells()==36,"Coherent region identity across negative boundaries");
  require(new BiomeRegionPlacement(7,(x,z)->true).region(new BiomeRegionPlacement.Cell(0,0)).overflow(),"Bounded oversized region rejection");
  if(reload){var q=Files.readString(marker).trim().split(",");plan=new DesertWell.Plan(Integer.parseInt(q[0]),Integer.parseInt(q[1]),Integer.parseInt(q[2]),Long.parseLong(q[3]));}
  else outer:for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
   if(!SandyDesert.contains(random.seed(),x,z,g.drainage(random),new ClimateField(random.seed())))continue;
   var cell=new BiomeRegionPlacement.Cell(Math.floorDiv(x,2048),Math.floorDiv(z,2048));var region=g.wells(random).region(cell);
   if(region.owner()==null)continue;var candidate=g.wells(random).plan(region.owner());if(candidate.isPresent()){plan=candidate.get();break outer;}
  }
  require(plan!=null,"Natural well found");var witness=new BlockPos(plan.x()-3,plan.y()+2,plan.z()-3);
  if(reload)require(level.getBlockState(witness).isAir(),"Player edit survives complete reopen");
  boolean consumedFixture=Boolean.getBoolean("blockbranch.structureConsumedWell");int checked=0,archaeology=0,consumed=0;
  for(var block:DesertWell.blocks(plan)){
   if(block.pos().equals(witness)&&reload)continue;
   if(consumedFixture&&reload&&block.pos().equals(new BlockPos(plan.x()-1,plan.y()-1,plan.z()))){require(block.state().is(Blocks.SUSPICIOUS_SAND)&&level.getBlockState(block.pos()).is(Blocks.SAND)&&level.getBlockEntity(block.pos())==null,"Exact historically consumed deposit remains sand without recreated loot");consumed++;checked++;continue;}
   require(level.getBlockState(block.pos())==block.state(),"Native placement "+block.pos()+" expected "+block.state()+" got "+level.getBlockState(block.pos()));
   if(block.state().is(Blocks.SUSPICIOUS_SAND)){require(level.getBlockEntity(block.pos()) instanceof net.minecraft.world.level.block.entity.BrushableBlockEntity,"Archaeology entity");require(level.getBlockEntity(block.pos()).saveWithoutMetadata(level.registryAccess()).toString().contains("archaeology/desert_well"),"Vanilla archaeology loot table survives generation/reopen");archaeology++;}checked++;
  }
  require(archaeology+consumed==2&&consumed==(consumedFixture?1:0),"Exactly two original deposits, explicit consumed fixture preserved");
  int additional=0;var nativePlans=new LinkedHashSet<DesertWell.Plan>();
  if(g.structureDefinition()==3)for(int[] point:new int[][]{{222,-575},{207,-538},{187,-363},{208,-351},{201,-340}}){
   var owner=g.wells(random).region(new BiomeRegionPlacement.Cell(point[0],point[1])).owner();var extra=g.wells(random).plan(owner).orElseThrow();
   if(!nativePlans.add(extra))continue;
   for(var block:DesertWell.blocks(extra)){
    require(level.getBlockState(block.pos())==block.state(),"Native atlas terrain/seam "+block.pos());
    if(block.state().is(Blocks.SUSPICIOUS_SAND))require(level.getBlockEntity(block.pos())!=null&&level.getBlockEntity(block.pos()).saveWithoutMetadata(level.registryAccess()).toString().contains("archaeology/desert_well"),"High native archaeology");
    additional++;
   }
  }
  require(g.structureDefinition()!=3||nativePlans.size()==4,"Merged macro region has one native well budget");
  level.setBlock(witness,Blocks.AIR.defaultBlockState(),2);var terrain=((SparseColumn)level.getChunkAt(witness)).blockbranch$terrain();terrain.flush();terrain.evictExcept(Set.of());require(level.getBlockState(witness).isAir(),"Edit survives eviction");
  Files.writeString(marker,plan.x()+","+plan.y()+","+plan.z()+","+plan.seed());server.saveEverything(false,true,true);if(g.structureDefinition()==3)Files.writeString(cacheMarker,"saved definition-3 cache");
  Files.writeString(Path.of(System.getProperty("blockbranch.structureResult")),"PASS reload="+reload+" blocks="+checked+" additionalNativeBlocks="+additional+" archaeology="+archaeology+" consumed="+consumed+" regionCells="+g.wells(random).region(new BiomeRegionPlacement.Cell(Math.floorDiv(plan.x(),2048),Math.floorDiv(plan.z(),2048))).cells()+" plan="+plan+"\n");
 }catch(Throwable e){blockbranch.Blockbranch.LOGGER.error("STRUCTURE_FAIL",e);try{Files.writeString(Path.of(System.getProperty("blockbranch.structureResult")),"FAIL "+e);}catch(Exception ignored){}}finally{server.halt(false);}});}
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
}
