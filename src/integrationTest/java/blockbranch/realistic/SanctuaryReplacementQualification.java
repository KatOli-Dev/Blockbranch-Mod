package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;

/** Native replacement qualification: gameplay remains exact while the exterior may change. */
public final class SanctuaryReplacementQualification {
    public static Map<String,Object> verify(ServerLevel level,ReimaginedSanctuaryPiece piece,StructureStart start,Path dir,boolean reopen)throws Exception {
        var definition=(RealisticGenerator)level.getChunkSource().getGenerator();
        if((piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getIntOr("SanctuaryStonework",0)>0)!=(definition.reimaginedDefinition()>=3))throw new AssertionError("Sanctuary appearance dispatch matches saved generator definition");
        int routes=ReimaginedRoutes.sanctuary(level,piece);
        int doors=(int)invoke("doorways",new Class[]{ReimaginedSanctuaryPiece.class,Function.class,boolean.class},piece,(Function<BlockPos,BlockState>)level::getBlockState,true);
        var core=(OceanMonumentPieces.MonumentBuilding)start.getPieces().getFirst();
        var box=core.getBoundingBox();
        var digest=java.security.MessageDigest.getInstance("SHA-256");int gold=0,sponges=0;
        for(var p:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())) {
            var state=level.getBlockState(p);digest.update((state+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(state.is(Blocks.GOLD_BLOCK))gold++;if(state.is(Blocks.WET_SPONGE))sponges++;
        }
        var harvested=harvestEdits(dir);int consumed=harvested.isEmpty()?0:1;
        if(gold!=8-consumed||sponges!=31-consumed)throw new AssertionError("Native loot quota after explicit player harvest: gold="+gold+" sponge="+sponges);
        var elders=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1).inflate(8)).stream().filter(m->m.getType()==net.minecraft.world.entity.EntityTypes.ELDER_GUARDIAN).map(m->m.getUUID().toString()).sorted().toList();
        if(elders.size()!=3)throw new AssertionError("Exactly three original elder guardians: "+elders);
        var record=dir.resolve("elders.json");var gson=new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        if(reopen){if(!com.google.gson.JsonParser.parseString(Files.readString(record)).equals(gson.toJsonTree(elders)))throw new AssertionError("Saved elder UUIDs preserved");}
        else Files.writeString(record,gson.toJson(elders));
        boolean playerRepair=piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getIntOr("SanctuaryReplacement",0)==1;
        if(playerRepair){var edit=piece.local(-12,-20,-12);
            if(reopen){if(!level.getBlockState(edit).is(Blocks.OAK_PLANKS))throw new AssertionError("Saved player foundation repair");}
            else level.setBlock(edit,Blocks.OAK_PLANKS.defaultBlockState(),3);
        }
        var report=new LinkedHashMap<String,Object>();report.put("ringRouteCells",routes);report.put("fullWidthThresholdCells",doors);report.put("nativeCoreHash",HexFormat.of().formatHex(digest.digest()));report.put("nativeGold",gold);report.put("nativeWetSponge",sponges);report.put("elderUUIDs",elders);report.put("nativeChildren",ReimaginedRemainingQualification.childBoxes(core));
        var children=dir.resolve("native-children.json");var childRecord=gson.toJsonTree(ReimaginedRemainingQualification.childBoxes(core));
        if(reopen){if(!(Files.exists(children)?com.google.gson.JsonParser.parseString(Files.readString(children)):com.google.gson.JsonParser.parseString(Files.readString(dir.resolve("qualification.json"))).getAsJsonObject().get("nativeChildren")).equals(childRecord))throw new AssertionError("Every native fitted child box preserved on reload");}else Files.writeString(children,gson.toJson(childRecord));
        var generator=(RealisticGenerator)level.getChunkSource().getGenerator();
        var inside=new BlockPos((box.minX()+box.maxX())/2,box.minY()+5,(box.minZ()+box.maxZ())/2);var outside=piece.local(-10,5,-10);
        if(generator.getMobsAt(level,level.structureManager(),net.minecraft.world.entity.MobCategory.MONSTER,inside).unwrap().stream().noneMatch(e->e.value().type()==net.minecraft.world.entity.EntityTypes.GUARDIAN)||generator.getMobsAt(level,level.structureManager(),net.minecraft.world.entity.MobCategory.MONSTER,outside).unwrap().stream().anyMatch(e->e.value().type()==net.minecraft.world.entity.EntityTypes.GUARDIAN))throw new AssertionError("Native guardian override stays confined to core");
        report.put("connectedSwimmingCells",invoke("swimming",new Class[]{ServerLevel.class,ReimaginedSanctuaryPiece.class},level,piece));
        report.put("guardianSpawnBounds",true);report.put("savedPlayerRepair",playerRepair);report.put("savedNativeChildGraph",true);
        report.put("nativeReplacement",protectedGameplay(level,piece,start,dir,reopen));
        report.put("codec",codec(level,dir));
        report.put("fullLayoutEmitters",emitters(level,piece,dir,reopen));
        report.put("originalNativeGold",8);report.put("originalNativeWetSponge",31);
        report.put("playerHarvest",harvest(level,piece,dir,reopen));
        return report;
    }
    private static Object invoke(String name,Class<?>[] types,Object... arguments)throws Exception {
        var method=SanctuaryQualification.class.getDeclaredMethod(name,types);method.setAccessible(true);
        try{return method.invoke(null,arguments);}catch(java.lang.reflect.InvocationTargetException failure){if(failure.getCause() instanceof Error e)throw e;throw failure;}
    }
    private static Map<String,Object> emitters(ServerLevel level,ReimaginedSanctuaryPiece piece,Path dir,boolean reopen)throws Exception {
        var cells=new ArrayList<Object>();var digest=java.security.MessageDigest.getInstance("SHA-256");var compact=new com.google.gson.Gson();
        for(int y=-34;y<=36;y++)for(int z=-15;z<=72;z++)for(int x=-15;x<=72;x++){
            var state=level.getBlockState(piece.local(x,y,z));if(state.getLightEmission()==0)continue;
            var row=Map.of("x",x,"y",y,"z",z,"state",state.toString(),"emission",state.getLightEmission());cells.add(row);
            digest.update((x+","+y+","+z+":"+state+":"+state.getLightEmission()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        var file=dir.resolve((reopen?"reopen-":"")+"full-layout-emitters.json");if(Files.exists(file))throw new AssertionError("Preserve final layout emitter evidence");
        Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(cells));
        return Map.of("cells",cells.size(),"sha256",HexFormat.of().formatHex(digest.digest()),"file",file.getFileName().toString());
    }
    private static Map<BlockPos,String> harvestEdits(Path dir)throws Exception {
        var file=dir.resolve("player-harvest.json");var edits=new HashMap<BlockPos,String>();if(!Files.exists(file))return edits;
        var record=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        for(String key:List.of("gold","sponge")){var p=record.getAsJsonArray(key);edits.put(new BlockPos(p.get(0).getAsInt(),p.get(1).getAsInt(),p.get(2).getAsInt()),Blocks.WATER.defaultBlockState().toString());}
        if(edits.size()!=2)throw new AssertionError("Exactly two explicitly recorded harvested native blocks");return edits;
    }
    private static Map<String,Object> harvest(ServerLevel level,ReimaginedSanctuaryPiece piece,Path dir,boolean reopen)throws Exception {
        boolean replacement=piece.createTag(StructurePieceSerializationContext.fromLevel(level)).getIntOr("SanctuaryReplacement",0)==1;
        if(!replacement)return Map.of("performed",false,"old172Unchanged",true);
        var player=level.getServer().getPlayerList().getPlayers().getFirst();var file=dir.resolve("player-harvest.json");
        if(reopen){
            var edits=harvestEdits(dir);if(edits.size()!=2)throw new AssertionError("Saved replacement harvest witness");
            for(var p:edits.keySet())if(!level.getBlockState(piece.local(p.getX(),p.getY(),p.getZ())).is(Blocks.WATER))throw new AssertionError("Consumed native loot must not regenerate "+p);
            int gold=0,sponge=0;for(int slot=0;slot<player.getInventory().getContainerSize();slot++){var stack=player.getInventory().getItem(slot);if(stack.is(net.minecraft.world.item.Items.GOLD_BLOCK))gold+=stack.getCount();if(stack.is(net.minecraft.world.item.Items.WET_SPONGE))sponge+=stack.getCount();}
            if(gold<1||sponge<1)throw new AssertionError("Actual harvested gold and sponge items remain in player inventory");return Map.of("performed",false,"savedConsumedCells",2,"goldItems",gold,"wetSpongeItems",sponge,"remainingNativeGold",7,"remainingNativeWetSponge",30);
        }
        if(Files.exists(file))throw new AssertionError("Preserve original native harvest witness");
        var candidates=com.google.gson.JsonParser.parseString(Files.readString(dir.resolve("native-protected-features.json"))).getAsJsonObject().getAsJsonArray("blocks");
        BlockPos gold=null,sponge=null;for(var entry:candidates){var row=entry.getAsJsonObject();var a=row.getAsJsonArray("local");var p=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());String state=row.get("state").getAsString();if(gold==null&&state.contains("minecraft:gold_block"))gold=p;if(sponge==null&&state.contains("minecraft:wet_sponge"))sponge=p;}
        if(gold==null||sponge==null)throw new AssertionError("Known actual native harvest locations");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        try {
            for(var p:List.of(gold,sponge)){var at=piece.local(p.getX(),p.getY(),p.getZ());player.setPos(at.getX()+.5,at.getY()+1,at.getZ()+1.5);
                var item=p.equals(gold)?net.minecraft.world.item.Items.GOLD_BLOCK:net.minecraft.world.item.Items.WET_SPONGE;
                if(!player.gameMode.destroyBlock(at))throw new AssertionError("Native survival harvest succeeds "+p);
                var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(3)).stream().filter(e->e.getItem().is(item)).toList();
                if(drops.stream().mapToInt(e->e.getItem().getCount()).sum()!=1)throw new AssertionError("Native harvest drops exactly one expected item "+p);
                for(var entity:drops){if(!player.getInventory().add(entity.getItem().copy()))throw new AssertionError("Collect actual native harvested item");entity.discard();}
                // Settle the vacated underwater cell before the architecture save witness.
                level.setBlock(at,Blocks.WATER.defaultBlockState(),3);
            }
        }finally{player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);}
        Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("gold",List.of(gold.getX(),gold.getY(),gold.getZ()),"sponge",List.of(sponge.getX(),sponge.getY(),sponge.getZ()),"nativeSurvivalHarvest",true,"goldDrops",1,"wetSpongeDrops",1,"settledCell",Blocks.WATER.defaultBlockState().toString())));
        return Map.of("performed",true,"nativeSurvivalHarvest",true,"goldDrops",1,"wetSpongeDrops",1,"remainingNativeGold",7,"remainingNativeWetSponge",30);
    }
    private static Map<BlockPos,String> voxels(Path file)throws Exception {
        var result=new HashMap<BlockPos,String>();
        try(var lines=Files.lines(file)){lines.forEach(line->{var row=com.google.gson.JsonParser.parseString(line).getAsJsonObject();result.put(new BlockPos(row.get("x").getAsInt(),row.get("y").getAsInt(),row.get("z").getAsInt()),row.get("state").getAsString());});}
        return result;
    }
    private static Map<String,Object> protectedGameplay(ServerLevel level,ReimaginedSanctuaryPiece piece,StructureStart start,Path dir,boolean reopen)throws Exception {
        String reference=System.getProperty("blockbranch.sanctuaryCoreReference","");
        Path baseline=reference.isEmpty()?dir:Path.of(reference);
        var source=piece.createTag(StructurePieceSerializationContext.fromLevel(level));int replacement=source.getIntOr("SanctuaryReplacement",0);
        var original=voxels(baseline.resolve("native-core.voxels.jsonl"));
        var edits=harvestEdits(dir);var expected=new HashMap<>(original);expected.putAll(edits);
        if(!edits.isEmpty()){
            long goldEdits=edits.keySet().stream().filter(p->Blocks.GOLD_BLOCK.defaultBlockState().toString().equals(original.get(p))).count();
            long spongeEdits=edits.keySet().stream().filter(p->Blocks.WET_SPONGE.defaultBlockState().toString().equals(original.get(p))).count();
            if(goldEdits!=1||spongeEdits!=1)throw new AssertionError("Harvest exemptions are exactly one original native gold block and wet sponge");
        }
        var foundations=voxels(baseline.resolve("native-foundation.voxels.jsonl"));
        var rooms=com.google.gson.JsonParser.parseString(Files.readString(baseline.resolve("native-child-rooms.json"))).getAsJsonArray();
        var protectedCells=new HashSet<BlockPos>();
        for(var entry:rooms){var room=entry.getAsJsonObject();var b=room.getAsJsonArray("localBounds");int lo=b.get(1).getAsInt(),hi=b.get(4).getAsInt(),maxZ=b.get(5).getAsInt();String type=room.get("class").getAsString();
            // Native room writers emit ceilings one cell above the room grid. The elder wing
            // writes through its back connector; the penthouse also writes the floor below its box.
            if(type.equals("OceanMonumentPenthouse"))lo--;else if(type.equals("OceanMonumentWingRoom"))maxZ++;else hi++;
            for(var pos:BlockPos.betweenClosed(b.get(0).getAsInt(),lo,b.get(2).getAsInt(),b.get(3).getAsInt(),hi,maxZ))protectedCells.add(pos.immutable());
        }
        for(var pos:BlockPos.betweenClosed(25,0,0,32,8,20))protectedCells.add(pos.immutable());
        for(var pos:BlockPos.betweenClosed(25,0,21,32,4,21))protectedCells.add(pos.immutable());
        var featureRecord=com.google.gson.JsonParser.parseString(Files.readString(baseline.resolve("native-protected-features.json"))).getAsJsonObject();
        var expectedEmitters=new HashMap<BlockPos,String>();
        for(var entry:featureRecord.getAsJsonArray("blocks")){var row=entry.getAsJsonObject();var p=row.getAsJsonArray("local");var pos=new BlockPos(p.get(0).getAsInt(),p.get(1).getAsInt(),p.get(2).getAsInt());protectedCells.add(pos);if(row.get("emission").getAsInt()>0)expectedEmitters.put(pos,row.get("state").getAsString());}
        var actual=new HashMap<BlockPos,String>();var actualEmitters=new HashMap<BlockPos,String>();int changed=0,shaped=0;
        var allowed=Set.of(Blocks.PRISMARINE,Blocks.PRISMARINE_BRICKS,Blocks.DARK_PRISMARINE,Blocks.WATER,Blocks.GOLD_BLOCK,Blocks.WET_SPONGE,Blocks.SEA_LANTERN,Blocks.PRISMARINE_BRICK_STAIRS,Blocks.DARK_PRISMARINE_SLAB,Blocks.PRISMARINE_BRICK_SLAB);
        for(var entry:original.entrySet()){
            var p=entry.getKey();var at=piece.local(p.getX(),p.getY(),p.getZ());var state=level.getBlockState(at);String value=state.toString();actual.put(p,value);
            if(!value.equals(entry.getValue())&&!edits.containsKey(p))changed++;
            if(!allowed.contains(state.getBlock()))throw new AssertionError("Native replacement material family "+p+" "+state);
            if(state.getLightEmission()>0)actualEmitters.put(p,value);
            if(state.is(Blocks.PRISMARINE_BRICK_STAIRS)||state.is(Blocks.DARK_PRISMARINE_SLAB)||state.is(Blocks.PRISMARINE_BRICK_SLAB)){if(!state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))throw new AssertionError("Flooded roof shape is waterlogged "+p);shaped++;}
        }
        int protectedExact=0,roomMasonryRemaps=0,coffers=0,niches=0;var reliefCells=new HashSet<BlockPos>();
        for(var p:protectedCells){
            if(Objects.equals(expected.get(p),actual.get(p))){protectedExact++;continue;}
            var now=level.getBlockState(piece.local(p.getX(),p.getY(),p.getZ()));
            if(replacement==1&&sameMasonryGameplay(expected.get(p),now,p)){roomMasonryRemaps++;continue;}
            if(replacement==1&&masonry(expected.get(p))){int relief=roomRelief(level,piece,p,now,original);if(relief>0){reliefCells.add(p);if(relief==1)coffers++;else niches++;continue;}}
            throw new AssertionError("Protected native gameplay changed "+p+" expected="+expected.get(p)+" actual="+actual.get(p));
        }
        if(replacement==1&&(coffers==0||niches==0))throw new AssertionError("Replacement has actual backed ceiling coffers and closed-back wall niches");
        if(expectedEmitters.size()!=126||!expectedEmitters.equals(actualEmitters))throw new AssertionError("Every original native emitter and no extra native light sources");
        var footingAudit=footings(level,piece,foundations,replacement,original,protectedCells);
        var originalNodes=feet(expected,protectedCells);var currentNodes=feet(actual,protectedCells);
        if(!originalNodes.equals(currentNodes))throw new AssertionError("Native protected standing/swimming route cells preserved");
        var before=components(originalNodes);var after=components(currentNodes);if(!before.equals(after))throw new AssertionError("Native protected route BFS components");
        var obstructed=new HashMap<>(actual);var obstruction=new BlockPos(28,1,25);obstructed.put(obstruction,Blocks.PRISMARINE_BRICKS.defaultBlockState().toString());
        if(feet(obstructed,protectedCells).equals(originalNodes)||components(feet(obstructed,protectedCells)).equals(before))throw new AssertionError("Native route obstruction negative control missed");
        var generatorMetadata=RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),(RealisticGenerator)level.getChunkSource().getGenerator()).getOrThrow().getAsJsonObject();
        int definition=generatorMetadata.has("sanctuary_replacement_definition")?generatorMetadata.get("sanctuary_replacement_definition").getAsInt():0;
        if(replacement!=definition)throw new AssertionError("Replacement dispatch follows independent saved sanctuary definition");
        if(!reference.isEmpty()&&(replacement==1?changed==0:changed!=0))throw new AssertionError("New replacement changes its exterior; old saved definition keeps its exact native core");
        var old=source.copy();old.remove("SanctuaryReplacement");var zero=old.copy();zero.putInt("SanctuaryReplacement",0);var context=StructurePieceSerializationContext.fromLevel(level);
        var paint=SanctuaryQualification.class.getDeclaredMethod("paint",StructurePieceSerializationContext.class,net.minecraft.nbt.CompoundTag.class,boolean.class);paint.setAccessible(true);
        if(!paint.invoke(null,context,old,false).equals(paint.invoke(null,context,zero,true)))throw new AssertionError("Old missing replacement flag equals explicit zero in opposite chunk orders");
        if(replacement==1){
            for(int bad:new int[]{-1,2,Integer.MAX_VALUE}){var tag=source.copy();tag.putInt("SanctuaryReplacement",bad);rejectReplacement(context,tag);}
            var tag=source.copy();tag.putString("SanctuaryReplacement","1");rejectReplacement(context,tag);
            tag=source.copy();tag.putFloat("SanctuaryReplacement",1.5F);rejectReplacement(context,tag);
            tag=source.copy();tag.putByte("SanctuaryReplacement",(byte)1);rejectReplacement(context,tag);
            tag=source.copy();tag.putLong("SanctuaryReplacement",4294967297L);rejectReplacement(context,tag);
        }

        var childShape=com.google.gson.JsonParser.parseString(Files.readString((Files.exists(dir.resolve("reopen-native-child-rooms.json"))?dir.resolve("reopen-native-child-rooms.json"):dir.resolve("native-child-rooms.json"))));
        if(!childShape.equals(rooms))throw new AssertionError("Actual native child classes and fitted local bounds preserved");
        var ordered=new ArrayList<>(protectedCells);ordered.sort(Comparator.comparingInt((BlockPos p)->p.getY()).thenComparingInt(p->p.getZ()).thenComparingInt(p->p.getX()));
        var lightDigest=java.security.MessageDigest.getInstance("SHA-256");
        var lightFile=dir.resolve((reopen?"reopen-":"")+"protected-native-light.jsonl");
        if(Files.exists(lightFile))throw new AssertionError("Preserve prior protected light evidence");
        var compact=new com.google.gson.Gson();var lightLevels=new HashMap<BlockPos,Integer>();
        try(var writer=Files.newBufferedWriter(lightFile)){for(var p:ordered){int light=level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,piece.local(p.getX(),p.getY(),p.getZ()));lightLevels.put(p,light);lightDigest.update((p.toShortString()+":"+light+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));writer.write(compact.toJson(Map.of("x",p.getX(),"y",p.getY(),"z",p.getZ(),"blockLight",light))+"\n");}}
        var result=new LinkedHashMap<String,Object>(Map.of("pieceFlag",replacement,"changedNativeExteriorCells",changed-roomMasonryRemaps-reliefCells.size(),"shapedRoofCells",shaped,"exactProtectedCells",protectedExact,"exactNativeFoundationCells",footingAudit.get("strictExactCells"),"nativeEmitters",actualEmitters.size(),"nativeWaterRouteFeet",originalNodes.size(),"nativeRouteBfs",before,"nativeRouteObstructionControl",true,"actual233Reference",!reference.isEmpty()));
        result.put("protectedNativeCellsVerified",protectedCells.size());result.put("protectedNativeMasonryRemaps",roomMasonryRemaps);result.put("backedCeilingCoffers",coffers);result.put("closedBackWallNiches",niches);result.put("protectedNativeReliefCells",reliefCells.size());
        result.put("nativeFoundationCellsVerified",foundations.size());result.put("nativeFoundationMasonryRemaps",footingAudit.get("materialRemaps"));result.put("nativeFootingAudit",footingAudit);
        result.put("changedNativeStructureCells",changed);
        result.put("nativeProtectedBlockLightHash",HexFormat.of().formatHex(lightDigest.digest()));result.put("nativeProtectedBlockLightFile",lightFile.getFileName().toString());
        String lightReference=System.getProperty("blockbranch.sanctuaryCoreLightReference","");
        if(!lightReference.isEmpty()){
            var actualLight=new HashMap<String,Integer>();for(var entry:lightLevels.entrySet()){var p=entry.getKey();actualLight.put(p.getX()+","+p.getY()+","+p.getZ(),entry.getValue());}
            var harvestedKeys=new HashSet<String>();for(var p:edits.keySet())harvestedKeys.add(p.getX()+","+p.getY()+","+p.getZ());
            var mayBrighten=new HashSet<String>();if(replacement==1)for(var p:protectedCells)if(isWater(original.get(p))||reliefCells.contains(p))mayBrighten.add(p.getX()+","+p.getY()+","+p.getZ());
            result.put("strictProtectedLight",SanctuaryLightAudit.compare(Path.of(lightReference),actualLight,harvestedKeys,mayBrighten));
        }
        return result;
    }
    private record FootingColumn(int bottom,boolean corner,boolean known){}
    private static boolean masonry(String state){return Blocks.PRISMARINE.defaultBlockState().toString().equals(state)||Blocks.PRISMARINE_BRICKS.defaultBlockState().toString().equals(state)||Blocks.DARK_PRISMARINE.defaultBlockState().toString().equals(state);}
    private static Map<String,Object> footings(ServerLevel level,ReimaginedSanctuaryPiece piece,Map<BlockPos,String> original,int replacement,Map<BlockPos,String> oldCore,Set<BlockPos> protectedCells){
        var columns=new HashMap<BlockPos,FootingColumn>();int piers=0;
        for(int gx=0;gx<7;gx++)for(int gz=0;gz<7;gz++){
            if(gx>0&&gx<6&&gz>0&&gz<6||gx==3&&gz==0)continue;piers++;
            for(int dx=0;dx<4;dx++)for(int dz=0;dz<4;dz++){
                int x=gx*9+dx,z=gz*9+dz,bottom=0;
                // Infer the old contact from immutable actual233 data, never from the new writer.
                for(int y=-1;y>=-34;y--){if(!masonry(original.get(new BlockPos(x,y,z))))break;bottom=y;}
                boolean known=bottom<0&&bottom>-34;columns.put(new BlockPos(x,0,z),new FootingColumn(bottom,(dx==0||dx==3)&&(dz==0||dz==3),known));
            }
        }
        if(piers!=23||columns.size()!=368)throw new AssertionError("The original 23 native perimeter pier footprints");
        var podium=podiumColumns(level,piece,original,oldCore,protectedCells);
        var podiumNodes=new HashSet<BlockPos>();int podiumStairs=0,podiumSlabs=0,podiumFull=0,naturalSolids=0;
        int exact=0,remaps=0,water=0,slabs=0,inner=0,contacts=0,shortOrUnknown=0;
        for(var entry:original.entrySet()){
            var p=entry.getKey();var at=piece.local(p.getX(),p.getY(),p.getZ());var now=level.getBlockState(at);String before=entry.getValue();var column=columns.get(new BlockPos(p.getX(),0,p.getZ()));
            if(!masonry(before)&&!isWater(before)&&!before.contains("minecraft:air"))naturalSolids++;
            if(replacement==1&&isWater(before)){
                var seat=podium.get(new BlockPos(p.getX(),0,p.getZ()));
                if(seat!=null&&p.getY()>seat.bed()&&p.getY()<=seat.top()){
                    int kind=podiumCell(piece,p,now,seat);if(kind==1)podiumStairs++;else if(kind==2)podiumSlabs++;else podiumFull++;
                    podiumNodes.add(p);continue;
                }
            }
            if(replacement==0||!masonry(before)||column==null){if(!before.equals(now.toString()))throw new AssertionError("Strict native legacy footing / natural terrain / unpermitted original fluid "+p);exact++;continue;}
            boolean longColumn=column.known()&&column.bottom()<=-7;
            if(longColumn&&column.corner()&&p.getY()==column.bottom()+2){
                if(!now.is(Blocks.DARK_PRISMARINE_SLAB)||now.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)!=net.minecraft.world.level.block.state.properties.SlabType.BOTTOM||!now.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED))throw new AssertionError("Native pier third-course bottom corner slab "+p);slabs++;continue;
            }
            if(longColumn&&column.corner()&&p.getY()==-2){
                if(!now.is(Blocks.DARK_PRISMARINE_SLAB)||now.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)!=net.minecraft.world.level.block.state.properties.SlabType.TOP||!now.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED))throw new AssertionError("Native pier top corner slab connects to original cap "+p);slabs++;continue;
            }
            if(longColumn&&column.corner()&&p.getY()>=column.bottom()+3&&p.getY()<=-3){
                if(!now.equals(Blocks.WATER.defaultBlockState()))throw new AssertionError("Only original native pier midshaft corners are chamfered "+p);water++;continue;
            }
            if(!sameMasonryGameplay(before,now,p))throw new AssertionError("Native pier contact, base, cap and inner twelve columns remain full solid "+p);
            if(before.equals(now.toString()))exact++;else remaps++;
            if(!column.corner())inner++;
        }
        for(var entry:columns.entrySet()){
            var xz=entry.getKey();var col=entry.getValue();if(!col.known()){shortOrUnknown++;continue;}
            var p=new BlockPos(xz.getX(),col.bottom(),xz.getZ());var at=piece.local(p.getX(),p.getY(),p.getZ());
            if(!sameMasonryGameplay(original.get(p),level.getBlockState(at),p))throw new AssertionError("Every original native pier lowest masonry contact stays full solid "+p);contacts++;
            var ground=p.below();var below=piece.local(ground.getX(),ground.getY(),ground.getZ());var state=level.getBlockState(below);
            if(!Objects.equals(original.get(ground),state.toString())||!state.getFluidState().isEmpty()||state.getCollisionShape(level,below).isEmpty())throw new AssertionError("Native pier remains grounded on the same untouched original seabed "+ground);
            if(!level.getBlockState(piece.local(xz.getX(),-1,xz.getZ())).isCollisionShapeFullBlock(level,piece.local(xz.getX(),-1,xz.getZ()))||!level.getBlockState(piece.local(xz.getX(),0,xz.getZ())).isCollisionShapeFullBlock(level,piece.local(xz.getX(),0,xz.getZ())))throw new AssertionError("Native pier full cap remains attached to the original body floor "+xz);
        }
        if(replacement==1&&podiumNodes.isEmpty())throw new AssertionError("Replacement has a bounded grounded podium in the actual native footprint");
        int pierConnected=replacement==1&&!podiumNodes.isEmpty()?podiumConnected(level,piece,original,podiumNodes):0;
        var result=new LinkedHashMap<String,Object>(Map.of("originalPierFootprints",piers,"originalColumns",columns.size(),"unchangedGroundContacts",contacts,"unknownContacts",shortOrUnknown,"strictExactCells",exact,"materialRemaps",remaps,"chamferWaterCells",water,"waterloggedCornerSlabs",slabs,"fullInnerShaftCells",inner,"sameGroundAndOriginalFootprint",true));
        result.put("naturalSolidCellsStrict",naturalSolids);result.put("podiumOldWaterCellsOccupied",podiumNodes.size());result.put("podiumStairCaps",podiumStairs);result.put("podiumSlabCaps",podiumSlabs);result.put("podiumFullBaseCells",podiumFull);result.put("podiumPierConnectedCells",pierConnected);result.put("podiumGroundedSeparateCells",podiumNodes.size()-pierConnected);result.put("podiumAllCellsOnOriginalFullUPGround",true);return result;
    }
    private static int roomRelief(ServerLevel level,ReimaginedSanctuaryPiece piece,BlockPos p,BlockState now,Map<BlockPos,String> old){
        var up=p.above();var down=p.below();var upper=level.getBlockState(piece.local(up.getX(),up.getY(),up.getZ()));
        if(!sameMasonryGameplay(old.get(up),upper,up))return 0;
        var empty=net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
        if(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(now.getCollisionShape(empty,p),net.minecraft.world.phys.shapes.Shapes.block(),net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST))return 0;
        if(now.is(Blocks.DARK_PRISMARINE_SLAB)&&now.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.TOP&&now.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED)&&isWater(old.get(down)))return 1;
        if(now.is(Blocks.PRISMARINE_BRICK_STAIRS)&&now.getValue(net.minecraft.world.level.block.StairBlock.HALF)==net.minecraft.world.level.block.state.properties.Half.TOP&&now.getValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED)){
            var lower=level.getBlockState(piece.local(down.getX(),down.getY(),down.getZ()));if(!sameMasonryGameplay(old.get(down),lower,down))return 0;
            var back=now.getValue(net.minecraft.world.level.block.StairBlock.FACING);
            if(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(now.getCollisionShape(empty,p).getFaceShape(back),net.minecraft.world.phys.shapes.Shapes.block(),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME))return 2;
        }
        return 0;
    }
    private record PodiumColumn(int bed,int top,int edge){}
    private static Map<BlockPos,PodiumColumn> podiumColumns(ServerLevel level,ReimaginedSanctuaryPiece piece,Map<BlockPos,String> old,Map<BlockPos,String> oldCore,Set<BlockPos> protectedCells){
        var result=new HashMap<BlockPos,PodiumColumn>();
        for(int x=0;x<58;x++)for(int z=0;z<58;z++){
            int edge=Math.min(Math.min(x,57-x),Math.min(z,57-z));if(edge>6)continue;
            var floor=new BlockPos(x,0,z);if(protectedCells.contains(floor)&&isWater(oldCore.get(floor)))continue;
            int bed=0;
            for(int y=-1;y>=-34;y--){var p=new BlockPos(x,y,z);String before=old.get(p);if(before==null||masonry(before)||isWater(before)||before.contains("minecraft:air"))continue;
                var at=piece.local(x,y,z);var state=level.getBlockState(at);if(!before.equals(state.toString()))throw new AssertionError("Original natural podium seabed never changes "+p);
                if(state.getFluidState().isEmpty()&&!state.getCollisionShape(level,at).isEmpty()){
                    var upper=state.getCollisionShape(level,at).getFaceShape(net.minecraft.core.Direction.UP);
                    if(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(upper,net.minecraft.world.phys.shapes.Shapes.block(),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME))bed=y;
                    break;
                }}
            if(bed==0)continue;int top=Math.min(-1,bed+Math.min(3,edge+1));boolean supported=true;
            for(int y=bed+1;y<=top;y++){String before=old.get(new BlockPos(x,y,z));if(!isWater(before)&&!masonry(before)){supported=false;break;}}
            if(supported)result.put(floor,new PodiumColumn(bed,top,edge));
        }
        return result;
    }
    private static int podiumCell(ReimaginedSanctuaryPiece piece,BlockPos p,BlockState now,PodiumColumn seat){
        if(p.getY()<seat.top()){
            if(!(now.is(Blocks.PRISMARINE)||now.is(Blocks.PRISMARINE_BRICKS))||!now.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p)||!now.getFluidState().isEmpty()||now.getLightEmission()!=0)throw new AssertionError("Grounded full podium courses remain inside old water above the unmodified seabed "+p);return 3;
        }
        if(seat.edge()>=2){if(!now.is(Blocks.PRISMARINE_BRICK_SLAB)||now.getValue(net.minecraft.world.level.block.SlabBlock.TYPE)!=net.minecraft.world.level.block.state.properties.SlabType.BOTTOM||!now.getValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED))throw new AssertionError("Bounded podium bottom slab cap "+p);return 2;}
        if(!now.is(Blocks.PRISMARINE_BRICK_STAIRS)||now.getValue(net.minecraft.world.level.block.StairBlock.HALF)!=net.minecraft.world.level.block.state.properties.Half.BOTTOM||!now.getValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED))throw new AssertionError("Battered podium stair toe "+p);
        var face=now.getValue(net.minecraft.world.level.block.StairBlock.FACING);boolean inward=false;
        for(var d:List.of(net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.NORTH)){
            boolean edge=d==net.minecraft.core.Direction.EAST?p.getX()==seat.edge():d==net.minecraft.core.Direction.WEST?57-p.getX()==seat.edge():d==net.minecraft.core.Direction.SOUTH?p.getZ()==seat.edge():57-p.getZ()==seat.edge();if(!edge)continue;
            var a=piece.local(p.getX(),p.getY(),p.getZ());var b=piece.local(p.getX()+d.getStepX(),p.getY(),p.getZ()+d.getStepZ());if(b.getX()-a.getX()==face.getStepX()&&b.getZ()-a.getZ()==face.getStepZ())inward=true;
        }
        if(!inward)throw new AssertionError("Podium stair rises inward from an original footprint edge "+p);return 1;
    }
    private static int podiumConnected(ServerLevel level,ReimaginedSanctuaryPiece piece,Map<BlockPos,String> old,Set<BlockPos> podium){
        var states=new HashMap<BlockPos,BlockState>();var seen=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();
        for(var entry:old.entrySet())if(masonry(entry.getValue())){var p=entry.getKey();var state=level.getBlockState(piece.local(p.getX(),p.getY(),p.getZ()));if(!state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p).isEmpty()){states.put(p,state);seen.add(p);queue.add(p);}}
        for(var p:podium)states.put(p,level.getBlockState(piece.local(p.getX(),p.getY(),p.getZ())));
        while(!queue.isEmpty()){var p=queue.removeFirst();for(var d:net.minecraft.core.Direction.values()){var q=p.relative(d);if(seen.contains(q)||!states.containsKey(q))continue;
            var a=states.get(p).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,p).getFaceShape(d);var b=states.get(q).getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,q).getFaceShape(d.getOpposite());
            if(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(a,b,net.minecraft.world.phys.shapes.BooleanOp.AND)){seen.add(q);queue.addLast(q);}}}
        // Terrain-following low paving can meet a raised natural ledge independently of a pier.
        // Full-UP bed support and bounded contiguous courses are mandatory; this is a diagnostic.
        int connected=0;for(var p:podium)if(seen.contains(p))connected++;return connected;
    }
    private static boolean sameMasonryGameplay(String expected,BlockState now,BlockPos p){
        BlockState before=null;for(var block:List.of(Blocks.PRISMARINE,Blocks.PRISMARINE_BRICKS,Blocks.DARK_PRISMARINE))if(block.defaultBlockState().toString().equals(expected))before=block.defaultBlockState();
        if(before==null||!(now.is(Blocks.PRISMARINE)||now.is(Blocks.PRISMARINE_BRICKS)||now.is(Blocks.DARK_PRISMARINE)))return false;
        var empty=net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
        return !net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(before.getCollisionShape(empty,p),now.getCollisionShape(empty,p),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME)
            &&!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(before.getOcclusionShape(),now.getOcclusionShape(),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME)
            &&before.useShapeForLightOcclusion()==now.useShapeForLightOcclusion()&&before.canOcclude()==now.canOcclude()
            &&before.isSolidRender()==now.isSolidRender()&&before.getLightEmission()==now.getLightEmission()
            &&before.getLightDampening()==now.getLightDampening()&&before.propagatesSkylightDown()==now.propagatesSkylightDown()
            &&before.getFluidState().equals(now.getFluidState());
    }
    private static Set<BlockPos> feet(Map<BlockPos,String> states,Set<BlockPos> region){var result=new HashSet<BlockPos>();for(var p:region)if(isWater(states.get(p))&&isWater(states.get(p.above())))result.add(p);return result;}
    private static boolean isWater(String state){return "Block{minecraft:water}[level=0]".equals(state);}
    private static Map<String,Object> components(Set<BlockPos> cells)throws Exception {
        var unseen=new HashSet<>(cells);var sizes=new ArrayList<Integer>();var signatures=new ArrayList<String>();
        while(!unseen.isEmpty()){var first=unseen.iterator().next();var component=new ArrayList<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(first);unseen.remove(first);
            while(!queue.isEmpty()){var p=queue.removeFirst();component.add(p);for(var direction:net.minecraft.core.Direction.values()){var next=p.relative(direction);if(unseen.remove(next))queue.addLast(next);}}
            component.sort(Comparator.comparingInt((BlockPos p)->p.getY()).thenComparingInt(BlockPos::getZ).thenComparingInt(BlockPos::getX));var digest=java.security.MessageDigest.getInstance("SHA-256");for(var p:component)digest.update((p.toShortString()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));sizes.add(component.size());signatures.add(HexFormat.of().formatHex(digest.digest()));
        }
        Collections.sort(sizes);Collections.sort(signatures);return Map.of("components",sizes.size(),"componentSizes",sizes,"componentHashes",signatures);
    }
    private static void rejectReplacement(StructurePieceSerializationContext context,net.minecraft.nbt.CompoundTag tag){
        try{ReimaginedSanctuaryPiece.TYPE.load(context,tag);throw new AssertionError("Malformed sanctuary replacement flag accepted");}catch(IllegalArgumentException expected){}
    }
    private static Map<String,Object> codec(ServerLevel level,Path dir)throws Exception {
        var method=PyramidGalleryQualification.class.getDeclaredMethod("codec",ServerLevel.class);method.setAccessible(true);var report=new LinkedHashMap<String,Object>();report.put("registryMatrix",method.invoke(null,level));
        if(RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),(RealisticGenerator)level.getChunkSource().getGenerator()).getOrThrow().getAsJsonObject().has("sanctuary_replacement_definition"))report.put("independentSanctuary",SanctuaryReplacementCodecQualification.verify(level));
        String jar=System.getProperty("blockbranch.sanctuaryCoreCurrentJar","");String reference=System.getProperty("blockbranch.sanctuaryCoreReference","");
        if(!jar.isEmpty()) {
            if(reference.isEmpty())throw new AssertionError("Archived233 reader needs authentic233 reference");var baseline=com.google.gson.JsonParser.parseString(Files.readString(Path.of(reference).resolve("generator.json")));var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());var current=RealisticGenerator.CODEC.codec().encodeStart(ops,(RealisticGenerator)level.getChunkSource().getGenerator()).getOrThrow();
            if(baseline.getAsJsonObject().get("shape_version").getAsInt()!=233)throw new AssertionError("Actual saved233 reference");
            if(!RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,baseline).getOrThrow()).getOrThrow().equals(baseline))throw new AssertionError("Current reader losslessly accepts actual233 metadata");
            try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},SanctuaryReplacementQualification.class.getClassLoader()){
                @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{boolean own=name.startsWith("blockbranch.realistic.RealisticGenerator")||name.startsWith("blockbranch.realistic.ReimaginedSites")||name.startsWith("blockbranch.realistic.ScreeBiomeSource")||name.startsWith("blockbranch.realistic.")&&name.endsWith("Schema");if(!own)return super.loadClass(name,resolve);synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}}
            }) {var old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();if(old.parse(ops,baseline).result().isEmpty())throw new AssertionError("Actual archived233 reader accepts own saved metadata");boolean future=current.getAsJsonObject().get("shape_version").getAsInt()!=233;if(future&&old.parse(ops,current).result().isPresent())throw new AssertionError("Actual archived233 reader rejects replacement metadata");report.put("actual233AcceptsOwn",true);report.put("current233Lossless",true);report.put("actual233RejectsReplacement",future);}
        }
        return report;
    }
}
