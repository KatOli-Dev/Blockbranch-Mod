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
/** Native views of the audited seed-1 ordinary-forest witnesses at fixed poses. The bundled preset (gated oak crowns) and
 * {@code blockbranch:broadleaf_crowns_baseline} (the same code and preset with the gate absent) are captured from identical
 * cameras, never from tree positions; no blocks are staged. Poses 0 and 1 are the audit's views 4 and 9 exactly;
 * 2 and 3 look at two mature oaks from the south; 4 and 5 repeat the audit offsets at a second oak stand (30000,-346000) because the
 * audit's second forest scene (above 1100 m) is montane needleleaf under the bundled preset; 6 and 7 are the dappled woodland (audit views 6 and 11), which the gate
 * does not change. Software rendering is evidence, not acceptance. */
public final class BroadleafCrownsVisualReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.broadleafCrownsVisualDirectory","build/broadleaf-crowns-evidence/disabled-visual"));
    private static final boolean BASELINE=Boolean.getBoolean("blockbranch.broadleafCrownsVisualBaseline");
    private static final long BEGAN=System.nanoTime();
    /** camera x,y,z, yaw, pitch, expected biome at the audit target x,z. */
    private static final double[][] POSES={
        {47976.5,255.2,-304029.5,-38.659808254090095,45.66422181263511,48000,-304000},
        {47984.5,217.2,-304015.5,-45.0,-2.4293971074758813,48000,-304000},
        {47972.5,217.2,-304078.5,180,-20,47972,-304101},
        {48021.5,217.2,-304076.5,180,-20,48021,-304099},
        {29976.5,230.2,-346029.5,-38.659808254090095,45.66422181263511,30000,-346000},
        {29984.5,195.2,-346015.5,-45.0,-2.4293971074758813,30000,-346000},
        {15976.5,135.2,-240029.5,-38.659808254090095,45.6642218126351,16000,-240000},
        {15984.5,97.2,-240015.5,-45.0,-2.4293971074758813,16000,-240000}};
    private static final String[] BIOMES={"minecraft:forest","minecraft:forest","minecraft:forest","minecraft:forest",null,null,"blockbranch:dappled_oak_birch_woodland","blockbranch:dappled_oak_birch_woodland"};
    private static boolean opened,configured,saving;
    private static volatile boolean finished,posed;
    private static volatile int view;
    private static volatile Throwable failure;
    private static int settled;
    private static final List<Object> records=new ArrayList<>();

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
                if(g.broadleafCrownDefinition()!=(BASELINE?0:1))throw new AssertionError("Wrong preset for this capture: gate="+g.broadleafCrownDefinition());
                if(view==POSES.length){Files.writeString(DIR.resolve("result.txt"),"PASS native ordinary-forest views gate="+g.broadleafCrownDefinition()+"; no staged terrain");finished=true;return;}
                var commands=server.getCommands();var source=server.createCommandSourceStack();
                commands.performPrefixedCommand(source,"time set noon");commands.performPrefixedCommand(source,"weather clear");
                var p=POSES[view];
                for(int cz=((int)p[2]-80)>>4;cz<=((int)p[2]+80)>>4;cz++)for(int cx=((int)p[0]-80)>>4;cx<=((int)p[0]+80)>>4;cx++)level.setChunkForced(cx,cz,true);
                if(view==0)Files.writeString(DIR.resolve("generator.json"),RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().toString());
                var biome=level.getBiome(new BlockPos((int)p[5],level.getSeaLevel(),(int)p[6])).unwrapKey().orElseThrow().identifier().toString();
                if(BIOMES[view]!=null&&!biome.equals(BIOMES[view]))throw new AssertionError("Audit witness biome changed at view "+view+": "+biome+" vs "+BIOMES[view]);
                commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+p[0]+" "+p[1]+" "+p[2]+" "+p[3]+" "+p[4]);
                records.add(Map.of("view",view,"camera",List.of(p[0],p[1],p[2]),"yaw",p[3],"pitch",p[4],"seed",level.getSeed(),"gate",g.broadleafCrownDefinition(),"biome",biome));
                posed=true;
            }
            catch(Throwable t) {failure=t;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client-> {
            try {
                if(System.nanoTime()-BEGAN>1_500_000_000_000L)throw new AssertionError("Bounded broadleaf visual deadline");
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
                    var state=create.getUiState();state.setName("BroadleafCrownsVisualReview");state.setSeed("1");
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);state.setAllowCommands(true);
                    String id=BASELINE?"blockbranch:broadleaf_crowns_baseline":"blockbranch:realistic";
                    state.setWorldType(state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().toString().equals(id)).findFirst().orElseThrow());
                    configured=true;press(create,"Create New World");return;
                }
                if(screen!=null&&screen.getClass().getSimpleName().equals("ConfirmExperimentalFeaturesScreen")) {press(screen,"Proceed");return;}
                if(!posed||client.level==null||client.player==null||view>=POSES.length)return;
                var p=POSES[view];
                client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if(client.player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(p[0],p[1],p[2]))>.001||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-(float)p[3]))>.02||Math.abs(client.player.getXRot()-(float)p[4])>.02) {settled=0;return;}
                client.options.fov().set(65);client.gui.toastManager().clear();
                if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                var area=client.levelRenderer.viewArea();if(area==null)return;
                // Every visible section within 80 blocks of the camera must be meshed before the frame counts.
                var subject=new BoundingBox((int)p[0]-80,(int)p[1]-70,(int)p[2]-80,(int)p[0]+80,(int)p[1]+40,(int)p[2]+80);
                boolean complete=true;var cache=new net.minecraft.client.renderer.chunk.RenderRegionCache();int submitted=0;
                for(int z=subject.minZ()>>4;z<=subject.maxZ()>>4;z++)for(int x=subject.minX()>>4;x<=subject.maxX()>>4;x++)for(int y=subject.minY()>>4;y<=subject.maxY()>>4;y++) {
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
                saving=true;String file="broadleaf-"+view+".png";
                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()-> {
                    if(!Files.exists(DIR.resolve("screenshots/"+file)))failure=new AssertionError("Native capture missing");
                    view++;posed=false;saving=false;settled=0;
                }));
            }
            catch(Throwable t) {
                failure=t;finished=true;Blockbranch.LOGGER.error("BROADLEAF_CROWNS_VISUAL_FAIL",t);
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
