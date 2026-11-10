package blockbranch.cubic;
import blockbranch.Blockbranch;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
/** Opt-in in-game regression checks; never runs in ordinary play. */
public final class ColumnSmoke {
    private static int ticks;
    private static boolean passed;
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server-> {
            try {
                var level=server.overworld();
                check(level.getMinY()==ColumnLimits.MIN_Y && level.getMaxY()==ColumnLimits.MAX_Y-1,"Overworld bounds");
                check(server.getLevel(Level.NETHER).getMinY()==0 && server.getLevel(Level.NETHER).getHeight()==256,"Nether bounds");
                check(server.getLevel(Level.END).getMinY()==0 && server.getLevel(Level.END).getHeight()==256,"End bounds");
                int[] coords={-2097152,-1,0,2097151}; int[] ys={-262160,-262144,-1,0,262143,262159};
                for(int x:coords) for(int z:coords) for(int y:ys) { var pos=new BlockPos(x,y,z); check(BlockPos.of(pos.asLong()).equals(pos),"BlockPos roundtrip "+pos); }
                var chunk=level.getChunk(0,0); var sparse=(SparseColumn)chunk;
                check(sparse.blockbranch$expanded(),"Expanded column marker");
                check(chunk.getSections().length==24,"Compact native section window");
                ColumnGenerationCheck.check(level);
                int before=sparse.blockbranch$sections().size();
                for(int y=-200000;y<=200000;y+=10000) chunk.getBlockState(new BlockPos(1,y,1));
                check(sparse.blockbranch$sections().size()==before,"Empty reads do not allocate sections");
                var temporary=new BlockPos(5,100000,5);
                level.setBlock(temporary,Blocks.STONE.defaultBlockState(),3);
                check(sparse.blockbranch$sections().containsKey(temporary.getY()>>4),"First write allocates a sparse section");
                level.setBlock(temporary,Blocks.AIR.defaultBlockState(),3);
                check(!sparse.blockbranch$sections().containsKey(temporary.getY()>>4),"Last removal releases a sparse section");
                var top=new BlockPos(1,ColumnLimits.MAX_Y-1,1); var bottom=new BlockPos(1,ColumnLimits.MIN_Y,1);
                boolean reload=level.getBlockState(top).is(Blocks.DIAMOND_BLOCK);
                if(reload) check(level.getBlockState(bottom).is(Blocks.GOLD_BLOCK),"Bottom persisted");
                check(level.setBlock(top,Blocks.DIAMOND_BLOCK.defaultBlockState(),3) || reload,"Top write");
                check(level.setBlock(bottom,Blocks.GOLD_BLOCK.defaultBlockState(),3) || reload,"Bottom write");
                check(level.getBlockState(top).is(Blocks.DIAMOND_BLOCK),"Top read");
                check(level.getBlockState(bottom).is(Blocks.GOLD_BLOCK),"Bottom read");
                check(!level.setBlock(top.above(),Blocks.STONE.defaultBlockState(),3),"Exclusive top boundary");
                check(!level.setBlock(bottom.below(),Blocks.STONE.defaultBlockState(),3),"Bottom boundary");
                check(level.getHeight(Heightmap.Types.WORLD_SURFACE,1,1)==ColumnLimits.MAX_Y,"High heightmap");
                level.setBlock(top,Blocks.AIR.defaultBlockState(),3);
                check(level.getHeight(Heightmap.Types.WORLD_SURFACE,1,1)<1000,"Heightmap skips empty span");
                level.setBlock(top,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
                var serialized=SerializableChunkData.copyOf(level,chunk).write();
                check(serialized.getListOrEmpty("sections").compoundStream().anyMatch(t->t.getIntOr("Y",0)==16383),"Integer section Y saved");
                check(serialized.getListOrEmpty("sections").size()<100,"Sparse serialized sections");
                check(SerializableChunkData.parse(level,level.palettedContainerFactory(),serialized)!=null,"Sparse section parse");
                check(level.getBlockState(new BlockPos(1,-64,1)).is(Blocks.BEDROCK),"Vanilla bedrock elevation");
                level.setChunkForced(0,0,true);
                var chestPos=new BlockPos(2,ColumnLimits.MAX_Y-4,2);
                var barrelPos=new BlockPos(2,ColumnLimits.MIN_Y+4,2);
                boolean fixtures=level.getBlockState(chestPos).is(Blocks.CHEST);
                if(fixtures) {
                    check(((net.minecraft.world.Container)level.getBlockEntity(chestPos)).getItem(0).getCount()==7,"High chest contents persisted");
                    check(((net.minecraft.world.Container)level.getBlockEntity(barrelPos)).getItem(0).getCount()==9,"Low barrel contents persisted");
                    check(level.getBlockTicks().hasScheduledTick(new BlockPos(4,ColumnLimits.MIN_Y+4,4),Blocks.STONE),"Low scheduled block tick persisted");
                } else {
                    level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
                    level.setBlock(barrelPos,Blocks.BARREL.defaultBlockState(),3);
                    ((net.minecraft.world.Container)level.getBlockEntity(chestPos)).setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
                    ((net.minecraft.world.Container)level.getBlockEntity(barrelPos)).setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD,9));
                    level.setBlock(new BlockPos(4,ColumnLimits.MIN_Y+4,4),Blocks.STONE.defaultBlockState(),3);
                    level.scheduleTick(new BlockPos(4,ColumnLimits.MIN_Y+4,4),Blocks.STONE,2000);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"summon armor_stand 4 262136 4 {NoGravity:1b,Tags:[\"column_smoke\"]}");
                }
                level.setBlock(new BlockPos(3,ColumnLimits.MAX_Y-8,3),Blocks.COMPOSTER.defaultBlockState(),3);
                level.setBlock(new BlockPos(3,ColumnLimits.MIN_Y+8,3),Blocks.COMPOSTER.defaultBlockState(),3);
                for(int fixtureY:new int[]{ColumnLimits.MIN_Y+16,ColumnLimits.MAX_Y-16}) {
                    for(int x=9;x<=12;x++) for(int z=9;z<=11;z++) {
                        level.setBlock(new BlockPos(x,fixtureY-1,z),Blocks.STONE.defaultBlockState(),3);
                        if(x==9 || x==12 || z==9 || z==11) level.setBlock(new BlockPos(x,fixtureY,z),Blocks.STONE.defaultBlockState(),3);
                    }
                    level.setBlock(new BlockPos(10,fixtureY,10),Blocks.WATER.defaultBlockState(),3);
                    level.setBlock(new BlockPos(8,fixtureY+3,8),Blocks.AIR.defaultBlockState(),3);
                    level.setBlock(new BlockPos(8,fixtureY+3,8),Blocks.GLOWSTONE.defaultBlockState(),3);
                    level.setBlock(new BlockPos(6,fixtureY+3,6),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
                    level.setBlock(new BlockPos(7,fixtureY+3,6),Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,level.hasNeighborSignal(new BlockPos(7,fixtureY+3,6))),3);
                    check(level.getBlockState(new BlockPos(7,fixtureY+3,6)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT),"Extreme-height redstone power");
                    level.setBlock(new BlockPos(6,fixtureY+3,6),Blocks.AIR.defaultBlockState(),3);
                }
                var broken=serialized.copy(); broken.putInt("blockbranch:column_format",999);
                boolean rejected=false; try { SerializableChunkData.parse(level,level.palettedContainerFactory(),broken); } catch(IllegalArgumentException expected) { rejected=true; }
                check(rejected,"Unsupported format rejected");
                Blockbranch.LOGGER.info("COLUMN_SMOKE_PASS {} sections={} entries={}",reload?"RELOAD":"NEW",sparse.blockbranch$sections().size(),serialized.getListOrEmpty("sections").size());
                passed=true;
            } catch(Throwable failure) { Blockbranch.LOGGER.error("COLUMN_SMOKE_FAIL",failure); server.halt(false); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server-> {
            if(passed && ++ticks==100) {
                try {
                    var level=server.overworld();
                    check(!level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,new net.minecraft.world.phys.AABB(0,ColumnLimits.MAX_Y-16,0,16,ColumnLimits.MAX_Y,16),e->true).isEmpty(),"High entity persisted");
                    check(level.getPoiManager().exists(new BlockPos(3,ColumnLimits.MAX_Y-8,3),type->true),"High POI available");
                    check(level.getPoiManager().exists(new BlockPos(3,ColumnLimits.MIN_Y+8,3),type->true),"Low POI available");
                    var lightFence=level.getChunkSource().getLightEngine().waitForPendingTasks(0,0);
                    server.managedBlock(lightFence::isDone);
                    for(int fixtureY:new int[]{ColumnLimits.MIN_Y+16,ColumnLimits.MAX_Y-16}) {
                        check(!level.getFluidState(new BlockPos(11,fixtureY,10)).isEmpty(),"Extreme-height fluid tick");
                        check(!level.getBlockState(new BlockPos(7,fixtureY+3,6)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT),"Extreme-height scheduled redstone tick");
                        int brightness=level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,new BlockPos(9,fixtureY+3,8));
                        Blockbranch.LOGGER.info("COLUMN_LIGHT y={} brightness={}",fixtureY,brightness);
                        check(brightness>=14,"Extreme-height block light at "+fixtureY);
                    }
                    check(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,new BlockPos(15,ColumnLimits.MAX_Y-8,15))==15,"High skylight");
                    server.saveEverything(false,true,true);
                    String result=System.getProperty("blockbranch.columnSmokeResult");
                    if(result!=null) Files.writeString(Path.of(result),"PASS\n");
                    Blockbranch.LOGGER.info("COLUMN_FIXTURES_PASS");
                } catch(Throwable failure) { Blockbranch.LOGGER.error("COLUMN_SMOKE_FAIL",failure); }
                server.halt(false);
            }
        });
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private ColumnSmoke() {}
}
