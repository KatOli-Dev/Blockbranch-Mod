package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.*;

/** Optional local recording fixture; resource playback is not human sound-quality approval. */
public final class BeaverAudioReview {
    private record Clip(String label,SoundEvent sound,float pitch){}
    private final Clip[] clips;private int index;private long next=System.nanoTime()+1_000_000_000L;
    public BeaverAudioReview(){clips=new Clip[]{
        new Clip("beaver-contact",blockbranch.wildlife.WildlifeSounds.BEAVER_CONTACT,.8F),
        new Clip("beaver-hurt",blockbranch.wildlife.WildlifeSounds.BEAVER_HURT,.8F),
        new Clip("beaver-death",blockbranch.wildlife.WildlifeSounds.BEAVER_DEATH,.8F),
        new Clip("kit-contact",blockbranch.wildlife.WildlifeSounds.BEAVER_CONTACT,1.2F),
        new Clip("beaver-feet",SoundEvents.RABBIT_JUMP,.8F)};}
    public boolean tick(Minecraft client){long now=System.nanoTime();if(now<next)return false;client.getSoundManager().stop();
        if(index==clips.length){Blockbranch.LOGGER.info("BEAVER_AUDIO_SEQUENCE_DONE clips={}",index);return true;}
        var clip=clips[index++];Blockbranch.LOGGER.info("BEAVER_AUDIO_CLIP index={} label={} pitch={} utc={}",index,clip.label,clip.pitch,java.time.Instant.now());
        client.getSoundManager().play(SimpleSoundInstance.forUI(clip.sound,clip.pitch,.7F));next=now+3_000_000_000L;return false;}
}
