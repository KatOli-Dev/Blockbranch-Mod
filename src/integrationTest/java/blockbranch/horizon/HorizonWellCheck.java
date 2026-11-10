package blockbranch.horizon;

import static blockbranch.horizon.HorizonTiles.*;
import blockbranch.Blockbranch;
import blockbranch.realistic.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Atlas wells on the horizon against native generation. For each {@link HorizonWellSites} well
 * (the nearest, and the nearest whose footprint crosses a chunk border), the horizon samples the level-0 tiles of
 * every chunk the well can reach while the world has never generated them (the detached path).
 * The server then generates those chunks natively, and the same tiles are sampled from the
 * generated chunks. Within the well's planning square every column must match in top, ground,
 * their blocks and water, and native generation must show the well (columns within its span that
 * differ from the bare analytic terrain), in more than one chunk for the border well. Writes
 * {@code result.txt} and halts.
 */
public final class HorizonWellCheck {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.horizonWellDirectory"));
    private static final int[] COMPARED={TOP,TOP_STATE,GROUND,GROUND_STATE,WATER};

    public static void register() {
        if(net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()!=net.fabricmc.api.EnvType.SERVER)return;
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{
            var worker=new Thread(()->run(server),"HorizonWellCheck");worker.setDaemon(true);worker.start();
        });
    }

    private static void run(MinecraftServer server) {
        var report=new StringBuilder();
        try {
            var level=server.overworld();var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
            int span=HorizonWellSites.span(generator);
            var spawn=level.getRespawnData().pos();
            var sites=HorizonWellSites.find(level);
            line(report,"wells %s, spawn %s",span==2?"vanilla feature":"mod cistern",spawn.toShortString());
            if(sites.size()<2)line(report,"the nearest well already crosses a chunk border");

            // Detached: the horizon reads these chunks before the world has them.
            var biomes=level.registryAccess().lookupOrThrow(Registries.BIOME);
            var before=new HashMap<ChunkPos,int[]>();
            var sources=new HorizonSources(level);var none=new Long2ObjectOpenHashMap<LevelChunk>();
            for(var s:sites)for(var c:s.chunks()) {
                if(before.containsKey(c))continue;
                boolean present=server.submit(()->level.getChunkSource().getChunkNow(c.x(),c.z())!=null).join();
                var source=sources.column(c.x(),c.z(),none);
                String kind=source==null?"analytic":source.getClass().getSimpleName();
                if(present||kind.equals("Saved"))throw new AssertionError("Chunk "+c+" existed before the detached read ("+kind+")");
                before.put(c,HorizonService.sample(generator,random,key(0,c.x(),c.z()),h->biomes.getId(h.value()),sources,none));
                line(report,"%s chunk %d %d: detached source %s",s.name(),c.x(),c.z(),kind);
            }
            boolean pass=true;
            for(var s:sites) {
                var loaded=new Long2ObjectOpenHashMap<LevelChunk>();
                for(var c:s.chunks())loaded.put(c.pack(),server.submit(()->level.getChunk(c.x(),c.z())).join());
                var p=s.plan();int mismatched=0,columns=0,wellColumns=0;var wellChunks=new HashSet<ChunkPos>();
                line(report,"%s well at %d %d %d (%.0f blocks from spawn), chunks %s",s.name(),p.x(),p.y(),p.z(),Math.hypot(p.x()-spawn.getX(),p.z()-spawn.getZ()),s.chunks());
                for(var c:s.chunks()) {
                    int[] a=before.get(c),b=HorizonService.sample(generator,random,key(0,c.x(),c.z()),h->biomes.getId(h.value()),new HorizonSources(level),loaded);
                    int[] terrain=HorizonService.sample(generator,random,key(0,c.x(),c.z()),h->biomes.getId(h.value()));
                    for(int j=1;j<=CELLS;j++)for(int i=1;i<=CELLS;i++) {
                        int x=c.getMinBlockX()+i-1,z=c.getMinBlockZ()+j-1;
                        if(Math.abs(x-p.x())>HorizonWellSites.REACH||Math.abs(z-p.z())>HorizonWellSites.REACH)continue;
                        int o=(j*SAMPLES+i)*FIELDS;columns++;
                        // A well column: native generation shows something other than the bare terrain there.
                        boolean built=false,differs=false;for(int f:COMPARED){built|=terrain[o+f]!=b[o+f];differs|=a[o+f]!=b[o+f];}
                        if(built&&Math.abs(x-p.x())<=span&&Math.abs(z-p.z())<=span){wellColumns++;wellChunks.add(c);}
                        if(differs&&mismatched++<40)line(report,"  differs at %d %d: horizon %s native %s",x,z,describe(a,o),describe(b,o));
                    }
                }
                boolean ok=mismatched==0&&wellColumns>0&&(!s.name().equals("border")||wellChunks.size()>1);
                pass&=ok;
                line(report,"%s: %s, %d of %d columns differ, native well columns %d in %d chunks",s.name(),ok?"match":"MISMATCH",mismatched,columns,wellColumns,wellChunks.size());
            }
            Files.writeString(DIR.resolve("report.txt"),report);
            Files.writeString(DIR.resolve("result.txt"),(pass?"PASS":"FAIL")+" horizon wells against native generation\n"+report);
        } catch(Throwable t) {
            Blockbranch.LOGGER.error("HORIZON_WELL_FAIL",t);
            try{Files.writeString(DIR.resolve("result.txt"),"FAIL "+t+"\n"+report);}catch(Exception ignored){}
        } finally {
            server.execute(()->server.halt(false));
        }
    }
    private static String describe(int[] d,int o) {
        return "top="+d[o+TOP]+" "+name(d[o+TOP_STATE])+" ground="+d[o+GROUND]+" "+name(d[o+GROUND_STATE])+" water="+(d[o+WATER]==NONE?"-":d[o+WATER]);
    }
    private static String name(int state){return BuiltInRegistries.BLOCK.getKey(Block.stateById(state).getBlock()).getPath();}
    private static void line(StringBuilder report,String format,Object... args) {
        var s=String.format(Locale.ROOT,format,args);report.append(s).append('\n');Blockbranch.LOGGER.info("HORIZON_WELL {}",s);
    }
    private HorizonWellCheck(){}
}
