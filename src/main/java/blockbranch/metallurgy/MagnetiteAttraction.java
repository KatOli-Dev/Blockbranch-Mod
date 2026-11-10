package blockbranch.metallurgy;
import blockbranch.mixin.metallurgy.ItemPickupTargetAccessor;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;

/** Bounded server-side steering; native physics, ownership and pickup still own item collection. */
public final class MagnetiteAttraction {
    public static final double RANGE=3,SPEED=.2;
    public static final int PERIOD=4,MAX_ITEMS=64;
    public static void register(){ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%PERIOD==0)pulse(server.getPlayerList().getPlayers());});}
    private static boolean wearing(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&p.getItemBySlot(EquipmentSlot.FEET).is(MagnetiteContent.BOOTS);}
    private static Vec3 target(ServerPlayer p){return p.position().add(0,.35,0);}
    public static void pulse(List<ServerPlayer> players){
        ArrayList<ServerPlayer> wearers=null;for(var p:players)if(wearing(p)){if(wearers==null)wearers=new ArrayList<>();wearers.add(p);}if(wearers==null)return;
        final var active=wearers;
        for(var p:active){
            var items=new ArrayList<ItemEntity>();
            // Native limited query bounds retained candidates; nearby items await ordinary pickup.
            p.level().getEntities(EntityTypeTest.forClass(ItemEntity.class),p.getBoundingBox().inflate(RANGE),item->nearest(item,active)==p,items,MAX_ITEMS);
            for(var item:items){
                var start=item.getBoundingBox().getCenter();var end=target(p);
                if(p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;
                item.setDeltaMovement(end.subtract(start).normalize().scale(SPEED));item.syncVelocity=true;
            }
        }
    }
    private static ServerPlayer nearest(ItemEntity item,List<ServerPlayer> players){
        if(item.isRemoved()||item.getItem().isEmpty()||((ItemPickupTargetAccessor)item).blockbranch$pickupDelay()!=0)return null;
        var owner=((ItemPickupTargetAccessor)item).blockbranch$pickupTarget();var point=item.getBoundingBox().getCenter();ServerPlayer best=null;double distance=RANGE*RANGE;
        for(var p:players){if(p.level()!=item.level()||(owner!=null&&!owner.equals(p.getUUID())))continue;double next=point.distanceToSqr(target(p));if(next<=.75*.75)return null;
            if(next<distance||(next==distance&&best!=null&&p.getUUID().compareTo(best.getUUID())<0)){best=p;distance=next;}
        }
        return best;
    }
    private MagnetiteAttraction(){}
}
