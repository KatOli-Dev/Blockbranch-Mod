package blockbranch.realistic;

import java.util.*;

/** Natural interior witnesses, supported non-grid acacias and retained completed climates. */
public final class SavannaUplandsCheck {
    public static void main(String[]args)throws Exception{
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var ecology=new GroundEcology.Field(d::sample,c);
            for(int kind:new int[]{1,2}){
                var p=blockbranch.realistic.SavannaUplandsFixtures.fixture(d,seed,kind);int selected=0,soil=0,rock=0,plants=0,edges=0;
                for(int z=p.z()-48;z<=p.z()+48;z++)for(int x=p.x()-48;x<=p.x()+48;x++){
                    if(SavannaUplands.surfaceKind(seed,x,z,d,c)!=kind)continue;selected++;
                    require(!Savanna.surfaceContains(seed,x,z,d,c)&&!Jungle.surfaceContains(seed,x,z,d,c)&&TropicalForestVariants.surfaceKind(seed,x,z,d,c)==0,"Completed warm vanilla biomes retained");
                    require((!LowlandMeadow.surfacePotential(x,z,d,c)||WarmWoodland.surfaceContains(seed,x,z,d,c))&&!Plains.potential(seed,x,z,d,c)&&!MontaneMeadow.contains(seed,x,z,d,c)&&!OakBirchWoodland.surfacePotential(seed,x,z,d,c)&&!BorealWoodland.surfaceContains(seed,x,z,d,c)&&!DrySteppe.surfaceContains(x,z,d,c),"Actual completed soil domains retained; overlapping meadow potential may already route to warm placeholder");
                    require(!LandVegetation.surfaceReserved(d,x,z)&&!Swamp.surfaceSoilContains(seed,x,z,d,c)&&!MangroveSwamp.exact(seed,x,z,d,c),"Specialist and wet habitats retained");
                    if(SavannaUplands.soil(seed,x,z,d)){
                        soil++;int cover=SavannaUplands.cover(seed,x,z,ecology.sample(x,z),true);require(cover>=9&&cover<=12,"Native grass/coarse soil/grass blades only");if(cover==10||cover==11)plants++;
                        if((x&3)!=0&&SavannaUplands.cover(seed,x-1,z,ecology.sample(x-1,z),true)!=cover)edges++;
                    }else rock++;
                    int h=d.sample(x,z).height();for(int[]q:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(Math.abs(d.sample(x+q[0],z+q[1]).height()-h)>1)require(!SavannaUplands.soil(seed,x,z,d),"Real metre-scale scarps retain stone");
                }
                var roots=SavannaUplands.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c);require(roots.equals(SavannaUplands.roots(seed,p.x()-64,p.z()-64,p.x()+64,p.z()+64,d,c)),"Order-independent root selection");
                var heights=new TreeSet<Integer>();int offQuart=0,logs=0,leaves=0;
                for(var root:roots){
                    require(SavannaUplands.kind(seed,root.x(),root.z(),d,c)>0&&SavannaUplands.surfaceKind(seed,root.x(),root.z(),d,c)>0&&SavannaUplands.soil(seed,root.x(),root.z(),d),"Actual physical soil and quart identity both required at every root");
                    require(SavannaUplands.cover(seed,root.x(),root.z(),ecology.sample(root.x(),root.z()),false)==9,"Tree stem has actual grass soil cap");
                    heights.add(root.height());if((root.x()&3)!=0||(root.z()&3)!=0)offQuart++;
                    for(var v:SavannaUplands.tree(root)){if(WoodlandTrees.logCode(v.code()))logs++;else leaves++;}
                }
                require(selected>1000&&soil>500&&plants>0&&edges>0,"Representative supported interior with irregular grass colonies");
                require(roots.size()>=3&&heights.size()>=2&&offQuart>roots.size()/2&&logs>0&&leaves>0,"Irregular acacia cohorts and connected foliage");
                System.out.println("SAVANNA_UPLAND seed="+seed+" kind="+kind+" point="+p+" selected="+selected+" soil="+soil+" rock="+rock+" plants="+plants+" offQuartEdges="+edges+" roots="+roots.size()+" heights="+heights);
            }
        }
        {long seed=424242;var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);
            for(int x:new int[]{1551688,1551691}){int z=-495152;require(RoutedRiver.bankContains(seed,x,z,d,c)&&SavannaUplands.kind(seed,x,z,d,c)==0&&SavannaUplands.surfaceKind(seed,x,z,d,c)==0,"Recorded River holder/fringe cannot be overwritten by warm uplands at "+x+","+z);}
            require(RoutedRiver.bankCover(seed,1551688,-495152,d,c)>0,"Recorded physical bank retains its existing material selector");
            System.out.println("SAVANNA_UPLAND_RETAINED_RIVER_BANK_SELECTOR_PASS witness=1551691,-495152 quart=1551688,-495152");
        }
        var lowlandShapes=new HashSet<String>();var uplandShapes=new HashSet<String>();
        var uplandDigest=java.security.MessageDigest.getInstance("SHA-256");
        for(int i=0;i<64;i++){
            var root=new WoodlandTrees.Root(0,0,0,false,12,(int)OakBirchWoodland.mix(i*1297L));
            lowlandShapes.add(SavannaTrees.tree(root).stream().filter(v->WoodlandTrees.logCode(v.code())).toList().toString());
            var shape=SavannaUplands.tree(root);uplandShapes.add(shape.stream().filter(v->WoodlandTrees.logCode(v.code())).toList().toString());
            for(var voxel:shape)uplandDigest.update(java.nio.ByteBuffer.allocate(16).putInt(voxel.x()).putInt(voxel.y()).putInt(voxel.z()).putInt(voxel.code()).array());
        }
        require(lowlandShapes.size()>=24&&uplandShapes.size()>=16,"Both acacia regions retain varied scaffolds");
        // Frozen from main c3e7f78 for this exact height/variant catalog: lowland
        // visual changes must not silently alter the shared upland implementation.
        require(java.util.HexFormat.of().formatHex(uplandDigest.digest()).equals("324fa0176e54bf10a0881bcc81760e1efa5ff714e48e7842893836579357f978"),"Upland geometry remains byte-for-byte unchanged");
        System.out.println("SAVANNA_UPLAND_STRUCTURE lowland="+lowlandShapes.size()+" upland="+uplandShapes.size());
        System.out.println("SAVANNA_UPLAND_NATURAL_INTERIOR_PHYSICAL_SUPPORT_MATERIAL_ORDER_PASS");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private SavannaUplandsCheck(){}
}
