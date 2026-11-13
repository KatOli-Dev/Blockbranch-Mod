package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Focused native generation, saved arrays/edits, future chunks and partial clearing qualification. */
public final class ForestGrasslandTransitionSmoke {
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception {
        var random=level.getChunkSource().randomState();long seed=random.seed();
        var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
        Path dir=Path.of(System.getProperty("blockbranch.memorySmokeResult")).toAbsolutePath().getParent();
        Files.writeString(dir.resolve("generator.json"),encoded.toString());
        if(encoded.get("shape_version").getAsInt()!=257){verifyCurrent(level,g,encoded,reload,dir);return;}
        require(encoded.get("shape_version").getAsInt()==257&&g.biomeTransitionDefinition()==1&&g.redwoodForestDefinition()==1,"Fresh and reopened world retain schema257/transition1/redwood1");
        // Schema 256 is the transition-only foundation: the same world without the redwood opt-in.
        var transitionOnly=encoded.deepCopy();transitionOnly.remove("redwood_forest_definition");transitionOnly.addProperty("shape_version",256);
        var foundation=RealisticGenerator.CODEC.codec().parse(ops,transitionOnly).getOrThrow();
        require(foundation.biomeTransitionDefinition()==1&&foundation.redwoodForestDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,foundation).getOrThrow().equals(transitionOnly),"Schema256 transition-only round trip, omitted redwood opt-in");
        var old=transitionOnly.deepCopy();old.remove("biome_transition_definition");old.addProperty("shape_version",255);
        var legacy=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
        require(legacy.biomeTransitionDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,legacy).getOrThrow().equals(old),"Historical codec round trip, omitted opt-in");
        for(int definition:new int[]{-1,0,2}){var bad=encoded.deepCopy();bad.addProperty("biome_transition_definition",definition);require(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isEmpty(),"Reject wrong transition flag");}
        var missing=encoded.deepCopy();missing.remove("biome_transition_definition");require(RealisticGenerator.CODEC.codec().parse(ops,missing).result().isEmpty(),"No silent transition fallback");
        for(int schema:new int[]{254,255,256,258}){var bad=encoded.deepCopy();bad.addProperty("shape_version",schema);require(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isEmpty(),"Exact supported foundation");}
        // The previous production reader is deliberately independent of this codec.
        String prior=System.getProperty("blockbranch.transitionPriorJar","");
        require(!prior.isEmpty(),"Prior production jar required for qualification");
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(prior).toUri().toURL()},ForestGrasslandTransitionSmoke.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(!name.equals("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.RealisticGenerator$")&&!name.equals("blockbranch.realistic.ScreeBiomeSource")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource$"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        }){
            var type=loader.loadClass("blockbranch.realistic.RealisticGenerator");
            var codec=(com.mojang.serialization.MapCodec<?>)type.getField("CODEC").get(null);
            require(codec.codec().parse(ops,old).result().isPresent(),"Prior reader accepts historical255 control");
            require(codec.codec().parse(ops,transitionOnly).result().isEmpty(),"Prior reader rejects256 before chunks/clear plans");
            require(codec.codec().parse(ops,encoded).result().isEmpty(),"Prior reader rejects257 before chunks/clear plans");
        }
        qualify(level,g,legacy,reload,dir,random,seed,OakBirchWoodlandFixtures.fixture(g.drainage(random),seed),true);
    }
    /** Current gated preset: the schema-257 contract above needs its own world and prior jar, so here the same transition paths run with a transition-off twin of this generator. */
    private static void verifyCurrent(ServerLevel level,RealisticGenerator g,JsonObject encoded,boolean reload,Path dir)throws Exception {
        var random=level.getChunkSource().randomState();long seed=random.seed();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        require(encoded.get("shape_version").getAsInt()==272&&g.borealSlopeDefinition()==1&&g.montaneForestDefinition()==1&&g.alluvialDefinition()==1&&g.seasonalForestDefinition()==1&&g.warmXericDefinition()==1&&encoded.get("leaf_litter_definition").getAsInt()==1&&encoded.get("continental_ice_definition").getAsInt()==1&&g.biomeTransitionDefinition()==1&&g.redwoodForestDefinition()==1,"Current preset retains schema272/slope1/montane1/alluvial1/seasonal1/xeric1/leaf-litter1/ice1/transition1/redwood1");
        var legacy=RealisticGenerator.CODEC.codec().parse(ops,encoded).getOrThrow();
        var field=RealisticGenerator.class.getDeclaredField("biomeTransitionDefinition");field.setAccessible(true);field.setInt(legacy,0);
        require(legacy.biomeTransitionDefinition()==0&&g.biomeTransitionDefinition()==1,"Transition-off twin differs only in the transition definition");
        var source=g.getBiomeSource().createUncachedResolver(random);var drainage=g.drainage(random);
        // The frozen schema-257 root set no longer equals current transition-off roots (later closed-canopy infill stems, family 1), so here the transition effect is measured against current transition-off roots; the frozen comparison stays in the 257 contract.
        // Habitat membership alone now selects points owned by coastal winter-rain oak woodland, whose roots are not the frozen temperate set.
        var owned=OakBirchWoodlandFixtures.fixture(drainage,seed,(x,z)->source.getNoiseBiome(x>>2,drainage.sample(x,z).height()>>2,z>>2).is(Blockbranch.id("dappled_oak_birch_woodland")));
        qualify(level,g,legacy,reload,dir,random,seed,owned,false);
    }
    private static void qualify(ServerLevel level,RealisticGenerator g,RealisticGenerator legacy,boolean reload,Path dir,net.minecraft.world.level.levelgen.RandomState random,long seed,OakBirchWoodland.WoodlandPoint p,boolean frozen)throws Exception {
        var d=g.drainage(random);var c=new ClimateField(seed);
        WoodlandTrees.Root added=null,removed=null;int rootSamples=0;
        for(int z=p.z()-384;z<=p.z()+384;z+=64)for(int x=p.x()-384;x<=p.x()+384;x+=64){
            var a=frozen?HistoricalTemperateRoots.roots(seed,x,z,x+31,z+31,d,c):WoodlandTrees.roots(seed,x,z,x+31,z+31,d,c,0);
            if(frozen)require(a.equals(WoodlandTrees.roots(seed,x,z,x+31,z+31,d,c,0)),"Frozen roots including unexplored coordinates");rootSamples+=a.size();
            var b=WoodlandTrees.roots(seed,x,z,x+31,z+31,d,c,1);
            if(added==null)added=b.stream().filter(v->!a.contains(v)).findFirst().orElse(null);
            if(removed==null)removed=a.stream().filter(v->!b.contains(v)).findFirst().orElse(null);
        }
        require(added!=null&&removed!=null,"Both sides of edge taper have native witnesses");
        Files.writeString(dir.resolve("witness.json"),new com.google.gson.Gson().toJson(Map.of("seed",seed,"added",List.of(added.x(),added.y(),added.z()),"removed",List.of(removed.x(),removed.y(),removed.z()))));
        BorealClearSmoke.lazyView(level,g,random,added);
        legacy.drainage(random);
        int changes=0,subtypeChecks=0;
        for(int z=added.z()-16;z<=added.z()+16;z+=4)for(int x=added.x()-16;x<=added.x()+16;x+=4){
            var before=legacy.getBiomeSource().createUncachedResolver(random);var after=g.getBiomeSource().createUncachedResolver(random);
            require(before.getNoiseBiome(x>>2,0,z>>2).equals(after.getNoiseBiome(x>>2,0,z>>2)),"Biome identity unchanged");
            require(legacy.drainage(random).sample(x,z).equals(d.sample(x,z)),"Terrain/drainage unchanged");
            int m=g.meadowCover(x,z,random),w=LeafLitter.woodlandBase(g.woodlandCover(x,z,random));
            require(m==0||w==0,"Mutually exclusive cover vocabularies");
            int om=legacy.meadowCover(x,z,random),ow=LeafLitter.woodlandBase(legacy.woodlandCover(x,z,random));
            int routedMeadow=originalCover(g,"originalMeadowCover",x,z,random),routedWoodland=originalCover(g,"originalWoodlandCover",x,z,random);
            if(m>0&&routedMeadow>0){require(m==routedMeadow,"Retain routed meadow subtype");subtypeChecks++;}
            if(w>0&&routedWoodland>0){require(w==routedWoodland,"Retain routed woodland subtype");subtypeChecks++;}
            if(m!=om||w!=ow)changes++;
        }
        require(changes>0&&subtypeChecks>0,"Actual floor blend and retained subtype witnesses");
        long baselineNanos=0,candidateNanos=0;int sink=0;
        // Alternating warm queries isolate added cover routing from native startup
        // and chunk IO; this is a bounded regression sample, not hardware FPS.
        for(int round=0;round<8;round++)for(int order=0;order<2;order++){
            boolean candidate=((round+order)&1)==0;var target=candidate?g:legacy;long began=System.nanoTime();
            for(int z=added.z()-16;z<=added.z()+16;z+=4)for(int x=added.x()-16;x<=added.x()+16;x+=4)sink+=target.meadowCover(x,z,random)+target.woodlandCover(x,z,random);
            if(round>1){if(candidate)candidateNanos+=System.nanoTime()-began;else baselineNanos+=System.nanoTime()-began;}
        }
        Blockbranch.LOGGER.info("TRANSITION_COVER_TIMING baselineNanos={} candidateNanos={} ratio={} queries=972 sink={}",baselineNanos,candidateNanos,(double)candidateNanos/baselineNanos,sink);

        var context=StructurePieceSerializationContext.fromLevel(level);
        var construction=new BoundingBox(added.x(),added.y(),added.z(),added.x(),added.y(),added.z());
        for(int transitions=0;transitions<=1;transitions++)for(int crowns=0;crowns<=1;crowns++){
            int definition=1+crowns+2*transitions;
            var piece=new ReimaginedTreeClearPiece(construction,seed,crowns,transitions);
            var tag=piece.createTag(context);require(tag.getIntOr("ClearDefinition",0)==definition,"Independent clear definitions");
            var plan=BorealClearSmoke.plan(piece,g,random);
            Path file=dir.resolve("clear-"+definition+".nbt");NbtIo.writeCompressed(tag,file);
            var restored=BorealClearSmoke.load(context,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()));
            require(plan.equals(BorealClearSmoke.plan(restored,g,random)),"Saved plan retains exact replacement geometry");
            if(transitions==0)require(!plan.containsKey(BlockPos.asLong(added.x(),added.y(),added.z())),"Historical clear excludes new edge stem");
            else{
                require(!plan.isEmpty(),"Transition cut tree exercised");
                var states=new HashMap<Long,net.minecraft.world.level.block.state.BlockState>();
                for(var v:WoodlandTrees.tree(added))states.put(BlockPos.asLong(v.x(),v.y(),v.z()),new CanopyLayer(new int[]{(1<<16)|v.code()}).state(0,TerrainShape.FLOOR+1,0));
                states.put(BlockPos.asLong(added.x(),added.y(),added.z()),Blocks.GOLD_BLOCK.defaultBlockState());
                var a=BorealClearSmoke.replay(level,g,context,tag,states,false,false,dir);
                var b=BorealClearSmoke.replay(level,g,context,tag,states,true,true,dir);
                require(a.equals(b),"Partial clear NBT reload and reversed chunk order agree");
                require(a.get(BlockPos.asLong(added.x(),added.y(),added.z())).is(Blocks.GOLD_BLOCK),"Player edit survives clear replay");
            }
        }
        // Real saved chunk descriptors and an actual player edit, followed by a
        // future coordinate on reopen. Cached/saved arrays are never regenerated.
        int cx=added.x()>>4,cz=added.z()>>4;
        Path profileFile=dir.resolve("saved-profile.nbt");var chunk=level.getChunk(cx,cz);
        var profile=((SparseColumn)chunk).blockbranch$terrain().base();
        var profileTag=profile.encode(level.palettedContainerFactory());
        var edit=new BlockPos(added.x(),added.y()+30,added.z());
        if(reload){
            require(profileTag.equals(NbtIo.readCompressed(profileFile,NbtAccounter.unlimitedHeap())),"Saved canopy and profile arrays exact on reopen");
            require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Actual saved player edit survives reopen");
            var future=level.getChunk(cx+12,cz+12);var expected=g.profile(future,random);
            require(((SparseColumn)future).blockbranch$terrain().base().encode(level.palettedContainerFactory()).equals(expected.encode(level.palettedContainerFactory())),"Unexplored chunk uses saved transition definition");
        }else{NbtIo.writeCompressed(profileTag,profileFile);level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),2);}
        Blockbranch.LOGGER.info("FOREST_GRASSLAND_NATIVE_PASS reload={} historicalRoots={} floorChanges={} subtypeChecks={} added={} removed={}",reload,rootSamples,changes,subtypeChecks,added,removed);
    }
    private static int originalCover(RealisticGenerator g,String name,int x,int z,net.minecraft.world.level.levelgen.RandomState random)throws Exception{
        var method=RealisticGenerator.class.getDeclaredMethod(name,int.class,int.class,net.minecraft.world.level.levelgen.RandomState.class);method.setAccessible(true);return(int)method.invoke(g,x,z,random);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
