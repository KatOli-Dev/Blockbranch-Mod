package blockbranch.client.cubic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
/** Headless-renderer smoke mode enabled only by an explicit JVM property. */
public final class ClientColumnSmoke {
    private static int ticks;
    private static boolean finished;
    private static int phase;
    private static int settle;
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client-> {
            if(finished || client.level==null || client.player==null) return;
            if(++ticks<100) return;
            try {
                var level=client.level;
                boolean dimensions=Boolean.getBoolean("blockbranch.columnDimensionSmoke");
                if(dimensions && phase>0) {
                    if(ticks>6000) throw new AssertionError("Dimension/unload smoke timed out at phase "+phase);
                    if(phase==1) {
                        if(level.dimension().equals(net.minecraft.world.level.Level.NETHER)) {
                            if(level.getMinY()!=0 || level.getHeight()!=256) throw new AssertionError("Nether client height changed");
                            phase=2; Blockbranch.LOGGER.info("COLUMN_DIMENSION_PASS Nether");
                        }
                        return;
                    }
                    if(phase==2) {
                        if(level.dimension().equals(net.minecraft.world.level.Level.END)) {
                            if(level.getMinY()!=0 || level.getHeight()!=256) throw new AssertionError("End client height changed");
                            phase=3; Blockbranch.LOGGER.info("COLUMN_DIMENSION_PASS End");
                        }
                        return;
                    }
                    if(phase==3) {
                        if(level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD) && client.player.getY()>200000) {
                            phase=4; Blockbranch.LOGGER.info("COLUMN_DIMENSION_PASS Overworld return");
                        }
                        return;
                    }
                    if(phase==4) {
                        if(client.player.getX()>1000 && !level.getChunkSource().hasChunk(0,0)) {
                            phase=5; Blockbranch.LOGGER.info("COLUMN_UNLOAD_PASS");
                        }
                        return;
                    }
                    if(client.player.getX()>1000) return;
                    if(++settle<100) return;
                }
                var chunk=level.getChunkSource().getChunk(0,0,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
                if(chunk==null) { if(ticks>(dimensions ? 6000 : 1200)) throw new AssertionError("Chunk never loaded"); return; }
                if(!chunk.getBlockState(new BlockPos(1,ColumnLimits.MAX_Y-1,1)).is(Blocks.DIAMOND_BLOCK)) { if(ticks>(dimensions ? 6000 : 1200)) throw new AssertionError("High section not synchronized"); return; }
                if(!chunk.getBlockState(new BlockPos(1,ColumnLimits.MIN_Y,1)).is(Blocks.GOLD_BLOCK)) throw new AssertionError("Low section not synchronized");
                boolean low=Integer.getInteger("blockbranch.columnSmokeY",262140)<0;
                if(low ? client.player.getY()>-200000 : client.player.getY()<200000) throw new AssertionError("Extreme-height teleport not applied");
                if(Boolean.getBoolean("blockbranch.columnClientFixtures")) {
                    if(!level.getBlockState(new BlockPos(10,100000,10)).isAir()) { if(ticks>(dimensions ? 6000 : 1200)) throw new AssertionError("Replacement snapshot retained a deleted section"); return; }
                    if(level.getBlockEntity(new BlockPos(2,ColumnLimits.MAX_Y-4,2))==null || level.getBlockEntity(new BlockPos(2,ColumnLimits.MIN_Y+4,2))==null) throw new AssertionError("Extreme-height block entities missing");
                    if(!level.getBlockState(new BlockPos(12,(low ? -262140 : 262140)+1,12)).is(Blocks.LAPIS_BLOCK)) { if(ticks>(dimensions ? 6000 : 1200)) throw new AssertionError("Incremental section update missing"); return; }
                }
                int radius=client.options.getEffectiveRenderDistance();
                var area=client.levelRenderer.viewArea();
                if(area==null) return;
                if(area.sectionCount()>2*(radius+1)+1) throw new AssertionError("Full-height rendering allocation");
                if(area.minSectionY()>client.player.blockPosition().getY()>>4 || area.maxSectionY()<client.player.blockPosition().getY()>>4) throw new AssertionError("Render window does not follow player");
                var rendered=area.getRenderSectionAt(new BlockPos(1,low ? ColumnLimits.MIN_Y : ColumnLimits.MAX_Y-1,1));
                if(rendered==null || !rendered.getSectionMesh().hasRenderableLayers()) { if(ticks>(dimensions ? 6000 : 1200)) throw new AssertionError("Extreme-height section was not meshed"); return; }
                if(dimensions && phase==0) { phase=1; Blockbranch.LOGGER.info("COLUMN_DIMENSION_SMOKE initial scene passed"); return; }
                finished=true;
                Blockbranch.LOGGER.info("COLUMN_CLIENT_SMOKE_PASS y={} sections={} height={}",client.player.getY(),area.size(),area.sectionCount());
                java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("blockbranch.clientSmokeResult","/tmp/blockbranch-columns/client-pass")),"PASS\n");
                client.stop();
            } catch(Throwable failure) { finished=true; Blockbranch.LOGGER.error("COLUMN_CLIENT_SMOKE_FAIL",failure); client.stop(); }
        });
    }
    private ClientColumnSmoke() {}
}
