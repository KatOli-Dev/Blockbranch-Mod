package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;

/** Preserve frozen profiles and native edits; optional fresh differences only establish witness coverage. */
public final class DrySteppeSavedSmoke {
    private static Path ledgerPath(){return Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("dry-steppe-witness.properties");}
    public static void prepare(ServerLevel level,RealisticGenerator generator)throws IOException {
        var random=level.getChunkSource().randomState();var center=SteppeSmoke.fixture(generator,random);
        BlockPos harvested=center,grown=null;
        for(int dz=-48;dz<=48&&grown==null;dz++)for(int dx=-48;dx<=48;dx++){
            if(Math.abs(dx)+Math.abs(dz)<24)continue;
            int x=center.getX()+dx,z=center.getZ()+dz;var p=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
            if(level.getBlockState(p.above()).is(SteppeContent.SILVER_SHRUB)){grown=p;break;}
        }
        require(grown!=null,"Second natural shrub witness");
        var shrub=SteppeContent.SILVER_SHRUB;
        require(shrub.clip(level,harvested.above(),level.getBlockState(harvested.above())),"Native clipped retained crown");
        require(shrub.clip(level,grown.above(),level.getBlockState(grown.above())),"Native second crown clipped");
        shrub.performBonemeal(level,level.getRandom(),grown.above(),level.getBlockState(grown.above()),BonemealSource.INTERACTION);
        level.setBlock(grown.below(6),Blocks.GOLD_BLOCK.defaultBlockState(),3);
        var witnesses=new LinkedHashMap<String,BlockPos>();witnesses.put("harvested",harvested);witnesses.put("grown",grown);
        int grass=0,soil=0;
        for(int dz=-96;dz<=96;dz+=7)for(int dx=-96;dx<=96;dx+=7){
            int x=center.getX()+dx,z=center.getZ()+dz,c=generator.steppeCover(x,z,random);if(c!=1&&c!=3)continue;
            var p=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
            if(c==3&&grass<24&&level.getBlockState(p.above()).is(Blocks.SHORT_DRY_GRASS)){level.destroyBlock(p.above(),false);witnesses.put("grass"+grass++,p);}
            else if(c==1&&soil<16){level.setBlock(p.below(6),Blocks.GOLD_BLOCK.defaultBlockState(),3);witnesses.put("soil"+soil++,p);}
        }
        require(grass==24&&soil==16,"Spaced original grass harvest and bare soil markers");
        var ledger=new Properties();ledger.setProperty("names",String.join(",",witnesses.keySet()));
        for(var entry:witnesses.entrySet()) {
            String name=entry.getKey();var p=entry.getValue();var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();
            ledger.setProperty(name,p.getX()+","+p.getY()+","+p.getZ());
            ledger.setProperty(name+".profile",digest(terrain.base().encode(level.palettedContainerFactory()).toString()));
            for(int dy=-8;dy<=7;dy++) {
                var at=p.above(dy);ledger.setProperty(name+".base."+dy,terrain.base().state(at.getX(),at.getY(),at.getZ()).toString());
                ledger.setProperty(name+".live."+dy,level.getBlockState(at).toString());
            }
            terrain.flush();level.getChunkAt(p).markUnsaved();
        }
        try(var out=Files.newBufferedWriter(ledgerPath())){ledger.store(out,"Read-only comparison of frozen profiles and actual native clip/regrowth/marker edits");}
        verify(level);
        Blockbranch.LOGGER.info("DRY_STEPPE_SAVED_LEDGER_PREPARED harvested={} grown={} profileSha256=true",harvested,grown);
    }
    public static void verify(ServerLevel level)throws IOException {
        var ledger=new Properties();try(var in=Files.newBufferedReader(ledgerPath())){ledger.load(in);}
        for(String name:ledger.getProperty("names").split(",")) {
            String[] xyz=ledger.getProperty(name).split(",");var p=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            var chunk=level.getChunkAt(p);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();
            var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            require(digest(tag.toString()).equals(ledger.getProperty(name+".profile")),"Entire immutable saved profile retained "+name);
            for(int dy=-8;dy<=7;dy++) {
                var at=p.above(dy);String expected=ledger.getProperty(name+".base."+dy);
                require(base.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15).toString().equals(expected),"Saved base/decoded/materialized shrub unchanged "+name+" dy="+dy);
                require(level.getBlockState(at).toString().equals(ledger.getProperty(name+".live."+dy)),"Saved live player edit unchanged "+name+" dy="+dy);
            }
            terrain.flush();terrain.evictOutside(-100,-90);
            for(int dy=-8;dy<=7;dy++)require(level.getBlockState(p.above(dy)).toString().equals(ledger.getProperty(name+".live."+dy)),"Saved edits survive eviction "+name);
            if(name.equals("harvested"))require(level.getBlockState(p.above()).is(SteppeContent.SILVER_SHRUB)&&level.getBlockState(p.above()).getValue(SteppeShrubBlock.CLIPPED)&&!base.state(p.getX(),p.getY()+1,p.getZ()).getValue(SteppeShrubBlock.CLIPPED),"Native clipped crown retained over mature base");
            else if(name.equals("grown"))require(level.getBlockState(p.above()).is(SteppeContent.SILVER_SHRUB)&&!level.getBlockState(p.above()).getValue(SteppeShrubBlock.CLIPPED)&&level.getBlockState(p.below(6)).is(Blocks.GOLD_BLOCK),"Native regrown crown and player marker retained");
        }
        if(Boolean.getBoolean("blockbranch.drySteppeExpectFreshDifferences")){
            var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
            var chunks=new HashSet<Long>();int untouched=0,changedHarvest=0;
            for(String name:ledger.getProperty("names").split(",")){
                var xyz=ledger.getProperty(name).split(",");int x=Integer.parseInt(xyz[0]),y=Integer.parseInt(xyz[1]),z=Integer.parseInt(xyz[2]);var p=new BlockPos(x,y,z);
                if(name.startsWith("grass")&&generator.steppeCover(x,z,random)==1&&level.getBlockState(p.above()).isAir())changedHarvest++;
                int cx=x>>4,cz=z>>4;if(!chunks.add(((cx&0xffffffffL)|((long)cz<<32))))continue;
                var base=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var ground=tag.getIntArray("ground").orElseThrow();var covers=tag.getByteArray("steppe").orElseThrow();
                for(int n=0;n<256;n++){
                    int wx=cx*16+(n&15),wz=cz*16+(n>>4);if(covers[n]!=1&&covers[n]!=3)continue;
                    if(covers[n]==generator.steppeCover(wx,wz,random))continue;
                    var at=new BlockPos(wx,ground[n]+1,wz);if(level.getBlockState(at)==base.state(wx,at.getY(),wz))untouched++;
                }
            }
            require(untouched>0&&changedHarvest>0,"Saved old 1/3 bases and harvested old grass must differ from the fresh candidate");
            Blockbranch.LOGGER.info("DRY_STEPPE_OLD_SAVE_FRESH_DIFFERENCES_PASS untouchedOldCells={} harvestedOldGrassNowFreshBare={} restore=false",untouched,changedHarvest);
        }
        Blockbranch.LOGGER.info("DRY_STEPPE_SAVED_WITNESSES_PASS restore=false currentGeneratorEquality=false immutableProfiles=true nativeClipRegrowthMarkerEdits=true");
    }
    private static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private DrySteppeSavedSmoke(){}
}
