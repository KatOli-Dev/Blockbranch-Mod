package blockbranch.client.realistic;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Mob;
/** Native close-up cameras inside the existing loaded review region. */
public final class WildlifeCloseups {
    private final java.util.List<Mob> subjects;private final String species;
    private final net.minecraft.world.phys.Vec3 original;private final float yaw,pitch;private final int fov;
    private final FixtureSceneReadiness readiness=new FixtureSceneReadiness();
    private int index,settled,restoreTicks;private boolean positioned,saving,restored;
    public WildlifeCloseups(Minecraft client,Class<? extends Mob> type,String name){
        subjects=new ArrayList<>();for(var entity:client.level.entitiesForRendering())if(type.isInstance(entity)&&entity instanceof Mob mob&&mob.isNoAi()&&mob.distanceToSqr(client.player)<400)subjects.add(mob);
        subjects.sort(Comparator.comparingDouble(Mob::getX));species=name;original=client.player.position();yaw=client.player.getYRot();pitch=client.player.getXRot();fov=client.options.fov().get();
    }
    private static void move(double x,double y,double z,float yaw,float pitch){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new blockbranch.wildlife.WildlifeProbe(30,x+" "+y+" "+z+" "+yaw+" "+pitch));}
    public boolean tick(Minecraft client){
        if(saving)return false;
        if(index==subjects.size()){
            if(!restored){client.options.fov().set(fov);move(original.x,original.y,original.z,yaw,pitch);restored=true;}
            return ++restoreTicks>=40;
        }
        var actor=subjects.get(index);
        if(!positioned){double d=actor.getBbHeight()*2.5+.25;client.options.fov().set(40);move(actor.getX()-d*.707,actor.getY()+actor.getBbHeight()*.65-client.player.getEyeHeight(),actor.getZ()+d*.707,-135,8);positioned=true;settled=0;}
        if(!readiness.ready(client,species+"-closeup-"+index)){settled=0;return false;}if(++settled<40)return false;
        saving=true;String file=species+"-closeup-"+(index+1)+".png";
        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{index++;positioned=false;saving=false;}));return false;
    }
}
