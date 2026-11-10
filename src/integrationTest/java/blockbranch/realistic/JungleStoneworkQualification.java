package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Clipped production-piece replay, independent of native chunk completion order. */
public final class JungleStoneworkQualification {
    private JungleStoneworkQualification() {}

    public static Map<String,Object> verify(ServerLevel level, RealisticGenerator generator,
                                           ReimaginedLayoutPiece actual) throws Exception {
        var context = StructurePieceSerializationContext.fromLevel(level);
        var original = actual.createTag(context);
        // These replays specifically qualify the historical stonework recipes.
        original.remove("JungleReplacement");
        int changed = 0;
        for (int rotation = 0; rotation < 4; rotation++) {
            var legacy = rotated(original, rotation);
            legacy.remove("JungleStonework");
            var revised = legacy.copy(); revised.putInt("JungleStonework", 2);
            var oldStates = replay(context, legacy, false);
            var newStates = replay(context, revised, false);
            require(newStates.equals(replay(context, revised, true)), "Clipped reverse order and piece reload");
            require(oldStates.equals(replay(context, legacy, true)), "Legacy clipped order and piece reload");
            var oldPiece = (ReimaginedLayoutPiece)ReimaginedLayoutPiece.TYPE.load(context, legacy);
            require(!oldPiece.createTag(context).contains("JungleStonework"), "Legacy absent field stays absent");
            require(bridgeExitClear(oldPiece,p->newStates.getOrDefault(p,Blocks.AIR.defaultBlockState())),"Full-width short-tower exterior doorway clearance, rotation "+rotation);
            // Negative control reproduces the cornice crossing the doorway above its y18 floor.
            var blocked=new HashMap<>(newStates);
            for(int x=-10;x<=-8;x++)blocked.put(oldPiece.local(x,20,5),Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState());
            require(!bridgeExitClear(oldPiece,p->blocked.getOrDefault(p,Blocks.AIR.defaultBlockState())),"Doorway regression detects original bottom-slab obstruction");
            // Every stair, landing and interior cell retains the original state.
            for (int[] tower : new int[][]{{-13,-4,21},{16,13,27}})
                for (int x = 1; x < 8; x++) for (int z = 1; z < 8; z++)
                    for (int y = -1; y < tower[2]; y++) {
                        var pos = oldPiece.local(tower[0]+x,y,tower[1]+z);
                        require(Objects.equals(oldStates.get(pos),newStates.get(pos)), "Unchanged tower interior " + pos);
                    }
            // The native core, trap/loot footprint, and lower route are outside the art edit.
            for (int x = 0; x < 12; x++) for (int z = 0; z < 15; z++) for (int y = -5; y <= 10; y++) {
                var pos = oldPiece.local(x,y,z);
                require(Objects.equals(oldStates.get(pos),newStates.get(pos)), "Unchanged native core footprint");
            }
            var all = new HashSet<>(oldStates.keySet()); all.addAll(newStates.keySet());
            for (var pos : all) if (!Objects.equals(oldStates.get(pos),newStates.get(pos))) changed++;
            var bad = revised.copy(); bad.putInt("JungleStonework", 3);
            try { ReimaginedLayoutPiece.TYPE.load(context,bad); throw new AssertionError("Future stonework accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        require(changed > 1000, "Substantial exterior replacement across rotations");
        require(bridgeExitClear(actual,level::getBlockState),"Native short-tower exterior doorway clearance");
        codec(level,generator);
        return Map.of("rotations",4,"changedExteriorCells",changed,"clippedOrderAndReload",true,
            "stairsLandingsAndCoreUnchanged",true,"exteriorDoorwayRotations",4,"routeCells",ReimaginedRoutes.verify(level,actual),"nativeCore",nativeCore(level,actual));
    }

    private static boolean bridgeExitClear(ReimaginedLayoutPiece piece,java.util.function.Function<BlockPos,BlockState> state){
        for(int x=-10;x<=-8;x++)for(int z=4;z<=6;z++){
            var floor=piece.local(x,18,z);
            if(!net.minecraft.world.level.block.Block.isShapeFullBlock(state.apply(floor).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,floor)))return false;
            for(int y=19;y<=20;y++){var p=piece.local(x,y,z);if(!state.apply(p).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p).isEmpty())return false;}
        }
        return true;
    }

    public static void savedEdits(ServerLevel level,ReimaginedLayoutPiece layout,Path directory,boolean reopen) throws Exception {
        var marker=directory.resolve("player-edit.txt");
        if(reopen&&!Files.exists(marker))return; // Original external save has its own exact architecture witness.
        var edit=layout.local(-13,-8,-4);
        if(!reopen) {
            level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);
            Files.writeString(marker,edit.toShortString());
        } else require(level.getBlockState(edit).is(Blocks.OAK_PLANKS),"Saved player foundation repair retained");
        int chests=0;
        for(int x=0;x<12;x++)for(int z=0;z<15;z++)for(int y=-5;y<=10;y++) {
            if(level.getBlockEntity(layout.local(x,y,z)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                chests++;
                if(reopen)require(chest.isEmpty()&&chest.getLootTable()==null,"Consumed native loot retained on reopen");
                else {chest.unpackLootTable(null);chest.clearContent();chest.setChanged();}
            }
        }
        require(chests==2,"Both native loot witnesses");
        Files.writeString(directory.resolve(reopen?"reopen-edit-result.txt":"edit-result.txt"),"PASS player repair and consumed native loot");
    }

    /** Live native signals change when wildlife crosses tripwire; geometry and all other properties stay exact. */
    public static BlockState architecturalState(BlockState state) {
        if(state.is(Blocks.REDSTONE_WIRE))return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER,0);
        if(state.is(Blocks.TRIPWIRE))return state.setValue(net.minecraft.world.level.block.TripWireBlock.POWERED,false);
        if(state.is(Blocks.TRIPWIRE_HOOK))return state.setValue(net.minecraft.world.level.block.TripWireHookBlock.POWERED,false);
        if(state.is(Blocks.DISPENSER))return state.setValue(net.minecraft.world.level.block.DispenserBlock.TRIGGERED,false);
        return state;
    }

    private static Map<String,Object> nativeCore(ServerLevel level,ReimaginedLayoutPiece actual) throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        int chests=0,dispensers=0,levers=0,pistons=0;
        for(int x=0;x<12;x++)for(int z=0;z<15;z++)for(int y=-5;y<=10;y++) {
            var state=level.getBlockState(actual.local(x,y,z));
            digest.update(architecturalState(state).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(state.is(Blocks.CHEST))chests++;if(state.is(Blocks.DISPENSER))dispensers++;
            if(state.is(Blocks.LEVER))levers++;if(state.is(Blocks.STICKY_PISTON))pistons++;
        }
        require(chests==2&&dispensers==2&&levers==3&&pistons>=2,"Original native trap and loot blocks");
        return Map.of("sha256",HexFormat.of().formatHex(digest.digest()),"chests",chests,
            "dispensers",dispensers,"levers",levers,"stickyPistons",pistons);
    }

    private static CompoundTag rotated(CompoundTag source,int rotation) {
        int[][] basis={{1,0,0,1},{0,1,-1,0},{-1,0,0,-1},{0,-1,1,0}};
        var axes=basis[rotation]; var tag=source.copy();
        tag.putInt("OX",8); tag.putInt("OY",64); tag.putInt("OZ",8);
        tag.putInt("XX",axes[0]);tag.putInt("XZ",axes[1]);tag.putInt("ZX",axes[2]);tag.putInt("ZZ",axes[3]);
        int minX=Integer.MAX_VALUE,minZ=minX,maxX=Integer.MIN_VALUE,maxZ=maxX;
        for(int x:new int[]{-14,28})for(int z:new int[]{-14,29}) {
            int wx=8+x*axes[0]+z*axes[2],wz=8+x*axes[1]+z*axes[3];
            minX=Math.min(minX,wx);maxX=Math.max(maxX,wx);minZ=Math.min(minZ,wz);maxZ=Math.max(maxZ,wz);
        }
        tag.putIntArray("BB",new int[]{minX,44,minZ,maxX,104,maxZ}); return tag;
    }

    private static Map<BlockPos,BlockState> replay(StructurePieceSerializationContext context,
                                                  CompoundTag tag, boolean reverse) {
        var states=new HashMap<BlockPos,BlockState>();
        var piece=ReimaginedLayoutPiece.TYPE.load(context,tag); var box=piece.getBoundingBox();
        var active=new BoundingBox[]{box};
        var proxy=(WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(
            JungleStoneworkQualification.class.getClassLoader(),new Class[]{WorldGenLevel.class},(object,method,args)->{
                if(method.getName().equals("getBlockState")) {
                    var pos=(BlockPos)args[0];require(active[0].isInside(pos),"No neighboring-chunk reads");
                    return states.getOrDefault(pos,Blocks.AIR.defaultBlockState());
                }
                if(method.getName().equals("setBlock")) {
                    var pos=((BlockPos)args[0]).immutable();
                    require(active[0].isInside(pos)&&box.isInside(pos),"Every write owned and inside admitted bounds");
                    states.put(pos,(BlockState)args[1]);return true;
                }
                throw new AssertionError("Unexpected world access " + method.getName());
            });
        var chunks=new ArrayList<ChunkPos>();
        for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)chunks.add(new ChunkPos(x,z));
        if(reverse)Collections.reverse(chunks);
        for(var chunk:chunks) {
            if(reverse)piece=ReimaginedLayoutPiece.TYPE.load(context,piece.createTag(context));
            active[0]=new BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ());
            piece.postProcess(proxy,null,null,net.minecraft.util.RandomSource.create(1),active[0],chunk,BlockPos.ZERO);
        }
        return states;
    }

    private static void codec(ServerLevel level,RealisticGenerator generator) throws Exception {
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var current=RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().getAsJsonObject();
        require(RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,current).getOrThrow()).getOrThrow().equals(current),"Lossless current definition");
        for(int definition=1;definition<=5;definition++)for(int coverage=0;coverage<=1;coverage++) {
            var meta=current.deepCopy();meta.remove("boreal_crown_definition");meta.remove("mansion_wing_definition");meta.remove("pyramid_gallery_definition");meta.getAsJsonObject("reimagined_layouts").addProperty("definition",definition);
            if(coverage==0)meta.remove("native_overworld_structures");else meta.addProperty("native_overworld_structures",1);
            meta.addProperty("shape_version",definition==5?160+coverage:definition==4?130+coverage:definition==3?118+coverage:definition==2?(coverage==1?105:104):(coverage==1?103:102));
            var decoded=RealisticGenerator.CODEC.codec().parse(ops,meta).getOrThrow();
            require(decoded.reimaginedDefinition()==definition&&decoded.overworldDefinition()==coverage,"Saved explicit definitions");
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(meta),"Lossless saved definition");
            var mismatch=meta.deepCopy();mismatch.remove("boreal_crown_definition");mismatch.addProperty("shape_version",definition==2?103:105);
            require(RealisticGenerator.CODEC.codec().parse(ops,mismatch).error().isPresent(),"Definition downgrade mismatch rejected");
        }
        String prior=System.getProperty("blockbranch.structureVisualPriorJar","");
        if(!prior.isEmpty())try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(prior).toUri().toURL()},JungleStoneworkQualification.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.ReimaginedSites")&&!name.equals("blockbranch.realistic.StructureGenerationSchema")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        }) {
            var codec=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
            var legacy=current.deepCopy();legacy.remove("boreal_crown_definition");legacy.remove("mansion_wing_definition");legacy.remove("pyramid_gallery_definition");legacy.addProperty("shape_version",103);legacy.getAsJsonObject("reimagined_layouts").addProperty("definition",1);
            require(codec.parse(ops,legacy).result().isPresent(),"Actual previous-main reader accepts legacy103");
            var modern=legacy.deepCopy();modern.addProperty("shape_version",105);modern.getAsJsonObject("reimagined_layouts").addProperty("definition",2);
            require(codec.parse(ops,modern).error().isPresent(),"Actual previous-main reader rejects new105");
        }
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
