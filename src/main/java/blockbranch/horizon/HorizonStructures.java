package blockbranch.horizon;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import it.unimi.dsi.fastutil.longs.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.ticks.BlackholeTickAccess;
/**
 * Structures for horizon chunks the world has never generated. Starts are planned exactly as
 * chunk generation plans them, and the generator's own structure decoration places them, but
 * into throwaway in-memory columns: nothing is registered with the chunk map, saved, lit or
 * ticked, and the level is never asked for a chunk. A chunk is decorated only when a start
 * within the native eight-chunk reference range reaches it and comes within
 * {@link #DEPTH} blocks of its ground, or when an atlas well reaches it; every other chunk
 * stays analytic. Wells are not structure starts: decoration places them from the atlas,
 * whether or not structures are enabled, so they are checked separately.
 *
 * <p>Neighbouring columns are terrain only, so a structure reads its neighbours' terrain but
 * not their structures; writes outside the decorated chunk are discarded, as generation
 * discards writes outside its region. A failure falls back to analytic terrain for that chunk.
 *
 * <p>Native generation places the vanilla well feature only from its centre chunk, which writes
 * across the border into its neighbours. Here a neighbour's well is placed again into the
 * decorated chunk, with the centre chunk's position and seed, so each side of the border keeps
 * its part. The mod's own cistern is already placed per chunk.
 */
final class HorizonStructures {
    /** Native reference range, in chunks. */
    private static final int RANGE=8;
    /** Starts lying entirely deeper than this below the chunk's ground cannot be seen. */
    private static final int DEPTH=64;
    private static final int STARTS=32768,NEIGHBOURS=96;
    private final ServerLevel level;
    private final RealisticGenerator generator;
    private final RandomState random;
    private final ChunkGeneratorStructureState state;
    private final Long2ObjectLinkedOpenHashMap<List<StructureStart>> starts=new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<ProtoChunk> neighbours=new Long2ObjectLinkedOpenHashMap<>();
    private final StructureManager planning;
    /** Atlas wells, or null when this world places none. */
    private final DesertWell wells;
    private volatile boolean warned;
    /** Decorated chunks and the time spent placing them, for the sampler log. */
    static final java.util.concurrent.atomic.LongAdder DECORATED=new java.util.concurrent.atomic.LongAdder(),DECORATE_NANOS=new java.util.concurrent.atomic.LongAdder();

    HorizonStructures(ServerLevel level) {
        this.level=level;this.generator=(RealisticGenerator)level.getChunkSource().getGenerator();
        this.random=level.getChunkSource().randomState();this.state=level.getChunkSource().getGeneratorState();
        this.planning=new Scratch(level);
        this.wells=generator.structureDefinition()>0?generator.wells(random):null;
    }

    /** The chunk's blocks with its structures placed, or null when no visible structure reaches it. */
    HorizonColumns.Source column(int cx,int cz) {
        var well=wells==null?Optional.<DesertWell.Plan>empty():wells.forChunk(new ChunkPos(cx,cz));
        boolean structures=level.structureManager().shouldGenerateStructures();
        if(!structures&&well.isEmpty())return null;
        int minX=cx<<4,minZ=cz<<4;
        var reaching=new ArrayList<StructureStart>();var references=new LongArrayList();var own=new ArrayList<StructureStart>();
        boolean visible=well.isPresent();
        if(structures) {
            int ground=OverworldStructures.baseHeight(generator,random,minX+8,minZ+8,Heightmap.Types.OCEAN_FLOOR_WG,level);
            for(int sz=cz-RANGE;sz<=cz+RANGE;sz++)for(int sx=cx-RANGE;sx<=cx+RANGE;sx++)
                for(var start:starts(sx,sz)) {
                    if(sx==cx&&sz==cz)own.add(start);
                    var box=start.getBoundingBox();
                    if(!box.intersects(minX,minZ,minX+15,minZ+15))continue;
                    reaching.add(start);references.add(ChunkPos.pack(sx,sz));
                    visible|=box.maxY()>=ground-DEPTH;
                }
        }
        if(!visible)return null;
        long began=System.nanoTime();
        try {
            var chunk=terrain(cx,cz);
            for(var start:own)chunk.setStartForStructure(start.getStructure(),start);
            for(int i=0;i<reaching.size();i++)chunk.addReferenceForStructure(reaching.get(i).getStructure(),references.getLong(i));
            var sandbox=new Sandbox(chunk);
            generator.applyBiomeDecoration(sandbox.level,chunk,new Scratch(sandbox.level));
            well.filter(p->generator.nativeOrdinaryDefinition()>0&&(p.x()>>4!=cx||p.z()>>4!=cz))
                .ifPresent(p->NativeOrdinaryStructures.placeWell(wells,sandbox.level,new ChunkPos(p.x()>>4,p.z()>>4),generator));
            DECORATED.increment();DECORATE_NANOS.add(System.nanoTime()-began);
            return HorizonSources.loaded(chunk,null);
        } catch(RuntimeException|LinkageError failure) {
            if(!warned){warned=true;Blockbranch.LOGGER.warn("Horizon could not place structures in chunk {},{}; showing terrain there",cx,cz,failure);}
            return null;
        }
    }

    /** Valid starts planned in a chunk, as structure generation would plan them. */
    private List<StructureStart> starts(int cx,int cz) {
        long key=ChunkPos.pack(cx,cz);
        synchronized(starts){var known=starts.getAndMoveToLast(key);if(known!=null)return known;}
        List<StructureStart> found=List.of();
        boolean candidate=false;
        for(var set:state.possibleStructureSets())if(set.value().placement().isStructureChunk(state,cx,cz)){candidate=true;break;}
        if(candidate) {
            var chunk=new ProtoChunk(new ChunkPos(cx,cz),UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
            generator.createStructures(level.registryAccess(),state,planning,chunk,level.getServer().getStructureTemplateManager(),level.dimension());
            found=chunk.getAllStarts().values().stream().filter(StructureStart::isValid).toList();
        }
        synchronized(starts){starts.putAndMoveToLast(key,found);while(starts.size()>STARTS)starts.removeFirst();}
        return found;
    }

    /** A detached column holding only the chunk's procedural terrain, as built before decoration. */
    private ProtoChunk terrain(int cx,int cz) {
        var chunk=new ProtoChunk(new ChunkPos(cx,cz),UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
        var sparse=(SparseColumn)chunk;
        sparse.blockbranch$terrain(new ColumnTerrain(chunk,generator.profile(chunk,random),sparse.blockbranch$factory(),null,new int[0]));
        Heightmap.primeHeightmaps(chunk,EnumSet.allOf(Heightmap.Types.class));
        // Decoration follows terrain; earlier statuses refuse biome queries.
        chunk.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.TERRAIN);
        return chunk;
    }
    private ProtoChunk neighbour(int cx,int cz) {
        long key=ChunkPos.pack(cx,cz);
        synchronized(neighbours){var known=neighbours.getAndMoveToLast(key);if(known!=null)return known;}
        var chunk=terrain(cx,cz);
        synchronized(neighbours){neighbours.putAndMoveToLast(key,chunk);while(neighbours.size()>NEIGHBOURS)neighbours.removeFirst();}
        return chunk;
    }

    /** Structure lookups over the sandbox and the planned starts; never over the level's chunks. */
    private final class Scratch extends StructureManager {
        Scratch(LevelAccessor sandbox){super(sandbox,null,null);}
        @Override public boolean shouldGenerateStructures(){return level.structureManager().shouldGenerateStructures();}
        @Override public void fillStartsForStructure(Structure structure,LongSet references,Consumer<StructureStart> consumer) {
            for(long key:references)for(var start:starts(ChunkPos.getX(key),ChunkPos.getZ(key)))if(start.getStructure()==structure)consumer.accept(start);
        }
        @Override public void addReference(StructureStart start){}
    }

    /**
     * A {@link WorldGenLevel} over one decorated column and its terrain-only neighbours, in the
     * generation height window. Entities, block entities, ticks and updates are discarded;
     * registries, seed and level data come from the level, which is never asked for blocks.
     */
    private final class Sandbox implements InvocationHandler {
        final ProtoChunk center;
        final ChunkPos pos;
        final WorldGenLevel level;
        final RandomSource sandboxRandom;
        Sandbox(ProtoChunk center) {
            this.center=center;this.pos=center.getPos();
            this.sandboxRandom=RandomSource.create(HorizonStructures.this.level.getSeed()^pos.pack());
            this.level=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},this);
        }
        private ChunkAccess chunk(int cx,int cz){return cx==pos.x()&&cz==pos.z()?center:neighbour(cx,cz);}
        private boolean inside(BlockPos p){return p.getY()>=TerrainShape.FLOOR&&p.getY()<TerrainShape.CEILING;}
        private BlockState read(BlockPos p){return inside(p)?chunk(p.getX()>>4,p.getZ()>>4).getBlockState(p):Blocks.VOID_AIR.defaultBlockState();}
        private boolean writable(BlockPos p){return inside(p)&&p.getX()>>4==pos.x()&&p.getZ()>>4==pos.z();}
        private boolean write(BlockPos p,BlockState s) {
            if(!writable(p))return false;
            center.setBlockState(p,s,0);VillagePaths.written(p,s);return true;
        }
        private int height(Heightmap.Types type,int x,int z) {
            if(OverworldStructures.terrainQueries())return generator.getBaseHeight(x,z,type,level,random);
            return chunk(x>>4,z>>4).getHeight(type,x&15,z&15)+1;
        }
        @Override public Object invoke(Object proxy,Method method,Object[] a) throws Throwable {
            var real=HorizonStructures.this.level;
            int n=a==null?0:a.length;
            switch(method.getName()) {
                case "getBlockState": if(n==1)return read((BlockPos)a[0]);break;
                case "getFluidState": if(n==1)return read((BlockPos)a[0]).getFluidState();break;
                case "isStateAtPosition": return ((Predicate<BlockState>)a[1]).test(read((BlockPos)a[0]));
                case "isFluidAtPosition": return ((Predicate<net.minecraft.world.level.material.FluidState>)a[1]).test(read((BlockPos)a[0]).getFluidState());
                case "setBlock": return write((BlockPos)a[0],(BlockState)a[1]);
                case "removeBlock": return write((BlockPos)a[0],read((BlockPos)a[0]).getFluidState().createLegacyBlock());
                case "destroyBlock": return write((BlockPos)a[0],Blocks.AIR.defaultBlockState());
                case "ensureCanWrite": return writable((BlockPos)a[0]);
                case "getHeight": if(n==3)return height((Heightmap.Types)a[0],(int)a[1],(int)a[2]);if(n==0)return TerrainShape.CEILING-TerrainShape.FLOOR;break;
                case "getMinY": if(n==0)return TerrainShape.FLOOR;break;
                case "getChunk": if(n==4)return chunk((int)a[0],(int)a[1]);break;
                case "hasChunk": return true;
                case "getBlockEntity": if(n==1)return null;break;
                case "getEntities","getEntitiesOfClass","players": return List.of();
                case "addFreshEntity": return false;
                case "getBlockTicks","getFluidTicks": return BlackholeTickAccess.emptyLevelList();
                case "nextSubTickCount": return 0L;
                case "getSeed": return real.getSeed();
                case "getLevel": return real;
                case "getRandom": return sandboxRandom;
                case "getBiomeManager": return new BiomeManager(level,BiomeManager.obfuscateSeed(real.getSeed()));
                case "getUncachedNoiseBiome": return ((ScreeBiomeSource)generator.getBiomeSource()).surfaceResolver().getNoiseBiome((int)a[0],(int)a[1],(int)a[2]);
                case "getCurrentDifficultyAt": return new DifficultyInstance(real.getDifficulty(),0,0,0);
                case "isClientSide": return false;
                case "getSkyDarken": return 0;
                case "setCurrentlyGenerating","playSound","addParticle","levelEvent","gameEvent","blockUpdated","updateNeighborsAt","neighborShapeChanged","scheduleTick": if(method.getReturnType()==void.class)return null;break;
                case "toString": if(n==0)return "HorizonSandbox"+pos;break;
                case "hashCode": if(n==0)return System.identityHashCode(proxy);break;
                case "equals": if(n==1)return proxy==a[0];break;
                default: break;
            }
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,a);
            // Registries, level data, the border and other read-only context.
            return method.invoke(real,a);
        }
    }
}
