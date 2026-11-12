package blockbranch.realistic;

import com.google.gson.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;

/** Whole-profile preflight and dense original scalar states. Rejected original
 * profiles are recorded holes, never fabricated native output or passing samples. */
public final class Published269CoverAudit {
    static Set<Integer> capture(ServerLevel level,RealisticGenerator g,Path output)throws Exception{
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var entries=new ArrayList<Object>();var excluded=new TreeSet<Integer>();var physical=MessageDigest.getInstance("SHA-256");
        int reads=0;var visited=new HashSet<ChunkPos>();
        for(int point=0;point<Published269Snapshot.POINTS.length;point++){
            var center=Published269Snapshot.POINTS[point];
            for(int oz=-8;oz<=8;oz++)for(int ox=-8;ox<=8;ox++){
                var pos=new ChunkPos((center[0]>>4)+ox,(center[1]>>4)+oz);
                boolean core=Math.abs(ox)<=3&&Math.abs(oz)<=3,dense=Math.abs(ox)<=1&&Math.abs(oz)<=1,invalid=false;
                var proto=new ProtoChunk(pos,UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
                try{
                    if(core){var profile=g.profile(proto,random);String encoded=profile.encode(level.palettedContainerFactory()).toString();entries.add(Map.of("point",point,"chunk",List.of(pos.x(),pos.z()),"valid",true,"profileSHA256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8)))));}
                    else{g.prepareCaveProfile(proto,random);entries.add(Map.of("point",point,"chunk",List.of(pos.x(),pos.z()),"stage","BIOMES cave-preparation (may be no-op)","valid",true));}
                }
                catch(IllegalArgumentException failure){
                    if(!failure.getMessage().equals("Invalid woodland cover"))throw failure;
                    invalid=true;excluded.add(point);var conflicts=new ArrayList<Object>();
                    for(int z=pos.getMinBlockZ();z<=pos.getMaxBlockZ();z++)for(int x=pos.getMinBlockX();x<=pos.getMaxBlockX();x++){
                        var s=d.sample(x,z);int woodland=g.woodlandCover(x,z,random),river=g.riverMarginCover(x,z,random),meadow=g.meadowCover(x,z,random);
                        if(woodland>0&&(river>0||meadow>0||s.waterLevel()>s.height()))conflicts.add(Map.of("x",x,"z",z,"height",s.height(),"water",s.waterLevel(),"woodland",woodland,"river",river,"meadow",meadow));
                    }
                    entries.add(Map.of("point",point,"chunk",List.of(pos.x(),pos.z()),"valid",false,"error",failure.toString(),"conflicts",conflicts));
                    blockbranch.Blockbranch.LOGGER.warn("PUBLISHED269_PROFILE_REJECTED seed={} point={} chunk={} conflicts={}",random.seed(),point,pos,conflicts);
                }
                if((!dense&&!invalid)||!visited.add(pos))continue;
                for(int z=pos.getMinBlockZ();z<=pos.getMaxBlockZ();z++)for(int x=pos.getMinBlockX();x<=pos.getMaxBlockX();x++){
                    int h=d.sample(x,z).height();for(int y=h-3;y<=h+40;y++){physical.update((x+","+y+","+z+":"+g.surfaceState(x,y,z,random)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));reads++;}
                }
            }
        }
        var report=Map.of("seed",random.seed(),"profiles",entries,"nativeExcludedPoints",excluded,"densePhysicalReads",reads,"densePhysicalSHA256",HexFormat.of().formatHex(physical.digest()),"scope","Original profile rejection is a recorded counterexample, not a completed native comparison. Direct profile preparation does not generate native chunks.");
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");return excluded;
    }
    public static void logFailure(RealisticGenerator g,net.minecraft.world.level.chunk.ChunkAccess chunk,net.minecraft.world.level.levelgen.RandomState random,Throwable failure)throws Exception{
        var pos=chunk.getPos();var d=g.drainage(random);var cells=new ArrayList<Object>();
        for(int z=pos.getMinBlockZ();z<=pos.getMaxBlockZ();z++)for(int x=pos.getMinBlockX();x<=pos.getMaxBlockX();x++){
            int wood=g.woodlandCover(x,z,random);if(wood==0)continue;var q=d.sample(x,z);
            int scree=g.screeDepth(x,z,random),reed=g.reedbedCover(x,z,random),river=g.riverMarginCover(x,z,random),shore=g.sandyShoreCover(x,z,random),meadow=g.meadowCover(x,z,random);
            if(wood<0||wood>LeafLitter.WOODLAND_LAST||scree>0||reed>0||river>0||shore>0||meadow>0||q.waterLevel()>q.height())cells.add(Map.of("xyz",List.of(x,q.height(),z),"water",q.waterLevel(),"woodland",wood,"screeReedRiverShoreMeadow",List.of(scree,reed,river,shore,meadow)));
        }
        var report=Map.of("seed",random.seed(),"failedNativeChunk",List.of(pos.x(),pos.z()),"exception",failure.toString(),"vegetationFlags",g.vegetationFlags(),"cells",cells);
        Path result=Path.of(System.getProperty("blockbranch.published269SnapshotResult"));
        Files.writeString(result.resolveSibling("native-profile-error-"+pos.x()+"-"+pos.z()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
        blockbranch.Blockbranch.LOGGER.error("PUBLISHED269_NATIVE_PROFILE_FAILURE {}",report);
    }
    private Published269CoverAudit(){}
}
