package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;

/** Opt-in metric review platform, isolated from the natural population survey. */
final class WildlifeScaleScene {
    static void create(MinecraftServer server,ServerLevel level,BlockPos scene,String name) {
        int y=scene.getY(),z=scene.getZ(),x=scene.getX();
        for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof Mob mob&&mob.isNoAi()&&mob.isPersistenceRequired())mob.discard();
        for(int dx=-9;dx<=10;dx++)for(int dz=-3;dz<=3;dz++){
            level.setBlock(new BlockPos(x+dx,y-1,z+dz),((dx+dz)&1)==0?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.POLISHED_ANDESITE.defaultBlockState(),2);
            for(int dy=0;dy<4;dy++)level.setBlock(new BlockPos(x+dx,y+dy,z+dz),Blocks.AIR.defaultBlockState(),2);
        }
        String[] labels={"Adult boar","Piglet","Stag","Hind","Calf"};int[] offsets={-7,-4,-1,2,5};
        for(int i=0;i<5;i++){
            net.minecraft.world.entity.animal.Animal animal=i<2?WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND):WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);
            animal.snapTo(x+offsets[i]+.5,y,z+.5,90,0);animal.setYBodyRot(90);animal.setYHeadRot(90);
            if(i==1||i==4)animal.setAge(-24000);
            if(animal instanceof RedDeer deer)deer.setAntlers(i==2);
            animal.setNoAi(true);animal.setSilent(true);animal.setPersistenceRequired();animal.setCustomName(net.minecraft.network.chat.Component.literal(labels[i]));animal.setCustomNameVisible(true);level.addFreshEntity(animal);
        }
        var dummy=new ServerPlayer(server,level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ScaleReference"),ClientInformation.createDefault());
        if(Math.abs(dummy.getBbHeight()-1.8F)>.00001||Math.abs(dummy.getBbWidth()-.6F)>.00001)throw new AssertionError("Native player reference changed");
        command(server,"kill @e[type=minecraft:block_display]");command(server,"kill @e[type=minecraft:text_display]");
        command(server,"summon minecraft:block_display "+(x+7.2)+" "+y+" "+(z+.2)+" {block_state:\"minecraft:lime_concrete\",transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],scale:[0.6f,1.8f,0.6f],right_rotation:[0f,0f,0f,1f]}}");
        command(server,"data get entity @e[type=minecraft:block_display,limit=1] transformation");
        command(server,"data get entity @e[type=minecraft:block_display,limit=1] block_state");
        for(int dy=0;dy<3;dy++)level.setBlock(new BlockPos(x+9,y+dy,z),dy%2==0?Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState():Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState(),2);
        command(server,"summon minecraft:text_display "+(x+7.5)+" "+(y+2.5)+" "+(z+.5)+" {text:\"Player: 1.8 blocks\",billboard:\"center\",background:1073741824}");
        command(server,"summon minecraft:text_display "+(x+9.5)+" "+(y+3.5)+" "+(z+.5)+" {text:\"1 block per band\",billboard:\"center\",background:1073741824}");
        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
        command(server,"gamemode spectator "+name);command(server,"time set 6000");command(server,"weather clear");
        command(server,"tp "+name+" "+(x+1.5)+" "+(y+2.2)+" "+(z+11.5)+" 180 6");
        Blockbranch.LOGGER.info("WILDLIFE_SCALE_SCENE feetY={} rowZ={} playerMarker=.6x1.8x.6 nativePlayer={}x{} floor=oneBlockGrid allSubjectsSameGround=true",y,z+.5,dummy.getBbWidth(),dummy.getBbHeight());
    }
    private static void command(MinecraftServer server,String command){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);}
    private WildlifeScaleScene(){}
}
