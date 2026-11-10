package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Freeze reviewed blueprint intent before native execution; never samples candidate world states. */
public final class MansionBlueprintFreeze {
 public static void main(String[]args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();Path ref=Path.of(args[0]),out=Path.of(args[1]);String source=System.getProperty("blockbranch.mansionBlueprintSource","");if(source.isBlank())throw new AssertionError("Explicit reviewed source commit required when freezing blueprint");Files.createDirectories(out);
  var sources=new ArrayList<MansionRoofPlan.Source>();for(var v:JsonParser.parseString(Files.readString(ref.resolve("native-piece-anatomy.json"))).getAsJsonArray()){
   var row=v.getAsJsonObject();if(!row.get("class").getAsString().equals("WoodlandMansionPiece"))continue;var tag=TagParser.parseCompoundFully(row.get("fullNBT").getAsString());String name=tag.getStringOr("Template","");CompoundTag asset;try(var in=MansionBlueprintFreeze.class.getResourceAsStream("/data/minecraft/structure/woodland_mansion/"+name+".nbt")){if(in==null)throw new AssertionError("Native asset "+name);asset=NbtIo.readCompressed(in,NbtAccounter.unlimitedHeap());}
   sources.add(new MansionRoofPlan.Source(name,new BlockPos(tag.getIntOr("TPX",0),tag.getIntOr("TPY",0),tag.getIntOr("TPZ",0)),Mirror.valueOf(tag.getStringOr("Mi","NONE")),Rotation.valueOf(tag.getStringOr("Rot","NONE")),BlockPos.ZERO,asset));
  }
  var anatomy=JsonParser.parseString(Files.readString(ref.resolve("native-anatomy.json"))).getAsJsonObject();var a=anatomy.getAsJsonArray("markerBounds");var c=anatomy.getAsJsonArray("nativeCoreBounds");var bounds=new BoundingBox(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt(),a.get(3).getAsInt(),c.get(4).getAsInt()+6,a.get(5).getAsInt());var plan=MansionRoofPlan.captureSources(sources,bounds);var commands=new TreeMap<BlockPos,String>(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ));plan.place(bounds,(p,s)->commands.put(p,s.toString()));
  var gson=new Gson();Path file=out.resolve("reviewed-roof-commands.jsonl");if(Files.exists(file))throw new AssertionError("Preserve prior frozen blueprint");try(var writer=Files.newBufferedWriter(file)){for(var e:commands.entrySet()){var p=e.getKey();writer.write(gson.toJson(Map.of("x",p.getX(),"y",p.getY(),"z",p.getZ(),"state",e.getValue()))+"\n");}}
  Files.writeString(out.resolve("reviewed-roof-plan.snbt"),plan.save().toString());String sha=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));Files.writeString(Path.of(file+".sha256"),sha);
  Files.writeString(out.resolve("provenance.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("reviewedSourceCommit",source,"nativeSources",sources.size(),"commands",commands.size(),"raisedColumns",plan.raisedColumns(),"raisedComponents",plan.raisedComponents(),"commandsSHA256",sha,"roofClassSHA256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(MansionRoofPlan.class.getResourceAsStream("MansionRoofPlan.class").readAllBytes())))));
  try(var route=MansionBlueprintFreeze.class.getResourceAsStream("/blockbranch/realistic/mansion-replacement-front-walk.json")){Files.copy(route,out.resolve("native-front-walk.json"));}
  System.out.println("PASS frozen roof commands="+commands.size()+" SHA="+sha);
 }
}
