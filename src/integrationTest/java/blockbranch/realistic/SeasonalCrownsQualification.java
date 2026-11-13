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
 * Native generated-chunk witnesses for the gated seasonal dry-forest crowns on seed 1. Each window is the 5 x 5 chunks
 * the frozen published fixture covers, read from the chunks' own saved canopy (the real generation path).
 * Modes: {@code baseline274} (a fresh world from the gate-absent baseline preset records each window's digest),
 * {@code fresh275} (the bundled preset: gate on, dry-forest windows differ, controls do not, logs attached, saved clear plan covers
 * the new footprint, mismatched schema/definition never loads, edit and signature saved), {@code verify275} (reopen that closed
 * world in a second process) and {@code legacy274} (reopen an authentic schema-274 world written before the gate
 * existed: shape 274, gate absent, its dry-forest windows were never explored by the source world and equal the baseline digests, its
 * ordinary-forest/dappled controls were already generated there and equal them too, and its clear plans keep the published footprint). Counts are finite witnesses at two dry-forest scenes and two controls, not coverage.
 */
public final class SeasonalCrownsQualification {
    /** dry-a/dry-b are the audited seasonal dry-forest scenes (1488000,-304000) and (1520000,-336000); forest-a (ordinary forest) and
     * dappled are controls whose own trees the gate does not change. */
    private static final String[] NAMES={"dry-a","dry-b","forest-a","dappled"};
    private static final int[][] SITES={{1488000,-304000},{1520000,-336000},{47980,-304100},{16000,-240000}};
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.seasonalCrownsDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var json=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
            int shape=json.get("shape_version").getAsInt(),gate=g.seasonalCrownDefinition();
            if(level.getSeed()!=1L)throw new AssertionError("Seed 1 world required");
            var windows=new ArrayList<Window>();for(var s:SITES)windows.add(window(level,s[0]>>4,s[1]>>4));
            Path signature=dir.resolve("signature.txt");var edit=new BlockPos(SITES[0][0]+3,g.drainage(level.getChunkSource().randomState()).sample(SITES[0][0]+3,SITES[0][1]+3).height(),SITES[0][1]+3);
            switch(mode){
                case "baseline274"->{
                    if(shape!=274||gate!=0||g.broadleafCrownDefinition()!=1||(g.vegetationFlags()&SeasonalCrowns.FLAG)!=0)throw new AssertionError("Gate-absent 274 baseline required: "+shape+" "+gate);
                    for(int i=0;i<3;i++)if(windows.get(i).trees()==0)throw new AssertionError("Window without tree cells: "+NAMES[i]);
                    Files.writeString(dir.resolve("baseline.txt"),lines(windows));
                    Files.writeString(dir.resolve("result.txt"),"PASS baseline274: "+summary(windows)+"\n");
                }
                case "fresh275"->{
                    if(shape!=275||gate!=1||json.get("seasonal_crown_definition").getAsInt()!=1||(g.vegetationFlags()&SeasonalCrowns.FLAG)==0||g.broadleafCrownDefinition()!=1)throw new AssertionError("Bundled schema 275 preset required: "+shape+" "+gate);
                    var base=baseline();
                    for(int i=0;i<2;i++)if(windows.get(i).digest().equals(base.get(i)))throw new AssertionError("The gate changed nothing in "+NAMES[i]);
                    for(int i=2;i<4;i++)if(!windows.get(i).digest().equals(base.get(i)))throw new AssertionError("The gate changed the control window "+NAMES[i]);
                    // A log cell is also lost where a neighbouring tree's leaf merges over it; the published stand already has such cells.
                    for(int i=0;i<windows.size();i++)if(windows.get(i).isolated()>4)throw new AssertionError("Orphan log cells in "+NAMES[i]+": "+windows.get(i));
                    for(var mutation:new String[][]{{"seasonal_crown_definition","0"},{"seasonal_crown_definition","2"},{"seasonal_crown_definition","-1"},{"shape_version","274"},{"shape_version","276"},{"broadleaf_crown_definition","0"}}){
                        var bad=json.deepCopy();bad.addProperty(mutation[0],Integer.parseInt(mutation[1]));
                        if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Mismatched schema/definition accepted: "+mutation[0]+"="+mutation[1]);
                    }
                    var missing=json.deepCopy();missing.remove("seasonal_crown_definition");
                    if(RealisticGenerator.CODEC.codec().parse(ops,missing).result().isPresent())throw new AssertionError("Schema 275 without its definition accepted");
                    var older=json.deepCopy();older.remove("seasonal_crown_definition");older.addProperty("shape_version",274);
                    var retained=RealisticGenerator.CODEC.codec().parse(ops,older).getOrThrow();
                    if(retained.seasonalCrownDefinition()!=0||(retained.vegetationFlags()&SeasonalCrowns.FLAG)!=0)throw new AssertionError("Complete 274 without the gate must read");
                    var clear=clearing(level,g,retained,dir);
                    level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                    Files.writeString(signature,lines(windows));server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS fresh275: "+summary(windows)+"; baseline digests differ in dry-forest windows and match in controls; "+clear+"; edit "+edit.toShortString()+"\n");
                }
                case "verify275"->{
                    if(shape!=275||gate!=1)throw new AssertionError("Saved schema 275 world must reopen as 275: "+shape+" "+gate);
                    if(!lines(windows).equals(Files.readString(signature)))throw new AssertionError("Saved gated windows changed on reopen");
                    if(!level.getBlockState(edit).is(Blocks.GOLD_BLOCK))throw new AssertionError("Explicit edit lost across reopen at "+edit);
                    Files.writeString(dir.resolve("result.txt"),"PASS verify275: "+summary(windows)+" identical, edit retained\n");
                }
                case "legacy274"->{
                    if(shape!=274||gate!=0||g.broadleafCrownDefinition()!=1)throw new AssertionError("Authentic 274 world must reopen as 274 with the gate absent: "+shape+" "+gate);
                    var base=baseline();
                    for(int i=0;i<windows.size();i++)if(!windows.get(i).digest().equals(base.get(i)))throw new AssertionError((i<2?"Unexplored dry-forest window ":"Retained control window ")+NAMES[i]+" differs from the published baseline: "+windows.get(i));
                    var plan=publishedPlan(level,g,dir);
                    Files.writeString(dir.resolve("result.txt"),"PASS legacy274: windows "+summary(windows)+" equal the published baseline digests (dry-a/dry-b generated first in this reopen, forest-a/dappled already stored in the source world); "+plan+"\n");
                }
                default->throw new IllegalArgumentException("Unknown mode "+mode);
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("SEASONAL_CROWNS_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static List<String> baseline()throws Exception{
        var out=new ArrayList<String>();for(var line:Files.readAllLines(Path.of(System.getProperty("blockbranch.seasonalCrownsBaseline"))))out.add(line.substring(line.indexOf(' ')+1));
        if(out.size()!=SITES.length)throw new AssertionError("Baseline digests missing");return out;
    }
    private static String lines(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append(' ').append(windows.get(i).digest()).append('\n');return sb.toString();}
    private static String summary(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append('[').append(windows.get(i)).append("] ");return sb.toString().trim();}
    private static ReimaginedTreeClearPiece piece(RealisticGenerator g,long seed,int flags,WoodlandTrees.Root root){
        var box=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());return new ReimaginedTreeClearPiece(box,seed,g.borealCrownDefinition(),flags);
    }
    /** A mature stem of the seasonal dry-forest owner at the audited scene. */
    private static WoodlandTrees.Root oak(RealisticGenerator g,ServerLevel level){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed(),(g.vegetationFlags()&16)!=0);var p=SITES[0];
        return DryForestTrees.roots(random.seed(),p[0]-48,p[1]-48,p[0]+48,p[1]+48,d,c,g.dryForestOwner(random,true)).stream().filter(r->r.height()>=20).findFirst().orElseThrow();
    }
    /** A new plan names the gated footprint, round-trips its NBT, and the historical definition keeps the published one. */
    private static String clearing(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);var root=oak(actual,level);
        var piece=piece(actual,random.seed(),actual.vegetationFlags(),root);var tag=piece.createTag(context);int definition=tag.getIntOr("ClearDefinition",0);
        if(definition!=1+actual.borealCrownDefinition()+2*actual.vegetationFlags()||definition<1025)throw new AssertionError("Saved clear definition names the seasonal-crown bit: "+definition);
        var plan=BorealClearSmoke.plan(piece,actual,random);var gated=ForestTrees.tree(root,false,true);
        if(plan.isEmpty()||!gated.stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("New tree-clear plan covers the entire gated dry-forest footprint");
        Path file=directory.resolve("seasonal-tree-clear.nbt");net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
        var restored=BorealClearSmoke.load(context,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
        if(!plan.equals(BorealClearSmoke.plan(restored,actual,random)))throw new AssertionError("Compressed saved clear plan retains the exact footprint");
        var published=publishedPlan(level,actual,retained,directory,root);
        return "gated clear definition "+definition+" covers "+gated.size()+" dry-forest cells; "+published;
    }
    private static String publishedPlan(ServerLevel level,RealisticGenerator g,Path directory)throws Exception{return publishedPlan(level,g,g,directory,oak(g,level));}
    /** Every historical definition (seasonal-crown bit absent) replays the published footprint, whichever generator replays it. */
    private static String publishedPlan(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory,WoodlandTrees.Root root)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);
        int flags=actual.vegetationFlags()&~SeasonalCrowns.FLAG;int compared=0,cells=0;var witnesses=new StringBuilder();
        for(int mask:new int[]{flags,flags&~SeasonalForestExpansion.FLAG|DryForest.FLAG}){
            // The regional-only definition replays the regional owner, so its stem is taken from that owner, not from the expansion's wider one.
            var stem=(mask&SeasonalForestExpansion.FLAG)!=0?root:regionalStem(actual,random);var old=ForestTrees.tree(stem,false,false);
            var tag=piece(actual,random.seed(),mask,stem).createTag(context);
            var before=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),retained,random);var after=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),actual,random);
            if(before.isEmpty()||!before.equals(after))throw new AssertionError("Historical clear footprints do not adopt the gated crowns: mask "+mask+" stem "+stem+" before "+before.size()+" after "+after.size());
            if(!old.stream().allMatch(v->after.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("Historical plan covers the published oak footprint: mask "+mask);
            compared++;cells+=old.size();witnesses.append(" mask").append(mask).append('@').append(stem.x()).append(',').append(stem.z());
        }
        return "historical definitions compared "+compared+" ("+cells+" published dry-forest cells; stems"+witnesses+")";
    }
    /**
     * A mature stem of the regional-only dry-forest owner (the historical definition-9..16 domain). The audited seasonal scenes need not
     * contain one, so this is a bounded deterministic lookup: a square grid around each audited site, widening 2048/8192/32768 blocks,
     * then the first mature stem within 96 blocks of the first regional-owned cell. The seasonal-expansion owner is never used here.
     */
    private static WoodlandTrees.Root regionalStem(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){
        var d=g.drainage(random);var c=new ClimateField(random.seed(),(g.vegetationFlags()&16)!=0);var owner=g.dryForestOwner(random,false);
        for(int reach:new int[]{2048,8192,32768})for(var p:SITES)for(int z=p[1]-reach;z<=p[1]+reach;z+=reach/32)for(int x=p[0]-reach;x<=p[0]+reach;x+=reach/32){
            if(!owner.test(x,z))continue;
            var found=DryForestTrees.roots(random.seed(),x-96,z-96,x+96,z+96,d,c,owner).stream().filter(t->t.height()>=20).findFirst();
            if(found.isPresent())return found.get();
        }
        throw new AssertionError("No regional-only dry-forest stem within 32768 blocks of the audited sites");
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
    private SeasonalCrownsQualification(){}
}
