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
public final class TundraFloorSavedSmoke {
    private static Path ledgerPath(){return Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("tundra-floor-witness.properties");}
    public static void prepare(ServerLevel level,RealisticGenerator generator)throws IOException {
        var random=level.getChunkSource().randomState();var center=TundraSmoke.fixture(generator,random);var witnesses=new LinkedHashMap<String,BlockPos>();
        for(int role:new int[]{3,6}){
            int found=0,required=role==3?4:5;
            for(int dz=-96;dz<=96&&found<required;dz+=2)for(int dx=-96;dx<=96&&found<required;dx+=2){
                int x=center.getX()+dx,z=center.getZ()+dz;if(generator.borealCover(x,z,random)!=role||!ColdTundra.retained(x,z,generator.drainage(random),new ClimateField(random.seed())))continue;
                var p=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
                if(witnesses.values().stream().anyMatch(q->Math.abs(q.getX()-x)+Math.abs(q.getZ()-z)<8))continue;
                require(level.getBlockState(p).is(Blocks.MOSS_BLOCK),"Original cubic moss ground "+role);
                String action=found<2?"untouched"+found:found==required-1?"marker":role==6&&found==2?"carpet-removal":"ground-removal";
                if(action.equals("ground-removal")){require(level.destroyBlock(p,false)&&level.getBlockState(p).isAir(),"Native moss ground excavation");if(role==6)require(level.getBlockState(p.above()).isAir(),"Carpet loses excavated support");}
                if(action.equals("carpet-removal"))require(level.getBlockState(p.above()).is(Blocks.MOSS_CARPET)&&level.destroyBlock(p.above(),false)&&level.getBlockState(p.above()).isAir(),"Native original moss carpet removal");
                if(action.equals("marker"))level.setBlock(p.below(6),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                witnesses.put("role"+role+"-"+action,p);found++;
            }
            require(found==required,"Original moss ground/carpet untouched, excavation and marker witnesses "+role);
        }
        var ledger=new Properties();ledger.setProperty("names",String.join(",",witnesses.keySet()));
        for(var entry:witnesses.entrySet()){
            String name=entry.getKey();var p=entry.getValue();var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();
            ledger.setProperty(name,p.getX()+","+p.getY()+","+p.getZ());
            ledger.setProperty(name+".profile",digest(terrain.base().encode(level.palettedContainerFactory()).toString()));
            for(int dy=-8;dy<=7;dy++){var at=p.above(dy);ledger.setProperty(name+".base."+dy,terrain.base().state(at.getX(),at.getY(),at.getZ()).toString());ledger.setProperty(name+".live."+dy,level.getBlockState(at).toString());}
            terrain.flush();level.getChunkAt(p).markUnsaved();
        }
        try(var out=Files.newBufferedWriter(ledgerPath())){ledger.store(out,"Frozen old-production tundra moss bases, actual native carpet/ground excavation and gold markers");}
        verify(level);Blockbranch.LOGGER.info("TUNDRA_FLOOR_SAVED_LEDGER_PREPARED witnesses={} center={}",witnesses.size(),center);
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
            require(base.state(p.getX(),p.getY(),p.getZ()).is(Blocks.MOSS_BLOCK),"Original stored cubic moss retained "+name);
            if(name.contains("ground-removal"))require(level.getBlockState(p).isAir(),"Saved ground excavation retained");
            if(name.contains("carpet-removal"))require(level.getBlockState(p.above()).isAir(),"Saved carpet excavation retained");
            if(name.endsWith("marker"))require(level.getBlockState(p.below(6)).is(Blocks.GOLD_BLOCK),"Player marker retained");
        }
        Blockbranch.LOGGER.info("TUNDRA_FLOOR_SAVED_WITNESSES_PASS restore=false currentGeneratorEquality=false immutableProfiles=true nativeGroundCarpetRemovalMarkers=true originalRoles=3,6");
    }
    private static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private TundraFloorSavedSmoke(){}
}
