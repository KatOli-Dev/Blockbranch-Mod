package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/** Constrained pond: native vertical travel and FloatGoal, fixed horizontal positions. */
final class BeaverWaterArtReview {
    private static final List<UUID> actors=new ArrayList<>();
    private static volatile boolean ready;
    private static boolean started,captured=true;
    private static int view=-1,wait;
    private static double x,surface,z;
    private static final String[] NAMES={"adult-active-side","adult-rest-side","kit-active-side","kit-rest-side","adult-active-front","adult-rest-front","kit-active-front","kit-rest-front","adult-land-rest","kit-land-rest"};
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static int selected(){return view>=8?(view==8?1:3):view<4?view:view-4;}
    static boolean tick(Minecraft client)throws Exception{
        if(!started){
            started=true;
            require(!Boolean.getBoolean("blockbranch.itemReview")&&!Boolean.getBoolean("blockbranch.foodArtReview")&&!Boolean.getBoolean("blockbranch.perchArtReview"),"beaver water review runs alone");
            BeaverWaterPoseAudit.verify();
            client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                var origin=player.blockPosition().offset(24,0,0);x=origin.getX()+.5;surface=origin.getY()+1;z=origin.getZ()+.5;
                player.setGameMode(GameType.SPECTATOR);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"weather clear");
                for(int dx=-9;dx<=9;dx++)for(int dz=-6;dz<=7;dz++)for(int dy=-3;dy<=5;dy++){
                    var block=dy>0?Blocks.AIR:dy==-3||Math.abs(dx)==9||dz==-6||dz==7?Blocks.SMOOTH_STONE:Blocks.WATER;
                    level.setBlock(origin.offset(dx,dy,dz),block.defaultBlockState(),3);
                }
                for(int i=0;i<4;i++){
                    var animal=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);require(animal!=null,"beaver exists");
                    if(i>=2)animal.setAge(-24000);
                    animal.setPermanentlyInvulnerable(true);animal.setPersistenceRequired();animal.removeAllGoals(goal->!(goal instanceof FloatGoal));
                    try{var field=EurasianBeaver.class.getDeclaredField("ACTIVITY");field.setAccessible(true);animal.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i%2==1?2:0);}catch(Exception e){throw new RuntimeException(e);}
                    animal.snapTo(x+(i-1.5)*4,surface-.15,z,0,0);require(level.addFreshEntity(animal),"beaver added");actors.add(animal.getUUID());
                }
                // The overview eye must clear the surrounding two-block grass.
                player.connection.teleport(x,surface+1.4,z+11,180,16);ready=true;
            });return false;
        }
        if(!ready||!captured)return false;
        if(view>=NAMES.length){Files.writeString(client.gameDirectory.toPath().resolve("beaver-water-review.txt"),"PASS constrained native vertical buoyancy; eight adult/kit active/rest pond portraits, family and two land-rest controls; no free-roaming behavior or safety qualification\n");return true;}
        client.getSingleplayerServer().execute(()->{
            var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){
                var animal=level.getEntity(actors.get(i));require(animal!=null&&animal.isAlive(),"pond actor survives");
                animal.snapTo(x+(i-1.5)*4,animal.getY(),z,0,0);animal.setDeltaMovement(0,animal.getDeltaMovement().y,0);
            }
            for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BlockPos.containing(x,surface,z)).inflate(15)))drop.discard();
        });
        if(++wait<(view<0?200:100))return false;
        if(view>=0){
            EurasianBeaver animal=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof EurasianBeaver beaver&&entity.getUUID().equals(actors.get(selected())))animal=beaver;
            require(animal!=null&&animal.isAlive()&&animal.isInWater()==(view<8),"selected beaver alive in expected fluid state");
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
            require(frustum!=null&&frustum.isVisible(animal.getBoundingBox()),"selected beaver in frustum");
            observe(client,animal,NAMES[view]);
        }
        String file="beaver-water-"+(view<0?"family":NAMES[view])+".png";
        captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));
        view++;wait=0;
        if(view<NAMES.length){ready=false;client.getSingleplayerServer().execute(()->{
            int selected=selected();boolean land=view>=8,front=view>=4&&!land,kit=selected>=2;double ax=x+(selected-1.5)*4,distance=kit?1.25:2.2;
            if(land){
                var level=client.getSingleplayerServer().overworld();
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.setBlock(BlockPos.containing(ax+dx,surface,z+dz),Blocks.SMOOTH_STONE.defaultBlockState(),3);
                level.getEntity(actors.get(selected)).snapTo(ax,surface+1,z,0,0);
            }
            client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(ax+(front?0:distance),surface+(land?1:0)-(kit?1.48:1.35),z+distance*(front?1:.35),front?180:109.29F,land?5:front?7:10);ready=true;
        });}
        return false;
    }
    private static void observe(Minecraft client,EurasianBeaver animal,String name)throws Exception{
        var renderer=(BeaverRenderer)client.getEntityRenderDispatcher().getRenderer(animal);var state=renderer.createRenderState();renderer.extractRenderState(animal,state,1);
        var root=BeaverModel.layer().bakeRoot();new BeaverModel(root).setupAnim(state);
        var pose=new PoseStack();pose.scale(state.scale,state.scale,state.scale);pose.scale(-1,-1,1);renderer.scale(state,pose);pose.translate(0,-1.501F,0);
        double[] nose={Double.NaN},headTop={Double.NEGATIVE_INFINITY};
        root.visit(pose,(matrix,path,index,cube)->{
            if(!path.equals("/head"))return;
            if(index==1)nose[0]=animal.getY()+matrix.pose().transformPosition(new Vector3f(0,.5F/16,cube.minZ/16)).y;
            for(var face:cube.polygons)for(var vertex:face.vertices())headTop[0]=Math.max(headTop[0],animal.getY()+matrix.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y);
        });
        var waterPos=BlockPos.containing(x,surface-.2,z);double waterY=waterPos.getY()+client.level.getFluidState(waterPos).getHeight(client.level,waterPos);
        require(Double.isFinite(nose[0])&&Double.isFinite(headTop[0])&&waterY>surface-.5,"finite mesh and surface measurements");
        if(state.swimming&&state.activity==2)require(nose[0]>waterY,"floating rest nose above measured water surface");
        String line="name="+name+" baby="+state.isBaby+" activity="+state.activity+" feetY="+animal.getY()+" vertical="+animal.getDeltaMovement().y+" waterY="+waterY+" noseAboveWater="+(nose[0]-waterY)+" headTopAboveWater="+(headTop[0]-waterY);
        Blockbranch.LOGGER.info("BEAVER_WATER_KEYFRAME {}",line);
        Files.writeString(client.gameDirectory.toPath().resolve("beaver-water-keyframes.txt"),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    private BeaverWaterArtReview(){}
}
