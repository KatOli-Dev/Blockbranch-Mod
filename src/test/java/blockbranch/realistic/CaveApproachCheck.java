package blockbranch.realistic;

import java.util.*;

/** Bounded planning safety/immutability checks; native walking is tested separately. */
public final class CaveApproachCheck {
    public static void main(String[] args){
        var step=new CaveEntryRoute(new int[]{128,20,128,129,21,128,130,21,128,131,21,128,132,21,128,133,21,128,134,21,128});
        require(step.contains(128,22,128)&&step.contains(128,23,128),"Raised step has jumping headroom over lower floor");
        require(!step.contains(128,19,128)&&!step.contains(129,20,128),"Clearance never removes either walking floor");
        long[] seeds={424242,1,-17};int[][] centres={{237582,-262158},{57336,-57376},{-122868,180198}};
        for(int s=0;s<3;s++){
            long seed=seeds[s];var terrain=new DrainageNetwork(new TerrainShape(seed),seed,true,true);var field=new UndergroundStructure(seed);int found=0,eligible=0,extendedFound=0;
            for(int z=-3;z<=3;z++)for(int x=-3;x<=3;x++){
                int cx=Math.floorDiv(centres[s][0],384)+x,cz=Math.floorDiv(centres[s][1],384)+z;
                if(CaveApproach.eligibleSite(seed,cx,cz,terrain,(xx,zz)->false).present())eligible++;
                var approach=CaveApproach.plan(seed,cx,cz,terrain,(xx,zz)->false);
                if(!approach.present())continue;found++;
                var route=approach.route(terrain,field);require(route.size()>40,"Useful supported route");
                var end=route.getLast();int ground=terrain.sample(end[0],end[2]).height();
                require(field.voidAt(end[0],end[1],end[2],ground)&&ground-end[1]>=40,"Approach reaches a pre-existing cave");
                require(Arrays.equals(approach.encode(),CaveApproach.plan(seed,cx,cz,terrain,(xx,zz)->false).encode()),"Deterministic repeated plan");
                int[] encoded=approach.encode(),before=approach.encode();encoded[0]++;
                require(Arrays.equals(before,approach.encode()),"Immutable encoding");
                var roundtrip=new CaveApproach(before);
                for(var p:route){require(terrain.sample(p[0],p[2]).waterLevel()==DrainageNetwork.DRY,"Dry route");require(approach.contains(p[0],p[1],p[2])==roundtrip.contains(p[0],p[1],p[2]),"Geometry roundtrip");}
                var extended=approach.extend(terrain,field);
                if(extended.present()){extendedFound++;
                    var continuation=extended.entry();require(continuation.present(),"Accepted entrance has local continuation");var steps=continuation.route();require(steps.size()>=7&&steps.size()<=33,"Finite useful continuation");
                    var rebuilt=new CaveApproach(before,new CaveEntryRoute(continuation.encode()));require(Arrays.equals(continuation.encode(),approach.extend(terrain,field).entry().encode()),"Deterministic continuation");
                    for(var p:steps){int h=terrain.sample(p[0],p[2]).height();require(p[1]<=h-16&&field.voidAt(p[0],p[1],p[2],h)&&field.voidAt(p[0],p[1]+1,p[2],h),"Existing natural air and protected roof");require(!extended.contains(p[0],p[1]-1,p[2]),"Continuation preserves its walking floor");require(rebuilt.contains(p[0],p[1]+2,p[2])==extended.contains(p[0],p[1]+2,p[2]),"Continuation roundtrip");}
                    var mutableEntry=continuation.encode();mutableEntry[0]++;require(!Arrays.equals(mutableEntry,continuation.encode()),"Immutable continuation encoding");
                }
                int[] mutable=before.clone();mutable[1]=TerrainShape.FLOOR;boolean rejected=false;try{new CaveApproach(mutable);}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Out-of-envelope approach rejected");
            }
            require(extendedFound>0,"At least one continuation per qualified seed");
            require(found>0,"At least one bounded dry approach per qualified seed");
            System.out.println("CAVE_APPROACH_PLAN seed="+seed+" extended="+extendedFound+" accepted="+found+" eligible="+eligible+" sites=49 denominator=planning_sites_not_cave_systems");
        }
        System.out.println("CAVE_APPROACH_ANALYTIC_PASS no native movement claim");
    }
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
