package blockbranch.realistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

/** Exact derived work per terminal lake. No world, executor, task queue or partial save. */
final class WetlandHutRegions {
    private final long seed;
    private final RealisticGenerator.HutPlanningTerrain terrain;
    private final List<Region> regions;
    private final Int2ObjectOpenHashMap<Region> byLake=new Int2ObjectOpenHashMap<>();
    private volatile WetlandHuts complete;

    WetlandHutRegions(long seed,RealisticGenerator.HutPlanningTerrain terrain){
        this.seed=seed;this.terrain=terrain;var d=terrain.drainage();var found=new ArrayList<Region>();
        BooleanSupplier cancelled=Thread.currentThread()::isInterrupted;
        for(int lake=0;lake<d.size();lake++){
            HutPlanningWork.checkCancelled(cancelled);
            if(d.parent(lake)!=-1||!d.closed(lake)||d.waterLevel(lake)<=64||d.waterLevel(lake)>664
                ||Math.floorMod(OakBirchWoodland.mix(seed^0x4855544C414B4531L^lake),3)!=0)continue;
            double largest=0;for(int angle=0;angle<64;angle++)largest=Math.max(largest,d.lakeShoreRadius(lake,angle*Math.PI/32));
            // Same 64 directions/radial maximum as definition1, plus conservative
            // integer rounding and chunk-center snapping. No terrain sampling.
            var region=new Region(lake,d.nodeX(lake),d.nodeZ(lake),(int)Math.ceil(largest)+16);
            found.add(region);byLake.put(lake,region);
        }
        regions=List.copyOf(found);if(regions.isEmpty())complete=WetlandHuts.fromPlans(seed,List.of());
    }
    List<Region> regions(){return regions;}
    WetlandHuts completed(){return complete;}
    int resolvedCount(){int count=0;for(var region:regions)if(region.result!=null)count++;return count;}

    Optional<WetlandHuts.Plan> forStart(int chunkX,int chunkZ){
        long wx=((long)chunkX<<4)+8,wz=((long)chunkZ<<4)+8;
        if(Math.abs(wx)>2097100||Math.abs(wz)>2097100)return Optional.empty();
        int x=(int)wx,z=(int)wz,cx=Math.floorDiv(x-DrainageNetwork.MIN,DrainageNetwork.STEP),cz=Math.floorDiv(z-DrainageNetwork.MIN,DrainageNetwork.STEP);
        Optional<WetlandHuts.Plan> answer=Optional.empty();
        // Jitter <738 and reach <2215: every owner is in these nine nominal cells.
        // Visit all possible owners before returning, with original lake-index
        // order so even duplicate-start precedence remains the original last one.
        for(int nz=Math.max(0,cz-1);nz<=Math.min(DrainageNetwork.SIDE-1,cz+1);nz++)
            for(int nx=Math.max(0,cx-1);nx<=Math.min(DrainageNetwork.SIDE-1,cx+1);nx++){
                var region=byLake.get(nz*DrainageNetwork.SIDE+nx);
                if(region==null||Math.abs((long)x-region.x)>region.reach||Math.abs((long)z-region.z)>region.reach)continue;
                var plan=region.plan();if(plan.isPresent()&&(plan.get().x()>>4)==chunkX&&(plan.get().z()>>4)==chunkZ)answer=plan;
            }
        return answer;
    }
    synchronized WetlandHuts all(){
        if(complete!=null)return complete;
        var plans=HutPlanningWork.run(regions.size(),(first,end,cancelled)->{
            var out=new ArrayList<WetlandHuts.Plan>();
            for(int i=first;i<end;i++){HutPlanningWork.checkCancelled(cancelled);regions.get(i).resolve(cancelled).ifPresent(out::add);}
            return out;
        });
        HutPlanningWork.checkCancelled(Thread.currentThread()::isInterrupted);
        complete=WetlandHuts.fromPlans(seed,plans);return complete;
    }
    final class Region implements WetlandHuts.Candidate {
        private final int lake,x,z,reach;
        private volatile Optional<WetlandHuts.Plan> result;
        Region(int lake,int x,int z,int reach){this.lake=lake;this.x=x;this.z=z;this.reach=reach;}
        public int lake(){return lake;}
        public double lowerDistance(BlockPos origin){return Math.max(0,Math.nextDown(Math.hypot(Math.max(0,Math.abs((long)origin.getX()-x)-reach),Math.max(0,Math.abs((long)origin.getZ()-z)-reach))));}
        public Optional<WetlandHuts.Plan> plan(){return resolve(Thread.currentThread()::isInterrupted);}
        private synchronized Optional<WetlandHuts.Plan> resolve(BooleanSupplier cancelled){
            HutPlanningWork.checkCancelled(cancelled);if(result!=null)return result;
            var values=WetlandHuts.buildRange(seed,terrain,lake,lake+1,cancelled);
            var evaluated=values.isEmpty()?Optional.<WetlandHuts.Plan>empty():Optional.of(new WetlandHuts.Plan(values.get(0),values.get(1),values.get(2),values.get(3),values.get(4),values.get(5)));
            HutPlanningWork.checkCancelled(cancelled);result=evaluated;return evaluated;
        }
    }
}
