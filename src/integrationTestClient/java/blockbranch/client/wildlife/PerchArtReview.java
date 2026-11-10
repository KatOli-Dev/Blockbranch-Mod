package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.EuropeanPerch;
import blockbranch.wildlife.WildlifeRuntime;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Opt-in aquarium poses for inspecting both sides of the planar fish fins. */
final class PerchArtReview {
    private static int stage,wait;
    private static volatile boolean ready;
    private static boolean captured=true;
    private static double x,y,z;
    private static final List<UUID> actors=new ArrayList<>();
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(stage==0){
            ready=false;
            client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                x=Math.floor(player.getX())+24;y=Math.floor(player.getY());z=Math.floor(player.getZ());
                player.setGameMode(GameType.SPECTATOR);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"effect give @a minecraft:night_vision 600 0 true");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"weather clear");
                for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=5;dz++)for(int dy=-1;dy<=6;dy++){
                    var block=dy<0?Blocks.STONE:dy==0?Blocks.SAND:Math.abs(dx)==6||Math.abs(dz)==5?Blocks.GLASS:dy<=5?Blocks.WATER:Blocks.AIR;
                    level.setBlock(BlockPos.containing(x+dx,y+dy,z+dz),block.defaultBlockState(),3);
                }
                var perch=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);
                var cod=(Mob)net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cod")).create(level,EntitySpawnReason.COMMAND);
                require(perch!=null&&cod!=null,"fish creation");
                for(var fish:List.of(perch,cod)){fish.setNoAi(true);fish.setNoGravity(true);fish.setPermanentlyInvulnerable(true);fish.setPersistenceRequired();fish.snapTo(x+(actors.isEmpty()?-1:1),y+2,z,0,0);require(level.addFreshEntity(fish),"fish added");actors.add(fish.getUUID());}
                ready=true;
            });stage=1;return false;
        }
        if(!ready)return false;
        // Fish travel can add sinking even without gravity. Freeze only these review actors.
        client.getSingleplayerServer().execute(()->{
            var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){
                var fish=level.getEntity(actors.get(i));require(fish!=null&&fish.isAlive(),"staged fish survives");fish.snapTo(x+(i==0?-1:1),y+2,z,0,0);fish.setDeltaMovement(Vec3.ZERO);
            }
        });
        if(stage>=12){if(!captured)return false;Files.writeString(client.gameDirectory.toPath().resolve("perch-art-review.txt"),"PASS native staged aquarium; both fin faces; perch scale; vanilla cod context; no natural behavior claim\n");return true;}
        if(stage%2==1){
            if(!captured)return false;int target=(stage-1)/2;ready=false;
            client.getSingleplayerServer().execute(()->{
                var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                switch(target){
                    case 0->player.connection.teleport(x,y+.6,z+2.7,180,0);
                    case 1->player.connection.teleport(x-.1,y+.6,z+.32,109.29F,0);
                    case 2->player.connection.teleport(x-1.9,y+.6,z+.32,250.71F,0);
                    case 3->player.connection.teleport(x-1,y+.6,z+.9,180,0);
                    case 4->player.connection.teleport(x-.4,y+.6,z-.8,36.87F,0);
                    default->throw new AssertionError("camera");
                }ready=true;
            });stage++;wait=0;return false;
        }
        if(++wait<80)return false;
        EuropeanPerch subject=null;for(var entity:client.level.entitiesForRendering())if(entity.getUUID().equals(actors.getFirst()))subject=(EuropeanPerch)entity;
        require(subject!=null&&subject.isAlive()&&subject.isInWater(),"native perch alive and submerged");
        var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
        require(frustum!=null&&frustum.isVisible(subject.getBoundingBox()),"perch in capture frustum");
        var section=client.levelRenderer.viewArea().getRenderSectionAt(BlockPos.containing(x,y,z));
        require(section!=null&&section.getSectionMesh().hasRenderableLayers(),"aquarium mesh ready");
        PerchScaleAudit.measure(client,subject);
        String file=switch(stage){case 2->"perch-context.png";case 4->"perch-left.png";case 6->"perch-right.png";case 8->"perch-front.png";case 10->"perch-tail.png";default->throw new AssertionError("capture");};
        captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));
        Blockbranch.LOGGER.info("PERCH_ART_CAPTURE {} explicitlyStaged=true nightVision=true",file);stage++;if(stage==11)stage=12;return false;
    }
    private PerchArtReview(){}
}
