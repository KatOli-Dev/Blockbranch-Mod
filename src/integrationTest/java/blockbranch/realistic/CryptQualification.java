package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Native monster room, standing routes, saved loot/repairs and immutable clipped shell replay. */
public final class CryptQualification {
    private static final com.google.gson.Gson JSON=new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    public static Map<String,Object> verify(ServerLevel level,ReimaginedLayoutPiece piece,StructureStart start,Path dir,boolean reopen)throws Exception {
        var context=StructurePieceSerializationContext.fromLevel(level);var tag=piece.createTag(context);
        if(piece.kind()!=1||!tag.getBooleanOr("NativeCrypt",false))throw new AssertionError("Actual original native monster room placed once");
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();boolean styled=tag.getIntOr("CryptStonework",0)>0;
        if(styled!=(generator.reimaginedDefinition()>=4))throw new AssertionError("Crypt saved appearance dispatch");
        var report=new LinkedHashMap<String,Object>();report.put("standingRouteCells",routes(piece,p->level.getBlockState(p),true,tag.getIntOr("CaveDirection",0)));report.put("doorwayCells",doorways(piece,p->level.getBlockState(p)));
        var chests=new ArrayList<ChestBlockEntity>();var spawners=new ArrayList<SpawnerBlockEntity>();var digest=java.security.MessageDigest.getInstance("SHA-256");
        for(int y=-1;y<=4;y++)for(int z=-5;z<=5;z++)for(int x=-5;x<=5;x++) {
            var at=piece.local(x,y,z);var state=level.getBlockState(at);digest.update((state+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var entity=state.hasBlockEntity()?level.getBlockEntity(at):null;if(entity instanceof ChestBlockEntity chest)chests.add(chest);if(entity instanceof SpawnerBlockEntity spawner)spawners.add(spawner);
        }
        if(spawners.size()!=1||chests.size()>2)throw new AssertionError("Original native monster-room quotas");
        var spawner=spawners.getFirst();var data=spawner.saveWithoutMetadata(level.registryAccess());var stable=data.copy();stable.remove("Delay");String stableString=stable.toString();
        if(!stableString.contains("minecraft:zombie")&&!stableString.contains("minecraft:skeleton")&&!stableString.contains("minecraft:spider"))throw new AssertionError("Original native monster spawner type");
        var witnesses=new LinkedHashMap<String,Object>();witnesses.put("spawnerConfigurationExceptVolatileDelay",stableString);witnesses.put("chestPositions",chests.stream().map(c->c.getBlockPos().toString()).sorted().toList());
        var witness=dir.resolve("native-witness.json");
        if(reopen){if(!JSON.toJsonTree(witnesses).equals(com.google.gson.JsonParser.parseString(Files.readString(witness))))throw new AssertionError("Native spawner configuration/chest positions preserved");for(var chest:chests)if(chest.getLootTable()!=null||!chest.isEmpty())throw new AssertionError("Consumed native monster-room loot remains consumed");}
        else {Files.writeString(witness,JSON.toJson(witnesses));var tables=new ArrayList<String>();for(var chest:chests){if(chest.getLootTable()==null)throw new AssertionError("Original native monster-room loot table");tables.add(chest.getLootTable().identifier().toString());chest.unpackLootTable(null);chest.clearContent();chest.setChanged();}report.put("originalLootTables",tables);}
        var edit=piece.local(-13,-1,0);if(reopen){if(!level.getBlockState(edit).is(Blocks.OAK_PLANKS))throw new AssertionError("Saved crypt foundation repair");}else level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);
        int direction=tag.getIntOr("CaveDirection",0),dx=direction==0?1:direction==1?-1:0,dz=direction==2?1:direction==3?-1:0;
        var original=new UndergroundStructure(level.getChunkSource().randomState().seed());var terrain=generator.hutPlanningTerrain(level.getChunkSource().randomState()).drainage();var endpoint=piece.local(dx*24,0,dz*24);
        int surface=terrain.sample(endpoint.getX(),endpoint.getZ()).height();if(!original.voidAt(endpoint.getX(),endpoint.getY()+1,endpoint.getZ(),surface)||!original.voidAt(endpoint.getX(),endpoint.getY()+2,endpoint.getZ(),surface))throw new AssertionError("Elected native cave-air endpoint retains immutable admission witness");
        var lightDigest=java.security.MessageDigest.getInstance("SHA-256");for(int y=-1;y<=4;y++)for(int z=-5;z<=5;z++)for(int x=-5;x<=5;x++)lightDigest.update((byte)level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,piece.local(x,y,z)));
        String lightHash=HexFormat.of().formatHex(lightDigest.digest()),coreHash=HexFormat.of().formatHex(digest.digest());String reference=System.getProperty("blockbranch.cryptVisualReference","");
        if(!reference.isEmpty()){var prior=com.google.gson.JsonParser.parseString(Files.readString(Path.of(reference).resolve("qualification.json"))).getAsJsonObject();if(!lightHash.equals(prior.get("nativeCoreBlockLightHash").getAsString()))throw new AssertionError("Native monster room block lighting unchanged");if(!coreHash.equals(prior.get("nativeCoreHash").getAsString()))throw new AssertionError("Actual original native room architecture unchanged");if(!JSON.toJsonTree(witnesses).equals(com.google.gson.JsonParser.parseString(Files.readString(Path.of(reference).resolve("native-witness.json")))))throw new AssertionError("Actual original native spawner configuration/chest positions unchanged");if(!reopen&&!JSON.toJsonTree(report.get("originalLootTables")).equals(prior.get("originalLootTables")))throw new AssertionError("Actual original native loot tables unchanged");}
        if(styled){report.put("supportedNewWallTorches",torches(piece,level::getBlockState));report.put("ceilingBackedCorbels",corbels(piece,level::getBlockState));}report.put("nativeCoreBlockLightHash",lightHash);
        report.put("nativeCoreHash",coreHash);report.put("nativeSpawners",spawners.size());report.put("nativeChests",chests.size());report.put("spawnerNbt",data.toString());report.put("savedSpawnerConfiguration",true);report.put("consumedLootPreserved",reopen);report.put("savedRepair",true);report.put("caveDirection",direction);report.put("naturalCaveEndpoint",endpoint.toString());report.put("clippedReplays",replays(level,piece));
        if(!Boolean.getBoolean("blockbranch.cryptVisualBaseline"))report.put("codec",codec(level,dir));
        return report;
    }
    private static int routes(ReimaginedLayoutPiece p,Function<BlockPos,BlockState> state,boolean actual,int d) {
        int dx=d==0?1:d==1?-1:0,dz=d==2?1:d==3?-1:0;
        var points=new ArrayList<BlockPos>(List.of(new BlockPos(8,0,0),new BlockPos(-10,0,0),new BlockPos(10,0,-10),new BlockPos(0,0,10),new BlockPos(-17,0,0),new BlockPos(0,0,-15),new BlockPos(15,0,11),new BlockPos(dx*24,0,dz*24)));
        if(actual)points.add(new BlockPos(1,0,1));var seen=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();var origin=points.getFirst();if(!stand(p,origin,state))throw new AssertionError("Crypt route origin");seen.add(origin);queue.add(origin);
        while(!queue.isEmpty()){var at=queue.removeFirst();for(int[] delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,1,-1}){var next=at.offset(delta[0],dy,delta[1]);if(next.getX()< -30||next.getX()>30||next.getZ()< -30||next.getZ()>30||next.getY()<0||next.getY()>3||seen.contains(next)||!stand(p,next,state))continue;if(dy>0&&!clear(state.apply(p.local(at.getX(),at.getY()+2,at.getZ()))))continue;seen.add(next);queue.addLast(next);}}
        for(var target:points)if(!seen.contains(target))throw new AssertionError("Crypt standing route to loop, chambers, original room and elected cave "+target+" visited="+seen.size());return seen.size();
    }
    private static boolean clear(BlockState s){return s.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO).isEmpty();}
    private static boolean stand(ReimaginedLayoutPiece p,BlockPos a,Function<BlockPos,BlockState> s){return clear(s.apply(p.local(a.getX(),a.getY(),a.getZ())))&&clear(s.apply(p.local(a.getX(),a.getY()+1,a.getZ())))&&!clear(s.apply(p.local(a.getX(),a.getY()-1,a.getZ())));}
    private static int doorways(ReimaginedLayoutPiece p,Function<BlockPos,BlockState> state){int cells=0;for(int y=0;y<=3;y++)for(int side=-2;side<=1;side++){requireClear(state,p.local(-13,y,side));requireClear(state,p.local(side,y,-13));if(side<1){requireClear(state,p.local(12,y,side+9));cells++;}cells+=2;}for(int x=6;x<=8;x++)for(int y=0;y<=2;y++){requireClear(state,p.local(x,y,0));cells++;}return cells;}
    private static void requireClear(Function<BlockPos,BlockState> state,BlockPos at){if(!clear(state.apply(at)))throw new AssertionError("Crypt doorway clearance "+at+" "+state.apply(at));}
    private static Map<String,Object> replays(ServerLevel level,ReimaginedLayoutPiece actual) {
        var context=StructurePieceSerializationContext.fromLevel(level);var saved=actual.createTag(context);boolean styled=saved.getIntOr("CryptStonework",0)>0;int changed=0;
        int[][] axes={{1,0,0,1},{0,1,-1,0},{-1,0,0,-1},{0,-1,1,0},{1,0,0,-1},{0,1,1,0},{-1,0,0,1},{0,-1,-1,0}};
        for(int direction=0;direction<4;direction++)for(var a:axes){var tag=saved.copy();tag.putBoolean("NativeCrypt",true);tag.putInt("CaveDirection",direction);tag.putInt("OX",8);tag.putInt("OZ",8);tag.putInt("XX",a[0]);tag.putInt("XZ",a[1]);tag.putInt("ZX",a[2]);tag.putInt("ZZ",a[3]);int oy=tag.getIntOr("OY",0);tag.putIntArray("BB",new int[]{-24,oy-20,-24,40,oy+40,40});
            var piece=(ReimaginedLayoutPiece)ReimaginedLayoutPiece.TYPE.load(context,tag);var current=paint(context,tag,false);if(!current.equals(paint(context,tag,true)))throw new AssertionError("Crypt opposite clipped order/per-chunk saved-piece replay");
            doorways(piece,p->current.getOrDefault(p,Blocks.AIR.defaultBlockState()));routes(piece,p->current.getOrDefault(p,Blocks.AIR.defaultBlockState()),false,direction);
            var obstruction=new HashMap<>(current);obstruction.put(piece.local(-13,1,0),Blocks.STONE_BRICKS.defaultBlockState());try{doorways(piece,p->obstruction.getOrDefault(p,Blocks.AIR.defaultBlockState()));throw new AssertionError("Missed crypt doorway obstruction");}catch(AssertionError expected){if(!expected.getMessage().startsWith("Crypt doorway clearance"))throw expected;}
            var old=tag.copy();old.remove("CryptStonework");var legacy=paint(context,old,false);var zero=old.copy();zero.putInt("CryptStonework",0);if(!legacy.equals(paint(context,zero,false)))throw new AssertionError("Crypt absent/zero legacy equivalence");
            for(int y=-1;y<=4;y++)for(int z=-5;z<=5;z++)for(int x=-5;x<=5;x++)if(!Objects.equals(current.get(piece.local(x,y,z)),legacy.get(piece.local(x,y,z))))throw new AssertionError("Prepared original monster-room footprint changed");
            var positions=new HashSet<>(current.keySet());positions.addAll(legacy.keySet());for(var pos:positions){var before=legacy.getOrDefault(pos,Blocks.AIR.defaultBlockState());var after=current.getOrDefault(pos,Blocks.AIR.defaultBlockState());if(!before.equals(after))changed++;
                var beforeShape=before.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,pos);var afterShape=after.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,pos);
                if(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(beforeShape,afterShape,net.minecraft.world.phys.shapes.BooleanOp.ONLY_SECOND))throw new AssertionError("Crypt added collision "+pos);
                int lx=(pos.getX()-8)*a[0]+(pos.getZ()-8)*a[1],lz=(pos.getX()-8)*a[2]+(pos.getZ()-8)*a[3],ly=pos.getY()-oy;
                boolean niche=ly==2&&(lx==-20&&(lz==-5||lz==-4||lz==1||lz==2)||lz==-21&&(lx==-3||lx==-2||lx==3||lx==4));
                boolean corbel=(ly==2||ly==3)&&(lx==-10&&lz==-8||lx==10&&lz==8)||ly==4&&(lx==-10&&lz==-9||lx==10&&lz==9);
                if(!niche&&!corbel&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(beforeShape,afterShape,net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME))throw new AssertionError("Crypt collision changed outside existing wall niches "+pos);
                if(before.hasProperty(SlabBlock.TYPE)&&(!after.hasProperty(SlabBlock.TYPE)||before.getValue(SlabBlock.TYPE)!=after.getValue(SlabBlock.TYPE)||before.getValue(SlabBlock.WATERLOGGED)!=after.getValue(SlabBlock.WATERLOGGED)))throw new AssertionError("Crypt original tomb slab shape/waterlogging changed");
            }
            if(styled){torches(piece,p->current.getOrDefault(p,Blocks.AIR.defaultBlockState()));corbels(piece,p->current.getOrDefault(p,Blocks.AIR.defaultBlockState()));for(int value:new int[]{-1,2,Integer.MAX_VALUE}){var bad=tag.copy();bad.putInt("CryptStonework",value);reject(context,bad);}var bad=tag.copy();bad.putString("CryptStonework","1");reject(context,bad);bad=tag.copy();bad.putFloat("CryptStonework",1.5F);reject(context,bad);bad=tag.copy();bad.putByte("CryptStonework",(byte)1);reject(context,bad);bad=tag.copy();bad.putLong("CryptStonework",4294967297L);reject(context,bad);
                var noncrypt=tag.copy();noncrypt.remove("CryptStonework");noncrypt.putInt("Kind",2);noncrypt.putInt("Width",1);noncrypt.putInt("Depth",1);
                int minX=Integer.MAX_VALUE,minZ=minX,maxX=Integer.MIN_VALUE,maxZ=maxX;for(int x:new int[]{-26,15})for(int z:new int[]{-26,15}){int wx=8+x*a[0]+z*a[2],wz=8+x*a[1]+z*a[3];minX=Math.min(minX,wx);maxX=Math.max(maxX,wx);minZ=Math.min(minZ,wz);maxZ=Math.max(maxZ,wz);}noncrypt.putIntArray("BB",new int[]{minX,oy-20,minZ,maxX,oy+40,maxZ});ReimaginedLayoutPiece.TYPE.load(context,noncrypt);noncrypt.putInt("CryptStonework",1);reject(context,noncrypt);
            }
        }
        if(styled&&changed==0)throw new AssertionError("New crypt stonework changes shell");return Map.of("directions",4,"orthonormalBases",8,"changedCells",changed,"oppositeChunkOrdersAndPerChunkReload",true,"nativeFeatureAlreadyPlacedPreserved",true,"nativeCorePreparationUnchanged",true,"standingRoutesAndDoorwayObstructionControl",true,"legacyAbsentEqualsZero",true,"noNewCollisionAndOnlyNichesOrLampCorbelsMayLoseVolume",true);
    }
    private static int torches(ReimaginedLayoutPiece piece,Function<BlockPos,BlockState> state){
        for(int[] local:new int[][]{{-19,3,-6,1,0},{-19,3,3,1,0},{-4,2,-20,0,1},{6,2,-20,0,1}}){
            var at=piece.local(local[0],local[1],local[2]);var value=state.apply(at);if(!value.is(Blocks.WALL_TORCH))throw new AssertionError("Supported crypt wall torch "+at);
            var facing=value.getValue(WallTorchBlock.FACING);var o=piece.local(0,0,0);var forward=piece.local(local[3],0,local[4]);if(facing.getStepX()!=forward.getX()-o.getX()||facing.getStepZ()!=forward.getZ()-o.getZ())throw new AssertionError("Rotated crypt torch facing "+at);
            var support=at.relative(facing.getOpposite());if(!state.apply(support).isFaceSturdy(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,support,facing))throw new AssertionError("Crypt torch supported inside owned clip "+at);
        }return 4;
    }
    private static int corbels(ReimaginedLayoutPiece piece,Function<BlockPos,BlockState> state){
        for(int sign:new int[]{-1,1}){
            for(int y:new int[]{2,3})requireClear(state,piece.local(sign*10,y,sign*8));
            var at=piece.local(sign*10,4,sign*9);var value=state.apply(at);
            if(!value.is(Blocks.STONE_BRICK_STAIRS)||value.getValue(StairBlock.HALF)!=net.minecraft.world.level.block.state.properties.Half.TOP)throw new AssertionError("Crypt ceiling corbel shape");
            var support=at.above();if(!state.apply(support).isFaceSturdy(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,support,Direction.DOWN))throw new AssertionError("Crypt ceiling-backed corbel support");
            var origin=piece.local(0,0,0);var direction=piece.local(0,0,sign);
            var facing=value.getValue(StairBlock.FACING);if(facing.getStepX()!=direction.getX()-origin.getX()||facing.getStepZ()!=direction.getZ()-origin.getZ())throw new AssertionError("Crypt ceiling corbel facing");
            if(!state.apply(piece.local(sign*10,1,sign*8)).is(Blocks.TORCH))throw new AssertionError("Original crypt lamp retained");
        }return 2;
    }
    private static void reject(StructurePieceSerializationContext c,CompoundTag t){try{ReimaginedLayoutPiece.TYPE.load(c,t);throw new AssertionError("Invalid crypt stonework accepted");}catch(IllegalArgumentException expected){}}
    private static Map<BlockPos,BlockState> paint(StructurePieceSerializationContext context,CompoundTag tag,boolean reverse){var piece=(ReimaginedLayoutPiece)ReimaginedLayoutPiece.TYPE.load(context,tag);var bounds=piece.getBoundingBox();var states=new HashMap<BlockPos,BlockState>();var active=new BoundingBox[]{bounds};var world=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(CryptQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(o,m,args)->{var at=args!=null&&args.length>0&&args[0] instanceof BlockPos p?p:null;if(m.getName().equals("getBlockState")){if(!active[0].isInside(at))throw new AssertionError("Neighbor crypt read");return states.getOrDefault(at,Blocks.AIR.defaultBlockState());}if(m.getName().equals("setBlock")){at=at.immutable();if(!active[0].isInside(at)||!bounds.isInside(at))throw new AssertionError("Crypt unowned/out-of-bounds write");states.put(at,(BlockState)args[1]);return true;}throw new AssertionError("Unexpected crypt world access "+m.getName());});var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)chunks.add(new net.minecraft.world.level.ChunkPos(x,z));if(reverse)Collections.reverse(chunks);for(var chunk:chunks){if(reverse)piece=(ReimaginedLayoutPiece)ReimaginedLayoutPiece.TYPE.load(context,piece.createTag(context));if(!piece.createTag(context).getBooleanOr("NativeCrypt",false))throw new AssertionError("NativeCrypt true retained across partial replay");active[0]=new BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ());piece.postProcess(world,null,null,net.minecraft.util.RandomSource.create(1),active[0],chunk,BlockPos.ZERO);}return states;}
    private static Map<String,Object> codec(ServerLevel level,Path dir)throws Exception {
        var method=PyramidGalleryQualification.class.getDeclaredMethod("codec",ServerLevel.class);method.setAccessible(true);var out=new LinkedHashMap<String,Object>();out.put("registryMatrix",method.invoke(null,level));String jar=System.getProperty("blockbranch.cryptVisualCurrentJar","");String reference=System.getProperty("blockbranch.cryptVisualReference","");
        if(!jar.isEmpty()){if(reference.isEmpty())throw new AssertionError("Archived129 reader needs actual generator reference");var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());var old=com.google.gson.JsonParser.parseString(Files.readString(Path.of(reference).resolve("generator.json")));var now=RealisticGenerator.CODEC.codec().encodeStart(ops,(RealisticGenerator)level.getChunkSource().getGenerator()).getOrThrow();if(!RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow()).getOrThrow().equals(old))throw new AssertionError("Actual129 generator lossless");try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},CryptQualification.class.getClassLoader()){@Override protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{if(!n.startsWith("blockbranch.realistic.RealisticGenerator")&&!n.startsWith("blockbranch.realistic.ReimaginedSites")&&!n.startsWith("blockbranch.realistic.ScreeBiomeSource")&&!n.equals("blockbranch.realistic.StructureGenerationSchema")&&!n.equals("blockbranch.realistic.PyramidGallerySchema"))return super.loadClass(n,resolve);synchronized(getClassLoadingLock(n)){var c=findLoadedClass(n);if(c==null)c=findClass(n);if(resolve)resolveClass(c);return c;}}}){var codec=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();if(codec.parse(ops,old).result().isEmpty())throw new AssertionError("Authentic129 reader accepts own generator");if(now.getAsJsonObject().get("shape_version").getAsInt()>129&&codec.parse(ops,now).result().isPresent())throw new AssertionError("Authentic129 reader rejects new crypt generator");}out.put("authentic129AcceptsOwn",true);out.put("authentic129RejectsNew",now.getAsJsonObject().get("shape_version").getAsInt()>129);out.put("actual129Lossless",true);}
        return out;
    }
}
