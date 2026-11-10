package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Frozen native profiles and edits, verified without consulting a fresh cover map or restoring terrain. */
public final class MeadowForbSavedSmoke {
    private static Path ledgerPath(){return Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("meadow-forb-witness.properties");}
    public static void prepare(ServerLevel level,RealisticGenerator generator)throws IOException {
        var random=level.getChunkSource().randomState();var center=MeadowForbSmoke.anchor(generator,random);
        Files.writeString(ledgerPath().resolveSibling("meadow-forb-anchor.properties"),"capturePoint="+center.getX()+","+center.getY()+","+center.getZ()+"\nflowerCriterion=false\n");
        var witnesses=new LinkedHashMap<String,BlockPos>();
        for(int role:new int[]{3,4,5,6}){
            int found=0;
            for(int dz=-192;dz<=192&&found<4;dz+=2)for(int dx=-192;dx<=192&&found<4;dx+=2){
                int x=center.getX()+dx,z=center.getZ()+dz;if(generator.meadowCover(x,z,random)!=role||generator.canopy(x,z,random).topY(x,z)!=Integer.MIN_VALUE)continue;
                var p=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
                if(witnesses.values().stream().anyMatch(q->Math.abs(q.getX()-x)+Math.abs(q.getZ()-z)<8))continue;
                var natural=RealisticGenerator.material(p.getY()+1,p.getY(),DrainageNetwork.DRY,0,0,0,0,role);
                if(!level.getBiome(p).is(Blockbranch.id("rolling_lowland_forb_grass_meadow"))||level.getBlockState(p.above())!=natural)continue;
                String action=found<2?"untouched"+found:found==2?"harvested":"marker";
                if(found==2){if(role==3)require(MeadowContent.MEADOW_FESCUE.divide(level,p.above(),natural),"Genuine native fescue clipping");else require(level.destroyBlock(p.above(),true)&&level.getBlockState(p.above()).isAir(),"Genuine native flower harvest "+role);}
                if(found==3)level.setBlock(p.below(6),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                witnesses.put("role"+role+"-"+action,p);found++;
            }
            require(found==4,"Natural untouched/harvest/marker witnesses for meadow role "+role);
        }
        var ledger=new Properties();ledger.setProperty("names",String.join(",",witnesses.keySet()));
        for(var entry:witnesses.entrySet()){
            String name=entry.getKey();var p=entry.getValue();var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();
            ledger.setProperty(name,p.getX()+","+p.getY()+","+p.getZ());
            ledger.setProperty(name+".profile",digest(terrain.base().encode(level.palettedContainerFactory()).toString()));
            for(int dy=-8;dy<=7;dy++){var at=p.above(dy);ledger.setProperty(name+".base."+dy,terrain.base().state(at.getX(),at.getY(),at.getZ()).toString());ledger.setProperty(name+".live."+dy,level.getBlockState(at).toString());}
            terrain.flush();level.getChunkAt(p).markUnsaved();
        }
        try(var out=Files.newBufferedWriter(ledgerPath())){ledger.store(out,"Frozen old-production meadow bases; real flower harvest, fescue clipping and gold markers");}
        verify(level);Blockbranch.LOGGER.info("MEADOW_FORB_SAVED_LEDGER_PREPARED witnesses={} center={}",witnesses.size(),center);
    }
    public static void verify(ServerLevel level)throws IOException {
        var ledger=new Properties();try(var in=Files.newBufferedReader(ledgerPath())){ledger.load(in);}
        for(String name:ledger.getProperty("names").split(",")){
            var xyz=ledger.getProperty(name).split(",");var p=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();var base=terrain.base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            require(digest(tag.toString()).equals(ledger.getProperty(name+".profile")),"Entire frozen profile retained "+name);
            for(int dy=-8;dy<=7;dy++){
                var at=p.above(dy);String expected=ledger.getProperty(name+".base."+dy);
                require(base.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15).toString().equals(expected),"Saved base/decoded/materialized unchanged "+name+" dy="+dy);
                require(level.getBlockState(at).toString().equals(ledger.getProperty(name+".live."+dy)),"Saved native edit unchanged "+name+" dy="+dy);
            }
            terrain.flush();terrain.evictOutside(-100,-90);
            for(int dy=-8;dy<=7;dy++)require(level.getBlockState(p.above(dy)).toString().equals(ledger.getProperty(name+".live."+dy)),"Saved edits survive eviction "+name);
            require(base.state(p.getX(),p.getY(),p.getZ()).is(Blocks.GRASS_BLOCK),"Original stored meadow sward retained");
            if(name.equals("role3-harvested"))require(level.getBlockState(p.above()).is(MeadowContent.MEADOW_FESCUE)&&level.getBlockState(p.above()).getValue(MeadowFescueBlock.CROPPED),"Saved clipped fescue retained");
            if(name.endsWith("harvested")&&!name.startsWith("role3"))require(level.getBlockState(p.above()).isAir(),"Saved flower harvest retained");
            if(name.endsWith("marker"))require(level.getBlockState(p.below(6)).is(Blocks.GOLD_BLOCK),"Player marker retained");
        }
        verifyBareSward(level);
        Blockbranch.LOGGER.info("MEADOW_FORB_SAVED_WITNESSES_PASS restore=false currentGeneratorEquality=false immutableProfiles=true nativeFlowerHarvestFescueClipMarkers=true originalRoles=3,4,5,6");
    }
    private static void verifyBareSward(ServerLevel level)throws IOException {
        var file=ledgerPath().resolveSibling("meadow-forb-bare-witness.properties");if(!Files.exists(file))return;
        var ledger=new Properties();try(var input=Files.newBufferedReader(file)){ledger.load(input);}int count=0,freshFlowers=0;
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
        for(String name:ledger.getProperty("names").split(",")){
            var xyz=ledger.getProperty(name).split(",");var p=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));var base=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());int index=(p.getZ()&15)*16+(p.getX()&15);
            require(tag.getByteArray("meadow").orElseThrow()[index]==1&&tag.getIntArray("ground").orElseThrow()[index]==p.getY(),"Frozen bare sward code/height retained "+name);var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            for(int dy=0;dy<=1;dy++){var at=p.above(dy);var expected=(dy==0?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState();require(base.state(at.getX(),at.getY(),at.getZ())==expected&&copy.state(at.getX(),at.getY(),at.getZ())==expected&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15)==expected&&level.getBlockState(at)==expected,"Native/stored/decoded/materialized original bare Grass+AIR retained "+name);}
            int fresh=generator.meadowCover(p.getX(),p.getZ(),random);if(fresh>=4&&fresh<=6)freshFlowers++;count++;
            Blockbranch.LOGGER.info("MEADOW_FORB_OLD_BARE_WITNESS name={} pos={} storedCode=1 storedAbove=AIR freshRole={} restore=false",name,p,fresh);
        }
        Blockbranch.LOGGER.info("MEADOW_FORB_OLD_BARE_PASS witnesses={} observationalFreshFlowers={} storedAIR=true restore=false freshEqualityAssertion=false",count,freshFlowers);
    }
    private static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private MeadowForbSavedSmoke(){}
}
