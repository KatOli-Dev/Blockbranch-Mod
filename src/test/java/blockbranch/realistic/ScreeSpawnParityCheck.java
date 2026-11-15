package blockbranch.realistic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Pure-JVM guard that scree keeps the declared hostile-spawn behaviour of the gravel holders (decided: scree matches gravel).
 *
 * <p>Scope. It compares only what a biome file declares for the MONSTER category: the attribute's modifier, each monster
 * entry (type, weight, group sizes and any other field) as an unordered set, and the spawn costs of the entity types that
 * appear in that list. Passive categories (creature, ambient, water, misc) and costs of non-monster types are deliberately
 * not compared, so scree and gravel may differ there. It does not observe a running game. The effective hostile list is
 * the declared list merged into an inherited base by the {@code overlay} modifier; that the overworld dimension supplies
 * no base list was read from the game jar and is not asserted here. Fabric registrations are covered only by a textual
 * scan for {@code MobCategory.MONSTER} in the wildlife code and mixins, which does not see other spellings of the category,
 * other packages or other mods. Gravel is {@code continental_gravel_desert} and the gravel-bed river margin; vanilla
 * {@code windswept_gravelly_hills} is a different family and is not a target. The in-memory self-test pins the detection
 * itself: hostile drift must fail and passive-only differences must pass. */
public final class ScreeSpawnParityCheck {
    private static final String ATTRIBUTE="minecraft:gameplay/natural_mob_spawns";
    private static final String[] GRAVEL={"continental_gravel_desert","gravel_bed_river_margin"};
    private static final Pattern FIELD=Pattern.compile("\"([A-Za-z_]+)\":(\"[^\"]*\"|-?[0-9.]+|true|false)");
    private static final Pattern TYPE=Pattern.compile("\"type\":\"([^\"]*)\"");
    private static final Pattern MODIFIER=Pattern.compile("\"modifier\":\"([^\"]*)\"");

    public static void main(String[] args)throws Exception{
        selfTest();
        var scree=read("alpine_scree_slope");
        for(String gravel:GRAVEL)requireParity(scree,read(gravel),"scree vs "+gravel);
        noMonsterRegistration();
        System.out.println("SCREE_SPAWN_PARITY_PASS");
    }

    /** The hostile-relevant declaration of one biome, independent of whitespace, key order and entry order. */
    record Hostile(String modifier,List<String> monsters,Map<String,String> costs){}

    static Hostile hostile(String json){
        String text=json.replaceAll("\\s+","");
        int key=text.indexOf("\""+ATTRIBUTE+"\":{");
        require(key>=0&&text.indexOf("\""+ATTRIBUTE+"\"",key+1)<0,"Natural spawns are declared exactly once");
        String attribute=balanced(text,key+ATTRIBUTE.length()+3,'{','}',ATTRIBUTE);
        var modifier=MODIFIER.matcher(attribute);
        require(modifier.find(),"Attribute declares a modifier");
        int monster=attribute.indexOf("\"monster\":[");
        require(monster>=0,"Attribute declares a monster category");
        String category=balanced(attribute,monster+"\"monster\":".length(),'[',']',"monster");
        List<String> monsters=new ArrayList<>();
        Set<String> types=new TreeSet<>();
        for(int i=1;i<category.length()-1;){
            if(category.charAt(i)==','){i++;continue;}
            String entry=balanced(category,i,'{','}',"monster entry");
            i+=entry.length();
            var fields=new TreeMap<String,String>();
            var field=FIELD.matcher(entry);
            while(field.find())fields.put(field.group(1),field.group(2));
            monsters.add(fields.toString());
            var type=TYPE.matcher(entry);
            require(type.find(),"Monster entry names a type: "+entry);
            types.add(type.group(1));
        }
        Collections.sort(monsters);
        var costs=new TreeMap<String,String>();
        int costKey=attribute.indexOf("\"spawn_costs\":{");
        if(costKey>=0){
            String block=balanced(attribute,costKey+"\"spawn_costs\":".length(),'{','}',"spawn_costs");
            for(String type:types){
                int at=block.indexOf("\""+type+"\":{");
                if(at>=0)costs.put(type,balanced(block,at+type.length()+3,'{','}',"cost of "+type));
            }
        }
        return new Hostile(modifier.group(1),monsters,costs);
    }

    static void requireParity(String scree,String gravel,String label){
        var a=hostile(scree);var b=hostile(gravel);
        require(a.modifier().equals(b.modifier()),label+": same attribute modifier");
        require(a.monsters().equals(b.monsters()),label+": same monster entries "+a.monsters()+" vs "+b.monsters());
        require(a.costs().equals(b.costs()),label+": same costs for the monster types "+a.costs()+" vs "+b.costs());
    }

    /** The balanced bracket group starting at index {@code open}. */
    private static String balanced(String text,int open,char start,char end,String what){
        require(open>=0&&open<text.length()&&text.charAt(open)==start,what+" is present");
        int depth=0;
        for(int i=open;i<text.length();i++){
            char c=text.charAt(i);
            if(c==start)depth++;
            else if(c==end&&--depth==0)return text.substring(open,i+1);
        }
        throw new IllegalStateException(what+" is balanced");
    }

    /** In-memory detection tests on copies of the real gravel declaration; no file is written or changed. */
    private static void selfTest()throws IOException{
        String gravel=read(GRAVEL[0]).replaceAll("\\s+","");
        String zombie="{\"type\":\"minecraft:zombie\",\"weight\":95,\"minCount\":4,\"maxCount\":4}";
        String sheep="{\"type\":\"minecraft:sheep\",\"weight\":12,\"minCount\":4,\"maxCount\":4}";
        requireParity(gravel,gravel,"self-test identity");
        requireParity(gravel,read(GRAVEL[1]),"self-test the two gravel holders");
        // Hostile drift fails.
        String withZombie=replace(gravel,"\"monster\":[]","\"monster\":["+zombie+"]");
        requireFails(()->requireParity(withZombie,gravel,"zombie"),"a zombie only in the scree monster list");
        requireFails(()->requireParity(gravel,withZombie,"zombie"),"a zombie only in the gravel monster list");
        requireFails(()->requireParity(replace(withZombie,"\"weight\":95","\"weight\":96"),withZombie,"weight"),"a changed monster weight");
        requireFails(()->requireParity(replace(withZombie,"\"maxCount\":4","\"maxCount\":5"),withZombie,"group"),"a changed group size");
        requireFails(()->requireParity(replace(withZombie,"minecraft:zombie","minecraft:husk"),withZombie,"type"),"a changed monster type");
        requireFails(()->requireParity(replace(gravel,"\"modifier\":\"overlay\"","\"modifier\":\"replace\""),gravel,"modifier"),"a changed modifier");
        String costed=replace(withZombie,"\"spawn_costs\":{}","\"spawn_costs\":{\"minecraft:zombie\":{\"energy_budget\":0.1,\"charge\":0.1}}");
        requireFails(()->requireParity(costed,withZombie,"cost"),"a cost on a monster type");
        // Passive-only differences pass.
        String passive=replace(gravel,"\"creature\":[]","\"creature\":["+sheep+"]");
        requireParity(passive,gravel,"self-test passive list");
        requireParity(gravel,passive,"self-test passive list reversed");
        String passiveCost=replace(passive,"\"spawn_costs\":{}","\"spawn_costs\":{\"minecraft:sheep\":{\"energy_budget\":0.1,\"charge\":0.1}}");
        requireParity(passiveCost,gravel,"self-test passive spawn cost");
        requireParity(replace(passiveCost,"\"ambient\":[]","\"ambient\":["+sheep+"]"),gravel,"self-test other categories");
        requireParity(replace(withZombie,"\"creature\":[]","\"creature\":["+sheep+"]"),withZombie,"self-test passive difference beside equal hostiles");
        // Entry order and whitespace are not semantics.
        String skeleton=sheep.replace("sheep","skeleton");
        String ordered=replace(gravel,"\"monster\":[]","\"monster\":["+zombie+","+skeleton+"]");
        String reversed=replace(gravel,"\"monster\":[]","\"monster\":["+skeleton+","+zombie+"]");
        requireParity(ordered,reversed.replace(",","\n,"),"self-test entry order and whitespace");
    }
    private static String read(String biome)throws IOException{
        try(var in=Objects.requireNonNull(ScreeSpawnParityCheck.class.getResourceAsStream("/data/blockbranch/worldgen/biome/"+biome+".json"),biome)){
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String replace(String text,String from,String to){
        require(text.contains(from),"Self-test input contains "+from);
        return text.replace(from,to);
    }
    private static void requireFails(Runnable check,String what){
        try{check.run();}catch(IllegalStateException expected){return;}
        throw new IllegalStateException("Parity must reject "+what);
    }

    /** Biome modifications register by biome id, so a monster registration in the wildlife code could single out scree. */
    private static void noMonsterRegistration()throws IOException{
        for(String root:new String[]{"src/main/java/blockbranch/wildlife","src/main/java/blockbranch/mixin/wildlife"}){
            try(Stream<Path> files=Files.walk(Path.of(root))){
                for(Path file:(Iterable<Path>)files.filter(f->f.toString().endsWith(".java"))::iterator)
                    require(!Files.readString(file).contains("MobCategory.MONSTER"),file+" registers no monster category spawn");
            }
        }
    }
    private static void require(boolean condition,String message){
        if(!condition)throw new IllegalStateException(message);
    }
}
