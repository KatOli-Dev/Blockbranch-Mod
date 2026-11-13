package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.CanopyLayer;
import blockbranch.cubic.SparseColumn;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import com.mojang.serialization.JsonOps;

/**
 * Native boreal-slope qualification over the sampled treeless slope (seed 1). Modes:
 * {@code record270} writes a published-270 world window with per-chunk profile hashes (run it on a
 * build before any slope rule exists); {@code verify270} reopens that closed world and requires the
 * saved chunks to read back unchanged and the unexplored remainder of the window to stay treeless.
 * {@code fresh272} writes a fresh world from the bundled preset (schema 272 when it was written, 273 once mangrove roots wrap it) whose generated chunks, saved
 * clear plan and explicit edit {@code verify272} must read back identically in a second process.
 * {@code verifyMontane271} reopens a copy of an authentic montane-271 world (written by that build) and
 * requires it to stay 271 with the slope definition absent and its saved clear plan to replay unchanged.
 * The window is {@code 2*HALF_CHUNKS} chunks square; the saved core is its central 4 x 4 chunks.
 */
public final class BorealSlopeQualification {
    static final int SITE_X=-1392000,SITE_Z=-1616000,HALF_CHUNKS=4;
    public static void register(String mode){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.borealSlopeDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var json=RealisticGenerator.CODEC.codec().encodeStart(RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().getAsJsonObject();
            int shape=json.get("shape_version").getAsInt(),slope=json.has("boreal_slope_definition")?json.get("boreal_slope_definition").getAsInt():0;
            long wantSeed=mode.equals("overlap272")?Long.getLong("blockbranch.borealSlopeSeed",1L):1L;if(level.getSeed()!=wantSeed)throw new AssertionError("Seed "+wantSeed+" world required");
            int cx=SITE_X>>4,cz=SITE_Z>>4;Path signature=dir.resolve("signature.txt");
            switch(mode){
                case "record270"->{
                    if(shape!=270||slope!=0)throw new AssertionError("Published schema 270 build required: "+shape+" "+slope);
                    var lines=new ArrayList<String>();
                    for(int dz=-2;dz<2;dz++)for(int dx=-2;dx<2;dx++)lines.add(describe(level,cx+dx,cz+dz));
                    long logs=lines.stream().mapToLong(l->Long.parseLong(l.split(" ")[3])).sum();
                    if(logs!=0)throw new AssertionError("Published 270 sampled window is treeless; logs="+logs);
                    Files.write(signature,lines);server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS recorded published 270 treeless window: "+lines.size()+" chunks, "+logs+" logs\n");
                }
                case "verify270"->{
                    if(shape!=270||slope!=0)throw new AssertionError("Saved published schema 270 world must still read as 270 with the slope definition absent: "+shape+" "+slope);
                    var saved=Files.readAllLines(signature);int i=0;
                    for(int dz=-2;dz<2;dz++)for(int dx=-2;dx<2;dx++)if(!describe(level,cx+dx,cz+dz).equals(saved.get(i++)))throw new AssertionError("Saved published chunk changed on reopen: "+(cx+dx)+","+(cz+dz));
                    long logs=0;int fresh=0;
                    for(int dz=-HALF_CHUNKS;dz<HALF_CHUNKS;dz++)for(int dx=-HALF_CHUNKS;dx<HALF_CHUNKS;dx++){
                        if(dx>=-2&&dx<2&&dz>=-2&&dz<2)continue;
                        logs+=Long.parseLong(describe(level,cx+dx,cz+dz).split(" ")[3]);fresh++;
                    }
                    if(logs!=0)throw new AssertionError("Unexplored chunks of a published 270 world must stay treeless; logs="+logs);
                    Files.writeString(dir.resolve("result.txt"),"PASS verify270: "+saved.size()+" saved chunks identical, "+fresh+" unexplored chunks treeless\n");
                }
                case "overlap272"->{
                    var sites=new ArrayList<int[]>();for(var p:MontaneForestQualification.points(wantSeed))sites.add(p);
                    if(wantSeed==1)sites.add(0,new int[]{SITE_X,SITE_Z});else{sites.add(0,new int[]{-972000,-1676000});sites.add(1,new int[]{1828000,-1196000});}
                    Files.writeString(dir.resolve("result.txt"),"PASS overlap272: "+BorealMontaneOverlap.run(level,g,dir,wantSeed,sites.toArray(new int[0][])));
                }
                case "fresh272"->{
                    int current=g.broadleafCrownDefinition()==1?274:g.mangroveRootsDefinition()==1?273:272;
                    if(shape!=current||slope!=1||(g.vegetationFlags()&BorealSlope.FLAG)==0)throw new AssertionError("Bundled boreal-slope preset required (272, 273 with mangrove roots, or 274 with broadleaf crowns): "+shape+" "+slope);
                    var random=level.getChunkSource().randomState();var lines=new ArrayList<String>();long logs=0,leaves=0;int stocked=0;
                    for(int dz=-HALF_CHUNKS;dz<HALF_CHUNKS;dz++)for(int dx=-HALF_CHUNKS;dx<HALF_CHUNKS;dx++){
                        var row=describe(level,cx+dx,cz+dz);var f=row.split(" ");logs+=Long.parseLong(f[3]);leaves+=Long.parseLong(f[4]);if(Long.parseLong(f[3])>0)stocked++;
                        if(dx>=-2&&dx<2&&dz>=-2&&dz<2)lines.add(row);
                    }
                    if(logs==0||leaves==0||stocked<8)throw new AssertionError("The sampled slope must carry a stand in schema 272: logs="+logs+" leaves="+leaves+" stocked chunks="+stocked);
                    var drainage=g.drainage(random);int ground=drainage.sample(SITE_X+3,SITE_Z+3).height();var edit=new BlockPos(SITE_X+3,ground,SITE_Z+3);
                    level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                    var context=StructurePieceSerializationContext.fromLevel(level);var box=new BoundingBox(SITE_X-16,ground,SITE_Z-16,SITE_X+16,ground+32,SITE_Z+16);
                    // Saved plans here pin the slope contract (definitions 257..512); the later broadleaf bit 256 is outside it and has its own qualification.
                    int slopeFlags=g.vegetationFlags()&~BroadleafCrowns.FLAG;
                    var tolerant=new ReimaginedTreeClearPiece(box,1,g.borealCrownDefinition(),slopeFlags);
                    if(tolerant.createTag(context).getIntOr("ClearDefinition",0)!=512)throw new AssertionError("Schema 272 plans carry the slope flag");
                    var plan=BorealClearSmoke.plan(tolerant,g,random);if(plan.size()<=50)throw new AssertionError("Saved slope plan clears the stand: "+plan.size());
                    var older=new ReimaginedTreeClearPiece(box,1,g.borealCrownDefinition(),slopeFlags&~BorealSlope.FLAG);
                    if(older.createTag(context).getIntOr("ClearDefinition",0)!=256||!BorealClearSmoke.plan(older,g,random).isEmpty())throw new AssertionError("A plan saved without the flag keeps the published treeless footprint");
                    // Range and coupling: definitions 257..512 are valid (513..1024 belong to the broadleaf bit), 0 and 1025 are not, and the flag needs its generator definition.
                    for(int bad:new int[]{512,-1})try{new ReimaginedTreeClearPiece(box,1,0,bad);throw new AssertionError("Vegetation flags outside 0..511 accepted: "+bad);}catch(IllegalArgumentException expected){}
                    new ReimaginedTreeClearPiece(box,1,0,511);
                    for(int bad:new int[]{0,1025}){var tag=tolerant.createTag(context).copy();tag.putInt("ClearDefinition",bad);try{BorealClearSmoke.load(context,tag);throw new AssertionError("Clear definition accepted: "+bad);}catch(IllegalArgumentException expected){}}
                    var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
                    var without=json.deepCopy();without.remove("broadleaf_crown_definition");without.remove("mangrove_roots_definition");without.remove("boreal_slope_definition");without.addProperty("shape_version",271);
                    var gateless=RealisticGenerator.CODEC.codec().parse(ops,without).getOrThrow();
                    try{BorealClearSmoke.plan(BorealClearSmoke.load(context,tolerant.createTag(context).copy()),gateless,random);throw new AssertionError("A bit-128 plan replayed under a generator without the definition");}catch(java.lang.reflect.InvocationTargetException expected){if(!(expected.getCause() instanceof IllegalStateException))throw expected;}
                    for(var mutation:new String[][]{{"shape_version","270"},{"shape_version","271"},{"shape_version",current==274?"275":"274"},{"shape_version",current==273?"272":"273"},{"boreal_slope_definition","0"},{"boreal_slope_definition","2"},{"montane_forest_definition","0"},{"alluvial_floodplain_definition","0"}}){
                        var bad=json.deepCopy();bad.addProperty(mutation[0],Integer.parseInt(mutation[1]));
                        if(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent())throw new AssertionError("Mismatched schema/definition accepted: "+mutation[0]+"="+mutation[1]);
                    }
                    var missing=json.deepCopy();missing.remove("boreal_slope_definition");
                    if(RealisticGenerator.CODEC.codec().parse(ops,missing).result().isPresent())throw new AssertionError("Schema 272 without its definition accepted");
                    NbtIo.writeCompressed(tolerant.createTag(context),dir.resolve("clear-plan.nbt"));
                    Files.writeString(dir.resolve("clear-footprint.txt"),planDigest(plan));Files.write(signature,lines);
                    server.saveEverything(false,true,true);
                    Files.writeString(dir.resolve("result.txt"),"PASS fresh272: "+logs+" logs, "+leaves+" leaves, "+stocked+" stocked chunks of "+(4*HALF_CHUNKS*HALF_CHUNKS)+", clear plan "+plan.size()+" cells, edit "+edit.toShortString()+"\n");
                }
                case "verifyMontane271"->{
                    if(shape!=271||slope!=0||g.montaneForestDefinition()!=1||(g.vegetationFlags()&BorealSlope.FLAG)!=0)throw new AssertionError("Saved montane 271 world must reopen as 271 with the slope definition absent: "+shape+" "+slope);
                    var context=StructurePieceSerializationContext.fromLevel(level);
                    var piece=BorealClearSmoke.load(context,NbtIo.readCompressed(dir.resolve("original-clear.nbt"),NbtAccounter.unlimitedHeap()));
                    if(piece.createTag(context).getIntOr("ClearDefinition",0)!=256)throw new AssertionError("Authentic montane plan keeps its saved definition");
                    var footprint=new java.util.TreeMap<>(BorealClearSmoke.plan(piece,g,level.getChunkSource().randomState())).toString();
                    if(!footprint.equals(Files.readString(dir.resolve("clear-footprint.txt"))))throw new AssertionError("Authentic montane saved clear plan replays a different footprint");
                    Files.writeString(dir.resolve("boreal-slope-result.txt"),"PASS verifyMontane271: shape 271, slope absent, saved plan 256 replays "+footprint.length()+" chars identically\n");
                    Files.writeString(dir.resolve("result.txt"),"PASS verifyMontane271\n");
                }
                case "verify272"->{
                    if(shape<272||shape>274||slope!=1||(shape>=273)!=(g.mangroveRootsDefinition()==1)||(shape==274)!=(g.broadleafCrownDefinition()==1))throw new AssertionError("Saved boreal-slope world must reopen as 272 (mangrove roots absent) or 273 (present) with its definition: "+shape+" "+slope);
                    var saved=Files.readAllLines(signature);int i=0;
                    for(int dz=-2;dz<2;dz++)for(int dx=-2;dx<2;dx++)if(!describe(level,cx+dx,cz+dz).equals(saved.get(i++)))throw new AssertionError("Saved schema 272 chunk changed on reopen: "+(cx+dx)+","+(cz+dz));
                    var edit=new BlockPos(SITE_X+3,g.drainage(level.getChunkSource().randomState()).sample(SITE_X+3,SITE_Z+3).height(),SITE_Z+3);
                    if(!level.getBlockState(edit).is(Blocks.GOLD_BLOCK))throw new AssertionError("Explicit edit lost across reopen at "+edit);
                    var context=StructurePieceSerializationContext.fromLevel(level);
                    var piece=BorealClearSmoke.load(context,NbtIo.readCompressed(dir.resolve("clear-plan.nbt"),NbtAccounter.unlimitedHeap()));
                    if(piece.createTag(context).getIntOr("ClearDefinition",0)!=512)throw new AssertionError("Saved plan keeps the slope flag across processes");
                    if(!planDigest(BorealClearSmoke.plan(piece,g,level.getChunkSource().randomState())).equals(Files.readString(dir.resolve("clear-footprint.txt"))))throw new AssertionError("Saved clear plan replays a different footprint after reopen");
                    Files.writeString(dir.resolve("result.txt"),"PASS verify272: "+saved.size()+" chunks identical, edit and clear plan retained\n");
                }
                default->throw new IllegalArgumentException("Unknown mode "+mode);
            }
        }catch(Throwable t){Blockbranch.LOGGER.error("BOREAL_SLOPE_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    /** {@code cx cz sha256(profile) logs leaves}: the saved profile hash and the wood/leaf block counts of its canopy. */
    static String describe(net.minecraft.server.level.ServerLevel level,int cx,int cz)throws Exception{
        var base=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain().base();
        int logs=0,leaves=0;for(int packed:base.canopy().encode()){int code=packed&CanopyLayer.STATE_MASK;if(WoodlandTrees.logCode(code))logs++;else leaves++;}
        var hash=MessageDigest.getInstance("SHA-256").digest(base.encode(level.palettedContainerFactory()).toString().getBytes());
        return cx+" "+cz+" "+HexFormat.of().formatHex(hash)+" "+logs+" "+leaves;
    }
    /** Cell count and digest of a clear footprint; the saved plan must replay the same cells in any process. */
    static String planDigest(Map<Long,Integer> plan)throws Exception{
        var hash=MessageDigest.getInstance("SHA-256");
        for(var e:new TreeMap<>(plan).entrySet())hash.update((e.getKey()+":"+e.getValue()+";").getBytes());
        return plan.size()+" "+HexFormat.of().formatHex(hash.digest());
    }
    private BorealSlopeQualification(){}
}
