package blockbranch.wildlife;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;

/** Read-only, opt-in counters for actual native spawn routes. Never performs a spawn or consumes RNG. */
public final class WildlifeSpawnAudit {
    public static final boolean ENABLED=Boolean.getBoolean("blockbranch.populationAudit");
    private static volatile ServerLevel level;
    private static volatile BlockPos center;
    private static volatile String stage;
    private static final Map<String,LongAdder> counts=new ConcurrentHashMap<>();
    private static final Map<UUID,EntitySpawnReason> finalized=new ConcurrentHashMap<>();
    private static final List<Map<String,Object>> rejections=new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final ThreadLocal<MobCategory> category=new ThreadLocal<>();
    private record Selection(net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData data,BlockPos pos,String biome){}
    private static final ThreadLocal<Selection> origin=new ThreadLocal<>();
    public static void register(){
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(entity instanceof Mob mob&&inside(world,mob.blockPosition())&&target(mob.getType())){
                var reason=finalized.remove(mob.getUUID());
                if(reason!=null)add("loaded."+id(mob.getType())+"."+reason);
            }
        });
    }
    public static void begin(ServerLevel world,BlockPos at){stage=null;counts.clear();finalized.clear();rejections.clear();level=world;center=at;stage="loading";}
    public static void measured(){stage="measured";}
    public static Map<String,Object> end(){stage=null;var out=new TreeMap<String,Object>();counts.forEach((key,value)->out.put(key,value.sum()));out.put("firstTypeRejections",List.copyOf(rejections));return out;}
    private static boolean inside(ServerLevel world,BlockPos pos){var at=center;return stage!=null&&world==level&&at!=null&&Math.abs(pos.getX()-at.getX())<=128&&Math.abs(pos.getZ()-at.getZ())<=128;}
    private static void add(String key){String current=stage;if(current!=null)counts.computeIfAbsent(current+"."+key,ignored->new LongAdder()).increment();}
    public static boolean target(EntityType<?> type){return type==WildlifeRuntime.MALLARD||type==WildlifeRuntime.EURASIAN_BEAVER||type==WildlifeRuntime.HARBOR_SEAL;}
    private static String id(EntityType<?> type){return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();}
    public static void categories(net.minecraft.world.level.NaturalSpawner.SpawnState state,boolean persistentInterval,List<MobCategory> selected){if(stage!=null&&level!=null&&level.getChunkSource().getLastSpawnState()==state&&persistentInterval)add(selected.contains(MobCategory.CREATURE)?"creatureInterval.allowed":"creatureInterval.globalCapBlocked");}
    public static void candidateChunk(ServerLevel world,ChunkPos pos,List<MobCategory> selected){if(inside(world,new BlockPos(pos.getMinBlockX()+8,0,pos.getMinBlockZ()+8))&&selected.contains(MobCategory.CREATURE))add("creatureChunk.beforeLocalCap");}
    public static void enter(ServerLevel world,ChunkPos pos,MobCategory value){category.set(value);if(value==MobCategory.CREATURE&&inside(world,new BlockPos(pos.getMinBlockX()+8,0,pos.getMinBlockZ()+8)))add("creatureChunk.afterLocalCap");}
    public static void leave(){category.remove();}
    public static void sampled(ServerLevel world,BlockPos pos){if(category.get()==MobCategory.CREATURE&&inside(world,pos)){
        add("creatureAnchors.total");
        if(!blockbranch.cubic.VerticalRuntime.active(world,pos.getX(),pos.getY(),pos.getZ())){add("creatureAnchors.inactiveSentinel");return;}
        if(world.getBlockState(pos).isRedstoneConductor(world,pos))add("creatureAnchors.conductorEarlyReturn");
    }}
    public static void selection(ServerLevel world,BlockPos pos,MobCategory value,Optional<net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData> selected){
        if(value!=MobCategory.CREATURE||!inside(world,pos))return;
        if(selected.isPresent())origin.set(new Selection(selected.get(),pos.immutable(),world.getBiome(pos).unwrapKey().orElseThrow().identifier().toString()));else origin.remove();
        add("selectionCalls");add(selected.map(data->"selected."+id(data.type())).orElse("selected.empty"));
    }
    public static void validType(ServerLevel world,BlockPos pos,net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData selected,boolean result){EntityType<?> type=selected.type();if(target(type)&&inside(world,pos)){
        add("validType."+id(type)+"."+result);
        if(!result&&blockbranch.cubic.VerticalRuntime.active(world,pos.getX(),pos.getY(),pos.getZ())){
            boolean placement=SpawnPlacements.isSpawnPositionOk(type,world,pos);
            var settings=world.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,pos);
            boolean registered=settings.getMobsToSpawn(type.getCategory()).unwrap().stream().anyMatch(e->e.value().type()==type);
            boolean exact=world.getChunkSource().getGenerator().getMobsAt(world,world.structureManager(),type.getCategory(),pos).contains(selected);
            add("typeRejection."+id(type)+".placement_"+placement+".registered_"+registered+".exactEntry_"+exact);
            if(rejections.size()<32){var data=new LinkedHashMap<String,Object>();data.put("stage",stage);data.put("type",id(type));data.put("position",pos.toShortString());data.put("biome",world.getBiome(pos).unwrapKey().orElseThrow().identifier().toString());data.put("floor",world.getBlockState(pos.below()).toString());data.put("body",world.getBlockState(pos).toString());data.put("placement",placement);data.put("registered",registered);data.put("exactEntry",exact);var first=origin.get();if(first!=null&&first.data()==selected){data.put("selectedAt",first.pos().toShortString());data.put("selectedBiome",first.biome());}rejections.add(data);}
        }
    }}
    public static void validMob(ServerLevel world,Mob mob,boolean result){if(target(mob.getType())&&inside(world,mob.blockPosition()))add("validMob."+id(mob.getType())+"."+result);}
    public static void predicate(ServerLevel world,BlockPos pos,EntityType<?> type,EntitySpawnReason reason,boolean result){if(target(type)&&inside(world,pos))add("placementPredicate."+id(type)+"."+reason+"."+result);}
    public static void finalized(ServerLevel world,Mob mob,EntitySpawnReason reason){if(target(mob.getType())&&inside(world,mob.blockPosition())&&(reason==EntitySpawnReason.NATURAL||reason==EntitySpawnReason.CHUNK_GENERATION)){
        add("finalized."+id(mob.getType())+"."+reason);finalized.put(mob.getUUID(),reason);
    }}
    private WildlifeSpawnAudit(){}
}
