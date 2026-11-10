package blockbranch.realistic;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

/** Recorded same-view and interior regressions; preserves physical root and completed-soil boundaries. */
public final class WindsweptQualityCheck {
    public static void main(String[] args)throws Exception{
        long seed=424242;var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var ecology=new GroundEcology.Field(d::sample,c);
        int current=forestRoots(seed,1668109,-1676137,d,c),interior=forestRoots(seed,1659917,-1668457,d,c);
        require(current>=16,"Reviewed Forest gains woodland presence: original accepted only 8 roots");
        require(interior>=30,"High-coverage Forest interior gains a stand: original accepted no trees");
        int forest=0,scarp=0,offQuart=0;
        for(int z=-1668457-64;z<=-1668457+64;z+=4)for(int x=1659917-64;x<=1659917+64;x+=4){
            if(WindsweptUplands.surfaceKind(seed,x,z,d,c)!=2)continue;forest++;
            int h=d.sample(x,z).height();boolean abrupt=false;
            for(int[]p:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(Math.abs(d.sample(x+p[0],z+p[1]).height()-h)>1)abrupt=true;
            if(abrupt){scarp++;require(!WindsweptUplands.soil(seed,x,z,d),"Actual scarps retain exposed rock");}
        }
        require(forest>850&&scarp>0,"Interior identity and real exposed scarp controls");
        for(var r:WindsweptTrees.roots(seed,1668109-128,-1676137-128,1668109+128,-1676137+128,d,c)){
            require(WindsweptUplands.surfaceKind(seed,r.x(),r.z(),d,c)>0&&WindsweptUplands.soil(seed,r.x(),r.z(),d),"No root admitted on an uncapped physical block");
            if(WindsweptUplands.kind(seed,r.x(),r.z(),d,c)==1)require(flat(r.x(),r.z(),d),"Hills retain their conservative sparse footholds");
            if((r.x()&3)!=0||(r.z()&3)!=0)offQuart++;
        }
        require(offQuart>current/2,"Additional roots retain irregular block-coordinate placement");
        require(WindsweptUplands.surfaceKind(seed,1668130,-1676152,d,c)==0&&WindsweptUplands.surfaceMaterialKind(seed,1668130,-1676152,d,c)==2,"Unassigned supported shoulder fills the rigid material gap without changing biome routing");
        require(LowlandMeadow.surfacePotential(1668099,-1676086,d,c)&&WindsweptUplands.surfaceMaterialKind(seed,1668099,-1676086,d,c)==0,"Completed meadow soil is retained at the neighbouring shoulder");
        invariant(seed,1700059,-1427951,d,c,"2d7b679dd84eae42dfd914702d7d3f7f4f0ad3cb0d69cb066142e227cb4e7b21","73cd46978e8ca01d538d907f9a8d982ed21415e0ca0226478a8752a4eadd59a7","ee382ff0ae707e8bbc9cca88c2bf3024fd2038e1894eb1f112c4e5d4edfe2459");
        invariant(seed,1668109,-1676137,d,c,"329e16db0260a026e65be766985a4385a66eafe4f34b2fab8abe7c0d32a9ed83","b68d8c526078fe5f182702fae88823e047ccd5fcb8d7750228b9502104c4783d","a334472f8f43e839a2c308ae85d88cd0fa133ec722c8bb1bc42ea376205d3ed8");
        System.out.println("WINDSWEPT_QUALITY currentForest="+current+" interiorForest="+interior+" interiorCells="+forest+" exposedScarps="+scarp+" offQuartRoots="+offQuart);
        System.out.println("WINDSWEPT_WOODLAND_SOIL_SHOULDER_SAME_GEOMETRY_ROUTING_PASS");
    }
    private static int forestRoots(long seed,int x,int z,DrainageNetwork d,ClimateField c){return (int)WindsweptTrees.roots(seed,x-128,z-128,x+128,z+128,d,c).stream().filter(r->r.x()>=x-128&&r.x()<=x+128&&r.z()>=z-128&&r.z()<=z+128&&WindsweptUplands.kind(seed,r.x(),r.z(),d,c)==2).count();}
    private static boolean flat(int x,int z,DrainageNetwork d){int h=d.sample(x,z).height();for(int dz=-8;dz<=8;dz+=8)for(int dx=-8;dx<=8;dx+=8)if(Math.abs(d.sample(x+dx,z+dz).height()-h)>5)return false;return true;}
    private static void invariant(long seed,int x,int z,DrainageNetwork d,ClimateField c,String expectedH,String expectedW,String expectedB)throws Exception{
        var h=MessageDigest.getInstance("SHA-256");var w=MessageDigest.getInstance("SHA-256");var b=MessageDigest.getInstance("SHA-256");var buf=ByteBuffer.allocate(4);
        for(int wz=z-64;wz<z+64;wz++)for(int wx=x-64;wx<x+64;wx++){var q=d.sample(wx,wz);buf.clear();h.update(buf.putInt(q.height()).array());buf.clear();w.update(buf.putInt(q.waterLevel()).array());buf.clear();b.update(buf.putInt(WindsweptUplands.kind(seed,wx,wz,d,c)).array());}
        require(HexFormat.of().formatHex(h.digest()).equals(expectedH)&&HexFormat.of().formatHex(w.digest()).equals(expectedW)&&HexFormat.of().formatHex(b.digest()).equals(expectedB),"Exact matched-view terrain, water and biome routing retained");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private WindsweptQualityCheck(){}
}
