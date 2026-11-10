package blockbranch.metallurgy;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/** Disposable native visual witness; production code never creates this scene. */
public final class StainlessViewScene {
    public static void register(){
        java.util.UUID[] pending={null};int[] ticks={0};
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{pending[0]=handler.player.getUUID();ticks[0]=0;});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            if(pending[0]==null||++ticks[0]<20)return;
            var player=server.getPlayerList().getPlayer(pending[0]);if(player==null)return;pending[0]=null;
            var level=server.overworld();
            level.getGameRules().set(GameRules.ADVANCE_TIME,false,server);level.getGameRules().set(GameRules.SPAWN_MOBS,false,server);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
            for(int z=-11;z<=5;z++)for(int x=-8;x<=7;x++)level.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
            var columns=java.util.List.of(Blocks.STONE,StainlessContent.NICKEL_ORE,StainlessContent.DEEPSLATE_NICKEL_ORE,StainlessContent.CHROMIUM_ORE,StainlessContent.DEEPSLATE_CHROMIUM_ORE,StainlessContent.STAINLESS_STEEL_BLOCK);
            for(int group=0;group<6;group++)for(int x=0;x<2;x++)for(int y=65;y<=67;y++)level.setBlock(new BlockPos(-6+group*2+x,y,1),columns.get(group).defaultBlockState(),2);
            level.setBlock(new BlockPos(-3,65,-2),StainlessContent.STAINLESS_STEEL_SLAB.defaultBlockState(),2);
            level.setBlock(new BlockPos(0,65,-2),StainlessContent.STAINLESS_STEEL_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.SOUTH),2);
            level.setBlock(new BlockPos(3,65,-2),StainlessContent.STAINLESS_STEEL_BLOCK.defaultBlockState(),2);
            var miningPos=new BlockPos(7,65,3);level.setBlock(miningPos,StainlessContent.NICKEL_ORE.defaultBlockState(),2);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var miningTool=new ItemStack(StainlessContent.STAINLESS_STEEL_PICKAXE);player.getInventory().setItem(0,miningTool);player.getInventory().setSelectedSlot(0);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" 7.5 65 5.5");
            if(!player.gameMode.destroyBlock(miningPos)||!level.getBlockState(miningPos).isAir()||miningTool.getDamageValue()!=1)throw new AssertionError("Actual native stainless steel mining/durability failed");
            blockbranch.Blockbranch.LOGGER.info("METALLURGY actual player mining passed; stainless steel damage={}",miningTool.getDamageValue());
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" 0.0 67.25 -8.5 0 12");
            ItemLike[] items={StainlessContent.STAINLESS_STEEL_PICKAXE,StainlessContent.RAW_NICKEL,StainlessContent.NICKEL_INGOT,StainlessContent.RAW_CHROMIUM,StainlessContent.CHROMIUM_INGOT,StainlessContent.STAINLESS_STEEL_INGOT,StainlessContent.NICKEL_ORE,StainlessContent.DEEPSLATE_NICKEL_ORE,StainlessContent.CHROMIUM_ORE,StainlessContent.DEEPSLATE_CHROMIUM_ORE,StainlessContent.STAINLESS_STEEL_BLOCK,StainlessContent.STAINLESS_STEEL_SLAB,StainlessContent.STAINLESS_STEEL_STAIRS};
            for(int i=0;i<items.length;i++)player.getInventory().setItem(i,new ItemStack(items[i]));
            player.getInventory().setSelectedSlot(8);player.containerMenu.broadcastChanges();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
    }
    private StainlessViewScene(){}
}
