package blockbranch.realistic;
import java.util.*;

/** Confirmed routes, retained margins, geometry sorting, quiet photic vegetation and deterministic quart seams. */
public final class RiverChannelCheck {
    public static void main(String[]args){
        verifyMaterialPatches();
        var warm=new ClimateField.Sample(12,.7);var quiet=new DrainageNetwork.Channel(1,106,48,34,0);
        var s=new RiverChannel.Site(100,106,quiet,false,false,.05);
        require(RiverChannel.eligible(s)&&RiverChannel.plantSuitable(s,warm),"Quiet shallow flank is suitable");
        for(var bad:List.of(new RiverChannel.Site(100,106,quiet,true,false,0),new RiverChannel.Site(100,106,quiet,false,true,0),new RiverChannel.Site(100,105,quiet,false,false,0),new RiverChannel.Site(100,101,quiet,false,false,0),new RiverChannel.Site(100,106,DrainageNetwork.Channel.NONE,false,false,0)))require(!RiverChannel.eligible(bad),"Not lake/ocean/dry/mismatched water or merely nearby channel");
        for(double gradient:new double[]{0,.0005,.001,.004,.021})for(int depth=1;depth<=12;depth++)for(double lateral:new double[]{.2,.6,.9,1.1}){
            var c=new DrainageNetwork.Channel(1,101+depth,48,lateral*48,gradient);var site=new RiverChannel.Site(100,101+depth,c,false,false,.05);int code=RiverChannel.cover(424242,13,-29,true,site,warm);
            require((code>0)==(lateral<1),"Only wetted routed ribbon receives bed materials");
            if(code>=10)require(RiverChannel.plantSuitable(site,warm),"Plants only in quiet shallow flanks");
            if(lateral<1&&gradient>.02)require(code==9,"Steep reach retains rock");
            if(lateral<1&&gradient>.003&&gradient<=.02)require(code==8,"Moderate reach has coarse bed");
            require(RiverChannel.cover(424242,13,-29,false,site,warm)==0,"Unassigned margins retain their material");
        }
        require(!RiverChannel.plantSuitable(s,new ClimateField.Sample(3.9,.7)),"Cold plant gate");
        int occupied=0,pairs=0,total=0,edgeChanges=0,innerChanges=0,edgeN=0,innerN=0;
        for(int z=-128;z<128;z++)for(int x=-128;x<128;x++){
            boolean plant=RiverChannel.cover(424242,x,z,true,s,warm)>=10,next=RiverChannel.cover(424242,x+1,z,true,s,warm)>=10;total++;if(plant)occupied++;if(plant&&next)pairs++;
            if(Math.floorMod(x+1,28)==0){edgeN++;if(plant!=next)edgeChanges++;}else{innerN++;if(plant!=next)innerChanges++;}
        }
        require(pairs/(double)total>Math.pow(occupied/(double)total,2)*1.2,"River plants form coherent colonies beyond uniform scatter");
        require(Math.abs(edgeChanges/(double)edgeN-innerChanges/(double)innerN)<.03,"No colony-cell boundary artifact");
        for(long seed:new long[]{424242,1,-17}){
            long start=System.nanoTime();var d=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);int[] counts=new int[13];int reaches=0,protectedMargins=0;var fixture=blockbranch.realistic.RiverChannelFixtures.fixture(d,seed);
            for(int i=0;i<d.size()&&reaches<48;i++){
                if(!d.river(i)||Math.abs(d.nodeX(i))>1800000||Math.abs(d.nodeZ(i))>1800000)continue;reaches++;
                for(double t:new double[]{.2,.5,.8}){
                    double x=d.edgeX(i,t),z=d.edgeZ(i,t),dx=d.edgeX(i,t+.001)-d.edgeX(i,t-.001),dz=d.edgeZ(i,t+.001)-d.edgeZ(i,t-.001),length=Math.hypot(dx,dz),width=d.riverHalfWidth(i)*(1-t)+d.riverHalfWidth(d.parent(i))*t;
                    for(double lateral=-1.2;lateral<=1.2;lateral+=.05){int wx=(int)Math.round(x-dz/length*width*lateral),wz=(int)Math.round(z+dx/length*width*lateral);boolean habitat=RiverChannel.contains(wx,wz,d);var site=RiverChannel.site(d,wx,wz);int code=RiverChannel.cover(seed,wx,wz,habitat,site,climate.sample(wx,wz,d::sample));counts[code]++;
                        require(habitat==RiverChannel.contains(Math.floorDiv(wx,4)*4,Math.floorDiv(wz,4)*4,d),"Negative/quart sampling stable");
                        if(GravelRiverMargin.contains(wx,wz,(bx,bz)->GravelRiverMargin.site(d,bx,bz))){protectedMargins++;require(!habitat,"Existing river margin retained");}
                        if(habitat)require(PlacementBaseline.definition20(seed,wx,wz,d,climate).equals("placeholder"),"Every prior habitat retained");
                        if(code>0)require(RiverChannel.eligible(site),"Material stays on raw actual route water");
                        if(code>=10)require(RiverChannel.plantSuitable(site,climate.sample(wx,wz,d::sample)),"Natural plant suitability");
                    }
                }
            }
            var p=fixture;var site=RiverChannel.site(d,p.x(),p.z());require(RiverChannel.cover(seed,p.x(),p.z(),true,site,climate.sample(p.x(),p.z(),d::sample))>=10,"Natural plant witness exists");
            var edge=blockbranch.realistic.RiverChannelFixtures.transition(d,seed);require(RiverChannel.contains(edge.insideX(),edge.insideZ(),d)&&!RiverChannel.contains(edge.outsideX(),edge.outsideZ(),d)&&Math.max(Math.abs(edge.insideX()-edge.outsideX()),Math.abs(edge.insideZ()-edge.outsideZ()))<=4,"Adjacent canonical channel/margin boundary");require(edge.equals(blockbranch.realistic.RiverChannelFixtures.transition(d,seed)),"Repeated boundary deterministic");
            require(reaches==48&&protectedMargins>0&&counts[8]>0,"Natural route and retained coarse margin witnesses");
            System.out.printf("RIVER_CHANNEL seed=%d fixture=%s site=%s reaches=%d protectedMargins=%d codes=%s elapsedMs=%d%n",seed,p,site,reaches,protectedMargins,Arrays.toString(counts),(System.nanoTime()-start)/1000000);
        }
        System.out.println("ROUTED_RIVER_IDENTITY_GRADING_QUIET_VEGETATION_BOUNDARY_PASS");
    }
    /** Hold every geometric input fixed: relief cannot hide a material stripe. */
    private static void verifyMaterialPatches() {
        var channel = new DrainageNetwork.Channel(1,106,48,12,.0008);
        var site = new RiverChannel.Site(100,106,channel,false,false,.05);
        var climate = new ClimateField.Sample(12,.7);
        int sand=0, gravel=0, changes=0, neighbors=0;
        for(int z=-128;z<0;z++)for(int x=-128;x<0;x++) {
            int material=RiverChannel.cover(424242,x,z,true,site,climate);
            if(material==7)sand++;
            if(material==8)gravel++;
            for(int[] offset:new int[][]{{1,0},{0,1}}) {
                if(material!=RiverChannel.cover(424242,x+offset[0],z+offset[1],true,site,climate))changes++;
                neighbors++;
            }
        }
        require(sand>500&&gravel>500,"Fixed lateral position must not force one sediment stripe");
        require(changes<neighbors/4,"Sediment variation forms patches, not per-block noise");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private RiverChannelCheck(){}
}
