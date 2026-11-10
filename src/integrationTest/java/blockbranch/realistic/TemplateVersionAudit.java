package blockbranch.realistic;
import java.nio.file.*;import java.util.*;import java.lang.management.*;import javax.management.ObjectName;
import net.minecraft.server.MinecraftServer;import net.minecraft.resources.Identifier;import net.minecraft.world.level.levelgen.structure.templatesystem.*;import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplateSource;
public final class TemplateVersionAudit {
 static final class Source extends TemplateSource {
  Source(MinecraftServer s){super(s.getFixerUpper(),s.overworld().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK));}
  public Optional<StructureTemplate> load(Identifier id){return Optional.empty();}public java.util.stream.Stream<Identifier> list(){return java.util.stream.Stream.empty();}
  StructureTemplate file(Path p){return load(()->Files.newInputStream(p),false,t->{throw new RuntimeException(t);}).orElseThrow();}
 }
 static long heap(String label)throws Exception{
  String h=(String)ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"),"gcClassHistogram",new Object[]{new String[0]},new String[]{String[].class.getName()});Files.writeString(Path.of("live-"+label+".txt"),h);var last=h.lines().filter(s->s.startsWith("Total")).findFirst().orElseThrow().trim().split("\\s+");return Long.parseLong(last[2]);
 }
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  Path repo=Path.of(System.getProperty("blockbranch.templateRepo"));String mode=System.getProperty("blockbranch.templateMode");boolean fixed=mode.equals("fixed");var source=new Source(server);long before=heap("before"),start=System.nanoTime();var loaded=new LinkedHashMap<String,StructureTemplate>();
  for(String id:new String[]{"shipwreck/coastal_sloop_v1_0","shipwreck/coastal_sloop_v1_1","ruined_portal/desert_waygate_v1_0","ruined_portal/desert_waygate_v1_1"})loaded.put(id,source.file(repo.resolve(fixed?"src/main/resources/data/blockbranch/structure":"src/integrationTest/resources/blockbranch-fixtures/structure-2865").resolve(id+".nbt")));
  long elapsed=System.nanoTime()-start,after=heap("after");var report=new LinkedHashMap<String,Object>();report.put("mode",mode);report.put("nativeVersion",net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version());report.put("beforeLiveBytes",before);report.put("afterLiveBytes",after);report.put("liveDeltaBytes",after-before);report.put("fourTemplateLoadNanos",elapsed);Files.writeString(Path.of("load-cost.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
  // The measurement is complete before loading the other representation for equality.
  var equal=new LinkedHashMap<String,Boolean>();for(var e:loaded.entrySet()){
   var current=net.minecraft.nbt.NbtIo.readCompressed(repo.resolve("src/main/resources/data/blockbranch/structure").resolve(e.getKey()+".nbt"),net.minecraft.nbt.NbtAccounter.unlimitedHeap());WellInteraction.require(current.getIntOr("DataVersion",-1)==net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version(),"Builder authoring schema matches target game");
   var other=source.file(repo.resolve(fixed?"src/integrationTest/resources/blockbranch-fixtures/structure-2865":"src/main/resources/data/blockbranch/structure").resolve(e.getKey()+".nbt"));var a=e.getValue().save(new net.minecraft.nbt.CompoundTag());var b=other.save(new net.minecraft.nbt.CompoundTag());Files.writeString(Path.of(e.getKey().replace('/','-')+"-effective.snbt"),a.toString());Files.writeString(Path.of(e.getKey().replace('/','-')+"-other-effective.snbt"),b.toString());if(!fixed){var export=repo.resolve("build/native-upgraded-templates").resolve(e.getKey()+".nbt");Files.createDirectories(export.getParent());net.minecraft.nbt.NbtIo.writeCompressed(a,export);}equal.put(e.getKey(),a.equals(b));
  }report.put("nativeEffectiveEquality",equal);Files.writeString(Path.of("result.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));WellInteraction.require(equal.values().stream().allMatch(Boolean::booleanValue),"Exact native template equivalence "+equal);Files.writeString(Path.of("result.txt"),"PASS native template load and exact effective metadata equivalence");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("TEMPLATE_VERSION_AUDIT_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
