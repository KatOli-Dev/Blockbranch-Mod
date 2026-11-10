package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.minecraft.server.level.ServerLevel;

/** Registry-backed opt-in, historical defaults and authentic prior-reader rejection. */
public final class GravelReliefCodecSmoke {
    public static void verify(ServerLevel level)throws Exception {
        var ops=net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        final JsonObject fresh;
        try(var stream=GravelReliefCodecSmoke.class.getClassLoader().getResourceAsStream("data/blockbranch/worldgen/world_preset/realistic.json")) {
            if(stream==null)throw new AssertionError("Missing realistic preset");
            fresh=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject()
                .getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();
        }
        fresh.remove("type");
        require(fresh.get("shape_version").getAsInt()==213&&fresh.get("gravel_relief_definition").getAsInt()==1,"New preset explicitly selects relief schema213");
        var generator=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();
        require(generator.gravelReliefDefinition()==1&&RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().equals(fresh),"Flat schema213 round trip");
        var old212=fresh.deepCopy();old212.remove("gravel_relief_definition");old212.addProperty("shape_version",212);
        var old172=old212.deepCopy();old172.addProperty("shape_version",172);old172.getAsJsonObject("reimagined_layouts").addProperty("definition",5);
        for(var old:new JsonObject[]{old172,old212}) {
            var legacy=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(legacy.gravelReliefDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,legacy).getOrThrow().equals(old),"Missing relief definition retains exact historical flat settings");
            var enabled=old.deepCopy();enabled.addProperty("gravel_relief_definition",1);rejected(ops,enabled,"Historical schema cannot opt in without its barrier");
        }
        for(int definition:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("gravel_relief_definition",definition);rejected(ops,bad,"Wrong relief definition");}
        var absent=fresh.deepCopy();absent.remove("gravel_relief_definition");rejected(ops,absent,"Schema213 requires explicit relief opt-in");
        var text=fresh.deepCopy();text.addProperty("gravel_relief_definition","invalid");rejected(ops,text,"Malformed relief definition");
        for(int schema:new int[]{172,211,212,214}){var bad=fresh.deepCopy();bad.addProperty("shape_version",schema);rejected(ops,bad,"Relief requires exact complete foundation");}
        for(String key:new String[]{"boreal_crown_definition","pyramid_gallery_definition","native_overworld_structures","mansion_wing_definition"}){var bad=fresh.deepCopy();bad.remove(key);rejected(ops,bad,"Incomplete relief foundation: "+key);}
        var layout=fresh.deepCopy();layout.getAsJsonObject("reimagined_layouts").addProperty("definition",5);rejected(ops,layout,"Relief cannot silently wrap a different structure foundation");
        String archive=System.getProperty("blockbranch.gravelReliefPriorReader","");
        if(!archive.isEmpty())try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(archive).toUri().toURL()},GravelReliefCodecSmoke.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.equals("blockbranch.realistic.RealisticGenerator")&&!name.startsWith("blockbranch.realistic.RealisticGenerator$")&&!name.equals("blockbranch.realistic.ScreeBiomeSource")&&!name.startsWith("blockbranch.realistic.ScreeBiomeSource$"))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        }) {
            var prior=reader.loadClass("blockbranch.realistic.RealisticGenerator");require(prior!=RealisticGenerator.class,"Authentic212 generator is independently loaded");
            var codec=(MapCodec<?>)prior.getField("CODEC").get(null);
            require(codec.codec().parse(ops,old212).result().isPresent(),"Authentic212 reader accepts original212 control");
            require(codec.codec().parse(ops,old172).result().isPresent(),"Authentic212 reader accepts legacy172 control");
            require(codec.codec().parse(ops,fresh).result().isEmpty(),"Authentic212 reader rejects213 before world/structure opening");
            Blockbranch.LOGGER.info("GRAVEL_RELIEF_PRIOR_CODEC_PASS supported=172,212 rejected=213");
        }
        Blockbranch.LOGGER.info("GRAVEL_RELIEF_CODEC_PASS legacy=172,212 fresh=213 malformed=reject");
    }
    private static void rejected(net.minecraft.resources.RegistryOps<JsonElement> ops,JsonObject value,String message){require(RealisticGenerator.CODEC.codec().parse(ops,value).result().isEmpty(),message);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private GravelReliefCodecSmoke() {}
}
