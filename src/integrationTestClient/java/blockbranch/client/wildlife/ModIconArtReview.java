package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;

/** Actual native pack-icon rendering of unchanged mod-icon bytes; staged UI, not Mod Menu. */
final class ModIconArtReview {
    private static int stage,wait;
    private static boolean captured=true;
    private static PackSelectionScreen screen;
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(stage==0){
            require(!Boolean.getBoolean("blockbranch.itemReview")&&!Boolean.getBoolean("blockbranch.foodArtReview"),"mod icon review runs alone");
            var pack=client.getResourcePackDirectory().resolve("Blockbranch emblem preview");Files.createDirectories(pack);
            byte[] bytes;try(var stream=client.getResourceManager().open(Blockbranch.id("icon.png"))){bytes=stream.readAllBytes();}
            Files.write(pack.resolve("pack.png"),bytes);
            var format=SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
            var section=new PackMetadataSection(Component.literal("Terrain, wildlife and branching worlds"),new InclusiveRange<>(format,format));
            var metadata=new JsonObject();metadata.add("pack",PackMetadataSection.CLIENT_TYPE.codec().encodeStart(JsonOps.INSTANCE,section).getOrThrow());
            Files.writeString(pack.resolve("pack.mcmeta"),metadata.toString());
            var repository=client.getResourcePackRepository();repository.reload();
            require(repository.getAvailablePacks().stream().anyMatch(p->p.getId().contains("Blockbranch emblem preview")),"preview pack admitted by native pack repository");
            require(Arrays.equals(bytes,Files.readAllBytes(pack.resolve("pack.png"))),"preview uses exact mod metadata icon bytes");
            screen=new PackSelectionScreen(repository,p->{},client.getResourcePackDirectory(),Component.literal("Resource Packs"));
            client.options.guiScale().set(2);client.resizeGui();client.gui.setScreen(screen);
            for(String axis:new String[]{"xpos","ypos"}){var field=net.minecraft.client.MouseHandler.class.getDeclaredField(axis);field.setAccessible(true);field.setDouble(client.mouseHandler,0);}
            stage=1;return false;
        }
        if(!captured)return false;
        if(stage==3){client.gui.setScreen(null);Files.writeString(client.gameDirectory.toPath().resolve("mod-icon-review.txt"),"PASS exact mod-icon bytes rendered in staged native resource-pack list at GUI scales 2 and 1; not Mod Menu coverage\n");return true;}
        // Wait for screenshot readback before changing GUI dimensions/scissor state.
        if(stage==2&&wait==0){client.options.guiScale().set(1);client.resizeGui();}
        require(client.gui.screen()==screen,"native pack selector remains open");
        if(++wait<60)return false;
        var field=PackSelectionScreen.class.getDeclaredField("packIcons");field.setAccessible(true);
        require(((Map<?,?>)field.get(screen)).keySet().stream().anyMatch(id->id.toString().contains("Blockbranch emblem preview")),"native preview icon texture cached");
        int scale=stage==1?2:1;String file="blockbranch-icon-ui-scale-"+scale+".png";
        captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));
        Blockbranch.LOGGER.info("MOD_ICON_NATIVE_CAPTURE guiScale={} exactBytes=true",scale);
        stage++;wait=0;
        return false;
    }
    private ModIconArtReview(){}
}
