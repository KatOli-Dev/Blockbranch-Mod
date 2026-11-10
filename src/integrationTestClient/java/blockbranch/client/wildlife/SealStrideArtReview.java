package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Explicitly staged land-stride keyframes; no natural locomotion claim. */
final class SealStrideArtReview {
    private record View(String name,int actor,float phase,boolean moving,boolean quarter){}
    private static final float PEAK=(float)(Math.PI/(2*.8));
    private static final View[] VIEWS={
        new View("idle-front",0,0,false,false),
        new View("stroke-front",0,PEAK,true,false),
        new View("return-front",0,PEAK*3,true,false),
        new View("stroke-quarter",0,PEAK,true,true),
        new View("mid-quarter",0,0,true,true),
        new View("return-quarter",0,PEAK*3,true,true),
        new View("pup-stroke",1,PEAK,true,true),
        new View("pup-return",1,PEAK*3,true,true)
    };
    private static int view=-1,wait;
    private static boolean started,done,captured=true;
    private static volatile boolean ready;
    private static double x,y,z;
    private static final List<UUID> actors=new ArrayList<>();
    private static final List<String> measurements=new ArrayList<>();
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(done)return true;
        if(!started){
            require(!Boolean.getBoolean("blockbranch.foodArtReview")&&!Boolean.getBoolean("blockbranch.perchArtReview")&&!Boolean.getBoolean("blockbranch.itemReview"),"seal stride review runs alone");
            started=true;SealScaleAudit.verifyMesh();SealStrideAudit.verify();WildlifeModelGroundAudit.verify();WildlifeFaceAudit.verify();
            ready=false;client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                var origin=player.blockPosition().offset(24,0,0);x=origin.getX()+.5;y=origin.getY()+1;z=origin.getZ()+.5;
                player.setGameMode(GameType.SPECTATOR);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"weather clear");
                for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=6;dz++)for(int dy=0;dy<=5;dy++)level.setBlock(origin.offset(dx,dy,dz),(dy==0?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
                for(int i=0;i<3;i++){
                    var type=i==2?net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("turtle")):WildlifeRuntime.HARBOR_SEAL;var mob=(Mob)type.create(level,EntitySpawnReason.COMMAND);
                    require(mob!=null,"staged animal exists");mob.setNoAi(true);mob.setNoGravity(true);mob.setPermanentlyInvulnerable(true);mob.setPersistenceRequired();
                    if(i==1)((AgeableMob)mob).setAge(-24000);
                    mob.snapTo(x+(i==0?0:i==1?3:-3),y,z,0,0);require(level.addFreshEntity(mob),"staged animal added");actors.add(mob.getUUID());
                }
                player.connection.teleport(x,y+1,z+6.5,180,14);ready=true;
            });return false;
        }
        if(!ready||!captured)return false;
        if(view>=VIEWS.length){
            Files.writeString(client.gameDirectory.toPath().resolve("seal-stride-keyframes.txt"),String.join("\n",measurements)+"\n");
            done=true;Blockbranch.LOGGER.info("SEAL_STRIDE_ART PASS staged keyframes={}",VIEWS.length);return true;
        }
        View current=view<0?null:VIEWS[view];
        for(var entity:client.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.LivingEntity living&&actors.contains(entity.getUUID())){
            boolean selected=current!=null&&actors.get(current.actor).equals(entity.getUUID());
            float speed=selected&&current.moving?1:0,phase=selected?current.phase:0;
            // Tiny phase scale freezes interpolation while retaining native speed extraction.
            for(var entry:Map.of("speedOld",speed,"speed",speed,"position",phase*10000,"positionScale",.0001F).entrySet()){
                var field=net.minecraft.world.entity.WalkAnimationState.class.getDeclaredField(entry.getKey());field.setAccessible(true);field.setFloat(living.walkAnimation,entry.getValue());
            }
            living.setYBodyRot(0);living.setYHeadRot(0);living.setXRot(0);
        }
        if(++wait<60)return false;
        var section=client.levelRenderer.viewArea().getRenderSectionAt(BlockPos.containing(x,y-1,z));
        require(section!=null&&section.getSectionMesh().hasRenderableLayers(),"review floor mesh ready");
        String name="family";
        if(current!=null){
            HarborSeal subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof HarborSeal seal&&seal.getUUID().equals(actors.get(current.actor)))subject=seal;
            require(subject!=null&&subject.isAlive()&&!subject.isInWater(),"land seal alive");
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
            require(frustum!=null&&frustum.isVisible(subject.getBoundingBox()),"selected seal visible");
            var renderer=(SealRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
            require(Math.abs(state.walkAnimationSpeed-(current.moving?1:0))<.001&&Math.abs(state.walkAnimationPos-current.phase)<.001,"native stride state matches staged keyframe");
            var root=SealModel.layer().bakeRoot();new SealModel(root).setupAnim(state);float bottom=SealStrideAudit.bottom(root);
            require(bottom<=24.0001F,"native land stride foreflippers clear the floor");
            String line=current.name+" phase="+state.walkAnimationPos+" speed="+state.walkAnimationSpeed+" bottomModelY="+bottom+" floorModelY=24";measurements.add(line);Blockbranch.LOGGER.info("SEAL_STRIDE_KEYFRAME {}",line);name=current.name;
        }
        capture(client,"seal-stride-"+name+".png");view++;wait=0;
        if(view<VIEWS.length){
            ready=false;var next=VIEWS[view];client.getSingleplayerServer().execute(()->{
                var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){
                    var entity=level.getEntity(actors.get(i));require(entity!=null&&entity.isAlive(),"staged actor survives");entity.setDeltaMovement(Vec3.ZERO);entity.snapTo(i==next.actor?x:x+16+i*3,y,z,0,0);
                }
                boolean pup=next.actor==1;double distance=pup?1.5:2.8;
                var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.connection.teleport(x+(next.quarter?distance*.65:0),y-(pup?1.3:1.05),z+distance*(next.quarter?.8:1),next.quarter?140:180,7);ready=true;
            });
        }
        return false;
    }
    private static void capture(Minecraft client,String file){captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));}
    private SealStrideArtReview(){}
}
