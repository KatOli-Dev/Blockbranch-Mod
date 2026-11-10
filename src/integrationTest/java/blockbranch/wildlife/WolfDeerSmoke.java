package blockbranch.wildlife;

import blockbranch.Blockbranch;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/** Actual native wolf hunting; frozen receiver isolates attack policy from the deer's faster escape. */
final class WolfDeerSmoke {
    private static ServerLevel level;
    private static CountingWolf wolf;
    private static RedDeer deer;
    private static Player owner;
    private static int phase=-1,ticks,attackBaseline;
    private static boolean active,selected;
    private static double fleeStart;
    private static final List<String> results=new ArrayList<>();
    private static final class CountingWolf extends Wolf {
        int attacks;
        CountingWolf(){super(EntityTypes.WOLF,level);}
        @Override public boolean doHurtTarget(ServerLevel world,Entity target){attacks++;return super.doHurtTarget(world,target);}
    }
    private static final class Owner extends Player {
        Owner(){super(level,new GameProfile(UUID.randomUUID(),"WolfOwner"));}
        @Override public GameType gameMode(){return GameType.SURVIVAL;}
    }
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            level=server.overworld();for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
            for(var e:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(e instanceof Mob||e instanceof Player)e.discard();
            for(var type:List.of(EntityTypes.SHEEP,EntityTypes.RABBIT,EntityTypes.FOX,WildlifeRuntime.RED_DEER)){
                var prey=(LivingEntity)type.create(level,EntitySpawnReason.COMMAND);require(Wolf.PREY_SELECTOR.test(prey,level),"native prey preserved or approved deer "+type);prey.discard();
            }
            for(var type:List.of(EntityTypes.COW,WildlifeRuntime.WILD_BOAR)){
                var other=(LivingEntity)type.create(level,EntitySpawnReason.COMMAND);require(!Wolf.PREY_SELECTOR.test(other,level),"no unrelated prey expansion "+type);other.discard();
            }
            active=true;next();
        }catch(Throwable error){fail(error);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;try{
            ticks++;selected|=wolf.getTarget()==deer;
            if(phase<8){
                if(ticks<120)return;
                var mode=Difficulty.values()[phase%4];boolean shouldHunt=phase<4&&mode!=Difficulty.PEACEFUL;
                require(shouldHunt?selected&&wolf.attacks>0&&deer.getHealth()<deer.getMaxHealth():!selected&&wolf.attacks==0&&deer.getHealth()==deer.getMaxHealth(),"hunt/tame policy phase="+phase+" selected="+selected+" attacks="+wolf.attacks+" health="+deer.getHealth());
                results.add((phase<4?"untamed ":"tamed ")+mode+" selected="+selected+" attacks="+wolf.attacks+" deerHealth="+deer.getHealth());next();return;
            }
            if(phase==8){
                if(wolf.attacks==0){require(ticks<160,"Normal hunting positive before hot switch");return;}
                deer.setHealth(deer.getMaxHealth());attackBaseline=wolf.attacks;level.getServer().setDifficulty(Difficulty.PEACEFUL,true);phase=9;ticks=0;return;
            }
            if(phase==9&&ticks>=60){
                require(wolf.attacks==attackBaseline&&deer.getHealth()==deer.getMaxHealth()&&wolf.getTarget()!=deer,"hot Peaceful clears active hunt without attempts/damage");
                results.add("hot Peaceful 60 ticks: no attack attempts/damage, target cleared");level.getServer().setDifficulty(Difficulty.NORMAL,true);phase=10;ticks=0;return;
            }
            if(phase==10){
                if(wolf.attacks==attackBaseline){require(ticks<160,"native hunting resumes on Normal");return;}
                results.add("return Normal resumes hunting ticks="+ticks);next();return;
            }
            if(phase==11&&ticks>=160){
                require(deer.distanceTo(wolf)>fleeStart+2&&wolf.attacks==0,"deer still escapes a harmless Peaceful wolf");results.add("Peaceful deer escape "+fleeStart+" -> "+deer.distanceTo(wolf));
                Files.writeString(result(),"PASS\n"+String.join("\n",results)+"\n");active=false;Blockbranch.LOGGER.info("WOLF_DEER_SMOKE PASS {}",results);level.getServer().halt(false);
            }
        }catch(Throwable error){fail(error);}});
    }
    private static void next(){
        if(wolf!=null)wolf.discard();if(deer!=null)deer.discard();if(owner!=null){owner.discard();owner=null;}
        phase++;ticks=0;selected=false;
        var mode=phase<8?Difficulty.values()[phase%4]:phase==11?Difficulty.PEACEFUL:Difficulty.NORMAL;level.getServer().setDifficulty(mode,true);
        wolf=new CountingWolf();wolf.snapTo(.5,-60,.5,0,0);wolf.setOnGround(true);wolf.getRandom().setSeed(612);level.addFreshEntity(wolf);
        deer=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);deer.snapTo(phase==11?4.5:2,-60,.5,0,0);deer.setOnGround(true);deer.setNoAi(phase!=11);deer.getRandom().setSeed(923);level.addFreshEntity(deer);
        if(phase>=4&&phase<8){owner=new Owner();owner.snapTo(-12,-60,0,0,0);level.addFreshEntity(owner);wolf.tame(owner);}
        if(phase==11){wolf.setNoAi(true);fleeStart=deer.distanceTo(wolf);}
        Blockbranch.LOGGER.info("WOLF_DEER_PHASE {} difficulty={} tamed={}",phase,mode,wolf.isTame());
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.wolfDeerResult"));}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void fail(Throwable error){active=false;Blockbranch.LOGGER.error("WOLF_DEER_SMOKE FAIL",error);try{Files.writeString(result(),"FAIL "+error+"\n");}catch(Exception ignored){}if(level!=null)level.getServer().halt(false);}
}
