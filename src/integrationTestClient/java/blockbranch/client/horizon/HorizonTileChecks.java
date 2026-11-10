package blockbranch.client.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
/** Stateless checks on a target's level-0 tile, shared by the horizon lifecycle and reconnect reviews. */
final class HorizonTileChecks {
    /** Cached, meshed from its current data and drawn. */
    static boolean settled(HorizonClient.TileState t){return t!=null&&t.meshCurrent()&&t.drawn();}
    /** Interior samples (the target chunk's own columns) whose top state is {@code state}. */
    static int count(int[] d,BlockState state) {
        int n=0,id=Block.getId(state);
        for(int j=1;j<=CELLS;j++)for(int i=1;i<=CELLS;i++)if(d[(j*SAMPLES+i)*FIELDS+TOP_STATE]==id)n++;
        return n;
    }
    static BlockState layer(String name){return (switch(name){case "gold"->Blocks.GOLD_BLOCK;case "emerald"->Blocks.EMERALD_BLOCK;case "diamond"->Blocks.DIAMOND_BLOCK;case "lapis"->Blocks.LAPIS_BLOCK;default->throw new AssertionError(name);}).defaultBlockState();}
    /** Every interior sample tops out in {@code state} at the layer {@code y} (as block y or the y above it). */
    static boolean uniform(int[] d,BlockState state,int y) {
        if(count(d,state)!=CELLS*CELLS)return false;
        for(int j=1;j<=CELLS;j++)for(int i=1;i<=CELLS;i++){int top=d[(j*SAMPLES+i)*FIELDS+TOP];if(top!=y&&top!=y+1)return false;}
        return true;
    }
    static String describe(HorizonClient.TileState t) {
        if(t==null)return "absent";
        int o=(9*SAMPLES+9)*FIELDS;var top=Block.stateById(t.data()[o+TOP_STATE]).getBlock();
        return "centre top="+t.data()[o+TOP]+" "+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(top).getPath()
            +" gold="+count(t.data(),layer("gold"))+" emerald="+count(t.data(),layer("emerald"))+" diamond="+count(t.data(),layer("diamond"))
            +" meshCurrent="+t.meshCurrent()+" drawn="+t.drawn();
    }
    static void press(net.minecraft.client.gui.screens.Screen screen,String label) {
        for(var v:screen.children())if(v instanceof Button button&&button.active&&button.getMessage().getString().equals(label)){button.onPress(new KeyEvent(257,0,0));return;}
        throw new AssertionError("Missing UI button "+label);
    }
    private HorizonTileChecks(){}
}
