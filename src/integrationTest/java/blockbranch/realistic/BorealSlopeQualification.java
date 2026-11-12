package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.CanopyLayer;
import blockbranch.cubic.SparseColumn;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.JsonOps;

/**
 * Native boreal-slope qualification over the sampled treeless slope (seed 1). Modes:
 * {@code record270} writes a published-270 world window with per-chunk profile hashes (run it on a
 * build before any slope rule exists); {@code verify270} reopens that closed world and requires the
 * saved chunks to read back unchanged and the unexplored remainder of the window to stay treeless.
 * The window is {@code 2*HALF_CHUNKS} chunks square; the saved core is its central 4 x 4 chunks.
 */
public final class BorealSlopeQualification {
    static final int SITE_X=-1392000,SITE_Z=-1616000,HALF_CHUNKS=4;
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.borealSlopeDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var json=RealisticGenerator.CODEC.codec().encodeStart(RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().getAsJsonObject();
            int shape=json.get("shape_version").getAsInt(),slope=json.has("boreal_slope_definition")?json.get("boreal_slope_definition").getAsInt():0;
            if(level.getSeed()!=1)throw new AssertionError("Seed 1 world required");
            int cx=SITE_X>>4,cz=SITE_Z>>4;Path signature=dir.resolve("signature.txt");
            switch(mode){
                case "record270"->{
                    if(shape!=270||slope!=0)throw new AssertionError("Published schema 270 build required: "+shape+" "+slope);
                    var lines=new ArrayList<String>();
                    for(int dz=-2;dz<2;dz++)for(int dx=-2;dx<2;dx++)lines.add(describe(level,cx+dx,cz+dz));
                    long logs=lines.stream().mapToLong(l->Long.parseLong(l.split(" ")[3])).sum();
                    if(logs!=0)throw new AssertionError("Published 270 sampled window is treeless; logs="+logs);
                    Files.write(signature,lines);server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS recorded published 270 treeless window: "+lines.size()+" chunks, "+logs+" logs\n");
                }
                case "verify270"->{
                    if(shape!=270||slope!=0)throw new AssertionError("Saved published schema 270 world must still read as 270 with the slope definition absent: "+shape+" "+slope);
                    var saved=Files.readAllLines(signature);int i=0;
                    for(int dz=-2;dz<2;dz++)for(int dx=-2;dx<2;dx++)if(!describe(level,cx+dx,cz+dz).equals(saved.get(i++)))throw new AssertionError("Saved published chunk changed on reopen: "+(cx+dx)+","+(cz+dz));
                    long logs=0;int fresh=0;
                    for(int dz=-HALF_CHUNKS;dz<HALF_CHUNKS;dz++)for(int dx=-HALF_CHUNKS;dx<HALF_CHUNKS;dx++){
                        if(dx>=-2&&dx<2&&dz>=-2&&dz<2)continue;
                        logs+=Long.parseLong(describe(level,cx+dx,cz+dz).split(" ")[3]);fresh++;
                    }
                    if(logs!=0)throw new AssertionError("Unexplored chunks of a published 270 world must stay treeless; logs="+logs);
                    Files.writeString(dir.resolve("result.txt"),"PASS verify270: "+saved.size()+" saved chunks identical, "+fresh+" unexplored chunks treeless\n");
                }
                default->throw new IllegalArgumentException("Unknown mode "+mode);
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("BOREAL_SLOPE_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    /** {@code cx cz sha256(profile) logs leaves}: the saved profile hash and the wood/leaf block counts of its canopy. */
    static String describe(net.minecraft.server.level.ServerLevel level,int cx,int cz)throws Exception{
        var base=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain().base();
        int logs=0,leaves=0;for(int packed:base.canopy().encode()){int code=packed&CanopyLayer.STATE_MASK;if(WoodlandTrees.logCode(code))logs++;else leaves++;}
        var hash=MessageDigest.getInstance("SHA-256").digest(base.encode(level.palettedContainerFactory()).toString().getBytes());
        return cx+" "+cz+" "+HexFormat.of().formatHex(hash)+" "+logs+" "+leaves;
    }
    private BorealSlopeQualification(){}
}
