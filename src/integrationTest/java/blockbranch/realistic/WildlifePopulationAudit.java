package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.mixin.wildlife.NaturalSpawnerAccess;
import blockbranch.wildlife.*;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Real player, unedited habitat, ordinary native spawning. Zero sightings are evidence, not a PASS claim. */
public final class WildlifePopulationAudit {
    private record Site(String name,BlockPos bed){}
    private static final List<Site> sites=new ArrayList<>();
    private static final List<Map<String,Object>> reports=new ArrayList<>();
    private static final Map<UUID,Track> tracks=new HashMap<>();
    private static final Map<String,Integer> maxConcurrent=new TreeMap<>(),maxNativeCounts=new TreeMap<>();
    private static int spawnableChunks;
    private static com.google.gson.JsonArray previous;
    private static final List<Mob> low=new ArrayList<>(),high=new ArrayList<>();
    private static final EntityType<?>[] TYPES={WildlifeRuntime.WILD_BOAR,WildlifeRuntime.RED_DEER,WildlifeRuntime.AMERICAN_BISON,WildlifeRuntime.BROWN_BEAR,WildlifeRuntime.MALLARD,WildlifeRuntime.EURASIAN_BEAVER,WildlifeRuntime.EUROPEAN_PERCH,WildlifeRuntime.HARBOR_SEAL};
    private static ServerPlayer player;
    private static Site site;
    private static int index=-1,phase,ticks,stable,observedTicks,loadingTicks;
    private static int[] lowStamp,highStamp;
    private static final int SKY=8192;
    private static final int OBSERVE=Integer.getInteger("blockbranch.populationTicks",1200);
    private static boolean active;
    private static final class Track {
        final String type;Vec3 previous;boolean wet;Boolean stableWet;int samples,wetSamples,drySamples,transitions,groundSamples,waterEntries,landReturns;double distance;
        Track(Mob mob){type=id(mob);previous=mob.position();wet=mob.isInWater();}
        void sample(Mob mob){
            samples++;boolean now=mob.isInWater(),ground=mob.onGround()&&!now;
            if(now)wetSamples++;else drySamples++;if(now!=wet)transitions++;
            if(ground)groundSamples++;
            if(now||ground){
                if(stableWet!=null&&stableWet!=now){if(now)waterEntries++;else landReturns++;}
                stableWet=now;
            }
            distance+=previous.distanceTo(mob.position());previous=mob.position();wet=now;
        }
        Map<String,Object> data(UUID uuid){
            var out=new LinkedHashMap<String,Object>();
            out.put("uuid",uuid.toString());out.put("type",type);out.put("samples",samples);out.put("wet",wetSamples);out.put("dry",drySamples);
            out.put("contactTransitions",transitions);out.put("groundedDrySamples",groundSamples);out.put("waterEntries",waterEntries);out.put("groundedLandReturns",landReturns);
            out.put("travelBlocks",distance);out.put("lastPosition",List.of(previous.x,previous.y,previous.z));return out;
        }
    }
    public static void register(){
        WildlifeSpawnAudit.register();
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{if(player==null){player=handler.player;active=true;}});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->server.halt(false));
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;try{
            var level=player.level();
            if(sites.isEmpty()){
                Path prior=result().resolveSibling("population-observations.json");
                if(Files.exists(prior))previous=com.google.gson.JsonParser.parseString(Files.readString(prior)).getAsJsonArray();
                var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var d=g.drainage(r);
                sites.add(new Site("forest",ForestSmoke.fixture(g,r)));
                var swamp=SwampSmoke.fixture(g,r);sites.add(new Site("swamp-offset",offset(g,r,swamp,32,0)));
                if(r.seed()==424242L)sites.add(new Site("wooded-bank-offset",offset(g,r,new BlockPos(-798677,387,-1695084),32,0)));
                var snow=WildlifeNaturalSites.beachEdge(g,r,true);sites.add(new Site("seal-bank-offset",offset(g,r,snow,32,0)));
                sites.add(new Site("plains",PlainsSmoke.fixture(g,r)));
                sites.add(new Site("lake-reedbed",ReedbedSmoke.fixture(g,r)));
                var river=blockbranch.realistic.GravelRiverMarginFixtures.fixture(d,r.seed());sites.add(new Site("gravel-river",new BlockPos(river.x(),d.sample(river.x(),river.z()).height(),river.z())));
                var shore=blockbranch.realistic.LowGradientSandyShoreFixtures.fixture(d,r.seed());sites.add(new Site("sandy-shore",new BlockPos(shore.x(),d.sample(shore.x(),shore.z()).height(),shore.z())));
                sites.add(new Site("beach",WildlifeNaturalSites.beachEdge(g,r,false)));
                sites.add(new Site("rocky-shore",StonyShoreSmoke.fixture(g,r)));
                sites.add(new Site("snowy-beach",WildlifeNaturalSites.beachEdge(g,r,true)));
                String selected=System.getProperty("blockbranch.populationSites","");
                for(int side:new int[]{-1,1}){
                    String name=side<0?"beach-west":"beach-east";
                    if(selected.contains(name))try{sites.add(new Site(name,WildlifeNaturalSites.beachEdge(g,r,false,side*512)));}catch(AssertionError unavailable){Blockbranch.LOGGER.info("WILDLIFE_OPTIONAL_COAST_SITE unavailable={} reason={}",name,unavailable.getMessage());}
                }
                if(selected.contains("allocation-")){
                    if(selected.contains("allocation-scree"))sites.add(new Site("allocation-scree",AlpineScreeSmoke.fixture(g,r)));
                    if(selected.contains("allocation-cold-steppe"))sites.add(new Site("allocation-cold-steppe",ColdSteppeSmoke.fixture(g,r)));
                    if(selected.contains("allocation-tundra"))sites.add(new Site("allocation-tundra",TundraSmoke.fixture(g,r)));
                    if(selected.contains("allocation-desert"))sites.add(new Site("allocation-desert",DesertSmoke.fixture(g,r)));
                    if(selected.contains("allocation-dry-steppe"))sites.add(new Site("allocation-dry-steppe",SteppeSmoke.fixture(g,r)));
                    if(selected.contains("allocation-humid"))sites.add(new Site("allocation-humid",HumidSmoke.fixture(g,r)));
                    if(selected.contains("allocation-shelf-surface"))sites.add(new Site("allocation-shelf-surface",MarineShelfSmoke.fixture(g,r)));
                    if(selected.contains("allocation-basin-surface"))sites.add(new Site("allocation-basin-surface",DeepOceanSmoke.fixture(g,r)));
                }
                if(!selected.isBlank())sites.removeIf(s->!Set.of(selected.split(",")).contains(s.name));
                check(!sites.isEmpty(),"requested population sites exist");
                command("gamemode creative "+player.getName().getString());player.getAbilities().flying=true;player.onUpdateAbilities();
                command("time set 6000");level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);command("weather clear");
                next();return;
            }
            ++ticks;
            if(phase==-1){
                var foot=observationAnchor(level);
                boolean ready=level.isPositionEntityTicking(foot);
                if(!ready){
                    if(ticks%200==0)Blockbranch.LOGGER.info("WILDLIFE_POPULATION_LOADING site={} ticks={} view={} entitiesLoaded={} chunkTicking={} player={}",site.name,ticks,VerticalRuntime.active(level,foot.getX(),foot.getY(),foot.getZ()),level.areEntitiesLoaded(ChunkPos.pack(foot)),level.getChunkSource().isPositionTicking(ChunkPos.pack(foot)),player.position());
                    if(ticks>3600)throw new AssertionError("native entity-ticking readiness timeout "+site.name);
                    return;
                }
                loadingTicks=ticks;ticks=observedTicks=0;phase=0;WildlifeSpawnAudit.measured();
                Blockbranch.LOGGER.info("WILDLIFE_POPULATION_NATIVE_READY site={} loadingTicks={} player={}",site.name,loadingTicks,player.position());return;
            }
            if(phase==0){
                if(!level.isPositionEntityTicking(observationAnchor(level))){if(ticks>OBSERVE+3600)throw new AssertionError("native ticking lost during observation "+site.name);return;}
                if(++observedTicks>OBSERVE){finishObservation(level);return;}
                if(observedTicks%10==0)observe(level);
                if(observedTicks%200==0)Blockbranch.LOGGER.info("WILDLIFE_POPULATION_PROGRESS site={} ticks={} naturalTracked={}",site.name,observedTicks,tracks.size());
                return;
            }
            boolean groundActive=VerticalRuntime.active(level,site.bed.getX(),surfaceY(level),site.bed.getZ());
            boolean skyActive=VerticalRuntime.active(level,site.bed.getX(),SKY,site.bed.getZ());
            boolean wanted=phase==1?groundActive&&!skyActive:phase==2?skyActive&&!groundActive:groundActive&&!skyActive;
            var activeProbe=phase==2?high.getFirst():low.getFirst();
            wanted&=level.isPositionEntityTicking(activeProbe.blockPosition());
            if(ticks%100==0&&!wanted)Blockbranch.LOGGER.info("WILDLIFE_WINDOW_READINESS site={} phase={} viewGround={} viewSky={} nativeTick={} probeLoaded={} removed={} player={}",site.name,phase,groundActive,skyActive,level.isPositionEntityTicking(activeProbe.blockPosition()),level.getEntity(activeProbe.getUUID())==activeProbe,activeProbe.isRemoved(),player.position());
            if(!wanted){stable=0;if(ticks>600)throw new AssertionError("player window transition timeout "+site.name+" phase="+phase);return;}
            if(stable++==0){lowStamp=stamps(low);highStamp=stamps(high);return;}
            if(stable<40)return;
            for(int i=0;i<TYPES.length;i++){
                int dl=low.get(i).tickCount-lowStamp[i],dh=high.get(i).tickCount-highStamp[i];
                check(phase==2?dl==0&&dh>=30:dl>=30&&dh==0,"all species obey normal player window "+TYPES[i]+" phase="+phase+" ticks="+dl+"/"+dh);
            }
            Blockbranch.LOGGER.info("WILDLIFE_ALL_SPECIES_WINDOW site={} phase={} eightTypes=true lowActive={} highActive={}",site.name,phase,groundActive,skyActive);
            if(phase==1){phase=2;ticks=stable=0;move(SKY+3);}
            else if(phase==2){phase=3;ticks=stable=0;move(surfaceY(level)+2);}
            else{
                for(var mob:low)mob.discard();for(var mob:high)mob.discard();low.clear();high.clear();
                var last=reports.getLast();int returning=0;for(var uuid:tracks.keySet())if(level.getEntity(uuid) instanceof Mob)++returning;last.put("naturalWitnessesAfterVerticalReturn",returning);last.put("allEightVerticalWindow",true);
                write();next();
            }
        }catch(Throwable error){fail(error);}});
    }
    private static BlockPos observationAnchor(ServerLevel level){return new BlockPos(site.bed.getX(),surfaceY(level),site.bed.getZ());}
    private static int surfaceY(ServerLevel level){var sample=((RealisticGenerator)level.getChunkSource().getGenerator()).drainage(level.getChunkSource().randomState()).sample(site.bed.getX(),site.bed.getZ());return Math.max(sample.height()+1,sample.waterLevel()==DrainageNetwork.DRY?sample.height()+1:sample.waterLevel()+1);}
    private static void next()throws Exception{
        if(++index>=sites.size()){
            active=false;Files.writeString(result(),"PASS observations completed; zero sightings are validation gaps\n");
            Blockbranch.LOGGER.info("WILDLIFE_POPULATION_AUDIT PASS sites={} species=8 noTerrainEdits=true ordinaryPlayerWindows=true",reports.size());
            ServerPlayNetworking.send(player,new WildlifeProbe(0,"Population observations complete"));return;
        }
        site=sites.get(index);WildlifeSpawnAudit.begin(player.level(),site.bed);tracks.clear();maxConcurrent.clear();maxNativeCounts.clear();spawnableChunks=0;phase=-1;ticks=stable=observedTicks=loadingTicks=0;move(surfaceY(player.level())+2);
        Blockbranch.LOGGER.info("WILDLIFE_POPULATION_SITE {} bed={} seed={}",site.name,site.bed,player.level().getSeed());
    }
    private static BlockPos offset(RealisticGenerator generator,net.minecraft.world.level.levelgen.RandomState random,BlockPos anchor,int dx,int dz){int x=anchor.getX()+dx,z=anchor.getZ()+dz;return new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);}
    private static void move(int y){command("tp "+player.getName().getString()+" "+(site.bed.getX()+.5)+" "+y+" "+(site.bed.getZ()+.5));}
    private static void observe(ServerLevel level){
        var concurrent=new TreeMap<String,Integer>();
        for(var entity:level.getAllEntities())if(entity instanceof Mob mob&&WildlifeCombat.isAnimal(mob)&&!mob.isPersistenceRequired()&&Math.abs(mob.getX()-site.bed.getX())<=128&&Math.abs(mob.getZ()-site.bed.getZ())<=128&&VerticalRuntime.active(level,mob.getBlockX(),mob.getBlockY(),mob.getBlockZ())){
            tracks.computeIfAbsent(mob.getUUID(),ignored->new Track(mob)).sample(mob);concurrent.merge(id(mob),1,Integer::sum);
        }
        concurrent.forEach((type,count)->maxConcurrent.merge(type,count,Math::max));
        var state=level.getChunkSource().getLastSpawnState();if(state!=null){spawnableChunks=state.getSpawnableChunkCount();state.getMobCategoryCounts().forEach((category,count)->maxNativeCounts.merge(category.getName(),count,Math::max));}
    }
    private static void finishObservation(ServerLevel level)throws Exception{
        var nativeRoutes=WildlifeSpawnAudit.end();
        observe(level);var report=new LinkedHashMap<String,Object>();report.put("site",site.name);report.put("bed",site.bed.toShortString());report.put("seed",level.getSeed());report.put("observationTicks",observedTicks-1);report.put("loadingTicks",loadingTicks);report.put("terrainEdited",false);
        var counts=new TreeMap<String,Integer>();for(var type:TYPES)counts.put(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),0);for(var track:tracks.values())counts.merge(track.type,1,Integer::sum);report.put("uniqueNaturallyObserved",counts);
        report.put("peakConcurrentBySpecies",new TreeMap<>(maxConcurrent));report.put("peakNativeCapCounts",new TreeMap<>(maxNativeCounts));report.put("nativeSpawnableChunks",spawnableChunks);
        int priorLoaded=0;boolean replay=false;
        if(previous!=null)for(var old:previous){var data=old.getAsJsonObject();if(!data.get("site").getAsString().equals(site.name))continue;replay=true;for(var track:data.getAsJsonArray("tracks"))if(level.getEntity(UUID.fromString(track.getAsJsonObject().get("uuid").getAsString())) instanceof Mob)priorLoaded++;}
        report.put("savedWorldReplay",replay);report.put("priorNaturalWitnessesLoaded",priorLoaded);
        report.put("actualNativeSpawnRoutes",nativeRoutes);
        report.put("tracks",tracks.entrySet().stream().map(e->e.getValue().data(e.getKey())).toList());
        var matrix=new TreeMap<String,Object>();var eligibility=new TreeMap<String,Integer>();var eligibilityByBiome=new TreeMap<String,Integer>();var ruleChecks=new TreeMap<String,Integer>();var examples=new TreeMap<String,List<Object>>();var nearPlayerEligibility=new TreeMap<String,Integer>();var outsidePlayerEligibility=new TreeMap<String,Integer>();int samples=0,activeSamples=0;
        var center=new ChunkPos(site.bed.getX()>>4,site.bed.getZ()>>4);BlockPos woodedCandidate=null;
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){
            var chunk=level.getChunkSource().getChunkNow(center.x()+dx,center.z()+dz);if(chunk==null)continue;
            for(int i=0;i<128;i++){
                BlockPos p=NaturalSpawnerAccess.blockbranch$sample(level,chunk);samples++;if(VerticalRuntime.active(level,p.getX(),p.getY(),p.getZ()))activeSamples++;
                var holder=level.getBiome(p);var settings=level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS,p);
                String biome=holder.unwrapKey().orElseThrow().identifier().toString();
                if(!matrix.containsKey(biome)){
                    var categories=new TreeMap<String,Object>();for(var category:MobCategory.values())categories.put(category.getName(),settings.getMobsToSpawn(category).unwrap().stream().map(e->Map.of("type",BuiltInRegistries.ENTITY_TYPE.getKey(e.value().type()).toString(),"weight",e.weight(),"min",e.value().count().minInclusive(),"max",e.value().count().maxInclusive())).toList());matrix.put(biome,categories);
                }
                for(var type:java.util.stream.Stream.concat(Arrays.stream(TYPES),java.util.stream.Stream.of(EntityTypes.SALMON,EntityTypes.SQUID,EntityTypes.COD,EntityTypes.GOAT,EntityTypes.RABBIT,EntityTypes.HORSE,EntityTypes.DONKEY)).toList()){
                    if(settings.getMobsToSpawn(type.getCategory()).unwrap().stream().noneMatch(e->e.value().type()==type)||!SpawnPlacements.isSpawnPositionOk(type,level,p))continue;
                    String typeId=BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                    boolean allowed=SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.NATURAL,p,level.getRandom());ruleChecks.merge(biome+"|"+typeId+"|"+allowed,1,Integer::sum);
                    if(!allowed)continue;
                    String species=BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                    eligibility.merge(species,1,Integer::sum);eligibilityByBiome.merge(biome+"|"+species,1,Integer::sum);
                    var witnesses=examples.computeIfAbsent(biome+"|"+species,ignored->new ArrayList<>());
                    if(witnesses.size()<3)witnesses.add(Map.of("position",p.toShortString(),"floor",level.getBlockState(p.below()).toString(),"body",level.getBlockState(p).toString()));
                    (player.distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5)<=24*24?nearPlayerEligibility:outsidePlayerEligibility).merge(species,1,Integer::sum);
                    if(type==WildlifeRuntime.EURASIAN_BEAVER&&woodedCandidate==null)woodedCandidate=p;
                }
            }
        }
        int adapted=0;
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
        for(int dz=-48;dz<=48;dz+=8)for(int dx=-48;dx<=48;dx+=8){int x=site.bed.getX()+dx,z=site.bed.getZ()+dz;var water=generator.drainage(random).sample(x,z).waterLevel();if(water==DrainageNetwork.DRY||water==level.getSeaLevel())continue;
            var pos=new BlockPos(x,water-2,z);if(!level.hasChunkAt(pos)||pos.getY()>=level.getSeaLevel()-13&&pos.getY()<=level.getSeaLevel())continue;
            Boolean local=WildlifeHabitat.vanillaFreshwaterSpawn(EntityTypes.SALMON,level,pos);
            if(Boolean.TRUE.equals(local)){
                check(!net.minecraft.world.entity.animal.fish.WaterAnimal.checkSurfaceWaterAnimalSpawnRules(EntityTypes.SALMON,level,EntitySpawnReason.NATURAL,pos,level.getRandom()),"native global sea-level negative at elevated freshwater");
                check(SpawnPlacements.checkSpawnRules(EntityTypes.SALMON,level,EntitySpawnReason.NATURAL,pos,level.getRandom()),"adapted salmon depth band");
                check(SpawnPlacements.checkSpawnRules(EntityTypes.SQUID,level,EntitySpawnReason.NATURAL,pos,level.getRandom()),"adapted squid depth band");adapted++;
            }
        }
        if(woodedCandidate!=null&&System.getProperty("blockbranch.populationSites","").isBlank()&&sites.stream().noneMatch(s->s.name.equals("wooded-freshwater-bank"))){
            sites.add(index+1,new Site("wooded-freshwater-bank",woodedCandidate.below()));
            Blockbranch.LOGGER.info("WILDLIFE_WOODED_BANK_NATIVE_WITNESS position={}",woodedCandidate);
        }
        var siteWater=generator.drainage(random).sample(site.bed.getX(),site.bed.getZ());
        report.put("localWaterGeometry",Map.of("terrainY",siteWater.height(),"waterY",siteWater.waterLevel(),"depth",siteWater.waterLevel()==DrainageNetwork.DRY?0:siteWater.waterLevel()-siteWater.height()-1));
        if(site.name.contains("surface")){
            var tests=new ArrayList<Object>();
            for(int y:new int[]{level.getSeaLevel()-26,level.getSeaLevel()-25,level.getSeaLevel()-13,level.getSeaLevel()-5,level.getSeaLevel()-2,level.getSeaLevel(),siteWater.height()+1}){
                var pos=new BlockPos(site.bed.getX(),y,site.bed.getZ());var row=new LinkedHashMap<String,Object>();row.put("y",y);row.put("active",VerticalRuntime.active(level,pos.getX(),y,pos.getZ()));
                // Outside-window probes are numeric native band negatives, before any native water reads.
                if(y<level.getSeaLevel()-25||VerticalRuntime.active(level,pos.getX(),y,pos.getZ()))for(var type:List.of(EntityTypes.COD,EntityTypes.SQUID,EntityTypes.DOLPHIN,EntityTypes.NAUTILUS))row.put(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.NATURAL,pos,level.getRandom()));
                tests.add(row);
            }
            report.put("nativeMarineHeightPredicates",tests);
        }
        report.put("nativeHeightNegativeAdaptedPositivePositions",adapted);
        report.put("effectivePositionTables",matrix);report.put("naturalSamplerAttempts",samples);report.put("activeSamplerAttempts",activeSamples);report.put("eligibleSpeciesAttempts",eligibility);report.put("eligibleByBiomeAndSpecies",eligibilityByBiome);report.put("placementPredicateChecksByBiomeSpeciesResult",ruleChecks);report.put("eligibilityExamples",examples);report.put("eligibleWithinPlayerExclusion24",nearPlayerEligibility);report.put("eligibleOutsidePlayerExclusion24",outsidePlayerEligibility);reports.add(report);write();
        Blockbranch.LOGGER.info("WILDLIFE_NATURAL_OBSERVATION site={} unique={} eligible={} samples={}/{}",site.name,counts,eligibility,activeSamples,samples);
        for(var type:TYPES){low.add(probe(level,type,surfaceY(level)+1));high.add(probe(level,type,SKY));}
        phase=1;ticks=stable=0;
    }
    private static Mob probe(ServerLevel level,EntityType<?> type,int y){var mob=(Mob)type.create(level,EntitySpawnReason.COMMAND);mob.snapTo(site.bed.getX()+.5,y,site.bed.getZ()+.5,0,0);mob.addTag("blockbranch.population_window_probe");mob.setNoAi(true);mob.setNoGravity(true);mob.setPermanentlyInvulnerable(true);mob.setSilent(true);mob.setPersistenceRequired();level.addFreshEntity(mob);return mob;}
    private static int[] stamps(List<Mob> mobs){return mobs.stream().mapToInt(m->m.tickCount).toArray();}
    private static String id(Mob mob){return BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();}
    private static Path result(){return Path.of(System.getProperty("blockbranch.populationResult"));}
    private static void write()throws Exception{Files.writeString(result().resolveSibling("population-observations.json"),new GsonBuilder().setPrettyPrinting().create().toJson(reports));}
    private static void command(String text){player.level().getServer().getCommands().performPrefixedCommand(player.level().getServer().createCommandSourceStack(),text);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void fail(Throwable error){active=false;Blockbranch.LOGGER.error("WILDLIFE_POPULATION_AUDIT FAIL",error);try{Files.writeString(result(),"FAIL "+error+"\n");}catch(Exception ignored){}if(player!=null)ServerPlayNetworking.send(player,new WildlifeProbe(-1,error.toString().substring(0,Math.min(240,error.toString().length()))));}
    private WildlifePopulationAudit(){}
}
