package blockbranch.realistic;

import java.util.*;

/** Natural summit witnesses and block-resolution snow; completed slopes and river margins retained. */
public final class MountainPeaksCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var ecology=new GroundEcology.Field(d::sample,c);
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.MountainPeaksFixtures.fixture(d,seed,kind);int interior=MountainPeaks.interiorCount(seed,p.x(),p.z(),kind,d,c);require(interior>=60,"Representative natural summit interior");
                if(kind==2){
                    var v=MountainPeaks.overview(p.x(),d.sample(p.x(),p.z()).height(),p.z(),(x,z)->d.sample(x,z).height());
                    require(Math.hypot(v.x()-p.x()-.5,v.z()-p.z()-.5)<=48.00001,"Overview sampling remains inside client view radius");
                    int high=Integer.MIN_VALUE,hx=0,hz=0;
                    for(int dz=-192;dz<=192;dz+=16)for(int dx=-192;dx<=192;dx+=16)if(dx*dx+dz*dz<=192*192){int h=d.sample(p.x()+dx,p.z()+dz).height();if(h>high){high=h;hx=p.x()+dx;hz=p.z()+dz;}}
                    double crestPitch=-Math.toDegrees(Math.atan2(high-(v.y()+1.62),Math.hypot(hx+.5-v.x(),hz+.5-v.z())));
                    require(v.y()>=d.sample((int)Math.floor(v.x()),(int)Math.floor(v.z())).height()+65&&Math.abs(crestPitch-v.pitch())<16,"Actual nearby crest has clearance and vertical framing margin");
                    System.out.println("PEAK_OVERVIEW_CREST_FRAMING seed="+seed+" view="+v+" crest="+hx+","+high+","+hz);
                }
                int selected=0,snow=0,offQuartEdges=0;var codes=new HashSet<Integer>();
                for(int cz=(p.z()>>4)-1;cz<=(p.z()>>4)+1;cz++)for(int cx=(p.x()>>4)-1;cx<=(p.x()>>4)+1;cx++){
                    int[] packed=OpenSnowCover.peaksColumn(seed,cx,cz,new int[0],d,c,ecology);var at=new HashMap<Integer,Integer>();
                    for(int v:packed){require(SnowyTaiga.snow(v&blockbranch.cubic.CanopyLayer.STATE_MASK),"Only existing native snow codes added");at.put(v>>>blockbranch.cubic.CanopyLayer.STATE_BITS,v&blockbranch.cubic.CanopyLayer.STATE_MASK);}
                    for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                        int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height(),physical=MountainPeaks.surfaceKind(seed,wx,wz,d,c);
                        int code=at.getOrDefault(((h+1-TerrainShape.FLOOR)<<8)|(z*16+x),0);
                        int expected=physical==MountainPeaks.JAGGED?OpenSnowCover.layers(seed,wx,wz,ecology.sample(wx,wz),true):0;
                        require(code==(expected>0?SnowyTaiga.snowCode(expected):0),"Snow follows actual physical mountain/exposure per block");
                        if(physical==kind){
                            selected++;require(d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY&&d.channelAt(wx,wz).edge()<0,"No water or channel displaced");
                            require(!SnowySlopes.contains(wx,wz,d,c)&&!SnowySlopes.surfaceContains(wx,wz,d,c)&&!RoutedRiver.bankContains(seed,wx,wz,d,c)&&!RoutedRiver.surfaceBankContains(seed,wx,wz,d,c),"Completed snowy slopes and dry river banks retained");
                            require(!SnowyPlains.contains(wx,wz,d,c)&&!Taiga.contains(seed,wx,wz,d,c)&&!SnowyTaiga.contains(seed,wx,wz,d,c)&&OldGrowthTaiga.kind(seed,wx,wz,d,c)==0&&!MontaneMeadow.contains(seed,wx,wz,d,c),"Completed cold woodlands/plains/meadows retained");
                        }
                        if(code>0){snow++;codes.add(code);if((wx&3)!=0&&OpenSnowCover.layers(seed,wx-1,wz,ecology.sample(wx-1,wz),true)!=expected)offQuartEdges++;}
                    }
                    require(Arrays.equals(packed,OpenSnowCover.peaksColumn(seed,cx,cz,new int[0],d,c,ecology)),"Order-independent snow snapshots");
                }
                require(selected>1200,"Owned native-material interior");
                if(kind==2)require(snow>100&&codes.size()>1&&offQuartEdges>0,"Variable grounded snow: count="+snow+" codes="+codes+" offQuartEdges="+offQuartEdges+" point="+p);
                System.out.println("PEAK_CHECK seed="+seed+" kind="+kind+" point="+p+" interior="+interior+"/81 selected="+selected+" snow="+snow+" layers="+codes+" offQuartEdges="+offQuartEdges);
            }
        }
        System.out.println("PEAK_NATURAL_SUMMIT_PHYSICAL_SNOW_RETAINED_DOMAINS_ORDER_PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private MountainPeaksCheck(){}
}
