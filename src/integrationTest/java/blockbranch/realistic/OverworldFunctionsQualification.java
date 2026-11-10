package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.phys.AABB;

/** Full natural sites, native block entities, loot execution, edits and real world reopen. */
public final class OverworldFunctionsQualification {
    static String site=System.getProperty("blockbranch.overworldFunctionSite","trial_chambers");
    static final List<String> SITES=Arrays.asList(System.getProperty("blockbranch.overworldFunctionSites",site).split(","));
    static{site=SITES.getFirst();}
    static int selection;
    static long admissionNanos,admissionCpu,admissionAllocated;static int stage,ticks;static StructureStart start;static List<ChunkPos> chunks=new ArrayList<>();static boolean done;
    static void require(boolean b,String s){OverworldCoverageQualification.require(b,s);}
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
        if(done)return;
        try{
            var level=server.overworld();
            if(stage==0){
                long begin=System.nanoTime(),cpu=ReimaginedRemainingQualification.cpu(),allocated=ReimaginedRemainingQualification.allocated();
                var all=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.overworldWitness","../overworld-coverage/starts.json")))).getAsJsonObject();
                var entry=all.getAsJsonObject(site);require(entry!=null,"Natural surveyed site "+site);var at=entry.getAsJsonArray("chunk");
                var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace(site)));
                var chunk=level.getChunk(at.get(0).getAsInt(),at.get(1).getAsInt(),net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
                start=level.structureManager().getStartForStructure(holder.value(),chunk);require(start!=null&&start.isValid(),"Actual natural start "+site);
                var b=start.getBoundingBox();
                for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++){
                    var p=new ChunkPos(x,z);chunks.add(p);level.setChunkForced(x,z,true);level.getChunk(x,z);
                }
                admissionNanos=System.nanoTime()-begin;admissionCpu=ReimaginedRemainingQualification.cpu()-cpu;admissionAllocated=ReimaginedRemainingQualification.allocated()-allocated;stage=1;return;
            }
            if(ticks++<20||chunks.stream().anyMatch(c->!level.areEntitiesLoaded(c.pack()))){require(ticks<2400,"Full entity admission timeout");return;}
            verify(level);server.saveEverything(false,true,true);
            if(++selection<SITES.size()){site=SITES.get(selection);stage=0;ticks=0;chunks.clear();start=null;}
            else{Files.writeString(Path.of("result.txt"),"PASS "+String.join(",",SITES));done=true;}
        }catch(Throwable t){done=true;blockbranch.Blockbranch.LOGGER.error("OVERWORLD_FUNCTION_FAIL "+site,t);try{Files.writeString(Path.of("result.txt"),"FAIL "+site+" "+t);}catch(Exception ignored){}}
        finally{if(done)server.halt(false);}
    });}
    static void priorReader(ServerLevel level)throws Exception{
        var g=(RealisticGenerator)level.getChunkSource().getGenerator();
        var seeds=new ArrayList<Object>();var structureRegistry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for(var step:net.minecraft.world.level.levelgen.GenerationStep.Decoration.values()){int oldIndex=0,rawIndex=0;for(var structure:structureRegistry){if(structure.step()!=step)continue;var id=structureRegistry.getKey(structure);boolean crypt=id.equals(ReimaginedStructures.key(ReimaginedSites.CRYPT).identifier());seeds.add(Map.of("id",id.toString(),"step",step.toString(),"rawIndex",rawIndex++,"nativeIndex",crypt?-1:oldIndex++));}}
        OverworldCoverageQualification.write("native-decoration-indices.json",seeds);

        if(Files.exists(Path.of("prior-reader.json")))return;
        var ops=RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var meta=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
        var old=meta.deepCopy();old.addProperty("shape_version",101);old.remove("native_overworld_structures");old.remove("reimagined_layouts");old.getAsJsonObject("additional_biomes").remove("deep_dark");
        try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of("../overworld-baseline-101.jar").toUri().toURL()},OverworldFunctionsQualification.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(!name.startsWith("blockbranch.realistic.RealisticGenerator"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}
            }
        }){
            var codec=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
            require(codec.parse(ops,old).result().isPresent()&&codec.parse(ops,meta).error().isPresent(),"Actual schema101 reader accepts101 and rejects102");
        }
        OverworldCoverageQualification.write("prior-reader.json",Map.of("old101Accepted",true,"new102Rejected",true,"sourceCommit","5e85838"));
    }
    static void verify(ServerLevel level)throws Exception{
        boolean reopen=Files.exists(Path.of(site+"-saved.json"));
        priorReader(level);
        var box=StructurePiece.createBoundingBox(start.getPieces().stream());
        // Native scattered pieces intentionally place their basements below their piece box.
        if(site.equals("desert_pyramid")||site.equals("jungle_pyramid"))box=new BoundingBox(box.minX(),box.minY()-(site.equals("desert_pyramid")?16:8),box.minZ(),box.maxX(),box.maxY(),box.maxZ());
        var report=new TreeMap<String,Object>();report.put("admissionNanos",admissionNanos);report.put("admissionProcessCpuNanos",admissionCpu);report.put("admissionProcessAllocatedBytes",admissionAllocated);report.put("site",site);report.put("reopen",reopen);report.put("chunks",chunks.size());report.put("bounds",box.toString());
        var counts=new TreeMap<String,Integer>();var entities=new TreeMap<String,Integer>();var bes=new ArrayList<BlockEntity>();
        for(var c:chunks)for(var be:level.getChunk(c.x(),c.z()).getBlockEntities().values())if(box.isInside(be.getBlockPos()))bes.add(be);
        for(var be:bes)counts.merge(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString(),1,Integer::sum);
        for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,AABB.of(box).inflate(16),e->true))if(AABB.of(box).inflate(16).contains(entity.position()))entities.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),1,Integer::sum);
        var pos=new BlockPos.MutableBlockPos();var portalFrames=new ArrayList<BlockPos>();
        Set<Block> interesting=Set.of(Blocks.END_PORTAL_FRAME,Blocks.END_PORTAL,Blocks.SPAWNER,Blocks.RAIL,Blocks.POWERED_RAIL,
            Blocks.TRIPWIRE,Blocks.TRIPWIRE_HOOK,Blocks.REDSTONE_WIRE,Blocks.TNT,Blocks.SCULK_SHRIEKER,Blocks.SCULK_SENSOR,Blocks.SUSPICIOUS_GRAVEL,Blocks.SUSPICIOUS_SAND,Blocks.FARMLAND,Blocks.GOLD_BLOCK,Blocks.OBSIDIAN,Blocks.CRYING_OBSIDIAN);
        for(int z=box.minZ();z<=box.maxZ();z++)for(int x=box.minX();x<=box.maxX();x++)for(int y=box.minY();y<=box.maxY();y++){
            var state=level.getBlockState(pos.set(x,y,z));if(state.is(Blocks.END_PORTAL_FRAME))portalFrames.add(pos.immutable());if(interesting.contains(state.getBlock()))counts.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),1,Integer::sum);
        }
        var tables=new ArrayList<String>();var chestPositions=new ArrayList<List<Integer>>();int items=0,maps=0;
        for(var be:bes)if(be instanceof RandomizableContainerBlockEntity chest){
            var at=be.getBlockPos();chestPositions.add(List.of(at.getX(),at.getY(),at.getZ()));
            if(reopen){require(chest.getLootTable()==null,"Loot identity remains unpacked "+at);for(int i=0;i<chest.getContainerSize();i++)require(chest.getItem(i).isEmpty(),"Consumed inventory stays empty "+at);}
            else{
                if(chest.getLootTable()!=null)tables.add(chest.getLootTable().identifier().toString());
                chest.unpackLootTable(null);
                if(java.util.stream.IntStream.range(0,chest.getContainerSize()).anyMatch(i->chest.getItem(i).get(net.minecraft.core.component.DataComponents.MAP_ID)!=null)&&chest instanceof ChestBlockEntity mapChest)report.put("treasureMap",NativeOrdinaryQualification.map(level,mapChest));
                for(int i=0;i<chest.getContainerSize();i++){
                    var item=chest.getItem(i);items+=item.getCount();if(item.get(net.minecraft.core.component.DataComponents.MAP_ID)!=null)maps++;
                    chest.removeItem(i,item.getCount());
                }chest.setChanged();
            }
        }
        report.put("blockCounts",counts);report.put("entities",entities);report.put("lootTables",tables);report.put("unpackedItems",items);report.put("maps",maps);report.put("consumedChests",chestPositions);
        OverworldCoverageQualification.write(site+"-observed.json",report);
        if(site.startsWith("village")){
            long pois=0;for(var c:chunks)pois+=level.getPoiManager().getInChunk(h->true,c,net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).count();
            report.put("pois",pois);require(pois>0,"Native village POIs");require(entities.getOrDefault("minecraft:villager",0)+entities.getOrDefault("minecraft:zombie_villager",0)>0,"Village residents");
        }
        if(site.equals("trial_chambers")){
            var spawners=bes.stream().filter(b->b instanceof TrialSpawnerBlockEntity).map(b->(TrialSpawnerBlockEntity)b).toList();
            var vaults=bes.stream().filter(b->b instanceof VaultBlockEntity).map(b->(VaultBlockEntity)b).toList();
            require(!spawners.isEmpty()&&!vaults.isEmpty(),"Native trial spawners and vaults");
            for(var spawner:spawners){require(!spawner.getTrialSpawner().normalConfig().spawnPotentialsDefinition().isEmpty(),"Normal mob configuration");require(!spawner.getTrialSpawner().ominousConfig().spawnPotentialsDefinition().isEmpty(),"Ominous mob configuration");}
            for(var vault:vaults)require(!vault.getConfig().keyItem().isEmpty(),"Native vault key configured");
            if(!reopen){
                UUID spawned=null;
                for(var spawner:spawners){for(int i=0;i<30&&spawned==null;i++)spawned=spawner.getTrialSpawner().spawnMob(level,spawner.getBlockPos()).orElse(null);if(spawned!=null)break;}
                require(spawned!=null&&level.getEntity(spawned)!=null,"Native trial mob spawned into generated room");report.put("spawnedTrialMob",spawned.toString());
            }
        }
        if(site.equals("trial_chambers")&&!Files.exists(Path.of(site+"-vault-keys.json"))){
            var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("coverage-vault-keys".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"CoverageKeys"),ClientInformation.createDefault());
            var keys=new ArrayList<Object>();
            for(boolean ominous:new boolean[]{false,true}){
                var vault=bes.stream().filter(b->b instanceof VaultBlockEntity&&b.getBlockState().getValue(VaultBlock.OMINOUS)==ominous).map(b->(VaultBlockEntity)b).findFirst().orElseThrow();
                var key=vault.getConfig().keyItem().copy();var at=vault.getBlockPos();
                var active=vault.getBlockState().setValue(VaultBlock.STATE,net.minecraft.world.level.block.entity.vault.VaultState.ACTIVE);level.setBlock(at,active,3);
                VaultBlockEntity.Server.tryInsertKey(level,at,active,vault.getConfig(),vault.getServerData(),vault.getSharedData(),player,key);
                require(key.isEmpty()&&level.getBlockState(at).getValue(VaultBlock.STATE)==net.minecraft.world.level.block.entity.vault.VaultState.UNLOCKING,"Native vault accepts key and queues reward ominous="+ominous);
                vault.setChanged();keys.add(Map.of("ominous",ominous,"position",at.toShortString(),"loot",vault.getConfig().lootTable().identifier().toString(),"nativeState",level.getBlockState(at).getValue(VaultBlock.STATE).toString()));
            }
            OverworldCoverageQualification.write(site+"-vault-keys.json",keys);
        }
        if(site.equals("trial_chambers")&&reopen&&Files.exists(Path.of(site+"-vault-keys.json"))){
            var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("coverage-vault-keys".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"CoverageKeys"),ClientInformation.createDefault());
            var previous=com.google.gson.JsonParser.parseString(Files.readString(Path.of(site+"-vault-keys.json"))).getAsJsonArray();
            for(var row:previous){var coordinates=row.getAsJsonObject().get("position").getAsString().split(", ");var at=new BlockPos(Integer.parseInt(coordinates[0]),Integer.parseInt(coordinates[1]),Integer.parseInt(coordinates[2]));
                var vault=(VaultBlockEntity)level.getBlockEntity(at);var key=vault.getConfig().keyItem().copy();int before=key.getCount();var active=vault.getBlockState().setValue(VaultBlock.STATE,net.minecraft.world.level.block.entity.vault.VaultState.ACTIVE);level.setBlock(at,active,3);
                VaultBlockEntity.Server.tryInsertKey(level,at,active,vault.getConfig(),vault.getServerData(),vault.getSharedData(),player,key);
                require(key.getCount()==before,"Saved vault player cannot claim twice");
            }report.put("rewardedPlayersPersist",true);
        }
        if(site.equals("stronghold")){
            require(portalFrames.size()==12&&bes.stream().anyMatch(b->b instanceof SpawnerBlockEntity&&b.saveWithFullMetadata(level.registryAccess()).toString().contains("minecraft:silverfish")),"Complete native portal room and silverfish identity");
            var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CoverageEyes"),ClientInformation.createDefault());
            var eyes=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_EYE,16);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,eyes);
            for(var frame:portalFrames)if(!level.getBlockState(frame).getValue(EndPortalFrameBlock.HAS_EYE)){
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(frame),Direction.UP,frame,false);
                net.minecraft.world.item.Items.ENDER_EYE.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            }
            int portals=0,y=portalFrames.getFirst().getY();
            for(int z=portalFrames.stream().mapToInt(BlockPos::getZ).min().orElseThrow();z<=portalFrames.stream().mapToInt(BlockPos::getZ).max().orElseThrow();z++)
                for(int x=portalFrames.stream().mapToInt(BlockPos::getX).min().orElseThrow();x<=portalFrames.stream().mapToInt(BlockPos::getX).max().orElseThrow();x++)if(level.getBlockState(new BlockPos(x,y,z)).is(Blocks.END_PORTAL))portals++;
            require(portals==9,"Native Eye item activates complete 3x3 End portal");report.put("activePortalBlocks",portals);
            OverworldCoverageQualification.write(Files.exists(Path.of("stronghold-portal.json"))?"stronghold-portal-reopen.json":"stronghold-portal.json",Map.of("activePortalBlocks",portals,"eyesConsumed",16-eyes.getCount(),"frameCount",portalFrames.size()));
        }
        if(site.startsWith("mineshaft")){require(counts.getOrDefault("minecraft:rail",0)>0,"Native mineshaft rails and corridors");require(bes.stream().anyMatch(b->b instanceof SpawnerBlockEntity&&b.saveWithFullMetadata(level.registryAccess()).toString().contains("minecraft:cave_spider")),"Native cave-spider spawner identity");}
        if(site.equals("ancient_city")){
            require(box.maxY()+16<=blockbranch.cubic.TerrainProfile.DEEPSLATE_TOP,"Entire city and cavern roof fit the actual deepslate layer");
            require(counts.getOrDefault("minecraft:sculk_shrieker",0)>0&&counts.getOrDefault("minecraft:sculk_sensor",0)>0,"Native ancient city sculk functionality");
            var shade=bes.stream().filter(b->b instanceof SculkShriekerBlockEntity).map(b->b.getBlockPos().above()).filter(p->level.getBlockState(p).isAir()).findFirst().orElseThrow();
            require(!level.canSeeSky(shade)&&level.getBrightness(LightLayer.SKY,shade)==0,"City cavern remains roofed and receives no skylight");
            report.put("cavernSkyLight",level.getBrightness(LightLayer.SKY,shade));
            int minimumGround=Integer.MAX_VALUE;
            for(var column:chunks){var base=((blockbranch.cubic.SparseColumn)level.getChunk(column.x(),column.z())).blockbranch$terrain().base();
                for(int z=0;z<16;z++)for(int x=0;x<16;x++)minimumGround=Math.min(minimumGround,base.groundHeight(x,z));
            }
            int loadedRoof=minimumGround-(box.maxY()+16);require(loadedRoof>=16,"Every loaded city/cavern column has actual terrain above the roof");report.put("loadedMinimumRoof",loadedRoof);
            require(bes.stream().anyMatch(b->b instanceof SculkShriekerBlockEntity&&b.getBlockState().getValue(SculkShriekerBlock.CAN_SUMMON)),"Native shrieker can summon wardens");
            var middle=box.getCenter();require(level.getNoiseBiome(middle.getX()>>2,middle.getY()>>2,middle.getZ()>>2).is(net.minecraft.world.level.biome.Biomes.DEEP_DARK),"Actual sparse city deep-dark biome");
        }
        if((site.equals("trail_ruins")||site.equals("desert_pyramid"))&&!reopen){
            var at=bes.stream().filter(b->b instanceof BrushableBlockEntity).map(BlockEntity::getBlockPos).max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
            report.put("nativeBrush",NativeOrdinaryQualification.brush(level,at));
        }
        if(site.equals("trail_ruins"))require(counts.getOrDefault("minecraft:suspicious_gravel",0)>0,"Native trail archaeology");
        if(site.equals("desert_pyramid"))require(counts.getOrDefault("minecraft:tnt",0)==9&&counts.getOrDefault("minecraft:suspicious_sand",0)>0,"Native pyramid trap and archaeology");
        if(site.equals("jungle_pyramid"))require(counts.getOrDefault("minecraft:tripwire_hook",0)>=4&&counts.getOrDefault("minecraft:dispenser",0)>=2,"Native jungle traps");
        if(site.equals("mansion"))require(entities.getOrDefault("minecraft:evoker",0)+entities.getOrDefault("minecraft:vindicator",0)>0,"Native mansion residents");
        if(site.startsWith("abandoned_camp"))require(!chestPositions.isEmpty()&&(reopen||items>0),"Native campsite loot");
        if(site.equals("monument"))require(entities.getOrDefault("minecraft:elder_guardian",0)==3&&counts.getOrDefault("minecraft:gold_block",0)==8,"Native monument elders and treasure");
        if(site.equals("shipwreck"))require(chestPositions.size()>0&&(reopen||items>0),"Native ocean wreck loot execution");
        if(site.startsWith("ruined_portal"))require(counts.getOrDefault("minecraft:obsidian",0)+counts.getOrDefault("minecraft:crying_obsidian",0)>0,"Native portal frame");
        if(site.equals("pillager_outpost")){
            var mobs=((RealisticGenerator)level.getChunkSource().getGenerator()).getMobsAt(level,level.structureManager(),net.minecraft.world.entity.MobCategory.MONSTER,box.getCenter());
            require(mobs.unwrap().stream().anyMatch(w->w.value().type()==net.minecraft.world.entity.EntityTypes.PILLAGER),"Native outpost spawn override");
        }
        var marker=new BlockPos(box.minX(),box.maxY()+2,box.minZ());
        if(reopen){var previous=com.google.gson.JsonParser.parseString(Files.readString(Path.of(site+"-saved.json"))).getAsJsonObject().getAsJsonArray("edit");marker=new BlockPos(previous.get(0).getAsInt(),previous.get(1).getAsInt(),previous.get(2).getAsInt());}
        if(reopen)require(level.getBlockState(marker).is(Blocks.DIAMOND_BLOCK),"Player edit survives world reopen");else level.setBlock(marker,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
        report.put("edit",List.of(marker.getX(),marker.getY(),marker.getZ()));
        OverworldCoverageQualification.write(site+(reopen?"-reopen.json":"-saved.json"),report);
        for(var c:chunks)level.setChunkForced(c.x(),c.z(),false);
        blockbranch.Blockbranch.LOGGER.info("OVERWORLD_FUNCTION_PASS {} reopen={} counts={} entities={}",site,reopen,counts,entities);
    }
}
