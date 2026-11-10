package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;

/** Native procedural/materialized/snapshot/mined-overlay checks over a bounded depth envelope. */
public final class OreNativeSmoke {
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        try{verify(server.overworld());Files.writeString(result(),"PASS bounded native ore/profile/seam/mined-edit checks\n");}
        catch(Throwable error){Blockbranch.LOGGER.error("ORE_NATIVE_FAIL",error);try{Files.writeString(result(),"FAIL "+error);}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static Path result(){return Path.of(System.getProperty("blockbranch.oreNativeResult"));}
    private static void verify(ServerLevel level)throws Exception{
        var g=(RealisticGenerator)level.getChunkSource().getGenerator();require(g.oreDefinition()==Integer.getInteger("blockbranch.oreExpected",2),"Saved preset preserves its expected ore definition");if(Boolean.getBoolean("blockbranch.expectNoDripstone"))require(g.dripstoneDefinition()==0,"Existing saved generator remains opted out of dripstone");var random=level.getChunkSource().randomState();var origin=g.getOrigin(random);var marker=result().resolveSibling("ore-witness.json");boolean reload=Files.exists(marker);BlockPos savedWitness=null;
        if(reload){var xyz=JsonParser.parseString(Files.readString(marker)).getAsJsonArray();var pos=new BlockPos(xyz.get(0).getAsInt(),xyz.get(1).getAsInt(),xyz.get(2).getAsInt());savedWitness=pos;require(level.getBlockState(pos).isAir(),"Mined ore stays air after complete world reopen");require(((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().modifiedView().contains(pos.getY()>>4),"Mined overlay index survives world reopen");}
        var counts=new TreeMap<String,Integer>();var exposed=new TreeMap<String,Integer>();var topKinds=new HashSet<String>();BlockPos witness=null;int checked=0,sections=0;var factory=level.palettedContainerFactory();
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            var chunk=level.getChunk(origin.x()+dx,origin.z()+dz);var p=((SparseColumn)chunk).blockbranch$terrain().base();require(p.ores().enabled(),"Native FULL profile carries ore definition");var encoded=p.encode(factory);require(encoded.getIntOr("version",0)==(p.litCaves().enabled()?66:p.dripstone().enabled()?64:g.oreDefinition()==2?63:62),"Versioned ore profile");var decoded=TerrainProfile.decode(encoded,factory);var undecorated=p.withLitCaves(LitCaveLayer.NONE).withDripstone(DripstoneLayer.NONE);var without=p.withLayers(p.underground(),OreLayer.NONE);var direct=OreLayer.decode(p.ores().encode());if(g.oreDefinition()==2)require(direct.bind(-10000,9000)==direct,"Oversized derived index uses bounded direct fallback");
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=OreLayer.MIN_DEPTH;depth<=OreLayer.MAX_DEPTH;depth++){
                int y=p.groundHeight(x,z)-depth;var state=p.state(x,y,z);if(((x+z+depth)&7)==0)require(direct.state(x,y,z,p.groundHeight(x,z),without.state(x,y,z),p.underground().enabled())==undecorated.state(x,y,z),"Indexed and bounded direct ore queries agree");require(decoded.state(x,y,z)==state,"Snapshot ore state agrees");var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);
                // The persisted witness may be the one actual edit in this regenerated sample.
                var live=level.getBlockState(pos);if(live!=state&&!pos.equals(savedWitness))throw new AssertionError("Unedited native block agrees at "+pos+": live="+live+", base="+state+", witness="+savedWitness);checked++;
                String kind=OreReferenceSmoke.kind(state);if(kind==null)continue;counts.merge(kind,1,Integer::sum);if(topKinds.add(kind))require(p.top(s->s==state,x,z,y)==y+1,"Ore exactly at inclusive query limit remains visible to top query");for(var direction:net.minecraft.core.Direction.values())if(level.getBlockState(pos.relative(direction)).isAir()){exposed.merge(kind,1,Integer::sum);break;}var host=without.state(x,y,z);require(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE),"Ore only replaces surviving host; no ore in cave air/water/surface");if(witness==null&&live==state)witness=pos;
            }
            int sy=(p.groundHeight(8,8)-96)>>4;var generated=p.generate(sy);var view=p.view(sy);boolean ore=false;
            for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){var expected=p.state(x,(sy<<4)+y,z);require(generated.getBlockState(x,y,z)==expected&&view.getBlockState(x,y,z)==expected,"Procedural and materialized states agree");ore|=OreReferenceSmoke.kind(expected)!=null;}
            if(ore)require(view.maybeHas(s->OreReferenceSmoke.kind(s)!=null),"Ore-aware procedural predicate");sections++;
            int lx=8,lz=8,ceiling=p.groundHeight(lx,lz)+1;
            var column=g.getBaseColumn(chunk.getPos().getMinBlockX()+lx,chunk.getPos().getMinBlockZ()+lz,level,random);
            for(int depth=OreLayer.MIN_DEPTH;depth<=OreLayer.MAX_DEPTH;depth++){int y=p.groundHeight(lx,lz)-depth;require(column.getBlock(y)==p.state(lx,y,lz),"Generator base column agrees with saved sparse profile");}for(var type:net.minecraft.world.level.levelgen.Heightmap.Types.values())for(int limit:new int[]{ceiling,ceiling-64,ceiling-224}){
                int expected=limit;while(!type.isOpaque().test(p.state(lx,expected,lz)))expected--;
                int actual=p.top(type.isOpaque(),lx,lz,limit);require(actual==expected+1,"Normal height predicate "+type+" at "+chunk.getPos()+" limit="+limit+" expected="+(expected+1)+" actual="+actual+" ground="+p.groundHeight(lx,lz));
            }
            for(var host:new BlockState[]{Blocks.STONE.defaultBlockState(),Blocks.DEEPSLATE.defaultBlockState()})for(int limit:new int[]{ceiling-64,ceiling-240,ceiling-300}){
                int expected=ColumnLimits.MIN_Y;for(int y=limit;y>=TerrainShape.FLOOR;y--)if(p.state(lx,y,lz)==host){expected=y+1;break;}
                int actual=p.top(v->v==host,lx,lz,limit);require(actual==expected,"Host-rock query respects inclusive ceiling below/within ore support: limit="+limit+" expected="+expected+" actual="+actual);
            }
            for(var kind:OreLayer.Kind.values()){
                int expected=ColumnLimits.MIN_Y;for(int y=ceiling-1;y>=p.groundHeight(lx,lz)-OreLayer.MAX_DEPTH;y--)if(p.state(lx,y,lz)==kind.stone||p.state(lx,y,lz)==kind.deepslate){expected=y+1;break;}
                int[] calls={0};int actual=p.top(s->{calls[0]++;return s==kind.stone||s==kind.deepslate;},lx,lz,ceiling);require(actual==expected,"Ore-specific top query "+kind);require(calls[0]<1500,"Ore top query remains bounded, not full-height scan");
            }
            var bad=encoded.copy();bad.remove("ores");reject(bad,factory);bad=encoded.copy();bad.getCompoundOrEmpty("ores").putInt("definition",3);reject(bad,factory);bad=encoded.copy();bad.putInt("version",61);reject(bad,factory);bad=encoded.copy();bad.putInt("version",g.oreDefinition()==2?62:63);reject(bad,factory);
        }
        // Reuse the same edit witness on every reopen, rather than leaving an
        // unrecorded mined block behind when this fixture runs a third time.
        if(savedWitness!=null)witness=savedWitness;
        require(counts.size()>=6&&witness!=null,"Representative native ore families found");
        var home=level.getChunkAt(witness);var terrain=((SparseColumn)home).blockbranch$terrain();int sy=witness.getY()>>4;var ore=terrain.base().state(witness.getX(),witness.getY(),witness.getZ());
        level.setBlock(witness,Blocks.AIR.defaultBlockState(),2);terrain.flush();terrain.evictExcept(Set.of());require(level.getBlockState(witness).isAir(),"Mining survives eviction");
        var deferred=terrain.survey(sy);var backfill=witness.getY()<0?Blocks.DEEPSLATE.defaultBlockState():Blocks.STONE.defaultBlockState();level.setBlock(witness,backfill,2);require(deferred.getBlockState(witness.getX()&15,witness.getY()&15,witness.getZ()&15)==backfill,"Retained ore survey preserves newer backfill");
        level.setBlock(witness,Blocks.AIR.defaultBlockState(),2);terrain.flush();terrain.evictExcept(Set.of());require(level.getBlockState(witness).isAir(),"Mined ore does not regrow from procedural base");home.markUnsaved();
        Files.writeString(marker,new Gson().toJson(new int[]{witness.getX(),witness.getY(),witness.getZ()}));
        uniformBackfill(level,random.seed());
        level.getServer().saveEverything(false,true,true);
        var report=new LinkedHashMap<String,Object>();report.put("seed",random.seed());report.put("reload",reload);report.put("checkedNativePositions",checked);report.put("materializedSections",sections);report.put("counts",counts);report.put("caveOrSurfaceVisibleCounts",exposed);report.put("witness",witness.toShortString());report.put("originalOre",BuiltInRegistries.BLOCK.getKey(ore.getBlock()).toString());report.put("generatorDefinition",g.oreDefinition());Files.writeString(result().resolveSibling(reload?"ore-native-reopen.json":"ore-native-fresh.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));Blockbranch.LOGGER.info("ORE_NATIVE_PASS {}",report);
    }
    private static void uniformBackfill(ServerLevel level,long seed)throws Exception{
        var factory=level.palettedContainerFactory();var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);int[] ground=new int[256],water=new int[256];Arrays.fill(ground,64);Arrays.fill(water,Integer.MIN_VALUE);var pos=new ChunkPos(-3,2);
        var base=new TerrainProfile(TerrainShape.FLOOR,TerrainShape.CEILING,ground,water,new PalettedContainer<>(biome,factory.biomeStrategy()),factory.blockStatesStrategy()).withLayers(UndergroundLayer.NONE,new OreLayer(seed,pos.x(),pos.z(),2,65535,65535));
        int chosen=Integer.MIN_VALUE;
        for(int sy=-9;sy<=2;sy++){
            boolean inside=false,edge=false;for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(OreReferenceSmoke.kind(base.state(x,(sy<<4)+y,z))!=null){inside=true;edge|=y==0||y==15;}
            if(inside&&!edge){chosen=sy;break;}
        }
        require(chosen!=Integer.MIN_VALUE,"Interior ore section for migration shortcut regression");
        var chunk=new ProtoChunk(pos,UpgradeData.EMPTY,level,factory,null);var sparse=(SparseColumn)chunk;var terrain=new ColumnTerrain(chunk,base,factory,SectionStore.of(level),new int[0]);sparse.blockbranch$terrain(terrain);var live=sparse.blockbranch$write(chosen);var host=chosen<0?Blocks.DEEPSLATE.defaultBlockState():Blocks.STONE.defaultBlockState();for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)live.setBlockState(x,y,z,host);
        require(!ColumnMigration.matches(base,chosen,live),"Uniform host backfill must not be discarded over interior ore");terrain.flush();terrain.evictExcept(Set.of());require(terrain.modifiedView().contains(chosen),"Uniform backfill overlay index retained");
        var saved=SerializableChunkData.copyOf(level,chunk).write();SectionStore.closeAll();var reopened=SerializableChunkData.parse(level,factory,saved).read(level,level.getPoiManager(),new RegionStorageInfo("ore",level.dimension(),"ore"),pos);var section=((SparseColumn)reopened).blockbranch$read(chosen);for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)require(section.getBlockState(x,y,z)==host,"Uniform backfill survives native snapshot/store reopen without regrowth");
    }
    private static void reject(net.minecraft.nbt.CompoundTag tag,PalettedContainerFactory factory){boolean rejected=false;try{TerrainProfile.decode(tag,factory);}catch(RuntimeException expected){rejected=true;}require(rejected,"Malformed or misplaced ore descriptor rejected");}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
