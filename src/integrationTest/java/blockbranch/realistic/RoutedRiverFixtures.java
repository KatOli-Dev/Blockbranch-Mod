package blockbranch.realistic;

import static blockbranch.realistic.RoutedRiver.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class RoutedRiverFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int i=0;i<d.size();i++){
            if(!d.river(i)||Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000)continue;
            for(double t:new double[]{.3,.5,.7}){
                int x=Math.floorDiv((int)Math.round(d.edgeX(i,t)),4)*4,z=Math.floorDiv((int)Math.round(d.edgeZ(i,t)),4)*4;
                if(!contains(seed,x,z,d,c))continue;var s=RiverChannel.site(d,x,z);var cl=c.sample(x,z,d::sample);
                if(s.depth()<3||s.depth()>9||s.channel().halfWidth()<16||s.channel().halfWidth()>48||cl.meanTemperatureC()<10||cl.meanTemperatureC()>20)continue;
                var axis=axis(d,x,z);boolean banks=true;
                for(int side:new int[]{-1,1}){var q=d.sample(x+(int)Math.round(-axis[1]*s.channel().halfWidth()*1.6*side),z+(int)Math.round(axis[0]*s.channel().halfWidth()*1.6*side));if(q.waterLevel()!=DrainageNetwork.DRY||q.height()<s.water()-1||q.height()>s.water()+16)banks=false;}
                if(banks&&cover(seed,x,z,d,c)==7)return new Point(x,z);
            }
        }
        throw new AssertionError("No natural broad River with readable dry banks");
    }
    private RoutedRiverFixtures() {}
}
