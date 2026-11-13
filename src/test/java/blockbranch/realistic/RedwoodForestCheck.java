package blockbranch.realistic;

import java.util.*;

/**
 * Redwood forest invariants: bounded and supported tree geometry, a province disjoint from the
 * Forest arrays, chunk-order-independent canopy columns, flag isolation and the save-schema chain.
 * Run with {@code ./gradlew checkRedwoodForest}; it needs no world, renderer or Python.
 */
public final class RedwoodForestCheck {
    private static int sequoiaRoots;
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void main(String[] args) throws Exception {
        schema();
        geometry();
        resources();
        for(long seed:new long[]{424242,1,-17})world(seed);
        require(sequoiaRoots>0,"Giant sequoia grows at montane elevations ("+sequoiaRoots+")");
        System.out.println("REDWOOD_FOREST_SCHEMA_GEOMETRY_DOMAIN_ORDER_PASS");
    }
    private static void schema(){
        require(RedwoodForestSchema.FOUNDATION==256&&RedwoodForestSchema.MAX==257,"Redwood forests extend the biome-transition foundation by one schema");
        require(RedwoodForestSchema.schema(256,0)==256&&RedwoodForestSchema.schema(256,1)==257,"Schema follows the definition");
        require(RedwoodForestSchema.baseSchema(257,1)==256&&RedwoodForestSchema.baseSchema(256,0)==256,"Base schema unwinds");
        require(!RedwoodForestSchema.matches(257,0)&&!RedwoodForestSchema.matches(256,1)&&!RedwoodForestSchema.matches(255,1),"Mismatched schema/definition pairs are rejected");
        try{RedwoodForestSchema.schema(255,1);throw new AssertionError("Redwoods need the complete foundation");}catch(IllegalArgumentException expected){}
    }
    private static void geometry(){
        var shapes=new HashSet<String>();int giants=0,thick=0;
        for(int i=0;i<96;i++){
            int h=i%3==0?10+i%21:i%3==1?45+i%25:72+i%(RedwoodTrees.MAX_STEM-71);boolean sequoia=i%4==3;if(sequoia&&h>95)h=95;
            var root=new WoodlandTrees.Root(0,0,0,sequoia,h,(int)OakBirchWoodland.mix(i*1297L),RedwoodForest.FAMILY);
            var tree=RedwoodTrees.tree(root);shapes.add(tree.toString());var map=new HashMap<String,Integer>();
            int top=Integer.MIN_VALUE,leaves=0;
            for(var v:tree){
                require(Math.abs(v.x())<=RedwoodTrees.REACH&&Math.abs(v.z())<=RedwoodTrees.REACH,"Tree stays within the canopy halo");
                require(v.y()>=-RedwoodTrees.ROOT_DEPTH&&v.y()<=RedwoodTrees.MAX_HEIGHT&&v.y()<=h+1,"Tree stays within its vertical envelope");
                require(map.put(key(v.x(),v.y(),v.z()),v.code())==null,"No duplicate voxels");
                if(RedwoodTrees.woodCode(v.code()))top=Math.max(top,v.y());else{require(v.code()>=22&&v.code()<=27,"Foliage is spruce leaves at distance 1..6");leaves++;}
            }
            require(top==h-1&&leaves>20,"Leader reaches the requested height and carries foliage");
            for(int y=0;y<h;y++)require(map.containsKey(key(0,y,0))&&RedwoodTrees.woodCode(map.get(key(0,y,0))),"Continuous bole");
            if(h>=30){thick++;require(map.containsKey(key(1,5,1))&&map.containsKey(key(1,5,0))&&map.containsKey(key(0,5,1)),"Giants have a two-by-two bole");}
            if(h>=70)giants++;
            for(var v:tree)if(v.code()>=22&&v.code()<=27){
                int distance=WoodlandTrees.leafDistance(v.code());boolean support=false;
                for(var q:DIR){int n=map.getOrDefault(key(v.x()+q[0],v.y()+q[1],v.z()+q[2]),0);if(WoodlandTrees.logCode(n)&&distance==1||n>=22&&n<=27&&WoodlandTrees.leafDistance(n)<distance)support=true;}
                require(support,"Every leaf has a decreasing-distance path to wood");
            }
        }
        require(shapes.size()>85&&giants>20&&thick>50,"Varied silhouettes across all height classes");
        require(WoodlandTrees.logCode(170)&&WoodlandTrees.logCode(171)&&WoodlandTrees.logCode(172)&&!WoodlandTrees.logCode(173),"Bark codes are logs for support");
        require(blockbranch.cubic.CanopyLayer.REDWOOD_WOOD_CODE+2==172&&blockbranch.cubic.CanopyLayer.LAST_CODE>=172,"Historical bark-state codes retain their bounds");
        System.out.println("REDWOOD_SHAPES unique="+shapes.size()+" giants="+giants);
    }
    private static void resources() throws Exception {
        var preset=new String(RedwoodForestCheck.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json").readAllBytes());
        // This checks today's wrapping preset; the historical 256/257 schema and
        // frozen selector assertions above remain unchanged when a new gate wraps it.
        require(preset.contains("\"shape_version\": 272")&&preset.contains("\"redwood_forest_definition\": 1")&&preset.contains("\"redwood_forest\": \"blockbranch:maritime_redwood_fern_forest\""),"Bundled preset retains redwoods under the current wrapper");
        var biome=new String(RedwoodForestCheck.class.getResourceAsStream("/data/blockbranch/worldgen/biome/maritime_redwood_fern_forest.json").readAllBytes());
        require(biome.contains("natural_mob_spawns")&&biome.contains("\"has_precipitation\": true"),"Biome resource is a precipitating woodland with empty native spawns");
        var lang=new String(RedwoodForestCheck.class.getResourceAsStream("/assets/blockbranch/lang/en_us.json").readAllBytes());
        require(lang.contains("biome.blockbranch.maritime_redwood_fern_forest"),"Biome has a display name");
    }
    private static void world(long seed){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);
        int samples=0,wood=0,redwood=0,forestOverlap=0,swampOverlap=0,notWoodland=0;var candidates=new ArrayList<int[]>();
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            samples++;
            if(OakBirchWoodland.contains(seed,x,z,d,c))wood++;
            if(!RedwoodForest.contains(seed,x,z,d,c))continue;
            redwood++;
            if(Forest.potential(seed,x,z,d,c))forestOverlap++;
            if(Swamp.contains(seed,x,z,d,c))swampOverlap++;
            if(!OakBirchWoodland.contains(seed,x,z,d,c))notWoodland++;
            if(candidates.size()<40&&(candidates.isEmpty()||Math.abs(candidates.getLast()[0]-x)+Math.abs(candidates.getLast()[1]-z)>48000))candidates.add(new int[]{x,z});
        }
        require(forestOverlap==0&&swampOverlap==0&&notWoodland==0,"Redwood domain is inside plain woodland only, so cover arrays stay exclusive");
        require(redwood>40&&redwood<wood*.35,"Redwoods are a distinct but uncommon province ("+redwood+"/"+samples+")");
        int withTrees=0,logs=0,maxBlocks=0,high=0;
        for(var p:candidates){
            int cx=Math.floorDiv(p[0],16),cz=Math.floorDiv(p[1],16);var saved=new LinkedHashMap<String,int[]>();boolean trunk=false;
            for(int z=cz-1;z<=cz+1;z++)for(int x=cx-1;x<=cx+1;x++){
                var with=WoodlandTrees.column(seed,x,z,d,c,true,RedwoodForest.FLAG);
                var without=WoodlandTrees.column(seed,x,z,d,c,true,0);
                maxBlocks=Math.max(maxBlocks,with.length);
                require(with.length<blockbranch.cubic.CanopyLayer.MAX_BLOCKS*85/100,"Redwood canopy keeps headroom under the packed layer bound ("+with.length+")");
                // TerrainProfile rejects canopy at or below ground, and above its surface window.
                for(int v:with){int key=v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,y=(key>>>8)+TerrainShape.FLOOR,lx=(key&255)%16,lz=(key&255)/16;int ground=d.sample(x*16+lx,z*16+lz).height();require(y>ground&&y-ground<=blockbranch.cubic.TerrainProfile.CANOPY_WINDOW,"Canopy stays inside the profile surface window");}
                for(int v:with)if(RedwoodTrees.woodCode(v&blockbranch.cubic.CanopyLayer.STATE_MASK))trunk=true;
                for(int v:without)require(!RedwoodTrees.woodCode(v&blockbranch.cubic.CanopyLayer.STATE_MASK),"Historical definitions never place redwood bark");
                logs+=WoodlandDetails.redwoodLogs(seed,x,z,d,c).length;
                saved.put(x+","+z,with);
            }
            if(trunk)withTrees++;
            var order=new ArrayList<>(saved.keySet());Collections.reverse(order);
            for(var k:order){var q=k.split(",");require(Arrays.equals(saved.get(k),WoodlandTrees.column(seed,Integer.parseInt(q[0]),Integer.parseInt(q[1]),d,c,true,RedwoodForest.FLAG)),"Canopy columns do not depend on request order");}
        }
        require(logs>0,"Fallen redwood boles occur");
        // Species by elevation: sequoia only above its threshold; tall classes within bounds.
        var rootSites=new ArrayList<int[]>();
        for(int z=-1900000;z<=1900000&&rootSites.size()<8;z+=16000)for(int x=-1900000;x<=1900000&&rootSites.size()<8;x+=16000)
            if(RedwoodForest.contains(seed,x,z,d,c)&&d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL>=450)rootSites.add(new int[]{x,z});
        int tall=0;
        for(var p:rootSites)for(var r:RedwoodTrees.roots(seed,p[0]-64,p[1]-64,p[0]+64,p[1]+64,d,c)){
            if(r.birch()){sequoiaRoots++;require(r.y()-TerrainShape.SEA_LEVEL>=249&&r.height()<=95,"Sequoia sits at montane elevation within its height range");}
            if(r.height()>=70)tall++;
            require(r.height()>=10&&r.height()<=RedwoodTrees.MAX_STEM,"Stem heights stay in range");
        }
        for(var p:candidates)for(var r:RedwoodTrees.roots(seed,p[0]-64,p[1]-64,p[0]+64,p[1]+64,d,c)){
            if(r.birch())require(r.y()-TerrainShape.SEA_LEVEL>=249,"No sequoia below its elevation threshold");
            if(r.height()>=70)high++;
        }
        require(high>10,"Redwood stands contain true giants of 70 m and more ("+high+")");
        require(withTrees>=Math.min(8,candidates.size()/2),"Redwood provinces actually grow redwoods");
        System.out.println("REDWOOD_WORLD seed="+seed+" cells="+redwood+"/"+samples+" woodland="+wood+" sites="+candidates.size()+" withTrees="+withTrees+" fallenLogBlocks="+logs+" maxCanopyBlocks="+maxBlocks+" giants70+="+high);
    }
    private static String key(int x,int y,int z){return x+","+y+","+z;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private RedwoodForestCheck(){}
}
