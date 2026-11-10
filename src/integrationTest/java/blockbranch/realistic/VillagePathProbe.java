package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Explicit block-state expectations in a fresh QA chunk; no production predicate is used. */
public final class VillagePathProbe {
    public static final ChunkPos CHUNK=new ChunkPos(12288,-12288);
    private record Plant(String name,BlockState soil,BlockState state){}
    private static Plant plant(String name,BlockState state){return new Plant(name,Blocks.GRASS_BLOCK.defaultBlockState(),state);}
    private static BlockPos at(int index,int y){return new BlockPos(CHUNK.getMinBlockX()+1+(index%4)*4,y,CHUNK.getMinBlockZ()+1+(index/4)*4);}
    private static void set(WorldGenLevel level,BlockPos pos,BlockState state){OverworldCoverageQualification.require(level.setBlock(pos,state,2),"Probe write admitted "+pos);}
    private static void setup(WorldGenLevel level,BlockPos root,Plant plant){
        set(level,root,plant.soil());set(level,root.above(),plant.state());set(level,root.above(2),Blocks.AIR.defaultBlockState());
        if(plant.state().hasProperty(DoublePlantBlock.HALF))set(level,root.above(2),plant.state().setValue(DoublePlantBlock.HALF,DoubleBlockHalf.UPPER));
        OverworldCoverageQualification.require(plant.state().canSurvive(level,root.above()),"Supported fixture "+plant.name());
    }
    private static void expect(WorldGenLevel level,List<Object> expected,String label,BlockPos pos,BlockState state){
        OverworldCoverageQualification.require(level.getBlockState(pos).equals(state),"Probe "+label+" expected "+state+" at "+pos+" got "+level.getBlockState(pos));
        expected.add(Map.of("label",label,"pos",List.of(pos.getX(),pos.getY(),pos.getZ()),"state",state.toString()));
    }
    public static void generate(WorldGenLevel level){
        try{
            var g=(RealisticGenerator)level.getLevel().getChunkSource().getGenerator();var random=level.getLevel().getChunkSource().randomState();
            int y=TerrainShape.SEA_LEVEL+32;
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)y=Math.max(y,g.drainage(random).sample(CHUNK.getMinBlockX()+x,CHUNK.getMinBlockZ()+z).height()+32);
            var plants=List.of(plant("short_grass",Blocks.SHORT_GRASS.defaultBlockState()),plant("tall_grass",Blocks.TALL_GRASS.defaultBlockState()),
                plant("fern",Blocks.FERN.defaultBlockState()),plant("large_fern",Blocks.LARGE_FERN.defaultBlockState()),plant("short_dry_grass",Blocks.SHORT_DRY_GRASS.defaultBlockState()),
                plant("flower",Blocks.OXEYE_DAISY.defaultBlockState()),plant("leaf_litter",Blocks.LEAF_LITTER.defaultBlockState()),plant("berry_bush",Blocks.SWEET_BERRY_BUSH.defaultBlockState()),
                plant("fescue",MeadowContent.MEADOW_FESCUE.defaultBlockState()),plant("shrub",SteppeContent.SILVER_SHRUB.defaultBlockState()),
                new Plant("beach_grass",SandyShoreContent.ROOT_BOUND_SAND.defaultBlockState(),SandyShoreContent.BEACH_GRASS.defaultBlockState()));
            var expected=new ArrayList<Object>();var cases=new ArrayList<String>();int index=0;
            for(var plant:plants){
                var root=at(index++,y);setup(level,root,plant);var lawn=root.east();setup(level,lawn,plant);
                if(index==1){
                    var door=root.south().above();set(level,door.below(),Blocks.STONE_BRICKS.defaultBlockState());
                    var lower=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH).setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER);
                    set(level,door,lower);set(level,door.above(),lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
                    expect(level,expected,"native_door_lower",door,lower);expect(level,expected,"native_door_upper",door.above(),lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
                }
                VillagePaths.place(level,()->set(level,root,Blocks.DIRT_PATH.defaultBlockState()));
                expect(level,expected,plant.name()+"_path",root,Blocks.DIRT_PATH.defaultBlockState());
                expect(level,expected,plant.name()+"_feet",root.above(),Blocks.AIR.defaultBlockState());
                expect(level,expected,plant.name()+"_head",root.above(2),Blocks.AIR.defaultBlockState());
                expect(level,expected,plant.name()+"_lawn",lawn.above(),plant.state());
                if(plant.state().hasProperty(DoublePlantBlock.HALF))expect(level,expected,plant.name()+"_lawn_upper",lawn.above(2),plant.state().setValue(DoublePlantBlock.HALF,DoubleBlockHalf.UPPER));
                cases.add(plant.name());
            }
            var orphan=at(index++,y);set(level,orphan,Blocks.GRASS_BLOCK.defaultBlockState());set(level,orphan.above(),Blocks.AIR.defaultBlockState());
            set(level,orphan.above(2),Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF,DoubleBlockHalf.UPPER));
            VillagePaths.place(level,()->set(level,orphan,Blocks.DIRT_PATH.defaultBlockState()));
            expect(level,expected,"orphan_feet",orphan.above(),Blocks.AIR.defaultBlockState());expect(level,expected,"orphan_upper",orphan.above(2),Blocks.AIR.defaultBlockState());cases.add("orphan_upper");
            var grass=plant("short_grass",Blocks.SHORT_GRASS.defaultBlockState());
            var rewritten=at(index++,y);setup(level,rewritten,grass);
            VillagePaths.place(level,()->{set(level,rewritten,Blocks.DIRT_PATH.defaultBlockState());set(level,rewritten,grass.soil());});
            expect(level,expected,"rewritten_soil",rewritten,grass.soil());expect(level,expected,"rewritten_plant",rewritten.above(),grass.state());cases.add("path_overwritten_with_soil");
            var outside=at(index++,y);setup(level,outside,grass);set(level,outside,Blocks.DIRT_PATH.defaultBlockState());
            expect(level,expected,"non_village_plant_untouched",outside.above(),grass.state());cases.add("non_village_write");
            var solid=at(index,y);set(level,solid,grass.soil());set(level,solid.above(),Blocks.STONE_BRICKS.defaultBlockState());
            VillagePaths.place(level,()->set(level,solid,Blocks.DIRT_PATH.defaultBlockState()));
            expect(level,expected,"solid_native_block_untouched",solid.above(),Blocks.STONE_BRICKS.defaultBlockState());cases.add("solid_native_block");
            OverworldCoverageQualification.write("controlled-path-probe.json",Map.of("chunk",List.of(CHUNK.x(),CHUNK.z()),"baseY",y,"cases",cases,"expected",expected));
        }catch(Exception e){throw new IllegalStateException("Controlled path fixture failed",e);}
    }
    public static void verify(ServerLevel level)throws Exception{
        if(!Boolean.getBoolean("blockbranch.villageProbe"))return;
        var file=Path.of("controlled-path-probe.json");boolean reopen=Files.exists(file);level.getChunk(CHUNK.x(),CHUNK.z());
        OverworldCoverageQualification.require(Files.exists(file),"Controlled path fixture generated");
        var report=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        for(var item:report.getAsJsonArray("expected")){
            var entry=item.getAsJsonObject();var xyz=entry.getAsJsonArray("pos");var pos=new BlockPos(xyz.get(0).getAsInt(),xyz.get(1).getAsInt(),xyz.get(2).getAsInt());
            OverworldCoverageQualification.require(level.getBlockState(pos).toString().equals(entry.get("state").getAsString()),"Controlled fixture persists "+entry.get("label")+" at "+pos);
        }
        OverworldCoverageQualification.write("controlled-path-probe-result.json",Map.of("reopen",reopen,"cases",report.getAsJsonArray("cases").size(),"blockStates",report.getAsJsonArray("expected").size(),"passed",true));
    }
}
