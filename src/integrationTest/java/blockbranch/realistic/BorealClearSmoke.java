package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.CanopyLayer;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Opt-in native registry, structure NBT, clipped replay and actual chunk-save regression. */
public final class BorealClearSmoke {
    public static void verify(ServerLevel level,RealisticGenerator generator,boolean reload)throws Exception {
        var random=level.getChunkSource().randomState();long seed=random.seed();
        var context=StructurePieceSerializationContext.fromLevel(level);
        Path directory=Path.of(System.getProperty("blockbranch.memorySmokeResult")).toAbsolutePath().getParent();
        Path savedEdits=directory.resolve("boreal-clear-native-edits.nbt");
        if(reload){
            var expected=NbtIo.readCompressed(savedEdits,NbtAccounter.unlimitedHeap());
            require(expected.getLongOr("Seed",0)==seed,"Native edit witness belongs to this world");
            var keys=expected.getLongArray("Positions").orElseThrow();var ids=expected.getIntArray("States").orElseThrow();
            require(keys.length==ids.length&&keys.length>50,"Nontrivial saved native edit witness");
            for(int i=0;i<keys.length;i++)require(level.getBlockState(BlockPos.of(keys[i]))==Block.stateById(ids[i]),"Native cleared floor/player edits survive world reopen at "+BlockPos.of(keys[i]));
            Blockbranch.LOGGER.info("BOREAL_CLEAR_NATIVE_REOPEN_PASS states={}",keys.length);
        }
        priorGeometry();
        var definitions=generatorDefinitions(level,generator);
        var point=BorealWoodlandFixtures.fixture(generator.drainage(random),seed);
        var drainage=generator.hutPlanningTerrain(random).drainage();var climate=new ClimateField(seed);
        var roots=BorealTrees.roots(seed,point.x()-160,point.z()-160,point.x()+160,point.z()+160,drainage,climate);
        var shared=new HashMap<Long,Integer>();
        for(var root:roots)if(ColdUplands.surfaceKind(seed,root.x(),root.z(),drainage,climate)==0)
            for(var key:voxels(BorealTrees.legacyTree(root)).keySet())shared.merge(key,1,Integer::sum);
        WoodlandTrees.Root witness=null;
        for(var root:roots){
            if(root.family()!=0||root.birch()||ColdUplands.surfaceKind(seed,root.x(),root.z(),drainage,climate)>0)continue;
            var old=voxels(BorealTrees.legacyTree(root));var fresh=voxels(BorealTrees.tree(root));
            if(old.keySet().stream().anyMatch(k->!fresh.containsKey(k))&&fresh.keySet().stream().anyMatch(k->!old.containsKey(k))&&old.keySet().stream().anyMatch(k->shared.getOrDefault(k,0)>1)){witness=root;break;}
        }
        require(witness!=null,"Actual native spruce has both omitted historical leaves and appended crown cells");
        var root=witness;var oldTree=voxels(BorealTrees.legacyTree(root));var freshTree=voxels(BorealTrees.tree(root));
        var construction=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());
        var freshPiece=new ReimaginedTreeClearPiece(construction,seed,1);
        var freshTag=freshPiece.createTag(context);require(freshTag.getIntOr("ClearDefinition",0)==2,"Fresh clear definition is explicit");
        var oldTag=freshTag.copy();oldTag.putInt("ClearDefinition",1);
        var oldPiece=load(context,oldTag);
        require(new ReimaginedTreeClearPiece(construction,seed,definitions[0].borealCrownDefinition()).createTag(context).getIntOr("ClearDefinition",0)==1,"Legacy generator selects historical clearing definition");
        require(new ReimaginedTreeClearPiece(construction,seed,definitions[1].borealCrownDefinition()).createTag(context).getIntOr("ClearDefinition",0)==2,"New generator selects revised clearing definition");
        var oldPlan=plan(oldPiece,generator,random);var freshPlan=plan(freshPiece,generator,random);
        require(oldPlan.keySet().equals(oldTree.keySet()),"Definition 1 uses exactly historical cut-tree footprint");
        require(oldPlan.values().stream().anyMatch(v->v!=0),"Actual native retained-tree overlap is exercised");
        require(freshPlan.keySet().containsAll(freshTree.keySet()),"Definition 2 covers current cut-tree footprint");
        require(!oldPlan.keySet().equals(freshPlan.keySet()),"Legacy and fresh clearing plans are intentionally distinct");
        lazyView(level,definitions[1],random,root);
        boolean canopyChanged=false;
        for(int z=(root.z()-5)>>4;z<=(root.z()+5)>>4;z++)for(int x=(root.x()-5)>>4;x<=(root.x()+5)>>4;x++){
            var historical=definitions[0].canopy(x*16,z*16,random);var current=definitions[1].canopy(x*16,z*16,random);
            require(Arrays.equals(historical.encode(),definitions[0].hutPlanningTerrain(random).canopy().at(x*16,z*16).encode()),"Legacy direct/planning crown caches agree");
            require(Arrays.equals(current.encode(),definitions[1].hutPlanningTerrain(random).canopy().at(x*16,z*16).encode()),"New direct/planning crown caches agree");
            canopyChanged|=!Arrays.equals(historical.encode(),current.encode());
        }
        require(canopyChanged,"Old and new generator definitions actually select distinct native crown arrays");
        for(int definition:new int[]{1,2}){
            var tag=definition==1?oldTag:freshTag;
            Path file=directory.resolve("boreal-clear-definition-"+definition+".nbt");
            NbtIo.writeCompressed(tag,file);var read=load(context,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()));
            require(read.createTag(context).getIntOr("ClearDefinition",0)==definition,"Definition retained through actual compressed structure NBT");
            require(plan(read,generator,random).equals(definition==1?oldPlan:freshPlan),"Reload regenerates exact replacement keys and retained-tree values");
        }
        for(int invalid:new int[]{0,5,-1}){var bad=oldTag.copy();bad.putInt("ClearDefinition",invalid);try{load(context,bad);throw new AssertionError("Unknown clear definition accepted");}catch(IllegalArgumentException expected){}}
        long oldOnly=oldTree.keySet().stream().filter(k->!freshTree.containsKey(k)).findFirst().orElseThrow();
        long newOnly=freshTree.keySet().stream().filter(k->!oldTree.containsKey(k)).findFirst().orElseThrow();
        long stone=oldTree.keySet().stream().filter(k->k!=oldOnly&&!WoodlandTrees.logCode(oldTree.get(k))).findFirst().orElseThrow();
        long air=oldTree.keySet().stream().filter(k->k!=oldOnly&&k!=stone).findFirst().orElseThrow();
        var original=states(oldTree);original.put(newOnly,Blocks.BIRCH_LOG.defaultBlockState());original.put(stone,Blocks.GOLD_BLOCK.defaultBlockState());original.put(air,Blocks.AIR.defaultBlockState());
        var forward=replay(level,generator,context,oldTag,original,false,false,directory);
        var reverse=replay(level,generator,context,oldTag,original,true,true,directory);
        require(forward.equals(reverse),"Native definition-1 clipping is order independent across partial NBT reloads");
        expectedEdits(forward,oldPlan,oldOnly,newOnly,stone,air);
        for(int definition:new int[]{1,2}){
            var tag=definition==1?oldTag:freshTag;var footprint=definition==1?oldTree:freshTree;
            var start=states(footprint);var a=replay(level,generator,context,tag,start,false,false,directory);
            var b=replay(level,generator,context,tag,start,true,true,directory);
            require(a.equals(b),"Definition "+definition+" exact replay survives reversed chunks/reloaded partial structure");
            var replacement=definition==1?oldPlan:freshPlan;
            for(var entry:replacement.entrySet())if(start.containsKey(entry.getKey()))require(a.get(entry.getKey())==state(entry.getValue()),"Actual postProcess applies retained-tree replacement code exactly");
        }
        // This executes the production postProcess against actual sparse chunks,
        // then the ordinary server save and the next run verify native persistence.
        for(var e:original.entrySet())level.setBlock(BlockPos.of(e.getKey()),e.getValue(),2);
        var nativeResult=replay(level,generator,context,oldTag,original,false,true,directory,true);
        expectedEdits(nativeResult,oldPlan,oldOnly,newOnly,stone,air);
        var expected=new CompoundTag();expected.putLong("Seed",seed);long[] keys=nativeResult.keySet().stream().mapToLong(Long::longValue).sorted().toArray();int[] ids=new int[keys.length];
        for(int i=0;i<keys.length;i++){var pos=BlockPos.of(keys[i]);require(level.getBlockState(pos)==nativeResult.get(keys[i]),"Actual native chunk matches clipped replay");ids[i]=Block.getId(nativeResult.get(keys[i]));}
        expected.putLongArray("Positions",keys);expected.putIntArray("States",ids);NbtIo.writeCompressed(expected,savedEdits);
        Blockbranch.LOGGER.info("BOREAL_CLEAR_SAVE_REPLAY_PASS reload={} root={} oldCells={} freshCells={} legacyPlan={} freshPlan={} retainedLegacy={} nativeStates={} oldOnly={} newOnly={}",reload,root,oldTree.size(),freshTree.size(),oldPlan.size(),freshPlan.size(),oldPlan.values().stream().filter(v->v!=0).count(),keys.length,BlockPos.of(oldOnly),BlockPos.of(newOnly));
    }

    private static void expectedEdits(Map<Long,BlockState> result,Map<Long,Integer> plan,long oldOnly,long newOnly,long stone,long air){
        require(result.get(oldOnly)==state(plan.get(oldOnly)),"Historical-only leaf receives original clearing replacement");
        require(result.get(newOnly).is(Blocks.BIRCH_LOG),"Tagged player edit outside historical footprint remains untouched");
        require(result.get(stone).is(Blocks.GOLD_BLOCK),"Non-tree player edit inside historical footprint remains untouched");
        require(result.get(air).isAir(),"Previously removed tree cell remains air");
    }
    static Map<Long,BlockState> replay(ServerLevel level,RealisticGenerator generator,StructurePieceSerializationContext context,CompoundTag tag,Map<Long,BlockState> initial,boolean reverse,boolean reload,Path directory)throws Exception{
        return replay(level,generator,context,tag,initial,reverse,reload,directory,false);
    }
    static Map<Long,BlockState> replay(ServerLevel level,RealisticGenerator generator,StructurePieceSerializationContext context,CompoundTag tag,Map<Long,BlockState> initial,boolean reverse,boolean reload,Path directory,boolean nativeChunks)throws Exception{
        var piece=load(context,tag);var bounds=piece.getBoundingBox();var result=new HashMap<>(initial);var active=new BoundingBox[]{bounds};var writes=new HashSet<Long>();
        var world=(WorldGenLevel)Proxy.newProxyInstance(BorealClearSmoke.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},(object,method,args)->{
            if(method.getName().equals("getLevel"))return level;
            var pos=args!=null&&args.length>0&&args[0] instanceof BlockPos p?p:null;
            if(method.getName().equals("getBlockState")){require(active[0].isInside(pos),"No read outside active generation clip");return nativeChunks?level.getBlockState(pos):result.getOrDefault(pos.asLong(),Blocks.AIR.defaultBlockState());}
            if(method.getName().equals("setBlock")){require(active[0].isInside(pos)&&bounds.isInside(pos),"No write outside owned/active clear clip");long key=pos.asLong();result.put(key,(BlockState)args[1]);writes.add(key);if(nativeChunks)level.setBlock(pos,(BlockState)args[1],2);return true;}
            throw new AssertionError("Unexpected clearing world access "+method.getName());
        });
        var chunks=new ArrayList<ChunkPos>();for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)chunks.add(new ChunkPos(x,z));if(reverse)Collections.reverse(chunks);
        for(int i=0;i<chunks.size();i++){
            var chunk=chunks.get(i);active[0]=new BoundingBox(chunk.getMinBlockX(),bounds.minY(),chunk.getMinBlockZ(),chunk.getMaxBlockX(),bounds.maxY(),chunk.getMaxBlockZ());
            var before=new HashMap<>(result);piece.postProcess(world,null,generator,RandomSource.create(1),active[0],chunk,BlockPos.ZERO);
            for(var e:before.entrySet())if(!active[0].isInside(BlockPos.of(e.getKey())))require(result.get(e.getKey())==e.getValue(),"Outside-clip player/tree state unchanged during partial replay");
            if(reload){Path partial=directory.resolve("boreal-clear-partial.nbt");NbtIo.writeCompressed(piece.createTag(context),partial);piece=load(context,NbtIo.readCompressed(partial,NbtAccounter.unlimitedHeap()));}
        }
        require(!writes.isEmpty(),"Actual clearing writes exercised");return result;
    }
    @SuppressWarnings("unchecked") static Map<Long,Integer> plan(ReimaginedTreeClearPiece piece,RealisticGenerator generator,net.minecraft.world.level.levelgen.RandomState random)throws Exception{
        var method=ReimaginedTreeClearPiece.class.getDeclaredMethod("replacements",RealisticGenerator.class,net.minecraft.world.level.levelgen.RandomState.class);method.setAccessible(true);return(Map<Long,Integer>)method.invoke(piece,generator,random);
    }
    static ReimaginedTreeClearPiece load(StructurePieceSerializationContext context,CompoundTag tag){return(ReimaginedTreeClearPiece)ReimaginedTreeClearPiece.TYPE.load(context,tag);}
    static void lazyView(ServerLevel level,RealisticGenerator owner,net.minecraft.world.level.levelgen.RandomState random,WoodlandTrees.Root root)throws Exception{
        // Follow the actual cave-biome callback capture, rather than depending on
        // a cave approach happening to intersect this particular surface stand.
        owner.drainage(random);
        var boundField=ScreeBiomeSource.class.getDeclaredField("bound");boundField.setAccessible(true);var bound=boundField.get(owner.getBiomeSource());
        var cavesField=bound.getClass().getDeclaredField("caves");cavesField.setAccessible(true);var view=capturedView(cavesField.get(bound),0);
        require(view!=null&&view!=owner&&view.borealCrownDefinition()==owner.borealCrownDefinition()&&view.biomeTransitionDefinition()==owner.biomeTransitionDefinition(),"Actual fixed-seed cave callback captures the owner's crown and transition definitions");
        var stateField=RealisticGenerator.class.getDeclaredField("terrainState");stateField.setAccessible(true);
        require(stateField.get(owner)==stateField.get(view),"Lazy callback shares the tested native canopy cache");
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var clean=RealisticGenerator.CODEC.codec().parse(ops,RealisticGenerator.CODEC.codec().encodeStart(ops,owner).getOrThrow()).getOrThrow();
        int chunks=0;
        for(int z=(root.z()-5)>>4;z<=(root.z()+5)>>4;z++)for(int x=(root.x()-5)>>4;x<=(root.x()+5)>>4;x++){
            var fromView=view.canopy(x*16,z*16,random);var expected=clean.canopy(x*16,z*16,random);
            require(Arrays.equals(fromView.encode(),expected.encode()),"Lazy callback view primes the correct crown geometry before direct generation");
            require(Arrays.equals(owner.canopy(x*16,z*16,random).encode(),expected.encode()),"Lazy cache priming cannot change subsequent direct crown generation");chunks++;
        }
        Blockbranch.LOGGER.info("BOREAL_CLEAR_LAZY_VIEW_PASS chunks={} definition={}",chunks,owner.borealCrownDefinition());
    }
    private static RealisticGenerator capturedView(Object lambda,int depth)throws Exception{
        if(lambda==null||depth>2)return null;
        for(var field:lambda.getClass().getDeclaredFields()){
            if(Modifier.isStatic(field.getModifiers()))continue;field.setAccessible(true);var value=field.get(lambda);
            if(value instanceof RealisticGenerator generator)return generator;
            if(value instanceof java.util.function.BiFunction<?,?,?>){var nested=capturedView(value,depth+1);if(nested!=null)return nested;}
        }
        return null;
    }
    private static RealisticGenerator[] generatorDefinitions(ServerLevel level,RealisticGenerator generator)throws Exception{
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().getAsJsonObject();
        var old=encoded.deepCopy();old.remove("biome_transition_definition");old.remove("mansion_replacement_definition");old.remove("sanctuary_replacement_definition");old.remove("boreal_crown_definition");old.remove("gravel_relief_definition");old.addProperty("shape_version",141);old.getAsJsonObject("reimagined_layouts").addProperty("definition",4);
        var legacy=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
        require(legacy.borealCrownDefinition()==0,"Omitted crown definition preserves historical future-chunk generation");
        require(RealisticGenerator.CODEC.codec().encodeStart(ops,legacy).getOrThrow().equals(old),"Historical full-141 generator round trip is lossless and omits opt-in");
        var fresh=old.deepCopy();fresh.addProperty("shape_version",142);fresh.addProperty("boreal_crown_definition",1);
        var whorled=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();
        require(whorled.borealCrownDefinition()==1&&RealisticGenerator.CODEC.codec().encodeStart(ops,whorled).getOrThrow().equals(fresh),"Fresh-142 generator retains explicit crown definition");
        for(int definition:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("boreal_crown_definition",definition);require(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isEmpty(),"Schema142 rejects missing/wrong crown definition");}
        var missing=fresh.deepCopy();missing.remove("boreal_crown_definition");require(RealisticGenerator.CODEC.codec().parse(ops,missing).result().isEmpty(),"Schema142 cannot silently select historical crowns");
        for(int schema:new int[]{140,141,143}){var bad=fresh.deepCopy();bad.addProperty("shape_version",schema);require(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isEmpty(),"New crowns require exact supported foundation schema");}
        String jar=System.getProperty("blockbranch.borealPriorGeneratorJar","");
        if(!jar.isEmpty())try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},BorealClearSmoke.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{if(!name.equals("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.RealisticGenerator$")&&!name.equals("blockbranch.realistic.ScreeBiomeSource")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource$"))return super.loadClass(name,resolve);synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}}
        }){
            var prior=reader.loadClass("blockbranch.realistic.RealisticGenerator");require(prior!=RealisticGenerator.class,"Prior native generator codec is independently loaded");
            var codec=(com.mojang.serialization.MapCodec<?>)prior.getField("CODEC").get(null);
            require(codec.codec().parse(ops,old).result().isPresent(),"Actual prior-main codec supports historical schema141 control");
            require(codec.codec().parse(ops,fresh).result().isEmpty(),"Actual prior-main codec refuses schema142 before world/structure opening");
            Blockbranch.LOGGER.info("BOREAL_CLEAR_PRIOR_CODEC_PASS supported=141 rejected=142");
        }
        return new RealisticGenerator[]{legacy,whorled};
    }
    private static Map<Long,Integer> voxels(List<WoodlandTrees.Voxel> tree){var result=new HashMap<Long,Integer>();for(var v:tree)result.put(BlockPos.asLong(v.x(),v.y(),v.z()),v.code());return result;}
    private static Map<Long,BlockState> states(Map<Long,Integer> voxels){var result=new HashMap<Long,BlockState>();voxels.forEach((key,code)->result.put(key,state(code)));return result;}
    private static BlockState state(int code){return code==0?Blocks.AIR.defaultBlockState():new CanopyLayer(new int[]{(1<<16)|code}).state(0,TerrainShape.FLOOR+1,0);}
    private static void priorGeometry()throws Exception{
        String jar=System.getProperty("blockbranch.borealPriorTreeJar","");if(jar.isEmpty())return;
        try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},BorealClearSmoke.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{if(!name.equals("blockbranch.realistic.BorealTrees"))return super.loadClass(name,resolve);synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}}
        }){
            var prior=reader.loadClass("blockbranch.realistic.BorealTrees");require(prior!=BorealTrees.class,"Actual prior tree class is independently loaded");var tree=prior.getMethod("tree",WoodlandTrees.Root.class);int count=0;
            for(int family:new int[]{0,4,5,6,7,8})for(boolean birch:new boolean[]{false,true})for(int height:new int[]{9,12,16,22})for(int variant:new int[]{0,1,-1,0x12345678}){
                var root=new WoodlandTrees.Root(-17,TerrainShape.FLOOR+100,31,birch,height,variant,family);
                @SuppressWarnings("unchecked") var old=(List<WoodlandTrees.Voxel>)tree.invoke(null,root);
                require(old.equals(BorealTrees.legacyTree(root)),"Frozen definition-1 tree exactly matches actual prior main voxels");count++;
            }
            Blockbranch.LOGGER.info("BOREAL_CLEAR_PRIOR_GEOMETRY_PASS roots={}",count);
        }
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private BorealClearSmoke(){}
}
