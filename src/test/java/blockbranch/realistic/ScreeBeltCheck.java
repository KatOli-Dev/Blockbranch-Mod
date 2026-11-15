package blockbranch.realistic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;

/** Pure-JVM guards for the cold scree-belt label (schema 280, label only). The native qualification covers stored quarts,
 * block identity and reopened worlds; this check covers the saved-schema identity and the frozen published eligibility. */
public final class ScreeBeltCheck {
    public static void main(String[] args)throws Exception{
        schema();
        published();
        System.out.println("SCREE_BELT_GATED_PASS");
    }
    /** Schema 280 exists only as the complete humid-crown 279 plus the explicit definition. */
    private static void schema(){
        for(int saved=0;saved<=300;saved++){
            require(ScreeBeltSchema.baseSchema(saved,0)==(saved==280?Integer.MAX_VALUE:saved),"Absent definition never reads 280 ("+saved+")");
            require(ScreeBeltSchema.matches(saved,1)==(saved==280),"Only 280 reads under the definition ("+saved+")");
            require(!ScreeBeltSchema.matches(saved,2)&&!ScreeBeltSchema.matches(saved,-1),"Unknown definitions never match ("+saved+")");
        }
        require(ScreeBeltSchema.schema(279,1)==280&&ScreeBeltSchema.baseSchema(280,1)==279,"Complete independent wrapper");
        require(ScreeBeltSchema.schema(278,0)==278&&ScreeBeltSchema.schema(279,0)==279,"Absent definition leaves the schema");
        for(int base:new int[]{270,272,277,278,280})requireThrows(()->ScreeBeltSchema.schema(base,1),"Wrapper rejects incomplete or future base "+base);
        requireThrows(()->ScreeBeltSchema.schema(279,2),"Unknown definition rejected");
        require(HumidCrownSchema.schema(278,1)==279,"The wrapped schema keeps its number");
        require(AlpineScree.MIN_ELEVATION==2200&&ScreeBelt.MIN_ELEVATION==1500,"Belt sits exactly below the existing scree habitat");
    }
    /** The production predicate reproduces the independently restated eligibility on every frozen published cell, and the
     * published selector read the fallback grassland label on the cells the gate may relabel. */
    private static void published()throws Exception{
        long seed=424242;var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);
        int belt=0,plains=0,held=0,controls=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(ScreeBeltCheck.class.getResourceAsStream("/fixtures/scree-belt-279/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");
                require(f.length==5&&Long.parseLong(f[0])==seed,"Row shape: "+line);
                int x=Integer.parseInt(f[1]),z=Integer.parseInt(f[2]);
                boolean expected=Boolean.parseBoolean(f[4]);
                require(ScreeBelt.contains(x,z,d,c)==expected,"Eligibility changed at "+x+","+z+" ("+f[3]+")");
                if(expected){
                    belt++;
                    if(f[3].equals("plains"))plains++;else held++;
                    require(!f[3].equals("alpine_scree_slope"),"Belt cells are below the published scree habitat: "+line);
                }else controls++;
            }
        }
        require(belt==48&&plains==24&&held==24&&controls==162,"Frozen denominators: "+belt+" belt ("+plains+" plains, "+held+" held by another owner), "+controls+" controls");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void requireThrows(Runnable action,String message){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError(message);}
    private ScreeBeltCheck(){}
}
