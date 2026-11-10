package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Native rooms, resident IDs, loot and player edits are witnesses, not substitutes for visual review. */
public final class MansionWingQualification {
    public static int ground(ServerLevel level,int x,int z){return ((RealisticGenerator)level.getChunkSource().getGenerator()).hutPlanningTerrain(level.getChunkSource().randomState()).drainage().sample(x,z).height();}
    public static Map<String,Object> verify(ServerLevel level,RealisticGenerator generator,ReimaginedMansionPiece piece)throws Exception{
        var tag=piece.createTag(StructurePieceSerializationContext.fromLevel(level));
        int[] c=tag.getIntArray("Core").orElseThrow();
        var core=new BoundingBox(c[0],c[1],c[2],c[3],c[4],c[5]);
        int route=ReimaginedRoutes.mansion(level,piece);
        int service=ReimaginedRoutes.servicePassage(level,piece,Boolean.getBoolean("blockbranch.mansionVisualReopen"));
        var digest=java.security.MessageDigest.getInstance("SHA-256");int chests=0;
        for(var pos:BlockPos.betweenClosed(core.minX(),core.minY(),core.minZ(),core.maxX(),core.maxY(),core.maxZ())){
            digest.update(architecturalState(level.getBlockState(pos)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(level.getBlockEntity(pos) instanceof ChestBlockEntity)chests++;
        }
        if(chests==0)throw new AssertionError("Native mansion chest witnesses");
        var report=new LinkedHashMap<String,Object>();report.put("nativeCoreHash",HexFormat.of().formatHex(digest.digest()));report.put("nativeChests",chests);report.put("routeCells",route);report.put("serviceRouteCells",service);doorways(tag,level::getBlockState);report.put("wingDoorways",4);
        if(!Boolean.getBoolean("blockbranch.mansionVisualBaseline")){report.put("clippedReplays",replays(level,piece));report.put("codec",codec(level,generator));if(tag.getIntOr("WingDefinition",0)==1)report.put("approachRouteCells",approaches(level,piece));}
        return report;
    }
    public static void savedEdits(ServerLevel level,ReimaginedMansionPiece piece,Path dir,boolean reopen)throws Exception{
        var marker=dir.resolve("player-edit.txt");var box=piece.getBoundingBox();var edit=new BlockPos(box.minX()+1,box.minY()+1,box.minZ()+1);
        if(reopen){if(!level.getBlockState(edit).is(Blocks.OAK_PLANKS))throw new AssertionError("Saved foundation repair");}
        else{level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);Files.writeString(marker,edit.toShortString());}
        var residents=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1).inflate(64)).stream().filter(e->{String id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();return id.equals("evoker")||id.equals("vindicator")||id.equals("allay");}).map(e->e.getUUID().toString()).sorted().toList();
        if(residents.isEmpty())throw new AssertionError("Native occupied mansion residents");
        var residentFile=dir.resolve("resident-ids.json");var json=new com.google.gson.Gson();
        if(reopen&&Files.exists(residentFile)){if(!json.toJsonTree(residents).equals(com.google.gson.JsonParser.parseString(Files.readString(residentFile))))throw new AssertionError("Native resident identities preserved");}
        else if(!reopen)Files.writeString(residentFile,json.toJson(residents));
        int chests=0;for(var pos:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()))if(level.getBlockEntity(pos) instanceof ChestBlockEntity chest){chests++;if(reopen){if(!chest.isEmpty()||chest.getLootTable()!=null)throw new AssertionError("Consumed loot persisted");}else{chest.unpackLootTable(null);chest.clearContent();chest.setChanged();}}
        if(chests==0)throw new AssertionError("Native loot witnesses");Files.writeString(dir.resolve(reopen?"reopen-edit-result.txt":"edit-result.txt"),"PASS consumed native loot and player repair");
    }
    private static void doorways(net.minecraft.nbt.CompoundTag tag,java.util.function.Function<BlockPos,BlockState> state){
        int[] c=tag.getIntArray("Core").orElseThrow();int floor=tag.getIntOr("Floor",0);
        for(int[] wing:new int[][]{{c[0]-20,c[2]+8},{c[3]+12,c[5]-28}})for(int edge:new int[]{0,8}){
            int inward=edge==0?1:-1;
            // Full-width threshold and the one-block drop to the original interior floor.
            for(int z=3;z<=5;z++)for(int offset:new int[]{0,inward}){
                int x=wing[0]+edge+offset,y=floor+(offset==0?1:0);var foot=new BlockPos(x,y,wing[1]+z);
                if(!net.minecraft.world.level.block.Block.isShapeFullBlock(state.apply(foot.below()).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,foot.below())))throw new AssertionError("Wing doorway full support "+foot);
                for(var pos:List.of(foot,foot.above()))if(!state.apply(pos).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,pos).isEmpty())throw new AssertionError("Wing doorway two-block clearance "+pos);
            }
            // The exterior landing meets the threshold without an extra jump or low canopy.
            if(tag.getIntOr("WingDefinition",0)==1)for(int z=3;z<=4;z++){
                var foot=new BlockPos(wing[0]+edge-inward,floor+1,wing[1]+z);
                if(!net.minecraft.world.level.block.Block.isShapeFullBlock(state.apply(foot.below()).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,foot.below())))throw new AssertionError("Wing landing support "+foot);
                for(var pos:List.of(foot,foot.above()))if(!state.apply(pos).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,pos).isEmpty())throw new AssertionError("Wing landing clearance "+pos);
            }
        }
    }

    private static int approaches(ServerLevel level,ReimaginedMansionPiece piece){
        var tag=piece.createTag(StructurePieceSerializationContext.fromLevel(level));int[] c=tag.getIntArray("Core").orElseThrow(),steps=tag.getIntArray("WingSteps").orElseThrow();int floor=tag.getIntOr("Floor",0),total=0,index=0;
        for(int[] wing:new int[][]{{c[0]-20,c[2]+8},{c[3]+12,c[5]-28}})for(int side:new int[]{-1,9}){
            int x=wing[0]+side,z=wing[1],length=steps[index++];var start=new BlockPos(x,floor+1,z+4);if(!stand(level,start))throw new AssertionError("Wing landing headroom/support");
            var visited=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();visited.add(start);queue.add(start);boolean connected=false;
            while(!queue.isEmpty()){
                var p=queue.removeFirst();if(p.getX()==x&&p.getZ()>=z+5+length){connected=true;break;}
                for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,1,-1}){
                    var n=p.offset(d[0],dy,d[1]);if(Math.abs(n.getX()-x)>1||n.getZ()<z+1||n.getZ()>z+16||n.getY()<floor-10||n.getY()>floor+2||visited.contains(n)||!stand(level,n))continue;
                    if(dy>0&&!clear(level,p.above(2)))continue;visited.add(n);queue.add(n);
                }
            }
            if(!connected)throw new AssertionError("Native wing approach reaches surrounding ground "+index);total+=visited.size();
        }return total;
    }
    private static boolean clear(ServerLevel level,BlockPos pos){return level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();}
    private static boolean stand(ServerLevel level,BlockPos pos){return clear(level,pos)&&clear(level,pos.above())&&!clear(level,pos.below());}
    private static Map<String,Object> replays(ServerLevel level,ReimaginedMansionPiece actual)throws Exception{
        var context=StructurePieceSerializationContext.fromLevel(level);var source=actual.createTag(context);int changes=0;
        for(int scenario=0;scenario<4;scenario++){
            var legacy=shifted(source,scenario);legacy.remove("WingDefinition");legacy.remove("WingSteps");var revised=legacy.copy();revised.putInt("WingDefinition",1);revised.putIntArray("WingSteps",source.getIntArray("WingSteps").orElse(new int[]{5,5,5,5}));
            var old=paint(context,legacy,false);var current=paint(context,revised,false);
            doorways(revised,p->current.getOrDefault(p,Blocks.AIR.defaultBlockState()));
            for(int length:new int[]{1,10}){
                var extreme=revised.copy();extreme.putIntArray("WingSteps",new int[]{length,length,length,length});
                var extremeStates=paint(context,extreme,false);
                if(!extremeStates.equals(paint(context,extreme,true)))throw new AssertionError("Extreme stair blueprint clipped order/reload "+length);
                doorways(extreme,p->extremeStates.getOrDefault(p,Blocks.AIR.defaultBlockState()));
            }
            var blocked=new HashMap<>(current);int[] thresholdCore=revised.getIntArray("Core").orElseThrow();var obstruction=new BlockPos(thresholdCore[0]-20,revised.getIntOr("Floor",0)+2,thresholdCore[2]+12);blocked.put(obstruction,Blocks.DARK_OAK_SLAB.defaultBlockState());
            try{doorways(revised,p->blocked.getOrDefault(p,Blocks.AIR.defaultBlockState()));throw new AssertionError("Doorway obstruction control was missed");}catch(AssertionError expected){if(!expected.getMessage().startsWith("Wing doorway two-block clearance"))throw expected;}

            if(!current.equals(paint(context,revised,true))||!old.equals(paint(context,legacy,true)))throw new AssertionError("Clipped order and per-chunk reload");
            if(ReimaginedMansionPiece.TYPE.load(context,legacy).createTag(context).contains("WingDefinition"))throw new AssertionError("Legacy absent opt-in preserved");
            int[] c=legacy.getIntArray("Core").orElseThrow();int floor=legacy.getIntOr("Floor",0);var core=new BoundingBox(c[0],c[1],c[2],c[3],c[4],c[5]);
            var positions=new HashSet<>(old.keySet());positions.addAll(current.keySet());
            for(var pos:positions)if(!Objects.equals(old.get(pos),current.get(pos))){
                if(core.isInside(pos)||!facade(pos,c,floor))throw new AssertionError("Edit outside wing shell/eaves: "+pos);changes++;
            }
            for(int[] wing:new int[][]{{c[0]-20,c[2]+8,22},{c[3]+12,c[5]-28,20}}){
                for(int x=1;x<8;x++)for(int z=1;z<wing[2];z++)for(int y=-1;y<=6;y++){
                    var pos=new BlockPos(wing[0]+x,floor+y,wing[1]+z);if(!Objects.equals(old.get(pos),current.get(pos)))throw new AssertionError("Interior/furnishings changed");
                }
                for(int x:new int[]{0,8})for(int z=3;z<=5;z++)for(int y=1;y<=3;y++){
                    var pos=new BlockPos(wing[0]+x,floor+y,wing[1]+z);if(!Objects.equals(old.get(pos),current.get(pos)))throw new AssertionError("Door headroom changed");
                }
            }
            for(int[] malformed:new int[][]{new int[0],new int[]{1,1,1},new int[]{0,1,1,1},new int[]{1,11,1,1}}){var invalid=revised.copy();invalid.putIntArray("WingSteps",malformed);try{ReimaginedMansionPiece.TYPE.load(context,invalid);throw new AssertionError("Malformed wing steps accepted");}catch(IllegalArgumentException expected){}}
            var legacySteps=legacy.copy();legacySteps.putIntArray("WingSteps",new int[]{1,1,1,1});try{ReimaginedMansionPiece.TYPE.load(context,legacySteps);throw new AssertionError("Legacy wing steps accepted");}catch(IllegalArgumentException expected){}
            var bad=revised.copy();bad.putInt("WingDefinition",2);try{ReimaginedMansionPiece.TYPE.load(context,bad);throw new AssertionError("Future wing definition accepted");}catch(IllegalArgumentException expected){}
        }
        if(changes<1000)throw new AssertionError("Substantial facade change");return Map.of("placements",4,"changedShellCells",changes,"oppositeChunkOrdersAndReload",true,"interiorsNativeCoreDoorsAndCircuitUnchanged",true,"extremeStepLengths",List.of(1,10),"doorwayObstructionControl",true);
    }
    private static boolean facade(BlockPos p,int[] c,int floor){
        int y=p.getY()-floor;
        for(int[] wing:new int[][]{{c[0]-20,c[2]+8,22},{c[3]+12,c[5]-28,20}}){int x=p.getX()-wing[0],z=p.getZ()-wing[1],length=wing[2];
            if(y>=0&&y<=6&&x>=0&&x<=8&&z>=0&&z<=length&&(x==0||x==8||z==0||z==length))return true;
            if(y>=7&&y<=11&&x>=0&&x<=8&&z>=-1&&z<=length+1)return true;
            if((x==-1||x==9)&&((y>=3&&y<=5&&z>=1&&z<=7)||(y>=-12&&y<=3&&z>=3&&z<=14)))return true;
        }return false;
    }
    private static net.minecraft.nbt.CompoundTag shifted(net.minecraft.nbt.CompoundTag source,int scenario){
        var t=source.copy();int[] c=t.getIntArray("Core").orElseThrow();int dx=scenario==0?0:scenario==1?7:8-c[0],dz=scenario==0?0:scenario==1?-5:8-c[2];
        for(int i=0;i<6;i+=3){c[i]+=dx;c[i+2]+=dz;}
        if(scenario==3){int width=c[3]-c[0],depth=c[5]-c[2];c[3]=c[0]+depth;c[5]=c[2]+width;t.putIntArray("Openings",new int[0]);int floor=t.getIntOr("Floor",0);t.putIntArray("Passage",new int[]{c[3]+16,floor,c[5]-11,(c[0]+c[3])/2,floor+2,(c[2]+c[5])/2});}
        else for(String name:List.of("Openings","Passage")){int[] a=t.getIntArray(name).orElse(new int[0]);for(int i=0;i<a.length;i+=3){a[i]+=dx;a[i+2]+=dz;}t.putIntArray(name,a);}
        t.putIntArray("Core",c);int floor=t.getIntOr("Floor",0);t.putIntArray("BB",new int[]{c[0]-21,floor-12,c[2]-15,c[3]+21,Math.max(c[4],floor+18),c[5]+15});return t;
    }
    private static Map<BlockPos,BlockState> paint(StructurePieceSerializationContext context,net.minecraft.nbt.CompoundTag tag,boolean reverse){
        var piece=ReimaginedMansionPiece.TYPE.load(context,tag);var bounds=piece.getBoundingBox();var states=new HashMap<BlockPos,BlockState>();var active=new BoundingBox[]{bounds};
        var world=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(MansionWingQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(object,method,args)->{
            var pos=args!=null&&args.length>0&&args[0] instanceof BlockPos p?p:null;
            if(method.getName().equals("getBlockEntity")){if(!active[0].isInside(pos))throw new AssertionError("Neighbor block entity read");return null;}
            if(method.getName().equals("getBlockState")){if(!active[0].isInside(pos))throw new AssertionError("Neighbor state read");return states.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
            if(method.getName().equals("setBlock")){pos=pos.immutable();if(!active[0].isInside(pos)||!bounds.isInside(pos))throw new AssertionError("Unowned/out-of-bounds write "+pos);states.put(pos,(BlockState)args[1]);return true;}
            throw new AssertionError("Unexpected world access "+method.getName());
        });
        var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)chunks.add(new net.minecraft.world.level.ChunkPos(x,z));if(reverse)Collections.reverse(chunks);
        for(var chunk:chunks){if(reverse)piece=ReimaginedMansionPiece.TYPE.load(context,piece.createTag(context));active[0]=new BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ());piece.postProcess(world,null,null,net.minecraft.util.RandomSource.create(1),active[0],chunk,BlockPos.ZERO);}return states;
    }
    private static Map<String,Object> codec(ServerLevel level,RealisticGenerator generator)throws Exception{
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var meta=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().getAsJsonObject();
        meta.remove("mansion_replacement_definition");meta.remove("sanctuary_replacement_definition");meta.remove("gravel_relief_definition");
        for(int layout=1;layout<=5;layout++)for(int wings=0;wings<=1;wings++)for(int coverage=0;coverage<=1;coverage++)for(int gallery=0;gallery<=1;gallery++){
            var sample=meta.deepCopy();sample.remove("boreal_crown_definition");sample.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            if(wings==0)sample.remove("mansion_wing_definition");else sample.addProperty("mansion_wing_definition",1);
            if(coverage==0)sample.remove("native_overworld_structures");else sample.addProperty("native_overworld_structures",1);
            if(gallery==0)sample.remove("pyramid_gallery_definition");else sample.addProperty("pyramid_gallery_definition",1);
            int schema=(layout==5?160+2*wings:layout==4?130+2*wings:layout==3?118+2*wings:102+2*(layout-1)+4*wings)+coverage+8*gallery;sample.addProperty("shape_version",schema);
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,sample).getOrThrow();
            if(decoded.reimaginedDefinition()!=layout||decoded.pyramidGalleryDefinition()!=gallery||decoded.mansionWingDefinition()!=wings||decoded.overworldDefinition()!=coverage||!RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Lossless independent definitions "+layout+"/"+wings+"/"+coverage);
            var bad=sample.deepCopy();bad.addProperty("shape_version",schema==117?116:117);if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Schema mismatch accepted");
        }
        for(int layout:new int[]{4,5}) {
            var crowned=meta.deepCopy();crowned.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            crowned.addProperty("shape_version",layout==4?142:172);crowned.addProperty("boreal_crown_definition",1);
            crowned.addProperty("mansion_wing_definition",1);crowned.addProperty("pyramid_gallery_definition",1);crowned.addProperty("native_overworld_structures",1);
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,crowned).getOrThrow();
            if(decoded.borealCrownDefinition()!=1||!RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(crowned))throw new AssertionError("Lossless crowned foundation "+layout);
        }
        var future=meta.deepCopy();future.addProperty("mansion_wing_definition",2);if(RealisticGenerator.CODEC.codec().parse(ops,future).result().isPresent())throw new AssertionError("Unknown mansion definition accepted");
        for(String property:new String[]{"blockbranch.mansionVisualPriorJar","blockbranch.mansionVisualCurrentJar"}){
            String jar=System.getProperty(property,"");if(jar.isEmpty())continue;
            boolean current=property.endsWith("CurrentJar");
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},MansionWingQualification.class.getClassLoader()){
                // Site codecs must come from the actual archived binary as well as the generator.
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                    if(!name.startsWith("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.ReimaginedSites")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource")&&!name.equals("blockbranch.realistic.StructureGenerationSchema"))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }){
                var old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                var legacy=meta.deepCopy();legacy.remove("boreal_crown_definition");legacy.remove("mansion_wing_definition");legacy.remove("pyramid_gallery_definition");legacy.addProperty("native_overworld_structures",1);legacy.getAsJsonObject("reimagined_layouts").addProperty("definition",current?2:1);legacy.addProperty("shape_version",current?105:103);
                if(old.parse(ops,legacy).result().isEmpty())throw new AssertionError("Actual archived reader accepts its legacy "+(current?105:103));
                var revised=legacy.deepCopy();revised.getAsJsonObject("reimagined_layouts").addProperty("definition",2);revised.addProperty("shape_version",109);revised.addProperty("mansion_wing_definition",1);
                if(old.parse(ops,revised).result().isPresent())throw new AssertionError("Actual archived reader must reject109: "+(current?105:103));
                revised.addProperty("pyramid_gallery_definition",1);revised.addProperty("shape_version",117);
                if(old.parse(ops,revised).result().isPresent())throw new AssertionError("Actual archived reader must reject117: "+(current?105:103));
            }
        }
        return Map.of("savedDefinitionRoundTrips",42,"prior103ReaderChecked",!System.getProperty("blockbranch.mansionVisualPriorJar","").isEmpty(),"current105ReaderChecked",!System.getProperty("blockbranch.mansionVisualCurrentJar","").isEmpty(),"unknownAndMismatchedRejected",true);
    }
    public static BlockState architecturalState(BlockState state){
        if(state.getBlock() instanceof DoorBlock)return state.setValue(DoorBlock.OPEN,false).setValue(DoorBlock.POWERED,false);
        return state;
    }
}
