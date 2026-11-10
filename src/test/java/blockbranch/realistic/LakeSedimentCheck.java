package blockbranch.realistic;
import java.util.*;
/** Lake identity, depth/slope sorting, light-depth proxies, source-fluid-safe vegetation and seeded seams. */
public final class LakeSedimentCheck {
    public static void main(String[]args){
        var warm=new ClimateField.Sample(12,.65);
        for(var site:List.of(new LakeSediment.Site(100,100,false,false,false,0),new LakeSediment.Site(100,120,false,true,false,0),new LakeSediment.Site(100,120,false,false,true,0),new LakeSediment.Site(100,101,true,false,false,0),new LakeSediment.Site(100,120,true,true,false,0),new LakeSediment.Site(100,120,true,false,true,0)))require(!LakeSediment.eligible(site),"Dry, ocean and routed river water are never lakes");
        for(int depth=1;depth<=20;depth++)for(double slope:new double[]{0,.18,.20001,.50001}){
            var s=new LakeSediment.Site(100,101+depth,true,false,false,slope);
            require(LakeSediment.eligible(s),"All actual lake depths remain eligible, including steep rock beds");
            require(LakeSediment.plantSuitable(s,warm)==(depth>=2&&depth<=5&&slope<=.18),"Explicit photic shallow/gentle plant window");
            require(!LakeSediment.plantSuitable(s,new ClimateField.Sample(3.99,.7)),"Cold open water has no generated pondweed proxy");
            int code=LakeSediment.cover(424242,13,-29,true,s,warm);require(code>=5&&code<=12,"Bounded aquatic palette");
            if(slope>.5)require(code==9,"Steep slopes remain exposed rock");else if(slope>.2)require(code==8,"Coarser gravel on more exposed bed slopes");
            if(code>=10)require(LakeSediment.plantSuitable(s,warm),"No deep/dark/steep plant placement");
            require(LakeSediment.cover(424242,13,-29,false,s,warm)==0,"Excluded habitats retain old bed");
        }
        var steady=new LakeSediment.Site(100,105,true,false,false,.08);int occupied=0,pairs=0,total=0,edgeChanges=0,innerChanges=0,edgeN=0,innerN=0;
        for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
            boolean plant=LakeSediment.cover(424242,x,z,true,steady,warm)>=10,next=LakeSediment.cover(424242,x+1,z,true,steady,warm)>=10;
            total++;if(plant)occupied++;if(plant&&next)pairs++;if(Math.floorMod(x+1,24)==0){edgeN++;if(plant!=next)edgeChanges++;}else{innerN++;if(plant!=next)innerChanges++;}
        }
        require(pairs/(double)total>Math.pow(occupied/(double)total,2)*1.2,"Macrophytes have coherent colonies beyond uniform scatter");
        require(Math.abs(edgeChanges/(double)edgeN-innerChanges/(double)innerN)<.03,"No planted-cell boundary artifact");
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);var p=blockbranch.realistic.LakeSedimentFixtures.fixture(d,seed);int[] counts=new int[13];int lakes=0,plants=0,reed=0,riverRejected=0;long start=System.nanoTime();
            for(int i=0;i<d.size()&&lakes<12;i++){
                if(d.parent(i)!=-1||!d.closed(i)||Math.abs(d.nodeX(i))>1800000||Math.abs(d.nodeZ(i))>1800000)continue;lakes++;
                for(int angle=0;angle<32;angle++){
                    double a=angle*Math.PI/16,radius=d.lakeShoreRadius(i,a);
                    for(double r=0;r<radius*1.05;r+=8){
                        int x=d.nodeX(i)+(int)(Math.cos(a)*r),z=d.nodeZ(i)+(int)(Math.sin(a)*r);
                        var raw=d.sample(x,z);var site=LakeSediment.site(d,x,z);boolean habitat=LakeSediment.contains(x,z,d);
                        require(habitat==LakeSediment.contains(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d),"Seeded negative/native quart boundaries");
                        boolean margin=InlandLakeReedbed.contains(x,z,(wx,wz)->InlandLakeReedbed.site(d,wx,wz));if(margin){reed++;require(!habitat,"Existing reedbed assignment retained");}
                        int c=LakeSediment.cover(seed,x,z,habitat,site,climate.sample(x,z,d::sample));counts[c]++;
                        if(c>0){require(d.terminalLakeWater(x,z,raw)&&!d.oceanWater(x,z,raw)&&d.channelAt(x,z).edge()<0,"Confirmed lake, never ocean or catchment river");require(PlacementBaseline.definition19(seed,x,z,d,climate).equals("placeholder"),"All thirteen previous habitat assignments retained");}
                        if(c>=10){plants++;require(site.depth()>=2&&site.depth()<=5&&site.slope()<=.18,"Real shallow/gentle plant placement");}
                    }
                }
            }
            for(int i=0;i<d.size()&&riverRejected<4;i++)if(d.river(i)){
                int x=(int)Math.round(d.edgeX(i,.5)),z=(int)Math.round(d.edgeZ(i,.5));var s=d.sample(x,z);
                if(s.waterLevel()==DrainageNetwork.DRY||d.terminalLakeWater(x,z,s))continue;require(!LakeSediment.contains(x,z,d),"Routed rivers including closed-catchment inflows stay separate");riverRejected++;
            }
            require(lakes>3&&plants>0&&reed>0&&riverRejected>0,"Real lakes, shallow vegetation, protected reeds and rivers sampled");
            System.out.println("LAKE_NATURAL_SPECTRUM "+seed+" "+Arrays.toString(counts));
            require(counts[5]>0&&counts[6]>0&&counts[7]>0&&counts[8]>0,"Natural fine/clay/sand/gravel spectrum");
            var edge=blockbranch.realistic.LakeSedimentFixtures.transition(d,seed);require(LakeSediment.contains(edge.insideX(),edge.insideZ(),d)&&!LakeSediment.contains(edge.outsideX(),edge.outsideZ(),d)&&Math.max(Math.abs(edge.insideX()-edge.outsideX()),Math.abs(edge.insideZ()-edge.outsideZ()))<=4,"Adjacent lake/margin boundary");
            require(edge.equals(blockbranch.realistic.LakeSedimentFixtures.transition(d,seed)),"Stable seeded boundary");
            var site=LakeSediment.site(d,p.x(),p.z());require(LakeSediment.cover(seed,p.x(),p.z(),true,site,climate.sample(p.x(),p.z(),d::sample))>=10,"Natural plant fixture");
            for(int value:WoodlandTrees.column(seed,p.x()>>4,p.z()>>4,d,climate)){int key=value>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=(p.x()>>4)*16+(key&15),z=(p.z()>>4)*16+((key>>>4)&15);require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY,"Water columns remain canopy-free even in mixed shore chunks");}
            System.out.printf("LAKE_SEDIMENT seed=%d fixture=%s site=%s climate=%s cover=%s lakes=%d plants=%d preservedReed=%d elapsedMs=%d%n",seed,p,site,climate.sample(p.x(),p.z(),d::sample),Arrays.toString(counts),lakes,plants,reed,(System.nanoTime()-start)/1000000);
        }
        System.out.println("OPEN_INLAND_LAKE_IDENTITY_DEPTH_SEDIMENT_PLANT_PASS");
    }
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    private LakeSedimentCheck(){}
}
