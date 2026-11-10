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

/** Airborne clock-window poses, never a natural flight or continuous-motion qualification. */
final class MallardFlightArtReview {
    private record View(String name,int actor,int clock,boolean front,boolean flight){}
    private static final View[] VIEWS={
        new View("drake-upper-front",0,1,true,true),new View("drake-lower-front",0,5,true,true),
        new View("drake-upper-quarter",0,1,false,true),new View("drake-lower-quarter",0,5,false,true),
        new View("hen-upper-front",1,1,true,true),new View("hen-lower-front",1,5,true,true),
        new View("hen-upper-quarter",1,1,false,true),new View("hen-lower-quarter",1,5,false,true),
        new View("drake-rear-quarter",0,1,false,true),new View("hen-rear-quarter",1,1,false,true),
        new View("drake-land-control",0,0,false,false),new View("hen-land-control",1,0,false,false)
    };
    private static final List<UUID> actors=new ArrayList<>();
    private static boolean started,captured=true;
    private static volatile boolean ready;
    private static int view=-1,wait;
    private static double x,y,z;
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    static boolean tick(Minecraft client)throws Exception{
        if(!started){
            started=true;require(!Boolean.getBoolean("blockbranch.itemReview")&&!Boolean.getBoolean("blockbranch.foodArtReview"),"flight review runs alone");
            MallardPostureAudit.verify();MallardFlightAudit.verify();WildlifeModelGroundAudit.verify();WildlifeFaceAudit.verify();
            client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                var origin=player.blockPosition().offset(24,1,0);x=origin.getX()+.5;y=origin.getY()+1;z=origin.getZ()+.5;
                player.setGameMode(GameType.SPECTATOR);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"weather clear");
                for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=6;dz++)for(int dy=0;dy<=5;dy++)level.setBlock(origin.offset(dx,dy,dz),(dy==0?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
                for(int i=0;i<3;i++){
                    var type=i==2?net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("chicken")):WildlifeRuntime.MALLARD;
                    var animal=(Mob)type.create(level,EntitySpawnReason.COMMAND);require(animal!=null,"bird exists");
                    animal.setNoAi(true);animal.setNoGravity(i!=2);animal.setPermanentlyInvulnerable(true);animal.setPersistenceRequired();
                    if(animal instanceof Mallard mallard)mallard.setDrake(i==0);
                    animal.snapTo(x+(i-1)*1.4,y,z,0,0);require(level.addFreshEntity(animal),"bird added");actors.add(animal.getUUID());
                }
                player.connection.teleport(x,y-.8,z+3.5,180,8);ready=true;
            });return false;
        }
        if(!ready||!captured)return false;
        if(view>=VIEWS.length){Files.writeString(client.gameDirectory.toPath().resolve("mallard-flight-review.txt"),"PASS native clock-window wing poses and land controls; staged airborne positions, not natural flight or continuous animation\n");return true;}
        var current=view<0?null:VIEWS[view];
        client.getSingleplayerServer().execute(()->{
            var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){
                var animal=level.getEntity(actors.get(i));require(animal!=null&&animal.isAlive(),"bird alive");
                boolean selected=current!=null&&i==current.actor;
                animal.snapTo(current==null||selected?x+(i-1)*1.4:x+16+i*3,y+(selected&&current.flight?1.4:0),z,0,0);animal.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if(animal instanceof Mallard mallard)try{var field=Mallard.class.getDeclaredField("ACTIVITY");field.setAccessible(true);mallard.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),selected&&current.flight?3:0);}catch(Exception e){throw new RuntimeException(e);}
            }
            for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BlockPos.containing(x,y,z)).inflate(10)))drop.discard();
        });
        if(current!=null)for(var entity:client.level.entitiesForRendering())if(entity.getUUID().equals(actors.get(current.actor))){
            entity.tickCount=current.clock;entity.setXRot(0);entity.xRotO=0;
            if(entity instanceof net.minecraft.world.entity.LivingEntity living){living.setYBodyRot(0);living.yBodyRotO=0;living.setYHeadRot(0);living.yHeadRotO=0;}
        }
        if(++wait<70)return false;
        if(current!=null){
            Mallard subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof Mallard bird&&bird.getUUID().equals(actors.get(current.actor)))subject=bird;
            require(subject!=null&&subject.isAlive()&&!subject.isInWater(),"native flight bird alive and dry");
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
            require(frustum!=null&&frustum.isVisible(subject.getBoundingBox()),"bird in frustum");
            var renderer=(MallardRenderer)client.getEntityRenderDispatcher().getRenderer(subject);var state=renderer.createRenderState();renderer.extractRenderState(subject,state,.5F);
            require(state.activity==(current.flight?3:0),"native flight activity synchronized");
            var root=MallardModel.layer().bakeRoot();new MallardModel(root).setupAnim(state);
            var bounds=MallardFlightAudit.measure(root);
            Blockbranch.LOGGER.info("MALLARD_FLIGHT_KEYFRAME name={} clockWindow={}..{} auditAge={} wingAngle={} bodyBackModelY={} outerTipMinY={} outerTipMaxY={}",current.name,current.clock,current.clock+1,state.ageInTicks,root.getChild("right_wing").zRot,bounds.back(),bounds.min(),bounds.max());
        }
        String file="mallard-flight-"+(current==null?"family":current.name)+".png";
        captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));
        view++;wait=0;
        if(view<VIEWS.length){ready=false;var next=VIEWS[view];client.getSingleplayerServer().execute(()->{
            double ax=x+(next.actor-1)*1.4,foot=y+(next.flight?1.4:0),distance=1.7;
            boolean rear=next.name.endsWith("rear-quarter");
            client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().connection.teleport(ax+(next.front?0:distance*.65),foot-1.35,z+distance*(next.front?1:rear?-.8:.8),next.front?180:rear?40:140,0);ready=true;
        });}
        return false;
    }
    private MallardFlightArtReview(){}
}
