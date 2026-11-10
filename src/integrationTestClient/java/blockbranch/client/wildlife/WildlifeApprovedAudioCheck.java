package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.util.RandomSource;

/** Opt-in native resource/decoder audit. No sound-quality or audible-playback verdict. */
final class WildlifeApprovedAudioCheck {
    private static boolean finished;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void register(){
        long deadline=System.nanoTime()+120_000_000_000L;
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(finished)return;
            try{
                require(System.nanoTime()<deadline,"audio resource audit timeout");
                if(!(client.gui.screen() instanceof TitleScreen))return;
                BoarModelCheck.verifyRevision();BearModelCheck.verifyRevision();DeerModelCheck.verifyRevision();
                var manager=client.getSoundManager();var random=RandomSource.create(31835L);
                var selected=new TreeMap<String,net.minecraft.client.resources.sounds.Sound>();
                var report=new StringBuilder("PASS native all-variant ordinary/hurt/death audio mappings\n");
                for(String event:List.of("mallard.quack","mallard.hurt","mallard.death","red_deer.contact","red_deer.bellow","red_deer.hurt","red_deer.death","harbor_seal.contact","harbor_seal.hurt","harbor_seal.death")){
                    boolean duck=event.startsWith("mallard."),seal=event.startsWith("harbor_seal.");
                    float pitch=!seal&&(event.endsWith(".hurt")||event.endsWith(".death"))?1.1F:1F;
                    var loaded=manager.getSoundEvent(Blockbranch.id(event));
                    require(loaded!=null&&loaded.getWeight()==(duck?5:seal?4:1),"resolved alternatives for "+event);
                    var expected=new TreeSet<String>();
                    if(duck)for(int i=1;i<=5;i++)expected.add("blockbranch:mallard/quack"+i);
                    else if(seal)for(int i=1;i<=4;i++)expected.add("blockbranch:harbor_seal/call"+i);
                    else expected.add("blockbranch:red_deer/bellow_dobroide_104581");
                    var counts=new TreeMap<String,Integer>();
                    for(int i=0;i<1024;i++){
                        var sound=loaded.getSound(random);String id=sound.getLocation().toString();
                        require(expected.contains(id)&&sound.getWeight()==1,"approved equal-weight file for "+event);
                        require(sound.getVolume().sample(random)==1F&&Math.abs(sound.getPitch().sample(random)-pitch)<.00001F,"native pitch multiplier for "+event);
                        counts.merge(id,1,Integer::sum);selected.put(id,sound);
                    }
                    require(counts.keySet().equals(expected),"all alternatives selected for "+event);
                    report.append(event).append(" pitch=").append(pitch).append(" draws=").append(counts).append('\n');
                }
                for(var entry:selected.entrySet()){
                    int[] samples={0};float[] peak={0};
                    try(var input=client.getResourceManager().getResourceOrThrow(entry.getValue().getPath()).open();var audio=new JOrbisAudioStream(input)){
                        require(audio.getFormat().getChannels()==1&&audio.getFormat().getSampleRate()==44100F,"mono 44100Hz "+entry.getKey());
                        while(audio.readChunk(value->{require(Float.isFinite(value),"finite audio samples");samples[0]++;peak[0]=Math.max(peak[0],Math.abs(value));}))require(samples[0]<1_000_000,"bounded decoded audio");
                    }
                    require(samples[0]>0&&peak[0]>0&&peak[0]<1,"nonempty unclipped native decode "+entry.getKey());
                    report.append(entry.getKey()).append(" samples=").append(samples[0]).append(" peak=").append(peak[0]).append('\n');
                }
                Files.writeString(Path.of(System.getProperty("blockbranch.approvedAudioResult")),report);
                Blockbranch.LOGGER.info("APPROVED_AUDIO_NATIVE {}",report);finished=true;client.stop();
            }catch(Throwable error){
                finished=true;Blockbranch.LOGGER.error("APPROVED_AUDIO_NATIVE FAIL",error);
                try{Files.writeString(Path.of(System.getProperty("blockbranch.approvedAudioResult")),"FAIL "+error+"\n");}catch(Exception ignored){}
                client.stop();
            }
        });
    }
    private WildlifeApprovedAudioCheck(){}
}
