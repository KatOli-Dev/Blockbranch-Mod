package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.levelgen.structure.*;

/** Mixed planned/native tags must choose and reference the actual nearest start. */
public final class OverworldLocateQualification {
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
        var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var witnesses=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.overworldWitness","../overworld-coverage/starts.json")))).getAsJsonObject();var results=new ArrayList<Object>();
        for(String kind:List.of("shipwreck","ruined_portal")){
            var at=witnesses.getAsJsonObject(kind).getAsJsonArray("chunk");int cx=at.get(0).getAsInt(),cz=at.get(1).getAsInt();var origin=new BlockPos(cx*16,0,cz*16);
            var nativeHolder=registry.getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace(kind)));
            var planned=registry.getOrThrow(kind.equals("shipwreck")?BuiltinStructures.SHIPWRECK_BEACHED:BuiltinStructures.RUINED_PORTAL_DESERT);
            var targets=HolderSet.direct(nativeHolder,planned);var chunk=level.getChunk(cx,cz,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            var start=level.structureManager().getStartForStructure(nativeHolder.value(),chunk);OverworldCoverageQualification.require(start!=null&&start.isValid(),"Native mixed-tag witness "+kind);
            int before=start.getReferences();var found=g.findNearestMapStructure(level,targets,origin,0,true);
            if(before==0)OverworldCoverageQualification.require(found!=null&&found.getSecond().equals(nativeHolder)&&start.getReferences()==before+1,"Mixed native selection and exactly one reference "+kind);
            else OverworldCoverageQualification.require(found==null&&start.getReferences()==before,"Saved known native start remains skipped "+kind);
            int expected=before==0?1:before;
            var known=g.findNearestMapStructure(level,targets,origin,0,false);OverworldCoverageQualification.require(known!=null&&known.getSecond().equals(nativeHolder)&&start.getReferences()==expected,"Read-only locate preserves references "+kind);
            var skip=g.findNearestMapStructure(level,targets,origin,0,true);OverworldCoverageQualification.require(skip==null&&start.getReferences()==expected,"Already known native start skipped "+kind);
            results.add(Map.of("kind",kind,"position",known.getFirst().toShortString(),"referencesBefore",before,"referencesAfter",start.getReferences()));
        }
        var ops=RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());var meta=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
        var old=meta.deepCopy();old.addProperty("shape_version",101);old.remove("native_overworld_structures");
        var optOut=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();var state=optOut.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),level.getChunkSource().randomState(),level.getSeed());
        var city=witnesses.getAsJsonObject("ancient_city").getAsJsonArray("bounds");int x=(city.get(0).getAsInt()+city.get(3).getAsInt())/2,y=(city.get(1).getAsInt()+city.get(4).getAsInt())/2,z=(city.get(2).getAsInt()+city.get(5).getAsInt())/2;
        var resolver=optOut.getBiomeSource().createResolver(null);OverworldCoverageQualification.require(!resolver.getNoiseBiome(x>>2,y>>2,z>>2).is(net.minecraft.world.level.biome.Biomes.DEEP_DARK),"Coverage opt-out keeps cave identity even with extra palette holder");
        OverworldCoverageQualification.require(state.getPlacementsForStructure(registry.getOrThrow(BuiltinStructures.ANCIENT_CITY)).isEmpty(),"Coverage opt-out keeps ancient cities absent");
        OverworldCoverageQualification.write("locate.json",results);server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS");
    }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("OVERWORLD_LOCATE_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
