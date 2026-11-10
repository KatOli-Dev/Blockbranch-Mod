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
public final class GravelDesertSavedSmoke {
    private static Path ledgerPath(){return Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("gravel-desert-witness.properties");}
    public static void prepare(ServerLevel level,RealisticGenerator generator)throws IOException {
        var random=level.getChunkSource().randomState();var center=DesertSmoke.fixture(generator,random);
        var witnesses=new LinkedHashMap<String,BlockPos>();
        for(int role=1;role<=3;role++){
            int found=0;
            for(int dz=-96;dz<=96&&found<4;dz+=3)for(int dx=-96;dx<=96&&found<4;dx+=3){
                int x=center.getX()+dx,z=center.getZ()+dz;if(generator.desertCover(x,z,random)!=role)continue;
                var p=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
                if(witnesses.values().stream().anyMatch(q->Math.abs(q.getX()-x)+Math.abs(q.getZ()-z)<12))continue;
                String name="role"+role+"-"+(found<2?"untouched"+found:found==2?"edited":"marker");
                if(found==2){
                    if(role==1)require(DesertContent.PAVEMENT.disturb(level,p)&&level.getBlockState(p).is(Blocks.GRAVEL),"Genuine natural pavement disturbance");
                    else require(level.destroyBlock(p,false)&&level.getBlockState(p).isAir(),"Native gravel/sand excavation");
                }
                if(found==3)level.setBlock(p.below(6),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                witnesses.put(name,p);found++;
            }
            require(found==4,"Untouched, edit and marker witnesses for role "+role);
        }
        var ledger=new Properties();ledger.setProperty("names",String.join(",",witnesses.keySet()));
        for(var entry:witnesses.entrySet()){
            String name=entry.getKey();var p=entry.getValue();var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();
            ledger.setProperty(name,p.getX()+","+p.getY()+","+p.getZ());
            ledger.setProperty(name+".profile",digest(terrain.base().encode(level.palettedContainerFactory()).toString()));
            for(int dy=-8;dy<=7;dy++){var at=p.above(dy);ledger.setProperty(name+".base."+dy,terrain.base().state(at.getX(),at.getY(),at.getZ()).toString());ledger.setProperty(name+".live."+dy,level.getBlockState(at).toString());}
            terrain.flush();level.getChunkAt(p).markUnsaved();
        }
        try(var out=Files.newBufferedWriter(ledgerPath())){ledger.store(out,"Frozen old-production bases and native disturbance, excavation, markers and untouched roles 1/2/3");}
        verify(level);Blockbranch.LOGGER.info("GRAVEL_DESERT_SAVED_LEDGER_PREPARED witnesses={} center={}",witnesses.size(),center);
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
            if(name.equals("role1-edited"))require(base.state(p.getX(),p.getY(),p.getZ()).is(DesertContent.PAVEMENT)&&level.getBlockState(p).is(Blocks.GRAVEL),"Disturbed pavement retained over pavement base");
            if(name.equals("role2-edited")||name.equals("role3-edited"))require(level.getBlockState(p).isAir(),"Excavation retained");
            if(name.endsWith("marker"))require(level.getBlockState(p.below(6)).is(Blocks.GOLD_BLOCK),"Player marker retained");
        }
        verifyFrozenFineSoil(level);
        Blockbranch.LOGGER.info("GRAVEL_DESERT_SAVED_WITNESSES_PASS restore=false currentGeneratorEquality=false immutableProfiles=true nativeDisturbanceExcavationMarkers=true untouchedRoles=1,2,3");
    }
    /** Optional coordinates come from closed old-world NBT, never from the fresh selector. */
    private static void verifyFrozenFineSoil(ServerLevel level)throws IOException {
        var path=ledgerPath().resolveSibling("gravel-desert-fine-soil-witness.properties");if(!Files.exists(path))return;
        var ledger=new Properties();try(var in=Files.newBufferedReader(path)){ledger.load(in);}int count=0;
        for(String name:ledger.getProperty("names").split(",")){
            var xyz=ledger.getProperty(name).split(",");var p=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();var base=terrain.base();var tag=base.encode(level.palettedContainerFactory());
            int index=(p.getZ()&15)*16+(p.getX()&15);
            require(tag.getByteArray("desert").orElseThrow()[index]==4&&tag.getIntArray("ground").orElseThrow()[index]==p.getY(),"Frozen role4 and ground coordinate retained "+name);
            var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            for(int dy=-1;dy<=0;dy++){
                var at=p.above(dy);var expected=(dy==0?Blocks.COARSE_DIRT:Blocks.DIRT).defaultBlockState();
                require(base.state(at.getX(),at.getY(),at.getZ())==expected&&copy.state(at.getX(),at.getY(),at.getZ())==expected&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15)==expected,"Stored/decoded/materialized old fine soil retained "+name+" dy="+dy);
                require(level.getBlockState(at)==expected,"Native old fine soil and dirt underlayer retained "+name+" dy="+dy);
            }
            require(level.getBlockState(p.above()).isAir(),"Untouched bare fine-soil witness remains bare "+name);
            count++;Blockbranch.LOGGER.info("GRAVEL_DESERT_SAVED_FINE_SOIL_WITNESS name={} pos={} storedRole=4 top=coarse_dirt underlayer=dirt freshSelector=false restore=false",name,p);
        }
        Blockbranch.LOGGER.info("GRAVEL_DESERT_SAVED_FINE_SOIL_PASS witnesses={} storedProfilesOnly=true freshSelector=false restore=false",count);
    }
    private static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private GravelDesertSavedSmoke(){}
}
