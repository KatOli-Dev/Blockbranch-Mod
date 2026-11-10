package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
/** Read-only native jungle anatomy from an authentic disposable save. */
public final class JungleNativeExport {
 public static void write(ServerLevel level,ReimaginedLayoutPiece piece,StructureStart start,Path dir,boolean reopen)throws Exception {
  String prefix=reopen?"reopen-":"";var gson=new com.google.gson.Gson();var pretty=new com.google.gson.GsonBuilder().setPrettyPrinting().create();
  var features=new ArrayList<Object>();var palette=new TreeMap<String,Integer>();
  try(var out=Files.newBufferedWriter(dir.resolve(prefix+"native-core.voxels.jsonl"));var site=Files.newBufferedWriter(dir.resolve(prefix+"native-site.voxels.jsonl"))) {
   for(int y=-20;y<=40;y++)for(int z=-14;z<=29;z++)for(int x=-14;x<=28;x++) {
    var at=piece.local(x,y,z);var state=level.getBlockState(at);boolean core=x>=0&&x<=11&&z>=0&&z<=14&&y>=-5&&y<=10;
    var row=Map.of("x",x,"y",y,"z",z,"state",state.toString(),"collision",state.getCollisionShape(level,at).toAabbs().toString(),"fluid",net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(state.getFluidState().getType()).toString()+"/"+state.getFluidState().getAmount(),"blockLight",level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,at));
    site.write(gson.toJson(row)+"\n");if(core){out.write(gson.toJson(row)+"\n");palette.merge(state.toString(),1,Integer::sum);}
    if(core&&(level.getBlockEntity(at)!=null||state.getLightEmission()>0||state.toString().matches(".*(piston|lever|tripwire|redstone).*")))features.add(Map.of("local",List.of(x,y,z),"state",state.toString(),"blockEntity",level.getBlockEntity(at)==null?"":level.getBlockEntity(at).getClass().getSimpleName(),"lootTable",level.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c&&c.getLootTable()!=null?c.getLootTable().identifier().toString():""));
   }
  }
  var ctx=StructurePieceSerializationContext.fromLevel(level);
  Files.writeString(dir.resolve(prefix+"native-anatomy.json"),pretty.toJson(Map.of("source","actual current native save; read-only export before player edits","coreBounds",List.of(0,-5,0,11,10,14),"siteBounds",List.of(-14,-20,-14,28,40,29),"palette",palette,"features",features,"pieces",start.getPieces().stream().map(p->p.createTag(ctx).toString()).toList())));
 }
}
