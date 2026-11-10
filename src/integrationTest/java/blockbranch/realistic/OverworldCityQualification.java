package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.placement.*;

/** Positive/negative placement and enclosure probes tied to actual host-rock strata. */
public final class OverworldCityQualification {
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
        OverworldCoverageQualification.require(blockbranch.cubic.TerrainProfile.material(TerrainShape.FLOOR,TerrainShape.CEILING,blockbranch.cubic.TerrainProfile.DEEPSLATE_TOP,500,DrainageNetwork.DRY,0).is(net.minecraft.world.level.block.Blocks.DEEPSLATE)&&blockbranch.cubic.TerrainProfile.material(TerrainShape.FLOOR,TerrainShape.CEILING,blockbranch.cubic.TerrainProfile.DEEPSLATE_TOP+1,500,DrainageNetwork.DRY,0).is(net.minecraft.world.level.block.Blocks.STONE),"Boundary is the actual terrain material transition");
        var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var terrain=g.drainage(random);
        var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.ANCIENT_CITY);
        var placement=(RandomSpreadStructurePlacement)level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).getFirst();
        var anchors=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.overworldAnchors")))).getAsJsonObject().getAsJsonArray("minecraft:ancient_city");
        var records=new ArrayList<Object>();var seen=new HashSet<ChunkPos>();
        positive:for(var entry:anchors){var coordinates=entry.getAsJsonArray();int cx=coordinates.get(0).getAsInt(),cz=coordinates.get(1).getAsInt();
            for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                var at=placement.getPotentialStructureChunk(random.seed(),cx+dx*placement.spacing(),cz+dz*placement.spacing());if(!seen.add(at))continue;
                var start=generate(level,g,holder,at);if(!start.isValid())continue;
                var b=start.getBoundingBox(); // Includes native adaptation padding.
                OverworldCoverageQualification.require(b.maxY()+4<=blockbranch.cubic.TerrainProfile.DEEPSLATE_TOP,"Sampled city envelope/roof remains inside actual deepslate layer");
                int roof=Integer.MAX_VALUE;
                for(int z=b.minZ();z<=b.maxZ();z+=16)for(int x=b.minX();x<=b.maxX();x+=16)roof=Math.min(roof,terrain.sample(x,z).height()-b.maxY());
                OverworldCoverageQualification.require(roof>=16,"Sampled city remains enclosed below local terrain");
                var context=StructurePieceSerializationContext.fromLevel(level);var restored=StructureStart.loadStaticStart(context,start.createTag(context,at),random.seed());
                OverworldCoverageQualification.require(restored!=null&&restored.isValid()&&restored.getBoundingBox().equals(b),"Sampled city saved elevation/envelope");
                var point=b.getCenter();OverworldCoverageQualification.require(g.getBiomeSource().createResolver(null).getNoiseBiome(point.getX()>>2,point.getY()>>2,point.getZ()>>2).is(net.minecraft.world.level.biome.Biomes.DEEP_DARK),"Sampled city actual biome");
                OverworldCoverageQualification.require(!OverworldStructures.deepDark(random.seed(),point.getX(),0,point.getZ(),terrain),"Positive-Y stone is never deep-dark city habitat");
                records.add(Map.of("chunk",List.of(at.x(),at.z()),"bounds",b.toString(),"minimumRoof",roof,"pieces",start.getPieces().size()));
                if(records.size()==64)break positive;
            }
        }
        OverworldCoverageQualification.require(records.size()==64,"64 varied positive city placements");
        var rng=new Random(2741);int ocean=0,lowland=0;
        for(int attempt=0;attempt<10000&&(ocean<32||lowland<32);attempt++){
            int wx=rng.nextInt(4100000)-2050000,wz=rng.nextInt(4100000)-2050000;var column=new ChunkPos(wx>>4,wz>>4);int x=column.getMiddleBlockX(),z=column.getMiddleBlockZ();var s=terrain.sample(x,z);boolean water=s.waterLevel()!=DrainageNetwork.DRY;
            if(water&&ocean<32||!water&&s.height()<=384&&lowland<32){
                var at=new ChunkPos(x>>4,z>>4);OverworldCoverageQualification.require(!generate(level,g,holder,at).isValid(),"Reject unsuitable city column "+at);
                if(water)ocean++;else lowland++;
            }
        }
        OverworldCoverageQualification.require(ocean==32&&lowland==32,"64 negative city terrain cases");
        OverworldCoverageQualification.write("city-placement.json",Map.of("deepslateTop",blockbranch.cubic.TerrainProfile.DEEPSLATE_TOP,"positive",records,"oceanRejected",ocean,"lowlandRejected",lowland));
        Files.writeString(Path.of("result.txt"),"PASS");
    }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("OVERWORLD_CITY_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
    static StructureStart generate(net.minecraft.server.level.ServerLevel level,RealisticGenerator g,Holder<Structure> holder,ChunkPos at){
        var random=level.getChunkSource().randomState();return holder.value().generate(holder,level.dimension(),level.registryAccess(),g,g.getBiomeSource(),random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),random,level.getServer().getStructureTemplateManager(),random.seed(),at,0,OverworldCoverageQualification.generationBounds(),holder.value().biomes()::contains);
    }
}
