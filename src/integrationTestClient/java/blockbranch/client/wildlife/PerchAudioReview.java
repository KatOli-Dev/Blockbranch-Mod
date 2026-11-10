package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.*;

/** Optional local recording fixture; resource playback is not human sound-quality approval. */
public final class PerchAudioReview {
    private record Clip(String label,SoundEvent sound,float pitch){}
    private final Clip[] clips;private int index;private long next=System.nanoTime()+1_000_000_000L;
    public PerchAudioReview(){clips=new Clip[]{
        new Clip("perch-hurt",blockbranch.wildlife.WildlifeSounds.PERCH_HURT,1F),
        new Clip("perch-death",blockbranch.wildlife.WildlifeSounds.PERCH_DEATH,1F),
        new Clip("perch-flop",blockbranch.wildlife.WildlifeSounds.PERCH_FLOP,1F),
        new Clip("bucket-pickup",SoundEvents.BUCKET_FILL_FISH,1F),
        new Clip("bucket-release",SoundEvents.BUCKET_EMPTY_FISH,1F)};}
    public boolean tick(Minecraft client){long now=System.nanoTime();if(now<next)return false;client.getSoundManager().stop();
        if(index==clips.length){Blockbranch.LOGGER.info("PERCH_AUDIO_SEQUENCE_DONE clips={}",index);return true;}
        var clip=clips[index++];Blockbranch.LOGGER.info("PERCH_AUDIO_CLIP index={} label={} pitch={} utc={}",index,clip.label,clip.pitch,java.time.Instant.now());
        client.getSoundManager().play(SimpleSoundInstance.forUI(clip.sound,clip.pitch,.7F));next=now+3_000_000_000L;return false;}
}
