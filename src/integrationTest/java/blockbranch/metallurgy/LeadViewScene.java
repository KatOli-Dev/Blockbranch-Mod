package blockbranch.metallurgy;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/** Disposable native visual witness; production code never creates this scene. */
public final class LeadViewScene {
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
            var columns=java.util.List.of(Blocks.STONE,LeadContent.LEAD_ORE,LeadContent.DEEPSLATE_LEAD_ORE,LeadContent.LEAD_BLOCK,LeadContent.LEAD_ALLOY_BLOCK);
            for(int group=0;group<5;group++)for(int x=0;x<2;x++)for(int y=65;y<=67;y++)level.setBlock(new BlockPos(-5+group*2+x,y,1),columns.get(group).defaultBlockState(),2);
            var sets=java.util.List.of(LeadContent.LEAD_SLAB,LeadContent.LEAD_STAIRS,LeadContent.LEAD_BLOCK,LeadContent.LEAD_ALLOY_SLAB,LeadContent.LEAD_ALLOY_STAIRS,LeadContent.LEAD_ALLOY_BLOCK);
            for(int k=0;k<sets.size();k++){var state=sets.get(k).defaultBlockState();if(sets.get(k) instanceof net.minecraft.world.level.block.StairBlock)state=state.setValue(net.minecraft.world.level.block.StairBlock.FACING,net.minecraft.core.Direction.SOUTH);level.setBlock(new BlockPos(-5+k*2,65,-2),state,2);}
            var miningPos=new BlockPos(7,65,3);level.setBlock(miningPos,LeadContent.LEAD_ORE.defaultBlockState(),2);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var miningTool=new ItemStack(MetallurgyContent.BRONZE_PICKAXE);player.getInventory().setItem(0,miningTool);player.getInventory().setSelectedSlot(0);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" 7.5 65 5.5");
            if(!player.gameMode.destroyBlock(miningPos)||!level.getBlockState(miningPos).isAir()||miningTool.getDamageValue()!=1)throw new AssertionError("Actual native lead/bronze mining/durability failed");
            blockbranch.Blockbranch.LOGGER.info("METALLURGY actual player mining passed; lead/bronze damage={}",miningTool.getDamageValue());
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" 0.0 67.25 -10.5 0 12");
            ItemLike[] items={MetallurgyContent.BRONZE_PICKAXE,LeadContent.LEAD_ORE,LeadContent.DEEPSLATE_LEAD_ORE,LeadContent.RAW_LEAD,LeadContent.LEAD_INGOT,LeadContent.LEAD_ALLOY_INGOT,LeadContent.LEAD_BLOCK,LeadContent.LEAD_SLAB,LeadContent.LEAD_STAIRS,LeadContent.LEAD_ALLOY_BLOCK,LeadContent.LEAD_ALLOY_SLAB,LeadContent.LEAD_ALLOY_STAIRS};
            for(int i=0;i<items.length;i++)player.getInventory().setItem(i,new ItemStack(items[i]));
            player.getInventory().setSelectedSlot(8);player.containerMenu.broadcastChanges();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
    }
    private LeadViewScene(){}
}
