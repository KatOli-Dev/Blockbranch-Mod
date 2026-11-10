package blockbranch.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
public final class WildlifeSounds {
    public static final SoundEvent DEER_CONTACT = register("red_deer.contact");
    public static final SoundEvent DEER_BELLOW = register("red_deer.bellow");
    public static final SoundEvent DEER_HURT = register("red_deer.hurt");
    public static final SoundEvent DEER_DEATH = register("red_deer.death");
    public static final SoundEvent MALLARD_QUACK = register("mallard.quack");
    public static final SoundEvent MALLARD_HURT = register("mallard.hurt");
    public static final SoundEvent MALLARD_DEATH = register("mallard.death");
    public static final SoundEvent BEAVER_CONTACT=register("eurasian_beaver.contact");
    public static final SoundEvent BEAVER_HURT=register("eurasian_beaver.hurt");
    public static final SoundEvent BEAVER_DEATH=register("eurasian_beaver.death");
    public static final SoundEvent PERCH_HURT=register("european_perch.hurt");
    public static final SoundEvent PERCH_DEATH=register("european_perch.death");
    public static final SoundEvent PERCH_FLOP=register("european_perch.flop");
    public static final SoundEvent SEAL_CONTACT=register("harbor_seal.contact");
    public static final SoundEvent SEAL_HURT=register("harbor_seal.hurt");
    public static final SoundEvent SEAL_DEATH=register("harbor_seal.death");
    public static final SoundEvent SEAL_STEP=register("harbor_seal.step");
    private static SoundEvent register(String path) {
        var id = Blockbranch.id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
    public static void initialize() {}
    private WildlifeSounds() {}
}
