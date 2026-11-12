package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.cubic.TerrainProfile;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

/** Finite, deterministic leaf-litter measurements in native generated windows. Each window
 * also records exact native block hashes (raw, litter-normalized, litter-removed) over
 * ground-4..ground+24 so an original build's outputs can be compared bit-for-bit.
 * Sites are fixed seed-424242 land samples (32 km grid) at low latitude, where the
 * labelled biome was the survey's selected surface biome; the recorded actual biome
 * is the authority. Counts are sample observations, not area fractions. The
 * clustering statistics compare 8x8-column cells against independent placement. */
public final class LeafLitterAudit {
    private static final String[] NAMES={"forest","warm-subhumid-oak-pine","birch-forest","flower-forest","dappled-oak-birch","dark-forest","jungle","humid-subtropical-oak-fern","maritime-redwood","seasonal-dry-broadleaf","coastal-winter-rain-oak","old-growth-birch"};
    private static final int[][] SITES={{304000,784000},{-848000,-272000},{624000,1040000},{368000,656000},{752000,1072000},{1648000,-944000},{432000,80000},{1360000,-48000},{1200000,-1104000},{1616000,-176000},{144000,-688000},{656000,944000}};
    private static final int HALF_CHUNKS=4,CELL=8;
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.leafLitterDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
            var d=g.drainage(random);var rows=new JsonArray();
            variantContract();
            var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
            int gateCases=gateMatrix(ops,g);
            int profileChunks=0;
            for(int site=0;site<SITES.length;site++){
                int cx=SITES[site][0]>>4,cz=SITES[site][1]>>4,x0=(cx-HALF_CHUNKS)*16,z0=(cz-HALF_CHUNKS)*16,size=HALF_CHUNKS*32;
                for(int dz=-HALF_CHUNKS;dz<HALF_CHUNKS;dz++)for(int dx=-HALF_CHUNKS;dx<HALF_CHUNKS;dx++){
                    var chunk=level.getChunk(cx+dx,cz+dz);
                    var profile=((SparseColumn)chunk).blockbranch$terrain().base();var factory=level.palettedContainerFactory();
                    if(!TerrainProfile.decode(profile.encode(factory),factory).encode(factory).equals(profile.encode(factory)))throw new AssertionError("Leaf-litter profile round trip");
                    profileChunks++;
                }
                var litter=new boolean[size][size];var clear=new boolean[size][size];
                java.security.MessageDigest raw,normalized,without;
                try{raw=java.security.MessageDigest.getInstance("SHA-256");normalized=java.security.MessageDigest.getInstance("SHA-256");without=java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
                int air=net.minecraft.world.level.block.Block.getId(Blocks.AIR.defaultBlockState()),defaultLitter=net.minecraft.world.level.block.Block.getId(Blocks.LEAF_LITTER.defaultBlockState());
                var states=new TreeMap<String,Integer>();var soils=new TreeMap<String,Integer>();var tops=new TreeMap<String,Integer>();
                int clearColumns=0,litterColumns=0,blocked=0,adjacentPairs=0,litterPairs=0,identicalPairs=0;
                for(int z=0;z<size;z++)for(int x=0;x<size;x++){
                    int wx=x0+x,wz=z0+z,ground=d.sample(wx,wz).height();var top=level.getBlockState(new BlockPos(wx,ground+1,wz));
                    for(int y=ground-4;y<=ground+24;y++){var st=level.getBlockState(new BlockPos(wx,y,wz));int id=net.minecraft.world.level.block.Block.getId(st);boolean isLitter=st.is(Blocks.LEAF_LITTER);
                        update(raw,id);update(normalized,isLitter?defaultLitter:id);update(without,isLitter?air:id);}
                    boolean wet=!top.getFluidState().isEmpty()||d.sample(wx,wz).waterLevel()!=DrainageNetwork.DRY;
                    if(top.is(BlockTags.LOGS)||top.is(BlockTags.LEAVES)||wet){blocked++;continue;}
                    clear[z][x]=true;clearColumns++;
                    if(top.is(Blocks.LEAF_LITTER)){litter[z][x]=true;litterColumns++;states.merge(top.toString(),1,Integer::sum);soils.merge(String.valueOf(level.getBlockState(new BlockPos(wx,ground,wz)).getBlock()),1,Integer::sum);}
                    else tops.merge(String.valueOf(top.getBlock()),1,Integer::sum);
                }
                int pairs=0;
                for(int z=0;z<size;z++)for(int x=0;x+1<size;x++)if(clear[z][x]&&clear[z][x+1]){pairs++;if(litter[z][x]){adjacentPairs++;if(litter[z][x+1]){litterPairs++;
                    if(level.getBlockState(new BlockPos(x0+x,d.sample(x0+x,z0+z).height()+1,z0+z)).equals(level.getBlockState(new BlockPos(x0+x+1,d.sample(x0+x+1,z0+z).height()+1,z0+z))))identicalPairs++;}}}
                double p=clearColumns==0?0:litterColumns/(double)clearColumns,sum=0,sumSq=0,effective=0;int cells=0;
                for(int cz2=0;cz2<size;cz2+=CELL)for(int cx2=0;cx2<size;cx2+=CELL){int n=0,l=0;
                    for(int z=cz2;z<cz2+CELL;z++)for(int x=cx2;x<cx2+CELL;x++)if(clear[z][x]){n++;if(litter[z][x])l++;}
                    if(n>=32){double f=l/(double)n;sum+=f;sumSq+=f*f;effective+=n;cells++;}}
                double mean=0,var=0;
                if(cells>0){mean=sum/cells;var=sumSq/cells-mean*mean;}
                double expected=cells==0||p<=0||p>=1?0:p*(1-p)/(effective/cells);
                var row=new JsonObject();row.addProperty("site",NAMES[site]);row.addProperty("x",SITES[site][0]);row.addProperty("z",SITES[site][1]);
                row.addProperty("actualBiome",String.valueOf(level.getBiome(new BlockPos(SITES[site][0],d.sample(SITES[site][0],SITES[site][1]).height()+1,SITES[site][1])).unwrapKey().map(k->k.identifier().toString()).orElse("?")));
                row.addProperty("nativeSha256",java.util.HexFormat.of().formatHex(raw.digest()));row.addProperty("litterNormalizedSha256",java.util.HexFormat.of().formatHex(normalized.digest()));row.addProperty("withoutLitterSha256",java.util.HexFormat.of().formatHex(without.digest()));
                row.addProperty("columns",size*size);row.addProperty("blockedColumns",blocked);row.addProperty("clearColumns",clearColumns);row.addProperty("litterColumns",litterColumns);
                row.addProperty("litterShareOfClear",p);row.addProperty("cellsUsed",cells);row.addProperty("cellShareMean",mean);row.addProperty("cellShareVariance",var);
                row.addProperty("dispersionVsIndependent",expected<=0?0:var/expected);
                row.addProperty("adjacentClearPairs",pairs);row.addProperty("litterThenLitter",adjacentPairs==0?0:litterPairs/(double)adjacentPairs);
                row.addProperty("identicalStateShareOfLitterPairs",litterPairs==0?0:identicalPairs/(double)litterPairs);
                row.add("states",new Gson().toJsonTree(states));row.add("soils",new Gson().toJsonTree(soils));row.add("otherTops",new Gson().toJsonTree(tops));rows.add(row);
                Files.writeString(dir.resolve("partial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));
            }
            var pockets=new StringBuilder();
            for(int[] site:VISUAL_SITES){var c=pocket(level,d,site[0],site[1]);pockets.append("site ").append(site[0]).append(',').append(site[1]).append(" camera ").append(c[0]).append(',').append(c[1]).append('\n').append(strip(level,d,c)).append('\n');}
            Files.writeString(dir.resolve("pockets.txt"),pockets.toString());
            server.saveEverything(false,true,true);
            var report=new JsonObject();report.addProperty("profileRoundTripChunks",profileChunks);report.addProperty("gateMatrixCases",gateCases);report.addProperty("seed",424242);report.addProperty("window","8x8 chunks (128x128 columns) per site");report.add("sites",rows);
            report.add("generator",RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow());
            Files.writeString(dir.resolve("audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(dir.resolve("result.txt"),"PASS finite leaf-litter audit; no density target or visual claim\n");
        }catch(Throwable t){Blockbranch.LOGGER.error("LEAF_LITTER_AUDIT_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    /** Saved-schema identity through the real codec for the independent litter and ice opt-ins: exactly (264, neither), (265, ice),
     * (266, litter) and (267, both) are readable and round-trip; every other pairing, including a number under the wrong definitions, is rejected. */
    static int gateMatrix(net.minecraft.resources.RegistryOps<JsonElement> ops,RealisticGenerator live){
        var base=RealisticGenerator.CODEC.codec().encodeStart(ops,live).getOrThrow().getAsJsonObject();
        WarmXericFixtures.published267(base);
        int cases=0;
        // {schema, litter definition (-1 removes the field), ice definition}
        for(int[] c:new int[][]{{264,-1,0},{265,-1,1},{266,1,0},{267,1,1}}){var json=variant(base,c[0],c[1],c[2]);var parsed=RealisticGenerator.CODEC.codec().parse(ops,json);
            if(parsed.error().isPresent())throw new AssertionError("Readable pairing "+java.util.Arrays.toString(c)+": "+parsed.error().get().message());
            var round=parsed.getOrThrow();if(round.leafLitterDefinition()!=(c[1]==1?1:0)||round.continentalIceDefinition()!=c[2])throw new AssertionError("Definitions decode as saved "+c[0]);
            if(!variant(RealisticGenerator.CODEC.codec().encodeStart(ops,round).getOrThrow().getAsJsonObject(),c[0],c[1],c[2]).equals(json))throw new AssertionError("Encode round trip "+c[0]);cases++;}
        for(int[] c:new int[][]{{265,1,0},{265,-1,0},{265,1,1},{266,-1,0},{266,0,0},{266,2,0},{266,1,1},{266,-1,1},{264,1,0},{264,-1,1},{267,1,0},{267,-1,1},{267,-1,0},{268,1,1},{263,-1,0}}){
            if(RealisticGenerator.CODEC.codec().parse(ops,variant(base,c[0],c[1],c[2])).error().isEmpty())throw new AssertionError("Unreadable pairing accepted: "+java.util.Arrays.toString(c));cases++;}
        var malformed=variant(base,267,1,1);malformed.addProperty("leaf_litter_definition","invalid");
        if(RealisticGenerator.CODEC.codec().parse(ops,malformed).error().isEmpty())throw new AssertionError("Malformed definition");cases++;
        return cases;
    }
    /** Copy of the encoded generator with the given saved schema, litter definition (-1 removes the field) and ice definition (0 removes field and holder). */
    private static JsonObject variant(JsonObject base,int schema,int litter,int ice){var json=base.deepCopy();json.addProperty("shape_version",schema);if(litter<0)json.remove("leaf_litter_definition");else json.addProperty("leaf_litter_definition",litter);
        if(ice==0){json.remove("continental_ice_definition");json.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);}else{json.addProperty("continental_ice_definition",ice);var biomes=json.getAsJsonObject("additional_biomes");if(!biomes.has(ContinentalIceField.KEY))biomes.addProperty(ContinentalIceField.KEY,presetIceHolder());}return json;}
    /** The ice holder id of the bundled preset, so the codec matrix also runs against a legacy (no-ice) live world. */
    private static String presetIceHolder(){
        try(var input=new java.io.InputStreamReader(java.util.Objects.requireNonNull(LeafLitterAudit.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),java.nio.charset.StandardCharsets.UTF_8)){
            return com.google.gson.JsonParser.parseReader(input).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("additional_biomes").get(ContinentalIceField.KEY).getAsString();
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    /** Every reserved cover code renders its declared block state above exactly its base litter code's soil, in both the woodland and warm arrays. */
    private static void variantContract(){
        int ground=64;
        for(int c=LeafLitter.PODZOL_FIRST;c<=LeafLitter.WOODLAND_LAST;c++){
            int base=LeafLitter.woodlandBase(c);
            for(int y=ground-3;y<=ground+3;y++){
                var variant=TerrainProfile.material(-64,320,y,ground,0,0,0,0,0,0,c,0,0,0,0,0);
                var original=TerrainProfile.material(-64,320,y,ground,0,0,0,0,0,0,base,0,0,0,0,0);
                var expected=y==ground+1?TerrainProfile.leafLitterState(LeafLitter.woodlandIndex(c)):original;
                if(!variant.equals(expected))throw new AssertionError("Woodland litter code "+c+" y="+(y-ground)+": "+variant+" expected "+expected);
            }
        }
        for(int c=LeafLitter.WARM_FIRST;c<=LeafLitter.WARM_LAST;c++){
            for(int y=ground-3;y<=ground+3;y++){
                var variant=TerrainProfile.material(-64,320,y,ground,0,0,0,0,0,0,0,0,0,c,0,0);
                var original=TerrainProfile.material(-64,320,y,ground,0,0,0,0,0,0,0,0,0,3,0,0);
                var expected=y==ground+1?TerrainProfile.leafLitterState(LeafLitter.warmIndex(c)):original;
                if(!variant.equals(expected))throw new AssertionError("Warm litter code "+c+" y="+(y-ground)+": "+variant+" expected "+expected);
            }
        }
        if(!TerrainProfile.leafLitterState(0).equals(Blocks.LEAF_LITTER.defaultBlockState()))throw new AssertionError("Index 0 is the original default litter state");
        var distinct=new HashSet<net.minecraft.world.level.block.state.BlockState>();
        for(int i=0;i<LeafLitter.STATES;i++)distinct.add(TerrainProfile.leafLitterState(i));
        if(distinct.size()!=LeafLitter.STATES)throw new AssertionError("Sixteen distinct litter states");
    }
    /** Fixed ground-view sites shared with the client capture: forest, redwood, jungle, winter-rain oak, dry broadleaf. */
    public static final int[][] VISUAL_SITES={{304000,784000},{1200000,-1104000},{432000,80000},{144000,-688000},{1616000,-176000}};
    /** First flat, unobstructed 9x10 ground strip outward from a site: independent of litter and every cover choice. */
    public static int[] pocket(net.minecraft.server.level.ServerLevel level,DrainageNetwork d,int ex,int ez){
        for(int r=0;r<=24;r++)for(int z=ez-r;z<=ez+r;z++)for(int x=ex-r;x<=ex+r;x++){
            if(Math.max(Math.abs(x-ex),Math.abs(z-ez))!=r)continue;
            if(clearStrip(level,d,x,z))return new int[]{x,z};
        }
        throw new AssertionError("No clear flat ground pocket within 24 blocks");
    }
    private static boolean clearStrip(net.minecraft.server.level.ServerLevel level,DrainageNetwork d,int x,int z){
        int base=d.sample(x,z).height();
        for(int pz=z;pz<z+10;pz++)for(int px=x-4;px<=x+4;px++){
            var s=d.sample(px,pz);if(Math.abs(s.height()-base)>1||s.waterLevel()!=DrainageNetwork.DRY)return false;
            for(int y=s.height()+1;y<=s.height()+4;y++){var pos=new BlockPos(px,y,pz);var state=level.getBlockState(pos);
                if(state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)||!state.getFluidState().isEmpty()||(!state.isAir()&&!state.getCollisionShape(level,pos).isEmpty()))return false;}
        }
        return true;
    }
    /** ASCII map of ground+1 over the strip the camera looks across: L litter, . air, other plants by initial. */
    private static String strip(net.minecraft.server.level.ServerLevel level,DrainageNetwork d,int[] c){
        var b=new StringBuilder();
        for(int z=c[1];z<c[1]+10;z++){for(int x=c[0]-4;x<=c[0]+4;x++){var st=level.getBlockState(new BlockPos(x,d.sample(x,z).height()+1,z));
            b.append(st.isAir()?'.':st.is(Blocks.LEAF_LITTER)?'L':Character.toUpperCase(String.valueOf(st.getBlock().builtInRegistryHolder().key().identifier().getPath()).charAt(0)));}b.append('\n');}
        return b.toString();
    }
    private static void update(java.security.MessageDigest digest,int value){digest.update((byte)(value>>>24));digest.update((byte)(value>>>16));digest.update((byte)(value>>>8));digest.update((byte)value);}
    private LeafLitterAudit(){}
}
