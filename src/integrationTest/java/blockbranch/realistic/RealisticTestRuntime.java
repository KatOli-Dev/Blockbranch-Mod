package blockbranch.realistic;
import blockbranch.Blockbranch;
/** Opt-in native/review fixtures; excluded from production. */
public final class RealisticTestRuntime {
    public static void initialize() {
        if(Boolean.getBoolean("blockbranch.uniformSnapshotSmoke"))UniformSnapshotSmoke.register();
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(RealisticProbe.TYPE,RealisticProbe.CODEC);
        if (Boolean.getBoolean("blockbranch.realisticClientFixture")) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(RealisticProbe.TYPE,(packet,context)->context.server().execute(()->{
                var player=context.player(); var level=player.level();
                // Opt-in screenshot fixture: loading a complete scene must not turn noon into night.
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,context.server());
                if(packet.stage()==1) {
                    var center=RealisticProbe.ROOM;
                    for(int y=-3;y<=3;y++) for(int z=-3;z<=3;z++) for(int x=-3;x<=3;x++)
                        level.setBlock(center.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
                    level.setBlock(RealisticProbe.LAMP,net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState(),3);
                    level.setBlock(RealisticProbe.CHEST,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),3);
                }
                if(packet.stage()==3 && ((blockbranch.cubic.SparseColumn)level.getChunkAt(RealisticProbe.LAMP)).blockbranch$terrain().resident(RealisticProbe.LAMP.getY()>>4))
                    throw new AssertionError("Underground edit was not evicted after returning to surface");
                String name=player.getName().getString();
                if(Boolean.getBoolean("blockbranch.meadowForbClientOnly")&&(packet.stage()==125||packet.stage()==126)){MeadowForbSmoke.capture(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.tundraFloorClientOnly")&&(packet.stage()==125||packet.stage()==126)){TundraSmoke.capture(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.gravelDesertClientOnly")&&(packet.stage()==123||packet.stage()==124)){DesertSmoke.capture(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.drySteppeClientOnly")&&packet.stage()==123){var g=(RealisticGenerator)level.getChunkSource().getGenerator();var bed=SteppeSmoke.fixture(g,level.getChunkSource().randomState());context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+.5)+" "+(bed.getY()+36)+" "+(bed.getZ()+.5)+" 45 35");return;}
                if(Boolean.getBoolean("blockbranch.icebergClientOnly")&&packet.stage()>=90&&packet.stage()<=94){IcebergSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.seaIceClientOnly")&&packet.stage()>=90&&packet.stage()<=94){SeaIceSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.stonyShoreClientOnly")&&packet.stage()>=90&&packet.stage()<=94){StonyShoreSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.warmDappledClientOnly")&&packet.stage()>=90&&packet.stage()<=94){WarmDappledSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.coldUplandsClientOnly")&&packet.stage()>=90&&packet.stage()<=94){ColdUplandsSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.badlandsClientOnly")&&packet.stage()>=90&&packet.stage()<=94){BadlandsSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.peaksClientOnly")&&packet.stage()>=90&&packet.stage()<=94){MountainPeaksSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.savannaUplandsClientOnly")&&packet.stage()>=90&&packet.stage()<=94){SavannaUplandsSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.windsweptClientOnly")&&packet.stage()>=90&&packet.stage()<=94){WindsweptUplandsSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.tropicalClientOnly")&&packet.stage()>=90&&packet.stage()<=94){TropicalForestSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.darkCherryClientOnly")&&packet.stage()>=90&&packet.stage()<=94){DarkCherrySmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.temperateVariantClientOnly")&&packet.stage()>=90&&(packet.stage()<=94||Boolean.getBoolean("blockbranch.flowerVisibilityDiagnostic")&&packet.stage()==95)){TemperateForestVariantSmoke.probe(player,packet.stage());return;}
                if(Boolean.getBoolean("blockbranch.oldGrowthClientOnly")&&packet.stage()>=90&&packet.stage()<=94){OldGrowthTaigaSmoke.probe(player,packet.stage());return;}
                if((Boolean.getBoolean("blockbranch.lukewarmOceanClientOnly")||Boolean.getBoolean("blockbranch.deepLukewarmOceanClientOnly"))&&packet.stage()>=90&&packet.stage()<=95){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();
                    boolean deep=Boolean.getBoolean("blockbranch.deepLukewarmOceanClientOnly");var bed=LukewarmOceanSmoke.fixture(g,r,deep);
                    if(packet.stage()<=92||packet.stage()==95){
                        LukewarmOceanSmoke.restore(level,bed);level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());
                        boolean aided=deep&&(packet.stage()==92||packet.stage()==95);
                        if(aided)player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,6000,0,false,false,false));
                        else player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        double[] view=packet.stage()==90?new double[]{bed.getX()+.5,TerrainShape.SEA_LEVEL+5,bed.getZ()+.5,0,0}:LukewarmOceanSmoke.camera(g,r,bed,deep,packet.stage()==92);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+view[0]+" "+view[1]+" "+view[2]+" "+view[3]+" "+view[4]);
                        Blockbranch.LOGGER.info("LUKEWARM_CLIENT_CAMERA stage={} deep={} aided={} bed={} view={}",packet.stage(),deep,aided,bed,java.util.Arrays.toString(view));
                    }else if(packet.stage()==93)LukewarmOceanSmoke.removeSupport(level,bed);
                    else{LukewarmOceanSmoke.restore(level,bed);player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);}
                    return;
                }
                if(Boolean.getBoolean("blockbranch.beachClientOnly")&&packet.stage()>=90&&packet.stage()<=94){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();
                    var bed=CoastalBeachSmoke.fixture(g,r,Boolean.getBoolean("blockbranch.snowyBeachClient"));
                    if(packet.stage()<=92){
                        CoastalBeachSmoke.restore(level,bed);
                        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());
                        player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        double[] view=packet.stage()==90?new double[]{bed.getX()+.5,bed.getY()+5,bed.getZ()+.5,0,0}:CoastalBeachSmoke.camera(g,r,bed,packet.stage()==92);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+view[0]+" "+view[1]+" "+view[2]+" "+view[3]+" "+view[4]);
                        Blockbranch.LOGGER.info("BEACH_CAMERA stage={} bed={} view={}",packet.stage(),bed,java.util.Arrays.toString(view));
                    }else if(packet.stage()==93)level.setBlock(bed,net.minecraft.world.level.block.Blocks.CLAY.defaultBlockState(),3);
                    else CoastalBeachSmoke.restore(level,bed);
                    return;
                }
                if((Boolean.getBoolean("blockbranch.riverClientOnly")||Boolean.getBoolean("blockbranch.frozenRiverClientOnly"))&&packet.stage()>=90&&packet.stage()<=94){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=Boolean.getBoolean("blockbranch.frozenRiverClientOnly")?FrozenRiverSmoke.visualScene(g,r):RiverSmoke.scene(g,r);
                    if(packet.stage()<=92){RiverSmoke.restore(level,bed);level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");double[] view=packet.stage()==90?new double[]{bed.getX()+.5,g.drainage(r).sample(bed.getX(),bed.getZ()).waterLevel()+5,bed.getZ()+.5,0,0}:RiverSmoke.camera(g.drainage(r),bed,packet.stage()==92);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+view[0]+" "+view[1]+" "+view[2]+" "+view[3]+" "+view[4]);Blockbranch.LOGGER.info("RIVER_CAMERA stage={} bed={} view={}",packet.stage(),bed,java.util.Arrays.toString(view));}
                    else if(packet.stage()==93)level.setBlock(bed,net.minecraft.world.level.block.Blocks.CLAY.defaultBlockState(),3);else RiverSmoke.restore(level,bed);return;
                }
                if(Boolean.getBoolean("blockbranch.montaneMeadowClientOnly")&&packet.stage()>=90&&packet.stage()<=97){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var bed=MontaneMeadowSmoke.fixture(g,random);
                    if(packet.stage()<=92||packet.stage()>=95){MontaneMeadowSmoke.restore(level,bed);level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");double[] view=packet.stage()==90?new double[]{bed.getX()+.5,bed.getY()+5,bed.getZ()+.5,0,0}:packet.stage()==95||packet.stage()==96?MontaneMeadowSmoke.detailCamera(g,random,MontaneMeadowSmoke.scene(g,random),packet.stage()==96):MontaneMeadowSmoke.camera(g,random,MontaneMeadowSmoke.scene(g,random),packet.stage()==92||packet.stage()==97);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+view[0]+" "+view[1]+" "+view[2]+" "+view[3]+" "+view[4]);Blockbranch.LOGGER.info("MONTANE_MEADOW_CAMERA stage={} bed={} view={}",packet.stage(),bed,java.util.Arrays.toString(view));}
                    else if(packet.stage()==93)level.setBlock(bed,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    else MontaneMeadowSmoke.restore(level,bed);return;
                }
                if((Boolean.getBoolean("blockbranch.snowPlainsClientOnly")||Boolean.getBoolean("blockbranch.snowSlopesClientOnly"))&&packet.stage()>=90&&packet.stage()<=94){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();boolean slopes=Boolean.getBoolean("blockbranch.snowSlopesClientOnly");var bed=OpenSnowSmoke.fixture(g,random,slopes);
                    if(packet.stage()<=92){OpenSnowSmoke.restore(level,bed);level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");double[] view=packet.stage()==90?new double[]{bed.getX()+.5,bed.getY()+5,bed.getZ()+.5,0,0}:OpenSnowSmoke.camera(g,random,bed,packet.stage()==92,slopes);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+view[0]+" "+view[1]+" "+view[2]+" "+view[3]+" "+view[4]);Blockbranch.LOGGER.info("OPEN_SNOW_CAMERA stage={} bed={} view={}",packet.stage(),bed,java.util.Arrays.toString(view));}
                    else if(packet.stage()==93)level.setBlock(bed,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    else {OpenSnowSmoke.restore(level,bed);Blockbranch.LOGGER.info("OPEN_SNOW_SUPPORT_RESTORE_PASS {}",bed);}return;
                }
                if(Boolean.getBoolean("blockbranch.oceanClientOnly")&&packet.stage()>=90&&packet.stage()<=94){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var bed=OceanSmoke.fixture(g,random);
                    if(packet.stage()==90||packet.stage()==91){
                        OceanSmoke.restore(level,bed);level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        double x=bed.getX()+.5,z=bed.getZ()+.5,y=TerrainShape.SEA_LEVEL+6,yaw=40,pitch=22;
                        if(packet.stage()==91){boolean found=false;for(int radius:new int[]{10,14,18}){for(int n=0;n<16;n++){double a=n*Math.PI/8;int wx=bed.getX()+(int)Math.round(Math.cos(a)*radius),wz=bed.getZ()+(int)Math.round(Math.sin(a)*radius);boolean clear=true;for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=2;dy<=3;dy++)if(!g.surfaceState(wx+dx,bed.getY()+dy,wz+dz,random).is(net.minecraft.world.level.block.Blocks.WATER))clear=false;if(clear){x=wx+.5;z=wz+.5;y=bed.getY()+1;found=true;break;}}if(found)break;}if(!found)throw new AssertionError("No unobstructed natural kelp camera");yaw=Math.toDegrees(Math.atan2(-(bed.getX()+.5-x),bed.getZ()+.5-z));pitch=8;}
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+x+" "+y+" "+z+" "+yaw+" "+pitch);
                        Blockbranch.LOGGER.info("OCEAN_CLIENT_CAMERA underwater={} x={} y={} z={} visibility=normal bed={}",packet.stage()==91,x,y,z,bed);
                    }else if(packet.stage()==92)OceanSmoke.grow(level,g,bed);
                    else if(packet.stage()==93)OceanSmoke.removeSupport(level,g,bed);
                    else {OceanSmoke.restore(level,bed);Blockbranch.LOGGER.info("OCEAN_CLIENT_RESTORE_PASS {}",bed);}
                    return;
                }
                // Probe 122 also serves forests; this photo override belongs only to the sunflower fixture.
                if(packet.stage()==122&&Boolean.getBoolean("blockbranch.sunflowerClientOnly")&&SunflowerCameraFixture.fixed()){
                    var camera=SunflowerCameraFixture.anchor(net.minecraft.core.BlockPos.ZERO);
                    context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                    context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(camera.getX()+28.5)+" "+(camera.getY()+5)+" "+(camera.getZ()+18.5)+" 123 7");
                    return;
                }
                if(packet.stage()>=122){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();boolean grove=Boolean.getBoolean("blockbranch.groveClientOnly"),snowy=Boolean.getBoolean("blockbranch.snowyTaigaClientOnly"),taiga=Boolean.getBoolean("blockbranch.taigaClientOnly"),mangrove=Boolean.getBoolean("blockbranch.mangroveClientOnly"),birch=Boolean.getBoolean("blockbranch.birchForestClientOnly"),swamp=Boolean.getBoolean("blockbranch.swampClientOnly");var bed=grove?GroveSmoke.fixture(g,level.getChunkSource().randomState()):snowy?SnowyTaigaSmoke.fixture(g,level.getChunkSource().randomState()):taiga?TaigaSmoke.fixture(g,level.getChunkSource().randomState()):mangrove?MangroveSmoke.fixture(g,level.getChunkSource().randomState()):swamp?SwampSmoke.fixture(g,level.getChunkSource().randomState()):ForestSmoke.fixture(g,level.getChunkSource().randomState(),birch);var log=bed.above(40);var leaf=log.east();
                    if(packet.stage()==122||packet.stage()==123){
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");boolean canopy=packet.stage()==123;
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()-17.5)+" "+(bed.getY()+(canopy?35:5))+" "+(bed.getZ()+28.5)+" -147 "+(canopy?27:7));
                    }else if(packet.stage()==126&&(birch||swamp||mangrove||taiga||snowy||grove)){
                        // Pick an actual eye-level opening with clear space around the lens.
                        int cameraX=bed.getX()-24,cameraZ=bed.getZ()+24,cameraY=bed.getY()+1;boolean clear=false;
                        for(int radius:(mangrove?new int[]{24,32,40,56,72}:new int[]{24,30,36})){for(int n=0;n<16;n++){
                            double a=2.35+n*Math.PI/8;int x=bed.getX()+(int)Math.round(Math.cos(a)*radius),z=bed.getZ()+(int)Math.round(Math.sin(a)*radius),h=g.drainage(level.getChunkSource().randomState()).sample(x,z).height();
                            int lensBase=mangrove?Math.max(h+1,TerrainShape.SEA_LEVEL):h+1;
                            boolean open=true;for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)for(int dy=2;dy<=4;dy++)if(!g.surfaceState(x+dx,lensBase+dy-1,z+dz,level.getChunkSource().randomState()).isAir())open=false;
                            if(open){cameraX=x;cameraZ=z;cameraY=lensBase;clear=true;break;}
                        }if(clear)break;}
                        if(!clear)throw new AssertionError("No unobstructed woodland eye-level camera");
                        if(grove&&Boolean.getBoolean("blockbranch.groveComparison")){cameraX=-1359971;cameraY=1469;cameraZ=-1900027;boolean lensClear=true;for(int y=2;y<=3;y++)if(!g.surfaceState(cameraX,cameraY+y,cameraZ,level.getChunkSource().randomState()).isAir())lensClear=false;if(!lensClear)throw new AssertionError("Original Grove ground camera became obstructed");}
                        if(Boolean.getBoolean("blockbranch.coniferBranchComparison")&&(taiga||snowy||grove)){
                            cameraX=snowy?1651953:-1492044;cameraY=snowy?860:676;cameraZ=snowy?-1900033:-1900018;
                            for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)for(int dy=2;dy<=4;dy++)if(!g.surfaceState(cameraX+dx,cameraY+dy-1,cameraZ+dz,level.getChunkSource().randomState()).isAir())throw new AssertionError("Matched conifer camera is obstructed");
                        }
                        double dx=bed.getX()+.5-(cameraX+.5),dz=bed.getZ()+.5-(cameraZ+.5),dy=bed.getY()+7-(cameraY+1.62);
                        double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(cameraX+.5)+" "+cameraY+" "+(cameraZ+.5)+" "+yaw+" "+pitch);
                        Blockbranch.LOGGER.info("BIRCH_EYE_LEVEL_CAMERA x={} y={} z={} yaw={} pitch={}",cameraX,cameraY,cameraZ,yaw,pitch);
                    }else if(packet.stage()==127&&!swamp){
                        var view=ForestSmoke.detailView(level,g,bed);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+view[0]+" "+view[1]+" "+view[2]+" "+view[3]+" "+view[4]);
                    }else if(packet.stage()==124){
                        if(swamp)SwampSmoke.activeSetup(level,g,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(log,(taiga||snowy||grove?net.minecraft.world.level.block.Blocks.SPRUCE_WOOD:mangrove?net.minecraft.world.level.block.Blocks.MANGROVE_LOG:birch?net.minecraft.world.level.block.Blocks.BIRCH_LOG:net.minecraft.world.level.block.Blocks.OAK_LOG).defaultBlockState(),3);level.setBlock(leaf,(taiga||snowy||grove?net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES:mangrove?net.minecraft.world.level.block.Blocks.MANGROVE_LEAVES:birch?net.minecraft.world.level.block.Blocks.BIRCH_LEAVES:net.minecraft.world.level.block.Blocks.OAK_LEAVES).defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1),3);level.setBlock(log,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    }else{
                        if(swamp)SwampSmoke.activeRestore(level,bed);
                        for(var pos:java.util.List.of(log,leaf)){var base=((blockbranch.cubic.SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();level.setBlock(pos,base.state(pos.getX(),pos.getY(),pos.getZ()),3);}
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);Blockbranch.LOGGER.info("FOREST_ACTIVE_LEAF_SUPPORT_REMOVAL_RESTORE_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=117){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var bed=Boolean.getBoolean("blockbranch.sunflowerClientOnly")?SunflowerSmoke.fixture(g,level.getChunkSource().randomState()):PlainsSmoke.fixture(g,level.getChunkSource().randomState());
                    if(packet.stage()==117||packet.stage()==118){
                        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");boolean overview=packet.stage()==118,sunflower=Boolean.getBoolean("blockbranch.sunflowerClientOnly");var camera=packet.stage()==117||!sunflower?bed:SunflowerCameraFixture.anchor(bed);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(camera.getX()+(sunflower?28.5:-17.5))+" "+(camera.getY()+(overview?29:5))+" "+(camera.getZ()+(sunflower?18.5:28.5))+" "+(sunflower?123:-147)+" "+(overview?27:7));
                    }else if(packet.stage()==119){if(Boolean.getBoolean("blockbranch.sunflowerClientOnly"))SunflowerSmoke.activeBonemeal(level,bed);level.destroyBlock(bed.above(2),false);
                    }else if(packet.stage()==120){PlainsSmoke.restore(level,bed);level.setBlock(bed,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                    }else{PlainsSmoke.restore(level,bed);Blockbranch.LOGGER.info("PLAINS_ACTIVE_PAIRED_SUPPORT_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=114){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=SandyDesertSmoke.nativeWindowFixture(g,r);
                    if(packet.stage()==114){context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()-17.5)+" "+(bed.getY()+5)+" "+(bed.getZ()+28.5)+" -147 7");
                    }else if(packet.stage()==115){level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);SandyDesertSmoke.activeGrowth(level,bed);
                    }else{SandyDesertSmoke.restore(level,bed);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);Blockbranch.LOGGER.info("SANDY_DESERT_BOTH_WINDOWS_CLIENT_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=108){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=SandyDesertSmoke.clientFixture(g,r);
                    if(packet.stage()==108||packet.stage()==109){
                        level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");boolean overview=packet.stage()==109;
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()-17.5)+" "+(bed.getY()+(overview?29:5))+" "+(bed.getZ()+28.5)+" -147 "+(overview?27:7));
                    }else if(packet.stage()==110){level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);SandyDesertSmoke.activeGrowth(level,bed);
                    }else if(packet.stage()==111){level.destroyBlock(bed.above(),true);
                    }else if(packet.stage()==112){SandyDesertSmoke.restore(level,bed);level.setBlock(bed,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    }else{SandyDesertSmoke.restore(level,bed);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);Blockbranch.LOGGER.info("SANDY_DESERT_ACTIVE_GROW_HARVEST_SUPPORT_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=104){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=JungleSmoke.clientFixture(g,r);var log=bed.above(44);var leaf=log.east();
                    if(packet.stage()==104||packet.stage()==105){
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        boolean canopy=packet.stage()==105;
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()-17.5)+" "+(bed.getY()+(canopy?44:5))+" "+(bed.getZ()+28.5)+" -147 "+(canopy?27:7));
                    }else if(packet.stage()==106){
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.JUNGLE_LOG.defaultBlockState(),3);
                        level.setBlock(leaf,net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1),3);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    }else{
                        for(var pos:java.util.List.of(log,leaf)){var base=((blockbranch.cubic.SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();level.setBlock(pos,base.state(pos.getX(),pos.getY(),pos.getZ()),3);}
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);
                        Blockbranch.LOGGER.info("JUNGLE_ACTIVE_JUNGLE_SUPPORT_REMOVAL_RESTORE_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=100){
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=SavannaSmoke.fixture(g,r);var log=bed.above(30);var leaf=log.east();
                    if(packet.stage()==100||packet.stage()==101){
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        if(packet.stage()==100)SavannaSmoke.verifyWoodPalette(level,bed);
                        boolean canopy=packet.stage()==101;
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()-17.5)+" "+(bed.getY()+(canopy?23:5))+" "+(bed.getZ()+28.5)+" -147 "+(canopy?27:7));
                    }else if(packet.stage()==102){
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.ACACIA_WOOD.defaultBlockState(),3);
                        var terrain=((blockbranch.cubic.SparseColumn)level.getChunkAt(log)).blockbranch$terrain();terrain.flush();terrain.evictOutside(-100,-90);
                        if(!level.getBlockState(log).is(net.minecraft.world.level.block.Blocks.ACACIA_WOOD))throw new AssertionError("Acacia wood edit survives section eviction");
                        level.setBlock(leaf,net.minecraft.world.level.block.Blocks.ACACIA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1),3);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    }else{
                        for(var pos:java.util.List.of(log,leaf)){var base=((blockbranch.cubic.SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();level.setBlock(pos,base.state(pos.getX(),pos.getY(),pos.getZ()),3);}
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);
                        Blockbranch.LOGGER.info("SAVANNA_ACTIVE_ACACIA_SUPPORT_REMOVAL_RESTORE_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=96){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=DeepOceanSmoke.fixture(generator,r,Boolean.getBoolean("blockbranch.temperateDeepClientOnly"));var d=generator.drainage(r);var sample=d.sample(bed.getX(),bed.getZ());
                    if(packet.stage()==96||packet.stage()==97){
                        DeepOceanSmoke.restore(level,bed);boolean deepView=packet.stage()==97;
                        if(deepView)player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,6000,0,false,false,false));else player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        double dx=d.sample(bed.getX()+128,bed.getZ()).height()-d.sample(bed.getX()-128,bed.getZ()).height(),dz=d.sample(bed.getX(),bed.getZ()+128).height()-d.sample(bed.getX(),bed.getZ()-128).height();if(dx==0&&dz==0)dz=1;double yaw=Math.toDegrees(Math.atan2(-dx,dz));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+.5)+" "+(deepView?bed.getY()+4:sample.waterLevel()+8)+" "+(bed.getZ()+.5)+" "+yaw+" "+(deepView?22:8));
                        Blockbranch.LOGGER.info("DEEP_OCEAN_CLIENT_VIEW bedView={} visibility={} bed={} depth={}",deepView,deepView?"night_vision_fixture_only":"normal",bed,sample.waterLevel()-bed.getY()-1);
                    }else if(packet.stage()==98){level.destroyBlock(bed,false);Blockbranch.LOGGER.info("DEEP_BED_EXCAVATED {}",bed);
                    }else{DeepOceanSmoke.restore(level,bed);player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);Blockbranch.LOGGER.info("DEEP_BED_WATER_RESTORE_VISIBILITY_CLEARED_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=90){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var bed=MarineShelfSmoke.fixture(generator,random);var pos=bed.above();var d=generator.drainage(random);var sample=d.sample(bed.getX(),bed.getZ());
                    if(packet.stage()==90||packet.stage()==91){
                        MarineShelfSmoke.restore(level,bed);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        double dx=0,dz=0;for(int radius:new int[]{64,256,1024,4096}){dx=d.sample(bed.getX()+radius,bed.getZ()).height()-d.sample(bed.getX()-radius,bed.getZ()).height();dz=d.sample(bed.getX(),bed.getZ()+radius).height()-d.sample(bed.getX(),bed.getZ()-radius).height();if(Math.hypot(dx,dz)>0)break;}if(dx==0&&dz==0)dz=1;
                        double length=Math.hypot(dx,dz),yaw=Math.toDegrees(Math.atan2(-dx,dz));boolean under=packet.stage()==91;
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+.5+(under?-dx/length*5:0))+" "+(under?bed.getY()+1:sample.waterLevel()+6)+" "+(bed.getZ()+.5+(under?-dz/length*5:0))+" "+(under?yaw:yaw+80)+" "+(under?15:22));
                        Blockbranch.LOGGER.info("SHELF_CLIENT_VIEW underwater={} bed={} water={}",under,bed,sample.waterLevel());
                    }else if(packet.stage()==92){
                        var plant=(net.minecraft.world.level.block.BonemealableBlock)net.minecraft.world.level.block.Blocks.SEAGRASS;var state=level.getBlockState(pos);
                        if(!plant.isValidBonemealTarget(level,pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION))throw new AssertionError("Natural shelf plant has no room to grow");
                        plant.performBonemeal(level,level.getRandom(),pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION);
                    }else if(packet.stage()==93){level.destroyBlock(pos,true);
                    }else if(packet.stage()==94){MarineShelfSmoke.restore(level,bed);level.setBlock(bed,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),3);
                    }else{MarineShelfSmoke.restore(level,bed);Blockbranch.LOGGER.info("SHELF_ACTIVE_GROW_HARVEST_WATER_SUPPORT_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=84){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var bed=RiverChannelSmoke.fixture(generator,random);var pos=bed.above();var d=generator.drainage(random);var sample=d.sample(bed.getX(),bed.getZ());
                    if(packet.stage()==84||packet.stage()==85){
                        RiverChannelSmoke.restore(level,bed);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        int edge=d.channelAt(bed.getX(),bed.getZ()).edge();double t=0,best=Double.POSITIVE_INFINITY;
                        for(int n=0;n<=128;n++){double at=n/128.0,dist=Math.hypot(d.edgeX(edge,at)-bed.getX(),d.edgeZ(edge,at)-bed.getZ());if(dist<best){best=dist;t=at;}}
                        double dx=d.edgeX(edge,Math.min(1,t+.001))-d.edgeX(edge,Math.max(0,t-.001)),dz=d.edgeZ(edge,Math.min(1,t+.001))-d.edgeZ(edge,Math.max(0,t-.001)),length=Math.hypot(dx,dz),yaw=Math.toDegrees(Math.atan2(-dx,dz));boolean under=packet.stage()==85;
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+.5+(under?-dx/length*5:0))+" "+(under?bed.getY()+1:sample.waterLevel()+6)+" "+(bed.getZ()+.5+(under?-dz/length*5:0))+" "+yaw+" "+(under?15:22));
                        Blockbranch.LOGGER.info("RIVER_CLIENT_VIEW underwater={} bed={} water={}",under,bed,sample.waterLevel());
                    }else if(packet.stage()==86){
                        var plant=(net.minecraft.world.level.block.BonemealableBlock)net.minecraft.world.level.block.Blocks.SEAGRASS;var state=level.getBlockState(pos);
                        if(!plant.isValidBonemealTarget(level,pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION))throw new AssertionError("Natural river plant has no room to grow");
                        plant.performBonemeal(level,level.getRandom(),pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION);
                    }else if(packet.stage()==87){level.destroyBlock(pos,true);
                    }else if(packet.stage()==88){RiverChannelSmoke.restore(level,bed);level.setBlock(bed,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),3);
                    }else{RiverChannelSmoke.restore(level,bed);Blockbranch.LOGGER.info("RIVER_ACTIVE_GROW_HARVEST_WATER_SUPPORT_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=78){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var bed=LakeSmoke.fixture(generator,random);var pos=bed.above();var d=generator.drainage(random);var sample=d.sample(bed.getX(),bed.getZ());
                    if(packet.stage()==78||packet.stage()==79){
                        LakeSmoke.restore(level,bed);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        double dx=d.nodeX(sample.basin())-bed.getX(),dz=d.nodeZ(sample.basin())-bed.getZ(),length=Math.hypot(dx,dz),yaw=Math.toDegrees(Math.atan2(-dx,dz));boolean under=packet.stage()==79;
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+.5+(under?dx/length*5:0))+" "+(under?bed.getY()+1:sample.waterLevel()+6)+" "+(bed.getZ()+.5+(under?dz/length*5:0))+" "+(under?yaw+180:yaw+110)+" "+(under?15:22));
                        Blockbranch.LOGGER.info("LAKE_CLIENT_VIEW underwater={} bed={} water={}",under,bed,sample.waterLevel());
                    }else if(packet.stage()==80){
                        var plant=(net.minecraft.world.level.block.BonemealableBlock)net.minecraft.world.level.block.Blocks.SEAGRASS;var state=level.getBlockState(pos);
                        if(!plant.isValidBonemealTarget(level,pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION))throw new AssertionError("Natural lake plant has no room to grow");
                        plant.performBonemeal(level,level.getRandom(),pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION);
                    }else if(packet.stage()==81){level.destroyBlock(pos,true);
                    }else if(packet.stage()==82){LakeSmoke.restore(level,bed);level.setBlock(bed,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),3);
                    }else{LakeSmoke.restore(level,bed);Blockbranch.LOGGER.info("LAKE_ACTIVE_GROW_HARVEST_WATER_SUPPORT_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=69){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var bed=TundraSmoke.fixture(generator,level.getChunkSource().randomState());var pos=bed.above();var fescue=MeadowContent.MEADOW_FESCUE;var berry=TundraSmoke.berry(level,bed);var bush=(net.minecraft.world.level.block.SweetBerryBushBlock)net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH;
                    if(packet.stage()==69){
                        TundraSmoke.restore(level,bed);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        // The close view targets the natural fescue witness rather than the surrounding shrubs.
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),Boolean.getBoolean("blockbranch.tundraFescueCloseup") ? "tp "+name+" "+(bed.getX()+.5)+" "+(bed.getY()+2)+" "+(bed.getZ()-2.5)+" 0 28" : "tp "+name+" "+(bed.getX()+.5)+" "+(bed.getY()+8)+" "+(bed.getZ()+.5)+" -50 22");
                        Blockbranch.LOGGER.info("TUNDRA_CLIENT_WITNESS bed={} berry={}",bed,berry);
                    }else if(packet.stage()==70){
                        var shears=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,shears);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
                        level.getBlockState(pos).useItemOn(shears,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                        if(!level.getBlockState(pos).getValue(MeadowFescueBlock.CROPPED)||fescue.divide(level,pos,level.getBlockState(pos)))throw new AssertionError("Tundra fescue division must retain one crown");
                    }else if(packet.stage()==71){fescue.performBonemeal(level,level.getRandom(),pos,level.getBlockState(pos),net.minecraft.world.level.block.BonemealSource.INTERACTION);
                    }else if(packet.stage()==72){level.setBlock(bed,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);if(!level.getBlockState(pos).isAir())throw new AssertionError("Tundra fescue lost soil support incorrectly");
                    }else if(packet.stage()==73){TundraSmoke.restore(level,bed);
                    }else if(packet.stage()==74){for(int i=0;i<2;i++)bush.performBonemeal(level,level.getRandom(),berry,level.getBlockState(berry),net.minecraft.world.level.block.BonemealSource.INTERACTION);
                    }else if(packet.stage()==75){var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(berry),net.minecraft.core.Direction.UP,berry,false);level.getBlockState(berry).useWithoutItem(level,player,hit);if(level.getBlockState(berry).getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)!=1)throw new AssertionError("Tundra berry harvest must retain the native low shrub");
                    }else if(packet.stage()==76){level.setBlock(berry.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);if(!level.getBlockState(berry).isAir())throw new AssertionError("Low tundra shrub survived invalid soil");
                    }else{TundraSmoke.restore(level,bed);Blockbranch.LOGGER.info("TUNDRA_ACTIVE_DIVIDE_REGROW_BERRY_SUPPORT_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=63){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var bed=HumidSmoke.fixture(generator,level.getChunkSource().randomState());var log=HumidSmoke.testLog(bed);var leaf=HumidSmoke.testLeaf(bed);
                    if(packet.stage()==63||packet.stage()==64){
                        boolean canopy=packet.stage()==64;HumidSmoke.restorePhysics(level,bed);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+.5)+" "+(bed.getY()+(canopy?28:2))+" "+(bed.getZ()+(canopy?.5:-6.5))+" "+(canopy?"-50 35":"-25 0"));
                        Blockbranch.LOGGER.info("HUMID_WOODLAND_CLIENT_VIEW canopy={} bed={}",canopy,bed);
                    }else if(packet.stage()==65){
                        HumidSmoke.restorePhysics(level,bed);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),3);
                        level.setBlock(leaf,net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1),3);level.destroyBlock(bed.above(),true);
                    }else if(packet.stage()==66){
                        if(level.getBlockState(leaf).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1)throw new AssertionError("Supported humid woodland leaf unstable");level.destroyBlock(log,false);
                    }else if(packet.stage()==67){
                        var state=level.getBlockState(leaf);if(!state.isAir()){
                            if(!state.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)||state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)||state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7)throw new AssertionError("Disconnected humid leaf did not lose support");
                            state.randomTick(level,leaf,level.getRandom());
                        }
                        if(!level.getBlockState(leaf).isAir())throw new AssertionError("Unsupported humid leaf remained");
                    }else{HumidSmoke.restorePhysics(level,bed);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);Blockbranch.LOGGER.info("HUMID_ACTIVE_TIMBER_LEAF_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=61){
                    int x=-1050036,z=-1700033;var generator=(RealisticGenerator)level.getChunkSource().getGenerator();int h=generator.drainage(level.getChunkSource().randomState()).sample(x,z).height();
                    context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                    boolean canopy=packet.stage()==62;
                    context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(x+.5)+" "+(h+(canopy?22:2))+" "+(z+(canopy?.5:-5.5))+" "+(canopy?"-50 40":"-25 0"));
                    Blockbranch.LOGGER.info("WOODLAND_FOLIAGE_COMPARISON_VIEW {} x={} y={} z={}",canopy?"CANOPY":"GROUND",x,h,z);return;
                }
                if(packet.stage()==60){
                    if(Boolean.getBoolean("blockbranch.terrainCollateralClientOnly")){level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");}
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var bed=SlopeCorrectionSmoke.fixture(SlopeCorrection.Kind.MEADOW,generator,level.getChunkSource().randomState());var d=generator.drainage(level.getChunkSource().randomState());if(Boolean.getBoolean("blockbranch.terrainContourComparison")){int fixedX=1860000,fixedZ=-1404000;bed=new net.minecraft.core.BlockPos(fixedX,d.sample(fixedX,fixedZ).height(),fixedZ);}
                    double dx=d.sample(bed.getX()+32,bed.getZ()).height()-d.sample(bed.getX()-32,bed.getZ()).height(),dz=d.sample(bed.getX(),bed.getZ()+32).height()-d.sample(bed.getX(),bed.getZ()-32).height();double yaw=Math.toDegrees(Math.atan2(-dx,dz));if(Boolean.getBoolean("blockbranch.terrainContourComparison")){var baseline=new TerrainShape(level.getChunkSource().randomState().seed());yaw=Math.toDegrees(Math.atan2(-(baseline.height(1860032,-1404000)-baseline.height(1859968,-1404000)),baseline.height(1860000,-1403968)-baseline.height(1860000,-1404032)));}
                    context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+.5)+" "+(bed.getY()+24)+" "+(bed.getZ()+.5)+" "+yaw+" 28");
                    Blockbranch.LOGGER.info("SUPPORTED_SLOPE_CLIENT_WITNESS bed={} slope={}",bed,SlopeCorrection.slope(d,bed.getX(),bed.getZ()));return;
                }
                if(packet.stage()>=55){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var bed=DesertSmoke.fixture(generator,level.getChunkSource().randomState());var start=bed.above(6);
                    if(packet.stage()==55){
                        DesertSmoke.restorePhysics(level,bed);var drainage=generator.drainage(level.getChunkSource().randomState());
                        double dx=drainage.sample(bed.getX()+32,bed.getZ()).height()-drainage.sample(bed.getX()-32,bed.getZ()).height(),dz=drainage.sample(bed.getX(),bed.getZ()+32).height()-drainage.sample(bed.getX(),bed.getZ()-32).height();
                        double yaw=dx==0&&dz==0?45:Math.toDegrees(Math.atan2(dx,-dz));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+6)+" "+(bed.getZ()+0.5)+" "+yaw+" 18");
                    }else if(packet.stage()==56){
                        DesertSmoke.restorePhysics(level,bed);level.setBlock(start,DesertContent.PAVEMENT.defaultBlockState(),3);level.setBlock(DesertSmoke.gravelStart(bed),net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState(),3);
                    }else if(packet.stage()==57){
                        var shovel=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,shovel);
                        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(start),net.minecraft.core.Direction.UP,start,false);
                        level.getBlockState(start).useItemOn(shovel,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                    }else if(packet.stage()==58){
                        if(!DesertContent.PAVEMENT.disturb(level,bed)||DesertContent.PAVEMENT.disturb(level,bed))throw new AssertionError("Pavement disturbance must occur exactly once");
                    }else{DesertSmoke.restorePhysics(level,bed);Blockbranch.LOGGER.info("DESERT_ACTIVE_SHOVEL_STABILITY_FALL_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=50){
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var bed=ColdSteppeSmoke.fixture(generator,level.getChunkSource().randomState());var pos=bed.above();var fescue=MeadowContent.MEADOW_FESCUE;
                    if(packet.stage()==50){
                        ColdSteppeSmoke.restore(level,bed);var drainage=generator.drainage(level.getChunkSource().randomState());
                        double dx=drainage.sample(bed.getX()+32,bed.getZ()).height()-drainage.sample(bed.getX()-32,bed.getZ()).height(),dz=drainage.sample(bed.getX(),bed.getZ()+32).height()-drainage.sample(bed.getX(),bed.getZ()-32).height();
                        double yaw=dx==0&&dz==0?45:Math.toDegrees(Math.atan2(dx,-dz));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+6)+" "+(bed.getZ()+0.5)+" "+yaw+" 18");
                    }else if(packet.stage()==51){
                        var shears=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,shears);
                        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
                        level.getBlockState(pos).useItemOn(shears,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);var state=level.getBlockState(pos);
                        if(!state.is(fescue)||!state.getValue(MeadowFescueBlock.CROPPED)||fescue.divide(level,pos,state))throw new AssertionError("Cold-steppe division did not retain one cropped crown");
                    }else if(packet.stage()==52){
                        var state=level.getBlockState(pos);if(!fescue.isValidBonemealTarget(level,pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION))throw new AssertionError("Cold-steppe crown did not accept regrowth");
                        fescue.performBonemeal(level,level.getRandom(),pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION);
                    }else if(packet.stage()==53){
                        level.setBlock(bed,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);if(!level.getBlockState(pos).isAir())throw new AssertionError("Cold-steppe fescue survived invalid soil");
                    }else{ColdSteppeSmoke.restore(level,bed);Blockbranch.LOGGER.info("COLD_STEPPE_ACTIVE_DIVIDE_REGROW_SUPPORT_RESTORE_PASS {}",bed);}
                    return;
                }
                if(packet.stage()>=41) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    if(packet.stage()>=47) {
                        if(packet.stage()==49){Blockbranch.LOGGER.info("CORRECTED_LOWLAND_UPLAND_CLIENT_PASS");return;}
                        var bed=BoundaryCorrectionSmoke.fixture(generator,level.getChunkSource().randomState(),packet.stage()==48);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+24)+" "+(bed.getZ()+0.5)+" -50 40");return;
                    }
                    var bed=WarmSmoke.fixture(generator,level.getChunkSource().randomState());var log=WarmSmoke.testLog(bed);var leaf=WarmSmoke.testLeaf(bed);
                    if(packet.stage()==41) {
                        WarmSmoke.restorePhysics(level,bed);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+2)+" "+(bed.getZ()-6+0.5)+" -25 0");
                    } else if(packet.stage()==42) {
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+24)+" "+(bed.getZ()+0.5)+" -50 35");
                    } else if(packet.stage()==43) {
                        WarmSmoke.restorePhysics(level,bed);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.SPRUCE_LOG.defaultBlockState(),3);
                        level.setBlock(leaf,net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1),3);
                        level.destroyBlock(bed.above(),true);
                    } else if(packet.stage()==44) {
                        if(level.getBlockState(leaf).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1)throw new AssertionError("Supported pine-material leaf was unstable");level.destroyBlock(log,false);
                    } else if(packet.stage()==45) {
                        var state=level.getBlockState(leaf);
                        if(!state.isAir()) {
                            if(!state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7)throw new AssertionError("Disconnected pine-material leaf did not decay");
                            state.randomTick(level,leaf,level.getRandom());
                        }
                        if(!level.getBlockState(leaf).isAir())throw new AssertionError("Unsupported pine-material leaf remained");
                    } else {
                        WarmSmoke.restorePhysics(level,bed);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);Blockbranch.LOGGER.info("WARM_ACTIVE_TIMBER_LEAF_RESTORE_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=33) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var bed=BorealSmoke.fixture(generator,level.getChunkSource().randomState());var log=BorealSmoke.testLog(bed);var leaf=BorealSmoke.testLeaf(bed);
                    if(packet.stage()==33) {
                        BorealSmoke.restorePhysics(level,bed);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+2)+" "+(bed.getZ()-6+0.5)+" -25 0");
                    } else if(packet.stage()==34) {
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+26)+" "+(bed.getZ()+0.5)+" -50 35");
                    } else if(packet.stage()==35) {
                        BorealSmoke.restorePhysics(level,bed);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.SPRUCE_LOG.defaultBlockState(),3);
                        level.setBlock(leaf,net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1),3);
                        level.destroyBlock(bed.above(),true);
                    } else if(packet.stage()==36) {
                        if(level.getBlockState(leaf).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1) throw new AssertionError("Supported spruce leaf was unstable");
                        level.destroyBlock(log,false);
                    } else if(packet.stage()==37) {
                        var state=level.getBlockState(leaf);
                        if(!state.isAir()) {
                            if(!state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT) || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7) throw new AssertionError("Disconnected spruce leaf did not become decaying");
                            state.randomTick(level,leaf,level.getRandom());
                        }
                        if(!level.getBlockState(leaf).isAir()) throw new AssertionError("Unsupported spruce leaf did not decay");
                    } else if(packet.stage()==38) {
                        BorealSmoke.restorePhysics(level,bed);var berry=BorealSmoke.berry(level,bed);var state=level.getBlockState(berry);
                        if(state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)!=3) throw new AssertionError("Natural boreal berry was not ripe");
                        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(berry),net.minecraft.core.Direction.UP,berry,false);
                        state.useWithoutItem(level,player,hit);
                        if(level.getBlockState(berry).getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)!=1) throw new AssertionError("Native berry harvest did not retain the bush");
                    } else if(packet.stage()==39) {
                        var berry=BorealSmoke.berry(level,bed);var bush=(net.minecraft.world.level.block.SweetBerryBushBlock)net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH;
                        for(int i=0;i<2;i++) bush.performBonemeal(level,level.getRandom(),berry,level.getBlockState(berry),net.minecraft.world.level.block.BonemealSource.INTERACTION);
                        if(level.getBlockState(berry).getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)!=3) throw new AssertionError("Native berry regrowth failed");
                    } else {
                        BorealSmoke.restorePhysics(level,bed);var berry=BorealSmoke.berry(level,bed);var base=((blockbranch.cubic.SparseColumn)level.getChunkAt(berry)).blockbranch$terrain().base();
                        level.setBlock(berry,base.state(berry.getX(),berry.getY(),berry.getZ()),3);level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);
                        Blockbranch.LOGGER.info("BOREAL_ACTIVE_TIMBER_LEAF_BERRY_RESTORE_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=28) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var bed=SteppeSmoke.fixture(generator,level.getChunkSource().randomState());var pos=bed.above();var shrub=SteppeContent.SILVER_SHRUB;
                    if(Boolean.getBoolean("blockbranch.steppeArtReview")&&(packet.stage()==28||packet.stage()==30)){
                        // Dropped sticks from earlier clipping obscure the retained crown in art captures.
                        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(bed).inflate(4)))item.discard();
                    }
                    if(packet.stage()==28) {
                        if(Boolean.getBoolean("blockbranch.drySteppeClientOnly")){player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,context.server());context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");}
                        SteppeSmoke.restore(level,bed);var drainage=generator.drainage(level.getChunkSource().randomState());
                        double dx=drainage.sample(bed.getX()+32,bed.getZ()).height()-drainage.sample(bed.getX()-32,bed.getZ()).height();
                        double dz=drainage.sample(bed.getX(),bed.getZ()+32).height()-drainage.sample(bed.getX(),bed.getZ()-32).height();
                        double yaw=dx==0 && dz==0 ? 45 : Math.toDegrees(Math.atan2(dx,-dz));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                            Boolean.getBoolean("blockbranch.steppeArtReview") ? "tp "+name+" "+(bed.getX()+.5)+" "+(bed.getY()+2)+" "+(bed.getZ()-2.5)+" 0 25" : "tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+6)+" "+(bed.getZ()+0.5)+" "+yaw+" 20");
                    } else if(packet.stage()==29) {
                        var shears=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,shears);
                        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
                        level.getBlockState(pos).useItemOn(shears,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                        var state=level.getBlockState(pos);
                        if(!state.is(shrub) || !state.getValue(SteppeShrubBlock.CLIPPED) || shrub.clip(level,pos,state)) throw new AssertionError("Shrub clipping did not retain a bounded crown");
                    } else if(packet.stage()==30) {
                        var state=level.getBlockState(pos);
                        if(!shrub.isValidBonemealTarget(level,pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION)) throw new AssertionError("Clipped shrub did not accept regrowth");
                        shrub.performBonemeal(level,level.getRandom(),pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION);
                    } else if(packet.stage()==31) {
                        level.setBlock(bed,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                        if(!level.getBlockState(pos).isAir()) throw new AssertionError("Shrub remained after removing suitable soil");
                    } else {
                        SteppeSmoke.restore(level,bed);Blockbranch.LOGGER.info("STEPPE_ACTIVE_CLIP_REGROW_SUPPORT_RESTORE_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=22) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var bed=WoodlandSmoke.fixture(generator,level.getChunkSource().randomState());
                    var log=WoodlandSmoke.testLog(bed);var leaf=WoodlandSmoke.testLeaf(bed);
                    if(packet.stage()==22) {
                        WoodlandSmoke.restorePhysics(level,bed);
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),"time set 6000");
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                            "tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+2)+" "+(bed.getZ()-6+0.5)+" -25 0");
                    } else if(packet.stage()==23) {
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                            "tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+22)+" "+(bed.getZ()+0.5)+" -50 40");
                    } else if(packet.stage()==24) {
                        WoodlandSmoke.restorePhysics(level,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(log,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),3);
                        level.setBlock(leaf,net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,1),3);
                        level.destroyBlock(bed.above(),true);
                    } else if(packet.stage()==25) {
                        if(level.getBlockState(leaf).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=1) throw new AssertionError("Supported natural leaf was unstable");
                        level.destroyBlock(log,false);
                    } else if(packet.stage()==26) {
                        var state=level.getBlockState(leaf);
                        if(!state.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES) || state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)
                            || state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)!=7) throw new AssertionError("Disconnected natural leaf did not become decaying");
                        state.randomTick(level,leaf,level.getRandom());
                        if(!level.getBlockState(leaf).isAir()) throw new AssertionError("Unsupported natural leaf did not decay");
                    } else {
                        WoodlandSmoke.restorePhysics(level,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);
                        Blockbranch.LOGGER.info("WOODLAND_ACTIVE_TIMBER_LEAF_DECAY_RESTORE_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=17) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var bed=MeadowSmoke.fixture(generator,level.getChunkSource().randomState());
                    var pos=bed.above();var fescue=MeadowContent.MEADOW_FESCUE;
                    if(packet.stage()==17) {
                        MeadowSmoke.restore(level,bed);
                        var drainage=generator.drainage(level.getChunkSource().randomState());
                        double dx=drainage.sample(bed.getX()+32,bed.getZ()).height()-drainage.sample(bed.getX()-32,bed.getZ()).height();
                        double dz=drainage.sample(bed.getX(),bed.getZ()+32).height()-drainage.sample(bed.getX(),bed.getZ()-32).height();
                        double yaw=dx==0 && dz==0 ? 45 : Math.toDegrees(Math.atan2(dx,-dz));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                            "tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+6)+" "+(bed.getZ()+0.5)+" "+yaw+" 20");
                    } else if(packet.stage()==18) {
                        var shears=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS);
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,shears);
                        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
                        level.getBlockState(pos).useItemOn(shears,level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                        var state=level.getBlockState(pos);
                        if(!state.is(fescue) || !state.getValue(MeadowFescueBlock.CROPPED) || fescue.divide(level,pos,state))
                            throw new AssertionError("Shearing did not leave a single-use cropped crown");
                    } else if(packet.stage()==19) {
                        var state=level.getBlockState(pos);
                        if(!fescue.isValidBonemealTarget(level,pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION)) throw new AssertionError("Cropped crown did not accept regrowth");
                        fescue.performBonemeal(level,level.getRandom(),pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION);
                        if(level.getBlockState(pos).getValue(MeadowFescueBlock.CROPPED)) throw new AssertionError("Crown failed to regrow");
                    } else if(packet.stage()==20) {
                        level.setBlock(bed,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                        if(!level.getBlockState(pos).isAir()) throw new AssertionError("Meadow tuft survived removing suitable soil");
                    } else {
                        MeadowSmoke.restore(level,bed);
                        Blockbranch.LOGGER.info("LOWLAND_MEADOW_ACTIVE_DIVIDE_REGROW_SUPPORT_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=13) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var bed=SandyShoreSmoke.fixture(generator,level.getChunkSource().randomState());
                    var root=SandyShoreSmoke.testRoot(bed);
                    if(packet.stage()==13) {
                        SandyShoreSmoke.restorePhysics(level,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);
                        var drainage=generator.drainage(level.getChunkSource().randomState());
                        int dx=0,dz=128;
                        int nearest=Integer.MAX_VALUE;
                        for(int oz=-128;oz<=128;oz+=4) for(int ox=-128;ox<=128;ox+=4) {
                            int distance=ox*ox+oz*oz;
                            if(distance>0 && distance<nearest && LowGradientSandyShore.site(drainage,bed.getX()+ox,bed.getZ()+oz).oceanWater()) { dx=ox;dz=oz;nearest=distance; }
                        }
                        double yaw=Math.toDegrees(Math.atan2(-dx,dz));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                            "tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+12)+" "+(bed.getZ()+0.5)+" "+yaw+" 30");
                    } else if(packet.stage()==14) {
                        SandyShoreSmoke.restorePhysics(level,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(root,net.minecraft.world.level.block.Blocks.SAND.defaultBlockState(),2);
                        level.setBlock(root.above(),net.minecraft.world.level.block.Blocks.SAND.defaultBlockState(),2);
                        level.setBlock(root.above(2),SandyShoreContent.BEACH_GRASS.defaultBlockState(),3);
                        level.setBlock(SandyShoreSmoke.controlStart(bed),net.minecraft.world.level.block.Blocks.SAND.defaultBlockState(),3);
                    } else if(packet.stage()==15) {
                        if(!level.getBlockState(root).is(SandyShoreContent.ROOT_BOUND_SAND) || !level.getBlockState(root.above()).is(SandyShoreContent.ROOT_BOUND_SAND))
                            throw new AssertionError("Living roots did not hold both unsupported sand layers");
                        level.destroyBlock(root.above(2),false);
                    } else {
                        var landing=SandyShoreSmoke.landing(level,root);
                        if(!level.getBlockState(root).isAir() || !level.getBlockState(root.above()).isAir()
                            || !level.getBlockState(landing).is(net.minecraft.world.level.block.Blocks.SAND)
                            || !level.getBlockState(landing.above()).is(net.minecraft.world.level.block.Blocks.SAND))
                            throw new AssertionError("Uprooting did not release and drop both sand layers");
                        SandyShoreSmoke.restorePhysics(level,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);
                        Blockbranch.LOGGER.info("SANDY_SHORE_ACTIVE_ROOT_BINDING_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=10) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var bed=RiverMarginSmoke.fixture(generator,level.getChunkSource().randomState());
                    if(packet.stage()==10) {
                        var channel=generator.drainage(level.getChunkSource().randomState()).channelAt(bed.getX(),bed.getZ());
                        var drainage=generator.drainage(level.getChunkSource().randomState());
                        double dx=drainage.edgeX(channel.edge(),0.6)-drainage.edgeX(channel.edge(),0.4);
                        double dz=drainage.edgeZ(channel.edge(),0.6)-drainage.edgeZ(channel.edge(),0.4);
                        double yaw=Math.toDegrees(Math.atan2(-dx,dz));
                        context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                            "tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+8)+" "+(bed.getZ()+0.5)+" "+yaw+" 30");
                    } else if(packet.stage()==11) {
                        RiverMarginSmoke.restorePhysics(level,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,true);
                        level.setBlock(bed.above(6),RiverMarginContent.PACKED_RIVER_SHINGLE.defaultBlockState(),3);
                        level.setBlock(RiverMarginSmoke.gravelStart(bed),net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState(),3);
                    } else {
                        if(!level.getBlockState(bed.above(6)).is(RiverMarginContent.PACKED_RIVER_SHINGLE)
                            || !level.getBlockState(RiverMarginSmoke.gravelStart(bed)).isAir()
                            || !level.getBlockState(RiverMarginSmoke.gravelLanding(level,bed)).is(net.minecraft.world.level.block.Blocks.GRAVEL))
                            throw new AssertionError("Packed shingle stability and loose gravel falling differ");
                        RiverMarginSmoke.restorePhysics(level,bed);
                        level.setChunkForced(bed.getX()>>4,bed.getZ()>>4,false);
                        Blockbranch.LOGGER.info("GRAVEL_RIVER_MARGIN_ACTIVE_PHYSICS_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()>=7) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var bed=ReedbedSmoke.fixture(generator,level.getChunkSource().randomState());
                    if(packet.stage()==7) context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                        "tp "+name+" "+(bed.getX()+0.5)+" "+(bed.getY()+8)+" "+(bed.getZ()+0.5)+" 90 "+(Boolean.getBoolean("blockbranch.reedClientOnly")?10:30));
                    else if(packet.stage()==8) {
                        level.destroyBlock(bed.above(),false);
                        if(!level.getFluidState(bed.above()).isSource()) throw new AssertionError("Reed harvest removed lake water");
                    } else {
                        ReedbedSmoke.restore(level,bed);
                        if(!level.getBlockState(bed.above()).is(ReedbedContent.LAKE_REED)) throw new AssertionError("Reed fixture did not restore");
                        Blockbranch.LOGGER.info("INLAND_LAKE_REEDBED_ACTIVE_HARVEST_PASS {}",bed);
                    }
                    return;
                }
                if(packet.stage()==5 || packet.stage()==6) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var scree=AlpineScreeSmoke.fixture(generator,level.getChunkSource().randomState());
                    var falling=scree.above(6);
                    if(packet.stage()==5) {
                        level.setChunkForced(scree.getX()>>4,scree.getZ()>>4,true);
                        level.setBlock(scree.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                        level.setBlock(falling.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                        level.setBlock(falling,ScreeContent.LOOSE_SCREE.defaultBlockState(),3);
                        level.setBlock(falling.below(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    } else {
                        if(!level.getBlockState(falling).isAir() || !level.getBlockState(scree.above()).is(ScreeContent.LOOSE_SCREE))
                            throw new AssertionError("Active scree did not fall and land");
                        level.setBlock(scree.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                        level.setChunkForced(scree.getX()>>4,scree.getZ()>>4,false);
                        Blockbranch.LOGGER.info("ALPINE_SCREE_ACTIVE_FALL_PASS {}",scree);
                    }
                    return;
                }
                if(packet.stage()==4) {
                    var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var scree=AlpineScreeSmoke.fixture(generator,level.getChunkSource().randomState());
                    var drainage=generator.drainage(level.getChunkSource().randomState());
                    double dx=drainage.sample(scree.getX()+32,scree.getZ()).height()-drainage.sample(scree.getX()-32,scree.getZ()).height();
                    double dz=drainage.sample(scree.getX(),scree.getZ()+32).height()-drainage.sample(scree.getX(),scree.getZ()-32).height();
                    // A fixed 30-degree pitch looks above steep downhill terrain.
                    // Aim at a real nearby surface while preserving the bed witness
                    // and camera position used by the native falling-block check.
                    double length=Math.hypot(dx,dz);
                    int targetX=(int)Math.round(scree.getX()-dx/length*64),targetZ=(int)Math.round(scree.getZ()-dz/length*64);
                    double targetY=drainage.sample(targetX,targetZ).height()+.5;
                    double yaw=Math.toDegrees(Math.atan2(scree.getX()-targetX,targetZ-scree.getZ()));
                    double pitch=-Math.toDegrees(Math.atan2(targetY-(scree.getY()+32+player.getEyeHeight()),Math.hypot(targetX-scree.getX(),targetZ-scree.getZ())));
                    context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                        "tp "+name+" "+(scree.getX()+0.5)+" "+(scree.getY()+32)+" "+(scree.getZ()+0.5)+" "+yaw+" "+pitch);
                    Blockbranch.LOGGER.info("SCREE_CAPTURE_CAMERA bed={} target={} yaw={} pitch={}",scree,new net.minecraft.core.BlockPos(targetX,(int)targetY,targetZ),yaw,pitch);
                    return;
                }
                context.server().getCommands().performPrefixedCommand(context.server().createCommandSourceStack(),
                    "tp "+name+ (packet.stage()==2 ? " 1401431 272 -1045106 0 90" : " 1401431 -4096 -1045106 0 90"));
            }));
            var arrivals = new java.util.concurrent.ConcurrentLinkedQueue<String>();
            int[] diagnosticTicks={0};
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                arrivals.add(handler.player.getName().getString()));
            // JOIN fires before the player is visible to command selectors.
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
                String name;
                while ((name = arrivals.poll()) != null) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator " + name);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp " + name + " 1401431 272 -1045106 0 90");
                }
                if(++diagnosticTicks[0]%100==0) for(var player:server.getPlayerList().getPlayers()) if(player.getY()<0) {
                    var level=player.level();
                    Blockbranch.LOGGER.info("REALISTIC_SERVER_ROOM sky={} block={} lamp={} resident={}",level.getBrightness(net.minecraft.world.level.LightLayer.SKY,RealisticProbe.ROOM),level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,RealisticProbe.LAMP.above()),level.getBlockState(RealisticProbe.LAMP),((blockbranch.cubic.SparseColumn)level.getChunkAt(RealisticProbe.LAMP)).blockbranch$terrain().resident(RealisticProbe.LAMP.getY()>>4));
                }
            });
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.halt(false));
        }
        if (Boolean.getBoolean("blockbranch.habitatCodecSmoke")) HabitatCodecSmoke.register();
        if (Boolean.getBoolean("blockbranch.realisticMemorySmoke")) RealisticMemorySmoke.register();
        if(System.getProperty("blockbranch.focusHabitat")!=null)FocusedHabitatSmoke.register(System.getProperty("blockbranch.focusHabitat"));
        else if (Boolean.getBoolean("blockbranch.realisticSmoke")) RealisticSmoke.register();
        if (Boolean.getBoolean("blockbranch.screeSmoke")) AlpineScreeSmoke.register();
    }
}
