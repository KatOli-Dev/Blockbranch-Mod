package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;

/** Read-only old-save verification, independent of the fresh flower selector. */
public final class FlowerColorSavedSmoke {
    private static Path ledgerPath(){return Path.of(System.getProperty("blockbranch.memorySmokeResult")).resolveSibling("flower-color-witness.properties");}
    public static void prepare(ServerLevel level,RealisticGenerator generator)throws IOException {
        var random=level.getChunkSource().randomState();var center=TemperateForestVariantSmoke.fixture(generator,random,TemperateForestVariants.FLOWER);
        var witnesses=new LinkedHashMap<String,BlockPos>();var originalSpecies=new TreeMap<String,Integer>();int harvested=0,untouched=0;
        for(int dz=-96;dz<=96;dz++)for(int dx=-96;dx<=96;dx++){
            int x=center.getX()+dx,z=center.getZ()+dz;
            int role=generator.woodlandCover(x,z,random);if(role<30||role>36)continue;
            var pos=new BlockPos(x,generator.drainage(random).sample(x,z).height()+1,z);
            if(!level.getBlockState(pos).is(BlockTags.SMALL_FLOWERS))continue;originalSpecies.merge(level.getBlockState(pos).toString(),1,Integer::sum);
            if(((x+z)&1)==0&&harvested<96){require(level.destroyBlock(pos,true),"Native flower harvest");witnesses.put("harvest"+harvested++,pos);}
            else if(untouched<96)witnesses.put("untouched"+untouched++,pos);
        }
        require(harvested==96&&untouched==96,"Distributed original flower harvest and untouched witnesses");
        var marker=center.below(6);level.setBlock(marker,Blocks.GOLD_BLOCK.defaultBlockState(),3);witnesses.put("marker",marker);
        var ledger=new Properties();ledger.setProperty("names",String.join(",",witnesses.keySet()));
        for(var entry:witnesses.entrySet()){
            String name=entry.getKey();var p=entry.getValue();var chunk=level.getChunkAt(p);var terrain=((SparseColumn)chunk).blockbranch$terrain();
            ledger.setProperty(name,p.getX()+","+p.getY()+","+p.getZ());
            ledger.setProperty(name+".profile",digest(terrain.base().encode(level.palettedContainerFactory()).toString()));
            for(int dy=-8;dy<=8;dy++){var at=p.above(dy);ledger.setProperty(name+".base."+dy,terrain.base().state(at.getX(),at.getY(),at.getZ()).toString());ledger.setProperty(name+".live."+dy,level.getBlockState(at).toString());}
            terrain.flush();chunk.markUnsaved();
        }
        try(var out=Files.newBufferedWriter(ledgerPath())){ledger.store(out,"Frozen full-profile SHA256 plus original native flower harvest, untouched flower and player marker states");}
        verify(level);
        var climate=new ClimateField(random.seed());int selected=0,surfaceSelected=0,samples=0;
        for(int dz=-96;dz<=96;dz+=4)for(int dx=-96;dx<=96;dx+=4){int x=center.getX()+dx,z=center.getZ()+dz;samples++;if(TemperateForestVariants.kind(random.seed(),x,z,generator.drainage(random),climate)==TemperateForestVariants.FLOWER)selected++;if(TemperateForestVariants.surfaceKind(random.seed(),x,z,generator.drainage(random),climate)==TemperateForestVariants.FLOWER)surfaceSelected++;}
        Blockbranch.LOGGER.info("FLOWER_COLOR_BASELINE_FOOTPRINT center={} radius=96 step=4 selected={}/{} surfaceSelected={}/{} nativeFlowerSpecies={}",center,selected,samples,surfaceSelected,samples,originalSpecies);
        Blockbranch.LOGGER.info("FLOWER_COLOR_LEDGER_PREPARED harvested={} untouched={} marker={} nativeDestroyWithDrops=true",harvested,untouched,marker);
    }
    public static void verify(ServerLevel level)throws IOException {
        var ledger=new Properties();try(var in=Files.newBufferedReader(ledgerPath())){ledger.load(in);}
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();int changedHarvest=0,changedUntouched=0;
        for(String name:ledger.getProperty("names").split(",")){
            var xyz=ledger.getProperty(name).split(",");var p=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
            var chunk=level.getChunkAt(p);var terrain=((SparseColumn)chunk).blockbranch$terrain();var base=terrain.base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());
            require(digest(tag.toString()).equals(ledger.getProperty(name+".profile")),"Entire immutable old profile retained "+name);
            for(int dy=-8;dy<=8;dy++){
                var at=p.above(dy);String expected=ledger.getProperty(name+".base."+dy);
                require(base.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.state(at.getX(),at.getY(),at.getZ()).toString().equals(expected)&&copy.generate(at.getY()>>4).getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15).toString().equals(expected),"Old base/decoded/materialized state retained "+name);
                require(level.getBlockState(at).toString().equals(ledger.getProperty(name+".live."+dy)),"Old live edit retained "+name);
            }
            terrain.flush();terrain.evictOutside(-100,-90);
            for(int dy=-8;dy<=8;dy++)require(level.getBlockState(p.above(dy)).toString().equals(ledger.getProperty(name+".live."+dy)),"Old live edit retained after eviction "+name);
            if(name.startsWith("harvest"))require(level.getBlockState(p).isAir()&&base.state(p.getX(),p.getY(),p.getZ()).is(BlockTags.SMALL_FLOWERS),"Harvested native flower remains air over old flower base");
            if(Boolean.getBoolean("blockbranch.flowerColorExpectFreshDifferences")&&!name.equals("marker")){
                var oldState=base.state(p.getX(),p.getY(),p.getZ());var freshState=generator.surfaceState(p.getX(),p.getY(),p.getZ(),random);
                if(oldState!=freshState){
                    if(name.startsWith("harvest"))changedHarvest++;else changedUntouched++;
                    Blockbranch.LOGGER.info("FLOWER_COLOR_OLD_SAVE_CHANGED_CHOICE name={} pos={} oldBase={} freshChoice={} savedLive={}",name,p,oldState,freshState,level.getBlockState(p));
                }
            }
        }
        if(Boolean.getBoolean("blockbranch.flowerColorExpectFreshDifferences")){require(changedHarvest>0&&changedUntouched>0,"Edited and untouched old flowers differ from fresh species");Blockbranch.LOGGER.info("FLOWER_COLOR_OLD_SAVE_FRESH_DIFFERENCES_PASS harvested={} untouched={}",changedHarvest,changedUntouched);}
        Blockbranch.LOGGER.info("FLOWER_COLOR_SAVED_WITNESSES_PASS restore=false currentGeneratorEquality=false immutableProfiles=true nativeHarvest=true playerMarker=true");
    }
    public static void fresh(ServerLevel level,RealisticGenerator generator){
        var random=level.getChunkSource().randomState();var center=TemperateForestVariantSmoke.fixture(generator,random,TemperateForestVariants.FLOWER);var species=new TreeMap<String,Integer>();var roles=new HashSet<Integer>();var decoded=new HashMap<Long,TerrainProfile>();int flowers=0,heightChecks=0,clippedChecks=0;
        for(int dz=-96;dz<=96;dz++)for(int dx=-96;dx<=96;dx++){
            int x=center.getX()+dx,z=center.getZ()+dz,role=generator.woodlandCover(x,z,random);if(role<30||role>36)continue;
            var p=new BlockPos(x,generator.drainage(random).sample(x,z).height()+1,z);var state=level.getBlockState(p);require(state==generator.surfaceState(x,p.getY(),z,random)&&state.is(BlockTags.SMALL_FLOWERS)&&state.canSurvive(level,p),"Fresh native flower species and support");var expected=switch(role){case 30->Blocks.DANDELION;case 31->Blocks.OXEYE_DAISY;case 32->Blocks.ALLIUM;case 33->Blocks.AZURE_BLUET;case 34->Blocks.POPPY;case 35->Blocks.CORNFLOWER;case 36->Blocks.LILY_OF_THE_VALLEY;default->throw new AssertionError("flower role");};
            require(state.is(expected),"Literal role material "+role);boolean representative=roles.add(role);
            long key=((x>>4)&0xffffffffL)|((long)(z>>4)<<32);var copy=decoded.computeIfAbsent(key,unused->TerrainProfile.decode(((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base().encode(level.palettedContainerFactory()),level.palettedContainerFactory()));
            require(copy.state(x,p.getY(),z)==state&&copy.generate(p.getY()>>4).getBlockState(x&15,p.getY()&15,z&15)==state,"Fresh codec/client materialization");
            var chunk=level.getChunkAt(p);
            for(var type:Heightmap.Types.values()){
                int analytic=generator.getBaseHeight(x,z,type,level,random),profile=copy.top(type.isOpaque(),x,z,level.getMaxY());
                require(chunk.getHeight(type,x&15,z&15)+1==analytic&&profile==analytic,"Fresh flower native/analytic/decoded-profile top "+type+" "+p);heightChecks++;
            }
            if(representative)for(var bounds:List.of(LevelHeightAccessor.create(p.getY()-1,1),LevelHeightAccessor.create(p.getY(),1),LevelHeightAccessor.create(p.getY()+1,2))){
                var column=generator.getBaseColumn(x,z,bounds,random);
                for(var type:Heightmap.Types.values()){
                    int top=bounds.getMinY();
                    for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(column.getBlock(y))){top=y+1;break;}
                    require(generator.getBaseHeight(x,z,type,bounds,random)==top&&Math.max(bounds.getMinY(),copy.top(type.isOpaque(),x,z,bounds.getMaxY()))==top,"Fresh flower clipped column/analytic/profile top "+type+" "+p);clippedChecks++;
                }
            }
            species.merge(state.toString(),1,Integer::sum);flowers++;
        }
        require(flowers>100&&species.size()>=2,"Substantial diverse fresh native flowers");if(Boolean.getBoolean("blockbranch.flowerColorRequireAllSpecies"))require(roles.size()==7,"All seven literal native flower species");Blockbranch.LOGGER.info("FLOWER_COLOR_FRESH_NATIVE_PASS flowers={} roles={} species={} heightChecks={} clippedChecks={}",flowers,roles,species,heightChecks,clippedChecks);
    }
    private static String digest(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private FlowerColorSavedSmoke(){}
}
