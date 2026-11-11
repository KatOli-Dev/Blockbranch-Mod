package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.RandomState;

/** Actual published reader, native physical columns, and fresh/reopen saved-world checks. */
public final class SurfaceOwnershipQualification {
    private static final int[][] POINTS=PublishedSurfaceSnapshot.POINTS;
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            Path result=Path.of(System.getProperty("blockbranch.surfaceOwnershipResult"));
            try {
                var level=server.overworld();var random=level.getChunkSource().randomState();
                var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
                JsonObject fresh;
                try(var stream=Objects.requireNonNull(SurfaceOwnershipQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json"));
                    var input=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
                    fresh=JsonParser.parseReader(input).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();
                }
                fresh.remove("type");
                // Keep this historical 257->258 qualification and its frozen
                // counts intact when today's preset has an independent wrapper.
                if(fresh.get("shape_version").getAsInt()==264){
                require(fresh.get("summer_dry_definition").getAsInt()==1,"Known complete summer-dry wrapper");
                fresh.remove("summer_dry_definition");fresh.addProperty("shape_version",263);fresh.getAsJsonObject("additional_biomes").remove(SummerDryWoodland.KEY);
            }
            if(fresh.get("shape_version").getAsInt()==263){
                require(fresh.get("land_ice_definition").getAsInt()==1,"Known complete land-ice wrapper");
                fresh.remove("land_ice_definition");fresh.addProperty("shape_version",262);fresh.getAsJsonObject("additional_biomes").remove(LandIceField.KEY);
            }
            if(fresh.get("shape_version").getAsInt()==262){
                    require(fresh.get("deep_basin_relief_definition").getAsInt()==1,"Known complete deep-basin wrapper");
                    fresh.remove("deep_basin_relief_definition");fresh.addProperty("shape_version",261);
                }
                if(fresh.get("shape_version").getAsInt()==261){
                    require(fresh.get("abyssal_trace_definition").getAsInt()==1,"Known complete abyssal wrapper");
                    fresh.remove("abyssal_trace_definition");fresh.addProperty("shape_version",260);
                }
                if(fresh.get("shape_version").getAsInt()==260){
                    require(fresh.get("cool_desert_definition").getAsInt()==1,"Known complete cool-desert wrapper");
                    fresh.remove("cool_desert_definition");fresh.addProperty("shape_version",259);fresh.getAsJsonObject("additional_biomes").remove(CoolDesert.KEY);
                }
                if(fresh.get("shape_version").getAsInt()==259){
                    require(fresh.get("dry_forest_definition").getAsInt()==1,"Known complete dry-forest wrapper");
                    fresh.remove("dry_forest_definition");fresh.addProperty("shape_version",258);fresh.getAsJsonObject("additional_biomes").remove(DryForest.KEY);
                }
                require(fresh.get("shape_version").getAsInt()==258,"Historical surface-ownership foundation");
                var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();
                require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Exact flat new codec roundtrip");
                Biome.DIRECT_CODEC.encodeStart(ops,HabitatPalette.CODEC.codec().parse(ops,fresh).getOrThrow().additional(ColdRockland.KEY).value()).getOrThrow();
                var legacy=fresh.deepCopy();legacy.remove("surface_ownership_definition");legacy.addProperty("shape_version",257);
                legacy.getAsJsonObject("additional_biomes").remove(ColdRockland.KEY);
                var retained=RealisticGenerator.CODEC.codec().parse(ops,legacy).getOrThrow();
                require(retained.surfaceOwnershipDefinition()==0,"Missing gate does not promote old saves");
                require(RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(legacy),"Exact legacy flat settings retained");
                for(String key:new String[]{"surface_ownership_definition","redwood_forest_definition","biome_transition_definition"}) {
                    var bad=fresh.deepCopy();bad.remove(key);rejected(ops,bad,"Missing complete foundation: "+key);
                }
                for(int value:new int[]{-1,0,2}) {var bad=fresh.deepCopy();bad.addProperty("surface_ownership_definition",value);rejected(ops,bad,"Invalid gate");}
                for(int shape:new int[]{82,256,257,259}) {var bad=fresh.deepCopy();bad.addProperty("shape_version",shape);rejected(ops,bad,"Wrong/future foundation");}
                var malformed=fresh.deepCopy();malformed.addProperty("surface_ownership_definition","invalid");rejected(ops,malformed,"Malformed gate");
                var missing=fresh.deepCopy();missing.getAsJsonObject("additional_biomes").remove(ColdRockland.KEY);rejected(ops,missing,"Missing new holder");
                var report=new LinkedHashMap<String,Object>();
                { var reader=priorReader();
                    var priorType=reader.get("blockbranch.realistic.RealisticGenerator");
                    @SuppressWarnings("unchecked") var codec=(MapCodec<Object>)priorType.getField("CODEC").get(null);
                    require(codec.codec().parse(ops,fresh).error().isPresent(),"Actual published reader rejects schema258");
                    Object prior=codec.codec().parse(ops,legacy).getOrThrow();
                    var method=priorType.getDeclaredMethod("surfaceState",int.class,int.class,int.class,RandomState.class);method.setAccessible(true);
                    var getDrainage=priorType.getDeclaredMethod("drainage",RandomState.class);getDrainage.setAccessible(true);
                    var priorDrainage=(DrainageNetwork)getDrainage.invoke(prior,random);
                    int physical=0;
                    for(var p:POINTS)for(int dz=-8;dz<=8;dz+=4)for(int dx=-8;dx<=8;dx+=4){
                        int x=p[0]+dx,z=p[1]+dz,h=priorDrainage.sample(x,z).height();
                        for(int y=h-4;y<=Math.max(h+8,priorDrainage.sample(x,z).waterLevel()+1);y++) {
                            require(retained.surfaceState(x,y,z,random).equals(method.invoke(prior,x,y,z,random)),"Gate-disabled physical state equals original published reader at "+x+","+y+","+z);physical++;
                        }
                    }
                    report.put("legacyPhysicalComparisons",physical);
                    report.put("selectorComparison",compareSelectors(reader,HabitatPalette.CODEC.codec().parse(ops,legacy).getOrThrow(),HabitatPalette.CODEC.codec().parse(ops,fresh).getOrThrow()));
                }
                boolean legacyWorld=Boolean.getBoolean("blockbranch.surfaceOwnershipLegacy");
                JsonObject independent;
                try(var stream=Objects.requireNonNull(SurfaceOwnershipQualification.class.getResourceAsStream("/blockbranch-fixtures/surface-ownership-reader-257/baseline.json"));var input=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                    independent=JsonParser.parseReader(input).getAsJsonObject();
                }
                if(!legacyWorld){independent.remove("nativeStates");independent.remove("nativeSHA256");}
                var actualSnapshot=PublishedSurfaceSnapshot.capture(level,retained,legacyWorld);
                require(independent.equals(actualSnapshot),"Gate-disabled output matches separately recorded original production, including changed helpers: expected="+independent+" actual="+actualSnapshot);
                report.put("independentPublishedProductionOutput",actualSnapshot);
                report.put("independentProductionCommit","a41e39a00d4347a5a96773cfbb60fc3a7c038c70");
                var actual=(RealisticGenerator)level.getChunkSource().getGenerator();
                require(actual.surfaceOwnershipDefinition()==(legacyWorld?0:1),"Actual saved definition retains opt-in");
                report.put("actualDryForestDefinition",actual.dryForestDefinition());
                if(!legacyWorld){
                    int coastSand=0;
                    for(int z=-1776064;z<=-1775936;z++)for(int x=-1008064;x<=-1007936;x++){
                        int shore=actual.sandyShoreCover(x,z,random),sediment=actual.riverMarginCover(x,z,random);
                        require(shore==0||sediment==0,"Physical shore and aquatic sediment never overlap, including quart kelp edges");
                        if(shore>0)require(TemperateOcean.kelpHeight(random.seed(),x,z,actual.drainage(random),new ClimateField(random.seed()))==0,"Kelp never forces a hard bed onto physical shore sand");
                        if(shore>0)coastSand++;
                    }
                    require(coastSand>0,"Coastal regression exercises actual sand masks");
                    for(int z=(-1776000-64)>>4;z<=(-1776000+64)>>4;z++)for(int x=(-1008000-64)>>4;x<=(-1008000+64)>>4;x++)level.getChunk(x,z);
                    report.put("coastalSandRegressionBlocks",coastSand);
                }
                var scenes=new ArrayList<Object>();int nativeStates=0,gravel=0,snow=0;
                for(var p:POINTS){
                    var d=actual.drainage(random);var center=d.sample(p[0],p[1]);
                    level.getChunk(p[0]>>4,p[1]>>4);
                    var selected=((ScreeBiomeSource)actual.getBiomeSource()).surfaceResolver().getNoiseBiome(p[0]>>2,0,p[1]>>2);
                    scenes.add(Map.of("x",p[0],"z",p[1],"ground",center.height(),"biome",selected.unwrapKey().orElseThrow().identifier().toString()));
                    for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                        int x=(p[0]&~15)+dx,z=(p[1]&~15)+dz,h=d.sample(x,z).height();
                        for(int y=h-3;y<=Math.max(h+2,d.sample(x,z).waterLevel());y++) {
                            var nativeState=level.getBlockState(new BlockPos(x,y,z));
                            boolean savedEdit=(legacyWorld||Boolean.getBoolean("blockbranch.surfaceOwnershipReopen"))&&x==POINTS[0][0]+1&&z==POINTS[0][1]+1&&y==h-2;
                            require(savedEdit?nativeState.is(Blocks.GOLD_BLOCK):nativeState.equals(actual.surfaceState(x,y,z,random)),"Native stored/procedural surface agrees with analytic cover at "+x+","+y+","+z);
                            if(nativeState.is(Blocks.GRAVEL))gravel++;
                            if(nativeState.is(Blocks.SNOW))snow++;nativeStates++;
                        }
                    }
                }
                if(!legacyWorld)require(snow>0&&gravel>0,"Native snow and debris actually generated");
                var edit=new BlockPos(POINTS[0][0]+1,actual.drainage(random).sample(POINTS[0][0]+1,POINTS[0][1]+1).height()-2,POINTS[0][1]+1);
                if(legacyWorld||Boolean.getBoolean("blockbranch.surfaceOwnershipReopen"))require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Saved edit survives full relaunch");
                else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
                report.put("actualLegacyWorld",legacyWorld);report.put("nativeStates",nativeStates);report.put("gravelStates",gravel);report.put("snowStates",snow);report.put("scenes",scenes);
                Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                server.saveEverything(false,true,true);
                Files.writeString(result,"PASS independent published-output legacy hashes; shared-helper reader checks; gates; native states; saved definition/edit\n");
                Blockbranch.LOGGER.info("SURFACE_OWNERSHIP_NATIVE_PASS legacy={} nativeStates={} gravel={} snow={}",legacyWorld,nativeStates,gravel,snow);
            } catch(Throwable failure) {
                Blockbranch.LOGGER.error("SURFACE_OWNERSHIP_NATIVE_FAIL",failure);
                try{Files.writeString(result,"FAIL "+failure+"\n");}catch(IOException ignored){}
            } finally {server.halt(false);}
        });
    }
    private static Map<String,Integer> compareSelectors(Map<String,Class<?>> reader,HabitatPalette oldPalette,HabitatPalette palette)throws Exception {
        var type=reader.get("blockbranch.realistic.ScreeBiomeSource");
        var bind=type.getDeclaredMethod("bind",long.class,DrainageNetwork.class,ClimateField.class);bind.setAccessible(true);
        var redwood=type.getDeclaredMethod("withRedwoodForests",boolean.class);redwood.setAccessible(true);
        int samples=0,unowned=0,rockland=0,remaining=0,oldLandFallback=0,oldOceanFallback=0;
        for(long seed:new long[]{424242,1,-17}) {
            var shape=new TerrainShape(seed);var d=new DrainageNetwork(shape,seed,true,true,true);var now=new DrainageNetwork(shape,seed,true,true,true,true);var c=new ClimateField(seed);
            var prior=(BiomeSource)type.getConstructor(HabitatPalette.class,boolean.class,boolean.class).newInstance(oldPalette,true,true);redwood.invoke(prior,true);bind.invoke(prior,seed,d,c);
            var published=(BiomeResolver)type.getMethod("surfaceResolver").invoke(prior);
            var retained=new ScreeBiomeSource(oldPalette,true,true).withRedwoodForests(true);retained.bind(seed,d,c);
            var sentinel=Holder.direct(oldPalette.margins().fallback().value());var m=oldPalette.margins();
            var tagged=new HabitatPalette(new HabitatPalette.Margins(sentinel,m.scree(),m.reedbed(),m.riverMargin(),m.sandyShore(),m.lake(),m.riverChannel(),m.shelf(),m.deepOcean()),oldPalette.land(),oldPalette.additional());
            var baseline=new ScreeBiomeSource(tagged,true,true).withRedwoodForests(true);baseline.bind(seed,d,c);
            var candidate=new ScreeBiomeSource(palette,true,true).withRedwoodForests(true).withSurfaceOwnership(true);candidate.bind(seed,now,c);
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var old=published.getNoiseBiome(x>>2,0,z>>2);
                require(old==retained.surfaceResolver().getNoiseBiome(x>>2,0,z>>2),"Gate-disabled selector equals frozen reader");
                var mark=baseline.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);var selected=candidate.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);var s=d.sample(x,z);
                boolean wet=s.waterLevel()!=DrainageNetwork.DRY&&s.height()+1<s.waterLevel();
                if(mark==sentinel && !wet)oldLandFallback++;
                if(mark==sentinel && wet && d.oceanWater(x,z,s))oldOceanFallback++;
                if(mark==sentinel && wet && !d.oceanWater(x,z,s)&&!d.terminalLakeWater(x,z,s)&&d.channelAt(x,z).edge()<0){
                    require(now.habitatOceanWater(x,z,s),"Every old unowned-water witness has actual ocean-basin ownership");
                    require(selected!=palette.margins().fallback(),"Admitted marine column has actual habitat");unowned++;
                }
                if(selected==palette.additional(ColdRockland.KEY)){
                    require(mark==sentinel&&!wet&&c.sample(x,z,now::sample).meanTemperatureC()<-6,"Rockland replaces only cold fallback");rockland++;
                }else if(!wet&&old!=selected){
                    require(false,"Established dry-land owner changed outside cold fallback: "+seed+" "+x+","+z);
                }
                if(selected==palette.margins().fallback()&&old!=oldPalette.margins().fallback())remaining++;
                samples++;
            }
        }
        require(oldLandFallback==867&&oldOceanFallback==0&&unowned==99,"Frozen audit reproduced, no baseline count rewrite");
        require(rockland==308&&remaining==0,"Bounded cold-only gains; no newly stranded prior habitat");
        return Map.of("legacySelectorComparisons",samples,"oldLandFallback",oldLandFallback,"oldUnownedWater",unowned,"newColdRockland",rockland,"newlyStrandedHabitats",remaining);
    }
    private static Map<String,Class<?>> priorReader()throws ClassNotFoundException {
        return Map.of("blockbranch.realistic.RealisticGenerator",Class.forName("blockbranch.realistic.Published257RealisticGenerator"),
            "blockbranch.realistic.ScreeBiomeSource",Class.forName("blockbranch.realistic.Published257ScreeBiomeSource"));
    }
    private static void rejected(RegistryOps<JsonElement> ops,JsonObject json,String message){require(RealisticGenerator.CODEC.codec().parse(ops,json).error().isPresent(),message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SurfaceOwnershipQualification() {}
}
