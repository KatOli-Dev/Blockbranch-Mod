package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Native, read-only block anatomy exported from a disposable current-schema save. */
public final class SanctuaryCoreExport {
 public static void write(ServerLevel level,ReimaginedSanctuaryPiece piece,StructureStart start,Path dir)throws Exception {
  var gson=new com.google.gson.GsonBuilder().setPrettyPrinting().create();var compact=new com.google.gson.Gson();
  String prefix=Files.exists(dir.resolve("native-anatomy.json"))?"reopen-":"";
  if(Files.exists(dir.resolve(prefix+"native-anatomy.json")))throw new AssertionError("Preserve prior native anatomy evidence");
  var basis=piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getIntArray("Basis").orElseThrow();
  var core=start.getPieces().getFirst();var bounds=core.getBoundingBox();
  var protectedBlocks=new ArrayList<Object>();var palette=new TreeMap<String,Integer>();
  try(var blocks=Files.newBufferedWriter(dir.resolve(prefix+"native-core.voxels.jsonl"));var foundation=Files.newBufferedWriter(dir.resolve(prefix+"native-foundation.voxels.jsonl"))) {
   for(int y=-34;y<=22;y++)for(int z=0;z<=57;z++)for(int x=0;x<=57;x++) {
    var at=piece.local(x,y,z);var state=level.getBlockState(at);var value=state.toString();palette.merge(value,1,Integer::sum);
    var row=Map.of("x",x,"y",y,"z",z,"state",value);(y<0?foundation:blocks).write(compact.toJson(row)+"\n");
    if(state.is(Blocks.GOLD_BLOCK)||state.is(Blocks.WET_SPONGE)||state.getLightEmission()>0)protectedBlocks.add(Map.of("local",List.of(x,y,z),"world",List.of(at.getX(),at.getY(),at.getZ()),"state",value,"emission",state.getLightEmission()));
   }
  }
  var children=((blockbranch.mixin.cubic.ReimaginedMonumentChildren)core).blockbranch$children();
  var rooms=new ArrayList<Object>();
  for(var child:children){var box=child.getBoundingBox();var a=local(basis,new BlockPos(box.minX(),box.minY(),box.minZ()));var b=local(basis,new BlockPos(box.maxX(),box.maxY(),box.maxZ()));rooms.add(Map.of("class",child.getClass().getSimpleName(),"localBounds",List.of(Math.min(a.get(0),b.get(0)),a.get(1),Math.min(a.get(2),b.get(2)),Math.max(a.get(0),b.get(0)),b.get(1),Math.max(a.get(2),b.get(2))),"worldBounds",List.of(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())));}
  var elders=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX()+1,bounds.maxY()+1,bounds.maxZ()+1).inflate(8)).stream().filter(m->m.getType()==net.minecraft.world.entity.EntityTypes.ELDER_GUARDIAN).map(m->Map.of("uuid",m.getUUID().toString(),"local",local(basis,m.blockPosition()))).toList();
  Files.writeString(dir.resolve(prefix+"native-child-rooms.json"),gson.toJson(rooms));
  Files.writeString(dir.resolve(prefix+"native-protected-features.json"),gson.toJson(Map.of("blocks",protectedBlocks,"elders",elders)));
  Files.writeString(dir.resolve(prefix+"native-anatomy.json"),gson.toJson(Map.of("source","actual saved native core; no placement replay or production mutation","basis",basis,"localCoreBounds",List.of(0,0,0,57,22,57),"localFoundationBounds",List.of(0,-34,0,57,-1,57),"palette",palette,"childRooms",rooms.size(),"coreBlocks",58*23*58,"foundationBlocks",58*34*58)));
 }
 private static List<Integer> local(int[] a,BlockPos p){int dx=p.getX()-a[0],dz=p.getZ()-a[2];return List.of(dx*a[3]+dz*a[4],p.getY()-a[1],dx*a[5]+dz*a[6]);}
}
