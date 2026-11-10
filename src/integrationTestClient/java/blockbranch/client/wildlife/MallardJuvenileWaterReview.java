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

/** Fixed water-depth appearance views; neither native buoyancy nor safety evidence. */
final class MallardJuvenileWaterReview {
    private record View(String name,int actor,double depth,int activity,boolean underwater,boolean front){}
    private static final View[] VIEWS={
        new View("adult-surface-control",0,.15,0,false,false),
        new View("juvenile-surface-side",1,.15,0,false,false),
        new View("juvenile-surface-front",1,.15,0,false,true),
        new View("juvenile-deep-side",1,.45,0,true,false),
        new View("juvenile-deep-front",1,.45,0,true,true),
        new View("juvenile-deep-rest",1,.45,2,true,false)
    };
    private static final List<UUID> actors=new ArrayList<>();
    private static boolean started,captured=true;
    private static volatile boolean ready;
    private static int view,wait;
    private static double x,surface,z;
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(!started){
            started=true;
            require(!Boolean.getBoolean("blockbranch.itemReview")&&!Boolean.getBoolean("blockbranch.foodArtReview")&&!Boolean.getBoolean("blockbranch.perchArtReview"),"juvenile water review runs alone");
            client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                var origin=player.blockPosition().offset(24,0,0);x=origin.getX()+.5;z=origin.getZ()+.5;
                player.setGameMode(GameType.SPECTATOR);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"weather clear");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"effect give @a minecraft:night_vision 600 0 true");
                for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=5;dz++)for(int dy=-3;dy<=4;dy++){
                    var block=dy>0?Blocks.AIR:dy==-3||Math.abs(dx)==6||Math.abs(dz)==5?Blocks.SMOOTH_STONE:Blocks.WATER;
                    level.setBlock(origin.offset(dx,dy,dz),block.defaultBlockState(),3);
                }
                surface=origin.getY()+level.getFluidState(origin).getHeight(level,origin);
                for(int i=0;i<2;i++){
                    var duck=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);require(duck!=null,"water mallard exists");
                    duck.setNoAi(true);duck.setNoGravity(true);duck.setPermanentlyInvulnerable(true);duck.setPersistenceRequired();duck.setDrake(i==0);
                    if(i==1)duck.setAge(-24000);
                    duck.snapTo(x+(i==0?-2:2),surface-.15,z,0,0);require(level.addFreshEntity(duck),"water actor added");actors.add(duck.getUUID());
                }
                camera(player,VIEWS[0]);ready=true;
            });return false;
        }
        if(!ready||!captured)return false;
        if(view>=VIEWS.length){Files.writeString(client.gameDirectory.toPath().resolve("mallard-juvenile-water.txt"),"PASS six fixed-depth native appearance views; disabled AI/gravity; not buoyancy, unconstrained behavior or safety qualification\n");return true;}
        var current=VIEWS[view];
        client.getSingleplayerServer().execute(()->{
            var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){
                var duck=level.getEntity(actors.get(i));require(duck!=null&&duck.isAlive(),"water actor survives");
                duck.snapTo(i==current.actor?x+(i==0?-2:2):x+20+i*3,surface-current.depth,z,0,0);duck.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                try{var field=Mallard.class.getDeclaredField("ACTIVITY");field.setAccessible(true);duck.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),current.activity);}catch(Exception e){throw new RuntimeException(e);}
            }
            for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BlockPos.containing(x,surface,z)).inflate(12)))drop.discard();
        });
        if(++wait<80)return false;
        Mallard subject=null;for(var entity:client.level.entitiesForRendering())if(entity instanceof Mallard duck&&duck.getUUID().equals(actors.get(current.actor)))subject=duck;
        require(subject!=null&&subject.isAlive()&&subject.isInWater(),"water subject alive and submerged");
        var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
        require(frustum!=null&&frustum.isVisible(subject.getBoundingBox()),"water subject visible");
        require(Math.abs(subject.getY()-(surface-current.depth))<.01,"fixed appearance depth");
        Blockbranch.LOGGER.info("MALLARD_JUVENILE_WATER name={} baby={} depth={} activity={} cameraUnderwater={} nightVision=true",current.name,subject.isBaby(),surface-subject.getY(),subject.activity(),current.underwater);
        String file="mallard-water-"+current.name+".png";captured=false;
        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));
        view++;wait=0;
        if(view<VIEWS.length){ready=false;client.getSingleplayerServer().execute(()->{camera(client.getSingleplayerServer().getPlayerList().getPlayers().getFirst(),VIEWS[view]);ready=true;});}
        return false;
    }
    private static void camera(net.minecraft.server.level.ServerPlayer player,View v){
        double ax=x+(v.actor==0?-2:2),distance=v.actor==0?1.9:1.2;
        double eye=v.underwater?surface-v.depth+.15:surface+(v.actor==0?.23:.12);
        player.connection.teleport(ax+(v.front?0:distance),eye-1.62,z+distance*(v.front?1:.35),v.front?180:109.29F,v.underwater?-2:6);
    }
    private MallardJuvenileWaterReview(){}
}
