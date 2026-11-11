package blockbranch.realistic;

import blockbranch.cubic.TerrainProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

/** Production-path evidence that dripstone and sulfur habitat windows, formations and cave labels follow the
 * ice-free rock datum: under a grounded continental sheet ice is overburden, so rock is labelled and decorated
 * exactly as if the sheet were absent, nothing is labelled or placed inside ice, and without a sheet (gate off,
 * legacy, alpine caps) the datum is the unchanged surface. Counts are bounded observations at seed 424242. */
final class PolarHabitatChecks {
    private static final String[] BANDS={"thin(<48m)","intermediate(48-319m)","sulfur-band(320-383m)","thick(>=384m)","no-sheet"};
    private static final long[][] COUNTS=new long[5][9];
    private static final int COLUMNS=0,DRIPSTONE=1,SULFUR=2,ICE_LABELS=3,OLD_DATUM_ICE_LABELS=4,PLACEMENTS=5,FORMATIONS=6,CELLS=7,SECTION_CELLS=8;
    private static final TreeMap<Long,Long> SIGNATURES=new TreeMap<>();
    private static final Set<Long> SKIP=new HashSet<>();
    private PolarHabitatChecks(){}

    static int band(int ice){return ice==0?4:ice<48?0:ice<320?1:ice<384?2:3;}
    private static boolean sulfurOrDrip(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder,boolean sulfur){var id=holder.unwrapKey().orElseThrow().identifier().toString();return sulfur?id.contains("sulfur"):id.contains("dripstone");}

    /** One loaded chunk: datum, formations and every cave label, compared with the independent biome source. */
    static void chunk(ServerLevel level,RealisticGenerator g,RandomState random,DrainageNetwork d,ChunkAccess chunk,TerrainProfile profile){
        long seed=random.seed();var source=g.getBiomeSource().createUncachedResolver(random);int cx=chunk.getPos().x(),cz=chunk.getPos().z();long sig=1469598103934665603L;
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int wx=cx*16+x,wz=cz*16+z,bed=d.bedHeight(wx,wz),ice=d.continentalIceThickness(wx,wz);
            require(profile.habitatGround(wx,wz)==bed,"Habitat datum is the rock surface at "+wx+","+wz+": "+profile.habitatGround(wx,wz)+" vs "+bed);
            require(ice>0||profile.habitatGround(wx,wz)==profile.groundHeight(wx,wz),"Without a sheet the datum is the unchanged ground at "+wx+","+wz);
            COUNTS[band(ice)][COLUMNS]++;
        }
        var layer=profile.dripstone();var placements=layer.placements();sig=31*sig+Arrays.hashCode(placements);
        for(int i=0;i<placements.length;i+=3){
            int x=placements[i]&15,z=placements[i]>>4,y=placements[i+1],kind=placements[i+2],wx=cx*16+x,wz=cz*16+z,bed=d.bedHeight(wx,wz),b=band(d.continentalIceThickness(wx,wz));
            require(y<bed,"Formation lies below the rock surface at "+wx+","+y+","+wz+" bed "+bed);
            require(DripstoneHabitat.contains(seed,wx,y,wz,bed),"Formation lies in the rock-datum habitat window at "+wx+","+y+","+wz);
            COUNTS[b][PLACEMENTS]++;var pos=new BlockPos(wx,y,wz);
            if(SKIP.contains(pos.asLong()))continue;
            var state=level.getBlockState(pos);
            require(!state.is(Blocks.PACKED_ICE)&&!state.is(Blocks.ICE)&&!state.is(Blocks.SNOW_BLOCK),"Formation never inside ice at "+pos);
            if(kind>=2){require(state.is(Blocks.POINTED_DRIPSTONE),"Pointed formation present at "+pos+": "+state);COUNTS[b][FORMATIONS]++;}
            else require(!state.isAir(),"Formation facing block present at "+pos);
        }
        for(int qz=0;qz<4;qz++)for(int qx=0;qx<4;qx++){
            int gx=cx*4+qx,gz=cz*4+qz,x=gx*4,z=gz*4,bed=d.bedHeight(x,z),surface=d.sample(x,z).height(),ice=d.continentalIceThickness(x,z),b=band(ice);
            double province=DripstoneHabitat.province(seed,x,z);
            for(int qy=Math.floorDiv(Math.max(TerrainShape.FLOOR,bed-400),4);qy<=Math.floorDiv(bed,4);qy++){
                var held=profile.biome(gx,qy,gz);require(held.equals(source.getNoiseBiome(gx,qy,gz)),"Profile and biome source agree at "+gx+","+qy+","+gz);
                COUNTS[b][CELLS]++;sig=31*sig+held.unwrapKey().orElseThrow().identifier().hashCode();
                if(sulfurOrDrip(held,false))COUNTS[b][DRIPSTONE]++;if(sulfurOrDrip(held,true))COUNTS[b][SULFUR]++;
            }
            for(int qy=Math.floorDiv(bed,4)+1;qy<=Math.floorDiv(surface,4);qy+=ice>0?8:1){
                var held=profile.biome(gx,qy,gz);require(held.equals(source.getNoiseBiome(gx,qy,gz)),"Profile and biome source agree in ice at "+gx+","+qy+","+gz);
                if(sulfurOrDrip(held,false)||sulfurOrDrip(held,true)){COUNTS[b][ICE_LABELS]++;}
                // Negative control: the previous overburden datum would have labelled this ice cell.
                if(ice>0&&DripstoneHabitat.contains(seed,x,qy*4,z,surface,province))COUNTS[b][OLD_DATUM_ICE_LABELS]++;
            }
        }
        // The loaded sections' biome containers (their rock-bounds gate decides whether a section is labelled at all) and the fast
        // per-chunk resolver native biome filling uses are separate producers; both must give the same labels as the full resolver.
        int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){int bed=d.bedHeight(cx*16+x,cz*16+z);lo=Math.min(lo,bed);hi=Math.max(hi,bed);}
        lo=Math.max(TerrainShape.FLOOR,lo-400);
        var fast=g.getBiomeSource().createResolverForChunk(random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),cx*4,0,cz*4,4,0,4);
        for(int sy=Math.floorDiv(lo,16);sy<=Math.floorDiv(hi,16);sy++){
            var section=chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
            for(int qz=0;qz<4;qz++)for(int qx=0;qx<4;qx++)for(int qy=0;qy<4;qy++){
                int gx=cx*4+qx,gz=cz*4+qz,gy=sy*4+qy,bed=d.bedHeight(gx*4,gz*4);if(gy*4>bed||gy*4<bed-400)continue;
                var expected=source.getNoiseBiome(gx,gy,gz);
                require(section.getBiomes().get(qx,qy,qz).equals(expected),"Section biome container agrees at "+gx+","+gy+","+gz);
                require(fast.getNoiseBiome(gx,gy,gz).equals(expected),"Fast chunk resolver agrees at "+gx+","+gy+","+gz);
                COUNTS[band(d.continentalIceThickness(gx*4,gz*4))][SECTION_CELLS]++;
            }
        }
        SIGNATURES.put(chunk.getPos().pack(),sig);
    }

    /** Fixed deterministic sites, one per sheet-thickness band and habitat province, from the 16 km survey grid. */
    static List<int[]> sites(DrainageNetwork d,long seed){
        int[][] specs={{10,47,0},{100,300,0},{330,383,1},{400,100000,0},{400,100000,1},{10,47,1}};var out=new ArrayList<int[]>();
        for(int[] spec:specs){
            boolean found=false;
            for(int z=-2000000;z<=2000000&&!found;z+=16000)for(int x=-2000000;x<=2000000&&!found;x+=16000){
                int t=d.continentalIceThickness(x,z);if(t<spec[0]||t>spec[1]||d.bedHeight(x,z)<TerrainShape.FLOOR+420)continue;
                double province=spec[2]==0?DripstoneHabitat.province(seed,x,z):DripstoneHabitat.province(seed^0x73756c6675725f31L,x,z);
                if(province<(spec[2]==0?.35:.5))continue;
                out.add(new int[]{x,z,spec[0],spec[2]});found=true;
            }
            require(found,"Survey site for sheet "+spec[0]+".."+spec[1]+" province "+spec[2]);
        }
        return out;
    }

    /** Extra windows over every sheet band, a saved player edit pair, per-chunk signatures and a tamper negative. */
    static Map<String,Object> windows(ServerLevel level,RealisticGenerator g,RandomState random,DrainageNetwork d,boolean reopen,Path dir)throws Exception{
        var file=dir.resolve("habitat-signatures.txt");var edits=dir.resolve("habitat-edits.txt");var siteList=sites(d,random.seed());
        BlockPos air=null,gold=null;
        if(reopen){var lines=Files.readAllLines(edits);air=parse(lines.get(0));gold=parse(lines.get(1));}
        var chunks=new ArrayList<ChunkAccess>();ChunkAccess tamperChunk=null;
        for(var site:siteList){
            int radius=site[2]==400&&site[3]==0?2:1;
            for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++){
                var chunk=level.getChunk((site[0]>>4)+dx,(site[1]>>4)+dz);var profile=((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain().base();
                chunks.add(chunk);
                if(tamperChunk==null&&profile.perennialIce().enabled()&&profile.dripstone().placements().length>0&&firstPlacementSheet(d,chunk,profile)>=48)tamperChunk=chunk;
            }
        }
        if(reopen){SKIP.add(air.asLong());SKIP.add(gold.asLong());}
        for(var chunk:chunks){var profile=((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain().base();chunk(level,g,random,d,chunk,profile);
            // Saved layers are authoritative after reopen: identical placements and labels as in the generating process.
        }
        require(tamperChunk!=null,"A sheet-covered profile with formations exists for the tamper negative");
        var factory=level.palettedContainerFactory();var tamperProfile=((blockbranch.cubic.SparseColumn)tamperChunk).blockbranch$terrain().base();var encoded=tamperProfile.encode(factory);
        TerrainProfile.decode(encoded,factory);
        var bad=encoded.copy();var drip=bad.getCompoundOrEmpty("dripstone");int[] moved=drip.getIntArray("placements").orElseThrow().clone();
        // Move the first formation to 10 m below the rock surface: too shallow for the rock-datum window (48..320 m), but at
        // sheet thickness + 10 m below the ice-inclusive surface, inside the old overburden window. Only the window check can
        // reject it with this message, so the assertion fails if validation went back to the ice-inclusive height.
        int packed=moved[0],tx=tamperChunk.getPos().x()*16+(packed&15),tz=tamperChunk.getPos().z()*16+(packed>>4);
        require(d.continentalIceThickness(tx,tz)>=48,"Tamper column lies under a sheet of at least 48 m");
        moved[1]=d.bedHeight(tx,tz)-10;drip.putIntArray("placements",moved);bad.put("dripstone",drip);
        String reason=null;try{TerrainProfile.decode(bad,factory);}catch(IllegalArgumentException expected){reason=expected.getMessage();}
        boolean rejected=reason!=null&&reason.contains("outside its province/depth");
        require(rejected,"A formation above the rock-datum window is rejected on decode by the window check: "+reason);
        if(!reopen){
            var map=new StringBuilder();
            for(var e:SIGNATURES.entrySet())map.append(e.getKey()).append(' ').append(e.getValue()).append('\n');
            Files.writeString(file,map.toString());
            // Saved player edits: remove one pointed formation and place gold inside rock beside the thick window.
            BlockPos formation=null,rock=null;
            for(var chunk:chunks){var profile=((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain().base();int cx=chunk.getPos().x(),cz=chunk.getPos().z();var pl=profile.dripstone().placements();
                for(int i=0;i<pl.length&&formation==null;i+=3)if(pl[i+2]>=2&&d.continentalIceThickness(cx*16+(pl[i]&15),cz*16+(pl[i]>>4))>=384)formation=new BlockPos(cx*16+(pl[i]&15),pl[i+1],cz*16+(pl[i]>>4));
                if(formation!=null&&rock==null)for(int z=0;z<16&&rock==null;z++)for(int x=0;x<16&&rock==null;x++){int wx2=cx*16+x,wz2=cz*16+z;if(d.continentalIceThickness(wx2,wz2)<384)continue;var probe=new BlockPos(wx2,d.bedHeight(wx2,wz2)-60,wz2);var st=level.getBlockState(probe);if(st.is(Blocks.STONE)||st.is(Blocks.DEEPSLATE))rock=probe;}
            }
            require(formation!=null&&rock!=null,"A formation and a rock cell exist under a thick sheet for the saved edits");
            level.setBlock(formation,Blocks.AIR.defaultBlockState(),3);level.setBlock(rock,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            Files.writeString(edits,format(formation)+"\n"+format(rock)+"\n");
        }else{
            var expected=new TreeMap<Long,Long>();for(var line:Files.readAllLines(file)){var part=line.split(" ");expected.put(Long.parseLong(part[0]),Long.parseLong(part[1]));}
            for(var e:expected.entrySet())require(Objects.equals(SIGNATURES.get(e.getKey()),e.getValue()),"Reopened profile placements and labels equal the saved ones for chunk "+e.getKey());
            require(level.getBlockState(air).isAir()&&level.getBlockState(gold).is(Blocks.GOLD_BLOCK),"Saved formation removal and rock edit survive the relaunch");
        }
        var report=new LinkedHashMap<String,Object>();var sitesOut=new ArrayList<Object>();for(var s:siteList)sitesOut.add(List.of(s[0],s[1]));report.put("sites",sitesOut);report.put("signedChunks",SIGNATURES.size());
        return report;
    }
    /** Underground structure fitting through the production path ({@code Structure.findValidGenerationPoint}, wrapped by
     * the overworld mixin): under a sheet the structure sits in the rock below the ice-free bed exactly as without one.
     * Under a sheet the exact target of the deterministic-depth kinds must be the rock-bed one, not the higher ice-inclusive one. No chunk is generated; only the terrain queries the fit itself makes are used. */
    static Map<String,Object> structures(ServerLevel level,RealisticGenerator g,RandomState random,DrainageNetwork d){
        long seed=random.seed();var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var manager=level.getServer().getStructureTemplateManager();var surface=((ScreeBiomeSource)g.getBiomeSource()).surfaceResolver();
        var out=new LinkedHashMap<String,Object>();
        for(var kind:new String[]{"ancient_city","trial_chambers","stronghold","mineshaft"}){
            var holder=registry.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,net.minecraft.resources.Identifier.withDefaultNamespace(kind)));
            int[] accepted=new int[5],attempts=new int[5],differs=new int[5];
            for(int z=-2000000;z<=2000000;z+=16000)for(int x=-2000000;x<=2000000;x+=16000){
                int ice=d.continentalIceThickness(x,z),slot=ice==0?4:ice<48?0:ice<320?1:3,cap=kind.equals("ancient_city")?(slot==4?40:400):6;
                if(attempts[slot]>=cap||kind.equals("ancient_city")&&(slot==0||d.bedHeight(x,z)<=384))continue;
                attempts[slot]++;var chunkPos=new net.minecraft.world.level.ChunkPos(x>>4,z>>4);
                
                var rng=new net.minecraft.world.level.levelgen.WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(0));rng.setLargeFeatureSeed(seed,chunkPos.x(),chunkPos.z());
                var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(),g,g.getBiomeSource(),random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),surface,random,manager,rng,seed,chunkPos,level,b->true);
                var stub=holder.value().findValidGenerationPoint(context);if(stub.isEmpty())continue;var box=stub.get().getPiecesBuilder().getBoundingBox();
                int lowest=Integer.MAX_VALUE;
                for(int wz=box.minZ();wz<=box.maxZ();wz+=8)for(int wx=box.minX();wx<=box.maxX();wx+=8){lowest=Math.min(lowest,d.bedHeight(wx,wz));}
                accepted[slot]++;
                require(box.maxY()<=lowest-32,kind+" lies below the rock bed (not the ice) at chunk "+chunkPos+": maxY "+box.maxY()+" lowest bed "+lowest+" sheet "+ice);
                if(kind.equals("trial_chambers")||kind.equals("ancient_city")){
                    // Deterministic depth: the exact target is the rock-bed one (the ice-inclusive surface would be higher by up to the sheet).
                    int bed=d.bedHeight(chunkPos.getMiddleBlockX(),chunkPos.getMiddleBlockZ()),desired=kind.equals("ancient_city")?DeepDarkHabitat.cityFloor(bed,box.getYSpan()):bed-192;
                    require(box.minY()==Math.max(TerrainShape.FLOOR+64,Math.min(desired,lowest-32-box.getYSpan())),kind+" depth follows the rock bed at chunk "+chunkPos+": minY "+box.minY()+" bed "+bed+" sheet "+ice);
                }
                if(ice>0)differs[slot]++;
            }
            var row=new LinkedHashMap<String,Object>();String[] names={"thin","intermediate","unused","thick","no-sheet"};
            for(int i=0;i<5;i++)if(i!=2)row.put(names[i],"accepted "+accepted[i]+" of "+attempts[i]+" sites"+(i==4?"":" ("+differs[i]+" under a sheet)"));
            out.put(kind,row);
            if(kind.equals("ancient_city"))require(accepted[3]+accepted[1]>0,"At least one Ancient City is accepted under a sheet over a bed above 384 m");
            else require(accepted[3]+accepted[1]>0&&accepted[4]>0,kind+" is accepted under a sheet and without one");
        }
        return out;
    }
    private static int firstPlacementSheet(DrainageNetwork d,ChunkAccess chunk,TerrainProfile profile){int packed=profile.dripstone().placements()[0];return d.continentalIceThickness(chunk.getPos().x()*16+(packed&15),chunk.getPos().z()*16+(packed>>4));}
    private static String format(BlockPos p){return p.getX()+" "+p.getY()+" "+p.getZ();}
    private static BlockPos parse(String s){var p=s.split(" ");return new BlockPos(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]));}

    /** Aggregated bounded counts after all chunks, with the assertions that make them non-vacuous. */
    static Map<String,Object> finish(boolean legacy){
        var out=new LinkedHashMap<String,Object>();
        for(int b=0;b<5;b++){var row=new LinkedHashMap<String,Object>();row.put("columns",COUNTS[b][COLUMNS]);row.put("labelledCells",COUNTS[b][CELLS]);row.put("dripstoneLabelCells",COUNTS[b][DRIPSTONE]);row.put("sulfurLabelCells",COUNTS[b][SULFUR]);
            row.put("labelsInIce",COUNTS[b][ICE_LABELS]);row.put("previousDatumLabelsInIce",COUNTS[b][OLD_DATUM_ICE_LABELS]);row.put("formationPlacements",COUNTS[b][PLACEMENTS]);row.put("sectionAndFastResolverCells",COUNTS[b][SECTION_CELLS]);row.put("pointedFormationsInRock",COUNTS[b][FORMATIONS]);out.put(BANDS[b],row);
            require(COUNTS[b][ICE_LABELS]==0,"No dripstone or sulfur label inside ice in band "+BANDS[b]);}
        if(!legacy){
            require(COUNTS[0][COLUMNS]>0&&COUNTS[1][COLUMNS]>0&&COUNTS[2][COLUMNS]>0&&COUNTS[3][COLUMNS]>0,"Every sheet band is present");
            for(int b=0;b<4;b++)require(COUNTS[b][DRIPSTONE]>0||b==2,"Rock under band "+BANDS[b]+" carries the dripstone label");
            require(COUNTS[2][SULFUR]>0||COUNTS[3][SULFUR]>0||COUNTS[0][SULFUR]>0,"Sulfur label present in rock under a sheet");
            require(COUNTS[3][PLACEMENTS]>0&&COUNTS[3][FORMATIONS]>0,"Formations are planned and generated in rock under a thick sheet");
            require(COUNTS[0][OLD_DATUM_ICE_LABELS]+COUNTS[1][OLD_DATUM_ICE_LABELS]+COUNTS[3][OLD_DATUM_ICE_LABELS]>0,"Negative control: the previous overburden datum labels ice cells");
        }
        return out;
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
