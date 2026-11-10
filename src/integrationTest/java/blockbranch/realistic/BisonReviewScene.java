package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;

/** Opt-in grassland review with a common-ground metric staging platform. */
public final class BisonReviewScene {
    private static net.minecraft.world.phys.AABB cleanupArea;
    private static ServerLevel cleanupLevel;
    private static int cleanupTicks;
    public static void register(){
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.server().execute(()->WildlifeReviewCamera.move(context.player(),packet)));
        var arrivals=new java.util.concurrent.ConcurrentLinkedQueue<ServerPlayer>();
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->arrivals.add(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.halt(false));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(cleanupTicks>0&&--cleanupTicks%5==0)
                for(var item:cleanupLevel.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,cleanupArea))item.discard();
            ServerPlayer player;while((player=arrivals.poll())!=null){
                var level=player.level();var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
                var random=level.getChunkSource().randomState();var root=PlainsSmoke.fixture(generator,random);
                BlockPos scene=null;
                search:for(int radius=0;radius<=96;radius+=8)for(int dz=-radius;dz<=radius;dz+=8)for(int dx=-radius;dx<=radius;dx+=8){
                    if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
                    int cx=root.getX()+dx,cz=root.getZ()+dz,h=generator.drainage(random).sample(cx,cz).height();
                    // Keep the metric floor and native plant tops in one section.
                    // Clearing a boundary's only plants legitimately empties a required base surface.
                    if((h&15)<2||(h&15)>10||!generator.surfaceState(cx,h,cz,random).is(Blocks.GRASS_BLOCK))continue;
                    boolean flat=true;
                    for(int oz=-3;oz<=12&&flat;oz++)for(int ox=-8;ox<=10;ox++)if(Math.abs(generator.drainage(random).sample(cx+ox,cz+oz).height()-h)>1){flat=false;break;}
                    if(flat&&level.getBiome(new BlockPos(cx,h,cz)).is(net.minecraft.world.level.biome.Biomes.PLAINS)){scene=new BlockPos(cx,h+1,cz);break search;}
                }
                if(scene==null)throw new AssertionError("No native plains metric site away from section boundary");
                int x=scene.getX(),y=scene.getY(),z=scene.getZ();
                for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof AmericanBison animal&&animal.isNoAi()&&animal.isPersistenceRequired())animal.discard();
                for(int dx=-8;dx<=10;dx++)for(int dz=-3;dz<=12;dz++){
                    level.setBlock(new BlockPos(x+dx,y-1,z+dz),((dx+dz)&1)==0?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.POLISHED_ANDESITE.defaultBlockState(),2);
                    for(int dy=0;dy<4;dy++)level.setBlock(new BlockPos(x+dx,y+dy,z+dz),Blocks.AIR.defaultBlockState(),2);
                }
                for(int i=0;i<3;i++){
                    var animal=WildlifeRuntime.AMERICAN_BISON.create(level,EntitySpawnReason.COMMAND);
                    animal.snapTo(x-4+i*4+.5,y,z+.5,90,0);animal.setYBodyRot(90);animal.setYHeadRot(90);
                    if(i==2)animal.setAge(-24000);
                    animal.setNoAi(true);animal.setSilent(true);animal.setPersistenceRequired();level.addFreshEntity(animal);
                }
                if(Math.abs(player.getBbWidth()-.6F)>.00001||Math.abs(player.getBbHeight()-1.8F)>.00001)throw new AssertionError("Native player dimensions changed");
                command(server,"kill @e[type=minecraft:block_display]");command(server,"kill @e[type=minecraft:text_display]");
                command(server,"summon minecraft:block_display "+(x+7.2)+" "+y+" "+(z+.2)+" {block_state:\"minecraft:lime_concrete\",transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],scale:[0.6f,1.8f,0.6f],right_rotation:[0f,0f,0f,1f]}}");
                command(server,"data get entity @e[type=minecraft:block_display,limit=1] transformation");
                for(int i=0;i<3;i++)command(server,"summon minecraft:text_display "+(x-4+i*4+.5)+" "+(y+2.2)+" "+(z+.5)+" {text:\""+(i==2?"Calf: .875 blocks":"Adult: 1.75 blocks")+"\",billboard:\"center\",background:1073741824}");
                for(int dy=0;dy<3;dy++)level.setBlock(new BlockPos(x+9,y+dy,z),Blocks.CONCRETE.pick(dy%2==0?DyeColor.WHITE:DyeColor.BLACK).defaultBlockState(),2);
                command(server,"summon minecraft:text_display "+(x+7.5)+" "+(y+2.5)+" "+(z+.5)+" {text:\"Player: 1.8 blocks\",billboard:\"center\",background:1073741824}");
                command(server,"summon minecraft:text_display "+(x+9.5)+" "+(y+3.5)+" "+(z+.5)+" {text:\"1 block per band\",billboard:\"center\",background:1073741824}");
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                command(server,"gamemode spectator "+player.getName().getString());command(server,"time set 6000");command(server,"weather clear");
                command(server,"tp "+player.getName().getString()+" "+(x+1.5)+" "+(y+1.5)+" "+(z+10.5)+" 180 12");
                cleanupLevel=level;cleanupArea=new net.minecraft.world.phys.AABB(x-10,y-2,z-5,x+12,y+6,z+15);cleanupTicks=120;
                Blockbranch.LOGGER.info("BISON_REVIEW_SCENE position={} subjects=2adults+1calf sameGround=true stagedMetricFloor=true nativePlains=true playerMarker=.6x1.8x.6",scene);
            }
        });
    }
    private static void command(net.minecraft.server.MinecraftServer server,String command){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);}
    private BisonReviewScene(){}
}
