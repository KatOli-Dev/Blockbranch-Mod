package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.RealisticProbe;
import blockbranch.realistic.ScreeContent;
import blockbranch.realistic.ReedbedContent;
import blockbranch.realistic.LakeReedBlock;
import blockbranch.realistic.RiverMarginContent;
import blockbranch.realistic.RiverMarginSmoke;
import blockbranch.realistic.SandyShoreContent;
import blockbranch.realistic.SandyShoreSmoke;
import blockbranch.realistic.MeadowContent;
import blockbranch.realistic.MeadowFescueBlock;
import blockbranch.realistic.WoodlandSmoke;
import blockbranch.realistic.SteppeContent;
import blockbranch.realistic.BorealSmoke;
import blockbranch.realistic.WarmSmoke;
import blockbranch.realistic.HumidSmoke;
import blockbranch.realistic.TundraSmoke;
import blockbranch.realistic.ColdSteppeSmoke;
import blockbranch.realistic.DesertSmoke;
import blockbranch.realistic.DesertContent;
import blockbranch.realistic.SteppeShrubBlock;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Real rendering, light, vertical travel, eviction and reloading from section storage. */
public final class RealisticClientSmoke {
    private static void verifyBorealFloor(net.minecraft.client.Minecraft client,BlockPos bed){
        int floors=0,carpets=0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;
            var terrain=((SparseColumn)client.level.getChunk(cx,cz)).blockbranch$terrain();
            if(terrain==null)throw new AssertionError("Missing boreal floor profile");
            var tag=terrain.base().encode(terrain.factory());var ground=tag.getIntArray("ground").orElseThrow();var cover=tag.getByteArray("boreal").orElseThrow();
            for(int i=0;i<256;i++)if(cover[i]>0){
                var p=new BlockPos(cx*16+(i&15),ground[i],cz*16+(i>>4));floors++;
                if(client.level.getBlockState(p).is(Blocks.MOSS_BLOCK))throw new AssertionError("Fresh boreal floor contains full moss at "+p);
                if(client.level.getBlockState(p.above()).is(Blocks.MOSS_CARPET)){
                    carpets++;
                    if(!client.level.getBlockState(p).is(Blocks.PODZOL)||!client.level.getBlockState(p.below()).is(Blocks.DIRT))throw new AssertionError("Carpet lost soil support");
                }
            }
        }
        if(floors<100||carpets==0)throw new AssertionError("Boreal capture lacks soil/carpet witnesses");
        Blockbranch.LOGGER.info("BOREAL_SURFACE_CLIENT_IDENTITIES_PASS floors={} carpets={} mossCubes=0",floors,carpets);
    }
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            final boolean drySteppeOnly=Boolean.getBoolean("blockbranch.drySteppeClientOnly");
            final FixtureSceneReadiness drySteppeCapture=new FixtureSceneReadiness();
            BlockPos drySteppeBed;
            final boolean borealOnly=Boolean.getBoolean("blockbranch.borealClientOnly");
            final FixtureSceneReadiness borealCapture=new FixtureSceneReadiness();
            final boolean broadDeep=Boolean.getBoolean("blockbranch.temperateDeepClientOnly");
            final boolean screeOnly=Boolean.getBoolean("blockbranch.screeClientOnly");
            final FixtureSceneReadiness screeCapture=new FixtureSceneReadiness();
            final boolean coldSteppeOnly=Boolean.getBoolean("blockbranch.coldSteppeClientOnly");
            final FixtureSceneReadiness coldSteppeCapture=new FixtureSceneReadiness();
            final boolean reedOnly=Boolean.getBoolean("blockbranch.reedClientOnly");
            final FixtureSceneReadiness reedCapture=new FixtureSceneReadiness();
            BlockPos screeBed;
            final boolean boundaryOnly=Boolean.getBoolean("blockbranch.boundaryClientOnly");
            final long deadline=System.nanoTime()+(boundaryOnly?180_000_000_000L:780_000_000_000L);
            long nextProgress;
            int stage,settled,surfaceChunks,evictionWait;
            int reedHeight,reedWaterDepth;
            boolean finished;
            BlockPos humidBed,tundraBed,tundraBerry,lakeBed,riverBed,shelfBed,deepBed;
            int lakeWater,riverWater,shelfWater,deepWater;
            public void onEndTick(net.minecraft.client.Minecraft client) {
                if(finished) return;
                try {
                    if(System.nanoTime()>deadline) throw new AssertionError("Lazy terrain client timed out at stage "+stage+" "+screeCapture.diagnostic());
                    if(stage==139)return;
                    if(stage==6 || stage==10 || stage==14 || stage==20 || stage==27 || stage==33 || stage==34 || stage==41 || stage==50 || stage==51 || stage==58 || stage==59 || stage==67 || stage==73 || stage==75 || stage==78 || stage==79 || stage==86 || stage==87 || stage==99 || stage==106 || stage==107 || stage==116 || stage==117 || stage==126 || stage==127 || stage==134 || stage==135) return; // Wait for an asynchronous screenshot save.
                    if((coldSteppeOnly||borealOnly)&&stage>0&&(client.level==null||client.player==null))throw new AssertionError("Disconnected during focused biome capture");
                    if(client.level==null || client.player==null || (stage<4 && Math.abs(client.player.getX()-1401431)>2)) return;
                    var level=client.level;
                    if(stage==0&&reedOnly){
                        client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                        client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                        stage=7;ClientPlayNetworking.send(new RealisticProbe(7));return;
                    }
                    if(stage==0&&drySteppeOnly){client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);stage=36;ClientPlayNetworking.send(new RealisticProbe(28));return;}
                    if(stage==137&&drySteppeOnly){
                        if(Math.abs(client.player.getY()-(drySteppeBed.getY()+36))>1||client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))return;
                        if(!drySteppeCapture.readySurface(client,"dry-steppe-overlook"))return;
                        Blockbranch.LOGGER.info("DRY_STEPPE_CAPTURE_COMPLETE {}",drySteppeCapture.diagnostic());stage=139;String file="dry-steppe-overlook.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{stage=37;ClientPlayNetworking.send(new RealisticProbe(29));}));return;
                    }
                    if(stage==0&&borealOnly){stage=42;ClientPlayNetworking.send(new RealisticProbe(33));return;}
                    if(stage==0&&screeOnly){stage=4;ClientPlayNetworking.send(new RealisticProbe(4));return;}
                    if(stage==136){
                        var base=((SparseColumn)level.getChunkAt(screeBed)).blockbranch$terrain().base();
                        for(int dy:new int[]{0,1,5,6}){var p=screeBed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))return;}
                        if(++settled<20)return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");
                        Blockbranch.LOGGER.info("SCREE_CAPTURE_FALL_AND_RESTORE_PASS bed={}",screeBed);finished=true;client.stop();return;
                    }
                    if(stage==0&&boundaryOnly){stage=60;ClientPlayNetworking.send(new RealisticProbe(47));return;}
                    if(stage==0&&Boolean.getBoolean("blockbranch.terrainCollateralClientOnly")){stage=74;ClientPlayNetworking.send(new RealisticProbe(60));return;}
                    if(stage==0&&(broadDeep||Boolean.getBoolean("blockbranch.deepClientOnly"))){stage=130;ClientPlayNetworking.send(new RealisticProbe(96));return;}
                    if(stage==0&&Boolean.getBoolean("blockbranch.shelfClientOnly")){stage=120;ClientPlayNetworking.send(new RealisticProbe(90));return;}
                    if(stage==0&&Boolean.getBoolean("blockbranch.riverClientOnly")){stage=110;ClientPlayNetworking.send(new RealisticProbe(84));return;}
                    if(stage==0&&Boolean.getBoolean("blockbranch.lakeClientOnly")){stage=100;ClientPlayNetworking.send(new RealisticProbe(78));return;}
                    if(stage==0&&coldSteppeOnly){stage=62;ClientPlayNetworking.send(new RealisticProbe(50));return;}
                    if(stage==0&&Boolean.getBoolean("blockbranch.steppeArtReview")){stage=36;ClientPlayNetworking.send(new RealisticProbe(28));return;}
                    if(stage==0&&Boolean.getBoolean("blockbranch.tundraClientOnly")){stage=90;ClientPlayNetworking.send(new RealisticProbe(69));return;}
                    if(stage==0&&Boolean.getBoolean("blockbranch.humidClientOnly")){stage=80;ClientPlayNetworking.send(new RealisticProbe(63));return;}
                    if(stage>=130&&stage<=133){
                        client.gui.toastManager().clear();boolean aided=client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                        if(stage==130){int x=client.player.blockPosition().getX(),z=client.player.blockPosition().getZ();var terrain=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(terrain==null)return;int h=terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var p=new BlockPos(x,h,z);if(!(!broadDeep&&level.getBiome(p).is(net.minecraft.world.level.biome.Biomes.DEEP_COLD_OCEAN)||level.getBiome(p).is(broadDeep?net.minecraft.world.level.biome.Biomes.DEEP_OCEAN.identifier():Blockbranch.id("deep_open_ocean_basin_bed"))))return;deepWater=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);if(Math.abs(client.player.getY()-(deepWater+8))>1||aided)return;deepBed=p;}
                        var bed=deepBed;if(bed==null)return;
                        if(stage==132){if(!level.getFluidState(bed).is(net.minecraft.tags.FluidTags.WATER)||!level.getFluidState(bed.above()).isSource())return;if(++settled<20)return;settled=0;stage=133;ClientPlayNetworking.send(new RealisticProbe(99));return;}
                        if(stage==133){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=-1;dy<=2;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))return;}if(aided)return;if(++settled<20)return;if(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,bed.getX(),bed.getZ())!=deepWater)throw new AssertionError("Deep water plane changed during bed edit");Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("REALISTIC_CLIENT_PASS deepOcean={} depth={} priorHabitats={} visibilityAidCleared=true updates={} columnRefreshes={}",bed,deepWater-bed.getY()-1,(broadDeep||Boolean.getBoolean("blockbranch.deepClientOnly"))?0:16,ColumnReceiveBatch.received,ColumnReceiveBatch.refreshed);finished=true;client.stop();return;}
                        boolean bedView=stage==131;if(bedView&&(Math.abs(client.player.getY()-(bed.getY()+4))>1||!aided||!level.getFluidState(client.player.blockPosition().above()).is(net.minecraft.tags.FluidTags.WATER)))return;
                        var area=client.levelRenderer.viewArea();if(area==null)return;var target=bedView?bed:new BlockPos(bed.getX(),deepWater-1,bed.getZ());var rootMesh=area.getRenderSectionAt(target);if(rootMesh==null||!rootMesh.getSectionMesh().hasRenderableLayers())return;
                        int loaded=0,rendered=0;for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var nearby=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(nearby==null)continue;loaded++;int x=cx*16+8,z=cz*16+8,y=nearby.base().top((bedView?net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR:net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE).isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var mesh=area.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;}
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("DEEP_VIEW_READY stage={} loaded={} rendered={} visibility={}",stage,loaded,rendered,bedView?"night_vision_fixture_only":"normal");}
                        if(loaded<121||rendered<25){settled=0;return;}if(++settled<100)return;settled=0;stage=bedView?135:134;String file=broadDeep?(bedView?"deep-ocean-bed-night-vision.png":"deep-ocean-surface.png"):(bedView?"deep-open-ocean-bed-night-vision.png":"deep-open-ocean-surface.png");
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Deep screenshot missing");stage=bedView?132:131;ClientPlayNetworking.send(new RealisticProbe(bedView?98:97));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}}));return;
                    }
                    if(stage>=120&&stage<=125){
                        client.gui.toastManager().clear();
                        if(stage==120){
                            int x=client.player.blockPosition().getX(),z=client.player.blockPosition().getZ();var terrain=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(terrain==null)return;
                            // OCEAN_FLOOR is a server-only heightmap. An early client query can prime an empty map before the terrain window arrives.
                            int h=terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var candidate=new BlockPos(x,h,z);
                            if(!(level.getBiome(candidate).is(Blockbranch.id("open_marine_shelf_coastal_sea"))||level.getBiome(candidate).is(net.minecraft.world.level.biome.Biomes.COLD_OCEAN))||!level.getBlockState(candidate.above()).is(Blocks.SEAGRASS))return;
                            shelfWater=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);if(Math.abs(client.player.getY()-(shelfWater+6))>1)return;shelfBed=candidate;
                        }
                        var bed=shelfBed;if(bed==null)return;var pos=bed.above();
                        if(stage>=122){
                            var a=level.getBlockState(pos);var b=level.getBlockState(pos.above());boolean ready=switch(stage){
                                case 122->a.is(Blocks.TALL_SEAGRASS)&&b.is(Blocks.TALL_SEAGRASS)&&a.getFluidState().isSource()&&b.getFluidState().isSource();
                                case 123->a.is(Blocks.WATER)&&b.is(Blocks.WATER)&&a.getFluidState().isSource()&&b.getFluidState().isSource();
                                case 124->level.getBlockState(bed).is(Blocks.WATER)&&a.is(Blocks.WATER)&&a.getFluidState().isSource();
                                default->a.is(Blocks.SEAGRASS)&&a.getFluidState().isSource()&&b.is(Blocks.WATER);
                            };if(!ready)return;if(++settled<20)return;settled=0;
                            if(stage<125){stage++;ClientPlayNetworking.send(new RealisticProbe(stage-30));return;}
                            var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=0;dy<=2;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))throw new AssertionError("Shelf test restoration mismatch");}
                            if(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,bed.getX(),bed.getZ())!=shelfWater)throw new AssertionError("Shelf water plane changed after aquatic plant interactions");
                            Blockbranch.LOGGER.info("OPEN_MARINE_SHELF_CLIENT_PASS {}",bed);
                            if(Boolean.getBoolean("blockbranch.shelfClientOnly")){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();return;}
                            stage=130;settled=0;ClientPlayNetworking.send(new RealisticProbe(96));return;
                        }
                        if(stage==121&&(Math.abs(client.player.getY()-(bed.getY()+1))>1||!level.getFluidState(client.player.blockPosition().above()).is(net.minecraft.tags.FluidTags.WATER)))return;
                        var area=client.levelRenderer.viewArea();if(area==null)return;var rootMesh=area.getRenderSectionAt(bed);if(rootMesh==null||!rootMesh.getSectionMesh().hasRenderableLayers()){if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("SHELF_FOREGROUND_MESH_PENDING stage={} bed={}",stage,bed);}return;}
                        int loaded=0,rendered=0;
                        for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                            int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var nearby=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(nearby==null)continue;loaded++;
                            int x=cx*16+8,z=cz*16+8,y=nearby.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var mesh=area.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                        }
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("SHELF_VIEW_READY stage={} loaded={} rendered={}",stage,loaded,rendered);}
                        if(loaded<121||rendered<25){settled=0;return;}if(++settled<100)return;settled=0;boolean under=stage==121;stage=under?127:126;String file=under?"open-marine-shelf-underwater.png":"open-marine-shelf-surface.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Shelf screenshot missing");stage=under?122:121;ClientPlayNetworking.send(new RealisticProbe(under?92:91));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage>=110&&stage<=115){
                        client.gui.toastManager().clear();
                        if(stage==110){
                            int x=client.player.blockPosition().getX(),z=client.player.blockPosition().getZ();var terrain=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(terrain==null)return;
                            // OCEAN_FLOOR is a server-only heightmap. An early client query can prime an empty map before the terrain window arrives.
                            int h=terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var candidate=new BlockPos(x,h,z);
                            if(!level.getBiome(candidate).is(Blockbranch.id("open_routed_river_channel"))||!level.getBlockState(candidate.above()).is(Blocks.SEAGRASS))return;
                            riverWater=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);if(Math.abs(client.player.getY()-(riverWater+6))>1)return;riverBed=candidate;
                        }
                        var bed=riverBed;if(bed==null)return;var pos=bed.above();
                        if(stage>=112){
                            var a=level.getBlockState(pos);var b=level.getBlockState(pos.above());boolean ready=switch(stage){
                                case 112->a.is(Blocks.TALL_SEAGRASS)&&b.is(Blocks.TALL_SEAGRASS)&&a.getFluidState().isSource()&&b.getFluidState().isSource();
                                case 113->a.is(Blocks.WATER)&&b.is(Blocks.WATER)&&a.getFluidState().isSource()&&b.getFluidState().isSource();
                                case 114->level.getBlockState(bed).is(Blocks.WATER)&&a.is(Blocks.WATER)&&a.getFluidState().isSource();
                                default->a.is(Blocks.SEAGRASS)&&a.getFluidState().isSource()&&b.is(Blocks.WATER);
                            };if(!ready)return;if(++settled<20)return;settled=0;
                            if(stage<115){stage++;ClientPlayNetworking.send(new RealisticProbe(stage-26));return;}
                            var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=0;dy<=2;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))throw new AssertionError("River test restoration mismatch");}
                            if(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,bed.getX(),bed.getZ())!=riverWater)throw new AssertionError("River water plane changed after aquatic plant interactions");
                            Blockbranch.LOGGER.info("OPEN_ROUTED_RIVER_CLIENT_PASS {}",bed);
                            if(Boolean.getBoolean("blockbranch.riverClientOnly")){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();return;}
                            stage=120;settled=0;ClientPlayNetworking.send(new RealisticProbe(90));return;
                        }
                        if(stage==111&&(Math.abs(client.player.getY()-(bed.getY()+1))>1||!level.getFluidState(client.player.blockPosition().above()).is(net.minecraft.tags.FluidTags.WATER)))return;
                        var area=client.levelRenderer.viewArea();if(area==null)return;var rootMesh=area.getRenderSectionAt(bed);if(rootMesh==null||!rootMesh.getSectionMesh().hasRenderableLayers()){if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("RIVER_FOREGROUND_MESH_PENDING stage={} bed={}",stage,bed);}return;}
                        int loaded=0,rendered=0;
                        for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                            int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var nearby=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(nearby==null)continue;loaded++;
                            int x=cx*16+8,z=cz*16+8,y=nearby.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var mesh=area.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                        }
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("RIVER_VIEW_READY stage={} loaded={} rendered={}",stage,loaded,rendered);}
                        if(loaded<121||rendered<25){settled=0;return;}if(++settled<100)return;settled=0;boolean under=stage==111;stage=under?117:116;String file=under?"open-routed-river-underwater.png":"open-routed-river-surface.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("River screenshot missing");stage=under?112:111;ClientPlayNetworking.send(new RealisticProbe(under?86:85));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage>=100&&stage<=105){
                        client.gui.toastManager().clear();
                        if(stage==100){
                            int x=client.player.blockPosition().getX(),z=client.player.blockPosition().getZ();var terrain=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(terrain==null)return;
                            // OCEAN_FLOOR is a server-only heightmap. An early client query can prime an empty map before the terrain window arrives.
                            int h=terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var candidate=new BlockPos(x,h,z);
                            if(!level.getBiome(candidate).is(Blockbranch.id("open_inland_lake_sediment_bed"))||!level.getBlockState(candidate.above()).is(Blocks.SEAGRASS))return;
                            lakeWater=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);if(Math.abs(client.player.getY()-(lakeWater+6))>1)return;lakeBed=candidate;
                        }
                        var bed=lakeBed;if(bed==null)return;var pos=bed.above();
                        if(stage>=102){
                            var a=level.getBlockState(pos);var b=level.getBlockState(pos.above());boolean ready=switch(stage){
                                case 102->a.is(Blocks.TALL_SEAGRASS)&&b.is(Blocks.TALL_SEAGRASS)&&a.getFluidState().isSource()&&b.getFluidState().isSource();
                                case 103->a.is(Blocks.WATER)&&b.is(Blocks.WATER)&&a.getFluidState().isSource()&&b.getFluidState().isSource();
                                case 104->level.getBlockState(bed).is(Blocks.WATER)&&a.is(Blocks.WATER)&&a.getFluidState().isSource();
                                default->a.is(Blocks.SEAGRASS)&&a.getFluidState().isSource()&&b.is(Blocks.WATER);
                            };if(!ready)return;if(++settled<20)return;settled=0;
                            if(stage<105){stage++;ClientPlayNetworking.send(new RealisticProbe(stage-22));return;}
                            var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=0;dy<=2;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))throw new AssertionError("Lake test restoration mismatch");}
                            if(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,bed.getX(),bed.getZ())!=lakeWater)throw new AssertionError("Lake water plane changed after aquatic plant interactions");
                            Blockbranch.LOGGER.info("OPEN_INLAND_LAKE_CLIENT_PASS {}",bed);
                            if(Boolean.getBoolean("blockbranch.lakeClientOnly")){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();return;}
                            stage=110;settled=0;ClientPlayNetworking.send(new RealisticProbe(84));return;
                        }
                        if(stage==101&&(Math.abs(client.player.getY()-(bed.getY()+1))>1||!level.getFluidState(client.player.blockPosition().above()).is(net.minecraft.tags.FluidTags.WATER)))return;
                        var area=client.levelRenderer.viewArea();if(area==null)return;var rootMesh=area.getRenderSectionAt(bed);if(rootMesh==null||!rootMesh.getSectionMesh().hasRenderableLayers()){if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("LAKE_FOREGROUND_MESH_PENDING stage={} bed={}",stage,bed);}return;}
                        int loaded=0,rendered=0;
                        for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                            int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var nearby=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(nearby==null)continue;loaded++;
                            int x=cx*16+8,z=cz*16+8,y=nearby.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var mesh=area.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                        }
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("LAKE_VIEW_READY stage={} loaded={} rendered={}",stage,loaded,rendered);}
                        if(loaded<121||rendered<25){settled=0;return;}if(++settled<100)return;settled=0;boolean under=stage==101;stage=under?107:106;String file=under?"open-inland-lake-underwater.png":"open-inland-lake-surface.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Lake screenshot missing");stage=under?102:101;ClientPlayNetworking.send(new RealisticProbe(under?80:79));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage>=90&&stage<=98){
                        if(stage==90)client.gui.toastManager().clear();
                        if(stage==90){var candidate=Boolean.getBoolean("blockbranch.tundraFescueCloseup")?client.player.blockPosition().offset(0,-2,3):client.player.blockPosition().below(8);if(!level.getBiome(candidate).is(Blockbranch.id("cold_dwarf_shrub_tundra"))||!level.getBlockState(candidate.above()).is(MeadowContent.MEADOW_FESCUE))return;tundraBed=candidate;}
                        var bed=tundraBed;if(bed==null)return;var pos=bed.above();var fescue=MeadowContent.MEADOW_FESCUE;
                        if(stage>=91){
                            var state=level.getBlockState(pos);var berry=level.getBlockState(tundraBerry);boolean ready=switch(stage){
                                case 91->state.is(fescue)&&state.getValue(MeadowFescueBlock.CROPPED);
                                case 92->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED);
                                case 93->level.getBlockState(bed).is(Blocks.STONE)&&state.isAir();
                                case 94->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED)&&berry.is(Blocks.SWEET_BERRY_BUSH)&&berry.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==1;
                                case 95->berry.is(Blocks.SWEET_BERRY_BUSH)&&berry.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==3;
                                case 96->berry.is(Blocks.SWEET_BERRY_BUSH)&&berry.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==1;
                                case 97->berry.isAir()&&level.getBlockState(tundraBerry.below()).is(Blocks.STONE);
                                default->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED)&&berry.is(Blocks.SWEET_BERRY_BUSH)&&berry.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==1;
                            };if(!ready)return;if(++settled<15)return;settled=0;
                            if(Boolean.getBoolean("blockbranch.tundraFescueCloseup")&&(stage==91||stage==92)){
                                int capturedStage=stage;String file=stage==91?"fescue-cropped.png":"fescue-regrown.png";stage=99;
                                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                                    try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Fescue state screenshot missing");stage=capturedStage+1;ClientPlayNetworking.send(new RealisticProbe(stage-21));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                                }));return;
                            }
                            if(stage<98){stage++;ClientPlayNetworking.send(new RealisticProbe(stage-21));return;}
                            var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();if(level.getBlockState(bed)!=base.state(bed.getX(),bed.getY(),bed.getZ()))throw new AssertionError("Tundra fixture soil was not restored");
                            Blockbranch.LOGGER.info("TUNDRA_CLIENT_PASS {}",bed);settled=0;stage=100;ClientPlayNetworking.send(new RealisticProbe(78));return;
                        }
                        var view=client.levelRenderer.viewArea();if(view==null)return;int loaded=0,rendered=0;
                        for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                            int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var nearby=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(nearby==null)continue;loaded++;
                            int x=cx*16+8,z=cz*16+8,y=nearby.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;var mesh=view.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                        }
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("TUNDRA_VIEW_READY loaded={} rendered={}",loaded,rendered);}
                        if(loaded<121||rendered<25){settled=0;return;}if(++settled<100)return;settled=0;
                        outer:for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++)for(int x=bed.getX()-64;x<=bed.getX()+64;x++){
                            var p=new BlockPos(x,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z)-1,z);if(level.getBlockState(p).is(Blocks.SWEET_BERRY_BUSH)&&level.getBiome(p).is(Blockbranch.id("cold_dwarf_shrub_tundra"))){tundraBerry=p;break outer;}
                        }if(tundraBerry==null)throw new AssertionError("Natural low tundra shrub missing on client");stage=99;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"cold-dwarf-shrub-tundra.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/cold-dwarf-shrub-tundra.png")))throw new AssertionError("Tundra screenshot missing");stage=91;ClientPlayNetworking.send(new RealisticProbe(70));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage>=80&&stage<=85){
                        if(stage==80){var candidate=client.player.blockPosition().offset(0,-2,7);if(!level.getBiome(candidate).is(Blockbranch.id("humid_subtropical_oak_fern_woodland"))||!level.getBlockState(candidate.above()).is(Blocks.OAK_LOG))return;humidBed=candidate;}
                        var bed=humidBed;if(bed==null)return;var log=HumidSmoke.testLog(bed);var leaf=HumidSmoke.testLeaf(bed);
                        if(stage==82){var state=level.getBlockState(leaf);if(!level.getBlockState(bed.above()).isAir()||!level.getBlockState(log).is(Blocks.OAK_LOG)||!state.is(Blocks.OAK_LEAVES)||state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1)return;if(++settled<20)return;settled=0;stage=83;ClientPlayNetworking.send(new RealisticProbe(66));return;}
                        if(stage==83){var state=level.getBlockState(leaf);if(!level.getBlockState(log).isAir()||(!state.isAir()&&(!state.is(Blocks.OAK_LEAVES)||state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7)))return;if(++settled<10)return;settled=0;stage=84;ClientPlayNetworking.send(new RealisticProbe(67));return;}
                        if(stage==84){if(!level.getBlockState(leaf).isAir())return;if(++settled<10)return;settled=0;stage=85;ClientPlayNetworking.send(new RealisticProbe(68));return;}
                        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null||terrain.base().canopy().size()==0)return;
                        if(stage==85){for(var pos:java.util.List.of(log,leaf,bed.above()))if(level.getBlockState(pos)!=terrain.base().state(pos.getX(),pos.getY(),pos.getZ()))return;if(++settled<20)return;Blockbranch.LOGGER.info("HUMID_CLIENT_PASS {}",bed);settled=0;stage=90;ClientPlayNetworking.send(new RealisticProbe(69));return;}
                        if(stage==81&&Math.abs(client.player.getY()-(bed.getY()+28))>1)return;
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(40))!=15||level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(2))!=0)return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null?null:area.getRenderSectionAt(bed.above(10));if(mesh==null||!mesh.getSectionMesh().hasRenderableLayers())return;
                        int loaded=0,rendered=0;
                        for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                            int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var nearby=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(nearby==null)continue;loaded++;
                            int x=cx*16+8,z=cz*16+8,y=nearby.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;
                            var section=area.getRenderSectionAt(new BlockPos(x,y,z));if(section!=null&&section.getSectionMesh().hasRenderableLayers())rendered++;
                        }
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("HUMID_VIEW_READY stage={} loaded={} rendered={}",stage,loaded,rendered);}
                        if(loaded<121||rendered<25){settled=0;return;}
                        if(++settled<100)return;settled=0;boolean ground=stage==80;stage=ground?86:87;String file=ground?"humid-subtropical-oak-fern-woodland.png":"humid-subtropical-oak-fern-canopy.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Humid woodland screenshot missing");stage=ground?81:82;ClientPlayNetworking.send(new RealisticProbe(ground?64:65));}
                            catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage==76||stage==77){
                        var bed=new BlockPos(-1050036,148,-1700033);boolean canopy=stage==77;
                        if(Math.abs(client.player.getX()-(bed.getX()+.5))>1||Math.abs(client.player.getY()-(bed.getY()+(canopy?22:2)))>1)return;
                        if(!level.getBiome(bed).is(Blockbranch.id("dappled_oak_birch_woodland")))return;
                        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null||terrain.base().canopy().size()==0)return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null?null:area.getRenderSectionAt(bed.above(10));if(mesh==null||!mesh.getSectionMesh().hasRenderableLayers())return;
                        if(++settled<60)return;settled=0;stage=canopy?79:78;String file=canopy?"woodland-canopy-after.png":"woodland-ground-after.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Matched woodland view missing");if(!canopy){stage=77;ClientPlayNetworking.send(new RealisticProbe(62));}else{Blockbranch.LOGGER.info("MATCHED_WOODLAND_VIEWS_PASS {}",bed);settled=0;stage=80;ClientPlayNetworking.send(new RealisticProbe(63));}}
                            catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage==74){
                        if(Boolean.getBoolean("blockbranch.terrainCollateralClientOnly")){client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Collateral view must use normal visibility");}
                        if(Boolean.getBoolean("blockbranch.terrainCollateralClientOnly")){
                            boolean sceneReady=collateralSceneReady(client);
                            if(System.nanoTime()>collateralConditionProgress){collateralConditionProgress=System.nanoTime()+10_000_000_000L;var diagnosticBed=client.player.blockPosition().below(24);Blockbranch.LOGGER.info("COLLATERAL_CONDITIONS scene={} queueEmpty={} bed={} biome={} ground={} sky={} settled={}",sceneReady,client.levelRenderer.hasRenderedAllSections(),diagnosticBed,level.getBiome(diagnosticBed).unwrapKey(),level.getBlockState(diagnosticBed),level.getBrightness(net.minecraft.world.level.LightLayer.SKY,diagnosticBed.above(32)),settled);}
                            if(!sceneReady){settled=0;return;}
                        }
                        // The slope witness can legitimately be vanilla Meadow after the
                        // terrain relief audit; native tests verify its exact stored identity.
                        var bed=client.player.blockPosition().below(24);boolean meadow=level.getBiome(bed).is(Blockbranch.id("rolling_lowland_forb_grass_meadow"))||level.getBiome(bed).is(net.minecraft.world.level.biome.Biomes.MEADOW);if(!meadow||!level.getBlockState(bed).is(Blocks.GRASS_BLOCK))return;
                        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null)return;int canopyChunks=0;
                        for(int z=-4;z<=4;z++)for(int x=-4;x<=4;x++){var nearby=((SparseColumn)level.getChunk((bed.getX()>>4)+x,(bed.getZ()>>4)+z)).blockbranch$terrain();if(nearby!=null&&nearby.base().canopy().size()>0)canopyChunks++;}
                        if(canopyChunks==0||level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(32))!=15)return;
                        var view=client.levelRenderer.viewArea();var mesh=view==null?null:view.getRenderSectionAt(bed);if(mesh==null||!mesh.getSectionMesh().hasRenderableLayers())return;
                        if(++settled<60)return;stage=75;int wooded=canopyChunks;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"moderate-slope-meadow-woodland.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/moderate-slope-meadow-woodland.png")))throw new AssertionError("Corrected slope screenshot missing");Blockbranch.LOGGER.info("SUPPORTED_SLOPE_CLIENT_PASS {} nearbyCanopyChunks={}",bed,wooded);settled=0;if(Boolean.getBoolean("blockbranch.terrainCollateralClientOnly")){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();}else{stage=76;ClientPlayNetworking.send(new RealisticProbe(61));}}
                            catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage>=68&&stage<=72){
                        var bed=client.player.blockPosition().below(6);if(!level.getBiome(bed).is(Blockbranch.id("continental_gravel_desert")))return;
                        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null)return;
                        var start=bed.above(6);var other=DesertSmoke.gravelStart(bed);var base=terrain.base();
                        int landingY=base.top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),other.getX(),other.getZ(),blockbranch.cubic.ColumnLimits.MAX_Y-1);var landing=new BlockPos(other.getX(),landingY,other.getZ());
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("DESERT_CLIENT_PROGRESS stage={} bed={} start={} control={} landingY={} landing={}",stage,level.getBlockState(bed),level.getBlockState(start),level.getBlockState(other),landingY,level.getBlockState(landing));}
                        if(stage==69){
                            if(!level.getBlockState(start).is(DesertContent.PAVEMENT)||!level.getBlockState(other).isAir()||!level.getBlockState(landing).is(Blocks.GRAVEL))return;
                            if(++settled<30)return;settled=0;stage=70;ClientPlayNetworking.send(new RealisticProbe(57));return;
                        }
                        if(stage==70){if(!level.getBlockState(start).isAir()||!level.getBlockState(bed.above()).is(Blocks.GRAVEL))return;if(++settled<20)return;settled=0;stage=71;ClientPlayNetworking.send(new RealisticProbe(58));return;}
                        if(stage==71){if(!level.getBlockState(bed).is(Blocks.GRAVEL))return;if(++settled<20)return;settled=0;stage=72;ClientPlayNetworking.send(new RealisticProbe(59));return;}
                        if(!level.getBlockState(bed).is(DesertContent.PAVEMENT))return;
                        if(stage==72){
                            for(var column:java.util.List.of(bed,other))for(int y=Math.min(bed.getY(),landingY)-1;y<=bed.getY()+7;y++)if(level.getBlockState(new BlockPos(column.getX(),y,column.getZ()))!=base.state(column.getX(),y,column.getZ()))return;
                            if(++settled<20)return;if(ColumnReceiveBatch.refreshed*4>=ColumnReceiveBatch.received)throw new AssertionError("Desert updates were not coalesced");
                            Blockbranch.LOGGER.info("GRAVEL_DESERT_CLIENT_PASS {}",bed);settled=0;stage=74;ClientPlayNetworking.send(new RealisticProbe(60));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above())!=15)return;
                        var view=client.levelRenderer.viewArea();var mesh=view==null?null:view.getRenderSectionAt(bed);if(mesh==null||!mesh.getSectionMesh().hasRenderableLayers())return;
                        var models=client.getModelManager().getBlockStateModelSet();var model=models.get(level.getBlockState(bed));if(model==models.missingModel()||!model.particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/desert_pavement_top")))throw new AssertionError("Desert pavement model/texture missing");
                        if(++settled<60)return;settled=0;stage=73;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"continental-gravel-desert.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/continental-gravel-desert.png")))throw new AssertionError("Gravel desert screenshot missing: "+message.getString());stage=69;ClientPlayNetworking.send(new RealisticProbe(56));}
                            catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage>=62&&stage<=66){
                        var bed=client.player.blockPosition().below(6);var pos=bed.above();var state=level.getBlockState(pos);
                        if(!level.getBiome(bed).is(Blockbranch.id("cold_continental_fescue_steppe")))return;
                        if(stage==65){if(!level.getBlockState(bed).is(Blocks.STONE)||!state.isAir())return;if(++settled<10)return;settled=0;stage=66;ClientPlayNetworking.send(new RealisticProbe(54));return;}
                        if(!level.getBlockState(bed).is(Blocks.GRASS_BLOCK)||!state.is(MeadowContent.MEADOW_FESCUE))return;
                        if(stage==63){if(!state.getValue(MeadowFescueBlock.CROPPED)||state.getShape(level,pos).max(net.minecraft.core.Direction.Axis.Y)>0.375)return;if(++settled<20)return;settled=0;stage=64;ClientPlayNetworking.send(new RealisticProbe(52));return;}
                        if(state.getValue(MeadowFescueBlock.CROPPED))return;
                        if(stage==64){if(state.getShape(level,pos).max(net.minecraft.core.Direction.Axis.Y)<0.9)return;if(++settled<20)return;settled=0;stage=65;ClientPlayNetworking.send(new RealisticProbe(53));return;}
                        if(stage==66){
                            var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null||state!=terrain.base().state(pos.getX(),pos.getY(),pos.getZ()))return;
                            if(++settled<20)return;if(ColumnReceiveBatch.refreshed*4>=ColumnReceiveBatch.received)throw new AssertionError("Cold-steppe updates were not coalesced");
                            Blockbranch.LOGGER.info("COLD_STEPPE_CLIENT_PASS {}",bed);if(coldSteppeOnly){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();return;}settled=0;stage=68;ClientPlayNetworking.send(new RealisticProbe(55));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos.above())!=15)return;
                        var view=client.levelRenderer.viewArea();var mesh=view==null?null:view.getRenderSectionAt(pos);if(mesh==null||!mesh.getSectionMesh().hasRenderableLayers())return;
                        var models=client.getModelManager().getBlockStateModelSet();var model=models.get(state);if(model==models.missingModel()||!model.particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/meadow_fescue")))throw new AssertionError("Cold-steppe fescue model/texture missing");
                        if(coldSteppeOnly){client.gui.toastManager().clear();if(!coldSteppeCapture.readySurface(client,"cold-steppe")){settled=0;return;}}
                        if(++settled<60)return;if(coldSteppeOnly)Blockbranch.LOGGER.info("COLD_STEPPE_CAPTURE_COVERAGE {}",coldSteppeCapture.diagnostic());settled=0;stage=67;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"cold-continental-fescue-steppe.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/cold-continental-fescue-steppe.png")))throw new AssertionError("Cold steppe screenshot missing: "+message.getString());stage=63;ClientPlayNetworking.send(new RealisticProbe(51));}
                            catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage==60 || stage==61) {
                        var bed=client.player.blockPosition().below(24);var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();if(terrain==null)return;
                        var biome=chunk.getNoiseBiome(bed.getX()>>2,0,bed.getZ()>>2);
                        // The unchanged upland altitude-hole witness is refined to vanilla Meadow.
                        // Match BoundaryCorrectionSmoke while retaining the lowland identity guard.
                        boolean montane=stage==61&&biome.is(net.minecraft.world.level.biome.Biomes.MEADOW);
                        if(!montane&&!biome.is(Blockbranch.id("dappled_oak_birch_woodland"))&&!biome.is(Blockbranch.id("rolling_lowland_forb_grass_meadow")))return;
                        for(int qy:new int[]{-3000,0,bed.getY()>>2,2000})if(!chunk.getNoiseBiome(bed.getX()>>2,qy,bed.getZ()>>2).equals(biome))throw new AssertionError("Boundary biome differs across stored heights at "+bed);
                        int e=bed.getY()+1-blockbranch.realistic.TerrainShape.SEA_LEVEL;
                        if(stage==60 ? e<0 || e>=20 : e<=900)return;
                        if(level.getBlockState(bed)!=terrain.base().state(bed.getX(),bed.getY(),bed.getZ()) || level.getBlockState(bed.below())!=Blocks.DIRT.defaultBlockState())return;
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(40))!=15)return;
                        var view=client.levelRenderer.viewArea();var mesh=view==null ? null : view.getRenderSectionAt(bed);if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers())return;
                        if(++settled<40)return;settled=0;
                        if(stage==60){stage=61;ClientPlayNetworking.send(new RealisticProbe(48));return;}
                        if(ColumnReceiveBatch.refreshed*4>=ColumnReceiveBatch.received)throw new AssertionError("Warm/boundary updates were not coalesced");
                        if(boundaryOnly){
                            if(!montane)throw new AssertionError("Original upland witness must retain its vanilla Meadow refinement");
                            if(!bed.equals(new BlockPos(-884000,1303,-1668000)))throw new AssertionError("Original upland boundary witness moved: "+bed);
                            Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");
                            ClientPlayNetworking.send(new RealisticProbe(49));Blockbranch.LOGGER.info("BOUNDARY_CLIENT_LOWLAND_UPLAND_MEADOW_PASS bed={} biome={}",bed,biome);finished=true;client.stop();return;
                        }
                        ClientPlayNetworking.send(new RealisticProbe(49));Blockbranch.LOGGER.info("WARM_BOUNDARY_CLIENT_PASS");stage=62;ClientPlayNetworking.send(new RealisticProbe(50));return;
                    }
                    if(stage>=52 && stage<=57) {
                        var bed=stage==52 ? client.player.blockPosition().offset(0,-2,6) : client.player.blockPosition().below(24);
                        if(!level.getBiome(bed).is(Blockbranch.id("warm_subhumid_oak_pine_woodland")))return;
                        var log=WarmSmoke.testLog(bed);var leaf=WarmSmoke.testLeaf(bed);
                        if(stage==54) {
                            if(!level.getBlockState(bed.above()).isAir() || !level.getBlockState(log).is(Blocks.SPRUCE_LOG))return;
                            var state=level.getBlockState(leaf);if(!state.is(Blocks.SPRUCE_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1 || state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT))return;
                            if(++settled<20)return;settled=0;stage=55;ClientPlayNetworking.send(new RealisticProbe(44));return;
                        }
                        if(stage==55) {
                            var state=level.getBlockState(leaf);if(!level.getBlockState(log).isAir() || (!state.isAir() && (!state.is(Blocks.SPRUCE_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7)))return;
                            if(++settled<10)return;settled=0;stage=56;ClientPlayNetworking.send(new RealisticProbe(45));return;
                        }
                        if(stage==56) {if(!level.getBlockState(leaf).isAir())return;if(++settled<10)return;settled=0;stage=57;ClientPlayNetworking.send(new RealisticProbe(46));return;}
                        if(!level.getBlockState(bed.above()).is(Blocks.SPRUCE_LOG))return;
                        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null)return;
                        if(stage==57) {
                            for(var p:java.util.List.of(log,leaf,bed.above()))if(level.getBlockState(p)!=terrain.base().state(p.getX(),p.getY(),p.getZ()))return;
                            if(++settled<20)return;settled=0;stage=60;ClientPlayNetworking.send(new RealisticProbe(47));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(32))!=15 || level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(2))!=0)return;
                        var view=client.levelRenderer.viewArea();var mesh=view==null ? null : view.getRenderSectionAt(bed.above(10));if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers())return;
                        if(++settled<60)return;settled=0;boolean under=stage==52;stage=under ? 58 : 59;
                        String file=under ? "warm-subhumid-oak-pine-woodland.png" : "warm-subhumid-oak-pine-canopy.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Warm woodland screenshot missing: "+message.getString());
                                stage=under ? 53 : 54;ClientPlayNetworking.send(new RealisticProbe(under ? 42 : 43));
                            }catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                        }));return;
                    }
                    if(stage>=42 && stage<=49) {
                        var bed=stage==42 ? client.player.blockPosition().offset(0,-2,6) : client.player.blockPosition().below(26);
                        if(!level.getBiome(bed).is(Blockbranch.id("boreal_spruce_birch_woodland"))) return;
                        var log=BorealSmoke.testLog(bed);var leaf=BorealSmoke.testLeaf(bed);
                        if(stage==44) {
                            if(!level.getBlockState(bed.above()).isAir() || !level.getBlockState(log).is(Blocks.SPRUCE_LOG)) return;
                            var state=level.getBlockState(leaf);
                            if(!state.is(Blocks.SPRUCE_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1 || state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)) return;
                            if(++settled<20) return;settled=0;stage=45;ClientPlayNetworking.send(new RealisticProbe(36));return;
                        }
                        if(stage==45) {
                            var state=level.getBlockState(leaf);
                            if(!level.getBlockState(log).isAir() || (!state.isAir() && (!state.is(Blocks.SPRUCE_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7))) return;
                            if(++settled<10) return;settled=0;stage=46;ClientPlayNetworking.send(new RealisticProbe(37));return;
                        }
                        if(stage==46) {
                            if(!level.getBlockState(leaf).isAir()) return;
                            if(++settled<10) return;settled=0;stage=47;ClientPlayNetworking.send(new RealisticProbe(38));return;
                        }
                        if(!level.getBlockState(bed.above()).is(Blocks.SPRUCE_LOG)) return;
                        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null) return;
                        if(stage>=47) {
                            var berry=BorealSmoke.berry(level,bed);var state=level.getBlockState(berry);
                            if(!state.is(Blocks.SWEET_BERRY_BUSH) || state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)!=(stage==47 ? 1 : 3)) return;
                            if(++settled<10) return;settled=0;
                            if(stage==47) { stage=48;ClientPlayNetworking.send(new RealisticProbe(39));return; }
                            if(stage==48) { stage=49;ClientPlayNetworking.send(new RealisticProbe(40));return; }
                            for(var pos:java.util.List.of(log,leaf,bed.above())) if(level.getBlockState(pos)!=terrain.base().state(pos.getX(),pos.getY(),pos.getZ())) return;
                            Blockbranch.LOGGER.info("BOREAL_WOODLAND_CLIENT_PASS {}",bed);
                            if(borealOnly){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();return;}
                            stage=52;ClientPlayNetworking.send(new RealisticProbe(41));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(26))!=15 || level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(2))!=0) return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null ? null : area.getRenderSectionAt(bed.above(10));
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        if(borealOnly){
                            if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();
                            if(!borealCapture.readySurface(client,"boreal-"+stage)){settled=0;return;}
                        }
                        if(++settled<60) return;settled=0;
                        if(borealOnly){
                            Blockbranch.LOGGER.info("BOREAL_CAPTURE_COVERAGE {}",borealCapture.diagnostic());
                            if(Boolean.getBoolean("blockbranch.borealSurfaceCapture"))verifyBorealFloor(client,bed);
                        }
                        boolean under=stage==42;stage=under ? 50 : 51;
                        String file=under ? "boreal-spruce-birch-woodland.png" : "boreal-spruce-birch-canopy.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))) throw new AssertionError("Boreal screenshot missing: "+message.getString());
                                stage=under ? 43 : 44;ClientPlayNetworking.send(new RealisticProbe(under ? 34 : 35));
                            } catch(Throwable failure) { finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop(); }
                        }));return;
                    }
                    if(stage>=36 && stage<=40) {
                        if(Boolean.getBoolean("blockbranch.steppeArtReview"))client.gui.toastManager().clear();
                        var bed=drySteppeOnly&&drySteppeBed!=null?drySteppeBed:Boolean.getBoolean("blockbranch.steppeArtReview")?client.player.blockPosition().offset(0,-2,3):client.player.blockPosition().below(6);
                        if(drySteppeOnly)client.gui.toastManager().clear();var pos=bed.above();var state=level.getBlockState(pos);
                        if(!level.getBiome(bed).is(Blockbranch.id("dry_continental_scrub_steppe"))) return;
                        if(drySteppeOnly&&drySteppeBed==null)drySteppeBed=bed;
                        if(stage==39) {
                            if(!level.getBlockState(bed).is(Blocks.STONE) || !state.isAir()) return;
                            if(++settled<10) return;
                            settled=0;stage=40;ClientPlayNetworking.send(new RealisticProbe(32));return;
                        }
                        if(!level.getBlockState(bed).is(Blocks.COARSE_DIRT) || !state.is(SteppeContent.SILVER_SHRUB)) return;
                        if(stage==37) {
                            if(!state.getValue(SteppeShrubBlock.CLIPPED) || state.getShape(level,pos).max(net.minecraft.core.Direction.Axis.Y)>0.25) return;
                            if(++settled<20) return;
                            if(Boolean.getBoolean("blockbranch.steppeArtReview")){
                                settled=0;stage=41;String file="shrub-clipped.png";
                                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                                    try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Shrub state screenshot missing");stage=38;ClientPlayNetworking.send(new RealisticProbe(30));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                                }));return;
                            }
                            settled=0;stage=38;ClientPlayNetworking.send(new RealisticProbe(30));return;
                        }
                        if(state.getValue(SteppeShrubBlock.CLIPPED)) return;
                        if(stage==38) {
                            if(state.getShape(level,pos).max(net.minecraft.core.Direction.Axis.Y)<0.8) return;
                            if(++settled<20) return;
                            if(Boolean.getBoolean("blockbranch.steppeArtReview")){
                                settled=0;stage=41;String file="shrub-regrown.png";
                                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                                    try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Shrub state screenshot missing");stage=39;ClientPlayNetworking.send(new RealisticProbe(31));}catch(Throwable failure){finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop();}
                                }));return;
                            }
                            settled=0;stage=39;ClientPlayNetworking.send(new RealisticProbe(31));return;
                        }
                        if(stage==40) {
                            var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();
                            if(terrain==null || state!=terrain.base().state(pos.getX(),pos.getY(),pos.getZ())) return;
                            if(++settled<20) return;
                            Blockbranch.LOGGER.info("DRY_STEPPE_CLIENT_PASS {}",bed);
                            if(drySteppeOnly||Boolean.getBoolean("blockbranch.steppeArtReview")){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();return;}
                            settled=0;stage=42;ClientPlayNetworking.send(new RealisticProbe(33));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos.above())!=15) return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null ? null : area.getRenderSectionAt(pos);
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        var models=client.getModelManager().getBlockStateModelSet();var model=models.get(state);
                        if(model==models.missingModel() || !model.particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/silver_steppe_shrub_leaves"))) throw new AssertionError("Missing silver shrub model/texture");
                        if(drySteppeOnly){if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)||!drySteppeCapture.readySurface(client,"dry-steppe-ground"))return;Blockbranch.LOGGER.info("DRY_STEPPE_CAPTURE_COMPLETE {}",drySteppeCapture.diagnostic());}
                        if(++settled<60) return;
                        settled=0;stage=41;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"dry-continental-scrub-steppe.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/dry-continental-scrub-steppe.png"))) throw new AssertionError("Steppe screenshot missing: "+message.getString());
                                stage=drySteppeOnly?137:37;ClientPlayNetworking.send(new RealisticProbe(drySteppeOnly?123:29));
                            } catch(Throwable failure) { finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop(); }
                        }));
                        return;
                    }
                    if(stage>=28 && stage<=35) {
                        var bed=stage==28 ? client.player.blockPosition().offset(0,-2,6) : client.player.blockPosition().below(22);
                        if(!level.getBiome(bed).is(Blockbranch.id("dappled_oak_birch_woodland"))) return;
                        var log=WoodlandSmoke.testLog(bed);var leaf=WoodlandSmoke.testLeaf(bed);
                        if(stage==30) {
                            if(!level.getBlockState(bed.above()).isAir() || !level.getBlockState(log).is(Blocks.OAK_LOG)) return;
                            var state=level.getBlockState(leaf);
                            if(!state.is(Blocks.OAK_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1 || state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)) return;
                            if(++settled<20) return;
                            settled=0;stage=31;ClientPlayNetworking.send(new RealisticProbe(25));return;
                        }
                        if(stage==31) {
                            var state=level.getBlockState(leaf);
                            if(!level.getBlockState(log).isAir() || !state.is(Blocks.OAK_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7) return;
                            if(++settled<10) return;
                            settled=0;stage=32;ClientPlayNetworking.send(new RealisticProbe(26));return;
                        }
                        if(stage==32) {
                            if(!level.getBlockState(leaf).isAir() || !level.getBlockState(bed.above()).isAir()) return;
                            if(++settled<10) return;
                            settled=0;stage=35;ClientPlayNetworking.send(new RealisticProbe(27));return;
                        }
                        if(!level.getBlockState(bed.above()).is(Blocks.OAK_LOG)) return;
                        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null) return;
                        if(stage==35) {
                            for(var pos:java.util.List.of(log,leaf,bed.above())) if(level.getBlockState(pos)!=terrain.base().state(pos.getX(),pos.getY(),pos.getZ())) return;
                            if(++settled<20) return;
                            Blockbranch.LOGGER.info("OAK_BIRCH_WOODLAND_CLIENT_PASS {}",bed);
                            settled=0;stage=36;ClientPlayNetworking.send(new RealisticProbe(28));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(24))!=15 || level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(2))!=0) return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null ? null : area.getRenderSectionAt(bed.above(10));
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        if(++settled<60) return;
                        settled=0;boolean under=stage==28;stage=under ? 33 : 34;
                        String file=under ? "dappled-oak-birch-woodland.png" : "dappled-oak-birch-canopy.png";
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))) throw new AssertionError("Woodland screenshot missing: "+message.getString());
                                stage=under ? 29 : 30;ClientPlayNetworking.send(new RealisticProbe(under ? 23 : 24));
                            } catch(Throwable failure) { finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop(); }
                        }));
                        return;
                    }
                    if(stage>=22 && stage<=26) {
                        var bed=client.player.blockPosition().below(6);var pos=bed.above();var state=level.getBlockState(pos);
                        if(!level.getBiome(bed).is(Blockbranch.id("rolling_lowland_forb_grass_meadow"))) return;
                        if(stage==25) {
                            if(!level.getBlockState(bed).is(Blocks.STONE) || !state.isAir()) return;
                            if(++settled<10) return;
                            settled=0;stage=26;ClientPlayNetworking.send(new RealisticProbe(21));return;
                        }
                        if(!level.getBlockState(bed).is(Blocks.GRASS_BLOCK) || !state.is(MeadowContent.MEADOW_FESCUE)) return;
                        if(stage==23) {
                            if(!state.getValue(MeadowFescueBlock.CROPPED)) return;
                            var models=client.getModelManager().getBlockStateModelSet();
                            if(!models.get(state).particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/meadow_fescue_cropped"))) throw new AssertionError("Missing cropped crown texture");
                            if(++settled<20) return;
                            settled=0;stage=24;ClientPlayNetworking.send(new RealisticProbe(19));return;
                        }
                        if(state.getValue(MeadowFescueBlock.CROPPED)) return;
                        if(stage==24) {
                            if(++settled<20) return;
                            settled=0;stage=25;ClientPlayNetworking.send(new RealisticProbe(20));return;
                        }
                        if(stage==26) {
                            var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();
                            if(terrain==null || state!=terrain.base().state(pos.getX(),pos.getY(),pos.getZ())) return;
                            if(++settled<20) return;
                            Blockbranch.LOGGER.info("LOWLAND_MEADOW_CLIENT_PASS {}",bed);
                            settled=0;stage=28;ClientPlayNetworking.send(new RealisticProbe(22));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos.above())!=15) return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null ? null : area.getRenderSectionAt(pos);
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        var models=client.getModelManager().getBlockStateModelSet();var model=models.get(state);
                        if(model==models.missingModel() || !model.particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/meadow_fescue"))) throw new AssertionError("Missing meadow fescue model/texture");
                        if(++settled<60) return;
                        settled=0;stage=27;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"rolling-lowland-forb-grass-meadow.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/rolling-lowland-forb-grass-meadow.png"))) throw new AssertionError("Meadow screenshot missing: "+message.getString());
                                stage=23;ClientPlayNetworking.send(new RealisticProbe(18));
                            } catch(Throwable failure) { finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop(); }
                        }));
                        return;
                    }
                    if(stage>=15 && stage<=18) {
                        var bed=client.player.blockPosition().below(12);
                        if(!level.getBlockState(bed).is(SandyShoreContent.ROOT_BOUND_SAND) || !level.getBlockState(bed.above()).is(SandyShoreContent.BEACH_GRASS)) return;
                        if(!level.getBiome(bed).is(Blockbranch.id("low_gradient_sandy_shore"))) return;
                        var root=SandyShoreSmoke.testRoot(bed);var control=SandyShoreSmoke.controlStart(bed);
                        var terrain=((SparseColumn)level.getChunkAt(root)).blockbranch$terrain();
                        if(terrain==null) return;
                        var landing=new BlockPos(root.getX(),terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),root.getX(),root.getZ(),blockbranch.cubic.ColumnLimits.MAX_Y-1),root.getZ());
                        var controlLanding=new BlockPos(control.getX(),terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),control.getX(),control.getZ(),blockbranch.cubic.ColumnLimits.MAX_Y-1),control.getZ());
                        if(System.nanoTime()>nextProgress) {
                            nextProgress=System.nanoTime()+10_000_000_000L;
                            Blockbranch.LOGGER.info("SANDY_SHORE_CLIENT_PROGRESS stage={} root={} upper={} grass={} control={} controlLanding={} landing={} landingUpper={}",stage,level.getBlockState(root),level.getBlockState(root.above()),level.getBlockState(root.above(2)),level.getBlockState(control),level.getBlockState(controlLanding),level.getBlockState(landing),level.getBlockState(landing.above()));
                        }
                        if(stage==16) {
                            if(!level.getBlockState(root).is(SandyShoreContent.ROOT_BOUND_SAND) || !level.getBlockState(root.above()).is(SandyShoreContent.ROOT_BOUND_SAND)
                                || !level.getBlockState(root.above(2)).is(SandyShoreContent.BEACH_GRASS) || !level.getBlockState(control).isAir() || !level.getBlockState(controlLanding).is(Blocks.SAND)) return;
                            if(++settled<40) return;
                            settled=0;stage=17;ClientPlayNetworking.send(new RealisticProbe(15));return;
                        }
                        if(stage==17) {
                            if(!level.getBlockState(root).isAir() || !level.getBlockState(root.above()).isAir() || !level.getBlockState(root.above(2)).isAir()
                                || !level.getBlockState(landing).is(Blocks.SAND) || !level.getBlockState(landing.above()).is(Blocks.SAND)) return;
                            if(++settled<20) return;
                            settled=0;stage=18;ClientPlayNetworking.send(new RealisticProbe(16));return;
                        }
                        if(stage==18) {
                            for(var column:java.util.List.of(root,control)) {
                                int bottom=terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),column.getX(),column.getZ(),blockbranch.cubic.ColumnLimits.MAX_Y-1)-3;
                                for(int y=bottom;y<=root.getY()+3;y++) if(level.getBlockState(new BlockPos(column.getX(),y,column.getZ()))!=terrain.base().state(column.getX(),y,column.getZ())) return;
                            }
                            if(++settled<20) return;
                            Blockbranch.LOGGER.info("LOW_GRADIENT_SANDY_SHORE_CLIENT_PASS {}",bed);
                            settled=0;stage=22;ClientPlayNetworking.send(new RealisticProbe(17));return;
                        }
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(2))!=15) return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null ? null : area.getRenderSectionAt(bed);
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        var models=client.getModelManager().getBlockStateModelSet();
                        for(var state:java.util.List.of(level.getBlockState(bed),level.getBlockState(bed.above()))) {
                            var model=models.get(state);var expected=state.is(SandyShoreContent.BEACH_GRASS) ? "block/beach_grass" : "block/root_bound_sand";
                            if(model==models.missingModel() || !model.particleMaterial().sprite().contents().name().equals(Blockbranch.id(expected))) throw new AssertionError("Missing sandy shore model/texture "+expected);
                        }
                        if(++settled<60) return;
                        settled=0;stage=20;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"low-gradient-sandy-shore.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/low-gradient-sandy-shore.png"))) throw new AssertionError("Shore screenshot missing: "+message.getString());
                                stage=16;ClientPlayNetworking.send(new RealisticProbe(14));
                            } catch(Throwable failure) { finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop(); }
                        }));
                        return;
                    }
                    if(stage>=11 && stage<=13) {
                        var bed=client.player.blockPosition().below(8);
                        if(!level.getBlockState(bed).is(RiverMarginContent.PACKED_RIVER_SHINGLE)) return;
                        if(!level.getBiome(bed).is(Blockbranch.id("gravel_bed_river_margin"))) return;
                        var start=RiverMarginSmoke.gravelStart(bed);
                        var chunk=level.getChunkAt(start); var terrain=((SparseColumn)chunk).blockbranch$terrain();
                        if(terrain==null) return;
                        var landing=new BlockPos(start.getX(),terrain.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),start.getX(),start.getZ(),blockbranch.cubic.ColumnLimits.MAX_Y-1),start.getZ());
                        if(stage==12) {
                            if(!level.getBlockState(bed.above(6)).is(RiverMarginContent.PACKED_RIVER_SHINGLE)
                                || !level.getBlockState(start).isAir() || !level.getBlockState(landing).is(Blocks.GRAVEL)) return;
                            if(++settled<40) return;
                            settled=0; stage=13; ClientPlayNetworking.send(new RealisticProbe(12)); return;
                        }
                        if(stage==13) {
                            if(!level.getBlockState(bed.above(6)).isAir()) return;
                            for(int y=landing.getY();y<=start.getY();y++)
                                if(level.getBlockState(new BlockPos(start.getX(),y,start.getZ()))!=terrain.base().state(start.getX(),y,start.getZ())) return;
                            if(++settled<20) return;
                            Blockbranch.LOGGER.info("GRAVEL_RIVER_MARGIN_CLIENT_PASS {}",bed);
                            settled=0;stage=15;ClientPlayNetworking.send(new RealisticProbe(13));return;
                        }
                        if(!level.getFluidState(bed.above()).isSource()) return;
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(6))!=15) return;
                        var area=client.levelRenderer.viewArea();var mesh=area==null ? null : area.getRenderSectionAt(bed);
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        var models=client.getModelManager().getBlockStateModelSet();var model=models.get(level.getBlockState(bed));
                        if(model==models.missingModel() || !model.particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/packed_river_shingle"))) throw new AssertionError("Missing packed river shingle model/texture");
                        if(++settled<60) return;
                        settled=0;stage=14;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"gravel-bed-river-margin.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/gravel-bed-river-margin.png"))) throw new AssertionError("River screenshot missing: "+message.getString());
                                stage=12;ClientPlayNetworking.send(new RealisticProbe(11));
                            } catch(Throwable failure) { finished=true;Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure);client.stop(); }
                        }));
                        return;
                    }
                    if(stage>=7 && stage<=9) {
                        var bed=client.player.blockPosition().below(8);
                        if(!level.getBlockState(bed).is(Blocks.MUD)) return;
                        if(stage==8) {
                            for(int y=1;y<=reedHeight;y++) {
                                var state=level.getBlockState(bed.above(y));
                                if(state.is(ReedbedContent.LAKE_REED)) return;
                                if(y<=reedWaterDepth && !state.getFluidState().isSource()) return;
                                if(y>reedWaterDepth && !state.isAir()) return;
                            }
                            if(++settled<10) return;
                            settled=0; stage=9; ClientPlayNetworking.send(new RealisticProbe(9)); return;
                        }
                        if(!level.getBlockState(bed.above()).is(ReedbedContent.LAKE_REED)) return;
                        if(!level.getBiome(bed).is(Blockbranch.id("inland_lake_reedbed"))) return;
                        int height=0,water=0;
                        for(int y=1;y<=4 && level.getBlockState(bed.above(y)).is(ReedbedContent.LAKE_REED);y++) {
                            height=y;
                            if(level.getBlockState(bed.above(y)).getValue(LakeReedBlock.WATERLOGGED)) water++;
                        }
                        if(height<2 || water<1 || height<=water || !level.getBlockState(bed.above(height)).getValue(LakeReedBlock.TIP)) return;
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,bed.above(height+1))!=15) return;
                        var area=client.levelRenderer.viewArea(); var mesh=area==null ? null : area.getRenderSectionAt(bed.above(height));
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        var models=client.getModelManager().getBlockStateModelSet();
                        var model=models.get(level.getBlockState(bed.above(height)));
                        if(model==models.missingModel() || !model.particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/lake_reed_tip"))) throw new AssertionError("Missing reed head model/texture");
                        if(reedOnly&&!reedCapture.ready(client,"inland-lake-reedbed")){settled=0;return;}
                        if(++settled<40) return;
                        if(stage==9) {
                            Blockbranch.LOGGER.info("INLAND_LAKE_REEDBED_CLIENT_PASS reedbed={} stalkHeight={} submerged={}",bed,height,water);
                            if(reedOnly){Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");finished=true;client.stop();return;}
                            settled=0;stage=11;ClientPlayNetworking.send(new RealisticProbe(10));return;
                        }
                        reedHeight=height; reedWaterDepth=water; settled=0; stage=10;
                        if(reedOnly)Blockbranch.LOGGER.info("REED_CAPTURE_READY {}",reedCapture.diagnostic());
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"inland-lake-reedbed.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/inland-lake-reedbed.png"))) throw new AssertionError("Reedbed screenshot missing: "+message.getString());
                                stage=8; ClientPlayNetworking.send(new RealisticProbe(8));
                            } catch(Throwable failure) { finished=true; Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure); client.stop(); }
                        }));
                        return;
                    }
                    if(stage==4 || stage==5) {
                        if(Math.abs(client.player.getX()-1401431)<2) return;
                        client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                        if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Scree captures use normal visibility");
                        var scree=client.player.blockPosition().below(32);
                        if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+10_000_000_000L;Blockbranch.LOGGER.info("SCREE_CAPTURE_WAIT stage={} pose={} yaw={} pitch={} bed={} block={} biome={} coverage={}",stage,client.player.position(),client.player.getYRot(),client.player.getXRot(),scree,level.getBlockState(scree),level.getBiome(scree).unwrapKey(),screeCapture.diagnostic());}
                        if(!level.getBlockState(scree).is(ScreeContent.LOOSE_SCREE)) return;
                        if(!level.getBiome(scree).is(Blockbranch.id("alpine_scree_slope"))) return;
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,scree.above(2))!=15) return;
                        var area=client.levelRenderer.viewArea();
                        var mesh=area==null ? null : area.getRenderSectionAt(scree);
                        if(mesh==null || !mesh.getSectionMesh().hasRenderableLayers()) return;
                        var models=client.getModelManager().getBlockStateModelSet();
                        var model=models.get(ScreeContent.LOOSE_SCREE.defaultBlockState());
                        if(model==models.missingModel() || !model.particleMaterial().sprite().contents().name().equals(Blockbranch.id("block/loose_scree")))
                            throw new AssertionError("Scree block model or texture is missing");
                        if(stage==4) {
                            if(++settled<40) return;
                            settled=0; stage=5; ClientPlayNetworking.send(new RealisticProbe(5)); return;
                        }
                        if(!level.getBlockState(scree.above(6)).isAir() || !level.getBlockState(scree.above()).is(ScreeContent.LOOSE_SCREE)) return;
                        if(++settled<40) return;
                        if(!screeCapture.ready(client,"alpine-scree"))return;
                        screeBed=scree;
                        Blockbranch.LOGGER.info("SCREE_CAPTURE_COVERAGE {}",screeCapture.diagnostic());
                        ClientPlayNetworking.send(new RealisticProbe(6));
                        if(ColumnReceiveBatch.refreshed*4>=ColumnReceiveBatch.received) throw new AssertionError("Window refreshes were not coalesced");
                        stage=6;
                        net.minecraft.client.Screenshot.grab(client.gameDirectory,"alpine-scree.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                            try {
                                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/alpine-scree.png"))) throw new AssertionError("Scree screenshot was not saved: "+message.getString());
                                Blockbranch.LOGGER.info("ALPINE_SCREE_CLIENT_PASS alpineScree={} surfaceChunks={} updates={} columnRefreshes={} verticalTrips={} isolated={}",scree,surfaceChunks,ColumnReceiveBatch.received,ColumnReceiveBatch.refreshed,screeOnly?0:3,screeOnly);
                                settled=0;if(screeOnly){stage=136;return;} stage=7; ClientPlayNetworking.send(new RealisticProbe(7));
                            } catch(Throwable failure) { finished=true; Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure); client.stop(); }
                        }));
                        return;
                    }
                    if(System.nanoTime()>nextProgress) {
                        nextProgress=System.nanoTime()+10_000_000_000L;
                        Blockbranch.LOGGER.info("REALISTIC_CLIENT_PROGRESS stage={} y={} loadedChunks={} updates={} refreshes={}",stage,client.player.getY(),level.getChunkSource().getLoadedChunksCount(),ColumnReceiveBatch.received,ColumnReceiveBatch.refreshed);
                        if(stage==1 || stage==3) {
                            var v=client.levelRenderer.viewArea(); var r=v==null ? null : v.getRenderSectionAt(RealisticProbe.LAMP);
                            Blockbranch.LOGGER.info("REALISTIC_ROOM_STATE lamp={} room={} sky={} block={} chest={} entity={} mesh={}",level.getBlockState(RealisticProbe.LAMP),level.getBlockState(RealisticProbe.ROOM),level.getBrightness(net.minecraft.world.level.LightLayer.SKY,RealisticProbe.ROOM),level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,RealisticProbe.LAMP.above()),level.getBlockState(RealisticProbe.CHEST),level.getBlockEntity(RealisticProbe.CHEST)!=null,r!=null && r.getSectionMesh().hasRenderableLayers());
                        }
                    }
                    boolean underground=stage==1 || stage==3;
                    if(Math.abs(client.player.getY()-(underground ? -4096 : 272))>1) return;
                    var pos=underground ? RealisticProbe.LAMP : new BlockPos(1401431,223,-1045106);
                    var chunk=level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,ChunkStatus.FULL,false);
                    if(chunk==null) return;
                    var sparse=(SparseColumn)chunk; var terrain=sparse.blockbranch$terrain();
                    if(terrain==null) return;
                    if(terrain.lit.size()>80 || sparse.blockbranch$sections().size()>8) throw new AssertionError("Client retained unused vertical layers");
                    if(underground) {
                        if(!level.getBlockState(pos).is(Blocks.GLOWSTONE) || !level.getBlockState(RealisticProbe.ROOM).isAir()) return;
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos.above())<14) return;
                        if(level.getBrightness(net.minecraft.world.level.LightLayer.SKY,RealisticProbe.ROOM)!=0) return;
                        if(!level.getBlockState(RealisticProbe.CHEST).is(Blocks.CHEST) || level.getBlockEntity(RealisticProbe.CHEST)==null) return;
                        if(!terrain.resident(pos.getY()>>4)) throw new AssertionError("Edited room was not streamed into the new vertical window");
                    } else {
                        if(!level.getBlockState(pos).is(Blocks.WATER) || level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos.above())!=15) return;
                        if(level.getChunkSource().getLoadedChunksCount()<64) return;
                        if(stage==2 && terrain.resident(RealisticProbe.LAMP.getY()>>4)) {
                            if(++evictionWait>200) throw new AssertionError("Client retained the underground room outside its window after waiting for the asynchronous window update");
                            return;
                        }
                    }
                    var view=client.levelRenderer.viewArea();
                    if(view==null) return;
                    var rendered=view.getRenderSectionAt(pos);
                    if(rendered==null || !rendered.getSectionMesh().hasRenderableLayers()) return;
                    if(++settled<20) return;
                    settled=0;
                    if(stage==0) {
                        surfaceChunks=level.getChunkSource().getLoadedChunksCount(); stage=1;
                        ClientPlayNetworking.send(new RealisticProbe(1));
                    } else if(stage==1) {
                        stage=2; ClientPlayNetworking.send(new RealisticProbe(2));
                    } else if(stage==2) {
                        stage=3; ClientPlayNetworking.send(new RealisticProbe(3));
                    } else {
                        stage=4; ClientPlayNetworking.send(new RealisticProbe(4));
                    }
                } catch(Throwable failure) { finished=true; Blockbranch.LOGGER.error("REALISTIC_CLIENT_FAIL",failure); client.stop(); }
            }
        });
    }
    private static long collateralProgress,collateralConditionProgress;
    private static BlockPos fixtureCompileCamera;
    private static final java.util.Set<Long> fixtureCompiledSections = new java.util.HashSet<>();
    /** Same complete column/surface/canopy readiness used for the reported river comparison. */
    static boolean collateralSceneReady(net.minecraft.client.Minecraft client){
        var area=client.levelRenderer.viewArea();if(area==null)return false;
        var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
        int cameraX=client.player.blockPosition().getX()>>4,cameraZ=client.player.blockPosition().getZ()>>4;
        var missing = new java.util.ArrayList<BlockPos>();
        int required=0,loaded=0,surfaceRequired=0,surfaceReady=0,canopyRequired=0,canopyReady=0;
        for(int dz=-16;dz<=16;dz++)for(int dx=-16;dx<=16;dx++){
            if(dx*dx+dz*dz>256)continue;int cx=cameraX+dx,cz=cameraZ+dz;
            if(!frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,blockbranch.realistic.TerrainShape.FLOOR,cz*16,cx*16+16,blockbranch.realistic.TerrainShape.CEILING,cz*16+16)))continue;
            required++;var t=((SparseColumn)client.level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;
            for(int sy:t.base().canopy().sections()){
                if(!frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,sy*16,cz*16,cx*16+16,sy*16+16,cz*16+16)))continue;
                canopyRequired++;var pos=new BlockPos(cx*16+8,sy*16+8,cz*16+8);var mesh=area.getRenderSectionAt(pos);if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())canopyReady++;else missing.add(pos);
            }
            int x=cx*16+8,z=cz*16+8,y=t.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;
            if(frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,y-16,cz*16,cx*16+16,y+16,cz*16+16))){surfaceRequired++;var pos=new BlockPos(x,y,z);var mesh=area.getRenderSectionAt(pos);if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())surfaceReady++;else missing.add(pos);}
        }
        // Disposable fixed-camera captures can wait on sections never scheduled by
        // visibility traversal. Submit at most two native compilations per tick;
        // readiness still requires their actual uploaded meshes on a later frame.
        if((Boolean.getBoolean("blockbranch.terrainContourComparison")||Boolean.getBoolean("blockbranch.frozenRiverClientOnly")) && loaded==required) {
            var camera=client.player.blockPosition();
            if(!camera.equals(fixtureCompileCamera)){fixtureCompileCamera=camera;fixtureCompiledSections.clear();}
            var cache=new net.minecraft.client.renderer.chunk.RenderRegionCache();
            int submitted=0;
            for(var pos:missing) {
                long section=net.minecraft.core.SectionPos.asLong(pos.getX()>>4,pos.getY()>>4,pos.getZ()>>4);
                var mesh=area.getRenderSectionAt(pos);
                if(mesh==null || mesh.getSectionMesh()!=net.minecraft.client.renderer.chunk.CompiledSectionMesh.UNCOMPILED || fixtureCompiledSections.contains(section))continue;
                var region=cache.createRegion(client.level,section);
                if(region==null)continue;
                mesh.compileSync(region);fixtureCompiledSections.add(section);
                Blockbranch.LOGGER.info("FIXTURE_SECTION_COMPILE {}",pos);
                if(++submitted==2)break;
            }
        }
        if(System.nanoTime()>collateralProgress){collateralProgress=System.nanoTime()+10_000_000_000L;Blockbranch.LOGGER.info("COLLATERAL_VIEW_READY columns={}/{} surface={}/{} canopy={}/{}",loaded,required,surfaceReady,surfaceRequired,canopyReady,canopyRequired);}
        return loaded==required&&surfaceReady==surfaceRequired&&canopyReady==canopyRequired&&client.levelRenderer.hasRenderedAllSections();
    }
    private RealisticClientSmoke() {}
}
