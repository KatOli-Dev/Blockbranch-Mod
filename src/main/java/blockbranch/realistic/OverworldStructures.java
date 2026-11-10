package blockbranch.realistic;

import com.mojang.datafixers.util.Either;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.*;

/** Additional native structures, enabled only by an explicit saved definition. */
public final class OverworldStructures {
    private static final Set<String> SETS=Set.of("villages","mineshafts","pillager_outposts","strongholds",
        "ancient_cities","trial_chambers","trail_ruins","abandoned_camp","desert_pyramids",
        "jungle_temples","woodland_mansions","ocean_monuments");
    private static final Set<String> PLANNED=Set.of("buried_treasure","shipwreck_beached","ruined_portal_desert",
        "swamp_hut","igloo","ocean_ruin_cold","ocean_ruin_warm");
    private OverworldStructures(){}
    private static final ThreadLocal<Boolean> TERRAIN_QUERIES=ThreadLocal.withInitial(()->false);
    public static boolean terrainQueries(){return TERRAIN_QUERIES.get();}
    public static <T> T withTerrainQueries(java.util.function.Supplier<T> action){
        boolean before=TERRAIN_QUERIES.get();TERRAIN_QUERIES.set(true);
        try{return action.get();}finally{if(before)TERRAIN_QUERIES.set(true);else TERRAIN_QUERIES.remove();}
    }
    public static int baseHeight(RealisticGenerator g,RandomState random,int x,int z,Heightmap.Types type,LevelHeightAccessor bounds){
        var site=g.drainage(random).sample(x,z);int top=site.height()+1;
        if(type.isOpaque().test(net.minecraft.world.level.block.Blocks.WATER.defaultBlockState())&&site.waterLevel()!=DrainageNetwork.DRY)top=Math.max(top,site.waterLevel());
        return Math.clamp(top,bounds.getMinY(),bounds.getMaxY()+1);
    }

    public static boolean additional(Identifier id){
        return id.getNamespace().equals("minecraft")&&!PLANNED.contains(id.getPath())&&
            !Set.of("fortress","bastion_remnant","nether_fossil","end_city","ruined_portal_nether").contains(id.getPath());
    }
    public static boolean additional(Identifier id,RealisticGenerator generator){
        return additional(id)&&!(generator.reimaginedDefinition()>0&&Set.of("desert_pyramid","jungle_pyramid","mansion","monument").contains(id.getPath()));
    }
    public static void addSets(HolderLookup<StructureSet> lookup,List<Holder<StructureSet>> output,RealisticGenerator generator){
        lookup.listElements().forEach(holder->{
            var id=holder.key().identifier();
            if(!id.getNamespace().equals("minecraft"))return;
            if(SETS.contains(id.getPath())){if(holder.value().structures().stream().allMatch(e->e.structure().unwrapKey().map(k->additional(k.identifier(),generator)).orElse(false)))output.add(holder);} // Keep village identity for outpost exclusion zones.
            else if(id.getPath().equals("shipwrecks")||id.getPath().equals("ruined_portals")){
                var entries=holder.value().structures().stream().filter(e->e.structure().unwrapKey().map(k->additional(k.identifier())).orElse(false)).toList();
                if(!entries.isEmpty())output.add(Holder.direct(new StructureSet(entries,holder.value().placement())));
            }
        });
    }

    public static boolean deepDark(long seed,int x,int y,int z,DrainageNetwork terrain){return DeepDarkHabitat.contains(seed,x,y,z,terrain);}
    public static int deepDarkMask(long seed,int cx,int cz,DrainageNetwork terrain){return DeepDarkHabitat.mask(seed,cx,cz,terrain);}

    public static Optional<Structure.GenerationStub> fit(Structure structure,Structure.GenerationContext c,
            java.util.function.Function<Structure.GenerationContext,Optional<Structure.GenerationStub>> nativeGeneration){
        var g=(RealisticGenerator)c.chunkGenerator();
        var id=c.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
        if(id==null||!additional(id,g))return nativeGeneration.apply(c);
        String kind=id.getPath();var terrain=g.drainage(c.randomState());
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();var site=terrain.sample(x,z);
        var surface=((ScreeBiomeSource)c.biomeSource()).surfaceResolver();
        BiomeResolver resolver=surface;
        boolean underground=kind.equals("stronghold")||kind.startsWith("mineshaft")||kind.equals("trial_chambers")||kind.equals("ancient_city");
        if(kind.equals("ancient_city")){
            if(!deepDark(c.seed(),x,DeepDarkHabitat.proposalY(site.height()),z,terrain))return Optional.empty();
            var dark=c.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DEEP_DARK);
            resolver=(qx,qy,qz)->dark;
        }
        // A fixed-Y native monument remains reload-safe. Only shallow marine sites
        // with a bounded foundation are eligible; deep-basin relocation belongs to
        // the separately coordinated monument implementation.
        if(kind.equals("monument")){
            if(!marineFootprint(terrain,x-40,z-40,x+40,z+40,7,38))return Optional.empty();
            var ocean=c.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DEEP_OCEAN);
            resolver=(qx,qy,qz)->ocean;
        }
        var predicate=c.validBiome();
        if(kind.equals("monument")){
            var ocean=c.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DEEP_OCEAN);
            if(!predicate.test(ocean))return Optional.empty();
            predicate=b->true;
        }
        var bounds=underground?c.heightAccessor():LevelHeightAccessor.create(Math.max(TerrainShape.FLOOR,site.height()-256),512);
        if(kind.startsWith("ruined_portal"))bounds=LevelHeightAccessor.create(Math.max(TerrainShape.FLOOR,site.height()-128),256);
        var adjusted=new Structure.GenerationContext(c.registryAccess(),c.chunkGenerator(),c.biomeSource(),c.climateSampler(),resolver,
            c.randomState(),c.structureTemplateManager(),c.random(),c.seed(),c.chunkPos(),bounds,predicate);
        var result=nativeGeneration.apply(adjusted);
        if(result.isEmpty())return result;
        var stub=result.get();StructurePiecesBuilder builder=stub.getPiecesBuilder();
        if(builder.isEmpty())return Optional.empty();
        var box=builder.getBoundingBox();int dy=0;
        if(underground){
            int lowest=Integer.MAX_VALUE;
            for(int wz=box.minZ();wz<=box.maxZ();wz+=8)for(int wx=box.minX();wx<=box.maxX();wx+=8)lowest=Math.min(lowest,terrain.sample(wx,wz).height());
            int depth=kind.equals("ancient_city")?320:kind.equals("trial_chambers")?192:96+c.random().nextInt(129);
            int desired=kind.equals("ancient_city")?DeepDarkHabitat.cityFloor(site.height(),box.getYSpan()):site.height()-depth;
            int target=Math.max(TerrainShape.FLOOR+64,Math.min(desired,lowest-32-box.getYSpan()));
            dy=target-box.minY();builder.offsetPiecesVertically(dy);box=builder.getBoundingBox();
            if(box.maxY()>lowest-32)return Optional.empty();
            var anchor=stub.position().offset(0,dy,0);
            if(kind.equals("ancient_city")&&!deepDark(c.seed(),anchor.getX(),anchor.getY(),anchor.getZ(),terrain))return Optional.empty();
        }else if(!kind.equals("monument")&&!kind.equals("shipwreck")&&!kind.startsWith("ruined_portal")&&!kind.equals("trail_ruins")){
            // Native terrain matching paths handle local relief. Rigid buildings
            // need dry, gentle footprints since this generator has no beard density.
            for(var piece:builder.build().pieces())if(!dryFootprint(terrain,piece.getBoundingBox(),kind.startsWith("village")?12:16))return Optional.empty();
        }
        if(kind.equals("shipwreck")){
            if(!marineFootprint(terrain,box.minX(),box.minZ(),box.maxX(),box.maxZ(),TerrainShape.FLOOR+32,60))return Optional.empty();
            int floor=0,count=0,min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
            for(int wz=box.minZ();wz<=box.maxZ();wz++)for(int wx=box.minX();wx<=box.maxX();wx++){
                int y=terrain.sample(wx,wz).height();floor+=y;count++;min=Math.min(min,y);max=Math.max(max,y);
            }
            if(max-min>12)return Optional.empty();
            var piece=(ShipwreckPieces.ShipwreckPiece)builder.build().pieces().getFirst();
            int target=floor/count+1;
            piece.move(0,target-piece.getBoundingBox().minY(),0);
            piece.adjustPositionHeight(target);
            box=builder.getBoundingBox();
        }
        if(box.minY()<=TerrainShape.FLOOR+32||box.maxY()>=TerrainShape.CEILING-16)return Optional.empty();
        return Optional.of(new Structure.GenerationStub(stub.position().offset(0,dy,0),Either.right(builder)));
    }

    private static boolean dryFootprint(DrainageNetwork terrain,BoundingBox box,int relief){
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(int z=box.minZ();z<=box.maxZ();z+=4)for(int x=box.minX();x<=box.maxX();x+=4){
            var s=terrain.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)return false;
            min=Math.min(min,s.height());max=Math.max(max,s.height());if(max-min>relief)return false;
        }
        return true;
    }
    private static boolean marineFootprint(DrainageNetwork terrain,int minX,int minZ,int maxX,int maxZ,int minY,int maxY){
        for(int z=minZ;z<=maxZ;z+=4)for(int x=minX;x<=maxX;x+=4){
            var s=terrain.sample(x,z);
            if(!terrain.oceanWater(x,z,s)||s.waterLevel()!=TerrainShape.SEA_LEVEL||s.height()<minY||s.height()>maxY)return false;
        }
        return true;
    }

    public static void decorate(WorldGenLevel level,ChunkAccess chunk,StructureManager structures,RealisticGenerator g){
        if(!structures.shouldGenerateStructures())return;
        var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var random=new WorldgenRandom(new XoroshiroRandomSource(0));
        long seed=random.setDecorationSeed(level.getSeed(),chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ());
        var writable=new BoundingBox(chunk.getPos().getMinBlockX(),TerrainShape.FLOOR+1,chunk.getPos().getMinBlockZ(),
            chunk.getPos().getMaxBlockX(),TerrainShape.CEILING-1,chunk.getPos().getMaxBlockZ());
        for(var step:GenerationStep.Decoration.values()){
            int index=0;
            for(var structure:registry){
                // This registry entry did not exist in the saved coverage102 definition.
                // It has its own decoration pass and must never renumber native seeds.
                if(registry.getKey(structure).equals(ReimaginedStructures.key(ReimaginedSites.CRYPT).identifier()))continue;
                if(structure.step()!=step)continue;
                if(additional(registry.getKey(structure),g)){
                    random.setFeatureSeed(seed,index,step.ordinal());
                    for(var start:structures.startsForStructure(chunk.getPos().x(),chunk.getPos().z(),structure)){
                        if(!start.isValid())continue;
                        if(registry.getKey(structure).getPath().equals("ancient_city"))clearCity(level,start,writable);
                        String kind=registry.getKey(structure).getPath();
                        if(kind.startsWith("village_")||kind.startsWith("abandoned_camp_")||Set.of("pillager_outpost","mansion","desert_pyramid","jungle_pyramid","trail_ruins").contains(kind))clearCanopy(level,start,writable,g);
                        withTerrainQueries(()->{
                            Runnable placement=()->start.placeInChunk(level,structures,g,random,writable,chunk.getPos());
                            if(kind.startsWith("village_"))VillagePaths.place(level,placement);else placement.run();
                            return null;
                        });
                    }
                }
                index++;
            }
            // Match native ordering: underground structures, then their small
            // features, before surface structures and ancient-city decoration.
            if(step==GenerationStep.Decoration.LOCAL_MODIFICATIONS)geode(level,chunk,g,random);
            if(step==GenerationStep.Decoration.UNDERGROUND_STRUCTURES)smallStructures(level,chunk,g,random);
        }
    }

    private static void geode(WorldGenLevel level,ChunkAccess chunk,RealisticGenerator g,WorldgenRandom random){
        random.setDecorationSeed(level.getSeed()^0x47454f44454e4154L,chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ());
        if(random.nextInt(24)!=0)return;
        int x=chunk.getPos().getMinBlockX()+random.nextInt(16),z=chunk.getPos().getMinBlockZ()+random.nextInt(16);
        int y=Math.max(TerrainShape.FLOOR+40,g.drainage(level.getLevel().getChunkSource().randomState()).sample(x,z).height()-48-random.nextInt(209));
        level.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.FEATURE,Identifier.withDefaultNamespace("amethyst_geode"))).value()
            .place(level,g,random,new BlockPos(x,y,z));
    }

    private static void smallStructures(WorldGenLevel level,ChunkAccess chunk,RealisticGenerator g,WorldgenRandom random){
        var features=level.registryAccess().lookupOrThrow(Registries.FEATURE);
        var terrain=g.drainage(level.getLevel().getChunkSource().randomState());
        var room=features.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.FEATURE,Identifier.withDefaultNamespace("monster_room"))).value();
        random.setDecorationSeed(level.getSeed()^0x4e4154495645524fL,chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ());
        if(g.reimaginedDefinition()==0)for(int i=0;i<14;i++){
            int x=chunk.getPos().getMinBlockX()+random.nextInt(16),z=chunk.getPos().getMinBlockZ()+random.nextInt(16);
            int ground=terrain.sample(x,z).height(),y=Math.max(TerrainShape.FLOOR+40,ground-48-random.nextInt(273));
            if(!deepDark(level.getSeed(),x,y,z,terrain))room.place(level,g,random,new BlockPos(x,y,z));
        }
        var source=((ScreeBiomeSource)g.getBiomeSource()).surfaceResolver();
        var biome=source.getNoiseBiome(chunk.getPos().getMiddleBlockX()>>2,0,chunk.getPos().getMiddleBlockZ()>>2);
        if(!biome.is(Biomes.DESERT)&&!biome.is(Biomes.SWAMP)&&!biome.is(Biomes.MANGROVE_SWAMP))return;
        // Native upper/lower rarity budgets remain separate. Map their elevations
        // to this generator's existing coal and diamond depth bands.
        for(boolean deep:new boolean[]{false,true}){
            if(random.nextInt(64)!=0)continue;
            int x=chunk.getPos().getMinBlockX()+random.nextInt(16),z=chunk.getPos().getMinBlockZ()+random.nextInt(16);
            String fossil=deep?"fossil_diamonds":"fossil_coal";
            int depth=deep?96+random.nextInt(96):random.nextInt(64);
            features.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.FEATURE,Identifier.withDefaultNamespace(fossil))).value()
                .place(level,g,random,new BlockPos(x,terrain.sample(x,z).height()-depth,z));
        }
    }

    public record Located(BlockPos pos,Holder<Structure> holder,StructureStart start,ChunkAccess chunk){}
    /** Read-only native placement search so mixed map tags reference only the chosen result. */
    public static Located locate(net.minecraft.server.level.ServerLevel level,HolderSet<Structure> targets,BlockPos origin,int radius,boolean skipKnown){
        if(!level.structureManager().shouldGenerateStructures())return null;
        var state=level.getChunkSource().getGeneratorState();
        var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var scans=new LinkedHashMap<net.minecraft.world.level.levelgen.structure.placement.StructurePlacement,List<Holder<Structure>>>();
        for(var holder:targets)if(additional(registry.getKey(holder.value()),(RealisticGenerator)level.getChunkSource().getGenerator()))
            for(var placement:state.getPlacementsForStructure(holder))scans.computeIfAbsent(placement,k->new ArrayList<>()).add(holder);
        Located best=null;double distance=Double.MAX_VALUE;
        for(var entry:scans.entrySet())if(entry.getKey() instanceof net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement rings){
            for(var chunk:state.getRingPositionsFor(rings)){
                if(rings.getLocatePos(chunk).distSqr(origin)>=distance)continue;
                var found=at(level,entry.getValue(),entry.getKey(),chunk,skipKnown);
                if(found!=null){best=found;distance=found.pos().distSqr(origin);}
            }
        }
        for(int ring=0;ring<=radius;ring++){
            boolean foundRing=false;
            for(var entry:scans.entrySet())if(entry.getKey() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread){
                for(int z=-ring;z<=ring;z++)for(int x=-ring;x<=ring;x++){
                    if(Math.abs(x)!=ring&&Math.abs(z)!=ring)continue;
                    var chunk=spread.getPotentialStructureChunk(state.getLevelSeed(),(origin.getX()>>4)+x*spread.spacing(),(origin.getZ()>>4)+z*spread.spacing());
                    if(!spread.isStructureChunk(state,chunk.x(),chunk.z()))continue;
                    var found=at(level,entry.getValue(),spread,chunk,skipKnown);
                    if(found!=null){foundRing=true;double d=found.pos().distSqr(origin);if(d<distance){distance=d;best=found;}}
                }
            }
            if(foundRing)break;
        }
        return best;
    }
    private static Located at(net.minecraft.server.level.ServerLevel level,List<Holder<Structure>> targets,
            net.minecraft.world.level.levelgen.structure.placement.StructurePlacement placement,ChunkPos at,boolean skipKnown){
        for(var holder:targets){
            if(level.structureManager().checkStructurePresence(at,holder.value(),placement,skipKnown)==StructureCheckResult.START_NOT_PRESENT)continue;
            var chunk=level.getChunk(at.x(),at.z(),net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
            var start=level.structureManager().getStartForStructure(holder.value(),chunk);
            if(start!=null&&start.isValid()&&(!skipKnown||start.canBeReferenced()))return new Located(placement.getLocatePos(at),holder,start,chunk);
        }
        return null;
    }

    private static void clearCanopy(WorldGenLevel level,StructureStart start,BoundingBox writable,RealisticGenerator g){
        var terrain=g.drainage(level.getLevel().getChunkSource().randomState());
        var pos=new BlockPos.MutableBlockPos();
        for(var piece:start.getPieces()){var b=piece.getBoundingBox();
            for(int z=Math.max(b.minZ(),writable.minZ());z<=Math.min(b.maxZ(),writable.maxZ());z++)
                for(int x=Math.max(b.minX(),writable.minX());x<=Math.min(b.maxX(),writable.maxX());x++){
                    int ground=terrain.sample(x,z).height();
                    int top=g.canopy(x,z,level.getLevel().getChunkSource().randomState()).topY(x,z);
                    for(int y=ground+1;y<=top;y++){var block=level.getBlockState(pos.set(x,y,z));
                        if(block.is(net.minecraft.tags.BlockTags.LOGS)||block.is(net.minecraft.tags.BlockTags.LEAVES)||block.is(net.minecraft.world.level.block.Blocks.VINE)||block.is(net.minecraft.world.level.block.Blocks.BAMBOO))level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
                    }
                }
        }
    }

    private static void clearCity(WorldGenLevel level,StructureStart start,BoundingBox writable){
        // Native templates include structure voids. Give the complete city a
        // connected cavern with a tapered boundary, rather than leaving separate
        // cuboid excavations and floating rock slabs between adjoining templates.
        var whole=StructurePiece.createBoundingBox(start.getPieces().stream());
        int floor=whole.minY()-1;var pos=new BlockPos.MutableBlockPos();
        var rock=(blockbranch.cubic.TerrainProfile.deepslateAt(floor)?net.minecraft.world.level.block.Blocks.DEEPSLATE:net.minecraft.world.level.block.Blocks.STONE).defaultBlockState();
        for(int z=writable.minZ();z<=writable.maxZ();z++)for(int x=writable.minX();x<=writable.maxX();x++){
            double distance=12;
            for(var piece:start.getPieces()){
                var b=piece.getBoundingBox();int dx=Math.max(0,Math.max(b.minX()-x,x-b.maxX())),dz=Math.max(0,Math.max(b.minZ()-z,z-b.maxZ()));
                distance=Math.min(distance,Math.hypot(dx,dz));if(distance==0)break;
            }
            if(distance>=12)continue;
            double edge=1-distance/12,blend=edge*edge*(3-2*edge);
            int roof=floor+(int)Math.round((whole.getYSpan()+10+5*DripstoneHabitat.province(level.getSeed()^0x4349545943415645L,x*4,z*4))*blend);
            for(int down=0;down<4;down++){
                var old=level.getBlockState(pos.set(x,floor-down,z));
                if(old.isAir()||!old.getFluidState().isEmpty())level.setBlock(pos,rock,2);
            }
            for(int y=floor+1;y<=roof;y++)level.setBlock(pos.set(x,y,z),net.minecraft.world.level.block.Blocks.CAVE_AIR.defaultBlockState(),2);
        }
    }
}
