package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Observable-output collector, also compiled against the complete original schema-263 public
 * production tree in a separate JVM. No copied selection or hydrology algorithms. */
public final class Published263Snapshot {
    public static final int[][] POINTS={{1904000,-752000},{272000,-720000},{144000,-688000},{-16000,-656000},{-1648000,-624000},{80000,-624000},{1039616,1296768},{1039616,1297024},{592640,1392256},{-1744000,-1904000},{-528000,-1904000},{304000,-1904000},{-1328000,-1904000},{-1552000,-1840000},{-1328000,-1840000},
        {-1008000,-1776000},{1424000,-496000},{400000,-944000},{-1616000,-1904000},{1904000,-1552000},{1648000,-1744000},{1648000,-208000},{1744000,-176000},{1776000,-176000},{912000,1392000},{656000,1264000},{400000,-1200000},{-1904000,-1904000},{-1872000,-1904000},{-1840000,-1904000}};
    public static JsonObject capture(ServerLevel level,RealisticGenerator generator,boolean nativeWorld)throws Exception {
        var random=level.getChunkSource().randomState();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().getAsJsonObject();
        if(encoded.get("shape_version").getAsInt()!=263)throw new AssertionError("Original complete schema263 settings required");
        var palette=HabitatPalette.CODEC.codec().parse(ops,encoded).getOrThrow();
        var report=new JsonObject();var grids=new JsonArray();
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true);var c=new ClimateField(seed);
            var m=palette.margins();var sentinel=Holder.direct(m.fallback().value());
            var tagged=new HabitatPalette(new HabitatPalette.Margins(sentinel,m.scree(),m.reedbed(),m.riverMargin(),m.sandyShore(),m.lake(),m.riverChannel(),m.shelf(),m.deepOcean()),palette.land(),palette.additional());
            var source=new ScreeBiomeSource(tagged,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true).withLandIce(true);source.bind(seed,d,c);
            var terrain=digest();var biomes=digest();int samples=0,fallback=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var s=d.sample(x,z);var q=c.sample(x,z,d::sample);
                add(terrain,x+","+z+":"+s+":"+d.channelAt(x,z)+":"+d.oceanWater(x,z,s)+":"+d.terminalLakeWater(x,z,s)+":"+Double.toHexString(q.meanTemperatureC())+":"+Double.toHexString(q.moisture()));
                var biome=source.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);boolean isFallback=biome==sentinel;
                add(biomes,x+","+z+":"+(isFallback?"fallback":biome.unwrapKey().orElseThrow().identifier().toString()));
                if(isFallback)fallback++;samples++;
            }
            var entry=new JsonObject();entry.addProperty("seed",seed);entry.addProperty("samples",samples);entry.addProperty("fallback",fallback);
            entry.addProperty("terrainClimateOwnershipSHA256",hex(terrain));entry.addProperty("selectorSHA256",hex(biomes));grids.add(entry);
        }
        report.add("grids",grids);
        var physical=digest();var nativeStates=digest();var nativeProfiles=digest();var drainage=generator.drainage(random);int physicalCount=0,nativeCount=0;
        for(var p:POINTS){
            for(int dz=-8;dz<=8;dz+=4)for(int dx=-8;dx<=8;dx+=4){
                int x=p[0]+dx,z=p[1]+dz;var s=drainage.sample(x,z);
                for(int y=s.height()-4;y<=Math.max(s.height()+40,s.waterLevel()+1);y++){
                    add(physical,x+","+y+","+z+":"+generator.surfaceState(x,y,z,random));physicalCount++;
                }
            }
            if(nativeWorld){
                var nativeChunk=level.getChunk(p[0]>>4,p[1]>>4);
                var profile=((blockbranch.cubic.SparseColumn)nativeChunk).blockbranch$terrain().base();
                add(nativeProfiles,p[0]+","+p[1]+":"+profile.encode(level.palettedContainerFactory()));
                for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                    int x=(p[0]&~15)+dx,z=(p[1]&~15)+dz;var s=drainage.sample(x,z);
                    for(int y=s.height()-3;y<=Math.max(s.height()+40,s.waterLevel());y++){
                        // The actual old build writes this edit after the snapshot. Keep
                        // its retention assertion separate from natural-state hashes.
                        if(x==POINTS[0][0]+1&&z==POINTS[0][1]+1&&y==s.height()-2)continue;
                        add(nativeStates,x+","+y+","+z+":"+level.getBlockState(new BlockPos(x,y,z)));nativeCount++;
                    }
                }
            }
        }
        report.addProperty("physicalStates",physicalCount);report.addProperty("physicalSHA256",hex(physical));
        if(nativeWorld){report.addProperty("nativeStates",nativeCount);report.addProperty("nativeSHA256",hex(nativeStates));report.addProperty("nativeProfileSHA256",hex(nativeProfiles));}
        return report;
    }
    /** Recording is explicit and refuses any newer schema. Provenance must also
     * verify the entire production source tree before using a recording as an oracle. */
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.published263SnapshotResult"));
        try{
            var level=server.overworld();var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
            var snapshot=capture(level,generator,true);
            int x=POINTS[0][0]+1,z=POINTS[0][1]+1;
            level.setBlock(new BlockPos(x,generator.drainage(level.getChunkSource().randomState()).sample(x,z).height()-2,z),Blocks.GOLD_BLOCK.defaultBlockState(),3);
            server.saveEverything(false,true,true);
            Files.writeString(result.resolveSibling("baseline.json"),new GsonBuilder().setPrettyPrinting().create().toJson(snapshot)+"\n");
            Files.writeString(result,"PASS original production snapshot and saved edit\n");
        }catch(Throwable failure){Blockbranch.LOGGER.error("PUBLISHED_263_SNAPSHOT_FAIL",failure);try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static MessageDigest digest()throws NoSuchAlgorithmException{return MessageDigest.getInstance("SHA-256");}
    private static void add(MessageDigest digest,String text){digest.update((text+"\n").getBytes(StandardCharsets.UTF_8));}
    private static String hex(MessageDigest digest){return HexFormat.of().formatHex(digest.digest());}
    private Published263Snapshot(){}
}
