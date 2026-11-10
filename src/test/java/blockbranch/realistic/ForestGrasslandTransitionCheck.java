package blockbranch.realistic;
import java.util.*;

public final class ForestGrasslandTransitionCheck {
    public static void main(String[] args){
        require(BiomeTransitionSchema.schema(255,1)==256&&BiomeTransitionSchema.matches(256,1),"New barrier");
        for(int schema:new int[]{82,141,142,253,254,255})require(BiomeTransitionSchema.baseSchema(schema,0)==schema&&!BiomeTransitionSchema.matches(schema,1),"Historical barrier "+schema);
        require(!BiomeTransitionSchema.matches(256,0)&&!BiomeTransitionSchema.matches(256,2),"No silent promotion or unknown definition");
        double prev=0;for(int i=0;i<=100;i++){double weight=ForestGrasslandTransition.forestWeight(.49+.0012*i);require(weight>=prev&&weight>=0&&weight<=1,"Monotone bounded taper");prev=weight;}
        int changed=0,added=0,removed=0,active=0,legacyRoots=0,infill=0;
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true);var c=new ClimateField(seed);
            var p=OakBirchWoodlandFixtures.fixture(d,seed);
            for(int dz=-320;dz<=320;dz+=64)for(int dx=-320;dx<=320;dx+=64){
                int x=p.x()+dx,z=p.z()+dz;
                var old=HistoricalTemperateRoots.roots(seed,x,z,x+31,z+31,d,c);
                // Closed stands add infill stems beyond the frozen 7 m selector; every frozen stem must remain, unchanged and in order.
                var current=WoodlandTrees.roots(seed,x,z,x+31,z+31,d,c);
                var frozen=new HashSet<Long>();for(var q:TreeScatter.points(seed,x-WoodlandTrees.RADIUS,z-WoodlandTrees.RADIUS,x+31+WoodlandTrees.RADIUS,z+31+WoodlandTrees.RADIUS))frozen.add(((long)q.x()<<32)^(q.z()&0xffffffffL));
                require(old.equals(current.stream().filter(r->frozen.contains(((long)r.x()<<32)^(r.z()&0xffffffffL))).toList()),"Frozen historical roots");legacyRoots+=old.size();infill+=current.size()-old.size();
                require(current.size()>=old.size(),"Infill only adds stems");
                var next=WoodlandTrees.roots(seed,x,z,x+31,z+31,d,c,1);
                var before=new HashSet<>(current);var after=new HashSet<>(next);
                for(var root:after)if(!before.contains(root)){added++;require(ForestGrasslandTransition.active(seed,root.x(),root.z(),d,c),"Added stem confined to eligible edge");}
                for(var root:before)if(!after.contains(root)){removed++;require(ForestGrasslandTransition.active(seed,root.x(),root.z(),d,c),"Removed stem confined to eligible edge");}
                if(!current.equals(next))changed++;
                if(ForestGrasslandTransition.active(seed,x,z,d,c))active++;
                require(next.equals(WoodlandTrees.roots(seed,x,z,x+31,z+31,d,c,1)),"Request order determinism");
            }
            System.out.println("TRANSITION_FIXTURE seed="+seed+" x="+p.x()+" z="+p.z());
        }
        require(changed>0&&added>0&&removed>0&&active>0&&legacyRoots>100&&infill>0,"Nontrivial edge and historical witnesses "+changed+" "+added+" "+removed);
        System.out.println("TRANSITION_ANALYTIC_PASS legacyRoots="+legacyRoots+" closedStandInfill="+infill+" changedRectangles="+changed+" added="+added+" removed="+removed+" active="+active);
    }
    private static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
}
