package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;

/** Real native core, full-width flooded thresholds, chunk replay and saved-world witnesses. */
public final class SanctuaryQualification {
    public static Map<String,Object> verify(ServerLevel level,ReimaginedSanctuaryPiece piece,StructureStart start,Path dir,boolean reopen)throws Exception {
        var definition=(RealisticGenerator)level.getChunkSource().getGenerator();
        if(styled(level,piece)!=(definition.reimaginedDefinition()>=3))throw new AssertionError("Sanctuary appearance dispatch matches saved generator definition");
        int routes=ReimaginedRoutes.sanctuary(level,piece);
        int doors=doorways(piece,level::getBlockState,styled(level,piece));
        var core=(OceanMonumentPieces.MonumentBuilding)start.getPieces().getFirst();
        var box=core.getBoundingBox();
        var digest=java.security.MessageDigest.getInstance("SHA-256");int gold=0,sponges=0;
        for(var p:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())) {
            var state=level.getBlockState(p);digest.update((state+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(state.is(Blocks.GOLD_BLOCK))gold++;if(state.is(Blocks.WET_SPONGE))sponges++;
        }
        if(gold!=8)throw new AssertionError("Original monument eight gold blocks: "+gold);
        var elders=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1).inflate(8)).stream().filter(m->m.getType()==net.minecraft.world.entity.EntityTypes.ELDER_GUARDIAN).map(m->m.getUUID().toString()).sorted().toList();
        if(elders.size()!=3)throw new AssertionError("Exactly three original elder guardians: "+elders);
        var record=dir.resolve("elders.json");var gson=new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        if(reopen){if(!com.google.gson.JsonParser.parseString(Files.readString(record)).equals(gson.toJsonTree(elders)))throw new AssertionError("Saved elder UUIDs preserved");}
        else Files.writeString(record,gson.toJson(elders));
        var edit=piece.local(-12,-20,-12);
        if(reopen){if(!level.getBlockState(edit).is(Blocks.OAK_PLANKS))throw new AssertionError("Saved player foundation repair");}
        else level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);
        var report=new LinkedHashMap<String,Object>();report.put("ringRouteCells",routes);report.put("fullWidthThresholdCells",doors);report.put("nativeCoreHash",HexFormat.of().formatHex(digest.digest()));report.put("nativeGold",gold);report.put("nativeWetSponge",sponges);report.put("elderUUIDs",elders);report.put("nativeChildren",ReimaginedRemainingQualification.childBoxes(core));
        var children=dir.resolve("native-children.json");var childRecord=gson.toJsonTree(ReimaginedRemainingQualification.childBoxes(core));
        if(reopen){if(!(Files.exists(children)?com.google.gson.JsonParser.parseString(Files.readString(children)):com.google.gson.JsonParser.parseString(Files.readString(dir.resolve("qualification.json"))).getAsJsonObject().get("nativeChildren")).equals(childRecord))throw new AssertionError("Every native fitted child box preserved on reload");}else Files.writeString(children,gson.toJson(childRecord));
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
        var inside=new BlockPos((box.minX()+box.maxX())/2,box.minY()+5,(box.minZ()+box.maxZ())/2);var outside=piece.local(-10,5,-10);
        if(generator.getMobsAt(level,level.structureManager(),net.minecraft.world.entity.MobCategory.MONSTER,inside).unwrap().stream().noneMatch(e->e.value().type()==net.minecraft.world.entity.EntityTypes.GUARDIAN)||generator.getMobsAt(level,level.structureManager(),net.minecraft.world.entity.MobCategory.MONSTER,outside).unwrap().stream().anyMatch(e->e.value().type()==net.minecraft.world.entity.EntityTypes.GUARDIAN))throw new AssertionError("Native guardian override stays confined to core");
        report.put("connectedSwimmingCells",swimming(level,piece));
        report.put("guardianSpawnBounds",true);report.put("savedPlayerRepair",true);report.put("savedNativeChildGraph",true);
        report.put("clippedReplays",replays(level,piece));
        report.put("codec",codec(level));
        return report;
    }
    private static int swimming(ServerLevel level,ReimaginedSanctuaryPiece piece) {
        var origin=new BlockPos(-9,2,0);var nativeFront=new BlockPos(28,2,1);
        if(!swimClear(level,piece,nativeFront))throw new AssertionError("Actual native monument front swim target "+piece.local(28,2,1));
        var targets=new ArrayList<BlockPos>(List.of(nativeFront,new BlockPos(-9,13,0),new BlockPos(66,2,0),new BlockPos(66,13,0),new BlockPos(0,2,66),new BlockPos(0,13,66)));
        for(int x:new int[]{-10,67})for(int z:new int[]{-10,67})for(int y:new int[]{2,13})targets.add(new BlockPos(x,y,z));
        var seen=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();
        if(!swimClear(level,piece,origin))throw new AssertionError("Actual lower ring swim origin");seen.add(origin);queue.add(origin);
        while(!queue.isEmpty()){var current=queue.removeFirst();for(var direction:net.minecraft.core.Direction.values()) {
            var next=current.relative(direction);
            if(!seen.contains(next)&&swimRegion(next)&&swimClear(level,piece,next)){seen.add(next);queue.addLast(next);}
        }}
        for(var target:targets)if(!seen.contains(target))throw new AssertionError("Actual flooded route reaches every ring, shaft and native entrance "+target);
        return seen.size();
    }
    private static boolean swimClear(ServerLevel level,ReimaginedSanctuaryPiece piece,BlockPos local) {
        for(int dy=0;dy<=1;dy++){var at=piece.local(local.getX(),local.getY()+dy,local.getZ());var state=level.getBlockState(at);if(!state.is(Blocks.WATER)||!state.getCollisionShape(level,at).isEmpty())return false;}return true;
    }
    private static boolean swimRegion(BlockPos p) {
        int x=p.getX(),y=p.getY(),z=p.getZ();if(y<1||y>15)return false;
        if(y<=9&&x>=24&&x<=33&&z>=-12&&z<=3)return true;
        for(int tx:new int[]{-14,63})for(int tz:new int[]{-14,63})if(x>=tx&&x<=tx+8&&z>=tz&&z<=tz+8)return true;
        return (y<=4||y>=12)&&(x>=-12&&x<=69&&(z>=-12&&z<=-6||z>=63&&z<=69)||z>=-6&&z<=63&&(x>=-12&&x<=-6||x>=63&&x<=69));
    }
    private static Map<String,Object> codec(ServerLevel level)throws Exception {
        var matrix=PyramidGalleryQualification.class.getDeclaredMethod("codec",ServerLevel.class);matrix.setAccessible(true);
        var report=new LinkedHashMap<String,Object>();report.put("registryMatrix",matrix.invoke(null,level));
        String jar=System.getProperty("blockbranch.sanctuaryVisualCurrentJar","");
        String reference=System.getProperty("blockbranch.sanctuaryVisualReference","");
        if(!jar.isEmpty()) {
            if(reference.isEmpty())throw new AssertionError("Archived117 reader needs actual baseline generator JSON");
            var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
            var baseline=com.google.gson.JsonParser.parseString(Files.readString(Path.of(reference).resolve("generator.json")));
            var current=RealisticGenerator.CODEC.codec().encodeStart(ops,(RealisticGenerator)level.getChunkSource().getGenerator()).getOrThrow();
            if(!RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,baseline).getOrThrow()).getOrThrow().equals(baseline))throw new AssertionError("Actual saved117 generator stays lossless");
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},SanctuaryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.ReimaginedSites")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource")&&!name.equals("blockbranch.realistic.StructureGenerationSchema")&&!name.equals("blockbranch.realistic.PyramidGallerySchema"))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                var old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                if(old.parse(ops,baseline).result().isEmpty())throw new AssertionError("Actual archived117 reader accepts own saved generator");
                if(current.getAsJsonObject().get("shape_version").getAsInt()!=117&&old.parse(ops,current).result().isPresent())throw new AssertionError("Actual archived117 reader rejects new sanctuary generator");
            }
            report.put("actualArchived117AcceptsOwnGenerator",true);report.put("actualArchived117Rejects129",current.getAsJsonObject().get("shape_version").getAsInt()==129);report.put("actual117LosslessInCurrentReader",true);
        }
        return report;
    }
    /** A standing player fits every doorway; swimming is also possible without squeezing. */
    private static int doorways(ReimaginedSanctuaryPiece piece,Function<BlockPos,BlockState> state,boolean styled) {
        int cells=0;
        for(int[] tower:new int[][]{{-14,-14},{63,-14},{-14,63},{63,63}})for(int y:new int[]{1,12}) {
            for(int a=2;a<=6;a++)for(int dy=0;dy<=3;dy++)for(int edge:new int[]{0,8}) {
                if(styled||y!=12||dy!=0){wet(piece.local(tower[0]+a,y+dy,tower[1]+edge),state);cells++;}wet(piece.local(tower[0]+edge,y+dy,tower[1]+a),state);cells++;
            }
        }
        for(int x=25;x<=32;x++)for(int y=1;y<=9;y++)for(int z=-12;z<=-1;z++){wet(piece.local(x,y,z),state);cells++;}
        for(int x:new int[]{24,33})for(int y=1;y<=4;y++)for(int z=-11;z<=-7;z++){wet(piece.local(x,y,z),state);cells++;}
        return cells;
    }
    private static boolean styled(ServerLevel level,ReimaginedSanctuaryPiece piece){return piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getIntOr("SanctuaryStonework",0)>0;}
    private static void wet(BlockPos p,Function<BlockPos,BlockState> state){var s=state.apply(p);if(!s.is(Blocks.WATER)||!s.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p).isEmpty())throw new AssertionError("Sanctuary doorway clearance "+p+" "+s);}
    private static Map<String,Object> replays(ServerLevel level,ReimaginedSanctuaryPiece actual) {
        var context=StructurePieceSerializationContext.fromLevel(level);var saved=actual.createTag(context);int changed=0;
        boolean styled=saved.getIntOr("SanctuaryStonework",0)>0;
        for(int rotation=0;rotation<8;rotation++) {
            int[][] axes={{1,0,0,1},{0,1,-1,0},{-1,0,0,-1},{0,-1,1,0},{1,0,0,-1},{0,1,1,0},{-1,0,0,1},{0,-1,-1,0}};var a=axes[rotation];var tag=saved.copy();
            int oy=saved.getIntArray("Basis").orElseThrow()[1];tag.putIntArray("Basis",new int[]{8,oy,8,a[0],a[1],a[2],a[3]});
            int minX=Integer.MAX_VALUE,minZ=minX,maxX=Integer.MIN_VALUE,maxZ=maxX;
            for(int x:new int[]{-15,72})for(int z:new int[]{-15,72}){int wx=8+x*a[0]+z*a[2],wz=8+x*a[1]+z*a[3];minX=Math.min(minX,wx);maxX=Math.max(maxX,wx);minZ=Math.min(minZ,wz);maxZ=Math.max(maxZ,wz);}
            tag.putIntArray("BB",new int[]{minX,oy-34,minZ,maxX,oy+36,maxZ});
            var p=(ReimaginedSanctuaryPiece)ReimaginedSanctuaryPiece.TYPE.load(context,tag);var current=paint(context,tag,false);
            if(!current.equals(paint(context,tag,true)))throw new AssertionError("Sanctuary opposite chunk order / per-chunk serialization");
            doorways(p,pos->current.getOrDefault(pos,Blocks.AIR.defaultBlockState()),styled);
            var obstruction=new HashMap<>(current);obstruction.put(p.local(-10,14,-14),Blocks.PRISMARINE_BRICKS.defaultBlockState());
            try{doorways(p,pos->obstruction.getOrDefault(pos,Blocks.AIR.defaultBlockState()),styled);throw new AssertionError("Doorway obstruction control missed");}catch(AssertionError expected){if(!expected.getMessage().startsWith("Sanctuary doorway clearance"))throw expected;}
            for(int y=-34;y<=36;y++)for(int z=0;z<=57;z++)for(int x=0;x<=57;x++)if(current.containsKey(p.local(x,y,z)))throw new AssertionError("Native monument core write");
            var old=tag.copy();old.remove("SanctuaryStonework");var legacy=paint(context,old,false);var zero=old.copy();zero.putInt("SanctuaryStonework",0);
            if(!legacy.equals(paint(context,zero,false)))throw new AssertionError("Absent / explicit zero legacy sanctuary definition");
            if(styled){var positions=new HashSet<>(legacy.keySet());positions.addAll(current.keySet());for(var pos:positions){
                    var before=legacy.getOrDefault(pos,Blocks.AIR.defaultBlockState());var after=current.getOrDefault(pos,Blocks.AIR.defaultBlockState());
                    if(!before.equals(after))changed++;
                    int lx=(pos.getX()-8)*a[0]+(pos.getZ()-8)*a[1],lz=(pos.getX()-8)*a[2]+(pos.getZ()-8)*a[3],ly=pos.getY()-oy;
                    boolean allowed=false;
                    for(int[] tower:new int[][]{{-14,-14,22},{63,-14,29},{-14,63,34},{63,63,25}}){
                        boolean crown=lx>=tower[0]-1&&lx<=tower[0]+9&&lz>=tower[1]-1&&lz<=tower[1]+9&&ly>=tower[2]-1&&ly<=tower[2]+2;
                        boolean gate=ly==12&&lx>=tower[0]+2&&lx<=tower[0]+6&&(lz==tower[1]||lz==tower[1]+8);
                        allowed|=crown||gate;
                    }
                    if(!allowed&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(before.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,pos),after.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,pos),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME))throw new AssertionError("Existing sanctuary collision changed "+pos);
                    if(before.is(Blocks.WATER)&&!after.is(Blocks.WATER))throw new AssertionError("Existing flooded sanctuary cell blocked "+pos);
                }
                for(int bad:new int[]{-1,2,Integer.MAX_VALUE}){var t=tag.copy();t.putInt("SanctuaryStonework",bad);reject(context,t);}var malformed=tag.copy();malformed.putString("SanctuaryStonework","1");reject(context,malformed);
                malformed=tag.copy();malformed.putFloat("SanctuaryStonework",1.5F);reject(context,malformed);
                malformed=tag.copy();malformed.putByte("SanctuaryStonework",(byte)1);reject(context,malformed);
                malformed=tag.copy();malformed.putLong("SanctuaryStonework",4294967297L);reject(context,malformed);
            }
        }
        if(styled&&changed==0)throw new AssertionError("New sanctuary stonework changes exterior");
        return Map.of("orthonormalOrientations",8,"changedCells",changed,"oppositeChunkOrdersAndPerChunkReload",true,"nativeCoreNoWrites",true,"legacyAbsentEqualsZero",true,"doorwayObstructionControl",true);
    }
    private static void reject(StructurePieceSerializationContext context,net.minecraft.nbt.CompoundTag tag){try{ReimaginedSanctuaryPiece.TYPE.load(context,tag);throw new AssertionError("Malformed sanctuary definition accepted");}catch(IllegalArgumentException expected){}}
    private static Map<BlockPos,BlockState> paint(StructurePieceSerializationContext context,net.minecraft.nbt.CompoundTag tag,boolean reverse) {
        var piece=ReimaginedSanctuaryPiece.TYPE.load(context,tag);var bounds=piece.getBoundingBox();var states=new HashMap<BlockPos,BlockState>();var active=new BoundingBox[]{bounds};
        var world=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(SanctuaryQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(object,method,args)->{
            var pos=args!=null&&args.length>0&&args[0] instanceof BlockPos p?p:null;
            if(method.getName().equals("getBlockState")){if(!active[0].isInside(pos))throw new AssertionError("Neighbor state read");return states.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
            if(method.getName().equals("setBlock")){pos=pos.immutable();if(!active[0].isInside(pos)||!bounds.isInside(pos))throw new AssertionError("Sanctuary unowned/out-of-bounds write "+pos);states.put(pos,(BlockState)args[1]);return true;}
            throw new AssertionError("Unexpected sanctuary world access "+method.getName());
        });
        var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)chunks.add(new net.minecraft.world.level.ChunkPos(x,z));if(reverse)Collections.reverse(chunks);
        for(var chunk:chunks){if(reverse)piece=ReimaginedSanctuaryPiece.TYPE.load(context,piece.createTag(context));active[0]=new BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ());piece.postProcess(world,null,null,net.minecraft.util.RandomSource.create(1),active[0],chunk,BlockPos.ZERO);}return states;
    }
}
