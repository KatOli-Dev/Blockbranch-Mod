package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native routes, protected temple blocks, consumed loot and repairs complement native views. */
public final class PyramidGalleryQualification {
    public static Map<String,Object> verify(ServerLevel level,ReimaginedLayoutPiece piece)throws Exception {
        int routes=ReimaginedRoutes.verify(level,piece),chests=0,tnt=0,plates=0,archaeology=0;
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        for(int y=-14;y<=16;y++)for(int z=0;z<=20;z++)for(int x=0;x<=20;x++) {
            var pos=piece.local(x,y,z);var state=level.getBlockState(pos);
            digest.update((architecturalState(state)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(level.getBlockEntity(pos) instanceof ChestBlockEntity)chests++;
            if(state.is(Blocks.TNT))tnt++;
            if(state.is(Blocks.STONE_PRESSURE_PLATE))plates++;
            if(state.is(Blocks.SUSPICIOUS_SAND))archaeology++;
        }
        if(chests!=4||tnt!=9||plates!=1||archaeology==0)throw new AssertionError("Protected native pyramid witnesses "+List.of(chests,tnt,plates,archaeology));
        var report=new LinkedHashMap<String,Object>(Map.of("routeCells",routes,"nativeCoreHash",HexFormat.of().formatHex(digest.digest()),"nativeChests",chests,"nativeTNT",tnt,"nativePressurePlates",plates,"archaeologyBlocks",archaeology));
        if(!Boolean.getBoolean("blockbranch.pyramidVisualBaseline")){report.put("clippedReplays",replays(level,piece));report.put("codec",codec(level));}
        var tag=piece.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level));
        if(tag.getIntOr("PyramidGallery",0)==1&&tag.getIntOr("PyramidReplacement",0)==0){portal(piece,tag.getIntOr("EntryHeight",18),level::getBlockState);report.put("surfaceApproachCells",surfaceApproach(level,piece,tag.getIntOr("EntryHeight",18)));}
        return report;
    }
    public static void savedEdits(ServerLevel level,ReimaginedLayoutPiece piece,Path dir,boolean reopen)throws Exception {
        var edit=piece.local(-10,-12,-10);int chests=0;
        if(reopen) {
            if(!Files.readString(dir.resolve("player-edit.txt")).equals(edit.toShortString())||!level.getBlockState(edit).is(Blocks.OAK_PLANKS))throw new AssertionError("Saved player foundation repair");
        } else {level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);Files.writeString(dir.resolve("player-edit.txt"),edit.toShortString());}
        for(int y=-14;y<=16;y++)for(int z=0;z<=20;z++)for(int x=0;x<=20;x++)if(level.getBlockEntity(piece.local(x,y,z)) instanceof ChestBlockEntity chest) {
            chests++;
            if(reopen) {if(chest.getLootTable()!=null||!chest.isEmpty())throw new AssertionError("Consumed native loot persisted");}
            else {if(chest.getLootTable()==null)throw new AssertionError("Native chest loot table");chest.unpackLootTable(null);chest.clearContent();chest.setChanged();}
        }
        if(chests!=4)throw new AssertionError("Four native loot witnesses");
        Files.writeString(dir.resolve(reopen?"reopen-edit-result.txt":"edit-result.txt"),"PASS four consumed native chests and player foundation repair");
    }
    private static int surfaceApproach(ServerLevel level,ReimaginedLayoutPiece piece,int entry){
        var origin=new BlockPos(4,entry+1,-26);var visited=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();visited.add(origin);queue.add(origin);
        if(!surfaceStand(level,piece,origin))throw new AssertionError("Portal approach start");
        while(!queue.isEmpty()){
            var p=queue.removeFirst();if(p.getZ()<=-28)return visited.size();
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,1,-1}){
                var n=p.offset(d[0],dy,d[1]);if(n.getX()<0||n.getX()>8||n.getZ()< -30||n.getZ()>-24||n.getY()<entry-4||n.getY()>entry+4||visited.contains(n)||!surfaceStand(level,piece,n))continue;
                if(dy>0&&!surfaceClear(level,piece,p.above(2)))continue;visited.add(n);queue.add(n);
            }
        }
        throw new AssertionError("Portal landing reaches surrounding desert ground");
    }
    private static boolean surfaceClear(ServerLevel level,ReimaginedLayoutPiece piece,BlockPos local){var pos=piece.local(local.getX(),local.getY(),local.getZ());return level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();}
    private static boolean surfaceStand(ServerLevel level,ReimaginedLayoutPiece piece,BlockPos local){var floor=local.below();var pos=piece.local(floor.getX(),floor.getY(),floor.getZ());return surfaceClear(level,piece,local)&&surfaceClear(level,piece,local.above())&&!surfaceClear(level,piece,floor)&&!level.getBlockState(pos).is(Blocks.CACTUS);}
    private static void portal(ReimaginedLayoutPiece piece,int e,java.util.function.Function<BlockPos,BlockState> state) {
        for(int x=2;x<=6;x++)for(int z=-26;z<=-24;z++) {
            for(int y=e+1;y<=e+3;y++) {
                var at=piece.local(x,y,z);
                if(!state.apply(at).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,at).isEmpty())throw new AssertionError("Pyramid portal clearance "+at);
            }
            if(z<=-25) {
                var at=piece.local(x,e,z);
                if(!net.minecraft.world.level.block.Block.isShapeFullBlock(state.apply(at).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,at)))throw new AssertionError("Pyramid landing full support "+at);
            }
        }
    }
    private static Map<String,Object> replays(ServerLevel level,ReimaginedLayoutPiece actual) {
        var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
        var original=actual.createTag(context);
        // These historical recipes deliberately exercise the pre-replacement gallery definition.
        original.remove("PyramidReplacement");int changes=0,cases=0;
        for(int rotation=0;rotation<4;rotation++)for(int e:new int[]{original.getIntOr("EntryHeight",18),0,32}) {
            var old=original.copy();old.remove("PyramidGallery");old.putInt("EntryHeight",e);old.putInt("OX",8);old.putInt("OZ",8);
            int[][] axes={{1,0,0,1},{0,1,-1,0},{-1,0,0,-1},{0,-1,1,0}};var axis=axes[rotation];
            old.putInt("XX",axis[0]);old.putInt("XZ",axis[1]);old.putInt("ZX",axis[2]);old.putInt("ZZ",axis[3]);
            int minX=Integer.MAX_VALUE,minZ=minX,maxX=Integer.MIN_VALUE,maxZ=maxX,oy=old.getIntOr("OY",0);
            for(int x:new int[]{-26,35})for(int z:new int[]{-26,35}){int wx=8+x*axis[0]+z*axis[2],wz=8+x*axis[1]+z*axis[3];minX=Math.min(minX,wx);maxX=Math.max(maxX,wx);minZ=Math.min(minZ,wz);maxZ=Math.max(maxZ,wz);}
            old.putIntArray("BB",new int[]{minX,oy-20,minZ,maxX,oy+40,maxZ});
            var revised=old.copy();revised.putInt("PyramidGallery",1);
            var legacy=paint(context,old,false);var current=paint(context,revised,false);
            if(!legacy.equals(paint(context,old,true))||!current.equals(paint(context,revised,true)))throw new AssertionError("Pyramid clipped order / per-chunk reload");
            if(ReimaginedLayoutPiece.TYPE.load(context,old).createTag(context).contains("PyramidGallery"))throw new AssertionError("Absent legacy pyramid flag preserved");
            var zero=old.copy();zero.putInt("PyramidGallery",0);if(!legacy.equals(paint(context,zero,false)))throw new AssertionError("Explicit zero pyramid definition");
            var piece=(ReimaginedLayoutPiece)ReimaginedLayoutPiece.TYPE.load(context,revised);
            portal(piece,e,p->current.getOrDefault(p,Blocks.AIR.defaultBlockState()));
            for(int y=-14;y<=16;y++)for(int z=0;z<=20;z++)for(int x=0;x<=20;x++){var p=piece.local(x,y,z);if(!Objects.equals(legacy.get(p),current.get(p)))throw new AssertionError("Native pyramid core changed "+p);}
            var positions=new HashSet<BlockPos>(legacy.keySet());positions.addAll(current.keySet());
            for(var p:positions){
                var a=legacy.getOrDefault(p,Blocks.AIR.defaultBlockState());var b=current.getOrDefault(p,Blocks.AIR.defaultBlockState());
                if(!a.equals(b))changes++;
                int lx=(p.getX()-8)*axis[0]+(p.getZ()-8)*axis[1],lz=(p.getX()-8)*axis[2]+(p.getZ()-8)*axis[3],ly=p.getY()-oy;
                boolean entrance=lx>=0&&lx<=8&&lz>=-26&&lz<=-23&&ly>=-12&&ly<=e+5;
                if(!entrance&&net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(a.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p),b.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME))throw new AssertionError("Existing pyramid collision changed "+p);
            }
            var obstruction=new HashMap<>(current);obstruction.put(piece.local(4,e+2,-25),Blocks.SANDSTONE_SLAB.defaultBlockState());
            try{portal(piece,e,p->obstruction.getOrDefault(p,Blocks.AIR.defaultBlockState()));throw new AssertionError("Portal obstruction control missed");}catch(AssertionError expected){if(!expected.getMessage().startsWith("Pyramid portal clearance"))throw expected;}
            for(int bad:new int[]{-1,2,Integer.MAX_VALUE}){var t=revised.copy();t.putInt("PyramidGallery",bad);reject(context,t);}
            var malformed=revised.copy();malformed.putString("PyramidGallery","1");reject(context,malformed);
            for(int kind:new int[]{1,3}){var t=revised.copy();t.putInt("Kind",kind);reject(context,t);}
            cases++;
        }
        if(changes<1000)throw new AssertionError("Substantial gallery change");
        return Map.of("rotationHeightCases",cases,"changedCells",changes,"oppositeClippedOrdersAndPerChunkReload",true,"nativeCoreAndExistingCollisionUnchanged",true,"portalClearanceObstructionControl",true,"entryHeightExtremes",List.of(0,32));
    }
    private static void reject(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext c,net.minecraft.nbt.CompoundTag t){try{ReimaginedLayoutPiece.TYPE.load(c,t);throw new AssertionError("Malformed pyramid definition accepted");}catch(IllegalArgumentException expected){}}
    private static Map<BlockPos,BlockState> paint(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,net.minecraft.nbt.CompoundTag tag,boolean reverse) {
        var piece=ReimaginedLayoutPiece.TYPE.load(context,tag);var bounds=piece.getBoundingBox();var states=new HashMap<BlockPos,BlockState>();var active=new net.minecraft.world.level.levelgen.structure.BoundingBox[]{bounds};
        var world=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(PyramidGalleryQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(object,method,args)->{
            var pos=args!=null&&args.length>0&&args[0] instanceof BlockPos p?p:null;
            if(method.getName().equals("getBlockState")){if(!active[0].isInside(pos))throw new AssertionError("Neighbor state read");return states.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
            if(method.getName().equals("setBlock")){pos=pos.immutable();if(!active[0].isInside(pos)||!bounds.isInside(pos))throw new AssertionError("Pyramid unowned/out-of-bounds write "+pos);states.put(pos,(BlockState)args[1]);return true;}
            throw new AssertionError("Unexpected pyramid world access "+method.getName());
        });
        var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)chunks.add(new net.minecraft.world.level.ChunkPos(x,z));if(reverse)Collections.reverse(chunks);
        for(var chunk:chunks){if(reverse)piece=ReimaginedLayoutPiece.TYPE.load(context,piece.createTag(context));active[0]=new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ());piece.postProcess(world,null,null,net.minecraft.util.RandomSource.create(1),active[0],chunk,BlockPos.ZERO);}return states;
    }
    public static Map<String,Object> codec(ServerLevel level)throws Exception {
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
        var meta=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().getAsJsonObject();
        meta.remove("mansion_replacement_definition");
        int roundTrips=0;
        var codecSamples=new ArrayList<com.google.gson.JsonObject>();
        for(int layout:new int[]{1,2,3,4,5,7,8,9})for(int coverage=0;coverage<=1;coverage++)for(int gallery=0;gallery<=1;gallery++)for(int mansion=0;mansion<=1;mansion++) {
            var sample=meta.deepCopy();sample.remove("sanctuary_replacement_definition");sample.remove("gravel_relief_definition");sample.remove("boreal_crown_definition");sample.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            if(coverage==0)sample.remove("native_overworld_structures");else sample.addProperty("native_overworld_structures",1);
            if(gallery==0)sample.remove("pyramid_gallery_definition");else sample.addProperty("pyramid_gallery_definition",1);
            if(mansion==0)sample.remove("mansion_wing_definition");else sample.addProperty("mansion_wing_definition",1);
            int schema=(layout==9?240+2*mansion:layout==8?220+2*mansion:layout==7?200+2*mansion:layout==5?160+2*mansion:layout==4?130+2*mansion:layout==3?118+2*mansion:102+2*(layout-1)+4*mansion)+coverage+8*gallery;sample.addProperty("shape_version",schema);
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,sample).getOrThrow();
            if(decoded.mansionWingDefinition()!=mansion||decoded.pyramidGalleryDefinition()!=gallery||decoded.reimaginedDefinition()!=layout||decoded.overworldDefinition()!=coverage||!RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Independent pyramid codec roundtrip");
            roundTrips++;codecSamples.add(sample);
            var bad=sample.deepCopy();bad.addProperty("shape_version",schema==117?116:117);if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Mismatched pyramid schema accepted");
        }
        for(int layout:new int[]{4,5,7,8,9}) {
            var crowned=meta.deepCopy();crowned.remove("sanctuary_replacement_definition");crowned.remove("gravel_relief_definition");crowned.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            crowned.addProperty("shape_version",layout==4?142:layout==5?172:layout==7?212:layout==8?232:252);crowned.addProperty("boreal_crown_definition",1);
            crowned.addProperty("mansion_wing_definition",1);crowned.addProperty("pyramid_gallery_definition",1);crowned.addProperty("native_overworld_structures",1);
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,crowned).getOrThrow();
            if(decoded.borealCrownDefinition()!=1||!RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(crowned))throw new AssertionError("Lossless crowned foundation "+layout);
            roundTrips++;codecSamples.add(crowned);
        }
        for(int layout:new int[]{7,8,9}) {
            var wrapped=meta.deepCopy();wrapped.remove("sanctuary_replacement_definition");wrapped.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            wrapped.addProperty("shape_version",layout==7?213:layout==8?233:253);wrapped.addProperty("gravel_relief_definition",1);
            wrapped.addProperty("boreal_crown_definition",1);wrapped.addProperty("mansion_wing_definition",1);wrapped.addProperty("pyramid_gallery_definition",1);wrapped.addProperty("native_overworld_structures",1);
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,wrapped).getOrThrow();
            if(decoded.gravelReliefDefinition()!=1||!RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(wrapped))throw new AssertionError("Lossless gravel wrapper "+layout);
            roundTrips++;codecSamples.add(wrapped);
            for(int relief:new int[]{-1,0,2}) {
                var bad=wrapped.deepCopy();bad.addProperty("gravel_relief_definition",relief);
                if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Malformed gravel wrapper accepted");
            }
        }
        for(int layout:new int[]{8,9}) {
            var sanctuary=meta.deepCopy();sanctuary.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            sanctuary.addProperty("shape_version",layout==8?234:254);sanctuary.addProperty("sanctuary_replacement_definition",1);sanctuary.addProperty("gravel_relief_definition",1);
            sanctuary.addProperty("boreal_crown_definition",1);sanctuary.addProperty("mansion_wing_definition",1);sanctuary.addProperty("pyramid_gallery_definition",1);sanctuary.addProperty("native_overworld_structures",1);
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,sanctuary).getOrThrow();
            if(decoded.sanctuaryReplacementDefinition()!=1||!RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(sanctuary))throw new AssertionError("Lossless independent sanctuary wrapper "+layout);
            roundTrips++;codecSamples.add(sanctuary);
            for(int definition:new int[]{-1,0,2}){var bad=sanctuary.deepCopy();bad.addProperty("sanctuary_replacement_definition",definition);if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Malformed sanctuary wrapper accepted");}
        }
        var mansion=meta.deepCopy();mansion.getAsJsonObject("reimagined_layouts").addProperty("definition",9);
        mansion.addProperty("shape_version",255);mansion.addProperty("mansion_replacement_definition",1);mansion.addProperty("sanctuary_replacement_definition",1);mansion.addProperty("gravel_relief_definition",1);
        mansion.addProperty("boreal_crown_definition",1);mansion.addProperty("mansion_wing_definition",1);mansion.addProperty("pyramid_gallery_definition",1);mansion.addProperty("native_overworld_structures",1);
        var mansionDecoded=RealisticGenerator.CODEC.codec().parse(ops,mansion).getOrThrow();
        if(mansionDecoded.mansionReplacementDefinition()!=1||!RealisticGenerator.CODEC.codec().encodeStart(ops,mansionDecoded).getOrThrow().equals(mansion))throw new AssertionError("Lossless independent mansion wrapper");
        roundTrips++;codecSamples.add(mansion);
        for(int definition:new int[]{-1,0,2}){var bad=mansion.deepCopy();bad.addProperty("mansion_replacement_definition",definition);if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Malformed mansion wrapper accepted");}
        var absentMansion=mansion.deepCopy();absentMansion.remove("mansion_replacement_definition");if(RealisticGenerator.CODEC.codec().parse(ops,absentMansion).result().isPresent())throw new AssertionError("Mansion255 accepted missing opt-in");
        var textMansion=mansion.deepCopy();textMansion.addProperty("mansion_replacement_definition","invalid");if(RealisticGenerator.CODEC.codec().parse(ops,textMansion).result().isPresent())throw new AssertionError("Mansion255 accepted malformed opt-in");
        for(int saved:new int[]{233,234,253,254,256}){var bad=mansion.deepCopy();bad.addProperty("shape_version",saved);if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Incomplete mansion foundation accepted");}
        for(int layout:new int[]{6,8,10}){var bad=mansion.deepCopy();bad.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Unsupported mansion global layout accepted");}
        var future=meta.deepCopy();future.addProperty("pyramid_gallery_definition",2);if(RealisticGenerator.CODEC.codec().parse(ops,future).result().isPresent())throw new AssertionError("Future pyramid gallery accepted");
        for(int layout:new int[]{0,6,10}) {
            var invalid=meta.deepCopy();invalid.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            if(RealisticGenerator.CODEC.codec().parse(ops,invalid).result().isPresent())throw new AssertionError("Unsupported layout accepted "+layout);
        }
        int current172Readers=0;
        String current172Jar=System.getProperty("blockbranch.jungleReplacementCurrent172Jar","");
        if(!current172Jar.isEmpty()) {
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(current172Jar).toUri().toURL()},PyramidGalleryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                com.mojang.serialization.Codec old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                int published=0,replacement=0;
                for(var sample:codecSamples) {
                    int layout=sample.getAsJsonObject("reimagined_layouts").get("definition").getAsInt();
                    if(layout>=7) {
                        if(old.parse(ops,sample).result().isPresent())throw new AssertionError("Archived current172 reader accepted replacement schema "+sample.get("shape_version"));
                        replacement++;
                    } else {
                        var decoded=old.parse(ops,sample).getOrThrow();
                        if(!old.encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Archived current172 lossless published schema "+sample.get("shape_version"));
                        published++;
                    }
                }
                if(published!=42||replacement!=33)throw new AssertionError("Archived current172 boundary coverage");
                for(int layout:new int[]{6,7}) {
                    var downgrade=meta.deepCopy();downgrade.remove("gravel_relief_definition");downgrade.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);downgrade.addProperty("shape_version",172);
                    if(old.parse(ops,downgrade).result().isPresent())throw new AssertionError("Archived current172 reader accepted unsupported layout "+layout);
                }
                current172Readers++;
            }
        }
        int current212Readers=0;
        String current212Jar=System.getProperty("blockbranch.pyramidReplacementCurrent212Jar","");
        if(!current212Jar.isEmpty()) {
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(current212Jar).toUri().toURL()},PyramidGalleryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                com.mojang.serialization.Codec old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                int published=0,replacement=0;
                for(var sample:codecSamples) {
                    int layout=sample.getAsJsonObject("reimagined_layouts").get("definition").getAsInt();
                    if(layout>=8||sample.has("gravel_relief_definition")||sample.has("sanctuary_replacement_definition")) {
                        if(old.parse(ops,sample).result().isPresent())throw new AssertionError("Archived current212 reader accepted replacement schema "+sample.get("shape_version"));
                        replacement++;
                    } else {
                        var decoded=old.parse(ops,sample).getOrThrow();
                        if(!old.encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Archived current212 lossless published schema "+sample.get("shape_version"));
                        published++;
                    }
                }
                if(published!=51||replacement!=24)throw new AssertionError("Archived current212 boundary coverage");
                for(int layout:new int[]{6,8}) {
                    var downgrade=meta.deepCopy();downgrade.remove("gravel_relief_definition");downgrade.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);downgrade.addProperty("shape_version",212);
                    if(old.parse(ops,downgrade).result().isPresent())throw new AssertionError("Archived current212 reader accepted unsupported layout "+layout);
                }
                current212Readers++;
            }
        }
        int current213Readers=0;
        String current213Jar=System.getProperty("blockbranch.pyramidReplacementCurrent213Jar","");
        if(!current213Jar.isEmpty()) {
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(current213Jar).toUri().toURL()},PyramidGalleryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                com.mojang.serialization.Codec old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                int published=0,replacement=0;
                for(var sample:codecSamples) {
                    int layout=sample.getAsJsonObject("reimagined_layouts").get("definition").getAsInt();
                    if(layout>=8||sample.has("sanctuary_replacement_definition")) {
                        if(old.parse(ops,sample).result().isPresent())throw new AssertionError("Archived current213 reader accepted replacement schema "+sample.get("shape_version"));
                        replacement++;
                    } else {
                        var decoded=old.parse(ops,sample).getOrThrow();
                        if(!old.encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Archived current213 lossless published schema "+sample.get("shape_version"));
                        published++;
                    }
                }
                if(published!=52||replacement!=23)throw new AssertionError("Archived current213 boundary coverage");
                for(int layout:new int[]{6,8}) {
                    var downgrade=meta.deepCopy();downgrade.addProperty("gravel_relief_definition",1);downgrade.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);downgrade.addProperty("shape_version",213);
                    if(old.parse(ops,downgrade).result().isPresent())throw new AssertionError("Archived current213 reader accepted unsupported layout "+layout);
                }
                current213Readers++;
            }
        }
        int current233Readers=0;
        String current233Jar=System.getProperty("blockbranch.cryptReplacementCurrent233Jar","");
        if(!current233Jar.isEmpty()) {
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(current233Jar).toUri().toURL()},PyramidGalleryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                com.mojang.serialization.Codec old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                int published=0,replacement=0;
                for(var sample:codecSamples) {
                    int layout=sample.getAsJsonObject("reimagined_layouts").get("definition").getAsInt();
                    if(layout==9||sample.has("sanctuary_replacement_definition")) {
                        if(old.parse(ops,sample).result().isPresent())throw new AssertionError("Archived current233 reader accepted replacement schema "+sample.get("shape_version"));
                        replacement++;
                    } else {
                        var decoded=old.parse(ops,sample).getOrThrow();
                        if(!old.encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Archived current233 lossless published schema "+sample.get("shape_version"));
                        published++;
                    }
                }
                if(published!=62||replacement!=13)throw new AssertionError("Archived current233 boundary coverage");
                for(int layout:new int[]{6,9}) {
                    var downgrade=meta.deepCopy();downgrade.addProperty("gravel_relief_definition",1);downgrade.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);downgrade.addProperty("shape_version",233);
                    if(old.parse(ops,downgrade).result().isPresent())throw new AssertionError("Archived current233 reader accepted unsupported layout "+layout);
                }
                current233Readers++;
            }
        }
        int current234Readers=0;
        String current234Jar=System.getProperty("blockbranch.cryptReplacementCurrent234Jar","");
        if(!current234Jar.isEmpty()) {
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(current234Jar).toUri().toURL()},PyramidGalleryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                com.mojang.serialization.Codec old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                int published=0,replacement=0;
                for(var sample:codecSamples) {
                    int layout=sample.getAsJsonObject("reimagined_layouts").get("definition").getAsInt();
                    if(layout==9) {
                        if(old.parse(ops,sample).result().isPresent())throw new AssertionError("Archived current234 reader accepted replacement schema "+sample.get("shape_version"));
                        replacement++;
                    } else {
                        var decoded=old.parse(ops,sample).getOrThrow();
                        if(!old.encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Archived current234 lossless published schema "+sample.get("shape_version"));
                        published++;
                    }
                }
                if(published!=63||replacement!=12)throw new AssertionError("Archived current234 boundary coverage");
                for(int layout:new int[]{6,9}) {
                    var downgrade=meta.deepCopy();downgrade.addProperty("gravel_relief_definition",1);downgrade.addProperty("sanctuary_replacement_definition",1);downgrade.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);downgrade.addProperty("shape_version",234);
                    if(old.parse(ops,downgrade).result().isPresent())throw new AssertionError("Archived current234 reader accepted unsupported layout "+layout);
                }
                current234Readers++;
            }
        }
        int current254Readers=0;
        String current254Jar=System.getProperty("blockbranch.mansionReplacementCurrent254Jar","");
        if(!current254Jar.isEmpty()) {
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(current254Jar).toUri().toURL()},PyramidGalleryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                com.mojang.serialization.Codec old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                int published=0,replacement=0;
                for(var sample:codecSamples) {
                    int layout=sample.getAsJsonObject("reimagined_layouts").get("definition").getAsInt();
                    if(sample.has("mansion_replacement_definition")) {
                        if(old.parse(ops,sample).result().isPresent())throw new AssertionError("Archived current254 reader accepted replacement schema "+sample.get("shape_version"));
                        replacement++;
                    } else {
                        var decoded=old.parse(ops,sample).getOrThrow();
                        if(!old.encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Archived current254 lossless published schema "+sample.get("shape_version"));
                        published++;
                    }
                }
                if(published!=74||replacement!=1)throw new AssertionError("Archived current254 boundary coverage");
                for(int layout:new int[]{6,10}) {
                    var downgrade=meta.deepCopy();downgrade.addProperty("gravel_relief_definition",1);downgrade.addProperty("sanctuary_replacement_definition",1);downgrade.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);downgrade.addProperty("shape_version",254);
                    if(old.parse(ops,downgrade).result().isPresent())throw new AssertionError("Archived current254 reader accepted unsupported layout "+layout);
                }
                current254Readers++;
            }
        }
        int readers=0;
        for(String property:new String[]{"blockbranch.pyramidVisualPriorJar","blockbranch.pyramidVisualCurrentJar","blockbranch.pyramidVisualMansionJar"}) {
            String jar=System.getProperty(property,"");if(jar.isEmpty())continue;
            boolean current=property.endsWith("CurrentJar"),mansionReader=property.endsWith("MansionJar");
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},PyramidGalleryQualification.class.getClassLoader()) {
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                    if(!name.startsWith("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.ReimaginedSites")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource")&&!name.equals("blockbranch.realistic.StructureGenerationSchema"))return super.loadClass(name,resolve);
                    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
                }
            }) {
                var old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
                var legacy=meta.deepCopy();legacy.remove("sanctuary_replacement_definition");legacy.remove("boreal_crown_definition");legacy.remove("pyramid_gallery_definition");legacy.remove("mansion_wing_definition");if(mansionReader)legacy.addProperty("mansion_wing_definition",1);legacy.addProperty("native_overworld_structures",1);legacy.getAsJsonObject("reimagined_layouts").addProperty("definition",current||mansionReader?2:1);legacy.addProperty("shape_version",mansionReader?109:current?105:103);
                if(old.parse(ops,legacy).result().isEmpty())throw new AssertionError("Actual archived reader accepts own legacy "+(mansionReader?109:current?105:103));
                var revised=meta.deepCopy();revised.remove("sanctuary_replacement_definition");revised.remove("boreal_crown_definition");revised.addProperty("mansion_wing_definition",1);revised.addProperty("pyramid_gallery_definition",1);revised.addProperty("shape_version",117);revised.getAsJsonObject("reimagined_layouts").addProperty("definition",2);revised.addProperty("native_overworld_structures",1);
                if(old.parse(ops,revised).result().isPresent())throw new AssertionError("Actual archived reader rejects117 "+(mansionReader?109:current?105:103));
                revised.remove("mansion_wing_definition");revised.addProperty("shape_version",113);
                if(old.parse(ops,revised).result().isPresent())throw new AssertionError("Actual archived reader rejects113 "+(mansionReader?109:current?105:103));readers++;
            }
        }
        if(roundTrips!=75)throw new AssertionError("Expected 64 uncrowned, 5 crowned, 3 gravel, 2 sanctuary and 1 mansion wrapper codec cases");
        return Map.of("independentRoundTrips",roundTrips,"actualArchivedReaders",readers,"actualCurrent172Readers",current172Readers,"actualCurrent212Readers",current212Readers,"actualCurrent213Readers",current213Readers,"actualCurrent233Readers",current233Readers,"actualCurrent234Readers",current234Readers,"actualCurrent254Readers",current254Readers,"futureAndMismatchedRejected",true);
    }
    public static BlockState architecturalState(BlockState state) {
        return state.is(Blocks.STONE_PRESSURE_PLATE)?state.setValue(BlockStateProperties.POWERED,false):state;
    }
}
