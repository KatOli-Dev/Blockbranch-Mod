package blockbranch.realistic;

import blockbranch.cubic.*;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.storage.LevelResource;

/** Disposable region fixtures exercise native asynchronous loading and native saving. */
public final class ColumnSaveSafetySmoke {
    public static void verify(ServerLevel level,RealisticGenerator generator,boolean reload)throws Exception {
        Path dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();
        markerControls(dir.resolve("marker-controls"));
        verifyStartupMarker(level,generator);
        var map=level.getChunkSource().chunkMap;
        Method schedule=map.getClass().getDeclaredMethod("scheduleChunkLoad",ChunkPos.class);schedule.setAccessible(true);
        Method save=map.getClass().getDeclaredMethod("save",ChunkAccess.class);save.setAccessible(true);
        var rows=new ArrayList<Map<String,Object>>();
        int index=0;
        for(String kind:List.of("future","malformed-array","missing-growth","missing-cave-biomes","malformed-cave-biomes","missing-profile","missing-edits","missing-status","wrong-owner","legacy64","supported")) {
            var pos=new ChunkPos(100000+index++,-100000);
            require(!level.getChunkSource().hasChunk(pos.x(),pos.z()),"Disposable coordinate is not a resident chunk");
            CompoundTag valid;
            if(reload) {
                valid=map.read(pos).join().orElseThrow();
                var recovered=load(level,schedule,pos);
                require(edit(recovered),"Restored saved edit loads after world replacement");
                recovered.markUnsaved();require((boolean)save.invoke(map,recovered),"Quarantine ends only with previous world lifetime");
                map.synchronize(true).join();
            } else {
                var proto=new ProtoChunk(pos,UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
                var profile=generator.profile(proto,level.getChunkSource().randomState());
                if(kind.equals("legacy64")){
                    var bare=profile.withDripstone(DripstoneLayer.NONE).withLitCaves(LitCaveLayer.NONE);
                    profile=bare.withDripstone(DripstoneLayer.plan(bare,level.getChunkSource().randomState().seed(),pos.x(),pos.z(),level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.DRIPSTONE_CAVES),1));
                    require(profile.encode(level.palettedContainerFactory()).getIntOr("version",0)==64,"Actual supported legacy profile64 fixture");
                }
                var sparse=(SparseColumn)proto;
                sparse.blockbranch$terrain(new ColumnTerrain(proto,profile,level.palettedContainerFactory(),SectionStore.of(level),new int[0]));
                sparse.blockbranch$write(0).setBlockState(0,0,0,Blocks.DIAMOND_BLOCK.defaultBlockState());
                proto.setPersistedStatus(ChunkStatus.FULL);
                valid=SerializableChunkData.copyOf(level,proto).write();
                map.write(pos,valid.copy()).join();map.synchronize(true).join();
                require(edit(load(level,schedule,pos)),"Supported original loads with authoritative edit");
            }
            if(kind.equals("legacy64")||kind.equals("supported")) {
                var loaded=load(level,schedule,pos);require(edit(loaded),"Supported profile retains edit");
                loaded.markUnsaved();require((boolean)save.invoke(map,loaded),"Supported native save is not blocked");
                map.synchronize(true).join();
                require(edit(load(level,schedule,pos)),"Native saved supported profile reloads");
                rows.add(Map.of("case",kind,"loadAndSave",true,"editPreserved",true));continue;
            }
            var invalid=valid.copy();var profile=invalid.getCompoundOrEmpty("blockbranch:terrain");
            switch(kind) {
                case "future"->profile.putInt("version",71);
                case "malformed-array"->profile.putIntArray("ground",new int[]{1});
                case "missing-cave-biomes"->profile.remove("cave_biomes");
                case "malformed-cave-biomes"->profile.getCompoundOrEmpty("cave_biomes").putInt("definition",2);
                case "missing-growth"->profile.remove("lit_caves");
                case "missing-profile"->invalid.remove("blockbranch:terrain");
                case "missing-edits"->invalid.remove("blockbranch:edited_sections");
                case "missing-status"->invalid.remove("Status");
                case "wrong-owner"->{
                    for(String layer:List.of("underground","ores","dripstone","lit_caves"))
                        profile.getCompound(layer).ifPresent(t->t.putInt("column_x",pos.x()+1));
                }
            }
            map.write(pos,invalid.copy()).join();map.synchronize(true).join();
            Path region=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(LevelResource.ROOT)).resolve("region/r."+(pos.x()>>5)+"."+(pos.z()>>5)+".mca");
            String before=hash(region);
            boolean rejected=false;
            try {load(level,schedule,pos);}catch(CompletionException expected){rejected=causedByReported(expected);}
            require(rejected,"Native asynchronous load must fail, not produce replacement: "+kind);
            var replacement=new ProtoChunk(pos,UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
            replacement.setPersistedStatus(ChunkStatus.FULL);replacement.markUnsaved();
            require(!(boolean)save.invoke(map,replacement),"Native full replacement save blocked: "+kind);
            require(replacement.isUnsaved(),"Blocked save does not mark replacement saved");
            level.getServer().saveEverything(false,true,true);map.synchronize(true).join();
            require(map.read(pos).join().orElseThrow().equals(invalid),"Original invalid NBT not overwritten: "+kind);
            require(before.equals(hash(region)),"Original region bytes unchanged after rejection/save: "+kind);
            // Explicit QA repair, never an automatic production fallback; restart verifies recovery.
            map.write(pos,valid.copy()).join();map.synchronize(true).join();
            var repaired=load(level,schedule,pos);require(edit(repaired),"Explicitly restored original remains readable");
            repaired.markUnsaved();require(!(boolean)save.invoke(map,repaired),"Save quarantine persists until close/reopen");
            rows.add(Map.of("case",kind,"asynchronousLoadRejected",true,"replacementSaveBlocked",true,
                "originalRegionSha256",before,"originalRegionUnchanged",true,"repairedEditReadable",true));
        }
        Files.writeString(dir.resolve(reload?"column-safety-reload.json":"column-safety-first.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));
        blockbranch.Blockbranch.LOGGER.info("COLUMN_SAVE_SAFETY_PASS cases={} reload={} nativeAsyncLoad=true nativeSave=true",rows.size(),reload);
    }
    @SuppressWarnings("unchecked")
    private static ChunkAccess load(ServerLevel level,Method schedule,ChunkPos pos)throws Exception {
        var future=(CompletableFuture<ChunkAccess>)schedule.invoke(level.getChunkSource().chunkMap,pos);
        level.getServer().managedBlock(future::isDone);
        var result=future.join();
        return result instanceof ImposterProtoChunk imposter?imposter.getWrapped():result;
    }
    private static boolean edit(ChunkAccess access) {
        var owner=access instanceof ImposterProtoChunk imposter?imposter.getWrapped():access;
        return ((SparseColumn)owner).blockbranch$read(0).getBlockState(0,0,0).is(Blocks.DIAMOND_BLOCK);
    }
    private static boolean causedByReported(Throwable failure) {
        for(Throwable t=failure;t!=null;t=t.getCause())if(t instanceof net.minecraft.ReportedException&&t.getMessage().contains("Refusing to replace"))return true;
        return false;
    }
    private static String hash(Path path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
    public static void verifyStartupMarker(ServerLevel level,RealisticGenerator generator)throws Exception {
        String marker=Files.readString(level.getServer().getWorldPath(LevelResource.ROOT).resolve("blockbranch-columns.txt"));
        boolean decoration=generator.litCaveGrowthDefinition()>0||generator.dripstoneDefinition()==2;
        require(marker.equals("blockbranch-columns=4\nblock-position-bits=22,20,22\nmetallurgy-catalog=20\nversioned-decoration="+decoration+"\n"),"Actual startup marker retains saved decoration and current content catalog");
    }
    private static void markerControls(Path root)throws Exception {
        Files.createDirectories(root);
        for(boolean moss:new boolean[]{false,true}) {
            Path directory=Files.createTempDirectory(root,"fresh-");
            ColumnWorldFormat.check(directory,false,moss);
            String first=Files.readString(directory.resolve("blockbranch-columns.txt"));
            require(first.startsWith(moss?"blockbranch-columns=3\n":"blockbranch-columns=2\n"),"Fresh format selected");
            ColumnWorldFormat.check(directory,true,moss);
            require(first.equals(Files.readString(directory.resolve("blockbranch-columns.txt"))),"Reload marker unchanged");
        }
        for(int version:new int[]{1,2})for(boolean moss:new boolean[]{false,true}) {
            Path directory=Files.createTempDirectory(root,"legacy-");
            Files.writeString(directory.resolve("blockbranch-columns.txt"),"blockbranch-columns="+version+"\nblock-position-bits=22,20,22\n");
            ColumnWorldFormat.check(directory,true,moss);
            require(Files.readString(directory.resolve("blockbranch-columns.txt")).startsWith(moss?"blockbranch-columns=3\n":"blockbranch-columns=2\n"),"Only opted-in saves gain marker3");
        }
        for(String contents:List.of("blockbranch-columns=4\nblock-position-bits=22,20,22\n","broken","blockbranch-columns=3\nblock-position-bits=22,20,22\n")) {
            Path directory=Files.createTempDirectory(root,"rejected-");var marker=directory.resolve("blockbranch-columns.txt");Files.writeString(marker,contents);
            boolean rejected=false;try{ColumnWorldFormat.check(directory,true,false);}catch(IllegalStateException expected){rejected=true;}
            require(rejected&&Files.readString(marker).equals(contents),"Unsupported/mismatched marker preserved and rejected");
        }
        var unmarked=Files.createTempDirectory(root,"unmarked-");boolean rejected=false;
        try{ColumnWorldFormat.check(unmarked,true,true);}catch(IllegalStateException expected){rejected=true;}
        require(rejected&&!Files.exists(unmarked.resolve("blockbranch-columns.txt")),"Unmarked existing save not imported");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private ColumnSaveSafetySmoke(){}
}
