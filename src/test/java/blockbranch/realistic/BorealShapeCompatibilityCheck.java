package blockbranch.realistic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;

/** Frozen pre-change shapes from 56410d2; big-endian int encoding and canonical voxel order. */
public final class BorealShapeCompatibilityCheck {
    public static void main(String[] args) {
        verify();
        System.out.println("BOREAL_WARM_BIRCH_LOG_COMPATIBILITY_PASS");
    }
    public static void verify() {
        check("young warm pine",0,9,12,"b53077d19632ff92bfa3f4e4acd723356edd7aff370ecd14fd3f82badc30d9c6");
        check("retained boreal logs",1,9,22,"13f9ba9868b2075de1910ba96455949a221a616270e29b800499d083d12f5c51");
        check("boreal birch",2,9,16,"e8cf883904856f3d810b94dcfc04dfa5252236fc4491f2900d3e81db6a92e016");
    }
    private static void check(String label,int kind,int low,int high,String expected) {
        final MessageDigest hash;
        try { hash=MessageDigest.getInstance("SHA-256"); }
        catch(NoSuchAlgorithmException ex) { throw new AssertionError(ex); }
        for(int height=low;height<=high;height++)for(int i=0;i<64;i++) {
            int variant=(int)OakBirchWoodland.mix(i*1297L);
            integer(hash,height);integer(hash,variant);
            var root=new WoodlandTrees.Root(-17,261,31,kind==2,height,variant);
            var tree=kind==0?WarmTrees.tree(new WarmTrees.Root(root.x(),root.y(),root.z(),true,height,variant)):BorealTrees.tree(root);
            var voxels=tree.stream().filter(v->kind!=1||WoodlandTrees.logCode(v.code()))
                .sorted(Comparator.comparingInt(WoodlandTrees.Voxel::y).thenComparingInt(WoodlandTrees.Voxel::z).thenComparingInt(WoodlandTrees.Voxel::x)).toList();
            integer(hash,voxels.size());
            for(var v:voxels) { integer(hash,v.x());integer(hash,v.y());integer(hash,v.z());integer(hash,v.code()); }
        }
        String actual=HexFormat.of().formatHex(hash.digest());
        if(!actual.equals(expected))throw new AssertionError(label+" changed: expected "+expected+", actual "+actual);
    }
    private static void integer(MessageDigest hash,int value) {
        hash.update((byte)(value>>>24));hash.update((byte)(value>>>16));hash.update((byte)(value>>>8));hash.update((byte)value);
    }
    private BorealShapeCompatibilityCheck() {}
}
