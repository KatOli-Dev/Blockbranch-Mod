package blockbranch.wildlife;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
/** Opt-in staged pond and actual land return. No terrain edits in animal behavior. */
public final class SealBehaviorReview {
    private static HarborSeal actor;private static ServerPlayer viewer;private static int phase,ticks;private static double shoreZ;private static int y;
    public static void register(){
        SealPlayerCheck.register();
        ServerPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.server().execute(()->{try{
            if(packet.stage()==10){SealPlayerCheck.begin(context.player());return;}
            if(packet.stage()==30){WildlifeReviewCamera.move(context.player(),packet);return;}
            viewer=context.player();var level=viewer.level();
            if(packet.stage()==20){
                int x=(int)Math.floor(viewer.getX()),z=(int)Math.floor(viewer.getZ()-5);y=(int)Math.floor(viewer.getY()-1.5);shoreZ=z-2.5;
                for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=2;dz++){
                    level.setBlock(new BlockPos(x+dx,y-2,z+dz),Blocks.STONE.defaultBlockState(),2);level.setBlock(new BlockPos(x+dx,y-1,z+dz),Blocks.WATER.defaultBlockState(),2);
                }
                actor=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);actor.snapTo(x+.5,y-.9,z+.5,90,0);actor.setPersistenceRequired();level.addFreshEntity(actor);actor.removeAllGoals(g->!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));phase=1;ticks=0;
            }else if(packet.stage()==21){actor.removeAllGoals(g->!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));if(!actor.getNavigation().moveTo(actor.getX(),y,shoreZ,1))throw new AssertionError("Land return path missing");phase=2;ticks=0;}
            else if(packet.stage()==22){actor.discard();phase=0;}
        }catch(Throwable error){Blockbranch.LOGGER.error("SEAL_BEHAVIOR_REVIEW FAIL",error);ServerPlayNetworking.send(viewer,new WildlifeProbe(-1,error.toString()));}}));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(phase==0)return;ticks++;
            if(phase==1&&ticks==60){if(!actor.isInWater())throw new AssertionError("Review actor must actually be in water");Blockbranch.LOGGER.info("SEAL_SWIM_REVIEW pos={} inWater={} air={}",actor.position(),actor.isInWater(),actor.getAirSupply());ServerPlayNetworking.send(viewer,new WildlifeProbe(20,"swimming"));}
            if(phase==2&&ticks>20&&!actor.isInWater()&&actor.onGround()){Blockbranch.LOGGER.info("SEAL_LAND_REVIEW pos={} onGround=true",actor.position());ServerPlayNetworking.send(viewer,new WildlifeProbe(21,"land"));phase=3;}
            if(phase==2&&ticks>300){ServerPlayNetworking.send(viewer,new WildlifeProbe(-1,"Seal land return timed out"));phase=0;}
        });
    }
    private SealBehaviorReview(){}
}
