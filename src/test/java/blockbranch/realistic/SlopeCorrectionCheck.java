package blockbranch.realistic;
import java.util.*;
import java.util.function.BiFunction;

/** New terrain support, climate-led altitude edges, retained habitats and deterministic added canopy. */
public final class SlopeCorrectionCheck {
    public static void main(String[]args){
        require(LowlandMeadow.contains(0,0,(x,z)->site(263+(int)Math.round(x*.25),true)),"Smooth moderate soil slope accepted");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263+(int)Math.round(x*.40),true)),"Steeper exposed terrain retained");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263+(int)Math.round(x*.25)+(x>0?4:0),true)),"Local scarp hidden by broad slope remains bare");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263+(int)Math.round(x*.25)+(x==0&&z==0?5:0),true)),"Abrupt convex crest remains bare");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263+(int)Math.round(x*.25),!(x==8&&z==0))),"Wet/reserved breaks interrupt new soil support");
        require(!LowlandMeadow.contains(0,0,(x,z)->site(263+(int)Math.round((x+z)*.25),true)),"Diagonal slope cap applies to gradient magnitude");
        var field=new ClimateField(424242);
        for(boolean boreal:new boolean[]{true,false})for(int elevation:boreal?new int[]{0,2000}:new int[]{0,1600}){
            var flat=flat(elevation);int[] point=null;
            outer:for(int z=-1900000;z<=1900000;z+=128000)for(int x=-1900000;x<=1900000;x+=128000){var c=field.sample(x,z,flat);if(boreal?BorealWoodland.climateSuitable(c):DrySteppe.climateSuitable(c)){point=new int[]{x,z};break outer;}}
            require(point!=null,"Synthetic climate-compatible old-altitude exclusion found");int x=point[0],z=point[1];
            boolean selected=boreal?BorealWoodland.contains(424242,x,z,flat,(wx,wz)->false,field):DrySteppe.contains(x,z,flat,(wx,wz)->false,field);require(selected,"Stable climate-suitable terrain accepted beyond old altitude band");
            var high=flat(elevation+3000);require(Math.abs(field.sample(x,z,flat).meanTemperatureC()-field.sample(x,z,high).meanTemperatureC()-18)<1e-9,"Existing 6 C/km lapse supplies the altitude constraint");
            require(!(boreal?BorealWoodland.contains(424242,x,z,high,(wx,wz)->false,field):DrySteppe.contains(x,z,high,(wx,wz)->false,field)),"Additional lapse cooling still excludes unsuitable highland");
            BiFunction<Integer,Integer,DrainageNetwork.Sample> scarp=(wx,wz)->new DrainageNetwork.Sample(63+elevation+(int)Math.round((wx-Math.floorDiv(x,4)*4)*.25)+(wx>Math.floorDiv(x,4)*4?4:0),DrainageNetwork.DRY,0,false);
            require(!(boreal?BorealWoodland.contains(424242,x,z,scarp,(wx,wz)->false,field):DrySteppe.contains(x,z,scarp,(wx,wz)->false,field)),"New altitude domain excludes abrupt local scarps");
            if(!boreal){int qx=Math.floorDiv(x,4)*4,qz=Math.floorDiv(z,4)*4;BiFunction<Integer,Integer,DrainageNetwork.Sample> coast=(wx,wz)->wx==qx+2048&&wz==qz?new DrainageNetwork.Sample(60,64,0,false):flat.apply(wx,wz);require(!DrySteppe.contains(x,z,coast,(wx,wz)->false,field),"Actual sea crossing still excludes continental steppe");}
        }
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);
            for(var kind:SlopeCorrection.Kind.values()){
                var p=blockbranch.realistic.SlopeCorrectionFixtures.fixture(kind,d,seed);require(SlopeCorrection.added(kind,seed,p.x(),p.z(),d,c),"Actual added terrain");
                require(PlacementBaseline.habitat(seed,p.x(),p.z(),d,c).equals("placeholder"),"Actual witness was previously unassigned");
                for(int z=p.z()-16;z<=p.z()+16;z+=4)for(int x=p.x()-16;x<=p.x()+16;x+=4){var old=PlacementBaseline.habitat(seed,x,z,d,c);var now=PlacementBaseline.actual(seed,x,z,d,c);require(old.equals("placeholder")||old.equals(now),"Nearby existing habitats retained");}
                if(kind==SlopeCorrection.Kind.BOREAL){int cx=p.x()>>4,cz=p.z()>>4;var baseline=new HashMap<String,int[]>();for(int z=cz-1;z<=cz+1;z++)for(int x=cx-1;x<=cx+1;x++)baseline.put(x+","+z,WoodlandTrees.column(seed,x,z,d,c));require(baseline.values().stream().anyMatch(a->a.length>0),"Added highland/coastal woodland has real canopy");
                    var merged=new HashMap<String,Integer>();for(var entry:baseline.entrySet()){var key=entry.getKey().split(",");int bx=Integer.parseInt(key[0])*16,bz=Integer.parseInt(key[1])*16;for(int packed:entry.getValue()){int position=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS;merged.put((bx+(position&15))+","+((position>>>8)+TerrainShape.FLOOR)+","+(bz+((position>>>4)&15)),packed&blockbranch.cubic.CanopyLayer.STATE_MASK);}}
                    for(int packed:baseline.get(cx+","+cz)){int code=packed&blockbranch.cubic.CanopyLayer.STATE_MASK;if(WoodlandTrees.logCode(code))continue;int position=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=cx*16+(position&15),z=cz*16+((position>>>4)&15),y=(position>>>8)+TerrainShape.FLOOR,distance=WoodlandTrees.leafDistance(code);boolean supported=false;for(int[]direction:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){Integer near=merged.get((x+direction[0])+","+(y+direction[1])+","+(z+direction[2]));if(near!=null&&(WoodlandTrees.logCode(near)?distance==1:WoodlandTrees.leafDistance(near)<distance))supported=true;}require(supported,"New upland canopy retains a real leaf support path across native seams");}
                    for(int z=cz+1;z>=cz-1;z--)for(int x=cx+1;x>=cx-1;x--)require(Arrays.equals(baseline.get(x+","+z),WoodlandTrees.column(seed,x,z,d,c)),"New canopy independent of chunk request order");}
                System.out.printf("SLOPE_CORRECTION_WITNESS seed=%d kind=%s point=%s elevation=%d slope=%.4f climate=%s%n",seed,kind,p,d.sample(p.x(),p.z()).height()+1-64,SlopeCorrection.slope(d,p.x(),p.z()),c.sample(p.x(),p.z(),d::sample));
            }
        }
        System.out.println("SUPPORTED_SLOPE_AND_CLIMATE_ALTITUDE_CORRECTION_PASS");
    }
    private static LowlandMeadow.Site site(int height,boolean eligible){return new LowlandMeadow.Site(height,DrainageNetwork.DRY,!eligible);}
    private static BiFunction<Integer,Integer,DrainageNetwork.Sample> flat(int elevation){return(x,z)->new DrainageNetwork.Sample(63+elevation,DrainageNetwork.DRY,0,false);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private SlopeCorrectionCheck(){}
}
