package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.*;

/** Optional local recording fixture; resource playback is not human sound-quality approval. */
public final class BearAudioReview {
    private record Clip(String label,SoundEvent sound,float pitch){}
    private final Clip[] clips;private int index;private long next=System.nanoTime()+1_000_000_000L;
    public BearAudioReview(){clips=new Clip[]{
        new Clip("brown-bear-adult-native-bear-ambient",SoundEvents.POLAR_BEAR_AMBIENT,.9F),
        new Clip("brown-bear-hurt",SoundEvents.POLAR_BEAR_HURT,.9F),new Clip("brown-bear-death",SoundEvents.POLAR_BEAR_DEATH,.9F),
        new Clip("brown-bear-warning",SoundEvents.POLAR_BEAR_WARNING,.9F),new Clip("brown-bear-cub",SoundEvents.POLAR_BEAR_AMBIENT_BABY,1.35F),
        new Clip("brown-bear-eating",SoundEvents.GENERIC_EAT.value(),.8F)};}
    public boolean tick(Minecraft client){long now=System.nanoTime();if(now<next)return false;client.getSoundManager().stop();
        if(index==clips.length){Blockbranch.LOGGER.info("BEAR_AUDIO_SEQUENCE_DONE clips={}",index);return true;}
        var clip=clips[index++];Blockbranch.LOGGER.info("BEAR_AUDIO_CLIP index={} label={} pitch={} utc={}",index,clip.label,clip.pitch,java.time.Instant.now());
        client.getSoundManager().play(SimpleSoundInstance.forUI(clip.sound,clip.pitch,.7F));next=now+3_000_000_000L;return false;}
}
