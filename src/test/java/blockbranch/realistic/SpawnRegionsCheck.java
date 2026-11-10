package blockbranch.realistic;

import blockbranch.cubic.ColumnLimits;
import java.util.*;

/** Seed variation, exact border geometry, bounded final-terrain selection and explicit impossible borders. */
public final class SpawnRegionsCheck {
    public static void main(String[]args){
        int limit=ColumnLimits.BORDER_LIMIT;var border=new SpawnRegions.Bounds(-limit,-limit,limit,limit);
        require(border.allows(-limit+SpawnRegions.CLEARANCE,0)&&!border.allows(-limit+SpawnRegions.CLEARANCE-1,0),"Left block-centre clearance");
        require(border.allows(limit-SpawnRegions.CLEARANCE-1,0)&&!border.allows(limit-SpawnRegions.CLEARANCE,0),"Right block-centre clearance");
        require(!border.allows(0,limit-SpawnRegions.CLEARANCE),"Final safe-ground result outside margin is rejected");
        for(var b:List.of(new SpawnRegions.Bounds(-100000,-100000,100000,100000),new SpawnRegions.Bounds(-128000,-128000,128000,128000))){
            boolean rejected=false;try{SpawnRegions.select(0,b,null,null);}catch(IllegalArgumentException e){rejected=e.getMessage().contains("128 km")&&e.getMessage().contains("fallback");}require(rejected,"Impossible margin reported before terrain sampling");
        }
        require(SpawnRegions.mild(new ClimateField.Sample(4,.12))&&SpawnRegions.mild(new ClimateField.Sample(26,.88)),"Moderate policy includes its endpoints");
        for(var c:List.of(new ClimateField.Sample(3.99,.5),new ClimateField.Sample(26.01,.5),new ClimateField.Sample(15,.119),new ClimateField.Sample(15,.881)))require(!SpawnRegions.mild(c),"Extreme climate proxy rejected");
        var reserved=border.reserve(10);int edge=-limit+SpawnRegions.CLEARANCE;
        require(!reserved.allows(edge,0)&&reserved.allows(edge+10,0),"Actual respawn radius reserved beyond minimum final clearance");
        require(border.allows(edge+10-10,0),"Extreme native placement offset retains128km");
        var searchBounds=border.reserve(10);
        var latitudes=new HashSet<Integer>();boolean north=false,south=false;double maxLatitude=0;
        for(long seed:new long[]{424242,1,-17,0,42,987654321,Long.MIN_VALUE,Long.MAX_VALUE}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=SpawnRegions.select(seed,searchBounds,d,c);
            require(searchBounds.allows(p.x(),p.z())&&p.probes()<=SpawnRegions.MAX_PROBES,"Bounded preferred region with full border clearance");
            require(SpawnRegions.eligible(p.x(),p.z(),d,c)&&SpawnRegions.mild(c.sample(p.x(),p.z(),d::sample)),"Dry gentle lowland and moderate actual climate");
            d.sample(-p.x(),-p.z());require(p.equals(SpawnRegions.select(seed,searchBounds,d,c)),"Seeded selection stable across terrain request order");
            latitudes.add((int)Math.floor(p.z()/262144.));north|=p.z()<0;south|=p.z()>0;maxLatitude=Math.max(maxLatitude,p.latitude());
            System.out.println("SPAWN_REGION seed="+seed+" selected="+p+" clearance="+border.clearance(p.x(),p.z()));
            if(seed==424242){var shifted=new SpawnRegions.Bounds(-500000,-1250000,1500000,750000);var q=SpawnRegions.select(seed,shifted,d,c);require(shifted.allows(q.x(),q.z()),"Actual shifted rectangular border respected");System.out.println("SPAWN_SHIFTED_BORDER "+q);}
        }
        require(north&&south&&latitudes.size()>=4&&maxLatitude>.5,"Starts span latitude bands on both sides instead of returning to zero");
        System.out.println("SPAWN_REGIONS_SEED_LATITUDE_ACTUAL_BORDER_FINAL_MARGIN_MILD_BOUNDED_DETERMINISM_PASS bins="+latitudes+" maxLatitude="+maxLatitude);
    }
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
    private SpawnRegionsCheck(){}
}
