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
 * Native generated-chunk witnesses for the gated mature coastal winter-rain oak crowns on seed 1. Each window is the 5 x 5 chunks
 * of the chunks' own saved canopy (the real generation path), so a stem exists only where the actual saved summer-dry owner and holder select it.
 * Modes: {@code baseline277} (a fresh world from the gate-absent baseline preset, which keeps the pine gate, records each window's digest),
 * {@code fresh278} (the bundled preset: gate on, coastal-oak windows differ, every control does not, logs attached, saved clear plan covers
 * the new footprint, mismatched or malformed schema/definition never loads, edit and signature saved), {@code verify278} (reopen that closed
 * world in a second process) and {@code legacy277} (reopen a schema-277 world written before the gate existed: shape 277, gate absent,
 * its windows equal the baseline digests, and its clear plans keep the published footprint). The legacy world is either a prior-release
 * world (its coastal windows were never explored) or a copy of the baseline-generated world (whose coastal windows were explored); the two are different evidence.
 * Counts are finite witnesses at two coastal windows and seven controls, not coverage.
 */
public final class CoastalOakQualification {
    private static final String[] NAMES={"coast-1","coast-2","pine-1","pine-2","taiga","snowy","grove","birch","forest-a"};
    private static final int[][] COAST_ANCHORS={{464000,-528000},{1401088,-532224}};
    private static final int[][] CONTROLS={{24704,-1200000},{106624,-1167232},{110720,-1163136},{1492000,-940000},{47980,-304100}};
    private static final int[][] PINE_ANCHORS={{87552,-1591808},{595456,-1600000},{-1362432,-1591808},{79360,-1591808}};
    private static final String HOLDER="blockbranch:coastal_winter_rain_oak_woodland";
    private static int[][] sites;
    /** Orphan-log tolerance: neighbouring crowns can merge over a stem cell in the published stand too. */
    private static final int ISOLATED_LOGS=8;
    /** Mature coastal oaks of a window under the world's full saved flags, the way generation takes them: the actual saved summer-dry owner, then the generated holder at the root. */
    private static List<WoodlandTrees.Root> oaks(RealisticGenerator g,ServerLevel level,int x,int z,int radius){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed(),(g.vegetationFlags()&16)!=0);
        var out=new ArrayList<WoodlandTrees.Root>();
        for(var r:SummerDryTrees.roots(random.seed(),x-radius,z-radius,x+radius,z+radius,d,c,g.summerDryOwner(random)))
            if(r.family()==SummerDryWoodland.FAMILY&&r.height()>=CoastalOakCrowns.MATURE_HEIGHT&&level.getBiome(new BlockPos(r.x(),r.y(),r.z())).unwrapKey().orElseThrow().identifier().toString().equals(HOLDER))out.add(r);
        return out;
    }
    /** Mature old-growth pines, as PineCrownsQualification selects them; their windows are controls here. */
    private static List<WoodlandTrees.Root> pines(RealisticGenerator g,ServerLevel level,int x,int z,int radius){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed(),(g.vegetationFlags()&16)!=0);
        var out=new ArrayList<WoodlandTrees.Root>();
        for(var r:BorealTrees.roots(random.seed(),x-radius,z-radius,x+radius,z+radius,d,c,BorealSlope.relief(g.vegetationFlags())))
            if(r.family()==PineCrowns.FAMILY&&r.height()>=PineCrowns.MATURE_HEIGHT&&ColdUplands.surfaceKind(random.seed(),r.x(),r.z(),d,c)==0&&level.getBiome(new BlockPos(r.x(),r.y(),r.z())).is(net.minecraft.world.level.biome.Biomes.OLD_GROWTH_PINE_TAIGA))out.add(r);
        return out;
    }
    /** First 2048-block grid point within 8192 blocks of each coast anchor whose 80 x 80 window holds at least ten mature coastal oaks; then the pine windows and fixed controls. */
    private static int[][] select(RealisticGenerator g,ServerLevel level){
        var out=new ArrayList<int[]>();
        for(var a:COAST_ANCHORS){
            search:for(int dz=-8192;dz<=8192;dz+=2048)for(int dx=-8192;dx<=8192;dx+=2048){
                int x=a[0]+dx,z=a[1]+dz;
                if(oaks(g,level,x,z,40).size()>=10){out.add(new int[]{x,z});break search;}
            }
        }
        if(out.size()<2)throw new AssertionError("Fewer than two coastal-oak windows near the anchors: "+out.size());
        int pine=0;
        for(var a:PINE_ANCHORS){
            if(pine==2)break;
            search:for(int dz=-8192;dz<=8192;dz+=2048)for(int dx=-8192;dx<=8192;dx+=2048){
                int x=a[0]+dx,z=a[1]+dz;
                if(pines(g,level,x,z,40).size()>=10){out.add(new int[]{x,z});pine++;break search;}
            }
        }
        if(pine<2)throw new AssertionError("Fewer than two old-growth-pine control windows near the anchors: "+pine);
        for(var cs:CONTROLS)out.add(cs);
        return out.toArray(new int[0][]);
    }
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.coastalOakDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var json=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
            int shape=json.get("shape_version").getAsInt(),gate=g.coastalOakCrownDefinition();
            if(level.getSeed()!=1L)throw new AssertionError("Seed 1 world required");
            sites=select(g,level);var windows=new ArrayList<Window>();for(var s:sites)windows.add(window(level,s[0]>>4,s[1]>>4));
            Path signature=dir.resolve("signature.txt");var edit=new BlockPos(sites[0][0]+3,g.drainage(level.getChunkSource().randomState()).sample(sites[0][0]+3,sites[0][1]+3).height(),sites[0][1]+3);
            switch(mode){
                case "baseline277"->{
                    if(shape!=277||gate!=0||g.pineCrownDefinition()!=1||g.summerDryDefinition()!=1||(g.vegetationFlags()&CoastalOakCrowns.FLAG)!=0)throw new AssertionError("Gate-absent 277 baseline required: "+shape+" "+gate);
                    for(int i=0;i<windows.size();i++)if(windows.get(i).trees()==0)throw new AssertionError("Window without tree cells: "+NAMES[i]);
                    Files.writeString(dir.resolve("baseline.txt"),lines(windows));server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS baseline277: "+summary(windows)+"; mature coastal oaks "+oaks(g,level,sites[0][0],sites[0][1],40).size()+" and "+oaks(g,level,sites[1][0],sites[1][1],40).size()+"\n");
                }
                case "fresh278"->{
                    if(shape!=278||gate!=1||json.get("coastal_oak_crown_definition").getAsInt()!=1||(g.vegetationFlags()&CoastalOakCrowns.FLAG)==0||(g.vegetationFlags()&SummerDryWoodland.FLAG)==0||g.pineCrownDefinition()!=1)throw new AssertionError("Bundled schema 278 preset required: "+shape+" "+gate);
                    var base=baseline();int matureA=oaks(g,level,sites[0][0],sites[0][1],40).size(),matureB=oaks(g,level,sites[1][0],sites[1][1],40).size();
                    if(matureA<10||matureB<10)throw new AssertionError("Target windows hold mature coastal oaks under the full saved flags "+g.vegetationFlags()+": "+matureA+" "+matureB);
                    for(int i=0;i<2;i++)if(windows.get(i).digest().equals(base.get(i)))throw new AssertionError("The gate changed nothing in "+NAMES[i]+": "+windows.get(i)+" vs baseline "+base.get(i)+"; flags "+g.vegetationFlags());
                    for(int i=2;i<windows.size();i++)if(!windows.get(i).digest().equals(base.get(i)))throw new AssertionError("The gate changed the control window "+NAMES[i]);
                    for(int i=0;i<windows.size();i++)if(windows.get(i).isolated()>ISOLATED_LOGS)throw new AssertionError("Orphan log cells in "+NAMES[i]+": "+windows.get(i));
                    for(var mutation:new String[][]{{"coastal_oak_crown_definition","0"},{"coastal_oak_crown_definition","2"},{"coastal_oak_crown_definition","-1"},{"shape_version","277"},{"shape_version","279"},{"pine_crown_definition","0"},{"birch_crown_definition","0"},{"summer_dry_definition","0"}}){
                        var bad=json.deepCopy();bad.addProperty(mutation[0],Integer.parseInt(mutation[1]));
                        if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Mismatched schema/definition accepted: "+mutation[0]+"="+mutation[1]);
                    }
                    var missing=json.deepCopy();missing.remove("coastal_oak_crown_definition");
                    if(RealisticGenerator.CODEC.codec().parse(ops,missing).result().isPresent())throw new AssertionError("Schema 278 without its definition accepted");
                    var older=json.deepCopy();older.remove("coastal_oak_crown_definition");older.addProperty("shape_version",277);
                    var retained=RealisticGenerator.CODEC.codec().parse(ops,older).getOrThrow();
                    if(retained.coastalOakCrownDefinition()!=0||(retained.vegetationFlags()&CoastalOakCrowns.FLAG)!=0||retained.pineCrownDefinition()!=1)throw new AssertionError("Complete 277 without the gate must read");
                    var clear=clearing(level,g,retained,dir);
                    level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                    Files.writeString(signature,lines(windows));server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS fresh278: "+summary(windows)+"; mature coastal oaks "+matureA+" and "+matureB+" under flags "+g.vegetationFlags()+"; baseline digests differ in the coastal windows and match in controls; "+clear+"; edit "+edit.toShortString()+"\n");
                }
                case "verify278"->{
                    if(shape!=278||gate!=1||(g.vegetationFlags()&CoastalOakCrowns.FLAG)==0||g.pineCrownDefinition()!=1)throw new AssertionError("Saved schema 278 world must reopen as 278 with the gate on: "+shape+" "+gate);
                    if(!lines(windows).equals(Files.readString(signature)))throw new AssertionError("Saved gated windows changed on reopen");
                    if(!level.getBlockState(edit).is(Blocks.GOLD_BLOCK))throw new AssertionError("Explicit edit lost across reopen at "+edit);
                    Files.writeString(dir.resolve("result.txt"),"PASS verify278: "+summary(windows)+" identical, edit retained\n");
                }
                case "legacy277"->{
                    if(shape!=277||gate!=0||g.pineCrownDefinition()!=1)throw new AssertionError("A schema-277 world must reopen as 277 with the coastal-oak gate absent: "+shape+" "+gate);
                    var base=baseline();
                    for(int i=0;i<windows.size();i++)if(!windows.get(i).digest().equals(base.get(i)))throw new AssertionError((i<2?"Coastal-oak window ":"Control window ")+NAMES[i]+" differs from the published baseline: "+windows.get(i));
                    var plan=publishedPlan(level,g,dir);
                    Files.writeString(dir.resolve("result.txt"),"PASS legacy277: windows "+summary(windows)+" equal the published baseline digests (windows the source world never stored are generated first in this reopen; windows it stored are read from its saved chunks); "+plan+"\n");
                }
                default->throw new IllegalArgumentException("Unknown mode "+mode);
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("COASTAL_OAK_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static List<String> baseline()throws Exception{
        var out=new ArrayList<String>();for(var line:Files.readAllLines(Path.of(System.getProperty("blockbranch.coastalOakBaseline"))))out.add(line.substring(line.indexOf(' ')+1));
        if(out.size()!=NAMES.length)throw new AssertionError("Baseline digests missing");return out;
    }
    private static String lines(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append(' ').append(windows.get(i).digest()).append('\n');return sb.toString();}
    private static String summary(List<Window> windows){var sb=new StringBuilder();for(int i=0;i<windows.size();i++)sb.append(NAMES[i]).append('[').append(windows.get(i)).append("] ");return sb.toString().trim();}
    private static ReimaginedTreeClearPiece piece(RealisticGenerator g,long seed,int flags,WoodlandTrees.Root root){
        var box=new BoundingBox(root.x(),root.y(),root.z(),root.x(),root.y(),root.z());return new ReimaginedTreeClearPiece(box,seed,g.borealCrownDefinition(),flags);
    }
    /** A mature coastal oak of the first window. */
    private static WoodlandTrees.Root oak(RealisticGenerator g,ServerLevel level){return oaks(g,level,sites[0][0],sites[0][1],48).getFirst();}
    /** A new plan names the gated footprint, round-trips its NBT, malformed definitions never load, and the historical definitions keep the published footprint. */
    private static String clearing(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);var root=oak(actual,level);
        var piece=piece(actual,random.seed(),actual.vegetationFlags(),root);var tag=piece.createTag(context);int definition=tag.getIntOr("ClearDefinition",0);
        if(definition!=1+actual.borealCrownDefinition()+2*actual.vegetationFlags()||definition<8193||definition>16384)throw new AssertionError("Saved clear definition names the coastal-oak bit: "+definition);
        var plan=BorealClearSmoke.plan(piece,actual,random);var gated=SummerDryTrees.tree(root,true);
        if(plan.isEmpty()||!gated.stream().allMatch(v->plan.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("New tree-clear plan covers the entire gated oak footprint");
        try{BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),retained,random);throw new AssertionError("A bit-4096 plan replayed under a generator without the definition");}
        catch(java.lang.reflect.InvocationTargetException expected){if(!(expected.getCause() instanceof IllegalStateException))throw expected;}
        for(int bad:new int[]{0,16385,-1}){var malformed=tag.copy();malformed.putInt("ClearDefinition",bad);
            try{BorealClearSmoke.load(context,malformed);throw new AssertionError("Malformed clear definition "+bad+" loaded");}catch(RuntimeException expected){}}
        try{new ReimaginedTreeClearPiece(new BoundingBox(0,0,0,0,0,0),random.seed(),0,8192);throw new AssertionError("A flag beyond bit 4096 was accepted");}catch(IllegalArgumentException expected){}
        Path file=directory.resolve("coastal-oak-tree-clear.nbt");net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
        var restored=BorealClearSmoke.load(context,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
        if(!plan.equals(BorealClearSmoke.plan(restored,actual,random)))throw new AssertionError("Compressed saved clear plan retains the exact footprint");
        var published=publishedPlan(level,actual,retained,directory,root);
        return "gated clear definition "+definition+" covers "+gated.size()+" oak cells; "+published;
    }
    private static String publishedPlan(ServerLevel level,RealisticGenerator g,Path directory)throws Exception{return publishedPlan(level,g,g,directory,oak(g,level));}
    /** Every historical definition (coastal-oak bit absent) replays the published footprint, whichever generator replays it. */
    private static String publishedPlan(ServerLevel level,RealisticGenerator actual,RealisticGenerator retained,Path directory,WoodlandTrees.Root root)throws Exception{
        var random=level.getChunkSource().randomState();var context=StructurePieceSerializationContext.fromLevel(level);
        int flags=actual.vegetationFlags()&~CoastalOakCrowns.FLAG;int compared=0,cells=0;var old=SummerDryTrees.tree(root);
        for(int mask:new int[]{flags,flags&~PineCrowns.FLAG&~BirchCrowns.FLAG&~SeasonalCrowns.FLAG&~BroadleafCrowns.FLAG}){
            var tag=piece(actual,random.seed(),mask,root).createTag(context);
            var before=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),retained,random);var after=BorealClearSmoke.plan(BorealClearSmoke.load(context,tag),actual,random);
            if(before.isEmpty()||!before.equals(after))throw new AssertionError("Historical clear footprints do not adopt the gated crowns: mask "+mask+" stem "+root+" before "+before.size()+" after "+after.size());
            if(!old.stream().allMatch(v->after.containsKey(BlockPos.asLong(v.x(),v.y(),v.z()))))throw new AssertionError("Historical plan covers the published oak footprint: mask "+mask);
            compared++;cells+=old.size();
        }
        return "historical definitions compared "+compared+" ("+cells+" published oak cells at stem "+root.x()+","+root.z()+")";
    }
    private static Window window(ServerLevel level,int cx,int cz)throws Exception{
        var md=MessageDigest.getInstance("SHA-256");int ints=0,trees=0;var logs=new HashSet<Pos>();
        for(int z=cz-2;z<=cz+2;z++)for(int x=cx-2;x<=cx+2;x++){
            var column=((SparseColumn)level.getChunk(x,z)).blockbranch$terrain().base().canopy().encode();ints+=column.length;md.update(Arrays.toString(column).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for(int packed:column){int key=packed>>>CanopyLayer.STATE_BITS,code=packed&CanopyLayer.STATE_MASK;
                if(code!=0)trees++;
                if(WoodlandTrees.logCode(code))logs.add(new Pos(x*16+(key&15),(key>>>8)+TerrainShape.FLOOR,z*16+((key>>>4)&15)));}
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
    private CoastalOakQualification(){}
}
