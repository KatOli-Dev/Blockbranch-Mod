package blockbranch.metallurgy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Disposable zinc/brass scene; no production placement or world mutation hooks. */
public final class ZincBrassViewScene {
    public static void register(){
        java.util.UUID[] pending={null};int[] ticks={0};
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{pending[0]=handler.player.getUUID();ticks[0]=0;});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(pending[0]==null||++ticks[0]<20)return;var player=server.getPlayerList().getPlayer(pending[0]);if(player==null)return;pending[0]=null;var level=server.overworld();
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
            for(int z=-11;z<=5;z++)for(int x=-8;x<=7;x++)level.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
            var columns=java.util.List.of(Blocks.STONE,ZincBrassContent.ZINC_ORE,Blocks.IRON_ORE,ZincBrassContent.DEEPSLATE_ZINC_ORE,ZincBrassContent.BRASS_BLOCK);
            for(int group=0;group<5;group++)for(int x=0;x<2;x++)for(int y=65;y<=67;y++)level.setBlock(new BlockPos(-5+group*2+x,y,1),columns.get(group).defaultBlockState(),2);
            level.setBlock(new BlockPos(3,65,-2),ZincBrassContent.BRASS_SLAB.defaultBlockState(),2);
            level.setBlock(new BlockPos(4,65,-2),ZincBrassContent.BRASS_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH),2);
            for(int x=-3;x<=0;x++)for(int y=65;y<=66;y++)level.setBlock(new BlockPos(x,y,-2),ZincBrassContent.BRASS_BARS.defaultBlockState().setValue(IronBarsBlock.WEST,x>-3).setValue(IronBarsBlock.EAST,x<0),2);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" 0.0 67.25 -8.5 0 12");
            ItemLike[] items={MetallurgyContent.BRONZE_PICKAXE,ZincBrassContent.ZINC_ORE,ZincBrassContent.DEEPSLATE_ZINC_ORE,ZincBrassContent.RAW_ZINC,ZincBrassContent.ZINC_INGOT,ZincBrassContent.BRASS_INGOT,ZincBrassContent.BRASS_BLOCK,ZincBrassContent.BRASS_SLAB,ZincBrassContent.BRASS_STAIRS,ZincBrassContent.BRASS_BARS};
            for(int i=0;i<items.length;i++)player.getInventory().setItem(i,new ItemStack(items[i]));player.getInventory().setSelectedSlot(0);player.containerMenu.broadcastChanges();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
    }
    private ZincBrassViewScene(){}
}
