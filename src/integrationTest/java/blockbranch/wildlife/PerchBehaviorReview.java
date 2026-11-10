package blockbranch.wildlife;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
/** Opt-in actual swimming and native player avoidance, separate from frozen art subjects. */
public final class PerchBehaviorReview {
    private static EuropeanPerch actor;private static ServerPlayer viewer;private static int phase,ticks,fearCase;private static net.minecraft.world.entity.ai.goal.Goal actualAvoidance;private static boolean avoidanceRan;private static net.minecraft.world.phys.Vec3 fleeStart;
    private static double initialDistance,swimX,swimY,swimZ,camX,camY,camZ;
    public static void register(){
        ServerPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.server().execute(()->{try{
            if(packet.stage()==30){WildlifeReviewCamera.move(context.player(),packet);return;}
            viewer=context.player();var level=viewer.level();
            if(packet.stage()==20){
                int x=(int)Math.floor(viewer.getX()),z=(int)Math.floor(viewer.getZ()+4),y=blockbranch.realistic.PerchReviewScene.waterDatum();
                for(int dx=-6;dx<=6;dx++)for(int dz=-4;dz<=5;dz++){
                    level.setBlock(new BlockPos(x+dx,y-4,z+dz),Blocks.STONE.defaultBlockState(),2);
                    for(int dy=-3;dy<0;dy++)level.setBlock(new BlockPos(x+dx,y+dy,z+dz),(Math.abs(dx)==6||dz==-4||dz==5?Blocks.STONE:Blocks.WATER).defaultBlockState(),2);
                    for(int dy=0;dy<=3;dy++)level.setBlock(new BlockPos(x+dx,y+dy,z+dz),Blocks.AIR.defaultBlockState(),2);
                }
                actor=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);actor.snapTo(x-2.5,y-1,z+.5,90,0);actor.setPersistenceRequired();level.addFreshEntity(actor);actualAvoidance=actor.getGoalSelector().getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g instanceof WildlifeAvoidGoal).findFirst().orElseThrow();actor.removeAllGoals(g->true);
                swimX=x+2.5;swimY=y-1;swimZ=z+.5;camX=x+.5;camY=y+.5;camZ=z+5.5;
                WildlifeReviewCamera.move(viewer,new WildlifeProbe(30,(x+.5)+" "+(y+.5)+" "+(z+5.5)+" 180 38"));phase=1;ticks=0;
            }else if(packet.stage()==21){
                fearCase=0;startFear();
            }else if(packet.stage()==22){viewer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);viewer.setPermanentlyInvulnerable(false);actor.discard();phase=0;}
        }catch(Throwable error){fail(error.toString());}}));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(phase==0)return;ticks++;
            if(ticks%5==0)for(var item:viewer.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(camX-8,swimY-5,swimZ-7,camX+8,swimY+5,swimZ+7)))item.discard();
            if(phase==1&&ticks==10&&!actor.getNavigation().moveTo(swimX,swimY,swimZ,1)){fail("Native swim path missing after fluid initialization");return;}
            if(phase==1&&ticks==60){if(!actor.isInWater()){fail("Review swimmer left water");return;}Blockbranch.LOGGER.info("PERCH_SWIM_REVIEW pos={} inWater=true",actor.position());ServerPlayNetworking.send(viewer,new WildlifeProbe(20,"swimming"));}
            if(phase==2&&actor.getGoalSelector().getAvailableGoals().stream().anyMatch(g->g.isRunning()&&g.getGoal() instanceof net.minecraft.world.entity.ai.goal.AvoidEntityGoal))avoidanceRan=true;
            if(phase==2&&ticks>=20&&avoidanceRan&&actor.isInWater()&&horizontal(actor.position(),viewer.position())>initialDistance+9&&horizontal(actor.position(),fleeStart)>1){
                Blockbranch.LOGGER.info("PERCH_FLEE_REVIEW difficulty={} initialHorizontalDistance2={} finalHorizontalDistance2={} actualSurvivalPlayer=true productionAvoidance=true",net.minecraft.world.Difficulty.values()[fearCase],initialDistance,horizontal(actor.position(),viewer.position()));
                if(++fearCase<4){startFear();return;}
                viewer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);WildlifeReviewCamera.move(viewer,new WildlifeProbe(30,camX+" "+camY+" "+camZ+" 180 38"));ServerPlayNetworking.send(viewer,new WildlifeProbe(21,"fleeing four difficulties"));phase=3;
            }
            if(phase==2&&ticks>200)fail("Native player avoidance timed out distance2="+actor.distanceToSqr(viewer));
        });
    }
    private static void startFear(){
        viewer.level().getServer().setDifficulty(net.minecraft.world.Difficulty.values()[fearCase],true);
        actor.removeAllGoals(g->true);actor.getGoalSelector().addGoal(2,actualAvoidance);actor.getNavigation().stop();actor.snapTo(camX,swimY,swimZ,90,0);actor.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);actor.getRandom().setSeed(715);
        viewer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);viewer.setPermanentlyInvulnerable(true);viewer.teleportTo(camX-3,swimY,swimZ);
        initialDistance=horizontal(actor.position(),viewer.position());fleeStart=actor.position();avoidanceRan=false;phase=2;ticks=0;
    }
    private static double horizontal(net.minecraft.world.phys.Vec3 a,net.minecraft.world.phys.Vec3 b){double dx=a.x-b.x,dz=a.z-b.z;return dx*dx+dz*dz;}
    private static void fail(String message){Blockbranch.LOGGER.error("PERCH_BEHAVIOR_REVIEW FAIL {}",message);if(viewer!=null)ServerPlayNetworking.send(viewer,new WildlifeProbe(-1,message));phase=0;}
    private PerchBehaviorReview(){}
}
