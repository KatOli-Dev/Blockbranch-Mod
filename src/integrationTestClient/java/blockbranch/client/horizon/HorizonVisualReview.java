package blockbranch.client.horizon;
import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
/**
 * Matched horizon captures in a disposable UI-created world (seed 424242): for each view,
 * the horizon at a short render distance (filled from cold and timed), the same render
 * distance without it, and native terrain at a long render distance as ground truth.
 * Writes screenshots, {@code result.txt} and {@code views.json} under the run directory.
 */
public final class HorizonVisualReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.horizonVisualDirectory"));
    private static final int SHORT=Integer.getInteger("blockbranch.horizonVisualShort",6),LONG=Integer.getInteger("blockbranch.horizonVisualLong",12);
    private record Mode(String name,int renderDistance,boolean horizon) {}
    private static final Mode[] MODES={new Mode("horizon",SHORT,true),new Mode("short",SHORT,false),new Mode("native",LONG,false)};
    /**
     * Camera height above the surface, yaw (0 south, 90 west, 180 north, -90 east) and pitch for
     * each view. All share one column so horizon tiles fill once; steep high views look at the seam.
     * A last view, when water is found, stands on its own column (x, surface y, z appended) facing
     * the shore from between the short and long native radii, so both modes draw the same water.
     */
    private static final double[][] FIXED_VIEWS={{48,-90,8},{6,150,1},{2,180,0},{2,-90,2},{20,90,4},{20,0,4},{48,180,8},{100,45,15},{160,-135,45}};
    private static volatile double[][] VIEWS=FIXED_VIEWS;
    private static final long BEGAN=System.nanoTime();
    private static boolean opened,configured,saving;
    private static volatile boolean finished,posed;
    private static volatile Throwable failure;
    private static volatile Vec3 camera;
    private static volatile int step;
    private static int settled;
    private static long modeBegan,fpsSamples,fpsSum;
    private static int fpsMin;
    private static long lastWait;
    private static final List<Object> records=new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished||server.getPlayerList().getPlayers().isEmpty())return;
            var player=server.getPlayerList().getPlayers().getFirst();
            try {
                if(camera==null) {
                    var level=server.overworld();var rules=level.getGameRules();
                    rules.set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);
                    rules.set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
                    rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                    rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER,false,server);
                    var commands=server.getCommands();var source=server.createCommandSourceStack();
                    commands.performPrefixedCommand(source,"time set noon");commands.performPrefixedCommand(source,"weather clear");
                    var spawn=player.blockPosition();int y=level.getChunk(spawn.getX()>>4,spawn.getZ()>>4).getHeight(Heightmap.Types.MOTION_BLOCKING,spawn.getX()&15,spawn.getZ()&15);
                    String views=System.getProperty("blockbranch.horizonVisualViews");
                    VIEWS="structures".equals(views)?structureViews(level,spawn.getX(),y,spawn.getZ()):"wells".equals(views)?wellViews(level):withWaterView(level,spawn.getX(),spawn.getZ());
                    camera=new Vec3(spawn.getX()+.5,y,spawn.getZ()+.5);
                }
                if(!posed)return;
                double[] v=VIEWS[step/MODES.length];var target=target(v);
                player.getAbilities().flying=true;player.onUpdateAbilities();player.setDeltaMovement(Vec3.ZERO);
                if(player.position().distanceToSqr(target)>.001||Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot()-(float)v[1]))>.02)
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+target.x+" "+target.y+" "+target.z+" "+v[1]+" "+v[2]);
            } catch(Throwable t){failure=t;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(HorizonVisualReview::tick);
    }

    private static void tick(Minecraft client) {
        try {
            if(System.nanoTime()-BEGAN>2_280_000_000_000L)throw new AssertionError("Bounded horizon visual deadline");
            if(failure!=null)throw new AssertionError("Server driver",failure);
            if(saving)return;
            if(finished) {
                saving=true;
                Files.writeString(DIR.resolve("views.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(records));
                Files.writeString(DIR.resolve("result.txt"),"PASS "+records.size()+" matched horizon captures");
                client.disconnect(new TitleScreen(),false);client.stop();return;
            }
            var screen=client.gui.screen();
            if(!opened&&screen instanceof TitleScreen){opened=true;CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));return;}
            if(!configured&&screen instanceof CreateWorldScreen create) {
                var state=create.getUiState();state.setName("HorizonVisualReview");state.setSeed("424242");
                state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);state.setAllowCommands(true);
                state.setWorldType(state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().toString().equals("blockbranch:realistic")).findFirst().orElseThrow());
                configured=true;press(create,"Create New World");return;
            }
            if(screen!=null&&screen.getClass().getSimpleName().equals("ConfirmExperimentalFeaturesScreen")){press(screen,"Proceed");return;}
            if(client.level==null||client.player==null||camera==null)return;
            var mode=MODES[step%MODES.length];double[] v=VIEWS[step/MODES.length];
            if(!posed) {
                // The integrated server sizes chunk sending from the broadcast client information.
                client.options.renderDistance().set(mode.renderDistance());client.options.fov().set(v.length>6?(int)v[6]:70);client.options.broadcastOptions();
                HorizonClient.suppressed=!mode.horizon();
                posed=true;settled=0;modeBegan=System.nanoTime();fpsSamples=0;fpsSum=0;fpsMin=Integer.MAX_VALUE;
                return;
            }
            client.gui.toastManager().clear();
            if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            var target=target(v);
            if(client.player.position().distanceToSqr(target)>.001||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-(float)v[1]))>.02){settled=0;return;}
            if(!nativeComplete(client,mode.renderDistance())){settled=0;logWait(client,mode);return;}
            var stats=HorizonClient.stats();
            if(mode.horizon()&&(stats.missing()>0||stats.meshing()>0||stats.drawn()==0)){settled=0;return;}
            long now=System.nanoTime();
            // Frames per second over the settled window (the native counter covers the last second).
            if(settled>=20){int fps=client.getFps();fpsSamples++;fpsSum+=fps;fpsMin=Math.min(fpsMin,fps);}
            if(++settled<100)return;
            saving=true;
            String file="view"+step/MODES.length+"-"+mode.name()+".png";
            var record=new LinkedHashMap<String,Object>();
            record.put("file",file);record.put("renderDistance",mode.renderDistance());record.put("horizon",mode.horizon());
            record.put("camera",List.of(target.x,target.y,target.z,v[1],v[2]));record.put("fov",v.length>6?(int)v[6]:70);
            record.put("settleSeconds",(now-modeBegan)/1e9);record.put("meanFps",fpsSamples==0?0:(double)fpsSum/fpsSamples);record.put("minFps",fpsMin);record.put("horizonVertexMiB",stats.quads()*4L*HorizonMesher.STRIDE/1048576.0);
            record.put("horizonStats",stats);record.put("renderer",com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().name()+" ("+com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().backendName()+")");
            records.add(record);Blockbranch.LOGGER.info("HORIZON_VISUAL {}",record);
            net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                if(!Files.exists(DIR.resolve("screenshots/"+file)))failure=new AssertionError("Capture missing "+file);
                step++;posed=false;saving=false;
                if(step==MODES.length*VIEWS.length)finished=true;
            }));
        } catch(Throwable t) {
            failure=t;finished=true;Blockbranch.LOGGER.error("HORIZON_VISUAL_FAIL",t);
            try{Files.writeString(DIR.resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}
            client.stop();
        }
    }
    /** Whether water continues 16 and 32 blocks further from spawn, so the view finds a lake or wide river rather than a pond. */
    private static boolean waterBeyond(blockbranch.realistic.RealisticGenerator generator,net.minecraft.world.level.levelgen.RandomState random,int x,int z,int wx,int wz,int r) {
        for(int extra=16;extra<=32;extra+=16)
            if(generator.waterSurface(wx+(int)Math.round((wx-x)*(double)extra/r),wz+(int)Math.round((wz-z)*(double)extra/r),random)==blockbranch.horizon.HorizonTiles.NONE)return false;
        return true;
    }
    private static Vec3 target(double[] v){return v.length>3?new Vec3(v[3]+.5,v[4]+v[0],v[5]+.5):camera.add(0,v[0],0);}
    /**
     * Adds a view of the nearest open water within 16,000 blocks, scanned on rings 64 blocks
     * apart with the generator's cheap drainage query and bounded to a minute of server time.
     * With {@code blockbranch.horizonVisualViews=water} that view is the only one.
     */
    private static double[][] withWaterView(net.minecraft.server.level.ServerLevel level,int x,int z) {
        boolean only="water".equals(System.getProperty("blockbranch.horizonVisualViews"));
        if(!(level.getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator generator))return FIXED_VIEWS;
        var random=level.getChunkSource().randomState();int[] column=new int[blockbranch.horizon.HorizonTiles.FIELDS];
        long began=System.nanoTime();int scanned=0;
        search:for(int r=160;r<=16000;r+=64) {
            int samples=Math.max(72,(int)(2*Math.PI*r/64));
            for(int a=0;a<samples;a++) {
                if(System.nanoTime()-began>60_000_000_000L){Blockbranch.LOGGER.info("HORIZON_VISUAL_WATER search stopped at {} blocks after {} columns",r,scanned);break search;}
                double t=2*Math.PI*a/samples;int wx=x+(int)Math.round(Math.sin(t)*r),wz=z+(int)Math.round(Math.cos(t)*r);scanned++;
                if(generator.waterSurface(wx,wz,random)==blockbranch.horizon.HorizonTiles.NONE||!waterBeyond(generator,random,x,z,wx,wz,r))continue;
                // Stand 130-170 blocks back toward spawn (beyond the short radius, inside the long
                // one) on open ground, so no crown fills the frame.
                int cx=0,cz=0,ground=0;
                for(int back=130;back<=170;back+=4) {
                    cx=wx+(int)Math.round((x-wx)*(double)back/r);cz=wz+(int)Math.round((z-wz)*(double)back/r);
                    generator.horizonColumn(cx,cz,random,column,0,biome->0);
                    ground=Math.max(column[blockbranch.horizon.HorizonTiles.TOP],column[blockbranch.horizon.HorizonTiles.WATER])+1;
                    if(column[blockbranch.horizon.HorizonTiles.TOP]==column[blockbranch.horizon.HorizonTiles.GROUND])break;
                }
                double yaw=Math.toDegrees(Math.atan2(-(wx-cx),wz-cz));
                Blockbranch.LOGGER.info("HORIZON_VISUAL_WATER at {} {} distance {} after {} columns in {} s; camera {} {} {} yaw {}",wx,wz,r,scanned,(System.nanoTime()-began)/1e9,cx,ground,cz,yaw);
                double[] view={64,yaw,20,cx,ground,cz};
                if(only)return new double[][]{view};
                var views=java.util.Arrays.copyOf(FIXED_VIEWS,FIXED_VIEWS.length+1);views[FIXED_VIEWS.length]=view;return views;
            }
        }
        Blockbranch.LOGGER.info("HORIZON_VISUAL_WATER none found");
        if(only)throw new AssertionError("No open water for the water-only review");
        return FIXED_VIEWS;
    }
    /** Tower of the player-edit view, east of spawn: between the short and long native radii. */
    private static final int TOWER=150,TOWER_HEIGHT=40;
    /** Structure camera height over the canopy: steep enough to see between forest crowns. */
    private static final int RISE=110;
    /**
     * Views for {@code blockbranch.horizonVisualViews=structures}: a redstone-block tower placed as a player
     * edit {@value #TOWER} blocks east of spawn, seen from spawn, then the nearest planned surface
     * structure at least 600 blocks out (a village when one is planned), seen from 140 blocks
     * away and {@value #RISE} blocks up. It is found from structure placement alone, planning starts in detached chunks, so
     * the world has not generated it when the horizon draws it.
     */
    private static double[][] structureViews(net.minecraft.server.level.ServerLevel level,int x,int y,int z) {
        var chunk=level.getChunk((x+TOWER)>>4,z>>4);
        int ground=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(x+TOWER)&15,z&15);
        var red=net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState();
        for(int dy=0;dy<TOWER_HEIGHT;dy++)for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)level.setBlock(new net.minecraft.core.BlockPos(x+TOWER+dx,ground+dy,z+dz),red,3);
        Blockbranch.LOGGER.info("HORIZON_VISUAL_EDIT tower at {} {} {}",x+TOWER,ground,z);
        compareReaders(level,(x>>4)+8,z>>4);
        var views=new ArrayList<double[]>();views.add(new double[]{20,-90,4});
        var village=plannedStructure(level,x,z);
        if(village!=null) {
            int vx=village.getX(),vz=village.getZ();double r=Math.hypot(vx-x,vz-z);
            int cx=vx+(int)Math.round((x-vx)*140/r),cz=vz+(int)Math.round((z-vz)*140/r);
            var generator=(blockbranch.realistic.RealisticGenerator)level.getChunkSource().getGenerator();
            int[] column=new int[blockbranch.horizon.HorizonTiles.FIELDS];
            generator.horizonColumn(cx,cz,level.getChunkSource().randomState(),column,0,biome->0);
            int top=Math.max(column[blockbranch.horizon.HorizonTiles.TOP],column[blockbranch.horizon.HorizonTiles.WATER])+1;
            double yaw=Math.toDegrees(Math.atan2(-(vx-cx),vz-cz));
            compareReaders(level,(cx+vx)/2>>4,(cz+vz)/2>>4);
            double pitch=Math.toDegrees(Math.atan2(top+RISE-village.getY(),140));
            Blockbranch.LOGGER.info("HORIZON_VISUAL_STRUCTURE at {} {} distance {}; camera {} {} {} yaw {}",vx,vz,r,cx,top,cz,yaw);
            views.add(new double[]{RISE,yaw,pitch,cx,top,cz});
        }
        return views.toArray(double[][]::new);
    }
    /** Well cameras: distance (past the short native radius, inside the long one, on level-0 tiles), rise above ground, field of view. */
    private static final int WELL_DISTANCE=136,WELL_RISE=30,WELL_FOV=30;
    /**
     * Views for {@code blockbranch.horizonVisualViews=wells}: each {@link blockbranch.horizon.HorizonWellSites}
     * well (the nearest, and the nearest crossing a chunk border) seen from {@value #WELL_DISTANCE} blocks
     * south and {@value #WELL_RISE} blocks up, with a {@value #WELL_FOV}° field of view. Horizon mode runs
     * first, so the well is drawn from a detached chunk before the long native mode generates it.
     */
    private static double[][] wellViews(net.minecraft.server.level.ServerLevel level) {
        var generator=(blockbranch.realistic.RealisticGenerator)level.getChunkSource().getGenerator();
        var views=new ArrayList<double[]>();int[] column=new int[blockbranch.horizon.HorizonTiles.FIELDS];
        for(var site:blockbranch.horizon.HorizonWellSites.find(level)) {
            var p=site.plan();int cx=p.x(),cz=p.z()+WELL_DISTANCE;
            generator.horizonColumn(cx,cz,level.getChunkSource().randomState(),column,0,biome->0);
            int top=Math.max(column[blockbranch.horizon.HorizonTiles.TOP],column[blockbranch.horizon.HorizonTiles.WATER])+1;
            double pitch=Math.toDegrees(Math.atan2(top+WELL_RISE-(p.y()+2),WELL_DISTANCE));
            Blockbranch.LOGGER.info("HORIZON_VISUAL_WELL {} well at {} {} {}; camera {} {} {} yaw 180 pitch {} fov {}",site.name(),p.x(),p.y(),p.z(),cx,top+WELL_RISE,cz,pitch,WELL_FOV);
            views.add(new double[]{WELL_RISE,180,pitch,cx,top,cz,WELL_FOV});
        }
        return views.toArray(double[][]::new);
    }
    /**
     * Compares the analytic sampler with the block reader over detached terrain-only chunks
     * (4×4 chunks from the given one) and logs per-field mismatches, since generated and
     * sandboxed chunks are read from blocks beside analytic ones.
     */
    private static void compareReaders(net.minecraft.server.level.ServerLevel level,int fromX,int fromZ) {
        var generator=(blockbranch.realistic.RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
        int f=blockbranch.horizon.HorizonTiles.FIELDS;int[] a=new int[f],b=new int[f],mismatches=new int[f];int columns=0,logged=0;
        for(int cz=fromZ;cz<fromZ+4;cz++)for(int cx=fromX;cx<fromX+4;cx++) {
            var chunk=new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(cx,cz),net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
            var sparse=(blockbranch.cubic.SparseColumn)chunk;
            sparse.blockbranch$terrain(new blockbranch.cubic.ColumnTerrain(chunk,generator.profile(chunk,random),sparse.blockbranch$factory(),null,new int[0]));
            var source=new blockbranch.horizon.HorizonColumns.Source(){
                @Override public net.minecraft.world.level.block.state.BlockState state(int x,int y,int z){return chunk.getBlockState(new net.minecraft.core.BlockPos(x,y,z));}
                @Override public int top(java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> p,int x,int z,int limit){return blockbranch.cubic.ColumnQueries.top(chunk,p,x&15,z&15,limit);}
            };
            for(int z=cz*16;z<cz*16+16;z++)for(int x=cx*16;x<cx*16+16;x++) {
                generator.horizonColumn(x,z,1,random,a,0,biome->0);
                blockbranch.horizon.HorizonColumns.read(source,x,z,blockbranch.cubic.ColumnLimits.MIN_Y,blockbranch.cubic.ColumnLimits.MAX_Y-1,b,0);
                columns++;boolean differs=false;
                for(int i=0;i<f;i++)if(i!=blockbranch.horizon.HorizonTiles.BIOME&&a[i]!=b[i]){mismatches[i]++;differs=true;}
                if(differs&&logged++<12)Blockbranch.LOGGER.info("HORIZON_VISUAL_READERS {} {} analytic={} blocks={}",x,z,describe(a),describe(b));
            }
        }
        Blockbranch.LOGGER.info("HORIZON_VISUAL_READERS columns={} mismatches top,topState,ground,groundState,deep,water,biome,canopyBottom={}",columns,java.util.Arrays.toString(mismatches));
    }
    private static String describe(int[] d) {
        var sb=new StringBuilder("[");
        for(int i=0;i<d.length;i++){if(i>0)sb.append(',');
            if(i==1||i==3||i==4)sb.append(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(net.minecraft.world.level.block.Block.stateById(d[i]).getBlock()).getPath());
            else sb.append(d[i]==blockbranch.horizon.HorizonTiles.NONE?"-":String.valueOf(d[i]));}
        return sb.append(']').toString();
    }
    /**
     * Centre of the nearest valid start 600 to 4,000 blocks from (x,z) that rises at least four
     * blocks above the ground at its centre, villages first; null when none is planned. Sets
     * spaced closer than eight chunks (mineshafts) are skipped, and the search is bounded to a
     * minute of server time.
     */
    private static net.minecraft.core.BlockPos plannedStructure(net.minecraft.server.level.ServerLevel level,int x,int z) {
        var source=level.getChunkSource();var state=source.getGeneratorState();var random=source.randomState();
        var generator=(blockbranch.realistic.RealisticGenerator)source.getGenerator();
        var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var manager=new net.minecraft.world.level.StructureManager(level,null,null){@Override public boolean shouldGenerateStructures(){return true;}};
        int[] column=new int[blockbranch.horizon.HorizonTiles.FIELDS];long began=System.nanoTime();int candidates=0,valid=0;
        net.minecraft.core.BlockPos best=null;String bestKind=null;double bestDistance=Double.MAX_VALUE;
        for(boolean villages:new boolean[]{true,false}) {
            for(var set:state.possibleStructureSets()) {
                boolean village=set.value().structures().stream().anyMatch(e->registry.getKey(e.structure().value()).getPath().startsWith("village_"));
                if(village!=villages||!(set.value().placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread)||spread.spacing()<8)continue;
                search:for(int ring=1;ring<=4000/16/spread.spacing()+1;ring++)
                    for(int gz=-ring;gz<=ring;gz++)for(int gx=-ring;gx<=ring;gx++) {
                        if(Math.abs(gx)!=ring&&Math.abs(gz)!=ring)continue;
                        if(System.nanoTime()-began>60_000_000_000L)break search;
                        var at=spread.getPotentialStructureChunk(state.getLevelSeed(),(x>>4)+gx*spread.spacing(),(z>>4)+gz*spread.spacing());
                        double d=Math.hypot(at.getMiddleBlockX()-x,at.getMiddleBlockZ()-z);
                        if(d<600||d>4000||d>=bestDistance||!spread.isStructureChunk(state,at.x(),at.z()))continue;
                        candidates++;
                        var scratch=new net.minecraft.world.level.chunk.ProtoChunk(at,net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
                        generator.createStructures(level.registryAccess(),state,manager,scratch,level.getServer().getStructureTemplateManager(),level.dimension());
                        for(var start:scratch.getAllStarts().values()) {
                            if(!start.isValid())continue;valid++;
                            var box=start.getBoundingBox();var c=box.getCenter();
                            generator.horizonColumn(c.getX(),c.getZ(),random,column,0,biome->0);
                            if(box.maxY()<column[blockbranch.horizon.HorizonTiles.GROUND]+4)continue;
                            best=c;bestKind=registry.getKey(start.getStructure()).toString();bestDistance=d;
                        }
                    }
            }
            if(best!=null)break;
        }
        Blockbranch.LOGGER.info("HORIZON_VISUAL_STRUCTURE {} at {} after {} candidates, {} valid starts, {} s",bestKind,best,candidates,valid,(System.nanoTime()-began)/1e9);
        return best;
    }
    /** Every chunk within the native view distance is loaded and every section has compiled. */
    private static boolean nativeComplete(Minecraft client,int renderDistance) {
        var area=client.levelRenderer.viewArea();
        if(area==null||area.getViewDistance()!=renderDistance)return false;
        int cx=client.player.blockPosition().getX()>>4,cz=client.player.blockPosition().getZ()>>4;
        for(int z=cz-renderDistance;z<=cz+renderDistance;z++)for(int x=cx-renderDistance;x<=cx+renderDistance;x++)
            if(ChunkTrackingView.isInViewDistance(cx,cz,renderDistance,x,z)&&!client.level.getChunkSource().hasChunk(x,z))return false;
        return client.levelRenderer.hasRenderedAllSections();
    }
    /** Periodic progress line so a stalled native settle is diagnosable from the log. */
    private static void logWait(Minecraft client,Mode mode) {
        long now=System.nanoTime();if(now-lastWait<15_000_000_000L)return;lastWait=now;
        var area=client.levelRenderer.viewArea();int rd=mode.renderDistance(),missing=0;
        int cx=client.player.blockPosition().getX()>>4,cz=client.player.blockPosition().getZ()>>4;
        for(int z=cz-rd;z<=cz+rd;z++)for(int x=cx-rd;x<=cx+rd;x++)
            if(ChunkTrackingView.isInViewDistance(cx,cz,rd,x,z)&&!client.level.getChunkSource().hasChunk(x,z))missing++;
        Blockbranch.LOGGER.info("HORIZON_VISUAL_WAIT mode={} rd={} areaRd={} missingChunks={} renderedAll={} elapsed={}s",mode.name(),rd,area==null?-1:area.getViewDistance(),missing,client.levelRenderer.hasRenderedAllSections(),(now-modeBegan)/1_000_000_000L);
    }
    private static void press(net.minecraft.client.gui.screens.Screen screen,String label) {
        for(var v:screen.children())if(v instanceof Button button&&button.active&&button.getMessage().getString().equals(label)){button.onPress(new KeyEvent(257,0,0));return;}
        throw new AssertionError("Missing UI button "+label);
    }
}
