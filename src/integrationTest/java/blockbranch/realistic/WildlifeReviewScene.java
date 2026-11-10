package blockbranch.realistic;

import blockbranch.wildlife.WildlifeRuntime;

import blockbranch.Blockbranch;
import blockbranch.realistic.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

/** Isolated review world only. Subjects hold still so morphology can be compared. */
public final class WildlifeReviewScene {
    public static void register() {
        var arrivals = new java.util.concurrent.ConcurrentLinkedQueue<String>();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> arrivals.add(handler.player.getName().getString()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.halt(false));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            String name;
            while ((name = arrivals.poll()) != null) {
                var level=server.overworld(); var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                var random=level.getChunkSource().randomState(); var root=ForestSmoke.fixture(generator,random);
                BlockPos scene=null;
                outer: for(int radius=4;radius<96;radius+=4) for(int dz=-radius;dz<=radius;dz+=4) for(int dx=-radius;dx<=radius;dx+=4) {
                    int x=root.getX()+dx,z=root.getZ()+dz,h=generator.drainage(random).sample(x,z).height();
                    boolean open=true;
                    for(int oz=-5;oz<=5&&open;oz++) for(int ox=-5;ox<=5&&open;ox++) {
                        int local=generator.drainage(random).sample(x+ox,z+oz).height();
                        if(Math.abs(local-h)>1){open=false;break;}
                        for(int y=local+1;y<local+5;y++) if(!generator.surfaceState(x+ox,y,z+oz,random).isAir()){open=false;break;}
                    }
                    if(open){scene=new BlockPos(x,h+1,z);break outer;}
                }
                if(scene==null)throw new AssertionError("No native woodland opening for wildlife review");
                if(Boolean.getBoolean("blockbranch.wildlifeScaleReview")){WildlifeScaleScene.create(server,level,scene,name);continue;}
                boolean deer=Boolean.getBoolean("blockbranch.deerReview");
                if(!deer)for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof blockbranch.wildlife.WildBoar boar && boar.isNoAi())boar.discard();
                if(deer)for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof net.minecraft.world.entity.Mob mob && mob.isNoAi()&&mob.isPersistenceRequired()&&(mob instanceof blockbranch.wildlife.RedDeer||mob instanceof blockbranch.wildlife.WildBoar))mob.discard();
                for(int i=0;i<3;i++) {
                    int x=scene.getX()+i*2-2,z=scene.getZ();
                    var column=level.getChunk(x>>4,z>>4);
                    int y=column.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)+1;
                    int expected=generator.drainage(random).sample(x,z).height()+1;
                    if(y!=expected)throw new AssertionError("Loaded native animal floor disagrees with selected opening: "+y+" != "+expected);
                    net.minecraft.world.entity.animal.Animal boar=deer?WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND):WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);
                    boar.snapTo(x+.5,y,z+.5, i==1?115:65,0);
                    Blockbranch.LOGGER.info("WILDLIFE_SUBJECT initial={} nativeHeight={} analyticFeet={} boar={}",i,y,generator.drainage(random).sample(x,z).height()+1,boar.position());
                    if(i==2)boar.setAge(-24000);
                    if(deer){((blockbranch.wildlife.RedDeer)boar).setAntlers(i==0);boar.setYBodyRot(i==1?115:65);boar.setYHeadRot(i==1?115:65);boar.setSilent(true);}
                    boar.setNoAi(true);boar.setPersistenceRequired();level.addFreshEntity(boar);
                }
                if(deer)level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                var source=server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(source,"gamemode spectator "+name);
                server.getCommands().performPrefixedCommand(source,"time set 6000");
                server.getCommands().performPrefixedCommand(source,"weather clear");
                server.getCommands().performPrefixedCommand(source,"tp "+name+" "+(scene.getX()+4.5)+" "+(scene.getY()+1)+" "+(scene.getZ()+6.5)+" 147 14");
                Blockbranch.LOGGER.info("WILDLIFE_REVIEW_SCENE root={} scene={} species={} subjects=2adults+1juvenile frozenAI=true normalVisibility=true",root,scene,deer?"red_deer":"wild_boar");
            }
        });
    }
}
