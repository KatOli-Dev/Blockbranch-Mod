package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import java.lang.ref.Reference;
import java.nio.file.*;
import java.util.ArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** A render-distance-16 column footprint, mirrored client section storage, then save. */
public final class RealisticMemorySmoke {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var mirrors = new ArrayList<LevelChunkSection[]>();
            try {
                var level = server.overworld();
                if (!(level.getChunkSource().getGenerator() instanceof RealisticGenerator generator)) throw new AssertionError("Realistic generator required");
                if(Boolean.getBoolean("blockbranch.meadowForbFreshOnly")||Boolean.getBoolean("blockbranch.meadowForbSavedOnly")||Boolean.getBoolean("blockbranch.meadowForbPrepareSavedOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    if(Boolean.getBoolean("blockbranch.meadowForbSavedOnly"))MeadowForbSavedSmoke.verify(level);
                    else if(Boolean.getBoolean("blockbranch.meadowForbPrepareSavedOnly"))MeadowForbSavedSmoke.prepare(level,generator);
                    else MeadowSmoke.verify(level,generator,MeadowSmoke.fixture(generator,level.getChunkSource().randomState()),Files.exists(result.resolveSibling("saved.txt")));
                    server.saveEverything(false,true,true);Files.writeString(result.resolveSibling("saved.txt"),"saved\n");Files.writeString(result,"PASS\n");Blockbranch.LOGGER.info("MEADOW_FORB_FOCUSED_PASS savedOnly={} prepareOnly={}",Boolean.getBoolean("blockbranch.meadowForbSavedOnly"),Boolean.getBoolean("blockbranch.meadowForbPrepareSavedOnly"));return;
                }
                if(Boolean.getBoolean("blockbranch.tundraFloorFreshOnly")||Boolean.getBoolean("blockbranch.tundraFloorSavedOnly")||Boolean.getBoolean("blockbranch.tundraFloorPrepareSavedOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    if(Boolean.getBoolean("blockbranch.tundraFloorSavedOnly"))TundraFloorSavedSmoke.verify(level);
                    else if(Boolean.getBoolean("blockbranch.tundraFloorPrepareSavedOnly"))TundraFloorSavedSmoke.prepare(level,generator);
                    else TundraSmoke.verify(level,generator,TundraSmoke.fixture(generator,level.getChunkSource().randomState()),Files.exists(result.resolveSibling("saved.txt")));
                    server.saveEverything(false,true,true);Files.writeString(result.resolveSibling("saved.txt"),"saved\n");Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("TUNDRA_FLOOR_FOCUSED_PASS savedOnly={} prepareOnly={}",Boolean.getBoolean("blockbranch.tundraFloorSavedOnly"),Boolean.getBoolean("blockbranch.tundraFloorPrepareSavedOnly"));return;
                }
                if(Boolean.getBoolean("blockbranch.gravelDesertFreshOnly")||Boolean.getBoolean("blockbranch.gravelDesertSavedOnly")||Boolean.getBoolean("blockbranch.gravelDesertPrepareSavedOnly")){
                    GravelReliefCodecSmoke.verify(level);
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    if(Boolean.getBoolean("blockbranch.gravelDesertSavedOnly"))GravelDesertSavedSmoke.verify(level);
                    else if(Boolean.getBoolean("blockbranch.gravelDesertPrepareSavedOnly"))GravelDesertSavedSmoke.prepare(level,generator);
                    else DesertSmoke.verify(level,generator,DesertSmoke.fixture(generator,level.getChunkSource().randomState()),Files.exists(result.resolveSibling("saved.txt")));
                    server.saveEverything(false,true,true);Files.writeString(result.resolveSibling("saved.txt"),"saved\n");Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("GRAVEL_DESERT_FOCUSED_PASS savedOnly={} prepareOnly={}",Boolean.getBoolean("blockbranch.gravelDesertSavedOnly"),Boolean.getBoolean("blockbranch.gravelDesertPrepareSavedOnly"));return;
                }
                if(Boolean.getBoolean("blockbranch.flowerColorSavedOnly")||Boolean.getBoolean("blockbranch.flowerColorPrepareSavedOnly")||Boolean.getBoolean("blockbranch.flowerColorFreshOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    if(Boolean.getBoolean("blockbranch.flowerColorSavedOnly"))FlowerColorSavedSmoke.verify(level);
                    else if(Boolean.getBoolean("blockbranch.flowerColorPrepareSavedOnly"))FlowerColorSavedSmoke.prepare(level,generator);
                    else FlowerColorSavedSmoke.fresh(level,generator);
                    server.saveEverything(false,true,true);Files.writeString(result.resolveSibling("saved.txt"),"saved\n");Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("FLOWER_COLOR_FOCUSED_PASS savedOnly={} prepareOnly={} freshOnly={}",Boolean.getBoolean("blockbranch.flowerColorSavedOnly"),Boolean.getBoolean("blockbranch.flowerColorPrepareSavedOnly"),Boolean.getBoolean("blockbranch.flowerColorFreshOnly"));return;
                }
                if(Boolean.getBoolean("blockbranch.drySteppeCompatibilityOnly")||Boolean.getBoolean("blockbranch.drySteppeSavedOnly")||Boolean.getBoolean("blockbranch.drySteppePrepareSavedOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));boolean reload=Files.exists(result.resolveSibling("saved.txt"));
                    if(Boolean.getBoolean("blockbranch.drySteppeSavedOnly"))DrySteppeSavedSmoke.verify(level);
                    else if(Boolean.getBoolean("blockbranch.drySteppePrepareSavedOnly"))DrySteppeSavedSmoke.prepare(level,generator);
                    else {SteppeSmoke.verify(level,generator,SteppeSmoke.fixture(generator,level.getChunkSource().randomState()),reload);}
                    server.saveEverything(false,true,true);Files.writeString(result.resolveSibling("saved.txt"),"saved\n");Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("DRY_STEPPE_FOCUSED_PASS reload={} savedOnly={}",reload,Boolean.getBoolean("blockbranch.drySteppeSavedOnly"));return;
                }
                if(Boolean.getBoolean("blockbranch.reedCompatibilityOnly")||Boolean.getBoolean("blockbranch.reedSavedOnly")||Boolean.getBoolean("blockbranch.reedPrepareSavedOnly")) {
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    boolean reload=Files.exists(result.resolveSibling("saved.txt"));
                    if(Boolean.getBoolean("blockbranch.reedSavedOnly")) ReedSavedSmoke.verify(level);
                    else if(Boolean.getBoolean("blockbranch.reedPrepareSavedOnly")) ReedSavedSmoke.prepare(level,generator);
                    else {
                        if(reload) ReedSavedSmoke.verify(level);
                        var bed=ReedbedSmoke.fixture(generator,level.getChunkSource().randomState());
                        ReedbedSmoke.verify(level,generator,bed,reload);
                        ReedbedSmoke.verifyShortForms(level,generator,bed);
                        ReedProfileCompatibilitySmoke.verify(level,generator);
                        if(!reload) ReedSavedSmoke.prepare(level,generator);
                    }
                    server.saveEverything(false,true,true);
                    Files.writeString(result.resolveSibling("saved.txt"),"saved\n");Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("REED_FOCUSED_PASS reload={} savedOnly={} prepareOnly={}",reload,Boolean.getBoolean("blockbranch.reedSavedOnly"),Boolean.getBoolean("blockbranch.reedPrepareSavedOnly"));
                    return;
                }
                if(Boolean.getBoolean("blockbranch.desertSavedOnly")||Boolean.getBoolean("blockbranch.desertCompatibilityOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    boolean savedOnly=Boolean.getBoolean("blockbranch.desertSavedOnly");
                    boolean reload=Files.exists(result.resolveSibling("saved.txt"));
                    if(savedOnly)SandyDesertSmoke.verifySaved(level);
                    else {SandyDesertSmoke.verify(level,generator,reload);SandyDesertSmoke.saveGrownWitness(level,generator);}
                    server.saveEverything(false,true,true);
                    Files.writeString(result.resolveSibling("saved.txt"),"saved\n");
                    Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("SANDY_DESERT_FOCUSED_PASS savedOnly={} reload={} ordinaryMemoryChecks=false",savedOnly,reload);
                    return;
                }
                if(Boolean.getBoolean("blockbranch.sunflowerSavedOnly")){
                    verifySavedSunflower(level,generator);
                    server.saveEverything(false,true,true);
                    Files.writeString(Path.of(System.getProperty("blockbranch.memorySmokeResult")),"PASS\n");
                    Blockbranch.LOGGER.info("SUNFLOWER_SAVED_ONLY_PASS ordinaryMemoryChecks=false");
                    return;
                }
                if(Boolean.getBoolean("blockbranch.sunflowerCompatibilityOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    boolean reload=Files.exists(result.resolveSibling("saved.txt"));
                    SunflowerSmoke.verify(level,generator,reload);
                    server.saveEverything(false,true,true);
                    Files.writeString(result.resolveSibling("saved.txt"),"saved\n");
                    Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("SUNFLOWER_COMPATIBILITY_ONLY_PASS {} ordinaryMemoryChecks=false",reload?"RELOAD":"NEW");
                    return;
                }
                if(Boolean.getBoolean("blockbranch.forestGrasslandOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    boolean reload=Files.exists(result.resolveSibling("saved.txt"));
                    ForestGrasslandTransitionSmoke.verify(level,generator,reload);
                    server.saveEverything(false,true,true);
                    Files.writeString(result.resolveSibling("saved.txt"),"saved\n");Files.writeString(result,"PASS\n");return;
                }
                if(Boolean.getBoolean("blockbranch.transitionLegacyOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    TransitionLegacySnapshotSmoke.verify(level,generator);server.saveEverything(false,true,true);
                    Files.writeString(result,"PASS\n");return;
                }
                if(Boolean.getBoolean("blockbranch.borealClearOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    boolean reload=Files.exists(result.resolveSibling("saved.txt"));
                    BorealClearSmoke.verify(level,generator,reload);
                    server.saveEverything(false,true,true);
                    Files.writeString(result.resolveSibling("saved.txt"),"saved\n");
                    Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("BOREAL_CLEAR_ONLY_PASS {} ordinaryMemoryChecks=false",reload?"RELOAD":"NEW");
                    return;
                }
                if(Boolean.getBoolean("blockbranch.borealSurfaceOnly")){
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    boolean reload=Files.exists(result.resolveSibling("saved.txt"));
                    BorealSurfaceSmoke.verify(level,generator,reload);
                    server.saveEverything(false,true,true);
                    Files.writeString(result.resolveSibling("saved.txt"),"saved\n");
                    Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("BOREAL_SURFACE_ONLY_PASS {} ordinaryMemoryChecks=false",reload?"RELOAD":"NEW");
                    return;
                }
                int radius = Integer.getInteger("blockbranch.memorySmokeRadius", 16);
                var steppe=Boolean.getBoolean("blockbranch.frozenRiverMemory")?FrozenRiverSmoke.scene(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.groveMemory")?GroveSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.openSnowMemory")?OpenSnowSmoke.fixture(generator,level.getChunkSource().randomState(),true):Boolean.getBoolean("blockbranch.oceanMemory")?OceanSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.snowyTaigaMemory")?SnowyTaigaSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.taigaMemory")?TaigaSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.mangroveMemory")?MangroveSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.swampMemory")?SwampSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.birchForestMemory")?ForestSmoke.fixture(generator,level.getChunkSource().randomState(),true):Boolean.getBoolean("blockbranch.sunflowerMemory")?SunflowerSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.desertMemory")?SandyDesertSmoke.clientFixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.jungleMemory")?JungleSmoke.fixture(generator,level.getChunkSource().randomState()):Boolean.getBoolean("blockbranch.savannaMemory")?SavannaSmoke.fixture(generator,level.getChunkSource().randomState()):DeepOceanSmoke.fixture(generator,level.getChunkSource().randomState());
                int cx = steppe.getX() >> 4, cz = steppe.getZ() >> 4, cy=steppe.getY()>>4;
                int count = 0, sections = 0, uniform = 0;
                long started = System.nanoTime();
                boolean reload = Files.exists(Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("saved.txt"));
                if(Boolean.getBoolean("blockbranch.savannaCompatibilityOnly")){
                    if(Boolean.getBoolean("blockbranch.savannaLegacyCanopy"))SavannaSmoke.verifyLegacy(level,generator,reload);
                    else SavannaSmoke.verify(level,generator,reload);
                    SavannaSmoke.verifyLegacyBoundary(level);
                    server.saveEverything(false,true,true);
                    Path result=Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                    Files.writeString(result.resolveSibling("saved.txt"),"saved\n");
                    Files.writeString(result,"PASS\n");
                    Blockbranch.LOGGER.info("SAVANNA_COMPATIBILITY_ONLY_PASS {} ordinaryMemoryChecks=false",reload?"RELOAD":"NEW");
                    return;
                }
                if(System.getProperty("blockbranch.savannaLegacyBoundary")!=null){
                    SavannaSmoke.verifyLegacyBoundary(level);
                    server.saveEverything(false,true,true);
                    Files.writeString(Path.of(System.getProperty("blockbranch.memorySmokeResult")),"PASS\n");
                    Blockbranch.LOGGER.info("SAVANNA_LEGACY_BOUNDARY_ONLY_PASS ordinaryMemoryChecks=false");
                    return;
                }
                for (int z = cz - radius; z <= cz + radius; z++) for (int x = cx - radius; x <= cx + radius; x++) {
                    level.setChunkForced(x, z, true);
                    var chunk = level.getChunk(x, z);
                    var sparse=(SparseColumn)chunk;
                    if(sparse.blockbranch$terrain()==null) throw new AssertionError("Procedural column required");
                    if(sparse.blockbranch$sections().size()>2) throw new AssertionError("Unvisited layers allocated");
                    // Retain the visible vertical window as independently mutable render copies.
                    var content = new LevelChunkSection[35];
                    for(int i=0;i<content.length;i++) content[i]=sparse.blockbranch$read(cy-17+i);
                    var mirror = new LevelChunkSection[content.length];
                    for (int i = 0; i < content.length; i++) {
                        if (content[i].getStates().bitsPerEntry() == 0) uniform++;
                        mirror[i] = content[i].copy();
                    }
                    mirrors.add(mirror); sections += content.length;
                    var edit = new BlockPos(x * 16 + 2, TerrainShape.FLOOR + 4, z * 16 + 2);
                    if (reload && !level.getBlockState(edit).is(Blocks.DIAMOND_BLOCK)) throw new AssertionError("Saved edit missing at " + edit);
                    level.setBlock(edit, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
                    int editedY=(TerrainShape.FLOOR+512)>>4;
                    var edited=sparse.blockbranch$write(editedY);
                    for(int ly=0;ly<16;ly++) for(int lz=0;lz<16;lz++) for(int lx=0;lx<16;lx++) {
                        var expected=((lx+3*ly+7*lz)&1)==0 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
                        if(reload && edited.getBlockState(lx,ly,lz)!=expected) throw new AssertionError("Nonuniform saved edit missing");
                        edited.setBlockState(lx,ly,lz,expected);
                    }
                    chunk.markUnsaved(); sparse.blockbranch$terrain().evictOutside(-4,30);
                    if(!sparse.blockbranch$sections().isEmpty()) throw new AssertionError("Off-window edited sections retained");
                    if (++count % 64 == 0) Blockbranch.LOGGER.info("REALISTIC_MEMORY_PROGRESS chunks={} sections={} heapMiB={} elapsedSeconds={}", count, sections, usedMiB(), (System.nanoTime() - started) / 1_000_000_000);
                }
                if (sections != count*35) throw new AssertionError("Render copies were not bounded vertically");
                server.saveEverything(false, true, true);
                // Keep the client mirrors live throughout the actual save, not
                // merely while constructing them. Each contains independent palettes.
                Reference.reachabilityFence(mirrors);
                System.gc();
                long live = usedMiB(), max = Runtime.getRuntime().maxMemory() >> 20;
                if (live > max * 0.8) throw new AssertionError("Insufficient save headroom: " + live + "/" + max + " MiB");
                Path result = Path.of(System.getProperty("blockbranch.memorySmokeResult"));
                Files.writeString(result.resolveSibling("saved.txt"), "saved\n");
                Files.writeString(result, "PASS\n");
                Blockbranch.LOGGER.info("REALISTIC_MEMORY_PASS {} chunks={} mirroredSections={} uniform={} liveMiB={} maxMiB={} elapsedSeconds={}", reload ? "RELOAD" : "NEW", count, sections, uniform, live, max, (System.nanoTime() - started) / 1_000_000_000);
                Reference.reachabilityFence(mirrors);
            } catch (Throwable failure) { Blockbranch.LOGGER.error("REALISTIC_MEMORY_FAIL", failure); }
            finally { server.halt(false); }
        });
    }
    /** Read saved terrain and live edits directly; fresh generation may choose a different plant here. */
    private static void verifySavedSunflower(net.minecraft.server.level.ServerLevel level,RealisticGenerator generator){
        String[] xyz=System.getProperty("blockbranch.sunflowerSavedPoint","").split(",");
        if(xyz.length!=3)throw new IllegalArgumentException("sunflowerSavedPoint requires the original x,y,z");
        var bed=new BlockPos(Integer.parseInt(xyz[0].trim()),Integer.parseInt(xyz[1].trim()),Integer.parseInt(xyz[2].trim()));
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();
        if(terrain==null)throw new AssertionError("Original sunflower profile must be loaded from disk");
        var base=terrain.base();var copy=blockbranch.cubic.TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
        if(!base.state(bed.getX(),bed.getY(),bed.getZ()).is(Blocks.GRASS_BLOCK))throw new AssertionError("Stored sunflower soil retained");
        for(int stem=1;stem<=2;stem++){
            var at=bed.above(stem);var stored=base.state(at.getX(),at.getY(),at.getZ());
            var half=stem==1?net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER:net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
            if(!stored.is(Blocks.SUNFLOWER)||stored.getValue(net.minecraft.world.level.block.DoublePlantBlock.HALF)!=half)throw new AssertionError("Original saved sunflower half retained");
            if(copy.state(at.getX(),at.getY(),at.getZ())!=stored||copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15)!=stored)throw new AssertionError("Saved sunflower decode/materialization retains each half");
        }
        if(!level.getBlockState(bed.below(8)).is(Blocks.GOLD_BLOCK)||!level.getBlockState(bed.above()).isAir()||!level.getBlockState(bed.above(2)).isAir())throw new AssertionError("Original gold marker and harvested pair retained");
        terrain.flush();terrain.evictOutside(-100,-90);
        if(!level.getBlockState(bed.below(8)).is(Blocks.GOLD_BLOCK)||!level.getBlockState(bed.above()).isAir()||!level.getBlockState(bed.above(2)).isAir())throw new AssertionError("Original edits survive eviction under the new generator");
        var fresh=generator.surfaceState(bed.getX(),bed.getY()+1,bed.getZ(),level.getChunkSource().randomState());
        Blockbranch.LOGGER.info("SUNFLOWER_SAVED_NATIVE_PASS bed={} originalProfile=SUNFLOWER livePair=AIR goldRetained=true freshLower={} freshDifferent={}",bed,fresh,fresh!=base.state(bed.getX(),bed.getY()+1,bed.getZ()));
    }

    private static long usedMiB() { var r = Runtime.getRuntime(); return (r.totalMemory() - r.freeMemory()) >> 20; }
    private RealisticMemorySmoke() {}
}
