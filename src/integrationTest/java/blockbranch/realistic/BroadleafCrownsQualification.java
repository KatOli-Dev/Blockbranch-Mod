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
 * Native generated-chunk witnesses for the gated ordinary-forest oak crowns on seed 1. Each window is the 5 x 5 chunks
 * the frozen published fixture covers, read from the chunks' own saved canopy (the real generation path).
 * Modes: {@code baseline273} (a fresh world from the gate-absent baseline preset records each window's digest),
 * {@code fresh274} (the bundled preset: gate on, forest windows differ, logs attached, saved clear plan covers the new
 * footprint, mismatched schema/definition never loads, edit and signature saved), {@code verify274} (reopen that closed
 * world in a second process) and {@code legacy273} (reopen an authentic schema-273 world written before the gate
 * existed: shape 273, gate absent, its never-explored forest windows equal the baseline digests and its clear plans
 * keep the published footprint). Counts are finite witnesses at four sites, not coverage.
 */
public final class BroadleafCrownsQualification {
    /** forest-a is the audited (48000,-304000) witness; forest-d/e are coarse-grid finds of mature-oak stands below 300 m
     * (the audit's other forest scenes sit above 1100 m, where the bundled preset's montane needleleaf now owns the ground);
     * dappled is the audited woodland, whose own trees the gate does not change. */
    private static final String[] NAMES={"forest-a","forest-d","forest-e","dappled"};
    private static final int[][] SITES={{47980,-304100},{30000,-346000},{78000,-346000},{16000,-240000}};
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.broadleafCrownsDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var json=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
            int shape=json.get("shape_version").getAsInt(),gate=g.broadleafCrownDefinition();
            if(level.getSeed()!=1L)throw new AssertionError("Seed 1 world required");
            var windows=new ArrayList<Window>();for(var s:SITES)windows.add(window(level,s[0]>>4,s[1]>>4));
            Path signature=dir.resolve("signature.txt");var edit=new BlockPos(SITES[0][0]+3,g.drainage(level.getChunkSource().randomState()).sample(SITES[0][0]+3,SITES[0][1]+3).height(),SITES[0][1]+3);
            switch(mode){
                case "baseline273"->{
                    if(shape!=273||gate!=0||g.mangroveRootsDefinition()!=1||(g.vegetationFlags()&BroadleafCrowns.FLAG)!=0)throw new AssertionError("Gate-absent 273 baseline required: "+shape+" "+gate);
                    for(int i=0;i<3;i++)if(windows.get(i).trees()==0)throw new AssertionError("Forest window without oak/birch cells: "+NAMES[i]);
                    Files.writeString(dir.resolve("baseline.txt"),lines(windows));
                    Files.writeString(dir.resolve("result.txt"),"PASS baseline273: "+summary(windows)+"\n");
                }
                case "fresh274"->{
                    if(shape!=274||gate!=1||json.get("broadleaf_crown_definition").getAsInt()!=1||(g.vegetationFlags()&BroadleafCrowns.FLAG)==0||g.mangroveRootsDefinition()!=1)throw new AssertionError("Bundled schema 274 preset required: "+shape+" "+gate);
                    var base=baseline();
                    for(int i=0;i<3;i++)if(windows.get(i).digest().equals(base.get(i)))throw new AssertionError("The gate changed nothing in "+NAMES[i]);
                    if(!windows.get(3).digest().equals(base.get(3)))throw new AssertionError("The gate changed the dappled control window");
                    // A log cell is also lost where a neighbouring tree's leaf merges over it; the published stand already has such cells.
                    for(int i=0;i<windows.size();i++)if(windows.get(i).isolated()>4)throw new AssertionError("Orphan log cells in "+NAMES[i]+": "+windows.get(i));
                    for(var mutation:new String[][]{{"broadleaf_crown_definition","0"},{"broadleaf_crown_definition","2"},{"broadleaf_crown_definition","-1"},{"shape_version","273"},{"shape_version","275"},{"mangrove_roots_definition","0"}}){
                        var bad=json.deepCopy();bad.addProperty(mutation[0],Integer.parseInt(mutation[1]));
                        if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Mismatched schema/definition accepted: "+mutation[0]+"="+mutation[1]);
                    }
                    var missing=json.deepCopy();missing.remove("broadleaf_crown_definition");
                    if(RealisticGenerator.CODEC.codec().parse(ops,missing).result().isPresent())throw new AssertionError("Schema 274 without its definition accepted");
                    var older=json.deepCopy();older.remove("broadleaf_crown_definition");older.addProperty("shape_version",273);
                    var retained=RealisticGenerator.CODEC.codec().parse(ops,older).getOrThrow();
                    if(retained.broadleafCrownDefinition()!=0||(retained.vegetationFlags()&BroadleafCrowns.FLAG)!=0)throw new AssertionError("Complete 273 without the gate must read");
                    var clear=clearing(level,g,retained,dir);
                    level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                    Files.writeString(signature,lines(windows));server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS fresh274: "+summary(windows)+"; baseline digests differ in forest windows; "+clear+"; edit "+edit.toShortString()+"\n");
                }
                case "verify274"->{
                    if(shape!=274||gate!=1)throw new AssertionError("Saved schema 274 world must reopen as 274: "+shape+" "+gate);
                    if(!lines(windows).equals(Files.readString(signature)))throw new AssertionError("Saved gated windows changed on reopen");
                    if(!level.getBlockState(edit).is(Blocks.GOLD_BLOCK))throw new AssertionError("Explicit edit lost across reopen at "+edit);
                    Files.writeString(dir.resolve("result.txt"),"PASS verify274: "+summary(windows)+" identical, edit retained\n");
                }
                case "legacy273"->{
                    if(shape!=273||gate!=0||g.mangroveRootsDefinition()!=1)throw new AssertionError("Authentic 273 world must reopen as 273 with the gate absent: "+shape+" "+gate);
                    var base=baseline();
                    for(int i=0;i<windows.size();i++)if(!windows.get(i).digest().equals(base.get(i)))throw new AssertionError("Never-explored window "+NAMES[i]+" differs from the published baseline: "+windows.get(i));
                    var plan=publishedPlan(level,g,dir);
                    Files.writeString(dir.resolve("result.txt"),"PASS legacy273: never-explored windows "+summary(windows)+" equal the published baseline digests; "+plan+"\n");
                }
                default->throw new IllegalArgumentException("Unknown mode "+mode);
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("BROADLEAF_CROWNS_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static List<String> baseline()throws Exception{
        var out=new ArrayList<String>();for(var line:Files.readAllLines(Path.of(System.getProperty("blockbranch.broadleafCrownsBaseline"))))out.add(line.substring(line.indexOf(' ')+1));
        if(out.size()!=SITES.length)throw new AssertionError("Baseline digests missing");return out;
    }
    private static String lines(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append(' ').append(windows.get(i).digest()).append('\n');return sb.toString();}
    private static String summary(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append('[').append(windows.get(i)).append("] ");return sb.toString().trim();}
    private static ReimaginedTreeClearPiece piece(RealisticGenerator g,long seed,int flags,WoodlandTrees.Root root){
        var box=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());return new ReimaginedTreeClearPiece(box,seed,g.borealCrownDefinition(),flags);
    }
    private static WoodlandTrees.Root oak(RealisticGenerator g,ServerLevel level){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed(),(g.vegetationFlags()&16)!=0);var p=SITES[0];
        return WoodlandTrees.roots(random.seed(),p[0]-48,p[1]-48,p[0]+48,p[1]+48,d,c,g.vegetationFlags()&~BroadleafCrowns.FLAG).stream().filter(r->r.family()==1&&!r.birch()&&r.height()>=20).findFirst().orElseThrow();
    }
    /** A new plan names the gated footprint, round-trips its NBT, and the historical definition keeps the published one. */
    private static String clearing(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);var root=oak(actual,level);
        var piece=piece(actual,random.seed(),actual.vegetationFlags(),root);var tag=piece.createTag(context);int definition=tag.getIntOr("ClearDefinition",0);
        if(definition!=1+actual.borealCrownDefinition()+2*actual.vegetationFlags()||definition<513)throw new AssertionError("Saved clear definition names the broadleaf bit: "+definition);
        var plan=BorealClearSmoke.plan(piece,actual,random);var gated=ForestTrees.tree(root,true);
        if(plan.isEmpty()||!gated.stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("New tree-clear plan covers the entire gated oak footprint");
        Path file=directory.resolve("broadleaf-tree-clear.nbt");net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
        var restored=BorealClearSmoke.load(context,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
        if(!plan.equals(BorealClearSmoke.plan(restored,actual,random)))throw new AssertionError("Compressed saved clear plan retains the exact footprint");
        var published=publishedPlan(level,actual,retained,directory,root);
        return "gated clear definition "+definition+" covers "+gated.size()+" oak cells; "+published;
    }
    private static String publishedPlan(ServerLevel level,RealisticGenerator g,Path directory)throws Exception{return publishedPlan(level,g,g,directory,oak(g,level));}
    /** Every historical definition (broadleaf bit absent) replays the published footprint, whichever generator replays it. */
    private static String publishedPlan(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory,WoodlandTrees.Root root)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);
        int flags=actual.vegetationFlags()&~BroadleafCrowns.FLAG;var old=ForestTrees.tree(root,false);int compared=0;
        for(int mask:new int[]{flags,3}){
            var tag=piece(actual,random.seed(),mask,root).createTag(context);
            var before=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),retained,random);var after=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),actual,random);
            if(before.isEmpty()||!before.equals(after))throw new AssertionError("Historical clear footprints do not adopt the gated crowns");
            if(!old.stream().allMatch(v->after.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("Historical plan covers the published oak footprint");
            compared++;
        }
        return "historical definitions compared "+compared+" ("+old.size()+" published oak cells)";
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
    private BroadleafCrownsQualification(){}
}
