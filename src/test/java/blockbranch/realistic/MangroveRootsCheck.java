package blockbranch.realistic;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Saved-gate numbering, the frozen published mangrove canopy and the arched prop-root geometry.
 * The published roots reached the ground through one shared knee ring and square vertical legs; the gated
 * arch leaves each trunk at its own height and drops thinly. Both variants are measured on the same stems.
 */
public final class MangroveRootsCheck {
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static void main(String[] args)throws Exception{
        schema();
        published();
        morphology();
        System.out.println("MANGROVE_ROOTS_GATED_PASS");
    }
    /** Saved schema 273 exists only as the complete boreal-slope 272 plus the explicit definition. */
    private static void schema(){
        for(int saved=82;saved<=280;saved++){
            require(MangroveRootsSchema.schema(saved,0)==saved,"Absent definition retains "+saved);
            require(MangroveRootsSchema.baseSchema(saved,0)==(saved==273?Integer.MAX_VALUE:saved),"Absent definition never reads 273 ("+saved+")");
            require(MangroveRootsSchema.matches(saved,1)==(saved==273),"Only 273 reads under the definition ("+saved+")");
            require(!MangroveRootsSchema.matches(saved,2)&&!MangroveRootsSchema.matches(saved,-1),"Unknown definitions never match ("+saved+")");
            if(saved!=272){boolean threw=false;try{MangroveRootsSchema.schema(saved,1);}catch(IllegalArgumentException e){threw=true;}require(threw,"Only complete 272 is wrapped ("+saved+")");}
        }
        require(MangroveRootsSchema.schema(272,1)==273&&MangroveRootsSchema.baseSchema(273,1)==272,"Complete independent wrapper");
    }
    /** The published path, by default and by explicit gate off, stays bit-identical to the frozen columns. */
    private static void published()throws Exception{
        int rows=0;
        try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(MangroveRootsCheck.class.getResourceAsStream("/fixtures/mangrove-roots-272/published.txt"))))){
            for(String line;(line=in.readLine())!=null;){
                if(line.startsWith("#")||line.isBlank())continue;
                var f=line.split(" ");var site=site(f[0],Long.parseLong(f[1]));
                int cx=Integer.parseInt(f[2]),cz=Integer.parseInt(f[3]);
                for(boolean explicitOff:new boolean[]{false,true}){
                    var row=fingerprint(f[0],Long.parseLong(f[1]),cx,cz,site,explicitOff);
                    require(row.equals(f[4]+" "+f[5]),"Published mangrove columns changed at "+f[0]+" "+f[1]+": "+row+" vs frozen "+f[4]+" "+f[5]);
                }
                rows++;
            }
        }
        require(rows==4,"Every frozen mangrove window is checked");
    }
    /** The gated arch keeps the stems, bounds and grounding but breaks the shelf-and-wall mass. */
    private static void morphology(){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var p=MangroveSwampFixtures.fixture(d,seed,false);
            var stems=MangroveTrees.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c);
            var old=new Metrics();var arched=new Metrics();var arches=new Arches();
            for(var stem:stems){
                var a=MangroveTrees.tree(stem,d,false);var b=MangroveTrees.tree(stem,d,true);
                if(!b.isEmpty())arches.add(stem,MangroveTrees.archedRootCells(stem,d),d);
                // Placement is shared; a tree is dropped when fewer than two roots ground, which can differ per shape.
                if(!a.isEmpty())old.add(stem,a,d);
                if(!b.isEmpty())arched.add(stem,b,d);
            }
            require(old.trees>=40&&arched.trees>=old.trees-old.trees/5,"Arched roots keep the stand ("+seed+": "+old.trees+" -> "+arched.trees+")");
            require(arched.bulk()<=old.bulk()*.70,"Thin arches leave less square root mass ("+seed+": "+old.bulk()+" -> "+arched.bulk()+")");
            require(arched.densestLayer()<=old.densestLayer()*.85,"Roots no longer share one shelf layer ("+seed+": "+old.densestLayer()+" -> "+arched.densestLayer()+")");
            require(arched.plates()<=old.plates()*.60,"Fewer flat 2 x 2 root plates ("+seed+": "+old.plates()+" -> "+arched.plates()+")");
            require(arched.outer>=old.outer*1.5,"Roots reach beyond the trunk collar instead of piling against it ("+seed+": "+old.outer+" -> "+arched.outer+")");
            require(arched.perTree()<=old.perTree()*1.60&&arched.perTree()>=old.perTree(),"Bounded root volume ("+seed+": "+old.perTree()+" -> "+arched.perTree()+")");
            require(old.maxReach<=5&&arched.maxReach<=6,"Root reach stays inside the six-block halo ("+seed+": "+old.maxReach+" -> "+arched.maxReach+")");
            require(arched.minHeightAboveGround>=1,"Roots stay above the ground ("+seed+")");
            require(arches.roots>=arches.trees*2,"Every kept tree anchors at least two arched roots ("+seed+")");
            require(arches.touching()<=.04,"Roots beyond the collar rarely touch another root ("+seed+": "+arches.touching()+")");
            System.out.printf("MANGROVE_ARCHES seed=%d roots=%d outerCells=%d touchingAnotherRoot=%.3f%n",seed,arches.roots,arches.outer,arches.touching());
            System.out.printf("MANGROVE_ROOTS seed=%d trees=%d->%d rootVoxelsPerTree=%.1f->%.1f bulkShare=%.2f->%.2f plateShare=%.2f->%.2f densestLayerShare=%.2f->%.2f beyond2PerTree=%.1f->%.1f maxReach=%d->%d%n",
                seed,old.trees,arched.trees,old.perTree(),arched.perTree(),old.bulk(),arched.bulk(),old.plates(),arched.plates(),old.densestLayer(),arched.densestLayer(),old.outer/(double)old.trees,arched.outer/(double)arched.trees,old.maxReach,arched.maxReach);
        }
    }
    /** Per-root accounting from the accepted arch paths: face-connected cells from the trunk to a ground foot. */
    private static final class Arches {
        int trees,roots;long outer,touching;
        void add(MangroveTrees.Stem stem,List<List<int[]>> paths,DrainageNetwork d){
            trees++;var owner=new HashMap<Long,Integer>();int index=0;
            for(var path:paths){
                roots++;int[] first=path.getFirst(),last=path.getLast();
                require(first[0]==stem.x()&&first[2]==stem.z()&&first[1]>=stem.y()&&first[1]<=stem.y()+2,"Each arch leaves the trunk within two blocks of the stem base");
                require(last[1]==d.sample(last[0],last[2]).height()+1,"Each arch ends on a ground foot");
                var seen=new HashSet<Long>();seen.add(key(first[0],first[1],first[2]));
                for(int i=1;i<path.size();i++){
                    var c=path.get(i);boolean joined=false;
                    for(var q:DIR)if(seen.contains(key(c[0]+q[0],c[1]+q[1],c[2]+q[2])))joined=true;
                    require(joined,"Each arch cell touches an earlier cell of its root face to face");seen.add(key(c[0],c[1],c[2]));
                }
                for(var cell:path)owner.putIfAbsent(key(cell[0],cell[1],cell[2]),index);
                index++;
            }
            int own=0;
            for(var path:paths){
                for(var cell:path){
                    if(Math.max(Math.abs(cell[0]-stem.x()),Math.abs(cell[2]-stem.z()))<2)continue;
                    outer++;
                    if(owner.get(key(cell[0],cell[1],cell[2]))!=own){touching++;continue;}
                    for(var q:DIR){Integer other=owner.get(key(cell[0]+q[0],cell[1]+q[1],cell[2]+q[2]));if(other!=null&&other!=own){touching++;break;}}
                }
                own++;
            }
        }
        double touching(){return outer==0?0:touching/(double)outer;}
    }
    private static final class Metrics {
        int trees,maxReach,minHeightAboveGround=Integer.MAX_VALUE;long roots,bulk,plates,outer;double layerShare;
        void add(MangroveTrees.Stem stem,List<WoodlandTrees.Voxel> voxels,DrainageNetwork d){
            trees++;var set=new HashSet<Long>();var rootVoxels=new ArrayList<WoodlandTrees.Voxel>();
            for(var v:voxels)if(v.code()==MangroveTrees.ROOT||v.code()==MangroveTrees.WET_ROOT){rootVoxels.add(v);set.add(key(v.x(),v.y(),v.z()));}
            var perLayer=new HashMap<Integer,Integer>();
            for(var v:rootVoxels){
                roots++;perLayer.merge(v.y(),1,Integer::sum);minHeightAboveGround=Math.min(minHeightAboveGround,v.y()-d.sample(v.x(),v.z()).height());
                int n=0;for(var q:DIR)if(set.contains(key(v.x()+q[0],v.y()+q[1],v.z()+q[2])))n++;if(n>=3)bulk++;
                int reach=Math.max(Math.abs(v.x()-stem.x()),Math.abs(v.z()-stem.z()));maxReach=Math.max(maxReach,reach);if(reach>=3)outer++;
                if(plate(set,v.x(),v.y(),v.z()))plates++;
            }
            int most=0;for(int count:perLayer.values())most=Math.max(most,count);layerShare+=most/(double)Math.max(1,rootVoxels.size());
        }
        /** A root cell belongs to a flat plate when it is a corner of a 2 x 2 block of root cells in any axis plane. */
        private static boolean plate(Set<Long> set,int x,int y,int z){
            for(int a=-1;a<=0;a++)for(int b=-1;b<=0;b++){
                if(square(set,x+a,y+b,z,1,0,0,0,1,0)||square(set,x+a,y,z+b,1,0,0,0,0,1)||square(set,x,y+a,z+b,0,1,0,0,0,1))return true;
            }
            return false;
        }
        private static boolean square(Set<Long> set,int x,int y,int z,int ax,int ay,int az,int bx,int by,int bz){
            return set.contains(key(x,y,z))&&set.contains(key(x+ax,y+ay,z+az))&&set.contains(key(x+bx,y+by,z+bz))&&set.contains(key(x+ax+bx,y+ay+by,z+az+bz));
        }
        double plates(){return plates/(double)roots;}
        double perTree(){return roots/(double)trees;}
        double bulk(){return bulk/(double)roots;}
        double densestLayer(){return layerShare/trees;}
    }
    private static long key(int x,int y,int z){return ((long)x*73856093L)^((long)y*19349663L)^((long)z*83492791L)^(((long)x<<40)^((long)z<<20)^y);}
    private record Site(DrainageNetwork d,ClimateField c){}
    private static Site site(String kind,long seed){
        if(kind.equals("audit"))return new Site(BorealSlopeOracle.terrain(seed),new ClimateField(seed,true));
        return new Site(new DrainageNetwork(new TerrainShape(seed),seed),new ClimateField(seed));
    }
    private static String fingerprint(String kind,long seed,int cx,int cz,Site s,boolean explicitOff)throws Exception{
        var md=MessageDigest.getInstance("SHA-256");int ints=0;
        for(int z=cz-2;z<=cz+2;z++)for(int x=cx-2;x<=cx+2;x++){
            var col=explicitOff?MangroveTrees.column(seed,x,z,s.d(),s.c(),false):MangroveTrees.column(seed,x,z,s.d(),s.c());
            ints+=col.length;md.update(Arrays.toString(col).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return ints+" "+HexFormat.of().formatHex(md.digest());
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private MangroveRootsCheck(){}
}
