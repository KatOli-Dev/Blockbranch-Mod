package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Physical witnesses produced by the ordinary FEATURES pipeline, not direct feature calls. */
public final class OverworldFeatureQualification {
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
        var level=server.overworld();
        if(Files.exists(Path.of("features.json"))){
            var saved=com.google.gson.JsonParser.parseString(Files.readString(Path.of("features.json"))).getAsJsonObject();
            if(saved.has("geode")&&saved.getAsJsonObject("geode").has("grownBud")){
                var bone=point(saved.getAsJsonObject("fossil").get("firstBone").getAsString());var spawner=point(saved.getAsJsonObject("monsterRoom").get("spawner").getAsString());
                var bud=point(saved.getAsJsonObject("geode").get("grownBud").getAsString());var source=point(saved.getAsJsonObject("geode").get("budding").getAsString());
                OverworldCoverageQualification.require(level.getBlockState(bone).is(Blocks.BONE_BLOCK)&&level.getBlockEntity(spawner) instanceof SpawnerBlockEntity,"Fossil and dungeon persist");
                OverworldCoverageQualification.require(level.getBlockState(source).is(Blocks.BUDDING_AMETHYST)&&(level.getBlockState(bud).is(Blocks.SMALL_AMETHYST_BUD)||level.getBlockState(bud).is(Blocks.MEDIUM_AMETHYST_BUD)||level.getBlockState(bud).is(Blocks.LARGE_AMETHYST_BUD)||level.getBlockState(bud).is(Blocks.AMETHYST_CLUSTER)),"Budding geode and grown bud persist");
                OverworldCoverageQualification.write("features-reopen.json",Map.of("monsterRoom",true,"fossil",true,"nativeGrownBud",true));Files.writeString(Path.of("result.txt"),"PASS");return;
            }
        }
        var rows=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.overworldWitness","../overworld-coverage/starts.json")))).getAsJsonObject();
        var at=rows.getAsJsonObject("desert_pyramid").getAsJsonArray("chunk");int cx=at.get(0).getAsInt(),cz=at.get(1).getAsInt();
        Map<String,Object> room=null,fossil=null,geode=null;int admitted=0;var position=new BlockPos.MutableBlockPos();
        outer:for(int radius=0;radius<=20;radius++)for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++){
            if(Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
            var chunk=level.getChunk(cx+dx,cz+dz);admitted++;
            for(var be:chunk.getBlockEntities().values())if(room==null&&be instanceof SpawnerBlockEntity){
                var tag=be.saveWithFullMetadata(level.registryAccess());String saved=tag.toString();
                if(!saved.contains("minecraft:zombie")&&!saved.contains("minecraft:skeleton")&&!saved.contains("minecraft:spider"))continue;
                var p=be.getBlockPos();if(!level.getBlockState(p.below()).is(Blocks.COBBLESTONE)&&!level.getBlockState(p.below()).is(Blocks.MOSSY_COBBLESTONE))continue;
                int loot=0,chests=0;
                for(int z=p.getZ()-4;z<=p.getZ()+4;z++)for(int x=p.getX()-4;x<=p.getX()+4;x++)if(level.getBlockEntity(new BlockPos(x,p.getY(),z)) instanceof ChestBlockEntity chest&&BuiltInLootTables.SIMPLE_DUNGEON.equals(chest.getLootTable())){
                    chests++;chest.unpackLootTable(null);for(int i=0;i<chest.getContainerSize();i++)loot+=chest.getItem(i).getCount();
                }
                if(chests>0&&loot>0)room=Map.of("spawner",p.toShortString(),"nativeSpawner",saved,"nativeDungeonChests",chests,"generatedItems",loot);
            }
            var sparse=(blockbranch.cubic.SparseColumn)chunk;
            if((fossil==null||geode==null)&&sparse.blockbranch$terrain()!=null)for(int sy:sparse.blockbranch$terrain().modifiedView()){
                if(geode==null){var layer=sparse.blockbranch$read(sy);if(layer.maybeHas(s->s.is(Blocks.BUDDING_AMETHYST)))geode=geode(level,chunk,sy,layer);}
                var section=sparse.blockbranch$read(sy);if(!section.maybeHas(s->s.is(Blocks.BONE_BLOCK)))continue;
                int count=0;BlockPos first=null;
                for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(section.getBlockState(x,y,z).is(Blocks.BONE_BLOCK)){count++;if(first==null)first=new BlockPos((chunk.getPos().x()<<4)+x,(sy<<4)+y,(chunk.getPos().z()<<4)+z);}
                if(count>8&&fossil==null)fossil=Map.of("firstBone",first.toShortString(),"boneBlocksInSection",count);
            }
            if(room!=null&&fossil!=null&&geode!=null)break outer;
            if(admitted%64==0)blockbranch.Blockbranch.LOGGER.info("OVERWORLD_FEATURE_SCAN chunks={} room={} fossil={} geode={}",admitted,room!=null,fossil!=null,geode!=null);
        }
        OverworldCoverageQualification.write("features.json",Map.of("chunks",admitted,"monsterRoom",room==null?Map.of():room,"fossil",fossil==null?Map.of():fossil,"geode",geode==null?Map.of():geode));
        OverworldCoverageQualification.require(room!=null&&fossil!=null&&geode!=null,"Natural monster room, fossil and functioning amethyst geode");
        server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS");
    }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("OVERWORLD_FEATURE_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
    static BlockPos point(String value){var p=value.split(", ");return new BlockPos(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]));}
    static Map<String,Object> geode(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.chunk.LevelChunk chunk,int sy,net.minecraft.world.level.chunk.LevelChunkSection section){
        int budding=0,amethyst=0,calcite=0,basalt=0;BlockPos source=null,target=null;
        for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            var state=section.getBlockState(x,y,z);if(state.is(Blocks.AMETHYST_BLOCK))amethyst++;if(state.is(Blocks.CALCITE))calcite++;if(state.is(Blocks.SMOOTH_BASALT))basalt++;
            if(!state.is(Blocks.BUDDING_AMETHYST))continue; budding++;
            var p=new BlockPos(chunk.getPos().getMinBlockX()+x,(sy<<4)+y,chunk.getPos().getMinBlockZ()+z);
            if(source==null)for(var direction:Direction.values()){
                var neighbor=p.relative(direction);if(BuddingAmethystBlock.canClusterGrowAtState(level.getBlockState(neighbor))){source=p;target=neighbor;break;}
            }
        }
        if(source==null||budding<2||amethyst<5||calcite<5||basalt<5)return null;
        var random=net.minecraft.util.RandomSource.create(1141);int ticks=0;
        while(ticks++<1024&&BuddingAmethystBlock.canClusterGrowAtState(level.getBlockState(target)))level.getBlockState(source).randomTick(level,source,random);
        OverworldCoverageQualification.require(level.getBlockState(target).is(Blocks.SMALL_AMETHYST_BUD)||level.getBlockState(target).is(Blocks.MEDIUM_AMETHYST_BUD)||level.getBlockState(target).is(Blocks.LARGE_AMETHYST_BUD)||level.getBlockState(target).is(Blocks.AMETHYST_CLUSTER),"Native budding-amethyst random tick grows a bud");
        return Map.of("budding",source.toShortString(),"grownBud",target.toShortString(),"ticks",ticks,"buddingBlocksInSection",budding,"amethystInSection",amethyst,"calciteInSection",calcite,"basaltInSection",basalt);
    }
}
