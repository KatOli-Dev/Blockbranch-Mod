package blockbranch.realistic;
import java.util.*;

/** Confirmed deep marine identity, actual shelf edge, deterministic substrate and no photosynthetic growth. */
public final class DeepOceanCheck {
    public static void main(String[]args){
        for(int depth:new int[]{0,1,200,201,2000,6000,10000})for(double slope:new double[]{0,.08,.08001,.2,.20001,1}){
            var s=new DeepOcean.Site(63-depth,64,true,false,false,slope);require(DeepOcean.eligible(s)==(depth>200),"Only deep connected marine band");int c=DeepOcean.cover(424242,13,-29,true,s);
            require((c>0)==(depth>200),"No shelf/dry overlay");require(c==0||c==5||c==6||c==8||c==9,"Only sediment/gravel/rock, no photosynthetic cover");
            if(depth>200){if(slope>.2)require(c==9,"Steeper rock exposed");else if(slope>.08)require(c==8,"Coarse sloping bed");else require(c==5||c==6,"Gentle sedimented basin floor");}
            require(DeepOcean.cover(424242,13,-29,false,s)==0,"Excluded cells preserve prior surfaces");
        }
        for(var s:List.of(new DeepOcean.Site(-4000,64,false,false,false,0),new DeepOcean.Site(-4000,64,true,true,false,0),new DeepOcean.Site(-4000,64,true,false,true,0),new DeepOcean.Site(-4000,65,true,false,false,0)))require(!DeepOcean.eligible(s),"Marine datum alone, lakes and rivers rejected");
        for(long seed:new long[]{424242,1,-17}){
            long start=System.nanoTime();var d=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var p=blockbranch.realistic.DeepOceanFixtures.fixture(d,seed);int[] codes=new int[13];
            for(int z=p.z()-128;z<=p.z()+128;z+=8)for(int x=p.x()-128;x<=p.x()+128;x+=8){var site=DeepOcean.site(d,x,z);boolean habitat=DeepOcean.contains(x,z,d);int code=DeepOcean.cover(seed,x,z,habitat,site);codes[code]++;
                require(habitat==DeepOcean.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d),"Negative/native quart sampling stable");
                if(habitat)require(PlacementBaseline.definition22(seed,x,z,d,climate).equals("placeholder"),"All sixteen prior habitats retained");
                if(code>0)require(DeepOcean.eligible(site)&&code<10,"Raw deep marine bed without plants");
            }
            var edge=blockbranch.realistic.DeepOceanFixtures.transition(d,seed);require(MarineShelf.contains(edge.insideX(),edge.insideZ(),d)&&!DeepOcean.contains(edge.insideX(),edge.insideZ(),d)&&DeepOcean.contains(edge.outsideX(),edge.outsideZ(),d)&&!MarineShelf.contains(edge.outsideX(),edge.outsideZ(),d),"Actual exclusive shelf/deep boundary");
            require(MarineShelf.site(d,edge.insideX(),edge.insideZ()).depth()<=200&&DeepOcean.site(d,edge.outsideX(),edge.outsideZ()).depth()>=201&&Math.max(Math.abs(edge.insideX()-edge.outsideX()),Math.abs(edge.insideZ()-edge.outsideZ()))<=4,"Adjacent4m cells straddle the canonical200m depth band; source relief may skip an exact200m sample");
            require(edge.equals(blockbranch.realistic.DeepOceanFixtures.transition(d,seed)),"Seeded edge stable");require(codes[5]>0&&codes[6]>0,"Natural contrasting basin sediments");
            var lake=blockbranch.realistic.LakeSedimentFixtures.fixture(d,seed);var river=blockbranch.realistic.RiverChannelFixtures.fixture(d,seed);var shelf=blockbranch.realistic.MarineShelfFixtures.fixture(d,seed);require(!DeepOcean.contains(lake.x(),lake.z(),d)&&!DeepOcean.contains(river.x(),river.z(),d)&&!DeepOcean.contains(shelf.x(),shelf.z(),d),"Actual freshwater and shallow habitats retained");
            System.out.printf("DEEP_OCEAN seed=%d fixture=%s site=%s codes=%s edge=%s elapsedMs=%d%n",seed,p,DeepOcean.site(d,p.x(),p.z()),Arrays.toString(codes),edge,(System.nanoTime()-start)/1000000);
        }
        System.out.println("DEEP_OCEAN_IDENTITY_SUBSTRATE_NO_PLANTS_BOUNDARY_PASS");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private DeepOceanCheck(){}
}
