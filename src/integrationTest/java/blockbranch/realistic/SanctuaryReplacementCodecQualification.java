package blockbranch.realistic;

import java.nio.file.Path;
import java.util.*;
import net.minecraft.server.level.ServerLevel;

/** Published combinations remain unchanged beneath the independent sanctuary body barrier. */
public final class SanctuaryReplacementCodecQualification {
    public static Map<String,Object> verify(ServerLevel level)throws Exception {
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var codec=RealisticGenerator.CODEC.codec();
        var meta=codec.encodeStart(ops,(RealisticGenerator)level.getChunkSource().getGenerator()).getOrThrow().getAsJsonObject();
        meta.remove("mansion_replacement_definition");
        var samples=new ArrayList<com.google.gson.JsonObject>();
        for(int layout:new int[]{1,2,3,4,5,7,8})for(int coverage=0;coverage<=1;coverage++)for(int gallery=0;gallery<=1;gallery++)for(int mansion=0;mansion<=1;mansion++) {
            var sample=meta.deepCopy();sample.remove("sanctuary_replacement_definition");sample.remove("gravel_relief_definition");sample.remove("boreal_crown_definition");
            sample.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            if(coverage==0)sample.remove("native_overworld_structures");else sample.addProperty("native_overworld_structures",1);
            if(gallery==0)sample.remove("pyramid_gallery_definition");else sample.addProperty("pyramid_gallery_definition",1);
            if(mansion==0)sample.remove("mansion_wing_definition");else sample.addProperty("mansion_wing_definition",1);
            sample.addProperty("shape_version",(layout==8?220+2*mansion:layout==7?200+2*mansion:layout==5?160+2*mansion:layout==4?130+2*mansion:layout==3?118+2*mansion:102+2*(layout-1)+4*mansion)+coverage+8*gallery);samples.add(sample);
        }
        for(int layout:new int[]{4,5,7,8}) {
            var sample=meta.deepCopy();sample.remove("sanctuary_replacement_definition");sample.remove("gravel_relief_definition");sample.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            sample.addProperty("shape_version",layout==4?142:layout==5?172:layout==7?212:232);sample.addProperty("boreal_crown_definition",1);sample.addProperty("mansion_wing_definition",1);sample.addProperty("pyramid_gallery_definition",1);sample.addProperty("native_overworld_structures",1);samples.add(sample);
        }
        for(int layout:new int[]{7,8}) {
            var sample=meta.deepCopy();sample.remove("sanctuary_replacement_definition");sample.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);
            sample.addProperty("shape_version",layout==7?213:233);sample.addProperty("gravel_relief_definition",1);sample.addProperty("boreal_crown_definition",1);sample.addProperty("mansion_wing_definition",1);sample.addProperty("pyramid_gallery_definition",1);sample.addProperty("native_overworld_structures",1);samples.add(sample);
        }
        if(samples.size()!=62)throw new AssertionError("All 62 published combinations required");
        for(var sample:samples)if(!codec.encodeStart(ops,codec.parse(ops,sample).getOrThrow()).getOrThrow().equals(sample))throw new AssertionError("Published combination changed");
        var replacement=samples.getLast().deepCopy();replacement.addProperty("shape_version",234);replacement.addProperty("sanctuary_replacement_definition",1);
        if(!codec.encodeStart(ops,codec.parse(ops,replacement).getOrThrow()).getOrThrow().equals(replacement))throw new AssertionError("Independent sanctuary replacement roundtrip");
        for(int saved:new int[]{192,233,235,253,254}) {var bad=replacement.deepCopy();bad.addProperty("shape_version",saved);if(codec.parse(ops,bad).result().isPresent())throw new AssertionError("Unsupported sanctuary foundation accepted");}
        for(int definition:new int[]{-1,0,2}) {var bad=replacement.deepCopy();bad.addProperty("sanctuary_replacement_definition",definition);if(codec.parse(ops,bad).result().isPresent())throw new AssertionError("Sanctuary flag mismatch accepted");}
        for(int layout:new int[]{6,7,9}) {var bad=replacement.deepCopy();bad.getAsJsonObject("reimagined_layouts").addProperty("definition",layout);if(codec.parse(ops,bad).result().isPresent())throw new AssertionError("Sanctuary admitted unpublished or incomplete global layout");}
        int archived=0;String jar=System.getProperty("blockbranch.sanctuaryCoreCurrentJar","");
        if(jar.isEmpty())throw new AssertionError("Independent sanctuary fixture requires authentic233 reader archive");
        if(!jar.isEmpty())try(var reader=new java.net.URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},SanctuaryReplacementCodecQualification.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith("blockbranch.realistic."))return super.loadClass(name,resolve);
                synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;}
            }
        }) {
            com.mojang.serialization.Codec old=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();
            for(var sample:samples){var decoded=old.parse(ops,sample).getOrThrow();if(!old.encodeStart(ops,decoded).getOrThrow().equals(sample))throw new AssertionError("Archived233 published mapping changed");}
            if(old.parse(ops,replacement).result().isPresent())throw new AssertionError("Archived233 accepted independent sanctuary234");archived++;
        }
        return Map.of("publishedRoundTrips",62,"independentSanctuaryRoundTrips",1,"archived233Readers",archived,"archived233PublishedLossless",archived==1,"archived233Rejects234",archived==1,"unsupportedFoundationsRejected",true);
    }
}
