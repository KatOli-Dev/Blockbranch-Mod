package blockbranch.realistic;

import static blockbranch.realistic.Swamp.*;

/** Development-only fixture selection and camera helpers; no production registration. The climate follows the drainage network's polar gate so a gated world is searched with the climate it generates. */
public final class SwampFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed,d.polarClimateEnabled());
        for(int i=0;i<d.size();i++){
            if(d.parent(i)!=-1||!d.closed(i)||d.waterLevel(i)>664||Math.abs(d.nodeX(i))>1750000||Math.abs(d.nodeZ(i))>1750000||!climateSuitable(c.sample(d.nodeX(i),d.nodeZ(i),d::sample)))continue;
            for(int n=0;n<32;n++){double a=n*Math.PI/16,r=d.lakeShoreRadius(i,a);for(int k=16;k<=64;k++){
                int x=d.nodeX(i)+(int)(Math.cos(a)*r*k/64),z=d.nodeZ(i)+(int)(Math.sin(a)*r*k/64);if(!contains(seed,x,z,d,c))continue;
                var roots=SwampTrees.roots(seed,x-32,z-32,x+32,z+32,d,c);for(var root:roots)if(root.height()>=10&&roots.size()>4){boolean interior=true;for(int dz=-4;dz<=4;dz+=4)for(int dx=-4;dx<=4;dx+=4)if(!contains(seed,root.x()+dx,root.z()+dz,d,c))interior=false;if(interior)return new Point(root.x(),root.z());}
            }}
        }
        throw new AssertionError("No naturally rooted lake-bank Swamp stand");
    }

    /** Broader local wooded fringe witness;still a shoreline habitat,not a fabricated wetland plain. */
    static Point secondFixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed,d.polarClimateEnabled());var first=fixture(d,seed);var candidates=new java.util.ArrayList<int[]>();int lakes=0;
        for(int i=0;i<d.size()&&lakes<32;i++){
            if(d.parent(i)!=-1||!d.closed(i)||d.waterLevel(i)>664||d.waterLevel(i)<=64||Math.abs(d.nodeX(i))>1950000||Math.abs(d.nodeZ(i))>1850000||!climateSuitable(c.sample(d.nodeX(i),d.nodeZ(i),d::sample)))continue;lakes++;
            for(int n=0;n<24;n++){double a=n*Math.PI/12,r=d.lakeShoreRadius(i,a);for(int k=16;k<=64;k++){
                int x=d.nodeX(i)+(int)(Math.cos(a)*r*k/64),z=d.nodeZ(i)+(int)(Math.sin(a)*r*k/64);if(Math.hypot(x-first.x(),z-first.z())<5000||!contains(seed,x,z,d,c))continue;
                int count=0;for(int dz=-64;dz<=64;dz+=8)for(int dx=-64;dx<=64;dx+=8)if(contains(seed,x+dx,z+dz,d,c))count++;
                candidates.add(new int[]{x,z,count});break;
            }}
        }
        candidates.sort(java.util.Comparator.<int[]>comparingInt(p->p[2]).reversed());
        for(var p:candidates){var roots=SwampTrees.roots(seed,p[0]-32,p[1]-32,p[0]+32,p[1]+32,d,c);for(var root:roots)if(root.height()>=10&&roots.size()>4){boolean interior=true;for(int dz=-4;dz<=4;dz+=4)for(int dx=-4;dx<=4;dx+=4)if(!contains(seed,root.x()+dx,root.z()+dz,d,c))interior=false;if(interior)return new Point(root.x(),root.z());}}
        throw new AssertionError("No second natural Swamp stand");
    }
    private SwampFixtures() {}
}
