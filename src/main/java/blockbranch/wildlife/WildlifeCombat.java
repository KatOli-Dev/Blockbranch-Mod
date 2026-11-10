package blockbranch.wildlife;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;

/** Shared targeting and final damage guard, including vanilla brain-driven hunters. */
public final class WildlifeCombat {
    public static boolean isAnimal(Entity entity) {
        return entity instanceof Animal || entity instanceof WaterAnimal
            || entity instanceof AgeableWaterCreature || entity instanceof Bat;
    }
    public static boolean permits(Entity attacker, Entity target) {
        if (!isAnimal(attacker)) return true;
        Difficulty difficulty = attacker.level().getDifficulty();
        if (target instanceof Player) return difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD;
        return !isAnimal(target) || difficulty != Difficulty.PEACEFUL;
    }
    private WildlifeCombat() {}
}
