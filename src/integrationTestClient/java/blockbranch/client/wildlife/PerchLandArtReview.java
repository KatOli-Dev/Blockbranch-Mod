package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Staged fish contact and water controls, not natural stranding or flop timing. */
final class PerchLandArtReview {
    private record View(String name,int actor,int clock,int angle){}
    private static final View[] VIEWS={new View("land-side",0,0,0),new View("land-quarter",0,3,1),new View("land-top",0,7,2),new View("land-front",0,3,3),new View("land-contact",0,0,4),new View("land-tail",0,5,5),new View("cod-side",1,0,0),new View("water-side",2,0,0),new View("water-quarter",2,3,1)};
    private static final List<UUID> actors=new ArrayList<>();
    private static final List<String> measurements=new ArrayList<>();
    private static boolean started,captured=true,audited;
    private static volatile boolean ready;
    private static int view=-1,wait;
    private static double x,y,z;
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(!started){
            started=true;require(!Boolean.getBoolean("blockbranch.itemReview")&&!Boolean.getBoolean("blockbranch.foodArtReview")&&!Boolean.getBoolean("blockbranch.perchArtReview"),"perch land review runs alone");
            client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                var origin=player.blockPosition().offset(24,1,0);x=origin.getX()+.5;y=origin.getY()+1;z=origin.getZ()+.5;
                player.setGameMode(GameType.SPECTATOR);
                for(String cmd:List.of("time set 6000","weather clear","effect give @a minecraft:night_vision 600 0 true"))server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),cmd);
                for(int dx=-5;dx<=7;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=0;dy<=4;dy++)level.setBlock(origin.offset(dx,dy,dz),(dy==0?Blocks.SMOOTH_STONE:dx>=3&&dx<=6&&Math.abs(dz)<=2&&dy<=3?(dx==3||dx==6||Math.abs(dz)==2?Blocks.GLASS:Blocks.WATER):Blocks.AIR).defaultBlockState(),3);
                for(int i=0;i<3;i++){
                    var type=i==1?net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("cod")):WildlifeRuntime.EUROPEAN_PERCH;
                    var fish=(Mob)type.create(level,EntitySpawnReason.COMMAND);require(fish!=null,"fish created");fish.setNoAi(true);fish.setNoGravity(true);fish.setPermanentlyInvulnerable(true);fish.setPersistenceRequired();
                    fish.snapTo(x+(i==2?4:i==0?-.4:.4),y+(i==2?1:0),z,0,0);require(level.addFreshEntity(fish),"fish added");actors.add(fish.getUUID());
                }
                player.connection.teleport(x+1.4,y-1.4,z+1.4,135,8);ready=true;
            });return false;
        }
        if(!ready||!captured)return false;
        if(view>=VIEWS.length){Files.writeString(client.gameDirectory.toPath().resolve("perch-land-review.txt"),"PASS staged dry contact and submerged controls; AI/gravity disabled, clock windows, not natural behavior\n"+String.join("\n",measurements));return true;}
        var current=view<0?null:VIEWS[view];
        client.getSingleplayerServer().execute(()->{
            var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){
                var fish=level.getEntity(actors.get(i));require(fish!=null&&fish.isAlive(),"review fish alive");
                fish.snapTo(x+(i==2?4:current==null||i==current.actor?i==0?-.4:.4:16+i*2),y+(i==2?1:0),z,0,0);fish.setDeltaMovement(Vec3.ZERO);fish.setOnGround(false);
            }
        });
        if(current!=null)for(var entity:client.level.entitiesForRendering())if(entity.getUUID().equals(actors.get(current.actor))){entity.tickCount=current.clock;entity.setXRot(0);entity.xRotO=0;if(entity instanceof net.minecraft.world.entity.LivingEntity fish){fish.setYBodyRot(0);fish.yBodyRotO=0;fish.setYHeadRot(0);fish.yHeadRotO=0;}}
        if(++wait<70)return false;
        var section=client.levelRenderer.viewArea().getRenderSectionAt(BlockPos.containing(x,y-1,z));require(section!=null&&section.getSectionMesh().hasRenderableLayers(),"dry floor mesh ready");
        if(current!=null){
            Mob subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof Mob fish&&fish.getUUID().equals(actors.get(current.actor)))subject=fish;
            require(subject!=null&&subject.isAlive()&&subject.isInWater()==(current.actor==2),"native dry/submerged state verified");
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;require(frustum!=null&&frustum.isVisible(subject.getBoundingBox()),"selected fish visible");
            if(subject instanceof EuropeanPerch perch){
                var renderer=(PerchRenderer)client.getEntityRenderDispatcher().getRenderer(perch);var state=renderer.createRenderState();renderer.extractRenderState(perch,state,.5F);
                if(!audited){PerchLandAudit.verify(renderer);WildlifeFaceAudit.verify();audited=true;}
                require(client.getResourceManager().getResource(renderer.getTextureLocation(state)).isPresent(),"selected perch coat resource exists");
                float[] b=bodyBounds(renderer,state);String line=current.name+" water="+state.isInWater+" bodyMinY="+b[0]+" bodyMaxY="+b[1];measurements.add(line);Blockbranch.LOGGER.info("PERCH_LAND_KEYFRAME {}",line);
                if(state.isInWater)PerchScaleAudit.measure(client,perch);
            }
        }
        String file="perch-contact-"+(current==null?"context":current.name)+".png";captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));
        view++;wait=0;if(view<VIEWS.length){ready=false;var next=VIEWS[view];client.getSingleplayerServer().execute(()->{
            double ax=x+(next.actor==2?4:next.actor==0?-.4:.4),foot=y+(next.actor==2?1:0),distance=.7;
            double dx=next.angle==0||next.angle==4?distance:next.angle==3?0:next.angle==2?0:distance*.65;
            double dz=next.angle==0||next.angle==4?0:next.angle==3?distance:next.angle==2?distance*.25:distance*(next.angle==5?-.8:.8);
            double eye=next.actor==2?.16:next.angle==2?1.1:next.angle==4?.09:.23;
            float yaw=next.angle==0||next.angle==4?90:next.angle==3||next.angle==2?180:next.angle==5?40:140;
            float pitch=next.actor==2?0:next.angle==2?80:next.angle==4?4:12;
            client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(ax+dx,foot+eye-1.62,z+dz,yaw,pitch);ready=true;
        });}return false;
    }
    static float[] bodyBounds(PerchRenderer renderer,LivingEntityRenderState state){
        var root=PerchModel.layer().bakeRoot();new PerchModel(root).setupAnim(state);
        var pose=new PoseStack();pose.scale(state.scale,state.scale,state.scale);renderer.setupRotations(state,pose,0,state.scale);pose.scale(-1,-1,1);renderer.scale(state,pose);pose.translate(0,-1.501F,0);
        float[] b={Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY};root.visit(pose,(matrix,path,index,cube)->{if(!path.equals("/body"))return;for(var face:cube.polygons)for(var vertex:face.vertices()){
            float py=matrix.pose().transformPosition(new Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ())).y;b[0]=Math.min(b[0],py);b[1]=Math.max(b[1],py);
        }});return b;
    }
    private PerchLandArtReview(){}
}
