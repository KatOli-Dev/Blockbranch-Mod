package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;

/** Optional local recording fixture; resource playback is not human sound-quality approval. */
public final class BisonAudioReview {
    private record Clip(String label,SoundEvent sound,float pitch){}
    private final Clip[] clips;private int index;private long next=System.nanoTime()+1_000_000_000L;
    public BisonAudioReview(){var sounds=SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.MOODY);clips=new Clip[]{
        new Clip("bison-adult-native-moody-ambient",sounds.ambientSound().value(),.75F),
        new Clip("bison-hurt",sounds.hurtSound().value(),.75F),new Clip("bison-death",sounds.deathSound().value(),.75F),
        new Clip("bison-step",sounds.stepSound().value(),.8F),new Clip("bison-calf-native-moody-ambient",sounds.ambientSound().value(),1.125F),
        new Clip("bison-eat-native-horse",SoundEvents.HORSE_EAT,.8F)};}
    public boolean tick(Minecraft client){long now=System.nanoTime();if(now<next)return false;client.getSoundManager().stop();
        if(index==clips.length){Blockbranch.LOGGER.info("BISON_AUDIO_SEQUENCE_DONE clips={}",index);return true;}
        var clip=clips[index++];Blockbranch.LOGGER.info("BISON_AUDIO_CLIP index={} label={} pitch={} utc={}",index,clip.label,clip.pitch,java.time.Instant.now());
        client.getSoundManager().play(SimpleSoundInstance.forUI(clip.sound,clip.pitch,.7F));next=now+3_000_000_000L;return false;}
}
