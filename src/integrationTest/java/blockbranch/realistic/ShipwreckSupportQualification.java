package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces.ShipwreckPiece;

/** Support regression through native loading, markers, rotation and clipped replay. */
final class ShipwreckSupportQualification {
 static void check(net.minecraft.server.level.ServerLevel level,RealisticGenerator g,List<ShipwreckDiscovery.Plan> plans)throws Exception{
  var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
  var manager=context.structureTemplateManager();var report=new LinkedHashMap<String,Object>();
  var ops=com.mojang.serialization.JsonOps.INSTANCE;
  // The actual adopted runtime artifact supplies the historical reader, isolated only
  // for the discovery class family. Minecraft/Codec classes stay owned by Fabric.
  var oldJar=Path.of(System.getProperty("blockbranch.shipwreckOldJar"));
  try(var reader=new java.net.URLClassLoader(new java.net.URL[]{oldJar.toUri().toURL()},ShipwreckSupportQualification.class.getClassLoader()){
   @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
    if(!name.startsWith("blockbranch.realistic.ShipwreckDiscovery"))return super.loadClass(name,resolve);
    synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}
   }
  }){
   var type=reader.loadClass("blockbranch.realistic.ShipwreckDiscovery");
   var historical=(com.mojang.serialization.Codec<?>)type.getField("CODEC").get(null);
   var input=new com.google.gson.JsonObject();input.addProperty("definition",1);
   WellInteraction.require(historical.parse(ops,input).result().isPresent(),"Actual adopted binary reads definition1");
   input.addProperty("definition",2);WellInteraction.require(historical.parse(ops,input).error().isPresent(),"Actual adopted binary rejects definition2 before dropping v2 starts");
  }

  report.put("actualAdoptedBinaryRejectsDefinition2",true);
  for(int revision:new int[]{1,2}){
   var input=new com.google.gson.JsonObject();input.addProperty("definition",revision);
   var decoded=ShipwreckDiscovery.CODEC.parse(ops,input).getOrThrow();
   WellInteraction.require(ShipwreckDiscovery.CODEC.encodeStart(ops,decoded).getOrThrow().equals(input),"Template definition survives empty-cache round trip");
   var rebuilt=ShipwreckDiscovery.build(level.getSeed(),g.treasures(level.getChunkSource().randomState()),revision);
   WellInteraction.require(rebuilt.templateRevision()==revision,"Cache rebuild preserves template revision");
   rebuilt.bind(g.drainage(level.getChunkSource().randomState()),new ClimateField(level.getSeed()),g.treasures(level.getChunkSource().randomState()));
   WellInteraction.require(rebuilt.plans().equals(plans),"Both template revisions preserve every placement");
  }
  for(int variant=0;variant<2;variant++){
   var old=manager.getOrCreate(blockbranch.Blockbranch.id("shipwreck/coastal_sloop_v1_"+variant)).save(new CompoundTag());
   var fixed=manager.getOrCreate(blockbranch.Blockbranch.id("shipwreck/coastal_sloop_v2_"+variant)).save(new CompoundTag());
   var a=blocks(old);var b=blocks(fixed);var changed=new HashSet<BlockPos>();
   var all=new HashSet<>(a.keySet());all.addAll(b.keySet());for(var p:all)if(!Objects.equals(a.get(p),b.get(p)))changed.add(p);
   var allowed=new HashSet<BlockPos>();for(int x=1;x<=7;x++)allowed.add(new BlockPos(x,6,30));for(int step=1;step<4;step++)for(int y=3;y<3+step;y++)allowed.add(new BlockPos(4,y,17+step));
   WellInteraction.require(changed.equals(allowed),"Exactly rear-deck row and solid stair stringer differ: "+changed);
   WellInteraction.require(old.getListOrEmpty("entities").equals(fixed.getListOrEmpty("entities"))&&old.getListOrEmpty("size").equals(fixed.getListOrEmpty("size")),"Entities and dimensions unchanged");
   var p=plans.getFirst();
   for(var turn:Rotation.values()){
    var piece=new ShipwreckPiece(manager,blockbranch.Blockbranch.id("shipwreck/coastal_sloop_v2_"+variant),p.origin(),turn,true);piece.adjustPositionHeight(p.y());
    ShipwreckBoundaryQualification.check(level,g,piece.createTag(context));
   }
  }
  var terrain=g.drainage(level.getChunkSource().randomState());var fits=new ArrayList<Object>();
  for(var p:plans){int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE,groundedKeel=0;
   for(int z=-15;z<=15;z++)for(int x=-4;x<=4;x++){
    int height=terrain.sample(p.x()+ShipwreckDiscovery.rotateX(x,z,p.rotation()),p.z()+ShipwreckDiscovery.rotateZ(x,z,p.rotation())).height();min=Math.min(min,height);max=Math.max(max,height);if(x==0&&height>=p.y()-1)groundedKeel++;
   }
   WellInteraction.require(groundedKeel>0&&max-min<=3,"Natural hull grounded within existing fit tolerance");
   fits.add(Map.of("plan",p,"minimumTerrain",min,"maximumTerrain",max,"groundedKeelCells",groundedKeel));
  }
  report.put("nativeTemplateVariants",2);report.put("rotationsEach",4);report.put("oppositeChunkReplay",true);report.put("allNativeSolidsConnected",true);report.put("unchangedLootMarkersAndBlockEntities",true);report.put("exactChangedPositionsPerVariant",13);report.put("naturalTerrainFits",fits);report.put("scope","No modifications to retained v1 templates or existing blocks; definition2 selects v2 only in new generator configurations");
  Files.writeString(Path.of("support.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
 }
 private static Map<BlockPos,CompoundTag> blocks(CompoundTag template){
  var palette=template.getListOrEmpty("palette").compoundStream().toList();var result=new HashMap<BlockPos,CompoundTag>();
  template.getListOrEmpty("blocks").compoundStream().forEach(b->{var xyz=b.getListOrEmpty("pos");var p=new BlockPos(xyz.getInt(0).orElseThrow(),xyz.getInt(1).orElseThrow(),xyz.getInt(2).orElseThrow());var value=b.copy();value.remove("state");value.put("block_state",palette.get(b.getIntOr("state",-1)));result.put(p,value);});return result;
 }
}
