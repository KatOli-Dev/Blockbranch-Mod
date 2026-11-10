package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
/** Read-only current pyramid anatomy, including all archaeology outside the traditional core. */
public final class PyramidNativeExport {
 public static void write(ServerLevel level,ReimaginedLayoutPiece piece,StructureStart start,Path dir,boolean reopen)throws Exception {
  String prefix=reopen?"reopen-":"";var gson=new com.google.gson.Gson();var pretty=new com.google.gson.GsonBuilder().setPrettyPrinting().create();var features=new ArrayList<Object>();var palette=new TreeMap<String,Integer>();
  try(var out=Files.newBufferedWriter(dir.resolve(prefix+"native-core.voxels.jsonl"));var site=Files.newBufferedWriter(dir.resolve(prefix+"native-site.voxels.jsonl"))) {
   for(int y=-20;y<=40;y++)for(int z=-26;z<=35;z++)for(int x=-26;x<=35;x++) {
    var at=piece.local(x,y,z);var state=level.getBlockState(at);boolean core=x>=0&&x<=20&&z>=0&&z<=20&&y>=-14&&y<=16;
    var row=Map.of("x",x,"y",y,"z",z,"state",state.toString(),"collision",state.getCollisionShape(level,at).toAabbs().toString(),"fluid",net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(state.getFluidState().getType()).toString()+"/"+state.getFluidState().getAmount(),"blockLight",level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,at),"skyLight",level.getBrightness(net.minecraft.world.level.LightLayer.SKY,at));site.write(gson.toJson(row)+"\n");if(core){out.write(gson.toJson(row)+"\n");palette.merge(state.toString(),1,Integer::sum);}
    var be=level.getBlockEntity(at);if(be!=null||state.is(Blocks.TNT)||state.is(Blocks.STONE_PRESSURE_PLATE)||state.is(Blocks.SUSPICIOUS_SAND)){
     var feature=new LinkedHashMap<String,Object>();feature.put("local",List.of(x,y,z));feature.put("world",List.of(at.getX(),at.getY(),at.getZ()));feature.put("insideNativeCore",core);feature.put("state",state.toString());feature.put("blockEntity",be==null?"":be.getClass().getSimpleName());feature.put("blockEntityNBT",be==null?"":be.saveWithFullMetadata(level.registryAccess()).toString());feature.put("below",level.getBlockState(at.below()).toString());feature.put("belowCollision",level.getBlockState(at.below()).getCollisionShape(level,at.below()).toAabbs().toString());features.add(feature);
    }
   }
  }
  var ctx=StructurePieceSerializationContext.fromLevel(level);Files.writeString(dir.resolve(prefix+"native-anatomy.json"),pretty.toJson(Map.of("source","actual current saved pyramid; read-only before native player interactions","coreBounds",List.of(0,-14,0,20,16,20),"siteBounds",List.of(-26,-20,-26,35,40,35),"palette",palette,"features",features,"pieces",start.getPieces().stream().map(p->p.createTag(ctx).toString()).toList())));
 }
}
