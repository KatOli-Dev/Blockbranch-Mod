package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.CanopyLayer;
import blockbranch.cubic.SparseColumn;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Native generated-chunk witnesses for the gated mature old-growth-birch crowns on seed 1. Each window is the 5 x 5 chunks
 * the frozen published fixture covers, read from the chunks' own saved canopy (the real generation path).
 * Modes: {@code baseline275} (a fresh world from the gate-absent baseline preset records each window's digest),
 * {@code fresh276} (the bundled preset: gate on, old-growth-birch windows differ, controls do not, logs attached, saved clear plan covers
 * the new footprint, mismatched or malformed schema/definition never loads, edit and signature saved), {@code verify276} (reopen that closed
 * world in a second process) and {@code legacy275} (reopen an authentic schema-275 world written before the gate
 * existed: shape 275, gate absent, its old-growth-birch windows were never explored by the source world and equal the baseline digests, its
 * ordinary-forest/dappled controls were already generated there and equal them too, and its clear plans keep the published footprint). The same mode also reopens a copy of the {@code baseline275} world, whose old-growth-birch windows were explored and saved with the gate absent.
 * Counts are finite witnesses at two old-growth-birch windows and three controls, not coverage.
 */
public final class BirchCrownsQualification {
    /** The two old-growth-birch windows are selected at run time from the world's own generator, because the native terrain and climate of the bundled
     * preset need not hold a pure oracle's trees; the selection never reads a gate or flag, so every mode picks the same windows. Forest-a (ordinary
     * forest), dappled and birchforest (ordinary birch, family 2) are fixed controls whose own trees the gate does not change. */
    private static final String[] NAMES={"birch-1","birch-2","forest-a","dappled","birchforest"};
    private static final int[][] CONTROLS={{47980,-304100},{16000,-240000},{176000,8000}};
    private static final int[][] ANCHORS={{-684000,1364000},{1492000,-940000},{1684000,-428000},{424242,0}};
    private static int[][] sites;
    /** First 2048-block grid point within 8192 blocks of each anchor whose 80 x 80 window holds at least ten mature old-growth birch stems (family 9 birch, height at least 13, whose own generated holder is minecraft:old_growth_birch_forest), under the world's full saved flags. */
    private static int[][] select(RealisticGenerator g,ServerLevel level){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed(),(g.vegetationFlags()&16)!=0);
        var out=new ArrayList<int[]>();
        for(var a:ANCHORS){
            if(out.size()==2)break;
            search:for(int dz=-8192;dz<=8192;dz+=2048)for(int dx=-8192;dx<=8192;dx+=2048){
                int x=a[0]+dx,z=a[1]+dz;
                long mature=WoodlandTrees.roots(random.seed(),x-40,z-40,x+40,z+40,d,c,g.vegetationFlags()).stream().filter(r->r.family()==TemperateForestVariants.BIRCH_FAMILY&&r.birch()&&r.height()>=BirchCrowns.MATURE_HEIGHT&&level.getBiome(new BlockPos(r.x(),r.y(),r.z())).is(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_BIRCH_FOREST)).count();
                if(mature>=10){out.add(new int[]{x,z});break search;}
            }
        }
        if(out.size()<2)throw new AssertionError("Fewer than two old-growth-birch windows near the anchors: "+out.size());
        for(var cs:CONTROLS)out.add(cs);
        return out.toArray(new int[0][]);
    }
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.birchCrownsDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var json=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
            int shape=json.get("shape_version").getAsInt(),gate=g.birchCrownDefinition();
            if(level.getSeed()!=1L)throw new AssertionError("Seed 1 world required");
            sites=select(g,level);var windows=new ArrayList<Window>();for(var s:sites)windows.add(window(level,s[0]>>4,s[1]>>4));
            Path signature=dir.resolve("signature.txt");var edit=new BlockPos(sites[0][0]+3,g.drainage(level.getChunkSource().randomState()).sample(sites[0][0]+3,sites[0][1]+3).height(),sites[0][1]+3);
            switch(mode){
                case "baseline275"->{
                    if(shape!=275||gate!=0||g.seasonalCrownDefinition()!=1||(g.vegetationFlags()&BirchCrowns.FLAG)!=0)throw new AssertionError("Gate-absent 275 baseline required: "+shape+" "+gate);
                    for(int i=0;i<windows.size();i++)if(windows.get(i).trees()==0)throw new AssertionError("Window without tree cells: "+NAMES[i]);
                    Files.writeString(dir.resolve("baseline.txt"),lines(windows));server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS baseline275: "+summary(windows)+"\n");
                }
                case "fresh276"->{
                    if(shape!=276||gate!=1||json.get("birch_crown_definition").getAsInt()!=1||(g.vegetationFlags()&BirchCrowns.FLAG)==0||g.seasonalCrownDefinition()!=1)throw new AssertionError("Bundled schema 276 preset required: "+shape+" "+gate);
                    var base=baseline();
                    for(int i=0;i<2;i++)if(windows.get(i).digest().equals(base.get(i)))throw new AssertionError("The gate changed nothing in "+NAMES[i]+": "+windows.get(i)+" vs baseline "+base.get(i)+"; flags "+g.vegetationFlags());
                    for(int i=2;i<windows.size();i++)if(!windows.get(i).digest().equals(base.get(i)))throw new AssertionError("The gate changed the control window "+NAMES[i]);
                    // A log cell is also lost where a neighbouring tree's leaf merges over it; the published stand already has such cells.
                    for(int i=0;i<windows.size();i++)if(windows.get(i).isolated()>4)throw new AssertionError("Orphan log cells in "+NAMES[i]+": "+windows.get(i));
                    for(var mutation:new String[][]{{"birch_crown_definition","0"},{"birch_crown_definition","2"},{"birch_crown_definition","-1"},{"shape_version","275"},{"shape_version","277"},{"seasonal_crown_definition","0"}}){
                        var bad=json.deepCopy();bad.addProperty(mutation[0],Integer.parseInt(mutation[1]));
                        if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Mismatched schema/definition accepted: "+mutation[0]+"="+mutation[1]);
                    }
                    var missing=json.deepCopy();missing.remove("birch_crown_definition");
                    if(RealisticGenerator.CODEC.codec().parse(ops,missing).result().isPresent())throw new AssertionError("Schema 276 without its definition accepted");
                    var older=json.deepCopy();older.remove("birch_crown_definition");older.addProperty("shape_version",275);
                    var retained=RealisticGenerator.CODEC.codec().parse(ops,older).getOrThrow();
                    if(retained.birchCrownDefinition()!=0||(retained.vegetationFlags()&BirchCrowns.FLAG)!=0)throw new AssertionError("Complete 275 without the gate must read");
                    var clear=clearing(level,g,retained,dir);
                    level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                    Files.writeString(signature,lines(windows));server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS fresh276: "+summary(windows)+"; baseline digests differ in the old-growth-birch windows and match in controls; "+clear+"; edit "+edit.toShortString()+"\n");
                }
                case "verify276"->{
                    if(shape!=276||gate!=1||(g.vegetationFlags()&BirchCrowns.FLAG)==0||g.seasonalCrownDefinition()!=1)throw new AssertionError("Saved schema 276 world must reopen as 276 with the gate on: "+shape+" "+gate);
                    if(!lines(windows).equals(Files.readString(signature)))throw new AssertionError("Saved gated windows changed on reopen");
                    if(!level.getBlockState(edit).is(Blocks.GOLD_BLOCK))throw new AssertionError("Explicit edit lost across reopen at "+edit);
                    Files.writeString(dir.resolve("result.txt"),"PASS verify276: "+summary(windows)+" identical, edit retained\n");
                }
                case "legacy275"->{
                    if(shape!=275||gate!=0||g.seasonalCrownDefinition()!=1)throw new AssertionError("Authentic 275 world must reopen as 275 with the gate absent: "+shape+" "+gate);
                    var base=baseline();
                    for(int i=0;i<windows.size();i++)if(!windows.get(i).digest().equals(base.get(i)))throw new AssertionError((i<2?"Unexplored old-growth-birch window ":"Retained control window ")+NAMES[i]+" differs from the published baseline: "+windows.get(i));
                    var plan=publishedPlan(level,g,dir);
                    Files.writeString(dir.resolve("result.txt"),"PASS legacy275: windows "+summary(windows)+" equal the published baseline digests (windows the source world never stored are generated first in this reopen; windows it stored are read from its saved chunks); "+plan+"\n");
                }
                default->throw new IllegalArgumentException("Unknown mode "+mode);
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("BIRCH_CROWNS_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static List<String> baseline()throws Exception{
        var out=new ArrayList<String>();for(var line:Files.readAllLines(Path.of(System.getProperty("blockbranch.birchCrownsBaseline"))))out.add(line.substring(line.indexOf(' ')+1));
        if(out.size()!=NAMES.length)throw new AssertionError("Baseline digests missing");return out;
    }
    private static String lines(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append(' ').append(windows.get(i).digest()).append('\n');return sb.toString();}
    private static String summary(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append('[').append(windows.get(i)).append("] ");return sb.toString().trim();}
    private static ReimaginedTreeClearPiece piece(RealisticGenerator g,long seed,int flags,WoodlandTrees.Root root){
        var box=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());return new ReimaginedTreeClearPiece(box,seed,g.borealCrownDefinition(),flags);
    }
    /** A mature old-growth-birch stem of the first window. */
    private static WoodlandTrees.Root birch(RealisticGenerator g,ServerLevel level){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed(),(g.vegetationFlags()&16)!=0);var p=sites[0];
        return WoodlandTrees.roots(random.seed(),p[0]-48,p[1]-48,p[0]+48,p[1]+48,d,c,g.vegetationFlags()).stream().filter(r->r.family()==TemperateForestVariants.BIRCH_FAMILY&&r.birch()&&r.height()>=24).findFirst().orElseThrow();
    }
    /** A new plan names the gated footprint, round-trips its NBT, malformed definitions never load, and the historical definitions keep the published footprint. */
    private static String clearing(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);var root=birch(actual,level);
        var piece=piece(actual,random.seed(),actual.vegetationFlags(),root);var tag=piece.createTag(context);int definition=tag.getIntOr("ClearDefinition",0);
        if(definition!=1+actual.borealCrownDefinition()+2*actual.vegetationFlags()||definition<2049||definition>4096)throw new AssertionError("Saved clear definition names the birch-crown bit: "+definition);
        var plan=BorealClearSmoke.plan(piece,actual,random);var gated=ForestTrees.tree(root,false,false,true);
        if(plan.isEmpty()||!gated.stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("New tree-clear plan covers the entire gated birch footprint");
        try{BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),retained,random);throw new AssertionError("A bit-1024 plan replayed under a generator without the definition");}
        catch(java.lang.reflect.InvocationTargetException expected){if(!(expected.getCause() instanceof IllegalStateException))throw expected;}
        for(int bad:new int[]{0,8193,-1}){var malformed=tag.copy();malformed.putInt("ClearDefinition",bad);
            try{BorealClearSmoke.load(context,malformed);throw new AssertionError("Malformed clear definition "+bad+" loaded");}catch(RuntimeException expected){}}
        try{new ReimaginedTreeClearPiece(new BoundingBox(0,0,0,0,0,0),random.seed(),0,4096);throw new AssertionError("A flag beyond bit 2048 was accepted");}catch(IllegalArgumentException expected){}
        Path file=directory.resolve("birch-tree-clear.nbt");net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
        var restored=BorealClearSmoke.load(context,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
        if(!plan.equals(BorealClearSmoke.plan(restored,actual,random)))throw new AssertionError("Compressed saved clear plan retains the exact footprint");
        var published=publishedPlan(level,actual,retained,directory,root);
        return "gated clear definition "+definition+" covers "+gated.size()+" birch cells; "+published;
    }
    private static String publishedPlan(ServerLevel level,RealisticGenerator g,Path directory)throws Exception{return publishedPlan(level,g,g,directory,birch(g,level));}
    /** Every historical definition (birch bit absent) replays the published footprint, whichever generator replays it. */
    private static String publishedPlan(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory,WoodlandTrees.Root root)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);
        int flags=actual.vegetationFlags()&~BirchCrowns.FLAG;int compared=0,cells=0;var old=ForestTrees.tree(root,false,false);
        for(int mask:new int[]{flags,flags&~SeasonalCrowns.FLAG&~BroadleafCrowns.FLAG}){
            var tag=piece(actual,random.seed(),mask,root).createTag(context);
            var before=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),retained,random);var after=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),actual,random);
            if(before.isEmpty()||!before.equals(after))throw new AssertionError("Historical clear footprints do not adopt the gated crowns: mask "+mask+" stem "+root+" before "+before.size()+" after "+after.size());
            if(!old.stream().allMatch(v->after.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("Historical plan covers the published birch footprint: mask "+mask);
            compared++;cells+=old.size();
        }
        return "historical definitions compared "+compared+" ("+cells+" published birch cells at stem "+root.x()+","+root.z()+")";
    }
    private static Window window(ServerLevel level,int cx,int cz)throws Exception{
        var md=MessageDigest.getInstance("SHA-256");int ints=0,trees=0;var logs=new HashSet<Pos>();
        for(int z=cz-2;z<=cz+2;z++)for(int x=cx-2;x<=cx+2;x++){
            var column=((SparseColumn)level.getChunk(x,z)).blockbranch$terrain().base().canopy().encode();ints+=column.length;md.update(Arrays.toString(column).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for(int packed:column){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;
                if(code>=1&&code<=18)trees++;
                if(code>=1&&code<=6)logs.add(new Pos(x*16+(key&15),(key>>>8)+TerrainShape.FLOOR,z*16+((key>>>4)&15)));}
        }
        int isolated=0;int minX=(cx-2)*16+8,maxX=(cx+2)*16+23,minZ=(cz-2)*16+8,maxZ=(cz+2)*16+23;
        for(var k:logs){int x=k.x(),y=k.y(),z=k.z();if(x<minX||x>maxX||z<minZ||z>maxZ)continue;boolean joined=false;
            for(int[] q:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})if(logs.contains(new Pos(x+q[0],y+q[1],z+q[2])))joined=true;
            if(!joined)isolated++;}
        return new Window(ints,HexFormat.of().formatHex(md.digest()),trees,logs.size(),isolated);
    }
    private record Pos(int x,int y,int z){}
    private record Window(int ints,String sha,int trees,int logs,int isolated){
        String digest(){return ints+" "+sha;}
        @Override public String toString(){return "ints="+ints+" treeCells="+trees+" logCells="+logs+" isolatedLogCells="+isolated+" sha256="+sha.substring(0,12);}
    }
    private BirchCrownsQualification(){}
}
