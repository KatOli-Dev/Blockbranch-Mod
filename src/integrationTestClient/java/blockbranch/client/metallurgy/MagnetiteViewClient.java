package blockbranch.client.metallurgy;

import blockbranch.metallurgy.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Unedited native captures using a real connection, then the same view with vanilla host overrides. */
public final class MagnetiteViewClient {
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
            if(client.level==null||client.player==null||!client.player.getInventory().getItem(0).is(net.minecraft.world.item.Items.IRON_PICKAXE))return;
            client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            if(client.player.position().distanceToSqr(0,67.25,-10.5)>.04||Math.abs(client.player.getYRot())>1||Math.abs(client.player.getXRot()-12)>1){ticks[0]=0;return;}
            if(!client.level.getBlockState(new net.minecraft.core.BlockPos(-3,66,1)).is(MagnetiteContent.ORE)){ticks[0]=0;return;}
            client.player.getInventory().setSelectedSlot(0);
            net.minecraft.world.level.ItemLike[] expected={net.minecraft.world.item.Items.IRON_PICKAXE,MagnetiteContent.ORE,MagnetiteContent.DEEPSLATE_ORE,MagnetiteContent.FRAGMENT,MagnetiteContent.BOOTS,net.minecraft.world.item.Items.IRON_INGOT};for(int i=0;i<expected.length;i++)if(!client.player.getInventory().getItem(i).is(expected[i].asItem()))throw new AssertionError("native magnetite item sync");
            if(view[0]>=2)client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            if(client.gui.hud.isHidden()!=(view[0]==0))client.gui.hud.toggle();
            if(++ticks[0]<100)return;
            var speed=net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED;
            if(!client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(MagnetiteContent.BOOTS)||Math.abs(client.player.getAttributeValue(speed)-client.player.getAttribute(speed).getBaseValue())>1e-6||client.player.getArmorValue()!=2)throw new AssertionError("native worn boot attribute/equipment sync");
            if(client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).get(net.minecraft.core.component.DataComponents.TRIM)==null)throw new AssertionError("native boot trim sync");
            if(client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("expected real TCP");
            if(pack){
                var host=client.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/stone.png")).orElseThrow();
                if(!host.sourcePackId().contains("tin-host-test"))throw new AssertionError("host override pack not selected: "+host.sourcePackId());
            }
            saving[0]=true;String name=(pack?"retexture":"base")+(view[0]==0?"-blocks.png":view[0]==1?"-items.png":"-armor.png");
            net.minecraft.client.Screenshot.grab(client.gameDirectory,name,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                try{
                    if(!Files.exists(directory.resolve("screenshots/"+name)))throw new AssertionError("native capture missing");
                    if(++view[0]>=3){Files.writeString(directory.resolve("result.txt"),"PASS real TCP registered items, live attraction/native pickup and worn armor/trim sync and captures, hostPack="+pack+"\n");done[0]=true;client.stop();}
                    ticks[0]=0;saving[0]=false;
                }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}
            }));
        }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}});
    }
    private MagnetiteViewClient(){}
}
