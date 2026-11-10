package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.cubic.ColumnLimits;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Opt-in checks against real registries, generation stages, lighting and disk reload. */
public final class RealisticSmoke {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                for (int value : new int[]{0, 7, 15}) {
                    byte[] bytes = new byte[DataLayer.SIZE];
                    java.util.Arrays.fill(bytes, (byte)(value | value << 4));
                    var light = new DataLayer(bytes);
                    require(light.isDefinitelyFilledWith(value), "Compact decoded light " + value);
                    var copy = light.copy();
                    copy.set(0, 0, 0, (value + 1) & 15);
                    require(light.get(0, 0, 0) == value && copy.get(1, 0, 0) == value, "Independent light mutation");
                    var mixed = new DataLayer(copy.getData().clone());
                    require(mixed.get(0, 0, 0) == ((value + 1) & 15) && mixed.get(1, 0, 0) == value, "Mixed light round trip");
                }
                byte[] alternating = new byte[DataLayer.SIZE];
                java.util.Arrays.fill(alternating, (byte)0x0f);
                var stripedLight = new DataLayer(alternating);
                require(stripedLight.get(0, 0, 0) == 15 && stripedLight.get(1, 0, 0) == 0, "Distinct light nibbles preserved");
                var level = server.overworld();
                require(level.getChunkSource().getGenerator() instanceof RealisticGenerator, "Realistic preset selected");
                var generator = (RealisticGenerator)level.getChunkSource().getGenerator();
                var random = level.getChunkSource().randomState();
                var shape = new TerrainShape(random.seed());
                var drainage = generator.drainage(random);
                TerrainContourSmoke.verify(level,generator);
                Path result = Path.of(System.getProperty("blockbranch.realisticSmokeResult"));
                Path saved = result.resolveSibling("fingerprint.txt");
                boolean legacy = Files.exists(saved) && !Files.exists(result.resolveSibling("lazy-storage.txt"));
                boolean reload = Files.exists(saved) && !legacy;
                var origin = generator.getOrigin(random);
                require(drainage.sample(origin.getMinBlockX(), origin.getMinBlockZ()).waterLevel() == DrainageNetwork.DRY, "Dry generator origin");
                ChunkPos mountain = null, ocean = null;
                for (int z = -1900000; z <= 1900000 && (mountain == null || ocean == null); z += 32000)
                    for (int x = -1900000; x <= 1900000; x += 32000) {
                        int h = shape.height(x, z);
                        if (mountain == null && h > TerrainShape.SEA_LEVEL + 3000) mountain = new ChunkPos(x >> 4, z >> 4);
                        if (ocean == null && h < TerrainShape.SEA_LEVEL - 4500) ocean = new ChunkPos(x >> 4, z >> 4);
                    }
                require(mountain != null && ocean != null, "High and deep fixtures found");
                int terminal = -1, river = -1;
                for (int i = 0; i < drainage.size(); i++) {
                    // Leave room for vanilla's generation-ticket neighborhood.
                    if (Math.abs(drainage.nodeX(i)) > 1800000 || Math.abs(drainage.nodeZ(i)) > 1800000) continue;
                    if (terminal < 0 && drainage.closed(i) && drainage.parent(i) < 0) terminal = i;
                    if (river < 0 && drainage.river(i) && !drainage.closed(i)) river = i;
                    if (terminal >= 0 && river >= 0) break;
                }
                require(terminal >= 0 && river >= 0, "Lake and ocean-river fixtures");
                var lakePos = new BlockPos(drainage.nodeX(terminal), drainage.waterLevel(terminal) - 1, drainage.nodeZ(terminal));
                int riverX = (int)Math.round(drainage.edgeX(river, 0.5)), riverZ = (int)Math.round(drainage.edgeZ(river, 0.5));
                var riverSample = drainage.sample(riverX, riverZ);
                var riverPos = new BlockPos(riverX, riverSample.waterLevel() - 1, riverZ);
                var lakeChunk = new ChunkPos(lakePos.getX() >> 4, lakePos.getZ() >> 4);
                var riverChunk = new ChunkPos(riverPos.getX() >> 4, riverPos.getZ() >> 4);
                require(drainage.sample(lakePos.getX(), lakePos.getZ()).closed(), "Closed terminal");
                require(lakePos.getY() != TerrainShape.SEA_LEVEL - 1, "Local lake elevation");
                // Includes a river crossing a native chunk boundary and a lake
                // centered independently of Minecraft's chunk grid.
                var screePos = AlpineScreeSmoke.fixture(generator, random);
                var screeChunk = new ChunkPos(screePos.getX() >> 4, screePos.getZ() >> 4);
                var reedPos = ReedbedSmoke.fixture(generator,random);
                var reedChunk = new ChunkPos(reedPos.getX()>>4,reedPos.getZ()>>4);
                var marginPos=RiverMarginSmoke.fixture(generator,random);
                var marginChunk=new ChunkPos(marginPos.getX()>>4,marginPos.getZ()>>4);
                var shorePos=SandyShoreSmoke.fixture(generator,random);
                var shoreChunk=new ChunkPos(shorePos.getX()>>4,shorePos.getZ()>>4);
                var meadowPos=MeadowSmoke.fixture(generator,random);
                var meadowChunk=new ChunkPos(meadowPos.getX()>>4,meadowPos.getZ()>>4);
                var woodlandPos=WoodlandSmoke.fixture(generator,random);
                var woodlandChunk=new ChunkPos(woodlandPos.getX()>>4,woodlandPos.getZ()>>4);
                var steppePos=SteppeSmoke.fixture(generator,random);
                var steppeChunk=new ChunkPos(steppePos.getX()>>4,steppePos.getZ()>>4);
                var borealPos=BorealSmoke.fixture(generator,random);
                var borealChunk=new ChunkPos(borealPos.getX()>>4,borealPos.getZ()>>4);
                var warmPos=WarmSmoke.fixture(generator,random);
                var warmChunk=new ChunkPos(warmPos.getX()>>4,warmPos.getZ()>>4);
                var coldSteppePos=ColdSteppeSmoke.fixture(generator,random);
                var coldSteppeChunk=new ChunkPos(coldSteppePos.getX()>>4,coldSteppePos.getZ()>>4);
                var desertPos=DesertSmoke.fixture(generator,random);var desertChunk=new ChunkPos(desertPos.getX()>>4,desertPos.getZ()>>4);
                var deepPos=DeepOceanSmoke.fixture(generator,random);var deepChunk=new ChunkPos(deepPos.getX()>>4,deepPos.getZ()>>4);
                var shelfPos=MarineShelfSmoke.fixture(generator,random);var shelfChunk=new ChunkPos(shelfPos.getX()>>4,shelfPos.getZ()>>4);
                var routedPos=RiverChannelSmoke.fixture(generator,random);var routedChunk=new ChunkPos(routedPos.getX()>>4,routedPos.getZ()>>4);
                var lakeBedPos=LakeSmoke.fixture(generator,random);var lakeBedChunk=new ChunkPos(lakeBedPos.getX()>>4,lakeBedPos.getZ()>>4);
                var tundraPos=TundraSmoke.fixture(generator,random);var tundraChunk=new ChunkPos(tundraPos.getX()>>4,tundraPos.getZ()>>4);
                var humidPos=HumidSmoke.fixture(generator,random);var humidChunk=new ChunkPos(humidPos.getX()>>4,humidPos.getZ()>>4);
                var slopeMeadow=SlopeCorrectionSmoke.fixture(SlopeCorrection.Kind.MEADOW,generator,random);var slopeMeadowChunk=new ChunkPos(slopeMeadow.getX()>>4,slopeMeadow.getZ()>>4);
                var addedBoreal=SlopeCorrectionSmoke.fixture(SlopeCorrection.Kind.BOREAL,generator,random);var addedBorealChunk=new ChunkPos(addedBoreal.getX()>>4,addedBoreal.getZ()>>4);
                var addedSteppe=SlopeCorrectionSmoke.fixture(SlopeCorrection.Kind.STEPPE,generator,random);var addedSteppeChunk=new ChunkPos(addedSteppe.getX()>>4,addedSteppe.getZ()>>4);
                var fixtures = new ChunkPos[]{deepChunk,shelfChunk,routedChunk,lakeBedChunk,tundraChunk,humidChunk,slopeMeadowChunk,addedBorealChunk,addedSteppeChunk,desertChunk,mountain, ocean, lakeChunk, riverChunk, screeChunk,reedChunk,marginChunk,shoreChunk,meadowChunk,woodlandChunk,steppeChunk,borealChunk,warmChunk,coldSteppeChunk,
                    new ChunkPos(riverChunk.x() + 1, riverChunk.z()), new ChunkPos(-1, 0), new ChunkPos(0, 0)};
                for (var pos : fixtures) {
                    var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
                    var biomes = generator.createBiomes(random, Blender.empty(), level.structureManager(), chunk);
                    server.managedBlock(biomes::isDone); biomes.join();
                    chunk.setPersistedStatus(ChunkStatus.BIOMES);
                    generator.buildTerrain(chunk, Blender.empty(), random, level.structureManager(), level.getBiomeManager(), null, generator.getBiomeSource().possibleBiomes()).join();
                    verify(level, generator, chunk);
                }
                long fingerprint = 1;
                for (var pos : new ChunkPos[]{deepChunk,shelfChunk,routedChunk,lakeBedChunk,tundraChunk,humidChunk,mountain, ocean, lakeChunk, riverChunk, screeChunk,reedChunk,marginChunk,shoreChunk,meadowChunk,woodlandChunk,steppeChunk,borealChunk,warmChunk,coldSteppeChunk,desertChunk,slopeMeadowChunk,addedBorealChunk,addedSteppeChunk}) {
                    // Request FULL chunks through the actual server pipeline.
                    var chunk = level.getChunk(pos.x(), pos.z());
                    var witness = new BlockPos(pos.getMinBlockX(), TerrainShape.FLOOR + 4, pos.getMinBlockZ());
                    if (reload || legacy) require(level.getBlockState(witness).is(Blocks.DIAMOND_BLOCK), "Deep reload or migration witness");
                    // Also recover a witness saved by an interrupted first run.
                    if (level.getBlockState(witness).is(Blocks.DIAMOND_BLOCK))
                        level.setBlock(witness, RealisticGenerator.material(witness.getY(), drainage.sample(witness.getX(), witness.getZ()).height(), drainage.sample(witness.getX(), witness.getZ()).waterLevel()), 2);
                    verify(level, generator, chunk);
                    var fence = level.getChunkSource().getLightEngine().waitForPendingTasks(pos.x(), pos.z());
                    server.managedBlock(fence::isDone); fence.join();
                    int x = pos.getMinBlockX(), z = pos.getMinBlockZ();
                    var columnSample = drainage.sample(x, z);
                    int h = columnSample.height();
                    int skyY=Math.max(Math.max(h+1,columnSample.waterLevel()),((SparseColumn)chunk).blockbranch$terrain().base().canopy().topY(x,z)==Integer.MIN_VALUE ? h+1 : ((SparseColumn)chunk).blockbranch$terrain().base().canopy().topY(x,z)+1);
                    require(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,new BlockPos(x,skyY,z))==15,"Skylight above surface/canopy");
                    var serialized = SerializableChunkData.copyOf(level, chunk).write();
                    require(serialized.getListOrEmpty("sections").isEmpty() && serialized.contains("blockbranch:terrain"), "Untouched vertical layers omitted from disk");
                    require(SerializableChunkData.parse(level, level.palettedContainerFactory(), serialized) != null, "Chunk serialization accepted");
                    fingerprint = 31 * fingerprint + h;
                    level.setBlock(witness, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
                    // Exercise the new habitat and its maximum light-depth witness before the longer prior-habitat tour.
                    if(pos.equals(deepChunk))DeepOceanSmoke.verify(level,generator,deepPos,reload);
                    if(pos.equals(shelfChunk))MarineShelfSmoke.verify(level,generator,shelfPos,reload);
                }
                AlpineScreeSmoke.verify(level, generator, screePos, reload);
                ReedbedSmoke.verify(level,generator,reedPos,reload);
                RiverMarginSmoke.verify(level,generator,marginPos,reload);
                SandyShoreSmoke.verify(level,generator,shorePos,reload);
                MeadowSmoke.verify(level,generator,meadowPos,reload);
                WoodlandSmoke.verify(level,generator,woodlandPos,reload);
                SteppeSmoke.verify(level,generator,steppePos,reload);
                BorealSmoke.verify(level,generator,borealPos,reload);
                WarmSmoke.verify(level,generator,warmPos,reload);
                ColdSteppeSmoke.verify(level,generator,coldSteppePos,reload);
                DesertSmoke.verify(level,generator,desertPos,reload);
                SwampSmoke.verify(level,generator,SwampSmoke.fixture(generator,random),reload);
                MangroveSmoke.verify(level,generator,reload);
                TaigaSmoke.verify(level,generator,reload);
                SnowyTaigaSmoke.verify(level,generator,reload);
                GroveSmoke.verify(level,generator,reload);
                MontaneMeadowSmoke.verify(level,generator,reload);
                OpenSnowSmoke.verify(level,generator,reload);
                OpenSnowSmoke.verify(level,generator,reload,true);
                OceanSmoke.verify(level,generator,reload);
                DeepOceanSmoke.verify(level,generator,DeepOceanSmoke.fixture(generator,random,true),reload,true);
                ForestSmoke.verify(level,generator,ForestSmoke.fixture(generator,random,true),reload,true);
                ForestSmoke.verify(level,generator,ForestSmoke.fixture(generator,random),reload);
                SunflowerSmoke.verify(level,generator,reload);
                PlainsSmoke.verify(level,generator,reload);
                SandyDesertSmoke.verify(level,generator,reload);
                LakeSmoke.verify(level,generator,lakeBedPos,reload);
                RiverChannelSmoke.verify(level,generator,routedPos,reload);
                RiverSmoke.verify(level,generator,reload);
                TundraSmoke.verify(level,generator,tundraPos,reload);
                HumidSmoke.verify(level,generator,humidPos,reload);
                SavannaSmoke.verify(level,generator,reload);
                JungleSmoke.verify(level,generator,reload);
                BoundaryCorrectionSmoke.verify(level,generator);
                SlopeCorrectionSmoke.verify(level,generator,reload);
                verifyLazyEdits(level, level.getChunk(lakeChunk.x(),lakeChunk.z()), reload);
                require(level.getBlockState(lakePos).is(Blocks.WATER), "Terminal lake water");
                require(level.getBlockState(lakePos.above()).isAir(), "Lake surface at local elevation");
                require(level.getBlockState(riverPos).is(Blocks.WATER), "River water");
                require(level.getBlockState(riverPos.above()).isAir(), "River surface");
                if (reload) require(Files.readString(saved).trim().equals(Long.toString(fingerprint)), "Reload fingerprint");
                server.saveEverything(false, true, true);
                Files.writeString(saved, Long.toString(fingerprint) + "\n");
                Files.writeString(result.resolveSibling("lazy-storage.txt"), "1\n");
                Files.writeString(result, "PASS\n");
                Blockbranch.LOGGER.info("REALISTIC_SMOKE_PASS {} mountain={} ocean={} lake={} river={}", reload ? "RELOAD" : "NEW", mountain, ocean, lakePos, riverPos);
            } catch (Throwable failure) {
                Blockbranch.LOGGER.error("REALISTIC_SMOKE_FAIL", failure);
            } finally { server.halt(false); }
        });
    }

    private static void verify(ServerLevel level, RealisticGenerator generator, ChunkAccess chunk) {
        var random = level.getChunkSource().randomState();
        var drainage = generator.drainage(random);
        var cursor = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int wx = chunk.getPos().getMinBlockX() + x, wz = chunk.getPos().getMinBlockZ() + z;
            var sample = drainage.sample(wx, wz);
            int h = sample.height();
            int scree = generator.screeDepth(wx, wz, random);
            int reedbed=generator.reedbedCover(wx,wz,random);
            int margin=generator.riverMarginCover(wx,wz,random);
            int shore=generator.sandyShoreCover(wx,wz,random);
            int meadow=generator.meadowCover(wx,wz,random),woodland=generator.woodlandCover(wx,wz,random),steppe=generator.steppeCover(wx,wz,random),boreal=generator.borealCover(wx,wz,random),warm=generator.warmCover(wx,wz,random),coldSteppe=generator.coldSteppeCover(wx,wz,random),desert=generator.desertCover(wx,wz,random);
            var canopy=generator.canopy(wx,wz,random);
            int highest=Math.max(canopy.topY(wx,wz),Math.max(Math.max(h+blockbranch.cubic.TerrainProfile.desertPlantHeight(desert),h+blockbranch.cubic.TerrainProfile.meadowPlantHeight(meadow)),h+Math.max(reedbed,margin==4 || shore==2 || woodland>=4 || steppe==3 || steppe==4 || steppe==6 || blockbranch.cubic.TerrainProfile.borealPlantPresent(boreal) || (warm>=3 && warm!=7) || coldSteppe==3 || coldSteppe==4 || coldSteppe==6 || coldSteppe==7 || desert>=5 ? 1 : 0)));
            var column = generator.getBaseColumn(wx, wz, level, random);
            for (int y = TerrainShape.FLOOR - 1; y <= Math.max(highest, sample.waterLevel()) + 1; y++) {
                cursor.set(wx, y, wz);
                var tree=canopy.state(wx,y,wz);
                var expected=tree!=null ? tree : RealisticGenerator.material(y,h,sample.waterLevel(),scree,reedbed,margin,shore,meadow,woodland,steppe,boreal,warm,coldSteppe,desert);
                if (!((SparseColumn)chunk).blockbranch$terrain().modifiedView().contains(y>>4) && !chunk.getBlockState(cursor).equals(expected)) throw new AssertionError("Block agreement at " + cursor);
                if (!column.getBlock(y).equals(expected)) throw new AssertionError("Base column at " + cursor);
            }
            for (var type : Heightmap.Types.values()) {
                int expected=h+1;
                for(int y=Math.max(highest,sample.waterLevel()==DrainageNetwork.DRY ? h : sample.waterLevel()-1);y>h;y--)
                    if(type.isOpaque().test(column.getBlock(y))) { expected=y+1; break; }
                int actual=generator.getBaseHeight(wx,wz,type,level,random);
                require(actual==expected,"Base height "+type+" x="+wx+" z="+wz+" ground="+h+" desert="+desert+" expected="+expected+" actual="+actual+" surface="+column.getBlock(h)+" above="+column.getBlock(h+1));
                require(chunk.getHeight(type, x, z) + 1 == expected, "Chunk height " + type);
            }
            for (var bounds : new net.minecraft.world.level.LevelHeightAccessor[]{
                    net.minecraft.world.level.LevelHeightAccessor.create(-64, 384),
                    net.minecraft.world.level.LevelHeightAccessor.create(4096, 32),
                    net.minecraft.world.level.LevelHeightAccessor.create(TerrainShape.CEILING + 512, 32),
                    net.minecraft.world.level.LevelHeightAccessor.create(TerrainShape.FLOOR - 512, 32),
                    net.minecraft.world.level.LevelHeightAccessor.create(-1024, 32)}) {
                var clipped = generator.getBaseColumn(wx, wz, bounds, random);
                for (var type : Heightmap.Types.values()) {
                    int expected = bounds.getMinY();
                    for (int y = bounds.getMaxY(); y >= bounds.getMinY(); y--)
                        if (type.isOpaque().test(clipped.getBlock(y))) { expected = y + 1; break; }
                    require(generator.getBaseHeight(wx, wz, type, bounds, random) == expected, "Clipped query " + type+" x="+wx+" z="+wz+" min="+bounds.getMinY()+" max="+bounds.getMaxY()+" expected="+expected);
                }
            }
        }
        var sparse = (SparseColumn)chunk;
        require(sparse.blockbranch$terrain()!=null, "Procedural terrain description");
        require(sparse.blockbranch$sections().size() <= 8, "Unvisited depth does not allocate sections");
        // Uniform palettes must still become independently writable.
        var zero = new net.minecraft.world.level.chunk.PalettedContainer<>(Blocks.STONE.defaultBlockState(), level.palettedContainerFactory().blockStatesStrategy());
        var packed = zero.pack(level.palettedContainerFactory().blockStatesStrategy());
        require(packed.storage().isEmpty() && packed.paletteEntries().size() == 1, "Uniform palette encoding");
        var originalSection = new net.minecraft.world.level.chunk.LevelChunkSection(zero, level.palettedContainerFactory().createForBiomes());
        var biomeCopy = originalSection.copy();
        var originalBiome = originalSection.getNoiseBiome(0, 0, 0);
        require(originalSection.getBiomes() == biomeCopy.getBiomes(), "Uniform read-only biome palettes shared");
        var alternateBiome = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
            .getOrThrow(originalBiome.is(net.minecraft.world.level.biome.Biomes.DESERT) ? net.minecraft.world.level.biome.Biomes.PLAINS : net.minecraft.world.level.biome.Biomes.DESERT);
        biomeCopy.fillBiomesFromNoise((x, y, z) -> x == 0 ? alternateBiome : originalBiome, 0, 0, 0);
        require(originalSection.getNoiseBiome(0, 0, 0).equals(originalBiome)
            && biomeCopy.getNoiseBiome(0, 0, 0).equals(alternateBiome)
            && biomeCopy.getNoiseBiome(1, 0, 0).equals(originalBiome), "Independent mixed biome editing");
        var snapshot = UniformSnapshots.copyOf(originalSection);
        require(snapshot == UniformSnapshots.copyOf(originalSection), "Uniform snapshots shared");
        var editableSnapshot = snapshot.copy();
        editableSnapshot.setBlockState(1, 1, 1, Blocks.GOLD_BLOCK.defaultBlockState());
        require(snapshot.getBlockState(1, 1, 1).is(Blocks.STONE), "Snapshot copies independently mutable");
        boolean immutable = false;
        try { snapshot.getStates().set(0, 0, 0, Blocks.GOLD_BLOCK.defaultBlockState()); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        require(immutable, "Shared snapshot rejects mutation");
        zero.set(0, 0, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        require(snapshot.getBlockState(0, 0, 0).is(Blocks.STONE), "Save snapshot isolated from live mutation");
        zero.set(0, 0, 0, Blocks.STONE.defaultBlockState());
        var independent = zero.copy(); independent.set(0, 0, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        require(zero.get(0, 0, 0).is(Blocks.STONE) && independent.get(0, 0, 0).is(Blocks.GOLD_BLOCK), "Independent uniform mutations");
        require(chunk.getSections().length == 24, "Native section window stays compact");
        // The vanilla reader can restore light-only padding as empty sections.
        require(sparse.blockbranch$extraSections().entrySet().stream().filter(e -> !e.getValue().hasOnlyAir())
            .allMatch(e -> e.getKey() >= TerrainShape.FLOOR / 16 && e.getKey() < TerrainShape.CEILING / 16), "Bounded occupied sections");
        require(sparse.blockbranch$sections().size() <= generator.getGenDepth() / 16 + 2, "Bounded allocation including lighting padding");
        Blockbranch.LOGGER.info("REALISTIC_CHUNK_PASS {} sections={}", chunk.getPos(), sparse.blockbranch$sections().size());
    }

    private static void verifyLazyEdits(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk, boolean reload) {
        var sparse=(SparseColumn)chunk;
        var terrain=sparse.blockbranch$terrain();
        var untouched=new BlockPos(chunk.getPos().getMinBlockX()+6,-8192,chunk.getPos().getMinBlockZ()+6);
        require(!terrain.resident(untouched.getY()>>4),"Collision fixture begins unmaterialized");
        require(!level.noCollision(new net.minecraft.world.phys.AABB(untouched).deflate(.05)),"Unloaded procedural rock still collides");
        require(!terrain.resident(untouched.getY()>>4),"Collision reads do not allocate untouched terrain");
        var chestPos=new BlockPos(chunk.getPos().getMinBlockX()+5,-11240,chunk.getPos().getMinBlockZ()+5);
        if(reload) {
            require(level.getBlockState(chestPos).is(Blocks.CHEST),"Evicted block entity section reload");
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos);
            require(chest!=null && chest.getItem(0).is(net.minecraft.world.item.Items.DIAMOND) && chest.getItem(0).getCount()==17,"Block entity inventory survives section eviction and world reload");
        }
        level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos);
        chest.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,17));
        int[] ys={-704,0,2};
        for(int sy:ys) {
            if(reload) {
                require(!terrain.resident(sy), "Edited section starts unloaded");
                var loaded=sparse.blockbranch$read(sy);
                for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++)
                    require(loaded.getBlockState(x,y,z)==editPattern(sy,x,y,z), "Persisted nonuniform or empty overlay");
            }
            var section=sparse.blockbranch$write(sy);
            for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) section.setBlockState(x,y,z,editPattern(sy,x,y,z));
        }
        chunk.markUnsaved(); terrain.flush(); terrain.evictOutside(500,510);
        for(int sy:ys) require(!terrain.resident(sy), "Edited section evicted from RAM");
        for(int sy:ys) {
            var loaded=sparse.blockbranch$read(sy);
            for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++)
                require(loaded.getBlockState(x,y,z)==editPattern(sy,x,y,z), "Eviction preserves every edited block");
        }
        // Remove more than a section's depth and let heightmaps find implicit rock below it.
        int wx=chunk.getPos().getMinBlockX()+7,wz=chunk.getPos().getMinBlockZ()+7;
        int h=terrain.base().top(Heightmap.Types.OCEAN_FLOOR.isOpaque(),7,7,ColumnLimits.MAX_Y-1)-1;
        for(int y=h;y>h-32;y--) level.setBlock(new BlockPos(wx,y,wz),Blocks.AIR.defaultBlockState(),2);
        require(chunk.getHeight(Heightmap.Types.OCEAN_FLOOR,7,7)+1==h-31,"Digging into implicit layers updates the heightmap");
        for(int y=h;y>h-32;y--) level.setBlock(new BlockPos(wx,y,wz),terrain.base().state(7,y,7),2);
        terrain.flush(); terrain.evictOutside(500,510);
        require(!terrain.resident(-704) && !terrain.resident(0) && !terrain.resident(2),"Off-window overlays stay unloaded");
        Blockbranch.LOGGER.info("REALISTIC_LAZY_EDITS_PASS nonuniformSections=2 emptySections=1 resident={}",sparse.blockbranch$sections().size());
    }
    private static net.minecraft.world.level.block.state.BlockState editPattern(int sy,int x,int y,int z) {
        if(sy==2) return Blocks.AIR.defaultBlockState();
        return switch((x+3*y+7*z)&3) {
            case 0 -> Blocks.ANDESITE.defaultBlockState();
            case 1 -> Blocks.DIAMOND_BLOCK.defaultBlockState();
            case 2 -> Blocks.AIR.defaultBlockState();
            default -> Blocks.GOLD_BLOCK.defaultBlockState();
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private RealisticSmoke() {}
}
