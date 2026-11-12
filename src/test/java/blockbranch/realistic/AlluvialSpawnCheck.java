package blockbranch.realistic;

import blockbranch.cubic.ColumnLimits;
import java.util.HashSet;

/** Exercise established spawn policy against the complete new physical inputs. */
public final class AlluvialSpawnCheck {
    public static void main(String[] args){
        int limit=ColumnLimits.BORDER_LIMIT;var border=new SpawnRegions.Bounds(-limit,-limit,limit,limit);var reserved=border.reserve(10);var bins=new HashSet<Integer>();boolean north=false,south=false;
        for(long seed:new long[]{424242,1,-17,0,42,987654321,Long.MIN_VALUE,Long.MAX_VALUE}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true,true);var c=new ClimateField(seed,true);var p=SpawnRegions.select(seed,reserved,d,c);
            require(reserved.allows(p.x(),p.z())&&border.clearance(p.x(),p.z())>=128010,"128km clearance plus native player radius");
            require(p.probes()<=SpawnRegions.MAX_PROBES&&SpawnRegions.eligible(p.x(),p.z(),d,c)&&SpawnRegions.mild(c.sample(p.x(),p.z(),d::sample)),"Bounded dry gentle non-extreme final-terrain start");
            d.sample(-p.x(),-p.z());require(p.equals(SpawnRegions.select(seed,reserved,d,c)),"Stable across terrain request order");
            int edges=0,shifted=0,comparisons=0;
            for(int edge=0;edge<d.size()&&edges<128;edge+=97)if(d.river(edge)){
                edges++;for(double t:new double[]{0,.2,.5,.8,1}){var frame=d.channelDistanceFrame(edge,t);if(!frame.straight())shifted++;
                    for(int offset=-512;offset<=512;offset+=16){int x=(int)Math.round(d.edgeX(edge,t))+offset,z=(int)Math.round(d.edgeZ(edge,t))-offset/2;
                        require(Double.doubleToLongBits(frame.distance(x,z))==Double.doubleToLongBits(d.channelDistance(edge,t,x,z)),"Hoisted projection retains exact floating-point evaluation");comparisons++;}}
            }
            require(edges==128&&shifted>0,"Projection equivalence exercises real shifted river curves");System.out.println("ALLUVIAL_DISTANCE_FRAME_EXACT seed="+seed+" comparisons="+comparisons+" shiftedFrames="+shifted);
            bins.add(
Math.floorDiv(p.z(),262144));north|=p.z()<0;south|=p.z()>0;System.out.println("ALLUVIAL_SPAWN seed="+seed+" selection="+p+" clearance="+border.clearance(p.x(),p.z()));
        }
        require(north&&south&&bins.size()>=4,"Seed diversity spans both hemispheres and multiple latitude bands");System.out.println("ALLUVIAL_SPAWN_FINAL_TERRAIN_POLICY_PASS bins="+bins);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private AlluvialSpawnCheck(){}
}
