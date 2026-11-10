package blockbranch.wildlife;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
/** Opt-in staged pond and real short-flight review; no handler in ordinary worlds. */
public final class MallardBehaviorReview {
    private static Mallard actor;private static ServerPlayer viewer;private static int phase,ticks;
    public static void register(){
        ServerPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.server().execute(()->{try{
            if(packet.stage()==30){WildlifeReviewCamera.move(context.player(),packet);return;}
            viewer=context.player();var level=viewer.level();
            if(packet.stage()==20){
                int x=(int)Math.floor(viewer.getX()),y=(int)Math.floor(viewer.getY()-1.5),z=(int)Math.floor(viewer.getZ()-5);
                for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=2;dz++){
                    level.setBlock(new BlockPos(x+dx,y-2,z+dz),Blocks.STONE.defaultBlockState(),2);
                    level.setBlock(new BlockPos(x+dx,y-1,z+dz),Blocks.WATER.defaultBlockState(),2);
                }
                actor=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);actor.snapTo(x+.5,y-.9,z+.5,90,0);actor.setDrake(true);actor.setPersistenceRequired();level.addFreshEntity(actor);phase=1;ticks=0;
            }else if(packet.stage()==21){if(!actor.beginShortFlight())throw new AssertionError("Review flight did not launch");phase=2;ticks=0;}
            else if(packet.stage()==22){actor.discard();phase=0;}
        }catch(Throwable error){Blockbranch.LOGGER.error("MALLARD_BEHAVIOR_REVIEW FAIL",error);ServerPlayNetworking.send(viewer,new WildlifeProbe(-1,error.toString()));}}));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(phase==0)return;ticks++;
            if(phase==1&&ticks==60){Blockbranch.LOGGER.info("MALLARD_SWIM_REVIEW pos={} inWater={} air={}",actor.position(),actor.isInWater(),actor.getAirSupply());ServerPlayNetworking.send(viewer,new WildlifeProbe(20,"swimming"));}
            if(phase==2&&ticks==8){Blockbranch.LOGGER.info("MALLARD_FLIGHT_REVIEW pos={} onGround={} activity={}",actor.position(),actor.onGround(),actor.activity());ServerPlayNetworking.send(viewer,new WildlifeProbe(21,"flying"));}
        });
    }
    private MallardBehaviorReview(){}
}
