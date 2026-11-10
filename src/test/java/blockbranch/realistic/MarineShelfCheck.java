package blockbranch.realistic;
import java.util.*;

/** Marine identity, shelf/deep/shore boundaries, static bed sorting and light-limited irregular seagrass. */
public final class MarineShelfCheck {
    public static void main(String[]args){
        var warm=new ClimateField.Sample(12,.7);
        for(int depth:new int[]{0,1,2,7,8,40,200,201})for(double slope:new double[]{0,.12,.20001,.50001}){
            var s=new MarineShelf.Site(63-depth,64,true,false,false,slope);require(MarineShelf.eligible(s)==(depth>=1&&depth<=200),"Bounded marine depth band");
            require(MarineShelf.plantSuitable(s,warm)==(depth>=2&&depth<=7&&slope<=.12),"Shallow/gentle photic window");int code=MarineShelf.cover(424242,13,-29,true,s,warm);
            require((code>0)==MarineShelf.eligible(s),"Only eligible shelf has material");if(code>=10)require(code==12&&MarineShelf.plantSuitable(s,warm),"Seagrass only on stable shallow sand");
            if(MarineShelf.eligible(s)&&slope>.5)require(code==9,"Steep submarine rock retained");else if(MarineShelf.eligible(s)&&slope>.2)require(code==8,"Exposed shelf slope retains coarse gravel");
            require(MarineShelf.cover(424242,13,-29,false,s,warm)==0,"Protected habitats receive no shelf overlay");
        }
        for(var s:List.of(new MarineShelf.Site(58,64,false,false,false,0),new MarineShelf.Site(58,64,true,true,false,0),new MarineShelf.Site(58,64,true,false,true,0),new MarineShelf.Site(58,65,true,false,false,0)))require(!MarineShelf.eligible(s),"Inland, river, lake or mismatched water is not shelf");
        var steady=new MarineShelf.Site(58,64,true,false,false,.05);int occupied=0,pairs=0,total=0,edgeChanges=0,innerChanges=0,edgeN=0,innerN=0;
        for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){boolean plant=MarineShelf.cover(424242,x,z,true,steady,warm)>=10,next=MarineShelf.cover(424242,x+1,z,true,steady,warm)>=10;total++;if(plant)occupied++;if(plant&&next)pairs++;if(Math.floorMod(x+1,32)==0){edgeN++;if(plant!=next)edgeChanges++;}else{innerN++;if(plant!=next)innerChanges++;}}
        require(pairs/(double)total>Math.pow(occupied/(double)total,2)*1.2,"Marine seagrass has coherent colonies beyond uniform scatter");require(Math.abs(edgeChanges/(double)edgeN-innerChanges/(double)innerN)<.03,"No colony-cell edge bias");
        for(long seed:new long[]{424242,1,-17}){
            long start=System.nanoTime();var d=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var p=blockbranch.realistic.MarineShelfFixtures.fixture(d,seed);int[] codes=new int[13];int protectedShore=0,marine=0,deep=0;
            for(int z=p.z()-256;z<=p.z()+256;z+=8)for(int x=p.x()-256;x<=p.x()+256;x+=8){var site=MarineShelf.site(d,x,z);boolean habitat=MarineShelf.contains(x,z,d);int code=MarineShelf.cover(seed,x,z,habitat,site,climate.sample(x,z,d::sample));codes[code]++;
                require(habitat==MarineShelf.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d),"Negative/native quart sampling stable");
                if(LowGradientSandyShore.contains(x,z,(wx,wz)->LowGradientSandyShore.site(d,wx,wz))){protectedShore++;require(!habitat,"Existing shore retained");}
                if(habitat)require(PlacementBaseline.definition21(seed,x,z,d,climate).equals("placeholder"),"All fifteen previous habitats retained");
                if(code>0){marine++;require(MarineShelf.eligible(site)&&d.oceanWater(x,z,d.sample(x,z)),"Raw material only in confirmed marine shelf");}
                if(code>=10)require(MarineShelf.plantSuitable(site,climate.sample(x,z,d::sample)),"Real photic/gentle plant placement");
                if(site.ocean()&&site.depth()>200){deep++;require(code==0&&!habitat,"Deep ocean left untouched");}
            }
            var site=MarineShelf.site(d,p.x(),p.z());require(MarineShelf.cover(seed,p.x(),p.z(),true,site,climate.sample(p.x(),p.z(),d::sample))==12,"Natural seagrass over sand witness");
            var edge=blockbranch.realistic.MarineShelfFixtures.transition(d,seed);require(MarineShelf.contains(edge.insideX(),edge.insideZ(),d)&&!MarineShelf.contains(edge.outsideX(),edge.outsideZ(),d)&&Math.max(Math.abs(edge.insideX()-edge.outsideX()),Math.abs(edge.insideZ()-edge.outsideZ()))<=4,"Adjacent shelf/shore/deep edge");require(edge.equals(blockbranch.realistic.MarineShelfFixtures.transition(d,seed)),"Seeded transition stable");
            var river=blockbranch.realistic.RiverChannelFixtures.fixture(d,seed);var lake=blockbranch.realistic.LakeSedimentFixtures.fixture(d,seed);var shore=blockbranch.realistic.LowGradientSandyShoreFixtures.fixture(d,seed);
            require(!MarineShelf.contains(lake.x(),lake.z(),d)&&!MarineShelf.contains(river.x(),river.z(),d)&&!MarineShelf.contains(shore.x(),shore.z(),d),"Actual lake, routed river and shore remain distinct");
            var deepEdge=blockbranch.realistic.MarineShelfFixtures.deepTransition(d,seed);require(MarineShelf.contains(deepEdge.insideX(),deepEdge.insideZ(),d)&&!MarineShelf.contains(deepEdge.outsideX(),deepEdge.outsideZ(),d)&&MarineShelf.site(d,deepEdge.outsideX(),deepEdge.outsideZ()).depth()>200&&Math.max(Math.abs(deepEdge.insideX()-deepEdge.outsideX()),Math.abs(deepEdge.insideZ()-deepEdge.outsideZ()))<=4,"Actual canonical shelf/deep boundary");
            System.out.printf("MARINE_SHELF_DEEP_EDGE seed=%d edge=%s depths=%d/%d%n",seed,deepEdge,MarineShelf.site(d,deepEdge.insideX(),deepEdge.insideZ()).depth(),MarineShelf.site(d,deepEdge.outsideX(),deepEdge.outsideZ()).depth());
            require(marine>0&&codes[12]>0,"Real shelf and seagrass sampled");System.out.printf("MARINE_SHELF seed=%d fixture=%s site=%s codes=%s protectedShore=%d deep=%d elapsedMs=%d%n",seed,p,site,Arrays.toString(codes),protectedShore,deep,(System.nanoTime()-start)/1000000);
        }
        System.out.println("MARINE_SHELF_IDENTITY_DEPTH_SEDIMENT_COLONY_BOUNDARY_PASS");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private MarineShelfCheck(){}
}
