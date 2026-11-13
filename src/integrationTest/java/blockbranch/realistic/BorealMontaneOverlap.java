package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Native witness that boreal slope tolerance (flag 128) never touches montane-owned ground or stems.
 * Boreal roots are never filtered by the montane owner (published behaviour), so the guarantee is the
 * climate disjointness of the two domains plus additions-only rasterization and clear plans. Everything
 * here runs the production owner, canopy column and saved clear-plan replay of a world with both gates. */
final class BorealMontaneOverlap {
    static final int SCAN=1024,STEP=4,WINDOW=96;
    static String run(ServerLevel level,RealisticGenerator g,Path dir,long seed,int[][] sites)throws Exception{
        if(g.borealSlopeDefinition()!=1||g.montaneForestDefinition()!=1)throw new AssertionError("Both gates required");
        var random=level.getChunkSource().randomState();var terrain=g.hutPlanningTerrain(random);var d=terrain.drainage();var climate=terrain.climate();
        var montane=g.montaneForestOwner(random);var dry=g.dryForestOwner(random,true);var summer=g.summerDryOwner(random);
        int all=g.vegetationFlags(),base=all&~BorealSlope.FLAG;
        if((all&MontaneForest.FLAG)==0||(all&BorealSlope.FLAG)==0)throw new AssertionError("Both saved flags expected: "+all);
        var context=StructurePieceSerializationContext.fromLevel(level);var report=new StringBuilder();
        long totalAdded=0,totalOwned=0,nearBoth=0;int windowsWithAdded=0,windowsWithOwned=0,windowsWithBoth=0,columns=0,planWitnesses=0;long changedEntries=0;double globalMin=Double.MAX_VALUE;
        for(var site:sites){
            int sx=site[0],sz=site[1];
            var added=addedRoots(seed,sx-SCAN,sz-SCAN,sx+SCAN,sz+SCAN,d,climate);
            var owned=new ArrayList<int[]>();
            for(int z=sz-SCAN;z<=sz+SCAN;z+=STEP)for(int x=sx-SCAN;x<=sx+SCAN;x+=STEP)if(montane.test(x,z)){
                owned.add(new int[]{x,z});
                // Domain disjointness: a montane-owned cell is never inside the boreal woodland domain.
                if(BorealWoodland.contains(seed,x,z,d,climate))throw new AssertionError("Montane-owned cell inside boreal woodland domain "+x+","+z);
            }
            // Nearest montane-owned sample for every added root, by cell hash.
            var cells=new HashMap<Long,List<int[]>>();for(var o:owned)cells.computeIfAbsent(((long)Math.floorDiv(o[0],64)<<32)^(Math.floorDiv(o[1],64)&0xffffffffL),k->new ArrayList<>()).add(o);
            double min=Double.MAX_VALUE;WoodlandTrees.Root nearest=null;
            for(var r:added){
                if(montane.test(r.x(),r.z()))throw new AssertionError("Added boreal root on a montane-owned cell "+r);
                for(int cz=Math.floorDiv(r.z(),64)-2;cz<=Math.floorDiv(r.z(),64)+2;cz++)for(int cx=Math.floorDiv(r.x(),64)-2;cx<=Math.floorDiv(r.x(),64)+2;cx++){
                    for(var o:cells.getOrDefault(((long)cx<<32)^(cz&0xffffffffL),List.of())){double dist=Math.hypot(o[0]-r.x(),o[1]-r.z());if(dist<min){min=dist;nearest=r;}}
                }
            }
            totalAdded+=added.size();totalOwned+=owned.size();if(!added.isEmpty())windowsWithAdded++;if(!owned.isEmpty())windowsWithOwned++;if(!added.isEmpty()&&!owned.isEmpty())windowsWithBoth++;
            if(min<Double.MAX_VALUE)globalMin=Math.min(globalMin,min);if(min<=24)nearBoth++;
            report.append("site ").append(sx).append(',').append(sz).append(" addedRoots=").append(added.size()).append(" montaneOwnedSamples=").append(owned.size()).append(" nearestAddedToOwned=").append(min==Double.MAX_VALUE?"none":String.format("%.1f",min)).append('\n');
            // Whole-column check around the audited/closest pair: production canopy with and without the flag.
            var focus=new ArrayList<int[]>();focus.add(new int[]{sx,sz});if(nearest!=null)focus.add(new int[]{nearest.x(),nearest.z()});
            for(var f:focus){
                int pad=WINDOW+32;var local=addedRoots(seed,f[0]-pad,f[1]-pad,f[0]+pad,f[1]+pad,d,climate);
                var grown=new HashMap<Long,List<int[]>>();for(var r:local)for(var v:BorealTrees.tree(r))grown.computeIfAbsent(bucket(v.x(),v.y(),v.z()),k->new ArrayList<>()).add(new int[]{v.x(),v.y(),v.z()});
                var montaneVoxels=new HashSet<Long>();for(var m:MontaneForestTrees.roots(seed,f[0]-pad,f[1]-pad,f[0]+pad,f[1]+pad,d,climate,montane))for(var v:TaigaTrees.tree(m))montaneVoxels.add(BlockPos.asLong(v.x(),v.y(),v.z()));
                for(int cz=(f[1]-WINDOW)>>4;cz<=(f[1]+WINDOW)>>4;cz++)for(int cx=(f[0]-WINDOW)>>4;cx<=(f[0]+WINDOW)>>4;cx++){
                    var a=toSet(WoodlandTrees.column(seed,cx,cz,d,climate,true,base,dry,summer,montane));var b=toSet(WoodlandTrees.column(seed,cx,cz,d,climate,true,all,dry,summer,montane));
                    var changed=new ArrayList<Integer>();for(int v:a)if(!b.contains(v))changed.add(v);for(int v:b)if(!a.contains(v))changed.add(v);
                    for(int v:changed){
                        int key=v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,loc=key&255;int wx=cx*16+(loc&15),wz=cz*16+(loc>>4),wy=(key>>8)+TerrainShape.FLOOR;
                        if(!within(grown,wx,wy,wz,6))throw new AssertionError("Flag 128 changed canopy at "+wx+","+wy+","+wz+" farther than the six-block support reach from any added stem");
                        if(montaneVoxels.contains(BlockPos.asLong(wx,wy,wz)))throw new AssertionError("Flag 128 changed a montane stem voxel at "+wx+","+wy+","+wz);
                        changedEntries++;
                    }
                    columns++;
                }
            }
            // Saved clear plans: the tolerant plan only adds trees; existing cells keep their replacement value.
            int made=0;
            for(var r:added){
                if(made>=3)break;
                if(r.family()!=0&&r.family()!=15&&r.family()!=4&&r.family()!=5)continue;
                var box=new BoundingBox(r.x(),r.y(),r.z(),r.x(),r.y(),r.z());
                var oldPlan=BorealClearSmoke.plan(new ReimaginedTreeClearPiece(box,seed,g.borealCrownDefinition(),base),g,random);
                var newPlan=BorealClearSmoke.plan(new ReimaginedTreeClearPiece(box,seed,g.borealCrownDefinition(),all),g,random);
                var added3=new HashSet<Long>();for(var ar:addedRoots(seed,r.x()-96,r.z()-96,r.x()+96,r.z()+96,d,climate))for(var v:BorealTrees.tree(ar))added3.add(BlockPos.asLong(v.x(),v.y(),v.z()));
                for(var e:oldPlan.entrySet()){if(!newPlan.containsKey(e.getKey()))throw new AssertionError("Tolerant clear plan dropped a cleared cell at "+BlockPos.of(e.getKey()));
                    if(!newPlan.get(e.getKey()).equals(e.getValue())&&!added3.contains(e.getKey()))throw new AssertionError("Tolerant clear plan changed a cell outside any added stem at "+BlockPos.of(e.getKey()));}
                if(newPlan.size()<=oldPlan.size())throw new AssertionError("Clear plan at an added stem gains nothing "+r);
                // No montane stem voxel is ever cleared by a plan around a boreal-only stem: a cleared cell inside montane ground must also be cleared by the base plan.
                for(long key:newPlan.keySet()){var at=BlockPos.of(key);if(!oldPlan.containsKey(key)&&montane.test(at.getX(),at.getZ())&&MontaneForestTrees.roots(seed,at.getX()-8,at.getZ()-8,at.getX()+8,at.getZ()+8,d,climate,montane).stream().anyMatch(m->TaigaTrees.tree(m).stream().anyMatch(v->v.x()==at.getX()&&v.y()==at.getY()&&v.z()==at.getZ())))
                    throw new AssertionError("Tolerant plan clears a montane stem voxel at "+at);}
                made++;planWitnesses++;
            }
        }
        if(totalAdded==0||windowsWithAdded==0)throw new AssertionError("Witness must contain added boreal roots");
        if(totalOwned==0)throw new AssertionError("Witness must contain montane-owned ground");
        String text="windows="+sites.length+" addedRoots="+totalAdded+" montaneOwnedSamples="+totalOwned+" windowsWithBoth="+windowsWithBoth+" nearestAddedToOwned="+(globalMin==Double.MAX_VALUE?"none":String.format("%.1f",globalMin))+" within24="+nearBoth+" columnsCompared="+columns+" changedEntriesAllNearAddedStems="+changedEntries+" planWitnesses="+planWitnesses+"\n"+report;
        Files.writeString(dir.resolve("overlap-report.txt"),text);return text;
    }
    /** Roots present only at relief 5, by position. */
    static List<WoodlandTrees.Root> addedRoots(long seed,int x0,int z0,int x1,int z1,DrainageNetwork d,ClimateField c){
        var published=new HashSet<Long>();for(var r:BorealTrees.roots(seed,x0,z0,x1,z1,d,c,BorealSlope.PUBLISHED_RELIEF))published.add(((long)r.x()<<32)^(r.z()&0xffffffffL));
        var out=new ArrayList<WoodlandTrees.Root>();
        for(var r:BorealTrees.roots(seed,x0,z0,x1,z1,d,c,BorealSlope.RELIEF))if(!published.contains(((long)r.x()<<32)^(r.z()&0xffffffffL)))out.add(r);
        return out;
    }
    private static long bucket(int x,int y,int z){return ((long)(x>>3)&0x1fffff)<<42|((long)(y>>3)&0x1fffff)<<21|((long)(z>>3)&0x1fffff);}
    private static boolean within(Map<Long,List<int[]>> grown,int x,int y,int z,int l1){
        for(int dz=-1;dz<=1;dz++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)
            for(var v:grown.getOrDefault(bucket(x+dx*8,y+dy*8,z+dz*8),List.of()))if(Math.abs(v[0]-x)+Math.abs(v[1]-y)+Math.abs(v[2]-z)<=l1)return true;
        return false;
    }
    private static Set<Integer> toSet(int[] values){var s=new HashSet<Integer>();for(int v:values)s.add(v);return s;}
    private BorealMontaneOverlap(){}
}
