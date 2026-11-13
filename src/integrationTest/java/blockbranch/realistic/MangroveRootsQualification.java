package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.CanopyLayer;
import blockbranch.cubic.SparseColumn;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.Blocks;

/**
 * Native generated-chunk witnesses for the gated arched mangrove roots at the audited seed-1 swamp. The window is the
 * 5 x 5 chunks the frozen published fixture covers, read from the chunks' own saved canopy (the real generation path).
 * Modes: {@code baseline272} (a world from the gate-absent baseline preset must reproduce the frozen published
 * columns bit for bit), {@code fresh273} (the bundled preset: gate on, arched, thinner, edit and signature saved),
 * {@code verify273} (reopen that closed world in a second process) and {@code legacy272} (reopen an authentic
 * schema-272 world written before the gate existed: shape 272, gate absent, the never-explored swamp window still the
 * frozen published columns). Counts are finite witnesses at one site, not coverage.
 */
public final class MangroveRootsQualification {
    static final int SITE_X=1872000,SITE_Z=-144000;
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.mangroveRootsDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var json=RealisticGenerator.CODEC.codec().encodeStart(RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().getAsJsonObject();
            int shape=json.get("shape_version").getAsInt(),gate=g.mangroveRootsDefinition();
            if(level.getSeed()!=1L)throw new AssertionError("Seed 1 world required");
            int cx=SITE_X>>4,cz=SITE_Z>>4;var window=window(level,cx,cz);Path signature=dir.resolve("signature.txt");var edit=new BlockPos(SITE_X+3,g.drainage(level.getChunkSource().randomState()).sample(SITE_X+3,SITE_Z+3).height(),SITE_Z+3);
            switch(mode){
                case "baseline272"->{
                    if(shape!=272||gate!=0)throw new AssertionError("Gate-absent 272 baseline required: "+shape+" "+gate);
                    requirePublished(window);Files.writeString(signature,window.digest());
                    Files.writeString(dir.resolve("result.txt"),"PASS baseline272: "+window+" equals the frozen published columns\n");
                }
                case "fresh273"->{
                    if(shape!=273||gate!=1||json.get("mangrove_roots_definition").getAsInt()!=1)throw new AssertionError("Bundled schema 273 preset required: "+shape+" "+gate);
                    var published=published();var old=window.digest().equals(published.digest());
                    if(old)throw new AssertionError("The gate changed nothing at the audited swamp");
                    if(window.roots==0)throw new AssertionError("No arched roots generated: "+window);
                    if(window.bulkShare()>.20)throw new AssertionError("Arched roots still form square mass: "+window);
                    level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                    Files.writeString(signature,window.digest());server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS fresh273: "+window+" (published window sha256 "+published.sha+" differs), edit "+edit.toShortString()+"\n");
                }
                case "verify273"->{
                    if(shape!=273||gate!=1)throw new AssertionError("Saved schema 273 world must reopen as 273: "+shape+" "+gate);
                    if(!window.digest().equals(Files.readString(signature)))throw new AssertionError("Saved arched window changed on reopen");
                    if(!level.getBlockState(edit).is(Blocks.GOLD_BLOCK))throw new AssertionError("Explicit edit lost across reopen at "+edit);
                    Files.writeString(dir.resolve("result.txt"),"PASS verify273: "+window+" identical, edit retained\n");
                }
                case "legacy272"->{
                    if(shape!=272||gate!=0||g.borealSlopeDefinition()!=1)throw new AssertionError("Authentic 272 world must reopen as 272 with the gate absent: "+shape+" "+gate);
                    requirePublished(window);
                    Files.writeString(dir.resolve("result.txt"),"PASS legacy272: never-explored swamp window "+window+" equals the frozen published columns\n");
                }
                default->throw new IllegalArgumentException("Unknown mode "+mode);
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("MANGROVE_ROOTS_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    /** The frozen published audit row: ints in the window and sha256 of each column's Arrays.toString, x then z. */
    private static Window published()throws Exception{
        for(var line:Files.readAllLines(Path.of(System.getProperty("blockbranch.mangroveRootsPublished"))))if(line.startsWith("audit ")){var f=line.split(" ");return new Window(Integer.parseInt(f[4]),f[5]);}
        throw new AssertionError("Frozen audit row missing");
    }
    private static void requirePublished(Window w)throws Exception{
        var p=published();if(w.ints!=p.ints||!w.digest().equals(p.digest()))throw new AssertionError("Window differs from the frozen published columns: "+w+" vs "+p);
    }
    private static Window window(net.minecraft.server.level.ServerLevel level,int cx,int cz)throws Exception{
        var md=MessageDigest.getInstance("SHA-256");int ints=0;var cells=new HashMap<Long,Integer>();var perLayer=new HashMap<Integer,Integer>();
        for(int z=cz-2;z<=cz+2;z++)for(int x=cx-2;x<=cx+2;x++){
            var column=((SparseColumn)level.getChunk(x,z)).blockbranch$terrain().base().canopy().encode();ints+=column.length;md.update(Arrays.toString(column).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for(int packed:column){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;if(code==MangroveTrees.ROOT||code==MangroveTrees.WET_ROOT){int wx=x*16+(key&15),wz=z*16+((key>>>4)&15),wy=(key>>>8)+TerrainShape.FLOOR;cells.put(pack(wx,wy,wz),code);perLayer.merge(wy,1,Integer::sum);}}
        }
        long bulk=0;for(long k:cells.keySet()){int n=0;
            for(var q:DIR)if(cells.containsKey(pack(unpackX(k)+q[0],unpackY(k)+q[1],unpackZ(k)+q[2])))n++;if(n>=3)bulk++;}
        int most=0;for(int v:perLayer.values())most=Math.max(most,v);
        return new Window(ints,HexFormat.of().formatHex(md.digest()),cells.size(),bulk,most);
    }
    private static long pack(int x,int y,int z){return ((long)(x&0xFFFFF)<<40)|((long)((y+0x80000)&0xFFFFF)<<20)|((z+0x80000)&0xFFFFFL);}
    private static int unpackX(long k){int v=(int)((k>>40)&0xFFFFF);return v>=0x80000?v-0x100000:v;}
    private static int unpackY(long k){return (int)((k>>20)&0xFFFFF)-0x80000;}
    private static int unpackZ(long k){return (int)(k&0xFFFFF)-0x80000;}
    private static final class Window {
        final int ints,roots;final long bulk;final int densest;final String sha;
        Window(int ints,String sha){this(ints,sha,0,0,0);}
        Window(int ints,String sha,int roots,long bulk,int densest){this.ints=ints;this.sha=sha;this.roots=roots;this.bulk=bulk;this.densest=densest;}
        String digest(){return ints+" "+sha;}
        double bulkShare(){return roots==0?0:bulk/(double)roots;}
        @Override public String toString(){return "ints="+ints+" rootCells="+roots+" bulkShare(>=3 root neighbours)="+String.format("%.2f",bulkShare())+" densestLayerShare="+String.format("%.2f",roots==0?0:densest/(double)roots)+" sha256="+sha;}
    }
    private MangroveRootsQualification(){}
}
