package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/** Natural village paths after FULL generation, with repeatable chunk admission and reopen. */
public final class VillagePathQualification {
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            OverworldCoverageQualification.write("generator.json",RealisticGenerator.CODEC.codec().encodeStart(RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow());
            var all=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.villageWitness")))).getAsJsonObject();
            var report=new TreeMap<String,Object>();int total=0;BlockPos edit=null;
            var editFile=Path.of("player-edit.txt");boolean reopen=Files.exists(editFile);
            if(reopen){var xyz=Files.readString(editFile).split(",");edit=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));OverworldCoverageQualification.require(level.getBlockState(edit).is(Blocks.DIAMOND_BLOCK),"Player path edit survives reopen");}
            for(String site:List.of("village_plains","village_savanna","village_desert","village_taiga","village_snowy")){
                var at=all.getAsJsonObject(site).getAsJsonArray("chunk");var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace(site)));
                var c=level.getChunk(at.get(0).getAsInt(),at.get(1).getAsInt(),net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
                var start=level.structureManager().getStartForStructure(holder.value(),c);OverworldCoverageQualification.require(start!=null&&start.isValid(),"Natural "+site);
                var box=StructurePiece.createBoundingBox(start.getPieces().stream());var chunks=new ArrayList<ChunkPos>();
                for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)chunks.add(new ChunkPos(x,z));
                if(Boolean.getBoolean("blockbranch.villageReverse"))Collections.reverse(chunks);
                for(var p:chunks)level.getChunk(p.x(),p.z());
                var bad=new ArrayList<String>();int paths=0,nearbyPlants=0,profileMatches=0,boundaryPaths=0,doors=0,entrances=0;BlockPos entranceDoor=null,entrancePath=null;var pos=new BlockPos.MutableBlockPos();
                var roads=new ArrayList<String>();var controls=new ArrayList<String>();
                for(int z=box.minZ();z<=box.maxZ();z++)for(int x=box.minX();x<=box.maxX();x++){
                    int ground=g.drainage(level.getChunkSource().randomState()).sample(x,z).height();
                    for(int y=ground-24;y<=ground+24;y++){
                        var state=level.getBlockState(pos.set(x,y,z));
                        if(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock&&state.getValue(net.minecraft.world.level.block.DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER){
                            doors++;boolean connected=false;
                            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int distance=1;distance<=3;distance++)for(int step=-1;step<=0;step++)if(level.getBlockState(new BlockPos(x,y+step,z).relative(direction,distance)).is(Blocks.DIRT_PATH)){
                                connected=true;if(entranceDoor==null){entranceDoor=new BlockPos(x,y,z);entrancePath=new BlockPos(x,y+step,z).relative(direction,distance);}
                            }
                            if(connected)entrances++;
                        }
                        if(VillagePaths.groundCover(state)){
                            nearbyPlants++;int rootY=y-1;
                            if(state.hasProperty(net.minecraft.world.level.block.DoublePlantBlock.HALF)&&state.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)rootY--;
                            if(!level.getBlockState(new BlockPos(x,rootY,z)).is(Blocks.DIRT_PATH))controls.add(x+","+y+","+z+" "+state);
                        }
                        if(!state.is(Blocks.DIRT_PATH))continue;if(edit==null&&!Boolean.getBoolean("blockbranch.villageBaseline")&&level.getBlockState(pos.above()).isAir())edit=pos.above().immutable();paths++;roads.add(x+","+y+","+z);if((x&15)==0||(x&15)==15||(z&15)==0||(z&15)==15)boundaryPaths++;
                        for(int dy=1;dy<=2;dy++){
                            var above=level.getBlockState(pos.set(x,y+dy,z));
                            if(VillagePathAudit.obstruction(above,level,pos)){bad.add(pos.toString()+" "+above);if(above.equals(g.surfaceState(x,y+dy,z,level.getChunkSource().randomState())))profileMatches++;}
                        }
                    }
                }
                var wild=new ArrayList<String>();int wildMatches=0;
                int wildX=(box.maxX()+256)&~15,wildZ=(box.maxZ()+256)&~15;
                level.getChunk(wildX>>4,wildZ>>4);
                for(int z=wildZ;z<wildZ+16;z++)for(int x=wildX;x<wildX+16;x++){
                    int ground=g.drainage(level.getChunkSource().randomState()).sample(x,z).height();
                    for(int y=ground+1;y<=ground+2;y++){var actual=level.getBlockState(new BlockPos(x,y,z));if(VillagePaths.groundCover(actual)){wild.add(x+","+y+","+z+" "+actual);if(actual.equals(g.surfaceState(x,y,z,level.getChunkSource().randomState())))wildMatches++;}}
                }
                if(site.equals("village_plains")){
                    OverworldCoverageQualification.require(entranceDoor!=null&&entrancePath!=null,"Representative natural entrance path exists");
                    if(!Boolean.getBoolean("blockbranch.villageBaseline"))for(int dy=1;dy<=2;dy++){
                        var feet=entrancePath.above(dy);OverworldCoverageQualification.require(!VillagePathAudit.obstruction(level.getBlockState(feet),level,feet),"Representative entrance clear "+feet);
                    }
                    OverworldCoverageQualification.write("natural-entrance.json",Map.of("door",List.of(entranceDoor.getX(),entranceDoor.getY(),entranceDoor.getZ()),"path",List.of(entrancePath.getX(),entrancePath.getY(),entrancePath.getZ()),"baseline",Boolean.getBoolean("blockbranch.villageBaseline")));
                }
                OverworldCoverageQualification.write(site+"-plants.json",Map.of("nonPath",controls,"wild",wild));
                var digest=java.security.MessageDigest.getInstance("SHA-256");
                String roadHash=HexFormat.of().formatHex(digest.digest(String.join("\n",roads).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                String controlHash=HexFormat.of().formatHex(digest.digest(String.join("\n",controls).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                report.put(site,Map.of("paths",paths,"plants",nearbyPlants,"blocked",bad,"profileMatches",profileMatches,"boundaryPaths",boundaryPaths,"roadHash",roadHash,"nonPathPlants",controls.size(),"nonPathHash",controlHash,"entrances",Map.of("doors",doors,"pathConnected",entrances),"wild",Map.of("plants",wild.size(),"profileMatches",wildMatches,"hash",HexFormat.of().formatHex(digest.digest(String.join("\n",wild).getBytes(java.nio.charset.StandardCharsets.UTF_8))))));total+=bad.size();
                blockbranch.Blockbranch.LOGGER.info("VILLAGE_PATH {} paths={} plants={} blocked={}",site,paths,nearbyPlants,bad.size());
            }
            OverworldCoverageQualification.write("village-paths.json",report);
            OverworldCoverageQualification.write("path-write-audit.json",Map.of("plantsBeforeNativePathWrite",VillagePathAudit.PREEXISTING.sum(),"reopen",reopen));
            VillagePathProbe.verify(level);
            if(edit!=null&&!reopen){level.setBlock(edit,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);Files.writeString(editFile,edit.getX()+","+edit.getY()+","+edit.getZ());}
            server.saveEverything(false,true,true);
            if(!Boolean.getBoolean("blockbranch.villageBaseline"))OverworldCoverageQualification.require(total==0,"Vegetation on "+total+" paths");
            Files.writeString(Path.of("result.txt"),"PASS blocked="+total);
        }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("VILLAGE_PATH_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
}
