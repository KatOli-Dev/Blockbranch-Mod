package blockbranch.realistic;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
/** Focused actual prior binary and immutable-index recovery checks. */
final class SnowShelterCompatibility {
 static void check(net.minecraft.server.level.ServerLevel level,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random,JsonObject metadata,JsonObject old)throws Exception{
  var ops=net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
  try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(System.getProperty("blockbranch.snowPriorJar")).toUri().toURL()},SnowShelterCompatibility.class.getClassLoader()){
   @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{if(!name.startsWith("blockbranch.realistic.RealisticGenerator"))return super.loadClass(name,resolve);synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}}
  }){var type=reader.loadClass("blockbranch.realistic.RealisticGenerator");var codec=((com.mojang.serialization.MapCodec<?>)type.getField("CODEC").get(null)).codec();require(codec.parse(ops,old).result().isPresent(),"Actual adopted94 reader accepts old definition");require(codec.parse(ops,metadata).error().isPresent(),"Actual adopted94 reader rejects95 before chunks");}
  var encoded=SnowShelters.CODEC.encodeStart(JsonOps.INSTANCE,g.snowShelters(random)).getOrThrow().getAsJsonObject();
  for(int mode=0;mode<3;mode++){var bad=encoded.deepCopy();var index=bad.getAsJsonObject("region_cache");if(mode==0)index.addProperty("checksum","bad");if(mode==1)index.addProperty("seed",level.getSeed()+1);if(mode==2)index.getAsJsonArray("cells").remove(0);var decoded=SnowShelters.CODEC.parse(JsonOps.INSTANCE,bad).getOrThrow();require(!decoded.valid(level.getSeed()),"Corrupt checksum, seed or incomplete row rejected");}
  var bad=encoded.deepCopy();bad.getAsJsonObject("region_cache").addProperty("checksum","bad");var decoded=SnowShelters.CODEC.parse(JsonOps.INSTANCE,bad).getOrThrow();Thread.currentThread().interrupt();try{decoded.bindSaved(level.getSeed(),g,random);throw new AssertionError("Cancelled bind published");}catch(java.util.concurrent.CancellationException expected){require(Thread.currentThread().isInterrupted(),"Cancellation preserves interruption");}finally{Thread.interrupted();}
  require(SnowShelters.CODEC.encodeStart(JsonOps.INSTANCE,decoded).getOrThrow().equals(bad),"Failed binding leaves original index untouched");var recovered=decoded.bindSaved(level.getSeed(),g,random);require(SnowShelters.CODEC.encodeStart(JsonOps.INSTANCE,recovered).getOrThrow().equals(encoded),"Invalid index recovers exact complete original ownership");
 }
 static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
