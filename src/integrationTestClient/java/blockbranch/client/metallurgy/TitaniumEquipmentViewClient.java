package blockbranch.client.metallurgy;

import blockbranch.metallurgy.TitaniumEquipment;
import blockbranch.metallurgy.TitaniumContent;

import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Unedited native captures using a real connection, then the same view with vanilla host overrides. */
public final class TitaniumEquipmentViewClient {
    public static void register(){
        var directory=Path.of(System.getProperty("blockbranch.metallurgyViewDirectory"));boolean pack=Boolean.getBoolean("blockbranch.metallurgyViewPack");
        long deadline=System.nanoTime()+150_000_000_000L;int[] ticks={0},view={0};boolean[] saving={false},done={false};
        net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents.DISCONNECT.register((handler,client)->{
            if(done[0])return;done[0]=true;try{Files.writeString(directory.resolve("result.txt"),"FAIL login disconnected before world readiness; inspect server log");}catch(Exception ignored){}client.stop();
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            if(done[0])return;done[0]=true;try{Files.writeString(directory.resolve("result.txt"),"FAIL play disconnected: "+handler.getConnection().getDisconnectionDetails());}catch(Exception ignored){}client.stop();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(done[0]||saving[0])return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("native metallurgy capture timeout");
            if(client.level==null||client.player==null||!client.player.getInventory().getItem(0).is(TitaniumEquipment.TITANIUM_ALLOY_PICKAXE))return;
            client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            if(client.player.position().distanceToSqr(0,67.25,-8.5)>.04||Math.abs(client.player.getYRot())>1||Math.abs(client.player.getXRot()-12)>1){ticks[0]=0;return;}
            if(!client.level.getBlockState(new net.minecraft.core.BlockPos(-3,66,1)).is(TitaniumContent.ORE)){ticks[0]=0;return;}
            if(view[0]==2){
                net.minecraft.world.item.Item[] tools={TitaniumEquipment.TITANIUM_ALLOY_SWORD,TitaniumEquipment.TITANIUM_ALLOY_AXE,TitaniumEquipment.TITANIUM_ALLOY_SHOVEL,TitaniumEquipment.TITANIUM_ALLOY_HOE};
                for(int i=0;i<4;i++){var stack=client.player.getInventory().getItem(9+i);if(!stack.is(tools[i]))throw new AssertionError("new tool inventory sync");client.player.getInventory().setItem(i+1,stack.copy());}
                for(int i=5;i<=6;i++)client.player.getInventory().setItem(i,net.minecraft.world.item.ItemStack.EMPTY);
            }
            if(view[0]>=3){
                var armor=java.util.List.of(TitaniumEquipment.TITANIUM_ALLOY_HELMET,TitaniumEquipment.TITANIUM_ALLOY_CHESTPLATE,TitaniumEquipment.TITANIUM_ALLOY_LEGGINGS,TitaniumContent.BOOTS);
                var slots=java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET);
                for(int i=0;i<4;i++){var stack=client.player.getInventory().getItem(13+i);if(!stack.is(armor.get(i))||!client.player.getItemBySlot(slots.get(i)).is(armor.get(i)))throw new AssertionError("armor inventory/equipment sync");client.player.getInventory().setItem(i+1,stack.copy());}
                if(client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).get(net.minecraft.core.component.DataComponents.TRIM)==null)throw new AssertionError("native trim component sync");
                client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            }
            client.player.getInventory().setSelectedSlot(8);
            if(client.gui.hud.isHidden()!=(view[0]==0))client.gui.hud.toggle();
            if(++ticks[0]<100)return;
            var speed=net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
            if(client.player.getArmorValue()!=15||Math.abs(client.player.getAttributeValue(speed)-client.player.getAttribute(speed).getBaseValue()*1.05)>1e-6)throw new AssertionError("native full set must retain a single published boot movement bonus");
            if(client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("expected real TCP");
            if(pack){
                var host=client.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/stone.png")).orElseThrow();
                if(!host.sourcePackId().contains("tin-host-test"))throw new AssertionError("host override pack not selected: "+host.sourcePackId());
            }
            saving[0]=true;String name=(pack?"retexture":"base")+(view[0]==0?"-blocks.png":view[0]==1?"-items.png":view[0]==2?"-tools.png":"-armor.png");
            net.minecraft.client.Screenshot.grab(client.gameDirectory,name,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                try{
                    if(!Files.exists(directory.resolve("screenshots/"+name)))throw new AssertionError("native capture missing");
                    if(++view[0]>=4){Files.writeString(directory.resolve("result.txt"),"PASS real TCP registered items, native captures, hostPack="+pack+"\n");done[0]=true;client.stop();}
                    ticks[0]=0;saving[0]=false;
                }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}
            }));
        }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}});
    }
    private TitaniumEquipmentViewClient(){}
}
