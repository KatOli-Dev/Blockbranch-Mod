package blockbranch.wildlife;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.behavior.RamTarget;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.goat.GoatAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

/** Native effect-path microfixtures: no substitute hurt implementation, no claim of full AI coverage. */
final class WildlifeCombatPaths {
    static void check(ServerLevel level)throws Exception{
        Difficulty difficulty=level.getDifficulty();
        for(boolean player:new boolean[]{false,true}){
            boolean allowed=player?difficulty.getId()>=2:difficulty!=Difficulty.PEACEFUL;
            LivingEntity victim=victim(level,player);float health=victim.getHealth();
            var llama=EntityTypes.LLAMA.create(level,EntitySpawnReason.COMMAND);
            class CollisionSpit extends LlamaSpit {
                CollisionSpit(Llama owner){super(level,owner);}
                void hit(LivingEntity target){super.onHitEntity(new EntityHitResult(target));}
            }
            new CollisionSpit(llama).hit(victim);
            require((victim.getHealth()<health)==allowed,"native llama projectile ownership "+difficulty+" player="+player);
            ram(level,player,allowed,false);
        }
        // A previously started ram must not deal damage OR knockback after a forbidden switch.
        level.getServer().setDifficulty(Difficulty.NORMAL,true);
        ram(level,true,false,true);level.getServer().setDifficulty(difficulty,true);
    }
    private static LivingEntity victim(ServerLevel level,boolean player){
        if(!player){var sheep=EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);sheep.setNoAi(true);return sheep;}
        return new Player(level,new GameProfile(UUID.randomUUID(),"CombatPath")){
            @Override public GameType gameMode(){return GameType.SURVIVAL;}
        };
    }
    private static void ram(ServerLevel level,boolean player,boolean allowed,boolean switchPeaceful)throws Exception{
        var goat=EntityTypes.GOAT.create(level,EntitySpawnReason.COMMAND);goat.setNoAi(true);goat.snapTo(-16.5,-60,-16.5,0,0);goat.setOnGround(true);
        var victim=victim(level,player);victim.snapTo(-16.25,-60,-16.5,0,0);victim.setOnGround(true);victim.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(goat);level.addFreshEntity(victim);
        try{
            var brain=goat.getBrain();brain.eraseMemory(MemoryModuleType.RAM_COOLDOWN_TICKS);brain.setMemory(MemoryModuleType.RAM_TARGET,goat.position().add(3,0,0));
            var field=GoatAi.class.getDeclaredField("RAM_TARGET_CONDITIONS");field.setAccessible(true);
            var nativeConditions=((TargetingConditions)field.get(null)).copy();
            var ram=new RamTarget(g->UniformInt.of(20,20),nativeConditions,3,g->3,g->SoundEvents.GOAT_RAM_IMPACT,g->SoundEvents.GOAT_HORN_BREAK);
            long time=level.getGameTime();require(ram.tryStart(level,goat,time),"native ram starts from native memory requirements");
            float health=victim.getHealth();
            if(switchPeaceful)level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
            ram.tickOrStop(level,goat,time+1);
            require((victim.getHealth()<health)==allowed,"native goat ram damage "+level.getDifficulty()+" player="+player+" switch="+switchPeaceful);
            require((victim.getDeltaMovement().lengthSqr()>.001)==allowed,"native goat ram knockback "+level.getDifficulty()+" player="+player+" switch="+switchPeaceful+" velocity="+victim.getDeltaMovement());
        }finally{goat.discard();victim.discard();}
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private WildlifeCombatPaths(){}
}
