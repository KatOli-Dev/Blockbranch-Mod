package blockbranch.metallurgy;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

/** Disposable native visual witness; production code never creates this scene. */
public final class TitaniumViewScene {
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
            var columns=java.util.List.of(Blocks.STONE,TitaniumContent.ORE,TitaniumContent.DEEPSLATE_ORE,TitaniumContent.ALLOY_BLOCK,Blocks.IRON_BLOCK);
            for(int group=0;group<5;group++)for(int x=0;x<2;x++)for(int y=65;y<=67;y++)level.setBlock(new BlockPos(-5+group*2+x,y,1),columns.get(group).defaultBlockState(),2);
            var miningPos=new BlockPos(7,65,3);level.setBlock(miningPos,TitaniumContent.ORE.defaultBlockState(),2);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var miningTool=new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);player.getInventory().setItem(0,miningTool);player.getInventory().setSelectedSlot(0);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" 7.5 65 5.5");
            if(!player.gameMode.destroyBlock(miningPos)||!level.getBlockState(miningPos).isAir()||miningTool.getDamageValue()!=1)throw new AssertionError("Actual native titanium/diamond mining/durability failed");
            blockbranch.Blockbranch.LOGGER.info("METALLURGY actual player mining passed; titanium/diamond damage={}",miningTool.getDamageValue());
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" 0.0 67.25 -10.5 0 12");
            ItemLike[] items={net.minecraft.world.item.Items.DIAMOND_PICKAXE,TitaniumContent.ORE,TitaniumContent.DEEPSLATE_ORE,TitaniumContent.RAW,TitaniumContent.INGOT,TitaniumContent.ALLOY_INGOT,TitaniumContent.ALLOY_BLOCK,TitaniumContent.BOOTS,StainlessContent.NICKEL_INGOT};
            for(int i=0;i<items.length;i++)player.getInventory().setItem(i,new ItemStack(items[i]));
            var boots=new ItemStack(TitaniumContent.BOOTS);boots.set(net.minecraft.core.component.DataComponents.TRIM,new net.minecraft.world.item.equipment.trim.ArmorTrim(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRIM_MATERIAL).getOrThrow(net.minecraft.world.item.equipment.trim.TrimMaterials.QUARTZ),level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRIM_PATTERN).getOrThrow(net.minecraft.world.item.equipment.trim.TrimPatterns.SENTRY)));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,boots);player.getInventory().setItem(7,boots.copy());
            var babyId=java.util.UUID.fromString("8aedfc3b-71c5-4812-a68e-2c2c10e3ca64");var previous=level.getEntity(babyId);if(previous!=null)previous.discard();var baby=net.minecraft.world.entity.EntityTypes.HUSK.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);baby.setUUID(babyId);baby.setBaby(true);baby.setNoAi(true);baby.setNoGravity(true);baby.setPermanentlyInvulnerable(true);baby.setSilent(true);baby.setPos(1.5,67.25,-10.5);baby.setYRot(0);baby.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(TitaniumContent.BOOTS));level.addFreshEntity(baby);
            player.getInventory().setSelectedSlot(8);player.containerMenu.broadcastChanges();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
    }
    private TitaniumViewScene(){}
}
