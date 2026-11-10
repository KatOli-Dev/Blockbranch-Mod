package blockbranch.metallurgy;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
/** Native silver hand-tool actions, combat strength and saved stack qualification. */
public final class SilverGearSmoke {
 public static void verify(ServerLevel level,Path directory)throws Exception{tools(level,directory);try(var loader=MetallurgySmoke.historicalReader(Path.of(System.getProperty("blockbranch.oldPreciousCatalogClasses")).toUri().toURL())){var guard=loader.loadClass("blockbranch.cubic.ColumnWorldFormat").getMethod("check",Path.class,boolean.class,boolean.class,boolean.class);for(boolean decoration:new boolean[]{false,true}){var root=Files.createTempDirectory(directory,"old-silver-guard-");ColumnWorldFormat.check(root,false,decoration,true);var marker=root.resolve("blockbranch-columns.txt");String before=Files.readString(marker);try{guard.invoke(null,root,true,decoration,true);throw new AssertionError("prior catalog11 reader accepted12");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"prior foundation reader rejects equipment12");}require(Files.readString(marker).equals(before),"old reader rejection preserves marker");}}}
    private static void tools(ServerLevel level,Path directory)throws Exception{
        var tools=List.of(SilverEquipment.SILVER_SWORD,SilverEquipment.SILVER_AXE,SilverEquipment.SILVER_SHOVEL,SilverEquipment.SILVER_HOE);
        var tags=List.of(net.minecraft.tags.ItemTags.SWORDS,net.minecraft.tags.ItemTags.AXES,net.minecraft.tags.ItemTags.SHOVELS,net.minecraft.tags.ItemTags.HOES);
        String[] names={"sword","axe","shovel","hoe"};String[][] patterns={{"B","B","S"},{"BB","BS"," S"},{"B","S","S"},{"BB"," S"," S"}};
        var ops=RegistryOps.create(NbtOps.INSTANCE,level.registryAccess());var saved=directory.resolve("silver-tools.dat");
        var previous=Files.exists(saved)?net.minecraft.nbt.NbtIo.readCompressed(saved,net.minecraft.nbt.NbtAccounter.unlimitedHeap()):null;
        var next=new net.minecraft.nbt.CompoundTag();
        for(int i=0;i<tools.size();i++){
            var cells=new ArrayList<ItemStack>();for(String row:patterns[i])for(char ch:row.toCharArray())cells.add(ch=='B'?new ItemStack(PreciousContent.SILVER_INGOT):ch=='S'?new ItemStack(Items.STICK):ItemStack.EMPTY);
            var input=CraftingInput.of(patterns[i][0].length(),patterns[i].length,cells);
            var output=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input);
            require(output.is(tools.get(i))&&output.is(tags.get(i)),"native silver "+names[i]+" recipe and tool/enchantment tag");
            require(output.getMaxDamage()==224&&output.isValidRepairItem(new ItemStack(PreciousContent.SILVER_INGOT))&&!output.isValidRepairItem(new ItemStack(SteelContent.STEEL_INGOT)),"shared silver durability/repair");
            if(previous!=null){var restored=ItemStack.CODEC.parse(ops,previous.getCompoundOrEmpty(names[i])).getOrThrow();require(restored.is(tools.get(i))&&restored.getDamageValue()==37,"saved tool identity and damage survive relaunch");}
            output.setDamageValue(37);next.put(names[i],ItemStack.CODEC.encodeStart(ops,output).getOrThrow());
        }
        net.minecraft.nbt.NbtIo.writeCompressed(next,saved);
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SilverActions");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,48,0)+4;
        Block[] sources={Blocks.OAK_LOG,Blocks.GRASS_BLOCK,Blocks.GRASS_BLOCK};Block[] results={Blocks.STRIPPED_OAK_LOG,Blocks.DIRT_PATH,Blocks.FARMLAND};
        for(int i=0;i<3;i++){
            var pos=new BlockPos(48+i,y,0);level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,sources[i].defaultBlockState(),2);
            var tool=new ItemStack(tools.get(i+1));player.getInventory().setItem(0,tool);player.getInventory().setSelectedSlot(0);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);
            tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            require(level.getBlockState(pos).is(results[i])&&tool.getDamageValue()==1,"native "+names[i+1]+" action and durability");
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);tool.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            require(level.getBlockState(pos).is(Blocks.STONE)&&tool.getDamageValue()==1,"unsupported action leaves blocks and durability unchanged");
        }
        var sword=new ItemStack(SilverEquipment.SILVER_SWORD);player.getInventory().setItem(0,sword);
        var target=net.minecraft.world.entity.EntityTypes.PIG.create(level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        require(target!=null&&sword.hurtEnemy(target,player),"native sword weapon component");sword.postHurtEnemy(target,player);require(sword.getDamageValue()==1,"native sword hit durability");
        var attribute=net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;var slot=net.minecraft.world.entity.EquipmentSlot.MAINHAND;
        double silver=sword.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double iron=new ItemStack(Items.IRON_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        double diamond=new ItemStack(Items.DIAMOND_SWORD).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS).compute(attribute,1,slot);
        require(silver==iron&&silver<diamond,"silver retains iron attack strength and diamond advantage");
        Blockbranch.LOGGER.info("SILVER_TOOLS native recipes/actions/negative controls/sword durability and saved-stack reload={}",previous!=null);
    }
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 private SilverGearSmoke(){}
}
