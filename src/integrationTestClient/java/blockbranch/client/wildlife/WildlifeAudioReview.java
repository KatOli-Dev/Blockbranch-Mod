package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.WildlifeSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.animal.pig.PigSoundVariants;

/** Fixture-triggered resource playback through Minecraft's decoder and OpenAL mix. */
public final class WildlifeAudioReview {
    private record Clip(String label,SoundEvent sound,float pitch){}
    private final Clip[] clips;
    private int index;
    private long next=System.nanoTime()+1_000_000_000L;
    public WildlifeAudioReview(){
        var pig=SoundEvents.PIG_SOUNDS.get(PigSoundVariants.SoundSet.BIG);
        clips=new Clip[]{
            new Clip("boar-adult-ambient",pig.adultSounds().ambientSound().value(),.7F),
            new Clip("boar-adult-hurt",pig.adultSounds().hurtSound().value(),.7F),
            new Clip("boar-adult-death",pig.adultSounds().deathSound().value(),.7F),
            new Clip("boar-adult-eat",pig.adultSounds().eatSound().value(),.7F),
            new Clip("boar-piglet-ambient",pig.babySounds().ambientSound().value(),1.05F),
            new Clip("deer-contact-synthesized",WildlifeSounds.DEER_CONTACT,1),
            new Clip("deer-stag-bellow-synthesized",WildlifeSounds.DEER_BELLOW,1),
            new Clip("deer-hurt-synthesized",WildlifeSounds.DEER_HURT,1),
            new Clip("deer-death-synthesized",WildlifeSounds.DEER_DEATH,1),
            new Clip("deer-hoof-step",SoundEvents.HORSE_STEP,1.2F)};
    }
    public boolean tick(Minecraft client){
        long now=System.nanoTime();if(now<next)return false;
        client.getSoundManager().stop();
        if(index==clips.length){Blockbranch.LOGGER.info("WILDLIFE_AUDIO_SEQUENCE_DONE clips={}",index);return true;}
        var clip=clips[index++];
        Blockbranch.LOGGER.info("WILDLIFE_AUDIO_CLIP index={} label={} pitch={} utc={}",index,clip.label,clip.pitch,java.time.Instant.now());
        client.getSoundManager().play(SimpleSoundInstance.forUI(clip.sound,clip.pitch,.7F));
        next=now+3_000_000_000L;return false;
    }
}
