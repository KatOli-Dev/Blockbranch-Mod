package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Perimeter routes use collision shapes and full supports; legacy attic headroom is deliberately excluded. */
public final class MansionPerimeterQualification {
    public static Map<String,Object> verify(ServerLevel level,RealisticGenerator generator,ReimaginedMansionPiece piece)throws Exception {
        var report=new LinkedHashMap<String,Object>(MansionWingQualification.verify(level,generator,piece));
        if(!report.get("nativeCoreHash").equals("d32771ce3be4bf3848f4ec9c6672f877d6711e44c3b3713159819074173040da"))throw new AssertionError("Fixed native mansion core differs");
        var tag=piece.createTag(StructurePieceSerializationContext.fromLevel(level));
        if(tag.getIntOr("MansionPerimeter",0)!=(generator.reimaginedDefinition()>=5?1:0))throw new AssertionError("Perimeter piece matches saved generator definition");
        report.putAll(routes(tag,level::getBlockState));
        if(tag.getIntOr("MansionPerimeter",0)==1) {
            int[] tc=tag.getIntArray("Core").orElseThrow();int tf=tag.getIntOr("Floor",0),torches=0,minLight=15;
            for(int tx:new int[]{tc[0]-13,tc[3]+7})for(int tz:new int[]{tc[2]-13,tc[5]+7}) {
                for(int[] lamp:new int[][]{{1,6,3},{5,6,3},{1,13,3},{5,13,3},{3,16,2},{3,16,4}}) {
                    var p=new BlockPos(tx+lamp[0],tf+lamp[1],tz+lamp[2]);var state=level.getBlockState(p);
                    if(!state.is(Blocks.WALL_TORCH)||!state.canSurvive(level,p))throw new AssertionError("Supported tower torch "+p);
                    minLight=Math.min(minLight,level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,p));torches++;
                }
            }
            if(minLight<13)throw new AssertionError("Settled tower torch block light "+minLight);
            report.put("supportedTowerTorches",torches);report.put("minimumTorchBlockLight",minLight);
        }
        report.put("towerAtticRequired",tag.getIntOr("MansionPerimeter",0)==1);
        var dir=Path.of(System.getProperty("blockbranch.mansionPerimeterVisualDirectory"));
        boolean reopen=Boolean.getBoolean("blockbranch.mansionPerimeterVisualReopen");
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        int[] c=tag.getIntArray("Core").orElseThrow();int floor=tag.getIntOr("Floor",0);
        for(int[] wing:new int[][]{{c[0]-21,c[2]+7,10,24},{c[3]+11,c[5]-29,10,22}})
            for(var p:BlockPos.betweenClosed(wing[0],floor-12,wing[1],wing[0]+wing[2],floor+12,wing[1]+wing[3]))digest.update(MansionWingQualification.architecturalState(level.getBlockState(p)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        report.put("wingsHash",HexFormat.of().formatHex(digest.digest()));
        var residentCounts=new TreeMap<String,Long>();
        var box=piece.getBoundingBox();
        level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1).inflate(64)).forEach(e->{String id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();if(List.of("evoker","vindicator","allay").contains(id))residentCounts.merge(id,1L,Long::sum);});
        report.put("nativeResidentCounts",residentCounts);
        var entities=new TreeMap<String,String>();
        for(var p:BlockPos.betweenClosed(c[0],c[1],c[2],c[3],c[4],c[5])) {
            var entity=level.getBlockEntity(p);if(entity!=null)entities.put(p.toShortString(),entity.saveWithFullMetadata(level.registryAccess()).toString());
        }
        Files.writeString(dir.resolve(reopen?"reopen-block-entities.json":"block-entities.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(entities));
        var context=StructurePieceSerializationContext.fromLevel(level);
        var legacyTag=tag.copy();legacyTag.remove("MansionPerimeter");
        var blueprints=new ArrayList<String>();
        for(int scenario=0;scenario<4;scenario++)blueprints.add(blueprintHash(paint(context,shifted(legacyTag,scenario),false)));
        report.put("actual141LegacyBlueprintHashes",blueprints);
        String reference=System.getProperty("blockbranch.mansionPerimeterVisualReference","");
        if(!reference.isEmpty()) {
            var baseline=com.google.gson.JsonParser.parseString(Files.readString(Path.of(reference).resolve("qualification.json"))).getAsJsonObject();
            for(String name:List.of("nativeCoreHash","wingsHash","nativeChests","actual141LegacyBlueprintHashes","nativeResidentCounts"))if(!new com.google.gson.Gson().toJsonTree(report.get(name)).equals(baseline.get(name)))throw new AssertionError("Published141 control differs: "+name);
            if(!reopen&&!Files.readString(Path.of(reference).resolve("block-entities.json")).equals(Files.readString(dir.resolve("block-entities.json"))))throw new AssertionError("Native block entities/loot differ from141");
        }
        if(!Boolean.getBoolean("blockbranch.mansionPerimeterVisualBaseline"))report.put("clippedReplays",replays(level,piece));
        var readers=new ArrayList<Map<String,Object>>();
        String currentJar=System.getProperty("blockbranch.mansionPerimeterVisualCurrentJar","");
        if(!currentJar.isEmpty())readers.add(archivedReader(level,generator,currentJar,reference));
        String priorJar=System.getProperty("blockbranch.mansionPerimeterVisualPriorJar","");
        if(!priorJar.isEmpty())readers.add(archivedReader(level,generator,priorJar,System.getProperty("blockbranch.mansionPerimeterVisualPriorReference",reference)));
        report.put("archivedReaders",readers);
        return report;
    }
    private static Map<String,Object> archivedReader(ServerLevel level,RealisticGenerator generator,String jar,String reference)throws Exception {
        if(reference.isEmpty())throw new AssertionError("Archived reader requires its actual saved generator reference");
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var actual=com.google.gson.JsonParser.parseString(Files.readString(Path.of(reference).resolve("generator.json")));
        int version=actual.getAsJsonObject().get("shape_version").getAsInt();
        if(!RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,actual).getOrThrow()).getOrThrow().equals(actual))throw new AssertionError("Actual published generator remains lossless: "+version);
        boolean newPerimeter=generator.reimaginedDefinition()>=5;
        var current=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow();
        try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},MansionPerimeterQualification.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.ReimaginedSites")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource")&&!name.equals("blockbranch.realistic.StructureGenerationSchema")&&!name.equals("blockbranch.realistic.PyramidGallerySchema")&&!name.equals("blockbranch.realistic.BorealCrownSchema"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        }) {
            var old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
            if(old.parse(ops,actual).result().isEmpty())throw new AssertionError("Archived reader rejects its actual published format: "+version);
            if(newPerimeter&&old.parse(ops,current).result().isPresent())throw new AssertionError("Archived reader accepts new perimeter metadata: "+version);
        }
        return Map.of("referenceSchema",version,"acceptsOwn",true,"currentReaderLossless",true,"newMetadataChecked",newPerimeter,"rejectsNewPerimeter",newPerimeter);
    }
    static Map<String,Object> routes(net.minecraft.nbt.CompoundTag tag,Function<BlockPos,BlockState> state) {
        int[] c=tag.getIntArray("Core").orElseThrow();int f=tag.getIntOr("Floor",0),x0=c[0]-10,x1=c[3]+10,z0=c[2]-10,z1=c[5]+10,mx=(x0+x1)/2,mz=(z0+z1)/2;
        for(int d=-3;d<=3;d++)for(int y=0;y<=5;y++)for(var p:List.of(new BlockPos(mx+d,f+y,z0),new BlockPos(mx+d,f+y,z1),new BlockPos(x0,f+y,mz+d),new BlockPos(x1,f+y,mz+d))) {
            if(!clear(state,p))throw new AssertionError("Ground gate clearance "+p);
            if(y==0&&!support(state,p.below()))throw new AssertionError("Ground gate support "+p);
        }
        for(int i=0;i<9;i++)for(int width=3;width<=5;width++)for(var p:List.of(new BlockPos(x0+width,f+i,z0+4+i),new BlockPos(x1-width,f+i,z1-4-i)))requireStand(state,p,"Broad stair");
        var start=new BlockPos(x0+1,f+9,z0+5);var visited=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();requireStand(state,start,"Upper walk");visited.add(start);queue.add(start);
        while(!queue.isEmpty()){var p=queue.remove();for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){var n=p.offset(d[0],0,d[1]);if(n.getX()<x0-3||n.getX()>x1+3||n.getZ()<z0-3||n.getZ()>z1+3||visited.contains(n)||!stand(state,n))continue;if(n.getX()>x0+3&&n.getX()<x1-3&&n.getZ()>z0+3&&n.getZ()<z1-3)continue;visited.add(n);queue.add(n);}}
        for(var p:List.of(new BlockPos(mx,f+9,z0+1),new BlockPos(mx,f+9,z1-1),new BlockPos(x0+1,f+9,mz),new BlockPos(x1-1,f+9,mz)))if(!visited.contains(p))throw new AssertionError("Upper circuit disconnected "+p);
        var circuit=new ArrayList<BlockPos>();
        for(int x=x0+4;x<=x1-4;x++){circuit.add(new BlockPos(x,f+9,z0+1));circuit.add(new BlockPos(x,f+9,z1-1));}
        for(int z=z0+4;z<=z1-4;z++){circuit.add(new BlockPos(x0+1,f+9,z));circuit.add(new BlockPos(x1-1,f+9,z));}
        for(var p:circuit){requireStand(state,p,"Continuous circuit");if(!visited.contains(p))throw new AssertionError("Continuous circuit disconnected "+p);}
        for(int x:new int[]{x0-3,x1-3})for(int z:new int[]{z0-3,z1-3}) {
            for(int offset=2;offset<=4;offset++)for(var p:List.of(new BlockPos(x,f+9,z+offset),new BlockPos(x+6,f+9,z+offset),new BlockPos(x+offset,f+9,z),new BlockPos(x+offset,f+9,z+6))){requireStand(state,p,"Tower doorway");if(!visited.contains(p))throw new AssertionError("Tower doorway disconnected "+p);}
            for(int y=1;y<=13;y++){var p=new BlockPos(x+1,f+y,z+1);if(!state.apply(p).is(Blocks.LADDER)||!(state.apply(p.above()).is(Blocks.LADDER)||clear(state,p.above())))throw new AssertionError("Tower ladder clearance "+p);}
            if(tag.getIntOr("MansionPerimeter",0)==1){var top=new BlockPos(x+1,f+14,z+1);if(!state.apply(top).is(Blocks.LADDER)||!clear(state,top.above())||!clear(state,top.above(2)))throw new AssertionError("Tower ladder top clearance "+top);}
            if(tag.getIntOr("MansionPerimeter",0)==1)for(var p:List.of(new BlockPos(x+2,f+15,z+1),new BlockPos(x+1,f+15,z+2),new BlockPos(x+2,f+15,z+2)))requireStand(state,p,"Tower attic");
        }
        return Map.of("groundGates",4,"broadStairCells",54,"upperCircuitCells",visited.size(),"towerThresholds",48,"ladderColumns",4);
    }
    private static String blueprintHash(Map<BlockPos,BlockState> states)throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        var sorted=new TreeMap<String,String>();states.forEach((p,s)->sorted.put(p.toShortString(),s.toString()));
        sorted.forEach((p,s)->digest.update((p+"="+s+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return HexFormat.of().formatHex(digest.digest());
    }
    private static boolean clear(Function<BlockPos,BlockState>s,BlockPos p){return s.apply(p).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p).isEmpty();}
    private static boolean support(Function<BlockPos,BlockState>s,BlockPos p){return Block.isShapeFullBlock(s.apply(p).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p));}
    private static boolean stand(Function<BlockPos,BlockState>s,BlockPos p){return clear(s,p)&&clear(s,p.above())&&support(s,p.below());}
    private static void requireStand(Function<BlockPos,BlockState>s,BlockPos p,String label){if(!stand(s,p))throw new AssertionError(label+" support/headroom "+p);}
    private static net.minecraft.nbt.CompoundTag shifted(net.minecraft.nbt.CompoundTag source,int scenario){
        var t=source.copy();int[] c=t.getIntArray("Core").orElseThrow();int dx=scenario==0?0:scenario==1?7:8-c[0],dz=scenario==0?0:scenario==1?-5:8-c[2];
        for(int i=0;i<6;i+=3){c[i]+=dx;c[i+2]+=dz;}
        if(scenario==3){int width=c[3]-c[0],depth=c[5]-c[2];c[3]=c[0]+depth;c[5]=c[2]+width;t.putIntArray("Openings",new int[0]);int floor=t.getIntOr("Floor",0);t.putIntArray("Passage",new int[]{c[3]+16,floor,c[5]-11,(c[0]+c[3])/2,floor+2,(c[2]+c[5])/2});}
        else for(String name:List.of("Openings","Passage")){int[] a=t.getIntArray(name).orElse(new int[0]);for(int i=0;i<a.length;i+=3){a[i]+=dx;a[i+2]+=dz;}t.putIntArray(name,a);}
        t.putIntArray("Core",c);int floor=t.getIntOr("Floor",0);t.putIntArray("BB",new int[]{c[0]-21,floor-12,c[2]-15,c[3]+21,Math.max(c[4],floor+18),c[5]+15});return t;
    }
    private static Map<BlockPos,BlockState> paint(StructurePieceSerializationContext context,net.minecraft.nbt.CompoundTag tag,boolean reverse){
        var piece=ReimaginedMansionPiece.TYPE.load(context,tag);var bounds=piece.getBoundingBox();var states=new HashMap<BlockPos,BlockState>();var active=new BoundingBox[]{bounds};
        var world=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(MansionPerimeterQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(object,method,args)->{
            var pos=args!=null&&args.length>0&&args[0] instanceof BlockPos p?p:null;
            if(method.getName().equals("getBlockEntity")){if(!active[0].isInside(pos))throw new AssertionError("Neighbor block entity read");return null;}
            if(method.getName().equals("getBlockState")){if(!active[0].isInside(pos))throw new AssertionError("Neighbor state read");return states.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
            if(method.getName().equals("setBlock")){pos=pos.immutable();if(!active[0].isInside(pos)||!bounds.isInside(pos))throw new AssertionError("Unowned/out-of-bounds write "+pos);states.put(pos,(BlockState)args[1]);return true;}
            throw new AssertionError("Unexpected world access "+method.getName());
        });
        var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)chunks.add(new net.minecraft.world.level.ChunkPos(x,z));if(reverse)Collections.reverse(chunks);
        for(var chunk:chunks){if(reverse)piece=ReimaginedMansionPiece.TYPE.load(context,piece.createTag(context));active[0]=new BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ());piece.postProcess(world,null,null,net.minecraft.util.RandomSource.create(1),active[0],chunk,BlockPos.ZERO);}return states;
    }
    private static Map<String,Object> replays(ServerLevel level,ReimaginedMansionPiece actual)throws Exception {
        var context=StructurePieceSerializationContext.fromLevel(level);var source=actual.createTag(context);int changes=0;
        for(int scenario=0;scenario<4;scenario++) {
            var oldTag=shifted(source,scenario);oldTag.remove("MansionPerimeter");var newTag=oldTag.copy();newTag.putInt("MansionPerimeter",1);
            if(ReimaginedMansionPiece.TYPE.load(context,oldTag).createTag(context).contains("MansionPerimeter"))throw new AssertionError("Legacy perimeter opt-in appeared");
            for(net.minecraft.nbt.Tag bad:List.of(net.minecraft.nbt.IntTag.valueOf(-1),net.minecraft.nbt.IntTag.valueOf(2),net.minecraft.nbt.FloatTag.valueOf(1.5F),net.minecraft.nbt.ByteTag.valueOf((byte)1),net.minecraft.nbt.LongTag.valueOf(1),net.minecraft.nbt.StringTag.valueOf("1"),new net.minecraft.nbt.IntArrayTag(new int[]{1}))) {
                var malformed=newTag.copy();malformed.put("MansionPerimeter",bad);
                try{ReimaginedMansionPiece.TYPE.load(context,malformed);throw new AssertionError("Malformed perimeter tag accepted "+bad);}catch(IllegalArgumentException expected){}
            }
            var old=paint(context,oldTag,false);var zero=oldTag.copy();zero.putInt("MansionPerimeter",0);if(!old.equals(paint(context,zero,false)))throw new AssertionError("Absent and explicit-zero perimeter differ");var revised=paint(context,newTag,false);
            if(!old.equals(paint(context,oldTag,true))||!revised.equals(paint(context,newTag,true)))throw new AssertionError("Perimeter clipped opposite order/per-chunk reload");
            routes(newTag,p->revised.getOrDefault(p,Blocks.AIR.defaultBlockState()));
            routes(oldTag,p->old.getOrDefault(p,Blocks.AIR.defaultBlockState()));
            int[] c=newTag.getIntArray("Core").orElseThrow();int f=newTag.getIntOr("Floor",0);
            var all=new HashSet<>(old.keySet());all.addAll(revised.keySet());
            for(var p:all)if(!Objects.equals(old.get(p),revised.get(p))) {
                if(!perimeter(p,c,f))throw new AssertionError("Changed native core/wing/interior outside perimeter mask "+p);
                var before=old.getOrDefault(p,Blocks.AIR.defaultBlockState()).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p);
                var after=revised.getOrDefault(p,Blocks.AIR.defaultBlockState()).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p);
                if(!roof(p,c,f)&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(before,after,net.minecraft.world.phys.shapes.BooleanOp.ONLY_SECOND))throw new AssertionError("New collision outside tower roofs "+p);changes++;
            }
            for(var obstruction:List.of(new BlockPos((c[0]+c[3])/2,f+1,c[2]-10),new BlockPos(c[0]-13,f+10,c[2]-10),new BlockPos((c[0]+c[3])/2,f+10,c[2]-9))) {
                var blocked=new HashMap<>(revised);blocked.put(obstruction,Blocks.STONE.defaultBlockState());boolean caught=false;
                try{routes(newTag,p->blocked.getOrDefault(p,Blocks.AIR.defaultBlockState()));}catch(AssertionError expected){caught=true;}if(!caught)throw new AssertionError("Perimeter obstruction negative control missed "+obstruction);
            }
        }
        if(changes<1000)throw new AssertionError("Substantial perimeter change");
        return Map.of("placements",4,"changedCells",changes,"oppositeOrdersAndPerChunkReload",true,"routeObstructionControls",true);
    }
    private static boolean roof(BlockPos p,int[] c,int f){if(p.getY()<f+15||p.getY()>f+18)return false;for(int x:new int[]{c[0]-13,c[3]+7})for(int z:new int[]{c[2]-13,c[5]+7})if(p.getX()>=x-1&&p.getX()<=x+7&&p.getZ()>=z-1&&p.getZ()<=z+7)return true;return false;}
    private static boolean perimeter(BlockPos p,int[] c,int f) {
        int x=p.getX(),z=p.getZ(),y=p.getY()-f,x0=c[0]-10,x1=c[3]+10,z0=c[2]-10,z1=c[5]+10;
        if(x>=c[0]&&x<=c[3]&&z>=c[2]&&z<=c[5]&&p.getY()>=c[1]&&p.getY()<=c[4])return false;
        if(y<-3||y>18)return false;
        for(int tx:new int[]{x0,x1})for(int tz:new int[]{z0,z1}) {
            int a=x-tx+3,b=z-tz+3;
            if(y>=15&&a>=-1&&a<=7&&b>=-1&&b<=7)return true;
            if(y<=14&&a>=0&&a<=6&&b>=0&&b<=6&&(a==0||a==6||b==0||b==6))return true;
            if((y==6||y==13)&&(a==1||a==5)&&b==3)return true;
        }
        if(y>=-3&&y<=7||y==9||y==10) {
            if((z==z0||z==z1)&&x>=x0+4&&x<=x1-4)return true;
            if((x==x0||x==x1)&&z>=z0+4&&z<=z1-4)return true;
        }
        return false;
    }
}
