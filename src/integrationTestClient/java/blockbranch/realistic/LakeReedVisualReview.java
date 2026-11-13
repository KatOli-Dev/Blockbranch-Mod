package blockbranch.realistic;
import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
/** Native views of the audited seed-424242 terminal-lake reedbed at fixed poses: views 0 and 1 repeat the audit's views 2
 * (elevated) and 3 (underwater, night vision) exactly; views 2 and 3 look across the reeds from just above the water. Poses
 * are fixed, never derived from plant positions; no blocks are staged. Software rendering is evidence, not acceptance. */
public final class LakeReedVisualReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.lakeReedVisualDirectory","build/lake-reed-evidence/disabled-visual"));
    private static final long BEGAN=System.nanoTime();
    /** Site of the audited swamp; views 0 and 1 are the audit's views 2 (elevated) and 3 (underwater) exactly; view 4 is a close-up of one stacked reed column found by scanning the generated blocks. */
    private static final int SITE_X=464000,SITE_Z=1008000;
    /** camera x,y,z, yaw, pitch, night vision. */
    private static final double[][] POSES={
        {463976.5,339.2,1007970.5,-38.659808254090095,45.6642218126351,0},
        {463984.5,301.2,1007984.5,-45.0,-2.4293971074759013,1},
        {463990.5,304.7,1007990.5,-45.0,0.0,0},
        {463968.5,304.7,1008000.5,-90.0,0.0,0}};
    private static final int VIEWS=5;
    private static volatile double[] closeUp;
    private static double[] pose(int view){return view<POSES.length?POSES[view]:closeUp;}
    /** First column (scan order) with three stacked reed blocks rooted at y=301 on the mud; with no reed in a three-wide lane of seven blocks to its west, so the view is not blocked; the camera stands on the bed five blocks west, looking east at its middle (underwater, night vision like the audit's view 3). */
    private static double[] stack(net.minecraft.server.level.ServerLevel level){
        for(int z=SITE_Z-40;z<=SITE_Z+40;z++)for(int x=SITE_X-40;x<=SITE_X+40;x++){
            var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)continue;
            for(int y=301;y<=301;y++){
                boolean stacked=true;for(int k=0;k<3;k++)stacked&=chunk.getBlockState(new BlockPos(x,y+k,z)).is(ReedbedContent.LAKE_REED);
                if(!stacked)continue;
                boolean open=true;for(int k=1;k<=7&&open;k++)for(int dz=-1;dz<=1&&open;dz++){var c=level.getChunkSource().getChunkNow((x-k)>>4,(z+dz)>>4);if(c==null){open=false;break;}for(int dy=0;dy<=3;dy++)if(c.getBlockState(new BlockPos(x-k,y+dy,z+dz)).is(ReedbedContent.LAKE_REED))open=false;}
                if(!open)continue;
                double eye=301.0+1.62,dy=y+1.5-eye;
                return new double[]{x+.5-5,301.0,z+.5,-90.0,Math.toDegrees(Math.atan2(-dy,5.0)),1};
            }
        }
        throw new AssertionError("No three-block reed stack rooted at y=301 in the scene window");
    }
    private static boolean opened,configured,saving;
    private static volatile boolean finished,posed;
    private static volatile int view;
    private static volatile Throwable failure;
    private static int settled;
    private static final List<Object> records=new ArrayList<>();
    private static final BoundingBox SUBJECT=new BoundingBox(SITE_X-40,280,SITE_Z-40,SITE_X+40,345,SITE_Z+40);

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server-> {
            if(finished||server.getPlayerList().getPlayers().isEmpty())return;
            try {
                var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                if(posed){player.getAbilities().flying=true;player.onUpdateAbilities();player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return;}
                var g=(RealisticGenerator)level.getChunkSource().getGenerator();
                if(view==VIEWS){
                    int reeds=0;for(int z=SITE_Z-40;z<=SITE_Z+40;z++)for(int x=SITE_X-40;x<=SITE_X+40;x++){var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null)continue;for(int y=295;y<=312;y++)if(chunk.getBlockState(new BlockPos(x,y,z)).is(ReedbedContent.LAKE_REED))reeds++;}
                    String biome=level.getBiome(new BlockPos(SITE_X,301,SITE_Z)).unwrapKey().orElseThrow().identifier().toString();
                    if(reeds==0||!biome.equals("blockbranch:inland_lake_reedbed"))throw new AssertionError("Wrong scene: "+reeds+" reed blocks, biome "+biome);
                    Files.writeString(DIR.resolve("result.txt"),"PASS native lake-reed views; "+reeds+" reed blocks in the 81 x 81 m window y295..312; target biome "+biome+"; no staged terrain");finished=true;return;}
                var commands=server.getCommands();var source=server.createCommandSourceStack();
                commands.performPrefixedCommand(source,"time set noon");commands.performPrefixedCommand(source,"weather clear");
                if(view==0){
                    for(int cz=(SITE_Z-64)>>4;cz<=(SITE_Z+64)>>4;cz++)for(int cx=(SITE_X-64)>>4;cx<=(SITE_X+64)>>4;cx++)level.setChunkForced(cx,cz,true);
                    Files.writeString(DIR.resolve("generator.json"),RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().toString());
                }
                if(view==4&&closeUp==null)closeUp=stack(level);
                var p=pose(view);
                if(p[5]>0)commands.performPrefixedCommand(source,"effect give "+player.getName().getString()+" minecraft:night_vision infinite 0 true");else commands.performPrefixedCommand(source,"effect clear "+player.getName().getString()+" minecraft:night_vision");
                commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+p[0]+" "+p[1]+" "+p[2]+" "+p[3]+" "+p[4]);
                records.add(Map.of("view",view,"camera",List.of(p[0],p[1],p[2]),"yaw",p[3],"pitch",p[4],"seed",level.getSeed(),"nightVision",p[5]>0,"biome",level.getBiome(new BlockPos(SITE_X,301,SITE_Z)).unwrapKey().orElseThrow().identifier().toString()));
                posed=true;
            }
            catch(Throwable t) {failure=t;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client-> {
            try {
                if(System.nanoTime()-BEGAN>900_000_000_000L)throw new AssertionError("Bounded lake-reed visual deadline");
                if(failure!=null)throw new AssertionError("Server witness",failure);
                if(saving)return;
                if(finished) {
                    saving=true;
                    Files.writeString(DIR.resolve("views.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(records));
                    client.disconnect(new TitleScreen(),false);client.stop();return;
                }
                var screen=client.gui.screen();
                if(!opened&&screen instanceof TitleScreen) {opened=true;CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));return;}
                if(!configured&&screen instanceof CreateWorldScreen create) {
                    var state=create.getUiState();state.setName("LakeReedVisualReview");state.setSeed("424242");
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);state.setAllowCommands(true);
                    String id="blockbranch:realistic";
                    state.setWorldType(state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().toString().equals(id)).findFirst().orElseThrow());
                    configured=true;press(create,"Create New World");return;
                }
                if(screen!=null&&screen.getClass().getSimpleName().equals("ConfirmExperimentalFeaturesScreen")) {press(screen,"Proceed");return;}
                if(!posed||client.level==null||client.player==null||view>=VIEWS)return;
                var p=pose(view);
                client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if(client.player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(p[0],p[1],p[2]))>.001||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-(float)p[3]))>.02||Math.abs(client.player.getXRot()-(float)p[4])>.02) {settled=0;return;}
                client.options.fov().set(65);client.gui.toastManager().clear();
                if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                var area=client.levelRenderer.viewArea();if(area==null)return;
                boolean complete=true;var cache=new net.minecraft.client.renderer.chunk.RenderRegionCache();int submitted=0;
                for(int z=SUBJECT.minZ()>>4;z<=SUBJECT.maxZ()>>4;z++)for(int x=SUBJECT.minX()>>4;x<=SUBJECT.maxX()>>4;x++)for(int y=SUBJECT.minY()>>4;y<=SUBJECT.maxY()>>4;y++) {
                    if(Math.abs(x-(client.player.blockPosition().getX()>>4))>area.getViewDistance()||Math.abs(z-(client.player.blockPosition().getZ()>>4))>area.getViewDistance())continue;
                    var chunk=client.level.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
                    if(chunk==null||((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain()==null) {complete=false;continue;}
                    if(!client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum.isVisible(new net.minecraft.world.phys.AABB(x*16,y*16,z*16,x*16+16,y*16+16,z*16+16)))continue;
                    var section=area.getRenderSectionAt(new BlockPos(x*16+8,y*16+8,z*16+8));
                    if(section==null||section.getSectionNode()!=net.minecraft.core.SectionPos.asLong(x,y,z)) {complete=false;continue;}
                    if(section.getSectionMesh()==net.minecraft.client.renderer.chunk.CompiledSectionMesh.UNCOMPILED) {
                        complete=false;
                        if(submitted<2) {var region=cache.createRegion(client.level,section.getSectionNode());if(region!=null) {section.compileSync(region);submitted++;}}
                    }
                }
                if(!complete||!client.levelRenderer.hasRenderedAllSections()) {settled=0;return;}
                if(++settled<80)return;
                saving=true;String file="lake-reed-"+view+".png";
                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()-> {
                    if(!Files.exists(DIR.resolve("screenshots/"+file)))failure=new AssertionError("Native capture missing");
                    view++;posed=false;saving=false;settled=0;
                }));
            }
            catch(Throwable t) {
                failure=t;finished=true;Blockbranch.LOGGER.error("LAKE_REED_VISUAL_FAIL",t);
                try {Files.writeString(DIR.resolve("result.txt"),"FAIL "+t);}catch(Exception ignored) {}
                client.stop();
            }
        });
    }
    private static void press(net.minecraft.client.gui.screens.Screen screen,String label) {
        for(var v:screen.children())if(v instanceof Button button&&button.active&&button.getMessage().getString().equals(label)) {button.onPress(new KeyEvent(257,0,0));return;}
        throw new AssertionError("Missing UI button "+label);
    }
}
