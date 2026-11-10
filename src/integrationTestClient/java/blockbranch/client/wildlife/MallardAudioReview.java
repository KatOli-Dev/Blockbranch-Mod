package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.*;

/** Optional local recording fixture; resource playback is not human sound-quality approval. */
public final class MallardAudioReview {
    private record Clip(String label,SoundEvent sound,float pitch){}
    private final Clip[] clips;private int index;private long next=System.nanoTime()+1_000_000_000L;
    public MallardAudioReview(){clips=new Clip[]{
        new Clip("mallard-quack",blockbranch.wildlife.WildlifeSounds.MALLARD_QUACK,.8F),
        new Clip("mallard-hurt",blockbranch.wildlife.WildlifeSounds.MALLARD_HURT,.8F),
        new Clip("mallard-death",blockbranch.wildlife.WildlifeSounds.MALLARD_DEATH,.8F),
        new Clip("duckling-quack",blockbranch.wildlife.WildlifeSounds.MALLARD_QUACK,1.2F),
        new Clip("mallard-feet",SoundEvents.CHICKEN_STEP.value(),.8F)};}
    public boolean tick(Minecraft client){long now=System.nanoTime();if(now<next)return false;client.getSoundManager().stop();
        if(index==clips.length){Blockbranch.LOGGER.info("MALLARD_AUDIO_SEQUENCE_DONE clips={}",index);return true;}
        var clip=clips[index++];Blockbranch.LOGGER.info("MALLARD_AUDIO_CLIP index={} label={} pitch={} utc={}",index,clip.label,clip.pitch,java.time.Instant.now());
        client.getSoundManager().play(SimpleSoundInstance.forUI(clip.sound,clip.pitch,.7F));next=now+3_000_000_000L;return false;}
}
