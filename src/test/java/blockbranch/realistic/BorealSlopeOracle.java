package blockbranch.realistic;

import java.security.MessageDigest;
import java.util.*;

/**
 * Pure observable-output oracle for boreal stem placement over the published 270 preset's terrain
 * and climate. {@code main} prints one oracle row per window; the check compares live output with the file
 * frozen from the unmodified published 270 production tree. Sites cover the sampled treeless slope
 * (seed 1) plus flat and steep boreal windows, all in cold stand domain: no copied placement rules.
 */
public final class BorealSlopeOracle {
    /** seed, x, z of a 128 x 128 m window centre. */
    static final long[][] SITES={{1,-1392000,-1616000},{1,68000,-1644000},{1,-1564000,-1452000},{424242,-972000,-1676000},{424242,1828000,-1196000}};
    static final int HALF=64;

    /** The bundled published-270 preset: every definition on, polar climate and sheet. */
    static DrainageNetwork terrain(long seed){return new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true,true,true);}

    static String describe(long seed,int x,int z)throws Exception{
        var d=terrain(seed);var c=new ClimateField(seed,true);
        return fingerprint(BorealTrees.roots(seed,x-HALF,z-HALF,x+HALF,z+HALF,d,c));
    }
    static String fingerprint(List<WoodlandTrees.Root> roots)throws Exception{
        var rootHash=MessageDigest.getInstance("SHA-256");var voxelHash=MessageDigest.getInstance("SHA-256");int voxels=0;
        for(var r:roots){
            rootHash.update((r.x()+","+r.y()+","+r.z()+","+r.birch()+","+r.height()+","+r.variant()+","+r.family()+";").getBytes());
            for(var v:BorealTrees.tree(r)){voxelHash.update((v.x()+","+v.y()+","+v.z()+","+v.code()+";").getBytes());voxels++;}
        }
        return roots.size()+" "+voxels+" "+HexFormat.of().formatHex(rootHash.digest())+" "+HexFormat.of().formatHex(voxelHash.digest());
    }
    public static void main(String[] args)throws Exception{
        for(var s:SITES)System.out.println(s[0]+" "+s[1]+" "+s[2]+" "+describe(s[0],(int)s[1],(int)s[2]));
    }
    private BorealSlopeOracle(){}
}
