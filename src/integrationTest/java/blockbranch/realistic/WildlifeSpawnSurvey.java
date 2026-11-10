package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.mixin.wildlife.NaturalSpawnerAccess;
import blockbranch.wildlife.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

/** Real connected-player windows, native natural-spawn budgets and stored natural witnesses. */
public final class WildlifeSpawnSurvey {
    private static final int SKY=8192;
    private static ServerPlayer player;
    private static LevelChunk chunk;
    private static BlockPos ground;
    private static net.minecraft.world.phys.Vec3 camera;
    private static float yaw,pitch;
    private static WildBoar low,high;
    private static List<WildBoar> natural;
    private static int phase,ticks,lowStamp,highStamp;
    private static boolean transition;
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{restoreFloor(server.overworld());}catch(Exception error){throw new IllegalStateException("Cannot recover isolated survey roof",error);}});
        ServerPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.server().execute(()->{
            if(packet.stage()==2){for(var entity:context.player().level().getAllEntities())entity.setSilent(true);return;}
            if(packet.stage()==1&&phase==0)try{start(context.player());}catch(Throwable error){fail(error);}
        }));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(phase==0)return;try{
            if(++ticks>600)throw new AssertionError("Spawn survey timeout phase="+phase);
            var level=player.level();
            if(phase==1){
                if(ticks<40)return;
                if(((SparseColumn)chunk).blockbranch$terrain().resident(SKY>>4)){
                    if(ticks%20==0)Blockbranch.LOGGER.info("WILDLIFE_EVICTION_WAIT phase={} ticks={} highActive={}",phase,ticks,VerticalRuntime.active(level,ground.getX(),SKY,ground.getZ()));
                    return;
                }
                check(low.tickCount>=20&&high.tickCount==0,"only ground animal ticks before ascent low="+low.tickCount+" high="+high.tickCount);
                sample(level,false);
                command("tp "+player.getName().getString()+" "+(ground.getX()+.5)+" "+(SKY+3)+" "+(ground.getZ()+.5)+" 147 14");
                phase=2;ticks=0;transition=false;return;
            }
            boolean highActive=VerticalRuntime.active(level,ground.getX(),SKY+1,ground.getZ());
            boolean lowActive=VerticalRuntime.active(level,ground.getX(),ground.getY(),ground.getZ());
            if(phase==2){
                if(!transition){if(!highActive||lowActive)return;transition=true;ticks=0;lowStamp=low.tickCount;highStamp=high.tickCount;return;}
                if(ticks<40)return;
                check(low.tickCount==lowStamp&&high.tickCount-highStamp>=30,"vertical animal ticking switches without distant simulation");
                sample(level,true);
                command("tp "+player.getName().getString()+" "+camera.x+" "+camera.y+" "+camera.z+" "+yaw+" "+pitch);
                phase=3;ticks=0;transition=false;return;
            }
            if(!transition){if(!lowActive||highActive)return;transition=true;ticks=0;lowStamp=low.tickCount;highStamp=high.tickCount;return;}
            if(ticks<40)return;
            check(low.tickCount-lowStamp>=30&&high.tickCount==highStamp,"ground ticking resumes and distant sky animal stops");
            if(((SparseColumn)chunk).blockbranch$terrain().resident(SKY>>4))return;
            sample(level,false);
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)level.setBlock(new BlockPos(chunk.getPos().getMinBlockX()+x,SKY,chunk.getPos().getMinBlockZ()+z),Blocks.AIR.defaultBlockState(),2);
            ((SparseColumn)chunk).blockbranch$terrain().flush();Files.deleteIfExists(roofMarker());low.discard();high.discard();
            var lines=new ArrayList<String>();
            for(var boar:natural)lines.add(boar.getUUID()+" "+boar.getX()+" "+boar.getY()+" "+boar.getZ());
            Files.write(result().resolveSibling("natural-boar-witnesses.txt"),lines);
            server.saveEverything(false,true,true);
            Files.writeString(result(),"PASS\n");
            Blockbranch.LOGGER.info("WILDLIFE_SPAWN_SURVEY PASS naturalBoars={} nativeCap=true groundSkyGround=true inactiveEditsEvicted=true",natural.size());
            ServerPlayNetworking.send(player,new WildlifeProbe(0,"Native population and vertical-window survey PASS"));phase=0;
        }catch(Throwable error){fail(error);}});
    }
    private static void start(ServerPlayer viewer)throws Exception {
        player=viewer;var level=viewer.level();camera=viewer.position();yaw=viewer.getYRot();pitch=viewer.getXRot();
        chunk=level.getChunkAt(viewer.blockPosition());var terrain=((SparseColumn)chunk).blockbranch$terrain();
        int x=chunk.getPos().getMinBlockX()+8,z=chunk.getPos().getMinBlockZ()+8;
        ground=new BlockPos(x,terrain.base().groundHeight(x,z)+1,z);
        check(VerticalRuntime.active(level,x,ground.getY(),z),"connected player's ground simulation window");
        level.getGameRules().set(GameRules.SPAWN_MOBS,false,level.getServer());
        natural=new ArrayList<>();int deer=0,creatures=0;
        for(var entity:level.getAllEntities()){
            if(entity instanceof Mob mob&&!mob.isPersistenceRequired()&&mob.getType().getCategory()==MobCategory.CREATURE)creatures++;
            if(entity instanceof WildBoar boar&&!boar.isPersistenceRequired())natural.add(boar);
            if(entity instanceof RedDeer d&&!d.isPersistenceRequired())deer++;
        }
        Blockbranch.LOGGER.info("WILDLIFE_NATURAL_POPULATION boars={} deer={} creatures={} biome={} chunk={}",natural.size(),deer,creatures,level.getBiome(ground).unwrapKey(),chunk.getPos());
        check(!natural.isEmpty(),"no naturally generated boars in the fixed woodland review region");
        var witnessPath=result().resolveSibling("natural-boar-witnesses.txt");
        if(Files.exists(witnessPath))for(String line:Files.readAllLines(witnessPath)){
            var parts=line.split(" ");var entity=level.getEntity(UUID.fromString(parts[0]));
            check(entity instanceof WildBoar,"naturally generated UUID persists "+parts[0]);
            check(entity.position().distanceToSqr(Double.parseDouble(parts[1]),Double.parseDouble(parts[2]),Double.parseDouble(parts[3]))<.0001,"frozen natural witness position persists");
        }
        for(var boar:natural)boar.setNoAi(true);
        int eligible=0, placement=0, tagged=0, bright=0;var surfaces=new java.util.TreeMap<String,Integer>();
        for(int i=0;i<2048;i++){
            var p=NaturalSpawnerAccess.blockbranch$sample(level,chunk);
            if(SpawnPlacements.isSpawnPositionOk(WildlifeRuntime.WILD_BOAR,level,p))placement++;
            if(level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON))tagged++;
            if(level.getRawBrightness(p,0)>8)bright++;
            surfaces.merge(level.getBlockState(p.below()).toString(),1,Integer::sum);
            if(SpawnPlacements.isSpawnPositionOk(WildlifeRuntime.WILD_BOAR,level,p)
                &&SpawnPlacements.checkSpawnRules(WildlifeRuntime.WILD_BOAR,level,EntitySpawnReason.NATURAL,p,level.getRandom()))eligible++;
        }
        Blockbranch.LOGGER.info("WILDLIFE_SPAWN_ELIGIBILITY eligible={} placement={} tagged={} bright={} surfaces={}",eligible,placement,tagged,bright,surfaces);
        check(eligible>0,"real natural sampler produces eligible boar habitat positions");
        command("gamemode creative "+viewer.getName().getString());
        var capWitnesses=new ArrayList<WildBoar>();
        for(int i=0;i<MobCategory.CREATURE.getMaxInstancesPerChunk();i++){
            var boar=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);boar.snapTo(x+.5,ground.getY(),z+.5,0,0);level.addFreshEntity(boar);capWitnesses.add(boar);
        }
        var state=NaturalSpawner.createState(289,level,(key,consumer)->{
            var c=level.getChunkSource().getChunk(ChunkPos.getX(key),ChunkPos.getZ(key),ChunkStatus.FULL,false);
            if(c instanceof LevelChunk loaded)consumer.accept(loaded);
        },new LocalMobCapCalculator(level.getChunkSource().chunkMap));
        check(state.getMobCategoryCounts().getInt(MobCategory.CREATURE)>=MobCategory.CREATURE.getMaxInstancesPerChunk(),"boars count toward shared native creature budget");
        check(!NaturalSpawner.getFilteredSpawningCategories(state,true,true).contains(MobCategory.CREATURE),"native cap suppresses additional creature attempts");
        int before=state.getMobCategoryCounts().getInt(MobCategory.CREATURE);
        for(int i=0;i<32;i++)NaturalSpawner.spawnForChunk(level,chunk,state,NaturalSpawner.getFilteredSpawningCategories(state,true,true).stream().filter(c->c==MobCategory.CREATURE).toList());
        check(state.getMobCategoryCounts().getInt(MobCategory.CREATURE)==before,"repeated native capped passes add no duplicate animals");
        for(var boar:capWitnesses)boar.discard();
        Blockbranch.LOGGER.info("WILDLIFE_NATIVE_CAP counted={} eligibleBoarSamples={}/2048 capWitnesses={} nativePasses=32",before,eligible,capWitnesses.size());
        Files.writeString(roofMarker(),chunk.getPos().x()+" "+chunk.getPos().z());
        for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++)level.setBlock(new BlockPos(chunk.getPos().getMinBlockX()+dx,SKY,chunk.getPos().getMinBlockZ()+dz),Blocks.GRASS_BLOCK.defaultBlockState(),2);
        check(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)>=SKY,"high edited floor actually redirects global heightmap");
        terrain.flush();
        low=probe(level,ground);high=probe(level,new BlockPos(x,SKY+1,z));phase=1;ticks=0;
    }
    private static WildBoar probe(ServerLevel level,BlockPos position){var boar=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);boar.snapTo(position.getX()+.5,position.getY(),position.getZ()+.5,0,0);boar.setNoAi(true);boar.setSilent(true);boar.setPersistenceRequired();level.addFreshEntity(boar);return boar;}
    private static void sample(ServerLevel level,boolean sky){
        int floorHits=0;long start=System.nanoTime();
        for(int i=0;i<2048;i++){
            var p=NaturalSpawnerAccess.blockbranch$sample(level,chunk);
            check(VerticalRuntime.active(level,p.getX(),p.getY(),p.getZ()),"every attempt remains in an existing active window "+p);
            if(sky){check(p.getY()>=SKY-15&&p.getY()<=SKY+1,"active sky floor band "+p);if(p.getY()==SKY+1)floorHits++;}
            else {int top=((SparseColumn)chunk).blockbranch$terrain().base().top(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque(),p.getX(),p.getZ(),ColumnLimits.MAX_Y-1);check(p.getY()>=top-16&&p.getY()<=top,"inactive sky edit cannot mask natural ground "+p);}
        }
        if(sky)check(floorHits>16,"edited floor remains available for natural spawning");
        Blockbranch.LOGGER.info("WILDLIFE_VERTICAL_SAMPLES sky={} samples=2048 floorHits={} millis={}",sky,floorHits,(System.nanoTime()-start)/1e6);
    }
    private static void command(String value){player.level().getServer().getCommands().performPrefixedCommand(player.level().getServer().createCommandSourceStack(),value);}
    private static Path roofMarker(){return result().resolveSibling("survey-roof-recovery.txt");}
    private static void restoreFloor(ServerLevel level)throws Exception {
        if(!Files.exists(roofMarker()))return;
        var parts=Files.readString(roofMarker()).trim().split(" ");int cx=Integer.parseInt(parts[0]),cz=Integer.parseInt(parts[1]);
        var roofChunk=level.getChunk(cx,cz);
        for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++)level.setBlock(new BlockPos((cx<<4)+dx,SKY,(cz<<4)+dz),Blocks.AIR.defaultBlockState(),2);
        ((SparseColumn)roofChunk).blockbranch$terrain().flush();Files.delete(roofMarker());
        Blockbranch.LOGGER.info("WILDLIFE_SURVEY_RECOVERY restoredRoofChunk={},{}",cx,cz);
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.wildlifeSurveyResult"));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void fail(Throwable error){Blockbranch.LOGGER.error("WILDLIFE_SPAWN_SURVEY FAIL",error);try{Files.writeString(result(),"FAIL "+error+"\n");}catch(Exception ignored){}if(player!=null)ServerPlayNetworking.send(player,new WildlifeProbe(-1,error.toString().substring(0,Math.min(240,error.toString().length()))));phase=0;}
    private WildlifeSpawnSurvey(){}
}
