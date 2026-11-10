package blockbranch.realistic;

/** Frozen exact equations, priority, state codes and ordered output for affected crowns. */
public final class TreeRasterQualification {
    public static void main(String[] args) {
        int checked=0;long voxels=0;
        for(int kind=0;kind<3;kind++)for(int height=8;height<=(kind==2?22:20);height++)for(int n=0;n<128;n++) {
            var root=new WoodlandTrees.Root(-2097100,64+n%601,2097100,kind==1,height,variant(n,height),0);
            var expected=WoodlandShapeReference.shape(root,kind==2);
            var actual=kind==2?WoodlandTrees.humidTree(root):WoodlandTrees.tree(root);
            if(!expected.equals(actual))throw new AssertionError("Exact woodland voxel mismatch "+root+" humid="+(kind==2));
            checked++;voxels+=actual.size();
        }
        for(int family:new int[]{1,2,TemperateForestVariants.BIRCH_FAMILY})for(boolean birch:new boolean[]{false,true})
            for(int height=6;height<=(birch?(family==TemperateForestVariants.BIRCH_FAMILY?30:25):(family==1?22:12));height++)for(int n=0;n<64;n++) {
                var root=new WoodlandTrees.Root(2097100,664,-17,birch,height,variant(n,height),family);
                var expected=ForestTreesReference.tree(root);var actual=ForestTrees.tree(root);
                if(!expected.equals(actual))throw new AssertionError("Exact forest voxel mismatch "+root);
                checked++;voxels+=actual.size();
            }
        // Veteran oaks above 22 m scale their crowns with height, so the frozen oracle pins only the unscaled range;
        // the taller cohort must still be a non-empty tree inside the raster grid and the generated height ceiling.
        int tall=0;
        for(int height=23;height<=ForestTrees.MAX_HEIGHT-6;height++)for(int n=0;n<64;n++) {
            var root=new WoodlandTrees.Root(2097100,664,-17,false,height,variant(n,height),1);
            var actual=ForestTrees.tree(root);
            if(actual.isEmpty())throw new AssertionError("Empty tall oak "+root);
            for(var voxel:actual)if(Math.abs(voxel.x()-root.x())>WoodlandTrees.RADIUS||Math.abs(voxel.z()-root.z())>WoodlandTrees.RADIUS||voxel.y()<root.y()||voxel.y()>=root.y()+ForestTrees.MAX_HEIGHT)throw new AssertionError("Tall oak escapes raster "+root+" "+voxel);
            tall++;
        }
        System.out.println("PASS "+checked+" exact ordered woodland/humid/forest trees / "+voxels+" voxels; generated height ranges, signed variants and translated coordinates; "+tall+" bounded tall oaks");
    }
    private static int variant(int n,int height){return switch(n){case 0->Integer.MIN_VALUE;case 1->Integer.MAX_VALUE;case 2->0;case 3->-1;default->(int)OakBirchWoodland.mix(n*1297L+height*997L);};}
}
