package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/** Fixed aquatic travel keyframes; staged velocity inputs, not natural swimming evidence. */
final class SealSwimArtReview {
    private record View(String name,int actor,double vertical,boolean quarter,boolean land){}
    private static final View[] VIEWS={
        new View("adult-cruise",0,0,false,false),new View("adult-dive",0,-.04,false,false),
        new View("adult-rise",0,.04,false,false),new View("pup-cruise",1,0,false,false),
        new View("pup-dive",1,-.04,false,false),new View("pup-rise",1,.04,false,false),
        new View("adult-dive-quarter",0,-.04,true,false),new View("adult-rise-quarter",0,.04,true,false),
        new View("land-control",0,0,false,true)
    };
    private static final List<UUID> actors=new ArrayList<>();
    private static volatile boolean ready;
    private static boolean started,captured=true;
    private static int view=-1,wait;
    private static double x,y,z;
    private static boolean audited;
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(!started){
            started=true;
            require(!Boolean.getBoolean("blockbranch.itemReview")&&!Boolean.getBoolean("blockbranch.foodArtReview")&&!Boolean.getBoolean("blockbranch.perchArtReview"),"seal swim review runs alone");
            SealScaleAudit.verifyMesh();WildlifeModelGroundAudit.verify();WildlifeFaceAudit.verify();
            client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                var origin=player.blockPosition().offset(24,0,0);x=origin.getX()+.5;y=origin.getY();z=origin.getZ()+.5;
                player.setGameMode(GameType.SPECTATOR);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"weather clear");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"effect give @a minecraft:night_vision 600 0 true");
                for(int dx=-7;dx<=7;dx++)for(int dz=-6;dz<=6;dz++)for(int dy=-1;dy<=8;dy++){
                    var block=dy==-1?Blocks.STONE:dy==0?Blocks.SAND:Math.abs(dx)==7||Math.abs(dz)==6?Blocks.GLASS:dy<=7?Blocks.WATER:Blocks.AIR;
                    level.setBlock(origin.offset(dx,dy,dz),block.defaultBlockState(),3);
                }
                for(int i=0;i<3;i++){
                    var type=i==2?net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("dolphin")):WildlifeRuntime.HARBOR_SEAL;
                    var animal=(Mob)type.create(level,EntitySpawnReason.COMMAND);require(animal!=null,"aquatic actor exists");
                    animal.setNoAi(true);animal.setNoGravity(true);animal.setPermanentlyInvulnerable(true);animal.setPersistenceRequired();
                    if(i==1)((AgeableMob)animal).setAge(-24000);
                    animal.snapTo(x+(i-1)*2.5,y+3,z,0,0);require(level.addFreshEntity(animal),"aquatic actor added");actors.add(animal.getUUID());
                }
                player.connection.teleport(x,y+1.8,z+5.5,180,0);ready=true;
            });return false;
        }
        if(!ready||!captured)return false;
        if(view>=VIEWS.length){Files.writeString(client.gameDirectory.toPath().resolve("seal-swim-review.txt"),"PASS staged native aquatic travel keyframes and land control; not natural swimming/motion qualification\n");return true;}
        var current=view<0?null:VIEWS[view];
        client.getSingleplayerServer().execute(()->{
            var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){
                var animal=level.getEntity(actors.get(i));require(animal!=null&&animal.isAlive(),"aquatic actor alive");
                double actorX=current==null||i==current.actor?x+(i-1)*2.5:x+20+i*3;
                animal.snapTo(actorX,y+(current!=null&&current.land&&i==current.actor?9:3),z,0,0);animal.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            }
            for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BlockPos.containing(x,y+3,z)).inflate(12)))drop.discard();
        });
        for(var entity:client.level.entitiesForRendering())if(entity instanceof HarborSeal seal&&actors.contains(entity.getUUID())){
            boolean selected=current!=null&&actors.get(current.actor).equals(seal.getUUID());
            double vertical=selected&&!current.land?current.vertical:0;
            float pitch=(float)Math.toDegrees(Math.atan2(-vertical,.08));
            seal.setDeltaMovement(0,vertical,selected&&!current.land?.08:0);
            seal.setXRot(pitch);seal.xRotO=pitch;seal.setYBodyRot(0);seal.yBodyRotO=0;seal.setYHeadRot(0);seal.yHeadRotO=0;
        }
        if(++wait<70)return false;
        if(current!=null){
            HarborSeal subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof HarborSeal seal&&seal.getUUID().equals(actors.get(current.actor)))subject=seal;
            require(subject!=null&&subject.isAlive()&&subject.isInWater()!=current.land,"selected seal fluid state");
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
            require(frustum!=null&&frustum.isVisible(subject.getBoundingBox()),"selected seal visible");
            var renderer=(SealRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,1);
            if(!audited){SealSwimPoseAudit.verify(renderer);audited=true;}
            var root=SealModel.layer().bakeRoot();new SealModel(root).setupAnim(state);
            var pose=new PoseStack();pose.scale(state.scale,state.scale,state.scale);renderer.setupRotations(state,pose,0,1);
            pose.scale(-1,-1,1);renderer.scale(state,pose);pose.translate(0,-1.501F,0);
            float[] ends={0,0};int[] counts={0,0};
            root.visit(pose,(matrix,path,index,cube)->{
                if(!path.equals("/body")||index!=0)return;
                for(var face:cube.polygons)if(Math.abs(face.normal().z())>.99F){
                    int end=face.normal().z()<0?0:1;
                    for(var vertex:face.vertices()){ends[end]+=matrix.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y;counts[end]++;}
                }
            });
            require(counts[0]==4&&counts[1]==4,"both torso end faces measured");
            float slope=ends[0]/4-ends[1]/4;
            if(!current.land&&current.vertical!=0)require(Math.signum(slope)==Math.signum(current.vertical)&&Math.abs(slope)>.1,"torso follows vertical travel");
            else require(Math.abs(slope)<.0001,"cruise/land torso remains level");
            Blockbranch.LOGGER.info("SEAL_SWIM_KEYFRAME name={} inputVertical={} horizontal={} gazePitch={} torsoFrontMinusBackY={} baby={}",current.name,current.vertical,current.land?0:.08,state.xRot,ends[0]/4-ends[1]/4,state.isBaby);
        }
        String file="seal-swim-"+(current==null?"family":current.name)+".png";
        captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));
        view++;wait=0;
        if(view<VIEWS.length){ready=false;var next=VIEWS[view];client.getSingleplayerServer().execute(()->{
            if(next.land){var level=client.getSingleplayerServer().overworld();for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=0;dy<=3;dy++)level.setBlock(BlockPos.containing(x-2.5+dx,y+8+dy,z+dz),(dy==0?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);}
            double ax=x+(next.actor-1)*2.5,distance=next.actor==1?1.7:3;
            client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(ax+distance*(next.quarter?.65:1),y+(next.land?9:3)-(next.actor==1?1.5:1.35),z+distance*(next.quarter?.8:.35),next.quarter?140:109.29F,0);ready=true;
        });}
        return false;
    }
    private SealSwimArtReview(){}
}
