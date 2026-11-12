package blockbranch.realistic;

import java.util.LinkedHashMap;

/** Prescribed alluvial bench morphology and reference-stage potential, not flood
 * recurrence, discharge, sediment transport or a seasonal-marsh classifier.
 * USGS defines floodplain as relatively flat land beside a stream, inundated at
 * high water (https://water.usgs.gov/water-basics_glossary.html). IUCN TF1.4 also
 * requires flooding/drying regimes; this static foundation does not supply them.
 * The 2m bench, 4m reference stage, 8m cut limit, 512m lateral/1024m path bounds
 * and smooth .001–.003 grade response are voxel-scale construction conventions.
 * The grade range ends at the existing gentle routed-bank domain, not a
 * universal ecological threshold. Wider trunk reaches remain outside this pass.
 */
final class AlluvialFloodplain {
    static final int BENCH_RISE=2,REFERENCE_RISE=4,MAX_CUT=8,MAX_REACH=512,MAX_PATH=1024;
    record Plan(int height,int water,int cut,boolean connectedCore){
        static final Plan NONE=new Plan(Integer.MIN_VALUE,DrainageNetwork.DRY,0,false);
        boolean changed(){return cut>0;}
    }
    private final DrainageNetwork reference;
    // Exact block keys and immutable plans; cache hits never approximate slopes
    // or move a boundary. Positive/negative entries share the same fixed budget.
    private final LinkedHashMap<Long,Plan> plans=new LinkedHashMap<>(512,.75f,true);
    private static final int TILE=32,HALO=2*MAX_CUT,SPAN=TILE+2*HALO;
    private final LinkedHashMap<Long,byte[]> cutTiles=new LinkedHashMap<>(32,.75f,true);
    private final LinkedHashMap<Long,Boolean> connected=new LinkedHashMap<>(512,.75f,true);
    AlluvialFloodplain(DrainageNetwork reference){this.reference=reference;}
    private Plan geometryPlan(int x,int z){
        var raw=rawPlan(x,z);if(!raw.changed())return raw;
        int tx=Math.floorDiv(x,TILE),tz=Math.floorDiv(z,TILE);long key=(tx&0xffffffffL)|((long)tz<<32);byte[] tile;
        // One immutable tile fill at a time avoids duplicate radial traces by
        // neighboring generation workers. It reads only the original reference
        // and raw plans, never connectivity or another filtered tile.
        synchronized(cutTiles){tile=cutTiles.get(key);if(tile==null){tile=cutTile(tx,tz);cutTiles.put(key,tile);if(cutTiles.size()>128)cutTiles.remove(cutTiles.keySet().iterator().next());}}
        int cut=tile[Math.floorMod(z,TILE)*TILE+Math.floorMod(x,TILE)];
        return cut==0?Plan.NONE:new Plan(raw.height()+raw.cut()-cut,raw.water(),cut,false);
    }
    private byte[] cutTile(int tx,int tz){
        int[] units=new int[SPAN*SPAN];int ox=tx*TILE-HALO,oz=tz*TILE-HALO;
        for(int z=0;z<SPAN;z++)for(int x=0;x<SPAN;x++)units[z*SPAN+x]=2*rawPlan(ox+x,oz+z).cut();
        limitCutGradient(units,SPAN,SPAN);
        byte[] result=new byte[TILE*TILE];for(int z=0;z<TILE;z++)for(int x=0;x<TILE;x++)result[z*TILE+x]=(byte)(units[(z+HALO)*SPAN+x+HALO]/2);
        return result;
    }
    /** Exact min-plus Manhattan envelope: min_q(2*rawCut(q)+|p-q|).
     * Thus cuts never expand, and adjacent integer cuts differ by at most1m.
     * A16m halo is sufficient because cuts are bounded at8m: farther zeros
     * cannot lower the result. This prevents eligibility/fit boundaries from
     * adding a many-block scarp to a stepped original slope. Half-block units
     * are a construction convention, not an ecological slope calibration. */
    static void limitCutGradient(int[] units,int width,int height){
        for(int z=0;z<height;z++)for(int x=0;x<width;x++){int i=z*width+x;if(x>0)units[i]=Math.min(units[i],units[i-1]+1);if(z>0)units[i]=Math.min(units[i],units[i-width]+1);}
        for(int z=height-1;z>=0;z--)for(int x=width-1;x>=0;x--){int i=z*width+x;if(x+1<width)units[i]=Math.min(units[i],units[i+1]+1);if(z+1<height)units[i]=Math.min(units[i],units[i+width]+1);}
    }
    private Plan rawPlan(int x,int z){
        long key=(x&0xffffffffL)|((long)z<<32);
        synchronized(plans){var found=plans.get(key);if(found!=null)return found;}
        var value=compute(x,z);
        synchronized(plans){plans.put(key,value);if(plans.size()>16384)plans.remove(plans.keySet().iterator().next());}
        return value;
    }
    /** Geometry never consumes connectivity. The second pass reads each cell's
     * actual immutable geometry plan, including its own exclusions/reach/fit,
     * so a skipped obstacle cannot be replaced by a hypothetical radial cut. */
    Plan plan(int x,int z){
        var geometry=geometryPlan(x,z);if(!geometry.changed())return geometry;
        long key=(x&0xffffffffL)|((long)z<<32);Boolean value;
        synchronized(connected){value=connected.get(key);}
        if(value==null){value=connected(x,z,geometry,REFERENCE_RISE);synchronized(connected){connected.put(key,value);if(connected.size()>8192)connected.remove(connected.keySet().iterator().next());}}
        return new Plan(geometry.height(),geometry.water(),geometry.cut(),value);
    }
    private boolean connected(int x,int z,Plan target,int rise){
        if(!belowStage(target.height()+1,target.water(),rise))return false;
        var reach=reference.alluvialReachAt(x,z);if(reach.edge()<0||reach.waterLevel()!=target.water())return false;
        double t=reference.closestT(reach.edge(),x,z);
        int cx=(int)Math.round(reference.edgeX(reach.edge(),t)),cz=(int)Math.round(reference.edgeZ(reach.edge(),t));
        var center=reference.sample(cx,cz);if(center.waterLevel()!=target.water()||reference.permanentFreshwaterLid(cx,cz))return false;
        int ax=Math.abs(x-cx),az=Math.abs(z-cz),steps=ax+az;if(steps>MAX_PATH)return false;
        int px=cx,pz=cz;
        for(int n=0;n<=steps;n++){
            var old=reference.sample(px,pz);if(old.waterLevel()!=DrainageNetwork.DRY&&old.waterLevel()!=target.water())return false;
            var realized=geometryPlan(px,pz);int height=realized.changed()?realized.height():old.height();
            if(!belowStage(height+1,target.water(),rise))return false;
            if(px==x){if(pz!=z)pz+=Integer.signum(z-cz);}
            else if(pz==z)px+=Integer.signum(x-cx);
            else if((Math.abs(px-cx)+.5)/ax<=(Math.abs(pz-cz)+.5)/az)px+=Integer.signum(x-cx);
            else pz+=Integer.signum(z-cz);
        }
        return true;
    }
    /** Sensitivity control over modified columns only; no weather/gameplay knob. */
    boolean potential(int x,int z,int rise){
        if(rise<=0||rise>32)throw new IllegalArgumentException("Bounded positive reference stage required");
        if(rise==REFERENCE_RISE)return plan(x,z).connectedCore();
        var geometry=geometryPlan(x,z);return geometry.changed()&&connected(x,z,geometry,rise);
    }
    static boolean belowStage(int firstAir,int water,int rise){return (long)firstAir<(long)water+rise;}
    private Plan compute(int x,int z){
        var reach=reference.alluvialReachAt(x,z);
        if(reach.edge()<0||reach.waterLevel()<=TerrainShape.SEA_LEVEL+8||reach.gradient()<0||reach.gradient()>=.003
            ||!Double.isFinite(reach.gradient())||!Double.isFinite(reach.distance())||!Double.isFinite(reach.halfWidth()))return Plan.NONE;
        var old=reference.sample(x,z);
        if(old.waterLevel()!=DrainageNetwork.DRY||old.bedrockCoast())return Plan.NONE;
        if(old.basin()!=reference.outlet(reach.edge()))return Plan.NONE;
        int above=old.height()+1-reach.waterLevel();if(above<=BENCH_RISE||above>BENCH_RISE+MAX_CUT+4)return Plan.NONE;
        if(reference.continentalIceThickness(x,z)>0||reference.landIceThickness(x,z)>0||reference.erodedInterior(x,z))return Plan.NONE;
        double t=reference.closestT(reach.edge(),x,z);
        int cx=(int)Math.round(reference.edgeX(reach.edge(),t)),cz=(int)Math.round(reference.edgeZ(reach.edge(),t));
        var center=reference.sample(cx,cz);
        if(center.waterLevel()!=reach.waterLevel()||reference.permanentFreshwaterLid(cx,cz))return Plan.NONE;
        double end=Math.min(t,1-t)*Math.hypot(reference.nodeX(reference.parent(reach.edge()))-reference.nodeX(reach.edge()),reference.nodeZ(reference.parent(reach.edge()))-reference.nodeZ(reach.edge()));
        double response=gradeResponse(reach.gradient())*smooth(end/(4*reach.halfWidth()+64));
        int predicted=loweredFirstAir(old.height()+1,reach.waterLevel(),reach.distance(),reach.halfWidth(),response);
        if(predicted>=old.height()+1)return Plan.NONE;
        int ax=Math.abs(x-cx),az=Math.abs(z-cz),steps=ax+az;if(steps>MAX_PATH)return Plan.NONE;
        double barrier=Double.NEGATIVE_INFINITY;
        var distanceFrame=reference.channelDistanceFrame(reach.edge(),t);
        // This original-terrain trace bounds the prescribed construction fit.
        // It is not a realized connectivity proof: plan() checks each actual
        // morphed cell in a separate, nonrecursive second pass.
        int px=cx,pz=cz;
        for(int n=0;n<=steps;n++){
            var sample=reference.sample(px,pz);
            if(sample.waterLevel()!=DrainageNetwork.DRY&&sample.waterLevel()!=reach.waterLevel())return Plan.NONE;
            double distance=distanceFrame.distance(px,pz);
            barrier=Math.max(barrier,loweredFirstAir(sample.height()+1,reach.waterLevel(),distance,reach.halfWidth(),response)-reach.waterLevel());
            if(barrier>=REFERENCE_RISE+4)return Plan.NONE;
            // Advance across one face at a time: a diagonal-only trace could
            // incorrectly cross an intervening pair of high cells.
            if(px==x){if(pz!=z)pz+=Integer.signum(z-cz);}
            else if(pz==z)px+=Integer.signum(x-cx);
            else if((Math.abs(px-cx)+.5)/ax<=(Math.abs(pz-cz)+.5)/az)px+=Integer.signum(x-cx);
            else pz+=Integer.signum(z-cz);
        }
        // A bounded taper leaves confined valley walls intact. This
        // radial construction fit is not a connectivity proof; the separate
        // realized-height pass alone supplies that property.
        double corridor=1-smooth((barrier-REFERENCE_RISE)/4);
        int firstAir=(int)Math.round(old.height()+1-(old.height()+1-predicted)*corridor);
        int cut=old.height()+1-firstAir;
        return cut<=0?Plan.NONE:new Plan(firstAir-1,reach.waterLevel(),cut,false);
    }
    static double gradeResponse(double gradient){return 1-smooth((gradient-.001)/.002);}
    static int loweredFirstAir(int oldFirstAir,int water,double distance,double width,double response){
        if(!Double.isFinite(distance)||!Double.isFinite(width)||!Double.isFinite(response)||width<=0||response<0||response>1)return oldFirstAir;
        double limit=Math.min(3*width+48,MAX_REACH),inner=1.4*width;
        if(limit<=1.8*width||distance<=width||distance>=limit||oldFirstAir<=water+BENCH_RISE)return oldFirstAir;
        double lateral=smooth((distance-width)/(.4*width))*(1-smooth((distance-(inner+.55*(limit-inner)))/(.45*(limit-inner))));
        double fit=1-smooth((oldFirstAir-water-(BENCH_RISE+MAX_CUT))/4.);
        return (int)Math.round(oldFirstAir-Math.min(MAX_CUT,oldFirstAir-water-BENCH_RISE)*response*lateral*fit);
    }
    DrainageNetwork.Sample apply(int x,int z,DrainageNetwork.Sample old){
        if(old.waterLevel()!=DrainageNetwork.DRY||old.bedrockCoast())return old;
        var plan=geometryPlan(x,z);if(!plan.changed())return old;
        if(old.height()-plan.height()!=plan.cut()||old.waterLevel()!=DrainageNetwork.DRY)throw new IllegalStateException("Alluvial plan requires original physical sample");
        return new DrainageNetwork.Sample(plan.height(),old.waterLevel(),old.basin(),old.closed(),old.bedrockCoast(),old.regionalHeight(),old.regionalWaterLevel(),old.sandyBackshore());
    }
    int cachedPlans(){synchronized(plans){return plans.size();}}
    private static double smooth(double v){double t=Math.clamp(v,0,1);return t*t*t*(t*(t*6-15)+10);}
}
