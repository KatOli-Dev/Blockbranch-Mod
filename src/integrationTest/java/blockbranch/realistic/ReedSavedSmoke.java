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

/** Exact saved-profile and live-edit witnesses; never compare an old save with a fresh selector. */
public final class ReedSavedSmoke {
    private static Path ledgerPath(){return Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("reed-witness.properties");}
    public static void prepare(ServerLevel level,RealisticGenerator generator)throws IOException {
        var random=level.getChunkSource().randomState();var center=ReedbedSmoke.fixture(generator,random);
        BlockPos harvested=null,grown=null;
        search:for(int dz=-64;dz<=64;dz++)for(int dx=-64;dx<=64;dx++) {
            if(Math.abs(dx)+Math.abs(dz)<24)continue;
            int x=center.getX()+dx,z=center.getZ()+dz;var p=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
            var base=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base();
            if(!base.state(x,p.getY()+1,z).is(ReedbedContent.LAKE_REED)||!level.getBlockState(p.above()).is(ReedbedContent.LAKE_REED))continue;
            if(harvested==null){harvested=p;continue;}
            if(p.distManhattan(harvested)<8||base.state(x,p.getY()+4,z).is(ReedbedContent.LAKE_REED))continue;
            grown=p;break search;
        }
        require(harvested!=null&&grown!=null,"Separate old/fresh natural reed harvest and growth witnesses");
        level.destroyBlock(harvested.above(),false);
        require(!level.getBlockState(harvested.above()).is(ReedbedContent.LAKE_REED)&&level.getFluidState(harvested.above()).isSource(),"Actual native harvest retains root water");
        var root=grown.above();int steps=0;
        while(ReedbedContent.LAKE_REED.isValidBonemealTarget(level,root,level.getBlockState(root),BonemealSource.INTERACTION)) {
            require(++steps<=4,"Bounded witness growth");ReedbedContent.LAKE_REED.performBonemeal(level,level.getRandom(),root,level.getBlockState(root),BonemealSource.INTERACTION);
        }
        require(steps>0&&level.getBlockState(grown.above(4)).is(ReedbedContent.LAKE_REED),"Actual native grown witness reaches four");
        level.setBlock(harvested.below(7),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(harvested.below(6),ReedbedContent.WOVEN_REED_MAT.defaultBlockState(),2);
        var ledger=new Properties();
        for(var entry:Map.of("harvested",harvested,"grown",grown).entrySet()) {
            String name=entry.getKey();var p=entry.getValue();var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();
            ledger.setProperty(name,p.getX()+","+p.getY()+","+p.getZ());
            ledger.setProperty(name+".profile",digest(terrain.base().encode(level.palettedContainerFactory()).toString()));
            for(int dy=-8;dy<=7;dy++) {
                var at=p.above(dy);ledger.setProperty(name+".base."+dy,terrain.base().state(at.getX(),at.getY(),at.getZ()).toString());
                ledger.setProperty(name+".live."+dy,level.getBlockState(at).toString());
            }
            terrain.flush();level.getChunkAt(p).markUnsaved();
        }
        try(var out=Files.newBufferedWriter(ledgerPath())){ledger.store(out,"Read-only comparison of frozen profiles and actual native harvest/growth/mat edits");}
        verify(level);
        Blockbranch.LOGGER.info("REED_SAVED_LEDGER_PREPARED harvested={} grown={} profileSha256=true",harvested,grown);
    }
    public static void verify(ServerLevel level)throws IOException {
        var ledger=new Properties();try(var in=Files.newBufferedReader(ledgerPath())){ledger.load(in);}
        for(String name:List.of("harvested","grown")) {
            String[] xyz=ledger.getProperty(name).split(",");var p=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            var chunk=level.getChunkAt(p);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();
            var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            require(digest(tag.toString()).equals(ledger.getProperty(name+".profile")),"Entire immutable saved profile retained "+name);
            for(int dy=-8;dy<=7;dy++) {
                var at=p.above(dy);String expected=ledger.getProperty(name+".base."+dy);
                require(base.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15).toString().equals(expected),"Saved base/decoded/materialized reed unchanged "+name+" dy="+dy);
                require(level.getBlockState(at).toString().equals(ledger.getProperty(name+".live."+dy)),"Saved live player edit unchanged "+name+" dy="+dy);
            }
            terrain.flush();terrain.evictOutside(-100,-90);
            for(int dy=-8;dy<=7;dy++)require(level.getBlockState(p.above(dy)).toString().equals(ledger.getProperty(name+".live."+dy)),"Saved edits survive eviction "+name);
            if(name.equals("harvested"))require(!level.getBlockState(p.above()).is(ReedbedContent.LAKE_REED)&&base.state(p.getX(),p.getY()+1,p.getZ()).is(ReedbedContent.LAKE_REED)&&level.getBlockState(p.below(6)).is(ReedbedContent.WOVEN_REED_MAT),"Harvest and mat edit survive old natural reed base");
            else require(level.getBlockState(p.above(4)).is(ReedbedContent.LAKE_REED)&&!base.state(p.getX(),p.getY()+4,p.getZ()).is(ReedbedContent.LAKE_REED),"Native grown overlay survives old shorter base");
        }
        Blockbranch.LOGGER.info("REED_SAVED_WITNESSES_PASS restore=false currentGeneratorEquality=false immutableProfiles=true nativeHarvestGrowthMatEdits=true");
    }
    private static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private ReedSavedSmoke(){}
}
