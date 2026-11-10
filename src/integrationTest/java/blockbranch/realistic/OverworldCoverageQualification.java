package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.placement.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;

/** Natural placement survey plus actual FULL chunks and native state witnesses. */
public final class OverworldCoverageQualification {
    static final com.google.gson.Gson JSON=new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    static void require(boolean b,String m){if(!b)throw new IllegalStateException(m);}
    static void write(String name,Object value)throws Exception{Files.writeString(Path.of(name),JSON.toJson(value));}
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{
        try{run(server.overworld());Files.writeString(Path.of("result.txt"),"PASS");}
        catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("OVERWORLD_COVERAGE_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    static void run(ServerLevel level)throws Exception{
        var g=(RealisticGenerator)level.getChunkSource().getGenerator();var state=level.getChunkSource().getGeneratorState();
        var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);var random=level.getChunkSource().randomState();
        require(g.overworldDefinition()==1,"Explicit coverage definition");
        var ops=RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
        require(encoded.get("shape_version").getAsInt()==102,"New downgrade barrier");
        var old=encoded.deepCopy();old.addProperty("shape_version",101);old.remove("native_overworld_structures");old.getAsJsonObject("additional_biomes").remove("deep_dark");
        var prior=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();require(prior.overworldDefinition()==0,"Saved101 opt-out");
        var priorState=prior.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),random,random.seed());
        require(priorState.possibleStructureSets().stream().flatMap(h->h.value().structures().stream()).noneMatch(e->OverworldStructures.additional(registry.getKey(e.structure().value()))),"Saved101 unexplored native sets unchanged");
        var missing=encoded.deepCopy();missing.remove("native_overworld_structures");require(RealisticGenerator.CODEC.codec().parse(ops,missing).error().isPresent(),"102 cannot silently lose coverage");
        write("generator.json",encoded);
        var targets=registry.listElements().filter(h->OverworldStructures.additional(h.key().identifier())).toList();
        var inventory=new TreeMap<String,Object>();
        for(var holder:targets)inventory.put(holder.key().identifier().toString(),Map.of("placements",state.getPlacementsForStructure(holder).size(),"biomes",holder.value().biomes().stream().map(h->h.unwrapKey().map(k->k.identifier().toString()).orElse("direct")).toList()));
        write("inventory.json",inventory);
        var source=((ScreeBiomeSource)g.getBiomeSource()).surfaceResolver();
        var anchors=new HashMap<Identifier,List<ChunkPos>>();var rng=new java.util.Random(7241);
        for(int i=0;i<30000;i++){
            int x=rng.nextInt(4100000)-2050000,z=rng.nextInt(4100000)-2050000;var biome=source.getNoiseBiome(x>>2,0,z>>2);
            for(var h:targets){
                if(h.key().identifier().getPath().equals("ancient_city")){if(!OverworldStructures.deepDark(random.seed(),x,DeepDarkHabitat.proposalY(g.drainage(random).sample(x,z).height()),z,g.drainage(random)))continue;}
                else if(h.key().identifier().getPath().equals("monument")){var s=g.drainage(random).sample(x,z);if(!g.drainage(random).oceanWater(x,z,s)||s.height()<7||s.height()>38)continue;}
                else if(!h.value().biomes().contains(biome))continue;
                var list=anchors.computeIfAbsent(h.key().identifier(),id->new ArrayList<>());if(list.size()<16)list.add(new ChunkPos(x>>4,z>>4));
            }
        }
        for(var plan:g.wetlandHuts(random).plans()){
            for(int dz=-32;dz<=32;dz+=8)for(int dx=-32;dx<=32;dx+=8){
                int x=plan.x()+dx,z=plan.z()+dz;var biome=source.getNoiseBiome(x>>2,0,z>>2);
                if(!biome.is(net.minecraft.world.level.biome.Biomes.SWAMP))continue;
                for(var h:targets)if(h.value().biomes().contains(biome)){var list=anchors.computeIfAbsent(h.key().identifier(),id->new ArrayList<>());if(list.size()<128)list.add(new ChunkPos(x>>4,z>>4));}
            }
        }
        write("anchors.json",anchors.entrySet().stream().collect(java.util.stream.Collectors.toMap(e->e.getKey().toString(),e->e.getValue().stream().map(p->List.of(p.x(),p.z())).toList())));
        var witnesses=new TreeMap<String,Object>();var failures=new ArrayList<String>();
        for(var h:targets){
            String name=h.key().identifier().getPath();
            if(name.equals("abandoned_camp_pale_garden")){require(!anchors.containsKey(h.key().identifier()),"Pale garden habitat absent");continue;}
            require(!state.getPlacementsForStructure(h).isEmpty(),"Set is exposed "+name);
            StructureStart found=null;ChunkPos at=null;
            for(var placement:state.getPlacementsForStructure(h)){
                List<ChunkPos> candidates=new ArrayList<>();
                if(placement instanceof ConcentricRingsStructurePlacement rings)candidates.addAll(state.getRingPositionsFor(rings));
                else if(placement instanceof RandomSpreadStructurePlacement spread){
                    for(var anchor:anchors.getOrDefault(h.key().identifier(),List.of()))for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++)
                        candidates.add(spread.getPotentialStructureChunk(random.seed(),anchor.x()+dx*spread.spacing(),anchor.z()+dz*spread.spacing()));
                }
                for(var p:candidates){
                    if(!placement.isStructureChunk(state,p.x(),p.z()))continue;
                    var start=h.value().generate(h,level.dimension(),level.registryAccess(),g,g.getBiomeSource(),random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),random,level.getServer().getStructureTemplateManager(),random.seed(),p,0,generationBounds(),h.value().biomes()::contains);
                    if(!start.isValid())continue;
                    var actual=level.getChunk(p.x(),p.z(),ChunkStatus.STRUCTURE_STARTS);var natural=level.structureManager().getStartForStructure(h.value(),actual);
                    if(natural==null||!natural.isValid())continue;
                    found=natural;at=p;break;
                }
                if(found!=null)break;
            }
            if(found==null){failures.add(name+" anchors="+anchors.getOrDefault(h.key().identifier(),List.of()).size());write("missing.json",failures);continue;}
            if(name.equals("ancient_city"))require(StructurePiece.createBoundingBox(found.getPieces().stream()).maxY()+16<=blockbranch.cubic.TerrainProfile.DEEPSLATE_TOP,"Saved city fixture predates deepslate correction; use a fresh coverage directory");
            var context=StructurePieceSerializationContext.fromLevel(level);var saved=found.createTag(context,at);var restored=StructureStart.loadStaticStart(context,saved,random.seed());
            require(restored!=null&&restored.isValid()&&restored.getPieces().size()==found.getPieces().size(),"Native start roundtrip pieces "+name);
            var round=restored.createTag(context,at);
            var before=saved.getListOrEmpty("Children");var after=round.getListOrEmpty("Children");
            for(int i=0;i<before.size();i++){var a=((net.minecraft.nbt.CompoundTag)before.get(i)).copy();var b=((net.minecraft.nbt.CompoundTag)after.get(i)).copy();a.remove("BB");b.remove("BB");require(a.equals(b),"Native saved transform/state roundtrip "+name+" piece="+i);}
            var box=found.getBoundingBox();witnesses.put(name,Map.of("chunk",List.of(at.x(),at.z()),"bounds",List.of(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()),"pieces",found.getPieces().size(),"roundtrip",true));
            write("starts.json",witnesses);Files.writeString(Path.of("start-"+name+".snbt"),saved.toString());
            blockbranch.Blockbranch.LOGGER.info("OVERWORLD_START_PASS {} {} pieces={}",name,at,found.getPieces().size());
        }
        require(failures.isEmpty(),"Missing natural starts: "+failures);
        level.getServer().saveEverything(false,true,true);
    }
    static LevelHeightAccessor generationBounds(){return LevelHeightAccessor.create(TerrainShape.FLOOR,TerrainShape.CEILING-TerrainShape.FLOOR);}
}
