package blockbranch.wildlife;

import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Opt-in connected-player defense and hot-difficulty check on the staged review floor. */
public final class SealPlayerCheck {
    private static ServerPlayer player;
    private static HarborSeal adult,cub;
    private static Vec3 camera;
    private static float yaw,pitch;
    private static Difficulty original;
    private static int phase,ticks;
    private static boolean switched;
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(server->{if(phase==0)return;try{
            ticks++;if(ticks>200)throw new AssertionError("Connected seal defense timeout phase="+phase);
            if(ticks==10)adult.hurtServer(player.level(),adult.damageSources().playerAttack(player),1);
            boolean hit=player.getHealth()<20;
            if(phase<=2){
                if(hit||adult.getTarget()==player)throw new AssertionError("Forbidden player aggression phase="+phase);
                if(ticks<100)return;
            }else if(phase<=4){
                if(!hit)return;
                if(player.getLastHurtByMob()!=adult)throw new AssertionError("Expected actual seal melee source");
            }else{
                if(hit)throw new AssertionError("Hot Peaceful switch must precede melee contact");
                if(!switched){
                    if(adult.getTarget()!=player)return;
                    server.setDifficulty(Difficulty.PEACEFUL,true);switched=true;ticks=0;return;
                }
                if(ticks<50)return;
                if(adult.getTarget()!=null)throw new AssertionError("Target survived Peaceful transition");
            }
            Blockbranch.LOGGER.info("SEAL_CONNECTED_PLAYER_PASS phase={} difficulty={} ticks={} health={} actualMelee={} hotTransition={}",phase,player.level().getDifficulty(),ticks,player.getHealth(),hit,switched);
            player.setGameMode(GameType.SPECTATOR);discard();
            if(++phase<=5){start();return;}
            finish();ServerPlayNetworking.send(player,new WildlifeProbe(10,"Connected-player defense and Peaceful transition PASS"));phase=0;
        }catch(Throwable error){fail(error);}});
    }
    public static void begin(ServerPlayer connected){
        if(phase!=0)return;
        player=connected;camera=player.position();yaw=player.getYRot();pitch=player.getXRot();original=player.level().getDifficulty();phase=1;start();
    }
    private static void start(){
        var level=player.level();level.getServer().setDifficulty(switch(phase){case 1->Difficulty.PEACEFUL;case 2->Difficulty.EASY;case 4->Difficulty.HARD;default->Difficulty.NORMAL;},true);
        player.setGameMode(GameType.SPECTATOR);player.setHealth(20);player.getFoodData().setFoodLevel(20);
        command("tp "+player.getName().getString()+" "+camera.x+" "+(camera.y-1.5)+" "+(camera.z-3)+" 180 0");
        adult=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);
        adult.snapTo(camera.x,camera.y-1.5,camera.z-9,0,0);adult.setHuntCooldown(12000);adult.setPersistenceRequired();level.addFreshEntity(adult);
        ticks=0;switched=false;player.setGameMode(GameType.SURVIVAL);
    }
    private static void discard(){if(adult!=null){adult.discard();adult=null;}if(cub!=null){cub.discard();cub=null;}}
    private static void finish(){
        discard();player.setGameMode(GameType.SPECTATOR);player.setHealth(20);
        player.level().getServer().setDifficulty(original,true);
        command("tp "+player.getName().getString()+" "+camera.x+" "+camera.y+" "+camera.z+" "+yaw+" "+pitch);
    }
    private static void fail(Throwable error){
        Blockbranch.LOGGER.error("SEAL_CONNECTED_PLAYER FAIL",error);if(player!=null){finish();ServerPlayNetworking.send(player,new WildlifeProbe(-1,error.toString()));}phase=0;
    }
    private static void command(String command){var server=player.level().getServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);}
    private SealPlayerCheck(){}
}
