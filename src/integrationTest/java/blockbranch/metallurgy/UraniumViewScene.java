package blockbranch.metallurgy;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
/** Real native ticks prove world/carried exposure and shielding before unedited connected captures. */
public final class UraniumViewScene {
 static void update(LivingEntity entity){try{RadiationSmoke.update(entity);}catch(Exception e){throw new RuntimeException(e);}}
 public static void register(){
  java.util.UUID[] id={null};int[] ticks={0},stage={0},started={0};float[] before={0};var source=new BlockPos(-3,65,-10);
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{id[0]=handler.player.getUUID();ticks[0]=0;stage[0]=0;});
  ServerTickEvents.END_SERVER_TICK.register(server->{if(id[0]==null||++ticks[0]<20)return;var p=server.getPlayerList().getPlayer(id[0]);if(p==null)return;var l=server.overworld();
   if(stage[0]==4){p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.syncVelocity=true;if(p.position().distanceToSqr(0,67.25,-10.5)>.001)server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" 0.0 67.25 -10.5 0 12");return;}
   if(stage[0]==0){
    l.getGameRules().set(GameRules.ADVANCE_TIME,false,server);l.getGameRules().set(GameRules.SPAWN_MOBS,false,server);l.getGameRules().set(GameRules.NATURAL_HEALTH_REGENERATION,false,server);server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"weather clear");
    for(int z=-11;z<=6;z++)for(int x=-8;x<=8;x++)l.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
    var columns=java.util.List.of(Blocks.STONE,UraniumContent.ORE,UraniumContent.DEEPSLATE_ORE,Blocks.IRON_ORE,Blocks.DIAMOND_ORE);for(int group=0;group<5;group++)for(int x=0;x<2;x++)for(int y=65;y<=67;y++)l.setBlock(new BlockPos(-5+group*2+x,y,1),columns.get(group).defaultBlockState(),2);
    p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.removeAllEffects();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.setHealth(20);server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" 0.0 67.25 -10.5 0 12");l.setBlock(source,UraniumContent.ORE.defaultBlockState(),2);stage[0]=1;return;
   }
   if(stage[0]==1&&ticks[0]>=100){
    if(!p.hasEffect(UraniumContent.RADIATION)||p.getHealth()>=20)throw new AssertionError("real ticking world ore must irradiate and hurt partial armor player");l.setBlock(source,Blocks.AIR.defaultBlockState(),2);p.removeEffect(UraniumContent.RADIATION);p.getInventory().setItem(1,RadiationSmoke.shulker(new ItemStack(UraniumContent.RAW)));before[0]=p.getHealth();started[0]=ticks[0];stage[0]=2;return;
   }
   if(stage[0]==2&&ticks[0]-started[0]>=40){
    if(p.hasEffect(UraniumContent.RADIATION)||p.getHealth()!=before[0])throw new AssertionError("native carried shulker must shield after world source removal");p.getInventory().setItem(1,new ItemStack(UraniumContent.RAW));started[0]=ticks[0];before[0]=p.getHealth();stage[0]=3;return;
   }
   if(stage[0]!=3||ticks[0]-started[0]<40)return;
   if(!p.hasEffect(UraniumContent.RADIATION)||p.getHealth()>=before[0])throw new AssertionError("native unshielded inventory must irradiate/hurt");p.setHealth(12);p.heal(4);if(Math.abs(p.getHealth()-14)>1e-5)throw new AssertionError("native connected half healing");
   var victim=net.minecraft.world.entity.EntityTypes.HUSK.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);victim.setNoAi(true);victim.setPos(1.5,65,-9);var gear=new ItemStack(Items.IRON_SWORD);gear.enchant(l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),2);victim.setItemSlot(EquipmentSlot.MAINHAND,gear);l.addFreshEntity(victim);p.getInventory().setSelectedSlot(0);p.getInventory().setItem(0,new ItemStack(UraniumContent.SWORD));update(p);for(int i=0;i<100000;i++){p.getRandom().setSeed(i);if(p.getRandom().nextFloat()<UraniumCombat.STRIP_CHANCE){p.getRandom().setSeed(i);break;}}p.attack(victim);if(victim.getHealth()>=20||!gear.getEnchantments().isEmpty())throw new AssertionError("actual connected Player.attack must remove enchantment");victim.discard();
   var attacker=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);attacker.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(UraniumContent.AXE));var resistance=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);double previousResistance=resistance.getBaseValue();resistance.setBaseValue(1);try{p.hurtServer(l,l.damageSources().mobAttack(attacker),2);}finally{resistance.setBaseValue(previousResistance);}for(var slot:Radiation.ARMOR)if(p.getItemBySlot(slot).getDamageValue()!=3)throw new AssertionError("actual connected native armor wear must triple");
   var mine=new BlockPos(3,65,-9);l.setBlock(mine,UraniumContent.ORE.defaultBlockState(),2);var pick=new ItemStack(Items.DIAMOND_PICKAXE);p.getInventory().setItem(0,pick);update(p);if(!p.gameMode.destroyBlock(mine)||pick.getDamageValue()!=1)throw new AssertionError("native uranium harvest/wear");
   net.minecraft.world.level.ItemLike[] items={Items.DIAMOND_PICKAXE,UraniumContent.ORE,UraniumContent.DEEPSLATE_ORE,UraniumContent.RAW,UraniumContent.INGOT,UraniumContent.TIP,UraniumContent.SWORD,UraniumContent.AXE,UraniumContent.SPEAR};for(int i=0;i<items.length;i++)p.getInventory().setItem(i,new ItemStack(items[i]));p.getInventory().setSelectedSlot(0);p.setHealth(20);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.syncVelocity=true;server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" 0.0 67.25 -10.5 0 12");p.containerMenu.broadcastChanges();blockbranch.Blockbranch.LOGGER.info("URANIUM live native world/inventory DOT, shulker shielding, half healing, Player.attack strip, triple wear and mining passed; armor={}",p.getArmorValue());stage[0]=4;
  });
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.execute(()->server.halt(false)));
 }
 private UraniumViewScene(){}
}
