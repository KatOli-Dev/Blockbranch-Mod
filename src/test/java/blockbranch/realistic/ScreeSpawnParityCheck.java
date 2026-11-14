package blockbranch.realistic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/** Pure-JVM guard that scree keeps the hostile-spawn behaviour of the gravel holders (decided: scree matches gravel).
 * Gravel desert and the gravel-bed river margin declare empty natural-spawn lists, the overworld dimension supplies none,
 * and no Fabric biome modification adds a monster to any Blockbranch biome, so the effective MONSTER list is empty on
 * all three. This catches a monster list added to the scree JSON, drift from the gravel declaration, or a spawn
 * registration for the monster category in the wildlife code. It does not observe a running game. */
public final class ScreeSpawnParityCheck {
    private static final String ATTRIBUTE="minecraft:gameplay/natural_mob_spawns";
    private static final String[] GRAVEL={"continental_gravel_desert","gravel_bed_river_margin"};

    public static void main(String[] args)throws Exception{
        var scree=spawns("alpine_scree_slope");
        for(String gravel:GRAVEL)require(scree.equals(spawns(gravel)),"Scree natural spawns equal "+gravel);
        require(scree.contains("\"modifier\":\"overlay\""),"Scree overlays the inherited spawns");
        require(scree.contains("\"spawn_costs\":{}"),"Scree declares no spawn costs");
        require(scree.contains("\"monster\":[]"),"Scree declares an empty monster list");
        noMonsterRegistration();
        System.out.println("SCREE_SPAWN_PARITY_PASS");
    }
    /** The natural-spawn attribute object of a biome file with whitespace removed (no string value contains a space). */
    private static String spawns(String biome)throws IOException{
        String text;
        try(var in=Objects.requireNonNull(ScreeSpawnParityCheck.class.getResourceAsStream("/data/blockbranch/worldgen/biome/"+biome+".json"),biome)){
            text=new String(in.readAllBytes(),StandardCharsets.UTF_8).replaceAll("\\s+","");
        }
        int key=text.indexOf("\""+ATTRIBUTE+"\":{");
        require(key>=0&&text.indexOf("\""+ATTRIBUTE+"\"",key+1)<0,biome+" declares "+ATTRIBUTE+" once");
        int open=text.indexOf('{',key),depth=0,end=open;
        for(;end<text.length();end++){
            char c=text.charAt(end);
            if(c=='{')depth++;
            else if(c=='}'&&--depth==0)break;
        }
        require(depth==0,biome+" attribute object is balanced");
        return text.substring(open,end+1);
    }
    /** Biome modifications register by biome id, so any monster registration in the wildlife code could single out scree. */
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
