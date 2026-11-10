package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.*;

/** Optional local recording fixture; resource playback is not human sound-quality approval. */
public final class SealAudioReview {
    private record Clip(String label,SoundEvent sound,float pitch){}
    private final Clip[] clips;private int index;private long next=System.nanoTime()+1_000_000_000L;
    public SealAudioReview(){clips=new Clip[]{
        new Clip("seal-contact",blockbranch.wildlife.WildlifeSounds.SEAL_CONTACT,1F),
        new Clip("seal-hurt",blockbranch.wildlife.WildlifeSounds.SEAL_HURT,1F),
        new Clip("seal-death",blockbranch.wildlife.WildlifeSounds.SEAL_DEATH,1F),
        new Clip("pup-contact",blockbranch.wildlife.WildlifeSounds.SEAL_CONTACT,1.5F),
        new Clip("seal-feet",blockbranch.wildlife.WildlifeSounds.SEAL_STEP,1F)};}
    public boolean tick(Minecraft client){long now=System.nanoTime();if(now<next)return false;client.getSoundManager().stop();
        if(index==clips.length){Blockbranch.LOGGER.info("SEAL_AUDIO_SEQUENCE_DONE clips={}",index);return true;}
        var clip=clips[index++];Blockbranch.LOGGER.info("SEAL_AUDIO_CLIP index={} label={} pitch={} utc={}",index,clip.label,clip.pitch,java.time.Instant.now());
        client.getSoundManager().play(SimpleSoundInstance.forUI(clip.sound,clip.pitch,.7F));next=now+3_000_000_000L;return false;}
}
